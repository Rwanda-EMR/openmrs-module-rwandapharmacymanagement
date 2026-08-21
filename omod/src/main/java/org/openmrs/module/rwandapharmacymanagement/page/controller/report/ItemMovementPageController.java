package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentStockReturnService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemMovementPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("departmentBulkRequestService") DepartmentBulkRequestService departmentBulkRequestService,
                    @SpringBean("departmentStockReturnService") DepartmentStockReturnService departmentStockReturnService,
                    @SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "itemId", required = false) Integer itemId,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam) {
        Date endDate = ItemMovementReportSupport.startOfDay(parseDate(endDateParam, new Date()));
        Date startDate = ItemMovementReportSupport.startOfDay(parseDate(startDateParam, defaultStartDate(endDate)));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        Item selectedItem = itemId == null ? null : pharmacyService.getItem(itemId);
        List<ItemMovementReportSupport.Row> rows = selectedItem == null
                ? java.util.Collections.<ItemMovementReportSupport.Row>emptyList()
                : ItemMovementReportSupport.buildRows(inventoryService, selectedItem, startDate,
                ItemMovementReportSupport.nextDay(endDate), pharmacyLocationId, departmentBulkRequestService,
                departmentStockReturnService, pharmacyItemRequestService);
        BigDecimal openingBalance = selectedItem == null ? BigDecimal.ZERO
                : ItemMovementReportSupport.openingBalance(inventoryService, selectedItem.getItemId(), startDate, pharmacyLocationId);
        BigDecimal closingBalance = ItemMovementReportSupport.closingBalance(rows, openingBalance);
        model.addAttribute("items", ItemMovementReportSupport.sortedItems(inventoryItems(inventoryService)));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("selectedItem", selectedItem);
        model.addAttribute("selectedItemId", itemId);
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("startDate", ItemMovementReportSupport.formatDate(startDate));
        model.addAttribute("endDate", ItemMovementReportSupport.formatDate(endDate));
        model.addAttribute("rows", rows);
        model.addAttribute("openingBalance", openingBalance);
        model.addAttribute("closingBalance", closingBalance);
        model.addAttribute("totalReceived", ItemMovementReportSupport.totalReceived(rows));
        model.addAttribute("totalIssued", ItemMovementReportSupport.totalIssued(rows));
        model.addAttribute("currentMonthsOfStock", selectedItem == null ? null
                : ItemMovementReportSupport.currentMonthsOfStock(closingBalance, inventoryService, selectedItem.getItemId(),
                startDate, ItemMovementReportSupport.nextDay(endDate), pharmacyLocationId));
    }

    private Date parseDate(String value, Date fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(value.trim());
        }
        catch (ParseException ex) {
            return fallback;
        }
    }

    private Date defaultStartDate(Date endDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate == null ? new Date() : endDate);
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }

    private List<Item> inventoryItems(InventoryService inventoryService) {
        Map<Integer, Item> itemsById = new LinkedHashMap<Integer, Item>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            itemsById.put(balance.getItem().getItemId(), balance.getItem());
        }
        return new ArrayList<Item>(itemsById.values());
    }
}
