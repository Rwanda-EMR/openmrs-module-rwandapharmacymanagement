package org.openmrs.module.rwandapharmacymanagement.page.controller.prescription;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PrescriptionService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescriptionLineCommand;
import org.openmrs.module.rwandapharmacymanagement.integration.hie.HieMedicationPayloadBuilder;
import org.openmrs.module.rwandapharmacymanagement.integration.sms.SmsPhoneNumberUtil;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.util.ProductMasterPrivilegeSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EnterPageController {

    public void get(PageModel model,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @SpringBean("hieMedicationPayloadBuilder") HieMedicationPayloadBuilder hieMedicationPayloadBuilder,
                    @RequestParam(value = "patientId", required = false) String patientIdOrUuid,
                    @RequestParam(value = "patientBillId", required = false) Integer patientBillId,
                    @RequestParam(value = "billId", required = false) Integer billId,
                    @RequestParam(value = "patient_bill_id", required = false) Integer patientBillIdSnakeCase,
                    @RequestParam(value = "requestId", required = false) Integer requestId,
                    @RequestParam(value = "historyPage", required = false) Integer historyPage) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION);
        Integer resolvedPatientBillId = firstNonNull(patientBillId, billId, patientBillIdSnakeCase);
        Patient patient = resolvePatient(patientIdOrUuid);
        Visit activeVisit = resolveActiveVisit(patient);
        int pageSize = 8;
        int currentHistoryPage = historyPage == null || historyPage.intValue() < 1 ? 1 : historyPage.intValue();
        List<PharmacyLocation> candidatePharmacies = org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport.getCandidatePharmacies(pharmacyService);
        List<ProductMasterItem> prescribableItems = getPrescribableItems(pharmacyService);
        Map<Integer, BigDecimal> stockQuantityByProductMasterItemId = stockQuantityByProductMasterItemId(pharmacyService,
                inventoryService, prescribableItems);
        List<Integer> stockedProductMasterItemIds = stockedProductMasterItemIds(stockQuantityByProductMasterItemId);
        List<PatientDispensingRequest> historyRequests = patient == null
                ? Collections.<PatientDispensingRequest>emptyList()
                : dispensingService.getPatientPrescriptionHistory(patient.getPatientId(), (currentHistoryPage - 1) * pageSize, pageSize);
        long historyTotalCount = patient == null ? 0L : dispensingService.countPatientPrescriptionHistory(patient.getPatientId());
        model.addAttribute("patient", patient);
        model.addAttribute("activeVisit", activeVisit);
        model.addAttribute("currentLocation", Context.getUserContext() == null ? null : Context.getUserContext().getLocation());
        model.addAttribute("candidatePharmacies", candidatePharmacies);
        model.addAttribute("prescribableItems", prescribableItems);
        model.addAttribute("stockedProductMasterItemIds", stockedProductMasterItemIds);
        model.addAttribute("stockQuantityByProductMasterItemId", stockQuantityByProductMasterItemId);
        model.addAttribute("billingMappedByProductMasterItemId", Collections.<Integer, Boolean>emptyMap());
        model.addAttribute("patientReferenceForSubmit",
                patient != null && patient.getPatientId() != null ? String.valueOf(patient.getPatientId()) : null);
        model.addAttribute("patientUuidForSubmit", patient == null ? null : patient.getUuid());
        model.addAttribute("routes", sortConcepts(Context.getOrderService().getDrugRoutes()));
        List<Concept> frequencyConcepts = getConfiguredFrequencyConcepts();
        model.addAttribute("frequencies", frequencyConcepts);
        model.addAttribute("frequencyOptions", buildFrequencyOptions(frequencyConcepts));
        model.addAttribute("durationUnits", sortConcepts(Context.getOrderService().getDurationUnits()));
        model.addAttribute("pricingService", pricingService);
        model.addAttribute("pageTitle", "Prescription");
        model.addAttribute("patientBillId", resolvedPatientBillId);
        String patientPhoneNumber = resolvePatientPhoneNumber(patient, hieMedicationPayloadBuilder);
        model.addAttribute("patientPhoneNumber", patientPhoneNumber);
        model.addAttribute("patientSmsPhoneValid", SmsPhoneNumberUtil.isValidForSending(patientPhoneNumber));
        model.addAttribute("prescriptionSaved", Boolean.FALSE);
        model.addAttribute("requestId", requestId);
        model.addAttribute("historyEntries", buildHistoryEntries(historyRequests, dispensingService));
        model.addAttribute("historyPage", currentHistoryPage);
        model.addAttribute("historyPageSize", pageSize);
        model.addAttribute("historyTotalCount", historyTotalCount);
        model.addAttribute("historyTotalPages", historyTotalCount == 0L ? 1L : (long) Math.ceil((double) historyTotalCount / (double) pageSize));
    }

    public String post(@SpringBean("prescriptionService") PrescriptionService prescriptionService,
                       @SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @SpringBean("hieMedicationPayloadBuilder") HieMedicationPayloadBuilder hieMedicationPayloadBuilder,
                       @RequestParam(value = "patientId", required = false) String patientIdOrUuid,
                       @RequestParam(value = "patientUuid", required = false) String patientUuid,
                       @RequestParam(value = "patientBillId", required = false) Integer patientBillId,
                       @RequestParam(value = "billId", required = false) Integer billId,
                       @RequestParam(value = "patient_bill_id", required = false) Integer patientBillIdSnakeCase,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "requestId", required = false) Integer requestId,
                       @RequestParam(value = "requestLineId", required = false) Integer requestLineId,
                       @RequestParam(value = "historyPage", required = false) Integer historyPage,
                       @RequestParam(value = "voidReason", required = false) String voidReason,
                       @RequestParam(value = "productMasterItemId", required = false) Integer[] productMasterItemIds,
                       @RequestParam(value = "dose", required = false) BigDecimal[] doses,
                       @RequestParam(value = "route", required = false) String[] routes,
                       @RequestParam(value = "frequency", required = false) String[] frequencies,
                       @RequestParam(value = "duration", required = false) Integer[] durations,
                       @RequestParam(value = "durationUnit", required = false) String[] durationUnits,
                       @RequestParam(value = "startDate", required = false) String[] startDates,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "instructions", required = false) String[] instructions,
                       @RequestParam(value = "billable", required = false) Boolean[] billables,
                       @RequestParam(value = "billingExclusionReason", required = false) String[] billingExclusionReasons,
                       UiUtils ui) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION);
        Integer resolvedPatientBillId = firstNonNull(patientBillId, billId, patientBillIdSnakeCase);
        Patient patient = resolvePatient(patientIdOrUuid);
        if (patient == null) {
            patient = resolvePatient(patientUuid);
        }
        Visit activeVisit = resolveActiveVisit(patient);
        if (patient == null) {
            throw new IllegalArgumentException("Patient not found");
        }
        if (activeVisit == null) {
            throw new IllegalArgumentException("An active visit is required before prescribing");
        }
        List<PharmacyLocation> candidatePharmacies = org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport.getCandidatePharmacies(pharmacyService);
        if (candidatePharmacies == null || candidatePharmacies.isEmpty()) {
            throw new IllegalArgumentException("No dispensing pharmacy is configured for the current location");
        }
        Location encounterLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        Provider orderingProvider = resolveOrderingProvider();
        String normalizedAction = action == null ? "" : action.trim();
        if ("updateLine".equalsIgnoreCase(normalizedAction)) {
            List<PrescriptionLineCommand> lines = buildCommands(productMasterItemIds, doses, routes, frequencies, durations,
                    durationUnits, startDates, quantities, instructions, billables, billingExclusionReasons);
            prescriptionService.updatePrescriptionLine(requestId, requestLineId, orderingProvider, lines);
            return redirectToPrescriptionPage(ui, patient, historyPage);
        }
        if ("voidLine".equalsIgnoreCase(normalizedAction)) {
            prescriptionService.voidPrescriptionLine(requestId, requestLineId, voidReason);
            return redirectToPrescriptionPage(ui, patient, historyPage);
        }
        validatePatientPhoneForBilling(patient, hieMedicationPayloadBuilder);
        List<PrescriptionLineCommand> lines = buildCommands(productMasterItemIds, doses, routes, frequencies, durations,
                durationUnits, startDates, quantities, instructions, billables, billingExclusionReasons);
        PatientDispensingRequest request = prescriptionService.createPrescription(patient, activeVisit, orderingProvider,
                encounterLocation, lines, resolvedPatientBillId);
        Map<String, Object> redirectParams = new LinkedHashMap<String, Object>();
        redirectParams.put("patientId", patient.getPatientId());
        redirectParams.put("prescriptionSaved", 1);
        redirectParams.put("requestId", request.getPatientDispensingRequestId());
        return "redirect:" + ui.pageLink("coreapps", "clinicianfacing/patient", redirectParams);
    }

    private void validatePatientPhoneForBilling(Patient patient, HieMedicationPayloadBuilder hieMedicationPayloadBuilder) {
        String patientPhoneNumber = resolvePatientPhoneNumber(patient, hieMedicationPayloadBuilder);
        if (patientPhoneNumber == null || patientPhoneNumber.trim().isEmpty()) {
            return;
        }
        if (!SmsPhoneNumberUtil.isValidForSending(patientPhoneNumber)) {
            throw new IllegalArgumentException("Correct the patient phone number before prescribing. Use one Rwanda phone number in format 250XXXXXXXXX or 0XXXXXXXXX.");
        }
    }

    private Integer firstNonNull(Integer first, Integer second, Integer third) {
        if (first != null) {
            return first;
        }
        return second != null ? second : third;
    }

    private String redirectToPrescriptionPage(UiUtils ui, Patient patient, Integer historyPage) {
        Map<String, Object> redirectParams = new LinkedHashMap<String, Object>();
        redirectParams.put("patientId", patient.getUuid());
        if (historyPage != null && historyPage.intValue() > 1) {
            redirectParams.put("historyPage", historyPage);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "prescription/enter", redirectParams);
    }

    private Patient resolvePatient(String patientIdOrUuid) {
        if (patientIdOrUuid == null || patientIdOrUuid.trim().isEmpty()) {
            return null;
        }
        Patient patient = Context.getPatientService().getPatientByUuid(patientIdOrUuid.trim());
        if (patient != null) {
            return patient;
        }
        try {
            return Context.getPatientService().getPatient(Integer.valueOf(patientIdOrUuid.trim()));
        }
        catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolvePatientPhoneNumber(Patient patient, HieMedicationPayloadBuilder hieMedicationPayloadBuilder) {
        if (patient == null || hieMedicationPayloadBuilder == null) {
            return "";
        }
        return hieMedicationPayloadBuilder.resolvePatientPhoneNumber(patient,
                Context.getAdministrationService().getGlobalProperty(
                        RwandaPharmacyManagementConfig.GP_SMS_PHONE_PERSON_ATTRIBUTE_TYPE_UUID, ""));
    }

    private Visit resolveActiveVisit(Patient patient) {
        if (patient == null) {
            return null;
        }
        List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);
        return activeVisits == null || activeVisits.isEmpty() ? null : activeVisits.get(0);
    }

    private Provider resolveOrderingProvider() {
        if (Context.getAuthenticatedUser() == null || Context.getAuthenticatedUser().getPerson() == null) {
            return null;
        }
        java.util.Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(Context.getAuthenticatedUser().getPerson(), false);
        if (providers != null && !providers.isEmpty()) {
            return providers.iterator().next();
        }
        return Context.getProviderService().getUnknownProvider();
    }

    private List<PrescriptionLineCommand> buildCommands(Integer[] productMasterItemIds,
                                                        BigDecimal[] doses,
                                                        String[] routes,
                                                        String[] frequencies,
                                                        Integer[] durations,
                                                        String[] durationUnits,
                                                        String[] startDates,
                                                        BigDecimal[] quantities,
                                                        String[] instructions,
                                                        Boolean[] billables,
                                                        String[] billingExclusionReasons) {
        List<PrescriptionLineCommand> commands = new ArrayList<PrescriptionLineCommand>();
        if (productMasterItemIds == null) {
            return commands;
        }
        for (int i = 0; i < productMasterItemIds.length; i++) {
            Integer productMasterItemId = productMasterItemIds[i];
            if (productMasterItemId == null) {
                continue;
            }
            PrescriptionLineCommand command = new PrescriptionLineCommand();
            command.setProductMasterItemId(productMasterItemId);
            command.setDose(valueAt(doses, i));
            command.setRoute(valueAt(routes, i));
            command.setFrequency(valueAt(frequencies, i));
            command.setDuration(valueAt(durations, i));
            command.setDurationUnit(valueAt(durationUnits, i));
            command.setStartDate(parseDate(valueAt(startDates, i)));
            command.setQuantity(valueAt(quantities, i));
            command.setInstructions(valueAt(instructions, i));
            command.setBillable(valueAt(billables, i));
            command.setBillingExclusionReason(valueAt(billingExclusionReasons, i));
            commands.add(command);
        }
        return commands;
    }

    private List<ProductMasterItem> getPrescribableItems(PharmacyService pharmacyService) {
        List<ProductMasterItem> filtered = new ArrayList<ProductMasterItem>();
        for (ProductMasterItem item : pharmacyService.getAllProductMasterItems()) {
            if (item == null || Boolean.FALSE.equals(item.getActive())) {
                continue;
            }
            if (!"ZMED".equalsIgnoreCase(item.getMaterialType())) {
                continue;
            }
            if (item.getDrug() == null) {
                continue;
            }
            if (!ProductMasterPrivilegeSupport.isAllowed(item)) {
                continue;
            }
            filtered.add(item);
        }
        Collections.sort(filtered, new Comparator<ProductMasterItem>() {
            @Override
            public int compare(ProductMasterItem left, ProductMasterItem right) {
                String leftName = left == null || left.getItemName() == null ? "" : left.getItemName();
                String rightName = right == null || right.getItemName() == null ? "" : right.getItemName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return filtered;
    }

    private Map<Integer, BigDecimal> stockQuantityByProductMasterItemId(PharmacyService pharmacyService,
                                                                        InventoryService inventoryService,
                                                                        List<ProductMasterItem> prescribableItems) {
        Map<String, ProductMasterItem> byCode = new HashMap<String, ProductMasterItem>();
        Map<Integer, ProductMasterItem> byDrugId = new HashMap<Integer, ProductMasterItem>();
        for (ProductMasterItem item : prescribableItems) {
            String codeKey = normalizeKey(item == null ? null : item.getNpcCode());
            if (!codeKey.isEmpty() && !byCode.containsKey(codeKey)) {
                byCode.put(codeKey, item);
            }
            if (item != null && item.getDrug() != null && item.getDrug().getDrugId() != null
                    && !byDrugId.containsKey(item.getDrug().getDrugId())) {
                byDrugId.put(item.getDrug().getDrugId(), item);
            }
        }

        Map<Integer, BigDecimal> quantitiesByProductMasterItemId = new HashMap<Integer, BigDecimal>();
        for (PharmacyLocation pharmacyLocation : pharmacyService.getAllPharmacyLocations()) {
            if (pharmacyLocation == null || Boolean.FALSE.equals(pharmacyLocation.getActive())
                    || pharmacyLocation.getPharmacyLocationId() == null) {
                continue;
            }
            for (InventoryBalance balance : inventoryService.getAvailableInventoryForPharmacy(pharmacyLocation.getPharmacyLocationId())) {
                if (balance == null || balance.getItem() == null || balance.getQuantityOnHand() == null
                        || !Boolean.TRUE.equals(balance.getItem().getVisibleToRequesters())) {
                    continue;
                }
                ProductMasterItem matched = matchStockedProductMasterItem(balance.getItem(), byCode, byDrugId);
                if (matched != null && matched.getProductMasterItemId() != null
                        && balance.getQuantityOnHand() != null) {
                    BigDecimal currentQuantity = quantitiesByProductMasterItemId.get(matched.getProductMasterItemId());
                    quantitiesByProductMasterItemId.put(matched.getProductMasterItemId(),
                            (currentQuantity == null ? BigDecimal.ZERO : currentQuantity).add(balance.getQuantityOnHand()));
                }
            }
        }
        return quantitiesByProductMasterItemId;
    }

    private List<Integer> stockedProductMasterItemIds(Map<Integer, BigDecimal> stockQuantityByProductMasterItemId) {
        List<Integer> stockedIds = new ArrayList<Integer>();
        if (stockQuantityByProductMasterItemId == null || stockQuantityByProductMasterItemId.isEmpty()) {
            return stockedIds;
        }
        for (Map.Entry<Integer, BigDecimal> entry : stockQuantityByProductMasterItemId.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                stockedIds.add(entry.getKey());
            }
        }
        return stockedIds;
    }

    private ProductMasterItem matchStockedProductMasterItem(Item item,
                                                            Map<String, ProductMasterItem> byCode,
                                                            Map<Integer, ProductMasterItem> byDrugId) {
        if (item == null) {
            return null;
        }
        String codeKey = normalizeKey(item.getCode());
        if (!codeKey.isEmpty() && byCode.containsKey(codeKey)) {
            return byCode.get(codeKey);
        }
        if (item.getDrug() != null && item.getDrug().getDrugId() != null && byDrugId.containsKey(item.getDrug().getDrugId())) {
            return byDrugId.get(item.getDrug().getDrugId());
        }
        return null;
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private List<Concept> sortConcepts(List<Concept> concepts) {
        List<Concept> sorted = concepts == null ? new ArrayList<Concept>() : new ArrayList<Concept>(concepts);
        Collections.sort(sorted, new Comparator<Concept>() {
            @Override
            public int compare(Concept left, Concept right) {
                String leftName = left == null || left.getDisplayString() == null ? "" : left.getDisplayString();
                String rightName = right == null || right.getDisplayString() == null ? "" : right.getDisplayString();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return sorted;
    }

    private List<Concept> getConfiguredFrequencyConcepts() {
        Map<String, Concept> frequenciesByKey = new LinkedHashMap<String, Concept>();
        String configuredConcept = Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_PRESCRIPTION_FREQUENCY_CONCEPT_SET);
        Concept frequencySet = resolveConcept(configuredConcept);
        if (frequencySet != null) {
            if (frequencySet.getSetMembers() != null) {
                for (Concept member : frequencySet.getSetMembers()) {
                    addUniqueConcept(frequenciesByKey, member);
                }
            }
            if (frequencySet.getAnswers() != null) {
                for (ConceptAnswer answer : frequencySet.getAnswers()) {
                    if (answer != null) {
                        addUniqueConcept(frequenciesByKey, answer.getAnswerConcept());
                    }
                }
            }
        }
        if (!frequenciesByKey.isEmpty()) {
            return sortConcepts(new ArrayList<Concept>(frequenciesByKey.values()));
        } else {
            List<org.openmrs.OrderFrequency> orderFrequencies = Context.getOrderService().getOrderFrequencies(false);
            if (orderFrequencies != null) {
                for (org.openmrs.OrderFrequency orderFrequency : orderFrequencies) {
                    if (orderFrequency != null && orderFrequency.getConcept() != null) {
                        addUniqueConcept(frequenciesByKey, orderFrequency.getConcept());
                    }
                }
            }
        }
        return sortConcepts(new ArrayList<Concept>(frequenciesByKey.values()));
    }

    private List<Map<String, Object>> buildFrequencyOptions(List<Concept> frequencyConcepts) {
        List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
        Map<String, org.openmrs.OrderFrequency> orderFrequencyByConceptKey = orderFrequencyByConceptKey();
        for (Concept frequencyConcept : frequencyConcepts == null ? Collections.<Concept>emptyList() : frequencyConcepts) {
            if (frequencyConcept == null) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<String, Object>();
            option.put("label", frequencyConcept.getDisplayString());
            org.openmrs.OrderFrequency orderFrequency = orderFrequencyByConceptKey.get(conceptKey(frequencyConcept));
            option.put("frequencyPerDay", orderFrequency == null ? null : orderFrequency.getFrequencyPerDay());
            options.add(option);
        }
        return options;
    }

    private Map<String, org.openmrs.OrderFrequency> orderFrequencyByConceptKey() {
        Map<String, org.openmrs.OrderFrequency> frequenciesByConceptKey = new LinkedHashMap<String, org.openmrs.OrderFrequency>();
        List<org.openmrs.OrderFrequency> orderFrequencies = Context.getOrderService().getOrderFrequencies(false);
        if (orderFrequencies == null) {
            return frequenciesByConceptKey;
        }
        for (org.openmrs.OrderFrequency orderFrequency : orderFrequencies) {
            if (orderFrequency != null && orderFrequency.getConcept() != null) {
                frequenciesByConceptKey.put(conceptKey(orderFrequency.getConcept()), orderFrequency);
            }
        }
        return frequenciesByConceptKey;
    }

    private void addUniqueConcept(Map<String, Concept> conceptsByKey, Concept concept) {
        if (conceptsByKey == null || concept == null) {
            return;
        }
        String key = conceptKey(concept);
        if (!conceptsByKey.containsKey(key)) {
            conceptsByKey.put(key, concept);
        }
    }

    private String conceptKey(Concept concept) {
        if (concept == null) {
            return "";
        }
        if (concept.getConceptId() != null) {
            return "id:" + concept.getConceptId();
        }
        if (concept.getUuid() != null && !concept.getUuid().trim().isEmpty()) {
            return "uuid:" + concept.getUuid().trim();
        }
        return "display:" + (concept.getDisplayString() == null ? "" : concept.getDisplayString().trim().toLowerCase());
    }

    private Concept resolveConcept(String conceptIdentifier) {
        if (conceptIdentifier == null || conceptIdentifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = conceptIdentifier.trim();
        Concept concept = Context.getConceptService().getConceptByUuid(trimmed);
        if (concept != null) {
            return concept;
        }
        try {
            return Context.getConceptService().getConcept(Integer.valueOf(trimmed));
        }
        catch (NumberFormatException ignored) {
            return Context.getConceptService().getConceptByName(trimmed);
        }
    }

    private <T> T valueAt(T[] values, int index) {
        return values != null && values.length > index ? values[index] : null;
    }

    private Date parseDate(String value) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(trimmed);
        }
        catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Invalid start date: " + trimmed);
        }
    }

    private List<Map<String, Object>> buildHistoryEntries(List<PatientDispensingRequest> requests, DispensingService dispensingService) {
        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
        if (requests == null || requests.isEmpty()) {
            return entries;
        }
        Map<String, BigDecimal> dispensedByRequestAndItem = buildDispensedQuantityMap(requests, dispensingService);
        Map<String, List<Map<String, Object>>> dispensedLinesByRequestAndItem = buildDispensedLineDetailsMap(requests, dispensingService);
        for (PatientDispensingRequest request : requests) {
            if (request == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("requestId", request.getPatientDispensingRequestId());
            entry.put("dateCreated", request.getDateCreated());
            entry.put("encounterId", request.getEncounter() == null ? null : request.getEncounter().getEncounterId());
            entry.put("patientBillId", request.getPatientBillId());
            entry.put("paymentConfirmed", request.getPaymentConfirmed());
            entry.put("requestStatus", request.getRequestStatus());
            entry.put("orderingProvider", request.getOrderingProvider() == null ? "" : request.getOrderingProvider().getName());
            entry.put("medicalActReference", request.getMedicalActReference());
            List<Map<String, Object>> lineEntries = new ArrayList<Map<String, Object>>();
            if (request.getLines() != null) {
                for (PatientDispensingRequestLine line : request.getLines()) {
                    if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                        continue;
                    }
                    Map<String, Object> lineEntry = new LinkedHashMap<String, Object>();
                    DrugOrderHistoryInfo orderInfo = findOrderInfo(request, line);
                    boolean requestPaid = Boolean.TRUE.equals(request.getPaymentConfirmed());
                    boolean editable = !requestPaid
                            && (line.getLineStatus() == org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus.PENDING_PAYMENT
                            || line.getLineStatus() == org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus.PENDING_DISPENSE);
                    String statusLabel = String.valueOf(line.getLineStatus());
                    lineEntry.put("requestLineId", line.getPatientDispensingRequestLineId());
                    lineEntry.put("itemName", line.getItem() == null ? "" : line.getItem().getName());
                    lineEntry.put("requestedQuantity", line.getRequestedQuantity());
                    lineEntry.put("dispensedQuantity", dispensedByRequestAndItem.get(historyKey(request, line)));
                    lineEntry.put("dispensedLines", dispensedLinesByRequestAndItem.get(historyKey(request, line)));
                    lineEntry.put("lineStatus", statusLabel);
                    lineEntry.put("billable", !Boolean.FALSE.equals(line.getBillable()));
                    lineEntry.put("billingExclusionReason", line.getBillingExclusionReason());
                    lineEntry.put("rawLineStatus", line.getLineStatus());
                    lineEntry.put("provider", entry.get("orderingProvider"));
                    lineEntry.put("editable", editable);
                    lineEntry.put("voidReason", line.getVoidReason());
                    lineEntry.put("productMasterItemId", orderInfo.productMasterItemId);
                    lineEntry.put("startDate", orderInfo.startDate);
                    lineEntry.put("dose", orderInfo.dose);
                    lineEntry.put("doseUnits", orderInfo.doseUnits);
                    lineEntry.put("route", orderInfo.route);
                    lineEntry.put("frequency", orderInfo.frequency);
                    lineEntry.put("duration", orderInfo.duration);
                    lineEntry.put("durationUnits", orderInfo.durationUnits);
                    lineEntry.put("instructions", orderInfo.instructions);
                    lineEntry.put("orderId", orderInfo.orderId);
                    lineEntries.add(lineEntry);
                }
            }
            if (lineEntries.isEmpty()) {
                continue;
            }
            entry.put("lines", lineEntries);
            entries.add(entry);
        }
        return entries;
    }

    private Map<String, List<Map<String, Object>>> buildDispensedLineDetailsMap(List<PatientDispensingRequest> requests,
                                                                                DispensingService dispensingService) {
        List<Integer> requestIds = new ArrayList<Integer>();
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                requestIds.add(request.getPatientDispensingRequestId());
            }
        }
        Map<String, List<Map<String, Object>>> details = new LinkedHashMap<String, List<Map<String, Object>>>();
        if (requestIds.isEmpty()) {
            return details;
        }
        List<DispenseRecord> dispenseRecords = dispensingService.getDispenseRecordsForRequests(requestIds);
        for (DispenseRecord record : dispenseRecords) {
            if (record == null || record.getPatientDispensingRequest() == null || record.getLines() == null) {
                continue;
            }
            Integer requestId = record.getPatientDispensingRequest().getPatientDispensingRequestId();
            for (DispenseRecordLine line : record.getLines()) {
                if (line == null || line.getItem() == null || line.getItem().getItemId() == null) {
                    continue;
                }
                String key = historyKey(requestId, line.getItem().getItemId());
                if (key == null) {
                    continue;
                }
                List<Map<String, Object>> rows = details.containsKey(key) ? details.get(key) : new ArrayList<Map<String, Object>>();
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("dateCreated", record.getDateCreated());
                row.put("pharmacy", record.getDispensingPharmacyLocation() == null ? "" : record.getDispensingPharmacyLocation().getName());
                row.put("dispensedBy", record.getDispensedBy() == null ? "" : record.getDispensedBy().getName());
                row.put("batchNumber", line.getBatchNumber());
                row.put("expiryDate", line.getExpiryDate());
                row.put("quantityDispensed", line.getQuantityDispensed());
                rows.add(row);
                details.put(key, rows);
            }
        }
        return details;
    }

    private Map<String, BigDecimal> buildDispensedQuantityMap(List<PatientDispensingRequest> requests, DispensingService dispensingService) {
        List<Integer> requestIds = new ArrayList<Integer>();
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                requestIds.add(request.getPatientDispensingRequestId());
            }
        }
        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();
        if (requestIds.isEmpty()) {
            return totals;
        }
        List<DispenseRecord> dispenseRecords = dispensingService.getDispenseRecordsForRequests(requestIds);
        for (DispenseRecord record : dispenseRecords) {
            if (record == null || record.getPatientDispensingRequest() == null || record.getLines() == null) {
                continue;
            }
            Integer requestId = record.getPatientDispensingRequest().getPatientDispensingRequestId();
            for (DispenseRecordLine line : record.getLines()) {
                if (line == null || line.getItem() == null || line.getItem().getItemId() == null || line.getQuantityDispensed() == null) {
                    continue;
                }
                String key = historyKey(requestId, line.getItem().getItemId());
                if (key == null) {
                    continue;
                }
                BigDecimal current = totals.containsKey(key) ? totals.get(key) : BigDecimal.ZERO;
                totals.put(key, current.add(line.getQuantityDispensed()));
            }
        }
        return totals;
    }

    private String historyKey(PatientDispensingRequest request, PatientDispensingRequestLine line) {
        if (request == null || request.getPatientDispensingRequestId() == null || line == null || line.getItem() == null || line.getItem().getItemId() == null) {
            return null;
        }
        return historyKey(request.getPatientDispensingRequestId(), line.getItem().getItemId());
    }

    private String historyKey(Integer requestId, Integer itemId) {
        if (requestId == null || itemId == null) {
            return null;
        }
        return requestId + ":" + itemId;
    }

    private DrugOrderHistoryInfo findOrderInfo(PatientDispensingRequest request, PatientDispensingRequestLine requestLine) {
        DrugOrderHistoryInfo info = new DrugOrderHistoryInfo();
        if (request == null || request.getEncounter() == null || request.getEncounter().getOrders() == null
                || requestLine == null || requestLine.getItem() == null) {
            return info;
        }
        org.openmrs.DrugOrder linkedOrder = resolveLinkedDrugOrder(requestLine);
        if (linkedOrder != null) {
            populateOrderInfo(info, linkedOrder);
            return info;
        }
        List<org.openmrs.DrugOrder> activeDrugOrders = getActiveDrugOrders(request);
        if (activeDrugOrders.size() == 1) {
            populateOrderInfo(info, activeDrugOrders.get(0));
            return info;
        }
        org.openmrs.Drug lineDrug = resolveLineDrug(requestLine);
        Concept lineConcept = resolveLineConcept(requestLine, lineDrug);
        for (org.openmrs.DrugOrder drugOrder : activeDrugOrders) {
            if (lineDrug != null && drugOrder.getDrug() != null
                    && lineDrug.getDrugId() != null
                    && lineDrug.getDrugId().equals(drugOrder.getDrug().getDrugId())) {
                populateOrderInfo(info, drugOrder);
                break;
            }
            if (lineConcept != null && drugOrder.getConcept() != null
                    && lineConcept.getConceptId() != null
                    && lineConcept.getConceptId().equals(drugOrder.getConcept().getConceptId())) {
                populateOrderInfo(info, drugOrder);
                break;
            }
        }
        return info;
    }

    private org.openmrs.DrugOrder resolveLinkedDrugOrder(PatientDispensingRequestLine requestLine) {
        if (requestLine == null || requestLine.getOrder() == null || requestLine.getOrder().getOrderId() == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(requestLine.getOrder().getVoided())) {
            return requestLine.getOrder();
        }
        PatientDispensingRequest request = requestLine.getPatientDispensingRequest();
        if (request != null && request.getEncounter() != null && request.getEncounter().getOrders() != null) {
            for (org.openmrs.Order order : request.getEncounter().getOrders()) {
                if (order instanceof org.openmrs.DrugOrder && order.getOrderId() != null
                        && order.getOrderId().equals(requestLine.getOrder().getOrderId())) {
                    return (org.openmrs.DrugOrder) order;
                }
            }
        }
        org.openmrs.Order order = Context.getOrderService().getOrder(requestLine.getOrder().getOrderId());
        return order instanceof org.openmrs.DrugOrder ? (org.openmrs.DrugOrder) order : null;
    }

    private List<org.openmrs.DrugOrder> getActiveDrugOrders(PatientDispensingRequest request) {
        List<org.openmrs.DrugOrder> drugOrders = new ArrayList<org.openmrs.DrugOrder>();
        if (request == null || request.getEncounter() == null) {
            return drugOrders;
        }
        Collection<org.openmrs.Order> orders = request.getEncounter().getOrders();
        if (orders == null) {
            return drugOrders;
        }
        for (org.openmrs.Order order : orders) {
            if (order instanceof org.openmrs.DrugOrder && !Boolean.TRUE.equals(order.getVoided())) {
                drugOrders.add((org.openmrs.DrugOrder) order);
            }
        }
        return drugOrders;
    }

    private void populateOrderInfo(DrugOrderHistoryInfo info, org.openmrs.DrugOrder drugOrder) {
        info.orderId = drugOrder.getOrderId();
        info.productMasterItemId = findProductMasterItemId(drugOrder.getDrug());
        info.startDate = drugOrder.getDateActivated();
        info.dose = drugOrder.getDose() == null ? "" : drugOrder.getDose().toString();
        info.doseUnits = drugOrder.getDoseUnits() == null ? "" : drugOrder.getDoseUnits().getDisplayString();
        info.route = drugOrder.getRoute() == null ? "" : drugOrder.getRoute().getDisplayString();
        info.frequency = drugOrder.getFrequency() == null ? "" : drugOrder.getFrequency().getName();
        info.duration = drugOrder.getDuration() == null ? "" : drugOrder.getDuration().toString();
        info.durationUnits = drugOrder.getDurationUnits() == null ? "" : drugOrder.getDurationUnits().getDisplayString();
        info.instructions = normalizeFreeTextInstructions(drugOrder.getDosingInstructions() == null
                ? drugOrder.getInstructions() : drugOrder.getDosingInstructions());
    }

    private String normalizeFreeTextInstructions(String instructions) {
        String value = instructions == null ? "" : instructions.trim();
        if (value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String[] parts = value.split("\\s*;\\s*");
        for (String part : parts) {
            String cleanPart = part == null ? "" : part.trim();
            if (cleanPart.isEmpty() || isGeneratedInstructionPart(cleanPart)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(cleanPart);
        }
        return builder.toString();
    }

    private boolean isGeneratedInstructionPart(String part) {
        String normalized = part == null ? "" : part.trim().toLowerCase();
        return normalized.startsWith("dose:")
                || normalized.startsWith("route:")
                || normalized.startsWith("frequency:")
                || normalized.startsWith("duration:");
    }

    private org.openmrs.Drug resolveLineDrug(PatientDispensingRequestLine requestLine) {
        if (requestLine == null || requestLine.getItem() == null) {
            return null;
        }
        if (requestLine.getItem().getDrug() != null) {
            return requestLine.getItem().getDrug();
        }
        ProductMasterItem productMasterItem = resolveProductMasterItem(requestLine);
        return productMasterItem == null ? null : productMasterItem.getDrug();
    }

    private Concept resolveLineConcept(PatientDispensingRequestLine requestLine, org.openmrs.Drug resolvedDrug) {
        if (resolvedDrug != null && resolvedDrug.getConcept() != null) {
            return resolvedDrug.getConcept();
        }
        if (requestLine != null && requestLine.getItem() != null && requestLine.getItem().getConcept() != null) {
            return requestLine.getItem().getConcept();
        }
        ProductMasterItem productMasterItem = resolveProductMasterItem(requestLine);
        return productMasterItem != null && productMasterItem.getDrug() != null ? productMasterItem.getDrug().getConcept() : null;
    }

    private ProductMasterItem resolveProductMasterItem(PatientDispensingRequestLine requestLine) {
        if (requestLine == null || requestLine.getItem() == null) {
            return null;
        }
        PharmacyService pharmacyService = Context.getService(PharmacyService.class);
        String code = requestLine.getItem().getCode();
        if (pharmacyService != null && code != null && !code.trim().isEmpty()) {
            ProductMasterItem byCode = pharmacyService.getProductMasterItemByNpcCode(code.trim());
            if (byCode != null) {
                return byCode;
            }
        }
        if (pharmacyService == null) {
            return null;
        }
        List<ProductMasterItem> productMasterItems = pharmacyService.getAllProductMasterItems();
        if (productMasterItems == null) {
            return null;
        }
        for (ProductMasterItem item : productMasterItems) {
            if (item != null && item.getItemName() != null && requestLine.getItem().getName() != null
                    && item.getItemName().equalsIgnoreCase(requestLine.getItem().getName())) {
                return item;
            }
        }
        return null;
    }

    private Integer findProductMasterItemId(org.openmrs.Drug drug) {
        if (drug == null || drug.getDrugId() == null) {
            return null;
        }
        List<ProductMasterItem> productMasterItems = Context.getService(PharmacyService.class).getAllProductMasterItems();
        if (productMasterItems == null) {
            return null;
        }
        for (ProductMasterItem item : productMasterItems) {
            if (item != null && item.getDrug() != null && drug.getDrugId().equals(item.getDrug().getDrugId())) {
                return item.getProductMasterItemId();
            }
        }
        return null;
    }

    private static class DrugOrderHistoryInfo {
        private Integer orderId;
        private Integer productMasterItemId;
        private Date startDate;
        private String dose = "";
        private String doseUnits = "";
        private String route = "";
        private String frequency = "";
        private String duration = "";
        private String durationUnits = "";
        private String instructions = "";
    }
}
