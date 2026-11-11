package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Supplier product master facts for one local pharmacy item, aggregated over every linked product master row.
 */
public class PrescribableItemInfo {

    private final Integer itemId;
    private boolean linked;
    private boolean anyActive;
    private boolean anyDrugMaterial;
    private final Map<String, String> restrictionReasonsByPrivilege = new LinkedHashMap<String, String>();

    public PrescribableItemInfo(Integer itemId) {
        this.itemId = itemId;
    }

    public void addLinkedRow(boolean active, boolean drugMaterial, String requiredPrivilege, String restrictionReason) {
        linked = true;
        anyActive = anyActive || active;
        anyDrugMaterial = anyDrugMaterial || drugMaterial;
        String privilege = requiredPrivilege == null ? "" : requiredPrivilege.trim();
        if (!privilege.isEmpty() && !restrictionReasonsByPrivilege.containsKey(privilege)) {
            restrictionReasonsByPrivilege.put(privilege, restrictionReason == null ? "" : restrictionReason.trim());
        }
    }

    public Integer getItemId() {
        return itemId;
    }

    public boolean isLinked() {
        return linked;
    }

    public boolean isAnyActive() {
        return anyActive;
    }

    public boolean isAnyDrugMaterial() {
        return anyDrugMaterial;
    }

    public Map<String, String> getRestrictionReasonsByPrivilege() {
        return restrictionReasonsByPrivilege;
    }
}
