package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipInsurancePayloadBuilder.RhipContext;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipInsurancePayloadBuilder.RhipPrescriptionPayload;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class RhipInsurancePayloadBuilderTest {

    private final RhipInsurancePayloadBuilder builder = new RhipInsurancePayloadBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void buildInpatientPrescriptionShouldCreateMmiPayload() throws Exception {
        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setLines(Collections.singletonList(requestLine("NPC-001", BigDecimal.TEN, "Take after meals", 5)));

        RhipContext context = context("mmi", "30311017", "REC-1", "REC-1", "LIC-1");
        context.setInpatient(true);
        context.setDiagnosisIds(Arrays.asList("XN7K1", "CA40.1"));

        RhipPrescriptionPayload result = builder.buildInpatientPrescription(request, context);
        Map payload = payload(result.getPayload());

        Assert.assertTrue(result.isReady());
        Assert.assertEquals("mmi", payload.get("insuranceType"));
        Assert.assertEquals("30311017", payload.get("facilityFosaId"));
        Assert.assertEquals("REC-1", payload.get("visitReferenceNumber"));
        Assert.assertFalse(payload.containsKey("receptionNumber"));
        Assert.assertEquals("LIC-1", payload.get("licenseNumber"));
        Assert.assertEquals(Arrays.asList("XN7K1", "CA40.1"), payload.get("diagnosisIds"));
        Map medicine = (Map) ((java.util.List) payload.get("medicines")).get(0);
        Assert.assertEquals("NPC-001", medicine.get("code"));
        Assert.assertEquals(Integer.valueOf(10), medicine.get("quantity"));
        Assert.assertEquals("Take after meals", medicine.get("instructions"));
        Assert.assertEquals(Integer.valueOf(5), medicine.get("durationDays"));
    }

    @Test
    public void buildVisitDispenseShouldCreateRamaPayload() throws Exception {
        DispenseRecord record = new DispenseRecord();
        record.setLines(Collections.singletonList(dispenseLine("NPC-002", new BigDecimal("2.4"))));

        RhipPrescriptionPayload result = builder.buildVisitDispense(record,
                context("rama", "417", null, "BILL-123417", null));
        Map payload = payload(result.getPayload());

        Assert.assertTrue(result.isReady());
        Assert.assertEquals("rama", payload.get("insuranceType"));
        Assert.assertEquals("417", payload.get("facilityFosaId"));
        Assert.assertEquals("BILL-123417", payload.get("visitReferenceNumber"));
        Map prescription = (Map) ((java.util.List) payload.get("prescriptions")).get(0);
        Assert.assertEquals("NPC-002", prescription.get("productCode"));
        Assert.assertEquals(Integer.valueOf(2), prescription.get("dispensedQuantity"));
    }

    @Test
    public void buildInpatientPrescriptionShouldSkipNonMmiInsurance() throws Exception {
        RhipPrescriptionPayload result = builder.buildInpatientPrescription(new PatientDispensingRequest(),
                context("rama", "417", null, "BILL-123417", "LIC-1"));

        Assert.assertFalse(result.isReady());
        Assert.assertEquals("RHIP inpatient prescription is only supported for MMI", result.getSkipReason());
    }

    @Test
    public void buildInpatientPrescriptionShouldSkipMmiOutpatient() throws Exception {
        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setLines(Collections.singletonList(requestLine("NPC-001", BigDecimal.TEN, "Take after meals", 5)));

        RhipPrescriptionPayload result = builder.buildInpatientPrescription(request,
                context("mmi", "30311017", "REC-1", "REC-1", "LIC-1"));

        Assert.assertFalse(result.isReady());
        Assert.assertEquals(RhipInsurancePayloadBuilder.OUTPATIENT_SKIP_REASON, result.getSkipReason());
    }

    @Test
    public void buildVisitDispenseShouldCreateMmiPayloadForInpatient() throws Exception {
        DispenseRecord record = new DispenseRecord();
        record.setLines(Collections.singletonList(dispenseLine("J01CA04001", new BigDecimal("14"))));
        RhipContext context = context("mmi", "10023", "REC-1", "REC-1", null);
        context.setInpatient(true);

        RhipPrescriptionPayload result = builder.buildVisitDispense(record, context);
        Map payload = payload(result.getPayload());

        Assert.assertTrue(result.isReady());
        Assert.assertEquals("mmi", payload.get("insuranceType"));
        Assert.assertEquals("10023", payload.get("facilityFosaId"));
        Assert.assertEquals("REC-1", payload.get("visitReferenceNumber"));
        Assert.assertFalse("MMI records the dispensing time itself", payload.containsKey("dispensingDate"));
        Map prescription = (Map) ((java.util.List) payload.get("prescriptions")).get(0);
        Assert.assertEquals("J01CA04001", prescription.get("productCode"));
        Assert.assertEquals(Integer.valueOf(14), prescription.get("dispensedQuantity"));
    }

    @Test
    public void buildVisitDispenseShouldSkipMmiOutpatient() throws Exception {
        DispenseRecord record = new DispenseRecord();
        record.setLines(Collections.singletonList(dispenseLine("J01CA04001", new BigDecimal("14"))));

        RhipPrescriptionPayload result = builder.buildVisitDispense(record, context("mmi", "10023", "REC-1", "REC-1", null));

        Assert.assertFalse(result.isReady());
        Assert.assertEquals(RhipInsurancePayloadBuilder.OUTPATIENT_SKIP_REASON, result.getSkipReason());
    }

    @Test
    public void buildVisitDispenseShouldSkipMmiWithoutFacility() throws Exception {
        DispenseRecord record = new DispenseRecord();
        record.setLines(Collections.singletonList(dispenseLine("J01CA04001", new BigDecimal("14"))));
        RhipContext context = context("mmi", null, "REC-1", "REC-1", null);
        context.setInpatient(true);

        RhipPrescriptionPayload result = builder.buildVisitDispense(record, context);

        Assert.assertFalse(result.isReady());
        Assert.assertEquals("facilityFosaId is missing", result.getSkipReason());
    }

    @Test
    public void buildMedicineDeliveryShouldSendUnitPriceWithMarkupAsBoxesOfOne() throws Exception {
        DeliveryReceipt receipt = new DeliveryReceipt();
        receipt.setReceiptNumber("GRN-2026-041");
        receipt.setDateCreated(new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2026-11-20"));
        receipt.setLines(Arrays.asList(
                deliveryLine("G03HA01001", new BigDecimal("300"), new BigDecimal("100")),
                deliveryLine("J01CA04001", new BigDecimal("21"), new BigDecimal("79.20")),
                deliveryLine("NO-PRICE", new BigDecimal("5"), null),
                deliveryLine(null, new BigDecimal("5"), new BigDecimal("10"))));

        RhipPrescriptionPayload result = builder.buildMedicineDelivery(receipt, "10023", new BigDecimal("20"));
        Map payload = payload(result.getPayload());

        Assert.assertTrue(result.isReady());
        Assert.assertEquals("mmi", payload.get("insuranceType"));
        Assert.assertEquals("10023", payload.get("facilityFosaId"));
        Assert.assertEquals("GRN-2026-041", payload.get("referenceNumber"));
        Assert.assertEquals("2026-11-20", payload.get("deliveryDate"));
        java.util.List products = (java.util.List) payload.get("products");
        Assert.assertEquals(2, products.size());
        Map first = (Map) products.get(0);
        Assert.assertEquals("G03HA01001", first.get("productCode"));
        Assert.assertEquals(Integer.valueOf(120), first.get("tariffUponDelivery"));
        Assert.assertEquals(Integer.valueOf(1), first.get("quantityPerBox"));
        Assert.assertEquals(Integer.valueOf(300), first.get("box"));
        Map second = (Map) products.get(1);
        Assert.assertEquals("79.20 + 20% = 95.04, rounded to whole francs", Integer.valueOf(95), second.get("tariffUponDelivery"));
        Assert.assertEquals(2, result.getNotSentLines().size());
        Assert.assertEquals("NO-PRICE (no unit price)", result.getNotSentLines().get(0));
    }

    @Test
    public void buildMedicineDeliveryShouldUseReceiptIdWhenNumberIsMissing() throws Exception {
        DeliveryReceipt receipt = new DeliveryReceipt();
        receipt.setDeliveryReceiptId(42);
        receipt.setLines(Collections.singletonList(deliveryLine("G03HA01001", BigDecimal.ONE, BigDecimal.TEN)));

        Map payload = payload(builder.buildMedicineDelivery(receipt, "10023", new BigDecimal("20")).getPayload());

        Assert.assertEquals("DR-42", payload.get("referenceNumber"));
    }

    @Test
    public void buildMedicineDeliveryShouldSkipWhenNoLineCanBeSent() throws Exception {
        DeliveryReceipt receipt = new DeliveryReceipt();
        receipt.setReceiptNumber("GRN-1");
        receipt.setLines(Collections.singletonList(deliveryLine("G03HA01001", BigDecimal.ZERO, BigDecimal.TEN)));

        RhipPrescriptionPayload result = builder.buildMedicineDelivery(receipt, "10023", new BigDecimal("20"));

        Assert.assertFalse(result.isReady());
        Assert.assertEquals("No delivery line can be sent to MMI: G03HA01001 (no accepted quantity)", result.getSkipReason());
    }

    @Test
    public void deliveryTariffShouldAddMarkupAndRound() {
        Assert.assertEquals(new BigDecimal("120"), RhipInsurancePayloadBuilder.deliveryTariff(new BigDecimal("100"), new BigDecimal("20")));
        Assert.assertEquals(new BigDecimal("1"), RhipInsurancePayloadBuilder.deliveryTariff(new BigDecimal("0.2"), new BigDecimal("20")));
        Assert.assertNull(RhipInsurancePayloadBuilder.deliveryTariff(BigDecimal.ZERO, new BigDecimal("20")));
        Assert.assertNull(RhipInsurancePayloadBuilder.deliveryTariff(null, new BigDecimal("20")));
    }

    private DeliveryReceiptLine deliveryLine(String code, BigDecimal acceptedQuantity, BigDecimal unitPrice) {
        DeliveryReceiptLine line = new DeliveryReceiptLine();
        line.setItem(code == null ? null : item(code));
        line.setAcceptedQuantity(acceptedQuantity);
        line.setUnitPrice(unitPrice);
        return line;
    }

    private PatientDispensingRequestLine requestLine(String code, BigDecimal quantity, String instructions, Integer duration) {
        DrugOrder order = new DrugOrder();
        order.setDosingInstructions(instructions);
        order.setDuration(duration);

        PatientDispensingRequestLine line = new PatientDispensingRequestLine();
        line.setItem(item(code));
        line.setRequestedQuantity(quantity);
        line.setOrder(order);
        return line;
    }

    private DispenseRecordLine dispenseLine(String code, BigDecimal quantity) {
        DispenseRecordLine line = new DispenseRecordLine();
        line.setItem(item(code));
        line.setQuantityDispensed(quantity);
        return line;
    }

    private Item item(String code) {
        Item item = new Item();
        item.setCode(code);
        item.setName(code);
        return item;
    }

    private RhipContext context(String insuranceType, String facilityFosaId,
                                String receptionNumber, String visitReferenceNumber, String licenseNumber) {
        RhipContext context = new RhipContext();
        context.setInsuranceType(insuranceType);
        context.setFacilityFosaId(facilityFosaId);
        context.setReceptionNumber(receptionNumber);
        context.setVisitReferenceNumber(visitReferenceNumber);
        context.setLicenseNumber(licenseNumber);
        return context;
    }

    private Map payload(String json) throws Exception {
        return objectMapper.readValue(json, Map.class);
    }
}
