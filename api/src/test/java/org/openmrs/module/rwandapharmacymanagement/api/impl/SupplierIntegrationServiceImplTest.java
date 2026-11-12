package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.SupplierApiClient;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierAuthenticationResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierMatchedPrice;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierPriceLookupResponse;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.model.Supplier;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrder;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrderLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SupplierIntegrationServiceImplTest {

    private SupplierApiClient supplierApiClient;
    private SupplierIntegrationServiceImpl service;

    @Before
    public void setUp() {
        Context.setUserContext(new UserContext(null));
        supplierApiClient = Mockito.mock(SupplierApiClient.class);
        IntegrationLogService integrationLogService = Mockito.mock(IntegrationLogService.class);
        Mockito.when(integrationLogService.saveLog(ArgumentMatchers.any(IntegrationLog.class)))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
        service = new SupplierIntegrationServiceImpl();
        service.setSupplierApiClient(supplierApiClient);
        service.setIntegrationLogService(integrationLogService);
        service.setPharmacyService(Mockito.mock(PharmacyService.class));
        service.setAdministrationService(Mockito.mock(AdministrationService.class));
    }

    @After
    public void tearDown() {
        Context.clearUserContext();
    }

    @Test
    public void lineWithoutProgramShouldBeRefused() {
        PurchaseOrder order = apiOrder(null, line("NPC1", "Amoxicillin", ""));

        String error = service.getPurchaseOrderSubmissionError(order);

        Assert.assertNotNull(error);
        Assert.assertTrue(error, error.contains("no program"));
    }

    @Test
    public void orderMixingProgramsShouldBeRefused() {
        PurchaseOrder order = apiOrder(null, line("NPC1", "Amoxicillin", "CA"), line("NPC2", "Etamsylate", "CP"));

        String error = service.getPurchaseOrderSubmissionError(order);

        Assert.assertNotNull(error);
        Assert.assertTrue(error, error.contains("mixes programs"));
    }

    @Test
    public void orderProgramDifferentFromItsLinesShouldBeRefused() {
        PurchaseOrder order = apiOrder("CP", line("NPC1", "Amoxicillin", "CA"));

        String error = service.getPurchaseOrderSubmissionError(order);

        Assert.assertNotNull(error);
        Assert.assertTrue(error, error.contains("differs from the purchase order's program"));
    }

    @Test
    public void singleProgramOrderShouldPassAndTakeThatProgram() {
        PurchaseOrder order = apiOrder(null, line("NPC1", "Amoxicillin", "CA"), line("NPC2", "Azithromycin", "ca"));

        Assert.assertNull(service.getPurchaseOrderSubmissionError(order));
        Assert.assertEquals("CA", order.getProgram());
    }

    @Test
    public void missingPriceForAnyNpcCodeShouldBlockTheWholeOrder() throws Exception {
        PurchaseOrder order = apiOrder(null, line("NPC1", "Amoxicillin", "CA"), line("NPC2", "Azithromycin", "CA"));
        SupplierAuthenticationResponse authentication = new SupplierAuthenticationResponse();
        authentication.setAuthenticated(true);
        authentication.setToken("token");
        Mockito.when(supplierApiClient.authenticate(ArgumentMatchers.<String>any(), ArgumentMatchers.<String>any(),
                ArgumentMatchers.<String>any(), ArgumentMatchers.<Integer>any(), ArgumentMatchers.<Integer>any()))
                .thenReturn(authentication);
        SupplierMatchedPrice price = new SupplierMatchedPrice();
        price.setNpcCode("NPC1");
        price.setSellingPrice(Double.valueOf(120));
        SupplierPriceLookupResponse response = new SupplierPriceLookupResponse();
        response.setMatchedPrices(Collections.singletonList(price));
        response.setUnmatchedNpcCodes(Collections.singletonList("NPC2"));
        Mockito.when(supplierApiClient.lookupPrices(ArgumentMatchers.<String>any(), ArgumentMatchers.<String>any(),
                ArgumentMatchers.<String>any(), ArgumentMatchers.<Integer>any(), ArgumentMatchers.<Integer>any()))
                .thenReturn(response);

        IntegrationLog log = service.submitPurchaseOrder(order);

        Assert.assertEquals(IntegrationStatus.FAILED, log.getStatus());
        Assert.assertEquals("FAILED", order.getDispatchStatus());
        ArgumentCaptor<String> lookupPayload = ArgumentCaptor.forClass(String.class);
        Mockito.verify(supplierApiClient).lookupPrices(ArgumentMatchers.<String>any(), ArgumentMatchers.<String>any(),
                lookupPayload.capture(), ArgumentMatchers.<Integer>any(), ArgumentMatchers.<Integer>any());
        Assert.assertTrue(lookupPayload.getValue(), lookupPayload.getValue().contains("\"program\":\"CA\""));
        Mockito.verify(supplierApiClient, Mockito.never()).submitOrder(ArgumentMatchers.<String>any(),
                ArgumentMatchers.<String>any(), ArgumentMatchers.<String>any(), ArgumentMatchers.<Integer>any(),
                ArgumentMatchers.<Integer>any());
    }

    @Test
    public void supplierOrderLineWithoutProgramShouldNotBeSent() throws Exception {
        SupplierOrder supplierOrder = new SupplierOrder();
        SupplierOrderLine line = new SupplierOrderLine();
        line.setSupplierProductCode("NPC1");
        line.setSupplierProductName("Amoxicillin");
        supplierOrder.setLines(new ArrayList<SupplierOrderLine>(Collections.singletonList(line)));

        IntegrationLog log = service.submitSupplierOrder(supplierOrder, apiSupplier());

        Assert.assertEquals(IntegrationStatus.FAILED, log.getStatus());
        Assert.assertTrue(log.getSummary(), log.getSummary().contains("no program"));
        Mockito.verify(supplierApiClient, Mockito.never()).submitOrder(ArgumentMatchers.<String>any(),
                ArgumentMatchers.<String>any(), ArgumentMatchers.<String>any(), ArgumentMatchers.<Integer>any(),
                ArgumentMatchers.<Integer>any());
    }

    @Test
    public void fractionalQuantityShouldBeRefusedAndOnlyThatLineFlagged() {
        PurchaseOrderItem whole = line("NPC1", "Amoxicillin", "CA");
        PurchaseOrderItem fractional = line("NPC2", "Azithromycin", "CA");
        fractional.setQuantity(new BigDecimal("2.5"));
        PurchaseOrder order = apiOrder(null, whole, fractional);

        java.util.Map<PurchaseOrderItem, String> problems = service.getPurchaseOrderLineProblems(order);
        String error = service.getPurchaseOrderSubmissionError(order);

        Assert.assertEquals(1, problems.size());
        Assert.assertTrue(problems.get(fractional), problems.get(fractional).contains("must be a whole number"));
        Assert.assertNotNull(error);
        Assert.assertTrue(error, error.contains("NPC2"));
    }

    @Test
    public void wholeQuantityWrittenWithDecimalsShouldBeAccepted() {
        PurchaseOrderItem item = line("NPC1", "Amoxicillin", "CA");
        item.setQuantity(new BigDecimal("10.00"));

        Assert.assertNull(service.getPurchaseOrderSubmissionError(apiOrder(null, item)));
    }

    @Test
    public void mixedProgramsShouldFlagEveryLine() {
        PurchaseOrderItem first = line("NPC1", "Amoxicillin", "CA");
        PurchaseOrderItem second = line("NPC2", "Etamsylate", "CP");

        java.util.Map<PurchaseOrderItem, String> problems = service.getPurchaseOrderLineProblems(apiOrder(null, first, second));

        Assert.assertTrue(problems.containsKey(first));
        Assert.assertTrue(problems.containsKey(second));
    }

    @Test
    public void supplierOrderWithFractionalQuantityShouldNotBeSent() throws Exception {
        SupplierOrder supplierOrder = new SupplierOrder();
        SupplierOrderLine line = new SupplierOrderLine();
        line.setSupplierProductCode("NPC1");
        line.setSupplierProductName("Amoxicillin");
        line.setProgramName("CA");
        line.setOrderedQuantity(new BigDecimal("1.5"));
        supplierOrder.setLines(new ArrayList<SupplierOrderLine>(Collections.singletonList(line)));

        IntegrationLog log = service.submitSupplierOrder(supplierOrder, apiSupplier());

        Assert.assertEquals(IntegrationStatus.FAILED, log.getStatus());
        Assert.assertTrue(log.getSummary(), log.getSummary().contains("whole numbers"));
        Mockito.verify(supplierApiClient, Mockito.never()).submitOrder(ArgumentMatchers.<String>any(),
                ArgumentMatchers.<String>any(), ArgumentMatchers.<String>any(), ArgumentMatchers.<Integer>any(),
                ArgumentMatchers.<Integer>any());
    }

    private PurchaseOrder apiOrder(String program, PurchaseOrderItem... items) {
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplier(apiSupplier());
        order.setProgram(program);
        order.setFacilityId("0364");
        List<PurchaseOrderItem> lines = new ArrayList<PurchaseOrderItem>(Arrays.asList(items));
        for (PurchaseOrderItem item : lines) {
            item.setPurchaseOrder(order);
        }
        order.setItems(lines);
        return order;
    }

    private PurchaseOrderItem line(String npcCode, String name, String program) {
        ProductMasterItem productMasterItem = new ProductMasterItem();
        productMasterItem.setNpcCode(npcCode);
        productMasterItem.setItemName(name);
        productMasterItem.setProgram(program);
        productMasterItem.setActive(Boolean.TRUE);
        productMasterItem.setSource(ProductMasterItem.SOURCE_API_SYNCED);
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProductMasterItem(productMasterItem);
        item.setItemCode(npcCode);
        item.setItemName(name);
        item.setProgram(program);
        item.setQuantity(BigDecimal.TEN);
        item.setUnitPrice(BigDecimal.ONE);
        return item;
    }

    private Supplier apiSupplier() {
        Supplier supplier = new Supplier();
        supplier.setApiEnabled(Boolean.TRUE);
        supplier.setApiBaseUrl("http://supplier.test");
        return supplier;
    }
}
