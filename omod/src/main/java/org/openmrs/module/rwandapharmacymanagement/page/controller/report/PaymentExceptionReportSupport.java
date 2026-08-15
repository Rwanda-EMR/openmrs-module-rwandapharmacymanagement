package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PaymentExceptionReportSupport {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "dd/MMM/yyyy HH:mm";

    private PaymentExceptionReportSupport() {
    }

    public static List<Row> buildRows(List<PatientDispensingRequest> requests, List<DispenseRecord> dispenseRecords) {
        Map<Integer, DispenseSummary> dispenseSummaryByRequestId = summarizeDispenses(dispenseRecords);
        List<Row> rows = new ArrayList<Row>();
        if (requests == null) {
            return rows;
        }
        for (PatientDispensingRequest request : requests) {
            if (request == null) {
                continue;
            }
            rows.add(buildRow(request, dispenseSummaryByRequestId.get(request.getPatientDispensingRequestId())));
        }
        return rows;
    }

    public static Totals totals(List<Row> rows) {
        Totals totals = new Totals();
        if (rows == null) {
            return totals;
        }
        totals.requestCount = rows.size();
        for (Row row : rows) {
            if (row == null) {
                continue;
            }
            if (Boolean.TRUE.equals(row.dispensed)) {
                totals.dispensedCount++;
            } else {
                totals.pendingDispenseCount++;
            }
            if ("PRESCRIPTION".equals(row.requestType)) {
                totals.prescriptionCount++;
            } else if ("CONSUMABLE".equals(row.requestType)) {
                totals.consumableCount++;
            }
        }
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

    private static Row buildRow(PatientDispensingRequest request, DispenseSummary dispenseSummary) {
        Patient patient = request.getPatient();
        Row row = new Row();
        row.requestId = request.getPatientDispensingRequestId();
        row.requestDate = formatDateTime(request.getDateCreated());
        row.approvedDate = formatDateTime(request.getPaymentExceptionApprovedDate());
        row.patientName = patientName(patient);
        row.patientIdentifier = patientIdentifier(patient);
        row.patientId = patient == null ? null : patient.getPatientId();
        row.requestType = request.getRequestType() == null ? "" : request.getRequestType().name();
        row.requestStatus = request.getRequestStatus() == null ? "" : request.getRequestStatus().name();
        row.billId = request.getPatientBillId();
        row.paymentConfirmed = Boolean.TRUE.equals(request.getPaymentConfirmed());
        row.exceptionType = request.getPaymentExceptionType() == null ? "" : request.getPaymentExceptionType().name();
        row.exceptionReason = safe(request.getPaymentExceptionReason());
        row.exceptionNote = safe(request.getPaymentExceptionNote());
        row.approvedBy = userName(request.getPaymentExceptionApprovedBy());
        row.requestedBy = firstNonBlank(providerName(request.getOrderingProvider()), userName(request.getCreator()));
        row.requestReference = safe(request.getMedicalActReference());
        row.requestingLocation = request.getRequestingLocation() == null ? "" : safe(request.getRequestingLocation().getName());
        row.targetPharmacy = pharmacyName(request.getTargetPharmacyLocation());
        row.items = lineItems(request.getLines());
        row.itemCount = request.getLines() == null ? 0 : request.getLines().size();
        row.dispensed = dispenseSummary != null && dispenseSummary.dispensed;
        row.dispenseDates = dispenseSummary == null ? "" : dispenseSummary.dispenseDates;
        row.dispensingPharmacies = dispenseSummary == null ? "" : dispenseSummary.dispensingPharmacies;
        row.dispensedBy = dispenseSummary == null ? "" : dispenseSummary.dispensedBy;
        return row;
    }

    private static Map<Integer, DispenseSummary> summarizeDispenses(List<DispenseRecord> dispenseRecords) {
        Map<Integer, DispenseSummary> summaries = new LinkedHashMap<Integer, DispenseSummary>();
        if (dispenseRecords == null) {
            return summaries;
        }
        for (DispenseRecord record : dispenseRecords) {
            if (record == null || record.getPatientDispensingRequest() == null
                    || record.getPatientDispensingRequest().getPatientDispensingRequestId() == null) {
                continue;
            }
            Integer requestId = record.getPatientDispensingRequest().getPatientDispensingRequestId();
            DispenseSummary summary = summaries.get(requestId);
            if (summary == null) {
                summary = new DispenseSummary();
                summaries.put(requestId, summary);
            }
            summary.dispensed = true;
            summary.dispenseDates = appendUnique(summary.dispenseDates, formatDateTime(record.getDateCreated()));
            summary.dispensingPharmacies = appendUnique(summary.dispensingPharmacies, pharmacyName(record.getDispensingPharmacyLocation()));
            summary.dispensedBy = appendUnique(summary.dispensedBy,
                    firstNonBlank(providerName(record.getDispensedBy()), userName(record.getCreator())));
        }
        return summaries;
    }

    private static String lineItems(List<PatientDispensingRequestLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (PatientDispensingRequestLine line : lines) {
            if (line == null || line.getItem() == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(safe(line.getItem().getName()));
            if (line.getRequestedQuantity() != null) {
                builder.append(" x ").append(line.getRequestedQuantity());
            }
            if (line.getUnitOfMeasure() != null && !line.getUnitOfMeasure().trim().isEmpty()) {
                builder.append(" ").append(line.getUnitOfMeasure().trim());
            }
        }
        return builder.toString();
    }

    private static String appendUnique(String existing, String value) {
        String normalized = safe(value);
        if (normalized.isEmpty()) {
            return safe(existing);
        }
        String current = safe(existing);
        if (current.isEmpty()) {
            return normalized;
        }
        if ((";" + current + ";").contains(";" + normalized + ";")) {
            return current;
        }
        return current + "; " + normalized;
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
        public String requestDate;
        public String approvedDate;
        public Integer patientId;
        public String patientName;
        public String patientIdentifier;
        public String requestType;
        public String requestStatus;
        public Integer billId;
        public Boolean paymentConfirmed;
        public String exceptionType;
        public String exceptionReason;
        public String exceptionNote;
        public String approvedBy;
        public String requestedBy;
        public String requestReference;
        public String requestingLocation;
        public String targetPharmacy;
        public String items;
        public int itemCount;
        public Boolean dispensed;
        public String dispenseDates;
        public String dispensingPharmacies;
        public String dispensedBy;
    }

    public static class Totals {
        public int requestCount;
        public int dispensedCount;
        public int pendingDispenseCount;
        public int prescriptionCount;
        public int consumableCount;
    }

    private static class DispenseSummary {
        private boolean dispensed;
        private String dispenseDates = "";
        private String dispensingPharmacies = "";
        private String dispensedBy = "";
    }
}
