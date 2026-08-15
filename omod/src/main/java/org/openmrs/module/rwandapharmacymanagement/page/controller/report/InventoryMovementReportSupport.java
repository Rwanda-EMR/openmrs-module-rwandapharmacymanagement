package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class InventoryMovementReportSupport {

    static final String MODE_SUMMARY = "summary";
    static final String MODE_DETAIL = "detail";

    private InventoryMovementReportSupport() {
    }

    static List<Row> buildRows(InventoryService inventoryService,
                               PharmacyService pharmacyService,
                               Date startDate,
                               Date endExclusive,
                               Integer pharmacyLocationId,
                               String materialType,
                               String program,
                               String mode) {
        boolean detail = MODE_DETAIL.equalsIgnoreCase(mode);
        ProductMasterLookup productMasterLookup = new ProductMasterLookup(pharmacyService.getAllProductMasterItems());
        Map<String, Row> rowsByKey = new LinkedHashMap<String, Row>();

        for (StockTransaction tx : inventoryService.getStockTransactions(startDate, endExclusive, pharmacyLocationId)) {
            if (!usable(tx)) {
                continue;
            }
            ProductMasterItem productMasterItem = productMasterLookup.resolve(tx.getItem());
            if (!matchesFilters(productMasterItem, materialType, program)) {
                continue;
            }
            Row row = rowFor(rowsByKey, tx.getItem(), tx.getPharmacyLocation(), tx.getBatchNumber(), tx.getExpiryDate(),
                    productMasterItem, detail);
            BigDecimal qty = quantity(tx);
            applyPeriodMovement(row, tx, qty);
            row.periodNetQty = row.periodNetQty.add(qty);
            rememberUnitCost(row, tx.getUnitPrice());
        }

        for (StockTransaction tx : inventoryService.getStockTransactions(null, endExclusive, null, pharmacyLocationId)) {
            if (!usable(tx)) {
                continue;
            }
            ProductMasterItem productMasterItem = productMasterLookup.resolve(tx.getItem());
            if (!matchesFilters(productMasterItem, materialType, program)) {
                continue;
            }
            Row row = rowFor(rowsByKey, tx.getItem(), tx.getPharmacyLocation(), tx.getBatchNumber(), tx.getExpiryDate(),
                    productMasterItem, detail);
            row.afterPeriodNetQty = row.afterPeriodNetQty.add(quantity(tx));
            rememberUnitCost(row, tx.getUnitPrice());
        }

        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(pharmacyLocationId)) {
            if (balance == null || balance.getItem() == null || balance.getQuantityOnHand() == null) {
                continue;
            }
            ProductMasterItem productMasterItem = productMasterLookup.resolve(balance.getItem());
            if (!matchesFilters(productMasterItem, materialType, program)) {
                continue;
            }
            Row row = rowFor(rowsByKey, balance.getItem(), balance.getPharmacyLocation(), balance.getBatchNumber(), balance.getExpiryDate(),
                    productMasterItem, detail);
            row.stockAtHand = row.stockAtHand.add(balance.getQuantityOnHand());
            rememberUnitCost(row, balance.getUnitPrice());
        }

        List<Row> rows = new ArrayList<Row>(rowsByKey.values());
        for (Row row : rows) {
            row.closingQty = row.stockAtHand.subtract(row.afterPeriodNetQty);
            row.openingQty = row.closingQty.subtract(row.periodNetQty);
            row.closingValue = value(row.closingQty, row.unitCost);
            row.stockAtHandValue = value(row.stockAtHand, row.unitCost);
        }
        Collections.sort(rows, new Comparator<Row>() {
            @Override
            public int compare(Row left, Row right) {
                int pharmacy = safe(left.pharmacyName).compareToIgnoreCase(safe(right.pharmacyName));
                if (pharmacy != 0) {
                    return pharmacy;
                }
                int item = safe(left.itemName).compareToIgnoreCase(safe(right.itemName));
                if (item != 0) {
                    return item;
                }
                return safe(left.batchNumber).compareToIgnoreCase(safe(right.batchNumber));
            }
        });
        return rows;
    }

    static Totals totals(List<Row> rows) {
        Totals totals = new Totals();
        if (rows == null) {
            return totals;
        }
        for (Row row : rows) {
            totals.openingQty = totals.openingQty.add(row.openingQty);
            totals.receivedQty = totals.receivedQty.add(row.receivedQty);
            totals.transferInQty = totals.transferInQty.add(row.transferInQty);
            totals.adjustmentInQty = totals.adjustmentInQty.add(row.adjustmentInQty);
            totals.dispensedQty = totals.dispensedQty.add(row.dispensedQty);
            totals.transferOutQty = totals.transferOutQty.add(row.transferOutQty);
            totals.adjustmentOutQty = totals.adjustmentOutQty.add(row.adjustmentOutQty);
            totals.closingQty = totals.closingQty.add(row.closingQty);
            totals.stockAtHand = totals.stockAtHand.add(row.stockAtHand);
            totals.closingValue = totals.closingValue.add(row.closingValue);
            totals.stockAtHandValue = totals.stockAtHandValue.add(row.stockAtHandValue);
        }
        return totals;
    }

    static Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    static Date nextDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfDay(date));
        calendar.add(Calendar.DATE, 1);
        return calendar.getTime();
    }

    static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static void applyPeriodMovement(Row row, StockTransaction tx, BigDecimal qty) {
        StockTransactionType type = tx.getTransactionType();
        if (type == StockTransactionType.DELIVERY_RECEIPT) {
            row.receivedQty = row.receivedQty.add(qty.max(BigDecimal.ZERO));
        } else if (type == StockTransactionType.MAIN_TO_DISPENSING_TRANSFER || type == StockTransactionType.INTER_PHARMACY_TRANSFER) {
            if (qty.compareTo(BigDecimal.ZERO) >= 0) {
                row.transferInQty = row.transferInQty.add(qty);
            } else {
                row.transferOutQty = row.transferOutQty.add(qty.abs());
            }
        } else if (type == StockTransactionType.DISPENSE || type == StockTransactionType.DISPENSE_WITHOUT_ORDER) {
            row.dispensedQty = row.dispensedQty.add(qty.abs());
        } else if (type == StockTransactionType.ADJUSTMENT || type == StockTransactionType.RECONCILIATION
                || type == StockTransactionType.WRITE_OFF) {
            if (qty.compareTo(BigDecimal.ZERO) >= 0) {
                row.adjustmentInQty = row.adjustmentInQty.add(qty);
            } else {
                row.adjustmentOutQty = row.adjustmentOutQty.add(qty.abs());
            }
        } else if (qty.compareTo(BigDecimal.ZERO) >= 0) {
            row.adjustmentInQty = row.adjustmentInQty.add(qty);
        } else {
            row.adjustmentOutQty = row.adjustmentOutQty.add(qty.abs());
        }
    }

    private static Row rowFor(Map<String, Row> rowsByKey, Item item, PharmacyLocation pharmacyLocation,
                              String batchNumber, Date expiryDate, ProductMasterItem productMasterItem, boolean detail) {
        String key = key(item, pharmacyLocation, batchNumber, expiryDate, detail);
        Row row = rowsByKey.get(key);
        if (row == null) {
            row = new Row();
            row.itemId = item == null ? null : item.getItemId();
            row.itemName = item == null ? "" : item.getName();
            row.itemCode = item == null ? "" : item.getCode();
            row.pharmacyName = pharmacyLocation == null ? "" : pharmacyLocation.getName();
            row.batchNumber = detail ? safe(batchNumber) : "";
            row.expiryDate = detail ? expiryDate : null;
            row.materialType = productMasterItem == null ? "" : productMasterItem.getMaterialType();
            row.program = productMasterItem == null ? "" : productMasterItem.getProgram();
            rowsByKey.put(key, row);
        }
        return row;
    }

    private static String key(Item item, PharmacyLocation pharmacyLocation, String batchNumber, Date expiryDate, boolean detail) {
        StringBuilder key = new StringBuilder();
        key.append(item == null ? "" : item.getItemId()).append('|');
        key.append(pharmacyLocation == null ? "" : pharmacyLocation.getPharmacyLocationId());
        if (detail) {
            key.append('|').append(safe(batchNumber));
            key.append('|').append(expiryDate == null ? "" : expiryDate.getTime());
        }
        return key.toString();
    }

    private static boolean usable(StockTransaction tx) {
        return tx != null && tx.getItem() != null && tx.getPharmacyLocation() != null && tx.getQuantity() != null;
    }

    private static BigDecimal quantity(StockTransaction tx) {
        return tx == null || tx.getQuantity() == null || tx.getTransactionType() == StockTransactionType.DISPOSAL
                ? BigDecimal.ZERO : tx.getQuantity();
    }

    private static void rememberUnitCost(Row row, BigDecimal unitCost) {
        if (row.unitCost == null && unitCost != null) {
            row.unitCost = unitCost;
        }
    }

    private static BigDecimal value(BigDecimal quantity, BigDecimal unitCost) {
        return quantity == null || unitCost == null ? BigDecimal.ZERO : quantity.multiply(unitCost);
    }

    private static boolean matchesFilters(ProductMasterItem productMasterItem, String materialType, String program) {
        return matchesValue(productMasterItem == null ? null : productMasterItem.getMaterialType(), materialType)
                && matchesValue(productMasterItem == null ? null : productMasterItem.getProgram(), program);
    }

    private static boolean matchesValue(String actual, String selected) {
        String normalizedSelected = normalize(selected);
        return normalizedSelected.isEmpty() || normalizedSelected.equals(normalize(actual));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static class Row {
        private Integer itemId;
        private String itemName;
        private String itemCode;
        private String materialType;
        private String program;
        private String pharmacyName;
        private String batchNumber;
        private Date expiryDate;
        private BigDecimal openingQty = BigDecimal.ZERO;
        private BigDecimal receivedQty = BigDecimal.ZERO;
        private BigDecimal transferInQty = BigDecimal.ZERO;
        private BigDecimal adjustmentInQty = BigDecimal.ZERO;
        private BigDecimal dispensedQty = BigDecimal.ZERO;
        private BigDecimal transferOutQty = BigDecimal.ZERO;
        private BigDecimal adjustmentOutQty = BigDecimal.ZERO;
        private BigDecimal closingQty = BigDecimal.ZERO;
        private BigDecimal stockAtHand = BigDecimal.ZERO;
        private BigDecimal periodNetQty = BigDecimal.ZERO;
        private BigDecimal afterPeriodNetQty = BigDecimal.ZERO;
        private BigDecimal unitCost;
        private BigDecimal closingValue = BigDecimal.ZERO;
        private BigDecimal stockAtHandValue = BigDecimal.ZERO;

        public String getItemName() { return itemName; }
        public String getItemCode() { return itemCode; }
        public String getMaterialType() { return materialType; }
        public String getProgram() { return program; }
        public String getPharmacyName() { return pharmacyName; }
        public String getBatchNumber() { return batchNumber; }
        public Date getExpiryDate() { return expiryDate; }
        public BigDecimal getOpeningQty() { return openingQty; }
        public BigDecimal getReceivedQty() { return receivedQty; }
        public BigDecimal getTransferInQty() { return transferInQty; }
        public BigDecimal getAdjustmentInQty() { return adjustmentInQty; }
        public BigDecimal getDispensedQty() { return dispensedQty; }
        public BigDecimal getTransferOutQty() { return transferOutQty; }
        public BigDecimal getAdjustmentOutQty() { return adjustmentOutQty; }
        public BigDecimal getClosingQty() { return closingQty; }
        public BigDecimal getStockAtHand() { return stockAtHand; }
        public BigDecimal getUnitCost() { return unitCost; }
        public BigDecimal getClosingValue() { return closingValue; }
        public BigDecimal getStockAtHandValue() { return stockAtHandValue; }
    }

    static class Totals {
        private BigDecimal openingQty = BigDecimal.ZERO;
        private BigDecimal receivedQty = BigDecimal.ZERO;
        private BigDecimal transferInQty = BigDecimal.ZERO;
        private BigDecimal adjustmentInQty = BigDecimal.ZERO;
        private BigDecimal dispensedQty = BigDecimal.ZERO;
        private BigDecimal transferOutQty = BigDecimal.ZERO;
        private BigDecimal adjustmentOutQty = BigDecimal.ZERO;
        private BigDecimal closingQty = BigDecimal.ZERO;
        private BigDecimal stockAtHand = BigDecimal.ZERO;
        private BigDecimal closingValue = BigDecimal.ZERO;
        private BigDecimal stockAtHandValue = BigDecimal.ZERO;

        public BigDecimal getOpeningQty() { return openingQty; }
        public BigDecimal getReceivedQty() { return receivedQty; }
        public BigDecimal getTransferInQty() { return transferInQty; }
        public BigDecimal getAdjustmentInQty() { return adjustmentInQty; }
        public BigDecimal getDispensedQty() { return dispensedQty; }
        public BigDecimal getTransferOutQty() { return transferOutQty; }
        public BigDecimal getAdjustmentOutQty() { return adjustmentOutQty; }
        public BigDecimal getClosingQty() { return closingQty; }
        public BigDecimal getStockAtHand() { return stockAtHand; }
        public BigDecimal getClosingValue() { return closingValue; }
        public BigDecimal getStockAtHandValue() { return stockAtHandValue; }
    }

    private static class ProductMasterLookup {
        private final Map<String, ProductMasterItem> byCode = new HashMap<String, ProductMasterItem>();
        private final Map<String, ProductMasterItem> byCodeProgram = new HashMap<String, ProductMasterItem>();
        private final Map<String, ProductMasterItem> byName = new HashMap<String, ProductMasterItem>();
        private final Map<Integer, ProductMasterItem> byDrugId = new HashMap<Integer, ProductMasterItem>();

        private ProductMasterLookup(List<ProductMasterItem> productMasterItems) {
            if (productMasterItems == null) {
                return;
            }
            for (ProductMasterItem item : productMasterItems) {
                if (item == null) {
                    continue;
                }
                String code = normalize(item.getNpcCode());
                if (!code.isEmpty() && !byCode.containsKey(code)) {
                    byCode.put(code, item);
                }
                String programKey = codeProgramKey(item.getNpcCode(), item.getProgram());
                if (!programKey.isEmpty() && !byCodeProgram.containsKey(programKey)) {
                    byCodeProgram.put(programKey, item);
                }
                String name = normalize(item.getItemName());
                if (!name.isEmpty() && !byName.containsKey(name)) {
                    byName.put(name, item);
                }
                if (item.getDrug() != null && item.getDrug().getDrugId() != null
                        && !byDrugId.containsKey(item.getDrug().getDrugId())) {
                    byDrugId.put(item.getDrug().getDrugId(), item);
                }
            }
        }

        private ProductMasterItem resolve(Item item) {
            if (item == null) {
                return null;
            }
            String code = normalize(item.getCode());
            String programKey = codeProgramKey(item.getCode(), item.getProgram());
            if (!programKey.isEmpty() && byCodeProgram.containsKey(programKey)) {
                return byCodeProgram.get(programKey);
            }
            if (!code.isEmpty() && byCode.containsKey(code)) {
                return byCode.get(code);
            }
            String name = normalize(item.getName());
            if (!name.isEmpty() && byName.containsKey(name)) {
                return byName.get(name);
            }
            if (item.getDrug() != null && item.getDrug().getDrugId() != null) {
                return byDrugId.get(item.getDrug().getDrugId());
            }
            return null;
        }

        private String codeProgramKey(String code, String program) {
            String normalizedCode = normalize(code);
            String normalizedProgram = normalize(program);
            if (normalizedCode.isEmpty() || normalizedProgram.isEmpty()) {
                return "";
            }
            return normalizedCode + "|" + normalizedProgram;
        }
    }
}
