package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.rwandapharmacymanagement.api.BillingPriceService;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.db.PharmacyDao;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ItemRenameProposal;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.enums.ItemRenameProposalStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ItemRenameServiceImplTest {

    private PharmacyDao pharmacyDao;
    private PharmacyService pharmacyService;
    private BillingPriceService billingPriceService;
    private ConceptService conceptService;
    private ItemRenameServiceImpl service;

    @Before
    public void setUp() {
        Context.setUserContext(new UserContext(null));
        pharmacyDao = Mockito.mock(PharmacyDao.class);
        pharmacyService = Mockito.mock(PharmacyService.class);
        billingPriceService = Mockito.mock(BillingPriceService.class);
        conceptService = Mockito.mock(ConceptService.class);
        // The privilege check needs a full OpenMRS context; it is covered by OpenMRS itself.
        service = new ItemRenameServiceImpl() {
            @Override
            void requireManageSettingsPrivilege() {
            }
        };
        service.setPharmacyDao(pharmacyDao);
        service.setPharmacyService(pharmacyService);
        service.setBillingPriceService(billingPriceService);
        service.setIntegrationLogService(Mockito.mock(IntegrationLogService.class));
        service.setConceptService(conceptService);
        Mockito.when(pharmacyDao.getItemRenameProposals(ArgumentMatchers.<Integer>any(), ArgumentMatchers.<ItemRenameProposalStatus>any())).thenReturn(new ArrayList<ItemRenameProposal>());
        Mockito.when(pharmacyDao.getOtherActiveDrugIdsByName(ArgumentMatchers.<String>any(), ArgumentMatchers.<Integer>any())).thenReturn(new ArrayList<Integer>());
    }

    @After
    public void tearDown() {
        Context.clearUserContext();
    }

    @Test
    public void approveShouldRenameItemDrugAndBillingServiceTogether() {
        Item item = item(1, "Amoxicillin 500mg Caps", drug(10, "Amoxicillin 500mg Caps"));
        ItemRenameProposal proposal = pendingProposal(item, "Amoxicillin 500 mg Capsule");
        Mockito.when(pharmacyService.getItem(1)).thenReturn(item);
        Mockito.when(pharmacyDao.getItemRenameProposals(1, ItemRenameProposalStatus.PENDING))
                .thenReturn(Collections.singletonList(proposal));
        Mockito.when(pharmacyDao.getNonRetiredFacilityServiceIdsByName(ArgumentMatchers.<java.util.Collection<String>>any()))
                .thenReturn(billingServices("amoxicillin 500mg caps", 55));

        service.approveRename(1, "Amoxicillin 500 mg Capsule");

        Assert.assertEquals("Amoxicillin 500 mg Capsule", item.getName());
        Assert.assertEquals("Amoxicillin 500 mg Capsule", item.getDrug().getName());
        Mockito.verify(conceptService).saveDrug(item.getDrug());
        Mockito.verify(billingPriceService).renameFacilityService(55, "Amoxicillin 500 mg Capsule");
        Assert.assertEquals(ItemRenameProposalStatus.APPROVED, proposal.getStatus());
    }

    @Test
    public void approveShouldRefuseWhenAnotherBillingServiceHasTheNewName() {
        Item item = item(1, "Old name", drug(10, "Old name"));
        Mockito.when(pharmacyService.getItem(1)).thenReturn(item);
        Map<String, List<Integer>> services = billingServices("old name", 55);
        services.put("new name", new ArrayList<Integer>(Collections.singletonList(77)));
        Mockito.when(pharmacyDao.getNonRetiredFacilityServiceIdsByName(ArgumentMatchers.<java.util.Collection<String>>any())).thenReturn(services);

        assertRefused("New name", "already has another service");
        Assert.assertEquals("Old name", item.getName());
        Mockito.verify(pharmacyService, Mockito.never()).saveItem(any(Item.class));
        Mockito.verify(billingPriceService, Mockito.never()).renameFacilityService(ArgumentMatchers.<Integer>any(), ArgumentMatchers.<String>any());
    }

    @Test
    public void approveShouldRefuseWhenSeveralBillingServicesMatch() {
        Item item = item(1, "Old name", drug(10, "Old name"));
        Mockito.when(pharmacyService.getItem(1)).thenReturn(item);
        Map<String, List<Integer>> services = new HashMap<String, List<Integer>>();
        services.put("old name", new ArrayList<Integer>(Arrays.asList(55, 56)));
        Mockito.when(pharmacyDao.getNonRetiredFacilityServiceIdsByName(ArgumentMatchers.<java.util.Collection<String>>any())).thenReturn(services);

        assertRefused("New name", "MOH Billing services match");
        Assert.assertEquals("Old name", item.getDrug().getName());
    }

    @Test
    public void approveShouldRefuseANameAnotherItemAlreadyHas() {
        Item item = item(1, "Old name", drug(10, "Old name"));
        Mockito.when(pharmacyService.getItem(1)).thenReturn(item);
        Mockito.when(pharmacyService.getItemByExactName("New name")).thenReturn(item(2, "New name", null));

        assertRefused("New name", "Another pharmacy item");
    }

    @Test
    public void approveShouldRefuseANameAnotherDrugAlreadyHas() {
        Item item = item(1, "Old name", drug(10, "Old name"));
        Mockito.when(pharmacyService.getItem(1)).thenReturn(item);
        Mockito.when(pharmacyDao.getOtherActiveDrugIdsByName("New name", 10)).thenReturn(Collections.singletonList(11));

        assertRefused("New name", "Another OpenMRS drug");
    }

    @Test
    public void approveWithoutBillingServiceShouldStillRenameItemAndDrug() {
        Item item = item(1, "Old name", drug(10, "Old name"));
        Mockito.when(pharmacyService.getItem(1)).thenReturn(item);
        Mockito.when(pharmacyDao.getNonRetiredFacilityServiceIdsByName(ArgumentMatchers.<java.util.Collection<String>>any()))
                .thenReturn(new HashMap<String, List<Integer>>());

        String summary = service.approveRename(1, "New name");

        Assert.assertEquals("New name", item.getName());
        Assert.assertTrue(summary.contains("Missing billing mappings"));
        Mockito.verify(billingPriceService, Mockito.never()).renameFacilityService(ArgumentMatchers.<Integer>any(), ArgumentMatchers.<String>any());
    }

    @Test
    public void supplierRenameShouldBeProposedButNotApplied() {
        Item item = item(1, "Old name", drug(10, "Old name"));

        service.proposeSupplierRename(item, productMasterItem("New name", "ARV"));

        Assert.assertEquals("Old name", item.getName());
        ArgumentCaptor<ItemRenameProposal> saved = ArgumentCaptor.forClass(ItemRenameProposal.class);
        Mockito.verify(pharmacyDao).saveItemRenameProposal(saved.capture());
        Assert.assertEquals("New name", saved.getValue().getProposedName());
        Assert.assertEquals(ItemRenameProposalStatus.PENDING, saved.getValue().getStatus());
        Assert.assertEquals("ARV", saved.getValue().getProgram());
    }

    @Test
    public void caseOrSpacingChangeShouldNotBeProposed() {
        service.proposeSupplierRename(item(1, "Old  Name", null), productMasterItem("old name", ""));

        Mockito.verify(pharmacyDao, Mockito.never()).saveItemRenameProposal(any(ItemRenameProposal.class));
    }

    @Test
    public void rejectedNameShouldNotBeProposedAgainForTheSameProgram() {
        Item item = item(1, "Old name", null);
        ItemRenameProposal rejected = pendingProposal(item, "New name");
        rejected.setProgram("ARV");
        rejected.setStatus(ItemRenameProposalStatus.REJECTED);
        Mockito.when(pharmacyDao.getItemRenameProposals(1, ItemRenameProposalStatus.REJECTED))
                .thenReturn(Collections.singletonList(rejected));

        service.proposeSupplierRename(item, productMasterItem("New name", "ARV"));

        Mockito.verify(pharmacyDao, Mockito.never()).saveItemRenameProposal(any(ItemRenameProposal.class));
    }

    @Test
    public void consistencyCheckShouldFlagDrugAndBillingNameMismatches() {
        Item consistent = item(1, "Paracetamol 500mg Tab", drug(10, "Paracetamol 500mg Tab"));
        Item renamedDrug = item(2, "Amoxicillin 500 mg Capsule", drug(20, "Amoxicillin 500mg Caps"));
        Mockito.when(pharmacyService.getAllItems()).thenReturn(Arrays.asList(consistent, renamedDrug));
        Map<String, List<Integer>> services = billingServices("paracetamol 500mg tab", 1);
        services.put("amoxicillin 500mg caps", new ArrayList<Integer>(Collections.singletonList(2)));
        Mockito.when(pharmacyDao.getNonRetiredFacilityServiceIdsByName(null)).thenReturn(services);

        int flagged = service.runConsistencyCheck();

        Assert.assertEquals(1, flagged);
        ArgumentCaptor<ItemRenameProposal> saved = ArgumentCaptor.forClass(ItemRenameProposal.class);
        Mockito.verify(pharmacyDao).saveItemRenameProposal(saved.capture());
        Assert.assertSame(renamedDrug, saved.getValue().getItem());
        Assert.assertTrue(saved.getValue().getReason().contains("drug is named"));
        Assert.assertTrue(saved.getValue().getReason().contains("named after the drug"));
    }

    private void assertRefused(String newName, String expectedMessage) {
        try {
            service.approveRename(1, newName);
            Assert.fail("Expected the rename to be refused");
        }
        catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(expectedMessage));
        }
    }

    private Map<String, List<Integer>> billingServices(String key, Integer id) {
        Map<String, List<Integer>> services = new HashMap<String, List<Integer>>();
        services.put(key, new ArrayList<Integer>(Collections.singletonList(id)));
        return services;
    }

    private ItemRenameProposal pendingProposal(Item item, String proposedName) {
        ItemRenameProposal proposal = new ItemRenameProposal();
        proposal.setItem(item);
        proposal.setProposedName(proposedName);
        proposal.setSource(ItemRenameProposal.SOURCE_SUPPLIER_SYNC);
        proposal.setStatus(ItemRenameProposalStatus.PENDING);
        return proposal;
    }

    private ProductMasterItem productMasterItem(String name, String program) {
        ProductMasterItem productMasterItem = new ProductMasterItem();
        productMasterItem.setItemName(name);
        productMasterItem.setProgram(program);
        productMasterItem.setNpcCode("NPC-1");
        return productMasterItem;
    }

    private Item item(int itemId, String name, Drug drug) {
        Item item = new Item();
        item.setItemId(itemId);
        item.setName(name);
        item.setDrug(drug);
        return item;
    }

    private Drug drug(int drugId, String name) {
        Drug drug = new Drug();
        drug.setDrugId(drugId);
        drug.setName(name);
        return drug;
    }
}
