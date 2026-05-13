package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;

import java.util.List;

public interface DispensingService extends OpenmrsService {

    List<PatientDispensingRequest> getPendingRequests();

    List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId);

    long countPendingRequestsForPharmacy(Integer pharmacyLocationId);

    List<PatientDispensingRequest> getPendingPaidRequests();

    Integer findLatestPatientBillId(Integer patientId);

    Boolean isPatientBillPaid(Integer patientBillId);

    void refreshPendingRequestPayments();

    void refreshPendingRequestPayments(Integer pharmacyLocationId);

    List<DispenseRecord> getRecentDispenses();

    List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds);

    List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientPrescriptionHistory(Integer patientId);

    PatientDispensingRequest getRequest(Integer requestId);

    PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest);

    DispenseRecord dispense(DispenseRecord dispenseRecord);

    DispenseRecord dispenseWithoutOrder(DispenseRecord dispenseRecord);

    void markRequestLineUnavailable(Integer requestId, Integer requestLineId, String reason);
}
