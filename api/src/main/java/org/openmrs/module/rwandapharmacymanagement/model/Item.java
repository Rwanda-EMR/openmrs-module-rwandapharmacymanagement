package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Concept;
import org.openmrs.Drug;

public class Item extends BaseTrackedEntity {

    private Integer itemId;
    private String name;
    private String code;
    private String program;
    private ItemCategory itemCategory;
    private Concept concept;
    private Drug drug;
    private Boolean dispensable = Boolean.TRUE;
    private Boolean stockable = Boolean.TRUE;
    private Boolean supplierManaged = Boolean.FALSE;
    private Integer lowStockThreshold;
    private Boolean requiresBatch = Boolean.FALSE;
    private Boolean requiresExpiry = Boolean.FALSE;
    private Boolean visibleToRequesters = Boolean.TRUE;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public ItemCategory getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(ItemCategory itemCategory) {
        this.itemCategory = itemCategory;
    }

    public Concept getConcept() {
        return concept;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    public Drug getDrug() {
        return drug;
    }

    public void setDrug(Drug drug) {
        this.drug = drug;
    }

    public Boolean getDispensable() {
        return dispensable;
    }

    public void setDispensable(Boolean dispensable) {
        this.dispensable = dispensable;
    }

    public Boolean getStockable() {
        return stockable;
    }

    public void setStockable(Boolean stockable) {
        this.stockable = stockable;
    }

    public Boolean getSupplierManaged() {
        return supplierManaged;
    }

    public void setSupplierManaged(Boolean supplierManaged) {
        this.supplierManaged = supplierManaged;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Boolean getRequiresBatch() {
        return requiresBatch;
    }

    public void setRequiresBatch(Boolean requiresBatch) {
        this.requiresBatch = requiresBatch;
    }

    public Boolean getRequiresExpiry() {
        return requiresExpiry;
    }

    public void setRequiresExpiry(Boolean requiresExpiry) {
        this.requiresExpiry = requiresExpiry;
    }

    public Boolean getVisibleToRequesters() {
        return visibleToRequesters;
    }

    public void setVisibleToRequesters(Boolean visibleToRequesters) {
        this.visibleToRequesters = visibleToRequesters;
    }
}
