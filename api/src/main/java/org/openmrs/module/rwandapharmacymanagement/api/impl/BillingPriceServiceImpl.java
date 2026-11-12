package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.BillingPriceService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional
public class BillingPriceServiceImpl extends BaseOpenmrsService implements BillingPriceService {

    private final Log log = LogFactory.getLog(getClass());
    private InventoryDao inventoryDao;
    private PricingService pricingService;

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPricingService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Override
    public BigDecimal getHighestCurrentStockCost(Item item) {
        if (item == null || item.getItemId() == null) {
            return null;
        }
        return getHighestCurrentStockCosts(java.util.Collections.singleton(item.getItemId())).get(item.getItemId());
    }

    @Override
    public Map<Integer, BigDecimal> getHighestCurrentStockCosts(Collection<Integer> itemIds) {
        Map<Integer, BigDecimal> highestCosts = new LinkedHashMap<Integer, BigDecimal>();
        if (itemIds == null || itemIds.isEmpty()) {
            return highestCosts;
        }
        Set<Integer> requestedIds = new HashSet<Integer>(itemIds);
        Date today = startOfToday();
        List<InventoryBalance> balances = inventoryDao.getInventoryForPharmacy(null);
        if (balances != null) {
            for (InventoryBalance balance : balances) {
                if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                        || !requestedIds.contains(balance.getItem().getItemId())
                        || balance.getUnitPrice() == null || !isCurrentBatch(balance, today)) {
                    continue;
                }
                Integer itemId = balance.getItem().getItemId();
                BigDecimal highest = highestCosts.get(itemId);
                if (highest == null || balance.getUnitPrice().compareTo(highest) > 0) {
                    highestCosts.put(itemId, balance.getUnitPrice());
                }
            }
        }
        return highestCosts;
    }

    @Override
    public void createBillingMapping(Item item, BigDecimal displayedPrice) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
        if (!isEnabled(RwandaPharmacyManagementConfig.GP_BILLING_CREATE_MISSING_MAPPINGS)) {
            throw new IllegalStateException("Creation of missing billing services is disabled in pharmacy settings.");
        }
        if (item == null || item.getItemId() == null) {
            throw new IllegalArgumentException("Pharmacy item was not found.");
        }
        if (getHighestCurrentStockCost(item) == null) {
            throw new IllegalStateException("A current stock batch cost is required before creating a billing service.");
        }
        if (displayedPrice == null || displayedPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Enter a valid non-negative selling price.");
        }
        BigDecimal sellingPrice = displayedPrice.setScale(2, RoundingMode.HALF_UP);
        Object billingService = getBillingService();
        Object facilityServicePrice = findMatchingFacilityService(billingService, item);
        boolean created = facilityServicePrice == null;
        if (created) {
            facilityServicePrice = newBillingFacilityServicePrice(item, sellingPrice);
            invokeRequired(billingService, "saveFacilityServicePrice", facilityServicePrice);
        } else {
            configureFacilityServicePrice(facilityServicePrice, item, sellingPrice);
            invokeRequired(billingService, "saveFacilityServicePrice", facilityServicePrice);
        }
        facilityServicePrice = findMatchingFacilityService(billingService, item);
        if (facilityServicePrice == null) {
            throw new IllegalStateException("MOH Billing did not save the facility service price.");
        }
        createOrUpdateInsuranceBillableServices(billingService, facilityServicePrice, sellingPrice);
        invokeRequired(billingService, "saveFacilityServicePrice", facilityServicePrice);
        int activeBillableCount = activeBillableServiceCount(billingService, facilityServicePrice);
        if (activeBillableCount == 0) {
            throw new IllegalStateException("MOH Billing saved the facility service but created no active insurance billable services. "
                    + "Check that service categories exist for the configured insurance providers.");
        }
        log.info("Created pharmacy billing mapping. itemId=" + item.getItemId() + ", facilityServicePriceId="
                + invokeNoArg(facilityServicePrice, "getFacilityServicePriceId") + ", billableServices="
                + activeBillableCount + ", price=" + sellingPrice);
    }

    @Override
    public void updatePricesForReceivedItems(Collection<Item> items) {
        if (!isEnabled(RwandaPharmacyManagementConfig.GP_BILLING_UPDATE_PRICES_ON_RECEIPT)
                || items == null || items.isEmpty()) {
            return;
        }
        Object billingService = getBillingService();
        Set<Integer> processedIds = new HashSet<Integer>();
        List<Integer> itemIds = new ArrayList<Integer>();
        for (Item item : items) {
            if (item != null && item.getItemId() != null && !processedIds.contains(item.getItemId())) {
                processedIds.add(item.getItemId());
                itemIds.add(item.getItemId());
            }
        }
        Map<Integer, BigDecimal> highestCosts = getHighestCurrentStockCosts(itemIds);
        processedIds.clear();
        for (Item item : items) {
            if (item == null || item.getItemId() == null || !processedIds.add(item.getItemId())) {
                continue;
            }
            try {
                BigDecimal stockCost = highestCosts.get(item.getItemId());
                BigDecimal sellingPrice = stockCost == null ? null : pricingService.calculateSellingPrice(stockCost);
                if (sellingPrice == null) {
                    log.warn("Skipping billing price update because no current stock cost is available. itemId="
                            + item.getItemId());
                    continue;
                }
                Object facilityServicePrice = findMatchingFacilityService(billingService, item);
                if (facilityServicePrice == null) {
                    continue;
                }
                BigDecimal currentPrice = extractBigDecimal(facilityServicePrice, "getFullPrice");
                if (isZero(currentPrice) && !isEnabled(RwandaPharmacyManagementConfig.GP_BILLING_UPDATE_ZERO_PRICES)) {
                    log.info("Keeping zero-priced billing service unchanged after delivery receipt. itemId="
                            + item.getItemId() + ", facilityServicePriceId="
                            + invokeNoArg(facilityServicePrice, "getFacilityServicePriceId"));
                    continue;
                }
                invokeRequired(facilityServicePrice, "setFullPrice", sellingPrice);
                invokeRequired(billingService, "saveFacilityServicePrice", facilityServicePrice);
                updateBillableServicePrices(billingService, facilityServicePrice, sellingPrice,
                        isEnabled(RwandaPharmacyManagementConfig.GP_BILLING_UPDATE_ZERO_PRICES));
                log.info("Updated MOH Billing price after delivery receipt. itemId=" + item.getItemId()
                        + ", facilityServicePriceId=" + invokeNoArg(facilityServicePrice, "getFacilityServicePriceId")
                        + ", oldPrice=" + currentPrice + ", newPrice=" + sellingPrice);
            }
            catch (Exception e) {
                throw new IllegalStateException("Unable to update MOH Billing price after delivery receipt. itemId="
                        + item.getItemId(), e);
            }
        }
    }

    @Override
    public void renameFacilityService(Integer facilityServicePriceId, String newName) {
        if (facilityServicePriceId == null || newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("A billing service and a new name are required.");
        }
        Object billingService = getBillingService();
        Object facilityServicePrice = invokeRequired(billingService, "getFacilityServicePrice", facilityServicePriceId);
        if (facilityServicePrice == null) {
            throw new IllegalStateException("MOH Billing service " + facilityServicePriceId + " was not found.");
        }
        String name = newName.trim();
        invokeRequired(facilityServicePrice, "setName", name);
        invokeRequired(facilityServicePrice, "setShortName", name);
        invokeRequired(facilityServicePrice, "setDescription", name);
        invokeRequired(billingService, "saveFacilityServicePrice", facilityServicePrice);
    }

    private BigDecimal calculateCurrentSellingPrice(Item item) {
        BigDecimal cost = getHighestCurrentStockCost(item);
        return cost == null ? null : pricingService.calculateSellingPrice(cost);
    }

    private boolean isCurrentBatch(InventoryBalance balance, Date today) {
        return balance.getQuantityOnHand() != null && balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0
                && (balance.getExpiryDate() == null || !startOfDay(balance.getExpiryDate()).before(today));
    }

    private Object newBillingFacilityServicePrice(Item item, BigDecimal price) {
        try {
            Class<?> type = Context.loadClass("org.openmrs.module.mohbilling.model.FacilityServicePrice");
            Object facilityServicePrice = type.getDeclaredConstructor().newInstance();
            configureFacilityServicePrice(facilityServicePrice, item, price);
            invokeRequired(facilityServicePrice, "setStartDate", startOfToday());
            invokeRequired(facilityServicePrice, "setEndDate", (Object) null);
            invokeRequired(facilityServicePrice, "setItemType", Integer.valueOf(1));
            invokeRequired(facilityServicePrice, "setCreatedDate", new Date());
            invokeRequired(facilityServicePrice, "setCreator", Context.getAuthenticatedUser());
            invokeRequired(facilityServicePrice, "setRetired", Boolean.FALSE);
            invokeRequired(facilityServicePrice, "setHidden", Boolean.FALSE);
            Location defaultLocation = Context.getLocationService().getDefaultLocation();
            if (defaultLocation == null) {
                throw new IllegalStateException("A default OpenMRS location is required to create a billing service.");
            }
            invokeRequired(facilityServicePrice, "setLocation", defaultLocation);
            return facilityServicePrice;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to create the MOH Billing facility service price.", e);
        }
    }

    private void configureFacilityServicePrice(Object facilityServicePrice, Item item, BigDecimal price) {
        String name = item.getName() == null ? "" : item.getName().trim();
        invokeRequired(facilityServicePrice, "setName", name);
        invokeRequired(facilityServicePrice, "setShortName", name);
        invokeRequired(facilityServicePrice, "setDescription", name);
        invokeRequired(facilityServicePrice, "setCategory", item.getDrug() == null ? "CONSOMMABLES" : "MEDICAMENTS");
        invokeRequired(facilityServicePrice, "setFullPrice", price);
        invokeRequired(facilityServicePrice, "setItemType", Integer.valueOf(1));
        invokeRequired(facilityServicePrice, "setRetired", Boolean.FALSE);
    }

    private Object findMatchingFacilityService(Object billingService, Item item) {
        List<?> allServices = (List<?>) invokeRequired(billingService, "getAllFacilityServicePrices");
        if (allServices == null) {
            return null;
        }
        Date now = new Date();
        String expectedCategory = item.getDrug() == null ? "CONSOMMABLES" : "MEDICAMENTS";
        List<Object> matches = new ArrayList<Object>();
        for (Object service : allServices) {
            if (service == null || Boolean.TRUE.equals(invokeNoArg(service, "isRetired"))
                    || !isCurrent(service, now)) {
                continue;
            }
            String category = stringValue(invokeNoArg(service, "getCategory"));
            if (!expectedCategory.equalsIgnoreCase(category)) {
                continue;
            }
            if (matchesItemName(service, item)) {
                matches.add(service);
            }
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple active MOH Billing services match pharmacy item '"
                    + item.getName() + "'. Resolve the duplicate billing services before updating prices.");
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private boolean matchesItemName(Object service, Item item) {
        String itemName = normalize(item.getName());
        String drugName = item.getDrug() == null ? "" : normalize(item.getDrug().getName());
        String[] labels = new String[] {
                normalize(stringValue(invokeNoArg(service, "getName"))),
                normalize(stringValue(invokeNoArg(service, "getShortName"))),
                normalize(stringValue(invokeNoArg(service, "getDescription")))
        };
        for (String label : labels) {
            if ((!itemName.isEmpty() && itemName.equals(label)) || (!drugName.isEmpty() && drugName.equals(label))) {
                return true;
            }
        }
        return false;
    }

    private void createOrUpdateInsuranceBillableServices(Object billingService, Object facilityServicePrice,
                                                         BigDecimal price) {
        try {
            List<?> insurances = (List<?>) invokeRequired(billingService, "getAllInsurances");
            String categoryName = stringValue(invokeNoArg(facilityServicePrice, "getCategory"));
            Date today = startOfToday();
            int createdOrUpdated = 0;
            if (insurances != null) {
                for (Object insurance : insurances) {
                    if (insurance == null || Boolean.TRUE.equals(invokeNoArg(insurance, "isVoided"))) {
                        continue;
                    }
                    Object category = findActiveServiceCategory(billingService, categoryName, insurance);
                    if (category == null) {
                        throw new IllegalStateException("MOH Billing has no " + categoryName
                                + " service category configured for insurance "
                                + stringValue(invokeNoArg(insurance, "getName")) + ".");
                    }
                    Object billableService = findCurrentBillableService(billingService, facilityServicePrice, insurance);
                    if (billableService == null) {
                        Class<?> billableClass = Context.loadClass("org.openmrs.module.mohbilling.model.BillableService");
                        billableService = billableClass.getDeclaredConstructor().newInstance();
                        invokeRequired(billableService, "setCreatedDate", new Date());
                        invokeRequired(billableService, "setCreator", Context.getAuthenticatedUser());
                    }
                    invokeRequired(billableService, "setFacilityServicePrice", facilityServicePrice);
                    invokeRequired(billableService, "setInsurance", insurance);
                    invokeRequired(billableService, "setServiceCategory", category);
                    invokeRequired(billableService, "setStartDate", today);
                    invokeRequired(billableService, "setEndDate", (Object) null);
                    invokeRequired(billableService, "setRetired", Boolean.FALSE);
                    invokeRequired(billableService, "setMaximaToPay", price);
                    invokeRequired(facilityServicePrice, "addBillableService", billableService);
                    createdOrUpdated++;
                }
            }
            if (createdOrUpdated == 0) {
                throw new IllegalStateException("No active insurance providers are configured in MOH Billing.");
            }
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to create insurance billable services for this pharmacy item.", e);
        }
    }

    private Object findActiveServiceCategory(Object billingService, String categoryName, Object insurance) {
        List<?> categories = (List<?>) invokeRequired(billingService, "getAllServiceCategories");
        if (categories == null) {
            return null;
        }
        Object insuranceId = invokeNoArg(insurance, "getInsuranceId");
        List<Object> matches = new ArrayList<Object>();
        for (Object category : categories) {
            if (category == null || Boolean.TRUE.equals(invokeNoArg(category, "isRetired"))) {
                continue;
            }
            String name = stringValue(invokeNoArg(category, "getName"));
            Object categoryInsurance = invokeNoArg(category, "getInsurance");
            Object categoryInsuranceId = invokeNoArg(categoryInsurance, "getInsuranceId");
            if (categoryName.equalsIgnoreCase(name.trim()) && insuranceId != null
                    && insuranceId.equals(categoryInsuranceId)) {
                matches.add(category);
            }
        }
        if (matches.size() > 1) {
            Object mostRecent = null;
            for (Object category : matches) {
                if (mostRecent == null || isMoreRecentServiceCategory(category, mostRecent)) {
                    mostRecent = category;
                }
            }
            log.warn("Multiple active MOH Billing service categories match. Using the most recent category. categoryName="
                    + categoryName + ", insurance=" + stringValue(invokeNoArg(insurance, "getName"))
                    + ", selectedCategoryId=" + invokeNoArg(mostRecent, "getServiceCategoryId")
                    + ", matchingCategoryIds=" + serviceCategoryIds(matches));
            return mostRecent;
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private boolean isMoreRecentServiceCategory(Object candidate, Object current) {
        Date candidateCreated = (Date) invokeNoArg(candidate, "getCreatedDate");
        Date currentCreated = (Date) invokeNoArg(current, "getCreatedDate");
        if (candidateCreated != null && currentCreated == null) {
            return true;
        }
        if (candidateCreated != null && currentCreated != null && !candidateCreated.equals(currentCreated)) {
            return candidateCreated.after(currentCreated);
        }
        Object candidateId = invokeNoArg(candidate, "getServiceCategoryId");
        Object currentId = invokeNoArg(current, "getServiceCategoryId");
        return candidateId instanceof Number && currentId instanceof Number
                && ((Number) candidateId).intValue() > ((Number) currentId).intValue();
    }

    private List<String> serviceCategoryIds(List<Object> categories) {
        List<String> ids = new ArrayList<String>();
        for (Object category : categories) {
            ids.add(String.valueOf(invokeNoArg(category, "getServiceCategoryId")));
        }
        return ids;
    }

    private Object findCurrentBillableService(Object billingService, Object facilityServicePrice, Object insurance) {
        List<?> billableServices = (List<?>) invokeRequired(billingService, "getBillableServicesByFacilityService",
                facilityServicePrice);
        if (billableServices == null) {
            return null;
        }
        Date now = new Date();
        Object insuranceId = invokeNoArg(insurance, "getInsuranceId");
        for (Object billableService : billableServices) {
            Object serviceInsurance = invokeNoArg(billableService, "getInsurance");
            if (billableService != null && !Boolean.TRUE.equals(invokeNoArg(billableService, "getRetired"))
                    && isCurrent(billableService, now)
                    && insuranceId != null && insuranceId.equals(invokeNoArg(serviceInsurance, "getInsuranceId"))) {
                return billableService;
            }
        }
        return null;
    }

    private int activeBillableServiceCount(Object billingService, Object facilityServicePrice) {
        List<?> services = (List<?>) invokeRequired(billingService, "getBillableServicesByFacilityService", facilityServicePrice);
        if (services == null) {
            return 0;
        }
        int count = 0;
        Date now = new Date();
        for (Object service : services) {
            if (service != null && !Boolean.TRUE.equals(invokeNoArg(service, "getRetired")) && isCurrent(service, now)) {
                count++;
            }
        }
        return count;
    }

    private void updateBillableServicePrices(Object billingService, Object facilityServicePrice,
                                             BigDecimal price, boolean updateZeroPrices) {
        List<?> services = (List<?>) invokeRequired(billingService, "getBillableServicesByFacilityService", facilityServicePrice);
        if (services == null) {
            return;
        }
        Date now = new Date();
        for (Object billableService : services) {
            if (billableService == null || Boolean.TRUE.equals(invokeNoArg(billableService, "getRetired"))
                    || !isCurrent(billableService, now)) {
                continue;
            }
            BigDecimal currentMaxima = extractBigDecimal(billableService, "getMaximaToPay");
            if (isZero(currentMaxima) && !updateZeroPrices) {
                continue;
            }
            invokeRequired(billableService, "setMaximaToPay", price);
        }
        invokeRequired(billingService, "saveFacilityServicePrice", facilityServicePrice);
    }

    private Object getBillingService() {
        try {
            Class<?> type = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object service = Context.getService(type);
            if (service == null) {
                throw new IllegalStateException("MOH Billing service is not available.");
            }
            return service;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IllegalStateException("MOH Billing service is not available.", e);
        }
    }

    private Object invokeRequired(Object target, String methodName, Object... arguments) {
        if (target == null) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a missing MOH Billing object.");
        }
        try {
            Method selected = null;
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterTypes().length == arguments.length
                        && parametersMatch(method.getParameterTypes(), arguments)) {
                    selected = method;
                    break;
                }
            }
            if (selected == null) {
                throw new NoSuchMethodException(methodName);
            }
            return selected.invoke(target, arguments);
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to call MOH Billing " + methodName + ".", e);
        }
    }

    private boolean parametersMatch(Class<?>[] parameterTypes, Object[] arguments) {
        for (int i = 0; i < parameterTypes.length; i++) {
            if (arguments[i] == null) {
                if (parameterTypes[i].isPrimitive()) {
                    return false;
                }
            } else if (!wrap(parameterTypes[i]).isAssignableFrom(arguments[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        return type;
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) return null;
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal extractBigDecimal(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof BigDecimal ? (BigDecimal) value : null;
    }

    private boolean isCurrent(Object service, Date now) {
        Date start = (Date) invokeNoArg(service, "getStartDate");
        Date end = (Date) invokeNoArg(service, "getEndDate");
        return (start == null || !start.after(now)) && (end == null || !startOfDay(end).before(startOfDay(now)));
    }

    private boolean isEnabled(String property) {
        return "true".equalsIgnoreCase(Context.getAdministrationService().getGlobalProperty(property, "false"));
    }

    private boolean isZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Date startOfToday() {
        return startOfDay(new Date());
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
