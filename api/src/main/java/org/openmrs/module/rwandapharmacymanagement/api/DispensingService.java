package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
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

    List<PatientDispensingRequest> getPendingPaidRequests();

    Integer findLatestPatientBillId(Integer patientId);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate, Collection<String> itemNames);

    Boolean isPatientBillPaid(Integer patientBillId);

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

    List<PatientDispensingRequest> getPaymentExceptionApprovedRequests(Date fromInclusive,
                                                                       Date toExclusive,
                                                                       Integer pharmacyLocationId,
                                                                       RequestType requestType);

    List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds);

    List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientPrescriptionHistory(Integer patientId);

    PatientDispensingRequest getRequest(Integer requestId);

    PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest);

    DispenseRecord dispense(DispenseRecord dispenseRecord);

    DispenseRecord dispenseWithoutOrder(DispenseRecord dispenseRecord);

    void markRequestLineUnavailable(Integer requestId, Integer requestLineId, String reason);
}
