package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.springframework.stereotype.Controller;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class OverallStockPageController {

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "materialType", required = false) String materialType,
                    @RequestParam(value = "program", required = false) String program) {
        List<Map<String, Object>> rows = StockReportSupport.buildOverallRows(inventoryService, pricingService,
                pharmacyService, materialType, program);
        model.addAttribute("rows", rows);
        model.addAttribute("materialTypes", StockReportSupport.getMaterialTypes(pharmacyService));
        model.addAttribute("programs", StockReportSupport.getPrograms(pharmacyService));
        model.addAttribute("selectedMaterialType", materialType);
        model.addAttribute("selectedProgram", program);
        model.addAttribute("totalQuantity", StockReportSupport.sumMoney(rows, "quantity"));
        model.addAttribute("totalCostValue", StockReportSupport.sumMoney(rows, "totalValue"));
        model.addAttribute("totalSellingValue", StockReportSupport.sumMoney(rows, "totalSellingValue"));
    }
}
