package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Controller
public class DispensingReportPageController {

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "startDate", required = false) String startDateParam,
                    @RequestParam(value = "endDate", required = false) String endDateParam,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId) {
        Date endDate = DispensingReportSupport.startOfDay(parseDate(endDateParam, new Date()));
        Date startDate = DispensingReportSupport.startOfDay(parseDate(startDateParam, endDate));
        if (startDate.after(endDate)) {
            startDate = endDate;
        }
        List<DispenseRecord> records = dispensingService.getDispenseRecords(startDate,
                DispensingReportSupport.nextDay(endDate), pharmacyLocationId);
        List<DispensingReportSupport.Row> rows = DispensingReportSupport.buildRows(records);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", DispensingReportSupport.totals(rows));
        model.addAttribute("pharmacies", StockReportSupport.getActivePharmacies(pharmacyService));
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("startDate", DispensingReportSupport.formatDate(startDate));
        model.addAttribute("endDate", DispensingReportSupport.formatDate(endDate));
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
