package org.openmrs.module.rwandapharmacymanagement.web.support;

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Drug;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class DeliveryReceiptSupport {

    private DeliveryReceiptSupport() {
    }

    public static Item resolveOrCreateItemByCode(PharmacyService pharmacyService, String itemCode) {
        return resolveOrCreateItemByCode(pharmacyService, itemCode, null, null);
    }

    public static Item resolveOrCreateItemByCode(PharmacyService pharmacyService, String itemCode, String itemName) {
        return resolveOrCreateItemByCode(pharmacyService, itemCode, itemName, null);
    }

    public static Item resolveOrCreateItemByCode(PharmacyService pharmacyService, String itemCode, String itemName, String program) {
        String normalizedCode = normalized(itemCode);
        String normalizedProgram = normalized(program);
        ProductMasterItem productMasterItem = pharmacyService.getProductMasterItemByNpcCodeAndProgram(normalizedCode, normalizedProgram);
        if (productMasterItem == null && normalizedProgram.isEmpty()) {
            productMasterItem = pharmacyService.getProductMasterItemByNpcCode(normalizedCode);
        }
        if (productMasterItem != null) {
            return resolveOrCreateMappedItem(pharmacyService, productMasterItem);
        }

        String normalizedName = normalized(itemName);
        if (normalizedName.isEmpty()) {
            return null;
        }
        Item existingByName = findExistingItemByExactName(pharmacyService, normalizedName);
        if (existingByName != null) {
            return existingByName;
        }
        Item item = new Item();
        item.setCode(normalizedCode);
        item.setName(normalizedName);
        item.setProgram("");
        item.setSupplierManaged(Boolean.TRUE);
        item.setStockable(Boolean.TRUE);
        item.setDispensable(Boolean.TRUE);
        item.setRequiresBatch(Boolean.FALSE);
        item.setRequiresExpiry(Boolean.FALSE);
        item.setConcept(findOrCreateConcept(normalizedName));
        item.setCreator(Context.getAuthenticatedUser());
        item.setDateCreated(new Date());
        return pharmacyService.saveItem(item);
    }

    public static Item resolveOrCreateMappedItem(PharmacyService pharmacyService, ProductMasterItem productMasterItem) {
        if (productMasterItem == null) {
            return null;
        }
        Item mappedItem = pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
        if (mappedItem != null) {
            return mappedItem;
        }
        String itemName = normalized(productMasterItem.getItemName());
        Item existingByName = findExistingItemByExactName(pharmacyService, itemName);
        if (existingByName != null) {
            pharmacyService.mapProductMasterItemToItem(productMasterItem, existingByName);
            return existingByName;
        }
        Item existingByCode = pharmacyService.getItemByCode(normalized(productMasterItem.getNpcCode()));
        if (existingByCode != null) {
            pharmacyService.mapProductMasterItemToItem(productMasterItem, existingByCode);
            return existingByCode;
        }
        Item item = new Item();
        item.setCode(normalized(productMasterItem.getNpcCode()));
        item.setName(itemName);
        item.setProgram("");
        item.setSupplierManaged(Boolean.TRUE);
        item.setStockable(Boolean.TRUE);
        item.setDispensable(Boolean.TRUE);
        item.setRequiresBatch(Boolean.FALSE);
        item.setRequiresExpiry(Boolean.FALSE);
        Drug drug = productMasterItem.getDrug();
        item.setDrug(drug);
        item.setConcept(drug != null ? drug.getConcept() : findOrCreateConcept(itemName));
        item.setCreator(Context.getAuthenticatedUser());
        item.setDateCreated(new Date());
        Item savedItem = pharmacyService.saveItem(item);
        pharmacyService.mapProductMasterItemToItem(productMasterItem, savedItem);
        return savedItem;
    }

    private static Concept findOrCreateConcept(String conceptName) {
        String normalizedConceptName = normalized(conceptName);
        Concept existingConcept = Context.getConceptService().getConceptByName(normalizedConceptName);
        if (existingConcept != null) {
            return existingConcept;
        }
        List<Concept> matchingConcepts = Context.getConceptService().getConceptsByName(normalizedConceptName);
        if (matchingConcepts != null) {
            for (Concept concept : matchingConcepts) {
                if (concept == null || concept.getNames() == null) {
                    continue;
                }
                for (ConceptName name : concept.getNames()) {
                    if (name != null && normalizedConceptName.equalsIgnoreCase(normalized(name.getName()))) {
                        return concept;
                    }
                }
            }
        }
        Concept concept = new Concept();
        ConceptName preferredName = new ConceptName(normalizedConceptName, Locale.ENGLISH);
        preferredName.setLocalePreferred(true);
        concept.addName(preferredName);
        concept.setDatatype(resolveConceptDatatype());
        concept.setConceptClass(resolveConceptClass());
        concept.setSet(false);
        concept.setCreator(Context.getAuthenticatedUser());
        concept.setDateCreated(new Date());
        return Context.getConceptService().saveConcept(concept);
    }

    private static Item findExistingItemByExactName(PharmacyService pharmacyService, String itemName) {
        List<Item> matches = pharmacyService.searchItems(itemName, 20);
        if (matches == null) {
            return null;
        }
        for (Item item : matches) {
            if (item != null && item.getName() != null && itemName.equalsIgnoreCase(normalized(item.getName()))) {
                return item;
            }
        }
        return null;
    }

    private static ConceptDatatype resolveConceptDatatype() {
        ConceptDatatype datatype = Context.getConceptService().getConceptDatatypeByName("N/A");
        if (datatype == null) {
            datatype = Context.getConceptService().getConceptDatatypeByName("Text");
        }
        if (datatype == null) {
            throw new IllegalStateException("Unable to resolve concept datatype for delivery receipt item creation");
        }
        return datatype;
    }

    private static ConceptClass resolveConceptClass() {
        ConceptClass conceptClass = Context.getConceptService().getConceptClassByName("Drug");
        if (conceptClass == null) {
            conceptClass = Context.getConceptService().getConceptClassByName("Misc");
        }
        if (conceptClass == null) {
            throw new IllegalStateException("Unable to resolve concept class for delivery receipt item creation");
        }
        return conceptClass;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
