package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.rwandapharmacymanagement.db.IntegrationLogDao;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;

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
    public List<IntegrationLog> getPendingHieLogs(int maxResults) {
        Criterion hieTypes = Restrictions.disjunction()
                .add(Restrictions.eq("integrationType", IntegrationType.HIE_ENCOUNTER))
                .add(Restrictions.eq("integrationType", IntegrationType.HIE_MEDICATION_REQUEST))
                .add(Restrictions.eq("integrationType", IntegrationType.HIE_MEDICATION_DISPENSE))
                .add(Restrictions.eq("integrationType", IntegrationType.RHIP_INPATIENT_PRESCRIPTION))
                .add(Restrictions.eq("integrationType", IntegrationType.RHIP_VISIT_DISPENSE));
        return sessionFactory.getCurrentSession().createCriteria(IntegrationLog.class)
                .add(Restrictions.eq("status", IntegrationStatus.RETRY_PENDING))
                .add(hieTypes)
                .addOrder(Order.asc("dateCreated"))
                .addOrder(Order.asc("integrationLogId"))
                .setMaxResults(Math.max(maxResults, 1))
                .list();
    }

    @Override
    public List<IntegrationLog> getPendingSmsLogs(int maxResults) {
        return sessionFactory.getCurrentSession().createCriteria(IntegrationLog.class)
                .add(Restrictions.eq("status", IntegrationStatus.RETRY_PENDING))
                .add(Restrictions.eq("integrationType", IntegrationType.SMS_NOTIFICATION))
                .addOrder(Order.asc("dateCreated"))
                .addOrder(Order.asc("integrationLogId"))
                .setMaxResults(Math.max(maxResults, 1))
                .list();
    }

    @Override
    public IntegrationLog getLog(Integer integrationLogId) {
        return integrationLogId == null ? null
                : (IntegrationLog) sessionFactory.getCurrentSession().get(IntegrationLog.class, integrationLogId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IntegrationLog> getLogs(IntegrationType integrationType, java.util.Date fromInclusive) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(IntegrationLog.class)
                .add(Restrictions.eq("integrationType", integrationType));
        if (fromInclusive != null) {
            criteria.add(Restrictions.ge("dateCreated", fromInclusive));
        }
        return criteria.addOrder(Order.desc("dateCreated")).addOrder(Order.desc("integrationLogId")).list();
    }

    @Override
    public IntegrationLog getPendingLog(IntegrationType integrationType, String requestPayload) {
        if (integrationType == null || requestPayload == null) {
            return null;
        }
        return (IntegrationLog) sessionFactory.getCurrentSession().createCriteria(IntegrationLog.class)
                .add(Restrictions.eq("integrationType", integrationType))
                .add(Restrictions.eq("status", IntegrationStatus.RETRY_PENDING))
                .add(Restrictions.eq("requestPayload", requestPayload))
                .addOrder(Order.asc("dateCreated"))
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public IntegrationLog saveLog(IntegrationLog log) {
        sessionFactory.getCurrentSession().saveOrUpdate(log);
        return log;
    }
}
