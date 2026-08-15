package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

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
                               Integer pharmacyLocationId,
                               DepartmentBulkRequestService departmentBulkRequestService,
                               PharmacyItemRequestService pharmacyItemRequestService) {
        if (inventoryService == null || item == null || item.getItemId() == null) {
            return Collections.emptyList();
        }
        BigDecimal runningBalance = openingBalance(inventoryService, item.getItemId(), startDate, pharmacyLocationId);
        BigDecimal averageMonthlyUsage = averageMonthlyUsage(inventoryService, item.getItemId(), startDate, endExclusive, pharmacyLocationId);
        List<Row> rows = new ArrayList<Row>();
        Map<String, InterPharmacyTransfer> transfersByReference = new LinkedHashMap<String, InterPharmacyTransfer>();
        Map<String, DepartmentBulkRequest> departmentTransfersByReference = new LinkedHashMap<String, DepartmentBulkRequest>();
        Map<String, PharmacyItemRequest> pharmacyRequestsByReference = new LinkedHashMap<String, PharmacyItemRequest>();
        for (StockTransaction transaction : inventoryService.getStockTransactions(item.getItemId(), startDate, endExclusive, pharmacyLocationId)) {
            InterPharmacyTransfer transfer = transferForTransaction(inventoryService, transfersByReference, transaction);
            DepartmentBulkRequest departmentTransfer = departmentTransferForTransaction(departmentBulkRequestService,
                    departmentTransfersByReference, transaction);
            PharmacyItemRequest pharmacyItemRequest = pharmacyItemRequestForTransaction(pharmacyItemRequestService,
                    pharmacyRequestsByReference, transaction);
            BigDecimal quantity = quantity(transaction);
            runningBalance = runningBalance.add(quantity);
            Row row = new Row();
            row.movementDate = formatDateTime(transaction.getDateCreated());
            row.receivingPharmacyOrUser = receivingPharmacyOrUser(transaction, transfer, departmentTransfer);
            row.issuingPharmacy = issuingPharmacy(transaction, transfer, departmentTransfer);
            row.performedBy = performedBy(transaction);
            row.requestedBy = requestedBy(transaction, transfer, departmentTransfer, pharmacyItemRequest);
            row.requestedWhere = requestedWhere(transfer, departmentTransfer, pharmacyItemRequest);
            row.servedBy = servedBy(transaction, transfer, departmentTransfer, pharmacyItemRequest);
            row.servedWhere = servedWhere(transaction, transfer, departmentTransfer, pharmacyItemRequest);
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

    static List<Row> buildRows(InventoryService inventoryService,
                               Item item,
                               Date startDate,
                               Date endExclusive,
                               Integer pharmacyLocationId) {
        return buildRows(inventoryService, item, startDate, endExclusive, pharmacyLocationId, null, null);
    }

    static List<Row> buildRows(InventoryService inventoryService,
                               Item item,
                               Date startDate,
                               Date endExclusive,
                               Integer pharmacyLocationId,
                               DepartmentBulkRequestService departmentBulkRequestService) {
        return buildRows(inventoryService, item, startDate, endExclusive, pharmacyLocationId,
                departmentBulkRequestService, null);
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

    private static DepartmentBulkRequest departmentTransferForTransaction(
            DepartmentBulkRequestService departmentBulkRequestService,
            Map<String, DepartmentBulkRequest> departmentTransfersByReference,
            StockTransaction transaction) {
        if (departmentBulkRequestService == null || transaction == null || transaction.getTransactionType() == null
                || !"DEPARTMENT_BULK_TRANSFER".equals(transaction.getTransactionType().name())) {
            return null;
        }
        String reference = safe(transaction.getReferenceNumber()).trim();
        if (reference.isEmpty()) {
            return null;
        }
        if (departmentTransfersByReference.containsKey(reference)) {
            return departmentTransfersByReference.get(reference);
        }
        DepartmentBulkRequest request = null;
        try {
            request = departmentBulkRequestService.getRequest(Integer.valueOf(reference));
        }
        catch (NumberFormatException ignored) {
            request = null;
        }
        departmentTransfersByReference.put(reference, request);
        return request;
    }

    private static PharmacyItemRequest pharmacyItemRequestForTransaction(
            PharmacyItemRequestService pharmacyItemRequestService,
            Map<String, PharmacyItemRequest> pharmacyRequestsByReference,
            StockTransaction transaction) {
        if (pharmacyItemRequestService == null || transaction == null || transaction.getTransactionType() == null
                || !"DISPENSE".equals(transaction.getTransactionType().name())) {
            return null;
        }
        String reference = pharmacyItemRequestReference(transaction);
        if (reference.isEmpty()) {
            return null;
        }
        if (pharmacyRequestsByReference.containsKey(reference)) {
            return pharmacyRequestsByReference.get(reference);
        }
        PharmacyItemRequest request = null;
        try {
            request = pharmacyItemRequestService.getRequest(Integer.valueOf(reference));
        }
        catch (RuntimeException ignored) {
            request = null;
        }
        pharmacyRequestsByReference.put(reference, request);
        return request;
    }

    static BigDecimal openingBalance(InventoryService inventoryService,
                                     Integer itemId,
                                     Date startDate,
                                     Integer pharmacyLocationId) {
        BigDecimal balance = currentInventoryBalance(inventoryService, itemId, pharmacyLocationId);
        for (StockTransaction transaction : inventoryService.getStockTransactions(itemId, startDate, null, pharmacyLocationId)) {
            balance = balance.subtract(quantity(transaction));
        }
        return balance;
    }

    private static BigDecimal currentInventoryBalance(InventoryService inventoryService,
                                                      Integer itemId,
                                                      Integer pharmacyLocationId) {
        BigDecimal balance = BigDecimal.ZERO;
        if (inventoryService == null || itemId == null) {
            return balance;
        }
        for (org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance inventoryBalance
                : inventoryService.getInventoryForPharmacy(pharmacyLocationId)) {
            if (inventoryBalance == null || inventoryBalance.getItem() == null
                    || inventoryBalance.getItem().getItemId() == null
                    || !itemId.equals(inventoryBalance.getItem().getItemId())) {
                continue;
            }
            balance = balance.add(inventoryBalance.getQuantityOnHand() == null
                    ? BigDecimal.ZERO : inventoryBalance.getQuantityOnHand());
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
        return transaction == null || transaction.getQuantity() == null
                || transaction.getTransactionType() == StockTransactionType.DISPOSAL
                ? BigDecimal.ZERO : transaction.getQuantity();
    }

    private static String receivingPharmacyOrUser(StockTransaction transaction, InterPharmacyTransfer transfer,
                                                  DepartmentBulkRequest departmentTransfer) {
        if (transfer != null && transfer.getDestinationPharmacyLocation() != null) {
            return safe(transfer.getDestinationPharmacyLocation().getName());
        }
        if (departmentTransfer != null) {
            return departmentTransferSummary(transaction, departmentTransfer);
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

    private static String issuingPharmacy(StockTransaction transaction, InterPharmacyTransfer transfer,
                                          DepartmentBulkRequest departmentTransfer) {
        if (transfer != null && transfer.getSourcePharmacyLocation() != null) {
            return safe(transfer.getSourcePharmacyLocation().getName());
        }
        if (departmentTransfer != null && departmentTransfer.getSupplyingPharmacyLocation() != null) {
            return safe(departmentTransfer.getSupplyingPharmacyLocation().getName());
        }
        BigDecimal quantity = quantity(transaction);
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            return transaction.getPharmacyLocation() == null ? "" : safe(transaction.getPharmacyLocation().getName());
        }
        return "";
    }

    private static String departmentTransferSummary(StockTransaction transaction, DepartmentBulkRequest request) {
        List<String> parts = new ArrayList<String>();
        if (request.getSupplyingPharmacyLocation() != null) {
            parts.add("Dispensing pharmacy: " + safe(request.getSupplyingPharmacyLocation().getName()));
        }
        if (request.getDepartmentLocation() != null) {
            parts.add("Receiving department: " + safe(request.getDepartmentLocation().getName()));
        }
        String creator = performedBy(transaction);
        if (creator.trim().isEmpty()) {
            creator = UserNameUtils.plainName(request.getCreator());
        }
        if (!creator.trim().isEmpty()) {
            parts.add("Recorded by: " + creator);
        }
        return join(parts);
    }

    private static String join(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private static String performedBy(StockTransaction transaction) {
        if (transaction == null) {
            return "";
        }
        if (transaction.getPerformedBy() != null) {
            return safe(transaction.getPerformedBy().getDisplayString());
        }
        if (transaction.getCreator() != null) {
            return safe(transaction.getCreator().getDisplayString());
        }
        return "";
    }

    private static String requestedBy(StockTransaction transaction, InterPharmacyTransfer transfer,
                                      DepartmentBulkRequest departmentTransfer, PharmacyItemRequest pharmacyItemRequest) {
        if (transfer != null) {
            return UserNameUtils.plainName(transfer.getCreator());
        }
        if (departmentTransfer != null) {
            return UserNameUtils.plainName(departmentTransfer.getCreator());
        }
        if (pharmacyItemRequest != null) {
            return UserNameUtils.plainName(pharmacyItemRequest.getCreator());
        }
        if (isDispense(transaction)) {
            return "";
        }
        return UserNameUtils.plainName(transaction == null ? null : transaction.getCreator());
    }

    private static String requestedWhere(InterPharmacyTransfer transfer, DepartmentBulkRequest departmentTransfer,
                                         PharmacyItemRequest pharmacyItemRequest) {
        if (transfer != null && transfer.getDestinationPharmacyLocation() != null) {
            return safe(transfer.getDestinationPharmacyLocation().getName());
        }
        if (departmentTransfer != null && departmentTransfer.getDepartmentLocation() != null) {
            return safe(departmentTransfer.getDepartmentLocation().getName());
        }
        if (pharmacyItemRequest != null && pharmacyItemRequest.getRequestedPharmacyLocation() != null) {
            return safe(pharmacyItemRequest.getRequestedPharmacyLocation().getName());
        }
        return "";
    }

    private static String servedBy(StockTransaction transaction, InterPharmacyTransfer transfer,
                                   DepartmentBulkRequest departmentTransfer, PharmacyItemRequest pharmacyItemRequest) {
        if (transfer != null) {
            return firstNonBlank(UserNameUtils.plainName(transfer.getServedBy()), performedBy(transaction));
        }
        if (departmentTransfer != null) {
            return firstNonBlank(UserNameUtils.plainName(departmentTransfer.getDispensedBy()), performedBy(transaction));
        }
        if (pharmacyItemRequest != null) {
            return firstNonBlank(UserNameUtils.plainName(pharmacyItemRequest.getApprovedBy()), performedBy(transaction));
        }
        return performedBy(transaction);
    }

    private static String servedWhere(StockTransaction transaction, InterPharmacyTransfer transfer,
                                      DepartmentBulkRequest departmentTransfer, PharmacyItemRequest pharmacyItemRequest) {
        if (transfer != null && transfer.getSourcePharmacyLocation() != null) {
            return safe(transfer.getSourcePharmacyLocation().getName());
        }
        if (departmentTransfer != null && departmentTransfer.getSupplyingPharmacyLocation() != null) {
            return safe(departmentTransfer.getSupplyingPharmacyLocation().getName());
        }
        if (pharmacyItemRequest != null && transaction != null && transaction.getPharmacyLocation() != null) {
            return safe(transaction.getPharmacyLocation().getName());
        }
        return transaction == null || transaction.getPharmacyLocation() == null ? ""
                : safe(transaction.getPharmacyLocation().getName());
    }

    private static boolean isDispense(StockTransaction transaction) {
        return transaction != null && transaction.getTransactionType() != null
                && ("DISPENSE".equals(transaction.getTransactionType().name())
                || "DISPENSE_WITHOUT_ORDER".equals(transaction.getTransactionType().name()));
    }

    private static String pharmacyItemRequestReference(StockTransaction transaction) {
        String remarks = safe(transaction == null ? null : transaction.getRemarks());
        String marker = "pharmacy request #";
        int markerIndex = remarks.toLowerCase().indexOf(marker);
        if (markerIndex < 0) {
            return "";
        }
        String tail = remarks.substring(markerIndex + marker.length()).trim();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < tail.length(); i++) {
            char character = tail.charAt(i);
            if (!Character.isDigit(character)) {
                break;
            }
            digits.append(character);
        }
        return digits.toString();
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : safe(second);
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
        private String performedBy;
        private String requestedBy;
        private String requestedWhere;
        private String servedBy;
        private String servedWhere;
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
        public String getPerformedBy() { return performedBy; }
        public String getRequestedBy() { return requestedBy; }
        public String getRequestedWhere() { return requestedWhere; }
        public String getServedBy() { return servedBy; }
        public String getServedWhere() { return servedWhere; }
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
