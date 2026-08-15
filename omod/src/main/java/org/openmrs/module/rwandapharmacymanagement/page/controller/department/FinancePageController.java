package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller("departmentBulkFinancePageController")
public class FinancePageController {

    public void get(PageModel model,
                    @SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService) {
        model.addAttribute("submittedRequests", departmentBulkRequestService.getRequestsByStatus(
                DepartmentBulkRequestService.STATUS_SUBMITTED, 0, 200));
        model.addAttribute("correctionRequests", departmentBulkRequestService.getRequestsByStatus(
                DepartmentBulkRequestService.STATUS_CORRECTION_REQUESTED, 0, 200));
    }

    public String post(@SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam("action") String action,
                       @RequestParam(value = "removeLineId", required = false) Integer removeLineId,
                       @RequestParam(value = "lineId", required = false) Integer[] lineIds,
                       @RequestParam(value = "approvedQuantity", required = false) BigDecimal[] approvedQuantities,
                       @RequestParam(value = "note", required = false) String note,
                       @RequestParam(value = "lineVoidReason", required = false) String lineVoidReason,
                       UiUtils ui) {
        if ("removeLine".equalsIgnoreCase(action)) {
            departmentBulkRequestService.removeRequestLine(requestId, removeLineId,
                    lineVoidReason == null || lineVoidReason.trim().isEmpty() ? note : lineVoidReason);
        } else if ("reject".equalsIgnoreCase(action)) {
            departmentBulkRequestService.rejectRequest(requestId, note);
        } else if ("correction".equalsIgnoreCase(action)) {
            departmentBulkRequestService.requestCorrection(requestId, note);
        } else {
            departmentBulkRequestService.financeApproveRequest(requestId, lineIds, approvedQuantities);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "department/finance");
    }
}
