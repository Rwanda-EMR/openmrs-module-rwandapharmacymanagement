package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.db.DispensingDao;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

import java.util.List;

public class HibernateDispensingDao implements DispensingDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<PatientDispensingRequest> getPendingRequests() {
        return pendingRequestCriteria(null)
                .addOrder(Order.asc("dateCreated"))
                .list();
    }

    @Override
    public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId) {
        return pendingRequestCriteria(pharmacyLocationId)
                .addOrder(Order.asc("dateCreated"))
                .list();
    }

    @Override
    public long countPendingRequestsForPharmacy(Integer pharmacyLocationId) {
        Number count = (Number) pendingRequestCriteria(pharmacyLocationId)
                .setProjection(Projections.countDistinct("patientDispensingRequestId"))
                .uniqueResult();
        return count == null ? 0L : count.longValue();
    }

    private org.hibernate.Criteria pendingRequestCriteria(Integer pharmacyLocationId) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PatientDispensingRequest.class);
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.in("requestStatus", new Object[] {
                org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus.PENDING_PAYMENT,
                org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus.PENDING_DISPENSE,
                org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus.IN_PROGRESS
        }));
        if (pharmacyLocationId != null) {
            criteria.createAlias("targetPharmacyLocation", "targetPharmacyLocation", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN);
            criteria.createAlias("lines", "line", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN);
            criteria.createAlias("line.routedPharmacyLocation", "routedPharmacyLocation", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN);
            Disjunction pharmacyMatch = Restrictions.disjunction();
            pharmacyMatch.add(Restrictions.eq("targetPharmacyLocation.pharmacyLocationId", pharmacyLocationId));
            pharmacyMatch.add(Restrictions.eq("routedPharmacyLocation.pharmacyLocationId", pharmacyLocationId));
            criteria.add(pharmacyMatch);
            criteria.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
        }
        return criteria;
    }

    @Override
    public List<PatientDispensingRequest> getPendingPaidRequests() {
        return sessionFactory.getCurrentSession().createCriteria(PatientDispensingRequest.class)
                .add(Restrictions.eq("paymentConfirmed", true))
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.in("requestStatus", new Object[] {
                        org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus.PENDING_DISPENSE,
                        org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus.IN_PROGRESS
                }))
                .addOrder(Order.asc("dateCreated"))
                .list();
    }

    @Override
    public List<PatientDispensingRequest> getPatientPrescriptionHistory(Integer patientId, int firstResult, int maxResults) {
        if (patientId == null) {
            return java.util.Collections.emptyList();
        }
        org.openmrs.Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            return java.util.Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(PatientDispensingRequest.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.eq("patient", patient))
                .add(prescriptionHistoryRestriction())
                .addOrder(Order.desc("patientDispensingRequestId"))
                .setFirstResult(Math.max(0, firstResult))
                .setMaxResults(Math.max(1, maxResults))
                .list();
    }

    @Override
    public long countPatientPrescriptionHistory(Integer patientId) {
        if (patientId == null) {
            return 0L;
        }
        org.openmrs.Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            return 0L;
        }
        Object count = sessionFactory.getCurrentSession()
                .createQuery("select count(r.patientDispensingRequestId) from PatientDispensingRequest r where r.voided = false and r.patient = :patient and (r.requestType is null or r.requestType = :requestType)")
                .setParameter("patient", patient)
                .setParameter("requestType", RequestType.PRESCRIPTION)
                .uniqueResult();
        return count instanceof Number ? ((Number) count).longValue() : 0L;
    }

    private Criterion prescriptionHistoryRestriction() {
        return Restrictions.disjunction()
                .add(Restrictions.isNull("requestType"))
                .add(Restrictions.eq("requestType", RequestType.PRESCRIPTION));
    }

    @Override
    public Integer findLatestPatientBillId(Integer patientId) {
        if (patientId == null) {
            return null;
        }
        try {
            String patientColumn = resolveExistingBillingColumn("patient_id", "patient", "beneficiary_patient_id", "beneficiary_id");
            String billIdColumn = resolveExistingBillingColumn("patient_bill_id", "bill_id", "id");
            if (patientColumn == null || billIdColumn == null) {
                return null;
            }
            Object result = sessionFactory.getCurrentSession()
                    .createSQLQuery("select " + billIdColumn + " from moh_bill_patient_bill where " + patientColumn + " = :patientId order by " + billIdColumn + " desc")
                    .setParameter("patientId", patientId)
                    .setMaxResults(1)
                    .uniqueResult();
            return result instanceof Number ? Integer.valueOf(((Number) result).intValue()) : null;
        }
        catch (RuntimeException ignored) {
            return null;
        }
    }

    @Override
    public Boolean isPatientBillPaid(Integer patientBillId) {
        if (patientBillId == null) {
            return null;
        }
        try {
            String paidColumn = resolveExistingBillingColumn("is_paid", "paid");
            String billIdColumn = resolveExistingBillingColumn("patient_bill_id", "bill_id", "id");
            if (paidColumn == null || billIdColumn == null) {
                return null;
            }
            Object result = sessionFactory.getCurrentSession()
                    .createSQLQuery("select " + paidColumn + " from moh_bill_patient_bill where " + billIdColumn + " = :patientBillId")
                    .setParameter("patientBillId", patientBillId)
                    .setMaxResults(1)
                    .uniqueResult();
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            if (result instanceof Number) {
                return ((Number) result).intValue() != 0;
            }
            return result == null ? null : Boolean.valueOf(result.toString());
        }
        catch (RuntimeException ignored) {
            return null;
        }
    }

    private String resolveExistingBillingColumn(String... candidates) {
        for (String candidate : candidates) {
            try {
                Object result = sessionFactory.getCurrentSession()
                        .createSQLQuery("select column_name from information_schema.columns where table_schema = database() and table_name = 'moh_bill_patient_bill' and column_name = :columnName")
                        .setParameter("columnName", candidate)
                        .setMaxResults(1)
                        .uniqueResult();
                if (result != null) {
                    return candidate;
                }
            }
            catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    public List<DispenseRecord> getRecentDispenses() {
        return sessionFactory.getCurrentSession().createCriteria(DispenseRecord.class)
                .add(Restrictions.eq("voided", false))
                .addOrder(Order.desc("dateCreated"))
                .setMaxResults(100)
                .list();
    }

    @Override
    public PatientDispensingRequest getRequest(Integer requestId) {
        return (PatientDispensingRequest) sessionFactory.getCurrentSession().get(PatientDispensingRequest.class, requestId);
    }

    @Override
    public PatientDispensingRequest saveRequest(PatientDispensingRequest patientDispensingRequest) {
        sessionFactory.getCurrentSession().saveOrUpdate(patientDispensingRequest);
        return patientDispensingRequest;
    }

    @Override
    public PatientDispensingRequestLine saveRequestLine(PatientDispensingRequestLine patientDispensingRequestLine) {
        sessionFactory.getCurrentSession().saveOrUpdate(patientDispensingRequestLine);
        return patientDispensingRequestLine;
    }

    @Override
    public DispenseRecord saveDispenseRecord(DispenseRecord dispenseRecord) {
        sessionFactory.getCurrentSession().saveOrUpdate(dispenseRecord);
        return dispenseRecord;
    }

    @Override
    public DispenseRecordLine saveDispenseRecordLine(DispenseRecordLine dispenseRecordLine) {
        sessionFactory.getCurrentSession().saveOrUpdate(dispenseRecordLine);
        return dispenseRecordLine;
    }

    @Override
    public List<DispenseRecord> getDispenseRecordsForRequests(List<Integer> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return sessionFactory.getCurrentSession().createCriteria(DispenseRecord.class)
                .add(Restrictions.eq("voided", false))
                .add(Restrictions.in("patientDispensingRequest.patientDispensingRequestId", requestIds))
                .list();
    }
}
