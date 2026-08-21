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
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturnLine;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public final class DepartmentStockReturnPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

    private DepartmentStockReturnPdfRenderer() {
    }

    public static byte[] render(DepartmentStockReturn stockReturn, String hospitalName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Department Stock Return", TITLE_FONT));
            if (!safe(hospitalName).isEmpty()) {
                document.add(new Paragraph(hospitalName.trim(), LABEL_FONT));
            }
            document.add(new Paragraph(" "));

            addMeta(document, "Return ID", stockReturn == null || stockReturn.getDepartmentStockReturnId() == null
                    ? "" : "#" + stockReturn.getDepartmentStockReturnId());
            addMeta(document, "Department store", stockReturn == null || stockReturn.getDepartmentLocation() == null
                    ? "" : safe(stockReturn.getDepartmentLocation().getName()));
            addMeta(document, "Destination pharmacy", stockReturn == null || stockReturn.getDestinationPharmacyLocation() == null
                    ? "" : safe(stockReturn.getDestinationPharmacyLocation().getName()));
            addMeta(document, "Status", stockReturn == null ? "" : safe(stockReturn.getReturnStatus()));
            addMeta(document, "Submitted by", stockReturn == null ? "" : safe(UserNameUtils.plainName(stockReturn.getCreator())));
            addMeta(document, "Submitted date", formatDate(stockReturn == null ? null : stockReturn.getDateCreated()));
            addMeta(document, "Received by", stockReturn == null ? "" : safe(UserNameUtils.plainName(stockReturn.getReceivedBy())));
            addMeta(document, "Received date", formatDate(stockReturn == null ? null : stockReturn.getReceivedDate()));
            addMeta(document, "Rejected by", stockReturn == null ? "" : safe(UserNameUtils.plainName(stockReturn.getRejectedBy())));
            addMeta(document, "Rejected date", formatDate(stockReturn == null ? null : stockReturn.getRejectedDate()));
            addMeta(document, "Reason", stockReturn == null ? "" : safe(stockReturn.getReason()));
            addMeta(document, "Rejection reason", stockReturn == null ? "" : safe(stockReturn.getRejectionReason()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{3.0f, 1.3f, 1.3f, 1.2f, 1.2f, 1.3f});
            table.setWidthPercentage(100);
            addHeader(table, "Item");
            addHeader(table, "Batch");
            addHeader(table, "Expiry");
            addHeader(table, "Quantity");
            addHeader(table, "Unit price");
            addHeader(table, "Total");

            if (stockReturn != null && stockReturn.getLines() != null) {
                for (DepartmentStockReturnLine line : stockReturn.getLines()) {
                    if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                        continue;
                    }
                    table.addCell(bodyCell(line.getItem() == null ? "" : safe(line.getItem().getName())));
                    table.addCell(bodyCell(safe(line.getBatchNumber())));
                    table.addCell(bodyCell(formatDateOnly(line.getExpiryDate())));
                    table.addCell(bodyCell(number(line.getQuantity())));
                    table.addCell(bodyCell(number(line.getUnitPrice())));
                    table.addCell(bodyCell(number(lineTotal(line))));
                }
            }
            addTotal(table, totalQuantity(stockReturn), totalAmount(stockReturn));
            document.add(table);
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render department stock return PDF", e);
        }
    }

    private static void addTotal(PdfPTable table, BigDecimal totalQuantity, BigDecimal totalAmount) {
        PdfPCell totalLabel = new PdfPCell(new Phrase("Totals", LABEL_FONT));
        totalLabel.setColspan(3);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(new BaseColor(245, 245, 245));
        totalLabel.setPadding(6f);
        table.addCell(totalLabel);
        totalCell(table, number(totalQuantity));
        totalCell(table, "");
        totalCell(table, number(totalAmount));
    }

    private static void totalCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(value), LABEL_FONT));
        cell.setBackgroundColor(new BaseColor(245, 245, 245));
        cell.setPadding(6f);
        table.addCell(cell);
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

    private static BigDecimal totalQuantity(DepartmentStockReturn stockReturn) {
        BigDecimal total = BigDecimal.ZERO;
        if (stockReturn == null || stockReturn.getLines() == null) {
            return total;
        }
        for (DepartmentStockReturnLine line : stockReturn.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided()) && line.getQuantity() != null) {
                total = total.add(line.getQuantity());
            }
        }
        return total;
    }

    private static BigDecimal totalAmount(DepartmentStockReturn stockReturn) {
        BigDecimal total = BigDecimal.ZERO;
        if (stockReturn == null || stockReturn.getLines() == null) {
            return total;
        }
        for (DepartmentStockReturnLine line : stockReturn.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided())) {
                total = total.add(lineTotal(line));
            }
        }
        return total;
    }

    private static BigDecimal lineTotal(DepartmentStockReturnLine line) {
        if (line == null) {
            return BigDecimal.ZERO;
        }
        if (line.getTotalAmount() != null) {
            return line.getTotalAmount();
        }
        if (line.getQuantity() == null || line.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return line.getQuantity().multiply(line.getUnitPrice());
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
