package org.openmrs.module.rwandapharmacymanagement.config;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.scheduler.RmsDeliveryFetchTask;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SchedulerSetup implements Setup {

    public static final String RMS_DELIVERY_FETCH_TASK_NAME = "Rwanda Pharmacy RMS Delivery Fetch";

    @Override
    public void initialize() {
        SchedulerService schedulerService = Context.getSchedulerService();
        if (schedulerService == null) {
            return;
        }

        AdministrationService administrationService = Context.getAdministrationService();
        boolean enabled = Boolean.parseBoolean(administrationService.getGlobalProperty(
                RwandaPharmacyManagementConfig.GP_RMS_DELIVERY_FETCH_SCHEDULER_ENABLED, "false"));
        long repeatIntervalMs = resolveRepeatInterval(administrationService);

        TaskDefinition task = schedulerService.getTaskByName(RMS_DELIVERY_FETCH_TASK_NAME);
        if (task == null) {
            task = new TaskDefinition();
            task.setName(RMS_DELIVERY_FETCH_TASK_NAME);
        }

        task.setDescription("Fetch RMS delivery details for approved open purchase orders without auto-receiving stock.");
        task.setTaskClass(RmsDeliveryFetchTask.class.getName());
        task.setRepeatInterval(repeatIntervalMs);
        task.setStartOnStartup(enabled);
        task.setStarted(enabled);
        if (task.getStartTime() == null) {
            task.setStartTime(new Date(System.currentTimeMillis() + 60000L));
        }

        schedulerService.saveTaskDefinition(task);
        if (enabled) {
            schedulerService.scheduleIfNotRunning(task);
        }
    }

    @Override
    public void teardown() {
        // Intentionally retained across restarts.
    }

    private long resolveRepeatInterval(AdministrationService administrationService) {
        try {
            long intervalSeconds = Long.parseLong(administrationService.getGlobalProperty(
                    RwandaPharmacyManagementConfig.GP_RMS_DELIVERY_FETCH_SCHEDULER_INTERVAL_SECONDS, "3600"));
            return Math.max(intervalSeconds, 60L) * 1000L;
        }
        catch (Exception e) {
            return 3600L * 1000L;
        }
    }
}
