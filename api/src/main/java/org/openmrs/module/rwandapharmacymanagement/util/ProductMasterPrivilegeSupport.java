package org.openmrs.module.rwandapharmacymanagement.util;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;

public final class ProductMasterPrivilegeSupport {

    private ProductMasterPrivilegeSupport() {
    }

    public static boolean isAllowed(ProductMasterItem item) {
        String privilege = requiredPrivilege(item);
        return privilege.isEmpty() || Context.hasPrivilege(privilege);
    }

    public static void requireAllowed(ProductMasterItem item) {
        if (isAllowed(item)) {
            return;
        }
        String reason = safe(item == null ? null : item.getRestrictionReason());
        String message = reason.isEmpty()
                ? "You do not have the required privilege to order this product master item"
                : reason;
        throw new IllegalArgumentException(message);
    }

    public static String requiredPrivilege(ProductMasterItem item) {
        return safe(item == null ? null : item.getRequiredPrivilege());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
