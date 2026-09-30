package org.openmrs.module.rwandapharmacymanagement;

import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.rwandapharmacymanagement.config.Setup;
import org.openmrs.module.rwandapharmacymanagement.htmlformentry.RpmConsumableOrderTagHandler;
import org.openmrs.module.rwandapharmacymanagement.htmlformentry.RpmPrescriptionOrderTagHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RwandaPharmacyManagementActivator extends BaseModuleActivator {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void started() {
        log.info("Starting {}", RwandaPharmacyManagementConfig.MODULE_NAME);
        List<Setup> setups = getSetups();
        for (Setup setup : setups) {
            try {
                setup.initialize();
            }
            catch (Exception e) {
                log.error("Failed to initialize setup {}", setup.getClass().getName(), e);
                throw new RuntimeException("Failed to initialize setup " + setup.getClass().getName(), e);
            }
        }
        registerHtmlFormEntryTags();
    }

    @Override
    public void stopped() {
        log.info("Stopping {}", RwandaPharmacyManagementConfig.MODULE_NAME);
        List<Setup> setups = getSetups();
        for (Setup setup : setups) {
            try {
                setup.teardown();
            }
            catch (Exception e) {
                log.warn("Failed to teardown setup {}", setup.getClass().getName(), e);
            }
        }
    }

    private List<Setup> getSetups() {
        try {
            if (!Context.isSessionOpen()) {
                return java.util.Collections.emptyList();
            }
            List<Setup> setups = Context.getRegisteredComponents(Setup.class);
            return setups == null ? java.util.Collections.<Setup>emptyList() : setups;
        }
        catch (Exception e) {
            log.warn("Unable to access registered setup components during module lifecycle", e);
            return java.util.Collections.emptyList();
        }
    }

    private void registerHtmlFormEntryTags() {
        try {
            HtmlFormEntryService htmlFormEntryService = Context.getService(HtmlFormEntryService.class);
            htmlFormEntryService.addHandler("rpmPrescriptionOrder", new RpmPrescriptionOrderTagHandler());
            htmlFormEntryService.addHandler("rpmConsumableOrder", new RpmConsumableOrderTagHandler());
            log.info("Registered Rwanda Pharmacy Management HTML Form Entry tags");
        }
        catch (Exception e) {
            log.warn("Unable to register Rwanda Pharmacy Management HTML Form Entry tags", e);
        }
    }
}
