package org.openmrs.module.rwandapharmacymanagement.web.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.AlertService;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.LabSupplyService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QueueNotificationSupport {

    private static final Log log = LogFactory.getLog(QueueNotificationSupport.class);
    private static final String SESSION_KEY_PREFIX = "rpm.selectedPharmacyForLocation.";

    private QueueNotificationSupport() {
    }

    public static Map<String, Integer> counts(HttpSession session) {
        return counts((Object) session);
    }

    public static Map<String, Integer> counts(Object session) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        PharmacyService pharmacyService = null;
        PharmacyLocation activePharmacy = null;
        List<PharmacyLocation> candidatePharmacies = null;
        try {
            pharmacyService = Context.getRegisteredComponent("pharmacyService", PharmacyService.class);
            candidatePharmacies = CurrentPharmacySupport.getCandidatePharmacies(pharmacyService);
            activePharmacy = resolveActivePharmacy(pharmacyService, session);
        }
        catch (Exception ignored) {
            log.warn("Unable to resolve current pharmacy while building queue notification counts", ignored);
            pharmacyService = null;
            activePharmacy = null;
            candidatePharmacies = null;
        }
        try {
            counts.put("queue/patientQueue", patientQueueCount(activePharmacy, candidatePharmacies));
        }
        catch (Exception ignored) {
            log.warn("Unable to count patient queue notifications", ignored);
            counts.put("queue/patientQueue", 0);
        }
        try {
            counts.put("pharmacyorder/queue", pharmacyItemRequestQueueCount(pharmacyService, activePharmacy, candidatePharmacies));
        }
        catch (Exception ignored) {
            log.warn("Unable to count pharmacy item request queue notifications", ignored);
            counts.put("pharmacyorder/queue", 0);
        }
        try {
            counts.put("lab/queue", labQueueCount(pharmacyService, activePharmacy, candidatePharmacies));
        }
        catch (Exception ignored) {
            log.warn("Unable to count lab queue notifications", ignored);
            counts.put("lab/queue", 0);
        }
        try {
            counts.put("pharmacyorder/approve", pharmacyTransferApprovalCount(null));
        }
        catch (Exception ignored) {
            log.warn("Unable to count pharmacy transfer approval notifications", ignored);
            counts.put("pharmacyorder/approve", 0);
        }
        try {
            counts.put("purchaseorder/management", pendingPurchaseOrderCount(pharmacyService));
        }
        catch (Exception ignored) {
            log.warn("Unable to count purchase order notifications", ignored);
            counts.put("purchaseorder/management", 0);
        }
        try {
            counts.put("alerts/dashboard", alertCount());
        }
        catch (Exception ignored) {
            log.warn("Unable to count alert notifications", ignored);
            counts.put("alerts/dashboard", 0);
        }
        return counts;
    }

    public static int countFor(HttpSession session, String path) {
        Integer count = counts(session).get(path);
        return count == null ? 0 : count.intValue();
    }

    private static int patientQueueCount(PharmacyLocation activePharmacy, List<PharmacyLocation> candidatePharmacies) {
        if (activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return 0;
        }
        return safeInt(dispensingService().countPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId()));
    }

    private static PharmacyLocation resolveActivePharmacy(PharmacyService pharmacyService, Object session) {
        if (session instanceof HttpSession) {
            return CurrentPharmacySupport.resolveCurrentPharmacy(pharmacyService, (HttpSession) session);
        }
        List<PharmacyLocation> candidates = CurrentPharmacySupport.getCandidatePharmacies(pharmacyService);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        Integer selectedId = selectedPharmacyId(session);
        if (selectedId == null) {
            return null;
        }
        for (PharmacyLocation pharmacyLocation : candidates) {
            if (pharmacyLocation != null && selectedId.equals(pharmacyLocation.getPharmacyLocationId())) {
                return pharmacyLocation;
            }
        }
        return null;
    }

    private static Integer selectedPharmacyId(Object session) {
        if (session == null || Context.getUserContext() == null || Context.getUserContext().getLocationId() == null) {
            return null;
        }
        String key = SESSION_KEY_PREFIX + Context.getUserContext().getLocationId();
        try {
            Object value = session.getClass().getMethod("getAttribute", String.class, Class.class)
                    .invoke(session, key, Integer.class);
            return value instanceof Integer ? (Integer) value : null;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static int pharmacyItemRequestQueueCount(PharmacyService pharmacyService,
                                                     PharmacyLocation activePharmacy,
                                                     List<PharmacyLocation> candidatePharmacies) {
        PharmacyLocation requestPharmacy = activePharmacy == null ? matchingMainStore(pharmacyService) : activePharmacy;
        if (requestPharmacy == null) {
            return 0;
        }
        PharmacyItemRequestService requestService = pharmacyItemRequestService();
        return safeInt(requestService.countPendingRequestsForPharmacy(requestPharmacy));
    }

    private static int labQueueCount(PharmacyService pharmacyService,
                                     PharmacyLocation activePharmacy,
                                     List<PharmacyLocation> candidatePharmacies) {
        if (activePharmacy == null || pharmacyService == null) {
            return 0;
        }
        int count = 0;
        List<LabSupplyRequest> requests = labSupplyService().getPendingRequests();
        if (requests == null) {
            return 0;
        }
        for (LabSupplyRequest request : requests) {
            if (isLabRequestVisibleToPharmacy(request, activePharmacy, pharmacyService)) {
                count++;
            }
        }
        return count;
    }

    private static int pharmacyTransferApprovalCount(PharmacyLocation selectedSourcePharmacy) {
        PharmacyService pharmacyService = null;
        try {
            pharmacyService = Context.getRegisteredComponent("pharmacyService", PharmacyService.class);
        }
        catch (Exception ignored) {
            return 0;
        }
        if (matchingMainStore(pharmacyService) == null) {
            return 0;
        }
        Integer selectedSourceId = selectedSourcePharmacy == null ? null : selectedSourcePharmacy.getPharmacyLocationId();
        return safeInt(inventoryService().countTransfers("REQUESTED", selectedSourceId, null));
    }

    private static int pendingPurchaseOrderCount(PharmacyService pharmacyService) {
        if (!Context.hasPrivilege("Task: rwandapharmacymanagement.submitSupplierOrders")
                && !Context.hasPrivilege("Task: rwandapharmacymanagement.approvePurchaseOrders")) {
            return 0;
        }
        return safeInt(pharmacyService.countPurchaseOrdersByApprovalStatus("PENDING"));
    }

    private static int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static int alertCount() {
        AlertService alertService = alertService();
        int lowStock = alertService == null || alertService.getLowStockAlerts() == null ? 0 : alertService.getLowStockAlerts().size();
        int expiry = alertService == null || alertService.getExpiryAlerts() == null ? 0 : alertService.getExpiryAlerts().size();
        return lowStock + expiry;
    }

    private static DispensingService dispensingService() {
        return Context.getRegisteredComponent("dispensingService", DispensingService.class);
    }

    private static LabSupplyService labSupplyService() {
        return Context.getRegisteredComponent("labSupplyService", LabSupplyService.class);
    }

    private static PharmacyItemRequestService pharmacyItemRequestService() {
        return Context.getRegisteredComponent("pharmacyItemRequestService", PharmacyItemRequestService.class);
    }

    private static InventoryService inventoryService() {
        return Context.getRegisteredComponent("inventoryService", InventoryService.class);
    }

    private static AlertService alertService() {
        return Context.getRegisteredComponent("rwandaPharmacyAlertService", AlertService.class);
    }

    private static boolean isLabRequestVisibleToPharmacy(LabSupplyRequest request,
                                                        PharmacyLocation activePharmacy,
                                                        PharmacyService pharmacyService) {
        if (request == null || request.getLabUnit() == null || activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return false;
        }
        for (PharmacyLocation pharmacyLocation : CurrentPharmacySupport.getCandidatePharmaciesForLocation(pharmacyService, request.getLabUnit())) {
            if (pharmacyLocation != null && pharmacyLocation.getPharmacyLocationId() != null
                    && activePharmacy.getPharmacyLocationId().equals(pharmacyLocation.getPharmacyLocationId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLabRequestVisibleToAnyPharmacy(LabSupplyRequest request,
                                                            List<PharmacyLocation> candidatePharmacies,
                                                            PharmacyService pharmacyService) {
        if (candidatePharmacies == null || candidatePharmacies.isEmpty()) {
            return false;
        }
        for (PharmacyLocation pharmacyLocation : candidatePharmacies) {
            if (isLabRequestVisibleToPharmacy(request, pharmacyLocation, pharmacyService)) {
                return true;
            }
        }
        return false;
    }

    private static PharmacyLocation matchingMainStore(PharmacyService pharmacyService) {
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        if (mainStore == null || currentLocation == null) {
            return null;
        }
        if (mainStore.getLocation() != null && mainStore.getLocation().getLocationId() != null
                && mainStore.getLocation().getLocationId().equals(currentLocation.getLocationId())) {
            return mainStore;
        }
        if (mainStore.getLocationTag() != null && currentLocation.getTags() != null) {
            for (LocationTag tag : currentLocation.getTags()) {
                if (tag != null && tag.getLocationTagId() != null
                        && tag.getLocationTagId().equals(mainStore.getLocationTag().getLocationTagId())) {
                    return mainStore;
                }
            }
        }
        if (mainStore.getLocationTags() != null && currentLocation.getTags() != null) {
            for (LocationTag mainStoreTag : mainStore.getLocationTags()) {
                for (LocationTag currentTag : currentLocation.getTags()) {
                    if (mainStoreTag != null && currentTag != null && mainStoreTag.getLocationTagId() != null
                            && mainStoreTag.getLocationTagId().equals(currentTag.getLocationTagId())) {
                        return mainStore;
                    }
                }
            }
        }
        return null;
    }
}
