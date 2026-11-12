package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.Item;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface BillingPriceService extends OpenmrsService {

    BigDecimal getHighestCurrentStockCost(Item item);

    Map<Integer, BigDecimal> getHighestCurrentStockCosts(Collection<Integer> itemIds);

    void createBillingMapping(Item item, BigDecimal displayedPrice);

    void updatePricesForReceivedItems(Collection<Item> items);

    /**
     * Renames a MOH Billing facility service (name, short name and description) in place through MOH Billing's own
     * service, so drug orders keep finding it by drug name. Insurer prices stay attached to it.
     */
    void renameFacilityService(Integer facilityServicePriceId, String newName);
}
