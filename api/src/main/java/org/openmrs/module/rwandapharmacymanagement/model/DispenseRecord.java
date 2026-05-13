package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Patient;
import org.openmrs.Provider;

import java.util.List;

public class DispenseRecord extends BaseTrackedEntity {

    private Integer dispenseRecordId;
    private Patient patient;
    private PatientDispensingRequest patientDispensingRequest;
    private PharmacyLocation dispensingPharmacyLocation;
    private Provider dispensedBy;
    private Boolean dispensedWithoutOrder = Boolean.FALSE;
    private List<DispenseRecordLine> lines;

    public Integer getDispenseRecordId() {
        return dispenseRecordId;
    }

    public void setDispenseRecordId(Integer dispenseRecordId) {
        this.dispenseRecordId = dispenseRecordId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientDispensingRequest getPatientDispensingRequest() {
        return patientDispensingRequest;
    }

    public void setPatientDispensingRequest(PatientDispensingRequest patientDispensingRequest) {
        this.patientDispensingRequest = patientDispensingRequest;
    }

    public PharmacyLocation getDispensingPharmacyLocation() {
        return dispensingPharmacyLocation;
    }

    public void setDispensingPharmacyLocation(PharmacyLocation dispensingPharmacyLocation) {
        this.dispensingPharmacyLocation = dispensingPharmacyLocation;
    }

    public Provider getDispensedBy() {
        return dispensedBy;
    }

    public void setDispensedBy(Provider dispensedBy) {
        this.dispensedBy = dispensedBy;
    }

    public Boolean getDispensedWithoutOrder() {
        return dispensedWithoutOrder;
    }

    public void setDispensedWithoutOrder(Boolean dispensedWithoutOrder) {
        this.dispensedWithoutOrder = dispensedWithoutOrder;
    }

    public List<DispenseRecordLine> getLines() {
        return lines;
    }

    public void setLines(List<DispenseRecordLine> lines) {
        this.lines = lines;
    }
}
