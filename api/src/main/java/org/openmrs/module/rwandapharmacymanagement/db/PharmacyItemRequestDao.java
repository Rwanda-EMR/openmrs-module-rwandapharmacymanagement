package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.List;

public interface PharmacyItemRequestDao {

    List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation);

    long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation);

    List<PharmacyItemRequest> getRequestsByCreator(User creator, int offset, int limit);

    long countRequestsByCreator(User creator);

    PharmacyItemRequest getRequest(Integer requestId);

    PharmacyItemRequest saveRequest(PharmacyItemRequest request);

    PharmacyItemRequestLine saveRequestLine(PharmacyItemRequestLine line);
}
