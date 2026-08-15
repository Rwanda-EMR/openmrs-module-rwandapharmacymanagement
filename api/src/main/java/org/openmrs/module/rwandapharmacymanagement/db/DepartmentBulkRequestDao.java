package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockIssue;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.util.List;

public interface DepartmentBulkRequestDao {

    List<DepartmentBulkRequest> getRequestsByStatus(String status, int offset, int limit);

    long countRequestsByStatus(String status);

    long countRequestsByStatusForSupplyingPharmacy(String status, PharmacyLocation supplyingPharmacyLocation);

    List<DepartmentBulkRequest> getRequestsByCreator(User creator, int offset, int limit);

    long countRequestsByCreator(User creator);

    List<DepartmentBulkRequest> getRequestsForDepartment(PharmacyLocation departmentLocation, int offset, int limit);

    long countRequestsForDepartment(PharmacyLocation departmentLocation);

    List<DepartmentStockIssue> getIssuesForDepartment(PharmacyLocation departmentLocation, int offset, int limit);

    List<DepartmentBulkRequest> getFinanceHistory(int offset, int limit);

    DepartmentBulkRequest getRequest(Integer requestId);

    DepartmentBulkRequest saveRequest(DepartmentBulkRequest request);

    DepartmentBulkRequestLine saveRequestLine(DepartmentBulkRequestLine line);

    DepartmentStockIssue saveIssue(DepartmentStockIssue issue);
}
