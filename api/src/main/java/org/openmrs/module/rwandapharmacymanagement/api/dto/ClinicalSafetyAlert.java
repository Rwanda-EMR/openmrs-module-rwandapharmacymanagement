package org.openmrs.module.rwandapharmacymanagement.api.dto;

import org.openmrs.module.rwandapharmacymanagement.model.enums.RuleSeverity;

public class ClinicalSafetyAlert {

    private String message;
    private RuleSeverity severity;
    private boolean blocking;

    public ClinicalSafetyAlert(String message, RuleSeverity severity, boolean blocking) {
        this.message = message;
        this.severity = severity;
        this.blocking = blocking;
    }

    public String getMessage() {
        return message;
    }

    public RuleSeverity getSeverity() {
        return severity;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
