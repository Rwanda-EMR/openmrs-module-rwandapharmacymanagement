package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemVisibilityPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "q", required = false) String searchTerm,
                    @RequestParam(value = "visibility", required = false) String visibilityFilter,
                    @RequestParam(value = "updated", required = false) Boolean updated) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK);
        String safeVisibilityFilter = normalizeVisibilityFilter(visibilityFilter);
        List<Item> items = pharmacyService.searchItemsForVisibilityControl(searchTerm, safeVisibilityFilter, 300);
        model.addAttribute("items", items);
        model.addAttribute("availableQuantities", availableQuantitiesByItem(inventoryService));
        model.addAttribute("searchTerm", searchTerm == null ? "" : searchTerm.trim());
        model.addAttribute("visibilityFilter", safeVisibilityFilter);
        model.addAttribute("updated", Boolean.TRUE.equals(updated));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("itemId") Integer itemId,
                       @RequestParam("visibleToRequesters") Boolean visibleToRequesters,
                       @RequestParam(value = "q", required = false) String searchTerm,
                       @RequestParam(value = "visibility", required = false) String visibilityFilter,
                       UiUtils ui) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK);
        pharmacyService.setItemVisibleToRequesters(itemId, visibleToRequesters);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            params.put("q", searchTerm.trim());
        }
        params.put("visibility", normalizeVisibilityFilter(visibilityFilter));
        params.put("updated", Boolean.TRUE);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/itemVisibility", params);
    }

    private String normalizeVisibilityFilter(String visibilityFilter) {
        if ("hidden".equalsIgnoreCase(visibilityFilter)) {
            return "hidden";
        }
        if ("visible".equalsIgnoreCase(visibilityFilter)) {
            return "visible";
        }
        return "all";
    }

    private Map<Integer, BigDecimal> availableQuantitiesByItem(InventoryService inventoryService) {
        Map<Integer, BigDecimal> quantities = new LinkedHashMap<Integer, BigDecimal>();
        List<InventoryBalance> balances = inventoryService.getAvailableInventoryForPharmacy(null);
        if (balances == null) {
            return quantities;
        }
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getQuantityOnHand() == null) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            BigDecimal current = quantities.get(itemId);
            quantities.put(itemId, current == null ? balance.getQuantityOnHand() : current.add(balance.getQuantityOnHand()));
        }
        return quantities;
    }
}
