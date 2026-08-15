package org.openmrs.module.rwandapharmacymanagement.page.controller.pharmacyorder;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

@Controller("pharmacyItemRequestHistoryPageController")
public class HistoryPageController {

    private static final int MAX_RESULTS = 200;

    public void get(PageModel model,
                    @SpringBean("pharmacyItemRequestService") PharmacyItemRequestService pharmacyItemRequestService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "fromDate", required = false) String fromDate,
                    @RequestParam(value = "toDate", required = false) String toDate,
                    @RequestParam(value = "status", required = false) String status,
                    @RequestParam(value = "itemName", required = false) String itemName,
                    HttpSession session) throws ParseException {
        if (!Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK)
                && !Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        }
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        Date fromInclusive = parseDate(fromDate);
        Date toExclusive = addOneDay(parseDate(toDate));

        model.addAttribute("fromDate", safe(fromDate));
        model.addAttribute("toDate", safe(toDate));
        model.addAttribute("status", safe(status));
        model.addAttribute("itemName", safe(itemName));
        model.addAttribute("requests", activePharmacy == null
                ? Collections.emptyList()
                : pharmacyItemRequestService.getRequestsForPharmacy(activePharmacy, fromInclusive, toExclusive,
                status, itemName, MAX_RESULTS));
        model.addAttribute("maxResults", MAX_RESULTS);
    }

    private Date parseDate(String value) throws ParseException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setLenient(false);
        return formatter.parse(value.trim());
    }

    private Date addOneDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 1);
        return calendar.getTime();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
