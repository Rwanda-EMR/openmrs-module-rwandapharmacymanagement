package org.openmrs.module.rwandapharmacymanagement.page.controller.pharmacyorder;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PharmacyItemRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
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

@Controller("pharmacyOrderRequestPageController")
public class RequestPageController {

    private static final int HISTORY_PAGE_SIZE = 10;

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                    @RequestParam(value = "requested", required = false) Integer requested,
                    @RequestParam(value = "historyPage", required = false) Integer historyPage,
                    @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        User currentUser = Context.getAuthenticatedUser();
        int safeHistoryPage = historyPage == null || historyPage.intValue() < 1 ? 1 : historyPage.intValue();
        long historyTotalCount = currentUser == null ? 0L : pharmacyItemRequestService.countRequestsByCreator(currentUser);
        int historyTotalPages = historyTotalCount == 0 ? 1 : (int) Math.ceil((double) historyTotalCount / HISTORY_PAGE_SIZE);
        if (safeHistoryPage > historyTotalPages) {
            safeHistoryPage = historyTotalPages;
        }
        List<PharmacyLocation> pharmacies = getActivePharmacies(pharmacyService);
        model.addAttribute("pharmacies", pharmacies);
        model.addAttribute("sourcePharmacies", pharmacies);
        model.addAttribute("stockByPharmacy", buildStockByPharmacy(pharmacies, inventoryService));
        model.addAttribute("requestHistory", currentUser == null ? Collections.emptyList()
                : pharmacyItemRequestService.getRequestsByCreator(currentUser, (safeHistoryPage - 1) * HISTORY_PAGE_SIZE, HISTORY_PAGE_SIZE));
        model.addAttribute("historyPage", safeHistoryPage);
        model.addAttribute("historyTotalCount", historyTotalCount);
        model.addAttribute("historyTotalPages", historyTotalPages);
        model.addAttribute("requested", requested != null && requested.intValue() == 1);
        model.addAttribute("errorMessage", errorMessage);
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                       @RequestParam("requestedPharmacyLocationId") Integer requestedPharmacyLocationId,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "linePharmacyLocationId", required = false) Integer[] linePharmacyLocationIds,
                       @RequestParam(value = "reason", required = false) String reason,
                       UiUtils ui) {
        try {
            PharmacyLocation requestedPharmacyLocation = pharmacyService.getPharmacyLocation(requestedPharmacyLocationId);
            if (requestedPharmacyLocation == null) {
                throw new IllegalArgumentException("Select one pharmacy for the request.");
            }
            List<PharmacyItemRequestLineCommand> commands = buildCommands(requestedPharmacyLocationId, itemIds, quantities,
                    linePharmacyLocationIds);
            if (commands.isEmpty()) {
                throw new IllegalArgumentException("Add at least one item before submitting the request.");
            }
            pharmacyItemRequestService.createRequest(requestedPharmacyLocation, reason, commands);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "pharmacyorder/request") + "?requested=1";
        }
        catch (IllegalArgumentException e) {
            java.util.Map<String, Object> params = new java.util.LinkedHashMap<String, Object>();
            params.put("errorMessage", e.getMessage());
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "pharmacyorder/request", params);
        }
    }

    private List<PharmacyItemRequestLineCommand> buildCommands(Integer requestedPharmacyLocationId, Integer[] itemIds,
                                                               BigDecimal[] quantities, Integer[] linePharmacyLocationIds) {
        List<PharmacyItemRequestLineCommand> commands = new ArrayList<PharmacyItemRequestLineCommand>();
        if (itemIds == null) {
            return commands;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            BigDecimal quantity = quantities != null && quantities.length > i ? quantities[i] : null;
            if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer linePharmacyLocationId = linePharmacyLocationIds != null && linePharmacyLocationIds.length > i
                    ? linePharmacyLocationIds[i] : null;
            if (linePharmacyLocationId == null || !linePharmacyLocationId.equals(requestedPharmacyLocationId)) {
                throw new IllegalArgumentException("One request can contain items for only one pharmacy. Remove mixed pharmacy items and submit again.");
            }
            PharmacyItemRequestLineCommand command = new PharmacyItemRequestLineCommand();
            command.setItemId(itemId);
            command.setQuantity(quantity);
            commands.add(command);
        }
        return commands;
    }

    private List<PharmacyLocation> getActivePharmacies(PharmacyService pharmacyService) {
        List<PharmacyLocation> pharmacies = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation pharmacy : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacy != null && Boolean.TRUE.equals(pharmacy.getActive()) && pharmacy.getPharmacyLocationId() != null) {
                pharmacies.add(pharmacy);
            }
        }
        Collections.sort(pharmacies, new Comparator<PharmacyLocation>() {
            @Override
            public int compare(PharmacyLocation left, PharmacyLocation right) {
                String leftName = left == null || left.getName() == null ? "" : left.getName();
                String rightName = right == null || right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return pharmacies;
    }

    private Map<Integer, List<Map<String, Object>>> buildStockByPharmacy(List<PharmacyLocation> pharmacies,
                                                                         InventoryService inventoryService) {
        Map<Integer, List<Map<String, Object>>> stockByPharmacy = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        for (PharmacyLocation pharmacy : pharmacies) {
            Map<Integer, Map<String, Object>> byItem = new LinkedHashMap<Integer, Map<String, Object>>();
            for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(pharmacy.getPharmacyLocationId())) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
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
            stockByPharmacy.put(pharmacy.getPharmacyLocationId(), new ArrayList<Map<String, Object>>(byItem.values()));
        }
        return stockByPharmacy;
    }
}
