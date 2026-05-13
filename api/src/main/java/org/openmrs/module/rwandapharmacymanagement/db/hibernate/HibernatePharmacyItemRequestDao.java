package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.db.PharmacyItemRequestDao;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.Collections;
import java.util.List;

public class HibernatePharmacyItemRequestDao implements PharmacyItemRequestDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation) {
        if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null) {
            return Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(PharmacyItemRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("requestedPharmacyLocation", pharmacyLocation))
                .add(Restrictions.eq("requestStatus", "REQUESTED"))
                .addOrder(Order.asc("dateCreated"))
                .addOrder(Order.asc("pharmacyItemRequestId"))
                .list();
    }

    @Override
    public long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation) {
        if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null) {
            return 0L;
        }
        Object result = sessionFactory.getCurrentSession().createCriteria(PharmacyItemRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("requestedPharmacyLocation", pharmacyLocation))
                .add(Restrictions.eq("requestStatus", "REQUESTED"))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public List<PharmacyItemRequest> getRequestsByCreator(User creator, int offset, int limit) {
        if (creator == null || limit <= 0) {
            return Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(PharmacyItemRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("creator", creator))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("pharmacyItemRequestId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit)
                .list();
    }

    @Override
    public long countRequestsByCreator(User creator) {
        if (creator == null) {
            return 0L;
        }
        Object result = sessionFactory.getCurrentSession().createCriteria(PharmacyItemRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("creator", creator))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public PharmacyItemRequest getRequest(Integer requestId) {
        return (PharmacyItemRequest) sessionFactory.getCurrentSession().get(PharmacyItemRequest.class, requestId);
    }

    @Override
    public PharmacyItemRequest saveRequest(PharmacyItemRequest request) {
        sessionFactory.getCurrentSession().saveOrUpdate(request);
        return request;
    }

    @Override
    public PharmacyItemRequestLine saveRequestLine(PharmacyItemRequestLine line) {
        sessionFactory.getCurrentSession().saveOrUpdate(line);
        return line;
    }
}
