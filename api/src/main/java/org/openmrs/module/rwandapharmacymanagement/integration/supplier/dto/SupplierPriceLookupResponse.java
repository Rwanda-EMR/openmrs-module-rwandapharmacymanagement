package org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto;

import java.util.List;

public class SupplierPriceLookupResponse {

    private List<SupplierMatchedPrice> matchedPrices;
    private List<String> unmatchedNpcCodes;
    private String message;

    public List<SupplierMatchedPrice> getMatchedPrices() {
        return matchedPrices;
    }

    public void setMatchedPrices(List<SupplierMatchedPrice> matchedPrices) {
        this.matchedPrices = matchedPrices;
    }

    public List<String> getUnmatchedNpcCodes() {
        return unmatchedNpcCodes;
    }

    public void setUnmatchedNpcCodes(List<String> unmatchedNpcCodes) {
        this.unmatchedNpcCodes = unmatchedNpcCodes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
