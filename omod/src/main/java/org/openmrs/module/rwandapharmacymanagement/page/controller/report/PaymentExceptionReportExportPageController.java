package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportExcelRenderer;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Controller
public class PaymentExceptionReportExportPageController {

    public void get(@SpringBean("dispensingService") DispensingService dispensingService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "requestType", required = false) String requestTypeParam,
                    HttpServletResponse response) throws IOException {
        Date endDate = PaymentExceptionReportSupport.startOfDay(PaymentExceptionReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = PaymentExceptionReportSupport.startOfDay(PaymentExceptionReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        RequestType requestType = PaymentExceptionReportSupport.parseRequestType(requestTypeParam);
        List<PatientDispensingRequest> requests = dispensingService.getPaymentExceptionApprovedRequests(startDate,
                PaymentExceptionReportSupport.nextDay(endDate), pharmacyLocationId, requestType);
        List<Integer> requestIds = requestIds(requests);
        List<DispenseRecord> dispenseRecords = requestIds.isEmpty()
                ? java.util.Collections.<DispenseRecord>emptyList()
                : dispensingService.getDispenseRecordsForRequests(requestIds);
        List<PaymentExceptionReportSupport.Row> rows = PaymentExceptionReportSupport.buildRows(requests, dispenseRecords);
        List<String> headers = Arrays.asList("Approval date", "Request date", "Request ID", "Patient", "Identifier",
                "Request type", "Request status", "Bill", "Payment confirmed", "Exception type", "Reason", "Note",
                "Approved by", "Requested by", "Requesting location", "Target pharmacy", "Items", "Item count", "Dispensed",
                "Dispense date(s)", "Dispensing pharmacy", "Dispensed by", "Reference");
        List<List<String>> body = new ArrayList<List<String>>();
        for (PaymentExceptionReportSupport.Row row : rows) {
            body.add(Arrays.asList(
                    string(row.approvedDate),
                    string(row.requestDate),
                    row.requestId == null ? "" : String.valueOf(row.requestId),
                    string(row.patientName),
                    string(row.patientIdentifier),
                    string(row.requestType),
                    string(row.requestStatus),
                    row.billId == null ? "" : String.valueOf(row.billId),
                    Boolean.TRUE.equals(row.paymentConfirmed) ? "Yes" : "No",
                    string(row.exceptionType),
                    string(row.exceptionReason),
                    string(row.exceptionNote),
                    string(row.approvedBy),
                    string(row.requestedBy),
                    string(row.requestingLocation),
                    string(row.targetPharmacy),
                    string(row.items),
                    String.valueOf(row.itemCount),
                    Boolean.TRUE.equals(row.dispensed) ? "Yes" : "No",
                    string(row.dispenseDates),
                    string(row.dispensingPharmacies),
                    string(row.dispensedBy),
                    string(row.requestReference)
            ));
        }
        String title = "Payment Exception Report " + PaymentExceptionReportSupport.formatDate(startDate) + " to "
                + PaymentExceptionReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Payment Exception Report", headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"payment-exception-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title, headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"payment-exception-report.pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
            return;
        }
        response.getOutputStream().flush();
    }

    private List<Integer> requestIds(List<PatientDispensingRequest> requests) {
        List<Integer> ids = new ArrayList<Integer>();
        if (requests == null) {
            return ids;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                ids.add(request.getPatientDispensingRequestId());
            }
        }
        return ids;
    }

    private String string(String value) {
        return value == null ? "" : value;
    }
}
