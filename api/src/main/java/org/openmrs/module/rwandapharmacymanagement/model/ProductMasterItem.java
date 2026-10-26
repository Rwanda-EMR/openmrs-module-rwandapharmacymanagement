package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Drug;

import java.math.BigDecimal;

public class ProductMasterItem extends BaseTrackedEntity {

    public static final String SOURCE_API_SYNCED = "API_SYNCED";
    public static final String SOURCE_MANUAL = "MANUAL";

    private Integer productMasterItemId;
    private String itemName;
    private String npcCode;
    private String program;
    private String materialType;
    private BigDecimal itemPrice;
    private Drug drug;
    private Boolean active = Boolean.TRUE;
    private String source = SOURCE_MANUAL;
    private String requiredPrivilege;
    private String restrictionReason;

    public Integer getProductMasterItemId() {
        return productMasterItemId;
    }

    public void setProductMasterItemId(Integer productMasterItemId) {
        this.productMasterItemId = productMasterItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getNpcCode() {
        return npcCode;
    }

    public void setNpcCode(String npcCode) {
        this.npcCode = npcCode;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public Drug getDrug() {
        return drug;
    }

    public void setDrug(Drug drug) {
        this.drug = drug;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRequiredPrivilege() {
        return requiredPrivilege;
    }

    public void setRequiredPrivilege(String requiredPrivilege) {
        this.requiredPrivilege = requiredPrivilege;
    }

    public String getRestrictionReason() {
        return restrictionReason;
    }

    public void setRestrictionReason(String restrictionReason) {
        this.restrictionReason = restrictionReason;
    }
}
