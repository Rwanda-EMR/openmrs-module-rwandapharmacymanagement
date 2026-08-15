package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;

import java.math.BigDecimal;

public class DepartmentStockIssue extends BaseTrackedEntity {

    private Integer departmentStockIssueId;
    private PharmacyLocation departmentLocation;
    private Item item;
    private BigDecimal quantity;
    private String batchNumber;
    private String issuedTo;
    private String reason;
    private User issuedBy;

    public Integer getDepartmentStockIssueId() {
        return departmentStockIssueId;
    }

    public void setDepartmentStockIssueId(Integer departmentStockIssueId) {
        this.departmentStockIssueId = departmentStockIssueId;
    }

    public PharmacyLocation getDepartmentLocation() {
        return departmentLocation;
    }

    public void setDepartmentLocation(PharmacyLocation departmentLocation) {
        this.departmentLocation = departmentLocation;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(User issuedBy) {
        this.issuedBy = issuedBy;
    }
}
