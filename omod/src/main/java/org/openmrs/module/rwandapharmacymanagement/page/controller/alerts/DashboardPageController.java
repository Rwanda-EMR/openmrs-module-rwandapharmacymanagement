package org.openmrs.module.rwandapharmacymanagement.page.controller.alerts;

import org.openmrs.module.rwandapharmacymanagement.api.AlertService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;

@Controller
public class DashboardPageController {

    public void get(PageModel model, @SpringBean("rwandaPharmacyAlertService") AlertService alertService) {
        model.addAttribute("lowStockAlerts", alertService.getLowStockAlerts());
        model.addAttribute("expiryAlerts", alertService.getExpiryAlerts());
    }
}
