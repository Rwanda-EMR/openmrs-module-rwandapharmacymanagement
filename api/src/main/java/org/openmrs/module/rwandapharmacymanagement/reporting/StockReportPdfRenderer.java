package org.openmrs.module.rwandapharmacymanagement.reporting;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.List;

public final class StockReportPdfRenderer {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font BODY_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);

    private StockReportPdfRenderer() {
    }

    public static byte[] render(String title, List<String> headers, List<List<String>> rows) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int columnCount = headers == null ? 0 : headers.size();
            Rectangle pageSize = columnCount > 10 ? PageSize.A3.rotate() : PageSize.A4.rotate();
            Font bodyFont = columnCount > 10 ? new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL) : BODY_FONT;
            Font headerFont = columnCount > 10 ? new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, BaseColor.WHITE) : HEADER_FONT;
            Document document = new Document(pageSize, 18, 18, 18, 18);
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph(title, TITLE_FONT));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);
            table.setSplitLate(false);
            table.setSplitRows(true);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(31, 95, 153));
                cell.setPadding(4f);
                cell.setNoWrap(false);
                table.addCell(cell);
            }
            for (List<String> row : rows) {
                for (int i = 0; i < columnCount; i++) {
                    String value = row == null || row.size() <= i ? "" : row.get(i);
                    PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, bodyFont));
                    cell.setPadding(3f);
                    cell.setNoWrap(false);
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return outputStream.toByteArray();
        }
        catch (DocumentException e) {
            throw new IllegalStateException("Unable to render stock report PDF", e);
        }
    }
}
