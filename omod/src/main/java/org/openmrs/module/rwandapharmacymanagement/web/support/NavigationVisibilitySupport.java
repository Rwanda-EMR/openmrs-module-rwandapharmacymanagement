package org.openmrs.module.rwandapharmacymanagement.web.support;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NavigationVisibilitySupport {

    public static final String SCOPE_MAIN_ONLY = "MAIN_ONLY";
    public static final String SCOPE_DISPENSING_ONLY = "DISPENSING_ONLY";
    public static final String SCOPE_PHARMACY_ANY = "PHARMACY_ANY";

    private NavigationVisibilitySupport() {
    }

    public static boolean isVisible(String scope) {
        if (scope == null || scope.trim().length() == 0) {
            return true;
        }
        PharmacyService pharmacyService = Context.getService(PharmacyService.class);
        if (SCOPE_MAIN_ONLY.equalsIgnoreCase(scope)) {
            return isCurrentMainPharmacy(pharmacyService);
        }
        if (SCOPE_DISPENSING_ONLY.equalsIgnoreCase(scope)) {
            return hasCurrentDispensingPharmacy(pharmacyService);
        }
        if (SCOPE_PHARMACY_ANY.equalsIgnoreCase(scope)) {
            return isCurrentMainPharmacy(pharmacyService) || hasCurrentDispensingPharmacy(pharmacyService);
        }
        return true;
    }

    public static boolean isCurrentMainPharmacy(PharmacyService pharmacyService) {
        if (pharmacyService == null) {
            return false;
        }
        return matchesCurrentOpenmrsLocation(pharmacyService.getMainStore());
    }

    public static boolean hasCurrentDispensingPharmacy(PharmacyService pharmacyService) {
        if (pharmacyService == null) {
            return false;
        }
        List<PharmacyLocation> candidates = CurrentPharmacySupport.getCandidatePharmacies(pharmacyService);
        return candidates != null && !candidates.isEmpty();
    }

    public static boolean matchesCurrentOpenmrsLocation(PharmacyLocation pharmacyLocation) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        if (pharmacyLocation == null || currentLocation == null || currentLocation.getLocationId() == null) {
            return false;
        }
        if (pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null
                && pharmacyLocation.getLocation().getLocationId().equals(currentLocation.getLocationId())) {
            return true;
        }
        if (currentLocation.getTags() == null || currentLocation.getTags().isEmpty()) {
            return false;
        }
        Set<Integer> currentTagIds = new HashSet<Integer>();
        for (LocationTag tag : currentLocation.getTags()) {
            if (tag != null && tag.getLocationTagId() != null) {
                currentTagIds.add(tag.getLocationTagId());
            }
        }
        if (pharmacyLocation.getLocationTag() != null && pharmacyLocation.getLocationTag().getLocationTagId() != null
                && currentTagIds.contains(pharmacyLocation.getLocationTag().getLocationTagId())) {
            return true;
        }
        if (pharmacyLocation.getLocationTags() != null) {
            for (LocationTag tag : pharmacyLocation.getLocationTags()) {
                if (tag != null && tag.getLocationTagId() != null && currentTagIds.contains(tag.getLocationTagId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
