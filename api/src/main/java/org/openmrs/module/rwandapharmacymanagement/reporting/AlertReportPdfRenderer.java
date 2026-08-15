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
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.api.dto.AlertFilter;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ExpirationAlertRow;
import org.openmrs.module.rwandapharmacymanagement.api.dto.StockoutAlertRow;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class AlertReportPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
    private static final Font VALUE_FONT = new Font(Font.FontFamily.HELVETICA, 8);
    private static final Font META_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.DARK_GRAY);

    private AlertReportPdfRenderer() {
    }

    public static byte[] render(AlertFilter filter, String locationName, String categoryName,
                                List<ExpirationAlertRow> expirationRows, List<StockoutAlertRow> stockRows,
                                User generatedBy) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 24, 24);
            PdfWriter.getInstance(document, outputStream);
            document.open();
            Paragraph title = new Paragraph("Expiration and Stockout Alerts", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Generated: " + new SimpleDateFormat("dd/MMM/yyyy HH:mm").format(new Date())
                    + " | By: " + (generatedBy == null ? "" : generatedBy.getUsername()), META_FONT));
            document.add(new Paragraph("Location: " + safe(locationName)
                    + " | Category: " + safe(categoryName)
                    + " | Search: " + safe(filter == null ? "" : filter.getSearchTerm())
                    + " | Expiry days: " + number(filter == null ? null : filter.getExpiryAlertDays())
                    + " | Days of stock alert: " + number(filter == null ? null : filter.getDaysOfStockAlertDays())
                    + " | Consumption lookback: " + number(filter == null ? null : filter.getConsumptionLookbackDays()), META_FONT));
            addSection(document, "Expiration Alerts", expirationRows == null ? 0 : expirationRows.size());
            PdfPTable expiryTable = new PdfPTable(new float[]{2.8f, 1.0f, 1.8f, 1.2f, 1.2f, .9f, 1.0f, 1.1f});
            expiryTable.setWidthPercentage(100);
            addHeader(expiryTable, "Item");
            addHeader(expiryTable, "Code");
            addHeader(expiryTable, "Location");
            addHeader(expiryTable, "Batch");
            addHeader(expiryTable, "Expiry");
            addHeader(expiryTable, "Days");
            addHeader(expiryTable, "Qty");
            addHeader(expiryTable, "Status");
            if (expirationRows != null) {
                for (ExpirationAlertRow row : expirationRows) {
                    expiryTable.addCell(bodyCell(row.getItem() == null ? "" : row.getItem().getName()));
                    expiryTable.addCell(bodyCell(row.getItem() == null ? "" : row.getItem().getCode()));
                    expiryTable.addCell(bodyCell(row.getPharmacyLocation() == null ? "Whole hospital" : row.getPharmacyLocation().getName()));
                    expiryTable.addCell(bodyCell(row.getBatchNumber()));
                    expiryTable.addCell(bodyCell(date(row.getExpiryDate())));
                    expiryTable.addCell(bodyCell(String.valueOf(row.getDaysToExpiry())));
                    expiryTable.addCell(bodyCell(decimal(row.getQuantityOnHand())));
                    expiryTable.addCell(bodyCell(row.getStatus()));
                }
            }
            document.add(expiryTable);
            addSection(document, "Stockout and Low Stock Alerts", stockRows == null ? 0 : stockRows.size());
            PdfPTable stockTable = new PdfPTable(new float[]{2.8f, 1.0f, 1.8f, 1.0f, 1.0f, 1.0f, .9f, 1.1f});
            stockTable.setWidthPercentage(100);
            addHeader(stockTable, "Item");
            addHeader(stockTable, "Code");
            addHeader(stockTable, "Location");
            addHeader(stockTable, "Current");
            addHeader(stockTable, "Consumed");
            addHeader(stockTable, "Avg/day");
            addHeader(stockTable, "Days");
            addHeader(stockTable, "Status");
            if (stockRows != null) {
                for (StockoutAlertRow row : stockRows) {
                    stockTable.addCell(bodyCell(row.getItem() == null ? "" : row.getItem().getName()));
                    stockTable.addCell(bodyCell(row.getItem() == null ? "" : row.getItem().getCode()));
                    stockTable.addCell(bodyCell(row.getPharmacyLocation() == null ? "Whole hospital" : row.getPharmacyLocation().getName()));
                    stockTable.addCell(bodyCell(decimal(row.getCurrentQuantity())));
                    stockTable.addCell(bodyCell(decimal(row.getConsumedQuantity())));
                    stockTable.addCell(bodyCell(decimal(row.getAverageDailyConsumption())));
                    stockTable.addCell(bodyCell(row.getDaysOfStock() == null ? "" : decimal(row.getDaysOfStock())));
                    stockTable.addCell(bodyCell(row.getStatus()));
                }
            }
            document.add(stockTable);
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render alert report PDF", e);
        }
    }

    private static void addSection(Document document, String title, int count) throws DocumentException {
        Paragraph section = new Paragraph(title + " (" + count + ")", SECTION_FONT);
        section.setSpacingBefore(12);
        section.setSpacingAfter(5);
        document.add(section);
    }

    private static void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(4, 93, 86));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static PdfPCell bodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), VALUE_FONT));
        cell.setPadding(4);
        return cell;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String date(Date value) {
        return value == null ? "" : new SimpleDateFormat("dd/MMM/yyyy").format(value);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String number(Integer value) {
        return value == null ? "" : value.toString();
    }
}
