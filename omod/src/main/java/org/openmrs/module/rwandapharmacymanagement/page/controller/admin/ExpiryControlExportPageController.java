package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.openmrs.module.rwandapharmacymanagement.reporting.StockReportExcelRenderer;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ExpiryControlExportPageController {

    public void get(@SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    HttpServletResponse response) throws IOException {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK);
        PharmacyLocation mainStore = resolveMainPharmacy(pharmacyService);
        if (mainStore == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No active main pharmacy is configured");
            return;
        }

        List<StockTransaction> transactions = inventoryService.getStockTransactions(monthsAgo(12), null,
                mainStore.getPharmacyLocationId());
        Map<String, StockTransaction> disposalsByWriteOffId = disposalsByWriteOffId(transactions);
        List<List<String>> rows = new ArrayList<List<String>>();
        for (StockTransaction writeOff : transactions) {
            if (writeOff == null || writeOff.getTransactionType() != StockTransactionType.WRITE_OFF) {
                continue;
            }
            StockTransaction disposal = disposalsByWriteOffId.get(String.valueOf(writeOff.getStockTransactionId()));
            rows.add(Arrays.asList(
                    string(writeOff.getStockTransactionId()),
                    formatDateTime(writeOff.getDateCreated()),
                    writeOff.getPharmacyLocation() == null ? "" : writeOff.getPharmacyLocation().getName(),
                    writeOff.getItem() == null ? "" : writeOff.getItem().getCode(),
                    writeOff.getItem() == null ? "" : writeOff.getItem().getName(),
                    emptyToDefault(writeOff.getBatchNumber(), "No batch"),
                    formatDate(writeOff.getExpiryDate()),
                    decimalAbs(writeOff.getQuantity()),
                    emptyToDefault(writeOff.getRemarks(), ""),
                    displayUser(writeOff.getPerformedBy()),
                    disposal == null ? "Awaiting disposal" : "Disposed",
                    disposal == null ? "" : formatDateTime(disposal.getDateCreated()),
                    disposal == null ? "" : emptyToDefault(disposal.getRemarks(), ""),
                    disposal == null ? "" : displayUser(disposal.getPerformedBy()),
                    disposal == null ? "" : string(disposal.getStockTransactionId())
            ));
        }

        byte[] workbook = StockReportExcelRenderer.render("Write-off Disposal", headers(), rows);
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=\"write-off-disposal-report.xls\"");
        response.setContentLength(workbook.length);
        response.getOutputStream().write(workbook);
        response.getOutputStream().flush();
    }

    private List<String> headers() {
        return Arrays.asList("Write-off ID", "Date Written Off", "Pharmacy", "Item Code", "Item", "Batch",
                "Expiry Date", "Quantity Written Off", "Write-off Reason", "Written Off By", "Disposal Status",
                "Disposal Date", "Disposal Method / Notes", "Disposed By", "Disposal Transaction ID");
    }

    private Map<String, StockTransaction> disposalsByWriteOffId(List<StockTransaction> transactions) {
        Map<String, StockTransaction> disposals = new LinkedHashMap<String, StockTransaction>();
        for (StockTransaction transaction : transactions) {
            if (transaction != null && transaction.getTransactionType() == StockTransactionType.DISPOSAL
                    && transaction.getReferenceNumber() != null) {
                disposals.put(transaction.getReferenceNumber(), transaction);
            }
        }
        return disposals;
    }

    private PharmacyLocation resolveMainPharmacy(PharmacyService pharmacyService) {
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        return mainStore != null && mainStore.getPharmacyLocationId() != null
                && !Boolean.FALSE.equals(mainStore.getActive())
                && Boolean.TRUE.equals(mainStore.getMainStore()) ? mainStore : null;
    }

    private Date monthsAgo(int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -months);
        return calendar.getTime();
    }

    private String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String formatDateTime(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private String decimalAbs(BigDecimal value) {
        return value == null ? "0" : StockReportExcelRenderer.decimal(value.abs());
    }

    private String displayUser(User user) {
        return user == null ? "" : user.getDisplayString();
    }

    private String emptyToDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
