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
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public final class PurchaseOrderPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

    private PurchaseOrderPdfRenderer() {
    }

    public static byte[] render(PurchaseOrder purchaseOrder) {
        return render(purchaseOrder, null);
    }

    public static byte[] render(PurchaseOrder purchaseOrder, String hospitalName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Purchase Order", TITLE_FONT));
            if (hospitalName != null && !hospitalName.trim().isEmpty()) {
                document.add(new Paragraph(hospitalName.trim(), LABEL_FONT));
            }
            document.add(new Paragraph(" "));

            addMeta(document, "Hospital", safe(hospitalName));
            addMeta(document, "Program", safe(purchaseOrder.getProgram()));
            addMeta(document, "Supplier", purchaseOrder.getSupplier() == null ? "" : safe(purchaseOrder.getSupplier().getName()));
            addMeta(document, "Facility ID", safe(purchaseOrder.getFacilityId()));
            addMeta(document, "Requested date", purchaseOrder.getRequestedDate() == null ? "" : new SimpleDateFormat("dd MMM yyyy HH:mm").format(purchaseOrder.getRequestedDate()));
            addMeta(document, "Requested by", safe(UserNameUtils.plainName(purchaseOrder.getRequestedBy())));
            addMeta(document, "Approval status", safe(purchaseOrder.getApprovalStatus()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{3.0f, 1.8f, 1.1f, 1.2f, 1.4f});
            table.setWidthPercentage(100);
            addHeader(table, "Item");
            addHeader(table, "Code");
            addHeader(table, "Qty");
            addHeader(table, "Unit Price");
            addHeader(table, "Total");

            if (purchaseOrder.getItems() != null) {
                for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                    table.addCell(bodyCell(safe(item.getItemName())));
                    table.addCell(bodyCell(safe(item.getItemCode())));
                    table.addCell(bodyCell(number(item.getQuantity())));
                    table.addCell(bodyCell(number(item.getUnitPrice())));
                    table.addCell(bodyCell(number(item.getTotalPrice())));
                }
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total cost", LABEL_FONT));
            totalLabel.setColspan(4);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setBackgroundColor(new BaseColor(245, 245, 245));
            totalLabel.setPadding(6f);
            table.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase(number(purchaseOrder.getTotalCost()), LABEL_FONT));
            totalValue.setPadding(6f);
            totalValue.setBackgroundColor(new BaseColor(245, 245, 245));
            table.addCell(totalValue);

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Requested by: "
                    + safe(UserNameUtils.plainName(purchaseOrder.getRequestedBy())),
                    VALUE_FONT));
            document.add(new Paragraph("Approved by: "
                    + safe(UserNameUtils.plainName(purchaseOrder.getApprovedBy())),
                    VALUE_FONT));
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render purchase order PDF", e);
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
        cell.setBackgroundColor(new BaseColor(111, 80, 164));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static PdfPCell bodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, VALUE_FONT));
        cell.setPadding(6f);
        return cell;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
