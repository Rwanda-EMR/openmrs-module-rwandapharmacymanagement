package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Concept;

import java.math.BigDecimal;

public class LabSupplyRequestLine extends BaseTrackedEntity {

    private Integer labSupplyRequestLineId;
    private LabSupplyRequest labSupplyRequest;
    private Item item;
    private Concept labServiceConcept;
    private BigDecimal requestedQuantity;
    private BigDecimal approvedQuantity;

    public Integer getLabSupplyRequestLineId() {
        return labSupplyRequestLineId;
    }

    public void setLabSupplyRequestLineId(Integer labSupplyRequestLineId) {
        this.labSupplyRequestLineId = labSupplyRequestLineId;
    }

    public LabSupplyRequest getLabSupplyRequest() {
        return labSupplyRequest;
    }

    public void setLabSupplyRequest(LabSupplyRequest labSupplyRequest) {
        this.labSupplyRequest = labSupplyRequest;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Concept getLabServiceConcept() {
        return labServiceConcept;
    }

    public void setLabServiceConcept(Concept labServiceConcept) {
        this.labServiceConcept = labServiceConcept;
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
