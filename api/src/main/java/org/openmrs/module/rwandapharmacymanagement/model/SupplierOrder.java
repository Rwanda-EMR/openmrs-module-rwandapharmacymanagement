package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Location;
import org.openmrs.User;

import java.util.List;

public class SupplierOrder extends BaseTrackedEntity {

    private Integer supplierOrderId;
    private String orderNumber;
    private String supplierOrderReference;
    private Supplier supplier;
    private Location facilityLocation;
    private User requestedBy;
    private String status;
    private List<SupplierOrderLine> lines;

    public Integer getSupplierOrderId() {
        return supplierOrderId;
    }

    public void setSupplierOrderId(Integer supplierOrderId) {
        this.supplierOrderId = supplierOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getSupplierOrderReference() {
        return supplierOrderReference;
    }

    public void setSupplierOrderReference(String supplierOrderReference) {
        this.supplierOrderReference = supplierOrderReference;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Location getFacilityLocation() {
        return facilityLocation;
    }

    public void setFacilityLocation(Location facilityLocation) {
        this.facilityLocation = facilityLocation;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SupplierOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<SupplierOrderLine> lines) {
        this.lines = lines;
    }
}
