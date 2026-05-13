package org.openmrs.module.rwandapharmacymanagement.page.controller.lab;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.LabSupplyService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequest;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller("labSupplyQueuePageController")
public class QueuePageController {

    public void get(PageModel model,
                    @SpringBean("labSupplyService") LabSupplyService labSupplyService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "requestId", required = false) Integer requestId,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "approved", required = false) Integer approved,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        List<LabSupplyRequest> visibleRequests = activePharmacy == null
                ? Collections.<LabSupplyRequest>emptyList()
                : filterVisibleRequests(labSupplyService.getPendingRequests(), activePharmacy, pharmacyService);
        LabSupplyRequest selectedRequest = requestId == null ? null : labSupplyService.getRequest(requestId);
        if (selectedRequest != null && (activePharmacy == null || !isVisibleToPharmacy(selectedRequest, activePharmacy, pharmacyService))) {
            selectedRequest = null;
        }
        model.addAttribute("requests", visibleRequests);
        model.addAttribute("selectedRequest", selectedRequest);
        model.addAttribute("approved", approved != null && approved.intValue() == 1);
        model.addAttribute("availableByItemId", buildAvailableByItemId(activePharmacy, inventoryService));
        model.addAttribute("waitTimeByRequestId", buildWaitTimeMap(visibleRequests));
    }

    public String post(@SpringBean("labSupplyService") LabSupplyService labSupplyService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam(value = "lineId", required = false) Integer[] lineIds,
                       @RequestParam(value = "approvedQuantity", required = false) BigDecimal[] approvedQuantities,
                       HttpSession session,
                       UiUtils ui) {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        LabSupplyRequest request = labSupplyService.getRequest(requestId);
        if (!isVisibleToPharmacy(request, activePharmacy, pharmacyService)) {
            throw new IllegalArgumentException("Selected lab request is not available to the active dispensing pharmacy");
        }
        labSupplyService.approveRequest(requestId, activePharmacy, lineIds, approvedQuantities);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "lab/queue") + "?approved=1";
    }

    private List<LabSupplyRequest> filterVisibleRequests(List<LabSupplyRequest> requests,
                                                         PharmacyLocation activePharmacy,
                                                         PharmacyService pharmacyService) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<LabSupplyRequest> visible = new ArrayList<LabSupplyRequest>();
        for (LabSupplyRequest request : requests) {
            if (isVisibleToPharmacy(request, activePharmacy, pharmacyService)) {
                visible.add(request);
            }
        }
        return visible;
    }

    private boolean isVisibleToPharmacy(LabSupplyRequest request,
                                        PharmacyLocation activePharmacy,
                                        PharmacyService pharmacyService) {
        if (request == null || request.getLabUnit() == null || activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return false;
        }
        for (PharmacyLocation pharmacyLocation : CurrentPharmacySupport.getCandidatePharmaciesForLocation(pharmacyService, request.getLabUnit())) {
            if (pharmacyLocation != null && pharmacyLocation.getPharmacyLocationId() != null
                    && activePharmacy.getPharmacyLocationId().equals(pharmacyLocation.getPharmacyLocationId())) {
                return true;
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

    private Map<Integer, String> buildWaitTimeMap(List<LabSupplyRequest> requests) {
        Map<Integer, String> waitTimes = new LinkedHashMap<Integer, String>();
        if (requests == null) {
            return waitTimes;
        }
        Date now = new Date();
        for (LabSupplyRequest request : requests) {
            if (request != null && request.getLabSupplyRequestId() != null) {
                waitTimes.put(request.getLabSupplyRequestId(), formatWaitTime(request.getDateCreated(), now));
            }
        }
        return waitTimes;
    }

    private String formatWaitTime(Date created, Date now) {
        if (created == null || now == null || now.before(created)) {
            return "0 min";
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - created.getTime());
        if (minutes < 60L) {
            return minutes + " min";
        }
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        return remainingMinutes == 0L ? hours + " h" : hours + " h " + remainingMinutes + " min";
    }
}
