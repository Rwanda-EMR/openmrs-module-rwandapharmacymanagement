package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.ConsumableOrderService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableBillingReadiness;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableOrderLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.openmrs.module.rwandapharmacymanagement.util.ProductMasterPrivilegeSupport;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Transactional
public class ConsumableOrderServiceImpl extends BaseOpenmrsService implements ConsumableOrderService {

    private static final String MATERIAL_TYPE_DRUG = "ZMED";
    private static final String DEFAULT_UNIT_OF_MEASURE = "EA";

    private PharmacyService pharmacyService;
    private DispensingService dispensingService;
    private InventoryService inventoryService;

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setDispensingService(DispensingService dispensingService) {
        this.dispensingService = dispensingService;
    }

    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumableBillingReadiness getBillingReadiness(Patient patient) {
        if (patient == null || patient.getPatientId() == null) {
            return new ConsumableBillingReadiness(false, "Patient is required before adding consumables.");
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return new ConsumableBillingReadiness(false, "MOH Billing service is not available.");
            }
            Object insurancePolicy = resolveActiveVisitRegistrationInsurancePolicy(billingServiceClass, billingService, patient);
            if (insurancePolicy == null) {
                return new ConsumableBillingReadiness(false,
                        "No insurance policy from the active visit registration was found for this patient.");
            }
            Object globalBill = resolveOpenGlobalBill(billingServiceClass, billingService, insurancePolicy);
            if (globalBill == null) {
                return new ConsumableBillingReadiness(false,
                        "The insurance selected in the active visit registration has no open global bill.");
            }
            return new ConsumableBillingReadiness(true, "Ready for consumable billing.");
        }
        catch (Exception ex) {
            return new ConsumableBillingReadiness(false, "Unable to verify consumable billing readiness.");
        }
    }

    @Override
    public List<PatientDispensingRequest> createConsumableOrders(Patient patient,
                                                                 Location requestingLocation,
                                                                 List<ConsumableOrderLineCommand> lines) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ORDER_CONSUMABLES);
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        if (requestingLocation == null || requestingLocation.getLocationId() == null) {
            throw new IllegalArgumentException("Current requesting location is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("At least one consumable line is required");
        }

        List<PreparedLine> preparedLines = prepareLines(lines);
        if (preparedLines.isEmpty()) {
            throw new IllegalArgumentException("At least one valid consumable line is required");
        }
        boolean hasBillableLine = hasBillableLines(preparedLines);

        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setPatient(patient);
        request.setBillable(Boolean.valueOf(hasBillableLine));
        request.setPaymentConfirmed(Boolean.valueOf(!hasBillableLine));
        request.setPatientBillId(null);
        request.setRequestType(RequestType.CONSUMABLE);
        request.setRequestStatus(hasBillableLine ? RequestStatus.PENDING_PAYMENT : RequestStatus.PENDING_DISPENSE);
        request.setRequestingLocation(requestingLocation);
        request.setMedicalActReference(buildMedicalActReference(requestingLocation));

        List<PatientDispensingRequestLine> requestLines = new ArrayList<PatientDispensingRequestLine>();
        for (PreparedLine preparedLine : preparedLines) {
            PatientDispensingRequestLine requestLine = new PatientDispensingRequestLine();
            requestLine.setPatientDispensingRequest(request);
            requestLine.setItem(preparedLine.item);
            requestLine.setRequestedQuantity(preparedLine.quantity);
            requestLine.setUnitOfMeasure(preparedLine.unitOfMeasure);
            requestLine.setBillable(Boolean.valueOf(preparedLine.billable));
            requestLine.setBillingExclusionReason(preparedLine.billable ? null : preparedLine.billingExclusionReason);
            requestLine.setLineStatus(preparedLine.billable ? RequestStatus.PENDING_PAYMENT : RequestStatus.PENDING_DISPENSE);
            requestLines.add(requestLine);
        }
        request.setLines(requestLines);

        request = dispensingService.saveRequest(request);
        request.setMedicalActReference("Consumable request " + request.getPatientDispensingRequestId());
        if (hasBillableLine) {
            Integer patientBillId = createBillingItems(request, billablePreparedLines(preparedLines));
            request.setPatientBillId(patientBillId);
            Boolean patientBillPaid = dispensingService.isPatientBillPaid(patientBillId);
            request.setPaymentConfirmed(Boolean.TRUE.equals(patientBillPaid));
            request.setRequestStatus(Boolean.TRUE.equals(patientBillPaid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
            updateLineStatuses(request, request.getRequestStatus());
        } else {
            updateLineStatuses(request, RequestStatus.PENDING_DISPENSE);
        }
        dispensingService.saveRequest(request);

        List<PatientDispensingRequest> savedRequests = new ArrayList<PatientDispensingRequest>();
        savedRequests.add(request);
        return savedRequests;
    }

    @Override
    public PatientDispensingRequest updateConsumableLine(Integer requestId, Integer requestLineId,
                                                         ConsumableOrderLineCommand command) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ORDER_CONSUMABLES);
        if (requestId == null || requestLineId == null || command == null) {
            throw new IllegalArgumentException("Request, request line, and updated consumable are required");
        }
        PatientDispensingRequest request = requireEditableConsumableRequest(requestId);
        PatientDispensingRequestLine requestLine = findEditableConsumableLine(request, requestLineId);
        if (requestLine == null) {
            throw new IllegalArgumentException("Consumable request line was not found or cannot be edited");
        }
        PreparedLine preparedLine = prepareSingleLine(command);
        requestLine.setItem(preparedLine.item);
        requestLine.setRequestedQuantity(preparedLine.quantity);
        requestLine.setUnitOfMeasure(preparedLine.unitOfMeasure);
        requestLine.setBillable(Boolean.valueOf(preparedLine.billable));
        requestLine.setBillingExclusionReason(preparedLine.billable ? null : preparedLine.billingExclusionReason);
        rebuildConsumableBilling(request);
        return dispensingService.saveRequest(request);
    }

    @Override
    public PatientDispensingRequest voidConsumableLine(Integer requestId, Integer requestLineId, String voidReason) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ORDER_CONSUMABLES);
        String reason = voidReason == null ? "" : voidReason.trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("A void reason is required");
        }
        PatientDispensingRequest request = requireEditableConsumableRequest(requestId);
        PatientDispensingRequestLine requestLine = findEditableConsumableLine(request, requestLineId);
        if (requestLine == null) {
            throw new IllegalArgumentException("Consumable request line was not found or cannot be deleted");
        }
        requestLine.setVoided(Boolean.TRUE);
        requestLine.setVoidReason(reason);
        requestLine.setVoidedBy(Context.getAuthenticatedUser());
        requestLine.setDateVoided(new Date());
        requestLine.setLineStatus(RequestStatus.CANCELLED);
        rebuildConsumableBilling(request);
        updateRequestStatusAfterLineChange(request, reason);
        return dispensingService.saveRequest(request);
    }

    private List<PreparedLine> prepareLines(List<ConsumableOrderLineCommand> lines) {
        List<PreparedLine> preparedLines = new ArrayList<PreparedLine>();
        for (ConsumableOrderLineCommand command : lines) {
            if (command == null || command.getProductMasterItemId() == null) {
                continue;
            }
            BigDecimal quantity = normalizeQuantity(command.getQuantity());
            ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(command.getProductMasterItemId());
            validateConsumable(productMasterItem);
            Item item = resolveLocalItem(productMasterItem);
            if (item == null || item.getItemId() == null) {
                throw new IllegalArgumentException("No local item is linked to " + productMasterItem.getItemName());
            }
            if (Boolean.FALSE.equals(item.getVisibleToRequesters())) {
                throw new IllegalArgumentException("Selected consumable item is hidden from requester ordering: "
                        + productMasterItem.getItemName());
            }

            PreparedLine preparedLine = new PreparedLine();
            preparedLine.productMasterItem = productMasterItem;
            preparedLine.item = item;
            preparedLine.quantity = quantity;
            preparedLine.unitOfMeasure = normalizeUnitOfMeasure(command.getUnitOfMeasure());
            preparedLine.billable = !Boolean.FALSE.equals(command.getBillable());
            preparedLine.billingExclusionReason = normalizeBillingExclusionReason(command.getBillingExclusionReason(),
                    preparedLine.billable);
            preparedLines.add(preparedLine);
        }
        return preparedLines;
    }

    private PreparedLine prepareSingleLine(ConsumableOrderLineCommand command) {
        List<ConsumableOrderLineCommand> commands = new ArrayList<ConsumableOrderLineCommand>();
        commands.add(command);
        List<PreparedLine> preparedLines = prepareLines(commands);
        if (preparedLines.isEmpty()) {
            throw new IllegalArgumentException("Provide one valid consumable item with quantity greater than zero");
        }
        return preparedLines.get(0);
    }

    private boolean hasBillableLines(List<PreparedLine> lines) {
        if (lines == null) {
            return false;
        }
        for (PreparedLine line : lines) {
            if (line != null && line.billable) {
                return true;
            }
        }
        return false;
    }

    private List<PreparedLine> billablePreparedLines(List<PreparedLine> lines) {
        List<PreparedLine> billableLines = new ArrayList<PreparedLine>();
        if (lines == null) {
            return billableLines;
        }
        for (PreparedLine line : lines) {
            if (line != null && line.billable) {
                billableLines.add(line);
            }
        }
        return billableLines;
    }

    private String normalizeBillingExclusionReason(String reason, boolean billable) {
        if (billable) {
            return null;
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) {
            throw new IllegalArgumentException("Reason is required when a consumable item is marked not billable");
        }
        return normalizedReason;
    }

    private void validateConsumable(ProductMasterItem productMasterItem) {
        if (productMasterItem == null || Boolean.FALSE.equals(productMasterItem.getActive())) {
            throw new IllegalArgumentException("Selected consumable item was not found");
        }
        if (MATERIAL_TYPE_DRUG.equalsIgnoreCase(safe(productMasterItem.getMaterialType()))) {
            throw new IllegalArgumentException("Drug items must be ordered from the prescription page");
        }
        ProductMasterPrivilegeSupport.requireAllowed(productMasterItem);
    }

    private Item resolveLocalItem(ProductMasterItem productMasterItem) {
        if (productMasterItem == null) {
            return null;
        }
        Item mappedItem = pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
        if (mappedItem != null) {
            return mappedItem;
        }
        if (productMasterItem.getNpcCode() != null && !productMasterItem.getNpcCode().trim().isEmpty()) {
            Item byCode = pharmacyService.getItemByCodeAndProgram(productMasterItem.getNpcCode().trim(), productMasterItem.getProgram());
            if (byCode == null && safe(productMasterItem.getProgram()).trim().isEmpty()) {
                byCode = pharmacyService.getItemByCode(productMasterItem.getNpcCode().trim());
            }
            if (byCode != null) {
                return byCode;
            }
        }
        String productName = safe(productMasterItem.getItemName()).trim();
        if (productName.isEmpty()) {
            return null;
        }
        for (Item item : pharmacyService.getAllItems()) {
            if (item == null || item.getName() == null) {
                continue;
            }
            if (productName.equalsIgnoreCase(item.getName().trim())) {
                return item;
            }
        }
        return null;
    }

    private Integer createBillingItems(PatientDispensingRequest request, List<PreparedLine> lines) {
        if (request == null || request.getPatient() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Consumable request billing data is incomplete");
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Class<?> insurancePolicyClass = Context.loadClass("org.openmrs.module.mohbilling.model.InsurancePolicy");
            Class<?> insuranceClass = Context.loadClass("org.openmrs.module.mohbilling.model.Insurance");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                throw new IllegalStateException("Billing service is not available");
            }

            Object insurancePolicy = resolveActiveVisitRegistrationInsurancePolicy(billingServiceClass, billingService, request.getPatient());
            if (insurancePolicy == null) {
                throw new IllegalArgumentException("No insurance policy from the active visit registration was found for this patient. MOH Billing cannot create a consumable bill.");
            }
            Object insurance = invokeNoArg(insurancePolicy, "getInsurance");
            if (insurance == null) {
                throw new IllegalArgumentException("The patient's insurance policy has no insurance configured.");
            }
            Object beneficiary = resolveBeneficiary(billingServiceClass, billingService, insurancePolicy, request.getPatient());
            if (beneficiary == null) {
                throw new IllegalArgumentException("No MOH Billing beneficiary was found for this patient.");
            }
            Object globalBill = resolveOpenGlobalBill(billingServiceClass, billingService, insurancePolicy);
            if (globalBill == null) {
                throw new IllegalArgumentException("No open MOH Billing global bill was found for this patient.");
            }
            Object department = resolvePharmacyDepartment(billingServiceClass, billingService);
            if (department == null) {
                throw new IllegalArgumentException("No pharmacy department is configured in MOH Billing.");
            }

            Date now = new Date();
            List<Object> billedItems = new ArrayList<Object>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (PreparedLine line : lines) {
                Object billableService = resolveBillableService(billingServiceClass, billingService, line, insurance);
                if (billableService == null) {
                    throw new IllegalArgumentException("Consumable item is not mapped in billing: "
                            + (line.productMasterItem == null ? "" : line.productMasterItem.getItemName()));
                }
                Object billedItem = createBilledItem(patientServiceBillClass);
                BigDecimal unitPrice = resolveUnitPrice(billableService, line.productMasterItem);
                if (unitPrice == null) {
                    throw new IllegalArgumentException("Consumable billing price is missing: "
                            + (line.productMasterItem == null ? "" : line.productMasterItem.getItemName()));
                }
                Object facilityServicePrice = invokeNoArg(billableService, "getFacilityServicePrice");
                Object hopService = resolveHopService(billingServiceClass, billingService, facilityServicePrice);
                invokeSetter(billedItem, "setServiceDate", now, Date.class);
                invokeSetter(billedItem, "setService", billableService, Context.loadClass("org.openmrs.module.mohbilling.model.BillableService"));
                invokeSetter(billedItem, "setQuantity", line.quantity, BigDecimal.class);
                invokeSetter(billedItem, "setPaid", Boolean.FALSE, Boolean.class);
                invokeSetter(billedItem, "setCreator", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
                invokeSetter(billedItem, "setCreatedDate", now, Date.class);
                invokeSetter(billedItem, "setServiceOther", line.productMasterItem.getItemName(), String.class);
                invokeSetter(billedItem, "setServiceOtherDescription",
                        "Consumable request " + request.getPatientDispensingRequestId() + " - "
                                + line.productMasterItem.getItemName() + " " + line.unitOfMeasure, String.class);
                invokeSetter(billedItem, "setUnitPrice", unitPrice, BigDecimal.class);
                invokeSetterIfPresent(billedItem, "setItemType", invokeNoArg(facilityServicePrice, "getItemType"), Integer.class);
                if (hopService != null) {
                    invokeSetter(billedItem, "setHopService", hopService, Context.loadClass("org.openmrs.module.mohbilling.model.HopService"));
                }
                billedItems.add(billedItem);
                totalAmount = totalAmount.add(unitPrice.multiply(line.quantity));
            }

            Object patientBill = createPatientBill(totalAmount, insurancePolicy, insurancePolicyClass);
            Object insuranceBill = createInsuranceBill(totalAmount, insurance, insuranceClass);
            Object thirdPartyBill = createThirdPartyBill(totalAmount, insurancePolicy, insurancePolicyClass);
            Object consommation = consommationClass.getDeclaredConstructor().newInstance();
            invokeSetter(consommation, "setDepartment", department, Context.loadClass("org.openmrs.module.mohbilling.model.Department"));
            invokeSetter(consommation, "setBeneficiary", beneficiary, Context.loadClass("org.openmrs.module.mohbilling.model.Beneficiary"));
            invokeSetter(consommation, "setGlobalBill", globalBill, Context.loadClass("org.openmrs.module.mohbilling.model.GlobalBill"));
            invokeSetter(consommation, "setPatientBill", patientBill, patientBillClass);
            invokeSetter(consommation, "setInsuranceBill", insuranceBill, Context.loadClass("org.openmrs.module.mohbilling.model.InsuranceBill"));
            invokeSetter(consommation, "setThirdPartyBill", thirdPartyBill, Context.loadClass("org.openmrs.module.mohbilling.model.ThirdPartyBill"));
            invokeSetter(consommation, "setCreatedDate", now, Date.class);
            invokeSetter(consommation, "setCreator", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
            invokeSetter(consommation, "setVoided", Boolean.FALSE, boolean.class);
            saveConsommationWithBillingUtil(consommation, consommationClass);
            for (Object billedItem : billedItems) {
                invokeSetter(billedItem, "setConsommation", consommation, consommationClass);
                createPatientServiceBillWithBillingUtil(billedItem, patientServiceBillClass);
            }
            Object persistedConsommation = billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            validateCreatedConsumableBillItems(persistedConsommation == null ? consommation : persistedConsommation,
                    billedItems.size());
            BigDecimal globalAmount = extractBigDecimal(globalBill, "getGlobalAmount");
            invokeSetter(globalBill, "setGlobalAmount", (globalAmount == null ? BigDecimal.ZERO : globalAmount).add(totalAmount), BigDecimal.class);
            saveGlobalBillWithBillingUtil(globalBill, Context.loadClass("org.openmrs.module.mohbilling.model.GlobalBill"));
            Integer patientBillId = extractInteger(patientBill, "getPatientBillId");
            if (patientBillId == null) {
                throw new IllegalStateException("MOH Billing did not return a patient bill id.");
            }
            return patientBillId;
        }
        catch (IllegalArgumentException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Unable to create MOH Billing bill for consumables", ex);
        }
    }

    private void validateCreatedConsumableBillItems(Object consommation, int expectedItemCount) {
        int actualItemCount = countActiveBillItems(consommation);
        if (actualItemCount != expectedItemCount) {
            throw new IllegalStateException("MOH Billing created " + actualItemCount + " consumable bill item(s), expected "
                    + expectedItemCount + ". The request was not linked to the incomplete bill.");
        }
    }

    private int countActiveBillItems(Object consommation) {
        Object billItems = consommation == null ? null : invokeNoArg(consommation, "getBillItems");
        if (!(billItems instanceof Iterable)) {
            return 0;
        }
        int count = 0;
        for (Object billItem : (Iterable<?>) billItems) {
            if (billItem != null && !extractBoolean(billItem, "getVoided")) {
                count++;
            }
        }
        return count;
    }

    private boolean hasBillingMapping(PreparedLine line) {
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            return resolveBillableService(billingServiceClass, billingService, line, null) != null;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private Object resolveBillableService(Class<?> billingServiceClass, Object billingService, PreparedLine line,
                                          Object insurance) throws Exception {
        Object facilityService = resolveFacilityService(billingServiceClass, billingService, line);
        if (facilityService == null) {
            return null;
        }
        if (extractBoolean(facilityService, "isHidden")) {
            return null;
        }
        if (insurance != null) {
            Object insuranceSpecific = billingServiceClass.getMethod("getBillableServiceByConcept",
                            Context.loadClass("org.openmrs.module.mohbilling.model.FacilityServicePrice"),
                            Context.loadClass("org.openmrs.module.mohbilling.model.Insurance"))
                    .invoke(billingService, facilityService, insurance);
            if (insuranceSpecific != null && !extractBoolean(insuranceSpecific, "getRetired")) {
                return insuranceSpecific;
            }
        }
        Object billableServices = billingServiceClass.getMethod("getBillableServicesByFacilityService",
                Context.loadClass("org.openmrs.module.mohbilling.model.FacilityServicePrice")).invoke(billingService, facilityService);
        if (!(billableServices instanceof Collection)) {
            return null;
        }
        for (Object billableService : (Collection<?>) billableServices) {
            if (billableService != null && !extractBoolean(billableService, "getRetired")
                    && billableServiceMatchesName(billableService, line)) {
                return billableService;
            }
        }
        return null;
    }

    private Object resolveFacilityService(Class<?> billingServiceClass, Object billingService, PreparedLine line) throws Exception {
        Object facilityService = null;
        if (line != null && line.productMasterItem != null) {
            facilityService = billingServiceClass.getMethod("getFacilityServiceByName", String.class)
                    .invoke(billingService, line.productMasterItem.getItemName());
        }
        if (facilityService == null && line != null && line.item != null) {
            facilityService = billingServiceClass.getMethod("getFacilityServiceByName", String.class)
                    .invoke(billingService, line.item.getName());
        }
        Object concept = line == null || line.item == null ? null : line.item.getConcept();
        if (facilityService == null && concept != null) {
            facilityService = billingServiceClass.getMethod("getFacilityServiceByConcept", Context.loadClass("org.openmrs.Concept"))
                    .invoke(billingService, concept);
        }
        return facilityService;
    }

    private boolean billableServiceMatchesName(Object billableService, PreparedLine line) {
        if (line == null) {
            return false;
        }
        String expected = normalizeBillingName(line.productMasterItem == null ? null : line.productMasterItem.getItemName());
        if (expected.isEmpty()) {
            expected = normalizeBillingName(line.item == null ? null : line.item.getName());
        }
        if (expected.isEmpty()) {
            return false;
        }
        Object facilityServicePrice = invokeNoArg(billableService, "getFacilityServicePrice");
        String[] labels = new String[] {
                safeObject(invokeNoArg(facilityServicePrice, "getName")),
                safeObject(invokeNoArg(facilityServicePrice, "getShortName")),
                safeObject(invokeNoArg(facilityServicePrice, "getDescription")),
                safeObject(billableService)
        };
        for (String label : labels) {
            String normalized = normalizeBillingName(label);
            if (!normalized.isEmpty() && (normalized.equals(expected) || normalized.contains(expected))) {
                return true;
            }
        }
        return false;
    }

    private Object createBilledItem(Class<?> patientServiceBillClass) throws Exception {
        Constructor<?> constructor = patientServiceBillClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Object resolveActiveVisitRegistrationInsurancePolicy(Class<?> billingServiceClass, Object billingService,
                                                                 Patient patient) throws Exception {
        RegistrationInsurance registrationInsurance = resolveActiveVisitRegistrationInsurance(patient);
        if (registrationInsurance == null || registrationInsurance.insuranceNumber.trim().isEmpty()) {
            return null;
        }
        Object policies = billingServiceClass.getMethod("getAllInsurancePoliciesByPatient", Patient.class)
                .invoke(billingService, patient);
        if (!(policies instanceof Iterable)) {
            return null;
        }
        Date now = new Date();
        for (Object policy : (Iterable<?>) policies) {
            if (policy == null || extractBoolean(policy, "isRetired")) {
                continue;
            }
            Date start = extractDate(policy, "getCoverageStartDate");
            Date end = extractDate(policy, "getExpirationDate");
            boolean startsBeforeNow = start == null || !start.after(now);
            boolean endsAfterNow = end == null || !end.before(now);
            if (startsBeforeNow && endsAfterNow && matchesRegistrationInsurance(policy, registrationInsurance)) {
                return policy;
            }
        }
        return null;
    }

    private RegistrationInsurance resolveActiveVisitRegistrationInsurance(Patient patient) throws Exception {
        Concept insuranceNumberConcept = resolveConceptFromGlobalProperty("registration.insuranceNumberConcept");
        if (insuranceNumberConcept == null) {
            return null;
        }
        Concept insuranceTypeConcept = resolveConceptFromGlobalProperty("registration.insuranceTypeConcept");
        Visit activeVisit = resolveActiveVisit(patient);
        if (activeVisit == null) {
            return null;
        }
        Encounter latestEncounter = resolveActiveVisitRegistrationEncounter(activeVisit, insuranceNumberConcept);
        if (latestEncounter == null) {
            return null;
        }
        RegistrationInsurance registrationInsurance = new RegistrationInsurance();
        registrationInsurance.insuranceNumber = resolveRegistrationInsuranceNumber(latestEncounter, insuranceNumberConcept);
        registrationInsurance.insuranceType = insuranceTypeConcept == null ? null
                : resolveRegistrationInsuranceType(latestEncounter, insuranceTypeConcept);
        return registrationInsurance;
    }

    private Visit resolveActiveVisit(Patient patient) {
        List<Visit> activeVisits = patient == null ? null : Context.getVisitService().getActiveVisitsByPatient(patient);
        if (activeVisits == null || activeVisits.isEmpty()) {
            return null;
        }
        Visit selected = null;
        for (Visit visit : activeVisits) {
            if (visit == null || visit.getStartDatetime() == null || visit.getStopDatetime() != null) {
                continue;
            }
            if (selected == null || visit.getStartDatetime().after(selected.getStartDatetime())) {
                selected = visit;
            }
        }
        return selected;
    }

    private Encounter resolveActiveVisitRegistrationEncounter(Visit activeVisit, Concept insuranceNumberConcept) throws Exception {
        EncounterType registrationEncounterType = resolveRegistrationEncounterType();
        Collection<EncounterType> encounterTypes = registrationEncounterType == null ? null : Collections.singleton(registrationEncounterType);
        List<Visit> visits = Collections.singletonList(activeVisit);
        List<Encounter> encounters = Context.getEncounterService().getEncounters(activeVisit.getPatient(), null, null, null,
                null, encounterTypes, null, null, visits, false);
        Encounter latest = latestEncounterWithInsuranceNumber(encounters, insuranceNumberConcept);
        if (latest != null || registrationEncounterType == null) {
            return latest;
        }
        encounters = Context.getEncounterService().getEncounters(activeVisit.getPatient(), null, null, null,
                null, null, null, null, visits, false);
        return latestEncounterWithInsuranceNumber(encounters, insuranceNumberConcept);
    }

    private Encounter latestEncounterWithInsuranceNumber(List<Encounter> encounters, Concept insuranceNumberConcept) {
        Encounter latest = null;
        for (Encounter encounter : encounters) {
            if (encounter == null || encounter.getEncounterDatetime() == null) {
                continue;
            }
            if (resolveRegistrationInsuranceNumber(encounter, insuranceNumberConcept).trim().isEmpty()) {
                continue;
            }
            if (latest == null || encounter.getEncounterDatetime().after(latest.getEncounterDatetime())) {
                latest = encounter;
            }
        }
        return latest;
    }

    private EncounterType resolveRegistrationEncounterType() throws Exception {
        Class<?> configClass = Context.loadClass("org.openmrs.module.rwandaemr.RwandaEmrConfig");
        List<?> configs = Context.getRegisteredComponents(configClass);
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        Object config = configs.get(0);
        Object encounterType = configClass.getMethod("getRegistrationEncounterType").invoke(config);
        return encounterType instanceof EncounterType ? (EncounterType) encounterType : null;
    }

    private Concept resolveRegistrationInsuranceType(Encounter encounter, Concept insuranceTypeConcept) {
        if (insuranceTypeConcept == null) {
            return null;
        }
        for (Obs obs : encounter.getObsAtTopLevel(false)) {
            if (obs != null && insuranceTypeConcept.equals(obs.getConcept())) {
                return obs.getValueCoded();
            }
        }
        return null;
    }

    private String resolveRegistrationInsuranceNumber(Encounter encounter, Concept insuranceNumberConcept) {
        if (insuranceNumberConcept == null) {
            return "";
        }
        for (Obs obs : encounter.getObsAtTopLevel(false)) {
            if (obs != null && insuranceNumberConcept.equals(obs.getConcept())) {
                return safeObject(obs.getValueText()).trim();
            }
        }
        return "";
    }

    private Concept resolveConceptFromGlobalProperty(String globalPropertyName) {
        String value = Context.getAdministrationService().getGlobalProperty(globalPropertyName);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String key = value.trim();
        try {
            return Context.getConceptService().getConcept(Integer.valueOf(key));
        }
        catch (NumberFormatException ignored) {
        }
        Concept concept = Context.getConceptService().getConceptByUuid(key);
        if (concept != null) {
            return concept;
        }
        if (key.contains(":")) {
            String[] mapping = key.split(":", 2);
            concept = Context.getConceptService().getConceptByMapping(mapping[1], mapping[0]);
            if (concept != null) {
                return concept;
            }
        }
        return Context.getConceptService().getConceptByName(key);
    }

    private boolean matchesRegistrationInsurance(Object policy, RegistrationInsurance registrationInsurance) {
        Object insurance = invokeNoArg(policy, "getInsurance");
        Object insuranceConcept = invokeNoArg(insurance, "getConcept");
        String policyNumber = safeObject(invokeNoArg(policy, "getInsuranceCardNo")).trim();
        if (!normalizeRegistrationNumber(registrationInsurance.insuranceNumber).equalsIgnoreCase(normalizeRegistrationNumber(policyNumber))) {
            return false;
        }
        return registrationInsurance.insuranceType == null
                || !(insuranceConcept instanceof Concept)
                || registrationInsurance.insuranceType.equals(insuranceConcept);
    }

    private String normalizeRegistrationNumber(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private Object resolveBeneficiary(Class<?> billingServiceClass, Object billingService, Object insurancePolicy,
                                      Patient patient) throws Exception {
        Object beneficiaries = invokeNoArg(insurancePolicy, "getBeneficiaries");
        if (beneficiaries instanceof Iterable) {
            for (Object beneficiary : (Iterable<?>) beneficiaries) {
                if (matchesPatient(invokeNoArg(beneficiary, "getPatient"), patient)) {
                    return beneficiary;
                }
            }
        }
        String cardNumber = safeObject(invokeNoArg(insurancePolicy, "getInsuranceCardNo")).trim();
        return cardNumber.isEmpty() ? null
                : billingServiceClass.getMethod("getBeneficiaryByPolicyNumber", String.class).invoke(billingService, cardNumber);
    }

    private Object resolveOpenGlobalBill(Class<?> billingServiceClass, Object billingService, Object insurancePolicy) throws Exception {
        String cardNumber = safeObject(invokeNoArg(insurancePolicy, "getInsuranceCardNo")).trim();
        return cardNumber.isEmpty() ? null
                : billingServiceClass.getMethod("getOpenGlobalBillByInsuranceCardNo", String.class).invoke(billingService, cardNumber);
    }

    private Object resolvePharmacyDepartment(Class<?> billingServiceClass, Object billingService) throws Exception {
        Object departments = billingServiceClass.getMethod("getAllDepartements").invoke(billingService);
        if (!(departments instanceof Iterable)) {
            return null;
        }
        Object fallback = null;
        for (Object department : (Iterable<?>) departments) {
            if (department == null) {
                continue;
            }
            if (fallback == null) {
                fallback = department;
            }
            String name = safeObject(invokeNoArg(department, "getName")).trim().toUpperCase(Locale.ENGLISH);
            if ("PHARMACIE".equals(name) || "PHARMACY".equals(name)) {
                return department;
            }
        }
        return fallback;
    }

    private Object resolveHopService(Class<?> billingServiceClass, Object billingService, Object facilityServicePrice) {
        String category = safeObject(invokeNoArg(facilityServicePrice, "getCategory")).trim();
        if (category.isEmpty()) {
            return null;
        }
        try {
            return billingServiceClass.getMethod("getHopService", String.class).invoke(billingService, category);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private Object createPatientBill(BigDecimal totalAmount, Object insurancePolicy,
                                     Class<?> insurancePolicyClass) throws Exception {
        Class<?> patientBillUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.PatientBillUtil");
        return patientBillUtilClass.getMethod("createPatientBill", BigDecimal.class, insurancePolicyClass)
                .invoke(null, totalAmount, insurancePolicy);
    }

    private Object createInsuranceBill(BigDecimal totalAmount, Object insurance, Class<?> insuranceClass) throws Exception {
        Class<?> insuranceBillUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.InsuranceBillUtil");
        return insuranceBillUtilClass.getMethod("createInsuranceBill", insuranceClass, BigDecimal.class)
                .invoke(null, insurance, totalAmount);
    }

    private Object createThirdPartyBill(BigDecimal totalAmount, Object insurancePolicy,
                                        Class<?> insurancePolicyClass) throws Exception {
        Class<?> thirdPartyBillUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.ThirdPartyBillUtil");
        return thirdPartyBillUtilClass.getMethod("createThirdPartyBill", insurancePolicyClass, BigDecimal.class)
                .invoke(null, insurancePolicy, totalAmount);
    }

    private Object saveConsommationWithBillingUtil(Object consommation, Class<?> consommationClass) throws Exception {
        Class<?> consommationUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.ConsommationUtil");
        return consommationUtilClass.getMethod("saveConsommation", consommationClass).invoke(null, consommation);
    }

    private Object createPatientServiceBillWithBillingUtil(Object billedItem, Class<?> patientServiceBillClass) throws Exception {
        Class<?> consommationUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.ConsommationUtil");
        return consommationUtilClass.getMethod("createPatientServiceBill", patientServiceBillClass).invoke(null, billedItem);
    }

    private Object saveGlobalBillWithBillingUtil(Object globalBill, Class<?> globalBillClass) throws Exception {
        Class<?> globalBillUtilClass = Context.loadClass("org.openmrs.module.mohbilling.businesslogic.GlobalBillUtil");
        return globalBillUtilClass.getMethod("saveGlobalBill", globalBillClass).invoke(null, globalBill);
    }

    private BigDecimal resolveUnitPrice(Object billableService, ProductMasterItem productMasterItem) {
        BigDecimal maximaToPay = extractBigDecimal(billableService, "getMaximaToPay");
        if (maximaToPay != null) {
            return maximaToPay;
        }
        Object facilityServicePrice = invokeNoArg(billableService, "getFacilityServicePrice");
        BigDecimal fullPrice = extractBigDecimal(facilityServicePrice, "getFullPrice");
        if (fullPrice != null) {
            return fullPrice;
        }
        return productMasterItem == null ? null : productMasterItem.getItemPrice();
    }

    private void invokeNoArg(Object target, String methodName, Object argument, Class<?> argumentType) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, argumentType);
            method.invoke(target, argument);
        }
        catch (Exception ignored) {
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

    private void invokeSetter(Object target, String methodName, Object value, Class<?> parameterType) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterType);
        method.invoke(target, value);
    }

    private void invokeSetterIfPresent(Object target, String methodName, Object value, Class<?> parameterType) {
        if (target == null || value == null) {
            return;
        }
        try {
            invokeSetter(target, methodName, value, parameterType);
        }
        catch (Exception ignored) {
        }
    }

    private boolean extractBoolean(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value instanceof Number && ((Number) value).intValue() != 0;
    }

    private BigDecimal extractBigDecimal(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return null;
    }

    private Integer extractInteger(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
    }

    private Date extractDate(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Date ? (Date) value : null;
    }

    private boolean matchesPatient(Object candidate, Patient patient) {
        if (!(candidate instanceof Patient) || patient == null) {
            return false;
        }
        Patient candidatePatient = (Patient) candidate;
        if (candidatePatient.getPatientId() != null && patient.getPatientId() != null) {
            return candidatePatient.getPatientId().equals(patient.getPatientId());
        }
        return candidatePatient.getUuid() != null && candidatePatient.getUuid().equals(patient.getUuid());
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Consumable quantity must be greater than zero");
        }
        return quantity.stripTrailingZeros();
    }

    private String normalizeUnitOfMeasure(String unitOfMeasure) {
        String normalized = safe(unitOfMeasure).trim();
        return normalized.isEmpty() ? DEFAULT_UNIT_OF_MEASURE : normalized.toUpperCase(Locale.ENGLISH);
    }

    private String buildMedicalActReference(Location requestingLocation) {
        String locationName = requestingLocation == null ? "" : safe(requestingLocation.getName());
        return "Consumable order" + (locationName.isEmpty() ? "" : " - " + locationName);
    }

    private PatientDispensingRequest requireEditableConsumableRequest(Integer requestId) {
        PatientDispensingRequest request = dispensingService.getRequest(requestId);
        if (request == null || Boolean.TRUE.equals(request.getVoided())) {
            throw new IllegalArgumentException("Consumable request not found");
        }
        if (request.getRequestType() != RequestType.CONSUMABLE) {
            throw new IllegalArgumentException("Only consumable requests can be changed here");
        }
        if (request.getRequestStatus() != RequestStatus.PENDING_PAYMENT
                && request.getRequestStatus() != RequestStatus.PENDING_DISPENSE) {
            throw new IllegalArgumentException("Only pending consumable requests can be edited or deleted");
        }
        if (hasNonPendingLine(request)) {
            throw new IllegalArgumentException("Consumable requests cannot be edited or deleted after dispensing has started");
        }
        if (request.getPatientBillId() != null && Boolean.TRUE.equals(dispensingService.isPatientBillPaid(request.getPatientBillId()))) {
            throw new IllegalArgumentException("Paid consumable requests cannot be edited or deleted");
        }
        return request;
    }

    private PatientDispensingRequestLine findEditableConsumableLine(PatientDispensingRequest request, Integer requestLineId) {
        if (request == null || requestLineId == null || request.getLines() == null) {
            return null;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getPatientDispensingRequestLineId() == null
                    || !requestLineId.equals(line.getPatientDispensingRequestLineId())
                    || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            if (line.getLineStatus() == RequestStatus.PENDING_PAYMENT
                    || line.getLineStatus() == RequestStatus.PENDING_DISPENSE) {
                return line;
            }
        }
        return null;
    }

    private boolean hasNonPendingLine(PatientDispensingRequest request) {
        if (request == null || request.getLines() == null) {
            return false;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            if (line.getLineStatus() != RequestStatus.PENDING_PAYMENT
                    && line.getLineStatus() != RequestStatus.PENDING_DISPENSE) {
                return true;
            }
        }
        return false;
    }

    private void rebuildConsumableBilling(PatientDispensingRequest request) {
        if (request == null) {
            return;
        }
        if (request.getPatientBillId() != null) {
            voidLinkedConsumableBill(request, "Consumable request changed");
            request.setPatientBillId(null);
        }
        List<PreparedLine> billableLines = activeBillablePreparedLines(request);
        if (billableLines.isEmpty()) {
            request.setBillable(Boolean.FALSE);
            request.setPaymentConfirmed(Boolean.TRUE);
            request.setRequestStatus(RequestStatus.PENDING_DISPENSE);
            updateLineStatuses(request, RequestStatus.PENDING_DISPENSE);
            return;
        }
        Integer patientBillId = createBillingItems(request, billableLines);
        request.setPatientBillId(patientBillId);
        Boolean patientBillPaid = dispensingService.isPatientBillPaid(patientBillId);
        request.setBillable(Boolean.TRUE);
        request.setPaymentConfirmed(Boolean.TRUE.equals(patientBillPaid));
        request.setRequestStatus(Boolean.TRUE.equals(patientBillPaid) ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
        updateLineStatuses(request, request.getRequestStatus());
    }

    private List<PreparedLine> activeBillablePreparedLines(PatientDispensingRequest request) {
        List<PreparedLine> preparedLines = new ArrayList<PreparedLine>();
        if (request == null || request.getLines() == null) {
            return preparedLines;
        }
        for (PatientDispensingRequestLine requestLine : request.getLines()) {
            if (requestLine == null || Boolean.TRUE.equals(requestLine.getVoided())
                    || !Boolean.TRUE.equals(requestLine.getBillable())) {
                continue;
            }
            ProductMasterItem productMasterItem = resolveProductMasterItem(requestLine.getItem());
            if (productMasterItem == null) {
                throw new IllegalArgumentException("Consumable item is not linked to product master: "
                        + (requestLine.getItem() == null ? "" : requestLine.getItem().getName()));
            }
            PreparedLine preparedLine = new PreparedLine();
            preparedLine.productMasterItem = productMasterItem;
            preparedLine.item = requestLine.getItem();
            preparedLine.quantity = normalizeQuantity(requestLine.getRequestedQuantity());
            preparedLine.unitOfMeasure = normalizeUnitOfMeasure(requestLine.getUnitOfMeasure());
            preparedLine.billable = true;
            preparedLines.add(preparedLine);
        }
        return preparedLines;
    }

    private ProductMasterItem resolveProductMasterItem(Item item) {
        if (item == null) {
            return null;
        }
        for (ProductMasterItem productMasterItem : pharmacyService.getAllProductMasterItems()) {
            if (!isConsumableProductMasterItem(productMasterItem)) {
                continue;
            }
            Item mappedItem = pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
            if (mappedItem != null && mappedItem.getItemId() != null && mappedItem.getItemId().equals(item.getItemId())) {
                return productMasterItem;
            }
        }
        String code = safe(item.getCode()).trim();
        if (!code.isEmpty()) {
            ProductMasterItem byCode = pharmacyService.getProductMasterItemByNpcCode(code);
            if (isConsumableProductMasterItem(byCode)) {
                return byCode;
            }
        }
        String itemName = safe(item.getName()).trim();
        if (!itemName.isEmpty()) {
            for (ProductMasterItem productMasterItem : pharmacyService.getAllProductMasterItems()) {
                if (isConsumableProductMasterItem(productMasterItem)
                        && itemName.equalsIgnoreCase(safe(productMasterItem.getItemName()).trim())) {
                    return productMasterItem;
                }
            }
        }
        return null;
    }

    private void updateRequestStatusAfterLineChange(PatientDispensingRequest request, String reason) {
        boolean hasActiveLine = false;
        if (request != null && request.getLines() != null) {
            for (PatientDispensingRequestLine line : request.getLines()) {
                if (line != null && !Boolean.TRUE.equals(line.getVoided())) {
                    hasActiveLine = true;
                    break;
                }
            }
        }
        if (hasActiveLine) {
            return;
        }
        request.setVoided(Boolean.TRUE);
        request.setVoidReason(reason);
        request.setVoidedBy(Context.getAuthenticatedUser());
        request.setDateVoided(new Date());
        request.setRequestStatus(RequestStatus.CANCELLED);
    }

    private void voidLinkedConsumableBill(PatientDispensingRequest request, String reason) {
        if (request == null || request.getPatientBillId() == null) {
            return;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return;
            }
            Object patientBill = billingServiceClass.getMethod("getPatientBill", Integer.class)
                    .invoke(billingService, request.getPatientBillId());
            if (patientBill == null || extractBoolean(patientBill, "getIsPaid")) {
                return;
            }
            Object consommation = billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            Object globalBill = consommation == null ? null : invokeNoArg(consommation, "getGlobalBill");
            BigDecimal billAmount = activeConsumableBillItemsTotal(consommation);
            invokeSetter(patientBill, "setAmount", BigDecimal.ZERO, BigDecimal.class);
            invokeSetter(patientBill, "setVoided", Boolean.TRUE, Boolean.class);
            invokeSetter(patientBill, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
            invokeSetter(patientBill, "setVoidedDate", new Date(), Date.class);
            invokeSetter(patientBill, "setVoidReason", reason, String.class);
            invokeSetter(patientBill, "setStatus", "CANCELLED", String.class);
            billingServiceClass.getMethod("savePatientBill", patientBillClass).invoke(billingService, patientBill);
            if (consommation != null) {
                voidConsumableBillItems(billingServiceClass, billingService, patientServiceBillClass, consommation, reason);
                invokeSetter(consommation, "setVoided", Boolean.TRUE, boolean.class);
                invokeSetter(consommation, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
                invokeSetter(consommation, "setVoidedDate", new Date(), Date.class);
                invokeSetter(consommation, "setVoidReason", reason, String.class);
                zeroAndVoidNestedBill(billingServiceClass, billingService, consommation, "getInsuranceBill",
                        "org.openmrs.module.mohbilling.model.InsuranceBill", "saveInsuranceBill", reason);
                zeroAndVoidNestedBill(billingServiceClass, billingService, consommation, "getThirdPartyBill",
                        "org.openmrs.module.mohbilling.model.ThirdPartyBill", "saveThirdPartyBill", reason);
                detachConsommationFromGlobalBill(consommation);
                billingServiceClass.getMethod("saveConsommation", consommationClass).invoke(billingService, consommation);
                if (globalBill != null && billAmount != null) {
                    BigDecimal globalAmount = extractBigDecimal(globalBill, "getGlobalAmount");
                    BigDecimal adjusted = (globalAmount == null ? BigDecimal.ZERO : globalAmount).subtract(billAmount);
                    invokeSetter(globalBill, "setGlobalAmount",
                            adjusted.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : adjusted, BigDecimal.class);
                    saveGlobalBillWithBillingUtil(globalBill, Context.loadClass("org.openmrs.module.mohbilling.model.GlobalBill"));
                }
            }
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to void existing consumable bill before rebuilding it", e);
        }
    }

    private BigDecimal activeConsumableBillItemsTotal(Object consommation) {
        Object billItems = consommation == null ? null : invokeNoArg(consommation, "getBillItems");
        if (!(billItems instanceof Iterable)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Object billItem : (Iterable<?>) billItems) {
            if (billItem == null || extractBoolean(billItem, "getVoided")) {
                continue;
            }
            BigDecimal quantity = extractBigDecimal(billItem, "getQuantity");
            BigDecimal unitPrice = extractBigDecimal(billItem, "getUnitPrice");
            if (quantity != null && unitPrice != null) {
                total = total.add(quantity.multiply(unitPrice));
            }
        }
        return total;
    }

    private void detachConsommationFromGlobalBill(Object consommation) throws Exception {
        if (consommation == null || invokeNoArg(consommation, "getGlobalBill") == null) {
            return;
        }
        invokeSetter(consommation, "setGlobalBill", null,
                Context.loadClass("org.openmrs.module.mohbilling.model.GlobalBill"));
    }

    private void voidConsumableBillItems(Class<?> billingServiceClass,
                                         Object billingService,
                                         Class<?> patientServiceBillClass,
                                         Object consommation,
                                         String reason) throws Exception {
        Object billItems = consommation == null ? null : invokeNoArg(consommation, "getBillItems");
        if (!(billItems instanceof Iterable)) {
            return;
        }
        for (Object billItem : (Iterable<?>) billItems) {
            if (billItem == null || extractBoolean(billItem, "getVoided")) {
                continue;
            }
            invokeSetter(billItem, "setVoided", Boolean.TRUE, Boolean.class);
            invokeSetter(billItem, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
            invokeSetter(billItem, "setVoidedDate", new Date(), Date.class);
            invokeSetter(billItem, "setVoidReason", reason, String.class);
            billingServiceClass.getMethod("saveBilledItem", patientServiceBillClass).invoke(billingService, billItem);
        }
    }

    private void zeroAndVoidNestedBill(Class<?> billingServiceClass,
                                       Object billingService,
                                       Object consommation,
                                       String getterName,
                                       String billClassName,
                                       String saveMethodName,
                                       String reason) throws Exception {
        Object nestedBill = consommation == null ? null : invokeNoArg(consommation, getterName);
        if (nestedBill == null) {
            return;
        }
        invokeSetter(nestedBill, "setAmount", BigDecimal.ZERO, BigDecimal.class);
        invokeSetter(nestedBill, "setVoided", Boolean.TRUE, boolean.class);
        invokeSetter(nestedBill, "setVoidedBy", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
        invokeSetter(nestedBill, "setVoidedDate", new Date(), Date.class);
        invokeSetter(nestedBill, "setVoidReason", reason, String.class);
        billingServiceClass.getMethod(saveMethodName, Context.loadClass(billClassName)).invoke(billingService, nestedBill);
    }

    private void updateLineStatuses(PatientDispensingRequest request, RequestStatus status) {
        if (request == null || request.getLines() == null) {
            return;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line != null) {
                line.setLineStatus(Boolean.TRUE.equals(line.getBillable()) ? status : RequestStatus.PENDING_DISPENSE);
            }
        }
    }

    private boolean isConsumableProductMasterItem(ProductMasterItem productMasterItem) {
        return productMasterItem != null
                && !Boolean.FALSE.equals(productMasterItem.getActive())
                && !MATERIAL_TYPE_DRUG.equalsIgnoreCase(safe(productMasterItem.getMaterialType()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeObject(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeBillingName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH);
    }

    private static class PreparedLine {
        private ProductMasterItem productMasterItem;
        private Item item;
        private BigDecimal quantity;
        private String unitOfMeasure;
        private boolean billable = true;
        private String billingExclusionReason;
    }

    private static class RegistrationInsurance {
        private Concept insuranceType;
        private String insuranceNumber = "";
    }
}
