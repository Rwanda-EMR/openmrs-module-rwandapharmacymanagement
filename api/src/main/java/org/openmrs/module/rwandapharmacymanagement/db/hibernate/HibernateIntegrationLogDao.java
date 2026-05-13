package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.openmrs.module.rwandapharmacymanagement.db.IntegrationLogDao;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;

import java.util.List;

public class HibernateIntegrationLogDao implements IntegrationLogDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<IntegrationLog> getRecentLogs() {
        return sessionFactory.getCurrentSession().createCriteria(IntegrationLog.class)
                .addOrder(Order.desc("dateCreated"))
                .setMaxResults(50)
                .list();
    }

    @Override
    public IntegrationLog saveLog(IntegrationLog log) {
        sessionFactory.getCurrentSession().saveOrUpdate(log);
        return log;
    }
}
