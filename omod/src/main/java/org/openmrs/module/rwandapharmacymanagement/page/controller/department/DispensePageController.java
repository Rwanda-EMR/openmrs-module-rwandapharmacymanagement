package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller("departmentBulkDispensePageController")
public class DispensePageController {

    public void get(PageModel model,
                    @SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                    @SpringBean("inventoryService") InventoryService inventoryService) {
        List<DepartmentBulkRequest> requests = departmentBulkRequestService.getRequestsByStatus(
                DepartmentBulkRequestService.STATUS_FINANCE_APPROVED, 0, 200);
        requests = sourceAccessibleRequests(requests);
        model.addAttribute("requests", requests);
        model.addAttribute("batchesByLineId", batchesByLineId(requests, inventoryService));
    }

    public String post(@SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "removeLineId", required = false) Integer removeLineId,
                       @RequestParam(value = "lineVoidReason", required = false) String lineVoidReason,
                       @RequestParam(value = "lineId", required = false) Integer[] lineIds,
                       @RequestParam(value = "dispensedQuantity", required = false) BigDecimal[] dispensedQuantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       UiUtils ui) {
        requireCurrentLocationCanDispense(departmentBulkRequestService.getRequest(requestId));
        if ("removeLine".equalsIgnoreCase(action)) {
            departmentBulkRequestService.removeRequestLine(requestId, removeLineId, lineVoidReason);
        } else {
            departmentBulkRequestService.dispenseRequest(requestId, lineIds, dispensedQuantities, batchNumbers);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "department/dispense");
    }

    private List<DepartmentBulkRequest> sourceAccessibleRequests(List<DepartmentBulkRequest> requests) {
        List<DepartmentBulkRequest> accessible = new ArrayList<DepartmentBulkRequest>();
        if (requests == null) {
            return accessible;
        }
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        for (DepartmentBulkRequest request : requests) {
            if (request != null && canCurrentLocationDispense(request.getSupplyingPharmacyLocation(), currentLocation)) {
                accessible.add(request);
            }
        }
        return accessible;
    }

    private void requireCurrentLocationCanDispense(DepartmentBulkRequest request) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        if (request == null || !canCurrentLocationDispense(request.getSupplyingPharmacyLocation(), currentLocation)) {
            throw new IllegalArgumentException("Only users at the supplying pharmacy location can dispense this department request");
        }
    }

    private boolean canCurrentLocationDispense(PharmacyLocation supplyingPharmacyLocation, Location currentLocation) {
        if (!hasOpenmrsLocationBinding(supplyingPharmacyLocation)) {
            return true;
        }
        return matchesOpenmrsLocation(supplyingPharmacyLocation, currentLocation);
    }

    private boolean hasOpenmrsLocationBinding(PharmacyLocation pharmacyLocation) {
        return pharmacyLocation != null
                && ((pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null)
                || (pharmacyLocation.getLocationTag() != null && pharmacyLocation.getLocationTag().getLocationTagId() != null)
                || (pharmacyLocation.getLocationTags() != null && !pharmacyLocation.getLocationTags().isEmpty()));
    }

    private boolean matchesOpenmrsLocation(PharmacyLocation pharmacyLocation, Location location) {
        if (pharmacyLocation == null || location == null || location.getLocationId() == null) {
            return false;
        }
        if (pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null
                && pharmacyLocation.getLocation().getLocationId().equals(location.getLocationId())) {
            return true;
        }
        if (location.getTags() == null || location.getTags().isEmpty()) {
            return false;
        }
        Set<Integer> currentTagIds = new HashSet<Integer>();
        for (LocationTag tag : location.getTags()) {
            if (tag != null && tag.getLocationTagId() != null) {
                currentTagIds.add(tag.getLocationTagId());
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

    private Map<Integer, List<Map<String, Object>>> batchesByLineId(List<DepartmentBulkRequest> requests,
                                                                    InventoryService inventoryService) {
        Map<Integer, List<Map<String, Object>>> batchesByLineId = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        if (requests == null) {
            return batchesByLineId;
        }
        for (DepartmentBulkRequest request : requests) {
            if (request == null || request.getSupplyingPharmacyLocation() == null
                    || request.getSupplyingPharmacyLocation().getPharmacyLocationId() == null
                    || request.getLines() == null) {
                continue;
            }
            List<InventoryBalance> inventory = inventoryService.getAvailableInventoryForPharmacy(
                    request.getSupplyingPharmacyLocation().getPharmacyLocationId());
            for (DepartmentBulkRequestLine line : request.getLines()) {
                if (line == null || Boolean.TRUE.equals(line.getVoided()) || line.getDepartmentBulkRequestLineId() == null
                        || line.getItem() == null || line.getItem().getItemId() == null) {
                    continue;
                }
                List<Map<String, Object>> batches = batchesForItem(inventory, line.getItem().getItemId());
                batchesByLineId.put(line.getDepartmentBulkRequestLineId(), batches);
            }
        }
        return batchesByLineId;
    }

    private List<Map<String, Object>> batchesForItem(List<InventoryBalance> inventory, Integer itemId) {
        List<Map<String, Object>> batches = new ArrayList<Map<String, Object>>();
        if (inventory == null) {
            return batches;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (InventoryBalance balance : inventory) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || !itemId.equals(balance.getItem().getItemId()) || balance.getQuantityOnHand() == null
                    || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Map<String, Object> batch = new LinkedHashMap<String, Object>();
            batch.put("batchNumber", balance.getBatchNumber());
            batch.put("available", balance.getQuantityOnHand());
            batch.put("expiryDate", balance.getExpiryDate() == null ? "" : dateFormat.format(balance.getExpiryDate()));
            batches.add(batch);
        }
        Collections.sort(batches, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                String leftExpiry = left == null || left.get("expiryDate") == null ? "" : left.get("expiryDate").toString();
                String rightExpiry = right == null || right.get("expiryDate") == null ? "" : right.get("expiryDate").toString();
                int expiryComparison = leftExpiry.compareTo(rightExpiry);
                if (expiryComparison != 0) {
                    return expiryComparison;
                }
                String leftBatch = left == null || left.get("batchNumber") == null ? "" : left.get("batchNumber").toString();
                String rightBatch = right == null || right.get("batchNumber") == null ? "" : right.get("batchNumber").toString();
                return leftBatch.compareToIgnoreCase(rightBatch);
            }
        });
        return batches;
    }
}
