package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentStockReturnLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.List;

public interface DepartmentStockReturnService extends OpenmrsService {

    String STATUS_SUBMITTED = "SUBMITTED";

    String STATUS_REJECTED = "REJECTED";

    String STATUS_RECEIVED = "RECEIVED";

    DepartmentStockReturn createReturn(PharmacyLocation departmentLocation,
                                       PharmacyLocation destinationPharmacyLocation,
                                       String reason,
                                       List<DepartmentStockReturnLineCommand> commands);

    DepartmentStockReturn receiveReturn(Integer returnId);

    DepartmentStockReturn rejectReturn(Integer returnId, String rejectionReason);

    DepartmentStockReturn getReturn(Integer returnId);

    List<DepartmentStockReturn> getReturnsForDepartment(PharmacyLocation departmentLocation, int offset, int limit);

    List<DepartmentStockReturn> getReturnsForDestination(PharmacyLocation destinationPharmacyLocation, String status,
                                                         int offset, int limit);
}
