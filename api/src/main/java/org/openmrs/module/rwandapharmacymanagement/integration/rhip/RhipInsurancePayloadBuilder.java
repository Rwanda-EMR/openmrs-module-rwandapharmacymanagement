package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RhipInsurancePayloadBuilder {

    private static final String MOHBILLING_DEFAULT_FOSA_ID = "mohbilling.rhipVoucher.defaultFosaId";
    private static final String PROVIDER_LICENSE_ATTRIBUTE_TYPE_NAME = "Provider License";
    private static final String PROVIDER_LICENSE_ATTRIBUTE_TYPE_UUID = "mohbilling.rhipVoucher.providerLicenseAttributeTypeUuid";
    private static final String DIAGNOSIS_CONCEPT_IDS = "mohbilling.rhipVoucher.diagnosisConceptIds";
    private static final String MMI_RECEPTION_VISIT_ATTRIBUTE_TYPE_UUID = "rwandaemr.visitAttribute.mmiReceptionNumber.uuid";
    private static final String MMI_RECEPTION_VISIT_ATTRIBUTE_INITIALIZER_KEY = "visitAttribute.mmiReceptionNumber.uuid";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    static final String OUTPATIENT_SKIP_REASON = "MMI outpatient medicines are sent on the voucher, not in real time";
    private static final Pattern ICD_CODE_PATTERN = Pattern.compile("[A-Z0-9]{1,4}(?:\\.[A-Z0-9]{1,4})?");
    private static final Pattern ICD_CODE_WITH_NAME_PATTERN = Pattern.compile("\\b([A-Z0-9]{1,4}(?:\\.[A-Z0-9]{1,4})?)\\s*-");

    private final Log log = LogFactory.getLog(getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RhipPrescriptionPayload buildInpatientPrescription(PatientDispensingRequest request) throws java.io.IOException {
        return buildInpatientPrescription(request, resolveContext(request));
    }

    RhipPrescriptionPayload buildInpatientPrescription(PatientDispensingRequest request,
                                                       RhipContext context) throws java.io.IOException {
        if (!isMmi(context.insuranceType)) {
            return RhipPrescriptionPayload.skipped("RHIP inpatient prescription is only supported for MMI");
        }
        if (!context.inpatient) {
            return RhipPrescriptionPayload.skipped(OUTPATIENT_SKIP_REASON);
        }
        if (isBlank(context.facilityFosaId)) {
            return RhipPrescriptionPayload.skipped("facilityFosaId is missing");
        }
        if (isBlank(context.visitReferenceNumber)) {
            return RhipPrescriptionPayload.skipped("visitReferenceNumber is missing");
        }
        if (isBlank(context.licenseNumber)) {
            return RhipPrescriptionPayload.skipped("provider licenseNumber is missing");
        }
        List<Map<String, Object>> medicines = medicines(request);
        if (medicines.isEmpty()) {
            return RhipPrescriptionPayload.skipped("No RHIP medicine lines found");
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("insuranceType", "mmi");
        payload.put("facilityFosaId", context.facilityFosaId);
        payload.put("visitReferenceNumber", context.visitReferenceNumber);
        payload.put("licenseNumber", context.licenseNumber);
        if (!context.diagnosisIds.isEmpty()) {
            payload.put("diagnosisIds", context.diagnosisIds);
        }
        payload.put("medicines", medicines);
        return RhipPrescriptionPayload.ready(objectMapper.writeValueAsString(payload), context);
    }

    public RhipPrescriptionPayload buildVisitDispense(DispenseRecord dispenseRecord) throws java.io.IOException {
        PatientDispensingRequest request = dispenseRecord == null ? null : dispenseRecord.getPatientDispensingRequest();
        return buildVisitDispense(dispenseRecord, resolveContext(request));
    }

    RhipPrescriptionPayload buildVisitDispense(DispenseRecord dispenseRecord,
                                               RhipContext context) throws java.io.IOException {
        if (!isMmi(context.insuranceType) && !isRama(context.insuranceType)) {
            return RhipPrescriptionPayload.skipped("RHIP visit dispense is only supported for MMI and RAMA");
        }
        if (isMmi(context.insuranceType) && !context.inpatient) {
            return RhipPrescriptionPayload.skipped(OUTPATIENT_SKIP_REASON);
        }
        if (isMmi(context.insuranceType) && isBlank(context.facilityFosaId)) {
            return RhipPrescriptionPayload.skipped("facilityFosaId is missing");
        }
        if (isBlank(context.visitReferenceNumber)) {
            return RhipPrescriptionPayload.skipped("visitReferenceNumber is missing");
        }
        List<Map<String, Object>> prescriptions = prescriptions(dispenseRecord);
        if (prescriptions.isEmpty()) {
            return RhipPrescriptionPayload.skipped("No RHIP dispense lines found");
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("insuranceType", isMmi(context.insuranceType) ? "mmi" : "rama");
        if (!isBlank(context.facilityFosaId)) {
            payload.put("facilityFosaId", context.facilityFosaId);
        }
        payload.put("visitReferenceNumber", context.visitReferenceNumber);
        if (isRama(context.insuranceType)) {
            // MMI records the dispensing time itself and ignores dispensingDate
            payload.put("dispensingDate", formatDate(dispenseRecord == null ? null : dispenseRecord.getDateCreated()));
        }
        payload.put("prescriptions", prescriptions);
        return RhipPrescriptionPayload.ready(objectMapper.writeValueAsString(payload), context);
    }

    RhipContext resolveContext(PatientDispensingRequest request) {
        RhipContext context = new RhipContext();
        Object patientBill = resolvePatientBill(request);
        Object consommation = resolveConsommation(patientBill);
        Object globalBill = invokeNoArg(consommation, "getGlobalBill");
        Object insurancePolicy = resolveInsurancePolicy(request, patientBill, consommation);
        Object insurance = insurancePolicy == null ? invokeNoArg(globalBill, "getInsurance") : invokeNoArg(insurancePolicy, "getInsurance");

        context.insuranceType = normalizeInsuranceType(extractString(insurance, "getCategory"),
                extractString(insurance, "getName"));
        context.facilityFosaId = resolveFosaId(globalBill);
        context.inpatient = isAdmitted(globalBill);
        context.receptionNumber = resolveMmiReceptionNumber(request, insurancePolicy);
        context.visitReferenceNumber = isRama(context.insuranceType)
                ? resolveRamaVisitReferenceNumber(globalBill, context.facilityFosaId)
                : context.receptionNumber;
        context.licenseNumber = resolveProviderLicense(request == null ? null : request.getOrderingProvider());
        context.diagnosisIds = resolveDiagnosisIds(request, globalBill);
        return context;
    }

    private List<Map<String, Object>> medicines(PatientDispensingRequest request) {
        if (request == null || request.getLines() == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> medicines = new ArrayList<Map<String, Object>>();
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            String code = line.getItem() == null ? null : line.getItem().getCode();
            if (isBlank(code)) {
                continue;
            }
            Map<String, Object> medicine = new LinkedHashMap<String, Object>();
            medicine.put("code", code.trim());
            medicine.put("quantity", integerQuantity(line.getRequestedQuantity()));
            DrugOrder order = line.getOrder();
            String instructions = order == null ? null : firstNonBlank(order.getDosingInstructions(), order.getInstructions());
            if (!isBlank(instructions)) {
                medicine.put("instructions", instructions.trim());
            }
            Integer durationDays = durationDays(order == null ? null : order.getDuration(),
                    order == null ? null : order.getDurationUnits());
            if (durationDays != null) {
                medicine.put("durationDays", durationDays);
            }
            medicines.add(medicine);
        }
        return medicines;
    }

    private List<Map<String, Object>> prescriptions(DispenseRecord dispenseRecord) {
        if (dispenseRecord == null || dispenseRecord.getLines() == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> prescriptions = new ArrayList<Map<String, Object>>();
        for (DispenseRecordLine line : dispenseRecord.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            String code = line.getItem() == null ? null : line.getItem().getCode();
            if (isBlank(code)) {
                continue;
            }
            Map<String, Object> prescription = new LinkedHashMap<String, Object>();
            prescription.put("productCode", code.trim());
            prescription.put("dispensedQuantity", integerQuantity(line.getQuantityDispensed()));
            prescriptions.add(prescription);
        }
        return prescriptions;
    }

    private Object resolvePatientBill(PatientDispensingRequest request) {
        if (request == null || request.getPatientBillId() == null) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            return billingService == null ? null : billingServiceClass.getMethod("getPatientBill", Integer.class)
                    .invoke(billingService, request.getPatientBillId());
        }
        catch (Exception e) {
            log.debug("Unable to resolve RHIP patient bill", e);
            return null;
        }
    }

    private Object resolveConsommation(Object patientBill) {
        if (patientBill == null) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Object billingService = Context.getService(billingServiceClass);
            return billingService == null ? null : billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
        }
        catch (Exception e) {
            log.debug("Unable to resolve RHIP consommation", e);
            return null;
        }
    }

    private Object resolveInsurancePolicy(PatientDispensingRequest request, Object patientBill, Object consommation) {
        Object beneficiary = consommation == null ? null : invokeNoArg(consommation, "getBeneficiary");
        Object policy = beneficiary == null ? null : invokeNoArg(beneficiary, "getInsurancePolicy");
        if (policy != null) {
            return policy;
        }
        return resolveInsurancePolicyFromPatient(request == null ? null : request.getPatient());
    }

    private Object resolveInsurancePolicyFromPatient(Patient patient) {
        if (patient == null) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            Object policies = billingService == null ? null : billingServiceClass
                    .getMethod("getAllInsurancePoliciesByPatient", Patient.class).invoke(billingService, patient);
            if (policies instanceof Iterable) {
                for (Object policy : (Iterable<?>) policies) {
                    if (policy != null) {
                        return policy;
                    }
                }
            }
        }
        catch (Exception e) {
            log.debug("Unable to resolve RHIP insurance policy from patient", e);
        }
        return null;
    }

    /**
     * A patient is an inpatient when the admission on their global bill is marked admitted, as mohbilling decides
     * IN_PATIENT for the voucher
     */
    private boolean isAdmitted(Object globalBill) {
        Object admission = invokeNoArg(globalBill, "getAdmission");
        return Boolean.TRUE.equals(invokeNoArg(admission, "getIsAdmitted"));
    }

    /**
     * Builds the /practitioner/medicines/delivery body for a delivery receipt, so MMI can bill in-house medicines at the
     * delivery price. tariffUponDelivery is the receipt unit price plus the configured markup, in whole francs. RPM
     * counts stock in units, so each unit is sent as a box of one.
     */
    public RhipPrescriptionPayload buildMedicineDelivery(DeliveryReceipt receipt) throws java.io.IOException {
        return buildMedicineDelivery(receipt, resolveFosaId(null), deliveryMarkupPercent());
    }

    RhipPrescriptionPayload buildMedicineDelivery(DeliveryReceipt receipt, String facilityFosaId,
                                                  BigDecimal markupPercent) throws java.io.IOException {
        if (receipt == null) {
            return RhipPrescriptionPayload.skipped("Delivery receipt is missing");
        }
        if (isBlank(facilityFosaId)) {
            return RhipPrescriptionPayload.skipped("facilityFosaId is missing");
        }
        List<Map<String, Object>> products = new ArrayList<Map<String, Object>>();
        List<String> notSent = new ArrayList<String>();
        if (receipt.getLines() != null) {
            for (DeliveryReceiptLine line : receipt.getLines()) {
                if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                    continue;
                }
                String code = line.getItem() == null ? null : trimToNull(line.getItem().getCode());
                String label = code != null ? code : (line.getItem() == null ? "line without item" : line.getItem().getName());
                Integer units = line.getAcceptedQuantity() == null ? null
                        : Integer.valueOf(line.getAcceptedQuantity().setScale(0, BigDecimal.ROUND_DOWN).intValue());
                BigDecimal tariff = deliveryTariff(line.getUnitPrice(), markupPercent);
                if (code == null) {
                    notSent.add(label + " (no code)");
                    continue;
                }
                if (units == null || units.intValue() <= 0) {
                    notSent.add(label + " (no accepted quantity)");
                    continue;
                }
                if (tariff == null) {
                    notSent.add(label + " (no unit price)");
                    continue;
                }
                Map<String, Object> product = new LinkedHashMap<String, Object>();
                product.put("productCode", code);
                product.put("tariffUponDelivery", Integer.valueOf(tariff.intValue()));
                product.put("quantityPerBox", Integer.valueOf(1));
                product.put("box", units);
                products.add(product);
            }
        }
        if (products.isEmpty()) {
            return RhipPrescriptionPayload.skipped("No delivery line can be sent to MMI"
                    + (notSent.isEmpty() ? "" : ": " + join(notSent)));
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("insuranceType", "mmi");
        payload.put("facilityFosaId", facilityFosaId);
        payload.put("referenceNumber", deliveryReferenceNumber(receipt));
        payload.put("deliveryDate", formatDate(receipt.getDateCreated()));
        payload.put("products", products);
        RhipPrescriptionPayload result = RhipPrescriptionPayload.ready(objectMapper.writeValueAsString(payload), null);
        result.notSentLines = notSent;
        return result;
    }

    /**
     * @return the reference MMI knows this delivery by; medicine lines on the voucher send it as deliveryReferenceNumber
     */
    public static String deliveryReferenceNumber(DeliveryReceipt receipt) {
        if (receipt == null) {
            return null;
        }
        String receiptNumber = receipt.getReceiptNumber() == null ? null : receipt.getReceiptNumber().trim();
        if (receiptNumber != null && !receiptNumber.isEmpty()) {
            return receiptNumber;
        }
        return receipt.getDeliveryReceiptId() == null ? null : "DR-" + receipt.getDeliveryReceiptId();
    }

    static BigDecimal deliveryTariff(BigDecimal unitPrice, BigDecimal markupPercent) {
        if (unitPrice == null || unitPrice.signum() <= 0) {
            return null;
        }
        BigDecimal markup = markupPercent == null ? BigDecimal.ZERO : markupPercent;
        BigDecimal tariff = unitPrice.multiply(BigDecimal.ONE.add(markup.movePointLeft(2)))
                .setScale(0, BigDecimal.ROUND_HALF_UP);
        return tariff.signum() > 0 ? tariff : BigDecimal.ONE;
    }

    private BigDecimal deliveryMarkupPercent() {
        String configured = gp(RwandaPharmacyManagementConfig.GP_RHIP_DELIVERY_MARKUP_PERCENT, "20");
        try {
            return new BigDecimal(isBlank(configured) ? "20" : configured.trim());
        }
        catch (NumberFormatException e) {
            log.warn("Invalid " + RwandaPharmacyManagementConfig.GP_RHIP_DELIVERY_MARKUP_PERCENT + ": " + configured);
            return new BigDecimal("20");
        }
    }

    private String join(List<String> values) {
        StringBuilder ret = new StringBuilder();
        for (String value : values) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(value);
        }
        return ret.toString();
    }

    /**
     * @return the facility FOSA ID sent on every RHIP call: the one global property shared with rwandaemr and mohbilling
     */
    private String resolveFosaId(Object globalBill) {
        return trimToNull(gp(MOHBILLING_DEFAULT_FOSA_ID, null));
    }

    private String resolveMmiReceptionNumber(PatientDispensingRequest request, Object insurancePolicy) {
        String value = resolveMmiReceptionNumberFromVisit(request == null ? null : request.getVisit());
        if (!isBlank(value)) {
            return value;
        }
        String identifier = extractString(insurancePolicy, "getInsuranceCardNo");
        value = resolveMmiReceptionNumberFromLog(identifier, null);
        if (!isBlank(value)) {
            return value;
        }
        Patient patient = request == null ? null : request.getPatient();
        return resolveMmiReceptionNumberFromLog(null, patient == null ? null : patient.getPatientId());
    }

    private String resolveMmiReceptionNumberFromVisit(Visit visit) {
        if (visit == null) {
            return null;
        }
        VisitAttributeType attributeType = resolveMmiReceptionAttributeType();
        if (attributeType == null) {
            return null;
        }
        for (VisitAttribute attribute : visit.getActiveAttributes()) {
            if (attribute != null && attributeType.equals(attribute.getAttributeType())) {
                Object value = attribute.getValue();
                return value == null ? null : trimToNull(value.toString());
            }
        }
        return null;
    }

    private VisitAttributeType resolveMmiReceptionAttributeType() {
        String uuid = firstNonBlank(gp(MMI_RECEPTION_VISIT_ATTRIBUTE_TYPE_UUID, null),
                resolveInitializerValue(MMI_RECEPTION_VISIT_ATTRIBUTE_INITIALIZER_KEY));
        if (!isBlank(uuid)) {
            VisitAttributeType attributeType = Context.getVisitService().getVisitAttributeTypeByUuid(uuid);
            if (attributeType != null) {
                return attributeType;
            }
        }
        List<VisitAttributeType> attributeTypes = Context.getVisitService().getAllVisitAttributeTypes();
        if (attributeTypes == null) {
            return null;
        }
        for (VisitAttributeType attributeType : attributeTypes) {
            if (attributeType != null && attributeType.getName() != null) {
                String name = attributeType.getName().toLowerCase(Locale.ENGLISH);
                if (name.contains("mmi") && name.contains("reception")) {
                    return attributeType;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String resolveMmiReceptionNumberFromLog(String patientIdentifier, Integer patientId) {
        if (isBlank(patientIdentifier) && patientId == null) {
            return null;
        }
        StringBuilder sql = new StringBuilder("select reception_number from mmi_patient_reception_log where status = 'SUCCESS' ");
        if (!isBlank(patientIdentifier)) {
            sql.append("and patient_identifier = '").append(escapeSql(patientIdentifier.trim())).append("' ");
        } else {
            sql.append("and patient_id = ").append(patientId).append(" ");
        }
        sql.append("and reception_number is not null and reception_number <> '' order by date_created desc limit 1");
        try {
            List<List<Object>> rows = Context.getAdministrationService().executeSQL(sql.toString(), true);
            return rows == null || rows.isEmpty() || rows.get(0).isEmpty() || rows.get(0).get(0) == null
                    ? null : trimToNull(String.valueOf(rows.get(0).get(0)));
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * @return the prescriber's licence number: clinicians enter it on their transferapp profile, kept in the
     * "Provider License" provider attribute (the type in mohbilling.rhipVoucher.providerLicenseAttributeTypeUuid is only
     * read when it is empty). The ordering provider is used first, then the logged-in user.
     */
    private String resolveProviderLicense(Provider provider) {
        List<ProviderAttributeType> types = providerLicenseAttributeTypes();
        String fromProvider = providerAttributeValue(provider, types);
        if (!isBlank(fromProvider)) {
            return fromProvider;
        }
        User user = null;
        try {
            user = Context.getAuthenticatedUser();
        }
        catch (Exception ignored) {
        }
        if (user == null || user.getPerson() == null) {
            return null;
        }
        java.util.Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(user.getPerson(), false);
        if (providers != null) {
            for (Provider userProvider : providers) {
                String value = providerAttributeValue(userProvider, types);
                if (!isBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private List<ProviderAttributeType> providerLicenseAttributeTypes() {
        List<ProviderAttributeType> ret = new ArrayList<ProviderAttributeType>();
        List<ProviderAttributeType> all = Context.getProviderService().getAllProviderAttributeTypes(false);
        if (all != null) {
            for (ProviderAttributeType type : all) {
                if (type != null && PROVIDER_LICENSE_ATTRIBUTE_TYPE_NAME.equals(type.getName())) {
                    ret.add(type);
                }
            }
        }
        String legacyUuid = trimToNull(gp(PROVIDER_LICENSE_ATTRIBUTE_TYPE_UUID, null));
        if (legacyUuid != null) {
            ProviderAttributeType legacy = Context.getProviderService().getProviderAttributeTypeByUuid(legacyUuid);
            if (legacy != null && !ret.contains(legacy)) {
                ret.add(legacy);
            }
        }
        return ret;
    }

    private String providerAttributeValue(Provider provider, List<ProviderAttributeType> types) {
        if (provider == null || provider.getActiveAttributes() == null) {
            return null;
        }
        for (ProviderAttributeType type : types) {
            for (ProviderAttribute attribute : provider.getActiveAttributes()) {
                if (attribute != null && type.equals(attribute.getAttributeType())) {
                    String value = attribute.getValueReference();
                    if (isBlank(value) && attribute.getValue() != null) {
                        value = attribute.getValue().toString();
                    }
                    if (!isBlank(value)) {
                        return value.trim();
                    }
                }
            }
        }
        return null;
    }

    private List<String> resolveDiagnosisIds(PatientDispensingRequest request, Object globalBill) {
        List<String> ids = resolveIcd11Diagnoses(request == null ? null : request.getPatient(),
                request == null ? null : request.getEncounter());
        if (!ids.isEmpty()) {
            return ids;
        }
        String notes = resolveBillingDiagnosisNotes(request == null ? null : request.getPatient(), globalBill);
        return parseDiagnosisIds(notes);
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveIcd11Diagnoses(Patient patient, Encounter encounter) {
        if (patient == null) {
            return Collections.emptyList();
        }
        try {
            Class<?> serviceClass = Context.loadClass("org.openmrs.module.rwandaemr.icd11.Icd11Service");
            Object service = Context.getService(serviceClass);
            Object diagnoses = service == null ? null : serviceClass.getMethod("getDiagnoses", Patient.class, Encounter.class)
                    .invoke(service, patient, encounter);
            Set<String> ids = new LinkedHashSet<String>();
            if (diagnoses instanceof Iterable) {
                for (Object diagnosis : (Iterable<?>) diagnoses) {
                    String code = extractString(diagnosis, "getIcd11Code");
                    if (!isBlank(code)) {
                        ids.add(code.trim());
                    }
                }
            }
            return ids.isEmpty() ? Collections.<String>emptyList() : new ArrayList<String>(ids);
        }
        catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String resolveBillingDiagnosisNotes(Patient patient, Object globalBill) {
        if (patient == null || globalBill == null) {
            return null;
        }
        String conceptIds = gp(DIAGNOSIS_CONCEPT_IDS, null);
        if (isBlank(conceptIds)) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            Date start = startOfDay((Date) invokeNoArg(globalBill, "getCreatedDate"));
            Date end = endOfDay(new Date());
            return billingService == null ? null : (String) billingServiceClass
                    .getMethod("getDiagnosisFromAdmissionToDischarge", String.class, Date.class, Date.class, Integer.class)
                    .invoke(billingService, conceptIds, start, end, patient.getPatientId());
        }
        catch (Exception e) {
            return null;
        }
    }

    private List<String> parseDiagnosisIds(String diagnosisNotes) {
        if (isBlank(diagnosisNotes)) {
            return Collections.emptyList();
        }
        Set<String> diagnosisCodes = new LinkedHashSet<String>();
        Matcher matcher = ICD_CODE_WITH_NAME_PATTERN.matcher(diagnosisNotes);
        while (matcher.find()) {
            String code = matcher.group(1);
            if (!isBlank(code) && ICD_CODE_PATTERN.matcher(code.trim()).matches()) {
                diagnosisCodes.add(code.trim());
            }
        }
        String[] parts = diagnosisNotes.split("[,;\\n]+");
        for (int i = 0; i < parts.length; i++) {
            String code = extractDiagnosisCode(parts[i]);
            if (!isBlank(code)) {
                diagnosisCodes.add(code);
            }
        }
        return diagnosisCodes.isEmpty() ? Collections.<String>emptyList() : new ArrayList<String>(diagnosisCodes);
    }

    private String extractDiagnosisCode(String text) {
        if (isBlank(text)) {
            return null;
        }
        String candidate = text.trim();
        int dash = candidate.indexOf('-');
        if (dash > 0) {
            candidate = candidate.substring(0, dash).trim();
        }
        String[] tokens = candidate.split("\\s+");
        if (tokens.length > 0) {
            candidate = tokens[0].trim();
        }
        return ICD_CODE_PATTERN.matcher(candidate).matches() ? candidate : null;
    }

    private String resolveRamaVisitReferenceNumber(Object globalBill, String fosaId) {
        String billIdentifier = extractString(globalBill, "getBillIdentifier");
        if (isBlank(billIdentifier) || isBlank(fosaId)) {
            return null;
        }
        return billIdentifier.trim() + fosaId.trim();
    }

    private String normalizeInsuranceType(String category, String name) {
        String normalizedCategory = trimToNull(category) == null ? "" : category.trim().toUpperCase(Locale.ENGLISH);
        String normalizedName = trimToNull(name) == null ? "" : name.trim().toUpperCase(Locale.ENGLISH);
        if (normalizedName.contains("MMI") || normalizedCategory.equals("MMI")) {
            return "mmi";
        }
        if (normalizedName.contains("RAMA") || normalizedCategory.equals("RAMA")) {
            return "rama";
        }
        if (normalizedCategory.equals("MUTUELLE") || normalizedName.contains("MUTUELLE")
                || normalizedName.contains("CBHI")) {
            return "cbhi";
        }
        if (normalizedCategory.contains("SPECIAL") || normalizedName.contains("SPECIAL")) {
            return "special_case";
        }
        if (normalizedCategory.equals("RSSB") || normalizedName.contains("RSSB")) {
            return "cbhi";
        }
        return null;
    }

    private Integer durationDays(Integer duration, Concept durationUnits) {
        if (duration == null) {
            return null;
        }
        if (durationUnits == null || isBlank(durationUnits.getDisplayString())) {
            return duration;
        }
        String unit = durationUnits.getDisplayString().trim().toLowerCase(Locale.ENGLISH);
        if (unit.contains("day") || unit.equals("d")) {
            return duration;
        }
        if (unit.contains("week") || unit.equals("wk") || unit.equals("w")) {
            return Integer.valueOf(duration.intValue() * 7);
        }
        if (unit.contains("month")) {
            return Integer.valueOf(duration.intValue() * 30);
        }
        if (unit.contains("hour") || unit.equals("h")) {
            return duration.intValue() % 24 == 0 ? Integer.valueOf(duration.intValue() / 24) : null;
        }
        return duration;
    }

    private Integer integerQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return Integer.valueOf(0);
        }
        return Integer.valueOf(quantity.setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
    }

    private String resolveInitializerValue(String key) {
        try {
            Object initializerService = Context.getService(
                    Class.forName("org.openmrs.module.initializer.api.InitializerService"));
            Object value = initializerService.getClass().getMethod("getValueFromKey", String.class)
                    .invoke(initializerService, key);
            return value == null ? null : trimToNull(value.toString());
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null || isBlank(methodName)) {
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

    private String extractString(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value == null ? null : trimToNull(value.toString());
    }

    private String gp(String property, String defaultValue) {
        try {
            return Context.getAdministrationService().getGlobalProperty(property, defaultValue);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    private Date startOfDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date endOfDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_FORMAT).format(date == null ? new Date() : date);
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private boolean isMmi(String insuranceType) {
        return "mmi".equalsIgnoreCase(trimToNull(insuranceType));
    }

    private boolean isRama(String insuranceType) {
        return "rama".equalsIgnoreCase(trimToNull(insuranceType));
    }

    private String firstNonBlank(String first, String second) {
        return firstNonBlank(first, second, null);
    }

    private String firstNonBlank(String first, String second, String third) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }
        return isBlank(third) ? null : third.trim();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class RhipPrescriptionPayload {
        private final boolean ready;
        private final String payload;
        private final String skipReason;
        private final RhipContext context;
        private List<String> notSentLines = Collections.emptyList();

        private RhipPrescriptionPayload(boolean ready, String payload, String skipReason, RhipContext context) {
            this.ready = ready;
            this.payload = payload;
            this.skipReason = skipReason;
            this.context = context;
        }

        public static RhipPrescriptionPayload ready(String payload, RhipContext context) {
            return new RhipPrescriptionPayload(true, payload, null, context);
        }

        public static RhipPrescriptionPayload skipped(String reason) {
            return new RhipPrescriptionPayload(false, "", reason, null);
        }

        public boolean isReady() {
            return ready;
        }

        public String getPayload() {
            return payload;
        }

        public String getSkipReason() {
            return skipReason;
        }

        public RhipContext getContext() {
            return context;
        }

        /**
         * @return lines left out of the payload, with the reason, e.g. a delivery line without a unit price
         */
        public List<String> getNotSentLines() {
            return notSentLines;
        }
    }

    public static class RhipContext {
        private String insuranceType;
        private String facilityFosaId;
        private String receptionNumber;
        private String visitReferenceNumber;
        private String licenseNumber;
        private List<String> diagnosisIds = Collections.emptyList();
        private boolean inpatient;

        public boolean isInpatient() {
            return inpatient;
        }

        void setInpatient(boolean inpatient) {
            this.inpatient = inpatient;
        }

        public String getInsuranceType() {
            return insuranceType;
        }

        public String getFacilityFosaId() {
            return facilityFosaId;
        }

        public String getReceptionNumber() {
            return receptionNumber;
        }

        public String getVisitReferenceNumber() {
            return visitReferenceNumber;
        }

        public String getLicenseNumber() {
            return licenseNumber;
        }

        public List<String> getDiagnosisIds() {
            return diagnosisIds;
        }

        void setInsuranceType(String insuranceType) {
            this.insuranceType = insuranceType;
        }

        void setFacilityFosaId(String facilityFosaId) {
            this.facilityFosaId = facilityFosaId;
        }

        void setReceptionNumber(String receptionNumber) {
            this.receptionNumber = receptionNumber;
        }

        void setVisitReferenceNumber(String visitReferenceNumber) {
            this.visitReferenceNumber = visitReferenceNumber;
        }

        void setLicenseNumber(String licenseNumber) {
            this.licenseNumber = licenseNumber;
        }

        void setDiagnosisIds(List<String> diagnosisIds) {
            this.diagnosisIds = diagnosisIds == null ? Collections.<String>emptyList() : diagnosisIds;
        }
    }
}
