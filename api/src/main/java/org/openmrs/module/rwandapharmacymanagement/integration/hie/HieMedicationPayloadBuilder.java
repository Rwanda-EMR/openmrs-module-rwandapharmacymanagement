package org.openmrs.module.rwandapharmacymanagement.integration.hie;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.OrderFrequency;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HieMedicationPayloadBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildEncounter(PatientDispensingRequest request,
                                 String patientIdentifierTypeUuid,
                                 String serviceTypeCode,
                                 String serviceTypeDisplay) throws java.io.IOException {
        Encounter encounter = resolveManagedEncounter(request == null ? null : request.getEncounter());
        if (encounter == null) {
            return "";
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resourceType", "Encounter");
        payload.put("id", blankToDefault(encounter.getUuid(), String.valueOf(encounter.getEncounterId())));
        payload.put("meta", encounterMeta(encounter));
        payload.put("status", encounterStatus(encounter));
        payload.put("statusHistory", encounterStatusHistory(encounter));
        payload.put("class", encounterClass());
        List<Map<String, Object>> encounterTypes = encounterType(encounter.getEncounterType());
        if (!encounterTypes.isEmpty()) {
            payload.put("type", encounterTypes);
        }
        payload.put("serviceType", encounterServiceType(serviceTypeCode, serviceTypeDisplay));
        payload.put("subject", patientReference(request == null ? null : request.getPatient(), patientIdentifierTypeUuid));
        List<Map<String, Object>> participants = encounterParticipants(request, encounter);
        if (!participants.isEmpty()) {
            payload.put("participant", participants);
        }
        payload.put("period", period(encounter.getEncounterDatetime(), encounter.getEncounterDatetime()));
        List<Map<String, Object>> locations = encounterLocations(request, encounter);
        if (!locations.isEmpty()) {
            payload.put("location", locations);
        }
        return objectMapper.writeValueAsString(payload);
    }

    public String buildMedicationRequest(PatientDispensingRequest request,
                                         PatientDispensingRequestLine line,
                                         String patientIdentifierTypeUuid,
                                         String medicationCodeSystem,
                                         String facilityId,
                                         String requestingFacilityDisplay,
                                         String sourceSystem,
                                         String groupIdentifierSystem) throws java.io.IOException {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resourceType", "MedicationRequest");
        payload.put("id", medicationRequestId(line));
        payload.put("identifier", identifier(line));
        payload.put("status", "active");
        payload.put("intent", "order");
        payload.put("groupIdentifier", groupIdentifier(request, line, facilityId, groupIdentifierSystem));
        Map<String, Object> insurance = insuranceReference(request);
        if (insurance != null) {
            payload.put("insurance", Collections.singletonList(insurance));
        }
        payload.put("medicationCodeableConcept", medicationConcept(line == null ? null : line.getItem() == null ? null : line.getItem().getCode(),
                line == null ? null : line.getItem() == null ? null : line.getItem().getName(), medicationCodeSystem));
        payload.put("subject", medicationRequestSubjectReference(request == null ? null : request.getPatient(), patientIdentifierTypeUuid));
        Map<String, Object> encounter = encounterReference(request == null ? null : request.getEncounter());
        if (encounter != null) {
            payload.put("encounter", encounter);
        }
        if (request != null && request.getOrderingProvider() != null) {
            payload.put("requester", practitionerReference(request.getOrderingProvider()));
        }
        payload.put("authoredOn", isoDateTime(request == null ? null : request.getDateCreated()));
        List<Map<String, Object>> dosageInstructions = dosageInstruction(line == null ? null : line.getOrder());
        if (!dosageInstructions.isEmpty()) {
            payload.put("dosageInstruction", dosageInstructions);
        }
        Map<String, Object> dispenseRequest = dispenseRequest(line);
        if (!dispenseRequest.isEmpty()) {
            payload.put("dispenseRequest", dispenseRequest);
        }
        List<Map<String, Object>> extensions = medicationRequestExtensions(request, facilityId, requestingFacilityDisplay, sourceSystem);
        if (!extensions.isEmpty()) {
            payload.put("extension", extensions);
        }
        return objectMapper.writeValueAsString(payload);
    }

    public String buildMedicationDispense(DispenseRecord dispenseRecord,
                                          DispenseRecordLine line,
                                          String patientIdentifierTypeUuid,
                                          String medicationCodeSystem) throws java.io.IOException {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resourceType", "MedicationDispense");
        payload.put("id", medicationDispenseId(line));
        payload.put("status", "completed");
        payload.put("medicationCodeableConcept", medicationConcept(line == null ? null : line.getItem() == null ? null : line.getItem().getCode(),
                line == null ? null : line.getItem() == null ? null : line.getItem().getName(), medicationCodeSystem));
        payload.put("subject", reference("Patient/" + resolvePatientReference(dispenseRecord == null ? null : dispenseRecord.getPatient(), patientIdentifierTypeUuid)));
        PatientDispensingRequest request = dispenseRecord == null ? null : dispenseRecord.getPatientDispensingRequest();
        Map<String, Object> encounter = encounterReference(request == null ? null : request.getEncounter());
        if (encounter != null) {
            payload.put("encounter", encounter);
        }
        List<Map<String, Object>> authorizingPrescription = authorizingPrescriptionReferences(request, line);
        if (!authorizingPrescription.isEmpty()) {
            payload.put("authorizingPrescription", authorizingPrescription);
        }
        if (dispenseRecord != null && dispenseRecord.getDispensedBy() != null) {
            Map<String, Object> performer = new LinkedHashMap<String, Object>();
            performer.put("actor", performerReference(dispenseRecord.getDispensedBy()));
            payload.put("performer", Collections.singletonList(performer));
        }
        if (dispenseRecord != null && dispenseRecord.getDispensingPharmacyLocation() != null
                && dispenseRecord.getDispensingPharmacyLocation().getLocation() != null) {
            Map<String, Object> extension = new LinkedHashMap<String, Object>();
            extension.put("url", "http://example.org/fhir/StructureDefinition/location");
            extension.put("valueReference", reference("Location/" + locationReference(dispenseRecord.getDispensingPharmacyLocation().getLocation())));
            payload.put("extension", Collections.singletonList(extension));
        }
        payload.put("whenHandedOver", isoDateTime(dispenseRecord == null ? null : dispenseRecord.getDateCreated()));
        DrugOrder order = firstMatchingOrder(request, line);
        List<Map<String, Object>> dosageInstructions = dosageInstruction(order);
        if (!dosageInstructions.isEmpty()) {
            payload.put("dosageInstruction", dosageInstructions);
        }
        return objectMapper.writeValueAsString(payload);
    }

    public String buildPrescriptionSmsMessage(PatientDispensingRequest request, String facilityId) {
        return buildPrescriptionSmsMessage(request, facilityId, null);
    }

    public String buildPrescriptionSmsMessage(PatientDispensingRequest request, String facilityId, String hospitalName) {
        String requestNumber = prescriptionIdentifier(request, null, facilityId);
        int itemCount = activeDrugCount(request);
        String patientLastName = resolvePatientLastName(request == null ? null : request.getPatient());
        String hospitalPrefix = isBlank(hospitalName) ? "" : hospitalName.trim() + " irakumenyesha ko ";
        return "Kuri " + patientLastName + " , " + hospitalPrefix + "imiti ( " + itemCount + " ) ifite nomero "
                + requestNumber + " wandikiwe na muganga yoherejwe muri farumasi. Murware ubukira.";
    }

    public String prescriptionIdentifier(PatientDispensingRequest request, PatientDispensingRequestLine line, String facilityId) {
        String resolvedFacilityId = isBlank(facilityId) ? "unknown" : facilityId;
        Integer orderId = resolveOrderId(request, line);
        return "Pres-ord-" + resolvedFacilityId + "-" + (orderId == null ? "unknown" : orderId);
    }

    public String resolvePatientPhoneNumber(Patient patient, String personAttributeTypeUuid) {
        Patient managedPatient = resolveManagedPatient(patient);
        if (managedPatient == null || isBlank(personAttributeTypeUuid)) {
            return null;
        }
        org.openmrs.PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeTypeByUuid(personAttributeTypeUuid);
        if (attributeType == null) {
            return null;
        }
        PersonAttribute attribute = managedPatient.getAttribute(attributeType);
        return attribute == null || attribute.getValue() == null ? null : attribute.getValue().toString();
    }

    public String medicationRequestId(PatientDispensingRequestLine line) {
        return line == null ? "unknown" : blankToDefault(line.getUuid(), "unknown");
    }

    private String medicationDispenseId(DispenseRecordLine line) {
        return "rpm-meddisp-" + (line == null || line.getDispenseRecordLineId() == null ? "unknown" : line.getDispenseRecordLineId());
    }

    private Map<String, Object> medicationConcept(String code, String display, String system) {
        Map<String, Object> concept = new LinkedHashMap<String, Object>();
        Map<String, Object> codingEntry = new LinkedHashMap<String, Object>();
        codingEntry.put("system", blankToDefault(system, "http://openmrs.org/rwanda-pharmacy-management/item-code"));
        codingEntry.put("code", blankToDefault(code, safeIdentifier(display)));
        codingEntry.put("display", blankToDefault(display, code));
        concept.put("coding", Collections.singletonList(codingEntry));
        concept.put("text", blankToDefault(display, code));
        return concept;
    }

    private Map<String, Object> groupIdentifier(PatientDispensingRequest request,
                                                PatientDispensingRequestLine line,
                                                String facilityId,
                                                String groupIdentifierSystem) {
        Map<String, Object> groupIdentifier = new LinkedHashMap<String, Object>();
        groupIdentifier.put("system", blankToDefault(groupIdentifierSystem,
                "http://openmrs.org/rwanda-pharmacy-management/prescription-order-number"));
        groupIdentifier.put("value", prescriptionIdentifier(request, line, facilityId));
        return groupIdentifier;
    }

    private List<Map<String, Object>> identifier(PatientDispensingRequestLine line) {
        Map<String, Object> identifier = new LinkedHashMap<String, Object>();
        identifier.put("value", blankToDefault(npcCode(line), "unknown"));
        return Collections.singletonList(identifier);
    }

    private String npcCode(PatientDispensingRequestLine line) {
        Item item = line == null ? null : line.getItem();
        return item == null ? null : item.getCode();
    }

    private Map<String, Object> insuranceReference(PatientDispensingRequest request) {
        Object insurancePolicy = resolveInsurancePolicy(request);
        if (insurancePolicy == null) {
            return null;
        }
        Object insurance = invokeNoArg(insurancePolicy, "getInsurance");
        String display = extractString(insurance, "getName");
        String policyNumber = extractString(insurancePolicy, "getInsuranceCardNo");
        if (isBlank(policyNumber) && insurance == null) {
            return null;
        }
        Map<String, Object> insuranceReference = new LinkedHashMap<String, Object>();
        insuranceReference.put("reference", "Coverage/" + blankToDefault(policyNumber, "unknown"));
        if (!isBlank(display)) {
            insuranceReference.put("display", display);
        }
        return insuranceReference;
    }

    private String insuranceName(PatientDispensingRequest request) {
        Object insurancePolicy = resolveInsurancePolicy(request);
        Object insurance = insurancePolicy == null ? null : invokeNoArg(insurancePolicy, "getInsurance");
        return extractString(insurance, "getName");
    }

    private Map<String, Object> dispenseRequest(PatientDispensingRequestLine line) {
        Map<String, Object> dispenseRequest = new LinkedHashMap<String, Object>();
        if (line == null || line.getRequestedQuantity() == null) {
            return dispenseRequest;
        }
        Map<String, Object> quantity = new LinkedHashMap<String, Object>();
        quantity.put("value", line.getRequestedQuantity());
        quantity.put("unit", "units");
        dispenseRequest.put("quantity", quantity);
        return dispenseRequest;
    }

    private List<Map<String, Object>> medicationRequestExtensions(PatientDispensingRequest request,
                                                                 String facilityId,
                                                                 String requestingFacilityDisplay,
                                                                 String sourceSystem) {
        List<Map<String, Object>> extensions = new ArrayList<Map<String, Object>>();
        addStringExtension(extensions, "http://example.org/fhir/StructureDefinition/source-system",
                blankToDefault(sourceSystem, "eBuzima"));
        addRequestingFacilityExtension(extensions, blankToDefault(requestingFacilityDisplay, facilityId));
        addStringExtension(extensions, "http://example.org/fhir/StructureDefinition/insurance-name", insuranceName(request));
        return extensions;
    }

    private void addRequestingFacilityExtension(List<Map<String, Object>> extensions, String display) {
        if (isBlank(display)) {
            return;
        }
        Map<String, Object> valueReference = new LinkedHashMap<String, Object>();
        valueReference.put("display", display);
        Map<String, Object> extension = new LinkedHashMap<String, Object>();
        extension.put("url", "http://hie.moh.gov.rw/fhir/StructureDefinition/requesting-facility");
        extension.put("valueReference", valueReference);
        extensions.add(extension);
    }

    private void addStringExtension(List<Map<String, Object>> extensions, String url, String value) {
        if (isBlank(value)) {
            return;
        }
        Map<String, Object> extension = new LinkedHashMap<String, Object>();
        extension.put("url", url);
        extension.put("valueString", value);
        extensions.add(extension);
    }

    private Object resolveInsurancePolicy(PatientDispensingRequest request) {
        Object policy = resolveInsurancePolicyFromPatientBill(request);
        if (policy != null) {
            return policy;
        }
        policy = resolveInsurancePolicyFromLatestRegistration(request == null ? null : request.getPatient());
        if (policy != null) {
            return policy;
        }
        return resolveInsurancePolicyFromPatient(request == null ? null : request.getPatient());
    }

    private Object resolveInsurancePolicyFromPatientBill(PatientDispensingRequest request) {
        if (request == null || request.getPatientBillId() == null) {
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
                    .invoke(billingService, request.getPatientBillId());
            if (patientBill == null) {
                return null;
            }
            Object consommation = billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            Object beneficiary = consommation == null ? null : invokeNoArg(consommation, "getBeneficiary");
            Object insurancePolicy = beneficiary == null ? null : invokeNoArg(beneficiary, "getInsurancePolicy");
            return insurancePolicy;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Object resolveInsurancePolicyFromLatestRegistration(Patient patient) {
        if (patient == null) {
            return null;
        }
        String conceptRef = Context.getAdministrationService().getGlobalProperty("registration.insuranceNumberConcept");
        if (isBlank(conceptRef)) {
            return null;
        }
        try {
            Concept insuranceNumberConcept = resolveConcept(conceptRef);
            if (insuranceNumberConcept == null) {
                return null;
            }
            List<Obs> observations = Context.getObsService().getObservationsByPersonAndConcept(patient, insuranceNumberConcept);
            Obs latestInsuranceObs = latestNonVoidedObservation(observations);
            String insuranceCardNumber = latestInsuranceObs == null ? null : latestInsuranceObs.getValueText();
            if (isBlank(insuranceCardNumber)) {
                return null;
            }
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return null;
            }
            return billingServiceClass.getMethod("getInsurancePolicyByCardNo", String.class)
                    .invoke(billingService, insuranceCardNumber);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Concept resolveConcept(String conceptRef) {
        if (isBlank(conceptRef)) {
            return null;
        }
        String ref = conceptRef.trim();
        Concept concept = Context.getConceptService().getConceptByUuid(ref);
        if (concept != null) {
            return concept;
        }
        try {
            concept = Context.getConceptService().getConcept(Integer.valueOf(ref));
            if (concept != null) {
                return concept;
            }
        }
        catch (NumberFormatException ignored) {
        }
        return Context.getConceptService().getConceptByName(ref);
    }

    private Obs latestNonVoidedObservation(List<Obs> observations) {
        if (observations == null || observations.isEmpty()) {
            return null;
        }
        Obs latest = null;
        for (Obs observation : observations) {
            if (observation == null || Boolean.TRUE.equals(observation.getVoided())) {
                continue;
            }
            if (latest == null || compareObservationDate(observation, latest) > 0) {
                latest = observation;
            }
        }
        return latest;
    }

    private int compareObservationDate(Obs left, Obs right) {
        Date leftDate = left.getObsDatetime() == null ? left.getDateCreated() : left.getObsDatetime();
        Date rightDate = right.getObsDatetime() == null ? right.getDateCreated() : right.getObsDatetime();
        if (leftDate == null && rightDate == null) {
            return 0;
        }
        if (leftDate == null) {
            return -1;
        }
        if (rightDate == null) {
            return 1;
        }
        return leftDate.compareTo(rightDate);
    }

    private Object resolveInsurancePolicyFromPatient(Patient patient) {
        if (patient == null) {
            return null;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return null;
            }
            Object policies = billingServiceClass.getMethod("getAllInsurancePoliciesByPatient", Patient.class)
                    .invoke(billingService, patient);
            if (policies instanceof Iterable) {
                for (Object policy : (Iterable<?>) policies) {
                    if (policy != null) {
                        return policy;
                    }
                }
            }
            if (policies instanceof Collection && !((Collection<?>) policies).isEmpty()) {
                return ((Collection<?>) policies).iterator().next();
            }
            return null;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> encounterReference(Encounter encounter) {
        Encounter managedEncounter = resolveManagedEncounter(encounter);
        if (managedEncounter == null) {
            return null;
        }
        Map<String, Object> reference = new LinkedHashMap<String, Object>();
        reference.put("reference", "Encounter/" + blankToDefault(managedEncounter.getUuid(), String.valueOf(managedEncounter.getEncounterId())));
        reference.put("type", "Encounter");
        Map<String, Object> identifier = new LinkedHashMap<String, Object>();
        identifier.put("value", blankToDefault(managedEncounter.getUuid(), String.valueOf(managedEncounter.getEncounterId())));
        reference.put("identifier", identifier);
        reference.put("display", managedEncounter.getEncounterType() == null ? "Encounter" : managedEncounter.getEncounterType().getName());
        return reference;
    }

    private Map<String, Object> patientReference(Patient patient, String patientIdentifierTypeUuid) {
        Patient managedPatient = resolveManagedPatient(patient);
        String referenceValue = resolvePatientReference(managedPatient, patientIdentifierTypeUuid);
        Map<String, Object> subject = new LinkedHashMap<String, Object>();
        subject.put("reference", "Patient/" + referenceValue);
        subject.put("type", "Patient");
        Map<String, Object> identifier = new LinkedHashMap<String, Object>();
        identifier.put("value", referenceValue);
        subject.put("identifier", identifier);
        if (managedPatient != null) {
            subject.put("display", patientDisplay(managedPatient));
        }
        return subject;
    }

    private Map<String, Object> medicationRequestSubjectReference(Patient patient, String patientIdentifierTypeUuid) {
        Patient managedPatient = resolveManagedPatient(patient);
        Map<String, Object> subject = reference("Patient/" + resolvePatientReference(managedPatient, patientIdentifierTypeUuid));
        if (managedPatient != null) {
            subject.put("display", patientDisplay(managedPatient));
        }
        return subject;
    }

    private String encounterStatus(Encounter encounter) {
        return encounter != null && encounter.getVoided() != null && encounter.getVoided() ? "entered-in-error" : "unknown";
    }

    private Map<String, Object> encounterClass() {
        Map<String, Object> encounterClass = new LinkedHashMap<String, Object>();
        encounterClass.put("system", "http://terminology.hl7.org/CodeSystem/v3-ActCode");
        encounterClass.put("code", "UNKNOWN");
        return encounterClass;
    }

    private Map<String, Object> encounterMeta(Encounter encounter) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        if (encounter != null && encounter.getDateChanged() != null) {
            meta.put("versionId", String.valueOf(encounter.getDateChanged().getTime()));
        } else if (encounter != null && encounter.getDateCreated() != null) {
            meta.put("versionId", String.valueOf(encounter.getDateCreated().getTime()));
        }
        Map<String, Object> tag = new LinkedHashMap<String, Object>();
        tag.put("system", "http://fhir.openmrs.org/ext/encounter-tag");
        tag.put("code", "encounter");
        tag.put("display", "Encounter");
        meta.put("tag", Collections.singletonList(tag));
        return meta;
    }

    private List<Map<String, Object>> encounterStatusHistory(Encounter encounter) {
        Map<String, Object> statusHistory = new LinkedHashMap<String, Object>();
        statusHistory.put("status", encounterStatus(encounter));
        statusHistory.put("period", period(encounter == null ? null : encounter.getEncounterDatetime(),
                encounter == null ? null : encounter.getEncounterDatetime()));
        return Collections.singletonList(statusHistory);
    }

    private List<Map<String, Object>> encounterType(EncounterType encounterType) {
        if (encounterType == null) {
            return Collections.emptyList();
        }
        Map<String, Object> coding = new LinkedHashMap<String, Object>();
        coding.put("system", "http://fhir.openmrs.org/code-system/encounter-type");
        coding.put("code", String.valueOf(encounterType.getEncounterTypeId()));
        coding.put("display", encounterType.getName());
        Map<String, Object> type = new LinkedHashMap<String, Object>();
        type.put("coding", Collections.singletonList(coding));
        return Collections.singletonList(type);
    }

    private Map<String, Object> encounterServiceType(String serviceTypeCode, String serviceTypeDisplay) {
        Map<String, Object> coding = new LinkedHashMap<String, Object>();
        coding.put("system", "http://terminology.hl7.org/CodeSystem/service-type");
        coding.put("code", blankToDefault(serviceTypeCode, "57"));
        coding.put("display", blankToDefault(serviceTypeDisplay, "Pharmacy"));
        Map<String, Object> serviceType = new LinkedHashMap<String, Object>();
        serviceType.put("coding", Collections.singletonList(coding));
        return serviceType;
    }

    private List<Map<String, Object>> encounterParticipants(PatientDispensingRequest request, Encounter encounter) {
        if (request == null || request.getOrderingProvider() == null) {
            return Collections.emptyList();
        }
        Map<String, Object> participantTypeCoding = new LinkedHashMap<String, Object>();
        participantTypeCoding.put("system", "http://snomed.info/sct");
        participantTypeCoding.put("code", "PART");
        participantTypeCoding.put("display", "Participation");

        Map<String, Object> participantType = new LinkedHashMap<String, Object>();
        participantType.put("coding", Collections.singletonList(participantTypeCoding));

        Map<String, Object> participant = new LinkedHashMap<String, Object>();
        participant.put("type", participantType);

        Map<String, Object> individual = new LinkedHashMap<String, Object>();
        individual.put("reference", "Practitioner/" + providerReference(request.getOrderingProvider()));
        individual.put("type", "Practitioner");
        Map<String, Object> identifier = new LinkedHashMap<String, Object>();
        identifier.put("value", providerReference(request.getOrderingProvider()));
        individual.put("identifier", identifier);
        individual.put("display", request.getOrderingProvider().getName());
        participant.put("individual", individual);
        participant.put("period", period(encounter == null ? null : encounter.getEncounterDatetime(),
                encounter == null ? null : encounter.getEncounterDatetime()));
        return Collections.singletonList(participant);
    }

    private List<Map<String, Object>> encounterLocations(PatientDispensingRequest request, Encounter encounter) {
        Location location = null;
        if (encounter != null && encounter.getLocation() != null) {
            location = encounter.getLocation();
        } else if (request != null) {
            location = request.getRequestingLocation();
        }
        if (location == null) {
            return Collections.emptyList();
        }
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        Map<String, Object> locationReference = new LinkedHashMap<String, Object>();
        locationReference.put("reference", "Location/" + locationReference(location));
        locationReference.put("type", "Location");
        Map<String, Object> identifier = new LinkedHashMap<String, Object>();
        identifier.put("value", location.getName());
        locationReference.put("identifier", identifier);
        locationReference.put("display", location.getName());
        entry.put("location", locationReference);
        entry.put("status", "planned");
        entry.put("physicalType", locationPhysicalType());
        entry.put("period", period(encounter == null ? null : encounter.getEncounterDatetime(),
                encounter == null ? null : encounter.getEncounterDatetime()));
        return Collections.singletonList(entry);
    }

    private Map<String, Object> locationPhysicalType() {
        Map<String, Object> coding = new LinkedHashMap<String, Object>();
        coding.put("system", "http://terminology.hl7.org/CodeSystem/location-physical-type");
        coding.put("code", "bd");
        coding.put("display", "building");
        Map<String, Object> physicalType = new LinkedHashMap<String, Object>();
        physicalType.put("coding", Collections.singletonList(coding));
        return physicalType;
    }

    private Map<String, Object> period(Date start, Date end) {
        Map<String, Object> period = new LinkedHashMap<String, Object>();
        period.put("start", isoDateTime(start));
        period.put("end", isoDateTime(end == null ? start : end));
        return period;
    }

    private List<Map<String, Object>> dosageInstruction(DrugOrder order) {
        DrugOrder managedOrder = resolveManagedDrugOrder(order);
        if (managedOrder == null) {
            return Collections.emptyList();
        }
        Map<String, Object> instruction = new LinkedHashMap<String, Object>();
        instruction.put("text", blankToDefault(managedOrder.getDosingInstructions(), managedOrder.getInstructions()));
        Map<String, Object> timing = timing(managedOrder.getFrequency());
        if (!timing.isEmpty()) {
            instruction.put("timing", timing);
        }
        String routeDisplay = managedOrder.getRoute() == null ? null : managedOrder.getRoute().getDisplayString();
        if (!isBlank(routeDisplay)) {
            Map<String, Object> coding = new LinkedHashMap<String, Object>();
            coding.put("system", "http://terminology.hl7.org/CodeSystem/route-codes");
            coding.put("code", safeIdentifier(routeDisplay).toLowerCase(Locale.ENGLISH));
            coding.put("display", routeDisplay);
            Map<String, Object> route = new LinkedHashMap<String, Object>();
            route.put("coding", Collections.singletonList(coding));
            instruction.put("route", route);
        }
        if (managedOrder.getDose() != null) {
            Map<String, Object> doseQuantity = new LinkedHashMap<String, Object>();
            doseQuantity.put("value", managedOrder.getDose());
            if (managedOrder.getDoseUnits() != null) {
                String units = managedOrder.getDoseUnits().getDisplayString();
                doseQuantity.put("unit", units);
                doseQuantity.put("system", "http://unitsofmeasure.org");
                doseQuantity.put("code", units);
            }
            Map<String, Object> doseAndRate = new LinkedHashMap<String, Object>();
            doseAndRate.put("doseQuantity", doseQuantity);
            instruction.put("doseAndRate", Collections.singletonList(doseAndRate));
        }
        return Collections.singletonList(instruction);
    }

    private Map<String, Object> timing(OrderFrequency frequency) {
        if (frequency == null || isBlank(frequency.getName())) {
            return Collections.emptyMap();
        }
        Integer parsedFrequency = parseFrequencyValue(frequency.getName());
        if (parsedFrequency == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> repeat = new LinkedHashMap<String, Object>();
        repeat.put("frequency", parsedFrequency);
        repeat.put("period", 1);
        repeat.put("periodUnit", "d");
        Map<String, Object> timing = new LinkedHashMap<String, Object>();
        timing.put("repeat", repeat);
        return timing;
    }

    private Integer parseFrequencyValue(String frequencyName) {
        String normalized = frequencyName == null ? "" : frequencyName.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.contains("once") || normalized.contains("daily") || normalized.contains("od")) {
            return 1;
        }
        if (normalized.contains("twice") || normalized.contains("bid")) {
            return 2;
        }
        if (normalized.contains("three") || normalized.contains("tid")) {
            return 3;
        }
        if (normalized.contains("four") || normalized.contains("qid")) {
            return 4;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(normalized);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private List<Map<String, Object>> authorizingPrescriptionReferences(PatientDispensingRequest request, DispenseRecordLine dispenseLine) {
        List<Map<String, Object>> references = new ArrayList<Map<String, Object>>();
        if (request == null || request.getLines() == null || dispenseLine == null || dispenseLine.getItem() == null) {
            return references;
        }
        Integer itemId = dispenseLine.getItem().getItemId();
        for (PatientDispensingRequestLine requestLine : request.getLines()) {
            if (requestLine != null && requestLine.getItem() != null && requestLine.getPatientDispensingRequestLineId() != null
                    && itemId != null && itemId.equals(requestLine.getItem().getItemId())) {
                references.add(reference("MedicationRequest/" + medicationRequestId(requestLine)));
            }
        }
        return references;
    }

    private DrugOrder firstMatchingOrder(PatientDispensingRequest request, DispenseRecordLine dispenseLine) {
        if (request == null || request.getLines() == null || dispenseLine == null || dispenseLine.getItem() == null) {
            return null;
        }
        Integer itemId = dispenseLine.getItem().getItemId();
        for (PatientDispensingRequestLine requestLine : request.getLines()) {
            if (requestLine != null && requestLine.getItem() != null && requestLine.getOrder() != null
                    && itemId != null && itemId.equals(requestLine.getItem().getItemId())) {
                return resolveManagedDrugOrder(requestLine.getOrder());
            }
        }
        return null;
    }

    private DrugOrder resolveManagedDrugOrder(DrugOrder order) {
        if (order == null) {
            return null;
        }
        try {
            if (order.getOrderId() != null) {
                org.openmrs.Order managedOrder = Context.getOrderService().getOrder(order.getOrderId());
                if (managedOrder instanceof DrugOrder) {
                    return (DrugOrder) managedOrder;
                }
            }
            if (!isBlank(order.getUuid())) {
                org.openmrs.Order managedOrder = Context.getOrderService().getOrderByUuid(order.getUuid());
                if (managedOrder instanceof DrugOrder) {
                    return (DrugOrder) managedOrder;
                }
            }
        }
        catch (Exception ignored) {
        }
        return order;
    }

    private String resolvePatientReference(Patient patient, String patientIdentifierTypeUuid) {
        Patient managedPatient = resolveManagedPatient(patient);
        if (managedPatient == null) {
            return "unknown";
        }
        if (!isBlank(patientIdentifierTypeUuid)) {
            PatientIdentifierType identifierType = Context.getPatientService().getPatientIdentifierTypeByUuid(patientIdentifierTypeUuid);
            if (identifierType != null) {
                PatientIdentifier identifier = managedPatient.getPatientIdentifier(identifierType);
                if (identifier != null && !isBlank(identifier.getIdentifier())) {
                    return identifier.getIdentifier();
                }
            }
        }
        PatientIdentifier identifier = managedPatient.getPatientIdentifier();
        if (identifier != null && !isBlank(identifier.getIdentifier())) {
            return identifier.getIdentifier();
        }
        return blankToDefault(managedPatient.getUuid(), String.valueOf(managedPatient.getPatientId()));
    }

    private Patient resolveManagedPatient(Patient patient) {
        if (patient == null) {
            return null;
        }
        try {
            if (patient.getPatientId() != null) {
                Patient managedPatient = Context.getPatientService().getPatient(patient.getPatientId());
                if (managedPatient != null) {
                    return managedPatient;
                }
            }
            if (!isBlank(patient.getUuid())) {
                Patient managedPatient = Context.getPatientService().getPatientByUuid(patient.getUuid());
                if (managedPatient != null) {
                    return managedPatient;
                }
            }
        }
        catch (Exception ignored) {
        }
        return patient;
    }

    private Encounter resolveManagedEncounter(Encounter encounter) {
        if (encounter == null) {
            return null;
        }
        try {
            if (encounter.getEncounterId() != null) {
                Encounter managedEncounter = Context.getEncounterService().getEncounter(encounter.getEncounterId());
                if (managedEncounter != null) {
                    return managedEncounter;
                }
            }
            if (!isBlank(encounter.getUuid())) {
                Encounter managedEncounter = Context.getEncounterService().getEncounterByUuid(encounter.getUuid());
                if (managedEncounter != null) {
                    return managedEncounter;
                }
            }
        }
        catch (Exception ignored) {
        }
        return encounter;
    }

    private String patientDisplay(Patient patient) {
        if (patient == null) {
            return "";
        }
        return patient.getPersonName() == null ? patient.getFamilyName() : patient.getPersonName().getFullName();
    }

    private Map<String, Object> reference(String value) {
        Map<String, Object> reference = new LinkedHashMap<String, Object>();
        reference.put("reference", value);
        return reference;
    }

    private Map<String, Object> performerReference(Provider provider) {
        Map<String, Object> actor = new LinkedHashMap<String, Object>();
        actor.put("reference", "Practitioner/" + providerReference(provider));
        actor.put("display", provider == null ? "" : provider.getName());
        return actor;
    }

    private Map<String, Object> practitionerReference(Provider provider) {
        Map<String, Object> requester = new LinkedHashMap<String, Object>();
        requester.put("reference", "Practitioner/" + providerReference(provider));
        requester.put("display", provider == null ? "" : provider.getName());
        return requester;
    }

    private String providerReference(Provider provider) {
        return provider == null ? "unknown" : blankToDefault(provider.getUuid(), String.valueOf(provider.getProviderId()));
    }

    private String locationReference(Location location) {
        return location == null ? "unknown" : blankToDefault(location.getUuid(), String.valueOf(location.getLocationId()));
    }

    private Integer resolveOrderId(PatientDispensingRequest request, PatientDispensingRequestLine line) {
        DrugOrder directOrder = line == null ? null : line.getOrder();
        if (directOrder != null && directOrder.getOrderId() != null) {
            return directOrder.getOrderId();
        }
        if (request != null && request.getOrder() instanceof DrugOrder && request.getOrder().getOrderId() != null) {
            return request.getOrder().getOrderId();
        }
        if (request != null && request.getLines() != null) {
            for (PatientDispensingRequestLine requestLine : request.getLines()) {
                if (requestLine != null && requestLine.getOrder() != null && requestLine.getOrder().getOrderId() != null) {
                    return requestLine.getOrder().getOrderId();
                }
            }
        }
        return null;
    }

    private int activeDrugCount(PatientDispensingRequest request) {
        if (request == null || request.getLines() == null) {
            return 0;
        }
        int count = 0;
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided())) {
                count++;
            }
        }
        return count;
    }

    private String resolvePatientLastName(Patient patient) {
        Patient managedPatient = resolveManagedPatient(patient);
        if (managedPatient == null) {
            return "unknown";
        }
        if (managedPatient.getPersonName() != null && !isBlank(managedPatient.getPersonName().getFamilyName())) {
            return managedPatient.getPersonName().getFamilyName();
        }
        return isBlank(managedPatient.getFamilyName()) ? "unknown" : managedPatient.getFamilyName();
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

    private String extractString(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value == null ? null : value.toString();
    }

    private String isoDateTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        format.setTimeZone(TimeZone.getDefault());
        return format.format(date == null ? new Date() : date);
    }

    private String safeIdentifier(String value) {
        return isBlank(value) ? "unknown" : value.replaceAll("[^A-Za-z0-9\\-_.]", "-");
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
