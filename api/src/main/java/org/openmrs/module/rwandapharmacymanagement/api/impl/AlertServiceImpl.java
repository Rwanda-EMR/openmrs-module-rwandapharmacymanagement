package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.rwandapharmacymanagement.api.AlertService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, InventoryBalance> balancesByItemAndLocation = new LinkedHashMap<String, InventoryBalance>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            String key = lowStockKey(balance.getItem(), balance.getPharmacyLocation());
            InventoryBalance totalBalance = balancesByItemAndLocation.get(key);
            if (totalBalance == null) {
                totalBalance = new InventoryBalance();
                totalBalance.setItem(balance.getItem());
                totalBalance.setPharmacyLocation(balance.getPharmacyLocation());
                totalBalance.setQuantityOnHand(BigDecimal.ZERO);
                balancesByItemAndLocation.put(key, totalBalance);
            }
            totalBalance.setQuantityOnHand(totalBalance.getQuantityOnHand().add(quantityOnHand(balance)));
        }

        List<InventoryBalance> alerts = new ArrayList<InventoryBalance>();
        for (InventoryBalance balance : balancesByItemAndLocation.values()) {
            Item item = balance.getItem();
            if (item != null && item.getLowStockThreshold() != null
                    && balance.getQuantityOnHand().compareTo(BigDecimal.valueOf(item.getLowStockThreshold())) <= 0) {
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
            if (balance.getExpiryDate() != null && quantityOnHand(balance).compareTo(BigDecimal.ZERO) > 0
                    && !balance.getExpiryDate().after(threshold.getTime())) {
                alerts.add(balance);
            }
        }
        return alerts;
    }

    private String lowStockKey(Item item, PharmacyLocation pharmacyLocation) {
        Integer itemId = item == null ? null : item.getItemId();
        Integer pharmacyLocationId = pharmacyLocation == null ? null : pharmacyLocation.getPharmacyLocationId();
        return String.valueOf(itemId) + "|" + String.valueOf(pharmacyLocationId);
    }

    private BigDecimal quantityOnHand(InventoryBalance balance) {
        return balance == null || balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
    }
}
