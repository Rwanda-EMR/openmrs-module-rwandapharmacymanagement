package org.openmrs.module.rwandapharmacymanagement.reporting;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public final class DeliveryReceiptPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);

    private DeliveryReceiptPdfRenderer() {
    }

    public static byte[] render(DeliveryReceipt deliveryReceipt) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Delivery Receipt Proof", TITLE_FONT));
            document.add(new Paragraph(" "));

            addMeta(document, "Receipt number", safe(deliveryReceipt.getReceiptNumber()));
            addMeta(document, "Receipt date", deliveryReceipt.getDateCreated() == null ? "" : new SimpleDateFormat("dd MMM yyyy HH:mm").format(deliveryReceipt.getDateCreated()));
            addMeta(document, "Purchase order", deliveryReceipt.getPurchaseOrder() == null ? "" : "PO-" + deliveryReceipt.getPurchaseOrder().getPurchaseOrderId());
            addMeta(document, "RMS / supplier order", deliveryReceipt.getSupplierOrder() == null ? "" : safe(deliveryReceipt.getSupplierOrder().getOrderNumber()));
            addMeta(document, "Received into", deliveryReceipt.getReceivedIntoLocation() == null ? "" : safe(deliveryReceipt.getReceivedIntoLocation().getName()));
            addMeta(document, "Received by", safe(UserNameUtils.plainName(deliveryReceipt.getCreator())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2.6f, 1.2f, 0.8f, 1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.6f});
            table.setWidthPercentage(100);
            addHeader(table, "Item");
            addHeader(table, "Code");
            addHeader(table, "Line");
            addHeader(table, "Ordered");
            addHeader(table, "Received");
            addHeader(table, "Accepted");
            addHeader(table, "Rejected");
            addHeader(table, "Reason");
            addHeader(table, "Batch / Expiry");

            BigDecimal totalAccepted = BigDecimal.ZERO;
            BigDecimal totalRejected = BigDecimal.ZERO;
            if (deliveryReceipt.getLines() != null) {
                for (DeliveryReceiptLine line : deliveryReceipt.getLines()) {
                    BigDecimal accepted = safe(line.getAcceptedQuantity());
                    BigDecimal rejected = safe(line.getReceivedQuantity()).subtract(accepted);
                    if (rejected.compareTo(BigDecimal.ZERO) < 0) {
                        rejected = BigDecimal.ZERO;
                    }
                    totalAccepted = totalAccepted.add(accepted);
                    totalRejected = totalRejected.add(rejected);

                    table.addCell(bodyCell(line.getItem() == null ? "" : safe(line.getItem().getName())));
                    table.addCell(bodyCell(line.getItem() == null ? "" : safe(line.getItem().getCode())));
                    table.addCell(bodyCell(safe(line.getConfirmationItemNo())));
                    table.addCell(bodyCell(number(line.getOrderedQuantity())));
                    table.addCell(bodyCell(number(line.getReceivedQuantity())));
                    table.addCell(bodyCell(number(accepted)));
                    table.addCell(bodyCell(number(rejected)));
                    table.addCell(bodyCell(reasonText(line)));
                    table.addCell(bodyCell(batchExpiry(line)));
                }
            }

            PdfPCell summaryLabel = new PdfPCell(new Phrase("Accepted total / Rejected total", LABEL_FONT));
            summaryLabel.setColspan(5);
            summaryLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryLabel.setBackgroundColor(new BaseColor(245, 245, 245));
            summaryLabel.setPadding(6f);
            table.addCell(summaryLabel);
            table.addCell(summaryValue(number(totalAccepted)));
            table.addCell(summaryValue(number(totalRejected)));
            table.addCell(summaryValue(""));
            table.addCell(summaryValue(""));

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("This document is proof of what was physically received and accepted into stock.", VALUE_FONT));
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render delivery receipt PDF", e);
        }
    }

    private static void addMeta(Document document, String label, String value) throws DocumentException {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase(label + ": ", LABEL_FONT));
        paragraph.add(new Phrase(value, VALUE_FONT));
        document.add(paragraph);
    }

    private static void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(36, 109, 171));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static PdfPCell bodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, VALUE_FONT));
        cell.setPadding(6f);
        return cell;
    }

    private static PdfPCell summaryValue(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL_FONT));
        cell.setPadding(6f);
        cell.setBackgroundColor(new BaseColor(245, 245, 245));
        return cell;
    }

    private static String batchExpiry(DeliveryReceiptLine line) {
        String batch = line == null ? "" : safe(line.getBatchNumber());
        String expiry = line == null || line.getExpiryDate() == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(line.getExpiryDate());
        if (batch.isEmpty()) {
            return expiry;
        }
        if (expiry.isEmpty()) {
            return batch;
        }
        return batch + " / " + expiry;
    }

    private static String reasonText(DeliveryReceiptLine line) {
        if (line == null) {
            return "";
        }
        String code = safe(line.getRejectionReasonCode());
        String detail = safe(line.getRejectionReasonDetail());
        if (code.isEmpty()) {
            return detail;
        }
        if (detail.isEmpty()) {
            return code;
        }
        return code + ": " + detail;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
