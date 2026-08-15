package org.openmrs.module.rwandapharmacymanagement.page.controller.transfer;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.TransferBatchAllocationCommand;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainToDispensingReviewPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam("transferId") Integer transferId) {
        requireReviewAccess();
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        InterPharmacyTransfer transfer = inventoryService.getTransfer(transferId);
        if (transfer == null || mainStore == null
                || transfer.getSourcePharmacyLocation() == null
                || !mainStore.getPharmacyLocationId().equals(transfer.getSourcePharmacyLocation().getPharmacyLocationId())) {
            model.addAttribute("transfer", null);
            return;
        }
        List<InventoryBalance> mainStock = inventoryService.getInventoryForPharmacy(mainStore.getPharmacyLocationId());
        List<InventoryBalance> requestingStock = transfer.getDestinationPharmacyLocation() == null
                || transfer.getDestinationPharmacyLocation().getPharmacyLocationId() == null
                ? new ArrayList<InventoryBalance>()
                : inventoryService.getInventoryForPharmacy(transfer.getDestinationPharmacyLocation().getPharmacyLocationId());
        model.addAttribute("transfer", transfer);
        model.addAttribute("mainStore", mainStore);
        model.addAttribute("mainStockByItem", buildStockByItem(mainStock));
        model.addAttribute("mainStockSummary", buildStockSummary(mainStock));
        model.addAttribute("requestingStockSummary", buildStockSummary(requestingStock));
        model.addAttribute("canApproveMainToDispensingTransfer",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER));
        model.addAttribute("canTransferMainToDispensing",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK));
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam("transferId") Integer transferId,
                       @RequestParam("action") String action,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDate", required = false) String[] expiryDates,
                       @RequestParam(value = "allocationLineId", required = false) Integer[] allocationLineIds,
                       @RequestParam(value = "allocationBatchNumber", required = false) String[] allocationBatchNumbers,
                       @RequestParam(value = "allocationQuantity", required = false) BigDecimal[] allocationQuantities,
                       @RequestParam(value = "transferLineId", required = false) Integer transferLineId,
                       @RequestParam(value = "lineVoidReason", required = false) String lineVoidReason,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       UiUtils ui) throws ParseException {
        if ("removeLine".equalsIgnoreCase(action)) {
            inventoryService.removeTransferLine(transferId, transferLineId, lineVoidReason);
        } else if ("reject".equalsIgnoreCase(action)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
            inventoryService.rejectTransfer(transferId, rejectionReason);
        } else if ("approve".equalsIgnoreCase(action)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
            inventoryService.approveTransfer(transferId);
        } else if ("transfer".equalsIgnoreCase(action) || "serve".equalsIgnoreCase(action)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
            inventoryService.serveTransfer(transferId, quantities, batchNumbers, parseDates(expiryDates),
                    parseAllocations(allocationLineIds, allocationBatchNumbers, allocationQuantities));
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/mainToDispensingReview",
                java.util.Collections.<String, Object>singletonMap("transferId", transferId));
    }

    private void requireReviewAccess() {
        if (!Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER)
                && !Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
        }
    }

    private Map<Integer, BigDecimal> buildStockSummary(List<InventoryBalance> balances) {
        Map<Integer, BigDecimal> byItem = new LinkedHashMap<Integer, BigDecimal>();
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            BigDecimal current = byItem.containsKey(itemId) ? byItem.get(itemId) : BigDecimal.ZERO;
            byItem.put(itemId, current.add(balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand()));
        }
        return byItem;
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
            if (expiryDates[i] != null && !expiryDates[i].trim().isEmpty()) {
                parsedDates[i] = new SimpleDateFormat("yyyy-MM-dd").parse(expiryDates[i]);
            }
        }
        return parsedDates;
    }

    private List<TransferBatchAllocationCommand> parseAllocations(Integer[] allocationLineIds,
                                                                  String[] allocationBatchNumbers,
                                                                  BigDecimal[] allocationQuantities) {
        List<TransferBatchAllocationCommand> allocations = new ArrayList<TransferBatchAllocationCommand>();
        if (allocationLineIds == null || allocationBatchNumbers == null || allocationQuantities == null) {
            return allocations;
        }
        int count = Math.min(allocationLineIds.length, Math.min(allocationBatchNumbers.length, allocationQuantities.length));
        for (int i = 0; i < count; i++) {
            if (allocationLineIds[i] == null || allocationQuantities[i] == null
                    || allocationQuantities[i].compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            TransferBatchAllocationCommand allocation = new TransferBatchAllocationCommand();
            allocation.setTransferLineId(allocationLineIds[i]);
            allocation.setBatchNumber(allocationBatchNumbers[i]);
            allocation.setQuantity(allocationQuantities[i]);
            allocations.add(allocation);
        }
        return allocations;
    }
}
