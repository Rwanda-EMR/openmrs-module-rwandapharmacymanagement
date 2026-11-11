package org.openmrs.module.rwandapharmacymanagement.integration.hie;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.integration.RetryOutcome;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import org.mockito.ArgumentMatchers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class HieRetryServiceTest {

    private AdministrationService administrationService;
    private IntegrationLogService integrationLogService;
    private HieMedicationApiClient apiClient;
    private HieRetryService service;

    @Before
    public void setUp() {
        administrationService = Mockito.mock(AdministrationService.class);
        integrationLogService = Mockito.mock(IntegrationLogService.class);
        apiClient = Mockito.mock(HieMedicationApiClient.class);
        Mockito.when(administrationService.getGlobalProperty(eq(RwandaPharmacyManagementConfig.GP_HIE_ENABLED), anyString()))
                .thenReturn("true");
        Mockito.when(administrationService.getGlobalProperty(eq(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL), anyString()))
                .thenReturn("http://hie.test");
        service = new HieRetryService();
        service.setAdministrationService(administrationService);
        service.setIntegrationLogService(integrationLogService);
        service.setHieMedicationApiClient(apiClient);
    }

    @Test
    public void medicationRequestShouldWaitWhileItsEncounterIsUnsent() throws Exception {
        IntegrationLog encounterA = pending(1, IntegrationType.HIE_ENCOUNTER, 100, "encounter-A");
        IntegrationLog requestA = pending(2, IntegrationType.HIE_MEDICATION_REQUEST, 100, "request-A");
        IntegrationLog encounterB = pending(3, IntegrationType.HIE_ENCOUNTER, 200, "encounter-B");
        IntegrationLog requestB = pending(4, IntegrationType.HIE_MEDICATION_REQUEST, 200, "request-B");
        Mockito.when(integrationLogService.getPendingHieLogs(Mockito.anyInt()))
                .thenReturn(Arrays.asList(encounterA, requestA, encounterB, requestB));
        Mockito.when(post("encounter-A")).thenThrow(new IOException("HIE API call failed with HTTP 400: invalid"));
        Mockito.when(post("encounter-B")).thenReturn("ok");
        Mockito.when(post("request-B")).thenReturn("ok");

        int sent = service.retryPending(50);

        Assert.assertEquals(2, sent);
        verifyNeverPosted("request-A");
        Assert.assertEquals(IntegrationStatus.RETRY_PENDING, requestA.getStatus());
        Assert.assertEquals(IntegrationStatus.SUCCESS, requestB.getStatus());
    }

    @Test
    public void runShouldStopAtFirstUnreachableCall() throws Exception {
        IntegrationLog first = pending(1, IntegrationType.HIE_ENCOUNTER, 100, "encounter-A");
        IntegrationLog second = pending(2, IntegrationType.HIE_ENCOUNTER, 200, "encounter-B");
        Mockito.when(integrationLogService.getPendingHieLogs(Mockito.anyInt())).thenReturn(Arrays.asList(first, second));
        Mockito.when(post("encounter-A")).thenThrow(new ConnectException("Connection refused"));

        service.retryPending(50);

        verifyNeverPosted("encounter-B");
        Assert.assertEquals(IntegrationStatus.RETRY_PENDING, first.getStatus());
    }

    @Test
    public void rejectedPayloadShouldStopAfterMaxAttempts() throws Exception {
        IntegrationLog rejected = pending(1, IntegrationType.HIE_ENCOUNTER, 100, "encounter-A");
        rejected.setRetryCount(Integer.valueOf(11));
        Mockito.when(integrationLogService.getPendingHieLogs(Mockito.anyInt())).thenReturn(Arrays.asList(rejected));
        Mockito.when(post("encounter-A")).thenThrow(new IOException("HIE API call failed with HTTP 400: invalid"));

        service.retryPending(50);

        Assert.assertEquals(IntegrationStatus.FAILED, rejected.getStatus());
    }

    @Test
    public void unreachableHieShouldNeverGiveUp() throws Exception {
        IntegrationLog waiting = pending(1, IntegrationType.HIE_ENCOUNTER, 100, "encounter-A");
        waiting.setRetryCount(Integer.valueOf(40));
        Mockito.when(integrationLogService.getPendingHieLogs(Mockito.anyInt())).thenReturn(Arrays.asList(waiting));
        Mockito.when(post("encounter-A")).thenThrow(new IOException("HIE API call failed with HTTP 503: maintenance"));

        service.retryPending(50);

        Assert.assertEquals(IntegrationStatus.RETRY_PENDING, waiting.getStatus());
    }

    @Test
    public void unreachableShouldRecogniseNetworkAndServerErrorsOnly() {
        Assert.assertTrue(RetryOutcome.isUnreachable(new ConnectException("refused")));
        Assert.assertTrue(RetryOutcome.isUnreachable(new RuntimeException(new SocketTimeoutException("connect timed out"))));
        Assert.assertTrue(RetryOutcome.isUnreachable(new IOException("SMS gateway call failed with HTTP 502: bad gateway")));
        Assert.assertFalse(RetryOutcome.isUnreachable(new IOException("HIE API call failed with HTTP 422: invalid")));
        Assert.assertFalse(RetryOutcome.isUnreachable(new IOException("SMS retry payload is not JSON")));
    }

    private String post(String payload) throws IOException {
        return apiClient.post(anyText(), anyText(), anyText(), anyText(), eq(payload), anyNumber(), anyNumber());
    }

    private void verifyNeverPosted(String payload) throws IOException {
        Mockito.verify(apiClient, Mockito.never())
                .post(anyText(), anyText(), anyText(), anyText(), eq(payload), anyNumber(), anyNumber());
    }

    private static String anyText() {
        return ArgumentMatchers.<String>any();
    }

    private static Integer anyNumber() {
        return ArgumentMatchers.<Integer>any();
    }

    private IntegrationLog pending(int id, IntegrationType type, int referenceId, String payload) {
        IntegrationLog log = new IntegrationLog();
        log.setIntegrationLogId(Integer.valueOf(id));
        log.setIntegrationType(type);
        log.setStatus(IntegrationStatus.RETRY_PENDING);
        log.setReferenceId(Integer.valueOf(referenceId));
        log.setRequestPayload(payload);
        log.setRetryCount(Integer.valueOf(0));
        return log;
    }
}
