package org.openmrs.module.rwandapharmacymanagement.page.controller.inventory;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.page.controller.support.PaginationSupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ReceiptHistoryPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "page", required = false) Integer page,
                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<?> allReceiptLines = inventoryService.getRecentDeliveryReceiptLines();
        int currentPage = PaginationSupport.sanitizePage(page);
        int currentPageSize = PaginationSupport.sanitizePageSize(pageSize);
        currentPage = PaginationSupport.clampPage(currentPage, currentPageSize, allReceiptLines.size());
        model.addAttribute("receiptLines", PaginationSupport.slice(allReceiptLines, currentPage, currentPageSize));
        PaginationSupport.addAttributes(model, "receipt", currentPage, currentPageSize, allReceiptLines.size());
    }
}
