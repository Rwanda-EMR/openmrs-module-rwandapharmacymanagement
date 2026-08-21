package org.openmrs.module.rwandapharmacymanagement.page.controller.consumable;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.ConsumableOrderService;
import org.openmrs.module.rwandapharmacymanagement.api.DispensingService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableBillingReadiness;
import org.openmrs.module.rwandapharmacymanagement.api.dto.ConsumableOrderLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller("consumableEnterPageController")
public class EnterPageController {

    private static final String MATERIAL_TYPE_DRUG = "ZMED";
    private static final String DEFAULT_UNIT_OF_MEASURE = "EA";
    private static final String FLASH_ORDERED = "rpm.consumableEnter.ordered";
    private static final String FLASH_REQUEST_COUNT = "rpm.consumableEnter.requestCount";
    private static final String FLASH_ERROR_MESSAGE = "rpm.consumableEnter.errorMessage";
    private static final int HISTORY_PAGE_SIZE = 10;

    public void get(PageModel model,
                    @SpringBean("consumableOrderService") ConsumableOrderService consumableOrderService,
                    @SpringBean("dispensingService") DispensingService dispensingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @RequestParam(value = "patientId", required = false) String patientIdOrUuid,
                    @RequestParam(value = "historyPage", required = false) Integer historyPage,
                    HttpSession session) {
        Patient patient = resolvePatient(patientIdOrUuid);
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        List<Map<String, Object>> orderableItems = buildOrderableItems(pharmacyService, inventoryService);
        int currentHistoryPage = historyPage == null || historyPage.intValue() < 0 ? 0 : historyPage.intValue();
        int historyFirstResult = currentHistoryPage * HISTORY_PAGE_SIZE;
        long historyTotal = patient == null || patient.getPatientId() == null ? 0L
                : dispensingService.countPatientConsumableHistory(patient.getPatientId());
        List<PatientDispensingRequest> historyRequests = patient == null || patient.getPatientId() == null
                ? Collections.<PatientDispensingRequest>emptyList()
                : dispensingService.getPatientConsumableHistory(patient.getPatientId(), historyFirstResult, HISTORY_PAGE_SIZE);

        model.addAttribute("patient", patient);
        model.addAttribute("currentLocation", currentLocation);
        model.addAttribute("patientReferenceForSubmit",
                patient != null && patient.getPatientId() != null ? String.valueOf(patient.getPatientId()) : null);
        model.addAttribute("patientUuidForSubmit", patient == null ? null : patient.getUuid());
        model.addAttribute("orderableItems", orderableItems);
        model.addAttribute("consumableHistory", buildConsumableHistoryRows(historyRequests, orderableItems, dispensingService));
        model.addAttribute("historyPage", Integer.valueOf(currentHistoryPage));
        model.addAttribute("historyPageSize", Integer.valueOf(HISTORY_PAGE_SIZE));
        model.addAttribute("historyTotal", Long.valueOf(historyTotal));
        model.addAttribute("historyPreviousPage", currentHistoryPage > 0 ? Integer.valueOf(currentHistoryPage - 1) : null);
        model.addAttribute("historyNextPage", historyFirstResult + HISTORY_PAGE_SIZE < historyTotal
                ? Integer.valueOf(currentHistoryPage + 1) : null);
        ConsumableBillingReadiness billingReadiness = consumableOrderService.getBillingReadiness(patient);
        model.addAttribute("billingReady", billingReadiness.isReady());
        model.addAttribute("billingReadinessMessage", billingReadiness.getMessage());
        model.addAttribute("canAddConsumables", patient != null && currentLocation != null && billingReadiness.isReady());
        Integer requestCount = FlashMessageSupport.consumeInteger(session, FLASH_REQUEST_COUNT);
        model.addAttribute("ordered", FlashMessageSupport.consumeBoolean(session, FLASH_ORDERED));
        model.addAttribute("errorMessage", FlashMessageSupport.consumeString(session, FLASH_ERROR_MESSAGE));
        model.addAttribute("requestCount", requestCount == null || requestCount.intValue() < 1 ? 0 : requestCount.intValue());
    }

    public String post(@SpringBean("consumableOrderService") ConsumableOrderService consumableOrderService,
                       @RequestParam(value = "patientId", required = false) String patientIdOrUuid,
                       @RequestParam(value = "patientUuid", required = false) String patientUuid,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "requestId", required = false) Integer requestId,
                       @RequestParam(value = "requestLineId", required = false) Integer requestLineId,
                       @RequestParam(value = "voidReason", required = false) String voidReason,
                       @RequestParam(value = "productMasterItemId", required = false) Integer[] productMasterItemIds,
                       @RequestParam(value = "quantity", required = false) BigDecimal[] quantities,
                       @RequestParam(value = "unitOfMeasure", required = false) String[] unitOfMeasures,
                       @RequestParam(value = "billable", required = false) String[] billables,
                       @RequestParam(value = "billingExclusionReason", required = false) String[] billingExclusionReasons,
                       HttpSession session,
                       UiUtils ui) {
        Patient patient = resolvePatient(patientIdOrUuid);
        if (patient == null) {
            patient = resolvePatient(patientUuid);
        }
        if (patient == null) {
            throw new IllegalArgumentException("Patient not found");
        }
        String normalizedAction = action == null ? "" : action.trim();
        if ("updateLine".equalsIgnoreCase(normalizedAction)) {
            try {
                List<ConsumableOrderLineCommand> commands = buildCommands(productMasterItemIds, quantities, unitOfMeasures,
                        billables, billingExclusionReasons);
                if (commands.size() != 1) {
                    throw new IllegalArgumentException("Exactly one consumable item is required for update");
                }
                consumableOrderService.updateConsumableLine(requestId, requestLineId, commands.get(0));
            }
            catch (RuntimeException e) {
                FlashMessageSupport.set(session, FLASH_ERROR_MESSAGE, e.getMessage());
            }
            return redirectToConsumablePage(ui, patient);
        }
        if ("voidLine".equalsIgnoreCase(normalizedAction)) {
            try {
                consumableOrderService.voidConsumableLine(requestId, requestLineId, voidReason);
            }
            catch (RuntimeException e) {
                FlashMessageSupport.set(session, FLASH_ERROR_MESSAGE, e.getMessage());
            }
            return redirectToConsumablePage(ui, patient);
        }
        ConsumableBillingReadiness billingReadiness = consumableOrderService.getBillingReadiness(patient);
        if (!billingReadiness.isReady()) {
            throw new IllegalArgumentException(billingReadiness.getMessage());
        }
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        List<ConsumableOrderLineCommand> commands = buildCommands(productMasterItemIds, quantities, unitOfMeasures,
                billables, billingExclusionReasons);
        int createdRequests = consumableOrderService.createConsumableOrders(patient, currentLocation, commands).size();
        FlashMessageSupport.set(session, FLASH_ORDERED, Boolean.TRUE);
        FlashMessageSupport.set(session, FLASH_REQUEST_COUNT, Integer.valueOf(createdRequests));
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "consumable/enter",
                Collections.<String, Object>singletonMap("patientId", patient.getUuid()));
    }

    private String redirectToConsumablePage(UiUtils ui, Patient patient) {
        Map<String, Object> redirectParams = new LinkedHashMap<String, Object>();
        redirectParams.put("patientId", patient.getUuid());
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "consumable/enter", redirectParams);
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

    private List<ConsumableOrderLineCommand> buildCommands(Integer[] productMasterItemIds,
                                                           BigDecimal[] quantities,
                                                           String[] unitOfMeasures,
                                                           String[] billables,
                                                           String[] billingExclusionReasons) {
        List<ConsumableOrderLineCommand> commands = new ArrayList<ConsumableOrderLineCommand>();
        if (productMasterItemIds == null) {
            return commands;
        }
        for (int i = 0; i < productMasterItemIds.length; i++) {
            Integer productMasterItemId = productMasterItemIds[i];
            BigDecimal quantity = quantities != null && quantities.length > i ? quantities[i] : null;
            if (productMasterItemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ConsumableOrderLineCommand command = new ConsumableOrderLineCommand();
            command.setProductMasterItemId(productMasterItemId);
            command.setQuantity(quantity);
            command.setUnitOfMeasure(unitOfMeasures != null && unitOfMeasures.length > i ? unitOfMeasures[i] : DEFAULT_UNIT_OF_MEASURE);
            command.setBillable(parseBillable(billables != null && billables.length > i ? billables[i] : null));
            command.setBillingExclusionReason(billingExclusionReasons != null && billingExclusionReasons.length > i
                    ? billingExclusionReasons[i] : null);
            commands.add(command);
        }
        return commands;
    }

    private Boolean parseBillable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Boolean.TRUE;
        }
        return Boolean.valueOf(!"false".equalsIgnoreCase(value.trim())
                && !"0".equals(value.trim())
                && !"no".equalsIgnoreCase(value.trim()));
    }

    private List<Map<String, Object>> buildOrderableItems(PharmacyService pharmacyService, InventoryService inventoryService) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        List<Integer> addedProductMasterIds = new ArrayList<Integer>();
        List<Item> stockedItems = getStockedConsumableItems(pharmacyService, inventoryService);
        Map<String, ProductMasterItem> productMasterByCode = new LinkedHashMap<String, ProductMasterItem>();
        Map<String, ProductMasterItem> productMasterByName = new LinkedHashMap<String, ProductMasterItem>();
        for (ProductMasterItem productMasterItem : pharmacyService.getAllProductMasterItems()) {
            if (!isConsumableProductMasterItem(productMasterItem)) {
                continue;
            }
            String codeKey = normalizeKey(productMasterItem.getNpcCode());
            if (!codeKey.isEmpty() && !productMasterByCode.containsKey(codeKey)) {
                productMasterByCode.put(codeKey, productMasterItem);
            }
            String nameKey = normalizeKey(productMasterItem.getItemName());
            if (!nameKey.isEmpty() && !productMasterByName.containsKey(nameKey)) {
                productMasterByName.put(nameKey, productMasterItem);
            }
        }
        for (Item stockedItem : stockedItems) {
            if (stockedItem == null || stockedItem.getItemId() == null) {
                continue;
            }
            ProductMasterItem productMasterItem = resolveConsumableProductMasterItem(stockedItem, productMasterByCode, productMasterByName);
            if (productMasterItem == null || productMasterItem.getProductMasterItemId() == null
                    || addedProductMasterIds.contains(productMasterItem.getProductMasterItemId())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("productMasterItemId", productMasterItem.getProductMasterItemId());
            row.put("itemId", stockedItem.getItemId());
            row.put("itemName", productMasterItem.getItemName());
            row.put("npcCode", productMasterItem.getNpcCode());
            row.put("materialType", productMasterItem.getMaterialType());
            row.put("defaultUnitOfMeasure", DEFAULT_UNIT_OF_MEASURE);
            AvailabilitySummary availabilitySummary = availabilitySummary(productMasterItem, inventoryService);
            row.put("stockSites", Integer.valueOf(availabilitySummary.stockSites));
            row.put("stockTotal", availabilitySummary.totalQuantity);
            rows.add(row);
            addedProductMasterIds.add(productMasterItem.getProductMasterItemId());
        }

        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                String leftName = left == null || left.get("itemName") == null ? "" : left.get("itemName").toString();
                String rightName = right == null || right.get("itemName") == null ? "" : right.get("itemName").toString();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return rows;
    }

    private List<Map<String, Object>> buildConsumableHistoryRows(List<PatientDispensingRequest> requests,
                                                                 List<Map<String, Object>> orderableItems,
                                                                 DispensingService dispensingService) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (requests == null) {
            return rows;
        }
        for (PatientDispensingRequest request : requests) {
            if (request == null) {
                continue;
            }
            boolean paid = request.getPatientBillId() != null
                    && Boolean.TRUE.equals(dispensingService.isPatientBillPaid(request.getPatientBillId()));
            boolean hasDispensedLine = hasLineWithStatus(request, RequestStatus.DISPENSED)
                    || hasLineWithStatus(request, RequestStatus.PARTIALLY_DISPENSED)
                    || request.getRequestStatus() == RequestStatus.DISPENSED
                    || request.getRequestStatus() == RequestStatus.PARTIALLY_DISPENSED;
            boolean editable = !paid && !hasDispensedLine && isPending(request.getRequestStatus());
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("request", request);
            row.put("paid", Boolean.valueOf(paid));
            row.put("hasDispensedLine", Boolean.valueOf(hasDispensedLine));
            row.put("editable", Boolean.valueOf(editable));
            row.put("activeLineCount", Integer.valueOf(activeLineCount(request)));
            row.put("productMasterByLineId", productMasterByLineId(request, orderableItems));
            rows.add(row);
        }
        return rows;
    }

    private Map<Integer, Integer> productMasterByLineId(PatientDispensingRequest request, List<Map<String, Object>> orderableItems) {
        Map<Integer, Integer> values = new LinkedHashMap<Integer, Integer>();
        if (request == null || request.getLines() == null || orderableItems == null) {
            return values;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line == null || line.getPatientDispensingRequestLineId() == null || line.getItem() == null) {
                continue;
            }
            Integer productMasterItemId = findProductMasterItemId(line.getItem(), orderableItems);
            if (productMasterItemId != null) {
                values.put(line.getPatientDispensingRequestLineId(), productMasterItemId);
            }
        }
        return values;
    }

    private Integer findProductMasterItemId(Item item, List<Map<String, Object>> orderableItems) {
        if (item == null || orderableItems == null) {
            return null;
        }
        for (Map<String, Object> orderableItem : orderableItems) {
            if (orderableItem == null) {
                continue;
            }
            Object itemId = orderableItem.get("itemId");
            if (item.getItemId() != null && item.getItemId().equals(itemId)) {
                return (Integer) orderableItem.get("productMasterItemId");
            }
            Object itemName = orderableItem.get("itemName");
            if (item.getName() != null && itemName != null && item.getName().trim().equalsIgnoreCase(itemName.toString().trim())) {
                return (Integer) orderableItem.get("productMasterItemId");
            }
        }
        return null;
    }

    private boolean isPending(RequestStatus status) {
        return status == RequestStatus.PENDING_PAYMENT || status == RequestStatus.PENDING_DISPENSE;
    }

    private boolean hasLineWithStatus(PatientDispensingRequest request, RequestStatus status) {
        if (request == null || request.getLines() == null || status == null) {
            return false;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided()) && line.getLineStatus() == status) {
                return true;
            }
        }
        return false;
    }

    private int activeLineCount(PatientDispensingRequest request) {
        int count = 0;
        if (request == null || request.getLines() == null) {
            return count;
        }
        for (PatientDispensingRequestLine line : request.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided())) {
                count++;
            }
        }
        return count;
    }

    private boolean isConsumableProductMasterItem(ProductMasterItem productMasterItem) {
        return productMasterItem != null
                && Boolean.TRUE.equals(productMasterItem.getActive())
                && !MATERIAL_TYPE_DRUG.equalsIgnoreCase(safe(productMasterItem.getMaterialType()));
    }

    private List<Item> getStockedConsumableItems(PharmacyService pharmacyService, InventoryService inventoryService) {
        List<Item> stockedItems = new ArrayList<Item>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            if (balance == null || balance.getPharmacyLocation() == null
                    || balance.getPharmacyLocation().getPharmacyLocationId() == null
                    || !Boolean.TRUE.equals(balance.getPharmacyLocation().getActive())
                    || !supportsConsumableOrderingSource(balance.getPharmacyLocation())
                    || balance.getItem() == null || balance.getItem().getItemId() == null
                    || Boolean.FALSE.equals(balance.getItem().getVisibleToRequesters())
                    || balance.getQuantityOnHand() == null
                    || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0
                    || containsItem(stockedItems, balance.getItem().getItemId())) {
                continue;
            }
            stockedItems.add(balance.getItem());
        }
        return stockedItems;
    }

    private boolean supportsConsumableOrderingSource(PharmacyLocation pharmacyLocation) {
        return pharmacyLocation != null;
    }

    private boolean containsItem(List<Item> items, Integer itemId) {
        if (items == null || itemId == null) {
            return false;
        }
        for (Item item : items) {
            if (item != null && item.getItemId() != null && item.getItemId().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    private ProductMasterItem resolveConsumableProductMasterItem(Item stockedItem,
                                                                 Map<String, ProductMasterItem> productMasterByCode,
                                                                 Map<String, ProductMasterItem> productMasterByName) {
        if (stockedItem == null) {
            return null;
        }
        String codeKey = normalizeKey(stockedItem.getCode());
        if (!codeKey.isEmpty() && productMasterByCode.containsKey(codeKey)) {
            return productMasterByCode.get(codeKey);
        }
        String nameKey = normalizeKey(stockedItem.getName());
        if (!nameKey.isEmpty() && productMasterByName.containsKey(nameKey)) {
            return productMasterByName.get(nameKey);
        }
        return null;
    }

    private AvailabilitySummary availabilitySummary(ProductMasterItem productMasterItem, InventoryService inventoryService) {
        AvailabilitySummary summary = new AvailabilitySummary();
        List<Integer> pharmacyLocationIds = new ArrayList<Integer>();
        for (InventoryBalance balance : inventoryService.getInventoryForPharmacy(null)) {
            if (balance == null || balance.getPharmacyLocation() == null
                    || balance.getPharmacyLocation().getPharmacyLocationId() == null
                    || !Boolean.TRUE.equals(balance.getPharmacyLocation().getActive())
                    || !supportsConsumableOrderingSource(balance.getPharmacyLocation())
                    || balance.getItem() == null
                    || Boolean.FALSE.equals(balance.getItem().getVisibleToRequesters())
                    || balance.getQuantityOnHand() == null
                    || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0
                    || !matchesProductMasterItem(balance.getItem(), productMasterItem)) {
                continue;
            }
            summary.totalQuantity = summary.totalQuantity.add(balance.getQuantityOnHand());
            Integer pharmacyLocationId = balance.getPharmacyLocation().getPharmacyLocationId();
            if (!pharmacyLocationIds.contains(pharmacyLocationId)) {
                pharmacyLocationIds.add(pharmacyLocationId);
            }
        }
        summary.stockSites = pharmacyLocationIds.size();
        return summary;
    }

    private boolean matchesProductMasterItem(Item item, ProductMasterItem productMasterItem) {
        if (item == null || productMasterItem == null) {
            return false;
        }
        String itemCode = normalizeKey(item.getCode());
        String productCode = normalizeKey(productMasterItem.getNpcCode());
        if (!itemCode.isEmpty() && itemCode.equals(productCode)) {
            return true;
        }
        String itemName = normalizeKey(item.getName());
        String productName = normalizeKey(productMasterItem.getItemName());
        return !itemName.isEmpty() && itemName.equals(productName);
    }

    private String normalizeKey(String value) {
        return safe(value).trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class AvailabilitySummary {
        private int stockSites;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
    }
}
