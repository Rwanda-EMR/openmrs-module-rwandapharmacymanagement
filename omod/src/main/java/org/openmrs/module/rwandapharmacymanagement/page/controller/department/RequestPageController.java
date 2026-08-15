package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentBulkRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockIssue;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller("departmentBulkRequestPageController")
public class RequestPageController {

    public void get(PageModel model,
                    @SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService) {
        List<PharmacyLocation> departmentLocations = departmentLocations(pharmacyService);
        List<PharmacyLocation> supplyingPharmacies = supplyingPharmacies(pharmacyService);
        String workflowMode = Context.getAdministrationService().getGlobalProperty(
                DepartmentBulkRequestService.GP_WORKFLOW_MODE,
                DepartmentBulkRequestService.MODE_PERIODIC_ACCOUNTABILITY);
        model.addAttribute("departmentLocations", departmentLocations);
        model.addAttribute("supplyingPharmacies", supplyingPharmacies);
        model.addAttribute("stockByPharmacy", buildStockByPharmacy(supplyingPharmacies, inventoryService));
        model.addAttribute("accountabilityByDepartment", buildAccountabilityByDepartment(departmentBulkRequestService,
                departmentLocations, DepartmentBulkRequestService.MODE_FULL_VISIBILITY.equals(workflowMode)));
        model.addAttribute("workflowMode", workflowMode);
        model.addAttribute("recentRequests", departmentBulkRequestService.getRequestsByCreator(
                Context.getAuthenticatedUser(), 0, 20));
        model.addAttribute("pageTitle", "Department Bulk Item Request");
    }

    public String post(@SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("departmentLocationId") Integer departmentLocationId,
                       @RequestParam("supplyingPharmacyLocationId") Integer supplyingPharmacyLocationId,
                       @RequestParam(value = "justification", required = false) String justification,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "previousRequestedQuantity", required = false) BigDecimal[] previousRequestedQuantities,
                       @RequestParam(value = "previousReceivedQuantity", required = false) BigDecimal[] previousReceivedQuantities,
                       @RequestParam(value = "previousUsedQuantity", required = false) BigDecimal[] previousUsedQuantities,
                       @RequestParam(value = "currentRemainingQuantity", required = false) BigDecimal[] currentRemainingQuantities,
                       @RequestParam(value = "requestedQuantity", required = false) BigDecimal[] requestedQuantities,
                       @RequestParam(value = "lineJustification", required = false) String[] lineJustifications,
                       UiUtils ui) {
        departmentBulkRequestService.createRequest(pharmacyService.getPharmacyLocation(departmentLocationId),
                pharmacyService.getPharmacyLocation(supplyingPharmacyLocationId), justification,
                buildCommands(itemIds, previousRequestedQuantities, previousReceivedQuantities, previousUsedQuantities,
                        currentRemainingQuantities, requestedQuantities, lineJustifications));
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "department/request");
    }

    private List<DepartmentBulkRequestLineCommand> buildCommands(Integer[] itemIds,
                                                                 BigDecimal[] previousRequestedQuantities,
                                                                 BigDecimal[] previousReceivedQuantities,
                                                                 BigDecimal[] previousUsedQuantities,
                                                                 BigDecimal[] currentRemainingQuantities,
                                                                 BigDecimal[] requestedQuantities,
                                                                 String[] lineJustifications) {
        List<DepartmentBulkRequestLineCommand> commands = new ArrayList<DepartmentBulkRequestLineCommand>();
        if (itemIds == null) {
            return commands;
        }
        for (int i = 0; i < itemIds.length; i++) {
            if (itemIds[i] == null) {
                continue;
            }
            DepartmentBulkRequestLineCommand command = new DepartmentBulkRequestLineCommand();
            command.setItemId(itemIds[i]);
            command.setPreviousRequestedQuantity(valueAt(previousRequestedQuantities, i));
            command.setPreviousReceivedQuantity(valueAt(previousReceivedQuantities, i));
            command.setPreviousUsedQuantity(valueAt(previousUsedQuantities, i));
            command.setCurrentRemainingQuantity(valueAt(currentRemainingQuantities, i));
            command.setRequestedQuantity(valueAt(requestedQuantities, i));
            command.setLineJustification(valueAt(lineJustifications, i));
            commands.add(command);
        }
        return commands;
    }

    private List<PharmacyLocation> departmentLocations(PharmacyService pharmacyService) {
        List<PharmacyLocation> locations = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation location : pharmacyService.getAllPharmacyLocations()) {
            if (location != null && Boolean.TRUE.equals(location.getActive())
                    && location.getLocationType() == PharmacyLocationType.DEPARTMENT_STORE) {
                locations.add(location);
            }
        }
        sortLocations(locations);
        return locations;
    }

    private List<PharmacyLocation> supplyingPharmacies(PharmacyService pharmacyService) {
        List<PharmacyLocation> locations = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation location : pharmacyService.getAllPharmacyLocations()) {
            if (location != null && Boolean.TRUE.equals(location.getActive())
                    && location.getLocationType() != PharmacyLocationType.DEPARTMENT_STORE) {
                locations.add(location);
            }
        }
        sortLocations(locations);
        return locations;
    }

    private void sortLocations(List<PharmacyLocation> locations) {
        Collections.sort(locations, new Comparator<PharmacyLocation>() {
            @Override
            public int compare(PharmacyLocation left, PharmacyLocation right) {
                String leftName = left == null || left.getName() == null ? "" : left.getName();
                String rightName = right == null || right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
    }

    private Map<Integer, List<Map<String, Object>>> buildStockByPharmacy(List<PharmacyLocation> pharmacies,
                                                                         InventoryService inventoryService) {
        Map<Integer, List<Map<String, Object>>> stockByPharmacy = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        for (PharmacyLocation pharmacy : pharmacies) {
            Map<Integer, Map<String, Object>> byItem = new LinkedHashMap<Integer, Map<String, Object>>();
            for (InventoryBalance balance : inventoryService.getAvailableInventoryForPharmacy(pharmacy.getPharmacyLocationId())) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                        || !Boolean.TRUE.equals(balance.getItem().getVisibleToRequesters())
                        || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                Integer itemId = balance.getItem().getItemId();
                Map<String, Object> row = byItem.get(itemId);
                if (row == null) {
                    row = new LinkedHashMap<String, Object>();
                    row.put("itemId", itemId);
                    row.put("name", balance.getItem().getName());
                    row.put("available", BigDecimal.ZERO);
                    byItem.put(itemId, row);
                }
                row.put("available", ((BigDecimal) row.get("available")).add(balance.getQuantityOnHand()));
            }
            List<Map<String, Object>> itemRows = new ArrayList<Map<String, Object>>(byItem.values());
            Collections.sort(itemRows, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> left, Map<String, Object> right) {
                    String leftName = left == null || left.get("name") == null ? "" : left.get("name").toString();
                    String rightName = right == null || right.get("name") == null ? "" : right.get("name").toString();
                    return leftName.compareToIgnoreCase(rightName);
                }
            });
            stockByPharmacy.put(pharmacy.getPharmacyLocationId(), itemRows);
        }
        return stockByPharmacy;
    }

    private Map<Integer, Map<Integer, Map<String, Object>>> buildAccountabilityByDepartment(
            DepartmentBulkRequestService departmentBulkRequestService, List<PharmacyLocation> departments,
            boolean fullVisibilityMode) {
        Map<Integer, Map<Integer, Map<String, Object>>> valuesByDepartment = new LinkedHashMap<Integer, Map<Integer, Map<String, Object>>>();
        for (PharmacyLocation department : departments) {
            if (department == null || department.getPharmacyLocationId() == null) {
                continue;
            }
            Map<Integer, Map<String, Object>> valuesByItem = new LinkedHashMap<Integer, Map<String, Object>>();
            List<DepartmentBulkRequest> requests = departmentBulkRequestService.getRequestsForDepartment(department, 0, 100);
            List<DepartmentStockIssue> issues = fullVisibilityMode
                    ? departmentBulkRequestService.getIssuesForDepartment(department, 0, 500)
                    : Collections.<DepartmentStockIssue>emptyList();
            if (requests != null) {
                for (DepartmentBulkRequest request : requests) {
                    if (request == null || request.getLines() == null) {
                        continue;
                    }
                    for (DepartmentBulkRequestLine line : request.getLines()) {
                        if (line == null || line.getItem() == null || line.getItem().getItemId() == null
                                || valuesByItem.containsKey(line.getItem().getItemId())) {
                            continue;
                        }
                        Map<String, Object> values = new LinkedHashMap<String, Object>();
                        values.put("stockAtLastRequest", zeroIfNull(line.getCurrentRemainingQuantity()));
                        values.put("receivedSince", zeroIfNull(line.getDispensedQuantity()));
                        values.put("usedSince", fullVisibilityMode
                                ? issueQuantitySince(issues, line.getItem().getItemId(), request.getDateCreated())
                                : BigDecimal.ZERO);
                        valuesByItem.put(line.getItem().getItemId(), values);
                    }
                }
            }
            valuesByDepartment.put(department.getPharmacyLocationId(), valuesByItem);
        }
        return valuesByDepartment;
    }

    private BigDecimal issueQuantitySince(List<DepartmentStockIssue> issues, Integer itemId, Date since) {
        BigDecimal total = BigDecimal.ZERO;
        if (issues == null || itemId == null) {
            return total;
        }
        for (DepartmentStockIssue issue : issues) {
            if (issue == null || issue.getItem() == null || issue.getItem().getItemId() == null
                    || !itemId.equals(issue.getItem().getItemId()) || issue.getQuantity() == null
                    || Boolean.TRUE.equals(issue.getVoided())) {
                continue;
            }
            Date issueDate = issue.getDateCreated();
            if (since != null && issueDate != null && issueDate.before(since)) {
                continue;
            }
            total = total.add(issue.getQuantity());
        }
        return total;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal valueAt(BigDecimal[] values, int index) {
        return values == null || values.length <= index ? null : values[index];
    }

    private String valueAt(String[] values, int index) {
        return values == null || values.length <= index ? null : values[index];
    }
}
