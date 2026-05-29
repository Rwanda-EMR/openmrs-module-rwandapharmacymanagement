package org.openmrs.module.rwandapharmacymanagement.scheduler;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.integration.hie.HieRetryService;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.List;

public class HieRetryTask extends AbstractTask {

    @Override
    public void execute() {
        if (isExecuting()) {
            return;
        }

        try {
            startExecuting();
            List<HieRetryService> services = Context.getRegisteredComponents(HieRetryService.class);
            if (services != null && !services.isEmpty()) {
                services.get(0).retryPending();
            }
        }
        finally {
            stopExecuting();
        }
    }
}
