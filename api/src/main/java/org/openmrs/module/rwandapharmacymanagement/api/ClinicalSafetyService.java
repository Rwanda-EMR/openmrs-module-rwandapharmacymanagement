package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ClinicalSafetyAlert;
import org.openmrs.module.rwandapharmacymanagement.model.ClinicalSafetyRule;
import org.openmrs.module.rwandapharmacymanagement.model.Item;

import java.util.List;

public interface ClinicalSafetyService extends OpenmrsService {

    List<ClinicalSafetyRule> getAllRules();

    ClinicalSafetyRule saveRule(ClinicalSafetyRule rule);

    List<ClinicalSafetyAlert> evaluateDispenseSafety(Patient patient, List<Item> items);
}
