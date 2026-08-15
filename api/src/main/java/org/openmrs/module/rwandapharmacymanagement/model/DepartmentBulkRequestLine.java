package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;

public class DepartmentBulkRequestLine extends BaseTrackedEntity {

    private Integer departmentBulkRequestLineId;
    private DepartmentBulkRequest departmentBulkRequest;
    private Item item;
    private BigDecimal previousRequestedQuantity;
    private BigDecimal previousReceivedQuantity;
    private BigDecimal previousUsedQuantity;
    private BigDecimal currentRemainingQuantity;
    private BigDecimal requestedQuantity;
    private BigDecimal approvedQuantity;
    private BigDecimal dispensedQuantity;
    private String batchNumber;
    private String lineJustification;

    public Integer getDepartmentBulkRequestLineId() {
        return departmentBulkRequestLineId;
    }

    public void setDepartmentBulkRequestLineId(Integer departmentBulkRequestLineId) {
        this.departmentBulkRequestLineId = departmentBulkRequestLineId;
    }

    public DepartmentBulkRequest getDepartmentBulkRequest() {
        return departmentBulkRequest;
    }

    public void setDepartmentBulkRequest(DepartmentBulkRequest departmentBulkRequest) {
        this.departmentBulkRequest = departmentBulkRequest;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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

    public BigDecimal getApprovedQuantity() {
        return approvedQuantity;
    }

    public void setApprovedQuantity(BigDecimal approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }

    public BigDecimal getDispensedQuantity() {
        return dispensedQuantity;
    }

    public void setDispensedQuantity(BigDecimal dispensedQuantity) {
        this.dispensedQuantity = dispensedQuantity;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getLineJustification() {
        return lineJustification;
    }

    public void setLineJustification(String lineJustification) {
        this.lineJustification = lineJustification;
    }
}
