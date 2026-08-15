package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PaymentExceptionType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

import java.util.Date;
import java.util.List;

public class PatientDispensingRequest extends BaseTrackedEntity {

    private Integer patientDispensingRequestId;
    private Patient patient;
    private Visit visit;
    private Encounter encounter;
    private Location requestingLocation;
    private Order order;
    private Provider orderingProvider;
    private PharmacyLocation targetPharmacyLocation;
    private Integer patientBillId;
    private Boolean billable = Boolean.TRUE;
    private Boolean paymentConfirmed = Boolean.FALSE;
    private Boolean paymentExceptionApproved = Boolean.FALSE;
    private PaymentExceptionType paymentExceptionType;
    private String paymentExceptionReason;
    private String paymentExceptionNote;
    private User paymentExceptionApprovedBy;
    private Date paymentExceptionApprovedDate;
    private RequestStatus requestStatus;
    private RequestType requestType = RequestType.PRESCRIPTION;
    private String medicalActReference;
    private String hiePrescriptionGroupId;
    private List<PatientDispensingRequestLine> lines;

    public Integer getPatientDispensingRequestId() {
        return patientDispensingRequestId;
    }

    public void setPatientDispensingRequestId(Integer patientDispensingRequestId) {
        this.patientDispensingRequestId = patientDispensingRequestId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Visit getVisit() {
        return visit;
    }

    public void setVisit(Visit visit) {
        this.visit = visit;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public Location getRequestingLocation() {
        return requestingLocation;
    }

    public void setRequestingLocation(Location requestingLocation) {
        this.requestingLocation = requestingLocation;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Provider getOrderingProvider() {
        return orderingProvider;
    }

    public void setOrderingProvider(Provider orderingProvider) {
        this.orderingProvider = orderingProvider;
    }

    public PharmacyLocation getTargetPharmacyLocation() {
        return targetPharmacyLocation;
    }

    public void setTargetPharmacyLocation(PharmacyLocation targetPharmacyLocation) {
        this.targetPharmacyLocation = targetPharmacyLocation;
    }

    public Integer getPatientBillId() {
        return patientBillId;
    }

    public void setPatientBillId(Integer patientBillId) {
        this.patientBillId = patientBillId;
    }

    public Boolean getBillable() {
        return billable;
    }

    public void setBillable(Boolean billable) {
        this.billable = billable;
    }

    public Boolean getPaymentConfirmed() {
        return paymentConfirmed;
    }

    public void setPaymentConfirmed(Boolean paymentConfirmed) {
        this.paymentConfirmed = paymentConfirmed;
    }

    public Boolean getPaymentExceptionApproved() {
        return paymentExceptionApproved;
    }

    public void setPaymentExceptionApproved(Boolean paymentExceptionApproved) {
        this.paymentExceptionApproved = paymentExceptionApproved;
    }

    public PaymentExceptionType getPaymentExceptionType() {
        return paymentExceptionType;
    }

    public void setPaymentExceptionType(PaymentExceptionType paymentExceptionType) {
        this.paymentExceptionType = paymentExceptionType;
    }

    public String getPaymentExceptionReason() {
        return paymentExceptionReason;
    }

    public void setPaymentExceptionReason(String paymentExceptionReason) {
        this.paymentExceptionReason = paymentExceptionReason;
    }

    public String getPaymentExceptionNote() {
        return paymentExceptionNote;
    }

    public void setPaymentExceptionNote(String paymentExceptionNote) {
        this.paymentExceptionNote = paymentExceptionNote;
    }

    public User getPaymentExceptionApprovedBy() {
        return paymentExceptionApprovedBy;
    }

    public void setPaymentExceptionApprovedBy(User paymentExceptionApprovedBy) {
        this.paymentExceptionApprovedBy = paymentExceptionApprovedBy;
    }

    public Date getPaymentExceptionApprovedDate() {
        return paymentExceptionApprovedDate;
    }

    public void setPaymentExceptionApprovedDate(Date paymentExceptionApprovedDate) {
        this.paymentExceptionApprovedDate = paymentExceptionApprovedDate;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getMedicalActReference() {
        return medicalActReference;
    }

    public void setMedicalActReference(String medicalActReference) {
        this.medicalActReference = medicalActReference;
    }

    public String getHiePrescriptionGroupId() {
        return hiePrescriptionGroupId;
    }

    public void setHiePrescriptionGroupId(String hiePrescriptionGroupId) {
        this.hiePrescriptionGroupId = hiePrescriptionGroupId;
    }

    public List<PatientDispensingRequestLine> getLines() {
        return lines;
    }

    public void setLines(List<PatientDispensingRequestLine> lines) {
        this.lines = lines;
    }
}
