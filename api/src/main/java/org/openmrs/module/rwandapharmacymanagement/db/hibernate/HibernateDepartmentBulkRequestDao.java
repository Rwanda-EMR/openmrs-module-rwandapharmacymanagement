package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.db.DepartmentBulkRequestDao;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockIssue;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.Collections;
import java.util.List;

public class HibernateDepartmentBulkRequestDao implements DepartmentBulkRequestDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<DepartmentBulkRequest> getRequestsByStatus(String status, int offset, int limit) {
        if (status == null || limit <= 0) {
            return Collections.emptyList();
        }
        Criteria criteria = requestCriteria()
                .add(Restrictions.eq("requestStatus", status))
                .addOrder(Order.asc("dateCreated"))
                .addOrder(Order.asc("departmentBulkRequestId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit);
        return criteria.list();
    }

    @Override
    public long countRequestsByStatus(String status) {
        if (status == null) {
            return 0L;
        }
        Object result = requestCriteria()
                .add(Restrictions.eq("requestStatus", status))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public long countRequestsByStatusForSupplyingPharmacy(String status, PharmacyLocation supplyingPharmacyLocation) {
        if (status == null || supplyingPharmacyLocation == null || supplyingPharmacyLocation.getPharmacyLocationId() == null) {
            return 0L;
        }
        Object result = requestCriteria()
                .add(Restrictions.eq("requestStatus", status))
                .createAlias("supplyingPharmacyLocation", "supplyingPharmacyLocation")
                .add(Restrictions.eq("supplyingPharmacyLocation.pharmacyLocationId",
                        supplyingPharmacyLocation.getPharmacyLocationId()))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public List<DepartmentBulkRequest> getRequestsByCreator(User creator, int offset, int limit) {
        if (creator == null || limit <= 0) {
            return Collections.emptyList();
        }
        return requestCriteria()
                .add(Restrictions.eq("creator", creator))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("departmentBulkRequestId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit)
                .list();
    }

    @Override
    public long countRequestsByCreator(User creator) {
        if (creator == null) {
            return 0L;
        }
        Object result = requestCriteria()
                .add(Restrictions.eq("creator", creator))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public List<DepartmentBulkRequest> getRequestsForDepartment(PharmacyLocation departmentLocation, int offset, int limit) {
        if (departmentLocation == null || limit <= 0) {
            return Collections.emptyList();
        }
        return requestCriteria()
                .add(Restrictions.eq("departmentLocation", departmentLocation))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("departmentBulkRequestId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit)
                .list();
    }

    @Override
    public long countRequestsForDepartment(PharmacyLocation departmentLocation) {
        if (departmentLocation == null) {
            return 0L;
        }
        Object result = requestCriteria()
                .add(Restrictions.eq("departmentLocation", departmentLocation))
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public List<DepartmentStockIssue> getIssuesForDepartment(PharmacyLocation departmentLocation, int offset, int limit) {
        if (departmentLocation == null || limit <= 0) {
            return Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(DepartmentStockIssue.class)
                .add(Restrictions.eq("departmentLocation", departmentLocation))
                .add(Restrictions.or(Restrictions.eq("voided", false), Restrictions.isNull("voided")))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("departmentStockIssueId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit)
                .list();
    }

    @Override
    public List<DepartmentBulkRequest> getFinanceHistory(int offset, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return requestCriteria()
                .add(Restrictions.ne("requestStatus", "SUBMITTED"))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("departmentBulkRequestId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit)
                .list();
    }

    @Override
    public DepartmentBulkRequest getRequest(Integer requestId) {
        return (DepartmentBulkRequest) sessionFactory.getCurrentSession().get(DepartmentBulkRequest.class, requestId);
    }

    @Override
    public DepartmentBulkRequest saveRequest(DepartmentBulkRequest request) {
        sessionFactory.getCurrentSession().saveOrUpdate(request);
        return request;
    }

    @Override
    public DepartmentBulkRequestLine saveRequestLine(DepartmentBulkRequestLine line) {
        sessionFactory.getCurrentSession().saveOrUpdate(line);
        return line;
    }

    @Override
    public DepartmentStockIssue saveIssue(DepartmentStockIssue issue) {
        sessionFactory.getCurrentSession().saveOrUpdate(issue);
        return issue;
    }

    private Criteria requestCriteria() {
        return sessionFactory.getCurrentSession().createCriteria(DepartmentBulkRequest.class)
                .add(Restrictions.or(Restrictions.eq("voided", false), Restrictions.isNull("voided")));
    }
}
