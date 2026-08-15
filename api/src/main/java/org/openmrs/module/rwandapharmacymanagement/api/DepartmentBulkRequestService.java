package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.User;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentBulkRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentStockIssueLineCommand;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockIssue;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.math.BigDecimal;
import java.util.List;

public interface DepartmentBulkRequestService extends OpenmrsService {

    String GP_WORKFLOW_MODE = "rwandapharmacymanagement.departmentBulkRequest.workflowMode";

    String MODE_PERIODIC_ACCOUNTABILITY = "PERIODIC_ACCOUNTABILITY";

    String MODE_FULL_VISIBILITY = "FULL_VISIBILITY";

    String STATUS_SUBMITTED = "SUBMITTED";

    String STATUS_CORRECTION_REQUESTED = "CORRECTION_REQUESTED";

    String STATUS_FINANCE_APPROVED = "FINANCE_APPROVED";

    String STATUS_REJECTED = "REJECTED";

    String STATUS_DISPENSED = "DISPENSED";

    DepartmentBulkRequest createRequest(PharmacyLocation departmentLocation, PharmacyLocation supplyingPharmacyLocation,
                                        String justification, List<DepartmentBulkRequestLineCommand> commands);

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

    DepartmentBulkRequest financeApproveRequest(Integer requestId, Integer[] lineIds, BigDecimal[] approvedQuantities);

    DepartmentBulkRequest removeRequestLine(Integer requestId, Integer requestLineId, String reason);

    DepartmentBulkRequest rejectRequest(Integer requestId, String reason);

    DepartmentBulkRequest requestCorrection(Integer requestId, String correctionNote);

    DepartmentBulkRequest dispenseRequest(Integer requestId, Integer[] lineIds, BigDecimal[] dispensedQuantities,
                                          String[] batchNumbers);

    DepartmentStockIssue recordIssue(PharmacyLocation departmentLocation, Integer itemId, BigDecimal quantity,
                                     String batchNumber, String issuedTo, String reason);

    List<DepartmentStockIssue> recordIssues(PharmacyLocation departmentLocation,
                                            List<DepartmentStockIssueLineCommand> commands,
                                            String issuedTo, String reason);
}
