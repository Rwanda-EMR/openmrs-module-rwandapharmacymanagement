package org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto;

import java.util.List;

public class SupplierDeliveryDetailsResponse {

    private String orderNo;
    private String customerCode;
    private String customerName;
    private String deliveryId;
    private String deliveryDate;
    private List<SupplierDeliveryDetailsItem> items;

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

    public List<SupplierDeliveryDetailsItem> getItems() {
        return items;
    }

    public void setItems(List<SupplierDeliveryDetailsItem> items) {
        this.items = items;
    }
}
