package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.context.Context;
import org.openmrs.DrugOrder;
import org.openmrs.Patient;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.ClinicalSafetyService;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.PaymentStatusService;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.db.DispensingDao;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ClinicalSafetyAlert;
import org.openmrs.module.rwandapharmacymanagement.integration.PrescriptionCommunicationService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PaymentExceptionType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Method;
import java.math.BigDecimal;

@Transactional
public class DispensingServiceImpl extends BaseOpenmrsService implements DispensingService {

    private final Log log = LogFactory.getLog(getClass());

    private DispensingDao dispensingDao;
    private InventoryDao inventoryDao;
    private PricingService pricingService;
    private ClinicalSafetyService clinicalSafetyService;
    private PaymentStatusService paymentStatusService;
    private PrescriptionCommunicationService prescriptionCommunicationService;

    public void setDispensingDao(DispensingDao dispensingDao) {
        this.dispensingDao = dispensingDao;
    }

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPricingService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public void setClinicalSafetyService(ClinicalSafetyService clinicalSafetyService) {
        this.clinicalSafetyService = clinicalSafetyService;
    }

    public void setPaymentStatusService(PaymentStatusService paymentStatusService) {
        this.paymentStatusService = paymentStatusService;
    }

    public void setPrescriptionCommunicationService(PrescriptionCommunicationService prescriptionCommunicationService) {
        this.prescriptionCommunicationService = prescriptionCommunicationService;
    }

    @Override
    public List<PatientDispensingRequest> getPendingRequests() {
        return dispensingDao.getPendingRequests();
    }

    @Override
    public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId) {
        return dispensingDao.getPendingRequestsForPharmacy(pharmacyLocationId);
    }

    @Override
    public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId, String search, int firstResult, int maxResults) {
        return dispensingDao.getPendingRequestsForPharmacy(pharmacyLocationId, search, firstResult, maxResults);
    }

    @Override
    public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                                                        String search,
                                                                        int firstResult,
                                                                        int maxResults,
                                                                        String queueTab,
                                                                        String requestType,
                                                                        String dateFilter) {
        return dispensingDao.getPendingRequestsForPharmacy(pharmacyLocationId, search, firstResult, maxResults,
                queueTab, requestType, dateFilter);
    }

    @Override
    public long countPendingRequestsForPharmacy(Integer pharmacyLocationId) {
        return dispensingDao.countPendingRequestsForPharmacy(pharmacyLocationId);
    }

    @Override
    public long countPendingRequestsForPharmacy(Integer pharmacyLocationId, String search) {
        return dispensingDao.countPendingRequestsForPharmacy(pharmacyLocationId, search);
    }

    @Override
    public long countPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                                String search,
                                                String queueTab,
                                                String requestType,
                                                String dateFilter) {
        return dispensingDao.countPendingRequestsForPharmacy(pharmacyLocationId, search, queueTab, requestType, dateFilter);
    }

    @Override
    public List<PatientDispensingRequest> getPendingPaidRequests() {
        List<PatientDispensingRequest> requests = dispensingDao.getPendingPaidRequests();
        if (paymentStatusService == null || requests == null) {
            return requests;
        }
        List<PatientDispensingRequest> eligibleRequests = new ArrayList<PatientDispensingRequest>();
        for (PatientDispensingRequest request : requests) {
            if (paymentStatusService.isPaymentConfirmed(request)) {
                eligibleRequests.add(request);
            }
        }
        return eligibleRequests;
    }

    @Override
    public Integer findLatestPatientBillId(Integer patientId) {
        return dispensingDao.findLatestPatientBillId(patientId);
    }

    @Override
    public Integer findLatestPatientBillId(Integer patientId, Date referenceDate) {
        return dispensingDao.findLatestPatientBillId(patientId, referenceDate);
    }

    @Override
    public Integer findLatestPatientBillId(Integer patientId, Date referenceDate, Collection<String> itemNames) {
        return dispensingDao.findLatestPatientBillId(patientId, referenceDate, itemNames);
    }

    @Override
    public Boolean isPatientBillPaid(Integer patientBillId) {
        return dispensingDao.isPatientBillPaid(patientBillId);
    }

    @Override
    public void refreshPendingRequestPayments() {
        refreshPendingRequestPayments((Integer) null);
    }

    @Override
    public void refreshPendingRequestPayments(Integer pharmacyLocationId) {
        List<PatientDispensingRequest> requests = pharmacyLocationId == null
                ? dispensingDao.getPendingRequests()
                : dispensingDao.getPendingRequestsForPharmacy(pharmacyLocationId);
        if (requests == null) {
            return;
        }
        log.info("Refreshing dispensing request payment links. pharmacyLocationId=" + pharmacyLocationId
                + ", requestCount=" + requests.size());
        boolean strictBillMode = strictPaymentBillMode();
        for (PatientDispensingRequest request : requests) {
            refreshPendingRequestPayment(request, strictBillMode);
        }
    }

    @Override
    public void refreshPendingRequestPayments(Collection<Integer> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }
        Set<Integer> uniqueRequestIds = new HashSet<Integer>();
        for (Integer requestId : requestIds) {
            if (requestId != null) {
                uniqueRequestIds.add(requestId);
            }
        }
        if (uniqueRequestIds.isEmpty()) {
            return;
        }
        log.info("Refreshing dispensing request payment links for loaded queue requests. requestCount="
                + uniqueRequestIds.size());
        boolean strictBillMode = strictPaymentBillMode();
        for (Integer requestId : uniqueRequestIds) {
            refreshPendingRequestPayment(dispensingDao.getRequest(requestId), strictBillMode);
        }
    }

    @Override
    public void refreshPendingRequestPayment(Integer requestId) {
        if (requestId == null) {
            return;
        }
        refreshPendingRequestPayment(dispensingDao.getRequest(requestId), strictPaymentBillMode());
    }

    @Override
    public PatientDispensingRequest approvePaymentException(Integer requestId,
                                                            PaymentExceptionType exceptionType,
                                                            String reason,
                                                            String note) {
        if (requestId == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (exceptionType == null) {
            throw new IllegalArgumentException("Payment exception type is required");
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) {
            throw new IllegalArgumentException("Payment exception reason is required");
        }
        PatientDispensingRequest request = dispensingDao.getRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Dispensing request not found");
        }
        if (request.getRequestStatus() == RequestStatus.DISPENSED
                || request.getRequestStatus() == RequestStatus.CANCELLED
                || request.getRequestStatus() == RequestStatus.UNAVAILABLE) {
            throw new IllegalArgumentException("Payment exception can only be approved for active requests");
        }

        request.setPaymentExceptionApproved(Boolean.TRUE);
        request.setPaymentExceptionType(exceptionType);
        request.setPaymentExceptionReason(normalizedReason);
        request.setPaymentExceptionNote(note == null ? null : note.trim());
        request.setPaymentExceptionApprovedBy(Context.getAuthenticatedUser());
        request.setPaymentExceptionApprovedDate(new Date());
        if (request.getRequestStatus() == RequestStatus.PENDING_PAYMENT) {
            request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
        }
        updateActiveLineStatuses(request, RequestStatus.PENDING_DISPENSE);
        return dispensingDao.saveRequest(request);
    }

    private void refreshPendingRequestPayment(PatientDispensingRequest request, boolean strictBillMode) {
        if (request == null) {
            return;
        }
        if (!Boolean.TRUE.equals(request.getBillable())) {
            request.setPaymentConfirmed(Boolean.TRUE);
            if (request.getRequestStatus() == RequestStatus.PENDING_PAYMENT) {
                request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
            }
            updateActiveLineStatuses(request, RequestStatus.PENDING_DISPENSE);
            dispensingDao.saveRequest(request);
            return;
        }
        if (Boolean.TRUE.equals(request.getPaymentConfirmed())) {
            if (request.getRequestStatus() == RequestStatus.PENDING_PAYMENT) {
                request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
                updateActiveLineStatuses(request, RequestStatus.PENDING_DISPENSE);
                dispensingDao.saveRequest(request);
            }
            return;
        }
        Integer patientBillId = request.getPatientBillId();
        if (patientBillId == null && request.getPatient() != null) {
            Set<String> itemNames = billingLookupNames(request);
            log.info("Trying to link dispensing request to patient bill during queue refresh. requestId="
                    + request.getPatientDispensingRequestId() + ", patientId=" + request.getPatient().getPatientId()
                    + ", requestDate=" + request.getDateCreated() + ", itemNames=" + itemNames);
            patientBillId = dispensingDao.findLatestPatientBillId(request.getPatient().getPatientId(),
                    request.getDateCreated(), itemNames);
            if (patientBillId != null) {
                request.setPatientBillId(patientBillId);
                log.info("Matched dispensing request to patient bill during queue refresh. requestId="
                        + request.getPatientDispensingRequestId() + ", patientBillId=" + patientBillId);
            } else {
                log.warn("No patient bill found during queue refresh. requestId="
                        + request.getPatientDispensingRequestId() + ", patientId="
                        + request.getPatient().getPatientId() + ", requestDate=" + request.getDateCreated()
                        + ", itemNames=" + itemNames);
            }
        }
        Boolean paid = dispensingDao.isPatientBillPaid(patientBillId);
        if (paid == null) {
            paid = findPaymentStateFromBillingService(patientBillId);
        }
        if (paid == null) {
            if (strictBillMode) {
                paid = Boolean.FALSE;
            } else {
                if (patientBillId != null && request.getPatientBillId() != null) {
                    dispensingDao.saveRequest(request);
                    log.info("Saved dispensing request patient bill link during queue refresh with unknown payment status. requestId="
                            + request.getPatientDispensingRequestId() + ", patientBillId=" + patientBillId);
                }
                return;
            }
        }
        request.setPaymentConfirmed(paid);
        if (Boolean.TRUE.equals(paid)) {
            if (request.getRequestStatus() == RequestStatus.PENDING_PAYMENT) {
                request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
            }
        } else if (isPaymentExceptionApproved(request)) {
            if (request.getRequestStatus() == RequestStatus.PENDING_PAYMENT) {
                request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
            }
        } else {
            request.setRequestStatus(RequestStatus.PENDING_PAYMENT);
        }
        updateActiveLineStatuses(request, isPaymentSatisfied(request) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        dispensingDao.saveRequest(request);
        if (patientBillId != null) {
            log.info("Saved dispensing request payment state during queue refresh. requestId="
                    + request.getPatientDispensingRequestId() + ", patientBillId=" + patientBillId
                    + ", paid=" + paid + ", requestStatus=" + request.getRequestStatus());
        }
    }

    private void updateActiveLineStatuses(PatientDispensingRequest request, RequestStatus status) {
        if (request.getLines() == null) {
            return;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getLineStatus() == RequestStatus.DISPENSED
                    || line.getLineStatus() == RequestStatus.UNAVAILABLE
                    || line.getLineStatus() == RequestStatus.CANCELLED
                    || line.getLineStatus() == RequestStatus.PARTIALLY_DISPENSED) {
                continue;
            }
            line.setLineStatus(Boolean.TRUE.equals(line.getBillable()) ? status : RequestStatus.PENDING_DISPENSE);
            dispensingDao.saveRequestLine(line);
        }
    }

    private Set<String> billingLookupNames(PatientDispensingRequest request) {
        Set<String> names = new HashSet<String>();
        if (request == null || request.getLines() == null) {
            return names;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(line.getBillable())) {
                continue;
            }
            if (line.getItem() != null) {
                addBillingLookupName(names, line.getItem().getName());
                if (line.getItem().getDrug() != null) {
                    addBillingLookupName(names, line.getItem().getDrug().getName());
                }
            }
            if (line.getOrder() instanceof DrugOrder && ((DrugOrder) line.getOrder()).getDrug() != null) {
                addBillingLookupName(names, ((DrugOrder) line.getOrder()).getDrug().getName());
            }
        }
        return names;
    }

    private void addBillingLookupName(Collection<String> names, String name) {
        if (names == null || name == null) {
            return;
        }
        String rawName = name.trim().toLowerCase(Locale.ENGLISH);
        if (!rawName.isEmpty()) {
            names.add(rawName);
            String normalizedName = rawName.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
            if (!normalizedName.isEmpty()) {
                names.add(normalizedName);
            }
        }
    }

    private boolean strictPaymentBillMode() {
        String mode = Context.getAdministrationService().getGlobalProperty(
                RwandaPharmacyManagementConfig.GP_PRESCRIPTION_PAYMENT_BILL_MODE, "latest");
        return "strict".equalsIgnoreCase(mode == null ? "" : mode.trim());
    }

    private Boolean findPaymentStateFromBillingService(Integer patientBillId) {
        if (patientBillId == null) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return null;
            }
            Method getPatientBill = billingServiceClass.getMethod("getPatientBill", Integer.class);
            Object patientBill = getPatientBill.invoke(billingService, patientBillId);
            return extractBoolean(patientBill, "getIsPaid");
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Integer findPatientBillIdFromBeneficiaryFlow(Patient patient, Date requestDate) {
        if (patient == null) {
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
            Integer resolvedBillId = null;
            long bestDistance = Long.MAX_VALUE;
            Method getConsommationsByBeneficiary = billingServiceClass.getMethod("getConsommationsByBeneficiary",
                    Context.loadClass("org.openmrs.module.mohbilling.model.Beneficiary"));
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
                        Date candidateDate = extractDate(consommation, "getCreatedDate");
                        long distance = requestDate == null || candidateDate == null
                                ? Long.MAX_VALUE - candidateBillId.longValue()
                                : Math.abs(candidateDate.getTime() - requestDate.getTime());
                        if (resolvedBillId == null || distance < bestDistance) {
                            resolvedBillId = candidateBillId;
                            bestDistance = distance;
                        }
                    }
                }
            }
            return resolvedBillId;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Integer extractInteger(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Date extractDate(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Date ? (Date) value : null;
        }
        catch (Exception ignored) {
            return null;
        }
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
        Integer beneficiaryPatientId = extractInteger(beneficiaryPatient, "getPatientId");
        return patient.getPatientId().equals(beneficiaryPatientId);
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Boolean extractBoolean(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Boolean ? (Boolean) value : null;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public List<DispenseRecord> getRecentDispenses() {
        return dispensingDao.getRecentDispenses();
    }

    @Override
    public List<DispenseRecord> getDispenseRecords(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
        return dispensingDao.getDispenseRecords(fromInclusive, toExclusive, pharmacyLocationId);
    }

    @Override
    public List<PatientDispensingRequest> getPatientDispensingRequests(Date fromInclusive,
                                                                       Date toExclusive,
                                                                       Integer pharmacyLocationId,
                                                                       RequestType requestType) {
        return dispensingDao.getPatientDispensingRequests(fromInclusive, toExclusive, pharmacyLocationId, requestType);
    }

    @Override
    public List<PatientDispensingRequest> getPaymentExceptionApprovedRequests(Date fromInclusive,
                                                                              Date toExclusive,
                                                                              Integer pharmacyLocationId,
                                                                              RequestType requestType) {
        return dispensingDao.getPaymentExceptionApprovedRequests(fromInclusive, toExclusive, pharmacyLocationId, requestType);
    }

    @Override
    public List<PatientDispensingRequestLine> getNotBillableRequestLines(Date fromInclusive,
                                                                         Date toExclusive,
                                                                         Integer pharmacyLocationId,
                                                                         RequestType requestType) {
        return dispensingDao.getNotBillableRequestLines(fromInclusive, toExclusive, pharmacyLocationId, requestType);
    }

    @Override
    public List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds) {
        return dispensingDao.getDispenseRecordsForRequests(requestIds);
    }

    @Override
    public List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults) {
        return dispensingDao.getPatientPrescriptionHistory(patientId, firstResult, maxResults);
    }

    @Override
    public long countPatientPrescriptionHistory(Integer patientId) {
        return dispensingDao.countPatientPrescriptionHistory(patientId);
    }

    @Override
    public List<PatientDispensingRequest> getPatientConsumableHistory(Integer patientId, int firstResult, int maxResults) {
        return dispensingDao.getPatientConsumableHistory(patientId, firstResult, maxResults);
    }

    @Override
    public long countPatientConsumableHistory(Integer patientId) {
        return dispensingDao.countPatientConsumableHistory(patientId);
    }

    @Override
    public PatientDispensingRequest getRequest(Integer requestId) {
        return dispensingDao.getRequest(requestId);
    }

    @Override
    public PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest) {
        if (patientDispensingRequest.getCreator() == null) {
            patientDispensingRequest.setCreator(Context.getAuthenticatedUser());
        }
        if (patientDispensingRequest.getDateCreated() == null) {
            patientDispensingRequest.setDateCreated(new Date());
        }
        if (patientDispensingRequest.getRequestStatus() == null) {
            patientDispensingRequest.setRequestStatus(patientDispensingRequest.getPaymentConfirmed()
                    ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        }
        reconcileTerminalRequestStatus(patientDispensingRequest);
        dispensingDao.saveRequest(patientDispensingRequest);
        if (patientDispensingRequest.getLines() != null) {
            for (PatientDispensingRequestLine line : patientDispensingRequest.getLines()) {
                line.setPatientDispensingRequest(patientDispensingRequest);
                if (line.getRoutedPharmacyLocation() == null) {
                    line.setRoutedPharmacyLocation(patientDispensingRequest.getTargetPharmacyLocation());
                }
                if (line.getLineStatus() == null) {
                    line.setLineStatus(patientDispensingRequest.getRequestStatus());
                }
                if (line.getCreator() == null) {
                    line.setCreator(Context.getAuthenticatedUser());
                }
                if (line.getDateCreated() == null) {
                    line.setDateCreated(new Date());
                }
                dispensingDao.saveRequestLine(line);
            }
        }
        return patientDispensingRequest;
    }

    private void reconcileTerminalRequestStatus(PatientDispensingRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            return;
        }
        boolean hasNonVoidedLine = false;
        boolean hasQueueableLine = false;
        boolean hasDispensedLine = false;
        boolean hasUnavailableLine = false;
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            hasNonVoidedLine = true;
            RequestStatus status = line.getLineStatus();
            if (status == RequestStatus.DISPENSED) {
                hasDispensedLine = true;
            } else if (status == RequestStatus.UNAVAILABLE) {
                hasUnavailableLine = true;
            }
            if (status != RequestStatus.DISPENSED
                    && status != RequestStatus.UNAVAILABLE
                    && status != RequestStatus.CANCELLED) {
                hasQueueableLine = true;
            }
        }
        if (!hasNonVoidedLine || hasQueueableLine) {
            return;
        }
        if (hasDispensedLine) {
            request.setRequestStatus(RequestStatus.DISPENSED);
        } else if (hasUnavailableLine) {
            request.setRequestStatus(RequestStatus.UNAVAILABLE);
        } else {
            request.setRequestStatus(RequestStatus.CANCELLED);
        }
    }

    @Override
    public DispenseRecord dispense(DispenseRecord dispenseRecord) {
        persistDispense(dispenseRecord, false);
        DispenseRecord savedRecord = dispensingDao.saveDispenseRecord(dispenseRecord);
        if (prescriptionCommunicationService != null) {
            prescriptionCommunicationService.handlePrescriptionDispensed(savedRecord);
        }
        return savedRecord;
    }

    @Override
    public DispenseRecord dispenseWithoutOrder(DispenseRecord dispenseRecord) {
        persistDispense(dispenseRecord, true);
        return dispensingDao.saveDispenseRecord(dispenseRecord);
    }

    @Override
    public void markRequestLineUnavailable(Integer requestId, Integer requestLineId, String reason) {
        if (requestId == null || requestLineId == null) {
            return;
        }
        PatientDispensingRequest request = dispensingDao.getRequest(requestId);
        if (request == null || request.getLines() == null) {
            return;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || !requestLineId.equals(line.getPatientDispensingRequestLineId())) {
                continue;
            }
            if (line.getLineStatus() == RequestStatus.DISPENSED
                    || line.getLineStatus() == RequestStatus.UNAVAILABLE
                    || line.getLineStatus() == RequestStatus.CANCELLED) {
                return;
            }
            line.setLineStatus(RequestStatus.UNAVAILABLE);
            line.setVoidReason(reason);
            dispensingDao.saveRequestLine(line);
            refreshRequestDispenseStatus(requestId);
            return;
        }
    }

    private void persistDispense(DispenseRecord dispenseRecord, boolean withoutOrder) {
        PatientDispensingRequest request = resolveDispensingRequest(dispenseRecord, withoutOrder);
        validateDispenseAgainstActiveRequestLines(request, dispenseRecord.getLines(), withoutOrder);
        dispenseRecord.setCreator(Context.getAuthenticatedUser());
        dispenseRecord.setDateCreated(new Date());
        dispenseRecord.setDispensedWithoutOrder(withoutOrder);
        if (request != null) {
            dispenseRecord.setPatientDispensingRequest(request);
        }
        enforceClinicalSafety(dispenseRecord);
        Date now = new Date();
        if (dispenseRecord.getLines() != null) {
            for (DispenseRecordLine line : dispenseRecord.getLines()) {
                line.setDispenseRecord(dispenseRecord);
                line.setCreator(Context.getAuthenticatedUser());
                line.setDateCreated(now);
            }
        }
        dispensingDao.saveDispenseRecord(dispenseRecord);
        if (dispenseRecord.getLines() != null) {
            for (DispenseRecordLine line : dispenseRecord.getLines()) {
                InventoryBalance balance = inventoryDao.getInventoryBalance(line.getItem().getItemId(),
                        dispenseRecord.getDispensingPharmacyLocation().getPharmacyLocationId(), line.getBatchNumber());
                if (balance == null || balance.getQuantityOnHand() == null
                        || balance.getQuantityOnHand().compareTo(line.getQuantityDispensed()) < 0) {
                    throw new IllegalArgumentException("Cannot dispense above available stock");
                }
                line.setCostPrice(balance.getUnitPrice());
                line.setSellingPrice(pricingService.calculateSellingPrice(balance.getUnitPrice()));
                line.setLineTotal(line.getSellingPrice() == null ? null
                        : line.getSellingPrice().multiply(line.getQuantityDispensed()));
                dispensingDao.saveDispenseRecordLine(line);
                balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(line.getQuantityDispensed()));
                inventoryDao.saveInventoryBalance(balance);

                StockTransaction tx = new StockTransaction();
                tx.setItem(line.getItem());
                tx.setPharmacyLocation(dispenseRecord.getDispensingPharmacyLocation());
                tx.setBatchNumber(line.getBatchNumber());
                tx.setExpiryDate(line.getExpiryDate());
                tx.setQuantity(line.getQuantityDispensed().negate());
                tx.setTransactionType(withoutOrder ? StockTransactionType.DISPENSE_WITHOUT_ORDER : StockTransactionType.DISPENSE);
                tx.setUnitPrice(null);
                tx.setTotalAmount(null);
                if (!withoutOrder && request != null && request.getPatientDispensingRequestId() != null) {
                    tx.setReferenceNumber(request.getPatientDispensingRequestId().toString());
                }
                tx.setCreator(Context.getAuthenticatedUser());
                tx.setPerformedBy(Context.getAuthenticatedUser());
                tx.setDateCreated(new Date());
                tx.setRemarks(withoutOrder ? "Dispensed without order" : dispenseRequestRemarks(request));
                inventoryDao.saveStockTransaction(tx);
            }
        }
        if (dispenseRecord.getPatientDispensingRequest() != null
                && dispenseRecord.getPatientDispensingRequest().getPatientDispensingRequestId() != null) {
            refreshRequestDispenseStatus(dispenseRecord.getPatientDispensingRequest().getPatientDispensingRequestId());
        }
    }

    private PatientDispensingRequest resolveDispensingRequest(DispenseRecord dispenseRecord, boolean withoutOrder) {
        if (withoutOrder || dispenseRecord == null || dispenseRecord.getPatientDispensingRequest() == null
                || dispenseRecord.getPatientDispensingRequest().getPatientDispensingRequestId() == null) {
            return null;
        }
        PatientDispensingRequest request = dispensingDao.getRequestForUpdate(
                dispenseRecord.getPatientDispensingRequest().getPatientDispensingRequestId());
        if (request == null) {
            throw new IllegalArgumentException("Prescription request not found");
        }
        dispenseRecord.setPatientDispensingRequest(request);
        return request;
    }

    private String dispenseRequestRemarks(PatientDispensingRequest request) {
        if (request == null || request.getPatientDispensingRequestId() == null) {
            return "Dispensed against request";
        }
        return "Dispensed against request #" + request.getPatientDispensingRequestId();
    }

    private void validateDispenseAgainstActiveRequestLines(PatientDispensingRequest request,
                                                           List<DispenseRecordLine> dispenseLines,
                                                           boolean withoutOrder) {
        if (withoutOrder || request == null) {
            return;
        }
        if (request.getRequestStatus() == RequestStatus.CANCELLED || request.getRequestStatus() == RequestStatus.DISPENSED
                || Boolean.TRUE.equals(request.getVoided())) {
            throw new IllegalArgumentException("Cancelled or completed prescription requests cannot be dispensed");
        }
        if (!isPaymentSatisfied(request)) {
            throw new IllegalArgumentException("This prescription has not been paid yet");
        }
        rejectHiddenDispenseLines(dispenseLines);
        Map<Integer, BigDecimal> requestedByActiveItemId = requestedQuantitiesByActiveItem(request);
        if (requestedByActiveItemId.isEmpty()) {
            throw new IllegalArgumentException("This prescription has no active lines to dispense");
        }
        Map<Integer, BigDecimal> dispensedByItemId = calculateDispensedQuantitiesByItem(request.getPatientDispensingRequestId());
        Map<Integer, BigDecimal> submittedByItemId = submittedQuantitiesByItem(dispenseLines);
        if (submittedByItemId.isEmpty()) {
            throw new IllegalArgumentException("At least one active prescription line quantity is required to dispense");
        }
        for (Map.Entry<Integer, BigDecimal> entry : submittedByItemId.entrySet()) {
            Integer itemId = entry.getKey();
            BigDecimal activeRequested = requestedByActiveItemId.containsKey(itemId)
                    ? requestedByActiveItemId.get(itemId) : BigDecimal.ZERO;
            if (activeRequested.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Cannot dispense a cancelled or unavailable prescription line");
            }
            BigDecimal alreadyDispensed = dispensedByItemId.containsKey(itemId)
                    ? dispensedByItemId.get(itemId) : BigDecimal.ZERO;
            BigDecimal remaining = activeRequested.subtract(alreadyDispensed);
            if (entry.getValue().compareTo(remaining.max(BigDecimal.ZERO)) > 0) {
                throw new IllegalArgumentException("Dispense quantity cannot exceed active prescription quantity");
            }
        }
    }

    private void rejectHiddenDispenseLines(List<DispenseRecordLine> dispenseLines) {
        if (dispenseLines == null) {
            return;
        }
        for (DispenseRecordLine line : dispenseLines) {
            if (line == null || line.getItem() == null || line.getQuantityDispensed() == null
                    || line.getQuantityDispensed().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (Boolean.FALSE.equals(line.getItem().getVisibleToRequesters())) {
                throw new IllegalArgumentException("Hidden prescription items cannot be dispensed");
            }
        }
    }

    private Map<Integer, BigDecimal> requestedQuantitiesByActiveItem(PatientDispensingRequest request) {
        Map<Integer, BigDecimal> requestedByItemId = new LinkedHashMap<Integer, BigDecimal>();
        if (request == null || request.getLines() == null) {
            return requestedByItemId;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (!isActiveDispensingLine(line) || line.getItem() == null || line.getItem().getItemId() == null) {
                continue;
            }
            Integer itemId = line.getItem().getItemId();
            BigDecimal existing = requestedByItemId.containsKey(itemId) ? requestedByItemId.get(itemId) : BigDecimal.ZERO;
            requestedByItemId.put(itemId, existing.add(line.getRequestedQuantity() == null ? BigDecimal.ZERO : line.getRequestedQuantity()));
        }
        return requestedByItemId;
    }

    private boolean isActiveDispensingLine(PatientDispensingRequestLine line) {
        return line != null
                && !Boolean.TRUE.equals(line.getVoided())
                && line.getLineStatus() != RequestStatus.CANCELLED
                && line.getLineStatus() != RequestStatus.UNAVAILABLE
                && line.getLineStatus() != RequestStatus.DISPENSED;
    }

    private Map<Integer, BigDecimal> submittedQuantitiesByItem(List<DispenseRecordLine> dispenseLines) {
        Map<Integer, BigDecimal> submittedByItemId = new LinkedHashMap<Integer, BigDecimal>();
        if (dispenseLines == null) {
            return submittedByItemId;
        }
        for (DispenseRecordLine line : dispenseLines) {
            if (line == null || line.getQuantityDispensed() == null
                    || line.getQuantityDispensed().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (line.getItem() == null || line.getItem().getItemId() == null) {
                throw new IllegalArgumentException("Dispensed drug is required");
            }
            Integer itemId = line.getItem().getItemId();
            BigDecimal existing = submittedByItemId.containsKey(itemId) ? submittedByItemId.get(itemId) : BigDecimal.ZERO;
            submittedByItemId.put(itemId, existing.add(line.getQuantityDispensed()));
        }
        return submittedByItemId;
    }

    private void refreshRequestDispenseStatus(Integer requestId) {
        PatientDispensingRequest request = dispensingDao.getRequest(requestId);
        if (request == null || request.getLines() == null) {
            return;
        }
        Map<Integer, BigDecimal> dispensedByItemId = calculateDispensedQuantitiesByItem(requestId);
        Map<Integer, BigDecimal> remainingByItemId = new LinkedHashMap<Integer, BigDecimal>(dispensedByItemId);
        boolean hasActiveLine = false;
        boolean allActiveLinesDispensed = true;
        boolean anyQuantityServed = false;
        boolean anyUnavailableLine = false;
        for (PatientDispensingRequestLine requestLine : request.getLines()) {
            if (requestLine == null) {
                continue;
            }
            if (requestLine.getLineStatus() == RequestStatus.CANCELLED
                    || requestLine.getLineStatus() == RequestStatus.UNAVAILABLE) {
                if (requestLine.getLineStatus() == RequestStatus.UNAVAILABLE) {
                    anyUnavailableLine = true;
                }
                dispensingDao.saveRequestLine(requestLine);
                continue;
            }
            hasActiveLine = true;
            Integer itemId = requestLine.getItem() == null ? null : requestLine.getItem().getItemId();
            BigDecimal requestedQuantity = requestLine.getRequestedQuantity() == null
                    ? BigDecimal.ZERO : requestLine.getRequestedQuantity();
            BigDecimal remainingForItem = itemId == null || !remainingByItemId.containsKey(itemId)
                    ? BigDecimal.ZERO : remainingByItemId.get(itemId);
            BigDecimal servedForLine = requestedQuantity.min(remainingForItem.max(BigDecimal.ZERO));
            if (itemId != null) {
                remainingByItemId.put(itemId, remainingForItem.subtract(servedForLine));
            }
            if (servedForLine.compareTo(BigDecimal.ZERO) > 0) {
                anyQuantityServed = true;
            }
            if (servedForLine.compareTo(requestedQuantity) >= 0) {
                requestLine.setLineStatus(RequestStatus.DISPENSED);
            } else if (servedForLine.compareTo(BigDecimal.ZERO) > 0) {
                requestLine.setLineStatus(RequestStatus.PARTIALLY_DISPENSED);
                allActiveLinesDispensed = false;
            } else {
                requestLine.setLineStatus(resolvePendingLineStatus(request));
                allActiveLinesDispensed = false;
            }
            dispensingDao.saveRequestLine(requestLine);
        }
        if (!hasActiveLine) {
            request.setRequestStatus(anyUnavailableLine ? RequestStatus.UNAVAILABLE : RequestStatus.CANCELLED);
            dispensingDao.saveRequest(request);
            return;
        }
        if (allActiveLinesDispensed) {
            request.setRequestStatus(RequestStatus.DISPENSED);
        } else if (anyQuantityServed) {
            request.setRequestStatus(RequestStatus.IN_PROGRESS);
        } else {
            request.setRequestStatus(resolvePendingLineStatus(request));
        }
        dispensingDao.saveRequest(request);
    }

    private RequestStatus resolvePendingLineStatus(PatientDispensingRequest request) {
        return isPaymentSatisfied(request)
                ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT;
    }

    private boolean isPaymentSatisfied(PatientDispensingRequest request) {
        return request != null
                && (Boolean.TRUE.equals(request.getPaymentConfirmed()) || isPaymentExceptionApproved(request));
    }

    private boolean isPaymentExceptionApproved(PatientDispensingRequest request) {
        return request != null && Boolean.TRUE.equals(request.getPaymentExceptionApproved());
    }

    private Map<Integer, BigDecimal> calculateDispensedQuantitiesByItem(Integer requestId) {
        List<DispenseRecord> dispenseRecords = dispensingDao.getDispenseRecordsForRequests(Collections.singletonList(requestId));
        Map<Integer, BigDecimal> dispensedByItemId = new LinkedHashMap<Integer, BigDecimal>();
        if (dispenseRecords == null) {
            return dispensedByItemId;
        }
        for (DispenseRecord dispenseRecord : dispenseRecords) {
            if (dispenseRecord == null || dispenseRecord.getLines() == null) {
                continue;
            }
            for (DispenseRecordLine line : dispenseRecord.getLines()) {
                if (line == null || line.getItem() == null || line.getItem().getItemId() == null
                        || line.getQuantityDispensed() == null || line.getQuantityDispensed().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                Integer itemId = line.getItem().getItemId();
                BigDecimal accumulated = dispensedByItemId.containsKey(itemId)
                        ? dispensedByItemId.get(itemId) : BigDecimal.ZERO;
                dispensedByItemId.put(itemId, accumulated.add(line.getQuantityDispensed()));
            }
        }
        return dispensedByItemId;
    }

    private void enforceClinicalSafety(DispenseRecord dispenseRecord) {
        if (clinicalSafetyService == null || dispenseRecord.getLines() == null || dispenseRecord.getLines().isEmpty()) {
            return;
        }
        List<Item> items = new ArrayList<Item>();
        for (DispenseRecordLine line : dispenseRecord.getLines()) {
            if (line.getItem() != null) {
                items.add(line.getItem());
            }
        }
        List<ClinicalSafetyAlert> alerts = clinicalSafetyService.evaluateDispenseSafety(dispenseRecord.getPatient(), items);
        StringBuilder blockingMessage = new StringBuilder();
        for (ClinicalSafetyAlert alert : alerts) {
            if (alert.isBlocking()) {
                if (blockingMessage.length() > 0) {
                    blockingMessage.append("; ");
                }
                blockingMessage.append(alert.getMessage());
            }
        }
        if (blockingMessage.length() > 0) {
            throw new IllegalArgumentException("Clinical safety rule blocked dispense: " + blockingMessage);
        }
    }

}
