package org.openmrs.module.rwandapharmacymanagement.page.controller.purchaseorder;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.reporting.PurchaseOrderPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class DownloadPdfPageController {

    public void get(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam("purchaseOrderId") Integer purchaseOrderId,
                    HttpServletResponse response) throws IOException {
        PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(purchaseOrderId);
        if (purchaseOrder == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] pdf = PurchaseOrderPdfRenderer.render(purchaseOrder,
                Context.getAdministrationService().getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, ""));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"purchase-order-" + purchaseOrderId + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }
}
