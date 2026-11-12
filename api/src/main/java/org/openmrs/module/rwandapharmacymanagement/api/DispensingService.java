package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PaymentExceptionType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

import java.util.Date;
import java.util.Collection;
import java.util.List;

public interface DispensingService extends OpenmrsService {

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

    int markPendingRequestsNotServed(Date fromInclusive,
                                     Date toExclusive,
                                     Integer pharmacyLocationId,
                                     String reason);

    List<PatientDispensingRequest> getPendingPaidRequests();

    Integer findLatestPatientBillId(Integer patientId);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate, Collection<String> itemNames);

    Boolean isPatientBillPaid(Integer patientBillId);

    /**
     * True when the request's MOH bill is settled and money was actually received on it. A bill settled because the
     * patient owes nothing (zero price, fully insured) is not paid in this sense.
     */
    boolean isPaymentReceived(PatientDispensingRequest request);

    /**
     * Why the requester can no longer edit or delete this request because of payment (money received, or an
     * approved payment exception), or null when payment does not lock it.
     */
    String getPaymentEditLockReason(PatientDispensingRequest request);

    void refreshPendingRequestPayments();

    void refreshPendingRequestPayments(Integer pharmacyLocationId);

    void refreshPendingRequestPayments(Collection<Integer> requestIds);

    void refreshPendingRequestPayment(Integer requestId);

    PatientDispensingRequest approvePaymentException(Integer requestId,
                                                     PaymentExceptionType exceptionType,
                                                     String reason,
                                                     String note);

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

    List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds);

    List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientPrescriptionHistory(Integer patientId);

    /**
     * Request lines for these drug orders that the pharmacy has not finished: not voided, not dispensed, cancelled,
     * unavailable or not served, in a request that is still pending payment, pending dispense or in progress.
     */
    List<PatientDispensingRequestLine> getOpenRequestLinesForOrders(Collection<Integer> orderIds);

    List<PatientDispensingRequest> getPatientConsumableHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientConsumableHistory(Integer patientId);

    PatientDispensingRequest getRequest(Integer requestId);

    PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest);

    DispenseRecord dispense(DispenseRecord dispenseRecord);

    DispenseRecord dispenseWithoutOrder(DispenseRecord dispenseRecord);

    void markRequestLineUnavailable(Integer requestId, Integer requestLineId, String reason);
}
