package org.openmrs.module.rwandapharmacymanagement.page.controller.dispense;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WithoutOrderPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        model.addAttribute("items", pharmacyService.getAllItems());
        model.addAttribute("stockBalances", activePharmacy == null ? java.util.Collections.emptyList()
                : inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId()));
        model.addAttribute("pricingService", pricingService);
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("dispensingService") DispensingService dispensingService,
                       @RequestParam(value = "patientId", required = false) Integer patientId,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       HttpSession session,
                       UiUtils ui) {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        DispenseRecord record = new DispenseRecord();
        if (patientId != null) {
            Patient patient = Context.getPatientService().getPatient(patientId);
            record.setPatient(patient);
        }
        record.setDispensingPharmacyLocation(activePharmacy);
        record.setCreator(Context.getAuthenticatedUser());
        record.setDateCreated(new java.util.Date());
        record.setLines(buildLines(pharmacyService, itemIds, quantities, batchNumbers));
        dispensingService.dispenseWithoutOrder(record);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "dispense/withoutOrder");
    }

    private List<DispenseRecordLine> buildLines(PharmacyService pharmacyService,
                                                Integer[] itemIds,
                                                BigDecimal[] quantities,
                                                String[] batchNumbers) {
        List<DispenseRecordLine> lines = new ArrayList<DispenseRecordLine>();
        if (itemIds == null || quantities == null) {
            return lines;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            BigDecimal quantity = quantities.length > i ? quantities[i] : null;
            String batchNumber = batchNumbers != null && batchNumbers.length > i ? batchNumbers[i] : null;
            if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            DispenseRecordLine line = new DispenseRecordLine();
            line.setItem(pharmacyService.getItem(itemId));
            line.setBatchNumber(batchNumber);
            line.setQuantityDispensed(quantity);
            lines.add(line);
        }
        return lines;
    }

}
