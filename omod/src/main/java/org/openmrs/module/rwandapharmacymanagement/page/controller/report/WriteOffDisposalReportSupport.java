package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WriteOffDisposalReportSupport {

    static final String STATUS_ALL = "ALL";
    static final String STATUS_PENDING_DISPOSAL = "PENDING_DISPOSAL";
    static final String STATUS_DISPOSED = "DISPOSED";

    private WriteOffDisposalReportSupport() {
    }

    static List<Row> buildRows(InventoryService inventoryService,
                               Date startDate,
                               Date endExclusive,
                               Integer pharmacyLocationId,
                               Integer itemId,
                               String status) {
        Map<String, StockTransaction> disposalsByWriteOffId = disposalsByWriteOffId(inventoryService, pharmacyLocationId);
        List<Row> rows = new ArrayList<Row>();
        for (StockTransaction writeOff : inventoryService.getStockTransactions(startDate, endExclusive, pharmacyLocationId)) {
            if (writeOff == null || writeOff.getTransactionType() != StockTransactionType.WRITE_OFF
                    || writeOff.getItem() == null || writeOff.getItem().getItemId() == null) {
                continue;
            }
            if (itemId != null && !itemId.equals(writeOff.getItem().getItemId())) {
                continue;
            }
            StockTransaction disposal = disposalsByWriteOffId.get(String.valueOf(writeOff.getStockTransactionId()));
            boolean disposed = disposal != null;
            if (STATUS_PENDING_DISPOSAL.equalsIgnoreCase(status) && disposed) {
                continue;
            }
            if (STATUS_DISPOSED.equalsIgnoreCase(status) && !disposed) {
                continue;
            }
            rows.add(row(writeOff, disposal));
        }
        return rows;
    }

    static Totals totals(List<Row> rows) {
        Totals totals = new Totals();
        if (rows == null) {
            return totals;
        }
        for (Row row : rows) {
            totals.writeOffCount++;
            totals.writeOffQuantity = totals.writeOffQuantity.add(row.writeOffQuantity == null ? BigDecimal.ZERO : row.writeOffQuantity);
            totals.writeOffValue = totals.writeOffValue.add(row.writeOffValue == null ? BigDecimal.ZERO : row.writeOffValue);
            if (row.disposed) {
                totals.disposedCount++;
            } else {
                totals.pendingDisposalCount++;
            }
        }
        return totals;
    }

    static Date parseDate(String value, Date fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(value.trim());
        }
        catch (ParseException ex) {
            return fallback;
        }
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

    static String normalizedStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (STATUS_PENDING_DISPOSAL.equals(normalized) || STATUS_DISPOSED.equals(normalized)) {
            return normalized;
        }
        return STATUS_ALL;
    }

    private static Map<String, StockTransaction> disposalsByWriteOffId(InventoryService inventoryService,
                                                                        Integer pharmacyLocationId) {
        Map<String, StockTransaction> disposals = new LinkedHashMap<String, StockTransaction>();
        for (StockTransaction transaction : inventoryService.getStockTransactions(null, null, pharmacyLocationId)) {
            if (transaction == null || transaction.getTransactionType() != StockTransactionType.DISPOSAL
                    || transaction.getReferenceNumber() == null || transaction.getReferenceNumber().trim().isEmpty()) {
                continue;
            }
            disposals.put(transaction.getReferenceNumber().trim(), transaction);
        }
        return disposals;
    }

    private static Row row(StockTransaction writeOff, StockTransaction disposal) {
        Row row = new Row();
        row.writeOffTransactionId = writeOff.getStockTransactionId();
        row.writeOffDate = formatDateTime(writeOff.getDateCreated());
        row.pharmacyName = writeOff.getPharmacyLocation() == null ? "" : safe(writeOff.getPharmacyLocation().getName());
        row.itemName = writeOff.getItem() == null ? "" : safe(writeOff.getItem().getName());
        row.itemCode = writeOff.getItem() == null ? "" : safe(writeOff.getItem().getCode());
        row.batchNumber = safe(writeOff.getBatchNumber());
        row.expiryDate = formatDate(writeOff.getExpiryDate());
        row.writeOffQuantity = writeOff.getQuantity() == null ? BigDecimal.ZERO : writeOff.getQuantity().abs();
        row.unitPrice = writeOff.getUnitPrice();
        row.writeOffValue = writeOff.getTotalAmount() == null ? value(row.writeOffQuantity, row.unitPrice)
                : writeOff.getTotalAmount().abs();
        row.writeOffReason = safe(writeOff.getRemarks());
        row.writtenOffBy = user(writeOff);
        row.disposed = disposal != null;
        if (disposal != null) {
            row.disposalTransactionId = disposal.getStockTransactionId();
            row.disposalDate = formatDateTime(disposal.getDateCreated());
            row.disposalMethod = safe(disposal.getRemarks());
            row.disposedBy = user(disposal);
        }
        return row;
    }

    private static String formatDateTime(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private static BigDecimal value(BigDecimal quantity, BigDecimal unitPrice) {
        return quantity == null || unitPrice == null ? BigDecimal.ZERO : quantity.multiply(unitPrice);
    }

    private static String user(StockTransaction transaction) {
        if (transaction == null) {
            return "";
        }
        if (transaction.getPerformedBy() != null) {
            return UserNameUtils.plainName(transaction.getPerformedBy());
        }
        return transaction.getCreator() == null ? "" : UserNameUtils.plainName(transaction.getCreator());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static class Row {
        Integer writeOffTransactionId;
        String writeOffDate;
        String pharmacyName;
        String itemName;
        String itemCode;
        String batchNumber;
        String expiryDate;
        BigDecimal writeOffQuantity;
        BigDecimal unitPrice;
        BigDecimal writeOffValue;
        String writeOffReason;
        String writtenOffBy;
        boolean disposed;
        Integer disposalTransactionId;
        String disposalDate;
        String disposalMethod;
        String disposedBy;
    }

    static class Totals {
        int writeOffCount;
        int disposedCount;
        int pendingDisposalCount;
        BigDecimal writeOffQuantity = BigDecimal.ZERO;
        BigDecimal writeOffValue = BigDecimal.ZERO;
    }
}
