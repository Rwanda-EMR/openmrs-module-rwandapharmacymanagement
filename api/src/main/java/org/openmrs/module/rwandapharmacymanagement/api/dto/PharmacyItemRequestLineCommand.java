package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;

public class PharmacyItemRequestLineCommand {

    private Integer itemId;
    private BigDecimal quantity;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
