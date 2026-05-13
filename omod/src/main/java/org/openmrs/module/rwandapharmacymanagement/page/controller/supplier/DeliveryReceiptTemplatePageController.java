package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.reporting.DeliveryReceiptTemplateRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class DeliveryReceiptTemplatePageController {

    public void get(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "purchaseOrderId", required = false) Integer purchaseOrderId,
                    HttpServletResponse response) throws IOException {
        PurchaseOrder purchaseOrder = purchaseOrderId == null ? null : pharmacyService.getPurchaseOrder(purchaseOrderId);
        byte[] bytes = purchaseOrder == null
                ? DeliveryReceiptTemplateRenderer.renderBlankTemplate()
                : DeliveryReceiptTemplateRenderer.renderPurchaseOrderTemplate(purchaseOrder);
        String fileName = purchaseOrder == null
                ? "delivery-receipt-template.csv"
                : "purchase-order-" + purchaseOrder.getPurchaseOrderId() + "-delivery-receipt.csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }
}
