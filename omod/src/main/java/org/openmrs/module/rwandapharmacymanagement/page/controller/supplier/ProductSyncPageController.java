package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.openmrs.Location;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Privilege;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProductSyncPageController {
    private static final String FLASH_ERROR_MESSAGE = "rpm.productSync.errorMessage";

    public void get(PageModel model,
                    @SpringBean("integrationLogService") IntegrationLogService integrationLogService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "page", required = false) Integer page,
                    @RequestParam(value = "npcCode", required = false) String npcCode,
                    @RequestParam(value = "itemName", required = false) String itemName,
                    @RequestParam(value = "conceptName", required = false) String conceptName,
                    @RequestParam(value = "program", required = false) String program,
                    @RequestParam(value = "materialType", required = false) String materialType,
                    HttpSession session) {
        refreshSessionLocation();
        model.addAttribute("integrationLogs", integrationLogService.getRecentLogs());
        int pageSize = 25;
        int currentPage = page == null || page.intValue() < 1 ? 1 : page.intValue();
        int totalItems = pharmacyService.getProductMasterItemCount(npcCode, itemName, conceptName, program, materialType);
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / (double) pageSize);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        int offset = (currentPage - 1) * pageSize;
        List<ProductMasterItem> productMasterItems = pharmacyService.getProductMasterItems(Integer.valueOf(offset), Integer.valueOf(pageSize),
                npcCode, itemName, conceptName, program, materialType);
        List<String> npcCodes = new ArrayList<String>();
        for (ProductMasterItem productMasterItem : productMasterItems) {
            if (productMasterItem.getNpcCode() != null && !productMasterItem.getNpcCode().trim().isEmpty()) {
                npcCodes.add(productMasterItem.getNpcCode());
            }
        }
        List<Item> localItems = pharmacyService.getItemsByCodes(npcCodes);
        Map<String, Item> itemsByCode = new LinkedHashMap<String, Item>();
        for (Item item : localItems) {
            itemsByCode.put(item.getCode(), item);
        }
        Map<String, String> conceptNameByNpcCode = new LinkedHashMap<String, String>();
        for (ProductMasterItem productMasterItem : productMasterItems) {
            String resolvedConceptName = productMasterItem.getDrug() != null
                    ? conceptDisplayName(productMasterItem.getDrug().getConcept())
                    : "";
            if (resolvedConceptName.isEmpty()) {
                Item localItem = itemsByCode.get(productMasterItem.getNpcCode());
                resolvedConceptName = localItem == null ? "" : conceptDisplayName(localItem.getConcept());
            }
            conceptNameByNpcCode.put(productMasterItem.getNpcCode(), resolvedConceptName);
        }
        model.addAttribute("productMasterItems", productMasterItems);
        model.addAttribute("conceptNameByNpcCode", conceptNameByNpcCode);
        model.addAttribute("currentPage", Integer.valueOf(currentPage));
        model.addAttribute("pageSize", Integer.valueOf(pageSize));
        model.addAttribute("totalItems", Integer.valueOf(totalItems));
        model.addAttribute("totalPages", Integer.valueOf(totalPages));
        model.addAttribute("npcCode", safe(npcCode));
        model.addAttribute("itemName", safe(itemName));
        model.addAttribute("conceptName", safe(conceptName));
        model.addAttribute("program", safe(program));
        model.addAttribute("materialType", safe(materialType));
        boolean configured = pharmacyService.getSupplierSetting() != null
                && Boolean.TRUE.equals(pharmacyService.getSupplierSetting().getIntegrationEnabled())
                && pharmacyService.getSupplierSetting().getBaseUrl() != null
                && !pharmacyService.getSupplierSetting().getBaseUrl().trim().isEmpty();
        model.addAttribute("supplierConfigured", configured);
        model.addAttribute("itemsUpdated", Boolean.FALSE);
        model.addAttribute("pricesUpdated", Boolean.FALSE);
        String errorMessage = FlashMessageSupport.consumeString(session, FLASH_ERROR_MESSAGE);
        if (!configured && errorMessage == null) {
            errorMessage = "Configure and enable supplier settings before syncing supplier items or prices.";
        }
        model.addAttribute("errorMessage", errorMessage);
    }

    public String post(@SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "productMasterItemId", required = false) Integer productMasterItemId,
                       @RequestParam(value = "requiredPrivilege", required = false) String requiredPrivilege,
                       @RequestParam(value = "restrictionReason", required = false) String restrictionReason,
                       HttpSession session,
                       UiUtils ui) {
        try {
            if ("updatePrices".equals(action)) {
                supplierIntegrationService.syncPrices();
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/productSync");
            }
            if ("saveRestriction".equals(action)) {
                saveRestriction(pharmacyService, productMasterItemId, requiredPrivilege, restrictionReason);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/productSync");
            }
            if ("clearRestriction".equals(action)) {
                saveRestriction(pharmacyService, productMasterItemId, null, null);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/productSync");
            }
            supplierIntegrationService.syncProductMaster();
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/productSync");
        }
        catch (Exception e) {
            FlashMessageSupport.set(session, FLASH_ERROR_MESSAGE, e.getMessage());
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/productSync");
        }
    }

    private void saveRestriction(PharmacyService pharmacyService, Integer productMasterItemId, String requiredPrivilege,
                                 String restrictionReason) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
        if (productMasterItemId == null) {
            throw new IllegalArgumentException("Product master item is required");
        }
        ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(productMasterItemId);
        if (productMasterItem == null) {
            throw new IllegalArgumentException("Product master item was not found");
        }
        String privilege = trimToNull(requiredPrivilege);
        if (privilege != null) {
            Privilege openmrsPrivilege = Context.getUserService().getPrivilege(privilege);
            if (openmrsPrivilege == null) {
                throw new IllegalArgumentException("Select an existing OpenMRS privilege");
            }
        }
        productMasterItem.setRequiredPrivilege(privilege);
        productMasterItem.setRestrictionReason(trimToNull(restrictionReason));
        pharmacyService.saveProductMasterItem(productMasterItem);
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

    private String conceptDisplayName(Concept concept) {
        if (concept == null) {
            return "";
        }
        ConceptName preferredName = concept.getPreferredName(Context.getLocale());
        if (preferredName == null) {
            preferredName = concept.getPreferredName(java.util.Locale.ENGLISH);
        }
        if (preferredName != null && preferredName.getName() != null && !preferredName.getName().trim().isEmpty()) {
            return preferredName.getName().trim();
        }
        if (concept.getNames() != null && !concept.getNames().isEmpty()) {
            List<ConceptName> conceptNames = new ArrayList<ConceptName>(concept.getNames());
            Collections.sort(conceptNames, new Comparator<ConceptName>() {
                @Override
                public int compare(ConceptName left, ConceptName right) {
                    String leftName = left == null || left.getName() == null ? "" : left.getName();
                    String rightName = right == null || right.getName() == null ? "" : right.getName();
                    return leftName.compareToIgnoreCase(rightName);
                }
            });
            for (ConceptName conceptName : conceptNames) {
                if (conceptName != null && conceptName.getName() != null && !conceptName.getName().trim().isEmpty()) {
                    return conceptName.getName().trim();
                }
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
