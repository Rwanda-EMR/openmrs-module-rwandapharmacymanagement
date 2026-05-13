package org.openmrs.module.rwandapharmacymanagement.model;

public class SupplierProductMapping extends BaseTrackedEntity {

    private Integer supplierProductMappingId;
    private Item item;
    private String supplierProductCode;
    private String supplierProductName;
    private Boolean active = Boolean.TRUE;

    public Integer getSupplierProductMappingId() {
        return supplierProductMappingId;
    }

    public void setSupplierProductMappingId(Integer supplierProductMappingId) {
        this.supplierProductMappingId = supplierProductMappingId;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
