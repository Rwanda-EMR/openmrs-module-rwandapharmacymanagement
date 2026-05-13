package org.openmrs.module.rwandapharmacymanagement.page.controller.pharmacyorder;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Controller("pharmacyOrderApprovePageController")
public class ApprovePageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "approved", required = false) Integer approved,
                    @RequestParam(value = "rejected", required = false) Integer rejected) {
        PharmacyLocation selectedPharmacy = pharmacyLocationId == null ? null : pharmacyService.getPharmacyLocation(pharmacyLocationId);
        model.addAttribute("pharmacies", pharmacyService.getAllPharmacyLocations());
        model.addAttribute("selectedPharmacy", selectedPharmacy);
        model.addAttribute("requests", getPendingApprovalRequests(inventoryService, selectedPharmacy));
        model.addAttribute("approved", approved != null && approved.intValue() == 1);
        model.addAttribute("rejected", rejected != null && rejected.intValue() == 1);
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam("transferId") Integer transferId,
                       @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                       @RequestParam("action") String action,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       UiUtils ui) {
        InterPharmacyTransfer transfer = inventoryService.getTransfer(transferId);
        if (transfer == null || !"REQUESTED".equalsIgnoreCase(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Only requested pharmacy orders can be approved or rejected");
        }
        String flag;
        if ("reject".equalsIgnoreCase(action)) {
            inventoryService.rejectTransfer(transferId, rejectionReason);
            flag = "rejected=1";
        } else {
            inventoryService.approveTransfer(transferId);
            flag = "approved=1";
        }
        String redirect = ui.pageLink("rwandapharmacymanagement", "pharmacyorder/approve",
                pharmacyLocationId == null ? java.util.Collections.<String, Object>emptyMap()
                        : java.util.Collections.<String, Object>singletonMap("pharmacyLocationId", pharmacyLocationId));
        return "redirect:" + redirect + (redirect.contains("?") ? "&" : "?") + flag;
    }

    private List<InterPharmacyTransfer> getPendingApprovalRequests(InventoryService inventoryService,
                                                                   PharmacyLocation selectedPharmacy) {
        Integer selectedSourceId = selectedPharmacy == null ? null : selectedPharmacy.getPharmacyLocationId();
        List<InterPharmacyTransfer> requests = new ArrayList<InterPharmacyTransfer>(
                inventoryService.getTransfers("REQUESTED", selectedSourceId, null, 200));
        Collections.sort(requests, new Comparator<InterPharmacyTransfer>() {
            @Override
            public int compare(InterPharmacyTransfer left, InterPharmacyTransfer right) {
                Date leftDate = left == null ? null : left.getDateCreated();
                Date rightDate = right == null ? null : right.getDateCreated();
                if (leftDate == null && rightDate == null) {
                    return 0;
                }
                if (leftDate == null) {
                    return 1;
                }
                if (rightDate == null) {
                    return -1;
                }
                return rightDate.compareTo(leftDate);
            }
        });
        return requests;
    }
}
