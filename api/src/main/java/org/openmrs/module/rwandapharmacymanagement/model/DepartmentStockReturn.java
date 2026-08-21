package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;

import java.util.Date;
import java.util.List;

public class DepartmentStockReturn extends BaseTrackedEntity {

    private Integer departmentStockReturnId;
    private PharmacyLocation departmentLocation;
    private PharmacyLocation destinationPharmacyLocation;
    private String returnStatus;
    private String reason;
    private User receivedBy;
    private Date receivedDate;
    private User rejectedBy;
    private Date rejectedDate;
    private String rejectionReason;
    private List<DepartmentStockReturnLine> lines;

    public Integer getDepartmentStockReturnId() {
        return departmentStockReturnId;
    }

    public void setDepartmentStockReturnId(Integer departmentStockReturnId) {
        this.departmentStockReturnId = departmentStockReturnId;
    }

    public PharmacyLocation getDepartmentLocation() {
        return departmentLocation;
    }

    public void setDepartmentLocation(PharmacyLocation departmentLocation) {
        this.departmentLocation = departmentLocation;
    }

    public PharmacyLocation getDestinationPharmacyLocation() {
        return destinationPharmacyLocation;
    }

    public void setDestinationPharmacyLocation(PharmacyLocation destinationPharmacyLocation) {
        this.destinationPharmacyLocation = destinationPharmacyLocation;
    }

    public String getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(String returnStatus) {
        this.returnStatus = returnStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(User receivedBy) {
        this.receivedBy = receivedBy;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
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

    public List<DepartmentStockReturnLine> getLines() {
        return lines;
    }

    public void setLines(List<DepartmentStockReturnLine> lines) {
        this.lines = lines;
    }
}
