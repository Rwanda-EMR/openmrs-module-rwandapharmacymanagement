package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class PharmacyStockPageController {

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "materialType", required = false) String materialType,
                    @RequestParam(value = "program", required = false) String program) {
        List<PharmacyLocation> pharmacies = StockReportSupport.getActivePharmacies(pharmacyService);
        PharmacyLocation selectedPharmacy = pharmacyLocationId == null ? (pharmacies.isEmpty() ? null : pharmacies.get(0))
                : pharmacyService.getPharmacyLocation(pharmacyLocationId);
        List<Map<String, Object>> rows = StockReportSupport.buildPharmacyRows(selectedPharmacy, inventoryService, pricingService,
                pharmacyService, materialType, program);
        model.addAttribute("pharmacies", pharmacies);
        model.addAttribute("selectedPharmacy", selectedPharmacy);
        model.addAttribute("materialTypes", StockReportSupport.getMaterialTypes(pharmacyService));
        model.addAttribute("programs", StockReportSupport.getPrograms(pharmacyService));
        model.addAttribute("selectedMaterialType", materialType);
        model.addAttribute("selectedProgram", program);
        model.addAttribute("rows", rows);
        model.addAttribute("totalQuantity", StockReportSupport.sumPharmacyQuantity(rows));
        model.addAttribute("totalCostValue", StockReportSupport.sumMoney(rows, "totalValue"));
        model.addAttribute("totalSellingValue", StockReportSupport.sumMoney(rows, "totalSellingValue"));
    }
}
