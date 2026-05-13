package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;

public interface PaymentStatusService extends OpenmrsService {

    boolean isPaymentConfirmed(PatientDispensingRequest patientDispensingRequest);
}
