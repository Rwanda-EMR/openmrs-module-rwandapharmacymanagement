package org.openmrs.module.rwandapharmacymanagement.page.controller.purchaseorder;

import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.PurchaseOrderPriceRefreshResult;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ManagementPageController {

    private static final Log log = LogFactory.getLog(ManagementPageController.class);
    private static final String FLASH_RETRIED = "rpm.purchaseOrderManagement.retried";
    private static final String FLASH_RETRY_FAILED = "rpm.purchaseOrderManagement.retryFailed";
    private static final String FLASH_RETRY_PRICE_FAILED = "rpm.purchaseOrderManagement.retryPriceFailed";
    private static final String FLASH_ERROR_REFERENCE = "rpm.purchaseOrderManagement.errorReference";
    public static final String FLASH_INITIATED = "rpm.purchaseOrderManagement.initiated";

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "status", required = false) String status,
                    @RequestParam(value = "dateFrom", required = false) String dateFrom,
                    @RequestParam(value = "dateTo", required = false) String dateTo,
                    HttpSession session) {
        Date fromDate = parseDate(dateFrom, defaultFromDate());
        Date toDate = parseDate(dateTo, defaultToDate());
        model.addAttribute("purchaseOrders", pharmacyService.getPurchaseOrders(status, fromDate, toDate, 0, 200));
        model.addAttribute("selectedStatus", status == null ? "ALL" : status);
        model.addAttribute("dateFrom", formatDate(fromDate));
        model.addAttribute("dateTo", formatDate(toDate));
        model.addAttribute("initiated", FlashMessageSupport.consumeBoolean(session, FLASH_INITIATED));
        model.addAttribute("retried", FlashMessageSupport.consumeBoolean(session, FLASH_RETRIED));
        model.addAttribute("retryFailed", FlashMessageSupport.consumeBoolean(session, FLASH_RETRY_FAILED));
        model.addAttribute("retryPriceFailed", FlashMessageSupport.consumeBoolean(session, FLASH_RETRY_PRICE_FAILED));
        model.addAttribute("errorReference", FlashMessageSupport.consumeString(session, FLASH_ERROR_REFERENCE));
        model.addAttribute("pendingCount", safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("PENDING")));
        model.addAttribute("draftCount", safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("DRAFT")));
        model.addAttribute("approvedCount", safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("APPROVED")));
        model.addAttribute("rejectedCount", safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("REJECTED")));
        model.addAttribute("dispatchedCount", safeInt(pharmacyService.countPurchaseOrdersByDispatchStatus("DISPATCHED")));
        model.addAttribute("canApprovePurchaseOrders",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS));
        model.addAttribute("canRetryPurchaseOrderDispatch", canRetryPurchaseOrderDispatch());
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @RequestParam("purchaseOrderId") Integer purchaseOrderId,
                       @RequestParam("action") String action,
                       HttpSession session,
                       UiUtils ui) {
        PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(purchaseOrderId);
        if (purchaseOrder == null || !"retryDispatch".equals(action)) {
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management");
        }
        requireRetryPurchaseOrderDispatchPrivilege();

        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("status", "APPROVED");
        try {
            if (!"APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus())
                    || !"FAILED".equalsIgnoreCase(purchaseOrder.getDispatchStatus())) {
                FlashMessageSupport.set(session, FLASH_RETRY_FAILED, Boolean.TRUE);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management", params);
            }

            if (isRmsPurchaseOrder(purchaseOrder)) {
                PurchaseOrderPriceRefreshResult priceRefreshResult = supplierIntegrationService.refreshPurchaseOrderPrices(purchaseOrder);
                if (priceRefreshResult.hasBlockingIssue()) {
                    FlashMessageSupport.set(session, FLASH_RETRY_PRICE_FAILED, Boolean.TRUE);
                    return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management", params);
                }
            }
            supplierIntegrationService.submitPurchaseOrder(purchaseOrder);
            FlashMessageSupport.set(session, FLASH_RETRIED, Boolean.TRUE);
        }
        catch (RuntimeException e) {
            String reference = "RPM-PO-RETRY-" + System.currentTimeMillis();
            log.error("Purchase order retry failed. Reference: " + reference, e);
            FlashMessageSupport.set(session, FLASH_ERROR_REFERENCE, reference);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management", params);
    }

    private int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private boolean canRetryPurchaseOrderDispatch() {
        return Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS)
                || Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS);
    }

    private void requireRetryPurchaseOrderDispatchPrivilege() {
        if (canRetryPurchaseOrderDispatch()) {
            return;
        }
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS);
    }

    private Date parseDate(String value, Date defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(value);
        }
        catch (ParseException e) {
            return defaultValue;
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
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

    private Date defaultFromDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -4);
        return startOfDay(calendar.getTime());
    }

    private Date defaultToDate() {
        return endOfDay(new Date());
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date endOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
}
