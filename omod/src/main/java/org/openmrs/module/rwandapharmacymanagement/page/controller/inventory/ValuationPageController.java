package org.openmrs.module.rwandapharmacymanagement.page.controller.inventory;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.page.controller.support.PaginationSupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ValuationPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @RequestParam(value = "page", required = false) Integer page,
                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<InventoryBalance> balances = inventoryService.getInventoryForPharmacy(null);
        int currentPage = PaginationSupport.sanitizePage(page);
        int currentPageSize = PaginationSupport.sanitizePageSize(pageSize);
        currentPage = PaginationSupport.clampPage(currentPage, currentPageSize, balances.size());
        List<InventoryBalance> pagedBalances = PaginationSupport.slice(balances, currentPage, currentPageSize);
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal grandSellingTotal = BigDecimal.ZERO;
        for (InventoryBalance balance : balances) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("balance", balance);
            BigDecimal unitPrice = balance.getUnitPrice();
            BigDecimal totalValue = unitPrice == null || balance.getQuantityOnHand() == null
                    ? null : unitPrice.multiply(balance.getQuantityOnHand());
            BigDecimal sellingPrice = pricingService.calculateSellingPrice(unitPrice);
            BigDecimal totalSellingValue = sellingPrice == null || balance.getQuantityOnHand() == null
                    ? null : sellingPrice.multiply(balance.getQuantityOnHand());
            if (totalValue != null) {
                grandTotal = grandTotal.add(totalValue);
            }
            if (totalSellingValue != null) {
                grandSellingTotal = grandSellingTotal.add(totalSellingValue);
            }
            row.put("unitPrice", unitPrice);
            row.put("totalValue", totalValue);
            row.put("sellingPrice", sellingPrice);
            row.put("totalSellingValue", totalSellingValue);
            if (pagedBalances.contains(balance)) {
                rows.add(row);
            }
        }
        model.addAttribute("valuationRows", rows);
        PaginationSupport.addAttributes(model, "valuation", currentPage, currentPageSize, balances.size());
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("grandSellingTotal", grandSellingTotal);
        model.addAttribute("dispensingMarkupPercentage", pricingService.getDispensingMarkupPercentage());
    }
}
