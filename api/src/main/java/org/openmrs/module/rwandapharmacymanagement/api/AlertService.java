package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;

import java.util.List;

public interface AlertService extends OpenmrsService {

    List<InventoryBalance> getLowStockAlerts();

    List<InventoryBalance> getExpiryAlerts();
}
