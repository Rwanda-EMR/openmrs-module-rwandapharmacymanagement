package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.module.rwandapharmacymanagement.model.enums.ClinicalRuleType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RuleSeverity;

public class ClinicalSafetyRule extends BaseTrackedEntity {

    private Integer clinicalSafetyRuleId;
    private String ruleName;
    private ClinicalRuleType ruleType;
    private RuleSeverity severity;
    private Boolean blocking = Boolean.FALSE;
    private Boolean active = Boolean.TRUE;
    private Drug drug;
    private Drug interactingDrug;
    private Concept contraindicatedCondition;
    private String genderRestriction;
    private String ruleDefinition;

    public Integer getClinicalSafetyRuleId() {
        return clinicalSafetyRuleId;
    }

    public void setClinicalSafetyRuleId(Integer clinicalSafetyRuleId) {
        this.clinicalSafetyRuleId = clinicalSafetyRuleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public ClinicalRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(ClinicalRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public RuleSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(RuleSeverity severity) {
        this.severity = severity;
    }

    public Boolean getBlocking() {
        return blocking;
    }

    public void setBlocking(Boolean blocking) {
        this.blocking = blocking;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Drug getDrug() {
        return drug;
    }

    public void setDrug(Drug drug) {
        this.drug = drug;
    }

    public Drug getInteractingDrug() {
        return interactingDrug;
    }

    public void setInteractingDrug(Drug interactingDrug) {
        this.interactingDrug = interactingDrug;
    }

    public Concept getContraindicatedCondition() {
        return contraindicatedCondition;
    }

    public void setContraindicatedCondition(Concept contraindicatedCondition) {
        this.contraindicatedCondition = contraindicatedCondition;
    }

    public String getGenderRestriction() {
        return genderRestriction;
    }

    public void setGenderRestriction(String genderRestriction) {
        this.genderRestriction = genderRestriction;
    }

    public String getRuleDefinition() {
        return ruleDefinition;
    }

    public void setRuleDefinition(String ruleDefinition) {
        this.ruleDefinition = ruleDefinition;
    }
}
