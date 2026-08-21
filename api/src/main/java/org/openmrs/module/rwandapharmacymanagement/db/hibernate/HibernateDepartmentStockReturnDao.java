package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.rwandapharmacymanagement.db.DepartmentStockReturnDao;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturnLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.Collections;
import java.util.List;

public class HibernateDepartmentStockReturnDao implements DepartmentStockReturnDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<DepartmentStockReturn> getReturnsForDepartment(PharmacyLocation departmentLocation, int offset, int limit) {
        if (departmentLocation == null || limit <= 0) {
            return Collections.emptyList();
        }
        return returnCriteria()
                .add(Restrictions.eq("departmentLocation", departmentLocation))
                .addOrder(Order.desc("dateCreated"))
                .addOrder(Order.desc("departmentStockReturnId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit)
                .list();
    }

    @Override
    public List<DepartmentStockReturn> getReturnsForDestination(PharmacyLocation destinationPharmacyLocation,
                                                                String status, int offset, int limit) {
        if (destinationPharmacyLocation == null || limit <= 0) {
            return Collections.emptyList();
        }
        Criteria criteria = returnCriteria()
                .add(Restrictions.eq("destinationPharmacyLocation", destinationPharmacyLocation))
                .addOrder(Order.asc("dateCreated"))
                .addOrder(Order.asc("departmentStockReturnId"))
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(limit);
        if (status != null && status.trim().length() > 0) {
            criteria.add(Restrictions.eq("returnStatus", status));
        }
        return criteria.list();
    }

    @Override
    public DepartmentStockReturn getReturn(Integer returnId) {
        return (DepartmentStockReturn) sessionFactory.getCurrentSession().get(DepartmentStockReturn.class, returnId);
    }

    @Override
    public DepartmentStockReturn saveReturn(DepartmentStockReturn stockReturn) {
        sessionFactory.getCurrentSession().saveOrUpdate(stockReturn);
        return stockReturn;
    }

    @Override
    public DepartmentStockReturnLine saveReturnLine(DepartmentStockReturnLine line) {
        sessionFactory.getCurrentSession().saveOrUpdate(line);
        return line;
    }

    private Criteria returnCriteria() {
        return sessionFactory.getCurrentSession().createCriteria(DepartmentStockReturn.class)
                .add(Restrictions.or(Restrictions.eq("voided", false), Restrictions.isNull("voided")));
    }
}
