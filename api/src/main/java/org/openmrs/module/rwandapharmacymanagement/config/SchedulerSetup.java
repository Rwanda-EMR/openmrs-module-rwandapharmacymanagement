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

import java.util.Calendar;
import java.util.Date;

@Component
public class SchedulerSetup implements Setup {

    public static final String RMS_DELIVERY_FETCH_TASK_NAME = "Rwanda Pharmacy RMS Delivery Fetch";
    public static final String HIE_RETRY_TASK_NAME = "Rwanda Pharmacy HIE Retry";
    public static final String SMS_RETRY_TASK_NAME = "Rwanda Pharmacy SMS Retry";

    /**
     * The HIE and SMS queues are always processed every 5 minutes; this is not configurable.
     * OpenMRS TaskDefinition repeat intervals are in seconds.
     */
    public static final long RETRY_INTERVAL_SECONDS = 5L * 60L;

    /**
     * RMS delivery details are fetched once a day at 03:00 server time; this is not configurable.
     */
    public static final long RMS_DELIVERY_FETCH_INTERVAL_SECONDS = 24L * 60L * 60L;
    public static final int RMS_DELIVERY_FETCH_HOUR = 3;

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

    /**
     * Always started, running daily at 03:00: OpenMRS runs a task at its start time plus whole repeat intervals,
     * so a 03:00 start time with a one-day interval runs every day at 03:00.
     */
    private void configureRmsDeliveryFetchTask(SchedulerService schedulerService, AdministrationService administrationService) {
        TaskDefinition task = schedulerService.getTaskByName(RMS_DELIVERY_FETCH_TASK_NAME);
        if (task == null) {
            task = new TaskDefinition();
            task.setName(RMS_DELIVERY_FETCH_TASK_NAME);
        }

        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, RMS_DELIVERY_FETCH_HOUR);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        task.setDescription("Fetch RMS delivery details for approved open purchase orders without auto-receiving stock.");
        task.setTaskClass(RmsDeliveryFetchTask.class.getName());
        task.setRepeatInterval(RMS_DELIVERY_FETCH_INTERVAL_SECONDS);
        task.setStartTime(startTime.getTime());
        task.setStartOnStartup(true);
        task.setStarted(true);

        schedulerService.saveTaskDefinition(task);
        rescheduleWithCurrentInterval(schedulerService, task);
    }

    private void configureHieRetryTask(SchedulerService schedulerService, AdministrationService administrationService) {
        boolean enabled = Boolean.parseBoolean(administrationService.getGlobalProperty(
                RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_ENABLED, "true"));
        TaskDefinition task = schedulerService.getTaskByName(HIE_RETRY_TASK_NAME);
        if (task == null) {
            task = new TaskDefinition();
            task.setName(HIE_RETRY_TASK_NAME);
        }

        task.setDescription("Retry queued HIE Encounter, MedicationRequest, and MedicationDispense payloads.");
        task.setTaskClass(HieRetryTask.class.getName());
        task.setRepeatInterval(RETRY_INTERVAL_SECONDS);
        task.setStartOnStartup(enabled);
        task.setStarted(enabled);
        if (task.getStartTime() == null) {
            task.setStartTime(new Date(System.currentTimeMillis() + 60000L));
        }

        schedulerService.saveTaskDefinition(task);
        if (enabled) {
            rescheduleWithCurrentInterval(schedulerService, task);
        }
    }

    private void configureSmsRetryTask(SchedulerService schedulerService, AdministrationService administrationService) {
        boolean enabled = Boolean.parseBoolean(administrationService.getGlobalProperty(
                RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_ENABLED, "true"));
        TaskDefinition task = schedulerService.getTaskByName(SMS_RETRY_TASK_NAME);
        if (task == null) {
            task = new TaskDefinition();
            task.setName(SMS_RETRY_TASK_NAME);
        }

        task.setDescription("Retry queued prescription SMS notifications.");
        task.setTaskClass(SmsRetryTask.class.getName());
        task.setRepeatInterval(RETRY_INTERVAL_SECONDS);
        task.setStartOnStartup(enabled);
        task.setStarted(enabled);
        if (task.getStartTime() == null) {
            task.setStartTime(new Date(System.currentTimeMillis() + 60000L));
        }

        schedulerService.saveTaskDefinition(task);
        if (enabled) {
            rescheduleWithCurrentInterval(schedulerService, task);
        }
    }

    /**
     * A task that is already running keeps the interval it was scheduled with, so it is rescheduled to pick up the
     * fixed interval. Falls back to scheduling it when it is not running.
     */
    private void rescheduleWithCurrentInterval(SchedulerService schedulerService, TaskDefinition task) {
        try {
            schedulerService.rescheduleTask(task);
        }
        catch (Exception e) {
            schedulerService.scheduleIfNotRunning(task);
        }
    }

    @Override
    public void teardown() {
        // Intentionally retained across restarts.
    }

}
