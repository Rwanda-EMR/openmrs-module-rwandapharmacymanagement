package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class ItemMovementExportPageController {

    public void get(@SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam("format") String format,
                    @RequestParam("itemId") Integer itemId,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    HttpServletResponse response) throws IOException {
        Date endDate = ItemMovementReportSupport.startOfDay(parseDate(endDateParam, new Date()));
        Date startDate = ItemMovementReportSupport.startOfDay(parseDate(startDateParam, defaultStartDate(endDate)));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        Item selectedItem = pharmacyService.getItem(itemId);
        if (selectedItem == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Item not found");
            return;
        }
        List<ItemMovementReportSupport.Row> rows = ItemMovementReportSupport.buildRows(inventoryService, selectedItem,
                startDate, ItemMovementReportSupport.nextDay(endDate), pharmacyLocationId);
        List<String> headers = Arrays.asList("Date of movement", "Receiving pharmacy / user", "Issuing pharmacy",
                "Batch number", "Expiry date", "Quantity received", "Quantity issued", "Balance in stock",
                "Months of stock", "Movement type", "Reference", "Remarks");
        List<List<String>> body = new ArrayList<List<String>>();
        for (ItemMovementReportSupport.Row row : rows) {
            body.add(Arrays.asList(
                    row.getMovementDate(),
                    row.getReceivingPharmacyOrUser(),
                    row.getIssuingPharmacy(),
                    row.getBatchNumber(),
                    row.getExpiryDate(),
                    decimal(row.getQuantityReceived()),
                    decimal(row.getQuantityIssued()),
                    decimal(row.getBalanceInStock()),
                    row.getMonthsOfStock() == null ? "N/A" : decimal(row.getMonthsOfStock()),
                    row.getTransactionType(),
                    row.getReferenceNumber(),
                    row.getRemarks()
            ));
        }
        String title = "Item Movement - " + selectedItem.getName() + " "
                + ItemMovementReportSupport.formatDate(startDate) + " to " + ItemMovementReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Item Movement", headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"item-movement-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title, headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"item-movement-report.pdf\"");
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

    private Date defaultStartDate(Date endDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate == null ? new Date() : endDate);
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }

    private String decimal(BigDecimal value) {
        return value == null ? "0" : StockReportExcelRenderer.decimal(value);
    }
}
