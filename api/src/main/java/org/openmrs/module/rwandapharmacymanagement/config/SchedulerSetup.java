package org.openmrs.module.rwandapharmacymanagement.config;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.scheduler.HieRetryTask;
import org.openmrs.module.rwandapharmacymanagement.scheduler.RmsDeliveryFetchTask;
import org.openmrs.module.rwandapharmacymanagement.scheduler.SmsRetryTask;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SchedulerSetup implements Setup {

    public static final String RMS_DELIVERY_FETCH_TASK_NAME = "Rwanda Pharmacy RMS Delivery Fetch";
    public static final String HIE_RETRY_TASK_NAME = "Rwanda Pharmacy HIE Retry";
    public static final String SMS_RETRY_TASK_NAME = "Rwanda Pharmacy SMS Retry";

    @Override
    public void initialize() {
        SchedulerService schedulerService = Context.getSchedulerService();
        if (schedulerService == null) {
            return;
        }

        AdministrationService administrationService = Context.getAdministrationService();
        configureRmsDeliveryFetchTask(schedulerService, administrationService);
        configureHieRetryTask(schedulerService, administrationService);
        configureSmsRetryTask(schedulerService, administrationService);
    }

    private void configureRmsDeliveryFetchTask(SchedulerService schedulerService, AdministrationService administrationService) {
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

    private void configureHieRetryTask(SchedulerService schedulerService, AdministrationService administrationService) {
        boolean enabled = Boolean.parseBoolean(administrationService.getGlobalProperty(
                RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_ENABLED, "true"));
        long repeatIntervalMs = resolveHieRetryInterval(administrationService);

        TaskDefinition task = schedulerService.getTaskByName(HIE_RETRY_TASK_NAME);
        if (task == null) {
            task = new TaskDefinition();
            task.setName(HIE_RETRY_TASK_NAME);
        }

        task.setDescription("Retry queued HIE Encounter, MedicationRequest, and MedicationDispense payloads.");
        task.setTaskClass(HieRetryTask.class.getName());
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

    private void configureSmsRetryTask(SchedulerService schedulerService, AdministrationService administrationService) {
        boolean enabled = Boolean.parseBoolean(administrationService.getGlobalProperty(
                RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_ENABLED, "true"));
        long repeatIntervalMs = resolveSmsRetryInterval(administrationService);

        TaskDefinition task = schedulerService.getTaskByName(SMS_RETRY_TASK_NAME);
        if (task == null) {
            task = new TaskDefinition();
            task.setName(SMS_RETRY_TASK_NAME);
        }

        task.setDescription("Retry queued prescription SMS notifications.");
        task.setTaskClass(SmsRetryTask.class.getName());
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

    private long resolveHieRetryInterval(AdministrationService administrationService) {
        try {
            long intervalSeconds = Long.parseLong(administrationService.getGlobalProperty(
                    RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_INTERVAL_SECONDS, "1800"));
            return Math.max(intervalSeconds, 60L) * 1000L;
        }
        catch (Exception e) {
            return 1800L * 1000L;
        }
    }

    private long resolveSmsRetryInterval(AdministrationService administrationService) {
        try {
            long intervalSeconds = Long.parseLong(administrationService.getGlobalProperty(
                    RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_INTERVAL_SECONDS, "1800"));
            return Math.max(intervalSeconds, 60L) * 1000L;
        }
        catch (Exception e) {
            return 1800L * 1000L;
        }
    }
}
