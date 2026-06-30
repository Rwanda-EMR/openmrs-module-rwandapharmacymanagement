package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

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

    List<PatientDispensingRequest> getPendingPaidRequests();

    List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults);

    long countPatientPrescriptionHistory(Integer patientId);

    Integer findLatestPatientBillId(Integer patientId);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate);

    Integer findLatestPatientBillId(Integer patientId, Date referenceDate, Collection<String> itemNames);

    Boolean isPatientBillPaid(Integer patientBillId);

    List<DispenseRecord> getRecentDispenses();

    List<DispenseRecord> getDispenseRecords(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId);

    List<PatientDispensingRequest> getPaymentExceptionApprovedRequests(Date fromInclusive,
                                                                       Date toExclusive,
                                                                       Integer pharmacyLocationId,
                                                                       RequestType requestType);

    PatientDispensingRequest getRequest(Integer requestId);

    PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest);

    PatientDispensingRequestLine saveRequestLine(PatientDispensingRequestLine patientDispensingRequestLine);

    DispenseRecord saveDispenseRecord(DispenseRecord dispenseRecord);

    DispenseRecordLine saveDispenseRecordLine(DispenseRecordLine dispenseRecordLine);

    List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds);
}
