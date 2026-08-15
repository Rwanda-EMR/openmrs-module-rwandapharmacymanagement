package org.openmrs.module.rwandapharmacymanagement.page.controller.report;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class DispensingReportSupport {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private DispensingReportSupport() {
    }

    public static List<Row> buildRows(List<DispenseRecord> dispenseRecords) {
        List<Row> rows = new ArrayList<Row>();
        if (dispenseRecords == null) {
            return rows;
        }
        for (DispenseRecord dispenseRecord : dispenseRecords) {
            if (dispenseRecord == null || dispenseRecord.getLines() == null) {
                continue;
            }
            for (DispenseRecordLine line : dispenseRecord.getLines()) {
                if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                    continue;
                }
                rows.add(buildRow(dispenseRecord, line));
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
        for (Row row : rows) {
            if (row == null) {
                continue;
            }
            totals.quantityDispensed = totals.quantityDispensed.add(nullToZero(row.quantityDispensed));
            totals.lineTotal = totals.lineTotal.add(nullToZero(row.lineTotal));
        }
        return totals;
    }

    public static Date startOfDay(Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date nextDay(Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(startOfDay(date));
        calendar.add(java.util.Calendar.DATE, 1);
        return calendar.getTime();
    }

    public static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_FORMAT).format(date);
    }

    private static Row buildRow(DispenseRecord dispenseRecord, DispenseRecordLine line) {
        Patient patient = dispenseRecord.getPatient();
        PatientDispensingRequest request = dispenseRecord.getPatientDispensingRequest();
        Row row = new Row();
        row.dispenseRecordId = dispenseRecord.getDispenseRecordId();
        row.dispenseDate = formatDateTime(dispenseRecord.getDateCreated());
        row.patientName = patientName(patient);
        row.patientIdentifier = patientIdentifier(patient);
        row.patientId = patient == null ? null : patient.getPatientId();
        row.pharmacyName = pharmacyName(dispenseRecord.getDispensingPharmacyLocation());
        row.servedWhere = pharmacyName(dispenseRecord.getDispensingPharmacyLocation());
        row.dispensedBy = userName(dispenseRecord.getCreator());
        row.servedBy = firstNonBlank(providerName(dispenseRecord.getDispensedBy()), userName(dispenseRecord.getCreator()));
        row.dispensedWithoutOrder = Boolean.TRUE.equals(dispenseRecord.getDispensedWithoutOrder());
        row.itemName = line.getItem() == null ? "" : safe(line.getItem().getName());
        row.itemCode = line.getItem() == null ? "" : safe(line.getItem().getCode());
        row.batchNumber = safe(line.getBatchNumber());
        row.expiryDate = formatDate(line.getExpiryDate());
        row.quantityDispensed = line.getQuantityDispensed();
        row.sellingPrice = line.getSellingPrice();
        row.lineTotal = line.getLineTotal();
        row.requestId = request == null ? null : request.getPatientDispensingRequestId();
        row.prescriptionReference = request == null ? "" : safe(request.getMedicalActReference());
        row.patientBillId = request == null ? null : request.getPatientBillId();
        row.requestStatus = request == null || request.getRequestStatus() == null ? "" : request.getRequestStatus().name();
        row.requestedBy = request == null ? "" : firstNonBlank(providerName(request.getOrderingProvider()), userName(request.getCreator()));
        row.requestedWhere = request == null || request.getRequestingLocation() == null ? ""
                : safe(request.getRequestingLocation().getName());
        row.targetPharmacy = request == null ? "" : pharmacyName(request.getTargetPharmacyLocation());
        return row;
    }

    private static String patientName(Patient patient) {
        if (patient == null) {
            return "";
        }
        PersonName personName = patient.getPersonName();
        return personName == null ? patient.toString() : personName.getFullName();
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

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : safe(second);
    }

    public static class Row {
        public Integer dispenseRecordId;
        public String dispenseDate;
        public Integer patientId;
        public String patientName;
        public String patientIdentifier;
        public String pharmacyName;
        public String dispensedBy;
        public String servedBy;
        public String servedWhere;
        public String requestedBy;
        public String requestedWhere;
        public String targetPharmacy;
        public Boolean dispensedWithoutOrder;
        public String itemName;
        public String itemCode;
        public String batchNumber;
        public String expiryDate;
        public BigDecimal quantityDispensed;
        public BigDecimal sellingPrice;
        public BigDecimal lineTotal;
        public Integer requestId;
        public String prescriptionReference;
        public Integer patientBillId;
        public String requestStatus;
    }

    public static class Totals {
        public int lineCount;
        public BigDecimal quantityDispensed = BigDecimal.ZERO;
        public BigDecimal lineTotal = BigDecimal.ZERO;
    }
}
