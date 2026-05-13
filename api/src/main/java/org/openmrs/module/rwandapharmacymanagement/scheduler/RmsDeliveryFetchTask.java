package org.openmrs.module.rwandapharmacymanagement.scheduler;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.scheduler.tasks.AbstractTask;

public class RmsDeliveryFetchTask extends AbstractTask {

    @Override
    public void execute() {
        if (isExecuting()) {
            return;
        }

        try {
            startExecuting();
            Context.getService(SupplierIntegrationService.class).fetchPendingRmsDeliveryDetails();
        }
        finally {
            stopExecuting();
        }
    }
}
