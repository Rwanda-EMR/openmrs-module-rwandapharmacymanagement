package org.openmrs.module.rwandapharmacymanagement.web.support;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.ui.framework.page.PageModel;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CurrentPharmacySupport {

    private static final String SESSION_KEY_PREFIX = "rpm.selectedPharmacyForLocation.";

    private CurrentPharmacySupport() {
    }

    public static void populateModel(PageModel model, PharmacyService pharmacyService, HttpSession session) {
        List<PharmacyLocation> candidates = getCandidatePharmacies(pharmacyService);
        PharmacyLocation activePharmacy = resolveCurrentPharmacy(pharmacyService, session);
        model.addAttribute("activePharmacy", activePharmacy);
        model.addAttribute("pharmacySelectionOptions", candidates);
        model.addAttribute("pharmacySelectionRequired", activePharmacy == null && candidates.size() > 1);
        model.addAttribute("pharmacySelectionLocationName", currentLocationName());
    }

    public static void rememberSelection(PharmacyService pharmacyService, HttpSession session, Integer pharmacyLocationId) {
        if (pharmacyLocationId == null || session == null) {
            return;
        }
        Integer currentLocationId = currentLocationId();
        if (currentLocationId == null) {
            return;
        }
        for (PharmacyLocation pharmacyLocation : getCandidatePharmacies(pharmacyService)) {
            if (pharmacyLocation != null && pharmacyLocation.getPharmacyLocationId() != null
                    && pharmacyLocation.getPharmacyLocationId().equals(pharmacyLocationId)) {
                session.setAttribute(selectionSessionKey(currentLocationId), pharmacyLocationId);
                return;
            }
        }
        throw new IllegalArgumentException("Selected pharmacy does not belong to your current OpenMRS location");
    }

    public static PharmacyLocation resolveCurrentPharmacy(PharmacyService pharmacyService, HttpSession session) {
        List<PharmacyLocation> candidates = getCandidatePharmacies(pharmacyService);
        Integer currentLocationId = currentLocationId();
        if (currentLocationId == null || candidates.isEmpty()) {
            clearSelection(session, currentLocationId);
            return null;
        }
        if (candidates.size() == 1) {
            PharmacyLocation pharmacyLocation = candidates.get(0);
            if (session != null) {
                session.setAttribute(selectionSessionKey(currentLocationId), pharmacyLocation.getPharmacyLocationId());
            }
            return pharmacyLocation;
        }
        Integer selectedId = session == null ? null : (Integer) session.getAttribute(selectionSessionKey(currentLocationId));
        if (selectedId == null) {
            return null;
        }
        for (PharmacyLocation pharmacyLocation : candidates) {
            if (pharmacyLocation != null && selectedId.equals(pharmacyLocation.getPharmacyLocationId())) {
                return pharmacyLocation;
            }
        }
        clearSelection(session, currentLocationId);
        return null;
    }

    public static PharmacyLocation requireCurrentPharmacy(PharmacyService pharmacyService, HttpSession session) {
        PharmacyLocation activePharmacy = resolveCurrentPharmacy(pharmacyService, session);
        if (activePharmacy != null) {
            return activePharmacy;
        }
        List<PharmacyLocation> candidates = getCandidatePharmacies(pharmacyService);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No pharmacy is configured for your current OpenMRS location");
        }
        throw new IllegalArgumentException("Multiple pharmacies are configured for your current OpenMRS location. Choose one before continuing");
    }

    public static List<PharmacyLocation> getCandidatePharmacies(PharmacyService pharmacyService) {
        Integer locationId = currentLocationId();
        if (locationId == null) {
            return Collections.emptyList();
        }
        return getCandidatePharmaciesForLocation(pharmacyService, Context.getLocationService().getLocation(locationId));
    }

    public static List<PharmacyLocation> getCandidatePharmaciesForLocation(PharmacyService pharmacyService, Location location) {
        if (location == null) {
            return Collections.emptyList();
        }
        Set<Integer> tagIds = new HashSet<Integer>();
        if (location.getTags() != null) {
            for (LocationTag locationTag : location.getTags()) {
                if (locationTag != null && locationTag.getLocationTagId() != null) {
                    tagIds.add(locationTag.getLocationTagId());
                }
            }
        }
        List<PharmacyLocation> candidates = new ArrayList<PharmacyLocation>();
        for (PharmacyLocation pharmacyLocation : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacyLocation == null || !Boolean.TRUE.equals(pharmacyLocation.getActive())
                    || Boolean.TRUE.equals(pharmacyLocation.getMainStore())) {
                continue;
            }
            boolean matchesTag = pharmacyLocation.getLocationTag() != null
                    && pharmacyLocation.getLocationTag().getLocationTagId() != null
                    && tagIds.contains(pharmacyLocation.getLocationTag().getLocationTagId());
            if (!matchesTag && pharmacyLocation.getLocationTags() != null) {
                for (LocationTag pharmacyTag : pharmacyLocation.getLocationTags()) {
                    if (pharmacyTag != null && pharmacyTag.getLocationTagId() != null
                            && tagIds.contains(pharmacyTag.getLocationTagId())) {
                        matchesTag = true;
                        break;
                    }
                }
            }
            boolean matchesLocation = pharmacyLocation.getLocation() != null
                    && pharmacyLocation.getLocation().getLocationId() != null
                    && pharmacyLocation.getLocation().getLocationId().equals(location.getLocationId());
            if (matchesTag || matchesLocation) {
                candidates.add(pharmacyLocation);
            }
        }
        Collections.sort(candidates, new Comparator<PharmacyLocation>() {
            @Override
            public int compare(PharmacyLocation left, PharmacyLocation right) {
                String leftName = left == null || left.getName() == null ? "" : left.getName();
                String rightName = right == null || right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return candidates;
    }

    public static String currentLocationName() {
        UserContext userContext = Context.getUserContext();
        return userContext == null || userContext.getLocation() == null ? "" : userContext.getLocation().getName();
    }

    private static Integer currentLocationId() {
        UserContext userContext = Context.getUserContext();
        return userContext == null ? null : userContext.getLocationId();
    }

    private static String selectionSessionKey(Integer locationId) {
        return SESSION_KEY_PREFIX + (locationId == null ? "unknown" : locationId);
    }

    private static void clearSelection(HttpSession session, Integer locationId) {
        if (session != null && locationId != null) {
            session.removeAttribute(selectionSessionKey(locationId));
        }
    }
}
