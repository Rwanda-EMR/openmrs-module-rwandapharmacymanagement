package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;

public class LabSupplyRequestLineCommand {

    private Integer itemId;
    private Integer labServiceConceptId;
    private BigDecimal quantity;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getLabServiceConceptId() {
        return labServiceConceptId;
    }

    public void setLabServiceConceptId(Integer labServiceConceptId) {
        this.labServiceConceptId = labServiceConceptId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
