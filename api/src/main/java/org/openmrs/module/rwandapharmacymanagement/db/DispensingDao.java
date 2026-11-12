package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Collection;
import java.util.List;

public interface DispensingDao {

    List<PatientDispensingRequest> getPendingRequests();

    List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId);

    List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId, String search, int firstResult, int maxResults);

    List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                                                 String search,
                                                                 int firstResult,
                                                                 int maxResults,
                                                                 String queueTab,
                                                                 String requestType,
                                                                 String dateFilter);

    long countPendingRequestsForPharmacy(Integer pharmacyLocationId);

    long countPendingRequestsForPharmacy(Integer pharmacyLocationId, String search);

    long countPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                         String search,
                                         String queueTab,
                                         String requestType,
                                         String dateFilter);

    List<PatientDispensingRequest> getPendingRequestsForCleanup(Date fromInclusive,
                                                                Date toExclusive,
                                                                Integer pharmacyLocationId);

    List<PatientDispensingRequest> getPendingPaidRequests();

    List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientPrescriptionHistory(Integer patientId);

    List<PatientDispensingRequestLine> getOpenRequestLinesForOrders(Collection<Integer> orderIds);

    List<PatientDispensingRequest> getPatientConsumableHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientConsumableHistory(Integer patientId);

    Integer findLatestPatientBillId(Integer patientId);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate, Collection<String> itemNames);

    Boolean isPatientBillPaid(Integer patientBillId);

    /**
     * Total of the payments recorded on the MOH bill, voided payments excluded, or null when it cannot be read.
     */
    BigDecimal getPatientBillAmountPaid(Integer patientBillId);

    List<DispenseRecord> getRecentDispenses();

    List<DispenseRecord> getDispenseRecords(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId);

    List<PatientDispensingRequest> getPatientDispensingRequests(Date fromInclusive,
                                                                 Date toExclusive,
                                                                 Integer pharmacyLocationId,
                                                                 RequestType requestType);

    List<PatientDispensingRequest> getPaymentExceptionApprovedRequests(Date fromInclusive,
                                                                       Date toExclusive,
                                                                       Integer pharmacyLocationId,
                                                                       RequestType requestType);

    List<PatientDispensingRequestLine> getNotBillableRequestLines(Date fromInclusive,
                                                                  Date toExclusive,
                                                                  Integer pharmacyLocationId,
                                                                  RequestType requestType);

    PatientDispensingRequest getRequest(Integer requestId);

    PatientDispensingRequest getRequestForUpdate(Integer requestId);

    PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest);

    PatientDispensingRequestLine saveRequestLine(PatientDispensingRequestLine patientDispensingRequestLine);

    DispenseRecord saveDispenseRecord(DispenseRecord dispenseRecord);

    DispenseRecordLine saveDispenseRecordLine(DispenseRecordLine dispenseRecordLine);

    List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds);
}
