package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustment;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustmentLine;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReconciliationPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        List<InventoryBalance> stockBalances = activePharmacy == null ? Collections.<InventoryBalance>emptyList()
                : inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId());
        model.addAttribute("stockBalances", stockBalances);
        model.addAttribute("items", buildStockedItems(stockBalances));
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDate", required = false) String[] expiryDates,
                       @RequestParam(value = "comments", required = false) String comments,
                       HttpSession session,
                       UiUtils ui) throws ParseException {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setPharmacyLocation(activePharmacy);
        adjustment.setComments(comments);
        adjustment.setCreator(Context.getAuthenticatedUser());
        adjustment.setDateCreated(new java.util.Date());
        adjustment.setLines(buildLines(pharmacyService, adjustment, itemIds, quantities, batchNumbers, expiryDates));
        inventoryService.saveStockAdjustment(adjustment);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/reconciliation");
    }

    private List<StockAdjustmentLine> buildLines(PharmacyService pharmacyService,
                                                 StockAdjustment adjustment,
                                                 Integer[] itemIds,
                                                 BigDecimal[] quantities,
                                                 String[] batchNumbers,
                                                 String[] expiryDates) throws ParseException {
        List<StockAdjustmentLine> lines = new ArrayList<StockAdjustmentLine>();
        if (itemIds == null || quantities == null) {
            return lines;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            BigDecimal quantity = quantities.length > i ? quantities[i] : null;
            if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }
            StockAdjustmentLine line = new StockAdjustmentLine();
            line.setStockAdjustment(adjustment);
            line.setItem(pharmacyService.getItem(itemId));
            line.setQuantity(quantity);
            line.setBatchNumber(batchNumbers != null && batchNumbers.length > i ? batchNumbers[i] : null);
            String expiryDate = expiryDates != null && expiryDates.length > i ? expiryDates[i] : null;
            if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                line.setExpiryDate(new SimpleDateFormat("yyyy-MM-dd").parse(expiryDate));
            }
            lines.add(line);
        }
        return lines;
    }

    private List<Item> buildStockedItems(List<InventoryBalance> stockBalances) {
        Map<Integer, Item> itemsById = new LinkedHashMap<Integer, Item>();
        if (stockBalances != null) {
            for (InventoryBalance balance : stockBalances) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                        || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                itemsById.put(balance.getItem().getItemId(), balance.getItem());
            }
        }
        List<Item> items = new ArrayList<Item>(itemsById.values());
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item left, Item right) {
                String leftName = left == null || left.getName() == null ? "" : left.getName();
                String rightName = right == null || right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return items;
    }

}
