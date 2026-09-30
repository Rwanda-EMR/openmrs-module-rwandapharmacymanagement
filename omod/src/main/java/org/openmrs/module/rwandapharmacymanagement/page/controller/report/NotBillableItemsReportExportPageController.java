package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
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
public class NotBillableItemsReportExportPageController {

    public void get(@SpringBean("dispensingService") DispensingService dispensingService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "requestType", required = false) String requestTypeParam,
                    HttpServletResponse response) throws IOException {
        Date endDate = NotBillableItemsReportSupport.startOfDay(NotBillableItemsReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = NotBillableItemsReportSupport.startOfDay(NotBillableItemsReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        RequestType requestType = NotBillableItemsReportSupport.parseRequestType(requestTypeParam);
        List<PatientDispensingRequestLine> lines = dispensingService.getNotBillableRequestLines(startDate,
                NotBillableItemsReportSupport.nextDay(endDate), pharmacyLocationId, requestType);
        List<DispenseRecord> dispenseRecords = requestIds(lines).isEmpty()
                ? java.util.Collections.<DispenseRecord>emptyList()
                : dispensingService.getDispenseRecordsForRequests(requestIds(lines));
        List<NotBillableItemsReportSupport.Row> rows = NotBillableItemsReportSupport.buildRows(lines, dispenseRecords);
        NotBillableItemsReportSupport.Totals totals = NotBillableItemsReportSupport.totals(rows);
        List<List<String>> summaryRows = ReportExportSummarySupport.rows(
                "Lines", ReportExportSummarySupport.integer(totals.lineCount),
                "Prescriptions", ReportExportSummarySupport.integer(totals.prescriptionCount),
                "Consumables", ReportExportSummarySupport.integer(totals.consumableCount),
                "Dispensed", ReportExportSummarySupport.integer(totals.dispensedCount),
                "Pending", ReportExportSummarySupport.integer(totals.pendingCount)
        );
        List<String> headers = Arrays.asList("Request date", "Patient", "Identifier", "Request", "Line",
                "Type", "Item", "Item code", "Quantity", "Reason", "Requested by", "Requesting location",
                "Target pharmacy", "Bill", "Payment confirmed", "Line status", "Dispensed", "Quantity dispensed",
                "Dispense date", "Served by", "Served where");
        List<List<String>> body = new ArrayList<List<String>>();
        for (NotBillableItemsReportSupport.Row row : rows) {
            body.add(Arrays.asList(
                    string(row.requestDate),
                    string(row.patientName),
                    string(row.patientIdentifier),
                    row.requestId == null ? "" : String.valueOf(row.requestId),
                    row.requestLineId == null ? "" : String.valueOf(row.requestLineId),
                    string(row.requestType),
                    string(row.itemName),
                    string(row.itemCode),
                    quantity(row.quantity, row.unitOfMeasure),
                    string(row.reason),
                    string(row.requestedBy),
                    string(row.requestingLocation),
                    string(row.targetPharmacy),
                    row.billId == null ? "" : String.valueOf(row.billId),
                    Boolean.TRUE.equals(row.paymentConfirmed) ? "Yes" : "No",
                    string(row.lineStatus),
                    Boolean.TRUE.equals(row.dispensed) ? "Yes" : "No",
                    decimal(row.quantityDispensed),
                    string(row.dispenseDates),
                    string(row.dispensedBy),
                    string(row.dispensingPharmacies)
            ));
        }
        String title = "Not Billable Items Report " + NotBillableItemsReportSupport.formatDate(startDate) + " to "
                + NotBillableItemsReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Not Billable Items", summaryRows, headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"not-billable-items-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title, summaryRows, headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"not-billable-items-report.pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
            return;
        }
        response.getOutputStream().flush();
    }

    private List<Integer> requestIds(List<PatientDispensingRequestLine> lines) {
        List<Integer> ids = new ArrayList<Integer>();
        if (lines == null) {
            return ids;
        }
        for (PatientDispensingRequestLine line : lines) {
            if (line == null || line.getPatientDispensingRequest() == null
                    || line.getPatientDispensingRequest().getPatientDispensingRequestId() == null) {
                continue;
            }
            Integer requestId = line.getPatientDispensingRequest().getPatientDispensingRequestId();
            if (!ids.contains(requestId)) {
                ids.add(requestId);
            }
        }
        return ids;
    }

    private String quantity(BigDecimal value, String unitOfMeasure) {
        String amount = decimal(value);
        String unit = string(unitOfMeasure);
        return unit.isEmpty() ? amount : amount + " " + unit;
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : StockReportExcelRenderer.decimal(value);
    }

    private String string(String value) {
        return value == null ? "" : value;
    }
}
