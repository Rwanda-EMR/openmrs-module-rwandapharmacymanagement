package org.openmrs.module.rwandapharmacymanagement.web.controller;

import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/module/rwandapharmacymanagement/prescription")
public class PrescriptionApiController {

    private final PharmacyService pharmacyService;
    private final InventoryService inventoryService;
    private final PricingService pricingService;

    public PrescriptionApiController(PharmacyService pharmacyService,
                                     InventoryService inventoryService,
                                     PricingService pricingService) {
        this.pharmacyService = pharmacyService;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
    }

    @RequestMapping(value = "/availability", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> availability(@RequestParam("productMasterItemId") Integer productMasterItemId) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("productMasterItemId", productMasterItemId);
        response.put("pharmaciesWithStock", 0);
        response.put("totalQuantity", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        response.put("sellingPrice", null);
        response.put("unitPrice", null);
        try {
            ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(productMasterItemId);
            if (productMasterItem == null) {
                response.put("message", "Selected drug not found");
                return response;
            }
            BigDecimal lastKnownUnitPrice = null;
            if (productMasterItem.getNpcCode() != null && !productMasterItem.getNpcCode().trim().isEmpty()) {
                org.openmrs.module.rwandapharmacymanagement.model.SupplierPrice supplierPrice =
                        pharmacyService.getLatestSupplierPrice(productMasterItem.getNpcCode(), productMasterItem.getProgram());
                if (supplierPrice == null) {
                    supplierPrice = pharmacyService.getLatestSupplierPrice(productMasterItem.getNpcCode());
                }
                if (supplierPrice != null) {
                    lastKnownUnitPrice = supplierPrice.getUnitPrice();
                }
            }
            int pharmaciesWithStock = 0;
            BigDecimal totalQuantity = BigDecimal.ZERO;
            List<PharmacyLocation> pharmacies = pharmacyService.getAllPharmacyLocations();
            for (PharmacyLocation pharmacyLocation : pharmacies) {
                if (pharmacyLocation == null || !Boolean.TRUE.equals(pharmacyLocation.getActive())) {
                    continue;
                }
                BigDecimal pharmacyTotal = BigDecimal.ZERO;
                for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(pharmacyLocation.getPharmacyLocationId())) {
                    if (balance == null || balance.getItem() == null
                            || balance.getQuantityOnHand() == null
                            || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0
                            || !matchesProductMasterItem(balance.getItem(), productMasterItem)) {
                        continue;
                    }
                    pharmacyTotal = pharmacyTotal.add(balance.getQuantityOnHand());
                    if (lastKnownUnitPrice == null && balance.getUnitPrice() != null) {
                        lastKnownUnitPrice = balance.getUnitPrice();
                    }
                }
                if (pharmacyTotal.compareTo(BigDecimal.ZERO) > 0) {
                    pharmaciesWithStock++;
                    totalQuantity = totalQuantity.add(pharmacyTotal);
                }
            }
            response.put("pharmaciesWithStock", pharmaciesWithStock);
            response.put("totalQuantity", totalQuantity.setScale(2, RoundingMode.HALF_UP));
            response.put("sellingPrice", pricingService.calculateSellingPrice(lastKnownUnitPrice));
            response.put("unitPrice", lastKnownUnitPrice);
            response.put("message", "ok");
        }
        catch (Exception ex) {
            response.put("message", ex.getMessage() == null ? "Unable to load stock summary" : ex.getMessage());
        }
        return response;
    }

    private boolean matchesProductMasterItem(Item item, ProductMasterItem productMasterItem) {
        if (item == null || productMasterItem == null) {
            return false;
        }
        String itemCode = normalizeKey(item.getCode());
        String productCode = normalizeKey(productMasterItem.getNpcCode());
        if (!itemCode.isEmpty() && itemCode.equals(productCode)) {
            return true;
        }
        return item.getDrug() != null && productMasterItem.getDrug() != null
                && item.getDrug().getDrugId() != null
                && item.getDrug().getDrugId().equals(productMasterItem.getDrug().getDrugId());
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
