package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturnLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.List;

public interface DepartmentStockReturnDao {

    List<DepartmentStockReturn> getReturnsForDepartment(PharmacyLocation departmentLocation, int offset, int limit);

    List<DepartmentStockReturn> getReturnsForDestination(PharmacyLocation destinationPharmacyLocation, String status,
                                                         int offset, int limit);

    DepartmentStockReturn getReturn(Integer returnId);

    DepartmentStockReturn saveReturn(DepartmentStockReturn stockReturn);

    DepartmentStockReturnLine saveReturnLine(DepartmentStockReturnLine line);
}
