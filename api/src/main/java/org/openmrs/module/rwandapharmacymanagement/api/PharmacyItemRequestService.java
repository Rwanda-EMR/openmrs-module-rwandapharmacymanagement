package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.User;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PharmacyItemRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.math.BigDecimal;
import java.util.List;

public interface PharmacyItemRequestService extends OpenmrsService {

    PharmacyItemRequest createRequest(PharmacyLocation requestedPharmacyLocation, String reason,
                                      List<PharmacyItemRequestLineCommand> commands);

    List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation);

    long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation);

    List<PharmacyItemRequest> getRequestsByCreator(User creator, int offset, int limit);

    long countRequestsByCreator(User creator);

    PharmacyItemRequest getRequest(Integer requestId);

    PharmacyItemRequest approveRequest(Integer requestId, PharmacyLocation activePharmacy,
                                       Integer[] lineIds, BigDecimal[] approvedQuantities);
}
