package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.ConsumableOrderService;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableOrderLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Transactional
public class ConsumableOrderServiceImpl extends BaseOpenmrsService implements ConsumableOrderService {

    private static final String MATERIAL_TYPE_DRUG = "ZMED";
    private static final String DEFAULT_UNIT_OF_MEASURE = "EA";

    private PharmacyService pharmacyService;
    private DispensingService dispensingService;

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setDispensingService(DispensingService dispensingService) {
        this.dispensingService = dispensingService;
    }

    @Override
    public List<PatientDispensingRequest> createConsumableOrders(Patient patient,
                                                                 Location requestingLocation,
                                                                 List<ConsumableOrderLineCommand> lines) {
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

        Integer patientBillId = null;
        Boolean patientBillPaid = Boolean.FALSE;
        if (containsBillingMappedLine(preparedLines)) {
            patientBillId = dispensingService.findLatestPatientBillId(patient.getPatientId());
            if (patientBillId != null) {
                patientBillPaid = dispensingService.isPatientBillPaid(patientBillId);
                if (Boolean.TRUE.equals(patientBillPaid)) {
                    patientBillId = null;
                    patientBillPaid = Boolean.FALSE;
                }
            }
        }

        Map<String, GroupedRequest> grouped = new LinkedHashMap<String, GroupedRequest>();
        for (PreparedLine preparedLine : preparedLines) {
            String key = preparedLine.billingMapped ? "billingMapped" : "notBillingMapped";
            GroupedRequest groupedRequest = grouped.get(key);
            if (groupedRequest == null) {
                groupedRequest = new GroupedRequest();
                groupedRequest.billingMapped = preparedLine.billingMapped;
                groupedRequest.patientBillId = preparedLine.billingMapped ? patientBillId : null;
                groupedRequest.paymentConfirmed = preparedLine.billingMapped
                        ? patientBillId == null || Boolean.TRUE.equals(patientBillPaid) : Boolean.TRUE;
                grouped.put(key, groupedRequest);
            }
            groupedRequest.lines.add(preparedLine);
        }

        List<PatientDispensingRequest> savedRequests = new ArrayList<PatientDispensingRequest>();
        for (GroupedRequest groupedRequest : grouped.values()) {
            PatientDispensingRequest request = new PatientDispensingRequest();
            request.setPatient(patient);
            request.setBillable(groupedRequest.billingMapped);
            request.setPaymentConfirmed(groupedRequest.paymentConfirmed);
            request.setPatientBillId(groupedRequest.patientBillId);
            request.setRequestType(RequestType.CONSUMABLE);
            request.setRequestStatus(groupedRequest.paymentConfirmed ? RequestStatus.PENDING_DISPENSE : RequestStatus.PENDING_PAYMENT);
            request.setRequestingLocation(requestingLocation);
            request.setMedicalActReference(buildMedicalActReference(requestingLocation, groupedRequest.billingMapped));

            List<PatientDispensingRequestLine> requestLines = new ArrayList<PatientDispensingRequestLine>();
            for (PreparedLine preparedLine : groupedRequest.lines) {
                PatientDispensingRequestLine requestLine = new PatientDispensingRequestLine();
                requestLine.setPatientDispensingRequest(request);
                requestLine.setItem(preparedLine.item);
                requestLine.setRequestedQuantity(preparedLine.quantity);
                requestLine.setUnitOfMeasure(preparedLine.unitOfMeasure);
                requestLine.setLineStatus(request.getRequestStatus());
                requestLines.add(requestLine);
            }
            request.setLines(requestLines);
            request = dispensingService.saveRequest(request);
            if (groupedRequest.billingMapped) {
                createBillingItems(request, groupedRequest.lines);
            }
            request.setMedicalActReference("Consumable request " + request.getPatientDispensingRequestId());
            dispensingService.saveRequest(request);
            savedRequests.add(request);
        }
        return savedRequests;
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
            PreparedLine preparedLine = new PreparedLine();
            preparedLine.productMasterItem = productMasterItem;
            preparedLine.item = item;
            preparedLine.quantity = quantity;
            preparedLine.unitOfMeasure = normalizeUnitOfMeasure(command.getUnitOfMeasure());
            preparedLine.billingMapped = hasBillingMapping(preparedLine);
            preparedLines.add(preparedLine);
        }
        return preparedLines;
    }

    private void validateConsumable(ProductMasterItem productMasterItem) {
        if (productMasterItem == null || !Boolean.TRUE.equals(productMasterItem.getActive())) {
            throw new IllegalArgumentException("Selected consumable item was not found");
        }
        if (MATERIAL_TYPE_DRUG.equalsIgnoreCase(safe(productMasterItem.getMaterialType()))) {
            throw new IllegalArgumentException("Drug items must be ordered from the prescription page");
        }
    }

    private Item resolveLocalItem(ProductMasterItem productMasterItem) {
        if (productMasterItem == null) {
            return null;
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

    private void createBillingItems(PatientDispensingRequest request, List<PreparedLine> lines) {
        if (request == null || request.getPatientBillId() == null || lines == null || lines.isEmpty()) {
            return;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Class<?> patientBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientBill");
            Class<?> consommationClass = Context.loadClass("org.openmrs.module.mohbilling.model.Consommation");
            Class<?> patientServiceBillClass = Context.loadClass("org.openmrs.module.mohbilling.model.PatientServiceBill");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                throw new IllegalStateException("Billing service is not available");
            }
            Object patientBill = billingServiceClass.getMethod("getPatientBill", Integer.class)
                    .invoke(billingService, request.getPatientBillId());
            if (patientBill == null) {
                throw new IllegalArgumentException("Patient bill " + request.getPatientBillId() + " was not found");
            }
            Object consommation = billingServiceClass.getMethod("getConsommationByPatientBill", patientBillClass)
                    .invoke(billingService, patientBill);
            if (consommation == null) {
                consommation = consommationClass.getDeclaredConstructor().newInstance();
                invokeSetter(consommation, "setPatientBill", patientBill, patientBillClass);
                invokeSetter(consommation, "setCreator", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
                invokeSetter(consommation, "setCreatedDate", new Date(), Date.class);
                invokeSetter(consommation, "setVoided", Boolean.FALSE, boolean.class);
                billingServiceClass.getMethod("saveConsommation", consommationClass).invoke(billingService, consommation);
            }

            for (PreparedLine line : lines) {
                Object billableService = resolveBillableService(billingServiceClass, billingService, line);
                if (billableService == null) {
                    continue;
                }
                Object billedItem = createBilledItem(patientServiceBillClass);
                invokeSetter(billedItem, "setServiceDate", new Date(), Date.class);
                invokeSetter(billedItem, "setService", billableService, Context.loadClass("org.openmrs.module.mohbilling.model.BillableService"));
                invokeSetter(billedItem, "setQuantity", line.quantity, BigDecimal.class);
                invokeSetter(billedItem, "setPaid", Boolean.FALSE, Boolean.class);
                invokeSetter(billedItem, "setCreator", Context.getAuthenticatedUser(), Context.loadClass("org.openmrs.User"));
                invokeSetter(billedItem, "setCreatedDate", new Date(), Date.class);
                invokeSetter(billedItem, "setConsommation", consommation, consommationClass);
                invokeSetter(billedItem, "setServiceOther", line.productMasterItem.getItemName(), String.class);
                invokeSetter(billedItem, "setServiceOtherDescription",
                        line.productMasterItem.getItemName() + " " + line.unitOfMeasure, String.class);
                BigDecimal unitPrice = resolveUnitPrice(billableService, line.productMasterItem);
                if (unitPrice != null) {
                    invokeSetter(billedItem, "setUnitPrice", unitPrice, BigDecimal.class);
                }
                invokeNoArg(consommation, "addBillItem", billedItem, patientServiceBillClass);
                billingServiceClass.getMethod("saveBilledItem", patientServiceBillClass).invoke(billingService, billedItem);
            }
        }
        catch (IllegalArgumentException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Unable to create billing items for mapped consumables", ex);
        }
    }

    private boolean hasBillingMapping(PreparedLine line) {
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            return resolveBillableService(billingServiceClass, billingService, line) != null;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private Object resolveBillableService(Class<?> billingServiceClass, Object billingService, PreparedLine line) throws Exception {
        Object concept = line.item == null ? null : line.item.getConcept();
        if (concept == null) {
            return null;
        }
        Object facilityService = billingServiceClass.getMethod("getFacilityServiceByConcept", Context.loadClass("org.openmrs.Concept"))
                .invoke(billingService, concept);
        if (facilityService == null) {
            return null;
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

    private BigDecimal resolveUnitPrice(Object billableService, ProductMasterItem productMasterItem) {
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

    private boolean containsBillingMappedLine(List<PreparedLine> lines) {
        for (PreparedLine line : lines) {
            if (line != null && line.billingMapped) {
                return true;
            }
        }
        return false;
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

    private String buildMedicalActReference(Location requestingLocation, boolean billable) {
        String locationName = requestingLocation == null ? "" : safe(requestingLocation.getName());
        return (billable ? "Billing mapped" : "Billing not mapped") + " consumable order" + (locationName.isEmpty() ? "" : " - " + locationName);
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
        private boolean billingMapped;
    }

    private static class GroupedRequest {
        private boolean billingMapped;
        private Integer patientBillId;
        private boolean paymentConfirmed;
        private final List<PreparedLine> lines = new ArrayList<PreparedLine>();
    }
}
