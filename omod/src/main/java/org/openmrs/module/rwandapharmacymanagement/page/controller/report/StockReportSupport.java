package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StockReportSupport {

    private StockReportSupport() {
    }

    static List<PharmacyLocation> getActivePharmacies(PharmacyService pharmacyService) {
        List<PharmacyLocation> pharmacies = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation pharmacyLocation : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacyLocation != null && Boolean.TRUE.equals(pharmacyLocation.getActive())) {
                pharmacies.add(pharmacyLocation);
            }
        }
        Collections.sort(pharmacies, new Comparator<PharmacyLocation>() {
            @Override
            public int compare(PharmacyLocation left, PharmacyLocation right) {
                String leftName = left == null || left.getName() == null ? "" : left.getName();
                String rightName = right == null || right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return pharmacies;
    }

    static List<Map<String, Object>> buildPharmacyRows(PharmacyLocation selectedPharmacy,
                                                       InventoryService inventoryService,
                                                       PricingService pricingService,
                                                       PharmacyService pharmacyService,
                                                       String materialType,
                                                       String program) {
        if (selectedPharmacy == null || selectedPharmacy.getPharmacyLocationId() == null) {
            return Collections.emptyList();
        }
        ProductMasterLookup productMasterLookup = new ProductMasterLookup(pharmacyService.getAllProductMasterItems());
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(selectedPharmacy.getPharmacyLocationId())) {
            if (balance == null || balance.getItem() == null || balance.getQuantityOnHand() == null
                    || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ProductMasterItem productMasterItem = productMasterLookup.resolve(balance.getItem());
            if (!matchesFilters(productMasterItem, materialType, program)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            BigDecimal unitPrice = balance.getUnitPrice();
            BigDecimal sellingPrice = pricingService.calculateSellingPrice(unitPrice);
            row.put("balance", balance);
            row.put("materialType", productMasterItem == null ? "" : productMasterItem.getMaterialType());
            row.put("program", productMasterItem == null ? "" : productMasterItem.getProgram());
            row.put("unitPrice", unitPrice);
            row.put("sellingPrice", sellingPrice);
            row.put("totalValue", unitPrice == null ? null : unitPrice.multiply(balance.getQuantityOnHand()));
            row.put("totalSellingValue", sellingPrice == null ? null : sellingPrice.multiply(balance.getQuantityOnHand()));
            rows.add(row);
        }
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                InventoryBalance leftBalance = (InventoryBalance) left.get("balance");
                InventoryBalance rightBalance = (InventoryBalance) right.get("balance");
                String leftName = leftBalance == null || leftBalance.getItem() == null || leftBalance.getItem().getName() == null ? "" : leftBalance.getItem().getName();
                String rightName = rightBalance == null || rightBalance.getItem() == null || rightBalance.getItem().getName() == null ? "" : rightBalance.getItem().getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return rows;
    }

    static List<Map<String, Object>> buildOverallRows(InventoryService inventoryService,
                                                      PricingService pricingService,
                                                      PharmacyService pharmacyService,
                                                      String materialType,
                                                      String program) {
        ProductMasterLookup productMasterLookup = new ProductMasterLookup(pharmacyService.getAllProductMasterItems());
        Map<Integer, Map<String, Object>> byItemId = new LinkedHashMap<Integer, Map<String, Object>>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ProductMasterItem productMasterItem = productMasterLookup.resolve(balance.getItem());
            if (!matchesFilters(productMasterItem, materialType, program)) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            Map<String, Object> row = byItemId.get(itemId);
            if (row == null) {
                row = new LinkedHashMap<String, Object>();
                row.put("itemName", balance.getItem().getName());
                row.put("itemCode", balance.getItem().getCode());
                row.put("materialType", productMasterItem == null ? "" : productMasterItem.getMaterialType());
                row.put("program", productMasterItem == null ? "" : productMasterItem.getProgram());
                row.put("quantity", BigDecimal.ZERO);
                row.put("pharmacies", 0);
                row.put("totalValue", BigDecimal.ZERO);
                row.put("totalSellingValue", BigDecimal.ZERO);
                row.put("pharmacyIds", new HashSet<Integer>());
                byItemId.put(itemId, row);
            }
            row.put("quantity", ((BigDecimal) row.get("quantity")).add(balance.getQuantityOnHand()));
            BigDecimal unitPrice = balance.getUnitPrice();
            BigDecimal sellingPrice = pricingService.calculateSellingPrice(unitPrice);
            if (unitPrice != null) {
                row.put("totalValue", ((BigDecimal) row.get("totalValue")).add(unitPrice.multiply(balance.getQuantityOnHand())));
            }
            if (sellingPrice != null) {
                row.put("totalSellingValue", ((BigDecimal) row.get("totalSellingValue")).add(sellingPrice.multiply(balance.getQuantityOnHand())));
            }
            if (balance.getPharmacyLocation() != null && balance.getPharmacyLocation().getPharmacyLocationId() != null) {
                ((Set<Integer>) row.get("pharmacyIds")).add(balance.getPharmacyLocation().getPharmacyLocationId());
                row.put("pharmacies", ((Set<Integer>) row.get("pharmacyIds")).size());
            }
        }
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : byItemId.values()) {
            row.remove("pharmacyIds");
            rows.add(row);
        }
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                String leftName = left == null || left.get("itemName") == null ? "" : left.get("itemName").toString();
                String rightName = right == null || right.get("itemName") == null ? "" : right.get("itemName").toString();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return rows;
    }

    static List<String> getMaterialTypes(PharmacyService pharmacyService) {
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (ProductMasterItem item : pharmacyService.getAllProductMasterItems()) {
            addIfPresent(values, item == null ? null : item.getMaterialType());
        }
        return sorted(values);
    }

    static List<String> getPrograms(PharmacyService pharmacyService) {
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (ProductMasterItem item : pharmacyService.getAllProductMasterItems()) {
            addIfPresent(values, item == null ? null : item.getProgram());
        }
        return sorted(values);
    }

    static BigDecimal sumPharmacyQuantity(List<Map<String, Object>> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            InventoryBalance balance = (InventoryBalance) row.get("balance");
            if (balance != null && balance.getQuantityOnHand() != null) {
                total = total.add(balance.getQuantityOnHand());
            }
        }
        return total;
    }

    static BigDecimal sumMoney(List<Map<String, Object>> rows, String key) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal value = row.get(key) instanceof BigDecimal ? (BigDecimal) row.get(key) : null;
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private static boolean matchesFilters(ProductMasterItem productMasterItem, String materialType, String program) {
        return matchesValue(productMasterItem == null ? null : productMasterItem.getMaterialType(), materialType)
                && matchesValue(productMasterItem == null ? null : productMasterItem.getProgram(), program);
    }

    private static boolean matchesValue(String actual, String selected) {
        String normalizedSelected = normalize(selected);
        return normalizedSelected.isEmpty() || normalizedSelected.equals(normalize(actual));
    }

    private static void addIfPresent(Set<String> values, String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isEmpty()) {
            values.add(normalized);
        }
    }

    private static List<String> sorted(Set<String> values) {
        List<String> sorted = new ArrayList<String>(values);
        Collections.sort(sorted);
        return sorted;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static class ProductMasterLookup {

        private final Map<String, ProductMasterItem> byCode = new HashMap<String, ProductMasterItem>();
        private final Map<String, ProductMasterItem> byCodeProgram = new HashMap<String, ProductMasterItem>();
        private final Map<String, ProductMasterItem> byName = new HashMap<String, ProductMasterItem>();
        private final Map<Integer, ProductMasterItem> byDrugId = new HashMap<Integer, ProductMasterItem>();

        private ProductMasterLookup(List<ProductMasterItem> productMasterItems) {
            if (productMasterItems == null) {
                return;
            }
            for (ProductMasterItem item : productMasterItems) {
                if (item == null) {
                    continue;
                }
                String code = normalize(item.getNpcCode());
                if (!code.isEmpty() && !byCode.containsKey(code)) {
                    byCode.put(code, item);
                }
                String programKey = codeProgramKey(item.getNpcCode(), item.getProgram());
                if (!programKey.isEmpty() && !byCodeProgram.containsKey(programKey)) {
                    byCodeProgram.put(programKey, item);
                }
                String name = normalize(item.getItemName());
                if (!name.isEmpty() && !byName.containsKey(name)) {
                    byName.put(name, item);
                }
                if (item.getDrug() != null && item.getDrug().getDrugId() != null
                        && !byDrugId.containsKey(item.getDrug().getDrugId())) {
                    byDrugId.put(item.getDrug().getDrugId(), item);
                }
            }
        }

        private ProductMasterItem resolve(Item item) {
            if (item == null) {
                return null;
            }
            String code = normalize(item.getCode());
            String programKey = codeProgramKey(item.getCode(), item.getProgram());
            if (!programKey.isEmpty() && byCodeProgram.containsKey(programKey)) {
                return byCodeProgram.get(programKey);
            }
            if (!code.isEmpty() && byCode.containsKey(code)) {
                return byCode.get(code);
            }
            String name = normalize(item.getName());
            if (!name.isEmpty() && byName.containsKey(name)) {
                return byName.get(name);
            }
            if (item.getDrug() != null && item.getDrug().getDrugId() != null) {
                return byDrugId.get(item.getDrug().getDrugId());
            }
            return null;
        }

        private String codeProgramKey(String code, String program) {
            String normalizedCode = normalize(code);
            String normalizedProgram = normalize(program);
            if (normalizedCode.isEmpty() || normalizedProgram.isEmpty()) {
                return "";
            }
            return normalizedCode + "|" + normalizedProgram;
        }
    }
}
