package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller("departmentBulkFinancePageController")
public class FinancePageController {

    public void get(PageModel model,
                    @SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                    @SpringBean("inventoryService") InventoryService inventoryService) {
        List<DepartmentBulkRequest> submittedRequests = departmentBulkRequestService.getRequestsByStatus(
                DepartmentBulkRequestService.STATUS_SUBMITTED, 0, 200);
        model.addAttribute("submittedRequests", submittedRequests);
        model.addAttribute("correctionRequests", departmentBulkRequestService.getRequestsByStatus(
                DepartmentBulkRequestService.STATUS_CORRECTION_REQUESTED, 0, 200));
        model.addAttribute("servingAvailabilityByLineId", buildServingAvailabilityByLineId(inventoryService, submittedRequests));
    }

    public String post(@SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam("action") String action,
                       @RequestParam(value = "removeLineId", required = false) Integer removeLineId,
                       @RequestParam(value = "lineId", required = false) Integer[] lineIds,
                       @RequestParam(value = "approvedQuantity", required = false) BigDecimal[] approvedQuantities,
                       @RequestParam(value = "note", required = false) String note,
                       @RequestParam(value = "lineVoidReason", required = false) String lineVoidReason,
                       UiUtils ui) {
        if ("removeLine".equalsIgnoreCase(action)) {
            departmentBulkRequestService.removeRequestLine(requestId, removeLineId,
                    lineVoidReason == null || lineVoidReason.trim().isEmpty() ? note : lineVoidReason);
        } else if ("reject".equalsIgnoreCase(action)) {
            departmentBulkRequestService.rejectRequest(requestId, note);
        } else if ("correction".equalsIgnoreCase(action)) {
            departmentBulkRequestService.requestCorrection(requestId, note);
        } else {
            departmentBulkRequestService.financeApproveRequest(requestId, lineIds, approvedQuantities);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "department/finance");
    }

    private Map<Integer, Map<String, Object>> buildServingAvailabilityByLineId(InventoryService inventoryService,
                                                                               List<DepartmentBulkRequest> requests) {
        Map<Integer, Map<String, Object>> availabilityByLineId = new LinkedHashMap<Integer, Map<String, Object>>();
        Map<Integer, Map<Integer, BigDecimal>> quantitiesByPharmacyId = new LinkedHashMap<Integer, Map<Integer, BigDecimal>>();
        if (requests == null) {
            return availabilityByLineId;
        }
        for (DepartmentBulkRequest request : requests) {
            if (request == null || request.getSupplyingPharmacyLocation() == null
                    || request.getSupplyingPharmacyLocation().getPharmacyLocationId() == null
                    || request.getLines() == null) {
                continue;
            }
            Integer servingPharmacyId = request.getSupplyingPharmacyLocation().getPharmacyLocationId();
            Map<Integer, BigDecimal> quantitiesByItemId = quantitiesByPharmacyId.get(servingPharmacyId);
            if (quantitiesByItemId == null) {
                quantitiesByItemId = buildQuantityByItemId(inventoryService, servingPharmacyId);
                quantitiesByPharmacyId.put(servingPharmacyId, quantitiesByItemId);
            }
            for (DepartmentBulkRequestLine line : request.getLines()) {
                if (line == null || line.getDepartmentBulkRequestLineId() == null || line.getItem() == null
                        || line.getItem().getItemId() == null) {
                    continue;
                }
                BigDecimal available = quantitiesByItemId.containsKey(line.getItem().getItemId())
                        ? quantitiesByItemId.get(line.getItem().getItemId()) : BigDecimal.ZERO;
                BigDecimal requested = line.getRequestedQuantity() == null ? BigDecimal.ZERO : line.getRequestedQuantity();
                Map<String, Object> values = new LinkedHashMap<String, Object>();
                values.put("quantity", formatQuantity(available));
                values.put("insufficient", Boolean.valueOf(available.compareTo(requested) < 0));
                availabilityByLineId.put(line.getDepartmentBulkRequestLineId(), values);
            }
        }
        return availabilityByLineId;
    }

    private Map<Integer, BigDecimal> buildQuantityByItemId(InventoryService inventoryService, Integer pharmacyLocationId) {
        Map<Integer, BigDecimal> quantityByItemId = new LinkedHashMap<Integer, BigDecimal>();
        List<InventoryBalance> balances = inventoryService.getInventoryForPharmacy(pharmacyLocationId);
        if (balances == null) {
            return quantityByItemId;
        }
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            BigDecimal existing = quantityByItemId.containsKey(itemId) ? quantityByItemId.get(itemId) : BigDecimal.ZERO;
            quantityByItemId.put(itemId, existing.add(balance.getQuantityOnHand() == null
                    ? BigDecimal.ZERO : balance.getQuantityOnHand()));
        }
        return quantityByItemId;
    }

    private String formatQuantity(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }
}
