package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller("rwandaPharmacyAdminSettingsPageController")
public class SettingsPageController {

    public void get(PageModel model, @SpringBean("pricingService") PricingService pricingService) {
        model.addAttribute("supplierBaseUrl",
                Context.getAdministrationService().getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL));
        model.addAttribute("expiryAlertDays",
                Context.getAdministrationService().getGlobalProperty(RwandaPharmacyManagementConfig.GP_EXPIRY_ALERT_DAYS));
        model.addAttribute("dispensingMarkupPercentage", pricingService.getDispensingMarkupPercentage());
        model.addAttribute("prescriptionPaymentBillMode", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_PRESCRIPTION_PAYMENT_BILL_MODE, "latest"));
        model.addAttribute("smsEnabled", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_ENABLED, "false"));
        model.addAttribute("smsPostUrl", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_POST_URL, "http://41.186.44.171:1401/send"));
        model.addAttribute("smsUsername", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_USERNAME, ""));
        model.addAttribute("smsPassword", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_PASSWORD, ""));
        model.addAttribute("smsSender", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_SENDER, ""));
        model.addAttribute("smsCoding", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_CODING, "0"));
        model.addAttribute("smsDlrUrl", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_URL, ""));
        model.addAttribute("smsDlrLevel", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_LEVEL, "2"));
        model.addAttribute("smsPhonePersonAttributeTypeUuid", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_PHONE_PERSON_ATTRIBUTE_TYPE_UUID, ""));
        model.addAttribute("hieRetrySchedulerEnabled", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_ENABLED, "true"));
        model.addAttribute("hieRetrySchedulerIntervalSeconds", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_INTERVAL_SECONDS, "1800"));
        model.addAttribute("hieRetryBatchSize", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_RETRY_BATCH_SIZE, "50"));
        model.addAttribute("smsRetrySchedulerEnabled", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_ENABLED, "true"));
        model.addAttribute("smsRetrySchedulerIntervalSeconds", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_INTERVAL_SECONDS, "1800"));
        model.addAttribute("smsRetryBatchSize", Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_RETRY_BATCH_SIZE, "50"));
        String frequencyConceptSet = Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_PRESCRIPTION_FREQUENCY_CONCEPT_SET);
        model.addAttribute("prescriptionFrequencyConceptSet", frequencyConceptSet);
        model.addAttribute("prescriptionFrequencyConceptSetDisplay", displayConcept(frequencyConceptSet));
        String labServiceConceptSet = Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_LAB_SERVICE_CONCEPT_SET);
        model.addAttribute("labServiceConceptSet", labServiceConceptSet);
        model.addAttribute("labServiceConceptSetDisplay", displayConcept(labServiceConceptSet));
    }

    public String post(@SpringBean("pricingService") PricingService pricingService,
                       @RequestParam("dispensingMarkupPercentage") BigDecimal dispensingMarkupPercentage,
                       @RequestParam(value = "expiryAlertDays", required = false) String expiryAlertDays,
                       @RequestParam(value = "prescriptionPaymentBillMode", required = false) String prescriptionPaymentBillMode,
                       @RequestParam(value = "smsEnabled", required = false) String smsEnabled,
                       @RequestParam(value = "smsPostUrl", required = false) String smsPostUrl,
                       @RequestParam(value = "smsUsername", required = false) String smsUsername,
                       @RequestParam(value = "smsPassword", required = false) String smsPassword,
                       @RequestParam(value = "smsSender", required = false) String smsSender,
                       @RequestParam(value = "smsCoding", required = false) String smsCoding,
                       @RequestParam(value = "smsDlrUrl", required = false) String smsDlrUrl,
                       @RequestParam(value = "smsDlrLevel", required = false) String smsDlrLevel,
                       @RequestParam(value = "smsPhonePersonAttributeTypeUuid", required = false) String smsPhonePersonAttributeTypeUuid,
                       @RequestParam(value = "hieRetrySchedulerEnabled", required = false) String hieRetrySchedulerEnabled,
                       @RequestParam(value = "hieRetrySchedulerIntervalSeconds", required = false) String hieRetrySchedulerIntervalSeconds,
                       @RequestParam(value = "hieRetryBatchSize", required = false) String hieRetryBatchSize,
                       @RequestParam(value = "smsRetrySchedulerEnabled", required = false) String smsRetrySchedulerEnabled,
                       @RequestParam(value = "smsRetrySchedulerIntervalSeconds", required = false) String smsRetrySchedulerIntervalSeconds,
                       @RequestParam(value = "smsRetryBatchSize", required = false) String smsRetryBatchSize,
                       @RequestParam(value = "prescriptionFrequencyConceptSet", required = false) String prescriptionFrequencyConceptSet,
                       @RequestParam(value = "labServiceConceptSet", required = false) String labServiceConceptSet,
                       UiUtils ui) {
        pricingService.saveDispensingMarkupPercentage(dispensingMarkupPercentage);
        if (expiryAlertDays != null) {
            Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_EXPIRY_ALERT_DAYS, expiryAlertDays);
        }
        Context.getAdministrationService().setGlobalProperty(
                RwandaPharmacyManagementConfig.GP_PRESCRIPTION_PAYMENT_BILL_MODE,
                "strict".equalsIgnoreCase(prescriptionPaymentBillMode) ? "strict" : "latest");
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_ENABLED,
                "true".equalsIgnoreCase(smsEnabled) ? "true" : "false");
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_POST_URL,
                blankToDefault(smsPostUrl, "http://41.186.44.171:1401/send"));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_USERNAME,
                trimToEmpty(smsUsername));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_PASSWORD,
                trimToEmpty(smsPassword));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_SENDER,
                trimToEmpty(smsSender));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_CODING,
                blankToDefault(smsCoding, "0"));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_URL,
                trimToEmpty(smsDlrUrl));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_LEVEL,
                blankToDefault(smsDlrLevel, "2"));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_PHONE_PERSON_ATTRIBUTE_TYPE_UUID,
                trimToEmpty(smsPhonePersonAttributeTypeUuid));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_ENABLED,
                "true".equalsIgnoreCase(hieRetrySchedulerEnabled) ? "true" : "false");
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_INTERVAL_SECONDS,
                blankToDefault(hieRetrySchedulerIntervalSeconds, "1800"));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_RETRY_BATCH_SIZE,
                blankToDefault(hieRetryBatchSize, "50"));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_ENABLED,
                "true".equalsIgnoreCase(smsRetrySchedulerEnabled) ? "true" : "false");
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_INTERVAL_SECONDS,
                blankToDefault(smsRetrySchedulerIntervalSeconds, "1800"));
        Context.getAdministrationService().setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_RETRY_BATCH_SIZE,
                blankToDefault(smsRetryBatchSize, "50"));
        Context.getAdministrationService().setGlobalProperty(
                RwandaPharmacyManagementConfig.GP_PRESCRIPTION_FREQUENCY_CONCEPT_SET,
                prescriptionFrequencyConceptSet == null ? "" : prescriptionFrequencyConceptSet.trim());
        Context.getAdministrationService().setGlobalProperty(
                RwandaPharmacyManagementConfig.GP_LAB_SERVICE_CONCEPT_SET,
                labServiceConceptSet == null ? "" : labServiceConceptSet.trim());
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/settings");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String displayConcept(String conceptIdentifier) {
        Concept concept = resolveConcept(conceptIdentifier);
        return concept == null ? "" : concept.getDisplayString();
    }

    private Concept resolveConcept(String conceptIdentifier) {
        if (conceptIdentifier == null || conceptIdentifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = conceptIdentifier.trim();
        Concept concept = Context.getConceptService().getConceptByUuid(trimmed);
        if (concept != null) {
            return concept;
        }
        try {
            return Context.getConceptService().getConcept(Integer.valueOf(trimmed));
        }
        catch (NumberFormatException ignored) {
            return Context.getConceptService().getConceptByName(trimmed);
        }
    }
}
