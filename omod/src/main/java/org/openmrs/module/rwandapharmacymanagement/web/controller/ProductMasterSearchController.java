package org.openmrs.module.rwandapharmacymanagement.web.controller;

import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/module/rwandapharmacymanagement/product-master")
public class ProductMasterSearchController {

    private final PharmacyService pharmacyService;

    public ProductMasterSearchController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> search(@RequestParam(value = "q", required = false) String searchTerm,
                                            @RequestParam(value = "limit", required = false) Integer limit) {
        List<ProductMasterItem> items = pharmacyService.searchProductMasterItems(searchTerm, limit);
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        for (ProductMasterItem item : items) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", item.getProductMasterItemId());
            row.put("itemName", item.getItemName());
            row.put("npcCode", item.getNpcCode());
            row.put("program", item.getProgram());
            row.put("display", buildDisplay(item));
            response.add(row);
        }
        return response;
    }

    private String buildDisplay(ProductMasterItem item) {
        StringBuilder display = new StringBuilder();
        display.append(item.getItemName() == null ? "" : item.getItemName());
        display.append(" (").append(item.getNpcCode() == null ? "" : item.getNpcCode()).append(")");
        if (item.getProgram() != null && !item.getProgram().trim().isEmpty()) {
            display.append(" - ").append(item.getProgram().trim());
        }
        return display.toString();
    }
}
