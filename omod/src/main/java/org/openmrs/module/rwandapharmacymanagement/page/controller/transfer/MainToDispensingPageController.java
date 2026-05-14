package org.openmrs.module.rwandapharmacymanagement.page.controller.transfer;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainToDispensingPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        model.addAttribute("canApproveMainToDispensingTransfer",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER));
        model.addAttribute("canTransferMainToDispensing",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK));
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        model.addAttribute("mainStore", mainStore);
        model.addAttribute("dispensingPharmacies", activePharmacy == null
                ? new ArrayList<PharmacyLocation>() : Collections.singletonList(activePharmacy));
        List<InventoryBalance> mainStock = mainStore == null ? new ArrayList<InventoryBalance>() : inventoryService.getInventoryForPharmacy(mainStore.getPharmacyLocationId());
        model.addAttribute("mainStock", mainStock);
        model.addAttribute("mainStockSummary", buildStockSummary(mainStock));
        model.addAttribute("mainStockByItem", buildStockByItem(mainStock));
        List<InterPharmacyTransfer> requests = mainStore == null || mainStore.getPharmacyLocationId() == null
                ? new ArrayList<InterPharmacyTransfer>()
                : new ArrayList<InterPharmacyTransfer>(inventoryService.getTransfers(null, mainStore.getPharmacyLocationId(), null, 200));
        sortTransfers(requests);
        model.addAttribute("requests", requests);
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "transferId", required = false) Integer transferId,
                       @RequestParam(value = "destinationPharmacyLocationId", required = false) Integer destinationPharmacyLocationId,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDate", required = false) String[] expiryDates,
                       @RequestParam(value = "reason", required = false) String reason,
        @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       HttpSession session,
        UiUtils ui) throws ParseException {
        if ("approve".equalsIgnoreCase(action) && transferId != null) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
            inventoryService.approveTransfer(transferId);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/mainToDispensing");
        }
        if ("reject".equalsIgnoreCase(action) && transferId != null) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
            inventoryService.rejectTransfer(transferId, rejectionReason);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/mainToDispensing");
        }
        if (("transfer".equalsIgnoreCase(action) || "serve".equalsIgnoreCase(action)) && transferId != null) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
            inventoryService.serveTransfer(transferId, quantities, batchNumbers, parseDates(expiryDates));
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/mainToDispensing");
        }
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        if (mainStore == null) {
            throw new IllegalArgumentException("Create a main pharmacy before sending internal requests");
        }
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        InterPharmacyTransfer transfer = new InterPharmacyTransfer();
        transfer.setSourcePharmacyLocation(mainStore);
        transfer.setDestinationPharmacyLocation(activePharmacy);
        transfer.setReason(reason);
        transfer.setTransferStatus("REQUESTED");
        transfer.setCreator(Context.getAuthenticatedUser());
        transfer.setDateCreated(new java.util.Date());
        transfer.setLines(buildLines(pharmacyService, transfer, itemIds, quantities, batchNumbers, expiryDates));
        inventoryService.requestTransfer(transfer);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/mainToDispensing");
    }

    private List<PharmacyLocation> getDispensingPharmacies(PharmacyService pharmacyService) {
        List<PharmacyLocation> dispensingPharmacies = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation location : pharmacyService.getAllPharmacyLocations()) {
            if (location != null && Boolean.TRUE.equals(location.getActive())
                    && location.getLocationType() == org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType.DISPENSING_PHARMACY) {
                dispensingPharmacies.add(location);
            }
        }
        return dispensingPharmacies;
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
            line.setBatchNumber(batchNumbers != null && batchNumbers.length > i ? batchNumbers[i] : null);
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
