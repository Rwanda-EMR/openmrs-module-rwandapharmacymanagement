package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
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
public class WriteOffDisposalReportPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "itemId", required = false) Integer itemId,
                    @RequestParam(value = "status", required = false) String statusParam) {
        Date endDate = WriteOffDisposalReportSupport.startOfDay(
                WriteOffDisposalReportSupport.parseDate(endDateParam, new Date()));
        Date startDate = WriteOffDisposalReportSupport.startOfDay(
                WriteOffDisposalReportSupport.parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        String status = WriteOffDisposalReportSupport.normalizedStatus(statusParam);
        List<WriteOffDisposalReportSupport.Row> rows = WriteOffDisposalReportSupport.buildRows(inventoryService,
                startDate, WriteOffDisposalReportSupport.nextDay(endDate), pharmacyLocationId, itemId, status);

        model.addAttribute("rows", rows);
        model.addAttribute("totals", WriteOffDisposalReportSupport.totals(rows));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("items", inventoryItems(inventoryService));
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("selectedItemId", itemId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("startDate", WriteOffDisposalReportSupport.formatDate(startDate));
        model.addAttribute("endDate", WriteOffDisposalReportSupport.formatDate(endDate));
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
