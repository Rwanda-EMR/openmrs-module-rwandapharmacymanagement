package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentStockReturnService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentBulkRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentStockIssueLineCommand;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentStockReturnLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockIssue;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller("departmentStorePageController")
public class StorePageController {
    private static final String SESSION_SELECTED_DEPARTMENT = "rpm.departmentStore.selectedDepartment";

    public void get(PageModel model,
                    @SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                    @SpringBean("departmentStockReturnService") DepartmentStockReturnService departmentStockReturnService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "departmentLocationId", required = false) Integer departmentLocationId,
                    HttpSession session) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_DEPARTMENT_STORE);
        List<PharmacyLocation> departmentCandidates = currentDepartmentStores(pharmacyService);
        PharmacyLocation department = selectedDepartmentStore(departmentCandidates, departmentLocationId, session);
        List<PharmacyLocation> supplyingPharmacies = supplyingPharmacies(pharmacyService);
        List<InventoryBalance> stockBalances = department == null ? Collections.<InventoryBalance>emptyList()
                : positiveBalances(inventoryService.getInventoryForPharmacy(department.getPharmacyLocationId()));
        List<DepartmentBulkRequest> departmentRequests = department == null ? Collections.<DepartmentBulkRequest>emptyList()
                : departmentBulkRequestService.getRequestsForDepartment(department, 0, 25);
        List<DepartmentStockReturn> departmentReturns = department == null ? Collections.<DepartmentStockReturn>emptyList()
                : departmentStockReturnService.getReturnsForDepartment(department, 0, 25);
        String workflowMode = Context.getAdministrationService().getGlobalProperty(
                DepartmentBulkRequestService.GP_WORKFLOW_MODE,
                DepartmentBulkRequestService.MODE_PERIODIC_ACCOUNTABILITY);
        model.addAttribute("department", department);
        model.addAttribute("departmentCandidates", departmentCandidates);
        model.addAttribute("departmentSelectionRequired", department == null && departmentCandidates.size() > 1);
        model.addAttribute("supplyingPharmacies", supplyingPharmacies);
        model.addAttribute("stockByPharmacy", buildStockByPharmacy(supplyingPharmacies, inventoryService));
        model.addAttribute("accountabilityByItem", department == null ? Collections.<Integer, Map<String, Object>>emptyMap()
                : buildAccountabilityByItem(departmentBulkRequestService, department,
                DepartmentBulkRequestService.MODE_FULL_VISIBILITY.equals(workflowMode)));
        model.addAttribute("stockBalances", stockBalances);
        model.addAttribute("departmentRequests", departmentRequests);
        model.addAttribute("departmentReturns", departmentReturns);
        model.addAttribute("recentIssues", department == null ? Collections.emptyList()
                : departmentBulkRequestService.getIssuesForDepartment(department, 0, 25));
        model.addAttribute("workflowMode", workflowMode);
        model.addAttribute("pageTitle", "Department Store");
    }

    public String post(@SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                       @SpringBean("departmentStockReturnService") DepartmentStockReturnService departmentStockReturnService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("action") String action,
                       @RequestParam(value = "supplyingPharmacyLocationId", required = false) Integer supplyingPharmacyLocationId,
                       @RequestParam(value = "justification", required = false) String justification,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "previousRequestedQuantity", required = false) BigDecimal[] previousRequestedQuantities,
                       @RequestParam(value = "previousReceivedQuantity", required = false) BigDecimal[] previousReceivedQuantities,
                       @RequestParam(value = "previousUsedQuantity", required = false) BigDecimal[] previousUsedQuantities,
                       @RequestParam(value = "currentRemainingQuantity", required = false) BigDecimal[] currentRemainingQuantities,
                       @RequestParam(value = "requestedQuantity", required = false) BigDecimal[] requestedQuantities,
                       @RequestParam(value = "lineJustification", required = false) String[] lineJustifications,
                       @RequestParam(value = "issueItemId", required = false) Integer[] issueItemIds,
                       @RequestParam(value = "issueQuantity", required = false) BigDecimal[] issueQuantities,
                       @RequestParam(value = "issueBatchNumber", required = false) String[] issueBatchNumbers,
                       @RequestParam(value = "issuedTo", required = false) String issuedTo,
                       @RequestParam(value = "reason", required = false) String reason,
                       @RequestParam(value = "returnDestinationPharmacyLocationId", required = false) Integer returnDestinationPharmacyLocationId,
                       @RequestParam(value = "returnItemBatch", required = false) String[] returnItemBatches,
                       @RequestParam(value = "returnQuantity", required = false) BigDecimal[] returnQuantities,
                       @RequestParam(value = "returnReason", required = false) String returnReason,
                       @RequestParam(value = "departmentLocationId", required = false) Integer departmentLocationId,
                       HttpSession session,
                       UiUtils ui) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_DEPARTMENT_STORE);
        List<PharmacyLocation> departmentCandidates = currentDepartmentStores(pharmacyService);
        PharmacyLocation department = selectedDepartmentStore(departmentCandidates, departmentLocationId, session);
        if (department == null) {
            if (departmentCandidates.size() > 1) {
                throw new IllegalArgumentException("Choose the department store before submitting");
            }
            throw new IllegalArgumentException("Current OpenMRS location is not configured as a department store");
        }
        if ("request".equalsIgnoreCase(action)) {
            departmentBulkRequestService.createRequest(department, pharmacyService.getPharmacyLocation(supplyingPharmacyLocationId),
                    justification, requestCommands(itemIds, previousRequestedQuantities, previousReceivedQuantities,
                            previousUsedQuantities, currentRemainingQuantities, requestedQuantities, lineJustifications));
        } else if ("issue".equalsIgnoreCase(action)) {
            departmentBulkRequestService.recordIssues(department,
                    issueCommands(issueItemIds, issueQuantities, issueBatchNumbers), issuedTo, reason);
        } else if ("return".equalsIgnoreCase(action)) {
            departmentStockReturnService.createReturn(department,
                    pharmacyService.getPharmacyLocation(returnDestinationPharmacyLocationId), returnReason,
                    returnCommands(returnItemBatches, returnQuantities));
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "department/store");
    }

    private PharmacyLocation selectedDepartmentStore(List<PharmacyLocation> departmentCandidates,
                                                     Integer requestedDepartmentLocationId,
                                                     HttpSession session) {
        if (departmentCandidates == null || departmentCandidates.isEmpty()) {
            return null;
        }
        if (requestedDepartmentLocationId != null) {
            PharmacyLocation selected = findCandidate(departmentCandidates, requestedDepartmentLocationId);
            if (selected == null) {
                throw new IllegalArgumentException("Selected department store does not match your current OpenMRS location");
            }
            rememberDepartment(session, requestedDepartmentLocationId);
            return selected;
        }
        Integer sessionDepartmentLocationId = session == null ? null : (Integer) session.getAttribute(SESSION_SELECTED_DEPARTMENT);
        if (sessionDepartmentLocationId != null) {
            PharmacyLocation selected = findCandidate(departmentCandidates, sessionDepartmentLocationId);
            if (selected != null) {
                return selected;
            }
            session.removeAttribute(SESSION_SELECTED_DEPARTMENT);
        }
        if (departmentCandidates.size() == 1) {
            return departmentCandidates.get(0);
        }
        return null;
    }

    private PharmacyLocation findCandidate(List<PharmacyLocation> departmentCandidates, Integer departmentLocationId) {
        for (PharmacyLocation departmentCandidate : departmentCandidates) {
            if (departmentCandidate != null && departmentLocationId.equals(departmentCandidate.getPharmacyLocationId())) {
                return departmentCandidate;
            }
        }
        return null;
    }

    private void rememberDepartment(HttpSession session, Integer departmentLocationId) {
        if (session != null && departmentLocationId != null) {
            session.setAttribute(SESSION_SELECTED_DEPARTMENT, departmentLocationId);
        }
    }

    private List<PharmacyLocation> currentDepartmentStores(PharmacyService pharmacyService) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        List<PharmacyLocation> directMatches = new ArrayList<PharmacyLocation>();
        List<PharmacyLocation> tagMatches = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation pharmacyLocation : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacyLocation == null || Boolean.FALSE.equals(pharmacyLocation.getActive())
                    || pharmacyLocation.getLocationType() != PharmacyLocationType.DEPARTMENT_STORE) {
                continue;
            }
            if (matchesExactOpenmrsLocation(pharmacyLocation, currentLocation)) {
                directMatches.add(pharmacyLocation);
            } else if (matchesOpenmrsLocationTag(pharmacyLocation, currentLocation)) {
                tagMatches.add(pharmacyLocation);
            }
        }
        if (!directMatches.isEmpty()) {
            sortLocations(directMatches);
            return directMatches;
        }
        sortLocations(tagMatches);
        return tagMatches;
    }

    private List<PharmacyLocation> supplyingPharmacies(PharmacyService pharmacyService) {
        List<PharmacyLocation> locations = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation location : pharmacyService.getAllPharmacyLocations()) {
            if (location != null && !Boolean.FALSE.equals(location.getActive())
                    && location.getLocationType() != PharmacyLocationType.DEPARTMENT_STORE) {
                locations.add(location);
            }
        }
        sortLocations(locations);
        return locations;
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
            sortRowsByName(itemRows);
            stockByPharmacy.put(pharmacy.getPharmacyLocationId(), itemRows);
        }
        return stockByPharmacy;
    }

    private Map<Integer, Map<String, Object>> buildAccountabilityByItem(
            DepartmentBulkRequestService departmentBulkRequestService, PharmacyLocation department, boolean fullVisibilityMode) {
        Map<Integer, Map<String, Object>> valuesByItem = new LinkedHashMap<Integer, Map<String, Object>>();
        List<DepartmentBulkRequest> requests = departmentBulkRequestService.getRequestsForDepartment(department, 0, 100);
        List<DepartmentStockIssue> issues = fullVisibilityMode
                ? departmentBulkRequestService.getIssuesForDepartment(department, 0, 500)
                : Collections.<DepartmentStockIssue>emptyList();
        if (requests == null) {
            return valuesByItem;
        }
        for (DepartmentBulkRequest request : requests) {
            if (request == null || request.getLines() == null) {
                continue;
            }
            for (org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine line : request.getLines()) {
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
        return valuesByItem;
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

    private List<InventoryBalance> positiveBalances(List<InventoryBalance> balances) {
        List<InventoryBalance> filtered = new ArrayList<InventoryBalance>();
        if (balances != null) {
            for (InventoryBalance balance : balances) {
                if (balance != null && balance.getQuantityOnHand() != null
                        && balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0) {
                    filtered.add(balance);
                }
            }
        }
        Collections.sort(filtered, new Comparator<InventoryBalance>() {
            @Override
            public int compare(InventoryBalance left, InventoryBalance right) {
                String leftName = left == null || left.getItem() == null || left.getItem().getName() == null ? "" : left.getItem().getName();
                String rightName = right == null || right.getItem() == null || right.getItem().getName() == null ? "" : right.getItem().getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return filtered;
    }

    private List<DepartmentBulkRequestLineCommand> requestCommands(Integer[] itemIds,
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

    private List<DepartmentStockIssueLineCommand> issueCommands(Integer[] itemIds, BigDecimal[] quantities,
                                                                String[] batchNumbers) {
        List<DepartmentStockIssueLineCommand> commands = new ArrayList<DepartmentStockIssueLineCommand>();
        if (itemIds == null || quantities == null) {
            return commands;
        }
        int count = Math.min(itemIds.length, quantities.length);
        for (int i = 0; i < count; i++) {
            String batchNumber = batchNumbers == null || batchNumbers.length <= i ? null : batchNumbers[i];
            commands.add(new DepartmentStockIssueLineCommand(itemIds[i], quantities[i], batchNumber));
        }
        return commands;
    }

    private List<DepartmentStockReturnLineCommand> returnCommands(String[] itemBatches, BigDecimal[] quantities) {
        List<DepartmentStockReturnLineCommand> commands = new ArrayList<DepartmentStockReturnLineCommand>();
        if (itemBatches == null || quantities == null) {
            return commands;
        }
        int count = Math.min(itemBatches.length, quantities.length);
        for (int i = 0; i < count; i++) {
            Integer itemId = itemIdFromItemBatch(itemBatches[i]);
            if (itemId == null || quantities[i] == null || quantities[i].compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            commands.add(new DepartmentStockReturnLineCommand(itemId, quantities[i], batchFromItemBatch(itemBatches[i])));
        }
        return commands;
    }

    private Integer itemIdFromItemBatch(String itemBatch) {
        if (itemBatch == null || itemBatch.trim().isEmpty()) {
            return null;
        }
        String[] parts = itemBatch.split("\\|", 2);
        try {
            return Integer.valueOf(parts[0]);
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    private String batchFromItemBatch(String itemBatch) {
        if (itemBatch == null) {
            return null;
        }
        String[] parts = itemBatch.split("\\|", 2);
        return parts.length < 2 || parts[1].trim().isEmpty() ? null : parts[1].trim();
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

    private void sortRowsByName(List<Map<String, Object>> rows) {
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                String leftName = left == null || left.get("name") == null ? "" : left.get("name").toString();
                String rightName = right == null || right.get("name") == null ? "" : right.get("name").toString();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
    }

    private boolean matchesExactOpenmrsLocation(PharmacyLocation pharmacyLocation, Location location) {
        if (pharmacyLocation == null || location == null || location.getLocationId() == null) {
            return false;
        }
        return pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null
                && pharmacyLocation.getLocation().getLocationId().equals(location.getLocationId());
    }

    private boolean matchesOpenmrsLocationTag(PharmacyLocation pharmacyLocation, Location location) {
        if (pharmacyLocation == null || location == null || location.getLocationId() == null) {
            return false;
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
