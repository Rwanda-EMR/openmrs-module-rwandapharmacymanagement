package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.PaymentStatusService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PaymentStatusServiceImpl extends BaseOpenmrsService implements PaymentStatusService {

    @Override
    public boolean isPaymentConfirmed(PatientDispensingRequest patientDispensingRequest) {
        return patientDispensingRequest != null && Boolean.TRUE.equals(patientDispensingRequest.getPaymentConfirmed());
    }
}
