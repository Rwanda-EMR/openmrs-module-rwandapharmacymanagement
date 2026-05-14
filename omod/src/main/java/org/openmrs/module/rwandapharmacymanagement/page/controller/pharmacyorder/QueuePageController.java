package org.openmrs.module.rwandapharmacymanagement.page.controller.pharmacyorder;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Controller("pharmacyOrderQueuePageController")
public class QueuePageController {
    private static final String FLASH_APPROVED = "rpm.pharmacyOrderQueue.approved";

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = resolveActiveRequestPharmacy(pharmacyService, session);
        model.addAttribute("activePharmacy", activePharmacy);
        model.addAttribute("requests", activePharmacy == null ? java.util.Collections.emptyList()
                : pharmacyItemRequestService.getPendingRequestsForPharmacy(activePharmacy));
        model.addAttribute("availableByItemId", buildAvailableByItemId(activePharmacy, inventoryService));
        model.addAttribute("approved", FlashMessageSupport.consumeBoolean(session, FLASH_APPROVED));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam(value = "lineId", required = false) Integer[] lineIds,
                       @RequestParam(value = "approvedQuantity", required = false) BigDecimal[] approvedQuantities,
                       HttpSession session,
                       UiUtils ui) {
        PharmacyLocation activePharmacy = resolveActiveRequestPharmacy(pharmacyService, session);
        if (activePharmacy == null) {
            throw new IllegalArgumentException("No pharmacy is selected for the current session");
        }
        pharmacyItemRequestService.approveRequest(requestId, activePharmacy, lineIds, approvedQuantities);
        FlashMessageSupport.set(session, FLASH_APPROVED, Boolean.TRUE);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "pharmacyorder/queue");
    }

    private PharmacyLocation resolveActiveRequestPharmacy(PharmacyService pharmacyService, HttpSession session) {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.resolveCurrentPharmacy(pharmacyService, session);
        if (activePharmacy != null) {
            return activePharmacy;
        }
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        return matchesCurrentOpenmrsLocation(mainStore) ? mainStore : null;
    }

    private boolean matchesCurrentOpenmrsLocation(PharmacyLocation pharmacyLocation) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        if (pharmacyLocation == null || currentLocation == null) {
            return false;
        }
        if (pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null
                && pharmacyLocation.getLocation().getLocationId().equals(currentLocation.getLocationId())) {
            return true;
        }
        Set<Integer> currentTagIds = new HashSet<Integer>();
        if (currentLocation.getTags() != null) {
            for (LocationTag tag : currentLocation.getTags()) {
                if (tag != null && tag.getLocationTagId() != null) {
                    currentTagIds.add(tag.getLocationTagId());
                }
            }
        }
        if (pharmacyLocation.getLocationTag() != null && pharmacyLocation.getLocationTag().getLocationTagId() != null
                && currentTagIds.contains(pharmacyLocation.getLocationTag().getLocationTagId())) {
            return true;
        }
        if (pharmacyLocation.getLocationTags() != null) {
            for (LocationTag tag : pharmacyLocation.getLocationTags()) {
                if (tag != null && tag.getLocationTagId() != null && currentTagIds.contains(tag.getLocationTagId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<Integer, BigDecimal> buildAvailableByItemId(PharmacyLocation activePharmacy, InventoryService inventoryService) {
        Map<Integer, BigDecimal> availableByItemId = new LinkedHashMap<Integer, BigDecimal>();
        if (activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return availableByItemId;
        }
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId())) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            BigDecimal existing = availableByItemId.containsKey(itemId) ? availableByItemId.get(itemId) : BigDecimal.ZERO;
            availableByItemId.put(itemId, existing.add(balance.getQuantityOnHand()));
        }
        return availableByItemId;
    }
}
