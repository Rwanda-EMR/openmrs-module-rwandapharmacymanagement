package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class PurchaseOrder extends BaseTrackedEntity {

    private Integer purchaseOrderId;
    private String program;
    private String facilityId;
    private Supplier supplier;
    private BigDecimal totalCost;
    private Date requestedDate;
    private User requestedBy;
    private String approvalStatus;
    private Date approvedDate;
    private User approvedBy;
    private String dispatchStatus;
    private String dispatchReference;
    private String fetchedReceiptPayload;
    private Date fetchedReceiptDate;
    private List<PurchaseOrderItem> items;

    public Integer getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Integer purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Date getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(Date requestedDate) {
        this.requestedDate = requestedDate;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Date getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(Date approvedDate) {
        this.approvedDate = approvedDate;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(String dispatchStatus) {
        this.dispatchStatus = dispatchStatus;
    }

    public String getDispatchReference() {
        return dispatchReference;
    }

    public void setDispatchReference(String dispatchReference) {
        this.dispatchReference = dispatchReference;
    }

    public String getFetchedReceiptPayload() {
        return fetchedReceiptPayload;
    }

    public void setFetchedReceiptPayload(String fetchedReceiptPayload) {
        this.fetchedReceiptPayload = fetchedReceiptPayload;
    }

    public Date getFetchedReceiptDate() {
        return fetchedReceiptDate;
    }

    public void setFetchedReceiptDate(Date fetchedReceiptDate) {
        this.fetchedReceiptDate = fetchedReceiptDate;
    }

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }
}
