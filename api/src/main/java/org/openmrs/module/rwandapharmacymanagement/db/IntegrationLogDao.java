package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;

import java.util.List;

public interface IntegrationLogDao {

    List<IntegrationLog> getRecentLogs();

    IntegrationLog saveLog(IntegrationLog log);
}
