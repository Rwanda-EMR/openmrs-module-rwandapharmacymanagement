package org.openmrs.module.rwandapharmacymanagement.integration.supplier;

import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrder;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrderLine;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class SupplierPayloadBuilder {

    private static final SimpleDateFormat DATE_ONLY = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_COMPACT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat ISO_OFFSET = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    static {
        TimeZone utcPlusTwo = TimeZone.getTimeZone("GMT+02:00");
        DATE_ONLY.setTimeZone(utcPlusTwo);
        DATE_COMPACT.setTimeZone(utcPlusTwo);
        ISO_OFFSET.setTimeZone(utcPlusTwo);
    }

    private SupplierPayloadBuilder() {
    }

    public static String buildSubmitOrderPayload(SupplierOrder supplierOrder, String facilityId) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        appendField(builder, "orderNumber", value(supplierOrder == null ? null : supplierOrder.getOrderNumber()), true);
        appendField(builder, "facilityId", value(facilityId), true);
        builder.append("\"orderDetails\":[");
        List<SupplierOrderLine> lines = supplierOrder == null ? null : supplierOrder.getLines();
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                SupplierOrderLine line = lines.get(i);
                if (i > 0) {
                    builder.append(",");
                }
                builder.append("{");
                appendField(builder, "lineNumber", String.valueOf((i + 1) * 10), true);
                appendField(builder, "requestDeliveryDate", formatDate(new Date()), true);
                appendNumeric(builder, "requestQuantity", line.getOrderedQuantity(), true);
                appendField(builder, "quantityUom", "EA", true);
                appendNumeric(builder, "unitPrice", line.getUnitPrice(), true);
                BigDecimal total = safe(line.getOrderedQuantity()).multiply(safe(line.getUnitPrice()));
                appendNumeric(builder, "totalAmount", total, true);
                appendField(builder, "currency", "RWF", true);
                appendNumeric(builder, "lineAmount", total, true);
                appendField(builder, "itemName", line.getSupplierProductName() == null
                        ? (line.getItem() == null ? "" : value(line.getItem().getName()))
                        : value(line.getSupplierProductName()), true);
                appendField(builder, "lnProgramName", value(line.getProgramName() == null ? "CP" : line.getProgramName()), false);
                builder.append("}");
            }
        }
        builder.append("]}");
        return builder.toString();
    }

    public static String buildDeliveryConfirmationPayload(DeliveryReceipt deliveryReceipt, String customerCode) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        appendField(builder, "deliveryId", value(deliveryReceipt == null ? null : deliveryReceipt.getReceiptNumber()), true);
        appendField(builder, "orderNo", deliveryReceipt != null && deliveryReceipt.getSupplierOrder() != null
                ? value(deliveryReceipt.getSupplierOrder().getOrderNumber()) : "", true);
        appendField(builder, "customerCode", value(customerCode), true);
        appendField(builder, "deliveryDate", formatDate(deliveryReceipt == null ? null : deliveryReceipt.getDateCreated()), true);
        appendField(builder, "deliveryTimestamp", formatTimestamp(deliveryReceipt == null ? null : deliveryReceipt.getDateCreated()), true);
        builder.append("\"items\":[");
        List<DeliveryReceiptLine> lines = deliveryReceipt == null ? null : deliveryReceipt.getLines();
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                DeliveryReceiptLine line = lines.get(i);
                if (i > 0) {
                    builder.append(",");
                }
                builder.append("{");
                appendField(builder, "itemNo", value(line.getConfirmationItemNo() == null ? String.valueOf(i + 1) : line.getConfirmationItemNo()), true);
                appendField(builder, "npcCode", line.getItem() == null ? "" : value(line.getItem().getCode()), true);
                appendField(builder, "batch", value(line.getBatchNumber()), true);
                appendNumeric(builder, "deliveredQuantity", line.getReceivedQuantity(), true);
                appendNumeric(builder, "acceptedQuantity", line.getAcceptedQuantity(), true);
                BigDecimal rejected = safe(line.getReceivedQuantity()).subtract(safe(line.getAcceptedQuantity()));
                appendNumeric(builder, "rejectedQuantity", rejected.max(BigDecimal.ZERO), true);
                appendField(builder, "uom", "EA", true);
                appendRejection(builder, line, rejected);
                appendField(builder, "expiryDate", formatCompactDate(line.getExpiryDate()), true);
                appendField(builder, "productCode", line.getItem() == null ? "" : value(line.getItem().getCode()), true);
                appendField(builder, "notes", value(line.getNotes()), false);
                builder.append("}");
            }
        }
        builder.append("],");
        builder.append("\"signoff\":{");
        User creator = deliveryReceipt == null ? null : deliveryReceipt.getCreator();
        appendField(builder, "receivedByName", value(UserNameUtils.plainName(creator)), true);
        appendField(builder, "receivedByRole", "Pharmacist", true);
        appendField(builder, "nationalIdOrStaffId", creator == null ? "" : value(creator.getSystemId()), true);
        appendField(builder, "contactPhone", "", true);
        appendField(builder, "signedAt", formatTimestamp(deliveryReceipt == null ? null : deliveryReceipt.getDateCreated()), false);
        builder.append("},");
        appendField(builder, "overallNotes", "", true);
        appendField(builder, "idempotencyKey", "pod-" + value(deliveryReceipt == null ? null : deliveryReceipt.getReceiptNumber()) + "-v1", false);
        builder.append("}");
        return builder.toString();
    }

    public static String buildPriceLookupPayload(String facilityId, String program, List<String> npcCodes) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        appendField(builder, "facilityId", value(facilityId), true);
        appendField(builder, "program", value(program), true);
        builder.append("\"npcCodes\":[");
        List<String> cleaned = npcCodes == null ? new ArrayList<String>() : npcCodes;
        for (int i = 0; i < cleaned.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("\"").append(escape(value(cleaned.get(i)))).append("\"");
        }
        builder.append("]}");
        return builder.toString();
    }

    public static String buildDeliveryDetailsPayload(String facilityId, String orderNumber) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        appendField(builder, "facility_id", value(facilityId), true);
        appendField(builder, "order_number", value(orderNumber), false);
        builder.append("}");
        return builder.toString();
    }

    private static void appendField(StringBuilder builder, String key, String value, boolean withComma) {
        builder.append("\"").append(key).append("\":\"").append(escape(value)).append("\"");
        if (withComma) {
            builder.append(",");
        }
    }

    private static void appendNumeric(StringBuilder builder, String key, BigDecimal value, boolean withComma) {
        builder.append("\"").append(key).append("\":").append(value == null ? "0" : value.stripTrailingZeros().toPlainString());
        if (withComma) {
            builder.append(",");
        }
    }

    private static void appendRejection(StringBuilder builder, DeliveryReceiptLine line, BigDecimal rejectedQuantity) {
        if (rejectedQuantity == null || rejectedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        builder.append("\"rejection\":{");
        appendField(builder, "reasonCode", value(line == null || line.getRejectionReasonCode() == null
                ? "OTHER" : line.getRejectionReasonCode()), true);
        appendField(builder, "reasonDetail", value(line == null || line.getRejectionReasonDetail() == null
                ? "Rejected quantity recorded during receiving." : line.getRejectionReasonDetail()), true);
        builder.append("\"evidenceUrls\":[]");
        builder.append("},");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String formatDate(Date date) {
        return DATE_ONLY.format(date == null ? new Date() : date);
    }

    private static String formatCompactDate(Date date) {
        return date == null ? "" : DATE_COMPACT.format(date);
    }

    private static String formatTimestamp(Date date) {
        return ISO_OFFSET.format(date == null ? new Date() : date);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
