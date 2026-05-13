package org.openmrs.module.rwandapharmacymanagement.web.dto;

import java.util.List;

public class DeliveryReceiptImportRequest {

    private String orderNo;
    private String customerCode;
    private String customerName;
    private String deliveryId;
    private String deliveryDate;
    private Integer purchaseOrderId;
    private Integer pharmacyLocationId;
    private Boolean sendConfirmation;
    private List<DeliveryReceiptImportItem> items;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Integer getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Integer purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Integer getPharmacyLocationId() {
        return pharmacyLocationId;
    }

    public void setPharmacyLocationId(Integer pharmacyLocationId) {
        this.pharmacyLocationId = pharmacyLocationId;
    }

    public Boolean getSendConfirmation() {
        return sendConfirmation;
    }

    public void setSendConfirmation(Boolean sendConfirmation) {
        this.sendConfirmation = sendConfirmation;
    }

    public List<DeliveryReceiptImportItem> getItems() {
        return items;
    }

    public void setItems(List<DeliveryReceiptImportItem> items) {
        this.items = items;
    }
}
