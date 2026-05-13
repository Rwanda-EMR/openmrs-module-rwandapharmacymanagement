package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.db.LabSupplyDao;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequest;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.List;

public class HibernateLabSupplyDao implements LabSupplyDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<LabSupplyRequest> getPendingRequests() {
        return sessionFactory.getCurrentSession().createCriteria(LabSupplyRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("requestStatus", "REQUESTED"))
                .addOrder(Order.asc("dateCreated"))
                .addOrder(Order.asc("labSupplyRequestId"))
                .list();
    }

    @Override
    public long countPendingRequests() {
        Object result = sessionFactory.getCurrentSession().createCriteria(LabSupplyRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("requestStatus", "REQUESTED"))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public List<LabSupplyRequest> getRequestsByCreator(User creator) {
        if (creator == null) {
            return java.util.Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(LabSupplyRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("creator", creator))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("labSupplyRequestId"))
                .list();
    }

    @Override
    public List<LabSupplyRequest> getRequestsByCreator(User creator, int offset, int limit) {
        if (creator == null || limit <= 0) {
            return java.util.Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(LabSupplyRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("creator", creator))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("labSupplyRequestId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit)
                .list();
    }

    @Override
    public long countRequestsByCreator(User creator) {
        if (creator == null) {
            return 0L;
        }
        Object result = sessionFactory.getCurrentSession().createCriteria(LabSupplyRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("creator", creator))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public List<LabSupplyRequest> getApprovedRequestsByPharmacy(PharmacyLocation pharmacyLocation) {
        if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null) {
            return java.util.Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(LabSupplyRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("dispensingPharmacyLocation", pharmacyLocation))
                .add(Restrictions.eq("requestStatus", "APPROVED"))
                .addOrder(Order.desc("approvedDate"))
                .addOrder(Order.desc("labSupplyRequestId"))
                .list();
    }

    @Override
    public LabSupplyRequest getRequest(Integer requestId) {
        return (LabSupplyRequest) sessionFactory.getCurrentSession().get(LabSupplyRequest.class, requestId);
    }

    @Override
    public LabSupplyRequest saveRequest(LabSupplyRequest request) {
        sessionFactory.getCurrentSession().saveOrUpdate(request);
        return request;
    }

    @Override
    public LabSupplyRequestLine saveRequestLine(LabSupplyRequestLine line) {
        sessionFactory.getCurrentSession().saveOrUpdate(line);
        return line;
    }
}
