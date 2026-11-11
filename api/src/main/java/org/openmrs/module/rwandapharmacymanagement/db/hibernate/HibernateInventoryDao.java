package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustment;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustmentLine;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HibernateInventoryDao implements InventoryDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<InventoryBalance> getInventoryForPharmacy(Integer pharmacyLocationId) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(InventoryBalance.class);
        criteria.add(notVoided());
        if (pharmacyLocationId != null) {
            criteria.createAlias("pharmacyLocation", "pharmacyLocation");
            criteria.add(Restrictions.eq("pharmacyLocation.pharmacyLocationId", pharmacyLocationId));
        }
        criteria.addOrder(Order.asc("expiryDate"));
        return criteria.list();
    }

    @Override
    public List<InventoryBalance> getAvailableInventoryForPharmacy(Integer pharmacyLocationId) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(InventoryBalance.class);
        criteria.add(notVoided());
        criteria.add(Restrictions.gt("quantityOnHand", BigDecimal.ZERO));
        criteria.add(Restrictions.or(Restrictions.isNull("expiryDate"), Restrictions.ge("expiryDate", startOfToday())));
        if (pharmacyLocationId != null) {
            criteria.createAlias("pharmacyLocation", "pharmacyLocation");
            criteria.add(Restrictions.eq("pharmacyLocation.pharmacyLocationId", pharmacyLocationId));
        }
        criteria.addOrder(Order.asc("expiryDate"));
        return criteria.list();
    }

    @Override
    public java.util.Map<Integer, BigDecimal> getAvailableQuantitiesByItem(java.util.Collection<Integer> itemIds) {
        java.util.Map<Integer, BigDecimal> quantities = new java.util.HashMap<Integer, BigDecimal>();
        if (itemIds != null && itemIds.isEmpty()) {
            return quantities;
        }
        StringBuilder sql = new StringBuilder()
                .append("select ib.item_id, sum(ib.quantity_on_hand) from rpm_inventory_balance ib ")
                .append("join rpm_pharmacy_location pl on pl.pharmacy_location_id = ib.pharmacy_location_id ")
                .append("where ib.voided = 0 and pl.voided = 0 and coalesce(pl.active, 1) = 1 ")
                .append("and ib.quantity_on_hand > 0 and (ib.expiry_date is null or ib.expiry_date >= :today) ");
        if (itemIds != null) {
            sql.append("and ib.item_id in (:itemIds) ");
        }
        sql.append("group by ib.item_id");
        org.hibernate.SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery(sql.toString());
        query.setParameter("today", startOfToday());
        if (itemIds != null) {
            query.setParameterList("itemIds", itemIds);
        }
        for (Object row : query.list()) {
            Object[] values = (Object[]) row;
            if (values[0] != null && values[1] != null) {
                quantities.put(((Number) values[0]).intValue(), new BigDecimal(values[1].toString()));
            }
        }
        return quantities;
    }

    @Override
    public boolean hasStockInActivePharmacy(Integer itemId) {
        if (itemId == null) {
            return false;
        }
        Object match = sessionFactory.getCurrentSession()
                .createSQLQuery("select ib.inventory_balance_id from rpm_inventory_balance ib "
                        + "join rpm_pharmacy_location pl on pl.pharmacy_location_id = ib.pharmacy_location_id "
                        + "where ib.item_id = :itemId and ib.voided = 0 and ib.quantity_on_hand > 0 "
                        + "and coalesce(pl.active, 1) = 1")
                .setParameter("itemId", itemId)
                .setMaxResults(1)
                .uniqueResult();
        return match != null;
    }

    @Override
    public InventoryBalance getInventoryBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(InventoryBalance.class)
                .createAlias("item", "item")
                .createAlias("pharmacyLocation", "pharmacyLocation")
                .add(Restrictions.eq("item.itemId", itemId))
                .add(Restrictions.eq("pharmacyLocation.pharmacyLocationId", pharmacyLocationId))
                .add(notVoided());
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            criteria.add(Restrictions.or(Restrictions.isNull("batchNumber"), Restrictions.eq("batchNumber", "")));
        } else {
            criteria.add(Restrictions.eq("batchNumber", batchNumber));
        }
        criteria.addOrder(Order.asc("inventoryBalanceId"));
        List<InventoryBalance> balances = criteria.list();
        if (balances == null || balances.isEmpty()) {
            return null;
        }
        InventoryBalance primary = balances.get(0);
        if (balances.size() == 1) {
            return primary;
        }
        BigDecimal totalQuantity = primary.getQuantityOnHand() == null ? BigDecimal.ZERO : primary.getQuantityOnHand();
        for (int i = 1; i < balances.size(); i++) {
            InventoryBalance duplicate = balances.get(i);
            if (duplicate == null) {
                continue;
            }
            totalQuantity = totalQuantity.add(duplicate.getQuantityOnHand() == null ? BigDecimal.ZERO : duplicate.getQuantityOnHand());
            if (primary.getExpiryDate() == null && duplicate.getExpiryDate() != null) {
                primary.setExpiryDate(duplicate.getExpiryDate());
            }
            if (primary.getUnitPrice() == null && duplicate.getUnitPrice() != null) {
                primary.setUnitPrice(duplicate.getUnitPrice());
            }
            duplicate.setQuantityOnHand(BigDecimal.ZERO);
            duplicate.setVoided(Boolean.TRUE);
            duplicate.setVoidedBy(Context.getAuthenticatedUser());
            duplicate.setDateVoided(new Date());
            duplicate.setVoidReason("Merged duplicate inventory balance");
            sessionFactory.getCurrentSession().saveOrUpdate(duplicate);
        }
        primary.setQuantityOnHand(totalQuantity);
        sessionFactory.getCurrentSession().saveOrUpdate(primary);
        return primary;
    }

    @Override
    public InventoryBalance getInventoryBalance(Integer inventoryBalanceId) {
        if (inventoryBalanceId == null) {
            return null;
        }
        InventoryBalance balance = (InventoryBalance) sessionFactory.getCurrentSession()
                .get(InventoryBalance.class, inventoryBalanceId);
        return balance == null || Boolean.TRUE.equals(balance.getVoided()) ? null : balance;
    }

    @Override
    public List<DeliveryReceiptLine> getRecentDeliveryReceiptLines() {
        return sessionFactory.getCurrentSession().createCriteria(DeliveryReceiptLine.class)
                .add(notVoided())
                .addOrder(Order.desc("dateCreated"))
                .setMaxResults(50)
                .list();
    }

    @Override
    public List<DeliveryReceipt> getDeliveryReceipts(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId,
                                                     Integer purchaseOrderId) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(DeliveryReceipt.class);
        criteria.add(notVoided());
        if (fromInclusive != null) {
            criteria.add(Restrictions.ge("dateCreated", fromInclusive));
        }
        if (toExclusive != null) {
            criteria.add(Restrictions.lt("dateCreated", toExclusive));
        }
        if (pharmacyLocationId != null) {
            criteria.createAlias("receivedIntoLocation", "receivedIntoLocation");
            criteria.add(Restrictions.eq("receivedIntoLocation.pharmacyLocationId", pharmacyLocationId));
        }
        if (purchaseOrderId != null) {
            criteria.createAlias("purchaseOrder", "purchaseOrder");
            criteria.add(Restrictions.eq("purchaseOrder.purchaseOrderId", purchaseOrderId));
        }
        criteria.addOrder(Order.desc("dateCreated"));
        criteria.addOrder(Order.desc("deliveryReceiptId"));
        return criteria.list();
    }

    @Override
    public List<StockTransaction> getRecentStockTransactions() {
        return sessionFactory.getCurrentSession().createCriteria(StockTransaction.class)
                .add(notVoided())
                .addOrder(Order.desc("dateCreated"))
                .setMaxResults(100)
                .list();
    }

    @Override
    public StockTransaction getStockTransaction(Integer stockTransactionId) {
        if (stockTransactionId == null) {
            return null;
        }
        StockTransaction transaction = (StockTransaction) sessionFactory.getCurrentSession()
                .get(StockTransaction.class, stockTransactionId);
        return transaction == null || Boolean.TRUE.equals(transaction.getVoided()) ? null : transaction;
    }

    @Override
    public List<StockTransaction> getStockTransactions(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
        return getStockTransactions(null, fromInclusive, toExclusive, pharmacyLocationId);
    }

    @Override
    public List<StockTransaction> getStockTransactions(Integer itemId, Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
        return getStockTransactions(itemId, fromInclusive, toExclusive, pharmacyLocationId, null);
    }

    @Override
    public List<StockTransaction> getStockTransactions(Integer itemId, Date fromInclusive, Date toExclusive,
                                                       Integer pharmacyLocationId, Integer maxResults) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(StockTransaction.class);
        criteria.add(notVoided());
        if (itemId != null) {
            criteria.createAlias("item", "item");
            criteria.add(Restrictions.eq("item.itemId", itemId));
        }
        if (fromInclusive != null) {
            criteria.add(Restrictions.ge("dateCreated", fromInclusive));
        }
        if (toExclusive != null) {
            criteria.add(Restrictions.lt("dateCreated", toExclusive));
        }
        if (pharmacyLocationId != null) {
            criteria.createAlias("pharmacyLocation", "pharmacyLocation");
            criteria.add(Restrictions.eq("pharmacyLocation.pharmacyLocationId", pharmacyLocationId));
        }
        criteria.addOrder(Order.asc("dateCreated"));
        criteria.addOrder(Order.asc("stockTransactionId"));
        if (maxResults != null && maxResults > 0) {
            criteria.setMaxResults(maxResults);
        }
        return criteria.list();
    }

    @Override
    public List<InterPharmacyTransfer> getTransfers() {
        return sessionFactory.getCurrentSession().createCriteria(InterPharmacyTransfer.class)
                .add(notVoided())
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("interPharmacyTransferId"))
                .list();
    }

    @Override
    public List<InterPharmacyTransfer> getTransfers(String transferStatus, Integer sourcePharmacyLocationId,
                                                    Integer destinationPharmacyLocationId, Integer maxResults) {
        org.hibernate.Criteria criteria = transferCriteria(transferStatus, sourcePharmacyLocationId, destinationPharmacyLocationId);
        criteria.addOrder(Order.desc("dateCreated"));
        criteria.addOrder(Order.desc("interPharmacyTransferId"));
        if (maxResults != null && maxResults > 0) {
            criteria.setMaxResults(maxResults);
        }
        return criteria.list();
    }

    @Override
    public long countTransfers(String transferStatus, Integer sourcePharmacyLocationId, Integer destinationPharmacyLocationId) {
        Number count = (Number) transferCriteria(transferStatus, sourcePharmacyLocationId, destinationPharmacyLocationId)
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return count == null ? 0L : count.longValue();
    }

    private org.hibernate.Criteria transferCriteria(String transferStatus, Integer sourcePharmacyLocationId,
                                                    Integer destinationPharmacyLocationId) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(InterPharmacyTransfer.class);
        criteria.add(notVoided());
        if (transferStatus != null && transferStatus.trim().length() > 0) {
            criteria.add(Restrictions.eq("transferStatus", transferStatus.trim()));
        }
        if (sourcePharmacyLocationId != null) {
            criteria.createAlias("sourcePharmacyLocation", "sourcePharmacyLocation");
            criteria.add(Restrictions.eq("sourcePharmacyLocation.pharmacyLocationId", sourcePharmacyLocationId));
        }
        if (destinationPharmacyLocationId != null) {
            criteria.createAlias("destinationPharmacyLocation", "destinationPharmacyLocation");
            criteria.add(Restrictions.eq("destinationPharmacyLocation.pharmacyLocationId", destinationPharmacyLocationId));
        }
        return criteria;
    }

    @Override
    public InterPharmacyTransfer getTransfer(Integer transferId) {
        return (InterPharmacyTransfer) sessionFactory.getCurrentSession().get(InterPharmacyTransfer.class, transferId);
    }

    @Override
    public InterPharmacyTransferLine getLatestTransferLineBefore(Integer destinationPharmacyLocationId,
                                                                 Integer itemId,
                                                                 Date beforeDate,
                                                                 Integer excludeTransferId) {
        if (destinationPharmacyLocationId == null || itemId == null || beforeDate == null) {
            return null;
        }
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(InterPharmacyTransferLine.class);
        criteria.add(notVoided());
        criteria.createAlias("item", "item");
        criteria.createAlias("interPharmacyTransfer", "transfer");
        criteria.createAlias("transfer.destinationPharmacyLocation", "destinationPharmacyLocation");
        criteria.add(Restrictions.eq("item.itemId", itemId));
        criteria.add(Restrictions.eq("destinationPharmacyLocation.pharmacyLocationId", destinationPharmacyLocationId));
        criteria.add(Restrictions.lt("transfer.dateCreated", beforeDate));
        criteria.add(Restrictions.or(Restrictions.eq("transfer.voided", false), Restrictions.isNull("transfer.voided")));
        criteria.add(Restrictions.ne("transfer.transferStatus", "REJECTED"));
        if (excludeTransferId != null) {
            criteria.add(Restrictions.ne("transfer.interPharmacyTransferId", excludeTransferId));
        }
        criteria.addOrder(Order.desc("transfer.dateCreated"));
        criteria.addOrder(Order.desc("interPharmacyTransferLineId"));
        criteria.setMaxResults(1);
        List<InterPharmacyTransferLine> lines = criteria.list();
        return lines == null || lines.isEmpty() ? null : lines.get(0);
    }

    @Override
    public DeliveryReceipt getDeliveryReceipt(Integer deliveryReceiptId) {
        return (DeliveryReceipt) sessionFactory.getCurrentSession().get(DeliveryReceipt.class, deliveryReceiptId);
    }

    @Override
    public InventoryBalance saveInventoryBalance(InventoryBalance inventoryBalance) {
        sessionFactory.getCurrentSession().saveOrUpdate(inventoryBalance);
        return inventoryBalance;
    }

    @Override
    public DeliveryReceipt saveDeliveryReceipt(DeliveryReceipt deliveryReceipt) {
        sessionFactory.getCurrentSession().saveOrUpdate(deliveryReceipt);
        return deliveryReceipt;
    }

    @Override
    public DeliveryReceiptLine saveDeliveryReceiptLine(DeliveryReceiptLine deliveryReceiptLine) {
        sessionFactory.getCurrentSession().saveOrUpdate(deliveryReceiptLine);
        return deliveryReceiptLine;
    }

    @Override
    public InterPharmacyTransfer saveTransfer(InterPharmacyTransfer transfer) {
        sessionFactory.getCurrentSession().saveOrUpdate(transfer);
        return transfer;
    }

    @Override
    public InterPharmacyTransferLine saveTransferLine(InterPharmacyTransferLine transferLine) {
        sessionFactory.getCurrentSession().saveOrUpdate(transferLine);
        return transferLine;
    }

    @Override
    public StockAdjustment saveAdjustment(StockAdjustment adjustment) {
        sessionFactory.getCurrentSession().saveOrUpdate(adjustment);
        return adjustment;
    }

    @Override
    public StockAdjustmentLine saveAdjustmentLine(StockAdjustmentLine adjustmentLine) {
        sessionFactory.getCurrentSession().saveOrUpdate(adjustmentLine);
        return adjustmentLine;
    }

    @Override
    public StockTransaction saveStockTransaction(StockTransaction stockTransaction) {
        sessionFactory.getCurrentSession().saveOrUpdate(stockTransaction);
        return stockTransaction;
    }

    private org.hibernate.criterion.Criterion notVoided() {
        return Restrictions.or(Restrictions.eq("voided", false), Restrictions.isNull("voided"));
    }

    private Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
