package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportExcelRenderer;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Controller
public class WriteOffDisposalReportExportPageController {

    public void get(@SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "itemId", required = false) Integer itemId,
                    @RequestParam(value = "status", required = false) String statusParam,
                    HttpServletResponse response) throws IOException {
        Date endDate = WriteOffDisposalReportSupport.startOfDay(
                WriteOffDisposalReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = WriteOffDisposalReportSupport.startOfDay(
                WriteOffDisposalReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        String status = WriteOffDisposalReportSupport.normalizedStatus(statusParam);
        List<WriteOffDisposalReportSupport.Row> rows = WriteOffDisposalReportSupport.buildRows(inventoryService,
                startDate, WriteOffDisposalReportSupport.nextDay(endDate), pharmacyLocationId, itemId, status);
        WriteOffDisposalReportSupport.Totals totals = WriteOffDisposalReportSupport.totals(rows);
        List<List<String>> summaryRows = ReportExportSummarySupport.rows(
                "Write-offs", ReportExportSummarySupport.integer(totals.writeOffCount),
                "Disposed", ReportExportSummarySupport.integer(totals.disposedCount),
                "Pending disposal", ReportExportSummarySupport.integer(totals.pendingDisposalCount),
                "Total quantity", ReportExportSummarySupport.decimal(totals.writeOffQuantity),
                "Total value", ReportExportSummarySupport.decimal(totals.writeOffValue)
        );
        List<String> headers = Arrays.asList("Write-off ID", "Write-off date", "Written off by", "Pharmacy",
                "Item", "Item code", "Batch", "Expiry", "Quantity", "Unit price", "Value", "Reason",
                "Disposal status", "Disposal ID", "Disposal date", "Disposal method / notes", "Disposed by");
        List<List<String>> body = new ArrayList<List<String>>();
        for (WriteOffDisposalReportSupport.Row row : rows) {
            body.add(Arrays.asList(
                    row.writeOffTransactionId == null ? "" : String.valueOf(row.writeOffTransactionId),
                    string(row.writeOffDate),
                    string(row.writtenOffBy),
                    string(row.pharmacyName),
                    string(row.itemName),
                    string(row.itemCode),
                    string(row.batchNumber),
                    string(row.expiryDate),
                    decimal(row.writeOffQuantity),
                    decimal(row.unitPrice),
                    decimal(row.writeOffValue),
                    string(row.writeOffReason),
                    row.disposed ? "Disposed" : "Pending disposal",
                    row.disposalTransactionId == null ? "" : String.valueOf(row.disposalTransactionId),
                    string(row.disposalDate),
                    string(row.disposalMethod),
                    string(row.disposedBy)
            ));
        }
        String title = "Write-off and Disposal Report " + WriteOffDisposalReportSupport.formatDate(startDate) + " to "
                + WriteOffDisposalReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Write-off Disposal", summaryRows, headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"write-off-disposal-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title, summaryRows, headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"write-off-disposal-report.pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
            return;
        }
        response.getOutputStream().flush();
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : StockReportExcelRenderer.decimal(value);
    }

    private String string(String value) {
        return value == null ? "" : value;
    }
}
