package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;

import java.util.List;

public interface IntegrationLogDao {

    List<IntegrationLog> getRecentLogs();

    List<IntegrationLog> getPendingHieLogs(int maxResults);

    List<IntegrationLog> getPendingSmsLogs(int maxResults);

    IntegrationLog getLog(Integer integrationLogId);

    IntegrationLog getPendingLog(IntegrationType integrationType, String requestPayload);

    IntegrationLog saveLog(IntegrationLog log);
}
