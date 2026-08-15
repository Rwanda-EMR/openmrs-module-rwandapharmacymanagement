package org.openmrs.module.rwandapharmacymanagement.page.controller.transfer;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.reporting.InterPharmacyTransferPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller("pharmacyTransferPdfPageController")
public class PdfPageController {

    public void get(@SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam("transferId") Integer transferId,
                    HttpServletResponse response) throws IOException {
        InterPharmacyTransfer transfer = inventoryService.getTransfer(transferId);
        if (transfer == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Transfer not found");
            return;
        }
        if (!"SERVED".equalsIgnoreCase(transfer.getTransferStatus())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "PDF is available only after transfer is completed");
            return;
        }
        if (!canDownload(transfer)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        }
        byte[] pdf = InterPharmacyTransferPdfRenderer.render(transfer,
                Context.getAdministrationService().getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, ""));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"pharmacy-transfer-" + transferId + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }

    private boolean canDownload(InterPharmacyTransfer transfer) {
        if (Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)
                || Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER)) {
            return true;
        }
        User user = Context.getAuthenticatedUser();
        Integer userId = user == null ? null : user.getUserId();
        return userId != null && (sameUser(userId, transfer.getCreator()) || sameUser(userId, transfer.getApprovedBy())
                || sameUser(userId, transfer.getServedBy()));
    }

    private boolean sameUser(Integer userId, User user) {
        return user != null && user.getUserId() != null && user.getUserId().equals(userId);
    }
}
