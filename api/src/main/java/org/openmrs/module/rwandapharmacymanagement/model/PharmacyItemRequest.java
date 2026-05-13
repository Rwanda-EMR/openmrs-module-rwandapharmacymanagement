package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;

import java.util.Date;
import java.util.List;

public class PharmacyItemRequest extends BaseTrackedEntity {

    private Integer pharmacyItemRequestId;
    private PharmacyLocation requestedPharmacyLocation;
    private String requestStatus;
    private String reason;
    private User approvedBy;
    private Date approvedDate;
    private List<PharmacyItemRequestLine> lines;

    public Integer getPharmacyItemRequestId() {
        return pharmacyItemRequestId;
    }

    public void setPharmacyItemRequestId(Integer pharmacyItemRequestId) {
        this.pharmacyItemRequestId = pharmacyItemRequestId;
    }

    public PharmacyLocation getRequestedPharmacyLocation() {
        return requestedPharmacyLocation;
    }

    public void setRequestedPharmacyLocation(PharmacyLocation requestedPharmacyLocation) {
        this.requestedPharmacyLocation = requestedPharmacyLocation;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public List<PharmacyItemRequestLine> getLines() {
        return lines;
    }

    public void setLines(List<PharmacyItemRequestLine> lines) {
        this.lines = lines;
    }
}
