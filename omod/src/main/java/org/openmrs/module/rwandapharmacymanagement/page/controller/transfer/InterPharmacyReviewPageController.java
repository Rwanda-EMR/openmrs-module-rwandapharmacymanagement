package org.openmrs.module.rwandapharmacymanagement.page.controller.transfer;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InterPharmacyReviewPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam("transferId") Integer transferId,
                    HttpSession session) {
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        InterPharmacyTransfer transfer = inventoryService.getTransfer(transferId);
        if (transfer == null || activePharmacy == null || !canViewTransfer(transfer, activePharmacy)) {
            model.addAttribute("transfer", null);
            return;
        }
        List<PharmacyLocation> pharmacies = new ArrayList<PharmacyLocation>();
        pharmacies.add(transfer.getSourcePharmacyLocation());
        model.addAttribute("transfer", transfer);
        model.addAttribute("activePharmacy", activePharmacy);
        model.addAttribute("stockByPharmacy", buildStockByPharmacy(pharmacies, inventoryService));
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("transferId") Integer transferId,
                       @RequestParam("action") String action,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDate", required = false) String[] expiryDates,
                       @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                       HttpSession session,
                       UiUtils ui) throws ParseException {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        InterPharmacyTransfer transfer = inventoryService.getTransfer(transferId);
        if (!isSourcePharmacy(transfer, activePharmacy)) {
            throw new IllegalArgumentException("Only the supplying pharmacy can transfer or reject this request");
        }
        if ("reject".equalsIgnoreCase(action)) {
            inventoryService.rejectTransfer(transferId, rejectionReason);
        } else if ("transfer".equalsIgnoreCase(action) || "serve".equalsIgnoreCase(action) || "approve".equalsIgnoreCase(action)) {
            inventoryService.serveTransfer(transferId, quantities, batchNumbers, parseDates(expiryDates));
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "transfer/interPharmacyReview",
                java.util.Collections.<String, Object>singletonMap("transferId", transferId));
    }

    private boolean canViewTransfer(InterPharmacyTransfer transfer, PharmacyLocation activePharmacy) {
        Integer activeId = activePharmacy == null ? null : activePharmacy.getPharmacyLocationId();
        if (activeId == null || transfer == null) {
            return false;
        }
        return activeId.equals(transfer.getSourcePharmacyLocation() == null ? null : transfer.getSourcePharmacyLocation().getPharmacyLocationId())
                || activeId.equals(transfer.getDestinationPharmacyLocation() == null ? null : transfer.getDestinationPharmacyLocation().getPharmacyLocationId());
    }

    private boolean isSourcePharmacy(InterPharmacyTransfer transfer, PharmacyLocation activePharmacy) {
        Integer activeId = activePharmacy == null ? null : activePharmacy.getPharmacyLocationId();
        Integer sourceId = transfer == null || transfer.getSourcePharmacyLocation() == null ? null
                : transfer.getSourcePharmacyLocation().getPharmacyLocationId();
        return activeId != null && activeId.equals(sourceId);
    }

    private Map<Integer, List<Map<String, Object>>> buildStockByPharmacy(List<PharmacyLocation> pharmacies, InventoryService inventoryService) {
        Map<Integer, List<Map<String, Object>>> stockByPharmacy = new LinkedHashMap<Integer, List<Map<String, Object>>>();
        for (PharmacyLocation pharmacy : pharmacies) {
            if (pharmacy == null || pharmacy.getPharmacyLocationId() == null) {
                continue;
            }
            List<Map<String, Object>> itemRows = new ArrayList<Map<String, Object>>();
            Map<Integer, Map<String, Object>> byItem = new LinkedHashMap<Integer, Map<String, Object>>();
            for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(pharmacy.getPharmacyLocationId())) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                    continue;
                }
                Integer itemId = balance.getItem().getItemId();
                Map<String, Object> itemRow = byItem.get(itemId);
                if (itemRow == null) {
                    itemRow = new LinkedHashMap<String, Object>();
                    itemRow.put("itemId", itemId);
                    itemRow.put("name", balance.getItem().getName());
                    itemRow.put("available", BigDecimal.ZERO);
                    itemRow.put("unitPrice", balance.getUnitPrice());
                    itemRow.put("batches", new ArrayList<Map<String, Object>>());
                    byItem.put(itemId, itemRow);
                    itemRows.add(itemRow);
                }
                if (itemRow.get("unitPrice") == null && balance.getUnitPrice() != null) {
                    itemRow.put("unitPrice", balance.getUnitPrice());
                }
                itemRow.put("available", ((BigDecimal) itemRow.get("available")).add(balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand()));
                Map<String, Object> batch = new LinkedHashMap<String, Object>();
                batch.put("batchNumber", balance.getBatchNumber());
                batch.put("expiryDate", balance.getExpiryDate());
                batch.put("available", balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand());
                batch.put("unitPrice", balance.getUnitPrice());
                ((List<Map<String, Object>>) itemRow.get("batches")).add(batch);
            }
            stockByPharmacy.put(pharmacy.getPharmacyLocationId(), itemRows);
        }
        return stockByPharmacy;
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
}
