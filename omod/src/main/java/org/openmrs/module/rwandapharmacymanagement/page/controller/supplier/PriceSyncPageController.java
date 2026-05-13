package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierSetting;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class PriceSyncPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                    @RequestParam(value = "program", required = false) String program,
                    @RequestParam(value = "npcCodes", required = false) String npcCodes) {
        refreshSessionLocation();
        SupplierSetting supplierSetting = pharmacyService.getSupplierSetting();
        boolean configured = supplierSetting != null && Boolean.TRUE.equals(supplierSetting.getIntegrationEnabled())
                && supplierSetting.getBaseUrl() != null && !supplierSetting.getBaseUrl().trim().isEmpty();
        model.addAttribute("supplierConfigured", configured);
        if (!configured) {
            model.addAttribute("supplierPrices", Collections.emptyList());
            model.addAttribute("lookupResponse", null);
            model.addAttribute("errorMessage", "Configure and enable supplier settings before syncing prices.");
        }
        else {
            try {
                model.addAttribute("supplierPrices", supplierIntegrationService.loadSupplierPrices());
                model.addAttribute("lookupResponse",
                        npcCodes != null && !npcCodes.trim().isEmpty() ? supplierIntegrationService.lookupPrices(program, parseNpcCodes(npcCodes)) : null);
            }
            catch (Exception e) {
                model.addAttribute("supplierPrices", Collections.emptyList());
                model.addAttribute("lookupResponse", null);
                model.addAttribute("errorMessage", e.getMessage());
            }
        }
        model.addAttribute("selectedProgram", program);
        model.addAttribute("selectedNpcCodes", npcCodes);
    }

    public String post(@SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @RequestParam(value = "program", required = false) String program,
                       @RequestParam(value = "npcCodes", required = false) String npcCodes,
                       UiUtils ui) {
        if (program == null || npcCodes == null || npcCodes.trim().isEmpty()) {
            supplierIntegrationService.syncPrices();
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/priceSync");
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/priceSync",
                Collections.<String, Object>singletonMap("program", program)) + "&npcCodes=" + URLEncoder.encode(npcCodes);
    }

    private List<String> parseNpcCodes(String npcCodes) {
        List<String> codes = new ArrayList<String>();
        for (String code : npcCodes.split(",")) {
            if (code != null && !code.trim().isEmpty()) {
                codes.add(code.trim());
            }
        }
        return codes;
    }

    private void refreshSessionLocation() {
        UserContext userContext = Context.getUserContext();
        if (userContext == null || userContext.getLocation() == null || userContext.getLocationId() == null) {
            return;
        }
        Location location = Context.getLocationService().getLocation(userContext.getLocationId());
        if (location != null) {
            initializeLocationHierarchy(location);
            userContext.setLocation(location);
        }
    }

    private void initializeLocationHierarchy(Location location) {
        Location current = location;
        while (current != null) {
            current.getTags().size();
            current = current.getParentLocation();
        }
    }
}
