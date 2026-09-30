package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportExcelRenderer;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportPdfRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PurchaseOrderReconciliationReportExportPageController {

    public void get(@SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam("format") String format,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "purchaseOrderId", required = false) Integer purchaseOrderId,
                    HttpServletResponse response) throws IOException {
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
        PurchaseOrderReconciliationReportSupport.Totals totals = PurchaseOrderReconciliationReportSupport.totals(rows);
        List<List<String>> summaryRows = ReportExportSummarySupport.rows(
                "Purchase orders", ReportExportSummarySupport.integer(totals.purchaseOrderCount),
                "Receipts", ReportExportSummarySupport.integer(totals.receiptCount),
                "Lines", ReportExportSummarySupport.integer(totals.lineCount),
                "Ordered", ReportExportSummarySupport.decimal(totals.orderedQuantity),
                "Received", ReportExportSummarySupport.decimal(totals.receivedQuantity),
                "Difference", ReportExportSummarySupport.decimal(totals.differenceQuantity),
                "Fully received", ReportExportSummarySupport.integer(totals.fullyReceivedLineCount),
                "Partially received", ReportExportSummarySupport.integer(totals.partiallyReceivedLineCount),
                "Not received", ReportExportSummarySupport.integer(totals.notReceivedLineCount),
                "Receipt-only", ReportExportSummarySupport.integer(totals.receiptOnlyLineCount)
        );
        String title = "Purchase Order Reconciliation "
                + PurchaseOrderReconciliationReportSupport.formatDate(startDate) + " to "
                + PurchaseOrderReconciliationReportSupport.formatDate(endDate);
        if ("excel".equalsIgnoreCase(format)) {
            byte[] workbook = StockReportExcelRenderer.render("PO Reconciliation",
                    summaryRows,
                    PurchaseOrderReconciliationReportSupport.headers(),
                    PurchaseOrderReconciliationReportSupport.body(rows));
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=\"purchase-order-reconciliation-report.xls\"");
            response.setContentLength(workbook.length);
            response.getOutputStream().write(workbook);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = StockReportPdfRenderer.render(title,
                    summaryRows,
                    PurchaseOrderReconciliationReportSupport.headers(),
                    PurchaseOrderReconciliationReportSupport.body(rows));
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"purchase-order-reconciliation-report.pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
            return;
        }
        response.getOutputStream().flush();
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
}
