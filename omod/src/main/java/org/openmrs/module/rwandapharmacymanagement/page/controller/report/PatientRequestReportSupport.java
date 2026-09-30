package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class PatientRequestReportSupport {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private PatientRequestReportSupport() {
    }

    public static List<Row> buildRows(List<PatientDispensingRequest> requests) {
        List<Row> rows = new ArrayList<Row>();
        if (requests == null) {
            return rows;
        }
        for (PatientDispensingRequest request : requests) {
            if (request == null) {
                continue;
            }
            if (request.getLines() == null || request.getLines().isEmpty()) {
                rows.add(buildRow(request, null));
                continue;
            }
            for (PatientDispensingRequestLine line : request.getLines()) {
                if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                    continue;
                }
                rows.add(buildRow(request, line));
            }
        }
        return rows;
    }

    public static Totals totals(List<Row> rows) {
        Totals totals = new Totals();
        if (rows == null) {
            return totals;
        }
        totals.lineCount = rows.size();
        java.util.Set<Integer> requestIds = new java.util.HashSet<Integer>();
        for (Row row : rows) {
            if (row == null) {
                continue;
            }
            if (row.requestId != null) {
                requestIds.add(row.requestId);
            }
            totals.requestedQuantity = totals.requestedQuantity.add(row.requestedQuantity == null ? BigDecimal.ZERO : row.requestedQuantity);
            if ("PRESCRIPTION".equals(row.requestType)) {
                totals.prescriptionLineCount++;
            } else if ("CONSUMABLE".equals(row.requestType)) {
                totals.consumableLineCount++;
            }
        }
        totals.requestCount = requestIds.size();
        return totals;
    }

    public static Date parseDate(String value, Date fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(value.trim());
        }
        catch (ParseException ex) {
            return fallback;
        }
    }

    public static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_FORMAT).format(date);
    }

    public static Date startOfDay(Date date) {
        return DispensingReportSupport.startOfDay(date);
    }

    public static Date nextDay(Date date) {
        return DispensingReportSupport.nextDay(date);
    }

    public static RequestType parseRequestType(String value) {
        if (value == null || value.trim().isEmpty() || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return RequestType.valueOf(value.trim().toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Row buildRow(PatientDispensingRequest request, PatientDispensingRequestLine line) {
        Patient patient = request.getPatient();
        Item item = line == null ? null : line.getItem();
        Row row = new Row();
        row.requestId = request.getPatientDispensingRequestId();
        row.requestLineId = line == null ? null : line.getPatientDispensingRequestLineId();
        row.requestDate = formatDateTime(request.getDateCreated());
        row.patientName = patientName(patient);
        row.patientIdentifier = patientIdentifier(patient);
        row.requestType = request.getRequestType() == null ? "" : request.getRequestType().name();
        row.requestStatus = request.getRequestStatus() == null ? "" : request.getRequestStatus().name();
        row.lineStatus = line == null || line.getLineStatus() == null ? "" : line.getLineStatus().name();
        row.requestedBy = firstNonBlank(providerName(request.getOrderingProvider()), userName(request.getCreator()));
        row.requestingLocation = request.getRequestingLocation() == null ? "" : safe(request.getRequestingLocation().getName());
        row.targetPharmacy = pharmacyName(request.getTargetPharmacyLocation());
        row.routedPharmacy = line == null ? "" : pharmacyName(line.getRoutedPharmacyLocation());
        row.itemName = item == null ? "" : safe(item.getName());
        row.itemCode = item == null ? "" : safe(item.getCode());
        row.requestedQuantity = line == null ? null : line.getRequestedQuantity();
        row.unitOfMeasure = line == null ? "" : safe(line.getUnitOfMeasure());
        row.billable = line == null ? request.getBillable() : line.getBillable();
        row.billingExclusionReason = line == null ? "" : safe(line.getBillingExclusionReason());
        row.patientBillId = request.getPatientBillId();
        row.paymentConfirmed = Boolean.TRUE.equals(request.getPaymentConfirmed());
        row.paymentExceptionApproved = Boolean.TRUE.equals(request.getPaymentExceptionApproved());
        row.reference = safe(request.getMedicalActReference());
        return row;
    }

    private static String patientName(Patient patient) {
        if (patient == null) {
            return "";
        }
        PersonName personName = patient.getPersonName();
        return personName == null ? patient.toString() : safe(personName.getFullName());
    }

    private static String patientIdentifier(Patient patient) {
        if (patient == null) {
            return "";
        }
        PatientIdentifier identifier = patient.getPatientIdentifier();
        return identifier == null ? "" : safe(identifier.getIdentifier());
    }

    private static String userName(User user) {
        return UserNameUtils.plainName(user);
    }

    private static String providerName(Provider provider) {
        return provider == null ? "" : safe(provider.getName());
    }

    private static String pharmacyName(PharmacyLocation pharmacyLocation) {
        return pharmacyLocation == null ? "" : safe(pharmacyLocation.getName());
    }

    private static String formatDateTime(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_TIME_FORMAT).format(date);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : safe(second);
    }

    public static class Row {
        public Integer requestId;
        public Integer requestLineId;
        public String requestDate;
        public String patientName;
        public String patientIdentifier;
        public String requestType;
        public String requestStatus;
        public String lineStatus;
        public String requestedBy;
        public String requestingLocation;
        public String targetPharmacy;
        public String routedPharmacy;
        public String itemName;
        public String itemCode;
        public BigDecimal requestedQuantity;
        public String unitOfMeasure;
        public Boolean billable;
        public String billingExclusionReason;
        public Integer patientBillId;
        public Boolean paymentConfirmed;
        public Boolean paymentExceptionApproved;
        public String reference;
    }

    public static class Totals {
        public int requestCount;
        public int lineCount;
        public int prescriptionLineCount;
        public int consumableLineCount;
        public BigDecimal requestedQuantity = BigDecimal.ZERO;
    }
}
