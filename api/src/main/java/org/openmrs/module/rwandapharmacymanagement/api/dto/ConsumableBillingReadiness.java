package org.openmrs.module.rwandapharmacymanagement.api.dto;

public class ConsumableBillingReadiness {

    private final boolean ready;
    private final String message;

    public ConsumableBillingReadiness(boolean ready, String message) {
        this.ready = ready;
        this.message = message;
    }

    public boolean isReady() {
        return ready;
    }

    public String getMessage() {
        return message;
    }
}
