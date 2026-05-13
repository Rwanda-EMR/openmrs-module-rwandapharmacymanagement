package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;
import java.util.Date;

public class PrescriptionLineCommand {

    private Integer productMasterItemId;
    private BigDecimal dose;
    private String route;
    private String frequency;
    private Integer duration;
    private String durationUnit;
    private BigDecimal quantity;
    private String instructions;
    private Date startDate;

    public Integer getProductMasterItemId() {
        return productMasterItemId;
    }

    public void setProductMasterItemId(Integer productMasterItemId) {
        this.productMasterItemId = productMasterItemId;
    }

    public BigDecimal getDose() {
        return dose;
    }

    public void setDose(BigDecimal dose) {
        this.dose = dose;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
}
