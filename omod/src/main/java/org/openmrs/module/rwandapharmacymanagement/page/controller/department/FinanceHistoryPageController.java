package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;

@Controller("departmentBulkFinanceHistoryPageController")
public class FinanceHistoryPageController {

    public void get(PageModel model,
                    @SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService) {
        model.addAttribute("requests", departmentBulkRequestService.getFinanceHistory(0, 200));
    }
}
