package org.openmrs.module.rwandapharmacymanagement.page.controller.purchaseorder;

import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.PurchaseOrderPriceRefreshResult;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

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

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "status", required = false) String status,
                    @RequestParam(value = "dateFrom", required = false) String dateFrom,
                    @RequestParam(value = "dateTo", required = false) String dateTo,
                    @RequestParam(value = "initiated", required = false) String initiated,
                    @RequestParam(value = "retried", required = false) String retried,
                    @RequestParam(value = "retryFailed", required = false) String retryFailed,
                    @RequestParam(value = "retryPriceFailed", required = false) String retryPriceFailed,
                    @RequestParam(value = "errorReference", required = false) String errorReference) {
        Date fromDate = parseDate(dateFrom, defaultFromDate());
        Date toDate = parseDate(dateTo, defaultToDate());
        model.addAttribute("purchaseOrders", pharmacyService.getPurchaseOrders(status, fromDate, toDate, 0, 200));
        model.addAttribute("selectedStatus", status == null ? "ALL" : status);
        model.addAttribute("dateFrom", formatDate(fromDate));
        model.addAttribute("dateTo", formatDate(toDate));
        model.addAttribute("initiated", initiated != null);
        model.addAttribute("retried", retried != null);
        model.addAttribute("retryFailed", retryFailed != null);
        model.addAttribute("retryPriceFailed", retryPriceFailed != null);
        model.addAttribute("errorReference", errorReference);
        model.addAttribute("pendingCount", safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("PENDING")));
        model.addAttribute("approvedCount", safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("APPROVED")));
        model.addAttribute("rejectedCount", safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("REJECTED")));
        model.addAttribute("dispatchedCount", safeInt(pharmacyService.countPurchaseOrdersByDispatchStatus("DISPATCHED")));
        model.addAttribute("canApprovePurchaseOrders",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @RequestParam("purchaseOrderId") Integer purchaseOrderId,
                       @RequestParam("action") String action,
                       UiUtils ui) {
        PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(purchaseOrderId);
        if (purchaseOrder == null || !"retryDispatch".equals(action)) {
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management");
        }
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS);

        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("status", "APPROVED");
        try {
            if (!"APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus())
                    || !"FAILED".equalsIgnoreCase(purchaseOrder.getDispatchStatus())) {
                params.put("retryFailed", "true");
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management", params);
            }

            if (isRmsPurchaseOrder(purchaseOrder)) {
                PurchaseOrderPriceRefreshResult priceRefreshResult = supplierIntegrationService.refreshPurchaseOrderPrices(purchaseOrder);
                if (priceRefreshResult.hasBlockingIssue()) {
                    params.put("retryPriceFailed", "true");
                    return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management", params);
                }
            }
            supplierIntegrationService.submitPurchaseOrder(purchaseOrder);
            params.put("retried", "true");
        }
        catch (RuntimeException e) {
            String reference = "RPM-PO-RETRY-" + System.currentTimeMillis();
            log.error("Purchase order retry failed. Reference: " + reference, e);
            params.put("errorReference", reference);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management", params);
    }

    private int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
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
