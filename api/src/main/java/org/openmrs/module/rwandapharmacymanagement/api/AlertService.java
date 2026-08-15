package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.AlertFilter;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ExpirationAlertRow;
import org.openmrs.module.rwandapharmacymanagement.api.dto.StockoutAlertRow;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;

import java.util.List;

public interface AlertService extends OpenmrsService {

    List<InventoryBalance> getLowStockAlerts();

    List<InventoryBalance> getExpiryAlerts();

    List<ExpirationAlertRow> getExpirationAlertRows(AlertFilter filter);

    List<StockoutAlertRow> getStockoutAlertRows(AlertFilter filter);
}
