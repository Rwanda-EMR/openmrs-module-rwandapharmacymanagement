package org.openmrs.module.rwandapharmacymanagement.model;

public class AdjustmentReason extends BaseTrackedEntity {

    private Integer adjustmentReasonId;
    private String name;
    private String description;
    private Boolean active = Boolean.TRUE;

    public Integer getAdjustmentReasonId() {
        return adjustmentReasonId;
    }

    public void setAdjustmentReasonId(Integer adjustmentReasonId) {
        this.adjustmentReasonId = adjustmentReasonId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
