package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.User;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PharmacyItemRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface PharmacyItemRequestService extends OpenmrsService {

    PharmacyItemRequest createRequest(PharmacyLocation requestedPharmacyLocation, String reason,
                                      List<PharmacyItemRequestLineCommand> commands);

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

    PharmacyItemRequest approveRequest(Integer requestId, PharmacyLocation activePharmacy,
                                       Integer[] lineIds, BigDecimal[] approvedQuantities, String[] batchNumbers);

    PharmacyItemRequest rejectRequest(Integer requestId, PharmacyLocation activePharmacy, String reason);

    PharmacyItemRequest removeRequestLine(Integer requestId, PharmacyLocation activePharmacy, Integer requestLineId,
                                          String reason);
}
