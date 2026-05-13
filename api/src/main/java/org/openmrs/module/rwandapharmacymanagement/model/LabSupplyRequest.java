package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Location;
import org.openmrs.User;

import java.util.Date;
import java.util.List;

public class LabSupplyRequest extends BaseTrackedEntity {

    private Integer labSupplyRequestId;
    private Location labUnit;
    private PharmacyLocation dispensingPharmacyLocation;
    private String requestStatus;
    private User approvedBy;
    private Date approvedDate;
    private List<LabSupplyRequestLine> lines;

    public Integer getLabSupplyRequestId() {
        return labSupplyRequestId;
    }

    public void setLabSupplyRequestId(Integer labSupplyRequestId) {
        this.labSupplyRequestId = labSupplyRequestId;
    }

    public Location getLabUnit() {
        return labUnit;
    }

    public void setLabUnit(Location labUnit) {
        this.labUnit = labUnit;
    }

    public PharmacyLocation getDispensingPharmacyLocation() {
        return dispensingPharmacyLocation;
    }

    public void setDispensingPharmacyLocation(PharmacyLocation dispensingPharmacyLocation) {
        this.dispensingPharmacyLocation = dispensingPharmacyLocation;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
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

    public List<LabSupplyRequestLine> getLines() {
        return lines;
    }

    public void setLines(List<LabSupplyRequestLine> lines) {
        this.lines = lines;
    }
}
