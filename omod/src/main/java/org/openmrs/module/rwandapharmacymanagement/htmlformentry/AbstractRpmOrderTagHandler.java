package org.openmrs.module.rwandapharmacymanagement.htmlformentry;

import org.apache.commons.lang.StringEscapeUtils;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.util.ProductMasterPrivilegeSupport;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class AbstractRpmOrderTagHandler extends SubstitutionTagHandler {

    protected static final String MATERIAL_TYPE_DRUG = "ZMED";

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controller,
                                     Map<String, String> parameters) throws BadFormDesignException {
        registerAction(controller, parameters);
        return render(session, parameters);
    }

    protected abstract void registerAction(FormSubmissionController controller, Map<String, String> parameters);

    protected abstract String render(FormEntrySession session, Map<String, String> parameters) throws BadFormDesignException;

    protected Patient getPatient(FormEntrySession session) {
        return session == null ? null : session.getPatient();
    }

    protected List<ProductMasterItem> getProductMasterItems() {
        PharmacyService pharmacyService = Context.getRegisteredComponent("pharmacyService", PharmacyService.class);
        List<ProductMasterItem> items = pharmacyService.getAllProductMasterItems();
        if (items == null) {
            return Collections.emptyList();
        }
        Collections.sort(items, new Comparator<ProductMasterItem>() {
            @Override
            public int compare(ProductMasterItem left, ProductMasterItem right) {
                return safe(left == null ? null : left.getItemName())
                        .compareToIgnoreCase(safe(right == null ? null : right.getItemName()));
            }
        });
        return items;
    }

    protected boolean isActive(ProductMasterItem item) {
        return item != null && !Boolean.FALSE.equals(item.getActive());
    }

    protected boolean isDrug(ProductMasterItem item) {
        return isActive(item) && MATERIAL_TYPE_DRUG.equalsIgnoreCase(safe(item.getMaterialType())) && item.getDrug() != null
                && ProductMasterPrivilegeSupport.isAllowed(item);
    }

    protected boolean isConsumable(ProductMasterItem item) {
        return isActive(item) && !MATERIAL_TYPE_DRUG.equalsIgnoreCase(safe(item.getMaterialType()))
                && ProductMasterPrivilegeSupport.isAllowed(item);
    }

    protected String attr(Map<String, String> parameters, String name, String defaultValue) {
        String value = parameters == null ? null : parameters.get(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    protected boolean booleanAttr(Map<String, String> parameters, String name, boolean defaultValue) {
        String value = attr(parameters, name, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    protected Integer integerAttr(Map<String, String> parameters, String name) {
        String value = attr(parameters, name, null);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    protected String h(String value) {
        return StringEscapeUtils.escapeHtml(safe(value));
    }

    protected String js(String value) {
        return StringEscapeUtils.escapeJavaScript(safe(value));
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }

    protected String sharedStyles() {
        return "<style type=\"text/css\">"
                + ".rpm-hfe-order{border:1px solid #d7dee8;border-radius:4px;margin:12px 0;background:#fff;overflow:hidden}"
                + ".rpm-hfe-head{background:#1f5f99;color:#fff;padding:8px 12px;font-weight:600}"
                + ".rpm-hfe-body{padding:12px}.rpm-hfe-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}"
                + ".rpm-hfe-field label{display:block;font-size:12px;font-weight:600;margin-bottom:4px;color:#334155}"
                + ".rpm-hfe-field input,.rpm-hfe-field select{width:100%;box-sizing:border-box;border:1px solid #cbd5e1;border-radius:4px;padding:7px 8px}"
                + ".rpm-hfe-table{width:100%;border-collapse:collapse;margin-top:10px}.rpm-hfe-table th,.rpm-hfe-table td{border:1px solid #dbe5f0;padding:7px;font-size:12px;text-align:left}"
                + ".rpm-hfe-table th{background:#eef5fb;color:#475569}.rpm-hfe-btn{border:1px solid #15803d;background:#16a34a;color:#fff;border-radius:4px;padding:7px 12px;cursor:pointer}"
                + ".rpm-hfe-remove{border:0;background:#fff1f2;color:#be123c;border-radius:3px;padding:3px 7px;cursor:pointer}.rpm-hfe-note{font-size:12px;color:#64748b;margin-top:6px}"
                + "@media(max-width:900px){.rpm-hfe-grid{grid-template-columns:1fr}}"
                + "</style>";
    }
}
