package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
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
import java.util.List;
import java.util.Map;

@Controller
public class OverallStockExportPageController {

    public void get(@SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "materialType", required = false) String materialType,
                    @RequestParam(value = "program", required = false) String program,
                    HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows = StockReportSupport.buildOverallRows(inventoryService, pricingService,
                pharmacyService, materialType, program);
        List<String> headers = Arrays.asList("Item", "Code", "Material Type", "Program", "Total Quantity", "Pharmacies Holding Stock", "Total Cost Value", "Total Selling Value");
        List<List<String>> body = new ArrayList<List<String>>();
        for (Map<String, Object> row : rows) {
            body.add(Arrays.asList(
                    string(row.get("itemName")),
                    string(row.get("itemCode")),
                    string(row.get("materialType")),
                    string(row.get("program")),
                    decimal(row.get("quantity")),
                    String.valueOf(row.get("pharmacies") == null ? 0 : row.get("pharmacies")),
                    decimal(row.get("totalValue")),
                    decimal(row.get("totalSellingValue"))
            ));
        }
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Overall Stock", headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"overall-hospital-stock-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render("Overall Hospital Stock Report", headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"overall-hospital-stock-report.pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
            return;
        }
        response.getOutputStream().flush();
    }

    private String decimal(Object value) {
        return value instanceof BigDecimal ? StockReportExcelRenderer.decimal((BigDecimal) value) : "0";
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
