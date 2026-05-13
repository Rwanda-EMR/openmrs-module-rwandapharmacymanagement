package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.reporting.DeliveryReceiptPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ReceiptPdfPageController {

    public void get(@RequestParam("deliveryReceiptId") Integer deliveryReceiptId,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    HttpServletResponse response) throws IOException {
        DeliveryReceipt deliveryReceipt = inventoryService.getDeliveryReceipt(deliveryReceiptId);
        if (deliveryReceipt == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] pdf = DeliveryReceiptPdfRenderer.render(deliveryReceipt);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"delivery-receipt-" + deliveryReceiptId + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }
}
