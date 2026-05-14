package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.OrderContext;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PrescriptionService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescriptionLineCommand;
import org.openmrs.module.rwandapharmacymanagement.integration.PrescriptionCommunicationService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Transactional
public class PrescriptionServiceImpl extends BaseOpenmrsService implements PrescriptionService {

    private final Log log = LogFactory.getLog(getClass());
    private static final long[] BILL_LINK_RETRY_DELAYS_MILLIS = new long[] {
            1000L, 2000L, 4000L, 8000L, 15000L, 30000L
    };
    private static final ExecutorService BILL_LINK_RETRY_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "rpm-prescription-bill-linker");
            thread.setDaemon(true);
            return thread;
        }
    });
    private PharmacyService pharmacyService;
    private DispensingService dispensingService;
    private PrescriptionCommunicationService prescriptionCommunicationService;
    private PlatformTransactionManager transactionManager;

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setDispensingService(DispensingService dispensingService) {
        this.dispensingService = dispensingService;
    }

    public void setPrescriptionCommunicationService(PrescriptionCommunicationService prescriptionCommunicationService) {
        this.prescriptionCommunicationService = prescriptionCommunicationService;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public PatientDispensingRequest createPrescription(Patient patient,
                                                       Visit visit,
                                                       Provider orderingProvider,
                                                       Location encounterLocation,
                                                       List<PrescriptionLineCommand> lines,
                                                       Integer patientBillId) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        if (visit == null) {
            throw new IllegalArgumentException("An active visit is required before prescribing");
        }
        if (encounterLocation == null) {
            throw new IllegalArgumentException("Encounter location is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("At least one prescription line is required");
        }

        Date encounterDatetime = resolveEncounterDatetime(visit);
        Encounter encounter = buildEncounter(patient, visit, orderingProvider, encounterLocation, encounterDatetime);
        Encounter savedEncounter = Context.getEncounterService().saveEncounter(encounter);

        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setPatient(patient);
        request.setVisit(visit);
        request.setEncounter(savedEncounter);
        request.setRequestingLocation(encounterLocation);
        request.setOrderingProvider(orderingProvider);
        request.setBillable(Boolean.TRUE);
        request.setRequestType(RequestType.PRESCRIPTION);
        request.setMedicalActReference("Prescription encounter " + savedEncounter.getEncounterId());

        List<PatientDispensingRequestLine> requestLines = new ArrayList<PatientDispensingRequestLine>();
        Order firstSavedOrder = null;
        int sortWeight = 1;
        for (PrescriptionLineCommand line : lines) {
            if (line == null || line.getProductMasterItemId() == null) {
                continue;
            }
            ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(line.getProductMasterItemId());
            validateProductMasterDrug(productMasterItem);
            org.openmrs.module.rwandapharmacymanagement.model.Item localItem = ensureLocalItem(productMasterItem);
            BigDecimal requestedQuantity = normalizeQuantity(line.getQuantity());
            DrugOrder order = buildDrugOrder(patient, savedEncounter, orderingProvider, productMasterItem.getDrug(), line,
                    requestedQuantity, sortWeight++);
            Order savedOrder = Context.getOrderService().saveOrder(order, (OrderContext) null);
            savedEncounter.addOrder(savedOrder);
            if (firstSavedOrder == null) {
                firstSavedOrder = savedOrder;
            }
            PatientDispensingRequestLine requestLine = new PatientDispensingRequestLine();
            requestLine.setPatientDispensingRequest(request);
            requestLine.setOrder(order);
            requestLine.setItem(localItem);
            requestLine.setRequestedQuantity(requestedQuantity);
            requestLine.setLineStatus(RequestStatus.PENDING_PAYMENT);
            requestLines.add(requestLine);
        }

        if (requestLines.isEmpty()) {
            throw new IllegalArgumentException("At least one valid drug prescription line is required");
        }

        savedEncounter = Context.getEncounterService().saveEncounter(savedEncounter);

        Integer resolvedPatientBillId = resolvePatientBillId(patient, patientBillId, requestLines, encounterDatetime);
        Boolean paymentConfirmed = resolvedPatientBillId == null ? Boolean.FALSE : dispensingService.isPatientBillPaid(resolvedPatientBillId);
        request.setPatientBillId(resolvedPatientBillId);
        request.setPaymentConfirmed(Boolean.TRUE.equals(paymentConfirmed));
        request.setRequestStatus(Boolean.TRUE.equals(paymentConfirmed) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        for (PatientDispensingRequestLine requestLine : requestLines) {
            requestLine.setLineStatus(Boolean.TRUE.equals(paymentConfirmed) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        }

        request.setOrder(firstSavedOrder);
        request.setEncounter(savedEncounter);
        request.setLines(requestLines);
        PatientDispensingRequest savedRequest = dispensingService.saveRequest(request);
        notifyPrescriptionSavedAfterCommit(savedRequest, true);
        return savedRequest;
    }

    @Override
    public PatientDispensingRequest updatePrescriptionLine(Integer requestId,
                                                           Integer requestLineId,
                                                           Provider orderingProvider,
                                                           List<PrescriptionLineCommand> lines) {
        if (requestId == null || requestLineId == null) {
            throw new IllegalArgumentException("Request and request line are required");
        }
        if (lines == null || lines.size() != 1 || lines.get(0) == null) {
            throw new IllegalArgumentException("Exactly one updated prescription line is required");
        }
        PatientDispensingRequest request = dispensingService.getRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Prescription request not found");
        }
        PatientDispensingRequestLine requestLine = findEditableRequestLine(request, requestLineId);
        PrescriptionLineCommand line = lines.get(0);
        ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(line.getProductMasterItemId());
        validateProductMasterDrug(productMasterItem);
        org.openmrs.module.rwandapharmacymanagement.model.Item localItem = ensureLocalItem(productMasterItem);
        BigDecimal requestedQuantity = normalizeQuantity(line.getQuantity());
        DrugOrder matchingOrder = findMatchingOrder(request, requestLine);
        if (matchingOrder == null) {
            throw new IllegalStateException("Matching drug order was not found for the selected prescription line");
        }
        if (requestLine.getOrder() == null) {
            requestLine.setOrder(matchingOrder);
        }

        DrugOrder revisedOrder = matchingOrder.cloneForRevision();
        applyLineChanges(revisedOrder, request.getEncounter(), orderingProvider, productMasterItem.getDrug(), line, requestedQuantity);
        Order savedOrder = Context.getOrderService().saveOrder(revisedOrder, (OrderContext) null);
        DrugOrder savedDrugOrder = savedOrder instanceof DrugOrder ? (DrugOrder) savedOrder : revisedOrder;
        requestLine.setOrder(savedDrugOrder);
        if (request.getOrder() != null && request.getOrder().getOrderId() != null
                && matchingOrder.getOrderId() != null
                && request.getOrder().getOrderId().equals(matchingOrder.getOrderId())) {
            request.setOrder(savedDrugOrder);
        }

        requestLine.setItem(localItem);
        requestLine.setRequestedQuantity(requestedQuantity);
        dispensingService.saveRequest(request);
        notifyPrescriptionLineUpdatedAfterCommit(request, requestLine);
        return request;
    }

    private void notifyPrescriptionSavedAfterCommit(final PatientDispensingRequest request, final boolean sendSms) {
        if (prescriptionCommunicationService == null || request == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            PatientDispensingRequest linkedRequest = linkCommittedPrescriptionBill(request);
            schedulePrescriptionBillLinkRetries(linkedRequest == null ? request : linkedRequest, 2);
            prescriptionCommunicationService.handlePrescriptionSaved(linkedRequest == null ? request : linkedRequest, sendSms);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                PatientDispensingRequest linkedRequest = linkCommittedPrescriptionBillInSingleTransaction(request, 1);
                schedulePrescriptionBillLinkRetries(linkedRequest == null ? request : linkedRequest, 2);
                prescriptionCommunicationService.handlePrescriptionSaved(linkedRequest == null ? request : linkedRequest, sendSms);
            }
        });
    }

    private void schedulePrescriptionBillLinkRetries(final PatientDispensingRequest request, final int firstAttempt) {
        if (request == null || request.getPatientDispensingRequestId() == null || request.getPatientBillId() != null) {
            return;
        }
        BILL_LINK_RETRY_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                retryPrescriptionBillLink(request.getPatientDispensingRequestId(), firstAttempt);
            }
        });
    }

    private void retryPrescriptionBillLink(Integer requestId, int firstAttempt) {
        for (int i = 0; i < BILL_LINK_RETRY_DELAYS_MILLIS.length; i++) {
            try {
                Thread.sleep(BILL_LINK_RETRY_DELAYS_MILLIS[i]);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            org.openmrs.api.context.Context.openSession();
            try {
                PatientDispensingRequest request = dispensingService.getRequest(requestId);
                if (request == null || request.getPatientBillId() != null) {
                    return;
                }
                PatientDispensingRequest linkedRequest = linkCommittedPrescriptionBillInSingleTransaction(request, firstAttempt + i);
                if (linkedRequest == null || linkedRequest.getPatientBillId() != null) {
                    return;
                }
            }
            catch (Exception e) {
                log.error("Failed delayed prescription bill link attempt. requestId=" + requestId
                        + ", attempt=" + (firstAttempt + i), e);
            }
            finally {
                org.openmrs.api.context.Context.closeSession();
            }
        }
        log.warn("Prescription request is still not linked to a patient bill after delayed retries. requestId="
                + requestId + ", attempts=" + (firstAttempt + BILL_LINK_RETRY_DELAYS_MILLIS.length - 1));
    }

    private PatientDispensingRequest linkCommittedPrescriptionBillInSingleTransaction(final PatientDispensingRequest request,
                                                                                     final int attempt) {
        if (transactionManager == null) {
            return linkCommittedPrescriptionBill(request, attempt);
        }
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(new TransactionCallback<PatientDispensingRequest>() {
            @Override
            public PatientDispensingRequest doInTransaction(TransactionStatus status) {
                return linkCommittedPrescriptionBill(request, attempt);
            }
        });
    }

    private PatientDispensingRequest linkCommittedPrescriptionBill(PatientDispensingRequest request) {
        return linkCommittedPrescriptionBill(request, 1);
    }

    private PatientDispensingRequest linkCommittedPrescriptionBill(PatientDispensingRequest request, int attempt) {
        if (request == null || request.getPatientDispensingRequestId() == null || request.getPatientBillId() != null
                || request.getPatient() == null) {
            return request;
        }
        try {
            PatientDispensingRequest persistedRequest = dispensingService.getRequest(request.getPatientDispensingRequestId());
            if (persistedRequest == null || persistedRequest.getPatientBillId() != null || persistedRequest.getPatient() == null) {
                return persistedRequest == null ? request : persistedRequest;
            }
            Date requestDate = persistedRequest.getDateCreated();
            if (requestDate == null && persistedRequest.getEncounter() != null) {
                requestDate = persistedRequest.getEncounter().getEncounterDatetime();
            }
            Set<String> itemNames = billingLookupNames(persistedRequest);
            Integer patientBillId = null;
            boolean strictBillMode = strictPaymentBillMode();
            if (persistedRequest.getPatient().getPatientId() != null) {
                patientBillId = dispensingService.findLatestPatientBillId(persistedRequest.getPatient().getPatientId(),
                        requestDate, itemNames);
                if (patientBillId == null && !strictBillMode) {
                    patientBillId = findRecentPatientBillIdFromBilling(persistedRequest.getPatient(), requestDate);
                }
            }
            if (patientBillId == null) {
                if (attempt == 1 || attempt == 5 || attempt == 7) {
                    log.warn("No patient bill found to link prescription request. requestId="
                            + persistedRequest.getPatientDispensingRequestId() + ", patientId="
                            + persistedRequest.getPatient().getPatientId() + ", requestDate=" + requestDate
                            + ", itemNames=" + itemNames + ", attempts=" + attempt);
                }
                return request;
            }
            Boolean paid = dispensingService.isPatientBillPaid(patientBillId);
            persistedRequest.setPatientBillId(patientBillId);
            persistedRequest.setPaymentConfirmed(Boolean.TRUE.equals(paid));
            persistedRequest.setRequestStatus(Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
            if (persistedRequest.getLines() != null) {
                for (PatientDispensingRequestLine line : persistedRequest.getLines()) {
                    if (line == null || isFinalStatus(line.getLineStatus())) {
                        continue;
                    }
                    line.setLineStatus(Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
                }
            }
            PatientDispensingRequest savedRequest = dispensingService.saveRequest(persistedRequest);
            log.info("Linked prescription request to patient bill. requestId="
                    + savedRequest.getPatientDispensingRequestId() + ", patientBillId=" + patientBillId
                    + ", paid=" + Boolean.TRUE.equals(paid) + ", attempt=" + attempt);
            return savedRequest;
        }
        catch (Exception e) {
            log.error("Failed to link prescription request to patient bill. requestId="
                    + request.getPatientDispensingRequestId(), e);
            return request;
        }
    }

    private boolean isFinalStatus(RequestStatus status) {
        return status == RequestStatus.DISPENSED
                || status == RequestStatus.PARTIALLY_DISPENSED
                || status == RequestStatus.UNAVAILABLE
                || status == RequestStatus.CANCELLED;
    }

    private void notifyPrescriptionLineUpdatedAfterCommit(final PatientDispensingRequest request,
                                                          final PatientDispensingRequestLine requestLine) {
        if (prescriptionCommunicationService == null || request == null || requestLine == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            prescriptionCommunicationService.handlePrescriptionLineUpdated(request, requestLine);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                prescriptionCommunicationService.handlePrescriptionLineUpdated(request, requestLine);
            }
        });
    }

    @Override
    public PatientDispensingRequest voidPrescriptionLine(Integer requestId,
                                                         Integer requestLineId,
                                                         String voidReason) {
        if (requestId == null || requestLineId == null) {
            throw new IllegalArgumentException("Request and request line are required");
        }
        String reason = voidReason == null ? "" : voidReason.trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("A void reason is required");
        }
        PatientDispensingRequest request = dispensingService.getRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Prescription request not found");
        }
        PatientDispensingRequestLine requestLine = findEditableRequestLine(request, requestLineId);
        DrugOrder matchingOrder = findMatchingOrder(request, requestLine);
        if (matchingOrder != null && !Boolean.TRUE.equals(matchingOrder.getVoided())) {
            Context.getOrderService().voidOrder(matchingOrder, reason);
        }
        if (requestLine.getOrder() == null && matchingOrder != null) {
            requestLine.setOrder(matchingOrder);
        }
        requestLine.setVoided(Boolean.TRUE);
        requestLine.setVoidReason(reason);
        requestLine.setVoidedBy(Context.getAuthenticatedUser());
        requestLine.setDateVoided(new Date());
        requestLine.setLineStatus(RequestStatus.CANCELLED);
        voidRelatedBillingItems(request, requestLine, matchingOrder, reason);
        updateRequestStatusAfterLineChange(request, reason);
        dispensingService.saveRequest(request);
        return request;
    }

    private Encounter buildEncounter(Patient patient, Visit visit, Provider provider, Location encounterLocation, Date encounterDatetime) {
        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setVisit(visit);
        encounter.setLocation(encounterLocation);
        encounter.setEncounterDatetime(encounterDatetime == null ? new Date() : encounterDatetime);
        encounter.setEncounterType(resolveEncounterType());
        encounter.setCreator(Context.getAuthenticatedUser());
        encounter.setDateCreated(new Date());
        EncounterRole encounterRole = resolveEncounterRole();
        if (provider != null && encounterRole != null) {
            encounter.addProvider(encounterRole, provider);
        }
        return encounter;
    }

    private EncounterRole resolveEncounterRole() {
        EncounterRole encounterRole = Context.getEncounterService().getEncounterRoleByName("Clinician");
        if (encounterRole == null) {
            encounterRole = Context.getEncounterService().getEncounterRoleByName("Unknown");
        }
        if (encounterRole == null) {
            List<EncounterRole> encounterRoles = Context.getEncounterService().getAllEncounterRoles(false);
            if (encounterRoles != null && !encounterRoles.isEmpty()) {
                encounterRole = encounterRoles.get(0);
            }
        }
        return encounterRole;
    }

    private Date resolveEncounterDatetime(Visit visit) {
        return clampToVisitWindow(visit, new Date());
    }

    private Date clampToVisitWindow(Visit visit, Date candidate) {
        Date resolved = candidate == null ? new Date() : candidate;
        if (visit == null) {
            return resolved;
        }
        Date visitStart = visit.getStartDatetime();
        Date visitStop = visit.getStopDatetime();
        if (visitStart != null && resolved.before(visitStart)) {
            resolved = visitStart;
        }
        if (visitStop != null && resolved.after(visitStop)) {
            resolved = visitStop;
        }
        return resolved;
    }

    private Date resolveOrderStartDate(Encounter encounter, Date requestedStartDate) {
        Date resolved = requestedStartDate == null ? new Date() : requestedStartDate;
        if (encounter != null && encounter.getEncounterDatetime() != null && resolved.before(encounter.getEncounterDatetime())) {
            resolved = encounter.getEncounterDatetime();
        }
        return resolved;
    }

    private PatientDispensingRequestLine findEditableRequestLine(PatientDispensingRequest request, Integer requestLineId) {
        if (request == null || request.getLines() == null) {
            throw new IllegalArgumentException("Prescription request has no lines");
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getPatientDispensingRequestLineId() == null) {
                continue;
            }
            if (!line.getPatientDispensingRequestLineId().equals(requestLineId)) {
                continue;
            }
            if (Boolean.TRUE.equals(line.getVoided())) {
                throw new IllegalArgumentException("This prescribed drug has already been voided");
            }
            if (!isEditableStatus(line.getLineStatus())) {
                throw new IllegalArgumentException("Only non-dispensed prescription lines can be changed");
            }
            return line;
        }
        throw new IllegalArgumentException("Prescription line not found");
    }

    private boolean isEditableStatus(RequestStatus status) {
        return status == RequestStatus.PENDING_PAYMENT || status == RequestStatus.PENDING_DISPENSE;
    }

    private void updateRequestStatusAfterLineChange(PatientDispensingRequest request, String reason) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            request.setRequestStatus(RequestStatus.CANCELLED);
            markRequestVoided(request, reason);
            return;
        }
        boolean hasActiveLines = false;
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            hasActiveLines = true;
            break;
        }
        if (!hasActiveLines) {
            request.setRequestStatus(RequestStatus.CANCELLED);
            markRequestVoided(request, reason);
        }
    }

    private void markRequestVoided(PatientDispensingRequest request, String reason) {
        if (request == null) {
            return;
        }
        request.setVoided(Boolean.TRUE);
        request.setVoidedBy(Context.getAuthenticatedUser());
        request.setDateVoided(new Date());
        request.setVoidReason(reason);
    }

    private void voidRelatedBillingItems(PatientDispensingRequest request,
                                         PatientDispensingRequestLine requestLine,
                                         DrugOrder matchingOrder,
                                         String reason) {
        if (request == null || request.getPatientBillId() == null) {
            return;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return;
            }
            Object patientBill = billingServiceClass.getMethod("getPatientBill", Integer.class)
                    .invoke(billingService, request.getPatientBillId());
            if (patientBill == null) {
                return;
            }
            Object consommation = billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            if (consommation == null) {
                return;
            }
            Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
            if (billItems == null || billItems.isEmpty()) {
                return;
            }
            boolean anyVoided = false;
            for (Object billItem : billItems) {
                if (!matchesBillingItem(billItem, requestLine, matchingOrder)) {
                    continue;
                }
                invokeSetter(billItem, "setVoided", Boolean.TRUE, Boolean.class);
                invokeSetter(billItem, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
                invokeSetter(billItem, "setVoidedDate", new Date(), Date.class);
                invokeSetter(billItem, "setVoidReason", reason, String.class);
                billingServiceClass.getMethod("saveBilledItem", patientServiceBillClass).invoke(billingService, billItem);
                anyVoided = true;
            }
            if (!anyVoided) {
                return;
            }
            boolean hasActiveBillItems = false;
            for (Object billItem : billItems) {
                if (!extractBoolean(billItem, "getVoided")) {
                    hasActiveBillItems = true;
                    break;
                }
            }
            if (!hasActiveBillItems) {
                invokeSetter(consommation, "setVoided", Boolean.TRUE, boolean.class);
                invokeSetter(consommation, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
                invokeSetter(consommation, "setVoidedDate", new Date(), Date.class);
                invokeSetter(consommation, "setVoidReason", reason, String.class);
                billingServiceClass.getMethod("saveConsommation", consommationClass).invoke(billingService, consommation);
                if (!extractBoolean(patientBill, "getIsPaid")) {
                    invokeSetter(patientBill, "setVoided", Boolean.TRUE, Boolean.class);
                    invokeSetter(patientBill, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
                    invokeSetter(patientBill, "setVoidedDate", new Date(), Date.class);
                    invokeSetter(patientBill, "setVoidReason", reason, String.class);
                    invokeSetter(patientBill, "setStatus", "CANCELLED", String.class);
                    billingServiceClass.getMethod("savePatientBill", patientBillClass).invoke(billingService, patientBill);
                }
            }
        }
        catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private Set<?> safeCastSet(Object value) {
        return value instanceof Set ? (Set<?>) value : null;
    }

    private boolean matchesBillingItem(Object billItem,
                                       PatientDispensingRequestLine requestLine,
                                       DrugOrder matchingOrder) {
        if (billItem == null || extractBoolean(billItem, "getVoided")) {
            return false;
        }
        return billItemMatchesAnyName(billItem, requestLineNames(requestLine, matchingOrder));
    }

    private Integer resolvePatientBillId(Patient patient,
                                         Integer explicitPatientBillId,
                                         List<PatientDispensingRequestLine> requestLines,
                                         Date requestDate) {
        if (explicitPatientBillId != null) {
            return explicitPatientBillId;
        }
        return null;
    }

    private Integer findRecentPatientBillIdFromBilling(Patient patient, Date requestDate) {
        if (patient == null) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return null;
            }
            Method getBills = billingServiceClass.getMethod("getBills", Patient.class, Date.class, Date.class);
            Date center = requestDate == null ? new Date() : requestDate;
            Object patientBill = getBills.invoke(billingService, patient, addHours(center, -12), addHours(center, 12));
            Integer patientBillId = extractInteger(patientBill, "getPatientBillId");
            if (patientBillId != null) {
                return patientBillId;
            }
            Method getUnpaidBills = findMethod(billingServiceClass, "getUnpaidBills", Patient.class);
            if (getUnpaidBills != null) {
                Object unpaidBills = getUnpaidBills.invoke(billingService, patient);
                if (unpaidBills instanceof Iterable) {
                    Integer latestBillId = null;
                    Date latestCreatedDate = null;
                    for (Object bill : (Iterable<?>) unpaidBills) {
                        Integer billId = extractInteger(bill, "getPatientBillId");
                        Date createdDate = extractDate(bill, "getCreatedDate");
                        if (billId == null) {
                            continue;
                        }
                        if (latestBillId == null || (createdDate != null
                                && (latestCreatedDate == null || createdDate.after(latestCreatedDate)))) {
                            latestBillId = billId;
                            latestCreatedDate = createdDate;
                        }
                    }
                    return latestBillId;
                }
            }
            return null;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Integer findPatientBillIdByPrescribedItemNames(Patient patient,
                                                           List<PatientDispensingRequestLine> requestLines,
                                                           Date requestDate) {
        Set<String> requestedNames = requestLineNames(requestLines);
        if (patient == null || requestedNames.isEmpty()) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return null;
            }
            Method getPolicies = billingServiceClass.getMethod("getAllInsurancePoliciesByPatient", Patient.class);
            Object policyResult = getPolicies.invoke(billingService, patient);
            if (!(policyResult instanceof Iterable)) {
                return null;
            }
            Method getConsommationsByBeneficiary = billingServiceClass.getMethod("getConsommationsByBeneficiary",
                    Context.loadClass("org.openmrs.module.mohbilling.model.Beneficiary"));
            Integer bestBillId = null;
            int bestScore = 0;
            long bestDistance = Long.MAX_VALUE;
            for (Object policy : (Iterable<?>) policyResult) {
                Iterable<?> beneficiaries = extractIterable(policy, "getBeneficiaries");
                if (beneficiaries == null) {
                    continue;
                }
                for (Object beneficiary : beneficiaries) {
                    if (!matchesPatient(beneficiary, patient)) {
                        continue;
                    }
                    Object consommations = getConsommationsByBeneficiary.invoke(billingService, beneficiary);
                    if (!(consommations instanceof Iterable)) {
                        continue;
                    }
                    for (Object consommation : (Iterable<?>) consommations) {
                        Object patientBill = invokeNoArg(consommation, "getPatientBill");
                        Integer candidateBillId = extractInteger(patientBill, "getPatientBillId");
                        if (candidateBillId == null) {
                            continue;
                        }
                        int score = countMatchingBillItems(consommation, requestedNames);
                        if (score <= 0) {
                            continue;
                        }
                        Date candidateDate = extractDate(consommation, "getCreatedDate");
                        long distance = requestDate == null || candidateDate == null
                                ? Long.MAX_VALUE - candidateBillId.longValue()
                                : Math.abs(candidateDate.getTime() - requestDate.getTime());
                        if (bestBillId == null || score > bestScore || (score == bestScore && distance < bestDistance)) {
                            bestBillId = candidateBillId;
                            bestScore = score;
                            bestDistance = distance;
                        }
                    }
                }
            }
            return bestBillId;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private int countMatchingBillItems(Object consommation, Set<String> requestedNames) {
        Iterable<?> billItems = extractIterable(consommation, "getBillItems");
        if (billItems == null || requestedNames == null || requestedNames.isEmpty()) {
            return 0;
        }
        int score = 0;
        for (Object billItem : billItems) {
            if (billItem == null || extractBoolean(billItem, "getVoided")) {
                continue;
            }
            if (billItemMatchesAnyName(billItem, requestedNames)) {
                score++;
            }
        }
        return score;
    }

    private boolean billItemMatchesAnyName(Object billItem, Set<String> requestedNames) {
        if (billItem == null || requestedNames == null || requestedNames.isEmpty()) {
            return false;
        }
        for (String label : billingItemLabels(billItem)) {
            for (String requestedName : requestedNames) {
                if (namesMatch(label, requestedName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> requestLineNames(List<PatientDispensingRequestLine> requestLines) {
        Set<String> names = new HashSet<String>();
        if (requestLines == null) {
            return names;
        }
        for (PatientDispensingRequestLine requestLine : requestLines) {
            names.addAll(requestLineNames(requestLine, requestLine == null ? null : requestLine.getOrder()));
        }
        return names;
    }

    private Set<String> billingLookupNames(PatientDispensingRequest request) {
        Set<String> names = new HashSet<String>();
        if (request == null || request.getLines() == null) {
            return names;
        }
        for (PatientDispensingRequestLine requestLine : request.getLines()) {
            if (requestLine == null) {
                continue;
            }
            names.addAll(requestLineNames(requestLine, requestLine.getOrder()));
            if (requestLine.getItem() != null) {
                addRawName(names, requestLine.getItem().getName());
                if (requestLine.getItem().getDrug() != null) {
                    addRawName(names, requestLine.getItem().getDrug().getName());
                }
            }
            if (requestLine.getOrder() instanceof DrugOrder && ((DrugOrder) requestLine.getOrder()).getDrug() != null) {
                addRawName(names, ((DrugOrder) requestLine.getOrder()).getDrug().getName());
            }
        }
        return names;
    }

    private Set<String> requestLineNames(PatientDispensingRequestLine requestLine, Order order) {
        Set<String> names = new HashSet<String>();
        if (requestLine != null && requestLine.getItem() != null) {
            addNormalizedName(names, requestLine.getItem().getName());
            if (requestLine.getItem().getDrug() != null) {
                addNormalizedName(names, requestLine.getItem().getDrug().getName());
            }
        }
        if (order instanceof DrugOrder && ((DrugOrder) order).getDrug() != null) {
            addNormalizedName(names, ((DrugOrder) order).getDrug().getName());
        }
        return names;
    }

    private List<String> billingItemLabels(Object billItem) {
        List<String> labels = new ArrayList<String>();
        Object service = invokeNoArg(billItem, "getService");
        Object facilityServicePrice = service == null ? null : invokeNoArg(service, "getFacilityServicePrice");
        addNormalizedName(labels, safeString(invokeNoArg(billItem, "getServiceOther")));
        addNormalizedName(labels, safeString(invokeNoArg(billItem, "getServiceOtherDescription")));
        addNormalizedName(labels, safeString(facilityServicePrice == null ? null : invokeNoArg(facilityServicePrice, "getName")));
        addNormalizedName(labels, safeString(facilityServicePrice == null ? null : invokeNoArg(facilityServicePrice, "getShortName")));
        addNormalizedName(labels, safeString(facilityServicePrice == null ? null : invokeNoArg(facilityServicePrice, "getDescription")));
        addNormalizedName(labels, safeString(service));
        return labels;
    }

    private boolean namesMatch(String left, String right) {
        String normalizedLeft = normalizeName(left);
        String normalizedRight = normalizeName(right);
        if (normalizedLeft.isEmpty() || normalizedRight.isEmpty()) {
            return false;
        }
        if (normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft)) {
            return true;
        }
        Set<String> leftTokens = nameTokens(normalizedLeft);
        Set<String> rightTokens = nameTokens(normalizedRight);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return false;
        }
        int overlap = 0;
        for (String token : leftTokens) {
            if (rightTokens.contains(token)) {
                overlap++;
            }
        }
        int smallerSize = Math.min(leftTokens.size(), rightTokens.size());
        return overlap >= Math.min(2, smallerSize);
    }

    private Set<String> nameTokens(String normalizedName) {
        Set<String> tokens = new HashSet<String>();
        if (normalizedName == null) {
            return tokens;
        }
        for (String token : normalizedName.split(" ")) {
            if (token != null && token.trim().length() > 1) {
                tokens.add(token.trim());
            }
        }
        return tokens;
    }

    private void addNormalizedName(Collection<String> names, String name) {
        String normalized = normalizeName(name);
        if (names != null && !normalized.isEmpty()) {
            names.add(normalized);
        }
    }

    private void addRawName(Collection<String> names, String name) {
        String raw = name == null ? "" : name.trim().toLowerCase(Locale.ENGLISH);
        if (names != null && !raw.isEmpty()) {
            names.add(raw);
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private Iterable<?> extractIterable(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        if (value instanceof Iterable) {
            return (Iterable<?>) value;
        }
        if (value instanceof Collection) {
            return (Collection<?>) value;
        }
        return null;
    }

    private boolean matchesPatient(Object beneficiary, Patient patient) {
        Object beneficiaryPatient = invokeNoArg(beneficiary, "getPatient");
        if (beneficiaryPatient == null || patient == null || patient.getPatientId() == null) {
            return false;
        }
        Object beneficiaryPatientId = invokeNoArg(beneficiaryPatient, "getPatientId");
        return beneficiaryPatientId instanceof Integer && patient.getPatientId().equals(beneficiaryPatientId);
    }

    private Date extractDate(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Date ? (Date) value : null;
    }

    private Date addHours(Date date, int hours) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);
        calendar.add(java.util.Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    private Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null) {
            return null;
        }
        try {
            return type.getMethod(name, parameterTypes);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        }
        catch (Exception e) {
            return null;
        }
    }

    private void invokeSetter(Object target, String methodName, Object value, Class<?> parameterType) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
        }
        catch (Exception ignored) {
        }
    }

    private boolean extractBoolean(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private Integer extractInteger(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Integer ? (Integer) value : null;
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private org.openmrs.DrugOrder buildDrugOrder(Patient patient,
                                                 Encounter encounter,
                                                 Provider provider,
                                                 Drug drug,
                                                 PrescriptionLineCommand line,
                                                 BigDecimal requestedQuantity,
                                                 int sortWeight) {
        org.openmrs.DrugOrder order = new org.openmrs.DrugOrder();
        order.setPatient(patient);
        order.setEncounter(encounter);
        order.setCareSetting(resolveCareSetting());
        order.setOrderType(resolveDrugOrderType(drug));
        order.setConcept(drug.getConcept());
        order.setDrug(drug);
        order.setOrderer(provider);
        order.setAction(Order.Action.NEW);
        order.setUrgency(Order.Urgency.ROUTINE);
        order.setDateActivated(resolveOrderStartDate(encounter, line.getStartDate()));
        order.setSortWeight(Double.valueOf(sortWeight));
        order.setDose(line.getDose() == null ? null : line.getDose().doubleValue());
        order.setDoseUnits(resolveDoseUnits(drug, line));
        order.setQuantity(requestedQuantity.doubleValue());
        order.setQuantityUnits(resolveQuantityUnits(drug));
        order.setNumRefills(Integer.valueOf(0));
        order.setRoute(resolveRoute(line.getRoute()));
        order.setFrequency(resolveFrequency(line.getFrequency()));
        order.setDuration(line.getDuration());
        order.setDurationUnits(resolveDurationUnit(line.getDurationUnit()));
        order.setInstructions(buildInstructions(line));
        order.setDosingInstructions(buildInstructions(line));
        if (line.getDuration() != null && line.getDuration().intValue() > 0) {
            order.setAutoExpireDateBasedOnDuration();
        }
        return order;
    }

    private void applyLineChanges(DrugOrder order,
                                  Encounter encounter,
                                  Provider provider,
                                  Drug drug,
                                  PrescriptionLineCommand line,
                                  BigDecimal requestedQuantity) {
        order.setEncounter(encounter);
        order.setPatient(encounter == null ? order.getPatient() : encounter.getPatient());
        order.setCareSetting(resolveCareSetting());
        order.setOrderer(provider);
        order.setDrug(drug);
        order.setConcept(drug.getConcept());
        order.setOrderType(resolveDrugOrderType(drug));
        order.setDateActivated(resolveOrderStartDate(encounter, line.getStartDate()));
        order.setDose(line.getDose() == null ? null : line.getDose().doubleValue());
        order.setDoseUnits(resolveDoseUnits(drug, line));
        order.setQuantity(requestedQuantity.doubleValue());
        order.setQuantityUnits(resolveQuantityUnits(drug));
        order.setRoute(resolveRoute(line.getRoute()));
        order.setFrequency(resolveFrequency(line.getFrequency()));
        order.setDuration(line.getDuration());
        order.setDurationUnits(resolveDurationUnit(line.getDurationUnit()));
        order.setInstructions(buildInstructions(line));
        order.setDosingInstructions(buildInstructions(line));
        if (line.getDuration() != null && line.getDuration().intValue() > 0) {
            order.setAutoExpireDateBasedOnDuration();
        } else {
            order.setAutoExpireDate(null);
        }
    }

    private DrugOrder findMatchingOrder(PatientDispensingRequest request, PatientDispensingRequestLine requestLine) {
        if (request == null || request.getEncounter() == null || request.getEncounter().getOrders() == null
                || requestLine == null || requestLine.getItem() == null) {
            return null;
        }
        DrugOrder linkedOrder = resolveLinkedDrugOrder(requestLine);
        if (linkedOrder != null) {
            return linkedOrder;
        }
        List<DrugOrder> activeDrugOrders = getActiveDrugOrders(request);
        if (activeDrugOrders.size() == 1) {
            return activeDrugOrders.get(0);
        }
        Drug lineDrug = resolveLineDrug(requestLine);
        Concept lineConcept = resolveLineConcept(requestLine, lineDrug);
        for (DrugOrder drugOrder : activeDrugOrders) {
            if (lineDrug != null && drugOrder.getDrug() != null
                    && lineDrug.getDrugId() != null
                    && lineDrug.getDrugId().equals(drugOrder.getDrug().getDrugId())) {
                return drugOrder;
            }
            if (lineConcept != null && drugOrder.getConcept() != null
                    && lineConcept.getConceptId() != null
                    && lineConcept.getConceptId().equals(drugOrder.getConcept().getConceptId())) {
                return drugOrder;
            }
        }
        return null;
    }

    private DrugOrder resolveLinkedDrugOrder(PatientDispensingRequestLine requestLine) {
        if (requestLine == null || requestLine.getOrder() == null || requestLine.getOrder().getOrderId() == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(requestLine.getOrder().getVoided())) {
            return requestLine.getOrder();
        }
        PatientDispensingRequest request = requestLine.getPatientDispensingRequest();
        if (request != null && request.getEncounter() != null && request.getEncounter().getOrders() != null) {
            for (Order order : request.getEncounter().getOrders()) {
                if (order instanceof DrugOrder && order.getOrderId() != null
                        && order.getOrderId().equals(requestLine.getOrder().getOrderId())) {
                    return (DrugOrder) order;
                }
            }
        }
        Order order = Context.getOrderService().getOrder(requestLine.getOrder().getOrderId());
        return order instanceof DrugOrder ? (DrugOrder) order : null;
    }

    private List<DrugOrder> getActiveDrugOrders(PatientDispensingRequest request) {
        List<DrugOrder> drugOrders = new ArrayList<DrugOrder>();
        if (request == null || request.getEncounter() == null) {
            return drugOrders;
        }
        Collection<Order> orders = request.getEncounter().getOrders();
        if (orders == null) {
            return drugOrders;
        }
        for (Order order : orders) {
            if (order instanceof DrugOrder && !Boolean.TRUE.equals(order.getVoided())) {
                drugOrders.add((DrugOrder) order);
            }
        }
        return drugOrders;
    }

    private Drug resolveLineDrug(PatientDispensingRequestLine requestLine) {
        if (requestLine == null || requestLine.getItem() == null) {
            return null;
        }
        if (requestLine.getItem().getDrug() != null) {
            return requestLine.getItem().getDrug();
        }
        ProductMasterItem productMasterItem = resolveProductMasterItem(requestLine);
        return productMasterItem == null ? null : productMasterItem.getDrug();
    }

    private Concept resolveLineConcept(PatientDispensingRequestLine requestLine, Drug resolvedDrug) {
        if (resolvedDrug != null && resolvedDrug.getConcept() != null) {
            return resolvedDrug.getConcept();
        }
        if (requestLine != null && requestLine.getItem() != null && requestLine.getItem().getConcept() != null) {
            return requestLine.getItem().getConcept();
        }
        ProductMasterItem productMasterItem = resolveProductMasterItem(requestLine);
        return productMasterItem != null && productMasterItem.getDrug() != null ? productMasterItem.getDrug().getConcept() : null;
    }

    private ProductMasterItem resolveProductMasterItem(PatientDispensingRequestLine requestLine) {
        if (requestLine == null || requestLine.getItem() == null) {
            return null;
        }
        String code = requestLine.getItem().getCode();
        if (code != null && !code.trim().isEmpty()) {
            ProductMasterItem byCode = pharmacyService.getProductMasterItemByNpcCode(code.trim());
            if (byCode != null) {
                return byCode;
            }
        }
        List<ProductMasterItem> productMasterItems = pharmacyService.getAllProductMasterItems();
        if (productMasterItems == null) {
            return null;
        }
        for (ProductMasterItem item : productMasterItems) {
            if (item != null && item.getItemName() != null && requestLine.getItem().getName() != null
                    && item.getItemName().equalsIgnoreCase(requestLine.getItem().getName())) {
                return item;
            }
        }
        return null;
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Prescription quantity must be greater than zero");
        }
        return quantity.stripTrailingZeros();
    }

    private void validateProductMasterDrug(ProductMasterItem productMasterItem) {
        if (productMasterItem == null) {
            throw new IllegalArgumentException("Selected product master drug was not found");
        }
        if (!Boolean.TRUE.equals(productMasterItem.getActive())) {
            throw new IllegalArgumentException("Selected product master drug is inactive");
        }
        if (!"ZMED".equalsIgnoreCase(safe(productMasterItem.getMaterialType()))) {
            throw new IllegalArgumentException("Only CMED drug products can be prescribed on this page");
        }
        if (productMasterItem.getDrug() == null) {
            throw new IllegalArgumentException("Selected product master drug is not linked to an OpenMRS drug");
        }
    }

    private org.openmrs.module.rwandapharmacymanagement.model.Item ensureLocalItem(ProductMasterItem productMasterItem) {
        String npcCode = safe(productMasterItem.getNpcCode());
        if (!npcCode.trim().isEmpty()) {
            org.openmrs.module.rwandapharmacymanagement.model.Item existingItem =
                    pharmacyService.getItemByCodeAndProgram(npcCode, productMasterItem.getProgram());
            if (existingItem == null && safe(productMasterItem.getProgram()).trim().isEmpty()) {
                existingItem = pharmacyService.getItemByCode(npcCode);
            }
            if (existingItem != null) {
                return existingItem;
            }
        }
        if (productMasterItem.getDrug() != null && productMasterItem.getDrug().getDrugId() != null) {
            for (org.openmrs.module.rwandapharmacymanagement.model.Item item : pharmacyService.getAllItems()) {
                if (item != null && item.getDrug() != null && item.getDrug().getDrugId() != null
                        && item.getDrug().getDrugId().equals(productMasterItem.getDrug().getDrugId())) {
                    return item;
                }
            }
        }
        org.openmrs.module.rwandapharmacymanagement.model.Item item = new org.openmrs.module.rwandapharmacymanagement.model.Item();
        item.setCode(npcCode.trim().isEmpty() ? "LOCAL-" + System.currentTimeMillis() : npcCode);
        item.setName(productMasterItem.getItemName());
        item.setProgram(productMasterItem.getProgram());
        item.setSupplierManaged(Boolean.TRUE);
        item.setStockable(Boolean.TRUE);
        item.setDispensable(Boolean.TRUE);
        item.setRequiresBatch(Boolean.FALSE);
        item.setRequiresExpiry(Boolean.FALSE);
        item.setDrug(productMasterItem.getDrug());
        item.setConcept(productMasterItem.getDrug() == null ? findOrCreateConcept(productMasterItem.getItemName())
                : productMasterItem.getDrug().getConcept());
        item.setCreator(Context.getAuthenticatedUser());
        item.setDateCreated(new Date());
        return pharmacyService.saveItem(item);
    }

    private Concept findOrCreateConcept(String conceptName) {
        String normalizedConceptName = safe(conceptName);
        Concept concept = Context.getConceptService().getConceptByName(normalizedConceptName);
        if (concept != null) {
            return concept;
        }
        concept = new Concept();
        ConceptName preferredName = new ConceptName(normalizedConceptName, Locale.ENGLISH);
        preferredName.setLocalePreferred(true);
        concept.addName(preferredName);
        concept.setDatatype(resolveConceptDatatype());
        concept.setConceptClass(resolveConceptClass());
        concept.setSet(false);
        concept.setCreator(Context.getAuthenticatedUser());
        concept.setDateCreated(new Date());
        return Context.getConceptService().saveConcept(concept);
    }

    private ConceptDatatype resolveConceptDatatype() {
        ConceptDatatype datatype = Context.getConceptService().getConceptDatatypeByName("N/A");
        if (datatype == null) {
            datatype = Context.getConceptService().getConceptDatatypeByName("Text");
        }
        if (datatype == null) {
            throw new IllegalStateException("Unable to resolve concept datatype for prescription item creation");
        }
        return datatype;
    }

    private boolean strictPaymentBillMode() {
        String mode = Context.getAdministrationService().getGlobalProperty(
                RwandaPharmacyManagementConfig.GP_PRESCRIPTION_PAYMENT_BILL_MODE, "latest");
        return "strict".equalsIgnoreCase(mode == null ? "" : mode.trim());
    }

    private ConceptClass resolveConceptClass() {
        ConceptClass conceptClass = Context.getConceptService().getConceptClassByName("Drug");
        if (conceptClass == null) {
            conceptClass = Context.getConceptService().getConceptClassByName("Misc");
        }
        if (conceptClass == null) {
            throw new IllegalStateException("Unable to resolve concept class for prescription item creation");
        }
        return conceptClass;
    }

    private EncounterType resolveEncounterType() {
        EncounterType encounterType = Context.getEncounterService().getEncounterType("Prescription");
        if (encounterType != null) {
            return encounterType;
        }
        encounterType = new EncounterType("Prescription", "Drug prescription encounter");
        encounterType.setCreator(Context.getAuthenticatedUser());
        encounterType.setDateCreated(new Date());
        return Context.getEncounterService().saveEncounterType(encounterType);
    }

    private CareSetting resolveCareSetting() {
        CareSetting careSetting = Context.getOrderService().getCareSettingByName("Outpatient");
        if (careSetting != null) {
            return careSetting;
        }
        List<CareSetting> careSettings = Context.getOrderService().getCareSettings(false);
        if (careSettings != null && !careSettings.isEmpty()) {
            return careSettings.get(0);
        }
        throw new IllegalStateException("Unable to resolve a care setting for prescription orders");
    }

    private OrderType resolveDrugOrderType(Drug drug) {
        OrderType orderType = Context.getOrderService().getOrderTypeByName("Drug order");
        if (orderType == null) {
            orderType = Context.getOrderService().getOrderTypeByName("Drug Order");
        }
        if (orderType != null) {
            return orderType;
        }
        if (drug != null && drug.getConcept() != null && drug.getConcept().getConceptClass() != null) {
            orderType = Context.getOrderService().getOrderTypeByConceptClass(drug.getConcept().getConceptClass());
        }
        if (orderType != null) {
            return orderType;
        }
        List<OrderType> orderTypes = Context.getOrderService().getOrderTypes(false);
        if (orderTypes != null) {
            for (OrderType candidate : orderTypes) {
                if (candidate != null && candidate.getName() != null
                        && candidate.getName().toLowerCase().contains("drug")) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("Unable to resolve a drug order type for prescription orders");
    }

    private OrderFrequency resolveFrequency(String frequencyName) {
        String desired = safe(frequencyName).toLowerCase();
        if (desired.isEmpty()) {
            return null;
        }
        List<OrderFrequency> frequencies = Context.getOrderService().getOrderFrequencies(false);
        if (frequencies == null) {
            return null;
        }
        for (OrderFrequency frequency : frequencies) {
            if (frequency != null && frequency.getName() != null && desired.equals(frequency.getName().trim().toLowerCase())) {
                return frequency;
            }
        }
        for (OrderFrequency frequency : frequencies) {
            if (frequency != null && frequency.getName() != null
                    && frequency.getName().trim().toLowerCase().contains(desired)) {
                return frequency;
            }
        }
        return null;
    }

    private Concept resolveRoute(String routeName) {
        String desired = safe(routeName).toLowerCase();
        if (desired.isEmpty()) {
            return null;
        }
        List<Concept> routes = Context.getOrderService().getDrugRoutes();
        if (routes == null) {
            return null;
        }
        for (Concept route : routes) {
            if (route != null && route.getDisplayString() != null
                    && desired.equals(route.getDisplayString().trim().toLowerCase())) {
                return route;
            }
        }
        return null;
    }

    private Concept resolveDurationUnit(String durationUnitName) {
        String desired = safe(durationUnitName).toLowerCase();
        if (desired.isEmpty()) {
            return null;
        }
        List<Concept> durationUnits = Context.getOrderService().getDurationUnits();
        if (durationUnits == null) {
            return null;
        }
        for (Concept durationUnit : durationUnits) {
            if (durationUnit != null && durationUnit.getDisplayString() != null
                    && desired.equals(durationUnit.getDisplayString().trim().toLowerCase())) {
                return durationUnit;
            }
        }
        for (Concept durationUnit : durationUnits) {
            if (durationUnit != null && durationUnit.getDisplayString() != null
                    && durationUnit.getDisplayString().trim().toLowerCase().startsWith(desired)) {
                return durationUnit;
            }
        }
        return null;
    }

    private String buildInstructions(PrescriptionLineCommand line) {
        List<String> parts = new ArrayList<String>();
        if (line.getDose() != null) {
            parts.add("Dose: " + line.getDose().stripTrailingZeros().toPlainString());
        }
        if (!safe(line.getRoute()).isEmpty()) {
            parts.add("Route: " + line.getRoute().trim());
        }
        if (!safe(line.getFrequency()).isEmpty()) {
            parts.add("Frequency: " + line.getFrequency().trim());
        }
        if (line.getDuration() != null && !safe(line.getDurationUnit()).isEmpty()) {
            parts.add("Duration: " + line.getDuration() + " " + line.getDurationUnit().trim());
        }
        if (!safe(line.getInstructions()).isEmpty()) {
            parts.add(line.getInstructions().trim());
        }
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Concept resolveDoseUnits(Drug drug, PrescriptionLineCommand line) {
        if (line == null || line.getDose() == null) {
            return null;
        }
        if (drug != null && drug.getDoseLimitUnits() != null) {
            return drug.getDoseLimitUnits();
        }
        List<Concept> dosingUnits = Context.getOrderService().getDrugDosingUnits();
        if (dosingUnits != null && !dosingUnits.isEmpty()) {
            return dosingUnits.get(0);
        }
        return drug == null ? null : drug.getConcept();
    }

    private Concept resolveQuantityUnits(Drug drug) {
        List<Concept> dispensingUnits = Context.getOrderService().getDrugDispensingUnits();
        if (dispensingUnits != null && !dispensingUnits.isEmpty()) {
            return dispensingUnits.get(0);
        }
        if (drug != null && drug.getDosageForm() != null) {
            return drug.getDosageForm();
        }
        return drug == null ? null : drug.getConcept();
    }
}
