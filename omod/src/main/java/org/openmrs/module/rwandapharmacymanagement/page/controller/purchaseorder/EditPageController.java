package org.openmrs.module.rwandapharmacymanagement.page.controller.purchaseorder;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.PurchaseOrderPriceRefreshResult;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EditPageController {

    private static final Log log = LogFactory.getLog(EditPageController.class);
    private static final String FLASH_SAVED = "rpm.purchaseOrderEdit.saved";
    private static final String FLASH_APPROVED = "rpm.purchaseOrderEdit.approved";
    private static final String FLASH_REJECTED = "rpm.purchaseOrderEdit.rejected";
    private static final String FLASH_PRICE_FAILED = "rpm.purchaseOrderEdit.priceFailed";
    private static final String FLASH_ERROR_REFERENCE = "rpm.purchaseOrderEdit.errorReference";

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                    @RequestParam("purchaseOrderId") Integer purchaseOrderId,
                    HttpSession session) {
        PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(purchaseOrderId);
        PurchaseOrderPriceRefreshResult priceRefreshResult = null;
        String errorReference = FlashMessageSupport.consumeString(session, FLASH_ERROR_REFERENCE);
        try {
            if (shouldRefreshPricesOnView(purchaseOrder)) {
                priceRefreshResult = supplierIntegrationService.refreshPurchaseOrderPrices(purchaseOrder);
            }
        }
        catch (RuntimeException e) {
            String reference = "RPM-PO-VIEW-" + System.currentTimeMillis();
            log.error("Purchase order price refresh failed on view. Reference: " + reference, e);
            errorReference = reference;
        }
        model.addAttribute("purchaseOrder", purchaseOrder);
        model.addAttribute("priceRefreshResult", priceRefreshResult);
        model.addAttribute("saved", FlashMessageSupport.consumeBoolean(session, FLASH_SAVED));
        model.addAttribute("approved", FlashMessageSupport.consumeBoolean(session, FLASH_APPROVED));
        model.addAttribute("rejected", FlashMessageSupport.consumeBoolean(session, FLASH_REJECTED));
        model.addAttribute("priceFailed", FlashMessageSupport.consumeBoolean(session, FLASH_PRICE_FAILED));
        model.addAttribute("errorReference", errorReference);
        model.addAttribute("canApprovePurchaseOrders",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS));
        model.addAttribute("canManagePurchaseOrders",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS)
                        || Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS));
        model.addAttribute("productMasterItems", pharmacyService.getAllProductMasterItems());
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @RequestParam("purchaseOrderId") Integer purchaseOrderId,
                       @RequestParam("action") String action,
                       @RequestParam(value = "purchaseOrderItemIds", required = false) Integer[] purchaseOrderItemIds,
                       @RequestParam(value = "quantities", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "unitPrices", required = false) BigDecimal[] unitPrices,
                       @RequestParam(value = "removeItemIds", required = false) Integer[] removeItemIds,
                       @RequestParam(value = "productMasterItemIds", required = false) Integer[] productMasterItemIds,
                       @RequestParam(value = "productMasterQuantities", required = false) BigDecimal[] productMasterQuantities,
                       HttpSession session,
                       UiUtils ui) {
        PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(purchaseOrderId);
        if (purchaseOrder == null) {
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management");
        }
        requirePurchaseOrderActionPrivilege(action);
        try {
            if (isEditable(purchaseOrder)) {
                applyChanges(pharmacyService, purchaseOrder, purchaseOrderItemIds, quantities, unitPrices, removeItemIds);
                addProductMasterItems(pharmacyService, purchaseOrder, productMasterItemIds, productMasterQuantities);
            }
            if ("approve".equals(action)) {
                if (!"PENDING".equalsIgnoreCase(purchaseOrder.getApprovalStatus())) {
                    throw new IllegalArgumentException("Only pending purchase orders can be approved.");
                }
                if (isRmsPurchaseOrder(purchaseOrder)) {
                    PurchaseOrderPriceRefreshResult priceRefreshResult = supplierIntegrationService.refreshPurchaseOrderPrices(purchaseOrder);
                    if (priceRefreshResult.hasBlockingIssue()) {
                        return redirectToEdit(ui, session, purchaseOrderId, FLASH_PRICE_FAILED);
                    }
                }
                purchaseOrder.setApprovalStatus("APPROVED");
                purchaseOrder.setApprovedBy(Context.getAuthenticatedUser());
                purchaseOrder.setApprovedDate(new Date());
                pharmacyService.savePurchaseOrder(purchaseOrder);
                supplierIntegrationService.submitPurchaseOrder(purchaseOrder);
                return redirectToEdit(ui, session, purchaseOrderId, FLASH_APPROVED);
            }
            if ("reject".equals(action)) {
                purchaseOrder.setApprovalStatus("REJECTED");
                purchaseOrder.setDispatchStatus("CANCELLED");
                pharmacyService.savePurchaseOrder(purchaseOrder);
                return redirectToEdit(ui, session, purchaseOrderId, FLASH_REJECTED);
            }
            pharmacyService.savePurchaseOrder(purchaseOrder);
            return redirectToEdit(ui, session, purchaseOrderId, FLASH_SAVED);
        }
        catch (RuntimeException e) {
            String reference = "RPM-PO-EDIT-" + System.currentTimeMillis();
            log.error("Purchase order review action failed. Reference: " + reference, e);
            return redirectToEditError(ui, session, purchaseOrderId, reference);
        }
    }

    private String redirectToEdit(UiUtils ui, HttpSession session, Integer purchaseOrderId, String flashKey) {
        FlashMessageSupport.set(session, flashKey, Boolean.TRUE);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("purchaseOrderId", purchaseOrderId);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/edit", params);
    }

    private String redirectToEditError(UiUtils ui, HttpSession session, Integer purchaseOrderId, String reference) {
        FlashMessageSupport.set(session, FLASH_ERROR_REFERENCE, reference);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("purchaseOrderId", purchaseOrderId);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/edit", params);
    }

    private void applyChanges(PharmacyService pharmacyService, PurchaseOrder purchaseOrder, Integer[] purchaseOrderItemIds,
                              BigDecimal[] quantities, BigDecimal[] unitPrices, Integer[] removeItemIds) {
        List<PurchaseOrderItem> retainedItems = new ArrayList<PurchaseOrderItem>();
        List<PurchaseOrderItem> removedItems = new ArrayList<PurchaseOrderItem>();
        BigDecimal totalCost = BigDecimal.ZERO;
        if (purchaseOrder.getItems() == null) {
            purchaseOrder.setItems(new ArrayList<PurchaseOrderItem>());
            purchaseOrder.setTotalCost(totalCost);
            return;
        }
        List<Integer> removed = new ArrayList<Integer>();
        if (removeItemIds != null) {
            for (Integer removeItemId : removeItemIds) {
                removed.add(removeItemId);
            }
        }
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            if (item.getPurchaseOrderItemId() != null && removed.contains(item.getPurchaseOrderItemId())) {
                removedItems.add(item);
                continue;
            }
            BigDecimal quantity = findQuantity(item.getPurchaseOrderItemId(), purchaseOrderItemIds, quantities);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                removedItems.add(item);
                continue;
            }
            item.setQuantity(quantity);
            item.setUnitPrice(findUnitPrice(item.getPurchaseOrderItemId(), purchaseOrderItemIds, unitPrices));
            item.setTotalPrice((item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice()).multiply(quantity));
            retainedItems.add(item);
            totalCost = totalCost.add(item.getTotalPrice());
        }
        purchaseOrder.getItems().clear();
        purchaseOrder.getItems().addAll(retainedItems);
        for (PurchaseOrderItem removedItem : removedItems) {
            if (removedItem.getPurchaseOrderItemId() != null) {
                removedItem.setPurchaseOrder(null);
                pharmacyService.deletePurchaseOrderItem(removedItem);
            }
        }
        purchaseOrder.setTotalCost(totalCost);
    }

    private void addProductMasterItems(PharmacyService pharmacyService, PurchaseOrder purchaseOrder,
                                       Integer[] productMasterItemIds, BigDecimal[] productMasterQuantities) {
        if (productMasterItemIds == null || productMasterQuantities == null) {
            return;
        }
        if (purchaseOrder.getItems() == null) {
            purchaseOrder.setItems(new ArrayList<PurchaseOrderItem>());
        }
        BigDecimal totalCost = purchaseOrder.getTotalCost() == null ? BigDecimal.ZERO : purchaseOrder.getTotalCost();
        String orderNumber = orderNumber(purchaseOrder);
        for (int i = 0; i < productMasterItemIds.length; i++) {
            Integer productMasterItemId = productMasterItemIds[i];
            BigDecimal quantity = productMasterQuantities.length > i ? productMasterQuantities[i] : null;
            if (productMasterItemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(productMasterItemId);
            if (productMasterItem == null) {
                continue;
            }
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(purchaseOrder);
            item.setProductMasterItem(productMasterItem);
            item.setOrderNumber(orderNumber);
            item.setProgram(requiredText(productMasterItem.getProgram(), requiredText(purchaseOrder.getProgram(), "General")));
            item.setItemName(requiredText(productMasterItem.getItemName(), "Unknown item"));
            item.setItemCode(requiredText(productMasterItem.getNpcCode(), "UNKNOWN"));
            item.setQuantity(quantity);
            item.setUnitPrice(productMasterItem.getItemPrice() == null ? BigDecimal.ZERO : productMasterItem.getItemPrice());
            item.setTotalPrice(item.getUnitPrice().multiply(quantity));
            item.setCreator(Context.getAuthenticatedUser());
            item.setDateCreated(new Date());
            totalCost = totalCost.add(item.getTotalPrice());
            purchaseOrder.getItems().add(item);
        }
        purchaseOrder.setTotalCost(totalCost);
    }

    private String orderNumber(PurchaseOrder purchaseOrder) {
        if (purchaseOrder != null && purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                if (item != null && item.getOrderNumber() != null && !item.getOrderNumber().trim().isEmpty()) {
                    return item.getOrderNumber();
                }
            }
        }
        String facilityId = purchaseOrder == null || purchaseOrder.getFacilityId() == null
                ? "" : purchaseOrder.getFacilityId();
        Integer purchaseOrderId = purchaseOrder == null ? null : purchaseOrder.getPurchaseOrderId();
        return "ord-" + facilityId + "-" + (purchaseOrderId == null ? "" : purchaseOrderId);
    }

    private String requiredText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean isEditable(PurchaseOrder purchaseOrder) {
        return purchaseOrder != null
                && ("PENDING".equalsIgnoreCase(purchaseOrder.getApprovalStatus())
                || "DRAFT".equalsIgnoreCase(purchaseOrder.getApprovalStatus()));
    }

    private void requirePurchaseOrderActionPrivilege(String action) {
        if ("approve".equalsIgnoreCase(action) || "reject".equalsIgnoreCase(action)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS);
        } else if (!Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS)
                && !Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS);
        }
    }

    private BigDecimal findQuantity(Integer purchaseOrderItemId, Integer[] purchaseOrderItemIds, BigDecimal[] quantities) {
        if (purchaseOrderItemIds == null || quantities == null) {
            return null;
        }
        for (int i = 0; i < purchaseOrderItemIds.length; i++) {
            if (purchaseOrderItemId != null && purchaseOrderItemId.equals(purchaseOrderItemIds[i])) {
                return quantities.length > i ? quantities[i] : null;
            }
        }
        return null;
    }

    private BigDecimal findUnitPrice(Integer purchaseOrderItemId, Integer[] purchaseOrderItemIds, BigDecimal[] unitPrices) {
        if (purchaseOrderItemIds == null || unitPrices == null) {
            return BigDecimal.ZERO;
        }
        for (int i = 0; i < purchaseOrderItemIds.length; i++) {
            if (purchaseOrderItemId != null && purchaseOrderItemId.equals(purchaseOrderItemIds[i])) {
                BigDecimal unitPrice = unitPrices.length > i ? unitPrices[i] : null;
                return unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : unitPrice;
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean shouldRefreshPricesOnView(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return false;
        }
        if (!isRmsPurchaseOrder(purchaseOrder)) {
            return false;
        }
        if ("PENDING".equalsIgnoreCase(purchaseOrder.getApprovalStatus())) {
            return true;
        }
        return "APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus())
                && "FAILED".equalsIgnoreCase(purchaseOrder.getDispatchStatus());
    }

    private boolean isRmsPurchaseOrder(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null || purchaseOrder.getSupplier() == null) {
            return false;
        }
        String code = purchaseOrder.getSupplier().getCode();
        String name = purchaseOrder.getSupplier().getName();
        return "RMS".equalsIgnoreCase(code == null ? "" : code.trim())
                || (name != null && name.toUpperCase().contains("RMS"));
    }
}
