package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;

@Controller
public class PriceHistoryPageController {

    public void get(PageModel model, @SpringBean("pharmacyService") PharmacyService pharmacyService) {
        model.addAttribute("supplierPriceHistory", pharmacyService.getSupplierPriceHistory());
    }
}
