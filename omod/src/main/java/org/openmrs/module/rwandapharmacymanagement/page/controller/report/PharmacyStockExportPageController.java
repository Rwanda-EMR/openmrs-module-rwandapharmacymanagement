package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
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
public class PharmacyStockExportPageController {

    public void get(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "materialType", required = false) String materialType,
                    @RequestParam(value = "program", required = false) String program,
                    HttpServletResponse response) throws IOException {
        List<PharmacyLocation> pharmacies = StockReportSupport.getActivePharmacies(pharmacyService);
        PharmacyLocation selectedPharmacy = pharmacyLocationId == null ? (pharmacies.isEmpty() ? null : pharmacies.get(0))
                : pharmacyService.getPharmacyLocation(pharmacyLocationId);
        if (selectedPharmacy == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Pharmacy not found");
            return;
        }
        List<Map<String, Object>> rows = StockReportSupport.buildPharmacyRows(selectedPharmacy, inventoryService, pricingService,
                pharmacyService, materialType, program);
        List<String> headers = Arrays.asList("Item", "Material Type", "Program", "Batch", "Expiry", "On Hand", "Cost Price", "Cost Value", "Selling Price", "Selling Value");
        List<List<String>> body = new ArrayList<List<String>>();
        for (Map<String, Object> row : rows) {
            InventoryBalance balance = (InventoryBalance) row.get("balance");
            body.add(Arrays.asList(
                    balance == null || balance.getItem() == null ? "" : safe(balance.getItem().getName()),
                    safe((String) row.get("materialType")),
                    safe((String) row.get("program")),
                    balance == null ? "" : safe(balance.getBatchNumber()),
                    balance == null || balance.getExpiryDate() == null ? "" : balance.getExpiryDate().toString(),
                    balance == null || balance.getQuantityOnHand() == null ? "0" : StockReportExcelRenderer.decimal(balance.getQuantityOnHand()),
                    decimal(row.get("unitPrice")),
                    decimal(row.get("totalValue")),
                    decimal(row.get("sellingPrice")),
                    decimal(row.get("totalSellingValue"))
            ));
        }
        String fileNameBase = "pharmacy-stock-report-" + selectedPharmacy.getPharmacyLocationId();
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("Pharmacy Stock", headers, body);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileNameBase + ".xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render("Individual Pharmacy Report - " + selectedPharmacy.getName(), headers, body);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileNameBase + ".pdf\"");
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
