package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;

import java.util.List;

public class StockAdjustment extends BaseTrackedEntity {

    private Integer stockAdjustmentId;
    private PharmacyLocation pharmacyLocation;
    private AdjustmentReason adjustmentReason;
    private String comments;
    private User adjustedBy;
    private List<StockAdjustmentLine> lines;

    public Integer getStockAdjustmentId() {
        return stockAdjustmentId;
    }

    public void setStockAdjustmentId(Integer stockAdjustmentId) {
        this.stockAdjustmentId = stockAdjustmentId;
    }

    public PharmacyLocation getPharmacyLocation() {
        return pharmacyLocation;
    }

    public void setPharmacyLocation(PharmacyLocation pharmacyLocation) {
        this.pharmacyLocation = pharmacyLocation;
    }

    public AdjustmentReason getAdjustmentReason() {
        return adjustmentReason;
    }

    public void setAdjustmentReason(AdjustmentReason adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public User getAdjustedBy() {
        return adjustedBy;
    }

    public void setAdjustedBy(User adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public List<StockAdjustmentLine> getLines() {
        return lines;
    }

    public void setLines(List<StockAdjustmentLine> lines) {
        this.lines = lines;
    }
}
