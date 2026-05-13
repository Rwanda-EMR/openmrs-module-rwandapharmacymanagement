package org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto;

public class SupplierProductMasterItem {

    private String npcCode;
    private String materialDescription;
    private String materialType;

    public String getNpcCode() {
        return npcCode;
    }

    public void setNpcCode(String npcCode) {
        this.npcCode = npcCode;
    }

    public String getMaterialDescription() {
        return materialDescription;
    }

    public void setMaterialDescription(String materialDescription) {
        this.materialDescription = materialDescription;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }
}
