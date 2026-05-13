package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.rwandapharmacymanagement.db.ClinicalSafetyDao;
import org.openmrs.module.rwandapharmacymanagement.model.ClinicalSafetyRule;

import java.util.List;

public class HibernateClinicalSafetyDao implements ClinicalSafetyDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<ClinicalSafetyRule> getAllRules() {
        return sessionFactory.getCurrentSession().createCriteria(ClinicalSafetyRule.class)
                .add(Restrictions.eq("voided", false))
                .addOrder(Order.asc("ruleName"))
                .list();
    }

    @Override
    public ClinicalSafetyRule saveRule(ClinicalSafetyRule rule) {
        sessionFactory.getCurrentSession().saveOrUpdate(rule);
        return rule;
    }
}
