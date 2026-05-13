package org.openmrs.module.rwandapharmacymanagement.page.controller.safety;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.ClinicalSafetyService;
import org.openmrs.module.rwandapharmacymanagement.model.ClinicalSafetyRule;
import org.openmrs.module.rwandapharmacymanagement.model.enums.ClinicalRuleType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RuleSeverity;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RulesPageController {

    public void get(PageModel model, @SpringBean("clinicalSafetyService") ClinicalSafetyService clinicalSafetyService) {
        model.addAttribute("rules", clinicalSafetyService.getAllRules());
    }

    public String post(@SpringBean("clinicalSafetyService") ClinicalSafetyService clinicalSafetyService,
                       @RequestParam("ruleName") String ruleName,
                       @RequestParam("ruleType") ClinicalRuleType ruleType,
                       @RequestParam("severity") RuleSeverity severity,
                       @RequestParam(value = "blocking", required = false) Boolean blocking,
                       @RequestParam(value = "genderRestriction", required = false) String genderRestriction,
                       @RequestParam(value = "ruleDefinition", required = false) String ruleDefinition,
                       UiUtils ui) {
        ClinicalSafetyRule rule = new ClinicalSafetyRule();
        rule.setRuleName(ruleName);
        rule.setRuleType(ruleType);
        rule.setSeverity(severity);
        rule.setBlocking(Boolean.TRUE.equals(blocking));
        rule.setActive(true);
        rule.setGenderRestriction(genderRestriction);
        rule.setRuleDefinition(ruleDefinition);
        rule.setCreator(Context.getAuthenticatedUser());
        rule.setDateCreated(new java.util.Date());
        clinicalSafetyService.saveRule(rule);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "safety/rules");
    }
}
