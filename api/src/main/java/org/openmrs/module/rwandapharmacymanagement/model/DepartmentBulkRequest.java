package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;

import java.util.Date;
import java.util.List;

public class DepartmentBulkRequest extends BaseTrackedEntity {

    private Integer departmentBulkRequestId;
    private PharmacyLocation departmentLocation;
    private PharmacyLocation supplyingPharmacyLocation;
    private String requestStatus;
    private String workflowMode;
    private String justification;
    private String correctionNote;
    private User financeApprovedBy;
    private Date financeApprovedDate;
    private User dispensedBy;
    private Date dispensedDate;
    private List<DepartmentBulkRequestLine> lines;

    public Integer getDepartmentBulkRequestId() {
        return departmentBulkRequestId;
    }

    public void setDepartmentBulkRequestId(Integer departmentBulkRequestId) {
        this.departmentBulkRequestId = departmentBulkRequestId;
    }

    public PharmacyLocation getDepartmentLocation() {
        return departmentLocation;
    }

    public void setDepartmentLocation(PharmacyLocation departmentLocation) {
        this.departmentLocation = departmentLocation;
    }

    public PharmacyLocation getSupplyingPharmacyLocation() {
        return supplyingPharmacyLocation;
    }

    public void setSupplyingPharmacyLocation(PharmacyLocation supplyingPharmacyLocation) {
        this.supplyingPharmacyLocation = supplyingPharmacyLocation;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getWorkflowMode() {
        return workflowMode;
    }

    public void setWorkflowMode(String workflowMode) {
        this.workflowMode = workflowMode;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getCorrectionNote() {
        return correctionNote;
    }

    public void setCorrectionNote(String correctionNote) {
        this.correctionNote = correctionNote;
    }

    public User getFinanceApprovedBy() {
        return financeApprovedBy;
    }

    public void setFinanceApprovedBy(User financeApprovedBy) {
        this.financeApprovedBy = financeApprovedBy;
    }

    public Date getFinanceApprovedDate() {
        return financeApprovedDate;
    }

    public void setFinanceApprovedDate(Date financeApprovedDate) {
        this.financeApprovedDate = financeApprovedDate;
    }

    public User getDispensedBy() {
        return dispensedBy;
    }

    public void setDispensedBy(User dispensedBy) {
        this.dispensedBy = dispensedBy;
    }

    public Date getDispensedDate() {
        return dispensedDate;
    }

    public void setDispensedDate(Date dispensedDate) {
        this.dispensedDate = dispensedDate;
    }

    public List<DepartmentBulkRequestLine> getLines() {
        return lines;
    }

    public void setLines(List<DepartmentBulkRequestLine> lines) {
        this.lines = lines;
    }
}
