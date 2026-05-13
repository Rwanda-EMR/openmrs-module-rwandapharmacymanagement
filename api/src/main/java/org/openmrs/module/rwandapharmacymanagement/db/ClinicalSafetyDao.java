package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.ClinicalSafetyRule;

import java.util.List;

public interface ClinicalSafetyDao {

    List<ClinicalSafetyRule> getAllRules();

    ClinicalSafetyRule saveRule(ClinicalSafetyRule rule);
}
