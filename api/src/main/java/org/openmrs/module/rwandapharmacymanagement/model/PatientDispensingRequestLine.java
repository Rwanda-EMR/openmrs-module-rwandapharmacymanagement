package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.DrugOrder;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;

import java.math.BigDecimal;

public class PatientDispensingRequestLine extends BaseTrackedEntity {

    private Integer patientDispensingRequestLineId;
    private PatientDispensingRequest patientDispensingRequest;
    private DrugOrder order;
    private Item item;
    private BigDecimal requestedQuantity;
    private String unitOfMeasure;
    private Boolean billable = Boolean.TRUE;
    private String billingExclusionReason;
    private PharmacyLocation routedPharmacyLocation;
    private RequestStatus lineStatus;
    private String hieMedicationRequestId;

    public Integer getPatientDispensingRequestLineId() {
        return patientDispensingRequestLineId;
    }

    public void setPatientDispensingRequestLineId(Integer patientDispensingRequestLineId) {
        this.patientDispensingRequestLineId = patientDispensingRequestLineId;
    }

    public PatientDispensingRequest getPatientDispensingRequest() {
        return patientDispensingRequest;
    }

    public void setPatientDispensingRequest(PatientDispensingRequest patientDispensingRequest) {
        this.patientDispensingRequest = patientDispensingRequest;
    }

    public DrugOrder getOrder() {
        return order;
    }

    public void setOrder(DrugOrder order) {
        this.order = order;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(BigDecimal requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Boolean getBillable() {
        return billable;
    }

    public void setBillable(Boolean billable) {
        this.billable = billable;
    }

    public String getBillingExclusionReason() {
        return billingExclusionReason;
    }

    public void setBillingExclusionReason(String billingExclusionReason) {
        this.billingExclusionReason = billingExclusionReason;
    }

    public PharmacyLocation getRoutedPharmacyLocation() {
        return routedPharmacyLocation;
    }

    public void setRoutedPharmacyLocation(PharmacyLocation routedPharmacyLocation) {
        this.routedPharmacyLocation = routedPharmacyLocation;
    }

    public RequestStatus getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(RequestStatus lineStatus) {
        this.lineStatus = lineStatus;
    }

    public String getHieMedicationRequestId() {
        return hieMedicationRequestId;
    }

    public void setHieMedicationRequestId(String hieMedicationRequestId) {
        this.hieMedicationRequestId = hieMedicationRequestId;
    }
}
