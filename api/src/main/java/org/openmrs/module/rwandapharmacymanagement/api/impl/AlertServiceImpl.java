package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.rwandapharmacymanagement.api.AlertService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.AlertFilter;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ExpirationAlertRow;
import org.openmrs.module.rwandapharmacymanagement.api.dto.StockoutAlertRow;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

    @Override
    public List<ExpirationAlertRow> getExpirationAlertRows(AlertFilter filter) {
        AlertFilter safeFilter = filter == null ? new AlertFilter() : filter;
        int expiryAlertDays = positiveOrDefault(safeFilter.getExpiryAlertDays(), defaultExpiryAlertDays());
        Date today = startOfToday();
        Date threshold = addDays(today, expiryAlertDays + 1);
        String statusFilter = normalize(safeFilter.getExpiryStatus());
        List<ExpirationAlertRow> rows = new ArrayList<ExpirationAlertRow>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(safeFilter.getPharmacyLocationId())) {
            if (balance == null || balance.getItem() == null || balance.getExpiryDate() == null
                    || quantityOnHand(balance).compareTo(BigDecimal.ZERO) <= 0 || !matchesItemFilters(balance.getItem(), safeFilter)) {
                continue;
            }
            Date expiryDate = startOfDay(balance.getExpiryDate());
            if (!expiryDate.before(threshold)) {
                continue;
            }
            long daysToExpiry = daysBetween(today, expiryDate);
            String status = expiryDate.before(today) ? "Expired" : "Expiring soon";
            if ("expired".equals(statusFilter) && !"Expired".equals(status)) {
                continue;
            }
            if ("expiring".equals(statusFilter) && !"Expiring soon".equals(status)) {
                continue;
            }
            rows.add(new ExpirationAlertRow(balance, Long.valueOf(daysToExpiry), status));
        }
        Collections.sort(rows, new Comparator<ExpirationAlertRow>() {
            @Override
            public int compare(ExpirationAlertRow left, ExpirationAlertRow right) {
                Date leftDate = left == null ? null : left.getExpiryDate();
                Date rightDate = right == null ? null : right.getExpiryDate();
                if (leftDate == null && rightDate == null) {
                    return 0;
                }
                if (leftDate == null) {
                    return 1;
                }
                if (rightDate == null) {
                    return -1;
                }
                return leftDate.compareTo(rightDate);
            }
        });
        return rows;
    }

    @Override
    public List<StockoutAlertRow> getStockoutAlertRows(AlertFilter filter) {
        AlertFilter safeFilter = filter == null ? new AlertFilter() : filter;
        int daysOfStockAlertDays = positiveOrDefault(safeFilter.getDaysOfStockAlertDays(), defaultDaysOfStockAlertDays());
        int consumptionLookbackDays = positiveOrDefault(safeFilter.getConsumptionLookbackDays(), 90);
        Integer locationId = safeFilter.getPharmacyLocationId();
        Date toExclusive = addDays(startOfToday(), 1);
        Date fromInclusive = addDays(toExclusive, -consumptionLookbackDays);
        Map<String, StockoutAlertRow> rowsByKey = new LinkedHashMap<String, StockoutAlertRow>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(locationId)) {
            if (balance == null || balance.getItem() == null || !matchesItemFilters(balance.getItem(), safeFilter)) {
                continue;
            }
            String key = stockKey(balance.getItem(), locationId == null ? null : balance.getPharmacyLocation());
            StockoutAlertRow row = rowsByKey.get(key);
            if (row == null) {
                row = newStockRow(balance.getItem(), locationId == null ? null : balance.getPharmacyLocation());
                rowsByKey.put(key, row);
            }
            row.setCurrentQuantity(row.getCurrentQuantity().add(quantityOnHand(balance)));
        }
        for (StockTransaction transaction : inventoryService.getStockTransactions(fromInclusive, toExclusive, locationId)) {
            if (transaction == null || transaction.getItem() == null || transaction.getQuantity() == null
                    || transaction.getQuantity().compareTo(BigDecimal.ZERO) >= 0 || !matchesItemFilters(transaction.getItem(), safeFilter)) {
                continue;
            }
            String key = stockKey(transaction.getItem(), locationId == null ? null : transaction.getPharmacyLocation());
            StockoutAlertRow row = rowsByKey.get(key);
            if (row == null) {
                row = newStockRow(transaction.getItem(), locationId == null ? null : transaction.getPharmacyLocation());
                rowsByKey.put(key, row);
            }
            row.setConsumedQuantity(row.getConsumedQuantity().add(transaction.getQuantity().abs()));
        }
        List<StockoutAlertRow> rows = new ArrayList<StockoutAlertRow>();
        BigDecimal lookbackDays = new BigDecimal(consumptionLookbackDays);
        String statusFilter = normalize(safeFilter.getStockStatus());
        for (StockoutAlertRow row : rowsByKey.values()) {
            BigDecimal averageDailyConsumption = row.getConsumedQuantity().compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO : row.getConsumedQuantity().divide(lookbackDays, 4, RoundingMode.HALF_UP);
            row.setAverageDailyConsumption(averageDailyConsumption);
            if (row.getCurrentQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                row.setStatus("Stockout");
                row.setDaysOfStock(BigDecimal.ZERO);
            } else if (averageDailyConsumption.compareTo(BigDecimal.ZERO) <= 0) {
                row.setStatus("No recent consumption");
                row.setDaysOfStock(null);
            } else {
                BigDecimal daysOfStock = row.getCurrentQuantity().divide(averageDailyConsumption, 1, RoundingMode.HALF_UP);
                row.setDaysOfStock(daysOfStock);
                row.setStatus(daysOfStock.compareTo(new BigDecimal(daysOfStockAlertDays)) <= 0 ? "Low stock" : "Adequate");
            }
            if ("stockout".equals(statusFilter) && !"Stockout".equals(row.getStatus())) {
                continue;
            }
            if ("low".equals(statusFilter) && !"Low stock".equals(row.getStatus())) {
                continue;
            }
            if ((statusFilter.isEmpty() || "all".equals(statusFilter))
                    && !("Stockout".equals(row.getStatus()) || "Low stock".equals(row.getStatus()))) {
                continue;
            }
            rows.add(row);
        }
        Collections.sort(rows, new Comparator<StockoutAlertRow>() {
            @Override
            public int compare(StockoutAlertRow left, StockoutAlertRow right) {
                String leftName = left == null || left.getItem() == null || left.getItem().getName() == null ? "" : left.getItem().getName();
                String rightName = right == null || right.getItem() == null || right.getItem().getName() == null ? "" : right.getItem().getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return rows;
    }

    private String lowStockKey(Item item, PharmacyLocation pharmacyLocation) {
        Integer itemId = item == null ? null : item.getItemId();
        Integer pharmacyLocationId = pharmacyLocation == null ? null : pharmacyLocation.getPharmacyLocationId();
        return String.valueOf(itemId) + "|" + String.valueOf(pharmacyLocationId);
    }

    private BigDecimal quantityOnHand(InventoryBalance balance) {
        return balance == null || balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
    }

    private StockoutAlertRow newStockRow(Item item, PharmacyLocation location) {
        StockoutAlertRow row = new StockoutAlertRow();
        row.setItem(item);
        row.setPharmacyLocation(location);
        row.setCurrentQuantity(BigDecimal.ZERO);
        row.setConsumedQuantity(BigDecimal.ZERO);
        return row;
    }

    private String stockKey(Item item, PharmacyLocation location) {
        Integer itemId = item == null ? null : item.getItemId();
        Integer locationId = location == null ? null : location.getPharmacyLocationId();
        return String.valueOf(itemId) + "|" + String.valueOf(locationId);
    }

    private boolean matchesItemFilters(Item item, AlertFilter filter) {
        if (item == null || Boolean.TRUE.equals(item.getVoided())) {
            return false;
        }
        if (filter != null && filter.getItemCategoryId() != null) {
            if (item.getItemCategory() == null || item.getItemCategory().getItemCategoryId() == null
                    || !filter.getItemCategoryId().equals(item.getItemCategory().getItemCategoryId())) {
                return false;
            }
        }
        String search = filter == null || filter.getSearchTerm() == null ? "" : filter.getSearchTerm().trim().toLowerCase();
        if (search.isEmpty()) {
            return true;
        }
        return contains(item.getName(), search) || contains(item.getCode(), search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private int defaultExpiryAlertDays() {
        return positiveOrDefault(parseInteger(administrationService.getGlobalProperty(
                "rwandapharmacymanagement.alerts.expiryDays", "90")), 90);
    }

    private int defaultDaysOfStockAlertDays() {
        return positiveOrDefault(parseInteger(administrationService.getGlobalProperty(
                "rwandapharmacymanagement.alerts.daysOfStockDays", "14")), 14);
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value.intValue() <= 0 ? defaultValue : value.intValue();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private Date startOfToday() {
        return startOfDay(new Date());
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

    private long daysBetween(Date from, Date to) {
        return (startOfDay(to).getTime() - startOfDay(from).getTime()) / (24L * 60L * 60L * 1000L);
    }
}
