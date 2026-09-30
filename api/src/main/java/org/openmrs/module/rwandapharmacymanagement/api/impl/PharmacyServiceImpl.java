package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.db.PharmacyDao;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ItemCategory;
import org.openmrs.module.rwandapharmacymanagement.model.ItemProductMasterMapping;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.Supplier;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrder;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierPrice;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierProductMapping;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierSetting;
import org.openmrs.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Date;
import java.util.Locale;

@Transactional
public class PharmacyServiceImpl extends BaseOpenmrsService implements PharmacyService {

    private PharmacyDao pharmacyDao;
    private AdministrationService administrationService;

    public void setPharmacyDao(PharmacyDao pharmacyDao) {
        this.pharmacyDao = pharmacyDao;
    }

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @Override
    public List<Item> getAllItems() {
        return pharmacyDao.getAllItems();
    }

    @Override
    public Item getItem(Integer itemId) {
        return pharmacyDao.getItem(itemId);
    }

    @Override
    public Item getItemByCode(String code) {
        return pharmacyDao.getItemByCode(code);
    }

    @Override
    public Item getItemByCodeAndProgram(String code, String program) {
        return pharmacyDao.getItemByCodeAndProgram(code, program);
    }

    @Override
    public Item getItemByExactName(String name) {
        return pharmacyDao.getItemByExactName(name);
    }

    @Override
    public List<Item> getItemsByCodes(List<String> codes) {
        return pharmacyDao.getItemsByCodes(codes);
    }

    @Override
    public List<Item> searchItems(String searchTerm, Integer limit) {
        return pharmacyDao.searchItems(searchTerm, limit);
    }

    @Override
    public List<Item> searchItemsForVisibilityControl(String searchTerm, String visibilityFilter, Integer limit) {
        return pharmacyDao.searchItemsForVisibilityControl(searchTerm, visibilityFilter, limit);
    }

    @Override
    public Item saveItem(Item item) {
        preventDuplicateNewItem(item);
        return pharmacyDao.saveItem(item);
    }

    @Override
    public Item setItemVisibleToRequesters(Integer itemId, Boolean visibleToRequesters) {
        if (itemId == null) {
            throw new IllegalArgumentException("Item is required");
        }
        Item item = pharmacyDao.getItem(itemId);
        if (item == null || Boolean.TRUE.equals(item.getVoided())) {
            throw new IllegalArgumentException("Item not found");
        }
        item.setVisibleToRequesters(Boolean.TRUE.equals(visibleToRequesters));
        return pharmacyDao.saveItem(item);
    }

    @Override
    public ItemProductMasterMapping getItemProductMasterMapping(Integer productMasterItemId) {
        return pharmacyDao.getItemProductMasterMapping(productMasterItemId);
    }

    @Override
    public ItemProductMasterMapping saveItemProductMasterMapping(ItemProductMasterMapping mapping) {
        return pharmacyDao.saveItemProductMasterMapping(mapping);
    }

    @Override
    public ItemProductMasterMapping mapProductMasterItemToItem(ProductMasterItem productMasterItem, Item item) {
        if (productMasterItem == null || productMasterItem.getProductMasterItemId() == null || item == null || item.getItemId() == null) {
            throw new IllegalArgumentException("Both product master item and pharmacy item are required for mapping");
        }
        ItemProductMasterMapping mapping = pharmacyDao.getItemProductMasterMapping(productMasterItem.getProductMasterItemId());
        if (mapping == null) {
            mapping = new ItemProductMasterMapping();
            mapping.setProductMasterItem(productMasterItem);
            mapping.setCreator(org.openmrs.api.context.Context.getAuthenticatedUser());
            mapping.setDateCreated(new Date());
        }
        mapping.setItem(item);
        mapping.setActive(Boolean.TRUE);
        return pharmacyDao.saveItemProductMasterMapping(mapping);
    }

    @Override
    public Item getMappedItemForProductMasterItem(ProductMasterItem productMasterItem) {
        if (productMasterItem == null || productMasterItem.getProductMasterItemId() == null) {
            return null;
        }
        ItemProductMasterMapping mapping = pharmacyDao.getItemProductMasterMapping(productMasterItem.getProductMasterItemId());
        return mapping == null ? null : mapping.getItem();
    }

    @Override
    public List<ItemCategory> getAllItemCategories() {
        return pharmacyDao.getAllItemCategories();
    }

    @Override
    public List<PharmacyLocation> getAllPharmacyLocations() {
        return pharmacyDao.getAllPharmacyLocations();
    }

    @Override
    public PharmacyLocation getPharmacyLocation(Integer pharmacyLocationId) {
        return pharmacyDao.getPharmacyLocation(pharmacyLocationId);
    }

    @Override
    public PharmacyLocation getMainStore() {
        return pharmacyDao.getMainStore();
    }

    @Override
    public PharmacyLocation getPharmacyLocationByLocationId(Integer locationId) {
        return pharmacyDao.getPharmacyLocationByLocationId(locationId);
    }

    @Override
    public PharmacyLocation savePharmacyLocation(PharmacyLocation pharmacyLocation) {
        boolean mainPharmacy = pharmacyLocation.getLocationType() == org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType.MAIN_PHARMACY;
        pharmacyLocation.setMainStore(mainPharmacy);
        if (mainPharmacy) {
            for (PharmacyLocation existing : pharmacyDao.getAllPharmacyLocations()) {
                if (existing.getPharmacyLocationId() != null
                        && pharmacyLocation.getPharmacyLocationId() != null
                        && existing.getPharmacyLocationId().equals(pharmacyLocation.getPharmacyLocationId())) {
                    continue;
                }
                if (isActiveMainPharmacy(existing)) {
                    throw new IllegalArgumentException("An active main pharmacy is already configured: " + existing.getName());
                }
            }
        }
        return pharmacyDao.savePharmacyLocation(pharmacyLocation);
    }

    private boolean isActiveMainPharmacy(PharmacyLocation pharmacyLocation) {
        return pharmacyLocation != null
                && !Boolean.TRUE.equals(pharmacyLocation.getVoided())
                && !Boolean.FALSE.equals(pharmacyLocation.getActive())
                && (Boolean.TRUE.equals(pharmacyLocation.getMainStore())
                || pharmacyLocation.getLocationType() == org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType.MAIN_PHARMACY);
    }

    @Override
    public SupplierSetting getSupplierSetting() {
        SupplierSetting setting = pharmacyDao.getSupplierSetting();
        if (setting == null) {
            setting = new SupplierSetting();
            setting.setBaseUrl(administrationService.getGlobalProperty("rwandapharmacymanagement.supplier.baseUrl", ""));
            setting.setUsername(administrationService.getGlobalProperty("rwandapharmacymanagement.supplier.username", ""));
            setting.setPassword(administrationService.getGlobalProperty("rwandapharmacymanagement.supplier.password", ""));
            setting.setConnectionTimeout(Integer.valueOf(administrationService.getGlobalProperty("rwandapharmacymanagement.supplier.connectionTimeout", "10000")));
            setting.setReadTimeout(Integer.valueOf(administrationService.getGlobalProperty("rwandapharmacymanagement.supplier.readTimeout", "30000")));
            setting.setIntegrationEnabled(Boolean.valueOf(administrationService.getGlobalProperty("rwandapharmacymanagement.supplier.enabled", "false")));
        }
        return setting;
    }

    @Override
    public SupplierSetting saveSupplierSetting(SupplierSetting supplierSetting) {
        administrationService.setGlobalProperty("rwandapharmacymanagement.supplier.baseUrl", safe(supplierSetting.getBaseUrl()));
        administrationService.setGlobalProperty("rwandapharmacymanagement.supplier.username", safe(supplierSetting.getUsername()));
        administrationService.setGlobalProperty("rwandapharmacymanagement.supplier.password", safe(supplierSetting.getPassword()));
        administrationService.setGlobalProperty("rwandapharmacymanagement.supplier.connectionTimeout",
                supplierSetting.getConnectionTimeout() == null ? "10000" : supplierSetting.getConnectionTimeout().toString());
        administrationService.setGlobalProperty("rwandapharmacymanagement.supplier.readTimeout",
                supplierSetting.getReadTimeout() == null ? "30000" : supplierSetting.getReadTimeout().toString());
        administrationService.setGlobalProperty("rwandapharmacymanagement.supplier.enabled",
                supplierSetting.getIntegrationEnabled() != null && supplierSetting.getIntegrationEnabled() ? "true" : "false");
        return pharmacyDao.saveSupplierSetting(supplierSetting);
    }

    @Override
    public List<SupplierProductMapping> getAllSupplierProductMappings() {
        return pharmacyDao.getAllSupplierProductMappings();
    }

    @Override
    public SupplierProductMapping getSupplierProductMappingByCode(String supplierProductCode) {
        return pharmacyDao.getSupplierProductMappingByCode(supplierProductCode);
    }

    @Override
    public SupplierProductMapping saveSupplierProductMapping(SupplierProductMapping supplierProductMapping) {
        return pharmacyDao.saveSupplierProductMapping(supplierProductMapping);
    }

    @Override
    public List<SupplierPrice> getSupplierPriceHistory() {
        return pharmacyDao.getSupplierPriceHistory();
    }

    @Override
    public SupplierPrice saveSupplierPrice(SupplierPrice supplierPrice) {
        return pharmacyDao.saveSupplierPrice(supplierPrice);
    }

    @Override
    public SupplierPrice getLatestSupplierPrice(String supplierProductCode) {
        return pharmacyDao.getLatestSupplierPrice(supplierProductCode);
    }

    @Override
    public SupplierPrice getLatestSupplierPrice(String supplierProductCode, String program) {
        return pharmacyDao.getLatestSupplierPrice(supplierProductCode, program);
    }

    @Override
    public List<ProductMasterItem> getAllProductMasterItems() {
        return pharmacyDao.getAllProductMasterItems();
    }

    @Override
    public List<ProductMasterItem> getProductMasterItems(Integer offset, Integer limit) {
        return pharmacyDao.getProductMasterItems(offset, limit);
    }

    @Override
    public List<ProductMasterItem> getProductMasterItems(Integer offset, Integer limit, String npcCode, String itemName,
                                                         String conceptName, String program, String materialType) {
        return pharmacyDao.getProductMasterItems(offset, limit, npcCode, itemName, conceptName, program, materialType);
    }

    @Override
    public Integer getProductMasterItemCount() {
        return pharmacyDao.getProductMasterItemCount();
    }

    @Override
    public Integer getProductMasterItemCount(String npcCode, String itemName, String conceptName, String program, String materialType) {
        return pharmacyDao.getProductMasterItemCount(npcCode, itemName, conceptName, program, materialType);
    }

    @Override
    public ProductMasterItem getProductMasterItem(Integer productMasterItemId) {
        return pharmacyDao.getProductMasterItem(productMasterItemId);
    }

    @Override
    public ProductMasterItem getProductMasterItemByNpcCode(String npcCode) {
        return pharmacyDao.getProductMasterItemByNpcCode(npcCode);
    }

    @Override
    public ProductMasterItem getProductMasterItemByNpcCodeAndProgram(String npcCode, String program) {
        return pharmacyDao.getProductMasterItemByNpcCodeAndProgram(npcCode, program);
    }

    @Override
    public List<ProductMasterItem> searchProductMasterItems(String searchTerm, Integer limit) {
        return pharmacyDao.searchProductMasterItems(searchTerm, limit);
    }

    @Override
    public ProductMasterItem saveProductMasterItem(ProductMasterItem productMasterItem) {
        return pharmacyDao.saveProductMasterItem(productMasterItem);
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return pharmacyDao.getAllSuppliers();
    }

    @Override
    public Supplier getSupplier(Integer supplierId) {
        return pharmacyDao.getSupplier(supplierId);
    }

    @Override
    public Supplier getSupplierByCode(String code) {
        return pharmacyDao.getSupplierByCode(code);
    }

    @Override
    public Supplier saveSupplier(Supplier supplier) {
        return pharmacyDao.saveSupplier(supplier);
    }

    @Override
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return pharmacyDao.getAllPurchaseOrders();
    }

    @Override
    public List<PurchaseOrder> getPurchaseOrders(String approvalStatus, java.util.Date fromInclusive,
                                                 java.util.Date toInclusive, Integer offset, Integer limit) {
        return pharmacyDao.getPurchaseOrders(approvalStatus, fromInclusive, toInclusive, offset, limit);
    }

    @Override
    public long countPurchaseOrdersByApprovalStatus(String approvalStatus) {
        return pharmacyDao.countPurchaseOrdersByApprovalStatus(approvalStatus);
    }

    @Override
    public long countPurchaseOrdersByDispatchStatus(String dispatchStatus) {
        return pharmacyDao.countPurchaseOrdersByDispatchStatus(dispatchStatus);
    }

    @Override
    public PurchaseOrder getPurchaseOrder(Integer purchaseOrderId) {
        return pharmacyDao.getPurchaseOrder(purchaseOrderId);
    }

    @Override
    public List<PurchaseOrder> getDraftPurchaseOrdersForUser(User requestedBy) {
        return pharmacyDao.getDraftPurchaseOrdersForUser(requestedBy);
    }

    @Override
    public PurchaseOrder savePurchaseOrder(PurchaseOrder purchaseOrder) {
        return pharmacyDao.savePurchaseOrder(purchaseOrder);
    }

    @Override
    public void deletePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        pharmacyDao.deletePurchaseOrderItem(purchaseOrderItem);
    }

    @Override
    public SupplierOrder saveSupplierOrder(SupplierOrder supplierOrder) {
        return pharmacyDao.saveSupplierOrder(supplierOrder);
    }

    @Override
    public SupplierOrder getSupplierOrderByOrderNumberOrReference(String orderNumber, String supplierOrderReference) {
        return pharmacyDao.getSupplierOrderByOrderNumberOrReference(orderNumber, supplierOrderReference);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void preventDuplicateNewItem(Item item) {
        if (item == null || item.getItemId() != null) {
            return;
        }
        Item existingByName = getItemByExactName(item.getName());
        if (existingByName != null) {
            throw new IllegalArgumentException("An active pharmacy item already exists with this name: " + item.getName());
        }
        String code = safe(item.getCode()).trim();
        if (!code.isEmpty()) {
            Item existingByCode = getItemByCode(code);
            if (existingByCode != null) {
                throw new IllegalArgumentException("An active pharmacy item already exists with this code: " + code);
            }
        }
        Integer drugId = item.getDrug() == null ? null : item.getDrug().getDrugId();
        Integer conceptId = item.getConcept() == null ? null : item.getConcept().getConceptId();
        if (drugId == null && conceptId == null) {
            return;
        }
        for (Item existing : getAllItems()) {
            if (existing == null) {
                continue;
            }
            Integer existingDrugId = existing.getDrug() == null ? null : existing.getDrug().getDrugId();
            Integer existingConceptId = existing.getConcept() == null ? null : existing.getConcept().getConceptId();
            if (drugId != null && drugId.equals(existingDrugId)) {
                throw new IllegalArgumentException("An active pharmacy item already exists for this OpenMRS drug: " + existing.getName());
            }
            if (drugId == null && conceptId != null && conceptId.equals(existingConceptId)
                    && normalized(item.getName()).equals(normalized(existing.getName()))) {
                throw new IllegalArgumentException("An active pharmacy item already exists for this concept and name: " + existing.getName());
            }
        }
    }

    private String normalized(String value) {
        return safe(value).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH);
    }
}
