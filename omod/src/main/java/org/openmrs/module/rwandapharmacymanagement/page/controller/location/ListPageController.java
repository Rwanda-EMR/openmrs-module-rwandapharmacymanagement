package org.openmrs.module.rwandapharmacymanagement.page.controller.location;

import org.openmrs.api.context.Context;
import org.openmrs.LocationTag;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;
import org.openmrs.module.rwandapharmacymanagement.page.controller.support.PaginationSupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Controller("rwandaPharmacyLocationListPageController")
public class ListPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "page", required = false) Integer page,
                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<PharmacyLocation> allPharmacyLocations = pharmacyService.getAllPharmacyLocations();
        int currentPage = PaginationSupport.sanitizePage(page);
        int currentPageSize = PaginationSupport.sanitizePageSize(pageSize);
        currentPage = PaginationSupport.clampPage(currentPage, currentPageSize, allPharmacyLocations.size());
        model.addAttribute("pharmacyLocations", PaginationSupport.slice(allPharmacyLocations, currentPage, currentPageSize));
        model.addAttribute("allPharmacyLocations", allPharmacyLocations);
        PaginationSupport.addAttributes(model, "location", currentPage, currentPageSize, allPharmacyLocations.size());
        model.addAttribute("locations", Context.getLocationService().getAllLocations(false));
        model.addAttribute("locationTags", Context.getLocationService().getAllLocationTags(false));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                       @RequestParam(value = "locationId", required = false) Integer locationId,
                       @RequestParam(value = "locationTagId", required = false) Integer[] locationTagIds,
                       @RequestParam("name") String name,
                       @RequestParam("code") String code,
                       @RequestParam("locationType") PharmacyLocationType locationType,
                       UiUtils ui) {
        PharmacyLocation pharmacyLocation = pharmacyLocationId == null ? new PharmacyLocation() : pharmacyService.getPharmacyLocation(pharmacyLocationId);
        if ("delete".equalsIgnoreCase(action) && pharmacyLocation != null) {
            if (!inventoryService.getInventoryForPharmacy(pharmacyLocation.getPharmacyLocationId()).isEmpty()) {
                throw new IllegalArgumentException("Cannot delete a pharmacy location that still has assigned stock");
            }
            pharmacyLocation.setVoided(Boolean.TRUE);
            pharmacyLocation.setActive(Boolean.FALSE);
            pharmacyService.savePharmacyLocation(pharmacyLocation);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "location/list");
        }
        if (pharmacyLocation == null) {
            pharmacyLocation = new PharmacyLocation();
        }
        pharmacyLocation.setName(name);
        pharmacyLocation.setCode(code);
        pharmacyLocation.setLocation(locationId == null ? null : Context.getLocationService().getLocation(locationId));
        Set<LocationTag> tags = new HashSet<LocationTag>();
        if (locationTagIds != null) {
            for (Integer locationTagId : locationTagIds) {
                LocationTag tag = locationTagId == null ? null : Context.getLocationService().getLocationTag(locationTagId);
                if (tag != null) {
                    tags.add(tag);
                }
            }
        }
        pharmacyLocation.setLocationTags(tags);
        pharmacyLocation.setLocationTag(tags.isEmpty() ? null : tags.iterator().next());
        pharmacyLocation.setLocationType(locationType);
        pharmacyLocation.setMainStore(locationType == PharmacyLocationType.MAIN_PHARMACY);
        pharmacyLocation.setActive(true);
        if (pharmacyLocation.getCreator() == null) {
            pharmacyLocation.setCreator(Context.getAuthenticatedUser());
        }
        if (pharmacyLocation.getDateCreated() == null) {
            pharmacyLocation.setDateCreated(new java.util.Date());
        }
        pharmacyService.savePharmacyLocation(pharmacyLocation);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "location/list");
    }
}
