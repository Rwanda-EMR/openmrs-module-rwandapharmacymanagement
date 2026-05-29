package org.openmrs.module.rwandapharmacymanagement.scheduler;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.integration.sms.SmsRetryService;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.List;

public class SmsRetryTask extends AbstractTask {

    @Override
    public void execute() {
        if (isExecuting()) {
            return;
        }

        try {
            startExecuting();
            List<SmsRetryService> services = Context.getRegisteredComponents(SmsRetryService.class);
            if (services != null && !services.isEmpty()) {
                services.get(0).retryPending();
            }
        }
        finally {
            stopExecuting();
        }
    }
}
