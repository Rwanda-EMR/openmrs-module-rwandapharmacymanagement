package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.model.Supplier;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierSetting;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@Controller("rwandaPharmacySupplierSettingsPageController")
public class SettingsPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "supplierId", required = false) Integer supplierId) {
        SupplierSetting supplierSetting = pharmacyService.getSupplierSetting();
        Supplier selectedSupplier = supplierId == null ? null : pharmacyService.getSupplier(supplierId);
        model.addAttribute("supplierSetting", supplierSetting);
        model.addAttribute("suppliers", pharmacyService.getAllSuppliers());
        model.addAttribute("selectedSupplier", selectedSupplier);
        // Saved passwords are never sent to the browser; the page only shows whether one is saved.
        model.addAttribute("supplierSettingPasswordSaved", supplierSetting != null && hasText(supplierSetting.getPassword()));
        model.addAttribute("selectedSupplierPasswordSaved", selectedSupplier != null && hasText(selectedSupplier.getApiPassword()));
        AdministrationService administrationService = Context.getAdministrationService();
        model.addAttribute("facilityId", facilityId(administrationService));
        String hospitalName = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL, "");
        if (hospitalName == null || hospitalName.trim().isEmpty()) {
            hospitalName = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, "");
        }
        model.addAttribute("hospitalName", hospitalName);
        model.addAttribute("smtpHost", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_HOST, ""));
        model.addAttribute("smtpPort", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_PORT, "587"));
        model.addAttribute("smtpUsername", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_USERNAME, ""));
        model.addAttribute("smtpPasswordSaved", hasText(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_PASSWORD, "")));
        model.addAttribute("smtpFrom", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_FROM, ""));
        model.addAttribute("smtpAuth", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_AUTH, "true"));
        model.addAttribute("smtpStarttls", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_STARTTLS, "true"));
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @RequestParam(value = "baseUrl", required = false) String baseUrl,
                       @RequestParam(value = "username", required = false) String username,
                       @RequestParam(value = "password", required = false) String password,
                       @RequestParam(value = "facilityId", required = false) String facilityId,
                       @RequestParam(value = "hospitalName", required = false) String hospitalName,
                       @RequestParam(value = "connectionTimeout", required = false) Integer connectionTimeout,
                       @RequestParam(value = "readTimeout", required = false) Integer readTimeout,
                       @RequestParam(value = "supplierId", required = false) Integer supplierId,
                       @RequestParam(value = "supplierName", required = false) String supplierName,
                       @RequestParam(value = "supplierCode", required = false) String supplierCode,
                       @RequestParam(value = "supplierEmailAddress", required = false) String supplierEmailAddress,
                       @RequestParam(value = "supplierApiBaseUrl", required = false) String supplierApiBaseUrl,
                       @RequestParam(value = "supplierApiUsername", required = false) String supplierApiUsername,
                       @RequestParam(value = "supplierApiPassword", required = false) String supplierApiPassword,
                       @RequestParam(value = "supplierConnectionTimeout", required = false) Integer supplierConnectionTimeout,
                       @RequestParam(value = "supplierReadTimeout", required = false) Integer supplierReadTimeout,
                       @RequestParam(value = "supplierApiEnabled", required = false) Boolean supplierApiEnabled,
                       @RequestParam(value = "supplierActive", required = false) Boolean supplierActive,
                       @RequestParam(value = "smtpHost", required = false) String smtpHost,
                       @RequestParam(value = "smtpPort", required = false) String smtpPort,
                       @RequestParam(value = "smtpUsername", required = false) String smtpUsername,
                       @RequestParam(value = "smtpPassword", required = false) String smtpPassword,
                       @RequestParam(value = "smtpFrom", required = false) String smtpFrom,
                       @RequestParam(value = "smtpAuth", required = false) Boolean smtpAuth,
                       @RequestParam(value = "smtpStarttls", required = false) Boolean smtpStarttls,
                       @RequestParam(value = "integrationEnabled", required = false) Boolean integrationEnabled,
                       @RequestParam(value = "action", required = false) String action,
                       UiUtils ui) {
        if ("saveSupplier".equals(action)) {
            Supplier supplier = supplierId == null ? new Supplier() : pharmacyService.getSupplier(supplierId);
            if (supplier == null) {
                supplier = new Supplier();
            }
            supplier.setName(supplierName);
            supplier.setCode(supplierCode);
            supplier.setEmailAddress(supplierEmailAddress);
            supplier.setApiEnabled(Boolean.TRUE.equals(supplierApiEnabled));
            supplier.setApiBaseUrl(supplierApiBaseUrl);
            supplier.setApiUsername(supplierApiUsername);
            if (hasText(supplierApiPassword)) {
                supplier.setApiPassword(supplierApiPassword);
            }
            supplier.setConnectionTimeout(supplierConnectionTimeout == null ? 10000 : supplierConnectionTimeout);
            supplier.setReadTimeout(supplierReadTimeout == null ? 30000 : supplierReadTimeout);
            supplier.setActive(Boolean.TRUE.equals(supplierActive));
            supplier.setCreator(Context.getAuthenticatedUser());
            supplier.setDateCreated(supplier.getDateCreated() == null ? new Date() : supplier.getDateCreated());
            pharmacyService.saveSupplier(supplier);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/settings");
        }
        if ("saveSmtp".equals(action)) {
            AdministrationService administrationService = Context.getAdministrationService();
            administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_HOST, safe(smtpHost));
            administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_PORT, safe(smtpPort));
            administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_USERNAME, safe(smtpUsername));
            if (hasText(smtpPassword)) {
                administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_PASSWORD, smtpPassword);
            }
            administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_FROM, safe(smtpFrom));
            administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_AUTH, Boolean.TRUE.equals(smtpAuth) ? "true" : "false");
            administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_STARTTLS, Boolean.TRUE.equals(smtpStarttls) ? "true" : "false");
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/settings");
        }
        SupplierSetting supplierSetting = pharmacyService.getSupplierSetting();
        supplierSetting.setBaseUrl(baseUrl);
        supplierSetting.setUsername(username);
        if (hasText(password)) {
            supplierSetting.setPassword(password);
        }
        supplierSetting.setConnectionTimeout(connectionTimeout);
        supplierSetting.setReadTimeout(readTimeout);
        supplierSetting.setIntegrationEnabled(Boolean.TRUE.equals(integrationEnabled));
        supplierSetting.setCreator(Context.getAuthenticatedUser());
        supplierSetting.setDateCreated(new java.util.Date());
        pharmacyService.saveSupplierSetting(supplierSetting);
        AdministrationService administrationService = Context.getAdministrationService();
        administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID, safe(facilityId));
        administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID_LEGACY, safe(facilityId));
        administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL, safe(hospitalName));
        administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, safe(hospitalName));
        if ("authenticate".equals(action)) {
            supplierIntegrationService.authenticate();
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/settings");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String facilityId(AdministrationService administrationService) {
        String facilityId = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID, "");
        if (facilityId == null || facilityId.trim().isEmpty()) {
            facilityId = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID_LEGACY, "1660");
        }
        return facilityId;
    }
}
