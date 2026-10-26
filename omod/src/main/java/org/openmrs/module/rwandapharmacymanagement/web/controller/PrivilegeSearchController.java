package org.openmrs.module.rwandapharmacymanagement.web.controller;

import org.openmrs.Privilege;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/module/rwandapharmacymanagement/privileges")
public class PrivilegeSearchController {

    private static final int DEFAULT_LIMIT = 25;

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> search(@RequestParam(value = "q", required = false) String searchTerm,
                                            @RequestParam(value = "limit", required = false) Integer limit) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
        String normalizedSearch = normalize(searchTerm);
        int resolvedLimit = limit == null || limit.intValue() < 1 ? DEFAULT_LIMIT : Math.min(limit.intValue(), DEFAULT_LIMIT);
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        Collection<Privilege> privileges = Context.getUserService().getAllPrivileges();
        if (privileges == null) {
            return response;
        }
        for (Privilege privilege : privileges) {
            if (privilege == null || privilege.getPrivilege() == null) {
                continue;
            }
            String privilegeName = privilege.getPrivilege();
            String description = privilege.getDescription();
            if (!normalizedSearch.isEmpty()
                    && !normalize(privilegeName).contains(normalizedSearch)
                    && !normalize(description).contains(normalizedSearch)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("name", privilegeName);
            row.put("description", description);
            response.add(row);
            if (response.size() >= resolvedLimit) {
                break;
            }
        }
        return response;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }
}
