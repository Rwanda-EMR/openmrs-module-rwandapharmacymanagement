package org.openmrs.module.rwandapharmacymanagement.page.controller.queue;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.PatientRequestVisibilitySupport;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class PatientQueuePageController {
    private static final int QUEUE_PAGE_SIZE = 100;

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "refreshPayments", required = false) Boolean refreshPayments,
                    @RequestParam(value = "refreshPaymentRequestId", required = false) Integer refreshPaymentRequestId,
                    @RequestParam(value = "q", required = false) String search,
                    @RequestParam(value = "tab", required = false) String tab,
                    @RequestParam(value = "page", required = false) Integer page,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        int currentPage = page == null || page.intValue() < 1 ? 1 : page.intValue();
        int firstResult = (currentPage - 1) * QUEUE_PAGE_SIZE;
        String searchTerm = normalizeSearch(search);
        String queueTab = normalizeTab(tab);
        long totalRequests = activePharmacy == null ? 0L
                : dispensingService.countPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId(), searchTerm,
                queueTab, "all", "all");
        List<PatientDispensingRequest> requests = activePharmacy == null ? java.util.Collections.<PatientDispensingRequest>emptyList()
                : PatientRequestVisibilitySupport.filterForActivePharmacy(
                        dispensingService.getPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId(), searchTerm,
                                firstResult, QUEUE_PAGE_SIZE, queueTab, "all", "all"), activePharmacy);
        if (Boolean.TRUE.equals(refreshPayments)) {
            dispensingService.refreshPendingRequestPayments(requestIds(requests));
            totalRequests = activePharmacy == null ? 0L
                    : dispensingService.countPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId(), searchTerm,
                    queueTab, "all", "all");
            requests = activePharmacy == null ? java.util.Collections.<PatientDispensingRequest>emptyList()
                    : PatientRequestVisibilitySupport.filterForActivePharmacy(
                            dispensingService.getPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId(), searchTerm,
                                    firstResult, QUEUE_PAGE_SIZE, queueTab, "all", "all"), activePharmacy);
        } else if (refreshPaymentRequestId != null && requestIds(requests).contains(refreshPaymentRequestId)) {
            dispensingService.refreshPendingRequestPayment(refreshPaymentRequestId);
            totalRequests = activePharmacy == null ? 0L
                    : dispensingService.countPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId(), searchTerm,
                    queueTab, "all", "all");
            requests = activePharmacy == null ? java.util.Collections.<PatientDispensingRequest>emptyList()
                    : PatientRequestVisibilitySupport.filterForActivePharmacy(
                            dispensingService.getPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId(), searchTerm,
                                    firstResult, QUEUE_PAGE_SIZE, queueTab, "all", "all"), activePharmacy);
        }
        model.addAttribute("queueTargetPharmacies", CurrentPharmacySupport.getCandidatePharmacies(pharmacyService));
        model.addAttribute("queueLocationName", CurrentPharmacySupport.currentLocationName());
        model.addAttribute("requests", requests);
        model.addAttribute("requestTargetDescriptionById", describeTargets(requests));
        model.addAttribute("patientIdentifierByRequestId", buildPatientIdentifierMap(requests));
        model.addAttribute("waitTimeByRequestId", buildWaitTimeMap(requests));
        model.addAttribute("drugCountByRequestId", buildDrugCountMap(requests));
        model.addAttribute("paymentStatusByRequestId", buildPaymentStatusMap(requests));
        model.addAttribute("paymentExceptionByRequestId", buildPaymentExceptionMap(requests));
        model.addAttribute("paymentRefreshRequested", Boolean.TRUE.equals(refreshPayments));
        model.addAttribute("refreshedPaymentRequestId", refreshPaymentRequestId);
        model.addAttribute("queueSearch", searchTerm);
        model.addAttribute("queueTab", queueTab);
        model.addAttribute("queuePage", currentPage);
        model.addAttribute("queuePageSize", QUEUE_PAGE_SIZE);
        model.addAttribute("queueTotal", totalRequests);
        model.addAttribute("queueFirstResult", totalRequests == 0L ? 0L : firstResult + 1L);
        model.addAttribute("queueLastResult", Math.min(totalRequests, firstResult + requests.size()));
        model.addAttribute("queueHasPreviousPage", currentPage > 1);
        model.addAttribute("queueHasNextPage", firstResult + QUEUE_PAGE_SIZE < totalRequests);
    }

    private List<Integer> requestIds(List<PatientDispensingRequest> requests) {
        List<Integer> requestIds = new ArrayList<Integer>();
        if (requests == null) {
            return requestIds;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                requestIds.add(request.getPatientDispensingRequestId());
            }
        }
        return requestIds;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return "";
        }
        return search.trim();
    }

    private String normalizeTab(String tab) {
        String normalized = tab == null ? "" : tab.trim().toLowerCase();
        if ("ready".equals(normalized) || "waiting".equals(normalized)
                || "exception".equals(normalized) || "older".equals(normalized)
                || "search".equals(normalized)) {
            return normalized;
        }
        return "recent";
    }

    public String post(@SpringBean("dispensingService") DispensingService dispensingService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam("patientId") Integer patientId,
                       @RequestParam("targetPharmacyLocationId") Integer targetPharmacyLocationId,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "paymentConfirmed", required = false) Boolean paymentConfirmed,
                       @RequestParam(value = "medicalActReference", required = false) String medicalActReference,
                       HttpSession session,
                       UiUtils ui) {
        List<PharmacyLocation> candidatePharmacies = CurrentPharmacySupport.getCandidatePharmacies(pharmacyService);
        PharmacyLocation activePharmacy = resolveSelectedQueuePharmacy(pharmacyService, candidatePharmacies, targetPharmacyLocationId, session);
        if (activePharmacy == null) {
            throw new IllegalArgumentException("Choose a target pharmacy for the request");
        }
        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient not found");
        }
        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setPatient(patient);
        request.setTargetPharmacyLocation(activePharmacy);
        request.setPaymentConfirmed(Boolean.TRUE.equals(paymentConfirmed));
        request.setMedicalActReference(medicalActReference);
        request.setRequestStatus(Boolean.TRUE.equals(paymentConfirmed) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        request.setLines(buildRequestLines(request, pharmacyService, activePharmacy.getPharmacyLocationId(), itemIds, quantities, Boolean.TRUE.equals(paymentConfirmed)));
        dispensingService.saveRequest(request);
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "queue/patientQueue");
    }

    private PharmacyLocation resolveSelectedQueuePharmacy(PharmacyService pharmacyService,
                                                          List<PharmacyLocation> candidatePharmacies,
                                                          Integer requestedPharmacyLocationId,
                                                          HttpSession session) {
        if (candidatePharmacies == null || candidatePharmacies.isEmpty()) {
            return null;
        }
        if (requestedPharmacyLocationId != null) {
            for (PharmacyLocation pharmacyLocation : candidatePharmacies) {
                if (pharmacyLocation != null && requestedPharmacyLocationId.equals(pharmacyLocation.getPharmacyLocationId())) {
                    if (session != null) {
                        session.setAttribute("rpm.queueTargetPharmacy", requestedPharmacyLocationId);
                    }
                    return pharmacyLocation;
                }
            }
            throw new IllegalArgumentException("Selected target pharmacy does not belong to your current OpenMRS location");
        }
        if (candidatePharmacies.size() == 1) {
            return candidatePharmacies.get(0);
        }
        Integer storedPharmacyLocationId = session == null ? null : (Integer) session.getAttribute("rpm.queueTargetPharmacy");
        if (storedPharmacyLocationId != null) {
            for (PharmacyLocation pharmacyLocation : candidatePharmacies) {
                if (pharmacyLocation != null && storedPharmacyLocationId.equals(pharmacyLocation.getPharmacyLocationId())) {
                    return pharmacyLocation;
                }
            }
        }
        return null;
    }

    private java.util.Map<Integer, String> describeTargets(List<PatientDispensingRequest> requests) {
        java.util.Map<Integer, String> descriptions = new java.util.LinkedHashMap<Integer, String>();
        if (requests == null) {
            return descriptions;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                descriptions.put(request.getPatientDispensingRequestId(), PatientRequestVisibilitySupport.describeTarget(request));
            }
        }
        return descriptions;
    }

    private Map<Integer, String> buildPatientIdentifierMap(List<PatientDispensingRequest> requests) {
        Map<Integer, String> identifiers = new LinkedHashMap<Integer, String>();
        if (requests == null) {
            return identifiers;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                identifiers.put(request.getPatientDispensingRequestId(), patientIdentifier(request.getPatient()));
            }
        }
        return identifiers;
    }

    private Map<Integer, String> buildWaitTimeMap(List<PatientDispensingRequest> requests) {
        Map<Integer, String> waitTimes = new LinkedHashMap<Integer, String>();
        if (requests == null) {
            return waitTimes;
        }
        Date now = new Date();
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                waitTimes.put(request.getPatientDispensingRequestId(), formatWaitTime(request.getDateCreated(), now));
            }
        }
        return waitTimes;
    }

    private Map<Integer, Integer> buildDrugCountMap(List<PatientDispensingRequest> requests) {
        Map<Integer, Integer> drugCounts = new LinkedHashMap<Integer, Integer>();
        if (requests == null) {
            return drugCounts;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                drugCounts.put(request.getPatientDispensingRequestId(),
                        request.getLines() == null ? Integer.valueOf(0) : Integer.valueOf(request.getLines().size()));
            }
        }
        return drugCounts;
    }

    private Map<Integer, Boolean> buildPaymentStatusMap(List<PatientDispensingRequest> requests) {
        Map<Integer, Boolean> statuses = new LinkedHashMap<Integer, Boolean>();
        if (requests == null) {
            return statuses;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                statuses.put(request.getPatientDispensingRequestId(), Boolean.TRUE.equals(request.getPaymentConfirmed()));
            }
        }
        return statuses;
    }

    private Map<Integer, Boolean> buildPaymentExceptionMap(List<PatientDispensingRequest> requests) {
        Map<Integer, Boolean> statuses = new LinkedHashMap<Integer, Boolean>();
        if (requests == null) {
            return statuses;
        }
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                statuses.put(request.getPatientDispensingRequestId(), Boolean.TRUE.equals(request.getPaymentExceptionApproved()));
            }
        }
        return statuses;
    }

    private String patientIdentifier(Patient patient) {
        if (patient == null) {
            return "";
        }
        if (patient.getPatientIdentifier() != null) {
            return patient.getPatientIdentifier().toString();
        }
        return patient.getPatientId() == null ? "" : patient.getPatientId().toString();
    }

    private String formatWaitTime(Date created, Date now) {
        if (created == null || now == null || now.before(created)) {
            return "0 min";
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - created.getTime());
        if (minutes < 60L) {
            return minutes + " min";
        }
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        return remainingMinutes == 0L ? hours + " h" : hours + " h " + remainingMinutes + " min";
    }

    private List<PatientDispensingRequestLine> buildRequestLines(PatientDispensingRequest request,
                                                                 PharmacyService pharmacyService,
                                                                 Integer pharmacyLocationId,
                                                                 Integer[] itemIds,
                                                                 BigDecimal[] quantities,
                                                                 boolean paymentConfirmed) {
        List<PatientDispensingRequestLine> lines = new ArrayList<PatientDispensingRequestLine>();
        if (itemIds == null || quantities == null) {
            return lines;
        }
        for (int i = 0; i < itemIds.length; i++) {
            Integer itemId = itemIds[i];
            BigDecimal quantity = quantities.length > i ? quantities[i] : null;
            if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PatientDispensingRequestLine line = new PatientDispensingRequestLine();
            line.setPatientDispensingRequest(request);
            line.setItem(pharmacyService.getItem(itemId));
            line.setRequestedQuantity(quantity);
            line.setRoutedPharmacyLocation(pharmacyService.getPharmacyLocation(pharmacyLocationId));
            line.setLineStatus(paymentConfirmed ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
            lines.add(line);
        }
        return lines;
    }

}
