package org.openmrs.module.rwandapharmacymanagement.page.controller.integration;

import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;

@Controller
public class LogsPageController {

    public void get(PageModel model, @SpringBean("integrationLogService") IntegrationLogService integrationLogService) {
        model.addAttribute("integrationLogs", integrationLogService.getRecentLogs());
    }
}
