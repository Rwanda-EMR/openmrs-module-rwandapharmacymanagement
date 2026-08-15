package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;

public class ConsumableOrderLineCommand {

    private Integer productMasterItemId;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private Boolean billable = Boolean.TRUE;
    private String billingExclusionReason;

    public Integer getProductMasterItemId() {
        return productMasterItemId;
    }

    public void setProductMasterItemId(Integer productMasterItemId) {
        this.productMasterItemId = productMasterItemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Boolean getBillable() {
        return billable;
    }

    public void setBillable(Boolean billable) {
        this.billable = billable;
    }

    public String getBillingExclusionReason() {
        return billingExclusionReason;
    }

    public void setBillingExclusionReason(String billingExclusionReason) {
        this.billingExclusionReason = billingExclusionReason;
    }
}
