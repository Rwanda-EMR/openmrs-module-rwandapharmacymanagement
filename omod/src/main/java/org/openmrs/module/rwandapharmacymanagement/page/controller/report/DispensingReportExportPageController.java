package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportExcelRenderer;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Controller
public class DispensingReportExportPageController {

    public void get(@SpringBean("dispensingService") DispensingService dispensingService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpServletResponse response) throws IOException {
        Date endDate = DispensingReportSupport.startOfDay(parseDate(endDateParam, new Date()));
        Date startDate = DispensingReportSupport.startOfDay(parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        List<DispenseRecord> records = dispensingService.getDispenseRecords(startDate,
                DispensingReportSupport.nextDay(endDate), pharmacyLocationId);
        List<DispensingReportSupport.Row> rows = DispensingReportSupport.buildRows(records);
        List<String> headers = Arrays.asList("Dispense date", "Patient", "Identifier", "Pharmacy", "Item dispensed",
                "Item code", "Batch", "Expiry", "Quantity", "Unit price", "Total", "Prescription / request",
                "Bill", "Status", "Dispensed by");
        List<List<String>> body = new ArrayList<List<String>>();
        for (DispensingReportSupport.Row row : rows) {
            body.add(Arrays.asList(
                    string(row.dispenseDate),
                    string(row.patientName),
                    string(row.patientIdentifier),
                    string(row.pharmacyName),
                    string(row.itemName),
                    string(row.itemCode),
                    string(row.batchNumber),
                    string(row.expiryDate),
                    decimal(row.quantityDispensed),
                    decimal(row.sellingPrice),
                    decimal(row.lineTotal),
                    prescriptionInfo(row),
                    row.patientBillId == null ? "" : String.valueOf(row.patientBillId),
                    string(row.requestStatus),
                    string(row.dispensedBy)
            ));
        }
        String title = "Dispensing Report " + DispensingReportSupport.formatDate(startDate) + " to "
                + DispensingReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Dispensing Report", headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"dispensing-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title, headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"dispensing-report.pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
            return;
        }
        response.getOutputStream().flush();
    }

    private Date parseDate(String value, Date fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(value.trim());
        }
        catch (ParseException ex) {
            return fallback;
        }
    }

    private String prescriptionInfo(DispensingReportSupport.Row row) {
        if (row == null) {
            return "";
        }
        if (Boolean.TRUE.equals(row.dispensedWithoutOrder)) {
            return "Dispensed without order";
        }
        return "Request " + (row.requestId == null ? "" : row.requestId)
                + (row.prescriptionReference == null || row.prescriptionReference.trim().isEmpty()
                ? "" : " - " + row.prescriptionReference);
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : StockReportExcelRenderer.decimal(value);
    }

    private String string(String value) {
        return value == null ? "" : value;
    }
}
