package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Transactional
public class PricingServiceImpl extends BaseOpenmrsService implements PricingService {

    private AdministrationService administrationService;

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @Override
    public BigDecimal getDispensingMarkupPercentage() {
        try {
            return new BigDecimal(administrationService.getGlobalProperty(
                    RwandaPharmacyManagementConfig.GP_DISPENSING_MARKUP_PERCENTAGE, "0"));
        }
        catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void saveDispensingMarkupPercentage(BigDecimal markupPercentage) {
        administrationService.setGlobalProperty(RwandaPharmacyManagementConfig.GP_DISPENSING_MARKUP_PERCENTAGE,
                markupPercentage == null ? "0" : markupPercentage.stripTrailingZeros().toPlainString());
    }

    @Override
    public BigDecimal calculateSellingPrice(BigDecimal costPrice) {
        if (costPrice == null) {
            return null;
        }
        BigDecimal markupMultiplier = BigDecimal.ONE.add(getDispensingMarkupPercentage()
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return costPrice.multiply(markupMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
