package org.openmrs.module.rwandapharmacymanagement.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.db.DispensingDao;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId, String search, int firstResult, int maxResults) {
        List<Integer> requestIds = pendingRequestIdsForPharmacy(pharmacyLocationId, search, firstResult, maxResults,
                null, null, null);
        return getRequestsByIdsInOrder(requestIds);
    }

    @Override
    public List<PatientDispensingRequest> getPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                                                        String search,
                                                                        int firstResult,
                                                                        int maxResults,
                                                                        String queueTab,
                                                                        String requestType,
                                                                        String dateFilter) {
        List<Integer> requestIds = pendingRequestIdsForPharmacy(pharmacyLocationId, search, firstResult, maxResults,
                queueTab, requestType, dateFilter);
        return getRequestsByIdsInOrder(requestIds);
    }

    private List<PatientDispensingRequest> getRequestsByIdsInOrder(List<Integer> requestIds) {
        if (requestIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<PatientDispensingRequest> requests = sessionFactory.getCurrentSession()
                .createCriteria(PatientDispensingRequest.class)
                .add(Restrictions.in("patientDispensingRequestId", requestIds))
                .setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY)
                .list();
        Map<Integer, PatientDispensingRequest> requestsById = new HashMap<Integer, PatientDispensingRequest>();
        for (PatientDispensingRequest request : requests) {
            if (request != null && request.getPatientDispensingRequestId() != null) {
                requestsById.put(request.getPatientDispensingRequestId(), request);
            }
        }
        List<PatientDispensingRequest> orderedRequests = new ArrayList<PatientDispensingRequest>();
        for (Integer requestId : requestIds) {
            PatientDispensingRequest request = requestsById.get(requestId);
            if (request != null) {
                orderedRequests.add(request);
            }
        }
        return orderedRequests;
    }

    @Override
    public long countPendingRequestsForPharmacy(Integer pharmacyLocationId) {
        return countPendingRequestsForPharmacy(pharmacyLocationId, null);
    }

    @Override
    public long countPendingRequestsForPharmacy(Integer pharmacyLocationId, String search) {
        return countPendingRequestsForPharmacy(pharmacyLocationId, search, null, null, null);
    }

    @Override
    public long countPendingRequestsForPharmacy(Integer pharmacyLocationId,
                                                String search,
                                                String queueTab,
                                                String requestType,
                                                String dateFilter) {
        Number count = (Number) sessionFactory.getCurrentSession()
                .createSQLQuery("select count(distinct r.patient_dispensing_request_id) "
                        + pendingRequestSqlFromWhere(pharmacyLocationId, search, queueTab, requestType, dateFilter))
                .setProperties(pendingRequestSqlParameters(pharmacyLocationId, search, requestType))
                .uniqueResult();
        return count == null ? 0L : count.longValue();
    }

    private List<Integer> pendingRequestIdsForPharmacy(Integer pharmacyLocationId,
                                                       String search,
                                                       int firstResult,
                                                       int maxResults,
                                                       String queueTab,
                                                       String requestType,
                                                       String dateFilter) {
        List results = sessionFactory.getCurrentSession()
                .createSQLQuery("select distinct r.patient_dispensing_request_id, r.date_created "
                        + pendingRequestSqlFromWhere(pharmacyLocationId, search, queueTab, requestType, dateFilter)
                        + " order by r.date_created desc, r.patient_dispensing_request_id desc")
                .setProperties(pendingRequestSqlParameters(pharmacyLocationId, search, requestType))
                .setFirstResult(Math.max(0, firstResult))
                .setMaxResults(Math.max(1, maxResults))
                .list();
        List<Integer> requestIds = new ArrayList<Integer>();
        for (Object result : results) {
            Object idValue = result instanceof Object[] ? ((Object[]) result)[0] : result;
            Integer requestId = integerValue(idValue);
            if (requestId != null && !requestIds.contains(requestId)) {
                requestIds.add(requestId);
            }
        }
        return requestIds;
    }

    private String pendingRequestSqlFromWhere(Integer pharmacyLocationId,
                                             String search,
                                             String queueTab,
                                             String requestType,
                                             String dateFilter) {
        StringBuilder sql = new StringBuilder();
        sql.append("from rpm_patient_dispensing_request r ")
                .append("left join rpm_patient_dispensing_request_line l on l.patient_dispensing_request_id = r.patient_dispensing_request_id ")
                .append("left join patient_identifier pi on pi.patient_id = r.patient_id and coalesce(pi.voided, 0) = 0 ")
                .append("left join person_name pn on pn.person_id = r.patient_id and coalesce(pn.voided, 0) = 0 ")
                .append("where coalesce(r.voided, 0) = 0 ")
                .append("and r.request_status in ('PENDING_PAYMENT', 'PENDING_DISPENSE', 'IN_PROGRESS') ");
        if (pharmacyLocationId != null) {
            sql.append("and (r.target_pharmacy_location_id = :pharmacyLocationId ")
                    .append("or l.routed_pharmacy_location_id = :pharmacyLocationId or r.target_pharmacy_location_id is null) ");
        }
        if (hasText(search)) {
            sql.append("and (lower(coalesce(pi.identifier, '')) like :search ")
                    .append("or lower(concat_ws(' ', coalesce(pn.given_name, ''), coalesce(pn.middle_name, ''), ")
                    .append("coalesce(pn.family_name, ''), coalesce(pn.family_name2, ''))) like :search ")
                    .append("or cast(r.patient_id as char) like :search) ");
        }
        String normalizedTab = normalizeFilter(queueTab);
        if ("ready".equals(normalizedTab)) {
            sql.append("and (coalesce(r.payment_confirmed, 0) = 1 or coalesce(r.payment_exception_approved, 0) = 1) ");
        } else if ("waiting".equals(normalizedTab)) {
            sql.append("and coalesce(r.payment_confirmed, 0) = 0 and coalesce(r.payment_exception_approved, 0) = 0 ");
        } else if ("exception".equals(normalizedTab)) {
            sql.append("and coalesce(r.payment_exception_approved, 0) = 1 ");
        } else if ("older".equals(normalizedTab)) {
            sql.append("and r.date_created < date_sub(now(), interval 3 day) ");
        }
        if (hasText(requestType) && !"all".equals(normalizeFilter(requestType))) {
            sql.append("and r.request_type = :requestType ");
        }
        String normalizedDateFilter = normalizeFilter(dateFilter);
        if ("today".equals(normalizedDateFilter)) {
            sql.append("and r.date_created >= curdate() ");
        } else if ("last24".equals(normalizedDateFilter)) {
            sql.append("and r.date_created >= date_sub(now(), interval 24 hour) ");
        } else if ("last7".equals(normalizedDateFilter)) {
            sql.append("and r.date_created >= date_sub(now(), interval 7 day) ");
        } else if ("older3".equals(normalizedDateFilter)) {
            sql.append("and r.date_created < date_sub(now(), interval 3 day) ");
        }
        return sql.toString();
    }

    private Map<String, Object> pendingRequestSqlParameters(Integer pharmacyLocationId, String search, String requestType) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (pharmacyLocationId != null) {
            parameters.put("pharmacyLocationId", pharmacyLocationId);
        }
        if (hasText(search)) {
            parameters.put("search", "%" + search.trim().toLowerCase() + "%");
        }
        if (hasText(requestType) && !"all".equals(normalizeFilter(requestType))) {
            parameters.put("requestType", requestType.trim().toUpperCase());
        }
        return parameters;
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
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
            pharmacyMatch.add(Restrictions.isNull("targetPharmacyLocation"));
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
        return findLatestPatientBillId(patientId, null);
    }

    @Override
    public Integer findLatestPatientBillId(Integer patientId, Date referenceDate) {
        if (patientId == null) {
            return null;
        }
        if (!tableExists("moh_bill_patient_bill")) {
            return null;
        }
        try {
            String billIdColumn = resolveExistingBillingColumn("patient_bill_id", "bill_id", "id");
            if (billIdColumn == null) {
                return null;
            }
            Integer patientBillId = findLatestPatientBillIdFromPatientBill(patientId, billIdColumn, referenceDate);
            if (patientBillId != null) {
                return patientBillId;
            }
            patientBillId = findLatestPatientBillIdViaBeneficiary(patientId, billIdColumn, referenceDate);
            if (patientBillId != null) {
                return patientBillId;
            }
            return findLatestPatientBillIdViaConsommationPatient(patientId, billIdColumn, referenceDate);
        }
        catch (RuntimeException ignored) {
            return null;
        }
    }

    @Override
    public Integer findLatestPatientBillId(Integer patientId, Date referenceDate, Collection<String> itemNames) {
        Integer patientBillId = findPatientBillIdByBilledItemNames(patientId, referenceDate, itemNames, true);
        if (patientBillId != null) {
            return patientBillId;
        }
        return itemNames == null || itemNames.isEmpty() ? findLatestPatientBillId(patientId, referenceDate) : null;
    }

    private Integer findPatientBillIdByBilledItemNames(Integer patientId, Date referenceDate, Collection<String> itemNames,
                                                       boolean requirePatientMatch) {
        if (referenceDate == null || itemNames == null || itemNames.isEmpty()
                || !tableExists("moh_bill_patient_bill") || !tableExists("moh_bill_consommation")
                || !tableExists("moh_bill_patient_service_bill")) {
            return null;
        }
        List<String> names = new ArrayList<String>();
        for (String itemName : itemNames) {
            String name = itemName == null ? "" : itemName.trim().toLowerCase();
            if (name.length() > 1 && !names.contains(name)) {
                names.add(name);
            }
        }
        if (names.isEmpty()) {
            return null;
        }
        StringBuilder nameClause = new StringBuilder();
        Map<String, Object> parameters = new HashMap<String, Object>();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                nameClause.append(" or ");
            }
            String parameterName = "itemName" + i;
            nameClause.append("lower(trim(coalesce(fsp.name, ''))) = :").append(parameterName)
                    .append(" or lower(trim(coalesce(fsp.short_name, ''))) = :").append(parameterName)
                    .append(" or lower(trim(coalesce(fsp.description, ''))) = :").append(parameterName)
                    .append(" or lower(trim(coalesce(psb.service_other, ''))) = :").append(parameterName)
                    .append(" or lower(trim(coalesce(psb.service_other_description, ''))) = :").append(parameterName);
            parameters.put(parameterName, names.get(i));
        }
        parameters.put("minimumBillDate", addMinutes(referenceDate, -5));
        parameters.put("maximumBillDate", addMinutes(referenceDate, 30));
        parameters.put("referenceDate", referenceDate);
        if (requirePatientMatch) {
            parameters.put("patientId", patientId);
        }
        String patientJoin = requirePatientMatch
                ? "left join moh_bill_beneficiary b on b.beneficiary_id = c.beneficiary_id "
                : "";
        String patientClause = requirePatientMatch ? " and b.patient_id = :patientId" : "";
        Object result = sessionFactory.getCurrentSession()
                .createSQLQuery("select pb.patient_bill_id from moh_bill_patient_bill pb "
                        + "join moh_bill_consommation c on c.patient_bill_id = pb.patient_bill_id "
                        + patientJoin
                        + "join moh_bill_patient_service_bill psb on psb.consommation_id = c.consommation_id "
                        + "left join moh_bill_billable_service bs on bs.billable_service_id = psb.billable_service_id "
                        + "left join moh_bill_facility_service_price fsp on fsp.facility_service_price_id = bs.facility_service_price_id "
                        + "where coalesce(c.created_date, psb.created_date, pb.created_date) between :minimumBillDate and :maximumBillDate "
                        + patientClause + " and (" + nameClause.toString() + ") "
                        + "group by pb.patient_bill_id "
                        + "order by count(distinct psb.patient_service_bill_id) desc, "
                        + "abs(timestampdiff(second, coalesce(c.created_date, psb.created_date, pb.created_date), :referenceDate)) asc, "
                        + "pb.patient_bill_id desc")
                .setProperties(parameters)
                .setMaxResults(1)
                .uniqueResult();
        return integerValue(result);
    }

    private Integer findLatestPatientBillIdFromPatientBill(Integer patientId, String billIdColumn, Date referenceDate) {
        String patientColumn = resolveExistingBillingColumn("patient_id", "patient", "beneficiary_patient_id");
        if (patientColumn == null) {
            return null;
        }
        String dateColumn = resolveExistingBillingColumn("date_created", "created_date", "dateCreated", "createdDate");
        String dateAlias = dateColumn == null ? null : "moh_bill_patient_bill." + dateColumn;
        String timingClause = timingWhereClause(dateAlias, referenceDate);
        String ordering = timingOrderClause(dateAlias, referenceDate, billIdColumn);
        Object result = sessionFactory.getCurrentSession()
                .createSQLQuery("select " + billIdColumn + " from moh_bill_patient_bill where " + patientColumn + " = :patientId"
                        + timingClause + ordering)
                .setParameter("patientId", patientId)
                .setProperties(timingParameters(dateAlias, referenceDate))
                .setMaxResults(1)
                .uniqueResult();
        return integerValue(result);
    }

    private Integer findLatestPatientBillIdViaBeneficiary(Integer patientId, String billIdColumn, Date referenceDate) {
        String consommationTable = resolveExistingTable("moh_bill_consommation", "moh_bill_consumption");
        String beneficiaryTable = resolveExistingTable("moh_bill_beneficiary", "moh_bill_beneficiaries");
        if (consommationTable == null || beneficiaryTable == null) {
            return null;
        }
        String consommationBillColumn = resolveExistingColumn(consommationTable, "patient_bill_id", "patient_bill", "bill_id");
        String consommationBeneficiaryColumn = resolveExistingColumn(consommationTable, "beneficiary_id", "beneficiary");
        String beneficiaryIdColumn = resolveExistingColumn(beneficiaryTable, "beneficiary_id", "id");
        String beneficiaryPatientColumn = resolveExistingColumn(beneficiaryTable, "patient_id", "patient");
        if (consommationBillColumn == null || consommationBeneficiaryColumn == null || beneficiaryIdColumn == null
                || beneficiaryPatientColumn == null) {
            return null;
        }
        String consommationDateColumn = resolveExistingColumn(consommationTable, "date_created", "created_date", "dateCreated", "createdDate");
        String billDateColumn = resolveExistingBillingColumn("date_created", "created_date", "dateCreated", "createdDate");
        String dateAlias = consommationDateColumn != null ? "c." + consommationDateColumn
                : (billDateColumn == null ? null : "pb." + billDateColumn);
        String timingClause = timingWhereClause(dateAlias, referenceDate);
        String ordering = timingOrderClause(dateAlias, referenceDate, "pb." + billIdColumn);
        Object result = sessionFactory.getCurrentSession()
                .createSQLQuery("select pb." + billIdColumn + " from moh_bill_patient_bill pb "
                        + "join " + consommationTable + " c on c." + consommationBillColumn + " = pb." + billIdColumn + " "
                        + "join " + beneficiaryTable + " b on b." + beneficiaryIdColumn + " = c." + consommationBeneficiaryColumn + " "
                        + "where b." + beneficiaryPatientColumn + " = :patientId" + timingClause + ordering)
                .setParameter("patientId", patientId)
                .setProperties(timingParameters(dateAlias, referenceDate))
                .setMaxResults(1)
                .uniqueResult();
        return integerValue(result);
    }

    private Integer findLatestPatientBillIdViaConsommationPatient(Integer patientId, String billIdColumn, Date referenceDate) {
        String consommationTable = resolveExistingTable("moh_bill_consommation", "moh_bill_consumption");
        if (consommationTable == null) {
            return null;
        }
        String consommationBillColumn = resolveExistingColumn(consommationTable, "patient_bill_id", "patient_bill", "bill_id");
        String consommationPatientColumn = resolveExistingColumn(consommationTable, "patient_id", "patient", "beneficiary_patient_id");
        if (consommationBillColumn == null || consommationPatientColumn == null) {
            return null;
        }
        String consommationDateColumn = resolveExistingColumn(consommationTable, "date_created", "created_date", "dateCreated", "createdDate");
        String billDateColumn = resolveExistingBillingColumn("date_created", "created_date", "dateCreated", "createdDate");
        String dateAlias = consommationDateColumn != null ? "c." + consommationDateColumn
                : (billDateColumn == null ? null : "pb." + billDateColumn);
        String timingClause = timingWhereClause(dateAlias, referenceDate);
        String ordering = timingOrderClause(dateAlias, referenceDate, "pb." + billIdColumn);
        Object result = sessionFactory.getCurrentSession()
                .createSQLQuery("select pb." + billIdColumn + " from moh_bill_patient_bill pb "
                        + "join " + consommationTable + " c on c." + consommationBillColumn + " = pb." + billIdColumn + " "
                        + "where c." + consommationPatientColumn + " = :patientId" + timingClause + ordering)
                .setParameter("patientId", patientId)
                .setProperties(timingParameters(dateAlias, referenceDate))
                .setMaxResults(1)
                .uniqueResult();
        return integerValue(result);
    }

    private String timingWhereClause(String dateExpression, Date referenceDate) {
        if (dateExpression == null || referenceDate == null) {
            return "";
        }
        return " and " + dateExpression + " between :minimumBillDate and :maximumBillDate";
    }

    private String timingOrderClause(String dateExpression, Date referenceDate, String fallbackIdExpression) {
        if (dateExpression == null || referenceDate == null) {
            return " order by " + fallbackIdExpression + " desc";
        }
        return " order by abs(timestampdiff(second, " + dateExpression + ", :referenceDate)) asc, "
                + fallbackIdExpression + " desc";
    }

    private java.util.Map<String, Date> timingParameters(String dateExpression, Date referenceDate) {
        java.util.Map<String, Date> parameters = new java.util.HashMap<String, Date>();
        if (dateExpression != null && referenceDate != null) {
            parameters.put("referenceDate", referenceDate);
            parameters.put("minimumBillDate", addMinutes(referenceDate, -5));
            parameters.put("maximumBillDate", addMinutes(referenceDate, 30));
        }
        return parameters;
    }

    private Date addMinutes(Date date, int minutes) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(java.util.Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    private Integer integerValue(Object result) {
        return result instanceof Number ? Integer.valueOf(((Number) result).intValue()) : null;
    }

    private String resolveExistingTable(String... candidates) {
        for (String candidate : candidates) {
            if (tableExists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean tableExists(String tableName) {
        try {
            Object result = sessionFactory.getCurrentSession()
                    .createSQLQuery("select table_name from information_schema.tables where table_schema = database() and table_name = :tableName")
                    .setParameter("tableName", tableName)
                    .setMaxResults(1)
                    .uniqueResult();
            return result != null;
        }
        catch (RuntimeException ignored) {
            return false;
        }
    }

    private String resolveExistingColumn(String tableName, String... candidates) {
        for (String candidate : candidates) {
            try {
                Object result = sessionFactory.getCurrentSession()
                        .createSQLQuery("select column_name from information_schema.columns where table_schema = database() and table_name = :tableName and column_name = :columnName")
                        .setParameter("tableName", tableName)
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

    private String resolveExistingBillingColumn(String... candidates) {
        return resolveExistingColumn("moh_bill_patient_bill", candidates);
    }

    @Override
    public Boolean isPatientBillPaid(Integer patientBillId) {
        if (patientBillId == null || !tableExists("moh_bill_patient_bill")) {
            return null;
        }
        try {
            String billIdColumn = resolveExistingBillingColumn("patient_bill_id", "bill_id", "id");
            if (billIdColumn == null) {
                return null;
            }
            Boolean paidByPayments = isPatientBillPaidByPayments(patientBillId, billIdColumn);
            if (paidByPayments != null) {
                return paidByPayments;
            }
            Boolean paidByStatus = isPatientBillPaidByStatus(patientBillId, billIdColumn);
            if (paidByStatus != null) {
                return paidByStatus;
            }
            return isPatientBillPaidByFlag(patientBillId, billIdColumn);
        }
        catch (RuntimeException ignored) {
            return null;
        }
    }

    private Boolean isPatientBillPaidByPayments(Integer patientBillId, String billIdColumn) {
        String paymentTable = resolveExistingTable("moh_bill_payment", "moh_bill_bill_payment", "moh_bill_billpayment");
        String billAmountColumn = resolveExistingBillingColumn("amount", "bill_amount", "total_amount");
        if (paymentTable == null || billAmountColumn == null) {
            return null;
        }
        String paymentBillColumn = resolveExistingColumn(paymentTable, "patient_bill_id", "patient_bill", "bill_id");
        String paymentAmountColumn = resolveExistingColumn(paymentTable, "amount_paid", "amountPaid", "amount");
        if (paymentBillColumn == null || paymentAmountColumn == null) {
            return null;
        }
        Object billAmountResult = sessionFactory.getCurrentSession()
                .createSQLQuery("select " + billAmountColumn + " from moh_bill_patient_bill where " + billIdColumn + " = :patientBillId")
                .setParameter("patientBillId", patientBillId)
                .setMaxResults(1)
                .uniqueResult();
        BigDecimal billAmount = bigDecimalValue(billAmountResult);
        if (billAmount == null) {
            return null;
        }
        if (billAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Boolean.TRUE;
        }
        String voidedColumn = resolveExistingColumn(paymentTable, "voided", "is_voided");
        String voidedClause = voidedColumn == null ? "" : " and (" + voidedColumn + " = 0 or " + voidedColumn + " is null)";
        Object paidAmountResult = sessionFactory.getCurrentSession()
                .createSQLQuery("select coalesce(sum(" + paymentAmountColumn + "), 0) from " + paymentTable
                        + " where " + paymentBillColumn + " = :patientBillId" + voidedClause)
                .setParameter("patientBillId", patientBillId)
                .uniqueResult();
        BigDecimal paidAmount = bigDecimalValue(paidAmountResult);
        return paidAmount != null && paidAmount.compareTo(billAmount) >= 0;
    }

    private Boolean isPatientBillPaidByStatus(Integer patientBillId, String billIdColumn) {
        String statusColumn = resolveExistingBillingColumn("status", "bill_status");
        if (statusColumn == null) {
            return null;
        }
        Object result = sessionFactory.getCurrentSession()
                .createSQLQuery("select " + statusColumn + " from moh_bill_patient_bill where " + billIdColumn + " = :patientBillId")
                .setParameter("patientBillId", patientBillId)
                .setMaxResults(1)
                .uniqueResult();
        if (result == null) {
            return null;
        }
        String status = result.toString().trim().toUpperCase(java.util.Locale.ENGLISH).replace('_', ' ');
        if ("FULLY PAID".equals(status) || "PAID".equals(status) || "COMPLETED".equals(status)) {
            return Boolean.TRUE;
        }
        if ("PARTLY PAID".equals(status) || "PARTIALLY PAID".equals(status) || "UNPAID".equals(status)
                || "CANCELLED".equals(status) || "VOIDED".equals(status)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Boolean isPatientBillPaidByFlag(Integer patientBillId, String billIdColumn) {
        String paidColumn = resolveExistingBillingColumn("is_paid", "paid");
        if (paidColumn == null) {
            return null;
        }
        Object result = sessionFactory.getCurrentSession()
                .createSQLQuery("select " + paidColumn + " from moh_bill_patient_bill where " + billIdColumn + " = :patientBillId")
                .setParameter("patientBillId", patientBillId)
                .setMaxResults(1)
                .uniqueResult();
        return booleanValue(result);
    }

    private BigDecimal bigDecimalValue(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof BigDecimal) {
            return (BigDecimal) result;
        }
        try {
            return new BigDecimal(result.toString());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean booleanValue(Object result) {
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        if (result instanceof Number) {
            return ((Number) result).intValue() != 0;
        }
        return result == null ? null : Boolean.valueOf(result.toString());
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
    public List<DispenseRecord> getDispenseRecords(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(DispenseRecord.class);
        criteria.add(Restrictions.eq("voided", false));
        if (fromInclusive != null) {
            criteria.add(Restrictions.ge("dateCreated", fromInclusive));
        }
        if (toExclusive != null) {
            criteria.add(Restrictions.lt("dateCreated", toExclusive));
        }
        if (pharmacyLocationId != null) {
            criteria.createAlias("dispensingPharmacyLocation", "dispensingPharmacyLocation");
            criteria.add(Restrictions.eq("dispensingPharmacyLocation.pharmacyLocationId", pharmacyLocationId));
        }
        criteria.addOrder(Order.asc("dateCreated"));
        return criteria.list();
    }

    @Override
    public List<PatientDispensingRequest> getPaymentExceptionApprovedRequests(Date fromInclusive,
                                                                              Date toExclusive,
                                                                              Integer pharmacyLocationId,
                                                                              RequestType requestType) {
        org.hibernate.Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PatientDispensingRequest.class);
        criteria.add(Restrictions.eq("voided", false));
        criteria.add(Restrictions.eq("paymentExceptionApproved", true));
        if (fromInclusive != null) {
            criteria.add(Restrictions.ge("paymentExceptionApprovedDate", fromInclusive));
        }
        if (toExclusive != null) {
            criteria.add(Restrictions.lt("paymentExceptionApprovedDate", toExclusive));
        }
        if (requestType != null) {
            criteria.add(Restrictions.eq("requestType", requestType));
        }
        if (pharmacyLocationId != null) {
            criteria.createAlias("targetPharmacyLocation", "targetPharmacyLocation", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN);
            criteria.createAlias("lines", "line", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN);
            criteria.createAlias("line.routedPharmacyLocation", "routedPharmacyLocation", org.hibernate.sql.JoinType.LEFT_OUTER_JOIN);
            Disjunction pharmacyMatch = Restrictions.disjunction();
            pharmacyMatch.add(Restrictions.eq("targetPharmacyLocation.pharmacyLocationId", pharmacyLocationId));
            pharmacyMatch.add(Restrictions.eq("routedPharmacyLocation.pharmacyLocationId", pharmacyLocationId));
            pharmacyMatch.add(Restrictions.isNull("targetPharmacyLocation"));
            criteria.add(pharmacyMatch);
            criteria.setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY);
        }
        criteria.addOrder(Order.desc("paymentExceptionApprovedDate"));
        criteria.addOrder(Order.desc("patientDispensingRequestId"));
        return criteria.list();
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
