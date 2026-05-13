package org.openmrs.module.rwandapharmacymanagement.page.controller.supplier;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrder;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrderLine;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierPrice;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class OrderFormPageController {

    public void get(PageModel model, @SpringBean("pharmacyService") PharmacyService pharmacyService) {
        model.addAttribute("items", pharmacyService.getAllItems());
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("supplierIntegrationService") SupplierIntegrationService supplierIntegrationService,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       UiUtils ui) {
        SupplierOrder supplierOrder = new SupplierOrder();
        supplierOrder.setOrderNumber("PO-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        supplierOrder.setFacilityLocation(Context.getLocationService().getDefaultLocation());
        supplierOrder.setRequestedBy(Context.getAuthenticatedUser());
        supplierOrder.setCreator(Context.getAuthenticatedUser());
        supplierOrder.setDateCreated(new Date());
        supplierOrder.setStatus("PENDING_SUBMISSION");
        supplierOrder.setLines(buildLines(pharmacyService, supplierOrder, itemIds, quantities));
        supplierIntegrationService.submitSupplierOrder(supplierOrder);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "supplier/orderForm");
    }

    private List<SupplierOrderLine> buildLines(PharmacyService pharmacyService,
                                               SupplierOrder supplierOrder,
                                               Integer[] itemIds,
                                               BigDecimal[] quantities) {
        List<SupplierOrderLine> lines = new ArrayList<SupplierOrderLine>();
        if (itemIds == null || quantities == null) {
            return lines;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            BigDecimal quantity = quantities.length > i ? quantities[i] : null;
            if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Item item = pharmacyService.getItem(itemId);
            SupplierOrderLine line = new SupplierOrderLine();
            line.setSupplierOrder(supplierOrder);
            line.setItem(item);
            line.setOrderedQuantity(quantity);
            SupplierPrice latestPrice = item == null ? null : pharmacyService.getLatestSupplierPrice(item.getCode());
            line.setUnitPrice(latestPrice == null ? null : latestPrice.getUnitPrice());
            line.setCreator(Context.getAuthenticatedUser());
            line.setDateCreated(new Date());
            lines.add(line);
        }
        return lines;
    }
}
