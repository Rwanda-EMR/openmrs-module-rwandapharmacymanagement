package org.openmrs.module.rwandapharmacymanagement.web.controller;

import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
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
@RequestMapping("/module/rwandapharmacymanagement/items")
public class ItemSearchController {

    private final PharmacyService pharmacyService;

    public ItemSearchController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> search(@RequestParam(value = "q", required = false) String searchTerm,
                                            @RequestParam(value = "limit", required = false) Integer limit) {
        List<Item> items = pharmacyService.searchItems(searchTerm, limit);
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        for (Item item : items) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", item.getItemId());
            row.put("name", item.getName());
            row.put("code", item.getCode());
            row.put("display", buildDisplay(item));
            response.add(row);
        }
        return response;
    }

    private String buildDisplay(Item item) {
        return (item.getName() == null ? "" : item.getName()) + " (" + (item.getCode() == null ? "" : item.getCode()) + ")";
    }
}
