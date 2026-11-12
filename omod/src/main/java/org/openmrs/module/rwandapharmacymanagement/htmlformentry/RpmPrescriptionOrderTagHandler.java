package org.openmrs.module.rwandapharmacymanagement.htmlformentry;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.rwandapharmacymanagement.api.PrescriptionService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescriptionLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.Item;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RpmPrescriptionOrderTagHandler extends AbstractRpmOrderTagHandler {

    private static final String ERROR_ID = "rpmPrescriptionOrder";
    private static final String ITEM_PARAM = "rpmPrescriptionItemId";
    private static final String LEGACY_PRODUCT_PARAM = "rpmPrescriptionProductMasterItemId";
    private static final String DOSE_PARAM = "rpmPrescriptionDose";
    private static final String ROUTE_PARAM = "rpmPrescriptionRoute";
    private static final String FREQUENCY_PARAM = "rpmPrescriptionFrequency";
    private static final String DURATION_PARAM = "rpmPrescriptionDuration";
    private static final String DURATION_UNIT_PARAM = "rpmPrescriptionDurationUnit";
    private static final String QUANTITY_PARAM = "rpmPrescriptionQuantity";
    private static final String INSTRUCTIONS_PARAM = "rpmPrescriptionInstructions";
    private static final String BILLABLE_PARAM = "rpmPrescriptionBillable";
    private static final String BILLING_REASON_PARAM = "rpmPrescriptionBillingExclusionReason";

    @Override
    protected void registerAction(FormSubmissionController controller, Map<String, String> parameters) {
        controller.addAction(new Action(booleanAttr(parameters, "required", false), integerAttr(parameters, "patientBillId")));
    }

    @Override
    protected String render(FormEntrySession session, Map<String, String> parameters) throws BadFormDesignException {
        Patient patient = getPatient(session);
        String title = attr(parameters, "title", "Prescription order");
        String buttonLabel = attr(parameters, "buttonLabel", "Add prescription line");
        StringBuilder html = new StringBuilder();
        html.append(sharedStyles());
        html.append("<div class=\"rpm-hfe-order\" id=\"rpm-prescription-order\">");
        html.append("<div class=\"rpm-hfe-head\">").append(h(title)).append("</div><div class=\"rpm-hfe-body\">");
        if (patient == null) {
            html.append("<div class=\"rpm-hfe-note\">No patient is available for this form.</div></div></div>");
            return html.toString();
        }
        html.append("<div class=\"rpm-hfe-grid\">");
        html.append("<div class=\"rpm-hfe-field\"><label>Drug</label><select id=\"rpm-rx-product\"><option value=\"\">Select drug</option>");
        for (Item item : getPrescribableItems()) {
            html.append("<option value=\"").append(item.getItemId()).append("\">")
                    .append(h(item.getName())).append("</option>");
        }
        html.append("</select></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Dose</label><input type=\"number\" step=\"0.01\" id=\"rpm-rx-dose\"/></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Route</label><select id=\"rpm-rx-route\"><option value=\"\">Select route</option>");
        for (Concept route : safeConcepts(Context.getOrderService().getDrugRoutes())) {
            html.append("<option value=\"").append(h(route.getDisplayString())).append("\">").append(h(route.getDisplayString())).append("</option>");
        }
        html.append("</select></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Frequency</label><select id=\"rpm-rx-frequency\"><option value=\"\">Select frequency</option>");
        for (org.openmrs.OrderFrequency frequency : safeOrderFrequencies(Context.getOrderService().getOrderFrequencies(false))) {
            if (frequency.getConcept() != null) {
                html.append("<option value=\"").append(h(frequency.getConcept().getDisplayString())).append("\">")
                        .append(h(frequency.getConcept().getDisplayString())).append("</option>");
            }
        }
        html.append("</select></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Duration</label><input type=\"number\" step=\"1\" id=\"rpm-rx-duration\"/></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Duration unit</label><select id=\"rpm-rx-duration-unit\"><option value=\"\">Select unit</option>");
        for (Concept durationUnit : safeConcepts(Context.getOrderService().getDurationUnits())) {
            html.append("<option value=\"").append(h(durationUnit.getDisplayString())).append("\">")
                    .append(h(durationUnit.getDisplayString())).append("</option>");
        }
        html.append("</select></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Quantity</label><input type=\"number\" step=\"0.01\" id=\"rpm-rx-quantity\"/></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Instructions</label><input type=\"text\" id=\"rpm-rx-instructions\"/></div>");
        html.append("<div class=\"rpm-hfe-field\"><label><input type=\"checkbox\" id=\"rpm-rx-not-billable\" style=\"width:auto\"/> Do not bill</label><input type=\"text\" id=\"rpm-rx-billing-reason\" placeholder=\"Reason\"/></div>");
        html.append("</div>");
        html.append("<div class=\"rpm-hfe-note\">Lines are queued when the HTML form is saved.</div>");
        html.append("<button type=\"button\" class=\"rpm-hfe-btn\" onclick=\"rpmAddPrescriptionLine()\">").append(h(buttonLabel)).append("</button>");
        html.append("<table class=\"rpm-hfe-table\"><thead><tr><th>Drug</th><th>Dose</th><th>Route</th><th>Frequency</th><th>Duration</th><th>Quantity</th><th></th></tr></thead><tbody id=\"rpm-rx-lines\"><tr id=\"rpm-rx-empty\"><td colspan=\"7\">No prescription lines added.</td></tr></tbody></table>");
        html.append("</div></div>");
        html.append("<script type=\"text/javascript\">")
                .append("function rpmRxVal(id){var e=document.getElementById(id);return e?e.value:'';}")
                .append("function rpmRxText(id){var e=document.getElementById(id);return e&&e.selectedIndex>=0?e.options[e.selectedIndex].text:'';}")
                .append("function rpmRxHidden(n,v){var i=document.createElement('input');i.type='hidden';i.name=n;i.value=v||'';return i;}")
                .append("function rpmRxCell(t){var td=document.createElement('td');td.appendChild(document.createTextNode(t||''));return td;}")
                .append("function rpmAddPrescriptionLine(){var pid=rpmRxVal('rpm-rx-product'),ptext=rpmRxText('rpm-rx-product'),qty=rpmRxVal('rpm-rx-quantity');")
                .append("if(!pid||!qty){alert('Select a drug and enter quantity.');return;}")
                .append("var tbody=document.getElementById('rpm-rx-lines'),empty=document.getElementById('rpm-rx-empty');if(empty){empty.parentNode.removeChild(empty);}")
                .append("var tr=document.createElement('tr');tr.appendChild(rpmRxCell(ptext));tr.appendChild(rpmRxCell(rpmRxVal('rpm-rx-dose')));tr.appendChild(rpmRxCell(rpmRxVal('rpm-rx-route')));tr.appendChild(rpmRxCell(rpmRxVal('rpm-rx-frequency')));tr.appendChild(rpmRxCell(rpmRxVal('rpm-rx-duration')+' '+rpmRxVal('rpm-rx-duration-unit')));tr.appendChild(rpmRxCell(qty));")
                .append("var td=document.createElement('td'),btn=document.createElement('button');btn.type='button';btn.className='rpm-hfe-remove';btn.appendChild(document.createTextNode('Remove'));btn.onclick=function(){tbody.removeChild(tr);};td.appendChild(btn);")
                .append("tr.appendChild(rpmRxHidden('").append(ITEM_PARAM).append("',pid));tr.appendChild(rpmRxHidden('").append(DOSE_PARAM).append("',rpmRxVal('rpm-rx-dose')));tr.appendChild(rpmRxHidden('").append(ROUTE_PARAM).append("',rpmRxVal('rpm-rx-route')));tr.appendChild(rpmRxHidden('").append(FREQUENCY_PARAM).append("',rpmRxVal('rpm-rx-frequency')));tr.appendChild(rpmRxHidden('").append(DURATION_PARAM).append("',rpmRxVal('rpm-rx-duration')));tr.appendChild(rpmRxHidden('").append(DURATION_UNIT_PARAM).append("',rpmRxVal('rpm-rx-duration-unit')));tr.appendChild(rpmRxHidden('").append(QUANTITY_PARAM).append("',qty));tr.appendChild(rpmRxHidden('").append(INSTRUCTIONS_PARAM).append("',rpmRxVal('rpm-rx-instructions')));tr.appendChild(rpmRxHidden('").append(BILLABLE_PARAM).append("',document.getElementById('rpm-rx-not-billable').checked?'false':'true'));tr.appendChild(rpmRxHidden('").append(BILLING_REASON_PARAM).append("',rpmRxVal('rpm-rx-billing-reason')));tr.appendChild(td);tbody.appendChild(tr);")
                .append("document.getElementById('rpm-rx-product').value='';document.getElementById('rpm-rx-dose').value='';document.getElementById('rpm-rx-quantity').value='';document.getElementById('rpm-rx-instructions').value='';document.getElementById('rpm-rx-billing-reason').value='';document.getElementById('rpm-rx-not-billable').checked=false;}")
                .append("</script>");
        return html.toString();
    }

    private static List<Concept> safeConcepts(List<Concept> concepts) {
        return concepts == null ? Collections.<Concept>emptyList() : concepts;
    }

    private static List<org.openmrs.OrderFrequency> safeOrderFrequencies(List<org.openmrs.OrderFrequency> frequencies) {
        return frequencies == null ? Collections.<org.openmrs.OrderFrequency>emptyList() : frequencies;
    }

    private static class Action implements FormSubmissionControllerAction {

        private final boolean required;
        private final Integer patientBillId;

        private Action(boolean required, Integer patientBillId) {
            this.required = required;
            this.patientBillId = patientBillId;
        }

        @Override
        public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest request) {
            List<FormSubmissionError> errors = new ArrayList<FormSubmissionError>();
            List<PrescriptionLineCommand> lines = buildCommands(request, errors);
            if (lines.isEmpty()) {
                if (required) {
                    errors.add(new FormSubmissionError(ERROR_ID, "Add at least one prescription line."));
                }
                return errors;
            }
            Patient patient = context == null ? null : context.getExistingPatient();
            if (patient == null) {
                errors.add(new FormSubmissionError(ERROR_ID, "No patient was found for the prescription order."));
            }
            if (patient != null && activeVisit(patient) == null) {
                errors.add(new FormSubmissionError(ERROR_ID, "An active visit is required before creating a prescription order."));
            }
            if (patient != null && errors.isEmpty()) {
                String openPrescriptionConflict = Context.getRegisteredComponent("prescriptionService", PrescriptionService.class)
                        .getOpenPrescriptionConflict(patient, lines);
                if (openPrescriptionConflict != null) {
                    errors.add(new FormSubmissionError(ERROR_ID, openPrescriptionConflict));
                }
            }
            return errors;
        }

        @Override
        public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
            List<PrescriptionLineCommand> lines = buildCommands(request, new ArrayList<FormSubmissionError>());
            if (lines.isEmpty()) {
                return;
            }
            Patient patient = session.getPatient();
            Visit visit = activeVisit(patient);
            Location location = session.getEncounter() != null && session.getEncounter().getLocation() != null
                    ? session.getEncounter().getLocation()
                    : Context.getUserContext().getLocation();
            Context.getRegisteredComponent("prescriptionService", PrescriptionService.class)
                    .createPrescription(patient, visit, orderingProvider(), location, lines, patientBillId);
        }

        private static List<PrescriptionLineCommand> buildCommands(HttpServletRequest request, List<FormSubmissionError> errors) {
            List<PrescriptionLineCommand> commands = new ArrayList<PrescriptionLineCommand>();
            String[] productIds = request.getParameterValues(ITEM_PARAM);
            // Forms rendered before the item-based list was deployed still post product master ids.
            boolean byItem = productIds != null;
            if (!byItem) {
                productIds = request.getParameterValues(LEGACY_PRODUCT_PARAM);
            }
            if (productIds == null) {
                return commands;
            }
            for (int i = 0; i < productIds.length; i++) {
                Integer productId = integerAt(productIds, i);
                BigDecimal quantity = decimalAt(request.getParameterValues(QUANTITY_PARAM), i);
                if (productId == null && quantity == null) {
                    continue;
                }
                if (productId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(new FormSubmissionError(ERROR_ID, "Each prescription line must have a drug and a positive quantity."));
                    continue;
                }
                PrescriptionLineCommand command = new PrescriptionLineCommand();
                if (byItem) {
                    command.setItemId(productId);
                } else {
                    command.setProductMasterItemId(productId);
                }
                command.setDose(decimalAt(request.getParameterValues(DOSE_PARAM), i));
                command.setRoute(valueAt(request.getParameterValues(ROUTE_PARAM), i));
                command.setFrequency(valueAt(request.getParameterValues(FREQUENCY_PARAM), i));
                command.setDuration(integerAt(request.getParameterValues(DURATION_PARAM), i));
                command.setDurationUnit(valueAt(request.getParameterValues(DURATION_UNIT_PARAM), i));
                command.setQuantity(quantity);
                command.setInstructions(valueAt(request.getParameterValues(INSTRUCTIONS_PARAM), i));
                command.setBillable(!"false".equalsIgnoreCase(valueAt(request.getParameterValues(BILLABLE_PARAM), i)));
                command.setBillingExclusionReason(valueAt(request.getParameterValues(BILLING_REASON_PARAM), i));
                command.setStartDate(new Date());
                commands.add(command);
            }
            return commands;
        }

        private static Visit activeVisit(Patient patient) {
            List<Visit> activeVisits = patient == null ? null : Context.getVisitService().getActiveVisitsByPatient(patient);
            return activeVisits == null || activeVisits.isEmpty() ? null : activeVisits.get(0);
        }

        private static Provider orderingProvider() {
            if (Context.getAuthenticatedUser() != null && Context.getAuthenticatedUser().getPerson() != null) {
                Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(Context.getAuthenticatedUser().getPerson(), false);
                if (providers != null && !providers.isEmpty()) {
                    return providers.iterator().next();
                }
            }
            return Context.getProviderService().getUnknownProvider();
        }
    }

    private static String valueAt(String[] values, int index) {
        return values != null && values.length > index ? values[index] : null;
    }

    private static Integer integerAt(String[] values, int index) {
        String value = valueAt(values, index);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal decimalAt(String[] values, int index) {
        String value = valueAt(values, index);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
