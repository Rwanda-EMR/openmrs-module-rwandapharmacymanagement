package org.openmrs.module.rwandapharmacymanagement.integration.hie;

import org.openmrs.Order;
import org.openmrs.Patient;

public interface HieIntegrationService {

    void submitDispensingOrder(Patient patient, Order order);
}
