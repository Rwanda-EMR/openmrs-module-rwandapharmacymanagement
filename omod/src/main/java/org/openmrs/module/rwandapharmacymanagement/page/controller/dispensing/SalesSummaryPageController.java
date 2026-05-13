package org.openmrs.module.rwandapharmacymanagement.page.controller.dispense;

import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SalesSummaryPageController {

    public void get(PageModel model, @SpringBean("dispensingService") DispensingService dispensingService) {
        List<DispenseRecord> dispenses = dispensingService.getRecentDispenses();
        model.addAttribute("dispenses", dispenses);
        model.addAttribute("pharmacyTotals", calculatePharmacyTotals(dispenses));
        model.addAttribute("itemTotals", calculateItemTotals(dispenses));
    }

    private Map<String, BigDecimal> calculatePharmacyTotals(List<DispenseRecord> dispenses) {
        Map<String, BigDecimal> totals = new LinkedHashMap<String, BigDecimal>();
        if (dispenses == null) {
            return totals;
        }
        for (DispenseRecord dispense : dispenses) {
            String pharmacy = dispense.getDispensingPharmacyLocation() == null ? "Unknown pharmacy"
                    : dispense.getDispensingPharmacyLocation().getName();
            BigDecimal running = totals.containsKey(pharmacy) ? totals.get(pharmacy) : BigDecimal.ZERO;
            if (dispense.getLines() != null) {
                for (DispenseRecordLine line : dispense.getLines()) {
                    running = running.add(line.getLineTotal() == null ? BigDecimal.ZERO : line.getLineTotal());
                }
            }
            totals.put(pharmacy, running);
        }
        return totals;
    }

    private Map<String, BigDecimal> calculateItemTotals(List<DispenseRecord> dispenses) {
        Map<String, BigDecimal> totals = new LinkedHashMap<String, BigDecimal>();
        if (dispenses == null) {
            return totals;
        }
        for (DispenseRecord dispense : dispenses) {
            if (dispense.getLines() == null) {
                continue;
            }
            for (DispenseRecordLine line : dispense.getLines()) {
                String item = line.getItem() == null ? "Unknown item" : line.getItem().getName();
                BigDecimal running = totals.containsKey(item) ? totals.get(item) : BigDecimal.ZERO;
                running = running.add(line.getLineTotal() == null ? BigDecimal.ZERO : line.getLineTotal());
                totals.put(item, running);
            }
        }
        return totals;
    }
}
