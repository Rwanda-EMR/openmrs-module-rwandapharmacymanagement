package org.openmrs.module.rwandapharmacymanagement.page.controller.dispense;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.PatientRequestVisibilitySupport;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PaymentExceptionType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WithOrderPageController {
    private static final Log log = LogFactory.getLog(WithOrderPageController.class);
    private static final int REQUEST_SELECTOR_LIMIT = 100;

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @RequestParam(value = "requestId", required = false) Integer requestId,
                    @RequestParam(value = "viewOnly", required = false) Boolean viewOnly,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "refreshPayments", required = false) Boolean refreshPayments,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        if (Boolean.TRUE.equals(refreshPayments)) {
            if (requestId != null) {
                dispensingService.refreshPendingRequestPayment(requestId);
            } else {
                dispensingService.refreshPendingRequestPayments(activePharmacy == null ? null : activePharmacy.getPharmacyLocationId());
            }
        }
        PatientDispensingRequest selectedRequest = requestId == null ? null : dispensingService.getRequest(requestId);
        String dispensingSelectionError = null;
        if (requestId != null && selectedRequest == null) {
            dispensingSelectionError = "Request " + requestId + " was not found or is no longer available in the dispensing queue.";
            log.warn(dispensingSelectionError);
        }
        if (selectedRequest != null && activePharmacy != null
                && !PatientRequestVisibilitySupport.isVisibleToPharmacy(selectedRequest, activePharmacy,
                Context.getUserContext() == null ? null : Context.getUserContext().getLocationId())) {
            dispensingSelectionError = describeRequestAccessIssue(selectedRequest, activePharmacy, requestId);
            log.warn(dispensingSelectionError);
            selectedRequest = null;
        }
        List<PatientDispensingRequest> visibleRequests = loadVisibleRequests(dispensingService, activePharmacy, selectedRequest);
        model.addAttribute("requests", visibleRequests);
        model.addAttribute("pricingService", pricingService);
        model.addAttribute("selectedRequest", selectedRequest);
        model.addAttribute("viewOnly", Boolean.TRUE.equals(viewOnly));
        model.addAttribute("requestTargetDescriptionById", describeTargets(visibleRequests, selectedRequest));
        model.addAttribute("patientIdentifierByRequestId", buildPatientIdentifierMap(visibleRequests, selectedRequest));
        model.addAttribute("prescriptionDetailByRequestLineId", buildPrescriptionDetails(selectedRequest));
        model.addAttribute("remainingQuantityByRequestLineId", buildRemainingQuantities(selectedRequest, dispensingService));
        model.addAttribute("batchOptionsByRequestLineId", buildBatchOptions(selectedRequest, activePharmacy, inventoryService));
        model.addAttribute("sourcePharmacyHintByRequestLineId",
                buildSourcePharmacyHints(selectedRequest, activePharmacy, pharmacyService, inventoryService));
        model.addAttribute("paymentRefreshRequested", Boolean.TRUE.equals(refreshPayments));
        model.addAttribute("dispensingSelectionError", dispensingSelectionError);
        model.addAttribute("paymentExceptionTypes", PaymentExceptionType.values());
        model.addAttribute("canApprovePaymentException",
                Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PAYMENT_EXCEPTION));
    }

    private List<PatientDispensingRequest> loadVisibleRequests(DispensingService dispensingService,
                                                               PharmacyLocation activePharmacy,
                                                               PatientDispensingRequest selectedRequest) {
        if (activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return java.util.Collections.emptyList();
        }
        List<PatientDispensingRequest> visibleRequests;
        if (selectedRequest == null) {
            visibleRequests = PatientRequestVisibilitySupport.filterForActivePharmacy(
                    dispensingService.getPendingRequestsForPharmacy(activePharmacy.getPharmacyLocationId(), null, 0,
                            REQUEST_SELECTOR_LIMIT), activePharmacy);
        } else {
            visibleRequests = new ArrayList<PatientDispensingRequest>();
        }
        if (selectedRequest != null && selectedRequest.getPatientDispensingRequestId() != null
                && PatientRequestVisibilitySupport.isVisibleToPharmacy(selectedRequest, activePharmacy,
                Context.getUserContext() == null ? null : Context.getUserContext().getLocationId())) {
            boolean alreadyIncluded = false;
            for (PatientDispensingRequest request : visibleRequests) {
                if (request != null && selectedRequest.getPatientDispensingRequestId().equals(request.getPatientDispensingRequestId())) {
                    alreadyIncluded = true;
                    break;
                }
            }
            if (!alreadyIncluded) {
                visibleRequests.add(0, selectedRequest);
            }
        }
        return visibleRequests;
    }

    public String post(@SpringBean("dispensingService") DispensingService dispensingService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "paymentExceptionType", required = false) PaymentExceptionType paymentExceptionType,
                       @RequestParam(value = "paymentExceptionReason", required = false) String paymentExceptionReason,
                       @RequestParam(value = "paymentExceptionNote", required = false) String paymentExceptionNote,
                       @RequestParam(value = "markUnavailableLineId", required = false) Integer markUnavailableLineId,
                       @RequestParam(value = "requestLineId", required = false) Integer[] requestLineIds,
                       @RequestParam(value = "itemId", required = false) Integer[] itemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "batchNumber", required = false) String[] batchNumbers,
                       @RequestParam(value = "expiryDate", required = false) String[] expiryDates,
                       @RequestParam(value = "viewOnly", required = false) Boolean viewOnly,
                       HttpSession session,
                       UiUtils ui) {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        PatientDispensingRequest request = dispensingService.getRequest(requestId);
        if (!PatientRequestVisibilitySupport.isVisibleToPharmacy(request, activePharmacy,
                Context.getUserContext() == null ? null : Context.getUserContext().getLocationId())) {
            throw new IllegalArgumentException("Selected request is not available to the active pharmacy");
        }
        if ("approvePaymentException".equalsIgnoreCase(action)) {
            if (!Context.hasPrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PAYMENT_EXCEPTION)) {
                throw new IllegalArgumentException("You do not have permission to approve payment exceptions");
            }
            dispensingService.approvePaymentException(requestId, paymentExceptionType, paymentExceptionReason, paymentExceptionNote);
            Map<String, Object> redirectParams = new LinkedHashMap<String, Object>();
            redirectParams.put("requestId", requestId);
            if (Boolean.TRUE.equals(viewOnly)) {
                redirectParams.put("viewOnly", Boolean.TRUE);
            }
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "dispense/withOrder", redirectParams);
        }
        if (markUnavailableLineId != null) {
            dispensingService.markRequestLineUnavailable(requestId, markUnavailableLineId,
                    "Drug not available in all pharmacies");
            Map<String, Object> redirectParams = new LinkedHashMap<String, Object>();
            redirectParams.put("requestId", requestId);
            if (Boolean.TRUE.equals(viewOnly)) {
                redirectParams.put("viewOnly", Boolean.TRUE);
            }
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "dispense/withOrder", redirectParams);
        }
        if (!isPaymentSatisfied(request)) {
            throw new IllegalArgumentException("This prescription has not been paid yet");
        }
        DispenseRecord record = new DispenseRecord();
        record.setPatient(request.getPatient());
        record.setPatientDispensingRequest(request);
        record.setDispensingPharmacyLocation(activePharmacy);
        record.setCreator(Context.getAuthenticatedUser());
        record.setDateCreated(new java.util.Date());
        record.setLines(buildLines(pharmacyService, inventoryService, request, activePharmacy, requestLineIds, itemIds,
                quantities, batchNumbers, expiryDates));
        dispensingService.dispense(record);
        Map<String, Object> redirectParams = new LinkedHashMap<String, Object>();
        if (activePharmacy.getPharmacyLocationId() != null) {
            redirectParams.put("pharmacyLocationId", activePharmacy.getPharmacyLocationId());
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "queue/patientQueue", redirectParams);
    }

    private List<DispenseRecordLine> buildLines(PharmacyService pharmacyService,
                                                InventoryService inventoryService,
                                                PatientDispensingRequest request,
                                                PharmacyLocation activePharmacy,
                                                Integer[] requestLineIds,
                                                Integer[] itemIds,
                                                BigDecimal[] quantities,
                                                String[] batchNumbers,
                                                String[] expiryDates) {
        List<DispenseRecordLine> lines = new ArrayList<DispenseRecordLine>();
        List<PatientDispensingRequestLine> requestLines = request == null ? null : request.getLines();
        if (requestLines == null || requestLines.isEmpty()) {
            return lines;
        }
        Map<Integer, BigDecimal> remainingByLineId = buildRemainingQuantities(request, dispensingServiceFromContext());
        Map<Integer, BigDecimal> allocatedByRequestLineId = new LinkedHashMap<Integer, BigDecimal>();
        int totalRows = maxLength(requestLineIds, itemIds, quantities, batchNumbers, expiryDates);
        for (int i = 0; i < totalRows; i++) {
            Integer requestLineId = valueAt(requestLineIds, i);
            BigDecimal quantity = valueAt(quantities, i);
            if (requestLineId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PatientDispensingRequestLine requestLine = findRequestLine(requestLines, requestLineId);
            if (requestLine == null || requestLine.getLineStatus() == RequestStatus.DISPENSED
                    || requestLine.getLineStatus() == RequestStatus.UNAVAILABLE
                    || requestLine.getLineStatus() == RequestStatus.CANCELLED) {
                continue;
            }
            BigDecimal remainingQuantity = remainingByLineId.containsKey(requestLineId)
                    ? remainingByLineId.get(requestLineId) : BigDecimal.ZERO;
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal currentlyAllocated = allocatedByRequestLineId.containsKey(requestLineId)
                    ? allocatedByRequestLineId.get(requestLineId) : BigDecimal.ZERO;
            BigDecimal newAllocatedTotal = currentlyAllocated.add(quantity);
            if (newAllocatedTotal.compareTo(remainingQuantity) > 0) {
                throw new IllegalArgumentException("Dispense quantity cannot exceed remaining quantity for " + requestLine.getItem().getName());
            }
            String batchNumber = valueAt(batchNumbers, i);
            Integer itemId = valueAt(itemIds, i);
            DispenseRecordLine line = new DispenseRecordLine();
            line.setItem(itemId == null ? requestLine.getItem() : pharmacyService.getItem(itemId));
            line.setBatchNumber(batchNumber);
            line.setQuantityDispensed(quantity);
            line.setExpiryDate(resolveExpiryDate(expiryDates, i, inventoryService, activePharmacy, line.getItem(), batchNumber));
            lines.add(line);
            allocatedByRequestLineId.put(requestLineId, newAllocatedTotal);
        }
        return lines;
    }

    private boolean isPaymentSatisfied(PatientDispensingRequest request) {
        return request != null
                && (Boolean.TRUE.equals(request.getPaymentConfirmed())
                || Boolean.TRUE.equals(request.getPaymentExceptionApproved()));
    }

    private DispensingService dispensingServiceFromContext() {
        return Context.getRegisteredComponent("dispensingService", DispensingService.class);
    }

    private Map<Integer, BigDecimal> buildRemainingQuantities(PatientDispensingRequest request,
                                                              DispensingService dispensingService) {
        Map<Integer, BigDecimal> remainingByLineId = new LinkedHashMap<Integer, BigDecimal>();
        if (request == null || request.getLines() == null) {
            return remainingByLineId;
        }
        Map<Integer, BigDecimal> dispensedByItemId = new HashMap<Integer, BigDecimal>();
        if (request.getPatientDispensingRequestId() != null && dispensingService != null) {
            List<DispenseRecord> dispenseRecords = dispensingService.getDispenseRecordsForRequests(
                    Collections.singletonList(request.getPatientDispensingRequestId()));
            if (dispenseRecords != null) {
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
                        BigDecimal existing = dispensedByItemId.containsKey(itemId) ? dispensedByItemId.get(itemId) : BigDecimal.ZERO;
                        dispensedByItemId.put(itemId, existing.add(line.getQuantityDispensed()));
                    }
                }
            }
        }
        Map<Integer, BigDecimal> remainingByItemId = new HashMap<Integer, BigDecimal>(dispensedByItemId);
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getPatientDispensingRequestLineId() == null) {
                continue;
            }
            if (line.getLineStatus() == RequestStatus.CANCELLED
                    || line.getLineStatus() == RequestStatus.UNAVAILABLE) {
                remainingByLineId.put(line.getPatientDispensingRequestLineId(), BigDecimal.ZERO);
                continue;
            }
            BigDecimal requestedQuantity = line.getRequestedQuantity() == null ? BigDecimal.ZERO : line.getRequestedQuantity();
            Integer itemId = line.getItem() == null ? null : line.getItem().getItemId();
            BigDecimal dispensedForItem = itemId == null || !remainingByItemId.containsKey(itemId)
                    ? BigDecimal.ZERO : remainingByItemId.get(itemId);
            BigDecimal servedForLine = requestedQuantity.min(dispensedForItem.max(BigDecimal.ZERO));
            BigDecimal remainingForLine = requestedQuantity.subtract(servedForLine);
            remainingByLineId.put(line.getPatientDispensingRequestLineId(), remainingForLine.max(BigDecimal.ZERO));
            if (itemId != null) {
                remainingByItemId.put(itemId, dispensedForItem.subtract(servedForLine));
            }
        }
        return remainingByLineId;
    }

    private java.util.Map<Integer, String> describeTargets(List<PatientDispensingRequest> requests,
                                                           PatientDispensingRequest selectedRequest) {
        java.util.Map<Integer, String> descriptions = new java.util.LinkedHashMap<Integer, String>();
        if (requests != null) {
            for (PatientDispensingRequest request : requests) {
                if (request != null && request.getPatientDispensingRequestId() != null) {
                    descriptions.put(request.getPatientDispensingRequestId(), PatientRequestVisibilitySupport.describeTarget(request));
                }
            }
        }
        if (selectedRequest != null && selectedRequest.getPatientDispensingRequestId() != null
                && !descriptions.containsKey(selectedRequest.getPatientDispensingRequestId())) {
            descriptions.put(selectedRequest.getPatientDispensingRequestId(),
                    PatientRequestVisibilitySupport.describeTarget(selectedRequest));
        }
        return descriptions;
    }

    private String describeRequestAccessIssue(PatientDispensingRequest request,
                                              PharmacyLocation activePharmacy,
                                              Integer requestId) {
        StringBuilder message = new StringBuilder("Request ")
                .append(requestId == null ? "" : requestId)
                .append(" cannot be opened from ")
                .append(activePharmacy == null || activePharmacy.getName() == null ? "the selected pharmacy" : activePharmacy.getName())
                .append(".");
        String targetDescription = PatientRequestVisibilitySupport.describeTarget(request);
        if (targetDescription != null && targetDescription.trim().length() > 0) {
            message.append(" It is routed to ").append(targetDescription).append(".");
        }
        message.append(" Change the active pharmacy or reroute the request before dispensing.");
        return message.toString();
    }

    private Map<Integer, String> buildPatientIdentifierMap(List<PatientDispensingRequest> requests,
                                                           PatientDispensingRequest selectedRequest) {
        Map<Integer, String> identifiers = new LinkedHashMap<Integer, String>();
        if (requests != null) {
            for (PatientDispensingRequest request : requests) {
                if (request != null && request.getPatientDispensingRequestId() != null) {
                    identifiers.put(request.getPatientDispensingRequestId(), patientIdentifier(request));
                }
            }
        }
        if (selectedRequest != null && selectedRequest.getPatientDispensingRequestId() != null
                && !identifiers.containsKey(selectedRequest.getPatientDispensingRequestId())) {
            identifiers.put(selectedRequest.getPatientDispensingRequestId(), patientIdentifier(selectedRequest));
        }
        return identifiers;
    }

    private String patientIdentifier(PatientDispensingRequest request) {
        if (request == null || request.getPatient() == null) {
            return "";
        }
        if (request.getPatient().getPatientIdentifier() != null) {
            return request.getPatient().getPatientIdentifier().toString();
        }
        return request.getPatient().getPatientId() == null ? "" : request.getPatient().getPatientId().toString();
    }

    private Map<Integer, Map<String, String>> buildPrescriptionDetails(PatientDispensingRequest request) {
        Map<Integer, Map<String, String>> detailsByLineId = new LinkedHashMap<Integer, Map<String, String>>();
        if (request == null || request.getLines() == null) {
            return detailsByLineId;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getPatientDispensingRequestLineId() == null) {
                continue;
            }
            Map<String, String> details = new LinkedHashMap<String, String>();
            DrugOrder matchingOrder = findMatchingOrder(request, line);
            if (matchingOrder != null) {
                details.put("dose", matchingOrder.getDose() == null ? "" : matchingOrder.getDose().toString());
                details.put("doseUnits", matchingOrder.getDoseUnits() == null ? "" : matchingOrder.getDoseUnits().getDisplayString());
                details.put("route", matchingOrder.getRoute() == null ? "" : matchingOrder.getRoute().getDisplayString());
                details.put("frequency", matchingOrder.getFrequency() == null ? "" : matchingOrder.getFrequency().getName());
                details.put("duration", matchingOrder.getDuration() == null ? "" : matchingOrder.getDuration().toString());
                details.put("durationUnits", matchingOrder.getDurationUnits() == null ? "" : matchingOrder.getDurationUnits().getDisplayString());
                details.put("instructions", matchingOrder.getDosingInstructions() == null ? matchingOrder.getInstructions() : matchingOrder.getDosingInstructions());
            }
            detailsByLineId.put(line.getPatientDispensingRequestLineId(), details);
        }
        return detailsByLineId;
    }

    private DrugOrder findMatchingOrder(PatientDispensingRequest request, PatientDispensingRequestLine requestLine) {
        if (requestLine != null && requestLine.getOrder() != null) {
            return requestLine.getOrder();
        }
        if (request == null || request.getEncounter() == null || request.getEncounter().getOrders() == null
                || requestLine == null || requestLine.getItem() == null) {
            return null;
        }
        for (Order order : request.getEncounter().getOrders()) {
            if (!(order instanceof DrugOrder)) {
                continue;
            }
            DrugOrder drugOrder = (DrugOrder) order;
            if (requestLine.getItem().getDrug() != null && drugOrder.getDrug() != null
                    && requestLine.getItem().getDrug().getDrugId() != null
                    && requestLine.getItem().getDrug().getDrugId().equals(drugOrder.getDrug().getDrugId())) {
                return drugOrder;
            }
            if (requestLine.getItem().getConcept() != null && drugOrder.getConcept() != null
                    && requestLine.getItem().getConcept().getConceptId() != null
                    && requestLine.getItem().getConcept().getConceptId().equals(drugOrder.getConcept().getConceptId())) {
                return drugOrder;
            }
        }
        return null;
    }

    private Map<Integer, List<InventoryBalance>> buildBatchOptions(PatientDispensingRequest request,
                                                                   PharmacyLocation activePharmacy,
                                                                   InventoryService inventoryService) {
        Map<Integer, List<InventoryBalance>> optionsByLineId = new LinkedHashMap<Integer, List<InventoryBalance>>();
        if (request == null || request.getLines() == null || activePharmacy == null) {
            return optionsByLineId;
        }
        List<InventoryBalance> balances = inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId());
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getPatientDispensingRequestLineId() == null || line.getItem() == null) {
                continue;
            }
            List<InventoryBalance> matchingBalances = new ArrayList<InventoryBalance>();
            for (InventoryBalance balance : balances) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                        || line.getItem().getItemId() == null
                        || !line.getItem().getItemId().equals(balance.getItem().getItemId())
                        || balance.getQuantityOnHand() == null
                        || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                matchingBalances.add(balance);
            }
            Collections.sort(matchingBalances, new Comparator<InventoryBalance>() {
                @Override
                public int compare(InventoryBalance left, InventoryBalance right) {
                    Date leftExpiry = left == null ? null : left.getExpiryDate();
                    Date rightExpiry = right == null ? null : right.getExpiryDate();
                    if (leftExpiry == null && rightExpiry == null) {
                        return 0;
                    }
                    if (leftExpiry == null) {
                        return 1;
                    }
                    if (rightExpiry == null) {
                        return -1;
                    }
                    return leftExpiry.compareTo(rightExpiry);
                }
            });
            optionsByLineId.put(line.getPatientDispensingRequestLineId(), matchingBalances);
        }
        return optionsByLineId;
    }

    private Map<Integer, String> buildSourcePharmacyHints(PatientDispensingRequest request,
                                                          PharmacyLocation activePharmacy,
                                                          PharmacyService pharmacyService,
                                                          InventoryService inventoryService) {
        Map<Integer, String> hintsByLineId = new LinkedHashMap<Integer, String>();
        if (request == null || request.getLines() == null || activePharmacy == null) {
            return hintsByLineId;
        }
        Map<Integer, BigDecimal> localQuantityByItemId = new HashMap<Integer, BigDecimal>();
        List<InventoryBalance> localBalances = inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId());
        for (InventoryBalance balance : localBalances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            BigDecimal current = localQuantityByItemId.containsKey(itemId) ? localQuantityByItemId.get(itemId) : BigDecimal.ZERO;
            localQuantityByItemId.put(itemId, current.add(balance.getQuantityOnHand()));
        }
        Map<Integer, PharmacyLocation> activePharmaciesById = new LinkedHashMap<Integer, PharmacyLocation>();
        for (PharmacyLocation pharmacyLocation : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null || !Boolean.TRUE.equals(pharmacyLocation.getActive())) {
                continue;
            }
            activePharmaciesById.put(pharmacyLocation.getPharmacyLocationId(), pharmacyLocation);
        }
        Map<Integer, Map<Integer, AlternativeSource>> sourcesByItemId = new HashMap<Integer, Map<Integer, AlternativeSource>>();
        List<InventoryBalance> allBalances = inventoryService.getInventoryForPharmacy(null);
        for (InventoryBalance balance : allBalances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getPharmacyLocation() == null || balance.getPharmacyLocation().getPharmacyLocationId() == null
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer pharmacyLocationId = balance.getPharmacyLocation().getPharmacyLocationId();
            if (activePharmacy.getPharmacyLocationId().equals(pharmacyLocationId)) {
                continue;
            }
            PharmacyLocation sourcePharmacy = activePharmaciesById.get(pharmacyLocationId);
            if (sourcePharmacy == null || !isEligibleSourcePharmacy(sourcePharmacy)) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            Map<Integer, AlternativeSource> sourcesByPharmacyId = sourcesByItemId.get(itemId);
            if (sourcesByPharmacyId == null) {
                sourcesByPharmacyId = new LinkedHashMap<Integer, AlternativeSource>();
                sourcesByItemId.put(itemId, sourcesByPharmacyId);
            }
            AlternativeSource source = sourcesByPharmacyId.get(pharmacyLocationId);
            if (source == null) {
                source = new AlternativeSource(sourcePharmacy, BigDecimal.ZERO);
                sourcesByPharmacyId.put(pharmacyLocationId, source);
            }
            source.availableQuantity = source.availableQuantity.add(balance.getQuantityOnHand());
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getPatientDispensingRequestLineId() == null || line.getItem() == null
                    || line.getItem().getItemId() == null || line.getRequestedQuantity() == null) {
                continue;
            }
            BigDecimal localQuantity = localQuantityByItemId.containsKey(line.getItem().getItemId())
                    ? localQuantityByItemId.get(line.getItem().getItemId()) : BigDecimal.ZERO;
            if (localQuantity.compareTo(line.getRequestedQuantity()) >= 0) {
                continue;
            }
            Map<Integer, AlternativeSource> sourcesByPharmacyId = sourcesByItemId.get(line.getItem().getItemId());
            String sourceSummary = sourceSummary(sourcesByPharmacyId);
            if (!sourceSummary.isEmpty()) {
                hintsByLineId.put(line.getPatientDispensingRequestLineId(), sourceSummary);
            }
        }
        return hintsByLineId;
    }

    private boolean isEligibleSourcePharmacy(PharmacyLocation pharmacyLocation) {
        return pharmacyLocation.getLocationType() == PharmacyLocationType.MAIN_PHARMACY
                || pharmacyLocation.getLocationType() == PharmacyLocationType.DISPENSING_PHARMACY;
    }

    private String sourceSummary(Map<Integer, AlternativeSource> sourcesByPharmacyId) {
        if (sourcesByPharmacyId == null || sourcesByPharmacyId.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder();
        for (AlternativeSource source : sourcesByPharmacyId.values()) {
            if (source == null || source.pharmacyLocation == null || source.availableQuantity == null
                    || source.availableQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(source.pharmacyLocation.getName())
                    .append(" (")
                    .append(stripTrailingZeros(source.availableQuantity))
                    .append(" available)");
        }
        return summary.toString();
    }

    private String stripTrailingZeros(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }

    private static class AlternativeSource {
        private final PharmacyLocation pharmacyLocation;
        private BigDecimal availableQuantity;

        private AlternativeSource(PharmacyLocation pharmacyLocation, BigDecimal availableQuantity) {
            this.pharmacyLocation = pharmacyLocation;
            this.availableQuantity = availableQuantity;
        }
    }

    private PatientDispensingRequestLine findRequestLine(List<PatientDispensingRequestLine> requestLines, Integer requestLineId) {
        if (requestLines == null || requestLineId == null) {
            return null;
        }
        for (PatientDispensingRequestLine requestLine : requestLines) {
            if (requestLine != null && requestLine.getPatientDispensingRequestLineId() != null
                    && requestLine.getPatientDispensingRequestLineId().equals(requestLineId)) {
                return requestLine;
            }
        }
        return null;
    }

    private Date resolveExpiryDate(String[] expiryDates,
                                   int index,
                                   InventoryService inventoryService,
                                   PharmacyLocation activePharmacy,
                                   org.openmrs.module.rwandapharmacymanagement.model.Item item,
                                   String batchNumber) {
        Date parsed = parseDate(valueAt(expiryDates, index));
        if (parsed != null) {
            return parsed;
        }
        if (item == null || item.getItemId() == null || activePharmacy == null) {
            return null;
        }
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId())) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || !balance.getItem().getItemId().equals(item.getItemId())) {
                continue;
            }
            if ((batchNumber == null && balance.getBatchNumber() == null)
                    || (batchNumber != null && batchNumber.equals(balance.getBatchNumber()))) {
                return balance.getExpiryDate();
            }
        }
        return null;
    }

    private Date parseDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(rawDate.trim());
        }
        catch (java.text.ParseException ignored) {
            return null;
        }
    }

    private int maxLength(Object[]... arrays) {
        int max = 0;
        if (arrays == null) {
            return max;
        }
        for (Object[] array : arrays) {
            if (array != null && array.length > max) {
                max = array.length;
            }
        }
        return max;
    }

    private <T> T valueAt(T[] values, int index) {
        return values != null && values.length > index ? values[index] : null;
    }

}
