package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustment;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustmentLine;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;

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
    public InventoryBalance getInventoryBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(InventoryBalance.class)
                .createAlias("item", "item")
                .createAlias("pharmacyLocation", "pharmacyLocation")
                .add(Restrictions.eq("item.itemId", itemId))
                .add(Restrictions.eq("pharmacyLocation.pharmacyLocationId", pharmacyLocationId))
                .add(notVoided());
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            criteria.add(Restrictions.isNull("batchNumber"));
        } else {
            criteria.add(Restrictions.eq("batchNumber", batchNumber));
        }
        return (InventoryBalance) criteria.uniqueResult();
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
    public List<StockTransaction> getRecentStockTransactions() {
        return sessionFactory.getCurrentSession().createCriteria(StockTransaction.class)
                .add(notVoided())
                .addOrder(Order.desc("dateCreated"))
                .setMaxResults(100)
                .list();
    }

    @Override
    public List<StockTransaction> getStockTransactions(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
        return getStockTransactions(null, fromInclusive, toExclusive, pharmacyLocationId);
    }

    @Override
    public List<StockTransaction> getStockTransactions(Integer itemId, Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
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
}
