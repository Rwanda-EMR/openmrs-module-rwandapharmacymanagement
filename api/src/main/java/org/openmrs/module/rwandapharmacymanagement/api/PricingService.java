package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;

import java.math.BigDecimal;

public interface PricingService extends OpenmrsService {

    BigDecimal getDispensingMarkupPercentage();

    void saveDispensingMarkupPercentage(BigDecimal markupPercentage);

    BigDecimal calculateSellingPrice(BigDecimal costPrice);
}
