package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class RhipCallOutcomeTest {

    @Test
    public void shouldRetryOnlyCallsThatAreSafeToSendAgain() {
        Assert.assertEquals(IntegrationStatus.RETRY_PENDING,
                RhipCallOutcome.failureStatus(RhipCallException.notSent(new ConnectException("Connection refused"))));
        Assert.assertEquals(IntegrationStatus.RETRY_PENDING,
                RhipCallOutcome.failureStatus(RhipCallException.httpError(429, "{}", "120")));
        Assert.assertEquals(IntegrationStatus.RETRY_PENDING,
                RhipCallOutcome.failureStatus(RhipCallException.httpError(503, "{}", null)));
        Assert.assertEquals(IntegrationStatus.FAILED,
                RhipCallOutcome.failureStatus(RhipCallException.httpError(400, "{}", null)));
        Assert.assertEquals(IntegrationStatus.FAILED,
                RhipCallOutcome.failureStatus(RhipCallException.httpError(500, "{}", null)));
        Assert.assertEquals("A dispense that got no reply may already be recorded", IntegrationStatus.FAILED,
                RhipCallOutcome.failureStatus(RhipCallException.noReply(new SocketTimeoutException("Read timed out"))));
        Assert.assertEquals(IntegrationStatus.FAILED, RhipCallOutcome.failureStatus(new IOException("other")));
    }

    @Test
    public void shouldExplainRefusalWithRhipMessage() {
        String body = "{\"success\":false,\"message\":\"facilityFosaId is required for MMI visit dispensing\",\"data\":null}";
        String summary = RhipCallOutcome.failureSummary(IntegrationType.RHIP_VISIT_DISPENSE,
                RhipCallException.httpError(400, body, null));
        Assert.assertEquals("Dispense refused by RHIP (HTTP 400): facilityFosaId is required for MMI visit dispensing", summary);
    }

    @Test
    public void shouldAskToCheckStatusWhenDispenseGotNoReply() {
        String summary = RhipCallOutcome.failureSummary(IntegrationType.RHIP_VISIT_DISPENSE,
                RhipCallException.noReply(new SocketTimeoutException("Read timed out")));
        Assert.assertTrue(summary.contains("may have been recorded"));
        Assert.assertTrue(summary.contains("prescription status"));
    }

    @Test
    public void shouldPointDeliveriesToResendButton() {
        String summary = RhipCallOutcome.failureSummary(IntegrationType.RHIP_MEDICINE_DELIVERY,
                RhipCallException.httpError(503, "{\"message\":\"MMI unavailable\"}", "60"));
        Assert.assertEquals("Medicine delivery refused by RHIP (HTTP 503): MMI unavailable. Use Resend on the MMI "
                + "deliveries page (RHIP asked to wait 60 s)", summary);
    }

    @Test
    public void shouldNameDispenseLinesSkippedAndRemaining() {
        String body = "{\"success\":true,\"message\":\"Medicines dispensed successfully\",\"data\":{"
                + "\"dispensingStatus\":\"Partially dispensed\","
                + "\"results\":[{\"productCode\":\"J01CA04001\",\"dispensedQuantity\":14}],"
                + "\"remaining\":[{\"productCode\":\"J01CA04001\",\"quantity\":7}],"
                + "\"skipped\":[{\"productCode\":\"N02BE01001\",\"reason\":\"Not prescribed on this reception\"}]}}";
        Assert.assertEquals("Dispense recorded by MMI (Partially dispensed); not dispensed: N02BE01001 "
                        + "(Not prescribed on this reception); still to dispense: J01CA04001 x 7",
                RhipCallOutcome.successSummary(IntegrationType.RHIP_VISIT_DISPENSE, body));
        Assert.assertTrue(RhipCallOutcome.hasRejectedLines(IntegrationType.RHIP_VISIT_DISPENSE, body));
    }

    @Test
    public void shouldNameInpatientPrescriptionLinesNotRecorded() {
        String body = "{\"success\":true,\"data\":{\"medicines\":[],\"invalid\":[{\"code\":\"J01CA04001\","
                + "\"reason\":\"instructions are required\"}]}}";
        Assert.assertEquals("Inpatient prescription recorded by MMI; not recorded: J01CA04001 (instructions are required)",
                RhipCallOutcome.successSummary(IntegrationType.RHIP_INPATIENT_PRESCRIPTION, body));
    }

    @Test
    public void shouldSummarizeCleanInpatientPrescription() {
        String body = "{\"success\":true,\"data\":{\"medicines\":[{\"npcCode\":\"J01CA04001\"}],\"invalid\":null}}";
        Assert.assertEquals("Inpatient prescription recorded by MMI",
                RhipCallOutcome.successSummary(IntegrationType.RHIP_INPATIENT_PRESCRIPTION, body));
        Assert.assertFalse(RhipCallOutcome.hasRejectedLines(IntegrationType.RHIP_INPATIENT_PRESCRIPTION, body));
    }

    @Test
    public void shouldReadDeliveryReferenceFromLoggedPayload() {
        Assert.assertEquals("GRN-1", RhipMedicineDeliveryService.referenceNumber(
                "{\"insuranceType\":\"mmi\",\"facilityFosaId\":\"10023\",\"referenceNumber\":\"GRN-1\",\"products\":[]}"));
        Assert.assertNull(RhipMedicineDeliveryService.referenceNumber(""));
        Assert.assertNull(RhipMedicineDeliveryService.referenceNumber("{\"truncated"));
    }

    @Test
    public void httpStatus_shouldBeTheStatusRhipAnsweredWith() {
        Assert.assertEquals(Integer.valueOf(400),
                RhipCallOutcome.httpStatus(RhipCallException.httpError(400, "{}", null)));
        Assert.assertNull(RhipCallOutcome.httpStatus(RhipCallException.notSent(new ConnectException("refused"))));
        Assert.assertNull(RhipCallOutcome.httpStatus(new IllegalStateException("no facility")));
    }
}
