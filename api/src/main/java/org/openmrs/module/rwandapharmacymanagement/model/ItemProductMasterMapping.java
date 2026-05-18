package org.openmrs.module.rwandapharmacymanagement.model;

public class ItemProductMasterMapping extends BaseTrackedEntity {

    private Integer itemProductMasterMappingId;
    private Item item;
    private ProductMasterItem productMasterItem;
    private Boolean active = Boolean.TRUE;

    public Integer getItemProductMasterMappingId() {
        return itemProductMasterMappingId;
    }

    public void setItemProductMasterMappingId(Integer itemProductMasterMappingId) {
        this.itemProductMasterMappingId = itemProductMasterMappingId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ProductMasterItem getProductMasterItem() {
        return productMasterItem;
    }

    public void setProductMasterItem(ProductMasterItem productMasterItem) {
        this.productMasterItem = productMasterItem;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
