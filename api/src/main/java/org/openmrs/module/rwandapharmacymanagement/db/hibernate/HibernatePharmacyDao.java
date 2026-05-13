package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Disjunction;
import org.openmrs.module.rwandapharmacymanagement.db.PharmacyDao;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
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
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

public class HibernatePharmacyDao implements PharmacyDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Item> getAllItems() {
        return sessionFactory.getCurrentSession().createCriteria(Item.class)
                .add(notVoided())
                .addOrder(Order.asc("name"))
                .list();
    }

    @Override
    public Item getItem(Integer itemId) {
        return (Item) sessionFactory.getCurrentSession().get(Item.class, itemId);
    }

    @Override
    public Item getItemByCode(String code) {
        return (Item) sessionFactory.getCurrentSession().createCriteria(Item.class)
                .add(Restrictions.eq("code", code))
                .add(notVoided())
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public Item getItemByCodeAndProgram(String code, String program) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Item.class)
                .add(Restrictions.eq("code", code))
                .add(notVoided());
        if (program == null || program.trim().isEmpty()) {
            criteria.add(Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", "")));
        } else {
            criteria.add(Restrictions.eq("program", program.trim()));
        }
        return (Item) criteria.setMaxResults(1).uniqueResult();
    }

    @Override
    public List<Item> getItemsByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(Item.class)
                .add(Restrictions.in("code", codes))
                .add(notVoided())
                .list();
    }

    @Override
    public List<Item> searchItems(String searchTerm, Integer limit) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Item.class)
                .add(notVoided())
                .addOrder(Order.asc("name"));
        if (hasText(searchTerm)) {
            String likeValue = "%" + searchTerm.trim() + "%";
            org.hibernate.criterion.Disjunction matches = Restrictions.disjunction();
            matches.add(Restrictions.ilike("name", likeValue));
            matches.add(Restrictions.ilike("code", likeValue));
            criteria.add(matches);
        }
        criteria.setMaxResults(limit == null || limit.intValue() <= 0 ? 20 : limit.intValue());
        return criteria.list();
    }

    @Override
    public Item saveItem(Item item) {
        sessionFactory.getCurrentSession().saveOrUpdate(item);
        return item;
    }

    @Override
    public List<ItemCategory> getAllItemCategories() {
        return sessionFactory.getCurrentSession().createCriteria(ItemCategory.class)
                .add(notVoided())
                .addOrder(Order.asc("name"))
                .list();
    }

    @Override
    public List<PharmacyLocation> getAllPharmacyLocations() {
        return sessionFactory.getCurrentSession().createCriteria(PharmacyLocation.class)
                .add(notVoided())
                .addOrder(Order.asc("name"))
                .list();
    }

    @Override
    public PharmacyLocation getPharmacyLocation(Integer pharmacyLocationId) {
        return (PharmacyLocation) sessionFactory.getCurrentSession().get(PharmacyLocation.class, pharmacyLocationId);
    }

    @Override
    public PharmacyLocation getMainStore() {
        return (PharmacyLocation) sessionFactory.getCurrentSession().createCriteria(PharmacyLocation.class)
                .add(notVoided())
                .add(activeOrUnset())
                .add(Restrictions.or(Restrictions.eq("mainStore", true),
                        Restrictions.eq("locationType", PharmacyLocationType.MAIN_PHARMACY)))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public PharmacyLocation getPharmacyLocationByLocationId(Integer locationId) {
        if (locationId == null) {
            return null;
        }
        return (PharmacyLocation) sessionFactory.getCurrentSession().createCriteria(PharmacyLocation.class)
                .createAlias("location", "location")
                .add(Restrictions.eq("location.locationId", locationId))
                .add(notVoided())
                .add(activeOrUnset())
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public PharmacyLocation savePharmacyLocation(PharmacyLocation pharmacyLocation) {
        sessionFactory.getCurrentSession().saveOrUpdate(pharmacyLocation);
        return pharmacyLocation;
    }

    @Override
    public SupplierSetting getSupplierSetting() {
        return (SupplierSetting) sessionFactory.getCurrentSession().createCriteria(SupplierSetting.class)
                .add(Restrictions.eq("voided", false))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public SupplierSetting saveSupplierSetting(SupplierSetting supplierSetting) {
        sessionFactory.getCurrentSession().saveOrUpdate(supplierSetting);
        return supplierSetting;
    }

    @Override
    public List<SupplierProductMapping> getAllSupplierProductMappings() {
        return sessionFactory.getCurrentSession().createCriteria(SupplierProductMapping.class)
                .add(Restrictions.eq("voided", false))
                .addOrder(Order.asc("supplierProductCode"))
                .list();
    }

    @Override
    public SupplierProductMapping getSupplierProductMappingByCode(String supplierProductCode) {
        return (SupplierProductMapping) sessionFactory.getCurrentSession().createCriteria(SupplierProductMapping.class)
                .add(Restrictions.eq("supplierProductCode", supplierProductCode))
                .add(Restrictions.eq("voided", false))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public SupplierProductMapping saveSupplierProductMapping(SupplierProductMapping supplierProductMapping) {
        sessionFactory.getCurrentSession().saveOrUpdate(supplierProductMapping);
        return supplierProductMapping;
    }

    @Override
    public List<SupplierPrice> getSupplierPriceHistory() {
        return sessionFactory.getCurrentSession().createCriteria(SupplierPrice.class)
                .add(Restrictions.eq("voided", false))
                .addOrder(Order.desc("effectiveDate"))
                .addOrder(Order.desc("dateCreated"))
                .setMaxResults(200)
                .list();
    }

    @Override
    public SupplierPrice saveSupplierPrice(SupplierPrice supplierPrice) {
        sessionFactory.getCurrentSession().saveOrUpdate(supplierPrice);
        return supplierPrice;
    }

    @Override
    public SupplierPrice getLatestSupplierPrice(String supplierProductCode) {
        return (SupplierPrice) sessionFactory.getCurrentSession().createCriteria(SupplierPrice.class)
                .add(Restrictions.eq("supplierProductCode", supplierProductCode))
                .add(Restrictions.eq("voided", false))
                .addOrder(Order.desc("effectiveDate"))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public SupplierPrice getLatestSupplierPrice(String supplierProductCode, String program) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SupplierPrice.class)
                .add(Restrictions.eq("supplierProductCode", supplierProductCode))
                .add(Restrictions.eq("voided", false));
        if (program == null || program.trim().isEmpty()) {
            criteria.add(Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", "")));
        } else {
            criteria.add(Restrictions.eq("program", program.trim()));
        }
        return (SupplierPrice) criteria.addOrder(Order.desc("effectiveDate"))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public List<ProductMasterItem> getAllProductMasterItems() {
        return sessionFactory.getCurrentSession().createCriteria(ProductMasterItem.class)
                .add(notVoided())
                .add(activeOrUnset())
                .addOrder(Order.asc("itemName"))
                .list();
    }

    @Override
    public List<ProductMasterItem> getProductMasterItems(Integer offset, Integer limit) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(ProductMasterItem.class)
                .add(notVoided())
                .add(activeOrUnset())
                .addOrder(Order.asc("itemName"));
        if (offset != null && offset.intValue() >= 0) {
            criteria.setFirstResult(offset.intValue());
        }
        if (limit != null && limit.intValue() > 0) {
            criteria.setMaxResults(limit.intValue());
        }
        return criteria.list();
    }

    @Override
    public List<ProductMasterItem> getProductMasterItems(Integer offset, Integer limit, String npcCode, String itemName,
                                                         String conceptName, String program, String materialType) {
        List<Integer> ids = findFilteredProductMasterItemIds(offset, limit, npcCode, itemName, conceptName, program, materialType);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(ProductMasterItem.class)
                .add(Restrictions.in("productMasterItemId", ids))
                .addOrder(Order.asc("itemName"))
                .list();
    }

    @Override
    public Integer getProductMasterItemCount() {
        Number count = (Number) sessionFactory.getCurrentSession().createCriteria(ProductMasterItem.class)
                .add(notVoided())
                .add(activeOrUnset())
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return count == null ? Integer.valueOf(0) : Integer.valueOf(count.intValue());
    }

    @Override
    public Integer getProductMasterItemCount(String npcCode, String itemName, String conceptName, String program, String materialType) {
        SQLQuery query = buildFilteredProductMasterQuery(true, npcCode, itemName, conceptName, program, materialType);
        Number count = (Number) query.uniqueResult();
        return count == null ? Integer.valueOf(0) : Integer.valueOf(count.intValue());
    }

    @Override
    public ProductMasterItem getProductMasterItem(Integer productMasterItemId) {
        return (ProductMasterItem) sessionFactory.getCurrentSession().get(ProductMasterItem.class, productMasterItemId);
    }

    @Override
    public ProductMasterItem getProductMasterItemByNpcCode(String npcCode) {
        return (ProductMasterItem) sessionFactory.getCurrentSession().createCriteria(ProductMasterItem.class)
                .add(Restrictions.eq("npcCode", npcCode))
                .add(Restrictions.eq("voided", false))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public ProductMasterItem getProductMasterItemByNpcCodeAndProgram(String npcCode, String program) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(ProductMasterItem.class)
                .add(Restrictions.eq("npcCode", npcCode))
                .add(Restrictions.eq("voided", false));
        if (program == null || program.trim().isEmpty()) {
            criteria.add(Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", "")));
        } else {
            criteria.add(Restrictions.eq("program", program.trim()));
        }
        return (ProductMasterItem) criteria.setMaxResults(1).uniqueResult();
    }

    @Override
    public List<ProductMasterItem> searchProductMasterItems(String searchTerm, Integer limit) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(ProductMasterItem.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("active", true))
                .addOrder(Order.asc("itemName"));
        if (hasText(searchTerm)) {
            String likeValue = "%" + searchTerm.trim() + "%";
            Disjunction matches = Restrictions.disjunction();
            matches.add(Restrictions.ilike("npcCode", likeValue));
            matches.add(Restrictions.ilike("itemName", likeValue));
            matches.add(Restrictions.ilike("program", likeValue));
            criteria.add(matches);
        }
        criteria.setMaxResults(limit == null || limit.intValue() <= 0 ? 20 : limit.intValue());
        return criteria.list();
    }

    @Override
    public ProductMasterItem saveProductMasterItem(ProductMasterItem productMasterItem) {
        sessionFactory.getCurrentSession().saveOrUpdate(productMasterItem);
        return productMasterItem;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> findFilteredProductMasterItemIds(Integer offset, Integer limit, String npcCode, String itemName,
                                                           String conceptName, String program, String materialType) {
        SQLQuery query = buildFilteredProductMasterQuery(false, npcCode, itemName, conceptName, program, materialType);
        if (offset != null && offset.intValue() >= 0) {
            query.setFirstResult(offset.intValue());
        }
        if (limit != null && limit.intValue() > 0) {
            query.setMaxResults(limit.intValue());
        }
        List<?> rawIds = query.list();
        List<Integer> ids = new ArrayList<Integer>();
        for (Object rawId : rawIds) {
            if (rawId instanceof Number) {
                ids.add(Integer.valueOf(((Number) rawId).intValue()));
            }
        }
        return ids;
    }

    private SQLQuery buildFilteredProductMasterQuery(boolean countOnly, String npcCode, String itemName,
                                                     String conceptName, String program, String materialType) {
        StringBuilder sql = new StringBuilder();
        if (countOnly) {
            sql.append("select count(distinct pmi.product_master_item_id) ");
        } else {
            sql.append("select distinct pmi.product_master_item_id ");
        }
        sql.append("from rpm_product_master_item pmi ")
                .append("left join drug d on d.drug_id = pmi.drug_id ")
                .append("left join concept_name dcn on dcn.concept_id = d.concept_id and dcn.voided = 0 ")
                .append("left join rpm_item i on i.code = pmi.npc_code and i.voided = 0 ")
                .append("left join concept_name icn on icn.concept_id = i.concept_id and icn.voided = 0 ")
                .append("where pmi.voided = 0 and pmi.active = 1 ");
        appendLikeClause(sql, "lower(pmi.npc_code)", npcCode, "npcCode");
        appendLikeClause(sql, "lower(pmi.item_name)", itemName, "itemName");
        appendLikeClause(sql, "lower(pmi.program)", program, "program");
        appendLikeClause(sql, "lower(pmi.material_type)", materialType, "materialType");
        if (hasText(conceptName)) {
            sql.append("and (lower(coalesce(dcn.name, '')) like :conceptName ")
                    .append("or lower(coalesce(icn.name, '')) like :conceptName) ");
        }
        if (!countOnly) {
            sql.append("order by pmi.item_name asc ");
        }
        SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery(sql.toString());
        bindLike(query, "npcCode", npcCode);
        bindLike(query, "itemName", itemName);
        bindLike(query, "program", program);
        bindLike(query, "materialType", materialType);
        bindLike(query, "conceptName", conceptName);
        return query;
    }

    private void appendLikeClause(StringBuilder sql, String column, String value, String parameter) {
        if (hasText(value)) {
            sql.append("and ").append(column).append(" like :").append(parameter).append(" ");
        }
    }

    private void bindLike(SQLQuery query, String parameter, String value) {
        if (hasText(value)) {
            query.setString(parameter, "%" + value.trim().toLowerCase() + "%");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private org.hibernate.criterion.Criterion notVoided() {
        return Restrictions.or(Restrictions.eq("voided", false), Restrictions.isNull("voided"));
    }

    private org.hibernate.criterion.Criterion activeOrUnset() {
        return Restrictions.or(Restrictions.eq("active", true), Restrictions.isNull("active"));
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return sessionFactory.getCurrentSession().createCriteria(Supplier.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("active", true))
                .addOrder(Order.asc("name"))
                .list();
    }

    @Override
    public Supplier getSupplier(Integer supplierId) {
        return (Supplier) sessionFactory.getCurrentSession().get(Supplier.class, supplierId);
    }

    @Override
    public Supplier getSupplierByCode(String code) {
        return (Supplier) sessionFactory.getCurrentSession().createCriteria(Supplier.class)
                .add(Restrictions.eq("code", code))
                .add(Restrictions.eq("voided", false))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public Supplier saveSupplier(Supplier supplier) {
        sessionFactory.getCurrentSession().saveOrUpdate(supplier);
        return supplier;
    }

    @Override
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return sessionFactory.getCurrentSession().createCriteria(PurchaseOrder.class)
                .add(Restrictions.eq("voided", false))
                .addOrder(Order.desc("requestedDate"))
                .addOrder(Order.desc("purchaseOrderId"))
                .list();
    }

    @Override
    public List<PurchaseOrder> getPurchaseOrders(String approvalStatus, java.util.Date fromInclusive,
                                                 java.util.Date toInclusive, Integer offset, Integer limit) {
        org.hibernate.Criteria criteria = purchaseOrderCriteria(approvalStatus, fromInclusive, toInclusive);
        criteria.addOrder(Order.desc("requestedDate"));
        criteria.addOrder(Order.desc("purchaseOrderId"));
        if (offset != null && offset > 0) {
            criteria.setFirstResult(offset);
        }
        if (limit != null && limit > 0) {
            criteria.setMaxResults(limit);
        }
        return criteria.list();
    }

    @Override
    public long countPurchaseOrdersByApprovalStatus(String approvalStatus) {
        Number count = (Number) purchaseOrderCriteria(approvalStatus, null, null)
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return count == null ? 0L : count.longValue();
    }

    @Override
    public long countPurchaseOrdersByDispatchStatus(String dispatchStatus) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PurchaseOrder.class)
                .add(Restrictions.eq("voided", false));
        if (dispatchStatus != null && dispatchStatus.trim().length() > 0) {
            criteria.add(Restrictions.eq("dispatchStatus", dispatchStatus.trim()));
        }
        Number count = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
        return count == null ? 0L : count.longValue();
    }

    private org.hibernate.Criteria purchaseOrderCriteria(String approvalStatus, java.util.Date fromInclusive,
                                                        java.util.Date toInclusive) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PurchaseOrder.class)
                .add(Restrictions.eq("voided", false));
        if (approvalStatus != null && approvalStatus.trim().length() > 0 && !"ALL".equalsIgnoreCase(approvalStatus.trim())) {
            if ("DISPATCHED".equalsIgnoreCase(approvalStatus.trim())) {
                criteria.add(Restrictions.eq("dispatchStatus", approvalStatus.trim()));
            } else {
                criteria.add(Restrictions.eq("approvalStatus", approvalStatus.trim()));
            }
        }
        if (fromInclusive != null) {
            criteria.add(Restrictions.ge("requestedDate", fromInclusive));
        }
        if (toInclusive != null) {
            criteria.add(Restrictions.le("requestedDate", toInclusive));
        }
        return criteria;
    }

    @Override
    public PurchaseOrder getPurchaseOrder(Integer purchaseOrderId) {
        return (PurchaseOrder) sessionFactory.getCurrentSession().get(PurchaseOrder.class, purchaseOrderId);
    }

    @Override
    public PurchaseOrder savePurchaseOrder(PurchaseOrder purchaseOrder) {
        sessionFactory.getCurrentSession().saveOrUpdate(purchaseOrder);
        return purchaseOrder;
    }

    @Override
    public void deletePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        sessionFactory.getCurrentSession().delete(purchaseOrderItem);
    }

    @Override
    public SupplierOrder saveSupplierOrder(SupplierOrder supplierOrder) {
        sessionFactory.getCurrentSession().saveOrUpdate(supplierOrder);
        return supplierOrder;
    }

    @Override
    public SupplierOrder getSupplierOrderByOrderNumberOrReference(String orderNumber, String supplierOrderReference) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(SupplierOrder.class)
                .add(Restrictions.eq("voided", false));
        org.hibernate.criterion.Disjunction lookup = Restrictions.disjunction();
        boolean hasLookup = false;
        if (orderNumber != null && !orderNumber.trim().isEmpty()) {
            lookup.add(Restrictions.eq("orderNumber", orderNumber.trim()));
            hasLookup = true;
        }
        if (supplierOrderReference != null && !supplierOrderReference.trim().isEmpty()) {
            lookup.add(Restrictions.eq("supplierOrderReference", supplierOrderReference.trim()));
            hasLookup = true;
        }
        if (!hasLookup) {
            return null;
        }
        return (SupplierOrder) criteria.add(lookup)
                .addOrder(Order.desc("supplierOrderId"))
                .setMaxResults(1)
                .uniqueResult();
    }
}
