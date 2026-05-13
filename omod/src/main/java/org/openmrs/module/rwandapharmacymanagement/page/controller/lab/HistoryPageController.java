package org.openmrs.module.rwandapharmacymanagement.page.controller.lab;

import org.openmrs.module.rwandapharmacymanagement.api.LabSupplyService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@Controller("labSupplyHistoryPageController")
public class HistoryPageController {

    public void get(PageModel model,
                    @SpringBean("labSupplyService") LabSupplyService labSupplyService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        model.addAttribute("requests", activePharmacy == null
                ? Collections.emptyList()
                : labSupplyService.getApprovedRequestsByPharmacy(activePharmacy));
    }
}
