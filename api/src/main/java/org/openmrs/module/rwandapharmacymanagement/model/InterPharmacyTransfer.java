package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;

import java.util.Date;
import java.util.List;

public class InterPharmacyTransfer extends BaseTrackedEntity {

    private Integer interPharmacyTransferId;
    private PharmacyLocation sourcePharmacyLocation;
    private PharmacyLocation destinationPharmacyLocation;
    private String reason;
    private String transferStatus;
    private User approvedBy;
    private Date approvedDate;
    private User rejectedBy;
    private Date rejectedDate;
    private String rejectionReason;
    private User servedBy;
    private Date servedDate;
    private List<InterPharmacyTransferLine> lines;

    public Integer getInterPharmacyTransferId() {
        return interPharmacyTransferId;
    }

    public void setInterPharmacyTransferId(Integer interPharmacyTransferId) {
        this.interPharmacyTransferId = interPharmacyTransferId;
    }

    public PharmacyLocation getSourcePharmacyLocation() {
        return sourcePharmacyLocation;
    }

    public void setSourcePharmacyLocation(PharmacyLocation sourcePharmacyLocation) {
        this.sourcePharmacyLocation = sourcePharmacyLocation;
    }

    public PharmacyLocation getDestinationPharmacyLocation() {
        return destinationPharmacyLocation;
    }

    public void setDestinationPharmacyLocation(PharmacyLocation destinationPharmacyLocation) {
        this.destinationPharmacyLocation = destinationPharmacyLocation;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTransferStatus() {
        return transferStatus;
    }

    public void setTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Date getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(Date approvedDate) {
        this.approvedDate = approvedDate;
    }

    public User getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(User rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public Date getRejectedDate() {
        return rejectedDate;
    }

    public void setRejectedDate(Date rejectedDate) {
        this.rejectedDate = rejectedDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public User getServedBy() {
        return servedBy;
    }

    public void setServedBy(User servedBy) {
        this.servedBy = servedBy;
    }

    public Date getServedDate() {
        return servedDate;
    }

    public void setServedDate(Date servedDate) {
        this.servedDate = servedDate;
    }

    public List<InterPharmacyTransferLine> getLines() {
        return lines;
    }

    public void setLines(List<InterPharmacyTransferLine> lines) {
        this.lines = lines;
    }
}
