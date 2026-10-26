package org.openmrs.module.rwandapharmacymanagement.page.controller.pharmacyorder;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.TransferPreviousInfo;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.NavigationVisibilitySupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller("pharmacyOrderApprovePageController")
public class ApprovePageController {
    private static final String FLASH_APPROVED = "rpm.pharmacyOrderApprove.approved";
    private static final String FLASH_REJECTED = "rpm.pharmacyOrderApprove.rejected";
    private static final String FLASH_LINE_REMOVED = "rpm.pharmacyOrderApprove.lineRemoved";

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        requireApprovalPrivilege();
        List<PharmacyLocation> accessiblePharmacies = getAccessibleApprovalPharmacies(pharmacyService, session);
        PharmacyLocation selectedPharmacy = resolveSelectedApprovalPharmacy(accessiblePharmacies, pharmacyLocationId);
        List<InterPharmacyTransfer> requests = getPendingApprovalRequests(inventoryService, accessiblePharmacies,
                selectedPharmacy);
        model.addAttribute("pharmacies", accessiblePharmacies);
        model.addAttribute("selectedPharmacy", selectedPharmacy);
        model.addAttribute("requests", requests);
        model.addAttribute("requestingQuantityByLineId", buildRequestingQuantityByLineId(inventoryService, requests));
        model.addAttribute("servingAvailabilityByLineId", buildServingAvailabilityByLineId(inventoryService, requests));
        model.addAttribute("previousInfoByLineId", buildPreviousInfoByLineId(inventoryService, requests));
        model.addAttribute("approved", FlashMessageSupport.consumeBoolean(session, FLASH_APPROVED));
        model.addAttribute("rejected", FlashMessageSupport.consumeBoolean(session, FLASH_REJECTED));
        model.addAttribute("lineRemoved", FlashMessageSupport.consumeBoolean(session, FLASH_LINE_REMOVED));
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("transferId") Integer transferId,
                       @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                       @RequestParam("action") String action,
                       @RequestParam(value = "transferLineId", required = false) Integer transferLineId,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       @RequestParam(value = "lineVoidReason", required = false) String lineVoidReason,
                       HttpSession session,
                       UiUtils ui) {
        requireApprovalPrivilege();
        InterPharmacyTransfer transfer = inventoryService.getTransfer(transferId);
        if (transfer == null || !"REQUESTED".equalsIgnoreCase(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Only requested pharmacy orders can be approved or rejected");
        }
        if (!isTransferSourceAccessible(transfer, getAccessibleApprovalPharmacies(pharmacyService, session))) {
            throw new IllegalArgumentException("Only the pharmacy responsible for serving this request can approve or reject it");
        }
        if ("removeLine".equalsIgnoreCase(action)) {
            inventoryService.removeTransferLine(transferId, transferLineId, lineVoidReason);
            FlashMessageSupport.set(session, FLASH_LINE_REMOVED, Boolean.TRUE);
        } else if ("reject".equalsIgnoreCase(action)) {
            inventoryService.rejectTransfer(transferId, rejectionReason);
            FlashMessageSupport.set(session, FLASH_REJECTED, Boolean.TRUE);
        } else {
            inventoryService.approveTransfer(transferId);
            FlashMessageSupport.set(session, FLASH_APPROVED, Boolean.TRUE);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "pharmacyorder/approve",
                pharmacyLocationId == null ? java.util.Collections.<String, Object>emptyMap()
                        : java.util.Collections.<String, Object>singletonMap("pharmacyLocationId", pharmacyLocationId));
    }

    private List<InterPharmacyTransfer> getPendingApprovalRequests(InventoryService inventoryService,
                                                                   List<PharmacyLocation> accessiblePharmacies,
                                                                   PharmacyLocation selectedPharmacy) {
        List<InterPharmacyTransfer> requests = new ArrayList<InterPharmacyTransfer>();
        if (selectedPharmacy != null && selectedPharmacy.getPharmacyLocationId() != null) {
            requests.addAll(inventoryService.getTransfers("REQUESTED", selectedPharmacy.getPharmacyLocationId(),
                    null, 200));
        } else if (accessiblePharmacies != null) {
            for (PharmacyLocation pharmacyLocation : accessiblePharmacies) {
                if (pharmacyLocation != null && pharmacyLocation.getPharmacyLocationId() != null) {
                    requests.addAll(inventoryService.getTransfers("REQUESTED",
                            pharmacyLocation.getPharmacyLocationId(), null, 200));
                }
            }
        }
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

    private List<PharmacyLocation> getAccessibleApprovalPharmacies(PharmacyService pharmacyService, HttpSession session) {
        List<PharmacyLocation> pharmacies = new ArrayList<PharmacyLocation>();
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        if (NavigationVisibilitySupport.matchesCurrentOpenmrsLocation(mainStore)) {
            pharmacies.add(mainStore);
        }
        PharmacyLocation selectedDispensingPharmacy = CurrentPharmacySupport.resolveCurrentPharmacy(pharmacyService, session);
        if (selectedDispensingPharmacy != null) {
            pharmacies.add(selectedDispensingPharmacy);
        } else {
            pharmacies.addAll(CurrentPharmacySupport.getCandidatePharmacies(pharmacyService));
        }
        return deduplicatePharmacies(pharmacies);
    }

    private List<PharmacyLocation> deduplicatePharmacies(List<PharmacyLocation> pharmacies) {
        List<PharmacyLocation> unique = new ArrayList<PharmacyLocation>();
        if (pharmacies == null) {
            return unique;
        }
        for (PharmacyLocation pharmacyLocation : pharmacies) {
            if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null) {
                continue;
            }
            boolean exists = false;
            for (PharmacyLocation existing : unique) {
                if (pharmacyLocation.getPharmacyLocationId().equals(existing.getPharmacyLocationId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                unique.add(pharmacyLocation);
            }
        }
        return unique;
    }

    private PharmacyLocation resolveSelectedApprovalPharmacy(List<PharmacyLocation> accessiblePharmacies,
                                                             Integer pharmacyLocationId) {
        if (accessiblePharmacies == null || accessiblePharmacies.isEmpty()) {
            return null;
        }
        if (pharmacyLocationId == null) {
            return accessiblePharmacies.size() == 1 ? accessiblePharmacies.get(0) : null;
        }
        for (PharmacyLocation pharmacyLocation : accessiblePharmacies) {
            if (pharmacyLocation != null && pharmacyLocationId.equals(pharmacyLocation.getPharmacyLocationId())) {
                return pharmacyLocation;
            }
        }
        throw new IllegalArgumentException("Selected pharmacy is not responsible for approvals at your current location");
    }

    private boolean isTransferSourceAccessible(InterPharmacyTransfer transfer, List<PharmacyLocation> accessiblePharmacies) {
        Integer sourceId = transfer == null || transfer.getSourcePharmacyLocation() == null
                ? null : transfer.getSourcePharmacyLocation().getPharmacyLocationId();
        if (sourceId == null || accessiblePharmacies == null) {
            return false;
        }
        for (PharmacyLocation pharmacyLocation : accessiblePharmacies) {
            if (pharmacyLocation != null && sourceId.equals(pharmacyLocation.getPharmacyLocationId())) {
                return true;
            }
        }
        return false;
    }

    private void requireApprovalPrivilege() {
        if (Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)
                || Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER)) {
            return;
        }
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
    }

    private Map<Integer, String> buildRequestingQuantityByLineId(InventoryService inventoryService,
                                                                 List<InterPharmacyTransfer> requests) {
        Map<Integer, String> quantityByLineId = new LinkedHashMap<Integer, String>();
        Map<Integer, Map<Integer, BigDecimal>> quantitiesByPharmacyId = new LinkedHashMap<Integer, Map<Integer, BigDecimal>>();
        if (requests == null) {
            return quantityByLineId;
        }
        for (InterPharmacyTransfer request : requests) {
            if (request == null || request.getDestinationPharmacyLocation() == null
                    || request.getDestinationPharmacyLocation().getPharmacyLocationId() == null
                    || request.getLines() == null) {
                continue;
            }
            Integer requestingPharmacyId = request.getDestinationPharmacyLocation().getPharmacyLocationId();
            Map<Integer, BigDecimal> quantitiesByItemId = quantitiesByPharmacyId.get(requestingPharmacyId);
            if (quantitiesByItemId == null) {
                quantitiesByItemId = buildQuantityByItemId(inventoryService, requestingPharmacyId);
                quantitiesByPharmacyId.put(requestingPharmacyId, quantitiesByItemId);
            }
            for (InterPharmacyTransferLine line : request.getLines()) {
                if (line == null || line.getInterPharmacyTransferLineId() == null || line.getItem() == null
                        || line.getItem().getItemId() == null) {
                    continue;
                }
                BigDecimal quantity = quantitiesByItemId.containsKey(line.getItem().getItemId())
                        ? quantitiesByItemId.get(line.getItem().getItemId()) : BigDecimal.ZERO;
                quantityByLineId.put(line.getInterPharmacyTransferLineId(), formatQuantity(quantity));
            }
        }
        return quantityByLineId;
    }

    private Map<Integer, Map<String, Object>> buildServingAvailabilityByLineId(InventoryService inventoryService,
                                                                               List<InterPharmacyTransfer> requests) {
        Map<Integer, Map<String, Object>> availabilityByLineId = new LinkedHashMap<Integer, Map<String, Object>>();
        Map<Integer, Map<Integer, BigDecimal>> quantitiesByPharmacyId = new LinkedHashMap<Integer, Map<Integer, BigDecimal>>();
        if (requests == null) {
            return availabilityByLineId;
        }
        for (InterPharmacyTransfer request : requests) {
            if (request == null || request.getSourcePharmacyLocation() == null
                    || request.getSourcePharmacyLocation().getPharmacyLocationId() == null
                    || request.getLines() == null) {
                continue;
            }
            Integer servingPharmacyId = request.getSourcePharmacyLocation().getPharmacyLocationId();
            Map<Integer, BigDecimal> quantitiesByItemId = quantitiesByPharmacyId.get(servingPharmacyId);
            if (quantitiesByItemId == null) {
                quantitiesByItemId = buildQuantityByItemId(inventoryService, servingPharmacyId);
                quantitiesByPharmacyId.put(servingPharmacyId, quantitiesByItemId);
            }
            for (InterPharmacyTransferLine line : request.getLines()) {
                if (line == null || line.getInterPharmacyTransferLineId() == null || line.getItem() == null
                        || line.getItem().getItemId() == null) {
                    continue;
                }
                BigDecimal available = quantitiesByItemId.containsKey(line.getItem().getItemId())
                        ? quantitiesByItemId.get(line.getItem().getItemId()) : BigDecimal.ZERO;
                BigDecimal requested = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
                Map<String, Object> values = new LinkedHashMap<String, Object>();
                values.put("quantity", formatQuantity(available));
                values.put("insufficient", Boolean.valueOf(available.compareTo(requested) < 0));
                availabilityByLineId.put(line.getInterPharmacyTransferLineId(), values);
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

    private Map<Integer, Map<String, Object>> buildPreviousInfoByLineId(InventoryService inventoryService,
                                                                        List<InterPharmacyTransfer> requests) {
        Map<Integer, Map<String, Object>> previousInfoByLineId = new LinkedHashMap<Integer, Map<String, Object>>();
        if (requests == null) {
            return previousInfoByLineId;
        }
        for (InterPharmacyTransfer request : requests) {
            if (request == null || request.getInterPharmacyTransferId() == null || request.getLines() == null) {
                continue;
            }
            for (InterPharmacyTransferLine line : request.getLines()) {
                if (line == null || line.getInterPharmacyTransferLineId() == null) {
                    continue;
                }
                previousInfoByLineId.put(line.getInterPharmacyTransferLineId(),
                        buildPreviousInfo(inventoryService, request.getInterPharmacyTransferId(),
                                line.getInterPharmacyTransferLineId()));
            }
        }
        return previousInfoByLineId;
    }

    private Map<String, Object> buildPreviousInfo(InventoryService inventoryService,
                                                  Integer transferId,
                                                  Integer transferLineId) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        try {
            TransferPreviousInfo info = inventoryService.getTransferPreviousInfo(transferId, transferLineId);
            values.put("success", Boolean.TRUE);
            values.put("hasPreviousRequest", Boolean.valueOf(info.getPreviousRequestDate() != null));
            values.put("previousTransferId", info.getPreviousTransferId());
            values.put("previousRequestDate", formatDate(info.getPreviousRequestDate()));
            values.put("quantityBeforePreviousRequest", formatQuantity(info.getQuantityBeforePreviousRequest()));
            values.put("receivedSincePreviousRequest", formatQuantity(info.getReceivedSincePreviousRequest()));
            values.put("dispensedSincePreviousRequest", formatQuantity(info.getDispensedSincePreviousRequest()));
            values.put("transferredOutSincePreviousRequest", formatQuantity(info.getTransferredOutSincePreviousRequest()));
            values.put("adjustedSincePreviousRequest", formatQuantity(info.getAdjustedSincePreviousRequest()));
            values.put("quantityAtCurrentRequest", formatQuantity(info.getQuantityAtCurrentRequest()));
            values.put("currentQuantity", formatQuantity(info.getCurrentQuantity()));
            values.put("currentRequestedQuantity", formatQuantity(info.getCurrentRequestedQuantity()));
        }
        catch (Exception ex) {
            values.put("success", Boolean.FALSE);
            values.put("message", ex.getMessage() == null ? "Unable to load previous information" : ex.getMessage());
        }
        return values;
    }

    private String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("dd/MMM/yyyy HH:mm").format(date);
    }

    private String formatQuantity(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }
}
