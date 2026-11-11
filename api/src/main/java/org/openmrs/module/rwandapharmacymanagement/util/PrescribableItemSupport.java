package org.openmrs.module.rwandapharmacymanagement.util;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescribableItemInfo;
import org.openmrs.module.rwandapharmacymanagement.model.Item;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Decides whether a local pharmacy item can be prescribed.
 * <ul>
 * <li>The item must be non-voided, dispensable and have an OpenMRS drug.</li>
 * <li>An item without a linked supplier product is prescribable.</li>
 * <li>A linked item must have a drug (ZMED) product row, and either an active row or stock left.</li>
 * <li>Privileges: the user needs every privilege required by the linked rows (the strictest).</li>
 * </ul>
 */
public final class PrescribableItemSupport {

    private PrescribableItemSupport() {
    }

    public static boolean isPrescribable(Item item, PrescribableItemInfo info, BigDecimal availableQuantity) {
        return unavailableReason(item, info, availableQuantity) == null;
    }

    public static boolean isAllowed(PrescribableItemInfo info) {
        return missingPrivilegeReason(info) == null;
    }

    public static void requirePrescribable(Item item, PrescribableItemInfo info, BigDecimal availableQuantity) {
        String reason = unavailableReason(item, info, availableQuantity);
        if (reason == null) {
            reason = missingPrivilegeReason(info);
        }
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
    }

    private static String unavailableReason(Item item, PrescribableItemInfo info, BigDecimal availableQuantity) {
        String name = item == null || item.getName() == null ? "" : " '" + item.getName().trim() + "'";
        if (item == null || Boolean.TRUE.equals(item.getVoided())) {
            return "Selected pharmacy item was not found";
        }
        if (!Boolean.TRUE.equals(item.getDispensable())) {
            return "Pharmacy item" + name + " is not dispensable";
        }
        if (item.getDrug() == null) {
            return "Pharmacy item" + name + " is not linked to an OpenMRS drug";
        }
        if (info == null || !info.isLinked()) {
            return null;
        }
        if (!info.isAnyDrugMaterial()) {
            return "Pharmacy item" + name + " is not a medicine in the supplier product master";
        }
        boolean hasStock = availableQuantity != null && availableQuantity.compareTo(BigDecimal.ZERO) > 0;
        if (!info.isAnyActive() && !hasStock) {
            return "Pharmacy item" + name + " is discontinued by the supplier and has no stock left";
        }
        return null;
    }

    private static String missingPrivilegeReason(PrescribableItemInfo info) {
        if (info == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : info.getRestrictionReasonsByPrivilege().entrySet()) {
            if (!Context.hasPrivilege(entry.getKey())) {
                String reason = entry.getValue() == null ? "" : entry.getValue().trim();
                return reason.isEmpty()
                        ? "You do not have the required privilege to order this product master item"
                        : reason;
            }
        }
        return null;
    }
}
