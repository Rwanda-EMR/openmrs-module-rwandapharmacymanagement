package org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto;

import java.util.ArrayList;
import java.util.List;

public class PurchaseOrderPriceRefreshResult {

    private int updatedCount;
    private List<String> unmatchedNpcCodes = new ArrayList<String>();
    private String errorMessage;

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public void incrementUpdatedCount() {
        this.updatedCount++;
    }

    public List<String> getUnmatchedNpcCodes() {
        return unmatchedNpcCodes;
    }

    public void setUnmatchedNpcCodes(List<String> unmatchedNpcCodes) {
        this.unmatchedNpcCodes = unmatchedNpcCodes == null ? new ArrayList<String>() : unmatchedNpcCodes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasBlockingIssue() {
        return errorMessage != null || (unmatchedNpcCodes != null && !unmatchedNpcCodes.isEmpty());
    }
}
