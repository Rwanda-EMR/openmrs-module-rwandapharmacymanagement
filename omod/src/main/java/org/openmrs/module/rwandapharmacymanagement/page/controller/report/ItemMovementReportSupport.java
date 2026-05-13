package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ItemMovementReportSupport {

    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30.4375");

    private ItemMovementReportSupport() {
    }

    static List<Row> buildRows(InventoryService inventoryService,
                               Item item,
                               Date startDate,
                               Date endExclusive,
                               Integer pharmacyLocationId) {
        if (inventoryService == null || item == null || item.getItemId() == null) {
            return Collections.emptyList();
        }
        BigDecimal runningBalance = openingBalance(inventoryService, item.getItemId(), startDate, pharmacyLocationId);
        BigDecimal averageMonthlyUsage = averageMonthlyUsage(inventoryService, item.getItemId(), startDate, endExclusive, pharmacyLocationId);
        List<Row> rows = new ArrayList<Row>();
        Map<String, InterPharmacyTransfer> transfersByReference = new LinkedHashMap<String, InterPharmacyTransfer>();
        for (StockTransaction transaction : inventoryService.getStockTransactions(item.getItemId(), startDate, endExclusive, pharmacyLocationId)) {
            InterPharmacyTransfer transfer = transferForTransaction(inventoryService, transfersByReference, transaction);
            BigDecimal quantity = quantity(transaction);
            runningBalance = runningBalance.add(quantity);
            Row row = new Row();
            row.movementDate = formatDateTime(transaction.getDateCreated());
            row.receivingPharmacyOrUser = receivingPharmacyOrUser(transaction, transfer);
            row.issuingPharmacy = issuingPharmacy(transaction, transfer);
            row.batchNumber = safe(transaction.getBatchNumber());
            row.expiryDate = formatDate(transaction.getExpiryDate());
            row.quantityReceived = quantity.compareTo(BigDecimal.ZERO) > 0 ? quantity : BigDecimal.ZERO;
            row.quantityIssued = quantity.compareTo(BigDecimal.ZERO) < 0 ? quantity.abs() : BigDecimal.ZERO;
            row.balanceInStock = runningBalance;
            row.monthsOfStock = monthsOfStock(runningBalance, averageMonthlyUsage);
            row.transactionType = transaction.getTransactionType() == null ? "" : transaction.getTransactionType().name();
            row.referenceNumber = safe(transaction.getReferenceNumber());
            row.remarks = safe(transaction.getRemarks());
            rows.add(row);
        }
        return rows;
    }

    private static InterPharmacyTransfer transferForTransaction(InventoryService inventoryService,
                                                               Map<String, InterPharmacyTransfer> transfersByReference,
                                                               StockTransaction transaction) {
        if (transaction == null || transaction.getTransactionType() == null
                || !"INTER_PHARMACY_TRANSFER".equals(transaction.getTransactionType().name())) {
            return null;
        }
        String reference = safe(transaction.getReferenceNumber()).trim();
        if (reference.isEmpty()) {
            return null;
        }
        if (transfersByReference.containsKey(reference)) {
            return transfersByReference.get(reference);
        }
        InterPharmacyTransfer transfer = null;
        try {
            transfer = inventoryService.getTransfer(Integer.valueOf(reference));
        }
        catch (NumberFormatException ignored) {
            transfer = null;
        }
        transfersByReference.put(reference, transfer);
        return transfer;
    }

    static BigDecimal openingBalance(InventoryService inventoryService,
                                     Integer itemId,
                                     Date startDate,
                                     Integer pharmacyLocationId) {
        BigDecimal balance = BigDecimal.ZERO;
        for (StockTransaction transaction : inventoryService.getStockTransactions(itemId, null, startDate, pharmacyLocationId)) {
            balance = balance.add(quantity(transaction));
        }
        return balance;
    }

    static BigDecimal closingBalance(List<Row> rows, BigDecimal openingBalance) {
        if (rows == null || rows.isEmpty()) {
            return openingBalance == null ? BigDecimal.ZERO : openingBalance;
        }
        return rows.get(rows.size() - 1).balanceInStock;
    }

    static BigDecimal totalReceived(List<Row> rows) {
        BigDecimal total = BigDecimal.ZERO;
        if (rows != null) {
            for (Row row : rows) {
                total = total.add(row.quantityReceived == null ? BigDecimal.ZERO : row.quantityReceived);
            }
        }
        return total;
    }

    static BigDecimal totalIssued(List<Row> rows) {
        BigDecimal total = BigDecimal.ZERO;
        if (rows != null) {
            for (Row row : rows) {
                total = total.add(row.quantityIssued == null ? BigDecimal.ZERO : row.quantityIssued);
            }
        }
        return total;
    }

    static BigDecimal currentMonthsOfStock(BigDecimal closingBalance, InventoryService inventoryService,
                                           Integer itemId, Date startDate, Date endExclusive,
                                           Integer pharmacyLocationId) {
        return monthsOfStock(closingBalance, averageMonthlyUsage(inventoryService, itemId, startDate, endExclusive, pharmacyLocationId));
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

    private static BigDecimal averageMonthlyUsage(InventoryService inventoryService,
                                                  Integer itemId,
                                                  Date startDate,
                                                  Date endExclusive,
                                                  Integer pharmacyLocationId) {
        UsageWindow priorWindow = priorUsageWindow(startDate);
        BigDecimal priorUsage = issuedQuantity(inventoryService, itemId, priorWindow.startInclusive, priorWindow.endExclusive, pharmacyLocationId);
        if (priorUsage.compareTo(BigDecimal.ZERO) > 0) {
            return monthlyAverage(priorUsage, priorWindow.startInclusive, priorWindow.endExclusive);
        }
        BigDecimal reportUsage = issuedQuantity(inventoryService, itemId, startDate, endExclusive, pharmacyLocationId);
        return monthlyAverage(reportUsage, startDate, endExclusive);
    }

    private static UsageWindow priorUsageWindow(Date startDate) {
        Date end = startOfDay(startDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(end);
        calendar.add(Calendar.DAY_OF_YEAR, -90);
        UsageWindow window = new UsageWindow();
        window.startInclusive = calendar.getTime();
        window.endExclusive = end;
        return window;
    }

    private static BigDecimal issuedQuantity(InventoryService inventoryService,
                                             Integer itemId,
                                             Date startInclusive,
                                             Date endExclusive,
                                             Integer pharmacyLocationId) {
        BigDecimal total = BigDecimal.ZERO;
        for (StockTransaction transaction : inventoryService.getStockTransactions(itemId, startInclusive, endExclusive, pharmacyLocationId)) {
            BigDecimal quantity = quantity(transaction);
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                total = total.add(quantity.abs());
            }
        }
        return total;
    }

    private static BigDecimal monthlyAverage(BigDecimal usage, Date startInclusive, Date endExclusive) {
        if (usage == null || usage.compareTo(BigDecimal.ZERO) <= 0 || startInclusive == null || endExclusive == null
                || !endExclusive.after(startInclusive)) {
            return BigDecimal.ZERO;
        }
        BigDecimal days = new BigDecimal(endExclusive.getTime() - startInclusive.getTime())
                .divide(new BigDecimal(24L * 60L * 60L * 1000L), 6, RoundingMode.HALF_UP);
        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return usage.divide(days.divide(DAYS_PER_MONTH, 6, RoundingMode.HALF_UP), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal monthsOfStock(BigDecimal balance, BigDecimal averageMonthlyUsage) {
        if (balance == null || averageMonthlyUsage == null || averageMonthlyUsage.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return balance.max(BigDecimal.ZERO).divide(averageMonthlyUsage, 2, RoundingMode.HALF_UP);
    }

    private static boolean matchesItem(StockTransaction transaction, Integer itemId) {
        return transaction != null && transaction.getItem() != null && transaction.getItem().getItemId() != null
                && transaction.getItem().getItemId().equals(itemId);
    }

    private static BigDecimal quantity(StockTransaction transaction) {
        return transaction == null || transaction.getQuantity() == null ? BigDecimal.ZERO : transaction.getQuantity();
    }

    private static String receivingPharmacyOrUser(StockTransaction transaction, InterPharmacyTransfer transfer) {
        if (transfer != null && transfer.getDestinationPharmacyLocation() != null) {
            return safe(transfer.getDestinationPharmacyLocation().getName());
        }
        BigDecimal quantity = quantity(transaction);
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            return transaction.getPharmacyLocation() == null ? "" : safe(transaction.getPharmacyLocation().getName());
        }
        if (transaction.getPerformedBy() != null) {
            return safe(transaction.getPerformedBy().getDisplayString());
        }
        if (transaction.getCreator() != null) {
            return safe(transaction.getCreator().getDisplayString());
        }
        return "";
    }

    private static String issuingPharmacy(StockTransaction transaction, InterPharmacyTransfer transfer) {
        if (transfer != null && transfer.getSourcePharmacyLocation() != null) {
            return safe(transfer.getSourcePharmacyLocation().getName());
        }
        BigDecimal quantity = quantity(transaction);
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            return transaction.getPharmacyLocation() == null ? "" : safe(transaction.getPharmacyLocation().getName());
        }
        return "";
    }

    private static String formatDateTime(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static List<Item> sortedItems(List<Item> items) {
        List<Item> sorted = items == null ? new ArrayList<Item>() : new ArrayList<Item>(items);
        Collections.sort(sorted, new Comparator<Item>() {
            @Override
            public int compare(Item left, Item right) {
                String leftName = left == null || left.getName() == null ? "" : left.getName();
                String rightName = right == null || right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return sorted;
    }

    static class Row {
        private String movementDate;
        private String receivingPharmacyOrUser;
        private String issuingPharmacy;
        private String batchNumber;
        private String expiryDate;
        private BigDecimal quantityReceived = BigDecimal.ZERO;
        private BigDecimal quantityIssued = BigDecimal.ZERO;
        private BigDecimal balanceInStock = BigDecimal.ZERO;
        private BigDecimal monthsOfStock;
        private String transactionType;
        private String referenceNumber;
        private String remarks;

        public String getMovementDate() { return movementDate; }
        public String getReceivingPharmacyOrUser() { return receivingPharmacyOrUser; }
        public String getIssuingPharmacy() { return issuingPharmacy; }
        public String getBatchNumber() { return batchNumber; }
        public String getExpiryDate() { return expiryDate; }
        public BigDecimal getQuantityReceived() { return quantityReceived; }
        public BigDecimal getQuantityIssued() { return quantityIssued; }
        public BigDecimal getBalanceInStock() { return balanceInStock; }
        public BigDecimal getMonthsOfStock() { return monthsOfStock; }
        public String getTransactionType() { return transactionType; }
        public String getReferenceNumber() { return referenceNumber; }
        public String getRemarks() { return remarks; }
    }

    private static class UsageWindow {
        private Date startInclusive;
        private Date endExclusive;
    }
}
