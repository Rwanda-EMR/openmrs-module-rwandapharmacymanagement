package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.BillingPriceService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MissingBillingMappingsPageController {

    private static final Log log = LogFactory.getLog(MissingBillingMappingsPageController.class);
    private static final String PRIVILEGE_MANAGE_SETTINGS = "Task: rwandapharmacymanagement.manageSettings";

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("billingPriceService") BillingPriceService billingPriceService,
                    @SpringBean("pricingService") PricingService pricingService,
                    HttpSession session) {
        Context.requirePrivilege(PRIVILEGE_MANAGE_SETTINGS);
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        List<StockedItemSummary> summaries = stockedItemSummaries(inventoryService, pharmacyService);
        List<Integer> itemIds = new ArrayList<Integer>();
        for (StockedItemSummary summary : summaries) {
            if (summary.item != null && summary.item.getItemId() != null) {
                itemIds.add(summary.item.getItemId());
            }
        }
        Map<Integer, BigDecimal> highestCosts = billingPriceService.getHighestCurrentStockCosts(itemIds);
        for (StockedItemSummary summary : summaries) {
            if (hasExactFacilityServicePriceName(summary.item)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("item", summary.item);
            row.put("quantityOnHand", summary.quantityOnHand);
            row.put("pharmacies", summary.pharmacies);
            row.put("itemType", summary.item.getDrug() == null ? "Consumable" : "Drug");
            row.put("drugName", summary.item.getDrug() == null ? "" : summary.item.getDrug().getName());
            row.put("drugConceptId", summary.item.getDrug() == null || summary.item.getDrug().getConcept() == null
                    ? null : summary.item.getDrug().getConcept().getConceptId());
            row.put("itemConceptId", summary.item.getConcept() == null ? null : summary.item.getConcept().getConceptId());
            row.put("candidateFacilityServices", candidateFacilityServiceNames(summary.item));
            BigDecimal currentStockCost = highestCosts.get(summary.item.getItemId());
            row.put("currentStockCost", currentStockCost);
            row.put("markupPercentage", pricingService.getDispensingMarkupPercentage());
            row.put("calculatedPrice", pricingService.calculateSellingPrice(currentStockCost));
            rows.add(row);
        }
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return stringValue(((Item) left.get("item")).getName())
                        .compareToIgnoreCase(stringValue(((Item) right.get("item")).getName()));
            }
        });
        model.addAttribute("rows", rows);
        model.addAttribute("rowCount", rows.size());
        model.addAttribute("canCreateBillingMappings", "true".equalsIgnoreCase(Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_BILLING_CREATE_MISSING_MAPPINGS, "false")));
        model.addAttribute("message", session.getAttribute("rpmBillingMappingMessage"));
        model.addAttribute("error", session.getAttribute("rpmBillingMappingError"));
        session.removeAttribute("rpmBillingMappingMessage");
        session.removeAttribute("rpmBillingMappingError");
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("billingPriceService") BillingPriceService billingPriceService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "itemId", required = false) Integer itemId,
                       @RequestParam(value = "displayedPrice", required = false) BigDecimal displayedPrice,
                       HttpSession session,
                       UiUtils ui) {
        Context.requirePrivilege(PRIVILEGE_MANAGE_SETTINGS);
        if (!"createBillingMapping".equals(action)) {
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/missingBillingMappings");
        }
        try {
            Item item = itemId == null ? null : pharmacyService.getItem(itemId);
            billingPriceService.createBillingMapping(item, displayedPrice);
            session.setAttribute("rpmBillingMappingMessage", "Billing services were created for the selected pharmacy item.");
        }
        catch (Exception e) {
            log.error("Unable to create MOH Billing mapping for pharmacy item. itemId=" + itemId, e);
            session.setAttribute("rpmBillingMappingError", e.getMessage() == null
                    ? "Unable to create billing services for the selected pharmacy item." : e.getMessage());
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/missingBillingMappings");
    }

    private List<StockedItemSummary> stockedItemSummaries(InventoryService inventoryService, PharmacyService pharmacyService) {
        Map<Integer, StockedItemSummary> byItemId = new LinkedHashMap<Integer, StockedItemSummary>();
        for (PharmacyLocation pharmacyLocation : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null
                    || Boolean.FALSE.equals(pharmacyLocation.getActive())) {
                continue;
            }
            List<InventoryBalance> balances = inventoryService.getInventoryForPharmacy(pharmacyLocation.getPharmacyLocationId());
            if (balances == null) {
                continue;
            }
            for (InventoryBalance balance : balances) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                        || balance.getQuantityOnHand() == null
                        || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                StockedItemSummary summary = byItemId.get(balance.getItem().getItemId());
                if (summary == null) {
                    summary = new StockedItemSummary();
                    summary.item = balance.getItem();
                    summary.quantityOnHand = BigDecimal.ZERO;
                    byItemId.put(balance.getItem().getItemId(), summary);
                }
                summary.quantityOnHand = summary.quantityOnHand.add(balance.getQuantityOnHand());
                addUnique(summary.pharmacies, pharmacyLocation.getName());
            }
        }
        return new ArrayList<StockedItemSummary>(byItemId.values());
    }

    private boolean hasExactFacilityServicePriceName(Item item) {
        List<String> names = billingLookupNames(item);
        if (names.isEmpty()) {
            return false;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("select fsp.facility_service_price_id ")
                .append("from moh_bill_facility_service_price fsp ")
                .append("join moh_bill_billable_service bs on bs.facility_service_price_id = fsp.facility_service_price_id ")
                .append("where fsp.retired = 0 ")
                .append("and lower(trim(coalesce(fsp.category, ''))) = '")
                .append(item != null && item.getDrug() != null ? "medicaments" : "consommables").append("' ")
                .append("and fsp.start_date <= curdate() ")
                .append("and (fsp.end_date is null or fsp.end_date >= curdate()) ")
                .append("and bs.retired = 0 and bs.start_date <= curdate() ")
                .append("and (bs.end_date is null or bs.end_date >= curdate()) ")
                .append("and (");
        appendNameClauses(sql, names);
        sql.append(") limit 1");
        return hasRows(sql.toString());
    }

    private List<String> candidateFacilityServiceNames(Item item) {
        String token = firstToken(item == null ? null : item.getName());
        if (token.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "select distinct fsp.name from moh_bill_facility_service_price fsp "
                + "join moh_bill_billable_service bs on bs.facility_service_price_id = fsp.facility_service_price_id "
                + "where fsp.retired = 0 and fsp.start_date <= curdate() "
                + "and (fsp.end_date is null or fsp.end_date >= curdate()) "
                + "and lower(fsp.name) like '%" + token.replace("'", "''") + "%' "
                + "order by fsp.name limit 8";
        List<List<Object>> rows = executeSql(sql);
        List<String> names = new ArrayList<String>();
        for (List<Object> row : rows) {
            if (row != null && !row.isEmpty() && row.get(0) != null) {
                addUnique(names, row.get(0).toString());
            }
        }
        return names;
    }

    private List<String> billingLookupNames(Item item) {
        List<String> names = new ArrayList<String>();
        if (item == null) {
            return names;
        }
        addUnique(names, normalizeName(item.getName()));
        if (item.getDrug() != null) {
            addUnique(names, normalizeName(item.getDrug().getName()));
        }
        return names;
    }

    private void appendNameClauses(StringBuilder sql, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sql.append(" or ");
            }
            String escapedName = names.get(i).replace("'", "''");
            sql.append("lower(trim(coalesce(fsp.name, ''))) = '").append(escapedName).append("'")
                    .append(" or lower(trim(coalesce(fsp.short_name, ''))) = '").append(escapedName).append("'")
                    .append(" or lower(trim(coalesce(fsp.description, ''))) = '").append(escapedName).append("'");
        }
    }

    private boolean hasRows(String sql) {
        return !executeSql(sql).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> executeSql(String sql) {
        Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        try {
            List<List<Object>> rows = Context.getAdministrationService().executeSQL(sql, true);
            return rows == null ? Collections.<List<Object>>emptyList() : rows;
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to check MOH Billing mappings.", e);
        }
        finally {
            Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private String firstToken(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) {
            return "";
        }
        int space = normalized.indexOf(' ');
        return space < 0 ? normalized : normalized.substring(0, space);
    }

    private String stringValue(String value) {
        return value == null ? "" : value;
    }

    private void addUnique(List<String> values, String value) {
        if (values == null || value == null || value.trim().isEmpty()) {
            return;
        }
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private static class StockedItemSummary {
        private Item item;
        private BigDecimal quantityOnHand;
        private List<String> pharmacies = new ArrayList<String>();
    }
}
