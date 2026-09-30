package org.openmrs.module.rwandapharmacymanagement.reporting;

import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public final class DeliveryReceiptTemplateRenderer {

    private static final String UTF_8 = "UTF-8";
    private static final String FORMAT_INSTRUCTION = "Allowed format: itemCode=0000000000, itemName=text, program=text, requestedQuantity=0, purchaseUnitPrice=0, receivedQuantity=0, receivingPrice=0, batchNumber=text, expiryDate=yyyy-MM-dd or N/A";

    private DeliveryReceiptTemplateRenderer() {
    }

    public static byte[] renderBlankTemplate() throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        appendFormatInstruction(builder);
        appendHeader(builder);
        appendRow(builder, "N02BE0106E", "PARACETAMOL 500MG", "CP", "", "", "15", "1250", "BATCH-001", "2028-06-30");
        return toCsvBytes(builder);
    }

    public static byte[] renderPurchaseOrderTemplate(PurchaseOrder purchaseOrder) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        appendFormatInstruction(builder);
        appendHeader(builder);
        if (purchaseOrder != null && purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                if (item == null) {
                    continue;
                }
                appendRow(
                        builder,
                        item.getItemCode(),
                        item.getItemName(),
                        item.getProgram(),
                        item.getQuantity() == null ? "" : item.getQuantity().stripTrailingZeros().toPlainString(),
                        item.getUnitPrice() == null ? "" : item.getUnitPrice().stripTrailingZeros().toPlainString(),
                        "",
                        "",
                        "",
                        ""
                );
            }
        }
        return toCsvBytes(builder);
    }

    private static void appendFormatInstruction(StringBuilder builder) {
        appendRow(builder, FORMAT_INSTRUCTION);
    }

    private static void appendHeader(StringBuilder builder) {
        appendRow(builder,
                "itemCode",
                "itemName",
                "program",
                "requestedQuantity",
                "purchaseUnitPrice",
                "receivedQuantity",
                "receivingPrice",
                "batchNumber",
                "expiryDate");
    }

    private static void appendRow(StringBuilder builder, String... values) {
        if (values == null) {
            builder.append("\r\n");
            return;
        }
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(csv(values[i]));
        }
        builder.append("\r\n");
    }

    private static String csv(String value) {
        String safe = sanitize(value);
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private static byte[] toCsvBytes(StringBuilder builder) throws UnsupportedEncodingException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);
            outputStream.write(builder.toString().getBytes(UTF_8));
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UnsupportedEncodingException(e.getMessage());
        }
    }
}
