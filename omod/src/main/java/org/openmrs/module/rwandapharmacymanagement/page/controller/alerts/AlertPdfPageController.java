package org.openmrs.module.rwandapharmacymanagement.page.controller.alerts;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.AlertService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.AlertFilter;
import org.openmrs.module.rwandapharmacymanagement.reporting.AlertReportPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class AlertPdfPageController {

    public void get(@SpringBean("rwandaPharmacyAlertService") AlertService alertService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "itemCategoryId", required = false) Integer itemCategoryId,
                    @RequestParam(value = "q", required = false) String searchTerm,
                    @RequestParam(value = "expiryStatus", required = false) String expiryStatus,
                    @RequestParam(value = "stockStatus", required = false) String stockStatus,
                    @RequestParam(value = "expiryAlertDays", required = false) Integer expiryAlertDays,
                    @RequestParam(value = "daysOfStockAlertDays", required = false) Integer daysOfStockAlertDays,
                    @RequestParam(value = "consumptionLookbackDays", required = false) Integer consumptionLookbackDays,
                    HttpServletResponse response) throws IOException {
        AlertFilter filter = DashboardPageController.buildFilter(pharmacyLocationId, itemCategoryId, searchTerm,
                expiryStatus, stockStatus, expiryAlertDays, daysOfStockAlertDays, consumptionLookbackDays);
        byte[] pdf = AlertReportPdfRenderer.render(filter,
                DashboardPageController.locationName(pharmacyService, filter.getPharmacyLocationId()),
                DashboardPageController.categoryName(pharmacyService.getAllItemCategories(), filter.getItemCategoryId()),
                alertService.getExpirationAlertRows(filter), alertService.getStockoutAlertRows(filter),
                Context.getAuthenticatedUser());
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"pharmacy-alerts-"
                + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }
}
