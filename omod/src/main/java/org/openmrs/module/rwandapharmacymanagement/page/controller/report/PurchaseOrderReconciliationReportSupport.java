package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportExcelRenderer;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PurchaseOrderReconciliationReportSupport {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private PurchaseOrderReconciliationReportSupport() {
    }

    public static List<Row> buildRows(List<PurchaseOrder> purchaseOrders, List<DeliveryReceipt> receipts,
                                      boolean includeReceiptOnlyRows) {
        Map<String, ReceiptAggregate> receiptAggregates = aggregateReceipts(receipts);
        Set<String> orderedKeys = new HashSet<String>();
        List<Row> rows = new ArrayList<Row>();
        if (purchaseOrders != null) {
            for (PurchaseOrder purchaseOrder : purchaseOrders) {
                if (purchaseOrder == null || Boolean.TRUE.equals(purchaseOrder.getVoided())) {
                    continue;
                }
                List<PurchaseOrderItem> items = purchaseOrder.getItems();
                if (items == null || items.isEmpty()) {
                    rows.add(rowForPurchaseOrder(purchaseOrder, null, null));
                    continue;
                }
                for (PurchaseOrderItem item : items) {
                    if (item == null || Boolean.TRUE.equals(item.getVoided())) {
                        continue;
                    }
                    String key = purchaseOrderLineKey(purchaseOrder, item);
                    orderedKeys.add(key);
                    rows.add(rowForPurchaseOrder(purchaseOrder, item, receiptAggregates.get(key)));
                }
            }
        }
        if (includeReceiptOnlyRows) {
            for (Map.Entry<String, ReceiptAggregate> entry : receiptAggregates.entrySet()) {
                if (!orderedKeys.contains(entry.getKey())) {
                    rows.add(rowForReceiptOnly(entry.getValue()));
                }
            }
        }
        return rows;
    }

    public static Totals totals(List<Row> rows) {
        Totals totals = new Totals();
        if (rows == null) {
            return totals;
        }
        totals.lineCount = rows.size();
        Set<Integer> purchaseOrderIds = new HashSet<Integer>();
        Set<Integer> receiptIds = new HashSet<Integer>();
        for (Row row : rows) {
            if (row == null) {
                continue;
            }
            if (row.purchaseOrderId != null) {
                purchaseOrderIds.add(row.purchaseOrderId);
            }
            if (row.receiptIds != null) {
                receiptIds.addAll(row.receiptIds);
            }
            totals.orderedQuantity = totals.orderedQuantity.add(zero(row.orderedQuantity));
            totals.receivedQuantity = totals.receivedQuantity.add(zero(row.receivedQuantity));
            totals.differenceQuantity = totals.differenceQuantity.add(zero(row.differenceQuantity));
            if ("Fully received".equals(row.status)) {
                totals.fullyReceivedLineCount++;
            } else if ("Partially received".equals(row.status)) {
                totals.partiallyReceivedLineCount++;
            } else if ("Not received".equals(row.status)) {
                totals.notReceivedLineCount++;
            } else if (row.status != null && row.status.startsWith("Received without")) {
                totals.receiptOnlyLineCount++;
            }
        }
        totals.purchaseOrderCount = purchaseOrderIds.size();
        totals.receiptCount = receiptIds.size();
        return totals;
    }

    public static List<String> headers() {
        return Arrays.asList("Purchase order", "Supplier", "Order date", "Receipt", "Receipt date", "Pharmacy",
                "Program", "Item", "Item code", "Ordered quantity", "Received quantity", "Difference", "Status");
    }

    public static List<List<String>> body(List<Row> rows) {
        List<List<String>> body = new ArrayList<List<String>>();
        if (rows == null) {
            return body;
        }
        for (Row row : rows) {
            body.add(Arrays.asList(
                    string(row.purchaseOrderReference),
                    string(row.supplierName),
                    string(row.orderDate),
                    string(row.receiptReference),
                    string(row.receiptDate),
                    string(row.pharmacyName),
                    string(row.program),
                    string(row.itemName),
                    string(row.itemCode),
                    decimal(row.orderedQuantity),
                    decimal(row.receivedQuantity),
                    decimal(row.differenceQuantity),
                    string(row.status)
            ));
        }
        return body;
    }

    public static Date parseDate(String value, Date fallback) {
        return PatientRequestReportSupport.parseDate(value, fallback);
    }

    public static Date startOfDay(Date date) {
        return PatientRequestReportSupport.startOfDay(date);
    }

    public static Date nextDay(Date date) {
        return PatientRequestReportSupport.nextDay(date);
    }

    public static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_FORMAT).format(date);
    }

    public static String purchaseOrderLabel(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null || purchaseOrder.getPurchaseOrderId() == null) {
            return "";
        }
        return "PO-" + purchaseOrder.getPurchaseOrderId();
    }

    private static Map<String, ReceiptAggregate> aggregateReceipts(List<DeliveryReceipt> receipts) {
        Map<String, ReceiptAggregate> aggregates = new LinkedHashMap<String, ReceiptAggregate>();
        if (receipts == null) {
            return aggregates;
        }
        for (DeliveryReceipt receipt : receipts) {
            if (receipt == null || Boolean.TRUE.equals(receipt.getVoided())) {
                continue;
            }
            List<DeliveryReceiptLine> lines = receipt.getLines();
            if (lines == null) {
                continue;
            }
            for (DeliveryReceiptLine line : lines) {
                if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                    continue;
                }
                String key = receiptLineKey(receipt, line);
                ReceiptAggregate aggregate = aggregates.get(key);
                if (aggregate == null) {
                    aggregate = new ReceiptAggregate(receipt, line);
                    aggregates.put(key, aggregate);
                }
                aggregate.add(receipt, line);
            }
        }
        return aggregates;
    }

    private static Row rowForPurchaseOrder(PurchaseOrder purchaseOrder, PurchaseOrderItem item, ReceiptAggregate aggregate) {
        Row row = new Row();
        row.purchaseOrderId = purchaseOrder.getPurchaseOrderId();
        row.purchaseOrderReference = purchaseOrderLabel(purchaseOrder);
        row.supplierName = purchaseOrder.getSupplier() == null ? "" : safe(purchaseOrder.getSupplier().getName());
        row.orderDate = formatDate(purchaseOrder.getRequestedDate());
        row.program = item == null ? safe(purchaseOrder.getProgram()) : firstNonBlank(item.getProgram(), purchaseOrder.getProgram());
        row.itemName = item == null ? "" : firstNonBlank(item.getItemName(), item.getProductMasterItem() == null ? "" : item.getProductMasterItem().getItemName());
        row.itemCode = item == null ? "" : firstNonBlank(item.getItemCode(), item.getProductMasterItem() == null ? "" : item.getProductMasterItem().getNpcCode());
        row.orderedQuantity = item == null ? null : item.getQuantity();
        if (aggregate != null) {
            row.receivedQuantity = aggregate.receivedQuantity;
            row.receiptReference = aggregate.receiptReference;
            row.receiptDate = aggregate.receiptDate;
            row.pharmacyName = aggregate.pharmacyName;
            row.receiptIds.addAll(aggregate.receiptIds);
        }
        row.differenceQuantity = zero(row.orderedQuantity).subtract(zero(row.receivedQuantity));
        row.status = status(row.orderedQuantity, row.receivedQuantity);
        return row;
    }

    private static Row rowForReceiptOnly(ReceiptAggregate aggregate) {
        Row row = new Row();
        row.purchaseOrderId = aggregate.purchaseOrderId;
        row.purchaseOrderReference = aggregate.purchaseOrderId == null ? "Manual receipt" : "PO-" + aggregate.purchaseOrderId;
        row.supplierName = aggregate.supplierName;
        row.receiptReference = aggregate.receiptReference;
        row.receiptDate = aggregate.receiptDate;
        row.pharmacyName = aggregate.pharmacyName;
        row.program = aggregate.program;
        row.itemName = aggregate.itemName;
        row.itemCode = aggregate.itemCode;
        row.orderedQuantity = BigDecimal.ZERO;
        row.receivedQuantity = aggregate.receivedQuantity;
        row.differenceQuantity = BigDecimal.ZERO.subtract(zero(row.receivedQuantity));
        row.status = aggregate.purchaseOrderId == null ? "Received without PO" : "Received without PO line";
        row.receiptIds.addAll(aggregate.receiptIds);
        return row;
    }

    private static String purchaseOrderLineKey(PurchaseOrder purchaseOrder, PurchaseOrderItem item) {
        Integer purchaseOrderId = purchaseOrder == null ? null : purchaseOrder.getPurchaseOrderId();
        String itemKey = item == null ? "" : itemKey(item.getProductMasterItem(), item.getItemCode(), item.getItemName());
        return String.valueOf(purchaseOrderId) + "|" + itemKey;
    }

    private static String receiptLineKey(DeliveryReceipt receipt, DeliveryReceiptLine line) {
        PurchaseOrder purchaseOrder = receipt == null ? null : receipt.getPurchaseOrder();
        Integer purchaseOrderId = purchaseOrder == null ? null : purchaseOrder.getPurchaseOrderId();
        Item item = line == null ? null : line.getItem();
        String code = item == null ? null : item.getCode();
        String name = item == null ? null : item.getName();
        String itemKey = itemKey(line == null ? null : line.getProductMasterItem(), code, name);
        return String.valueOf(purchaseOrderId) + "|" + itemKey;
    }

    private static String itemKey(ProductMasterItem productMasterItem, String code, String name) {
        if (productMasterItem != null && productMasterItem.getProductMasterItemId() != null) {
            return "pmi:" + productMasterItem.getProductMasterItemId();
        }
        if (code != null && code.trim().length() > 0) {
            return "code:" + normalize(code);
        }
        return "name:" + normalize(name);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    private static String status(BigDecimal orderedQuantity, BigDecimal receivedQuantity) {
        BigDecimal ordered = zero(orderedQuantity);
        BigDecimal received = zero(receivedQuantity);
        if (ordered.compareTo(BigDecimal.ZERO) <= 0 && received.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            return "Not received";
        }
        int comparison = received.compareTo(ordered);
        if (comparison < 0) {
            return "Partially received";
        }
        if (comparison == 0) {
            return "Fully received";
        }
        return "Over received";
    }

    private static String formatDateTime(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_TIME_FORMAT).format(date);
    }

    private static BigDecimal lineReceivedQuantity(DeliveryReceiptLine line) {
        if (line == null) {
            return BigDecimal.ZERO;
        }
        if (line.getReceivedQuantity() != null) {
            return line.getReceivedQuantity();
        }
        return line.getAcceptedQuantity() == null ? BigDecimal.ZERO : line.getAcceptedQuantity();
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : StockReportExcelRenderer.decimal(value);
    }

    private static String string(String value) {
        return value == null ? "" : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && first.trim().length() > 0 ? first : safe(second);
    }

    private static String joinDistinct(List<String> values) {
        List<String> distinct = new ArrayList<String>();
        for (String value : values) {
            if (value != null && value.trim().length() > 0 && !distinct.contains(value)) {
                distinct.add(value);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (String value : distinct) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    public static class Row {
        public Integer purchaseOrderId;
        public String purchaseOrderReference;
        public String supplierName;
        public String orderDate;
        public String receiptReference;
        public String receiptDate;
        public String pharmacyName;
        public String program;
        public String itemName;
        public String itemCode;
        public BigDecimal orderedQuantity;
        public BigDecimal receivedQuantity;
        public BigDecimal differenceQuantity;
        public String status;
        public List<Integer> receiptIds = new ArrayList<Integer>();
    }

    public static class Totals {
        public int purchaseOrderCount;
        public int receiptCount;
        public int lineCount;
        public int fullyReceivedLineCount;
        public int partiallyReceivedLineCount;
        public int notReceivedLineCount;
        public int receiptOnlyLineCount;
        public BigDecimal orderedQuantity = BigDecimal.ZERO;
        public BigDecimal receivedQuantity = BigDecimal.ZERO;
        public BigDecimal differenceQuantity = BigDecimal.ZERO;
    }

    private static class ReceiptAggregate {
        private Integer purchaseOrderId;
        private String supplierName;
        private String receiptReference;
        private String receiptDate;
        private String pharmacyName;
        private String program;
        private String itemName;
        private String itemCode;
        private BigDecimal receivedQuantity = BigDecimal.ZERO;
        private List<Integer> receiptIds = new ArrayList<Integer>();
        private List<String> receiptReferences = new ArrayList<String>();
        private List<String> receiptDates = new ArrayList<String>();
        private List<String> pharmacyNames = new ArrayList<String>();

        private ReceiptAggregate(DeliveryReceipt receipt, DeliveryReceiptLine line) {
            PurchaseOrder purchaseOrder = receipt == null ? null : receipt.getPurchaseOrder();
            purchaseOrderId = purchaseOrder == null ? null : purchaseOrder.getPurchaseOrderId();
            supplierName = purchaseOrder == null || purchaseOrder.getSupplier() == null ? "" : safe(purchaseOrder.getSupplier().getName());
            program = line == null ? "" : safe(line.getProgram());
            Item item = line == null ? null : line.getItem();
            ProductMasterItem productMasterItem = line == null ? null : line.getProductMasterItem();
            itemName = firstNonBlank(item == null ? null : item.getName(),
                    productMasterItem == null ? "" : productMasterItem.getItemName());
            itemCode = firstNonBlank(item == null ? null : item.getCode(),
                    productMasterItem == null ? "" : productMasterItem.getNpcCode());
        }

        private void add(DeliveryReceipt receipt, DeliveryReceiptLine line) {
            receivedQuantity = receivedQuantity.add(lineReceivedQuantity(line));
            if (receipt != null) {
                if (receipt.getDeliveryReceiptId() != null && !receiptIds.contains(receipt.getDeliveryReceiptId())) {
                    receiptIds.add(receipt.getDeliveryReceiptId());
                }
                receiptReferences.add(firstNonBlank(receipt.getReceiptNumber(),
                        receipt.getDeliveryReceiptId() == null ? "" : "Receipt-" + receipt.getDeliveryReceiptId()));
                receiptDates.add(formatDateTime(receipt.getDateCreated()));
                PharmacyLocation location = receipt.getReceivedIntoLocation();
                pharmacyNames.add(location == null ? "" : safe(location.getName()));
            }
            receiptReference = joinDistinct(receiptReferences);
            receiptDate = joinDistinct(receiptDates);
            pharmacyName = joinDistinct(pharmacyNames);
        }
    }
}
