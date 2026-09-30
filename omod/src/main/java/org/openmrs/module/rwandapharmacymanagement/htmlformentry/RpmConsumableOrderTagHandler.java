package org.openmrs.module.rwandapharmacymanagement.htmlformentry;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.rwandapharmacymanagement.api.ConsumableOrderService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableBillingReadiness;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableOrderLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RpmConsumableOrderTagHandler extends AbstractRpmOrderTagHandler {

    private static final String ERROR_ID = "rpmConsumableOrder";
    private static final String PRODUCT_PARAM = "rpmConsumableProductMasterItemId";
    private static final String QUANTITY_PARAM = "rpmConsumableQuantity";
    private static final String UOM_PARAM = "rpmConsumableUnitOfMeasure";
    private static final String BILLABLE_PARAM = "rpmConsumableBillable";
    private static final String BILLING_REASON_PARAM = "rpmConsumableBillingExclusionReason";

    @Override
    protected void registerAction(FormSubmissionController controller, Map<String, String> parameters) {
        controller.addAction(new Action(booleanAttr(parameters, "required", false)));
    }

    @Override
    protected String render(FormEntrySession session, Map<String, String> parameters) throws BadFormDesignException {
        Patient patient = getPatient(session);
        String title = attr(parameters, "title", "Consumables order");
        String buttonLabel = attr(parameters, "buttonLabel", "Add consumable line");
        StringBuilder html = new StringBuilder();
        html.append(sharedStyles());
        html.append("<div class=\"rpm-hfe-order\" id=\"rpm-consumable-order\">");
        html.append("<div class=\"rpm-hfe-head\">").append(h(title)).append("</div><div class=\"rpm-hfe-body\">");
        if (patient == null) {
            html.append("<div class=\"rpm-hfe-note\">No patient is available for this form.</div></div></div>");
            return html.toString();
        }
        html.append("<div class=\"rpm-hfe-grid\">");
        html.append("<div class=\"rpm-hfe-field\"><label>Consumable</label><select id=\"rpm-cons-product\"><option value=\"\">Select consumable</option>");
        for (ProductMasterItem item : getProductMasterItems()) {
            if (isConsumable(item)) {
                html.append("<option value=\"").append(item.getProductMasterItemId()).append("\">")
                        .append(h(item.getItemName())).append("</option>");
            }
        }
        html.append("</select></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Unit of measure</label><input type=\"text\" id=\"rpm-cons-uom\" value=\"EA\"/></div>");
        html.append("<div class=\"rpm-hfe-field\"><label>Quantity</label><input type=\"number\" step=\"0.01\" id=\"rpm-cons-quantity\"/></div>");
        html.append("<div class=\"rpm-hfe-field\"><label><input type=\"checkbox\" id=\"rpm-cons-not-billable\" style=\"width:auto\"/> Do not bill</label><input type=\"text\" id=\"rpm-cons-billing-reason\" placeholder=\"Reason\"/></div>");
        html.append("</div>");
        html.append("<div class=\"rpm-hfe-note\">Lines are queued when the HTML form is saved.</div>");
        html.append("<button type=\"button\" class=\"rpm-hfe-btn\" onclick=\"rpmAddConsumableLine()\">").append(h(buttonLabel)).append("</button>");
        html.append("<table class=\"rpm-hfe-table\"><thead><tr><th>Consumable</th><th>UOM</th><th>Quantity</th><th>Billing</th><th></th></tr></thead><tbody id=\"rpm-cons-lines\"><tr id=\"rpm-cons-empty\"><td colspan=\"5\">No consumable lines added.</td></tr></tbody></table>");
        html.append("</div></div>");
        html.append("<script type=\"text/javascript\">")
                .append("function rpmConsVal(id){var e=document.getElementById(id);return e?e.value:'';}")
                .append("function rpmConsText(id){var e=document.getElementById(id);return e&&e.selectedIndex>=0?e.options[e.selectedIndex].text:'';}")
                .append("function rpmConsHidden(n,v){var i=document.createElement('input');i.type='hidden';i.name=n;i.value=v||'';return i;}")
                .append("function rpmConsCell(t){var td=document.createElement('td');td.appendChild(document.createTextNode(t||''));return td;}")
                .append("function rpmAddConsumableLine(){var pid=rpmConsVal('rpm-cons-product'),ptext=rpmConsText('rpm-cons-product'),qty=rpmConsVal('rpm-cons-quantity'),uom=rpmConsVal('rpm-cons-uom')||'EA';")
                .append("if(!pid||!qty){alert('Select a consumable and enter quantity.');return;}")
                .append("var tbody=document.getElementById('rpm-cons-lines'),empty=document.getElementById('rpm-cons-empty');if(empty){empty.parentNode.removeChild(empty);}")
                .append("var tr=document.createElement('tr');tr.appendChild(rpmConsCell(ptext));tr.appendChild(rpmConsCell(uom));tr.appendChild(rpmConsCell(qty));tr.appendChild(rpmConsCell(document.getElementById('rpm-cons-not-billable').checked?'Not billable':'Billable'));")
                .append("var td=document.createElement('td'),btn=document.createElement('button');btn.type='button';btn.className='rpm-hfe-remove';btn.appendChild(document.createTextNode('Remove'));btn.onclick=function(){tbody.removeChild(tr);};td.appendChild(btn);")
                .append("tr.appendChild(rpmConsHidden('").append(PRODUCT_PARAM).append("',pid));tr.appendChild(rpmConsHidden('").append(UOM_PARAM).append("',uom));tr.appendChild(rpmConsHidden('").append(QUANTITY_PARAM).append("',qty));tr.appendChild(rpmConsHidden('").append(BILLABLE_PARAM).append("',document.getElementById('rpm-cons-not-billable').checked?'false':'true'));tr.appendChild(rpmConsHidden('").append(BILLING_REASON_PARAM).append("',rpmConsVal('rpm-cons-billing-reason')));tr.appendChild(td);tbody.appendChild(tr);")
                .append("document.getElementById('rpm-cons-product').value='';document.getElementById('rpm-cons-quantity').value='';document.getElementById('rpm-cons-billing-reason').value='';document.getElementById('rpm-cons-not-billable').checked=false;}")
                .append("</script>");
        return html.toString();
    }

    private static class Action implements FormSubmissionControllerAction {

        private final boolean required;

        private Action(boolean required) {
            this.required = required;
        }

        @Override
        public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest request) {
            List<FormSubmissionError> errors = new ArrayList<FormSubmissionError>();
            List<ConsumableOrderLineCommand> lines = buildCommands(request, errors);
            if (lines.isEmpty()) {
                if (required) {
                    errors.add(new FormSubmissionError(ERROR_ID, "Add at least one consumable line."));
                }
                return errors;
            }
            Patient patient = context == null ? null : context.getExistingPatient();
            if (patient == null) {
                errors.add(new FormSubmissionError(ERROR_ID, "No patient was found for the consumables order."));
                return errors;
            }
            ConsumableBillingReadiness readiness = Context.getRegisteredComponent("consumableOrderService",
                    ConsumableOrderService.class).getBillingReadiness(patient);
            if (!readiness.isReady()) {
                errors.add(new FormSubmissionError(ERROR_ID, readiness.getMessage()));
            }
            return errors;
        }

        @Override
        public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
            List<ConsumableOrderLineCommand> lines = buildCommands(request, new ArrayList<FormSubmissionError>());
            if (lines.isEmpty()) {
                return;
            }
            Location location = session.getEncounter() != null && session.getEncounter().getLocation() != null
                    ? session.getEncounter().getLocation()
                    : Context.getUserContext().getLocation();
            Context.getRegisteredComponent("consumableOrderService", ConsumableOrderService.class)
                    .createConsumableOrders(session.getPatient(), location, lines);
        }

        private static List<ConsumableOrderLineCommand> buildCommands(HttpServletRequest request, List<FormSubmissionError> errors) {
            List<ConsumableOrderLineCommand> commands = new ArrayList<ConsumableOrderLineCommand>();
            String[] productIds = request.getParameterValues(PRODUCT_PARAM);
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
                    errors.add(new FormSubmissionError(ERROR_ID, "Each consumable line must have an item and a positive quantity."));
                    continue;
                }
                ConsumableOrderLineCommand command = new ConsumableOrderLineCommand();
                command.setProductMasterItemId(productId);
                command.setQuantity(quantity);
                command.setUnitOfMeasure(defaultValue(valueAt(request.getParameterValues(UOM_PARAM), i), "EA"));
                command.setBillable(!"false".equalsIgnoreCase(valueAt(request.getParameterValues(BILLABLE_PARAM), i)));
                command.setBillingExclusionReason(valueAt(request.getParameterValues(BILLING_REASON_PARAM), i));
                commands.add(command);
            }
            return commands;
        }
    }

    private static String valueAt(String[] values, int index) {
        return values != null && values.length > index ? values[index] : null;
    }

    private static String defaultValue(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
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
