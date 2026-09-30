package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PurchaseOrderReconciliationReportPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "purchaseOrderId", required = false) Integer purchaseOrderId) {
        Date endDate = PurchaseOrderReconciliationReportSupport.startOfDay(
                PurchaseOrderReconciliationReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = PurchaseOrderReconciliationReportSupport.startOfDay(
                PurchaseOrderReconciliationReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        Date toExclusive = PurchaseOrderReconciliationReportSupport.nextDay(endDate);
        List<DeliveryReceipt> receipts = inventoryService.getDeliveryReceipts(startDate, toExclusive,
                pharmacyLocationId, purchaseOrderId);
        List<PurchaseOrder> purchaseOrders = reportPurchaseOrders(pharmacyService, receipts, purchaseOrderId,
                startDate, toExclusive);
        List<PurchaseOrderReconciliationReportSupport.Row> rows =
                PurchaseOrderReconciliationReportSupport.buildRows(purchaseOrders, receipts, purchaseOrderId == null);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", PurchaseOrderReconciliationReportSupport.totals(rows));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("purchaseOrders", selectorPurchaseOrders(pharmacyService, purchaseOrderId));
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("selectedPurchaseOrderId", purchaseOrderId);
        model.addAttribute("startDate", PurchaseOrderReconciliationReportSupport.formatDate(startDate));
        model.addAttribute("endDate", PurchaseOrderReconciliationReportSupport.formatDate(endDate));
    }

    private List<PurchaseOrder> reportPurchaseOrders(PharmacyService pharmacyService, List<DeliveryReceipt> receipts,
                                                     Integer purchaseOrderId, Date startDate, Date toDate) {
        if (purchaseOrderId != null) {
            List<PurchaseOrder> selected = new ArrayList<PurchaseOrder>();
            PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(purchaseOrderId);
            if (purchaseOrder != null && !Boolean.TRUE.equals(purchaseOrder.getVoided())) {
                selected.add(purchaseOrder);
            }
            return selected;
        }
        Map<Integer, PurchaseOrder> purchaseOrdersById = new LinkedHashMap<Integer, PurchaseOrder>();
        for (PurchaseOrder purchaseOrder : pharmacyService.getPurchaseOrders("ALL", startDate, toDate, 0, 1000)) {
            if (purchaseOrder != null && purchaseOrder.getPurchaseOrderId() != null) {
                purchaseOrdersById.put(purchaseOrder.getPurchaseOrderId(), purchaseOrder);
            }
        }
        if (receipts != null) {
            for (DeliveryReceipt receipt : receipts) {
                PurchaseOrder purchaseOrder = receipt == null ? null : receipt.getPurchaseOrder();
                if (purchaseOrder != null && purchaseOrder.getPurchaseOrderId() != null
                        && !purchaseOrdersById.containsKey(purchaseOrder.getPurchaseOrderId())) {
                    purchaseOrdersById.put(purchaseOrder.getPurchaseOrderId(), purchaseOrder);
                }
            }
        }
        return new ArrayList<PurchaseOrder>(purchaseOrdersById.values());
    }

    private List<PurchaseOrder> selectorPurchaseOrders(PharmacyService pharmacyService, Integer selectedPurchaseOrderId) {
        Map<Integer, PurchaseOrder> purchaseOrdersById = new LinkedHashMap<Integer, PurchaseOrder>();
        for (PurchaseOrder purchaseOrder : pharmacyService.getPurchaseOrders("ALL", null, null, 0, 500)) {
            if (purchaseOrder != null && purchaseOrder.getPurchaseOrderId() != null) {
                purchaseOrdersById.put(purchaseOrder.getPurchaseOrderId(), purchaseOrder);
            }
        }
        if (selectedPurchaseOrderId != null && !purchaseOrdersById.containsKey(selectedPurchaseOrderId)) {
            PurchaseOrder purchaseOrder = pharmacyService.getPurchaseOrder(selectedPurchaseOrderId);
            if (purchaseOrder != null && purchaseOrder.getPurchaseOrderId() != null) {
                purchaseOrdersById.put(purchaseOrder.getPurchaseOrderId(), purchaseOrder);
            }
        }
        return new ArrayList<PurchaseOrder>(purchaseOrdersById.values());
    }
}
