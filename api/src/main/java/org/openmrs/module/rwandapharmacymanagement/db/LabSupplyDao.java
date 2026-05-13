package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequest;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.List;

public interface LabSupplyDao {

    List<LabSupplyRequest> getPendingRequests();

    long countPendingRequests();

    List<LabSupplyRequest> getRequestsByCreator(User creator);

    List<LabSupplyRequest> getRequestsByCreator(User creator, int offset, int limit);

    long countRequestsByCreator(User creator);

    List<LabSupplyRequest> getApprovedRequestsByPharmacy(PharmacyLocation pharmacyLocation);

    LabSupplyRequest getRequest(Integer requestId);

    LabSupplyRequest saveRequest(LabSupplyRequest request);

    LabSupplyRequestLine saveRequestLine(LabSupplyRequestLine line);
}
