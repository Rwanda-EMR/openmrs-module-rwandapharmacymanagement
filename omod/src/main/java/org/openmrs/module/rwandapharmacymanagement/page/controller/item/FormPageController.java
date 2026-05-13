package org.openmrs.module.rwandapharmacymanagement.page.controller.item;

import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;

@Controller
public class FormPageController {

    public void get(PageModel model) {
        model.addAttribute("pageMode", "create");
    }
}
