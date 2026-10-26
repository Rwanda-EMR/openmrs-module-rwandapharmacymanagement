package org.openmrs.module.rwandapharmacymanagement.page.controller.item;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.page.controller.support.PaginationSupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.UiUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

@Controller("rwandaPharmacyItemListPageController")
public class ListPageController {

    private static final String MATERIAL_TYPE_DRUG = "ZMED";
    private static final String MATERIAL_TYPE_CONSUMABLE = "ZCON";

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "page", required = false) Integer page,
                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<Item> allItems = pharmacyService.getAllItems();
        int currentPage = PaginationSupport.sanitizePage(page);
        int currentPageSize = PaginationSupport.sanitizePageSize(pageSize);
        currentPage = PaginationSupport.clampPage(currentPage, currentPageSize, allItems.size());
        model.addAttribute("items", PaginationSupport.slice(allItems, currentPage, currentPageSize));
        PaginationSupport.addAttributes(model, "item", currentPage, currentPageSize, allItems.size());
        model.addAttribute("categories", pharmacyService.getAllItemCategories());
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("name") String name,
                       @RequestParam(value = "hasNpcCode", required = false) String hasNpcCode,
                       @RequestParam(value = "npcCode", required = false) String npcCode,
                       @RequestParam(value = "program", required = false) String program,
                       @RequestParam(value = "materialType", required = false) String materialType,
                       @RequestParam(value = "conceptId", required = false) Integer conceptId,
                       @RequestParam(value = "drugId", required = false) Integer drugId,
                       @RequestParam(value = "lowStockThreshold", required = false) Integer lowStockThreshold,
                       @RequestParam(value = "requiresBatch", required = false) Boolean requiresBatch,
                       @RequestParam(value = "requiresExpiry", required = false) Boolean requiresExpiry,
                       UiUtils ui) {
        boolean npcBacked = "true".equalsIgnoreCase(safe(hasNpcCode));
        String normalizedNpcCode = safe(npcCode).trim();
        if (npcBacked && normalizedNpcCode.isEmpty()) {
            throw new IllegalArgumentException("NPC code is required when the item has an NPC code");
        }
        String normalizedProgram = safe(program).trim();
        String normalizedMaterialType = safe(materialType).trim();
        if (normalizedMaterialType.isEmpty()) {
            normalizedMaterialType = MATERIAL_TYPE_CONSUMABLE;
        }
        Concept concept = null;
        Drug drug = null;
        if (MATERIAL_TYPE_DRUG.equalsIgnoreCase(normalizedMaterialType)) {
            if (drugId == null) {
                throw new IllegalArgumentException("OpenMRS drug is required when adding a drug item");
            }
            drug = Context.getConceptService().getDrug(drugId);
            if (drug == null) {
                throw new IllegalArgumentException("Selected OpenMRS drug was not found");
            }
            concept = drug.getConcept();
        } else if (conceptId != null) {
            concept = Context.getConceptService().getConcept(conceptId);
            if (concept == null) {
                throw new IllegalArgumentException("Selected OpenMRS concept was not found");
            }
        }
        String itemCode = npcBacked ? normalizedNpcCode : nextLocalCode();
        Item item = pharmacyService.getItemByExactName(name);
        if (item != null) {
            if (!npcBacked) {
                throw new IllegalArgumentException("An active pharmacy item already exists with this name: " + name);
            }
            String existingCode = safe(item.getCode()).trim();
            if (!existingCode.isEmpty() && !existingCode.equalsIgnoreCase(normalizedNpcCode)) {
                throw new IllegalArgumentException("An active pharmacy item already exists with this name but a different NPC code: " + name);
            }
        }
        if (item == null && npcBacked) {
            item = pharmacyService.getItemByCode(normalizedNpcCode);
        }
        if (item == null) {
            item = new Item();
            item.setName(name);
            item.setCode(itemCode);
            item.setProgram("");
            item.setDrug(drug);
            item.setConcept(concept);
            item.setSupplierManaged(Boolean.valueOf(npcBacked));
            item.setStockable(Boolean.TRUE);
            item.setDispensable(Boolean.TRUE);
            item.setLowStockThreshold(lowStockThreshold);
            item.setRequiresBatch(Boolean.TRUE.equals(requiresBatch));
            item.setRequiresExpiry(Boolean.TRUE.equals(requiresExpiry));
            item.setCreator(Context.getAuthenticatedUser());
            item.setDateCreated(new Date());
            pharmacyService.saveItem(item);
        }
        String productMasterNpcCode = npcBacked ? normalizedNpcCode : itemCode;
        ProductMasterItem productMasterItem = pharmacyService.getProductMasterItemByNpcCodeAndProgram(
                productMasterNpcCode, normalizedProgram);
        if (productMasterItem == null) {
            productMasterItem = new ProductMasterItem();
            productMasterItem.setCreator(Context.getAuthenticatedUser());
            productMasterItem.setDateCreated(new Date());
        }
        productMasterItem.setItemName(name);
        productMasterItem.setNpcCode(productMasterNpcCode);
        productMasterItem.setProgram(normalizedProgram);
        productMasterItem.setMaterialType(normalizedMaterialType);
        productMasterItem.setDrug(drug);
        productMasterItem.setActive(Boolean.TRUE);
        productMasterItem.setSource(ProductMasterItem.SOURCE_MANUAL);
        pharmacyService.saveProductMasterItem(productMasterItem);
        pharmacyService.mapProductMasterItemToItem(productMasterItem, item);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "item/list");
    }

    private String nextLocalCode() {
        return "LOCAL-" + System.currentTimeMillis();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
