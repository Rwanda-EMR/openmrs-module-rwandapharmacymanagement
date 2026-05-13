package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;
import java.util.Date;

public class SupplierPrice extends BaseTrackedEntity {

    private Integer supplierPriceId;
    private Item item;
    private String supplierProductCode;
    private String program;
    private BigDecimal unitPrice;
    private String currency;
    private Date effectiveDate;

    public Integer getSupplierPriceId() {
        return supplierPriceId;
    }

    public void setSupplierPriceId(Integer supplierPriceId) {
        this.supplierPriceId = supplierPriceId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getSupplierProductCode() {
        return supplierProductCode;
    }

    public void setSupplierProductCode(String supplierProductCode) {
        this.supplierProductCode = supplierProductCode;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
