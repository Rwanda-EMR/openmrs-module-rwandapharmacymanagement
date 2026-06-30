package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.module.rwandapharmacymanagement.db.DispensingDao;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DispensingServiceImplTest {

    @BeforeClass
    public static void configureOpenmrsApplicationDirectory() {
        File appDataDirectory = new File(System.getProperty("java.io.tmpdir"), "openmrs-test-appdata");
        appDataDirectory.mkdirs();
        System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", appDataDirectory.getAbsolutePath());
        System.setProperty("application_data_directory", appDataDirectory.getAbsolutePath());
    }

    @Test
    public void markRequestLineUnavailableShouldCloseUnpaidRequestWhenNoActiveLinesRemain() {
        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setPatientDispensingRequestId(1);
        request.setPaymentConfirmed(Boolean.FALSE);
        request.setRequestStatus(RequestStatus.PENDING_PAYMENT);

        PatientDispensingRequestLine line = new PatientDispensingRequestLine();
        line.setPatientDispensingRequestLineId(10);
        line.setRequestedQuantity(BigDecimal.ONE);
        line.setLineStatus(RequestStatus.PENDING_PAYMENT);
        request.setLines(Collections.singletonList(line));

        FakeDispensingDao dispensingDao = new FakeDispensingDao(request);
        DispensingServiceImpl service = new DispensingServiceImpl();
        service.setDispensingDao(dispensingDao);

        service.markRequestLineUnavailable(1, 10, "Drug not available in all pharmacies");

        Assert.assertEquals(RequestStatus.UNAVAILABLE, line.getLineStatus());
        Assert.assertEquals(RequestStatus.UNAVAILABLE, request.getRequestStatus());
        Assert.assertSame(request, dispensingDao.savedRequest);
    }

    @Test
    public void markRequestLineUnavailableShouldKeepRequestPendingWhenAnotherActiveLineRemains() {
        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setPatientDispensingRequestId(1);
        request.setPaymentConfirmed(Boolean.FALSE);
        request.setRequestStatus(RequestStatus.PENDING_PAYMENT);

        PatientDispensingRequestLine unavailableLine = new PatientDispensingRequestLine();
        unavailableLine.setPatientDispensingRequestLineId(10);
        unavailableLine.setRequestedQuantity(BigDecimal.ONE);
        unavailableLine.setLineStatus(RequestStatus.PENDING_PAYMENT);

        PatientDispensingRequestLine activeLine = new PatientDispensingRequestLine();
        activeLine.setPatientDispensingRequestLineId(11);
        activeLine.setRequestedQuantity(BigDecimal.ONE);
        activeLine.setLineStatus(RequestStatus.PENDING_PAYMENT);

        request.setLines(Arrays.asList(unavailableLine, activeLine));

        FakeDispensingDao dispensingDao = new FakeDispensingDao(request);
        DispensingServiceImpl service = new DispensingServiceImpl();
        service.setDispensingDao(dispensingDao);

        service.markRequestLineUnavailable(1, 10, "Drug not available in all pharmacies");

        Assert.assertEquals(RequestStatus.UNAVAILABLE, unavailableLine.getLineStatus());
        Assert.assertEquals(RequestStatus.PENDING_PAYMENT, activeLine.getLineStatus());
        Assert.assertEquals(RequestStatus.PENDING_PAYMENT, request.getRequestStatus());
    }

    private static class FakeDispensingDao implements DispensingDao {

        private final PatientDispensingRequest request;
        private PatientDispensingRequest savedRequest;

        private FakeDispensingDao(PatientDispensingRequest request) {
            this.request = request;
        }

        @Override
        public List<PatientDispensingRequest> getPendingRequests() {
            return Collections.emptyList();
        }

        @Override
        public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId) {
            return Collections.emptyList();
        }

        @Override
        public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId, String search, int firstResult, int maxResults) {
            return Collections.emptyList();
        }

        @Override
        public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                                                            String search,
                                                                            int firstResult,
                                                                            int maxResults,
                                                                            String queueTab,
                                                                            String requestType,
                                                                            String dateFilter) {
            return Collections.emptyList();
        }

        @Override
        public long countPendingRequestsForPharmacy(Integer pharmacyLocationId) {
            return 0;
        }

        @Override
        public long countPendingRequestsForPharmacy(Integer pharmacyLocationId, String search) {
            return 0;
        }

        @Override
        public long countPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                                    String search,
                                                    String queueTab,
                                                    String requestType,
                                                    String dateFilter) {
            return 0;
        }

        @Override
        public List<PatientDispensingRequest> getPendingPaidRequests() {
            return Collections.emptyList();
        }

        @Override
        public List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults) {
            return Collections.emptyList();
        }

        @Override
        public long countPatientPrescriptionHistory(Integer patientId) {
            return 0;
        }

        @Override
        public Integer findLatestPatientBillId(Integer patientId) {
            return null;
        }

        @Override
        public Integer findLatestPatientBillId(Integer patientId, Date referenceDate) {
            return null;
        }

        @Override
        public Integer findLatestPatientBillId(Integer patientId, Date referenceDate, Collection<String> itemNames) {
            return null;
        }

        @Override
        public Boolean isPatientBillPaid(Integer patientBillId) {
            return null;
        }

        @Override
        public List<DispenseRecord> getRecentDispenses() {
            return Collections.emptyList();
        }

        @Override
        public List<DispenseRecord> getDispenseRecords(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
            return Collections.emptyList();
        }

        @Override
        public List<PatientDispensingRequest> getPaymentExceptionApprovedRequests(Date fromInclusive,
                                                                                  Date toExclusive,
                                                                                  Integer pharmacyLocationId,
                                                                                  RequestType requestType) {
            return Collections.emptyList();
        }

        @Override
        public PatientDispensingRequest getRequest(Integer requestId) {
            return request;
        }

        @Override
        public PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest) {
            savedRequest = patientDispensingRequest;
            return patientDispensingRequest;
        }

        @Override
        public PatientDispensingRequestLine saveRequestLine(PatientDispensingRequestLine patientDispensingRequestLine) {
            return patientDispensingRequestLine;
        }

        @Override
        public DispenseRecord saveDispenseRecord(DispenseRecord dispenseRecord) {
            return dispenseRecord;
        }

        @Override
        public DispenseRecordLine saveDispenseRecordLine(DispenseRecordLine dispenseRecordLine) {
            return dispenseRecordLine;
        }

        @Override
        public List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds) {
            return Collections.emptyList();
        }
    }
}
