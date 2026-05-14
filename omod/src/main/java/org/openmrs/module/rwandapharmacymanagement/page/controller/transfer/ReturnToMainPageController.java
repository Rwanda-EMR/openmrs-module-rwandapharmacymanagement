package org.openmrs.module.rwandapharmacymanagement.page.controller.transfer;

import org.openmrs.api.context.Context;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ReturnToMainPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        List<PharmacyLocation> dispensingPharmacies = getDispensingPharmacies(pharmacyService);
        boolean canRequestReturn = isCurrentMainStore(mainStore);
        model.addAttribute("mainStore", mainStore);
        model.addAttribute("canRequestReturn", canRequestReturn);
        model.addAttribute("canApproveReturnTransfer",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK));
        model.addAttribute("dispensingPharmacies", dispensingPharmacies);
        model.addAttribute("stockByPharmacy", buildStockByPharmacy(dispensingPharmacies, inventoryService));
        model.addAttribute("requests", getReturnTransfers(inventoryService, activePharmacy, mainStore));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "transferId", required = false) Integer transferId,
                       @RequestParam(value = "sourcePharmacyLocationId", required = false) Integer sourcePharmacyLocationId,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDate", required = false) String[] expiryDates,
                       @RequestParam(value = "reason", required = false) String reason,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       HttpSession session,
                       UiUtils ui) throws ParseException {
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        if (mainStore == null) {
            throw new IllegalArgumentException("Create a main pharmacy before returning stock");
        }
        if ("reject".equalsIgnoreCase(action) && transferId != null) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
            PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
            validateReturnTransfer(inventoryService.getTransfer(transferId), activePharmacy, mainStore);
            inventoryService.rejectTransfer(transferId, rejectionReason);
            return redirect(ui);
        }
        if (("approve".equalsIgnoreCase(action) || "transfer".equalsIgnoreCase(action) || "serve".equalsIgnoreCase(action)) && transferId != null) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
            PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
            validateReturnTransfer(inventoryService.getTransfer(transferId), activePharmacy, mainStore);
            inventoryService.serveTransfer(transferId, quantities, batchNumbers, parseDates(expiryDates));
            return redirect(ui);
        }

        PharmacyLocation sourcePharmacy = pharmacyService.getPharmacyLocation(sourcePharmacyLocationId);
        if (!isCurrentMainStore(mainStore)) {
            throw new IllegalArgumentException("Only the main pharmacy can request items back from a dispensing pharmacy");
        }
        validateReturnRequestSource(sourcePharmacy);
        InterPharmacyTransfer transfer = new InterPharmacyTransfer();
        transfer.setSourcePharmacyLocation(sourcePharmacy);
        transfer.setDestinationPharmacyLocation(mainStore);
        transfer.setReason(reason);
        transfer.setTransferStatus("REQUESTED");
        transfer.setCreator(Context.getAuthenticatedUser());
        transfer.setDateCreated(new Date());
        transfer.setLines(buildLines(pharmacyService, transfer, itemIds, quantities, batchNumbers, expiryDates));
        inventoryService.requestTransfer(transfer);
        return redirect(ui);
    }

    private String redirect(UiUtils ui) {
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/returnToMain");
    }

    private void validateReturnTransfer(InterPharmacyTransfer transfer, PharmacyLocation activePharmacy, PharmacyLocation mainStore) {
        if (transfer == null || activePharmacy == null || mainStore == null
                || transfer.getSourcePharmacyLocation() == null || transfer.getDestinationPharmacyLocation() == null
                || activePharmacy.getPharmacyLocationId() == null || mainStore.getPharmacyLocationId() == null
                || !activePharmacy.getPharmacyLocationId().equals(transfer.getSourcePharmacyLocation().getPharmacyLocationId())
                || !mainStore.getPharmacyLocationId().equals(transfer.getDestinationPharmacyLocation().getPharmacyLocationId())) {
            throw new IllegalArgumentException("This return transfer does not belong to the current dispensing pharmacy");
        }
    }

    private void validateReturnRequestSource(PharmacyLocation sourcePharmacy) {
        if (sourcePharmacy == null || !Boolean.TRUE.equals(sourcePharmacy.getActive())
                || Boolean.TRUE.equals(sourcePharmacy.getMainStore())
                || sourcePharmacy.getLocationType() != PharmacyLocationType.DISPENSING_PHARMACY) {
            throw new IllegalArgumentException("Choose an active dispensing pharmacy to return stock from");
        }
    }

    private List<InterPharmacyTransfer> getReturnTransfers(InventoryService inventoryService, PharmacyLocation activePharmacy,
                                                           PharmacyLocation mainStore) {
        if (mainStore == null || mainStore.getPharmacyLocationId() == null) {
            return Collections.emptyList();
        }
        Integer activePharmacyId = activePharmacy == null ? null : activePharmacy.getPharmacyLocationId();
        boolean activeIsMainStore = isCurrentMainStore(mainStore);
        List<InterPharmacyTransfer> requests = new ArrayList<InterPharmacyTransfer>(
                inventoryService.getTransfers(null, activeIsMainStore ? null : activePharmacyId,
                        mainStore.getPharmacyLocationId(), 200));
        sortTransfers(requests);
        return requests;
    }

    private List<PharmacyLocation> getDispensingPharmacies(PharmacyService pharmacyService) {
        List<PharmacyLocation> pharmacies = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation location : pharmacyService.getAllPharmacyLocations()) {
            if (location != null && Boolean.TRUE.equals(location.getActive())
                    && !Boolean.TRUE.equals(location.getMainStore())
                    && location.getLocationType() == PharmacyLocationType.DISPENSING_PHARMACY) {
                pharmacies.add(location);
            }
        }
        return pharmacies;
    }

    private boolean isCurrentMainStore(PharmacyLocation mainStore) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        return matchesOpenmrsLocation(mainStore, currentLocation);
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

    private List<InterPharmacyTransferLine> buildLines(PharmacyService pharmacyService,
                                                       InterPharmacyTransfer transfer,
                                                       Integer[] itemIds,
                                                       BigDecimal[] quantities,
                                                       String[] batchNumbers,
                                                       String[] expiryDates) throws ParseException {
        List<InterPharmacyTransferLine> lines = new ArrayList<InterPharmacyTransferLine>();
        if (itemIds == null || quantities == null) {
            return lines;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            BigDecimal quantity = quantities.length > i ? quantities[i] : null;
            if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            InterPharmacyTransferLine line = new InterPharmacyTransferLine();
            line.setInterPharmacyTransfer(transfer);
            line.setItem(pharmacyService.getItem(itemId));
            line.setQuantity(quantity);
            line.setBatchNumber(batchNumbers != null && batchNumbers.length > i ? emptyToNull(batchNumbers[i]) : null);
            String expiryDate = expiryDates != null && expiryDates.length > i ? expiryDates[i] : null;
            if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                line.setExpiryDate(new SimpleDateFormat("yyyy-MM-dd").parse(expiryDate));
            }
            lines.add(line);
        }
        return lines;
    }

    private List<Map<String, Object>> buildStockSummary(List<InventoryBalance> balances) {
        List<Map<String, Object>> itemRows = new ArrayList<Map<String, Object>>();
        Map<Integer, Map<String, Object>> byItem = new LinkedHashMap<Integer, Map<String, Object>>();
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            Map<String, Object> itemRow = byItem.get(itemId);
            if (itemRow == null) {
                itemRow = new LinkedHashMap<String, Object>();
                itemRow.put("itemId", itemId);
                itemRow.put("name", balance.getItem().getName());
                itemRow.put("available", BigDecimal.ZERO);
                itemRow.put("unitPrice", balance.getUnitPrice());
                byItem.put(itemId, itemRow);
                itemRows.add(itemRow);
            }
            if (itemRow.get("unitPrice") == null && balance.getUnitPrice() != null) {
                itemRow.put("unitPrice", balance.getUnitPrice());
            }
            itemRow.put("available", ((BigDecimal) itemRow.get("available")).add(balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand()));
        }
        return itemRows;
    }

    private Map<Integer, List<Map<String, Object>>> buildStockByItem(List<InventoryBalance> balances) {
        Map<Integer, List<Map<String, Object>>> batchesByItem = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            List<Map<String, Object>> batches = batchesByItem.get(itemId);
            if (batches == null) {
                batches = new ArrayList<Map<String, Object>>();
                batchesByItem.put(itemId, batches);
            }
            Map<String, Object> batch = new LinkedHashMap<String, Object>();
            batch.put("batchNumber", balance.getBatchNumber());
            batch.put("expiryDate", balance.getExpiryDate());
            batch.put("available", balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand());
            batch.put("unitPrice", balance.getUnitPrice());
            batches.add(batch);
        }
        return batchesByItem;
    }

    private Map<Integer, List<Map<String, Object>>> buildStockByPharmacy(List<PharmacyLocation> pharmacies,
                                                                         InventoryService inventoryService) {
        Map<Integer, List<Map<String, Object>>> stockByPharmacy = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        for (PharmacyLocation pharmacy : pharmacies) {
            List<InventoryBalance> balances = pharmacy == null || pharmacy.getPharmacyLocationId() == null
                    ? Collections.<InventoryBalance>emptyList()
                    : inventoryService.getInventoryForPharmacy(pharmacy.getPharmacyLocationId());
            List<Map<String, Object>> itemRows = buildStockSummary(balances);
            Map<Integer, List<Map<String, Object>>> batchesByItem = buildStockByItem(balances);
            for (Map<String, Object> itemRow : itemRows) {
                itemRow.put("batches", batchesByItem.get(itemRow.get("itemId")));
            }
            stockByPharmacy.put(pharmacy.getPharmacyLocationId(), itemRows);
        }
        return stockByPharmacy;
    }

    private Date[] parseDates(String[] expiryDates) throws ParseException {
        if (expiryDates == null) {
            return null;
        }
        Date[] parsedDates = new Date[expiryDates.length];
        for (int i = 0; i < expiryDates.length; i++) {
            String expiryDate = expiryDates[i];
            if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                parsedDates[i] = new SimpleDateFormat("yyyy-MM-dd").parse(expiryDate);
            }
        }
        return parsedDates;
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void sortTransfers(List<InterPharmacyTransfer> requests) {
        Collections.sort(requests, new Comparator<InterPharmacyTransfer>() {
            @Override
            public int compare(InterPharmacyTransfer left, InterPharmacyTransfer right) {
                int statusCompare = Integer.valueOf(statusRank(left)).compareTo(statusRank(right));
                if (statusCompare != 0) {
                    return statusCompare;
                }
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
    }

    private int statusRank(InterPharmacyTransfer transfer) {
        String status = transfer == null ? null : transfer.getTransferStatus();
        if ("REQUESTED".equalsIgnoreCase(status)) {
            return 0;
        }
        if ("APPROVED".equalsIgnoreCase(status)) {
            return 1;
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return 2;
        }
        if ("SERVED".equalsIgnoreCase(status)) {
            return 3;
        }
        return 4;
    }
}
