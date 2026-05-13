package org.openmrs.module.rwandapharmacymanagement.web.support;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PatientRequestVisibilitySupport {

    private PatientRequestVisibilitySupport() {
    }

    public static List<PatientDispensingRequest> filterForCurrentLocation(List<PatientDispensingRequest> requests,
                                                                          List<PharmacyLocation> candidatePharmacies) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        Integer currentLocationId = Context.getUserContext() == null ? null : Context.getUserContext().getLocationId();
        List<PatientDispensingRequest> visible = new ArrayList<PatientDispensingRequest>();
        for (PatientDispensingRequest request : requests) {
            if (isVisibleToLocation(request, candidatePharmacies, currentLocationId)) {
                visible.add(request);
            }
        }
        return visible;
    }

    public static List<PatientDispensingRequest> filterForActivePharmacy(List<PatientDispensingRequest> requests,
                                                                         PharmacyLocation activePharmacy) {
        if (requests == null || requests.isEmpty() || activePharmacy == null) {
            return Collections.emptyList();
        }
        Integer currentLocationId = Context.getUserContext() == null ? null : Context.getUserContext().getLocationId();
        List<PatientDispensingRequest> visible = new ArrayList<PatientDispensingRequest>();
        for (PatientDispensingRequest request : requests) {
            if (isVisibleToPharmacy(request, activePharmacy, currentLocationId)) {
                visible.add(request);
            }
        }
        return visible;
    }

    public static boolean isVisibleToPharmacy(PatientDispensingRequest request,
                                              PharmacyLocation activePharmacy,
                                              Integer currentLocationId) {
        if (request == null || activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return false;
        }
        if (request.getTargetPharmacyLocation() != null
                && activePharmacy.getPharmacyLocationId().equals(request.getTargetPharmacyLocation().getPharmacyLocationId())) {
            return true;
        }
        if (request.getLines() != null) {
            for (PatientDispensingRequestLine line : request.getLines()) {
                if (line != null && line.getRoutedPharmacyLocation() != null
                        && activePharmacy.getPharmacyLocationId().equals(line.getRoutedPharmacyLocation().getPharmacyLocationId())) {
                    return true;
                }
            }
        }
        return request.getTargetPharmacyLocation() == null
                && isVisibleThroughRequestingLocation(request, activePharmacy, currentLocationId);
    }

    public static String describeTarget(PatientDispensingRequest request) {
        if (request == null) {
            return "";
        }
        if (request.getTargetPharmacyLocation() != null && request.getTargetPharmacyLocation().getName() != null) {
            return request.getTargetPharmacyLocation().getName();
        }
        if (request.getRequestingLocation() != null && request.getRequestingLocation().getName() != null) {
            return "Connected dispensing pharmacies for " + request.getRequestingLocation().getName();
        }
        if (request.getEncounter() != null && request.getEncounter().getLocation() != null
                && request.getEncounter().getLocation().getName() != null) {
            return "All dispensing pharmacies at " + request.getEncounter().getLocation().getName();
        }
        return "";
    }

    private static boolean isVisibleToLocation(PatientDispensingRequest request,
                                               List<PharmacyLocation> candidatePharmacies,
                                               Integer currentLocationId) {
        if (request == null) {
            return false;
        }
        if (candidatePharmacies != null) {
            for (PharmacyLocation pharmacyLocation : candidatePharmacies) {
                if (isVisibleToPharmacy(request, pharmacyLocation, currentLocationId)) {
                    return true;
                }
            }
        }
        return request.getTargetPharmacyLocation() == null
                && isVisibleThroughRequestingLocation(request, null, currentLocationId);
    }

    private static boolean isVisibleThroughRequestingLocation(PatientDispensingRequest request,
                                                              PharmacyLocation activePharmacy,
                                                              Integer currentLocationId) {
        if (request == null) {
            return false;
        }
        if (request.getRequestingLocation() == null && request.getEncounter() != null) {
            request.setRequestingLocation(request.getEncounter().getLocation());
        }
        if (request.getRequestingLocation() == null) {
            return false;
        }
        PharmacyService pharmacyService = Context.getService(PharmacyService.class);
        List<PharmacyLocation> candidatePharmacies = CurrentPharmacySupport.getCandidatePharmaciesForLocation(pharmacyService,
                request.getRequestingLocation());
        if (activePharmacy != null && activePharmacy.getPharmacyLocationId() != null) {
            for (PharmacyLocation pharmacyLocation : candidatePharmacies) {
                if (pharmacyLocation != null && pharmacyLocation.getPharmacyLocationId() != null
                        && activePharmacy.getPharmacyLocationId().equals(pharmacyLocation.getPharmacyLocationId())) {
                    return true;
                }
            }
            return false;
        }
        if (candidatePharmacies.isEmpty()) {
            return false;
        }
        if (currentLocationId == null) {
            return true;
        }
        for (PharmacyLocation pharmacyLocation : candidatePharmacies) {
            if (pharmacyLocation != null && pharmacyLocation.getLocation() != null
                    && pharmacyLocation.getLocation().getLocationId() != null
                    && currentLocationId.equals(pharmacyLocation.getLocation().getLocationId())) {
                return true;
            }
        }
        return true;
    }
}
