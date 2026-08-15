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
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLineBatch;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public final class InterPharmacyTransferPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

    private InterPharmacyTransferPdfRenderer() {
    }

    public static byte[] render(InterPharmacyTransfer transfer, String hospitalName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Pharmacy Transfer", TITLE_FONT));
            if (!safe(hospitalName).isEmpty()) {
                document.add(new Paragraph(hospitalName.trim(), LABEL_FONT));
            }
            document.add(new Paragraph(" "));

            addMeta(document, "Transfer ID", transfer == null || transfer.getInterPharmacyTransferId() == null
                    ? "" : "#" + transfer.getInterPharmacyTransferId());
            addMeta(document, "Source pharmacy", transfer == null || transfer.getSourcePharmacyLocation() == null
                    ? "" : safe(transfer.getSourcePharmacyLocation().getName()));
            addMeta(document, "Destination pharmacy", transfer == null || transfer.getDestinationPharmacyLocation() == null
                    ? "" : safe(transfer.getDestinationPharmacyLocation().getName()));
            addMeta(document, "Status", transfer == null ? "" : status(transfer.getTransferStatus()));
            addMeta(document, "Reason", transfer == null ? "" : safe(transfer.getReason()));
            addMeta(document, "Requested by", transfer == null ? "" : safe(UserNameUtils.plainName(transfer.getCreator())));
            addMeta(document, "Requested date", formatDate(transfer == null ? null : transfer.getDateCreated()));
            addMeta(document, "Approved by", transfer == null ? "" : safe(UserNameUtils.plainName(transfer.getApprovedBy())));
            addMeta(document, "Approved date", formatDate(transfer == null ? null : transfer.getApprovedDate()));
            addMeta(document, "Transferred by", transfer == null ? "" : safe(UserNameUtils.plainName(transfer.getServedBy())));
            addMeta(document, "Transferred date", formatDate(transfer == null ? null : transfer.getServedDate()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{3.0f, 1.1f, 1.4f, 1.4f, 1.2f, 1.2f});
            table.setWidthPercentage(100);
            addHeader(table, "Item");
            addHeader(table, "Qty");
            addHeader(table, "Batch");
            addHeader(table, "Expiry");
            addHeader(table, "Unit Cost");
            addHeader(table, "Value");

            if (transfer != null && transfer.getLines() != null) {
                for (InterPharmacyTransferLine line : transfer.getLines()) {
                    if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                        continue;
                    }
                    if (line.getBatchAllocations() != null && !line.getBatchAllocations().isEmpty()) {
                        for (InterPharmacyTransferLineBatch batch : line.getBatchAllocations()) {
                            if (batch != null && !Boolean.TRUE.equals(batch.getVoided())) {
                                addLine(table, line, batch.getQuantity(), batch.getBatchNumber(), batch.getExpiryDate(),
                                        batch.getUnitPrice(), batch.getTotalAmount());
                            }
                        }
                    } else {
                        addLine(table, line, line.getQuantity(), line.getBatchNumber(), line.getExpiryDate(),
                                line.getUnitPrice(), line.getTotalAmount());
                    }
                }
            }
            addTotal(table, totalValue(transfer));
            document.add(table);
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render pharmacy transfer PDF", e);
        }
    }

    private static void addLine(PdfPTable table, InterPharmacyTransferLine line, BigDecimal quantity, String batchNumber,
                                java.util.Date expiryDate, BigDecimal unitPrice, BigDecimal totalAmount) {
        table.addCell(bodyCell(line.getItem() == null ? "" : safe(line.getItem().getName())));
        table.addCell(bodyCell(number(quantity)));
        table.addCell(bodyCell(safe(batchNumber)));
        table.addCell(bodyCell(formatDateOnly(expiryDate)));
        table.addCell(bodyCell(number(unitPrice)));
        table.addCell(bodyCell(number(totalAmount == null && unitPrice != null && quantity != null
                ? unitPrice.multiply(quantity) : totalAmount)));
    }

    private static void addTotal(PdfPTable table, BigDecimal total) {
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total value", LABEL_FONT));
        totalLabel.setColspan(5);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(new BaseColor(245, 245, 245));
        totalLabel.setPadding(6f);
        table.addCell(totalLabel);
        PdfPCell totalValue = new PdfPCell(new Phrase(number(total), LABEL_FONT));
        totalValue.setBackgroundColor(new BaseColor(245, 245, 245));
        totalValue.setPadding(6f);
        table.addCell(totalValue);
    }

    private static void addMeta(Document document, String label, String value) throws DocumentException {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase(label + ": ", LABEL_FONT));
        paragraph.add(new Phrase(safe(value), VALUE_FONT));
        document.add(paragraph);
    }

    private static void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(47, 111, 173));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static PdfPCell bodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), VALUE_FONT));
        cell.setPadding(6f);
        return cell;
    }

    private static BigDecimal totalValue(InterPharmacyTransfer transfer) {
        BigDecimal total = BigDecimal.ZERO;
        if (transfer == null || transfer.getLines() == null) {
            return total;
        }
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            BigDecimal lineTotal = line.getTotalAmount();
            if (lineTotal == null && line.getUnitPrice() != null && line.getQuantity() != null) {
                lineTotal = line.getUnitPrice().multiply(line.getQuantity());
            }
            total = total.add(lineTotal == null ? BigDecimal.ZERO : lineTotal);
        }
        return total;
    }

    private static String status(String value) {
        return "SERVED".equalsIgnoreCase(value) ? "TRANSFERRED" : safe(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String formatDate(java.util.Date date) {
        return date == null ? "" : new SimpleDateFormat("dd MMM yyyy HH:mm").format(date);
    }

    private static String formatDateOnly(java.util.Date date) {
        return date == null ? "" : new SimpleDateFormat("dd MMM yyyy").format(date);
    }

    private static String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
