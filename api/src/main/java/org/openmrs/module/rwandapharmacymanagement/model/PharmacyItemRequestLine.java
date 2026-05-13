package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;

public class PharmacyItemRequestLine extends BaseTrackedEntity {

    private Integer pharmacyItemRequestLineId;
    private PharmacyItemRequest pharmacyItemRequest;
    private Item item;
    private BigDecimal requestedQuantity;
    private BigDecimal approvedQuantity;

    public Integer getPharmacyItemRequestLineId() {
        return pharmacyItemRequestLineId;
    }

    public void setPharmacyItemRequestLineId(Integer pharmacyItemRequestLineId) {
        this.pharmacyItemRequestLineId = pharmacyItemRequestLineId;
    }

    public PharmacyItemRequest getPharmacyItemRequest() {
        return pharmacyItemRequest;
    }

    public void setPharmacyItemRequest(PharmacyItemRequest pharmacyItemRequest) {
        this.pharmacyItemRequest = pharmacyItemRequest;
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

    public BigDecimal getApprovedQuantity() {
        return approvedQuantity;
    }

    public void setApprovedQuantity(BigDecimal approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }
}
