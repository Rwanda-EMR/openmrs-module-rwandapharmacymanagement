package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.Date;
import java.util.List;

public interface PharmacyItemRequestDao {

    List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation);

    List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, int offset, int limit);

    List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, String search,
                                                            int offset, int limit);

    long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation);

    long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, String search);

    List<PharmacyItemRequest> getRequestsByCreator(User creator, int offset, int limit);

    long countRequestsByCreator(User creator);

    List<PharmacyItemRequest> getRequestsForPharmacy(PharmacyLocation pharmacyLocation, Date fromInclusive,
                                                     Date toExclusive, String status, String itemName, int maxResults);

    PharmacyItemRequest getRequest(Integer requestId);

    PharmacyItemRequest saveRequest(PharmacyItemRequest request);

    PharmacyItemRequestLine saveRequestLine(PharmacyItemRequestLine line);
}
