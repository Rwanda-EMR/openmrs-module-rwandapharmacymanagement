package org.openmrs.module.rwandapharmacymanagement.page.controller.pharmacyorder;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.reporting.PharmacyItemRequestPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class HistoryPdfPageController {

    public void get(@SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                    @RequestParam("requestId") Integer requestId,
                    HttpServletResponse response) throws IOException {
        PharmacyItemRequest request = pharmacyItemRequestService.getRequest(requestId);
        if (request == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Pharmacy item request not found");
            return;
        }
        if (!canDownload(request)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        }
        byte[] pdf = PharmacyItemRequestPdfRenderer.render(request,
                Context.getAdministrationService().getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, ""));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"pharmacy-item-request-" + requestId + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private boolean canDownload(PharmacyItemRequest request) {
        if (Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)) {
            return true;
        }
        return request != null && request.getCreator() != null && Context.getAuthenticatedUser() != null
                && request.getCreator().getUserId() != null
                && request.getCreator().getUserId().equals(Context.getAuthenticatedUser().getUserId());
    }
}
