package org.openmrs.module.rwandapharmacymanagement.web.controller;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.openmrs.module.rwandapharmacymanagement.web.dto.DeliveryReceiptImportItem;
import org.openmrs.module.rwandapharmacymanagement.web.dto.DeliveryReceiptImportRequest;
import org.openmrs.module.rwandapharmacymanagement.web.support.DeliveryReceiptSupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/module/rwandapharmacymanagement/delivery-receipts")
public class DeliveryReceiptApiController {

    private final InventoryService inventoryService;
    private final PharmacyService pharmacyService;
    private final SupplierIntegrationService supplierIntegrationService;
    private final IntegrationLogService integrationLogService;

    public DeliveryReceiptApiController(InventoryService inventoryService,
                                        PharmacyService pharmacyService,
                                        SupplierIntegrationService supplierIntegrationService,
                                        IntegrationLogService integrationLogService) {
        this.inventoryService = inventoryService;
        this.pharmacyService = pharmacyService;
        this.supplierIntegrationService = supplierIntegrationService;
        this.integrationLogService = integrationLogService;
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> importReceipt(@RequestBody DeliveryReceiptImportRequest request,
                                             @RequestParam(value = "sendConfirmation", required = false) Boolean sendConfirmation) throws ParseException {
        long start = System.currentTimeMillis();
        try {
            DeliveryReceipt receipt = buildReceipt(request);
            DeliveryReceipt saved = inventoryService.receiveDelivery(receipt);
            if (Boolean.TRUE.equals(sendConfirmation) || Boolean.TRUE.equals(request.getSendConfirmation())) {
                supplierIntegrationService.confirmDelivery(saved);
            }
            integrationLogService.saveLog(createLog(IntegrationStatus.SUCCESS, request, saved, start, null));
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("deliveryReceiptId", saved.getDeliveryReceiptId());
            response.put("receiptNumber", saved.getReceiptNumber());
            response.put("receivedLines", saved.getLines() == null ? 0 : saved.getLines().size());
            response.put("status", "RECEIVED");
            return response;
        }
        catch (IllegalArgumentException e) {
            integrationLogService.saveLog(createLog(IntegrationStatus.FAILED, request, null, start, e.getMessage()));
            throw e;
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, Object> handleBadRequest(IllegalArgumentException exception) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("status", "FAILED");
        response.put("message", exception.getMessage());
        return response;
    }

    private DeliveryReceipt buildReceipt(DeliveryReceiptImportRequest request) throws ParseException {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getDeliveryId() == null || request.getDeliveryId().trim().isEmpty()) {
            throw new IllegalArgumentException("deliveryId is required");
        }
        PurchaseOrder purchaseOrder = resolveApprovedPurchaseOrder(request);
        PharmacyLocation pharmacyLocation = request.getPharmacyLocationId() == null
                ? resolveMainStore()
                : pharmacyService.getPharmacyLocation(request.getPharmacyLocationId());
        if (pharmacyLocation == null) {
            throw new IllegalArgumentException("Valid pharmacyLocationId is required");
        }
        DeliveryReceipt receipt = new DeliveryReceipt();
        receipt.setReceiptNumber(request.getDeliveryId().trim());
        receipt.setPurchaseOrder(purchaseOrder);
        receipt.setReceivedIntoLocation(pharmacyLocation);
        receipt.setStatus("RECEIVED");
        receipt.setLines(buildLines(request.getItems()));
        if (request.getDeliveryDate() != null && !request.getDeliveryDate().trim().isEmpty()) {
            Date dateCreated = parseFlexibleDate(request.getDeliveryDate());
            receipt.setDateCreated(dateCreated);
        }
        return receipt;
    }

    private List<DeliveryReceiptLine> buildLines(List<DeliveryReceiptImportItem> items) throws ParseException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
        List<DeliveryReceiptLine> lines = new ArrayList<DeliveryReceiptLine>();
        for (DeliveryReceiptImportItem importedItem : items) {
            String npcCode = safe(importedItem.getNpcCode());
            if (npcCode.isEmpty()) {
                throw new IllegalArgumentException("npcCode is required for every item");
            }
            Item item = DeliveryReceiptSupport.resolveOrCreateItemByCode(pharmacyService, npcCode);
            if (item == null) {
                throw new IllegalArgumentException("No local or product master item found for npcCode: " + npcCode);
            }
            BigDecimal quantity = importedItem.getDeliveryQuantity();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("deliveryQuantity must be greater than zero for npcCode: " + npcCode);
            }
            BigDecimal acceptedQuantity = importedItem.getAcceptedQuantity() == null
                    ? quantity : importedItem.getAcceptedQuantity();
            if (acceptedQuantity.compareTo(BigDecimal.ZERO) < 0 || acceptedQuantity.compareTo(quantity) > 0) {
                throw new IllegalArgumentException("acceptedQuantity must be between zero and deliveryQuantity for npcCode: " + npcCode);
            }
            DeliveryReceiptLine line = new DeliveryReceiptLine();
            line.setItem(item);
            line.setBatchNumber(safe(importedItem.getBatch()));
            line.setOrderedQuantity(quantity);
            line.setReceivedQuantity(quantity);
            line.setAcceptedQuantity(acceptedQuantity);
            line.setConfirmationItemNo(safe(importedItem.getItemNo()));
            line.setRejectionReasonCode(safe(importedItem.getRejectionReasonCode()));
            line.setRejectionReasonDetail(safe(importedItem.getRejectionReasonDetail()));
            line.setNotes(safe(importedItem.getNotes()));
            if (importedItem.getExpiryDate() != null && !importedItem.getExpiryDate().trim().isEmpty()
                    && !isNotApplicable(importedItem.getExpiryDate())) {
                line.setExpiryDate(parseFlexibleDate(importedItem.getExpiryDate()));
            }
            if (acceptedQuantity.compareTo(quantity) < 0 && line.getRejectionReasonDetail().isEmpty()) {
                throw new IllegalArgumentException("rejectionReasonDetail is required when acceptedQuantity is less than deliveryQuantity for npcCode: " + npcCode);
            }
            if (acceptedQuantity.compareTo(quantity) < 0 && line.getRejectionReasonCode().isEmpty()) {
                line.setRejectionReasonCode("OTHER");
            }
            lines.add(line);
        }
        return lines;
    }

    private PharmacyLocation resolveMainStore() {
        List<PharmacyLocation> locations = pharmacyService.getAllPharmacyLocations();
        for (PharmacyLocation location : locations) {
            if (Boolean.TRUE.equals(location.getMainStore())) {
                return location;
            }
        }
        return locations.isEmpty() ? null : locations.get(0);
    }

    private PurchaseOrder resolveApprovedPurchaseOrder(DeliveryReceiptImportRequest request) {
        if (request.getPurchaseOrderId() != null) {
            PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(request.getPurchaseOrderId());
            if (purchaseOrder == null || !"APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus())) {
                throw new IllegalArgumentException("purchaseOrderId must reference an approved purchase order");
            }
            return purchaseOrder;
        }
        String orderNo = safe(request.getOrderNo());
        if (orderNo.isEmpty()) {
            throw new IllegalArgumentException("purchaseOrderId or orderNo is required and must match an approved purchase order");
        }
        for (PurchaseOrder purchaseOrder : pharmacyService.getPurchaseOrders("APPROVED", null, null, 0, 500)) {
            if (!"APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus())) {
                continue;
            }
            if (orderNo.equalsIgnoreCase(safe(purchaseOrder.getDispatchReference()))
                    || orderNo.equalsIgnoreCase("PO-" + purchaseOrder.getPurchaseOrderId())
                    || orderNo.equalsIgnoreCase(String.valueOf(purchaseOrder.getPurchaseOrderId()))) {
                return purchaseOrder;
            }
            if (purchaseOrder.getItems() != null) {
                for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getItems()) {
                    if (orderNo.equalsIgnoreCase(safe(purchaseOrderItem.getOrderNumber()))) {
                        return purchaseOrder;
                    }
                }
            }
        }
        throw new IllegalArgumentException("No approved purchase order matched orderNo: " + orderNo);
    }

    private Date parseFlexibleDate(String value) throws ParseException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || isNotApplicable(text)) {
            return null;
        }
        if (text.contains("T")) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(text);
        }
        return new SimpleDateFormat("yyyy-MM-dd").parse(text);
    }

    private boolean isNotApplicable(String value) {
        if (value == null) {
            return false;
        }
        String text = value.trim();
        return "N/A".equalsIgnoreCase(text) || "NA".equalsIgnoreCase(text) || "NOT APPLICABLE".equalsIgnoreCase(text);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private IntegrationLog createLog(IntegrationStatus status, DeliveryReceiptImportRequest request,
                                     DeliveryReceipt receipt, long start, String errorMessage) {
        IntegrationLog log = new IntegrationLog();
        log.setIntegrationType(IntegrationType.STOCK_RECEIPT_IMPORT);
        log.setStatus(status);
        log.setEndpoint("/module/rwandapharmacymanagement/delivery-receipts/import");
        log.setSummary(status == IntegrationStatus.SUCCESS
                ? "API stock receipt import completed"
                : (errorMessage == null ? "API stock receipt import failed" : errorMessage));
        log.setRequestPayload(truncate(String.valueOf(request == null ? null : request.getDeliveryId()) + " | items="
                + (request == null || request.getItems() == null ? 0 : request.getItems().size())));
        log.setResponsePayload(truncate(receipt == null ? errorMessage : "receiptNumber=" + receipt.getReceiptNumber()
                + ", lines=" + (receipt.getLines() == null ? 0 : receipt.getLines().size())));
        log.setDurationMs(System.currentTimeMillis() - start);
        log.setCreator(Context.getAuthenticatedUser());
        log.setDateCreated(new Date());
        return log;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 4000 ? value.substring(0, 4000) : value;
    }
}
