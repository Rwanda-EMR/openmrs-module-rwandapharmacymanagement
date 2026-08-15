package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentStockIssueLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller("departmentStockIssuePageController")
public class IssuePageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "departmentLocationId", required = false) Integer departmentLocationId) {
        PharmacyLocation selectedDepartment = departmentLocationId == null ? null : pharmacyService.getPharmacyLocation(departmentLocationId);
        model.addAttribute("departmentLocations", departmentLocations(pharmacyService));
        model.addAttribute("selectedDepartment", selectedDepartment);
        model.addAttribute("workflowMode", Context.getAdministrationService().getGlobalProperty(
                DepartmentBulkRequestService.GP_WORKFLOW_MODE,
                DepartmentBulkRequestService.MODE_PERIODIC_ACCOUNTABILITY));
        model.addAttribute("stockBalances", selectedDepartment == null ? Collections.<InventoryBalance>emptyList()
                : positiveBalances(inventoryService.getInventoryForPharmacy(selectedDepartment.getPharmacyLocationId())));
    }

    public String post(@SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("departmentLocationId") Integer departmentLocationId,
                       @RequestParam("itemId") Integer[] itemIds,
                       @RequestParam("quantity") BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "issuedTo", required = false) String issuedTo,
                       @RequestParam(value = "reason", required = false) String reason,
                       UiUtils ui) {
        departmentBulkRequestService.recordIssues(pharmacyService.getPharmacyLocation(departmentLocationId),
                issueCommands(itemIds, quantities, batchNumbers), issuedTo, reason);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("departmentLocationId", departmentLocationId);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "department/issue",
                params);
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

    private List<PharmacyLocation> departmentLocations(PharmacyService pharmacyService) {
        List<PharmacyLocation> locations = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation location : pharmacyService.getAllPharmacyLocations()) {
            if (location != null && Boolean.TRUE.equals(location.getActive())
                    && location.getLocationType() == PharmacyLocationType.DEPARTMENT_STORE) {
                locations.add(location);
            }
        }
        return locations;
    }
}
