package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
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
public class InventoryMovementExportPageController {

    public void get(@SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "materialType", required = false) String materialType,
                    @RequestParam(value = "program", required = false) String program,
                    @RequestParam(value = "mode", required = false) String mode,
                    HttpServletResponse response) throws IOException {
        Date endDate = InventoryMovementReportSupport.startOfDay(parseDate(endDateParam, new Date()));
        Date startDate = InventoryMovementReportSupport.startOfDay(parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        String reportMode = InventoryMovementReportSupport.MODE_DETAIL.equalsIgnoreCase(mode)
                ? InventoryMovementReportSupport.MODE_DETAIL : InventoryMovementReportSupport.MODE_SUMMARY;
        List<InventoryMovementReportSupport.Row> rows = InventoryMovementReportSupport.buildRows(inventoryService, pharmacyService,
                startDate, InventoryMovementReportSupport.nextDay(endDate), pharmacyLocationId, materialType, program, reportMode);
        List<String> headers = Arrays.asList("Item", "Code", "Material Type", "Program", "Pharmacy", "Batch", "Expiry",
                "Opening", "Received", "Transfer In", "Adjustment In", "Dispensed", "Transfer Out", "Adjustment Out",
                "Closing", "Stock At Hand", "Unit Cost", "Closing Value", "Stock At Hand Value");
        List<List<String>> body = new ArrayList<List<String>>();
        for (InventoryMovementReportSupport.Row row : rows) {
            body.add(Arrays.asList(
                    row.getItemName(),
                    row.getItemCode(),
                    row.getMaterialType(),
                    row.getProgram(),
                    row.getPharmacyName(),
                    row.getBatchNumber(),
                    InventoryMovementReportSupport.formatDate(row.getExpiryDate()),
                    decimal(row.getOpeningQty()),
                    decimal(row.getReceivedQty()),
                    decimal(row.getTransferInQty()),
                    decimal(row.getAdjustmentInQty()),
                    decimal(row.getDispensedQty()),
                    decimal(row.getTransferOutQty()),
                    decimal(row.getAdjustmentOutQty()),
                    decimal(row.getClosingQty()),
                    decimal(row.getStockAtHand()),
                    decimal(row.getUnitCost()),
                    decimal(row.getClosingValue()),
                    decimal(row.getStockAtHandValue())
            ));
        }
        String title = "Inventory Movement " + InventoryMovementReportSupport.formatDate(startDate) + " to "
                + InventoryMovementReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Inventory Movement", headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"inventory-movement-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title, headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"inventory-movement-report.pdf\"");
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

    private String decimal(BigDecimal value) {
        return value == null ? "0" : StockReportExcelRenderer.decimal(value);
    }
}
