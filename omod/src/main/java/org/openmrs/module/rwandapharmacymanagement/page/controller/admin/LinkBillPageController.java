package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class LinkBillPageController {
    private static final String FLASH_LINKED_REQUEST_ID = "rpm.linkBill.linkedRequestId";
    private static final String FLASH_ERROR_MESSAGE = "rpm.linkBill.errorMessage";

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @RequestParam(value = "requestId", required = false) Integer requestId,
                    @RequestParam(value = "patient", required = false) String patientSearch,
                    HttpSession session) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (PatientDispensingRequest request : dispensingService.getPendingRequests()) {
            if (!isMissingLinkedBillPrescription(request)) {
                continue;
            }
            if (requestId != null && !requestId.equals(request.getPatientDispensingRequestId())) {
                continue;
            }
            if (!matchesPatientSearch(request.getPatient(), patientSearch)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("request", request);
            Integer suggestedBillId = request.getPatient() == null ? null
                    : dispensingService.findLatestPatientBillId(request.getPatient().getPatientId());
            row.put("suggestedBillId", suggestedBillId);
            row.put("suggestedConsommationId", findConsommationIdByPatientBillId(suggestedBillId));
            rows.add(row);
        }
        model.addAttribute("rows", rows);
        model.addAttribute("requestId", requestId);
        model.addAttribute("patientSearch", patientSearch);
        model.addAttribute("linkedRequestId", FlashMessageSupport.consumeInteger(session, FLASH_LINKED_REQUEST_ID));
        model.addAttribute("errorMessage", FlashMessageSupport.consumeString(session, FLASH_ERROR_MESSAGE));
    }

    public String post(@SpringBean("dispensingService") DispensingService dispensingService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam(value = "patientBillId", required = false) Integer patientBillId,
                       @RequestParam(value = "consommationId", required = false) Integer consommationId,
                       HttpSession session,
                       UiUtils ui) {
        try {
            PatientDispensingRequest request = dispensingService.getRequest(requestId);
            if (request == null || !isMissingLinkedBillPrescription(request)) {
                throw new IllegalArgumentException("Request was not found or does not need bill linking.");
            }
            patientBillId = resolvePatientBillId(patientBillId, consommationId);
            Boolean paid = dispensingService.isPatientBillPaid(patientBillId);
            if (paid == null) {
                throw new IllegalArgumentException("Patient bill " + patientBillId + " was not found.");
            }
            request.setPatientBillId(patientBillId);
            request.setPaymentConfirmed(Boolean.TRUE.equals(paid));
            request.setRequestStatus(Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
            updateLineStatuses(request, Boolean.TRUE.equals(paid));
            dispensingService.saveRequest(request);

            FlashMessageSupport.set(session, FLASH_LINKED_REQUEST_ID, requestId);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/linkBill");
        }
        catch (IllegalArgumentException e) {
            FlashMessageSupport.set(session, FLASH_ERROR_MESSAGE, e.getMessage());
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/linkBill");
        }
    }

    private Integer resolvePatientBillId(Integer patientBillId, Integer consommationId) {
        if (patientBillId != null) {
            return patientBillId;
        }
        if (consommationId == null) {
            throw new IllegalArgumentException("Enter either a patient bill ID or a consommation ID.");
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                throw new IllegalArgumentException("MOH Billing service is not available.");
            }
            Object consommation = billingServiceClass.getMethod("getConsommation", Integer.class)
                    .invoke(billingService, consommationId);
            Object patientBill = invokeNoArg(consommation, "getPatientBill");
            Integer resolvedPatientBillId = extractInteger(patientBill, "getPatientBillId");
            if (resolvedPatientBillId == null) {
                throw new IllegalArgumentException("Consommation " + consommationId + " has no patient bill.");
            }
            return resolvedPatientBillId;
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Consommation " + consommationId + " was not found.");
        }
    }

    private Integer findConsommationIdByPatientBillId(Integer patientBillId) {
        if (patientBillId == null) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return null;
            }
            Object patientBill = billingServiceClass.getMethod("getPatientBill", Integer.class)
                    .invoke(billingService, patientBillId);
            Object consommation = patientBill == null ? null
                    : billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            return extractInteger(consommation, "getConsommationId");
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
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Integer extractInteger(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Integer ? (Integer) value : null;
    }

    private boolean isMissingLinkedBillPrescription(PatientDispensingRequest request) {
        if (request == null || request.getPatientBillId() != null || Boolean.TRUE.equals(request.getVoided())) {
            return false;
        }
        if (request.getRequestType() != null && request.getRequestType() != RequestType.PRESCRIPTION
                && request.getRequestType() != RequestType.CONSUMABLE) {
            return false;
        }
        if (!Boolean.TRUE.equals(request.getBillable())) {
            return false;
        }
        return request.getRequestStatus() == RequestStatus.PENDING_PAYMENT
                || request.getRequestStatus() == RequestStatus.PENDING_DISPENSE
                || request.getRequestStatus() == RequestStatus.IN_PROGRESS;
    }

    private void updateLineStatuses(PatientDispensingRequest request, boolean paid) {
        if (request == null || request.getLines() == null) {
            return;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || isFinalStatus(line.getLineStatus())) {
                continue;
            }
            line.setLineStatus(paid ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        }
    }

    private boolean isFinalStatus(RequestStatus status) {
        return status == RequestStatus.DISPENSED
                || status == RequestStatus.PARTIALLY_DISPENSED
                || status == RequestStatus.UNAVAILABLE
                || status == RequestStatus.CANCELLED;
    }

    private boolean matchesPatientSearch(Patient patient, String patientSearch) {
        String search = patientSearch == null ? "" : patientSearch.trim().toLowerCase(Locale.ENGLISH);
        if (search.isEmpty()) {
            return true;
        }
        if (patient == null) {
            return false;
        }
        String identifier = patient.getPatientIdentifier() == null ? "" : patient.getPatientIdentifier().getIdentifier();
        String name = patient.getPersonName() == null ? "" : patient.getPersonName().getFullName();
        return identifier.toLowerCase(Locale.ENGLISH).contains(search)
                || name.toLowerCase(Locale.ENGLISH).contains(search);
    }
}
