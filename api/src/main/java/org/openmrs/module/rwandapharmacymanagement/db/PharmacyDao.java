package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.ItemProductMasterMapping;
import org.openmrs.module.rwandapharmacymanagement.model.ItemCategory;
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

import java.util.List;

public interface PharmacyDao {

    List<Item> getAllItems();

    Item getItem(Integer itemId);

    Item getItemByCode(String code);

    Item getItemByCodeAndProgram(String code, String program);

    Item getItemByExactName(String name);

    List<Item> getDispensableDrugItems();

    java.util.Map<Integer, org.openmrs.module.rwandapharmacymanagement.api.dto.PrescribableItemInfo> getPrescribableItemInfo(
            java.util.Collection<Integer> itemIds);

    List<Item> getItemsByCodes(List<String> codes);

    List<Item> searchItems(String searchTerm, Integer limit);

    List<Item> searchItemsForVisibilityControl(String searchTerm, String visibilityFilter, Integer limit);

    Item saveItem(Item item);

    ItemProductMasterMapping getItemProductMasterMapping(Integer productMasterItemId);

    ItemProductMasterMapping saveItemProductMasterMapping(ItemProductMasterMapping mapping);

    List<ItemCategory> getAllItemCategories();

    List<PharmacyLocation> getAllPharmacyLocations();

    PharmacyLocation getPharmacyLocation(Integer pharmacyLocationId);

    PharmacyLocation getMainStore();

    PharmacyLocation getPharmacyLocationByLocationId(Integer locationId);

    PharmacyLocation savePharmacyLocation(PharmacyLocation pharmacyLocation);

    SupplierSetting getSupplierSetting();

    SupplierSetting saveSupplierSetting(SupplierSetting supplierSetting);

    List<SupplierProductMapping> getAllSupplierProductMappings();

    SupplierProductMapping getSupplierProductMappingByCode(String supplierProductCode);

    SupplierProductMapping saveSupplierProductMapping(SupplierProductMapping supplierProductMapping);

    List<SupplierPrice> getSupplierPriceHistory();

    SupplierPrice saveSupplierPrice(SupplierPrice supplierPrice);

    SupplierPrice getLatestSupplierPrice(String supplierProductCode);

    SupplierPrice getLatestSupplierPrice(String supplierProductCode, String program);

    List<ProductMasterItem> getAllProductMasterItems();

    List<ProductMasterItem> getProductMasterItems(Integer offset, Integer limit);

    List<ProductMasterItem> getProductMasterItems(Integer offset, Integer limit, String npcCode, String itemName,
                                                  String conceptName, String program, String materialType);

    Integer getProductMasterItemCount();

    Integer getProductMasterItemCount(String npcCode, String itemName, String conceptName, String program, String materialType);

    ProductMasterItem getProductMasterItem(Integer productMasterItemId);

    ProductMasterItem getProductMasterItemByNpcCode(String npcCode);

    ProductMasterItem getProductMasterItemByNpcCodeAndProgram(String npcCode, String program);

    List<ProductMasterItem> searchProductMasterItems(String searchTerm, Integer limit);

    ProductMasterItem saveProductMasterItem(ProductMasterItem productMasterItem);

    List<Supplier> getAllSuppliers();

    Supplier getSupplier(Integer supplierId);

    Supplier getSupplierByCode(String code);

    Supplier saveSupplier(Supplier supplier);

    List<PurchaseOrder> getAllPurchaseOrders();

    List<PurchaseOrder> getPurchaseOrders(String approvalStatus, java.util.Date fromInclusive, java.util.Date toInclusive,
                                          Integer offset, Integer limit);

    long countPurchaseOrdersByApprovalStatus(String approvalStatus);

    long countPurchaseOrdersByDispatchStatus(String dispatchStatus);

    PurchaseOrder getPurchaseOrder(Integer purchaseOrderId);

    List<PurchaseOrder> getDraftPurchaseOrdersForUser(User requestedBy);

    PurchaseOrder savePurchaseOrder(PurchaseOrder purchaseOrder);

    void deletePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

    SupplierOrder saveSupplierOrder(SupplierOrder supplierOrder);

    SupplierOrder getSupplierOrderByOrderNumberOrReference(String orderNumber, String supplierOrderReference);
}
