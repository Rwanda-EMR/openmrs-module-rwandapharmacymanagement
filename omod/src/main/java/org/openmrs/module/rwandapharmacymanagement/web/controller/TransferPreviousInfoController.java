package org.openmrs.module.rwandapharmacymanagement.web.controller;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.TransferPreviousInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/module/rwandapharmacymanagement/pharmacyorder")
public class TransferPreviousInfoController {

    private static final Logger log = LoggerFactory.getLogger(TransferPreviousInfoController.class);

    private final InventoryService inventoryService;

    public TransferPreviousInfoController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RequestMapping(value = "/previousInfo", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> previousInfo(@RequestParam("transferId") Integer transferId,
                                            @RequestParam("transferLineId") Integer transferLineId) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        try {
            requireApprovalAccess();
            TransferPreviousInfo info = inventoryService.getTransferPreviousInfo(transferId, transferLineId);
            response.put("success", true);
            addPreviousInfo(response, info);
        }
        catch (Exception ex) {
            log.warn("Unable to load transfer previous information. transferId=" + transferId
                    + ", transferLineId=" + transferLineId, ex);
            response.put("success", false);
            response.put("message", ex.getMessage() == null ? "Unable to load previous information" : ex.getMessage());
        }
        return response;
    }

    private void addPreviousInfo(Map<String, Object> response, TransferPreviousInfo info) {
        response.put("transferId", info.getTransferId());
        response.put("transferLineId", info.getTransferLineId());
        response.put("itemName", safe(info.getItemName()));
        response.put("requestingPharmacyName", safe(info.getRequestingPharmacyName()));
        response.put("currentRequestDate", formatDate(info.getCurrentRequestDate()));
        response.put("currentRequestedQuantity", formatQuantity(info.getCurrentRequestedQuantity()));
        response.put("previousTransferId", info.getPreviousTransferId());
        response.put("previousRequestDate", formatDate(info.getPreviousRequestDate()));
        response.put("previousRequestedQuantity", formatQuantity(info.getPreviousRequestedQuantity()));
        response.put("quantityBeforePreviousRequest", formatQuantity(info.getQuantityBeforePreviousRequest()));
        response.put("receivedSincePreviousRequest", formatQuantity(info.getReceivedSincePreviousRequest()));
        response.put("dispensedSincePreviousRequest", formatQuantity(info.getDispensedSincePreviousRequest()));
        response.put("transferredOutSincePreviousRequest", formatQuantity(info.getTransferredOutSincePreviousRequest()));
        response.put("adjustedSincePreviousRequest", formatQuantity(info.getAdjustedSincePreviousRequest()));
        response.put("quantityAtCurrentRequest", formatQuantity(info.getQuantityAtCurrentRequest()));
        response.put("currentQuantity", formatQuantity(info.getCurrentQuantity()));
        response.put("hasPreviousRequest", info.getPreviousRequestDate() != null);
    }

    private void requireApprovalAccess() {
        if (Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER)
                || Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)) {
            return;
        }
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
    }

    private String formatDate(java.util.Date date) {
        return date == null ? "" : new SimpleDateFormat("dd/MMM/yyyy HH:mm").format(date);
    }

    private String formatQuantity(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
