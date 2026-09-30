package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
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
public class PatientRequestReportExportPageController {

    public void get(@SpringBean("dispensingService") DispensingService dispensingService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "requestType", required = false) String requestTypeParam,
                    HttpServletResponse response) throws IOException {
        Date endDate = PatientRequestReportSupport.startOfDay(PatientRequestReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = PatientRequestReportSupport.startOfDay(PatientRequestReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        RequestType requestType = PatientRequestReportSupport.parseRequestType(requestTypeParam);
        List<PatientDispensingRequest> requests = dispensingService.getPatientDispensingRequests(startDate,
                PatientRequestReportSupport.nextDay(endDate), pharmacyLocationId, requestType);
        List<PatientRequestReportSupport.Row> rows = PatientRequestReportSupport.buildRows(requests);
        PatientRequestReportSupport.Totals totals = PatientRequestReportSupport.totals(rows);
        List<List<String>> summaryRows = ReportExportSummarySupport.rows(
                "Requests", ReportExportSummarySupport.integer(totals.requestCount),
                "Lines", ReportExportSummarySupport.integer(totals.lineCount),
                "Prescriptions", ReportExportSummarySupport.integer(totals.prescriptionLineCount),
                "Consumables", ReportExportSummarySupport.integer(totals.consumableLineCount),
                "Total quantity", ReportExportSummarySupport.decimal(totals.requestedQuantity)
        );
        List<String> headers = Arrays.asList("Request date", "Request ID", "Line ID", "Patient", "Identifier",
                "Type", "Request status", "Line status", "Requested by", "Requesting location", "Target pharmacy",
                "Routed pharmacy", "Item", "Item code", "Requested quantity", "Unit", "Billable", "Exclusion reason",
                "Bill", "Payment confirmed", "Payment exception", "Reference");
        List<List<String>> body = new ArrayList<List<String>>();
        for (PatientRequestReportSupport.Row row : rows) {
            body.add(Arrays.asList(
                    string(row.requestDate),
                    row.requestId == null ? "" : String.valueOf(row.requestId),
                    row.requestLineId == null ? "" : String.valueOf(row.requestLineId),
                    string(row.patientName),
                    string(row.patientIdentifier),
                    string(row.requestType),
                    string(row.requestStatus),
                    string(row.lineStatus),
                    string(row.requestedBy),
                    string(row.requestingLocation),
                    string(row.targetPharmacy),
                    string(row.routedPharmacy),
                    string(row.itemName),
                    string(row.itemCode),
                    decimal(row.requestedQuantity),
                    string(row.unitOfMeasure),
                    Boolean.FALSE.equals(row.billable) ? "No" : "Yes",
                    string(row.billingExclusionReason),
                    row.patientBillId == null ? "" : String.valueOf(row.patientBillId),
                    Boolean.TRUE.equals(row.paymentConfirmed) ? "Yes" : "No",
                    Boolean.TRUE.equals(row.paymentExceptionApproved) ? "Yes" : "No",
                    string(row.reference)
            ));
        }
        String title = "Patient Requests to Pharmacy " + PatientRequestReportSupport.formatDate(startDate) + " to "
                + PatientRequestReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Patient Requests", summaryRows, headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"patient-request-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title, summaryRows, headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"patient-request-report.pdf\"");
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
