package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.Patient;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.ClinicalSafetyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ClinicalSafetyAlert;
import org.openmrs.module.rwandapharmacymanagement.db.ClinicalSafetyDao;
import org.openmrs.module.rwandapharmacymanagement.model.ClinicalSafetyRule;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.enums.ClinicalRuleType;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Transactional
public class ClinicalSafetyServiceImpl extends BaseOpenmrsService implements ClinicalSafetyService {

    private ClinicalSafetyDao clinicalSafetyDao;

    public void setClinicalSafetyDao(ClinicalSafetyDao clinicalSafetyDao) {
        this.clinicalSafetyDao = clinicalSafetyDao;
    }

    @Override
    public List<ClinicalSafetyRule> getAllRules() {
        return clinicalSafetyDao.getAllRules();
    }

    @Override
    public ClinicalSafetyRule saveRule(ClinicalSafetyRule rule) {
        return clinicalSafetyDao.saveRule(rule);
    }

    @Override
    public List<ClinicalSafetyAlert> evaluateDispenseSafety(Patient patient, List<Item> items) {
        List<ClinicalSafetyAlert> alerts = new ArrayList<ClinicalSafetyAlert>();
        if (items == null || items.isEmpty()) {
            return alerts;
        }
        Set<String> itemCodes = new HashSet<String>();
        for (Item item : items) {
            if (item != null && item.getCode() != null) {
                itemCodes.add(item.getCode().trim().toUpperCase());
            }
        }
        for (ClinicalSafetyRule rule : getAllRules()) {
            if (rule == null || !Boolean.TRUE.equals(rule.getActive())) {
                continue;
            }
            if (rule.getRuleType() == ClinicalRuleType.GENDER_RESTRICTION
                    && matchesGenderRestriction(rule, patient, itemCodes)) {
                alerts.add(new ClinicalSafetyAlert(buildMessage(rule), rule.getSeverity(), Boolean.TRUE.equals(rule.getBlocking())));
            } else if (rule.getRuleType() == ClinicalRuleType.DRUG_INTERACTION
                    && matchesInteractionRule(rule, itemCodes)) {
                alerts.add(new ClinicalSafetyAlert(buildMessage(rule), rule.getSeverity(), Boolean.TRUE.equals(rule.getBlocking())));
            }
        }
        return alerts;
    }

    private boolean matchesGenderRestriction(ClinicalSafetyRule rule, Patient patient, Set<String> itemCodes) {
        if (patient == null || patient.getGender() == null || rule.getGenderRestriction() == null) {
            return false;
        }
        Set<String> configuredCodes = parseCodes(rule.getRuleDefinition());
        return !configuredCodes.isEmpty()
                && configuredCodes.removeAll(itemCodes)
                && !patient.getGender().equalsIgnoreCase(rule.getGenderRestriction());
    }

    private boolean matchesInteractionRule(ClinicalSafetyRule rule, Set<String> itemCodes) {
        Set<String> configuredCodes = parseCodes(rule.getRuleDefinition());
        if (configuredCodes.size() < 2) {
            return false;
        }
        int matches = 0;
        for (String code : configuredCodes) {
            if (itemCodes.contains(code)) {
                matches++;
            }
        }
        return matches >= 2;
    }

    private Set<String> parseCodes(String ruleDefinition) {
        Set<String> codes = new HashSet<String>();
        if (ruleDefinition == null) {
            return codes;
        }
        String[] tokens = ruleDefinition.split("[,|]");
        for (String token : tokens) {
            if (token != null && !token.trim().isEmpty()) {
                codes.add(token.trim().toUpperCase());
            }
        }
        return codes;
    }

    private String buildMessage(ClinicalSafetyRule rule) {
        return rule.getRuleName() + (rule.getRuleDefinition() == null || rule.getRuleDefinition().trim().isEmpty()
                ? "" : " [" + rule.getRuleDefinition() + "]");
    }
}
