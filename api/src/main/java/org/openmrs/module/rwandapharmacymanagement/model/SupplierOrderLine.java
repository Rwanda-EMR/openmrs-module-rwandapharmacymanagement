package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;

public class SupplierOrderLine extends BaseTrackedEntity {

    private Integer supplierOrderLineId;
    private SupplierOrder supplierOrder;
    private Item item;
    private String supplierProductCode;
    private String supplierProductName;
    private String programName;
    private BigDecimal orderedQuantity;
    private BigDecimal unitPrice;

    public Integer getSupplierOrderLineId() {
        return supplierOrderLineId;
    }

    public void setSupplierOrderLineId(Integer supplierOrderLineId) {
        this.supplierOrderLineId = supplierOrderLineId;
    }

    public SupplierOrder getSupplierOrder() {
        return supplierOrder;
    }

    public void setSupplierOrder(SupplierOrder supplierOrder) {
        this.supplierOrder = supplierOrder;
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

    public String getSupplierProductName() {
        return supplierProductName;
    }

    public void setSupplierProductName(String supplierProductName) {
        this.supplierProductName = supplierProductName;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(BigDecimal orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
