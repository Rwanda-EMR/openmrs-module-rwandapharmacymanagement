package org.openmrs.module.rwandapharmacymanagement.page.controller.dispense;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;

@Controller
public class HistoryPageController {

    public void get(PageModel model, @SpringBean("dispensingService") DispensingService dispensingService) {
        model.addAttribute("dispenses", dispensingService.getRecentDispenses());
    }
}
