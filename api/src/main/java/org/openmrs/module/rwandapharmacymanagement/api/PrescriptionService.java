package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescriptionLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;

import java.util.List;

public interface PrescriptionService extends OpenmrsService {

    PatientDispensingRequest createPrescription(Patient patient,
                                               Visit visit,
                                               Provider orderingProvider,
                                               Location encounterLocation,
                                               List<PrescriptionLineCommand> lines,
                                               Integer patientBillId);

    /**
     * Why these lines cannot be prescribed because the same drug is still open at the pharmacy for this patient
     * (not yet paid, paid and waiting, or being dispensed), or null when nothing blocks them.
     */
    String getOpenPrescriptionConflict(Patient patient, List<PrescriptionLineCommand> lines);

    PatientDispensingRequest updatePrescriptionLine(Integer requestId,
                                                   Integer requestLineId,
                                                   Provider orderingProvider,
                                                   List<PrescriptionLineCommand> lines);

    PatientDispensingRequest voidPrescriptionLine(Integer requestId,
                                                 Integer requestLineId,
                                                 String voidReason);
}
