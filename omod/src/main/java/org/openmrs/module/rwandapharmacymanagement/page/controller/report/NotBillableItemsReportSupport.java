package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NotBillableItemsReportSupport {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "dd/MMM/yyyy HH:mm";

    private NotBillableItemsReportSupport() {
    }

    public static List<Row> buildRows(List<PatientDispensingRequestLine> lines, List<DispenseRecord> dispenseRecords) {
        Map<String, DispenseSummary> summaries = summarizeDispenses(dispenseRecords);
        List<Row> rows = new ArrayList<Row>();
        if (lines == null) {
            return rows;
        }
        for (PatientDispensingRequestLine line : lines) {
            if (line == null || line.getPatientDispensingRequest() == null) {
                continue;
            }
            PatientDispensingRequest request = line.getPatientDispensingRequest();
            String summaryKey = summaryKey(request.getPatientDispensingRequestId(), line.getItem() == null ? null : line.getItem().getItemId());
            rows.add(buildRow(line, summaries.get(summaryKey)));
        }
        return rows;
    }

    public static Totals totals(List<Row> rows) {
        Totals totals = new Totals();
        if (rows == null) {
            return totals;
        }
        totals.lineCount = rows.size();
        for (Row row : rows) {
            if (row == null) {
                continue;
            }
            if ("PRESCRIPTION".equals(row.requestType)) {
                totals.prescriptionCount++;
            } else if ("CONSUMABLE".equals(row.requestType)) {
                totals.consumableCount++;
            }
            if (Boolean.TRUE.equals(row.dispensed)) {
                totals.dispensedCount++;
            } else {
                totals.pendingCount++;
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

    private static Row buildRow(PatientDispensingRequestLine line, DispenseSummary dispenseSummary) {
        PatientDispensingRequest request = line.getPatientDispensingRequest();
        Patient patient = request.getPatient();
        Row row = new Row();
        row.requestId = request.getPatientDispensingRequestId();
        row.requestLineId = line.getPatientDispensingRequestLineId();
        row.requestDate = formatDateTime(request.getDateCreated());
        row.patientName = patientName(patient);
        row.patientIdentifier = patientIdentifier(patient);
        row.requestType = request.getRequestType() == null ? "" : request.getRequestType().name();
        row.requestStatus = request.getRequestStatus() == null ? "" : request.getRequestStatus().name();
        row.lineStatus = line.getLineStatus() == null ? "" : line.getLineStatus().name();
        row.itemName = line.getItem() == null ? "" : safe(line.getItem().getName());
        row.itemCode = line.getItem() == null ? "" : safe(line.getItem().getCode());
        row.quantity = line.getRequestedQuantity();
        row.unitOfMeasure = safe(line.getUnitOfMeasure());
        row.reason = safe(line.getBillingExclusionReason());
        row.requestedBy = firstNonBlank(providerName(request.getOrderingProvider()), userName(request.getCreator()));
        row.requestingLocation = request.getRequestingLocation() == null ? "" : safe(request.getRequestingLocation().getName());
        row.targetPharmacy = pharmacyName(request.getTargetPharmacyLocation());
        row.billId = request.getPatientBillId();
        row.paymentConfirmed = Boolean.TRUE.equals(request.getPaymentConfirmed());
        row.dispensed = dispenseSummary != null && dispenseSummary.dispensed;
        row.quantityDispensed = dispenseSummary == null ? null : dispenseSummary.quantityDispensed;
        row.dispenseDates = dispenseSummary == null ? "" : dispenseSummary.dispenseDates;
        row.dispensingPharmacies = dispenseSummary == null ? "" : dispenseSummary.dispensingPharmacies;
        row.dispensedBy = dispenseSummary == null ? "" : dispenseSummary.dispensedBy;
        return row;
    }

    private static Map<String, DispenseSummary> summarizeDispenses(List<DispenseRecord> dispenseRecords) {
        Map<String, DispenseSummary> summaries = new LinkedHashMap<String, DispenseSummary>();
        if (dispenseRecords == null) {
            return summaries;
        }
        for (DispenseRecord record : dispenseRecords) {
            if (record == null || record.getPatientDispensingRequest() == null
                    || record.getPatientDispensingRequest().getPatientDispensingRequestId() == null
                    || record.getLines() == null) {
                continue;
            }
            Integer requestId = record.getPatientDispensingRequest().getPatientDispensingRequestId();
            for (DispenseRecordLine line : record.getLines()) {
                if (line == null || line.getItem() == null || line.getItem().getItemId() == null) {
                    continue;
                }
                String key = summaryKey(requestId, line.getItem().getItemId());
                DispenseSummary summary = summaries.get(key);
                if (summary == null) {
                    summary = new DispenseSummary();
                    summaries.put(key, summary);
                }
                summary.dispensed = true;
                summary.quantityDispensed = add(summary.quantityDispensed, line.getQuantityDispensed());
                summary.dispenseDates = appendUnique(summary.dispenseDates, formatDateTime(record.getDateCreated()));
                summary.dispensingPharmacies = appendUnique(summary.dispensingPharmacies, pharmacyName(record.getDispensingPharmacyLocation()));
                summary.dispensedBy = appendUnique(summary.dispensedBy,
                        firstNonBlank(providerName(record.getDispensedBy()), userName(record.getCreator())));
            }
        }
        return summaries;
    }

    private static BigDecimal add(BigDecimal first, BigDecimal second) {
        if (first == null) {
            return second;
        }
        return second == null ? first : first.add(second);
    }

    private static String summaryKey(Integer requestId, Integer itemId) {
        return String.valueOf(requestId) + ":" + String.valueOf(itemId);
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
        public Integer requestLineId;
        public String requestDate;
        public String patientName;
        public String patientIdentifier;
        public String requestType;
        public String requestStatus;
        public String lineStatus;
        public String itemName;
        public String itemCode;
        public BigDecimal quantity;
        public String unitOfMeasure;
        public String reason;
        public String requestedBy;
        public String requestingLocation;
        public String targetPharmacy;
        public Integer billId;
        public Boolean paymentConfirmed;
        public Boolean dispensed;
        public BigDecimal quantityDispensed;
        public String dispenseDates;
        public String dispensingPharmacies;
        public String dispensedBy;
    }

    public static class Totals {
        public int lineCount;
        public int prescriptionCount;
        public int consumableCount;
        public int dispensedCount;
        public int pendingCount;
    }

    private static class DispenseSummary {
        private boolean dispensed;
        private BigDecimal quantityDispensed;
        private String dispenseDates = "";
        private String dispensingPharmacies = "";
        private String dispensedBy = "";
    }
}
