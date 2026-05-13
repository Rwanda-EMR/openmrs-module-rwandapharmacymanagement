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
        Item existingItem = pharmacyService.getItemByCodeAndProgram(normalizedCode, normalizedProgram);
        if (existingItem == null && normalizedProgram.isEmpty()) {
            existingItem = pharmacyService.getItemByCode(normalizedCode);
        }
        if (existingItem != null) {
            if ((existingItem.getName() == null || existingItem.getName().trim().isEmpty()) && normalized(itemName).length() > 0) {
                existingItem.setName(normalized(itemName));
                existingItem.setProgram(normalizedProgram);
                return pharmacyService.saveItem(existingItem);
            }
            return existingItem;
        }
        ProductMasterItem productMasterItem = pharmacyService.getProductMasterItemByNpcCodeAndProgram(normalizedCode, normalizedProgram);
        if (productMasterItem == null && normalizedProgram.isEmpty()) {
            productMasterItem = pharmacyService.getProductMasterItemByNpcCode(normalizedCode);
        }
        if (productMasterItem != null) {
            Item item = new Item();
            item.setCode(normalizedCode);
            item.setName(normalized(productMasterItem.getItemName()));
            item.setProgram(normalized(productMasterItem.getProgram()));
            item.setSupplierManaged(Boolean.TRUE);
            item.setStockable(Boolean.TRUE);
            item.setDispensable(Boolean.TRUE);
            item.setRequiresBatch(Boolean.FALSE);
            item.setRequiresExpiry(Boolean.FALSE);
            Drug drug = productMasterItem.getDrug();
            item.setDrug(drug);
            item.setConcept(drug != null ? drug.getConcept() : findOrCreateConcept(normalized(productMasterItem.getItemName())));
            item.setCreator(Context.getAuthenticatedUser());
            item.setDateCreated(new Date());
            return pharmacyService.saveItem(item);
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
        item.setProgram(normalizedProgram);
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
