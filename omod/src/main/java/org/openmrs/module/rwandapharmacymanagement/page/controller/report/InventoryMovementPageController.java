package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Controller
public class InventoryMovementPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "materialType", required = false) String materialType,
                    @RequestParam(value = "program", required = false) String program,
                    @RequestParam(value = "mode", required = false) String mode) {
        Date endDate = InventoryMovementReportSupport.startOfDay(parseDate(endDateParam, new Date()));
        Date startDate = InventoryMovementReportSupport.startOfDay(parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        String reportMode = InventoryMovementReportSupport.MODE_DETAIL.equalsIgnoreCase(mode)
                ? InventoryMovementReportSupport.MODE_DETAIL : InventoryMovementReportSupport.MODE_SUMMARY;
        List<InventoryMovementReportSupport.Row> rows = InventoryMovementReportSupport.buildRows(inventoryService, pharmacyService,
                startDate, InventoryMovementReportSupport.nextDay(endDate), pharmacyLocationId, materialType, program, reportMode);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", InventoryMovementReportSupport.totals(rows));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("materialTypes", StockReportSupport.getMaterialTypes(pharmacyService));
        model.addAttribute("programs", StockReportSupport.getPrograms(pharmacyService));
        model.addAttribute("selectedMaterialType", materialType);
        model.addAttribute("selectedProgram", program);
        model.addAttribute("mode", reportMode);
        model.addAttribute("startDate", InventoryMovementReportSupport.formatDate(startDate));
        model.addAttribute("endDate", InventoryMovementReportSupport.formatDate(endDate));
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
}
