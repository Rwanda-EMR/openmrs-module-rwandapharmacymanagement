package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;
import java.util.Date;

public class DeliveryReceiptLine extends BaseTrackedEntity {

    private Integer deliveryReceiptLineId;
    private DeliveryReceipt deliveryReceipt;
    private Item item;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal acceptedQuantity;
    private BigDecimal unitPrice;
    private String confirmationItemNo;
    private String rejectionReasonCode;
    private String rejectionReasonDetail;
    private String notes;

    public Integer getDeliveryReceiptLineId() {
        return deliveryReceiptLineId;
    }

    public void setDeliveryReceiptLineId(Integer deliveryReceiptLineId) {
        this.deliveryReceiptLineId = deliveryReceiptLineId;
    }

    public DeliveryReceipt getDeliveryReceipt() {
        return deliveryReceipt;
    }

    public void setDeliveryReceipt(DeliveryReceipt deliveryReceipt) {
        this.deliveryReceipt = deliveryReceipt;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(BigDecimal orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public BigDecimal getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(BigDecimal acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getConfirmationItemNo() {
        return confirmationItemNo;
    }

    public void setConfirmationItemNo(String confirmationItemNo) {
        this.confirmationItemNo = confirmationItemNo;
    }

    public String getRejectionReasonCode() {
        return rejectionReasonCode;
    }

    public void setRejectionReasonCode(String rejectionReasonCode) {
        this.rejectionReasonCode = rejectionReasonCode;
    }

    public String getRejectionReasonDetail() {
        return rejectionReasonDetail;
    }

    public void setRejectionReasonDetail(String rejectionReasonDetail) {
        this.rejectionReasonDetail = rejectionReasonDetail;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
