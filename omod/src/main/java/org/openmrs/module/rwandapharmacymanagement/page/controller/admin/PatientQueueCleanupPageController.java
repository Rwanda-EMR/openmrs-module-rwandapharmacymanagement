package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
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
import java.util.List;

@Controller
public class PatientQueueCleanupPageController {
    private static final String FLASH_UPDATED_COUNT = "rpm.patientQueueCleanup.updatedCount";
    private static final String FLASH_ERROR_MESSAGE = "rpm.patientQueueCleanup.errorMessage";
    private static final int PREVIEW_LIMIT = 50;

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "fromDate", required = false) String fromDate,
                    @RequestParam(value = "toDate", required = false) String toDate,
                    HttpSession session) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
        model.addAttribute("pharmacies", pharmacyService.getAllPharmacyLocations());
        model.addAttribute("selectedPharmacyLocationId", pharmacyLocationId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("updatedCount", FlashMessageSupport.consumeInteger(session, FLASH_UPDATED_COUNT));
        model.addAttribute("errorMessage", FlashMessageSupport.consumeString(session, FLASH_ERROR_MESSAGE));
        model.addAttribute("previewCount", null);
        model.addAttribute("previewRequests", Collections.emptyList());
        model.addAttribute("previewLimit", PREVIEW_LIMIT);
        model.addAttribute("selectedPharmacyName", pharmacyName(pharmacyService, pharmacyLocationId));

        if (hasText(fromDate) && hasText(toDate)) {
            try {
                Date fromInclusive = parseDate(fromDate);
                Date toExclusive = nextDay(parseDate(toDate));
                List<PatientDispensingRequest> matches = dispensingService.getPendingRequestsForCleanup(fromInclusive,
                        toExclusive, pharmacyLocationId);
                model.addAttribute("previewCount", matches.size());
                model.addAttribute("previewRequests", matches.size() > PREVIEW_LIMIT ? matches.subList(0, PREVIEW_LIMIT) : matches);
            }
            catch (IllegalArgumentException e) {
                model.addAttribute("errorMessage", e.getMessage());
            }
        }
    }

    public String post(@SpringBean("dispensingService") DispensingService dispensingService,
                       @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                       @RequestParam("fromDate") String fromDate,
                       @RequestParam("toDate") String toDate,
                       @RequestParam("reason") String reason,
                       @RequestParam(value = "confirm", required = false) String confirm,
                       HttpSession session,
                       UiUtils ui) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
        try {
            if (!"true".equalsIgnoreCase(confirm)) {
                throw new IllegalArgumentException("Confirm the cleanup before applying changes");
            }
            Date fromInclusive = parseDate(fromDate);
            Date toExclusive = nextDay(parseDate(toDate));
            int updated = dispensingService.markPendingRequestsNotServed(fromInclusive, toExclusive,
                    pharmacyLocationId, reason);
            FlashMessageSupport.set(session, FLASH_UPDATED_COUNT, Integer.valueOf(updated));
        }
        catch (IllegalArgumentException e) {
            FlashMessageSupport.set(session, FLASH_ERROR_MESSAGE, e.getMessage());
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/patientQueueCleanup");
    }

    private Date parseDate(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setLenient(false);
        try {
            return format.parse(value.trim());
        }
        catch (ParseException e) {
            throw new IllegalArgumentException("Dates must use yyyy-MM-dd format");
        }
    }

    private Date nextDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 1);
        return calendar.getTime();
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private String pharmacyName(PharmacyService pharmacyService, Integer pharmacyLocationId) {
        if (pharmacyLocationId == null) {
            return "All pharmacies";
        }
        PharmacyLocation pharmacyLocation = pharmacyService.getPharmacyLocation(pharmacyLocationId);
        return pharmacyLocation == null ? "Selected pharmacy" : pharmacyLocation.getName();
    }
}
