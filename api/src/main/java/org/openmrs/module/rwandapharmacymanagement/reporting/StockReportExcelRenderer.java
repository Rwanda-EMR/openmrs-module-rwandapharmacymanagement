package org.openmrs.module.rwandapharmacymanagement.reporting;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;

public final class StockReportExcelRenderer {

    private StockReportExcelRenderer() {
    }

    public static byte[] render(String sheetName, List<String> headers, List<List<String>> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<?mso-application progid=\"Excel.Sheet\"?>");
        xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
        xml.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"");
        xml.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"");
        xml.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");
        xml.append("<Styles>");
        xml.append("<Style ss:ID=\"Header\"><Font ss:Bold=\"1\"/></Style>");
        xml.append("</Styles>");
        xml.append("<Worksheet ss:Name=\"").append(escapeXml(trimSheetName(sheetName))).append("\">");
        xml.append("<Table>");
        xml.append("<Row>");
        for (String header : headers) {
            xml.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">")
                    .append(escapeXml(header))
                    .append("</Data></Cell>");
        }
        xml.append("</Row>");
        for (List<String> row : rows) {
            xml.append("<Row>");
            for (String value : row) {
                xml.append("<Cell><Data ss:Type=\"String\">")
                        .append(escapeXml(value))
                        .append("</Data></Cell>");
            }
            xml.append("</Row>");
        }
        xml.append("</Table>");
        xml.append("</Worksheet>");
        xml.append("</Workbook>");
        try {
            return xml.toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unable to encode Excel export", e);
        }
    }

    public static String decimal(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String trimSheetName(String sheetName) {
        String safeName = sheetName == null || sheetName.trim().isEmpty() ? "Report" : sheetName.trim();
        return safeName.length() > 31 ? safeName.substring(0, 31) : safeName;
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
