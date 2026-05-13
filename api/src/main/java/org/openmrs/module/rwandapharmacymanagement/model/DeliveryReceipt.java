package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Location;

import java.util.List;

public class DeliveryReceipt extends BaseTrackedEntity {

    private Integer deliveryReceiptId;
    private String receiptNumber;
    private PurchaseOrder purchaseOrder;
    private SupplierOrder supplierOrder;
    private Location deliveryLocation;
    private PharmacyLocation receivedIntoLocation;
    private String status;
    private List<DeliveryReceiptLine> lines;

    public Integer getDeliveryReceiptId() {
        return deliveryReceiptId;
    }

    public void setDeliveryReceiptId(Integer deliveryReceiptId) {
        this.deliveryReceiptId = deliveryReceiptId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public SupplierOrder getSupplierOrder() {
        return supplierOrder;
    }

    public void setSupplierOrder(SupplierOrder supplierOrder) {
        this.supplierOrder = supplierOrder;
    }

    public Location getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(Location deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public PharmacyLocation getReceivedIntoLocation() {
        return receivedIntoLocation;
    }

    public void setReceivedIntoLocation(PharmacyLocation receivedIntoLocation) {
        this.receivedIntoLocation = receivedIntoLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DeliveryReceiptLine> getLines() {
        return lines;
    }

    public void setLines(List<DeliveryReceiptLine> lines) {
        this.lines = lines;
    }
}
