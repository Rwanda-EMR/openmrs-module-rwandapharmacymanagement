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
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequestLine;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public final class PharmacyItemRequestPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

    private PharmacyItemRequestPdfRenderer() {
    }

    public static byte[] render(PharmacyItemRequest request, String hospitalName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Pharmacy Item Request", TITLE_FONT));
            if (!safe(hospitalName).isEmpty()) {
                document.add(new Paragraph(hospitalName.trim(), LABEL_FONT));
            }
            document.add(new Paragraph(" "));

            addMeta(document, "Request ID", request == null || request.getPharmacyItemRequestId() == null
                    ? "" : "#" + request.getPharmacyItemRequestId());
            addMeta(document, "Requested pharmacy", request == null || request.getRequestedPharmacyLocation() == null
                    ? "" : safe(request.getRequestedPharmacyLocation().getName()));
            addMeta(document, "Status", request == null ? "" : safe(request.getRequestStatus()));
            addMeta(document, "Reason", request == null ? "" : safe(request.getReason()));
            addMeta(document, "Requested by", request == null ? "" : safe(UserNameUtils.plainName(request.getCreator())));
            addMeta(document, "Requested date", formatDate(request == null ? null : request.getDateCreated()));
            addMeta(document, "Reviewed by", request == null ? "" : safe(UserNameUtils.plainName(request.getApprovedBy())));
            addMeta(document, "Reviewed date", formatDate(request == null ? null : request.getApprovedDate()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{3.2f, 1.2f, 1.2f});
            table.setWidthPercentage(100);
            addHeader(table, "Item");
            addHeader(table, "Requested Qty");
            addHeader(table, "Approved Qty");

            if (request != null && request.getLines() != null) {
                for (PharmacyItemRequestLine line : request.getLines()) {
                    if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                        continue;
                    }
                    table.addCell(bodyCell(line.getItem() == null ? "" : safe(line.getItem().getName())));
                    table.addCell(bodyCell(number(line.getRequestedQuantity())));
                    table.addCell(bodyCell(line.getApprovedQuantity() == null ? "" : number(line.getApprovedQuantity())));
                }
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total", LABEL_FONT));
            totalLabel.setColspan(1);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setBackgroundColor(new BaseColor(245, 245, 245));
            totalLabel.setPadding(6f);
            table.addCell(totalLabel);
            table.addCell(summaryValue(number(totalRequested(request))));
            table.addCell(summaryValue(number(totalApproved(request))));

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render pharmacy item request PDF", e);
        }
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

    private static PdfPCell summaryValue(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), LABEL_FONT));
        cell.setPadding(6f);
        cell.setBackgroundColor(new BaseColor(245, 245, 245));
        return cell;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String formatDate(java.util.Date date) {
        return date == null ? "" : new SimpleDateFormat("dd MMM yyyy HH:mm").format(date);
    }

    private static String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal totalRequested(PharmacyItemRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        if (request == null || request.getLines() == null) {
            return total;
        }
        for (PharmacyItemRequestLine line : request.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided()) && line.getRequestedQuantity() != null) {
                total = total.add(line.getRequestedQuantity());
            }
        }
        return total;
    }

    private static BigDecimal totalApproved(PharmacyItemRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        if (request == null || request.getLines() == null) {
            return total;
        }
        for (PharmacyItemRequestLine line : request.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided()) && line.getApprovedQuantity() != null) {
                total = total.add(line.getApprovedQuantity());
            }
        }
        return total;
    }
}
