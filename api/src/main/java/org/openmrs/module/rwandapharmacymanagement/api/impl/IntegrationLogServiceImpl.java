package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.db.IntegrationLogDao;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IntegrationLog saveLog(IntegrationLog log) {
        return integrationLogDao.saveLog(log);
    }
}
