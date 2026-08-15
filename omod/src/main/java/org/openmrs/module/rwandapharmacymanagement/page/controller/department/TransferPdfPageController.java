package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.reporting.DepartmentTransferPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller("departmentTransferPdfPageController")
public class TransferPdfPageController {

    public void get(@SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                    @RequestParam("requestId") Integer requestId,
                    HttpServletResponse response) throws IOException {
        DepartmentBulkRequest request = departmentBulkRequestService.getRequest(requestId);
        if (request == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Department transfer not found");
            return;
        }
        if (!DepartmentBulkRequestService.STATUS_DISPENSED.equalsIgnoreCase(request.getRequestStatus())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "PDF is available only after transfer is completed");
            return;
        }
        if (!canDownload(request)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        }
        byte[] pdf = DepartmentTransferPdfRenderer.render(request,
                Context.getAdministrationService().getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, ""));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"department-transfer-" + requestId + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }

    private boolean canDownload(DepartmentBulkRequest request) {
        if (Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)
                || Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PAYMENT_EXCEPTION)) {
            return true;
        }
        User user = Context.getAuthenticatedUser();
        Integer userId = user == null ? null : user.getUserId();
        return userId != null && (sameUser(userId, request.getCreator()) || sameUser(userId, request.getFinanceApprovedBy())
                || sameUser(userId, request.getDispensedBy()));
    }

    private boolean sameUser(Integer userId, User user) {
        return user != null && user.getUserId() != null && user.getUserId().equals(userId);
    }
}
