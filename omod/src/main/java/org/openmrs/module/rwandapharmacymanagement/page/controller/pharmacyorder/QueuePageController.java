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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller("pharmacyOrderQueuePageController")
public class QueuePageController {
    private static final String FLASH_APPROVED = "rpm.pharmacyOrderQueue.approved";
    private static final String FLASH_REJECTED = "rpm.pharmacyOrderQueue.rejected";
    private static final String FLASH_LINE_REMOVED = "rpm.pharmacyOrderQueue.lineRemoved";
    private static final String FLASH_ERROR_MESSAGE = "rpm.pharmacyOrderQueue.errorMessage";

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
        model.addAttribute("batchOptionsByItemId", buildBatchOptionsByItemId(activePharmacy, inventoryService));
        model.addAttribute("approved", FlashMessageSupport.consumeBoolean(session, FLASH_APPROVED));
        model.addAttribute("rejected", FlashMessageSupport.consumeBoolean(session, FLASH_REJECTED));
        model.addAttribute("lineRemoved", FlashMessageSupport.consumeBoolean(session, FLASH_LINE_REMOVED));
        model.addAttribute("errorMessage", FlashMessageSupport.consumeString(session, FLASH_ERROR_MESSAGE));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "requestLineId", required = false) Integer requestLineId,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       @RequestParam(value = "lineVoidReason", required = false) String lineVoidReason,
                       @RequestParam(value = "lineId", required = false) Integer[] lineIds,
                       @RequestParam(value = "approvedQuantity", required = false) BigDecimal[] approvedQuantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       HttpSession session,
                       UiUtils ui) {
        PharmacyLocation activePharmacy = resolveActiveRequestPharmacy(pharmacyService, session);
        if (activePharmacy == null) {
            throw new IllegalArgumentException("No pharmacy is selected for the current session");
        }
        try {
            if ("reject".equalsIgnoreCase(action)) {
                pharmacyItemRequestService.rejectRequest(requestId, activePharmacy, rejectionReason);
                FlashMessageSupport.set(session, FLASH_REJECTED, Boolean.TRUE);
            } else if ("removeLine".equalsIgnoreCase(action)) {
                pharmacyItemRequestService.removeRequestLine(requestId, activePharmacy, requestLineId, lineVoidReason);
                FlashMessageSupport.set(session, FLASH_LINE_REMOVED, Boolean.TRUE);
            } else {
                pharmacyItemRequestService.approveRequest(requestId, activePharmacy, lineIds, approvedQuantities, batchNumbers);
                FlashMessageSupport.set(session, FLASH_APPROVED, Boolean.TRUE);
            }
        }
        catch (IllegalArgumentException e) {
            FlashMessageSupport.set(session, FLASH_ERROR_MESSAGE, e.getMessage());
        }
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

    private Map<Integer, List<Map<String, Object>>> buildBatchOptionsByItemId(PharmacyLocation activePharmacy, InventoryService inventoryService) {
        Map<Integer, List<Map<String, Object>>> batchOptionsByItemId = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        if (activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return batchOptionsByItemId;
        }
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId())) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            if (!batchOptionsByItemId.containsKey(itemId)) {
                batchOptionsByItemId.put(itemId, new ArrayList<Map<String, Object>>());
            }
            Map<String, Object> option = new LinkedHashMap<String, Object>();
            option.put("batchNumber", balance.getBatchNumber());
            option.put("batchValue", balance.getBatchNumber() == null || balance.getBatchNumber().trim().isEmpty()
                    ? "__NO_BATCH__" : balance.getBatchNumber());
            option.put("expiryDate", balance.getExpiryDate());
            option.put("available", balance.getQuantityOnHand());
            batchOptionsByItemId.get(itemId).add(option);
        }
        return batchOptionsByItemId;
    }
}
