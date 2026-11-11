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
import org.openmrs.FreeTextDosingInstructions;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.ConsumableOrderService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PrescriptionService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescriptionLineCommand;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableBillingReadiness;
import org.openmrs.module.rwandapharmacymanagement.integration.PrescriptionCommunicationService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.module.rwandapharmacymanagement.util.PrescribableItemSupport;
import org.openmrs.module.rwandapharmacymanagement.util.ProductMasterPrivilegeSupport;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private ConsumableOrderService consumableOrderService;
    private InventoryService inventoryService;
    private PrescriptionCommunicationService prescriptionCommunicationService;
    private PlatformTransactionManager transactionManager;

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setDispensingService(DispensingService dispensingService) {
        this.dispensingService = dispensingService;
    }

    public void setConsumableOrderService(ConsumableOrderService consumableOrderService) {
        this.consumableOrderService = consumableOrderService;
    }

    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
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
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION);
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
        if (consumableOrderService == null) {
            throw new IllegalStateException("Prescription billing readiness service is not configured.");
        }
        ConsumableBillingReadiness billingReadiness = consumableOrderService.getBillingReadiness(patient);
        if (!billingReadiness.isReady()) {
            throw new IllegalArgumentException(billingReadiness.getMessage());
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
        boolean hasBillableLine = false;
        int sortWeight = 1;
        String inheritedHiePrescriptionGroupId = null;
        for (PrescriptionLineCommand line : lines) {
            if (line == null || (line.getItemId() == null && line.getProductMasterItemId() == null)) {
                continue;
            }
            org.openmrs.module.rwandapharmacymanagement.model.Item localItem = resolvePrescribedItem(line, true);
            Drug drug = localItem.getDrug();
            requireDoseInstructions(line);
            BigDecimal requestedQuantity = calculateRequestedQuantity(line);
            Date orderStartDate = resolveOrderStartDate(savedEncounter, line.getStartDate());
            HiePrescriptionIdentity inheritedIdentity = replaceActiveSameDrugOrder(patient, drug,
                    savedEncounter, orderingProvider, orderStartDate);
            if (inheritedHiePrescriptionGroupId == null && inheritedIdentity != null
                    && hasText(inheritedIdentity.getHiePrescriptionGroupId())) {
                inheritedHiePrescriptionGroupId = inheritedIdentity.getHiePrescriptionGroupId();
            }
            DrugOrder order = buildDrugOrder(patient, savedEncounter, orderingProvider, drug, line,
                    requestedQuantity, sortWeight++);
            Order savedOrder = Context.getOrderService().saveOrder(order, (OrderContext) null);
            savedEncounter.addOrder(savedOrder);
            if (firstSavedOrder == null) {
                firstSavedOrder = savedOrder;
            }
            PatientDispensingRequestLine requestLine = new PatientDispensingRequestLine();
            requestLine.setPatientDispensingRequest(request);
            requestLine.setOrder(savedOrder instanceof DrugOrder ? (DrugOrder) savedOrder : order);
            requestLine.setItem(localItem);
            requestLine.setRequestedQuantity(requestedQuantity);
            if (inheritedIdentity != null && hasText(inheritedIdentity.getHieMedicationRequestId())) {
                requestLine.setHieMedicationRequestId(inheritedIdentity.getHieMedicationRequestId());
            }
            applyLineBillingControl(requestLine, line);
            applyHiddenItemBillingControl(requestLine);
            boolean billableLine = Boolean.TRUE.equals(requestLine.getBillable()) && isAvailableInHospitalStock(localItem);
            hasBillableLine = hasBillableLine || billableLine;
            requestLine.setLineStatus(billableLine ? RequestStatus.PENDING_PAYMENT : RequestStatus.PENDING_DISPENSE);
            requestLines.add(requestLine);
        }

        if (requestLines.isEmpty()) {
            throw new IllegalArgumentException("At least one valid drug prescription line is required");
        }

        savedEncounter = Context.getEncounterService().saveEncounter(savedEncounter);

        Integer resolvedPatientBillId = hasBillableLine
                ? resolvePatientBillId(patient, patientBillId, requestLines, encounterDatetime) : null;
        if (hasBillableLine) {
            requireMohBillingCanBill(patient, requestLines);
        }
        Boolean paymentConfirmed = hasBillableLine && resolvedPatientBillId != null
                ? dispensingService.isPatientBillPaid(resolvedPatientBillId) : Boolean.valueOf(!hasBillableLine);
        request.setBillable(Boolean.valueOf(hasBillableLine));
        request.setPatientBillId(resolvedPatientBillId);
        request.setPaymentConfirmed(Boolean.TRUE.equals(paymentConfirmed));
        request.setRequestStatus(Boolean.TRUE.equals(paymentConfirmed) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        for (PatientDispensingRequestLine requestLine : requestLines) {
            if (requestLine.getLineStatus() == RequestStatus.PENDING_PAYMENT) {
                requestLine.setLineStatus(Boolean.TRUE.equals(paymentConfirmed) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
            }
        }

        request.setOrder(firstSavedOrder);
        request.setEncounter(savedEncounter);
        request.setLines(requestLines);
        PatientDispensingRequest savedRequest = dispensingService.saveRequest(request);
        initializeHiePrescriptionIdentity(savedRequest, inheritedHiePrescriptionGroupId);
        savedRequest = dispensingService.saveRequest(savedRequest);
        queuePrescriptionSavedCommunications(savedRequest, true);
        return savedRequest;
    }

    @Override
    public PatientDispensingRequest updatePrescriptionLine(Integer requestId,
                                                           Integer requestLineId,
                                                           Provider orderingProvider,
                                                           List<PrescriptionLineCommand> lines) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION);
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
        if (isPrescriptionPaymentConfirmed(request)) {
            throw new IllegalArgumentException("This prescription is already paid and cannot be edited");
        }
        PatientDispensingRequestLine requestLine = findEditableRequestLine(request, requestLineId);
        PrescriptionLineCommand line = lines.get(0);
        boolean billingOnlyEdit = Boolean.FALSE.equals(line.getBillable());
        org.openmrs.module.rwandapharmacymanagement.model.Item localItem = resolvePrescribedItem(line, !billingOnlyEdit);
        Drug drug = localItem.getDrug();
        requireDoseInstructions(line);
        BigDecimal requestedQuantity = calculateRequestedQuantity(line);
        DrugOrder matchingOrder = findMatchingOrder(request, requestLine);
        if (matchingOrder == null) {
            throw new IllegalStateException("Matching drug order was not found for the selected prescription line");
        }
        if (requestLine.getOrder() == null) {
            requestLine.setOrder(matchingOrder);
        }
        initializeHiePrescriptionIdentity(request, null);
        PatientDispensingRequestLine previousBillableLine = billingMatchSnapshot(requestLine, matchingOrder);
        boolean wasBillableLine = Boolean.TRUE.equals(requestLine.getBillable());
        Integer relatedPatientBillId = request.getPatientBillId();

        if (!billingOnlyEdit) {
            if (sameOrderable(matchingOrder, drug)) {
                if (!canReviseOrder(matchingOrder)) {
                    throw new IllegalArgumentException("This OpenMRS drug order is already stopped or expired. Only billing exclusion can be changed for this line.");
                }
                DrugOrder revisedOrder = matchingOrder.cloneForRevision();
                applyLineChanges(revisedOrder, request.getEncounter(), orderingProvider, drug, line, requestedQuantity);
                Order savedOrder = Context.getOrderService().saveOrder(revisedOrder, (OrderContext) null);
                DrugOrder savedDrugOrder = savedOrder instanceof DrugOrder ? (DrugOrder) savedOrder : revisedOrder;
                requestLine.setOrder(savedDrugOrder);
                if (request.getOrder() != null && request.getOrder().getOrderId() != null
                        && matchingOrder.getOrderId() != null
                        && request.getOrder().getOrderId().equals(matchingOrder.getOrderId())) {
                    request.setOrder(savedDrugOrder);
                }
            } else if (!canReviseOrder(matchingOrder)) {
                throw new IllegalArgumentException("This OpenMRS drug order is already stopped or expired. Only billing exclusion can be changed for this line.");
            } else {
                Date replacementDate = resolveOrderStartDate(request.getEncounter(), line.getStartDate());
                String replacementReason = "Replaced by edited pharmacy prescription";
                discontinueIfActive(matchingOrder, replacementReason, replacementDate,
                        orderingProvider, request.getEncounter());
                discontinueActiveSameOrderableOrders(request.getPatient(), drug, replacementDate,
                        orderingProvider, request.getEncounter(), matchingOrder.getOrderId(), "Replaced by newer pharmacy prescription");
                DrugOrder newOrder = buildDrugOrder(request.getPatient(), request.getEncounter(), orderingProvider,
                        drug, line, requestedQuantity, orderSortWeight(matchingOrder));
                Order savedOrder = Context.getOrderService().saveOrder(newOrder, (OrderContext) null);
                DrugOrder savedDrugOrder = savedOrder instanceof DrugOrder ? (DrugOrder) savedOrder : newOrder;
                requestLine.setOrder(savedDrugOrder);
                if (request.getOrder() != null && request.getOrder().getOrderId() != null
                        && matchingOrder.getOrderId() != null
                        && request.getOrder().getOrderId().equals(matchingOrder.getOrderId())) {
                    request.setOrder(savedDrugOrder);
                }
            }
        }

        if (!billingOnlyEdit) {
            requestLine.setItem(localItem);
            requestLine.setRequestedQuantity(requestedQuantity);
        }
        applyLineBillingControl(requestLine, line);
        applyHiddenItemBillingControl(requestLine);
        org.openmrs.module.rwandapharmacymanagement.model.Item statusItem = requestLine.getItem();
        if (!Boolean.TRUE.equals(requestLine.getBillable()) || !isAvailableInHospitalStock(statusItem)) {
            requestLine.setLineStatus(RequestStatus.PENDING_DISPENSE);
        } else if (!Boolean.TRUE.equals(request.getPaymentConfirmed())) {
            requestLine.setLineStatus(RequestStatus.PENDING_PAYMENT);
        } else if (!isFinalStatus(requestLine.getLineStatus())) {
            requestLine.setLineStatus(RequestStatus.PENDING_DISPENSE);
        }
        refreshBillingStateAfterLineChange(request);
        reconcileEditedPrescriptionBilling(request, previousBillableLine, matchingOrder, wasBillableLine, requestLine);
        refreshPrescriptionBillLinkAfterLineChange(request, wasBillableLine, previousBillableLine, matchingOrder,
                "Prescription billing reconciliation: item changed");
        updateRelatedBillingDrugFrequency(request);
        PatientDispensingRequest savedRequest = dispensingService.saveRequest(request);
        scheduleDuplicatePrescriptionBillCleanupAfterCommit(savedRequest, relatedPatientBillId);
        schedulePrescriptionBillRelinkAfterCommit(savedRequest);
        queuePrescriptionLineUpdatedCommunications(request, requestLine);
        return savedRequest;
    }

    private boolean canReviseOrder(DrugOrder order) {
        if (order == null || Boolean.TRUE.equals(order.getVoided())) {
            return false;
        }
        Date now = new Date();
        return order.isActive(now);
    }

    private PatientDispensingRequestLine billingMatchSnapshot(PatientDispensingRequestLine source, DrugOrder matchingOrder) {
        PatientDispensingRequestLine snapshot = new PatientDispensingRequestLine();
        if (source != null) {
            snapshot.setPatientDispensingRequestLineId(source.getPatientDispensingRequestLineId());
            snapshot.setItem(source.getItem());
            snapshot.setRequestedQuantity(source.getRequestedQuantity());
            snapshot.setUnitOfMeasure(source.getUnitOfMeasure());
            snapshot.setBillable(source.getBillable());
            snapshot.setOrder(source.getOrder());
        }
        if (matchingOrder != null) {
            snapshot.setOrder(matchingOrder);
        }
        return snapshot;
    }

    private void reconcileEditedPrescriptionBilling(PatientDispensingRequest request,
                                                    PatientDispensingRequestLine previousBillableLine,
                                                    DrugOrder previousOrder,
                                                    boolean wasBillableLine,
                                                    PatientDispensingRequestLine currentLine) {
        if (request == null || currentLine == null) {
            return;
        }
        boolean nowBillable = Boolean.TRUE.equals(currentLine.getBillable());
        if (wasBillableLine && nowBillable && isAvailableInHospitalStock(currentLine.getItem())) {
            boolean reconciled = updateRelatedBillingItem(request, previousBillableLine, previousOrder, currentLine);
            if (!reconciled) {
                log.warn("Prescription billing reconciliation did not find a matching bill item to update. requestId="
                        + request.getPatientDispensingRequestId() + ", requestLineId="
                        + currentLine.getPatientDispensingRequestLineId());
            }
            return;
        }
        if (!wasBillableLine) {
            return;
        }
        if (nowBillable) {
            return;
        }
        String reason = "Prescription billing reconciliation: item marked not billable"
                + (hasText(currentLine.getBillingExclusionReason())
                ? " - " + currentLine.getBillingExclusionReason() : "");
        boolean reconciled = voidRelatedBillingItems(request, previousBillableLine, previousOrder, reason)
                || reconcileNotBillablePrescriptionBilling(request, request.getDateCreated());
        if (!reconciled) {
            log.warn("Prescription billing reconciliation did not find a matching bill item. requestId="
                    + request.getPatientDispensingRequestId() + ", requestLineId="
                    + currentLine.getPatientDispensingRequestLineId() + ", reason=" + reason);
        }
    }

    private void applyLineBillingControl(PatientDispensingRequestLine requestLine, PrescriptionLineCommand command) {
        boolean billable = command == null || !Boolean.FALSE.equals(command.getBillable());
        String reason = command == null ? null : trimToNull(command.getBillingExclusionReason());
        if (!billable && reason == null) {
            throw new IllegalArgumentException("Reason is required when a prescription item is marked not billable");
        }
        requestLine.setBillable(Boolean.valueOf(billable));
        requestLine.setBillingExclusionReason(billable ? null : reason);
    }

    private void applyHiddenItemBillingControl(PatientDispensingRequestLine requestLine) {
        if (requestLine == null || requestLine.getItem() == null
                || Boolean.TRUE.equals(requestLine.getItem().getVisibleToRequesters())) {
            return;
        }
        requestLine.setBillable(Boolean.FALSE);
        if (!hasText(requestLine.getBillingExclusionReason())) {
            requestLine.setBillingExclusionReason("Item hidden from requesters");
        }
    }

    private void refreshBillingStateAfterLineChange(PatientDispensingRequest request) {
        if (request == null || request.getLines() == null) {
            return;
        }
        boolean hasActiveLine = false;
        boolean hasBillableLine = false;
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided()) || isFinalStatus(line.getLineStatus())) {
                continue;
            }
            hasActiveLine = true;
            boolean billableLine = Boolean.TRUE.equals(line.getBillable())
                    && line.getItem() != null && isAvailableInHospitalStock(line.getItem());
            hasBillableLine = hasBillableLine || billableLine;
        }
        if (!hasActiveLine) {
            return;
        }
        request.setBillable(Boolean.valueOf(hasBillableLine));
        if (!hasBillableLine) {
            request.setPaymentConfirmed(Boolean.TRUE);
            request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
            updateEditableLineStatuses(request, RequestStatus.PENDING_DISPENSE);
            return;
        }
        Boolean paid = request.getPatientBillId() == null ? Boolean.FALSE
                : dispensingService.isPatientBillPaid(request.getPatientBillId());
        request.setPaymentConfirmed(Boolean.TRUE.equals(paid));
        request.setRequestStatus(Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        updateEditableLineStatuses(request, Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
    }

    private void updateEditableLineStatuses(PatientDispensingRequest request, RequestStatus billableStatus) {
        if (request == null || request.getLines() == null) {
            return;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided()) || isFinalStatus(line.getLineStatus())) {
                continue;
            }
            boolean billableLine = Boolean.TRUE.equals(line.getBillable())
                    && line.getItem() != null && isAvailableInHospitalStock(line.getItem());
            line.setLineStatus(billableLine ? billableStatus : RequestStatus.PENDING_DISPENSE);
        }
    }

    /**
     * Queues the HIE and SMS messages inside this transaction; the HIE and SMS scheduler tasks send them, so the
     * user does not wait on the network. After commit only the patient bill link runs, which does nothing when the
     * bill was already found before commit.
     */
    private void queuePrescriptionSavedCommunications(final PatientDispensingRequest request, final boolean sendSms) {
        if (prescriptionCommunicationService == null || request == null) {
            return;
        }
        prescriptionCommunicationService.queuePrescriptionSaved(request, sendSms);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            PatientDispensingRequest linkedRequest = linkCommittedPrescriptionBill(request);
            schedulePrescriptionBillLinkRetries(linkedRequest == null ? request : linkedRequest, 2);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                PatientDispensingRequest linkedRequest = linkCommittedPrescriptionBillInSingleTransaction(request, 1);
                schedulePrescriptionBillLinkRetries(linkedRequest == null ? request : linkedRequest, 2);
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
            boolean reconciledNotBillable = reconcileNotBillablePrescriptionBilling(persistedRequest, requestDate);
            Set<String> itemNames = billingLookupNames(persistedRequest);
            if (itemNames.isEmpty()) {
                persistedRequest.setBillable(Boolean.FALSE);
                persistedRequest.setPaymentConfirmed(Boolean.TRUE);
                persistedRequest.setRequestStatus(RequestStatus.PENDING_DISPENSE);
                PatientDispensingRequest savedRequest = dispensingService.saveRequest(persistedRequest);
                log.info("Prescription request has no available billable lines. requestId="
                        + savedRequest.getPatientDispensingRequestId() + ", reconciledNotBillable="
                        + reconciledNotBillable);
                return savedRequest;
            }
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
                    if (line == null || isFinalStatus(line.getLineStatus())
                            || line.getLineStatus() != RequestStatus.PENDING_PAYMENT) {
                        continue;
                    }
                    line.setLineStatus(Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
                }
            }
            updateRelatedBillingDrugFrequency(persistedRequest);
            PatientDispensingRequest savedRequest = dispensingService.saveRequest(persistedRequest);
            log.info("Linked prescription request to patient bill. requestId="
                    + savedRequest.getPatientDispensingRequestId() + ", patientBillId=" + patientBillId
                    + ", paid=" + Boolean.TRUE.equals(paid) + ", attempt=" + attempt
                    + ", reconciledNotBillable=" + reconciledNotBillable);
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
                || status == RequestStatus.NOT_SERVED
                || status == RequestStatus.CANCELLED;
    }

    private HiePrescriptionIdentity replaceActiveSameDrugOrder(Patient patient,
                                                               Drug drug,
                                                               Encounter encounter,
                                                               Provider provider,
                                                               Date replacementDate) {
        List<DrugOrder> activeOrders = findActiveSameOrderableOrders(patient, drug, replacementDate, null);
        if (activeOrders.isEmpty()) {
            return null;
        }
        HiePrescriptionIdentity identity = null;
        String reason = "Replaced by newer pharmacy prescription";
        Date discontinuedDate = replacementDate == null ? new Date() : replacementDate;
        for (DrugOrder activeOrder : activeOrders) {
            if (identity == null) {
                identity = findOpenRequestLineHieIdentity(activeOrder);
            }
            discontinueIfActive(activeOrder, reason, discontinuedDate, provider, encounter);
            cancelOpenRequestLinesForReplacedOrder(activeOrder, reason);
        }
        return identity;
    }

    private void discontinueActiveSameOrderableOrders(Patient patient,
                                                      Drug drug,
                                                      Date activeDate,
                                                      Provider provider,
                                                      Encounter encounter,
                                                      Integer excludedOrderId,
                                                      String reason) {
        List<DrugOrder> activeOrders = findActiveSameOrderableOrders(patient, drug, activeDate, excludedOrderId);
        if (activeOrders.isEmpty()) {
            return;
        }
        Date discontinuedDate = activeDate == null ? new Date() : activeDate;
        for (DrugOrder activeOrder : activeOrders) {
            discontinueIfActive(activeOrder, reason, discontinuedDate, provider, encounter);
            cancelOpenRequestLinesForReplacedOrder(activeOrder, reason);
        }
    }

    private List<DrugOrder> findActiveSameOrderableOrders(Patient patient, Drug drug, Date activeDate,
                                                          Integer excludedOrderId) {
        List<DrugOrder> matches = new ArrayList<DrugOrder>();
        if (patient == null || drug == null || drug.getConcept() == null || drug.getConcept().getConceptId() == null) {
            return matches;
        }
        Date date = activeDate == null ? new Date() : activeDate;
        List<Order> activeOrders = Context.getOrderService().getActiveOrders(patient, resolveDrugOrderType(drug),
                resolveCareSetting(), date);
        if (activeOrders == null || activeOrders.isEmpty()) {
            return matches;
        }
        for (Order order : activeOrders) {
            if (!(order instanceof DrugOrder) || Boolean.TRUE.equals(order.getVoided()) || !order.isActive(date)) {
                continue;
            }
            if (excludedOrderId != null && excludedOrderId.equals(order.getOrderId())) {
                continue;
            }
            DrugOrder drugOrder = (DrugOrder) order;
            if (sameOrderable(drugOrder, drug)) {
                matches.add(drugOrder);
            }
        }
        return matches;
    }

    private void discontinueIfActive(DrugOrder order, String reason, Date discontinuedDate,
                                    Provider provider, Encounter encounter) {
        if (order == null || Boolean.TRUE.equals(order.getVoided()) || !order.isActive(discontinuedDate)) {
            return;
        }
        try {
            Context.getOrderService().discontinueOrder(order, reason, discontinuedDate, provider, encounter);
        }
        catch (RuntimeException e) {
            if (!Boolean.TRUE.equals(order.getVoided()) && order.isActive(discontinuedDate)) {
                throw e;
            }
            log.info("Skipping discontinuation because drug order is no longer active. orderId="
                    + order.getOrderId());
        }
    }

    private boolean sameOrderable(DrugOrder order, Drug drug) {
        if (order == null || drug == null || drug.getConcept() == null || drug.getConcept().getConceptId() == null) {
            return false;
        }
        Drug orderDrug = order.getDrug();
        if (orderDrug != null && orderDrug.getDrugId() != null && drug.getDrugId() != null) {
            return drug.getDrugId().equals(orderDrug.getDrugId());
        }
        Concept orderConcept = order.getConcept();
        if (orderConcept == null && orderDrug != null) {
            orderConcept = orderDrug.getConcept();
        }
        return orderConcept != null && orderConcept.getConceptId() != null
                && drug.getConcept().getConceptId().equals(orderConcept.getConceptId());
    }

    private int orderSortWeight(DrugOrder order) {
        if (order == null || order.getSortWeight() == null) {
            return 1;
        }
        return Math.max(1, order.getSortWeight().intValue());
    }

    private HiePrescriptionIdentity findOpenRequestLineHieIdentity(DrugOrder replacedOrder) {
        if (replacedOrder == null || replacedOrder.getOrderId() == null) {
            return null;
        }
        List<PatientDispensingRequest> pendingRequests = dispensingService.getPendingRequests();
        if (pendingRequests == null || pendingRequests.isEmpty()) {
            return null;
        }
        for (PatientDispensingRequest request : pendingRequests) {
            if (request == null || request.getLines() == null || Boolean.TRUE.equals(request.getVoided())) {
                continue;
            }
            for (PatientDispensingRequestLine line : request.getLines()) {
                if (isOpenLineForOrder(line, replacedOrder)) {
                    return new HiePrescriptionIdentity(resolveHiePrescriptionGroupId(request),
                            resolveHieMedicationRequestId(line));
                }
            }
        }
        return null;
    }

    private void initializeHiePrescriptionIdentity(PatientDispensingRequest request, String inheritedGroupId) {
        if (request == null) {
            return;
        }
        if (!hasText(request.getHiePrescriptionGroupId())) {
            request.setHiePrescriptionGroupId(hasText(inheritedGroupId) ? inheritedGroupId : resolveHiePrescriptionGroupId(request));
        }
        if (request.getLines() == null) {
            return;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line != null && !hasText(line.getHieMedicationRequestId())) {
                line.setHieMedicationRequestId(resolveHieMedicationRequestId(line));
            }
        }
    }

    private String resolveHiePrescriptionGroupId(PatientDispensingRequest request) {
        if (request == null) {
            return null;
        }
        if (hasText(request.getHiePrescriptionGroupId())) {
            return request.getHiePrescriptionGroupId();
        }
        if (request.getPatientDispensingRequestId() != null) {
            return "EP-unknown-" + request.getPatientDispensingRequestId();
        }
        return null;
    }

    private String resolveHieMedicationRequestId(PatientDispensingRequestLine line) {
        if (line == null) {
            return null;
        }
        if (hasText(line.getHieMedicationRequestId())) {
            return line.getHieMedicationRequestId();
        }
        return line.getUuid();
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class HiePrescriptionIdentity {
        private final String hiePrescriptionGroupId;
        private final String hieMedicationRequestId;

        private HiePrescriptionIdentity(String hiePrescriptionGroupId, String hieMedicationRequestId) {
            this.hiePrescriptionGroupId = hiePrescriptionGroupId;
            this.hieMedicationRequestId = hieMedicationRequestId;
        }

        private String getHiePrescriptionGroupId() {
            return hiePrescriptionGroupId;
        }

        private String getHieMedicationRequestId() {
            return hieMedicationRequestId;
        }
    }

    private void cancelOpenRequestLinesForReplacedOrder(DrugOrder replacedOrder, String reason) {
        if (replacedOrder == null || replacedOrder.getOrderId() == null) {
            return;
        }
        List<PatientDispensingRequest> pendingRequests = dispensingService.getPendingRequests();
        if (pendingRequests == null || pendingRequests.isEmpty()) {
            return;
        }
        for (PatientDispensingRequest request : pendingRequests) {
            if (request == null || request.getLines() == null || Boolean.TRUE.equals(request.getVoided())) {
                continue;
            }
            boolean changed = false;
            for (PatientDispensingRequestLine line : request.getLines()) {
                if (!isOpenLineForOrder(line, replacedOrder)) {
                    continue;
                }
                line.setLineStatus(RequestStatus.CANCELLED);
                line.setVoided(Boolean.TRUE);
                line.setVoidedBy(Context.getAuthenticatedUser());
                line.setDateVoided(new Date());
                line.setVoidReason(reason);
                voidRelatedBillingItems(request, line, replacedOrder, reason);
                queuePrescriptionLineUpdatedCommunications(request, line);
                changed = true;
            }
            if (changed) {
                updateRequestStatusAfterLineChange(request, reason);
                dispensingService.saveRequest(request);
            }
        }
    }

    private boolean isOpenLineForOrder(PatientDispensingRequestLine line, DrugOrder order) {
        if (line == null || order == null || order.getOrderId() == null || Boolean.TRUE.equals(line.getVoided())
                || isFinalStatus(line.getLineStatus()) || line.getOrder() == null || line.getOrder().getOrderId() == null) {
            return false;
        }
        return order.getOrderId().equals(line.getOrder().getOrderId());
    }

    /**
     * Queues the updated line's HIE messages inside this transaction; the HIE scheduler task sends them.
     */
    private void queuePrescriptionLineUpdatedCommunications(final PatientDispensingRequest request,
                                                            final PatientDispensingRequestLine requestLine) {
        if (prescriptionCommunicationService == null || request == null || requestLine == null) {
            return;
        }
        prescriptionCommunicationService.queuePrescriptionLineUpdated(request, requestLine);
    }

    @Override
    public PatientDispensingRequest voidPrescriptionLine(Integer requestId,
                                                         Integer requestLineId,
                                                         String voidReason) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION);
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
        if (isPrescriptionPaymentConfirmed(request)) {
            throw new IllegalArgumentException("This prescription is already paid and cannot be edited");
        }
        Integer relatedPatientBillId = request.getPatientBillId();
        PatientDispensingRequestLine requestLine = findEditableRequestLine(request, requestLineId);
        DrugOrder matchingOrder = findMatchingOrder(request, requestLine);
        if (matchingOrder != null && !Boolean.TRUE.equals(matchingOrder.getVoided())
                && canReviseOrder(matchingOrder)) {
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
        refreshPrescriptionBillLinkAfterLineChange(request, true, requestLine, matchingOrder,
                "Prescription billing reconciliation: item deleted - " + reason);
        PatientDispensingRequest savedRequest = dispensingService.saveRequest(request);
        scheduleDuplicatePrescriptionBillCleanupAfterCommit(savedRequest, relatedPatientBillId);
        schedulePrescriptionBillRelinkAfterCommit(savedRequest);
        queuePrescriptionLineUpdatedCommunications(request, requestLine);
        return savedRequest;
    }

    private void scheduleDuplicatePrescriptionBillCleanupAfterCommit(final PatientDispensingRequest request) {
        scheduleDuplicatePrescriptionBillCleanupAfterCommit(request, request == null ? null : request.getPatientBillId());
    }

    private void scheduleDuplicatePrescriptionBillCleanupAfterCommit(final PatientDispensingRequest request,
                                                                    final Integer relatedPatientBillId) {
        if (request == null || request.getPatientDispensingRequestId() == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            voidDuplicatePrescriptionBillsInSingleTransaction(request, relatedPatientBillId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                voidDuplicatePrescriptionBillsInSingleTransaction(request, relatedPatientBillId);
            }
        });
    }

    private void voidDuplicatePrescriptionBillsInSingleTransaction(final PatientDispensingRequest request) {
        voidDuplicatePrescriptionBillsInSingleTransaction(request, request == null ? null : request.getPatientBillId());
    }

    private void voidDuplicatePrescriptionBillsInSingleTransaction(final PatientDispensingRequest request,
                                                                  final Integer relatedPatientBillId) {
        if (transactionManager == null) {
            voidDuplicatePrescriptionBills(request, relatedPatientBillId);
            return;
        }
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
                voidDuplicatePrescriptionBills(request, relatedPatientBillId);
                return null;
            }
        });
    }

    private void refreshPrescriptionBillLinkAfterLineChange(PatientDispensingRequest request,
                                                            boolean hadBillableLineBeforeChange,
                                                            PatientDispensingRequestLine previousBillableLine,
                                                            DrugOrder previousOrder,
                                                            String reason) {
        if (request == null) {
            return;
        }
        if (!hasActiveBillablePrescriptionLine(request)) {
            request.setPatientBillId(null);
            request.setBillable(Boolean.FALSE);
            request.setPaymentConfirmed(Boolean.TRUE);
            if (request.getRequestStatus() == RequestStatus.PENDING_PAYMENT) {
                request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
            }
            updateEditableLineStatuses(request, RequestStatus.PENDING_DISPENSE);
            return;
        }
        request.setBillable(Boolean.TRUE);
        Boolean paid = request.getPatientBillId() == null ? Boolean.FALSE
                : dispensingService.isPatientBillPaid(request.getPatientBillId());
        request.setPaymentConfirmed(Boolean.TRUE.equals(paid));
        request.setRequestStatus(Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        updateEditableLineStatuses(request, Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE
                : RequestStatus.PENDING_PAYMENT);
    }

    private boolean hasActiveBillablePrescriptionLine(PatientDispensingRequest request) {
        if (request == null || request.getLines() == null) {
            return false;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided()) || isFinalStatus(line.getLineStatus())) {
                continue;
            }
            if (Boolean.TRUE.equals(line.getBillable()) && isAvailableInHospitalStock(line.getItem())) {
                return true;
            }
        }
        return false;
    }

    private void schedulePrescriptionBillRelinkAfterCommit(final PatientDispensingRequest request) {
        if (request == null || request.getPatientDispensingRequestId() == null || request.getPatientBillId() != null
                || !Boolean.TRUE.equals(request.getBillable())) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            PatientDispensingRequest linkedRequest = linkCommittedPrescriptionBillInSingleTransaction(request, 1);
            schedulePrescriptionBillLinkRetries(linkedRequest == null ? request : linkedRequest, 2);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                PatientDispensingRequest linkedRequest = linkCommittedPrescriptionBillInSingleTransaction(request, 1);
                schedulePrescriptionBillLinkRetries(linkedRequest == null ? request : linkedRequest, 2);
            }
        });
    }

    private void voidDuplicatePrescriptionBills(PatientDispensingRequest request) {
        voidDuplicatePrescriptionBills(request, request == null ? null : request.getPatientBillId());
    }

    private void voidDuplicatePrescriptionBills(PatientDispensingRequest request, Integer relatedPatientBillId) {
        if (request == null || request.getPatientDispensingRequestId() == null || request.getPatient() == null) {
            return;
        }
        PatientDispensingRequest persistedRequest = dispensingService.getRequest(request.getPatientDispensingRequestId());
        if (persistedRequest == null || persistedRequest.getPatient() == null) {
            return;
        }
        Set<String> activeRequestNames = activeBillableRequestLineNames(persistedRequest);
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return;
            }
            Method getPolicies = billingServiceClass.getMethod("getAllInsurancePoliciesByPatient", Patient.class);
            Object policyResult = getPolicies.invoke(billingService, persistedRequest.getPatient());
            if (!(policyResult instanceof Iterable)) {
                return;
            }
            Method getConsommationsByBeneficiary = billingServiceClass.getMethod("getConsommationsByBeneficiary",
                    Context.loadClass("org.openmrs.module.mohbilling.model.Beneficiary"));
            Date requestDate = persistedRequest.getDateCreated();
            Date cleanupDate = new Date();
            Integer keptPatientBillId = persistedRequest.getPatientBillId();
            String reason = keptPatientBillId == null
                    ? "Empty pharmacy prescription bill cleanup"
                    : "Duplicate pharmacy prescription bill merged into bill " + keptPatientBillId;
            int voidedBills = 0;
            for (Object policy : (Iterable<?>) policyResult) {
                Iterable<?> beneficiaries = extractIterable(policy, "getBeneficiaries");
                if (beneficiaries == null) {
                    continue;
                }
                for (Object beneficiary : beneficiaries) {
                    if (!matchesPatient(beneficiary, persistedRequest.getPatient())) {
                        continue;
                    }
                    Object consommations = getConsommationsByBeneficiary.invoke(billingService, beneficiary);
                    if (!(consommations instanceof Iterable)) {
                        continue;
                    }
                    for (Object consommation : (Iterable<?>) consommations) {
                        Object patientBill = invokeNoArg(consommation, "getPatientBill");
                        Integer candidateBillId = extractPatientBillId(patientBill);
                        if (!isRecentBillingCandidate(consommation, patientBill, requestDate, cleanupDate)
                                || extractBoolean(consommation, "getVoided")) {
                            continue;
                        }
                        if (!hasActiveBillItems(consommation) && isPharmacyRelatedBillCandidate(candidateBillId,
                                keptPatientBillId, relatedPatientBillId)) {
                            voidEmptyBillingContainers(billingServiceClass, billingService, patientBillClass,
                                    consommationClass, patientBill, consommation,
                                    "Empty pharmacy prescription consommation after bill item void/delete");
                            voidedBills++;
                            continue;
                        }
                        if (keptPatientBillId == null || activeRequestNames.isEmpty()
                                || candidateBillId == null || candidateBillId.equals(keptPatientBillId)
                                || candidateBillId.compareTo(keptPatientBillId) <= 0
                                || extractBoolean(patientBill, "getVoided") || extractBoolean(patientBill, "getIsPaid")) {
                            continue;
                        }
                        if (!containsOnlyActiveItemsMatchingNames(consommation, activeRequestNames)) {
                            continue;
                        }
                        BigDecimal previousActiveTotal = activeBillItemsTotal(safeCastSet(invokeNoArg(consommation,
                                "getBillItems")));
                        voidAllActiveBillItems(billingServiceClass, billingService, patientServiceBillClass,
                                consommation, reason);
                        recalculateBillingTotalsAfterActiveItemChange(billingServiceClass, billingService,
                                patientBillClass, patientBill, consommation, previousActiveTotal, BigDecimal.ZERO);
                        voidEmptyBillingContainers(billingServiceClass, billingService, patientBillClass,
                                consommationClass, patientBill, consommation, reason);
                        voidedBills++;
                    }
                }
            }
            if (voidedBills > 0) {
                log.info("Voided duplicate prescription bill(s). requestId="
                        + persistedRequest.getPatientDispensingRequestId() + ", keptPatientBillId="
                        + persistedRequest.getPatientBillId() + ", duplicateBillsVoided=" + voidedBills);
            }
        }
        catch (Exception e) {
            log.warn("Failed to void duplicate prescription bills. requestId="
                    + persistedRequest.getPatientDispensingRequestId() + ", patientBillId="
                    + persistedRequest.getPatientBillId(), e);
        }
    }

    private Set<String> activeBillableRequestLineNames(PatientDispensingRequest request) {
        Set<String> names = new HashSet<String>();
        if (request == null || request.getLines() == null) {
            return names;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided()) || isFinalStatus(line.getLineStatus())
                    || !Boolean.TRUE.equals(line.getBillable()) || !isAvailableInHospitalStock(line.getItem())) {
                continue;
            }
            names.addAll(requestLineNames(line, line.getOrder()));
        }
        return names;
    }

    private boolean hasActiveBillItems(Object consommation) {
        Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
        if (billItems == null || billItems.isEmpty()) {
            return false;
        }
        for (Object billItem : billItems) {
            if (billItem != null && !extractBoolean(billItem, "getVoided")) {
                return true;
            }
        }
        return false;
    }

    private boolean isPharmacyRelatedBillCandidate(Integer candidateBillId,
                                                   Integer keptPatientBillId,
                                                   Integer relatedPatientBillId) {
        if (candidateBillId == null) {
            return false;
        }
        Integer referenceBillId = relatedPatientBillId == null ? keptPatientBillId : relatedPatientBillId;
        if (referenceBillId == null) {
            return false;
        }
        return candidateBillId.equals(referenceBillId) || candidateBillId.compareTo(referenceBillId) > 0;
    }

    private boolean containsOnlyActiveItemsMatchingNames(Object consommation, Set<String> activeRequestNames) {
        Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
        if (billItems == null || billItems.isEmpty() || activeRequestNames == null || activeRequestNames.isEmpty()) {
            return false;
        }
        boolean hasActiveItem = false;
        for (Object billItem : billItems) {
            if (billItem == null || extractBoolean(billItem, "getVoided")) {
                continue;
            }
            hasActiveItem = true;
            if (!billItemMatchesAnyName(billItem, activeRequestNames)) {
                return false;
            }
        }
        return hasActiveItem;
    }

    private void voidAllActiveBillItems(Class<?> billingServiceClass,
                                        Object billingService,
                                        Class<?> patientServiceBillClass,
                                        Object consommation,
                                        String reason) throws Exception {
        Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
        if (billItems == null) {
            return;
        }
        for (Object billItem : billItems) {
            if (billItem == null || extractBoolean(billItem, "getVoided")) {
                continue;
            }
            invokeSetter(billItem, "setVoided", Boolean.TRUE, Boolean.class);
            invokeSetter(billItem, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
            invokeSetter(billItem, "setVoidedDate", new Date(), Date.class);
            invokeSetter(billItem, "setVoidReason", reason, String.class);
            billingServiceClass.getMethod("saveBilledItem", patientServiceBillClass).invoke(billingService, billItem);
        }
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

    private boolean isPrescriptionPaymentConfirmed(PatientDispensingRequest request) {
        if (request == null) {
            return false;
        }
        if (Boolean.TRUE.equals(request.getPaymentConfirmed())) {
            return true;
        }
        return request.getPatientBillId() != null && Boolean.TRUE.equals(dispensingService.isPatientBillPaid(request.getPatientBillId()));
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

    private void updateRelatedBillingDrugFrequency(PatientDispensingRequest request) {
        if (request == null || request.getPatientBillId() == null || request.getLines() == null
                || request.getLines().isEmpty()) {
            return;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
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

            int updated = 0;
            for (Object billItem : billItems) {
                if (billItem == null || extractBoolean(billItem, "getVoided")) {
                    continue;
                }
                PatientDispensingRequestLine matchingLine = matchingPrescriptionLine(billItem, request.getLines());
                if (matchingLine == null || !(matchingLine.getOrder() instanceof DrugOrder)) {
                    continue;
                }
                String drugFrequency = billingInstructionText((DrugOrder) matchingLine.getOrder());
                if (drugFrequency.isEmpty()) {
                    continue;
                }
                if (setDrugFrequencyOnBillingItem(billingServiceClass, billingService, patientServiceBillClass,
                        billItem, drugFrequency)) {
                    updated++;
                }
            }
            if (updated > 0) {
                log.info("Updated prescription drug frequency on billing items. requestId="
                        + request.getPatientDispensingRequestId() + ", patientBillId=" + request.getPatientBillId()
                        + ", updatedItems=" + updated);
            }
        }
        catch (Exception ex) {
            log.warn("Unable to update prescription drug frequency on billing items. requestId="
                    + request.getPatientDispensingRequestId() + ", patientBillId=" + request.getPatientBillId(), ex);
        }
    }

    private PatientDispensingRequestLine matchingPrescriptionLine(Object billItem,
                                                                 List<PatientDispensingRequestLine> requestLines) {
        if (billItem == null || requestLines == null) {
            return null;
        }
        for (PatientDispensingRequestLine requestLine : requestLines) {
            if (requestLine == null || !(requestLine.getOrder() instanceof DrugOrder)) {
                continue;
            }
            if (matchesBillingItem(billItem, requestLine, requestLine.getOrder())) {
                return requestLine;
            }
        }
        return null;
    }

    private String billingInstructionText(DrugOrder order) {
        if (order == null) {
            return "";
        }
        String instructions = safe(order.getDosingInstructions());
        if (!instructions.isEmpty()) {
            return instructions;
        }
        return safe(order.getInstructions());
    }

    private boolean setDrugFrequencyOnBillingItem(Class<?> billingServiceClass, Object billingService,
                                                  Class<?> patientServiceBillClass, Object billItem,
                                                  String drugFrequency) throws Exception {
        if (invokeSetterIfPresent(billItem, "setDrugFrequency", drugFrequency, String.class)) {
            billingServiceClass.getMethod("saveBilledItem", patientServiceBillClass).invoke(billingService, billItem);
            return true;
        }
        Integer patientServiceBillId = extractInteger(billItem, "getPatientServiceBillId");
        if (patientServiceBillId == null) {
            patientServiceBillId = extractInteger(billItem, "getId");
        }
        if (patientServiceBillId == null) {
            return false;
        }
        Context.getAdministrationService().executeSQL("update moh_bill_patient_service_bill set drug_frequency = '"
                + sqlEscape(drugFrequency) + "' where patient_service_bill_id = " + patientServiceBillId, false);
        return true;
    }

    private String sqlEscape(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private boolean updateRelatedBillingItem(PatientDispensingRequest request,
                                             PatientDispensingRequestLine previousLine,
                                             DrugOrder previousOrder,
                                             PatientDispensingRequestLine currentLine) {
        if (request == null || request.getPatientBillId() == null || currentLine == null) {
            return false;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return false;
            }
            Object patientBill = billingServiceClass.getMethod("getPatientBill", Integer.class)
                    .invoke(billingService, request.getPatientBillId());
            if (patientBill == null || extractBoolean(patientBill, "getVoided")) {
                return false;
            }
            Object consommation = billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            if (consommation == null || extractBoolean(consommation, "getVoided")) {
                return false;
            }
            Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
            if (billItems == null || billItems.isEmpty()) {
                return false;
            }
            Object matchingBillItem = null;
            for (Object billItem : billItems) {
                if (matchesBillingItem(billItem, previousLine, previousOrder)
                        || matchesBillingItem(billItem, currentLine, currentLine.getOrder())) {
                    matchingBillItem = billItem;
                    break;
                }
            }
            if (matchingBillItem == null) {
                return false;
            }
            BigDecimal previousActiveTotal = activeBillItemsTotal(billItems);
            updatePrescriptionBillItemFields(billingServiceClass, billingService, patientServiceBillClass,
                    matchingBillItem, request, currentLine);
            BigDecimal currentActiveTotal = activeBillItemsTotal(billItems);
            recalculateBillingTotalsAfterActiveItemChange(billingServiceClass, billingService, patientBillClass,
                    patientBill, consommation, previousActiveTotal, currentActiveTotal);
            return true;
        }
        catch (Exception e) {
            log.warn("Failed to update prescription bill item. requestId="
                    + request.getPatientDispensingRequestId() + ", patientBillId=" + request.getPatientBillId(), e);
            return false;
        }
    }

    private void updatePrescriptionBillItemFields(Class<?> billingServiceClass,
                                                  Object billingService,
                                                  Class<?> patientServiceBillClass,
                                                  Object billItem,
                                                  PatientDispensingRequest request,
                                                  PatientDispensingRequestLine currentLine) throws Exception {
        if (billItem == null || currentLine == null) {
            return;
        }
        if (currentLine.getRequestedQuantity() != null) {
            invokeSetter(billItem, "setQuantity", currentLine.getRequestedQuantity(), BigDecimal.class);
        }
        ProductMasterItem productMasterItem = resolveProductMasterItem(currentLine);
        if (productMasterItem != null && productMasterItem.getItemPrice() != null) {
            invokeSetter(billItem, "setUnitPrice", productMasterItem.getItemPrice(), BigDecimal.class);
        }
        String itemName = currentLine.getItem() == null ? null : currentLine.getItem().getName();
        if (hasText(itemName)) {
            invokeSetter(billItem, "setServiceOther", itemName, String.class);
            String description = "Prescription request " + request.getPatientDispensingRequestId() + " - "
                    + itemName + prescriptionBillingInstructionSuffix(currentLine);
            invokeSetter(billItem, "setServiceOtherDescription", description, String.class);
        }
        if (currentLine.getOrder() instanceof DrugOrder) {
            String drugFrequency = billingInstructionText((DrugOrder) currentLine.getOrder());
            if (hasText(drugFrequency)) {
                setDrugFrequencyOnBillingItem(billingServiceClass, billingService, patientServiceBillClass,
                        billItem, drugFrequency);
                return;
            }
        }
        billingServiceClass.getMethod("saveBilledItem", patientServiceBillClass).invoke(billingService, billItem);
    }

    private String prescriptionBillingInstructionSuffix(PatientDispensingRequestLine line) {
        if (line == null || !(line.getOrder() instanceof DrugOrder)) {
            return "";
        }
        String instructions = billingInstructionText((DrugOrder) line.getOrder());
        return hasText(instructions) ? " - " + instructions : "";
    }

    private boolean voidRelatedBillingItems(PatientDispensingRequest request,
                                            PatientDispensingRequestLine requestLine,
                                            DrugOrder matchingOrder,
                                            String reason) {
        if (request == null || request.getPatientBillId() == null) {
            return false;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return false;
            }
            Object patientBill = billingServiceClass.getMethod("getPatientBill", Integer.class)
                    .invoke(billingService, request.getPatientBillId());
            if (patientBill == null) {
                return false;
            }
            Object consommation = billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            if (consommation == null) {
                return false;
            }
            Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
            if (billItems == null || billItems.isEmpty()) {
                return false;
            }
            boolean anyVoided = false;
            BigDecimal previousActiveTotal = activeBillItemsTotal(billItems);
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
                return false;
            }
            BigDecimal currentActiveTotal = activeBillItemsTotal(billItems);
            recalculateBillingTotalsAfterActiveItemChange(billingServiceClass, billingService, patientBillClass,
                    patientBill, consommation, previousActiveTotal, currentActiveTotal);
            boolean hasActiveBillItems = false;
            for (Object billItem : billItems) {
                if (!extractBoolean(billItem, "getVoided")) {
                    hasActiveBillItems = true;
                    break;
                }
            }
            if (!hasActiveBillItems) {
                voidEmptyBillingContainers(billingServiceClass, billingService, patientBillClass, consommationClass,
                        patientBill, consommation, reason);
            }
            return true;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private boolean reconcileNotBillablePrescriptionBilling(PatientDispensingRequest request, Date requestDate) {
        if (request == null || request.getPatient() == null || request.getLines() == null
                || request.getLines().isEmpty()) {
            return false;
        }
        List<PatientDispensingRequestLine> notBillableLines = notBillableLines(request);
        if (notBillableLines.isEmpty()) {
            return false;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return false;
            }
            boolean anyVoided = false;
            if (request.getPatientBillId() != null) {
                Object patientBill = billingServiceClass.getMethod("getPatientBill", Integer.class)
                        .invoke(billingService, request.getPatientBillId());
                Object consommation = patientBill == null ? null
                        : billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                        .invoke(billingService, patientBill);
                anyVoided = voidNotBillableLinesFromConsommation(billingServiceClass, billingService,
                        patientBillClass, consommationClass, patientServiceBillClass, patientBill, consommation,
                        notBillableLines, "linked bill");
            }

            Method getPolicies = billingServiceClass.getMethod("getAllInsurancePoliciesByPatient", Patient.class);
            Object policyResult = getPolicies.invoke(billingService, request.getPatient());
            if (!(policyResult instanceof Iterable)) {
                return anyVoided;
            }
            Method getConsommationsByBeneficiary = billingServiceClass.getMethod("getConsommationsByBeneficiary",
                    Context.loadClass("org.openmrs.module.mohbilling.model.Beneficiary"));
            for (Object policy : (Iterable<?>) policyResult) {
                Iterable<?> beneficiaries = extractIterable(policy, "getBeneficiaries");
                if (beneficiaries == null) {
                    continue;
                }
                for (Object beneficiary : beneficiaries) {
                    if (!matchesPatient(beneficiary, request.getPatient())) {
                        continue;
                    }
                    Object consommations = getConsommationsByBeneficiary.invoke(billingService, beneficiary);
                    if (!(consommations instanceof Iterable)) {
                        continue;
                    }
                    for (Object consommation : (Iterable<?>) consommations) {
                        Object patientBill = invokeNoArg(consommation, "getPatientBill");
                        Integer candidateBillId = extractPatientBillId(patientBill);
                        if (request.getPatientBillId() != null && request.getPatientBillId().equals(candidateBillId)) {
                            continue;
                        }
                        if (!isRecentBillingCandidate(consommation, patientBill, requestDate)) {
                            continue;
                        }
                        anyVoided = voidNotBillableLinesFromConsommation(billingServiceClass, billingService,
                                patientBillClass, consommationClass, patientServiceBillClass, patientBill, consommation,
                                notBillableLines, "recent bill") || anyVoided;
                    }
                }
            }
            return anyVoided;
        }
        catch (Exception e) {
            log.warn("Failed to reconcile not-billable prescription bill items. requestId="
                    + request.getPatientDispensingRequestId(), e);
            return false;
        }
    }

    private List<PatientDispensingRequestLine> notBillableLines(PatientDispensingRequest request) {
        List<PatientDispensingRequestLine> lines = new ArrayList<PatientDispensingRequestLine>();
        if (request == null || request.getLines() == null) {
            return lines;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            if (Boolean.FALSE.equals(line.getBillable()) || isHiddenRequestLine(line)) {
                lines.add(line);
            }
        }
        return lines;
    }

    private boolean isHiddenRequestLine(PatientDispensingRequestLine line) {
        return line != null && line.getItem() != null
                && !Boolean.TRUE.equals(line.getItem().getVisibleToRequesters());
    }

    private boolean voidNotBillableLinesFromConsommation(Class<?> billingServiceClass,
                                                        Object billingService,
                                                        Class<?> patientBillClass,
                                                        Class<?> consommationClass,
                                                        Class<?> patientServiceBillClass,
                                                        Object patientBill,
                                                        Object consommation,
                                                        List<PatientDispensingRequestLine> notBillableLines,
                                                        String source) throws Exception {
        if (patientBill == null || consommation == null || notBillableLines == null || notBillableLines.isEmpty()
                || extractBoolean(patientBill, "getVoided") || extractBoolean(consommation, "getVoided")) {
            return false;
        }
        Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
        if (billItems == null || billItems.isEmpty()) {
            return false;
        }
        boolean anyVoided = false;
        BigDecimal previousActiveTotal = activeBillItemsTotal(billItems);
        for (Object billItem : billItems) {
            if (billItem == null || extractBoolean(billItem, "getVoided")) {
                continue;
            }
            PatientDispensingRequestLine matchingLine = matchingNotBillableLine(billItem, notBillableLines);
            if (matchingLine == null) {
                continue;
            }
            String reason = "RPM not-billable prescription reconciliation"
                    + (hasText(matchingLine.getBillingExclusionReason())
                    ? " - " + matchingLine.getBillingExclusionReason() : "");
            invokeSetter(billItem, "setVoided", Boolean.TRUE, Boolean.class);
            invokeSetter(billItem, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
            invokeSetter(billItem, "setVoidedDate", new Date(), Date.class);
            invokeSetter(billItem, "setVoidReason", reason, String.class);
            billingServiceClass.getMethod("saveBilledItem", patientServiceBillClass).invoke(billingService, billItem);
            anyVoided = true;
            log.info("Voided not-billable prescription bill item from " + source + ". requestLineId="
                    + matchingLine.getPatientDispensingRequestLineId() + ", patientBillId="
                    + extractInteger(patientBill, "getPatientBillId") + ", reason=" + reason);
        }
        if (anyVoided) {
            BigDecimal currentActiveTotal = activeBillItemsTotal(billItems);
            recalculateBillingTotalsAfterActiveItemChange(billingServiceClass, billingService, patientBillClass,
                    patientBill, consommation, previousActiveTotal, currentActiveTotal);
            voidEmptyBillingContainers(billingServiceClass, billingService, patientBillClass, consommationClass,
                    patientBill, consommation, "RPM not-billable prescription reconciliation");
        }
        return anyVoided;
    }

    private PatientDispensingRequestLine matchingNotBillableLine(Object billItem,
                                                                 List<PatientDispensingRequestLine> notBillableLines) {
        if (billItem == null || notBillableLines == null) {
            return null;
        }
        for (PatientDispensingRequestLine line : notBillableLines) {
            if (line == null || !matchesBillingItem(billItem, line, line.getOrder())) {
                continue;
            }
            if (!billQuantityMatchesLine(billItem, line)) {
                continue;
            }
            return line;
        }
        return null;
    }

    private boolean billQuantityMatchesLine(Object billItem, PatientDispensingRequestLine line) {
        BigDecimal billQuantity = extractBigDecimal(billItem, "getQuantity");
        BigDecimal requestedQuantity = line == null ? null : line.getRequestedQuantity();
        if (billQuantity == null || requestedQuantity == null) {
            return true;
        }
        return billQuantity.compareTo(requestedQuantity) == 0;
    }

    private BigDecimal billItemTotal(Object billItem) {
        BigDecimal quantity = extractBigDecimal(billItem, "getQuantity");
        BigDecimal unitPrice = extractBigDecimal(billItem, "getUnitPrice");
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return quantity.multiply(unitPrice);
    }

    private BigDecimal activeBillItemsTotal(Set<?> billItems) {
        BigDecimal total = BigDecimal.ZERO;
        if (billItems == null) {
            return total;
        }
        for (Object billItem : billItems) {
            if (billItem == null || extractBoolean(billItem, "getVoided")) {
                continue;
            }
            total = total.add(billItemTotal(billItem));
        }
        return total;
    }

    private void recalculateBillingTotalsAfterActiveItemChange(Class<?> billingServiceClass,
                                                               Object billingService,
                                                               Class<?> patientBillClass,
                                                               Object patientBill,
                                                               Object consommation,
                                                               BigDecimal previousActiveTotal,
                                                               BigDecimal currentActiveTotal) throws Exception {
        BigDecimal oldTotal = previousActiveTotal == null ? BigDecimal.ZERO : previousActiveTotal;
        BigDecimal newTotal = currentActiveTotal == null ? BigDecimal.ZERO : currentActiveTotal;
        if (refreshBillAmountsWithBillingUtil(consommation)) {
            return;
        }
        Object insuranceBill = consommation == null ? null : invokeNoArg(consommation, "getInsuranceBill");
        Object thirdPartyBill = consommation == null ? null : invokeNoArg(consommation, "getThirdPartyBill");
        BigDecimal newInsuranceAmount = prorateBillingComponent(extractBigDecimal(insuranceBill, "getAmount"),
                oldTotal, newTotal);
        BigDecimal newThirdPartyAmount = prorateBillingComponent(extractBigDecimal(thirdPartyBill, "getAmount"),
                oldTotal, newTotal);
        BigDecimal newPatientAmount = prorateBillingComponent(extractBigDecimal(patientBill, "getAmount"),
                oldTotal, newTotal);
        newInsuranceAmount = newInsuranceAmount == null ? BigDecimal.ZERO : newInsuranceAmount;
        newThirdPartyAmount = newThirdPartyAmount == null ? BigDecimal.ZERO : newThirdPartyAmount;
        if (newPatientAmount == null) {
            newPatientAmount = subtractWithoutNegative(newTotal,
                    newInsuranceAmount.add(newThirdPartyAmount));
        }
        if (patientBill != null) {
            invokeSetter(patientBill, "setAmount", newPatientAmount, BigDecimal.class);
            billingServiceClass.getMethod("savePatientBill", patientBillClass).invoke(billingService, patientBill);
        }
        setBillAmount(billingServiceClass, billingService, insuranceBill,
                "org.openmrs.module.mohbilling.model.InsuranceBill", "saveInsuranceBill", newInsuranceAmount);
        setBillAmount(billingServiceClass, billingService, thirdPartyBill,
                "org.openmrs.module.mohbilling.model.ThirdPartyBill", "saveThirdPartyBill", newThirdPartyAmount);
        Object globalBill = consommation == null ? null : invokeNoArg(consommation, "getGlobalBill");
        if (globalBill != null) {
            BigDecimal currentGlobalAmount = extractBigDecimal(globalBill, "getGlobalAmount");
            if (currentGlobalAmount != null) {
                BigDecimal delta = newTotal.subtract(oldTotal);
                invokeSetter(globalBill, "setGlobalAmount", subtractWithoutNegative(currentGlobalAmount.add(delta),
                        BigDecimal.ZERO), BigDecimal.class);
                saveGlobalBillWithBillingUtil(globalBill,
                        Context.loadClass("org.openmrs.module.mohbilling.model.GlobalBill"));
            }
        }
    }

    private boolean refreshBillAmountsWithBillingUtil(Object consommation) {
        Object beneficiary = consommation == null ? null : invokeNoArg(consommation, "getBeneficiary");
        Object insurancePolicy = beneficiary == null ? null : invokeNoArg(beneficiary, "getInsurancePolicy");
        if (insurancePolicy == null) {
            return false;
        }
        try {
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> insurancePolicyClass = Context.loadClass("org.openmrs.module.mohbilling.model.InsurancePolicy");
            Class<?> consommationUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.ConsommationUtil");
            consommationUtilClass.getMethod("refreshBillAmountsFromNonVoidedItems", consommationClass,
                    insurancePolicyClass).invoke(null, consommation, insurancePolicy);
            return true;
        }
        catch (Exception e) {
            log.warn("Failed to refresh MOH Billing amounts with ConsommationUtil; using fallback calculation.", e);
            return false;
        }
    }

    private BigDecimal prorateBillingComponent(BigDecimal currentAmount, BigDecimal oldTotal, BigDecimal newTotal) {
        if (currentAmount == null) {
            return null;
        }
        BigDecimal denominator = oldTotal == null ? BigDecimal.ZERO : oldTotal;
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return currentAmount.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : newTotal;
        }
        return currentAmount.multiply(newTotal == null ? BigDecimal.ZERO : newTotal)
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private void setBillAmount(Class<?> billingServiceClass,
                               Object billingService,
                               Object bill,
                               String billClassName,
                               String saveMethodName,
                               BigDecimal amount) throws Exception {
        if (bill == null) {
            return;
        }
        invokeSetter(bill, "setAmount", amount == null ? BigDecimal.ZERO : amount, BigDecimal.class);
        billingServiceClass.getMethod(saveMethodName, Context.loadClass(billClassName)).invoke(billingService, bill);
    }

    private void setNestedBillingAmount(Class<?> billingServiceClass,
                                        Object billingService,
                                        Object consommation,
                                        String getterName,
                                        String billClassName,
                                        String saveMethodName,
                                        BigDecimal amount) throws Exception {
        Object nestedBill = consommation == null ? null : invokeNoArg(consommation, getterName);
        if (nestedBill == null) {
            return;
        }
        invokeSetter(nestedBill, "setAmount", amount == null ? BigDecimal.ZERO : amount, BigDecimal.class);
        billingServiceClass.getMethod(saveMethodName, Context.loadClass(billClassName)).invoke(billingService, nestedBill);
    }

    private void adjustBillingTotalsAfterVoids(Class<?> billingServiceClass,
                                               Object billingService,
                                               Class<?> patientBillClass,
                                               Object patientBill,
                                               Object consommation,
                                               BigDecimal voidedTotal) throws Exception {
        if (voidedTotal == null || voidedTotal.compareTo(BigDecimal.ZERO) <= 0 || patientBill == null) {
            return;
        }
        BigDecimal currentBillAmount = extractBigDecimal(patientBill, "getAmount");
        if (currentBillAmount != null) {
            invokeSetter(patientBill, "setAmount", subtractWithoutNegative(currentBillAmount, voidedTotal),
                    BigDecimal.class);
            billingServiceClass.getMethod("savePatientBill", patientBillClass).invoke(billingService, patientBill);
        }
        adjustNestedBillingAmount(billingServiceClass, billingService, consommation, "getInsuranceBill",
                "org.openmrs.module.mohbilling.model.InsuranceBill", "saveInsuranceBill", voidedTotal);
        adjustNestedBillingAmount(billingServiceClass, billingService, consommation, "getThirdPartyBill",
                "org.openmrs.module.mohbilling.model.ThirdPartyBill", "saveThirdPartyBill", voidedTotal);
        Object globalBill = consommation == null ? null : invokeNoArg(consommation, "getGlobalBill");
        if (globalBill != null) {
            BigDecimal currentGlobalAmount = extractBigDecimal(globalBill, "getGlobalAmount");
            if (currentGlobalAmount != null) {
                invokeSetter(globalBill, "setGlobalAmount", subtractWithoutNegative(currentGlobalAmount, voidedTotal),
                        BigDecimal.class);
                saveGlobalBillWithBillingUtil(globalBill,
                        Context.loadClass("org.openmrs.module.mohbilling.model.GlobalBill"));
            }
        }
    }

    private BigDecimal subtractWithoutNegative(BigDecimal currentAmount, BigDecimal amountToSubtract) {
        BigDecimal result = (currentAmount == null ? BigDecimal.ZERO : currentAmount)
                .subtract(amountToSubtract == null ? BigDecimal.ZERO : amountToSubtract);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private void adjustNestedBillingAmount(Class<?> billingServiceClass,
                                           Object billingService,
                                           Object consommation,
                                           String getterName,
                                           String billClassName,
                                           String saveMethodName,
                                           BigDecimal voidedTotal) throws Exception {
        Object nestedBill = consommation == null ? null : invokeNoArg(consommation, getterName);
        if (nestedBill == null) {
            return;
        }
        BigDecimal currentAmount = extractBigDecimal(nestedBill, "getAmount");
        if (currentAmount == null) {
            return;
        }
        invokeSetter(nestedBill, "setAmount", subtractWithoutNegative(currentAmount, voidedTotal), BigDecimal.class);
        billingServiceClass.getMethod(saveMethodName, Context.loadClass(billClassName)).invoke(billingService, nestedBill);
    }

    private boolean isRecentBillingCandidate(Object consommation, Object patientBill, Date requestDate) {
        return isRecentBillingCandidate(consommation, patientBill, requestDate, null);
    }

    private boolean isRecentBillingCandidate(Object consommation, Object patientBill, Date requestDate,
                                             Date actionDate) {
        Date candidateDate = billingCandidateDate(consommation, patientBill);
        return isDateNear(candidateDate, requestDate) || isDateNear(candidateDate, actionDate);
    }

    private Date billingCandidateDate(Object consommation, Object patientBill) {
        Date candidateDate = extractDate(consommation, "getCreatedDate");
        if (candidateDate == null) {
            candidateDate = extractDate(patientBill, "getCreatedDate");
        }
        return candidateDate;
    }

    private boolean isDateNear(Date candidateDate, Date referenceDate) {
        if (referenceDate == null) {
            return true;
        }
        if (candidateDate == null) {
            return false;
        }
        Date from = addMinutes(referenceDate, -30);
        Date to = addMinutes(referenceDate, 30);
        return !candidateDate.before(from) && !candidateDate.after(to);
    }

    private void voidEmptyBillingContainers(Class<?> billingServiceClass,
                                            Object billingService,
                                            Class<?> patientBillClass,
                                            Class<?> consommationClass,
                                            Object patientBill,
                                            Object consommation,
                                            String reason) throws Exception {
        Set<?> billItems = safeCastSet(invokeNoArg(consommation, "getBillItems"));
        boolean hasActiveBillItems = false;
        if (billItems != null) {
            for (Object billItem : billItems) {
                if (!extractBoolean(billItem, "getVoided")) {
                    hasActiveBillItems = true;
                    break;
                }
            }
        }
        if (hasActiveBillItems) {
            return;
        }
        invokeSetter(consommation, "setVoided", Boolean.TRUE, boolean.class);
        invokeSetter(consommation, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
        invokeSetter(consommation, "setVoidedDate", new Date(), Date.class);
        invokeSetter(consommation, "setVoidReason", reason, String.class);
        zeroAndVoidNestedBill(billingServiceClass, billingService, consommation, "getInsuranceBill",
                "org.openmrs.module.mohbilling.model.InsuranceBill", "saveInsuranceBill", reason);
        zeroAndVoidNestedBill(billingServiceClass, billingService, consommation, "getThirdPartyBill",
                "org.openmrs.module.mohbilling.model.ThirdPartyBill", "saveThirdPartyBill", reason);
        detachEmptyConsommationFromGlobalBill(consommation);
        billingServiceClass.getMethod("saveConsommation", consommationClass).invoke(billingService, consommation);
        if (patientBill != null && !extractBoolean(patientBill, "getIsPaid")) {
            invokeSetter(patientBill, "setAmount", BigDecimal.ZERO, BigDecimal.class);
            invokeSetter(patientBill, "setVoided", Boolean.TRUE, Boolean.class);
            invokeSetter(patientBill, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
            invokeSetter(patientBill, "setVoidedDate", new Date(), Date.class);
            invokeSetter(patientBill, "setVoidReason", reason, String.class);
            invokeSetter(patientBill, "setStatus", "CANCELLED", String.class);
            billingServiceClass.getMethod("savePatientBill", patientBillClass).invoke(billingService, patientBill);
        }
    }

    private void detachEmptyConsommationFromGlobalBill(Object consommation) throws Exception {
        Object globalBill = consommation == null ? null : invokeNoArg(consommation, "getGlobalBill");
        if (globalBill == null) {
            return;
        }
        invokeSetter(consommation, "setGlobalBill", null,
                Context.loadClass("org.openmrs.module.mohbilling.model.GlobalBill"));
    }

    private void zeroAndVoidNestedBill(Class<?> billingServiceClass,
                                       Object billingService,
                                       Object consommation,
                                       String getterName,
                                       String billClassName,
                                       String saveMethodName,
                                       String reason) throws Exception {
        Object nestedBill = consommation == null ? null : invokeNoArg(consommation, getterName);
        if (nestedBill == null) {
            return;
        }
        invokeSetter(nestedBill, "setAmount", BigDecimal.ZERO, BigDecimal.class);
        invokeSetter(nestedBill, "setVoided", Boolean.TRUE, boolean.class);
        invokeSetter(nestedBill, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
        invokeSetter(nestedBill, "setVoidedDate", new Date(), Date.class);
        invokeSetter(nestedBill, "setVoidReason", reason, String.class);
        billingServiceClass.getMethod(saveMethodName, Context.loadClass(billClassName)).invoke(billingService, nestedBill);
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

    /**
     * MOH Billing bills new drug orders itself, just before this transaction commits (MohBillingOrderHandler), and
     * silently skips orders it cannot match. This repeats its lookups for every billable line so that a prescription
     * MOH Billing would not bill is rolled back with an error instead of being saved unbilled.
     */
    private void requireMohBillingCanBill(Patient patient, List<PatientDispensingRequestLine> requestLines) {
        Class<?> billingServiceClass;
        Object billingService;
        try {
            billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            billingService = Context.getService(billingServiceClass);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("MOH Billing service is not available. The prescription was not saved.");
        }
        List<String> problems = new ArrayList<String>();
        try {
            String cardNumber = latestInsuranceCardNumber(patient);
            Object insurancePolicy = cardNumber == null ? null
                    : billingServiceClass.getMethod("getInsurancePolicyByCardNo", String.class).invoke(billingService, cardNumber);
            if (insurancePolicy == null) {
                problems.add("no MOH Billing insurance policy matches the patient's latest insurance number"
                        + (cardNumber == null ? "" : " (" + cardNumber + ")"));
            } else if (billingServiceClass.getMethod("getOpenGlobalBillByInsuranceCardNo", String.class)
                    .invoke(billingService, cardNumber) == null) {
                problems.add("the patient has no open global bill for insurance card " + cardNumber);
            } else {
                Object insurance = invokeNoArg(insurancePolicy, "getInsurance");
                Method billableServiceLookup = billingServiceClass.getMethod("getBillableServiceByConcept",
                        Context.loadClass("org.openmrs.module.mohbilling.model.FacilityServicePrice"),
                        Context.loadClass("org.openmrs.module.mohbilling.model.Insurance"));
                for (PatientDispensingRequestLine requestLine : requestLines) {
                    if (requestLine.getLineStatus() != RequestStatus.PENDING_PAYMENT || requestLine.getItem() == null) {
                        continue;
                    }
                    String problem = mohBillingProblemForLine(billingServiceClass, billingService, billableServiceLookup,
                            insurance, requestLine);
                    if (problem != null) {
                        problems.add(problem);
                    }
                }
            }
        }
        catch (NoSuchMethodException e) {
            log.error("MOH Billing pre-commit check skipped: installed MOH Billing does not have the expected API", e);
            return;
        }
        catch (Exception e) {
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            log.error("MOH Billing pre-commit check failed", cause);
            throw new IllegalArgumentException("Unable to verify MOH Billing for this prescription: " + cause.getMessage()
                    + ". The prescription was not saved.");
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("MOH Billing cannot bill this prescription, so it was not saved: "
                    + StringUtils.join(problems, "; ") + ".");
        }
    }

    private String mohBillingProblemForLine(Class<?> billingServiceClass, Object billingService, Method billableServiceLookup,
                                            Object insurance, PatientDispensingRequestLine requestLine) throws Exception {
        String itemName = requestLine.getItem().getName();
        Drug drug = requestLine.getOrder() != null ? requestLine.getOrder().getDrug() : requestLine.getItem().getDrug();
        Object facilityServicePrice = null;
        if (drug != null && drug.getName() != null) {
            facilityServicePrice = billingServiceClass.getMethod("getFacilityServiceByName", String.class)
                    .invoke(billingService, drug.getName());
        }
        Concept orderable = requestLine.getOrder() != null ? requestLine.getOrder().getConcept()
                : drug != null ? drug.getConcept() : null;
        if (facilityServicePrice == null && orderable != null) {
            facilityServicePrice = billingServiceClass.getMethod("getFacilityServiceByConcept", Concept.class)
                    .invoke(billingService, orderable);
        }
        if (facilityServicePrice == null) {
            return "'" + itemName + "' has no MOH Billing service named '" + (drug == null ? itemName : drug.getName())
                    + "' or linked to its concept";
        }
        if (extractBoolean(facilityServicePrice, "isHidden")) {
            return "the MOH Billing service for '" + itemName + "' is hidden";
        }
        if (billableServiceLookup.invoke(billingService, facilityServicePrice, insurance) == null) {
            return "the MOH Billing service for '" + itemName + "' has no price for the patient's insurance "
                    + safe(String.valueOf(invokeNoArg(insurance, "getName")));
        }
        return null;
    }

    /**
     * Same source as MOH Billing: the value of the patient's most recent registration.insuranceNumberConcept obs.
     */
    private String latestInsuranceCardNumber(Patient patient) {
        String conceptRef = safe(Context.getAdministrationService().getGlobalProperty("registration.insuranceNumberConcept")).trim();
        if (conceptRef.isEmpty()) {
            return null;
        }
        Concept concept = Context.getConceptService().getConceptByUuid(conceptRef);
        if (concept == null) {
            concept = Context.getConceptService().getConcept(conceptRef);
        }
        if (concept == null) {
            return null;
        }
        org.openmrs.Obs latest = null;
        for (org.openmrs.Obs obs : Context.getObsService().getObservationsByPersonAndConcept(patient, concept)) {
            if (obs == null || Boolean.TRUE.equals(obs.getVoided()) || obs.getObsDatetime() == null) {
                continue;
            }
            if (latest == null || obs.getObsDatetime().after(latest.getObsDatetime())
                    || (obs.getObsDatetime().equals(latest.getObsDatetime()) && obs.getObsId() > latest.getObsId())) {
                latest = obs;
            }
        }
        String cardNumber = latest == null ? null : latest.getValueText();
        return cardNumber == null || cardNumber.trim().isEmpty() ? null : cardNumber;
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
            if (requestLine.getLineStatus() != RequestStatus.PENDING_PAYMENT) {
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

    private Date addMinutes(Date date, int minutes) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);
        calendar.add(java.util.Calendar.MINUTE, minutes);
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

    private Object saveGlobalBillWithBillingUtil(Object globalBill, Class<?> globalBillClass) throws Exception {
        Class<?> globalBillUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.GlobalBillUtil");
        return globalBillUtilClass.getMethod("saveGlobalBill", globalBillClass).invoke(null, globalBill);
    }

    private boolean invokeSetterIfPresent(Object target, String methodName, Object value, Class<?> parameterType) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
            return true;
        }
        catch (Exception ignored) {
            return false;
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

    private Integer extractPatientBillId(Object patientBill) {
        Integer patientBillId = extractInteger(patientBill, "getPatientBillId");
        if (patientBillId != null) {
            return patientBillId;
        }
        patientBillId = extractInteger(patientBill, "getId");
        if (patientBillId != null) {
            return patientBillId;
        }
        return extractInteger(patientBill, "getBillId");
    }

    private BigDecimal extractBigDecimal(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return null;
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
        applyFrequencyAndDosingType(order, line);
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
        applyFrequencyAndDosingType(order, line);
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
        Drug lineDrug = resolveLineDrug(requestLine);
        Concept lineConcept = resolveLineConcept(requestLine, lineDrug);
        List<DrugOrder> matches = new ArrayList<DrugOrder>();
        for (DrugOrder drugOrder : activeDrugOrders) {
            if (lineDrug != null && drugOrder.getDrug() != null
                    && lineDrug.getDrugId() != null
                    && lineDrug.getDrugId().equals(drugOrder.getDrug().getDrugId())) {
                matches.add(drugOrder);
                continue;
            }
            if ((lineDrug == null || drugOrder.getDrug() == null || drugOrder.getDrug().getDrugId() == null)
                    && lineConcept != null && drugOrder.getConcept() != null
                    && lineConcept.getConceptId() != null
                    && lineConcept.getConceptId().equals(drugOrder.getConcept().getConceptId())) {
                matches.add(drugOrder);
            }
        }
        return matches.size() == 1 ? matches.get(0) : null;
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
        Date now = new Date();
        for (Order order : orders) {
            if (order instanceof DrugOrder && !Boolean.TRUE.equals(order.getVoided()) && order.isActive(now)) {
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
        return normalizeQuantityScale(quantity);
    }

    private void requireDoseInstructions(PrescriptionLineCommand line) {
        if (line == null || normalizeFreeTextInstructions(line.getInstructions()).isEmpty()) {
            throw new IllegalArgumentException("Dose instructions are required");
        }
    }

    BigDecimal calculateRequestedQuantity(PrescriptionLineCommand line) {
        if (line == null) {
            throw new IllegalArgumentException("Prescription line is required");
        }
        BigDecimal submittedQuantity = line.getQuantity();
        if (submittedQuantity != null && submittedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return normalizeQuantity(submittedQuantity);
        }
        BigDecimal calculatedQuantity = calculateQuantityFromDoseSchedule(line);
        if (calculatedQuantity == null) {
            return normalizeQuantity(submittedQuantity);
        }
        return calculatedQuantity;
    }

    private BigDecimal calculateQuantityFromDoseSchedule(PrescriptionLineCommand line) {
        if (line.getDose() == null || line.getDose().compareTo(BigDecimal.ZERO) <= 0
                || line.getDuration() == null || line.getDuration().intValue() <= 0) {
            return null;
        }
        FrequencySchedule frequencySchedule = resolveFrequencySchedule(line.getFrequency());
        if (frequencySchedule == null || frequencySchedule.count.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal periods = durationToPeriods(line.getDuration(), line.getDurationUnit(), frequencySchedule.period);
        if (periods.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return line.getDose()
                .multiply(frequencySchedule.count)
                .multiply(periods)
                .setScale(0, RoundingMode.CEILING)
                .stripTrailingZeros()
                .setScale(0);
    }

    private BigDecimal normalizeQuantityScale(BigDecimal quantity) {
        BigDecimal normalized = quantity.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }

    private FrequencySchedule resolveFrequencySchedule(String frequencyName) {
        OrderFrequency orderFrequency = null;
        try {
            orderFrequency = resolveFrequency(frequencyName);
        }
        catch (Exception ignored) {
        }
        if (orderFrequency != null && orderFrequency.getFrequencyPerDay() != null
                && orderFrequency.getFrequencyPerDay().doubleValue() > 0) {
            return new FrequencySchedule(BigDecimal.valueOf(orderFrequency.getFrequencyPerDay().doubleValue()), "day");
        }
        return parseFrequencySchedule(frequencyName);
    }

    private FrequencySchedule parseFrequencySchedule(String frequencyName) {
        String text = normalizeFrequencyName(frequencyName);
        if (text.isEmpty() || text.contains("per need") || text.contains("as needed") || "prn".equals(text)) {
            return null;
        }
        if (text.contains("every other day") || text.contains("alternate day")) {
            return new FrequencySchedule(new BigDecimal("0.5"), "day");
        }
        if (containsWord(text, "qod") || containsWord(text, "eod")) {
            return new FrequencySchedule(new BigDecimal("0.5"), "day");
        }
        BigDecimal count = parseFrequencyCount(text);
        if (count == null) {
            if (text.contains("daily") || text.contains("per day") || text.contains("a day")
                    || text.contains("morning") || text.contains("evening") || text.contains("night")) {
                count = BigDecimal.ONE;
            } else if (text.contains("weekly") || text.contains("a week") || text.contains("per week")) {
                count = BigDecimal.ONE;
            } else if (text.contains("monthly") || text.contains("a month") || text.contains("per month")) {
                count = BigDecimal.ONE;
            }
        }
        if (count == null) {
            return null;
        }
        String period = "day";
        if (text.contains("week")) {
            period = "week";
        } else if (text.contains("month")) {
            period = "month";
        }
        return new FrequencySchedule(count, period);
    }

    private BigDecimal parseFrequencyCount(String text) {
        BigDecimal abbreviationCount = parseFrequencyAbbreviationCount(text);
        if (abbreviationCount != null) {
            return abbreviationCount;
        }
        if (containsWord(text, "once") || containsWord(text, "one")) {
            return BigDecimal.ONE;
        }
        if (containsWord(text, "twice") || containsWord(text, "two")) {
            return new BigDecimal("2");
        }
        if (containsWord(text, "thrice") || containsWord(text, "three")) {
            return new BigDecimal("3");
        }
        String[] words = new String[] {
                "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
                "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"
        };
        for (int i = 0; i < words.length; i++) {
            if (containsWord(text, words[i])) {
                return new BigDecimal(i + 4);
            }
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d+)\\b").matcher(text);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : null;
    }

    private BigDecimal parseFrequencyAbbreviationCount(String text) {
        if (containsWord(text, "qd") || containsWord(text, "od")
                || containsWord(text, "q24h") || containsHourlyPhrase(text, "24")) {
            return BigDecimal.ONE;
        }
        if (containsWord(text, "bid") || containsWord(text, "bd")
                || containsWord(text, "q12h") || containsHourlyPhrase(text, "12")) {
            return new BigDecimal("2");
        }
        if (containsWord(text, "tid") || containsWord(text, "tds")
                || containsWord(text, "q8h") || containsHourlyPhrase(text, "8")) {
            return new BigDecimal("3");
        }
        if (containsWord(text, "qid") || containsWord(text, "qds")
                || containsWord(text, "q6h") || containsHourlyPhrase(text, "6")) {
            return new BigDecimal("4");
        }
        return null;
    }

    private boolean containsHourlyPhrase(String text, String hours) {
        return text != null && text.matches(".*\\b" + hours + "\\s+hourly\\b.*");
    }

    private boolean containsWord(String text, String word) {
        return text != null && text.matches(".*\\b" + word + "\\b.*");
    }

    private BigDecimal durationToPeriods(Integer duration, String durationUnit, String targetPeriod) {
        BigDecimal days = new BigDecimal(duration.intValue());
        String unit = safe(durationUnit).trim().toLowerCase(Locale.ENGLISH);
        if (unit.startsWith("week")) {
            days = days.multiply(new BigDecimal("7"));
        } else if (unit.startsWith("month")) {
            days = days.multiply(new BigDecimal("30"));
        }
        if ("week".equals(targetPeriod)) {
            return days.divide(new BigDecimal("7"), 8, RoundingMode.HALF_UP);
        }
        if ("month".equals(targetPeriod)) {
            return days.divide(new BigDecimal("30"), 8, RoundingMode.HALF_UP);
        }
        return days;
    }

    private static class FrequencySchedule {
        private final BigDecimal count;
        private final String period;

        private FrequencySchedule(BigDecimal count, String period) {
            this.count = count;
            this.period = period;
        }
    }

    private boolean isAvailableInHospitalStock(org.openmrs.module.rwandapharmacymanagement.model.Item item) {
        if (inventoryService == null || item == null || item.getItemId() == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(item.getVisibleToRequesters())) {
            return false;
        }
        return inventoryService.hasStockInActivePharmacy(item.getItemId());
    }

    /**
     * Resolves the local pharmacy item a prescription line is for. Lines select the item directly; a legacy
     * product master id is resolved through its item mapping. The item's drug is what gets ordered.
     */
    private org.openmrs.module.rwandapharmacymanagement.model.Item resolvePrescribedItem(PrescriptionLineCommand line,
                                                                                       boolean validate) {
        org.openmrs.module.rwandapharmacymanagement.model.Item item = null;
        if (line.getItemId() != null) {
            item = pharmacyService.getItem(line.getItemId());
        } else if (line.getProductMasterItemId() != null) {
            ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(line.getProductMasterItemId());
            if (productMasterItem == null) {
                throw new IllegalArgumentException("Selected product master drug was not found");
            }
            ProductMasterPrivilegeSupport.requireAllowed(productMasterItem);
            item = pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
            if (item == null && productMasterItem.getDrug() != null) {
                item = ensureLocalItem(productMasterItem);
            }
        }
        if (item == null) {
            throw new IllegalArgumentException("Selected pharmacy item was not found");
        }
        if (validate) {
            Set<Integer> itemIds = Collections.singleton(item.getItemId());
            PrescribableItemSupport.requirePrescribable(item,
                    pharmacyService.getPrescribableItemInfo(itemIds).get(item.getItemId()),
                    inventoryService.getAvailableQuantitiesByItem(itemIds).get(item.getItemId()));
        } else if (item.getDrug() == null) {
            throw new IllegalArgumentException("Pharmacy item '" + item.getName() + "' is not linked to an OpenMRS drug");
        }
        return item;
    }

    private org.openmrs.module.rwandapharmacymanagement.model.Item ensureLocalItem(ProductMasterItem productMasterItem) {
        org.openmrs.module.rwandapharmacymanagement.model.Item mappedItem =
                pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
        if (mappedItem != null) {
            return mappedItem;
        }
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
        String desired = normalizeFrequencyName(frequencyName);
        if (desired.isEmpty()) {
            return null;
        }
        List<OrderFrequency> frequencies = Context.getOrderService().getOrderFrequencies(false);
        if (frequencies == null) {
            return null;
        }
        for (OrderFrequency frequency : frequencies) {
            if (frequencyNameMatches(frequency, desired, true)) {
                return frequency;
            }
        }
        for (OrderFrequency frequency : frequencies) {
            if (frequencyNameMatches(frequency, desired, false)) {
                return frequency;
            }
        }
        return null;
    }

    private void applyFrequencyAndDosingType(DrugOrder order, PrescriptionLineCommand line) {
        if (isPerNeedFrequency(line == null ? null : line.getFrequency())) {
            order.setAsNeeded(Boolean.TRUE);
            order.setAsNeededCondition(safe(line == null ? null : line.getInstructions()));
            order.setFrequency(null);
            order.setDosingType(FreeTextDosingInstructions.class);
            return;
        }
        order.setAsNeeded(Boolean.FALSE);
        order.setAsNeededCondition(null);
        order.setFrequency(resolveFrequency(line == null ? null : line.getFrequency()));
    }

    boolean isPerNeedFrequency(String frequencyName) {
        String text = normalizeFrequencyName(frequencyName);
        return text.contains("per need") || text.contains("as needed") || "prn".equals(text);
    }

    private boolean frequencyNameMatches(OrderFrequency frequency, String desired, boolean exact) {
        if (frequency == null || desired == null || desired.isEmpty()) {
            return false;
        }
        List<String> candidates = new ArrayList<String>();
        candidates.add(frequency.getName());
        if (frequency.getConcept() != null) {
            candidates.add(frequency.getConcept().getDisplayString());
            candidates.add(frequency.getConcept().getName() == null ? null : frequency.getConcept().getName().getName());
            candidates.add(frequency.getConcept().getConceptId() == null ? null : String.valueOf(frequency.getConcept().getConceptId()));
            if (frequency.getConcept().getNames() != null) {
                for (ConceptName conceptName : frequency.getConcept().getNames()) {
                    candidates.add(conceptName == null ? null : conceptName.getName());
                }
            }
        }
        for (String candidate : candidates) {
            String normalized = normalizeFrequencyName(candidate);
            if (normalized.isEmpty()) {
                continue;
            }
            if (exact ? normalized.equals(desired) : (normalized.contains(desired) || desired.contains(normalized))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeFrequencyName(String value) {
        return safe(value)
                .replaceAll("\\(\\s*\\d+\\s*\\)", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ENGLISH);
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
        String freeTextInstructions = normalizeFreeTextInstructions(line.getInstructions());
        if (!freeTextInstructions.isEmpty()) {
            parts.add(freeTextInstructions);
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

    String normalizeFreeTextInstructions(String instructions) {
        String value = safe(instructions);
        if (value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String[] parts = value.split("\\s*;\\s*");
        for (String part : parts) {
            String cleanPart = safe(part);
            if (cleanPart.isEmpty() || isGeneratedInstructionPart(cleanPart)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(cleanPart);
        }
        return builder.toString();
    }

    private boolean isGeneratedInstructionPart(String part) {
        String normalized = part == null ? "" : part.trim().toLowerCase(Locale.ENGLISH);
        return normalized.startsWith("dose:")
                || normalized.startsWith("route:")
                || normalized.startsWith("frequency:")
                || normalized.startsWith("duration:");
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
