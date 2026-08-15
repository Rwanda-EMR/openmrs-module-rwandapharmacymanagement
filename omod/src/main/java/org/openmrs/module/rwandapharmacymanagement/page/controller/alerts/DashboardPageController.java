package org.openmrs.module.rwandapharmacymanagement.page.controller.alerts;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.AlertService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.AlertFilter;
import org.openmrs.module.rwandapharmacymanagement.model.ItemCategory;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class DashboardPageController {

    public void get(PageModel model,
                    @SpringBean("rwandaPharmacyAlertService") AlertService alertService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "itemCategoryId", required = false) Integer itemCategoryId,
                    @RequestParam(value = "q", required = false) String searchTerm,
                    @RequestParam(value = "expiryStatus", required = false) String expiryStatus,
                    @RequestParam(value = "stockStatus", required = false) String stockStatus,
                    @RequestParam(value = "expiryAlertDays", required = false) Integer expiryAlertDays,
                    @RequestParam(value = "daysOfStockAlertDays", required = false) Integer daysOfStockAlertDays,
                    @RequestParam(value = "consumptionLookbackDays", required = false) Integer consumptionLookbackDays) {
        AlertFilter filter = buildFilter(pharmacyLocationId, itemCategoryId, searchTerm, expiryStatus, stockStatus,
                expiryAlertDays, daysOfStockAlertDays, consumptionLookbackDays);
        model.addAttribute("filter", filter);
        model.addAttribute("pharmacyLocations", pharmacyService.getAllPharmacyLocations());
        model.addAttribute("itemCategories", pharmacyService.getAllItemCategories());
        model.addAttribute("selectedLocationName", locationName(pharmacyService, filter.getPharmacyLocationId()));
        model.addAttribute("selectedCategoryName", categoryName(pharmacyService.getAllItemCategories(), filter.getItemCategoryId()));
        model.addAttribute("lowStockAlerts", alertService.getStockoutAlertRows(filter));
        model.addAttribute("expiryAlerts", alertService.getExpirationAlertRows(filter));
    }

    static AlertFilter buildFilter(Integer pharmacyLocationId,
                                   Integer itemCategoryId,
                                   String searchTerm,
                                   String expiryStatus,
                                   String stockStatus,
                                   Integer expiryAlertDays,
                                   Integer daysOfStockAlertDays,
                                   Integer consumptionLookbackDays) {
        AlertFilter filter = new AlertFilter();
        filter.setPharmacyLocationId(pharmacyLocationId);
        filter.setItemCategoryId(itemCategoryId);
        filter.setSearchTerm(searchTerm == null ? "" : searchTerm.trim());
        filter.setExpiryStatus(hasText(expiryStatus) ? expiryStatus : "all");
        filter.setStockStatus(hasText(stockStatus) ? stockStatus : "all");
        filter.setExpiryAlertDays(positiveOrDefault(expiryAlertDays, defaultInt("rwandapharmacymanagement.alerts.expiryDays", 90)));
        filter.setDaysOfStockAlertDays(positiveOrDefault(daysOfStockAlertDays,
                defaultInt("rwandapharmacymanagement.alerts.daysOfStockDays", 14)));
        filter.setConsumptionLookbackDays(positiveOrDefault(consumptionLookbackDays,
                defaultInt("rwandapharmacymanagement.alerts.consumptionLookbackDays", 90)));
        return filter;
    }

    static String locationName(PharmacyService pharmacyService, Integer pharmacyLocationId) {
        if (pharmacyLocationId == null) {
            return "Whole hospital";
        }
        PharmacyLocation location = pharmacyService.getPharmacyLocation(pharmacyLocationId);
        return location == null ? "" : location.getName();
    }

    static String categoryName(List<ItemCategory> categories, Integer itemCategoryId) {
        if (itemCategoryId == null) {
            return "All categories";
        }
        if (categories != null) {
            for (ItemCategory category : categories) {
                if (category != null && itemCategoryId.equals(category.getItemCategoryId())) {
                    return category.getName();
                }
            }
        }
        return "";
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static int defaultInt(String property, int fallback) {
        try {
            return Integer.parseInt(Context.getAdministrationService().getGlobalProperty(property, String.valueOf(fallback)));
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static int positiveOrDefault(Integer value, int fallback) {
        return value == null || value.intValue() <= 0 ? fallback : value.intValue();
    }
}
