package org.openmrs.module.rwandapharmacymanagement.page.controller.department;

import org.openmrs.api.context.Context;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentStockReturnService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller("departmentReturnQueuePageController")
public class ReturnQueuePageController {
    private static final String SESSION_SELECTED_RECEIVER = "rpm.departmentReturn.selectedReceiver";

    public void get(PageModel model,
                    @SpringBean("departmentStockReturnService") DepartmentStockReturnService departmentStockReturnService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        List<PharmacyLocation> candidates = receivingPharmacyCandidates(pharmacyService);
        PharmacyLocation activePharmacy = selectedReceivingPharmacy(candidates, pharmacyLocationId, session);
        model.addAttribute("activePharmacy", activePharmacy);
        model.addAttribute("pharmacySelectionOptions", candidates);
        model.addAttribute("pharmacySelectionRequired", activePharmacy == null && candidates.size() > 1);
        model.addAttribute("pharmacySelectionLocationName", currentLocationName());
        model.addAttribute("returns", activePharmacy == null ? Collections.emptyList()
                : departmentStockReturnService.getReturnsForDestination(activePharmacy,
                DepartmentStockReturnService.STATUS_SUBMITTED, 0, 100));
        model.addAttribute("pageTitle", "Department Return Queue");
    }

    public String post(@SpringBean("departmentStockReturnService") DepartmentStockReturnService departmentStockReturnService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("action") String action,
                       @RequestParam("returnId") Integer returnId,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       HttpSession session,
                       UiUtils ui) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        PharmacyLocation activePharmacy = requireReceivingPharmacy(pharmacyService, session);
        DepartmentStockReturn stockReturn = departmentStockReturnService.getReturn(returnId);
        validateDestination(stockReturn, activePharmacy);
        if ("receive".equalsIgnoreCase(action)) {
            departmentStockReturnService.receiveReturn(returnId);
        } else if ("reject".equalsIgnoreCase(action)) {
            departmentStockReturnService.rejectReturn(returnId, rejectionReason);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "department/returnQueue");
    }

    private void validateDestination(DepartmentStockReturn stockReturn, PharmacyLocation activePharmacy) {
        if (stockReturn == null || activePharmacy == null || stockReturn.getDestinationPharmacyLocation() == null
                || activePharmacy.getPharmacyLocationId() == null
                || !activePharmacy.getPharmacyLocationId().equals(
                stockReturn.getDestinationPharmacyLocation().getPharmacyLocationId())) {
            throw new IllegalArgumentException("This department return does not belong to the current pharmacy");
        }
    }

    private PharmacyLocation requireReceivingPharmacy(PharmacyService pharmacyService, HttpSession session) {
        PharmacyLocation activePharmacy = selectedReceivingPharmacy(receivingPharmacyCandidates(pharmacyService), null, session);
        if (activePharmacy != null) {
            return activePharmacy;
        }
        throw new IllegalArgumentException("Choose the receiving pharmacy before continuing");
    }

    private PharmacyLocation selectedReceivingPharmacy(List<PharmacyLocation> candidates,
                                                       Integer requestedPharmacyLocationId,
                                                       HttpSession session) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (requestedPharmacyLocationId != null) {
            PharmacyLocation selected = findCandidate(candidates, requestedPharmacyLocationId);
            if (selected == null) {
                throw new IllegalArgumentException("Selected pharmacy does not match your current OpenMRS location");
            }
            if (session != null) {
                session.setAttribute(SESSION_SELECTED_RECEIVER, requestedPharmacyLocationId);
            }
            return selected;
        }
        Integer selectedId = session == null ? null : (Integer) session.getAttribute(SESSION_SELECTED_RECEIVER);
        if (selectedId != null) {
            PharmacyLocation selected = findCandidate(candidates, selectedId);
            if (selected != null) {
                return selected;
            }
            session.removeAttribute(SESSION_SELECTED_RECEIVER);
        }
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private PharmacyLocation findCandidate(List<PharmacyLocation> candidates, Integer pharmacyLocationId) {
        for (PharmacyLocation candidate : candidates) {
            if (candidate != null && pharmacyLocationId.equals(candidate.getPharmacyLocationId())) {
                return candidate;
            }
        }
        return null;
    }

    private List<PharmacyLocation> receivingPharmacyCandidates(PharmacyService pharmacyService) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        if (currentLocation == null) {
            return Collections.emptyList();
        }
        List<PharmacyLocation> candidates = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation pharmacyLocation : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacyLocation == null || Boolean.FALSE.equals(pharmacyLocation.getActive())
                    || pharmacyLocation.getLocationType() == org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType.DEPARTMENT_STORE) {
                continue;
            }
            if (matchesOpenmrsLocation(pharmacyLocation, currentLocation)) {
                candidates.add(pharmacyLocation);
            }
        }
        Collections.sort(candidates, new Comparator<PharmacyLocation>() {
            @Override
            public int compare(PharmacyLocation left, PharmacyLocation right) {
                String leftName = left == null || left.getName() == null ? "" : left.getName();
                String rightName = right == null || right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return candidates;
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

    private String currentLocationName() {
        return Context.getUserContext() == null || Context.getUserContext().getLocation() == null
                ? "" : Context.getUserContext().getLocation().getName();
    }
}
