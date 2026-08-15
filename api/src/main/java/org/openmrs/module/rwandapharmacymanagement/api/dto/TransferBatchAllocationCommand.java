package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;

public class TransferBatchAllocationCommand {

    private Integer transferLineId;
    private String batchNumber;
    private BigDecimal quantity;

    public Integer getTransferLineId() {
        return transferLineId;
    }

    public void setTransferLineId(Integer transferLineId) {
        this.transferLineId = transferLineId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
