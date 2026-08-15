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
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public final class DepartmentTransferPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

    private DepartmentTransferPdfRenderer() {
    }

    public static byte[] render(DepartmentBulkRequest request, String hospitalName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Department Transfer", TITLE_FONT));
            if (!safe(hospitalName).isEmpty()) {
                document.add(new Paragraph(hospitalName.trim(), LABEL_FONT));
            }
            document.add(new Paragraph(" "));

            addMeta(document, "Request ID", request == null || request.getDepartmentBulkRequestId() == null
                    ? "" : "#" + request.getDepartmentBulkRequestId());
            addMeta(document, "Supplying pharmacy", request == null || request.getSupplyingPharmacyLocation() == null
                    ? "" : safe(request.getSupplyingPharmacyLocation().getName()));
            addMeta(document, "Department store", request == null || request.getDepartmentLocation() == null
                    ? "" : safe(request.getDepartmentLocation().getName()));
            addMeta(document, "Status", request == null ? "" : safe(request.getRequestStatus()));
            addMeta(document, "Workflow mode", request == null ? "" : safe(request.getWorkflowMode()));
            addMeta(document, "Requested by", request == null ? "" : safe(UserNameUtils.plainName(request.getCreator())));
            addMeta(document, "Requested date", formatDate(request == null ? null : request.getDateCreated()));
            addMeta(document, "Finance approved by", request == null ? "" : safe(UserNameUtils.plainName(request.getFinanceApprovedBy())));
            addMeta(document, "Finance approved date", formatDate(request == null ? null : request.getFinanceApprovedDate()));
            addMeta(document, "Transferred by", request == null ? "" : safe(UserNameUtils.plainName(request.getDispensedBy())));
            addMeta(document, "Transferred date", formatDate(request == null ? null : request.getDispensedDate()));
            addMeta(document, "Justification", request == null ? "" : safe(request.getJustification()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{3.0f, 1.1f, 1.1f, 1.1f, 1.4f});
            table.setWidthPercentage(100);
            addHeader(table, "Item");
            addHeader(table, "Requested");
            addHeader(table, "Approved");
            addHeader(table, "Transferred");
            addHeader(table, "Batch");

            if (request != null && request.getLines() != null) {
                for (DepartmentBulkRequestLine line : request.getLines()) {
                    if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                        continue;
                    }
                    table.addCell(bodyCell(line.getItem() == null ? "" : safe(line.getItem().getName())));
                    table.addCell(bodyCell(number(line.getRequestedQuantity())));
                    table.addCell(bodyCell(number(line.getApprovedQuantity())));
                    table.addCell(bodyCell(number(line.getDispensedQuantity())));
                    table.addCell(bodyCell(safe(line.getBatchNumber())));
                }
            }
            addTotal(table, totalDispensed(request));
            document.add(table);
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render department transfer PDF", e);
        }
    }

    private static void addTotal(PdfPTable table, BigDecimal total) {
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total transferred quantity", LABEL_FONT));
        totalLabel.setColspan(3);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(new BaseColor(245, 245, 245));
        totalLabel.setPadding(6f);
        table.addCell(totalLabel);
        PdfPCell totalValue = new PdfPCell(new Phrase(number(total), LABEL_FONT));
        totalValue.setColspan(2);
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

    private static BigDecimal totalDispensed(DepartmentBulkRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        if (request == null || request.getLines() == null) {
            return total;
        }
        for (DepartmentBulkRequestLine line : request.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided()) && line.getDispensedQuantity() != null) {
                total = total.add(line.getDispensedQuantity());
            }
        }
        return total;
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
}
