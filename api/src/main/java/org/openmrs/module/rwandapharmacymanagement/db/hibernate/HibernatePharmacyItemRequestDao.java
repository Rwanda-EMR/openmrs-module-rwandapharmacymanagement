package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.db.PharmacyItemRequestDao;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.Collections;
import java.util.Date;
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
    public List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, int offset, int limit) {
        return getPendingRequestsForPharmacy(pharmacyLocation, null, offset, limit);
    }

    @Override
    public List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, String search,
                                                                   int offset, int limit) {
        if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null || limit <= 0) {
            return Collections.emptyList();
        }
        Criteria criteria = pendingRequestsCriteria(pharmacyLocation, search);
        criteria.addOrder(Order.asc("dateCreated"))
                .addOrder(Order.asc("pharmacyItemRequestId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit);
        if (hasSearch(search)) {
            criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        }
        return criteria.list();
    }

    @Override
    public long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation) {
        return countPendingRequestsForPharmacy(pharmacyLocation, null);
    }

    @Override
    public long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, String search) {
        if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null) {
            return 0L;
        }
        Object result = pendingRequestsCriteria(pharmacyLocation, search)
                .setProjection(hasSearch(search)
                        ? Projections.countDistinct("pharmacyItemRequestId")
                        : Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    private Criteria pendingRequestsCriteria(PharmacyLocation pharmacyLocation, String search) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PharmacyItemRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("requestedPharmacyLocation", pharmacyLocation))
                .add(Restrictions.eq("requestStatus", "REQUESTED"));
        if (hasSearch(search)) {
            String searchTerm = search.trim();
            criteria.createAlias("creator", "creator", CriteriaSpecification.LEFT_JOIN);
            criteria.createAlias("creator.person", "person", CriteriaSpecification.LEFT_JOIN);
            criteria.createAlias("person.names", "personName", CriteriaSpecification.LEFT_JOIN);
            criteria.createAlias("lines", "line", CriteriaSpecification.LEFT_JOIN);
            criteria.createAlias("line.item", "item", CriteriaSpecification.LEFT_JOIN);
            Junction matches = Restrictions.disjunction()
                    .add(Restrictions.ilike("reason", searchTerm, MatchMode.ANYWHERE))
                    .add(Restrictions.ilike("creator.username", searchTerm, MatchMode.ANYWHERE))
                    .add(Restrictions.ilike("creator.systemId", searchTerm, MatchMode.ANYWHERE))
                    .add(Restrictions.ilike("personName.givenName", searchTerm, MatchMode.ANYWHERE))
                    .add(Restrictions.ilike("personName.middleName", searchTerm, MatchMode.ANYWHERE))
                    .add(Restrictions.ilike("personName.familyName", searchTerm, MatchMode.ANYWHERE))
                    .add(Restrictions.ilike("item.name", searchTerm, MatchMode.ANYWHERE))
                    .add(Restrictions.ilike("item.code", searchTerm, MatchMode.ANYWHERE));
            criteria.add(matches);
        }
        return criteria;
    }

    private boolean hasSearch(String search) {
        return search != null && !search.trim().isEmpty();
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
    public List<PharmacyItemRequest> getRequestsForPharmacy(PharmacyLocation pharmacyLocation, Date fromInclusive,
                                                            Date toExclusive, String status, String itemName,
                                                            int maxResults) {
        if (pharmacyLocation == null || pharmacyLocation.getPharmacyLocationId() == null || maxResults <= 0) {
            return Collections.emptyList();
        }
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PharmacyItemRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("requestedPharmacyLocation", pharmacyLocation));
        if (fromInclusive != null) {
            criteria.add(Restrictions.ge("dateCreated", fromInclusive));
        }
        if (toExclusive != null) {
            criteria.add(Restrictions.lt("dateCreated", toExclusive));
        }
        if (status != null && !status.trim().isEmpty()) {
            criteria.add(Restrictions.eq("requestStatus", status.trim().toUpperCase()));
        }
        if (itemName != null && !itemName.trim().isEmpty()) {
            criteria.createAlias("lines", "line");
            criteria.createAlias("line.item", "item");
            criteria.add(Restrictions.ilike("item.name", itemName.trim(), MatchMode.ANYWHERE));
            criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        }
        return criteria.addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("pharmacyItemRequestId"))
                .setMaxResults(maxResults)
                .list();
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
