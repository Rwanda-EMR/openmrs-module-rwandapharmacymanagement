package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.rwandapharmacymanagement.api.AlertService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Transactional
public class AlertServiceImpl extends BaseOpenmrsService implements AlertService {

    private InventoryService inventoryService;
    private AdministrationService administrationService;

    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @Override
    public List<InventoryBalance> getLowStockAlerts() {
        List<InventoryBalance> alerts = new ArrayList<InventoryBalance>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            Item item = balance.getItem();
            if (item != null && item.getLowStockThreshold() != null && balance.getQuantityOnHand() != null
                    && balance.getQuantityOnHand().intValue() <= item.getLowStockThreshold()) {
                alerts.add(balance);
            }
        }
        return alerts;
    }

    @Override
    public List<InventoryBalance> getExpiryAlerts() {
        int alertDays = Integer.parseInt(administrationService.getGlobalProperty("rwandapharmacymanagement.alerts.expiryDays", "90"));
        Calendar threshold = Calendar.getInstance();
        threshold.add(Calendar.DAY_OF_YEAR, alertDays);
        List<InventoryBalance> alerts = new ArrayList<InventoryBalance>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            if (balance.getExpiryDate() != null && !balance.getExpiryDate().after(threshold.getTime())) {
                alerts.add(balance);
            }
        }
        return alerts;
    }
}
