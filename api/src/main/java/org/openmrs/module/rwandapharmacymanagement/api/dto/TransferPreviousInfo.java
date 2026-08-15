package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;
import java.util.Date;

public class TransferPreviousInfo {

    private Integer transferId;
    private Integer transferLineId;
    private String itemName;
    private String requestingPharmacyName;
    private Date currentRequestDate;
    private BigDecimal currentRequestedQuantity = BigDecimal.ZERO;
    private Integer previousTransferId;
    private Date previousRequestDate;
    private BigDecimal previousRequestedQuantity = BigDecimal.ZERO;
    private BigDecimal quantityBeforePreviousRequest = BigDecimal.ZERO;
    private BigDecimal receivedSincePreviousRequest = BigDecimal.ZERO;
    private BigDecimal dispensedSincePreviousRequest = BigDecimal.ZERO;
    private BigDecimal transferredOutSincePreviousRequest = BigDecimal.ZERO;
    private BigDecimal adjustedSincePreviousRequest = BigDecimal.ZERO;
    private BigDecimal quantityAtCurrentRequest = BigDecimal.ZERO;
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    public Integer getTransferId() {
        return transferId;
    }

    public void setTransferId(Integer transferId) {
        this.transferId = transferId;
    }

    public Integer getTransferLineId() {
        return transferLineId;
    }

    public void setTransferLineId(Integer transferLineId) {
        this.transferLineId = transferLineId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getRequestingPharmacyName() {
        return requestingPharmacyName;
    }

    public void setRequestingPharmacyName(String requestingPharmacyName) {
        this.requestingPharmacyName = requestingPharmacyName;
    }

    public Date getCurrentRequestDate() {
        return currentRequestDate;
    }

    public void setCurrentRequestDate(Date currentRequestDate) {
        this.currentRequestDate = currentRequestDate;
    }

    public BigDecimal getCurrentRequestedQuantity() {
        return currentRequestedQuantity;
    }

    public void setCurrentRequestedQuantity(BigDecimal currentRequestedQuantity) {
        this.currentRequestedQuantity = zeroIfNull(currentRequestedQuantity);
    }

    public Integer getPreviousTransferId() {
        return previousTransferId;
    }

    public void setPreviousTransferId(Integer previousTransferId) {
        this.previousTransferId = previousTransferId;
    }

    public Date getPreviousRequestDate() {
        return previousRequestDate;
    }

    public void setPreviousRequestDate(Date previousRequestDate) {
        this.previousRequestDate = previousRequestDate;
    }

    public BigDecimal getPreviousRequestedQuantity() {
        return previousRequestedQuantity;
    }

    public void setPreviousRequestedQuantity(BigDecimal previousRequestedQuantity) {
        this.previousRequestedQuantity = zeroIfNull(previousRequestedQuantity);
    }

    public BigDecimal getQuantityBeforePreviousRequest() {
        return quantityBeforePreviousRequest;
    }

    public void setQuantityBeforePreviousRequest(BigDecimal quantityBeforePreviousRequest) {
        this.quantityBeforePreviousRequest = zeroIfNull(quantityBeforePreviousRequest);
    }

    public BigDecimal getReceivedSincePreviousRequest() {
        return receivedSincePreviousRequest;
    }

    public void setReceivedSincePreviousRequest(BigDecimal receivedSincePreviousRequest) {
        this.receivedSincePreviousRequest = zeroIfNull(receivedSincePreviousRequest);
    }

    public BigDecimal getDispensedSincePreviousRequest() {
        return dispensedSincePreviousRequest;
    }

    public void setDispensedSincePreviousRequest(BigDecimal dispensedSincePreviousRequest) {
        this.dispensedSincePreviousRequest = zeroIfNull(dispensedSincePreviousRequest);
    }

    public BigDecimal getTransferredOutSincePreviousRequest() {
        return transferredOutSincePreviousRequest;
    }

    public void setTransferredOutSincePreviousRequest(BigDecimal transferredOutSincePreviousRequest) {
        this.transferredOutSincePreviousRequest = zeroIfNull(transferredOutSincePreviousRequest);
    }

    public BigDecimal getAdjustedSincePreviousRequest() {
        return adjustedSincePreviousRequest;
    }

    public void setAdjustedSincePreviousRequest(BigDecimal adjustedSincePreviousRequest) {
        this.adjustedSincePreviousRequest = zeroIfNull(adjustedSincePreviousRequest);
    }

    public BigDecimal getQuantityAtCurrentRequest() {
        return quantityAtCurrentRequest;
    }

    public void setQuantityAtCurrentRequest(BigDecimal quantityAtCurrentRequest) {
        this.quantityAtCurrentRequest = zeroIfNull(quantityAtCurrentRequest);
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = zeroIfNull(currentQuantity);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
