package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableBillingReadiness;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableOrderLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;

import java.util.List;

public interface ConsumableOrderService extends OpenmrsService {

    List<PatientDispensingRequest> createConsumableOrders(Patient patient,
                                                          Location requestingLocation,
                                                          List<ConsumableOrderLineCommand> lines);

    PatientDispensingRequest updateConsumableLine(Integer requestId, Integer requestLineId,
                                                  ConsumableOrderLineCommand command);

    PatientDispensingRequest voidConsumableLine(Integer requestId, Integer requestLineId, String voidReason);

    ConsumableBillingReadiness getBillingReadiness(Patient patient);
}
