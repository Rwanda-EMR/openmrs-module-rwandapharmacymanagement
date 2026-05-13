package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.LabSupplyRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.math.BigDecimal;
import java.util.List;

public interface LabSupplyService extends OpenmrsService {

    LabSupplyRequest createRequest(Location labUnit, List<LabSupplyRequestLineCommand> commands);

    List<LabSupplyRequest> getPendingRequests();

    long countPendingRequests();

    List<LabSupplyRequest> getRequestsByCreator(User creator);

    List<LabSupplyRequest> getRequestsByCreator(User creator, int offset, int limit);

    long countRequestsByCreator(User creator);

    List<LabSupplyRequest> getApprovedRequestsByPharmacy(PharmacyLocation pharmacyLocation);

    LabSupplyRequest getRequest(Integer requestId);

    LabSupplyRequest approveRequest(Integer requestId, PharmacyLocation dispensingPharmacyLocation,
                                    Integer[] lineIds, BigDecimal[] approvedQuantities);
}
