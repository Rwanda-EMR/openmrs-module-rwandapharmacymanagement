package org.openmrs.module.rwandapharmacymanagement.page.controller.integration;

import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.integration.hie.HieRetryService;
import org.openmrs.module.rwandapharmacymanagement.integration.sms.SmsRetryService;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class PendingHiePageController {

    private static final String FLASH_MESSAGE = "rpm.pendingHie.message";

    public void get(PageModel model,
                    @SpringBean("integrationLogService") IntegrationLogService integrationLogService,
                    HttpSession session) {
        model.addAttribute("pendingLogs", integrationLogService.getPendingHieLogs(200));
        model.addAttribute("pendingSmsLogs", integrationLogService.getPendingSmsLogs(200));
        model.addAttribute("message", FlashMessageSupport.consumeString(session, FLASH_MESSAGE));
    }

    public String post(@SpringBean("hieRetryService") HieRetryService hieRetryService,
                       @SpringBean("smsRetryService") SmsRetryService smsRetryService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "integrationLogId", required = false) Integer integrationLogId,
                       HttpSession session,
                       UiUtils ui) {
        if ("retryOne".equals(action) && integrationLogId != null) {
            boolean success = hieRetryService.retryLog(integrationLogId);
            FlashMessageSupport.set(session, FLASH_MESSAGE, success
                    ? "Queued HIE payload sent successfully."
                    : "Queued HIE payload was attempted but is still pending.");
        } else if ("retryOneSms".equals(action) && integrationLogId != null) {
            boolean success = smsRetryService.retryLog(integrationLogId);
            FlashMessageSupport.set(session, FLASH_MESSAGE, success
                    ? "Queued SMS sent successfully."
                    : "Queued SMS was attempted but is still pending.");
        } else if ("retryAllSms".equals(action)) {
            int successful = smsRetryService.retryPending();
            FlashMessageSupport.set(session, FLASH_MESSAGE, successful + " queued SMS notification(s) sent successfully.");
        } else {
            int successful = hieRetryService.retryPending();
            FlashMessageSupport.set(session, FLASH_MESSAGE, successful + " queued HIE payload(s) sent successfully.");
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "integration/pendingHie");
    }
}
