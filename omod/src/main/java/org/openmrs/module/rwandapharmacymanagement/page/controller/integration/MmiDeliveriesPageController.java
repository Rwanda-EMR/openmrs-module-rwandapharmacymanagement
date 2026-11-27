package org.openmrs.module.rwandapharmacymanagement.page.controller.integration;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipInsurancePayloadBuilder;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipMedicineDeliveryService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Delivery receipts and whether each was recorded with MMI (RHIP /practitioner/medicines/delivery), with a button to
 * send one that failed or was received before the integration was switched on
 */
@Controller
public class MmiDeliveriesPageController {

    private static final String FLASH_MESSAGE = "rpm.mmiDeliveries.message";

    private static final int DEFAULT_DAYS = 30;

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("rhipMedicineDeliveryService") RhipMedicineDeliveryService deliveryService,
                    @RequestParam(value = "days", required = false) Integer days,
                    HttpSession session) {
        int lookBackDays = days == null || days.intValue() <= 0 ? DEFAULT_DAYS : Math.min(days.intValue(), 365);
        Calendar from = Calendar.getInstance();
        from.add(Calendar.DATE, -lookBackDays);
        from.set(Calendar.HOUR_OF_DAY, 0);
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.SECOND, 0);
        from.set(Calendar.MILLISECOND, 0);
        Calendar to = Calendar.getInstance();
        to.add(Calendar.DATE, 1);

        List<DeliveryReceipt> receipts = inventoryService.getDeliveryReceipts(from.getTime(), to.getTime(), null, null);
        Map<String, IntegrationLog> logs = deliveryService.getLatestLogsByReference(from.getTime());
        List<Row> rows = new ArrayList<Row>();
        if (receipts != null) {
            for (DeliveryReceipt receipt : receipts) {
                String reference = RhipInsurancePayloadBuilder.deliveryReferenceNumber(receipt);
                rows.add(new Row(receipt, reference, reference == null ? null : logs.get(reference)));
            }
        }
        model.addAttribute("rows", rows);
        model.addAttribute("days", lookBackDays);
        model.addAttribute("enabled", deliveryService.isEnabled());
        model.addAttribute("message", FlashMessageSupport.consumeString(session, FLASH_MESSAGE));
    }

    public String post(@SpringBean("inventoryService") InventoryService inventoryService,
                       @SpringBean("rhipMedicineDeliveryService") RhipMedicineDeliveryService deliveryService,
                       @RequestParam(value = "deliveryReceiptId", required = false) Integer deliveryReceiptId,
                       @RequestParam(value = "days", required = false) Integer days,
                       HttpSession session,
                       UiUtils ui) {
        DeliveryReceipt receipt = deliveryReceiptId == null ? null : inventoryService.getDeliveryReceipt(deliveryReceiptId);
        if (receipt == null) {
            FlashMessageSupport.set(session, FLASH_MESSAGE, "Delivery receipt not found.");
        }
        else {
            IntegrationLog result = deliveryService.send(receipt);
            String reference = RhipInsurancePayloadBuilder.deliveryReferenceNumber(receipt);
            FlashMessageSupport.set(session, FLASH_MESSAGE, reference + ": "
                    + (result == null || result.getSummary() == null ? "sent" : result.getSummary()));
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "integration/mmiDeliveries")
                + (days == null ? "" : "?days=" + days);
    }

    public static class Row {

        private final DeliveryReceipt receipt;

        private final String reference;

        private final IntegrationLog log;

        public Row(DeliveryReceipt receipt, String reference, IntegrationLog log) {
            this.receipt = receipt;
            this.reference = reference;
            this.log = log;
        }

        public DeliveryReceipt getReceipt() {
            return receipt;
        }

        public String getReference() {
            return reference;
        }

        public IntegrationLog getLog() {
            return log;
        }

        public Date getReceivedOn() {
            return receipt == null ? null : receipt.getDateCreated();
        }

        public int getLineCount() {
            return receipt == null || receipt.getLines() == null ? 0 : receipt.getLines().size();
        }

        /**
         * @return Recorded, Failed, Not sent (skipped) or Never sent
         */
        public String getStatusLabel() {
            if (log == null) {
                return "Never sent";
            }
            if (log.getStatus() == IntegrationStatus.SUCCESS) {
                return "Recorded";
            }
            if (log.getStatus() == IntegrationStatus.SKIPPED) {
                return "Not sent";
            }
            return "Failed";
        }

        public boolean isRecorded() {
            return log != null && log.getStatus() == IntegrationStatus.SUCCESS;
        }
    }
}
