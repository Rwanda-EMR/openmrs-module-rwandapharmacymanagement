package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ExpiryControlPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "updated", required = false) Boolean updated,
                    @RequestParam(value = "writtenOff", required = false) Boolean writtenOff,
                    @RequestParam(value = "disposed", required = false) Boolean disposed) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK);
        PharmacyLocation activePharmacy = resolveMainPharmacy(pharmacyService);
        model.addAttribute("activePharmacy", activePharmacy);
        model.addAttribute("pharmacySelectionRequired", Boolean.FALSE);
        if (activePharmacy != null) {
            model.addAttribute("pharmacySelectionLocationName", activePharmacy.getName());
        }
        List<InventoryBalance> stockBalances = activePharmacy == null ? Collections.<InventoryBalance>emptyList()
                : inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId());
        model.addAttribute("stockBalances", stockBalances);
        List<StockTransaction> recentWriteOffs = activePharmacy == null ? Collections.<StockTransaction>emptyList()
                : recentWriteOffs(inventoryService, activePharmacy.getPharmacyLocationId());
        model.addAttribute("recentWriteOffs", recentWriteOffs);
        model.addAttribute("disposedWriteOffReferences", disposedWriteOffReferences(inventoryService, activePharmacy));
        model.addAttribute("updated", Boolean.TRUE.equals(updated));
        model.addAttribute("writtenOff", Boolean.TRUE.equals(writtenOff));
        model.addAttribute("disposed", Boolean.TRUE.equals(disposed));
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "inventoryBalanceId", required = false) Integer inventoryBalanceId,
                       @RequestParam(value = "expiryDate", required = false) String expiryDate,
                       @RequestParam(value = "quantity", required = false) BigDecimal quantity,
                       @RequestParam(value = "reason", required = false) String reason,
                       @RequestParam(value = "writeOffStockTransactionId", required = false) Integer writeOffStockTransactionId,
                       @RequestParam(value = "disposalMethod", required = false) String disposalMethod,
                       @RequestParam(value = "notes", required = false) String notes,
                       @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                       UiUtils ui) throws ParseException {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK);
        PharmacyLocation activePharmacy = requireMainPharmacy(pharmacyService, pharmacyLocationId);
        Map<String, Object> redirectParams = new LinkedHashMap<String, Object>();
        redirectParams.put("pharmacyLocationId", activePharmacy.getPharmacyLocationId());
        String normalizedAction = action == null ? "updateExpiry" : action.trim();
        if ("writeOff".equalsIgnoreCase(normalizedAction)) {
            inventoryService.writeOffInventoryBalance(inventoryBalanceId, activePharmacy.getPharmacyLocationId(), quantity, reason);
            redirectParams.put("writtenOff", Boolean.TRUE);
        } else if ("dispose".equalsIgnoreCase(normalizedAction)) {
            inventoryService.recordWrittenOffStockDisposal(writeOffStockTransactionId, disposalMethod, notes);
            redirectParams.put("disposed", Boolean.TRUE);
        } else {
            inventoryService.updateBatchExpiryDate(inventoryBalanceId, activePharmacy.getPharmacyLocationId(), parseDate(expiryDate));
            redirectParams.put("updated", Boolean.TRUE);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/expiryControl", redirectParams);
    }

    private PharmacyLocation resolveMainPharmacy(PharmacyService pharmacyService) {
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        return isUsableMainPharmacy(mainStore) ? mainStore : null;
    }

    private PharmacyLocation requireMainPharmacy(PharmacyService pharmacyService, Integer pharmacyLocationId) {
        PharmacyLocation mainStore = resolveMainPharmacy(pharmacyService);
        if (mainStore == null) {
            throw new IllegalArgumentException("No active main pharmacy is configured");
        }
        if (pharmacyLocationId != null && !pharmacyLocationId.equals(mainStore.getPharmacyLocationId())) {
            throw new IllegalArgumentException("Batch expiry control is only available for the main pharmacy");
        }
        return mainStore;
    }

    private boolean isUsableMainPharmacy(PharmacyLocation pharmacyLocation) {
        return pharmacyLocation != null && pharmacyLocation.getPharmacyLocationId() != null
                && Boolean.TRUE.equals(pharmacyLocation.getActive())
                && Boolean.TRUE.equals(pharmacyLocation.getMainStore());
    }

    private Date parseDate(String value) throws ParseException {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setLenient(false);
        return format.parse(trimmed);
    }

    private List<StockTransaction> recentWriteOffs(InventoryService inventoryService, Integer pharmacyLocationId) {
        List<StockTransaction> writeOffs = new ArrayList<StockTransaction>();
        Date from = monthsAgo(12);
        for (StockTransaction transaction : inventoryService.getStockTransactions(from, null, pharmacyLocationId)) {
            if (transaction != null && transaction.getTransactionType() == StockTransactionType.WRITE_OFF) {
                writeOffs.add(transaction);
            }
        }
        Collections.reverse(writeOffs);
        return writeOffs;
    }

    private Set<String> disposedWriteOffReferences(InventoryService inventoryService, PharmacyLocation pharmacyLocation) {
        Set<String> references = new HashSet<String>();
        if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null) {
            return references;
        }
        for (StockTransaction transaction : inventoryService.getStockTransactions(monthsAgo(12), null,
                pharmacyLocation.getPharmacyLocationId())) {
            if (transaction != null && transaction.getTransactionType() == StockTransactionType.DISPOSAL
                    && transaction.getReferenceNumber() != null) {
                references.add(transaction.getReferenceNumber());
            }
        }
        return references;
    }

    private Date monthsAgo(int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -months);
        return calendar.getTime();
    }
}
