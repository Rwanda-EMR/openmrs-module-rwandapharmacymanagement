package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.db.IntegrationLogDao;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class IntegrationLogServiceImpl extends BaseOpenmrsService implements IntegrationLogService {

    private IntegrationLogDao integrationLogDao;

    public void setIntegrationLogDao(IntegrationLogDao integrationLogDao) {
        this.integrationLogDao = integrationLogDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLog> getRecentLogs() {
        return integrationLogDao.getRecentLogs();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLog> getPendingHieLogs(int maxResults) {
        return integrationLogDao.getPendingHieLogs(maxResults);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLog> getPendingSmsLogs(int maxResults) {
        return integrationLogDao.getPendingSmsLogs(maxResults);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationLog getLog(Integer integrationLogId) {
        return integrationLogDao.getLog(integrationLogId);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationLog getPendingLog(IntegrationType integrationType, String requestPayload) {
        return integrationLogDao.getPendingLog(integrationType, requestPayload);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IntegrationLog saveLog(IntegrationLog log) {
        return integrationLogDao.saveLog(log);
    }
}
