package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;

public class DepartmentBulkRequestLineCommand {

    private Integer itemId;
    private BigDecimal previousRequestedQuantity;
    private BigDecimal previousReceivedQuantity;
    private BigDecimal previousUsedQuantity;
    private BigDecimal currentRemainingQuantity;
    private BigDecimal requestedQuantity;
    private String lineJustification;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getPreviousRequestedQuantity() {
        return previousRequestedQuantity;
    }

    public void setPreviousRequestedQuantity(BigDecimal previousRequestedQuantity) {
        this.previousRequestedQuantity = previousRequestedQuantity;
    }

    public BigDecimal getPreviousReceivedQuantity() {
        return previousReceivedQuantity;
    }

    public void setPreviousReceivedQuantity(BigDecimal previousReceivedQuantity) {
        this.previousReceivedQuantity = previousReceivedQuantity;
    }

    public BigDecimal getPreviousUsedQuantity() {
        return previousUsedQuantity;
    }

    public void setPreviousUsedQuantity(BigDecimal previousUsedQuantity) {
        this.previousUsedQuantity = previousUsedQuantity;
    }

    public BigDecimal getCurrentRemainingQuantity() {
        return currentRemainingQuantity;
    }

    public void setCurrentRemainingQuantity(BigDecimal currentRemainingQuantity) {
        this.currentRemainingQuantity = currentRemainingQuantity;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(BigDecimal requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getLineJustification() {
        return lineJustification;
    }

    public void setLineJustification(String lineJustification) {
        this.lineJustification = lineJustification;
    }
}
