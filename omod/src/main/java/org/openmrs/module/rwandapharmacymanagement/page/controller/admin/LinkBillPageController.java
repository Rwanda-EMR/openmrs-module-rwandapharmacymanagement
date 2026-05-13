package org.openmrs.module.rwandapharmacymanagement.page.controller.admin;

import org.openmrs.Patient;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class LinkBillPageController {

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @RequestParam(value = "requestId", required = false) Integer requestId,
                    @RequestParam(value = "patient", required = false) String patientSearch,
                    @RequestParam(value = "linked", required = false) Integer linkedRequestId,
                    @RequestParam(value = "errorMessage", required = false) String errorMessage) {
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
            row.put("suggestedBillId", request.getPatient() == null ? null
                    : dispensingService.findLatestPatientBillId(request.getPatient().getPatientId()));
            rows.add(row);
        }
        model.addAttribute("rows", rows);
        model.addAttribute("requestId", requestId);
        model.addAttribute("patientSearch", patientSearch);
        model.addAttribute("linkedRequestId", linkedRequestId);
        model.addAttribute("errorMessage", errorMessage);
    }

    public String post(@SpringBean("dispensingService") DispensingService dispensingService,
                       @RequestParam("requestId") Integer requestId,
                       @RequestParam("patientBillId") Integer patientBillId,
                       UiUtils ui) {
        try {
            PatientDispensingRequest request = dispensingService.getRequest(requestId);
            if (request == null || !isMissingLinkedBillPrescription(request)) {
                throw new IllegalArgumentException("Request was not found or does not need bill linking.");
            }
            Boolean paid = dispensingService.isPatientBillPaid(patientBillId);
            if (paid == null) {
                throw new IllegalArgumentException("Patient bill " + patientBillId + " was not found.");
            }
            request.setPatientBillId(patientBillId);
            request.setPaymentConfirmed(Boolean.TRUE.equals(paid));
            request.setRequestStatus(Boolean.TRUE.equals(paid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
            updateLineStatuses(request, Boolean.TRUE.equals(paid));
            dispensingService.saveRequest(request);

            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put("linked", requestId);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/linkBill", params);
        }
        catch (IllegalArgumentException e) {
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put("errorMessage", e.getMessage());
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "admin/linkBill", params);
        }
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
