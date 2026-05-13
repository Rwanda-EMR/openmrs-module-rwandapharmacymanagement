package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;

import java.util.List;

public interface IntegrationLogService extends OpenmrsService {

    List<IntegrationLog> getRecentLogs();

    IntegrationLog saveLog(IntegrationLog log);
}
