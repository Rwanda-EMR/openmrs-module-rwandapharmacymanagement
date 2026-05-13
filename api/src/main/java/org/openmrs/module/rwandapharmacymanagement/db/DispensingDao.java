package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;

import java.util.List;

public interface DispensingDao {

    List<PatientDispensingRequest> getPendingRequests();

    List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId);

    long countPendingRequestsForPharmacy(Integer pharmacyLocationId);

    List<PatientDispensingRequest> getPendingPaidRequests();

    List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientPrescriptionHistory(Integer patientId);

    Integer findLatestPatientBillId(Integer patientId);

    Boolean isPatientBillPaid(Integer patientBillId);

    List<DispenseRecord> getRecentDispenses();

    PatientDispensingRequest getRequest(Integer requestId);

    PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest);

    PatientDispensingRequestLine saveRequestLine(PatientDispensingRequestLine patientDispensingRequestLine);

    DispenseRecord saveDispenseRecord(DispenseRecord dispenseRecord);

    DispenseRecordLine saveDispenseRecordLine(DispenseRecordLine dispenseRecordLine);

    List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds);
}
