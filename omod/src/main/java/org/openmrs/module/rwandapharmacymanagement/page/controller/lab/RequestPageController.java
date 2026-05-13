package org.openmrs.module.rwandapharmacymanagement.page.controller.lab;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.LabSupplyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.LabSupplyRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller("labSupplyRequestPageController")
public class RequestPageController {

    private static final int HISTORY_PAGE_SIZE = 10;

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("labSupplyService") LabSupplyService labSupplyService,
                    @RequestParam(value = "requested", required = false) Integer requested,
                    @RequestParam(value = "historyPage", required = false) Integer historyPage) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        User currentUser = Context.getAuthenticatedUser();
        List<Map<String, Object>> availableItems = buildAvailableItems(currentLocation, pharmacyService, inventoryService);
        int safeHistoryPage = historyPage == null || historyPage.intValue() < 1 ? 1 : historyPage.intValue();
        long requestHistoryTotalCount = currentUser == null ? 0L : labSupplyService.countRequestsByCreator(currentUser);
        int requestHistoryTotalPages = requestHistoryTotalCount == 0 ? 1 : (int) Math.ceil((double) requestHistoryTotalCount / HISTORY_PAGE_SIZE);
        if (safeHistoryPage > requestHistoryTotalPages) {
            safeHistoryPage = requestHistoryTotalPages;
        }
        model.addAttribute("currentLocation", currentLocation);
        model.addAttribute("availableItems", availableItems);
        model.addAttribute("labServices", getConfiguredLabServiceConcepts());
        model.addAttribute("requestHistory", currentUser == null ? Collections.emptyList()
                : labSupplyService.getRequestsByCreator(currentUser, (safeHistoryPage - 1) * HISTORY_PAGE_SIZE, HISTORY_PAGE_SIZE));
        model.addAttribute("historyPage", safeHistoryPage);
        model.addAttribute("historyPageSize", HISTORY_PAGE_SIZE);
        model.addAttribute("historyTotalCount", requestHistoryTotalCount);
        model.addAttribute("historyTotalPages", requestHistoryTotalPages);
        model.addAttribute("requested", requested != null && requested.intValue() == 1);
    }

    public String post(@SpringBean("labSupplyService") LabSupplyService labSupplyService,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "labServiceConceptId", required = false) Integer[] labServiceConceptIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       UiUtils ui) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        labSupplyService.createRequest(currentLocation, buildCommands(itemIds, labServiceConceptIds, quantities));
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "lab/request") + "?requested=1";
    }

    private List<LabSupplyRequestLineCommand> buildCommands(Integer[] itemIds, Integer[] labServiceConceptIds, BigDecimal[] quantities) {
        List<LabSupplyRequestLineCommand> commands = new ArrayList<LabSupplyRequestLineCommand>();
        if (itemIds == null) {
            return commands;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            Integer labServiceConceptId = labServiceConceptIds != null && labServiceConceptIds.length > i ? labServiceConceptIds[i] : null;
            BigDecimal quantity = quantities != null && quantities.length > i ? quantities[i] : null;
            if (itemId == null || labServiceConceptId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LabSupplyRequestLineCommand command = new LabSupplyRequestLineCommand();
            command.setItemId(itemId);
            command.setLabServiceConceptId(labServiceConceptId);
            command.setQuantity(quantity);
            commands.add(command);
        }
        return commands;
    }

    private List<Map<String, Object>> buildAvailableItems(Location currentLocation,
                                                          PharmacyService pharmacyService,
                                                          InventoryService inventoryService) {
        if (currentLocation == null) {
            return Collections.emptyList();
        }
        List<PharmacyLocation> candidatePharmacies = CurrentPharmacySupport.getCandidatePharmaciesForLocation(pharmacyService, currentLocation);
        Set<Integer> pharmacyIds = new HashSet<Integer>();
        for (PharmacyLocation pharmacyLocation : candidatePharmacies) {
            if (pharmacyLocation != null && pharmacyLocation.getPharmacyLocationId() != null) {
                pharmacyIds.add(pharmacyLocation.getPharmacyLocationId());
            }
        }
        Map<Integer, Map<String, Object>> rowsByItemId = new LinkedHashMap<Integer, Map<String, Object>>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            if (balance == null || balance.getPharmacyLocation() == null || balance.getPharmacyLocation().getPharmacyLocationId() == null
                    || !pharmacyIds.contains(balance.getPharmacyLocation().getPharmacyLocationId())
                    || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getItem().getDrug() != null
                    || !Boolean.TRUE.equals(balance.getItem().getStockable())
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            Map<String, Object> row = rowsByItemId.get(itemId);
            if (row == null) {
                row = new LinkedHashMap<String, Object>();
                row.put("itemId", itemId);
                row.put("name", balance.getItem().getName());
                row.put("code", balance.getItem().getCode());
                row.put("available", BigDecimal.ZERO);
                rowsByItemId.put(itemId, row);
            }
            row.put("available", ((BigDecimal) row.get("available")).add(balance.getQuantityOnHand()));
        }
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(rowsByItemId.values());
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                String leftName = left == null || left.get("name") == null ? "" : left.get("name").toString();
                String rightName = right == null || right.get("name") == null ? "" : right.get("name").toString();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return rows;
    }

    private List<Concept> getConfiguredLabServiceConcepts() {
        Map<String, Concept> servicesByKey = new LinkedHashMap<String, Concept>();
        String configuredConcept = Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_LAB_SERVICE_CONCEPT_SET);
        Concept serviceQuestion = resolveConcept(configuredConcept);
        if (serviceQuestion != null) {
            if (serviceQuestion.getSetMembers() != null) {
                for (Concept member : serviceQuestion.getSetMembers()) {
                    addUniqueConcept(servicesByKey, member);
                }
            }
            if (serviceQuestion.getAnswers() != null) {
                for (ConceptAnswer answer : serviceQuestion.getAnswers()) {
                    if (answer != null) {
                        addUniqueConcept(servicesByKey, answer.getAnswerConcept());
                    }
                }
            }
        }
        return sortConcepts(new ArrayList<Concept>(servicesByKey.values()));
    }

    private void addUniqueConcept(Map<String, Concept> conceptsByKey, Concept concept) {
        if (concept == null) {
            return;
        }
        String key = concept.getConceptId() == null ? concept.getUuid() : concept.getConceptId().toString();
        if (key != null && !conceptsByKey.containsKey(key)) {
            conceptsByKey.put(key, concept);
        }
    }

    private List<Concept> sortConcepts(List<Concept> concepts) {
        Collections.sort(concepts, new Comparator<Concept>() {
            @Override
            public int compare(Concept left, Concept right) {
                String leftName = left == null || left.getDisplayString() == null ? "" : left.getDisplayString();
                String rightName = right == null || right.getDisplayString() == null ? "" : right.getDisplayString();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return concepts;
    }

    private Concept resolveConcept(String conceptIdentifier) {
        if (conceptIdentifier == null || conceptIdentifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = conceptIdentifier.trim();
        Concept concept = Context.getConceptService().getConceptByUuid(trimmed);
        if (concept != null) {
            return concept;
        }
        try {
            return Context.getConceptService().getConcept(Integer.valueOf(trimmed));
        }
        catch (NumberFormatException ignored) {
            return Context.getConceptService().getConceptByName(trimmed);
        }
    }
}
