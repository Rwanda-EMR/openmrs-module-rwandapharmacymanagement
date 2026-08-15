package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentBulkRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentStockIssueLineCommand;
import org.openmrs.module.rwandapharmacymanagement.db.DepartmentBulkRequestDao;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequest;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentBulkRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockIssue;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Transactional
public class DepartmentBulkRequestServiceImpl extends BaseOpenmrsService implements DepartmentBulkRequestService {

    private DepartmentBulkRequestDao departmentBulkRequestDao;
    private InventoryDao inventoryDao;
    private PharmacyService pharmacyService;
    private AdministrationService administrationService;

    public void setDepartmentBulkRequestDao(DepartmentBulkRequestDao departmentBulkRequestDao) {
        this.departmentBulkRequestDao = departmentBulkRequestDao;
    }

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @Override
    public DepartmentBulkRequest createRequest(PharmacyLocation departmentLocation, PharmacyLocation supplyingPharmacyLocation,
                                               String justification, List<DepartmentBulkRequestLineCommand> commands) {
        requireDepartmentStore(departmentLocation);
        if (supplyingPharmacyLocation == null || supplyingPharmacyLocation.getPharmacyLocationId() == null) {
            throw new IllegalArgumentException("Supplying pharmacy is required");
        }
        List<PreparedLine> preparedLines = normalizeCommands(commands);
        if (preparedLines.isEmpty()) {
            throw new IllegalArgumentException("At least one requested item is required");
        }
        Date now = new Date();
        User currentUser = Context.getAuthenticatedUser();
        DepartmentBulkRequest request = new DepartmentBulkRequest();
        request.setDepartmentLocation(departmentLocation);
        request.setSupplyingPharmacyLocation(supplyingPharmacyLocation);
        request.setRequestStatus(STATUS_SUBMITTED);
        request.setWorkflowMode(resolveWorkflowMode());
        request.setJustification(trimToNull(justification));
        request.setCreator(currentUser);
        request.setDateCreated(now);
        request.setVoided(Boolean.FALSE);
        request.setUuid(UUID.randomUUID().toString());
        request.setLines(new ArrayList<DepartmentBulkRequestLine>());
        for (PreparedLine preparedLine : preparedLines) {
            DepartmentBulkRequestLine line = new DepartmentBulkRequestLine();
            line.setDepartmentBulkRequest(request);
            line.setItem(preparedLine.item);
            line.setPreviousRequestedQuantity(preparedLine.previousRequestedQuantity);
            line.setPreviousReceivedQuantity(preparedLine.previousReceivedQuantity);
            line.setPreviousUsedQuantity(preparedLine.previousUsedQuantity);
            line.setCurrentRemainingQuantity(preparedLine.currentRemainingQuantity);
            line.setRequestedQuantity(preparedLine.requestedQuantity);
            line.setLineJustification(trimToNull(preparedLine.lineJustification));
            line.setCreator(currentUser);
            line.setDateCreated(now);
            line.setVoided(Boolean.FALSE);
            line.setUuid(UUID.randomUUID().toString());
            request.getLines().add(line);
        }
        return departmentBulkRequestDao.saveRequest(request);
    }

    @Override
    public List<DepartmentBulkRequest> getRequestsByStatus(String status, int offset, int limit) {
        return departmentBulkRequestDao.getRequestsByStatus(status, offset, limit);
    }

    @Override
    public long countRequestsByStatus(String status) {
        return departmentBulkRequestDao.countRequestsByStatus(status);
    }

    @Override
    public long countRequestsByStatusForSupplyingPharmacy(String status, PharmacyLocation supplyingPharmacyLocation) {
        return departmentBulkRequestDao.countRequestsByStatusForSupplyingPharmacy(status, supplyingPharmacyLocation);
    }

    @Override
    public List<DepartmentBulkRequest> getRequestsByCreator(User creator, int offset, int limit) {
        return departmentBulkRequestDao.getRequestsByCreator(creator, offset, limit);
    }

    @Override
    public long countRequestsByCreator(User creator) {
        return departmentBulkRequestDao.countRequestsByCreator(creator);
    }

    @Override
    public List<DepartmentBulkRequest> getRequestsForDepartment(PharmacyLocation departmentLocation, int offset, int limit) {
        return departmentBulkRequestDao.getRequestsForDepartment(departmentLocation, offset, limit);
    }

    @Override
    public long countRequestsForDepartment(PharmacyLocation departmentLocation) {
        return departmentBulkRequestDao.countRequestsForDepartment(departmentLocation);
    }

    @Override
    public List<DepartmentStockIssue> getIssuesForDepartment(PharmacyLocation departmentLocation, int offset, int limit) {
        return departmentBulkRequestDao.getIssuesForDepartment(departmentLocation, offset, limit);
    }

    @Override
    public List<DepartmentBulkRequest> getFinanceHistory(int offset, int limit) {
        return departmentBulkRequestDao.getFinanceHistory(offset, limit);
    }

    @Override
    public DepartmentBulkRequest getRequest(Integer requestId) {
        return departmentBulkRequestDao.getRequest(requestId);
    }

    @Override
    public DepartmentBulkRequest financeApproveRequest(Integer requestId, Integer[] lineIds, BigDecimal[] approvedQuantities) {
        DepartmentBulkRequest request = requireRequest(requestId);
        if (!STATUS_SUBMITTED.equals(request.getRequestStatus()) && !STATUS_CORRECTION_REQUESTED.equals(request.getRequestStatus())) {
            throw new IllegalArgumentException("Only submitted department requests can be finance approved");
        }
        Map<Integer, BigDecimal> approvedByLineId = quantitiesByLineId(lineIds, approvedQuantities);
        boolean hasActiveApprovedLine = false;
        for (DepartmentBulkRequestLine line : request.getLines()) {
            if (!isActiveRequestLine(line)) {
                continue;
            }
            BigDecimal requestedQuantity = zeroIfNull(line.getRequestedQuantity());
            BigDecimal approvedQuantity = approvedByLineId.containsKey(line.getDepartmentBulkRequestLineId())
                    ? zeroIfNull(approvedByLineId.get(line.getDepartmentBulkRequestLineId())) : requestedQuantity;
            if (approvedQuantity.compareTo(BigDecimal.ZERO) < 0 || approvedQuantity.compareTo(requestedQuantity) > 0) {
                throw new IllegalArgumentException("Approved quantity must be between zero and requested quantity");
            }
            if (approvedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                hasActiveApprovedLine = true;
            }
            line.setApprovedQuantity(approvedQuantity);
            departmentBulkRequestDao.saveRequestLine(line);
        }
        if (!hasActiveApprovedLine) {
            throw new IllegalArgumentException("At least one active request line must have an approved quantity");
        }
        request.setRequestStatus(STATUS_FINANCE_APPROVED);
        request.setFinanceApprovedBy(Context.getAuthenticatedUser());
        request.setFinanceApprovedDate(new Date());
        request.setCorrectionNote(null);
        return departmentBulkRequestDao.saveRequest(request);
    }

    @Override
    public DepartmentBulkRequest removeRequestLine(Integer requestId, Integer requestLineId, String reason) {
        DepartmentBulkRequest request = requireRequest(requestId);
        if (!STATUS_SUBMITTED.equals(request.getRequestStatus()) && !STATUS_CORRECTION_REQUESTED.equals(request.getRequestStatus())
                && !STATUS_FINANCE_APPROVED.equals(request.getRequestStatus())) {
            throw new IllegalArgumentException("Only pending department request lines can be removed");
        }
        String trimmedReason = trimToNull(reason);
        if (trimmedReason == null) {
            throw new IllegalArgumentException("Reason is required when removing a request line");
        }
        DepartmentBulkRequestLine line = findRequestLine(request, requestLineId);
        if (line == null) {
            throw new IllegalArgumentException("Department request line not found");
        }
        if (!Boolean.TRUE.equals(line.getVoided())) {
            line.setVoided(Boolean.TRUE);
            line.setVoidedBy(Context.getAuthenticatedUser());
            line.setDateVoided(new Date());
            line.setVoidReason(trimmedReason);
            departmentBulkRequestDao.saveRequestLine(line);
        }
        if (!hasActiveRequestLines(request)) {
            request.setRequestStatus(STATUS_REJECTED);
            request.setCorrectionNote("All request lines removed. " + trimmedReason);
        }
        return departmentBulkRequestDao.saveRequest(request);
    }

    @Override
    public DepartmentBulkRequest rejectRequest(Integer requestId, String reason) {
        DepartmentBulkRequest request = requireRequest(requestId);
        if (STATUS_DISPENSED.equals(request.getRequestStatus())) {
            throw new IllegalArgumentException("Dispensed department requests cannot be rejected");
        }
        request.setRequestStatus(STATUS_REJECTED);
        request.setCorrectionNote(trimToNull(reason));
        return departmentBulkRequestDao.saveRequest(request);
    }

    @Override
    public DepartmentBulkRequest requestCorrection(Integer requestId, String correctionNote) {
        DepartmentBulkRequest request = requireRequest(requestId);
        if (!STATUS_SUBMITTED.equals(request.getRequestStatus())) {
            throw new IllegalArgumentException("Only submitted department requests can be returned for correction");
        }
        request.setRequestStatus(STATUS_CORRECTION_REQUESTED);
        request.setCorrectionNote(trimToNull(correctionNote));
        return departmentBulkRequestDao.saveRequest(request);
    }

    @Override
    public DepartmentBulkRequest dispenseRequest(Integer requestId, Integer[] lineIds, BigDecimal[] dispensedQuantities,
                                                 String[] batchNumbers) {
        DepartmentBulkRequest request = requireRequest(requestId);
        if (!STATUS_FINANCE_APPROVED.equals(request.getRequestStatus())) {
            throw new IllegalArgumentException("Only finance-approved department requests can be dispensed");
        }
        Map<Integer, List<DispenseAllocation>> allocationsByLineId = allocationsByLineId(lineIds, dispensedQuantities,
                batchNumbers);
        boolean hasDispensedLine = false;
        for (DepartmentBulkRequestLine line : request.getLines()) {
            if (!isActiveRequestLine(line)) {
                continue;
            }
            BigDecimal approvedQuantity = zeroIfNull(line.getApprovedQuantity());
            List<DispenseAllocation> allocations = allocationsByLineId.get(line.getDepartmentBulkRequestLineId());
            if (allocations == null || allocations.isEmpty()) {
                allocations = new ArrayList<DispenseAllocation>();
                allocations.add(new DispenseAllocation(approvedQuantity, null));
            }
            BigDecimal dispensedQuantity = totalAllocated(allocations);
            if (dispensedQuantity.compareTo(BigDecimal.ZERO) < 0 || dispensedQuantity.compareTo(approvedQuantity) > 0) {
                throw new IllegalArgumentException("Dispensed quantity must be between zero and approved quantity");
            }
            if (dispensedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                hasDispensedLine = true;
                for (DispenseAllocation allocation : allocations) {
                    if (allocation.quantity.compareTo(BigDecimal.ZERO) > 0) {
                        transferToDepartment(request, line, allocation.quantity, allocation.batchNumber);
                    }
                }
                line.setDispensedQuantity(dispensedQuantity);
                line.setBatchNumber(batchSummary(allocations));
            } else {
                line.setDispensedQuantity(BigDecimal.ZERO);
                line.setBatchNumber(null);
            }
            departmentBulkRequestDao.saveRequestLine(line);
        }
        if (!hasDispensedLine) {
            throw new IllegalArgumentException("At least one active request line must be dispensed");
        }
        request.setRequestStatus(STATUS_DISPENSED);
        request.setDispensedBy(Context.getAuthenticatedUser());
        request.setDispensedDate(new Date());
        return departmentBulkRequestDao.saveRequest(request);
    }

    @Override
    public DepartmentStockIssue recordIssue(PharmacyLocation departmentLocation, Integer itemId, BigDecimal quantity,
                                            String batchNumber, String issuedTo, String reason) {
        requireDepartmentStore(departmentLocation);
        if (!MODE_FULL_VISIBILITY.equals(resolveWorkflowMode())) {
            throw new IllegalArgumentException("Department stock issue recording is enabled only in full visibility mode");
        }
        if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Issue item and positive quantity are required");
        }
        List<ConsumedBalance> consumedBalances = decrementBalances(itemId, departmentLocation.getPharmacyLocationId(),
                batchNumber, quantity);
        DepartmentStockIssue issue = new DepartmentStockIssue();
        issue.setDepartmentLocation(departmentLocation);
        issue.setItem(pharmacyService.getItem(itemId));
        issue.setQuantity(quantity);
        issue.setBatchNumber(trimToNull(batchNumber));
        issue.setIssuedTo(trimToNull(issuedTo));
        issue.setReason(trimToNull(reason));
        issue.setIssuedBy(Context.getAuthenticatedUser());
        issue.setCreator(Context.getAuthenticatedUser());
        issue.setDateCreated(new Date());
        issue.setVoided(Boolean.FALSE);
        issue.setUuid(UUID.randomUUID().toString());
        departmentBulkRequestDao.saveIssue(issue);
        for (ConsumedBalance consumed : consumedBalances) {
            saveTransaction(itemId, departmentLocation.getPharmacyLocationId(), consumed.batchNumber, consumed.expiryDate,
                    consumed.quantity.negate(), StockTransactionType.DEPARTMENT_STOCK_ISSUE,
                    issue.getDepartmentStockIssueId() == null ? null : issue.getDepartmentStockIssueId().toString(),
                    "Department issue: " + nullToEmpty(reason), consumed.unitPrice);
        }
        return issue;
    }

    @Override
    public List<DepartmentStockIssue> recordIssues(PharmacyLocation departmentLocation,
                                                   List<DepartmentStockIssueLineCommand> commands,
                                                   String issuedTo, String reason) {
        requireDepartmentStore(departmentLocation);
        if (!MODE_FULL_VISIBILITY.equals(resolveWorkflowMode())) {
            throw new IllegalArgumentException("Department stock issue recording is enabled only in full visibility mode");
        }
        List<DepartmentStockIssueLineCommand> preparedCommands = normalizeIssueCommands(commands);
        if (preparedCommands.isEmpty()) {
            throw new IllegalArgumentException("At least one issue line is required");
        }
        List<DepartmentStockIssue> issues = new ArrayList<DepartmentStockIssue>();
        for (DepartmentStockIssueLineCommand command : preparedCommands) {
            issues.add(recordIssue(departmentLocation, command.getItemId(), command.getQuantity(),
                    command.getBatchNumber(), issuedTo, reason));
        }
        return issues;
    }

    private void transferToDepartment(DepartmentBulkRequest request, DepartmentBulkRequestLine line,
                                      BigDecimal quantity, String batchNumber) {
        List<ConsumedBalance> consumedBalances = decrementBalances(line.getItem().getItemId(),
                request.getSupplyingPharmacyLocation().getPharmacyLocationId(), batchNumber, quantity);
        for (ConsumedBalance consumed : consumedBalances) {
            incrementBalance(line.getItem().getItemId(), request.getDepartmentLocation().getPharmacyLocationId(),
                    consumed.batchNumber, consumed.expiryDate, consumed.quantity, consumed.unitPrice);
            saveTransaction(line.getItem().getItemId(), request.getSupplyingPharmacyLocation().getPharmacyLocationId(),
                    consumed.batchNumber, consumed.expiryDate, consumed.quantity.negate(),
                    StockTransactionType.DEPARTMENT_BULK_TRANSFER,
                    request.getDepartmentBulkRequestId() == null ? null : request.getDepartmentBulkRequestId().toString(),
                    "Department bulk transfer out", consumed.unitPrice);
            saveTransaction(line.getItem().getItemId(), request.getDepartmentLocation().getPharmacyLocationId(),
                    consumed.batchNumber, consumed.expiryDate, consumed.quantity,
                    StockTransactionType.DEPARTMENT_BULK_TRANSFER,
                    request.getDepartmentBulkRequestId() == null ? null : request.getDepartmentBulkRequestId().toString(),
                    "Department bulk transfer in", consumed.unitPrice);
        }
    }

    private List<PreparedLine> normalizeCommands(List<DepartmentBulkRequestLineCommand> commands) {
        Map<Integer, PreparedLine> linesByItemId = new LinkedHashMap<Integer, PreparedLine>();
        if (commands == null) {
            return new ArrayList<PreparedLine>();
        }
        for (DepartmentBulkRequestLineCommand command : commands) {
            if (command == null || command.getItemId() == null || command.getRequestedQuantity() == null
                    || command.getRequestedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PreparedLine preparedLine = linesByItemId.get(command.getItemId());
            if (preparedLine == null) {
                preparedLine = new PreparedLine();
                preparedLine.item = pharmacyService.getItem(command.getItemId());
                if (preparedLine.item == null) {
                    throw new IllegalArgumentException("Requested item not found");
                }
                if (!Boolean.TRUE.equals(preparedLine.item.getVisibleToRequesters())) {
                    throw new IllegalArgumentException("Requested item is hidden from requesters");
                }
                preparedLine.previousRequestedQuantity = zeroIfNull(command.getPreviousRequestedQuantity());
                preparedLine.previousReceivedQuantity = zeroIfNull(command.getPreviousReceivedQuantity());
                preparedLine.previousUsedQuantity = zeroIfNull(command.getPreviousUsedQuantity());
                preparedLine.currentRemainingQuantity = zeroIfNull(command.getCurrentRemainingQuantity());
                preparedLine.requestedQuantity = BigDecimal.ZERO;
                preparedLine.lineJustification = command.getLineJustification();
                linesByItemId.put(command.getItemId(), preparedLine);
            }
            preparedLine.requestedQuantity = preparedLine.requestedQuantity.add(command.getRequestedQuantity());
        }
        return new ArrayList<PreparedLine>(linesByItemId.values());
    }

    private List<DepartmentStockIssueLineCommand> normalizeIssueCommands(List<DepartmentStockIssueLineCommand> commands) {
        List<DepartmentStockIssueLineCommand> preparedCommands = new ArrayList<DepartmentStockIssueLineCommand>();
        if (commands == null) {
            return preparedCommands;
        }
        for (DepartmentStockIssueLineCommand command : commands) {
            if (command == null || command.getItemId() == null || command.getQuantity() == null
                    || command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (pharmacyService.getItem(command.getItemId()) == null) {
                throw new IllegalArgumentException("Issue item was not found");
            }
            preparedCommands.add(new DepartmentStockIssueLineCommand(command.getItemId(), command.getQuantity(),
                    trimToNull(command.getBatchNumber())));
        }
        return preparedCommands;
    }

    private DepartmentBulkRequest requireRequest(Integer requestId) {
        DepartmentBulkRequest request = departmentBulkRequestDao.getRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Department request not found");
        }
        return request;
    }

    private DepartmentBulkRequestLine findRequestLine(DepartmentBulkRequest request, Integer requestLineId) {
        if (request == null || request.getLines() == null || requestLineId == null) {
            return null;
        }
        for (DepartmentBulkRequestLine line : request.getLines()) {
            if (line != null && requestLineId.equals(line.getDepartmentBulkRequestLineId())) {
                return line;
            }
        }
        return null;
    }

    private boolean hasActiveRequestLines(DepartmentBulkRequest request) {
        if (request == null || request.getLines() == null) {
            return false;
        }
        for (DepartmentBulkRequestLine line : request.getLines()) {
            if (isActiveRequestLine(line)) {
                return true;
            }
        }
        return false;
    }

    private boolean isActiveRequestLine(DepartmentBulkRequestLine line) {
        return line != null && !Boolean.TRUE.equals(line.getVoided());
    }

    private void requireDepartmentStore(PharmacyLocation departmentLocation) {
        if (departmentLocation == null || departmentLocation.getPharmacyLocationId() == null
                || departmentLocation.getLocationType() != PharmacyLocationType.DEPARTMENT_STORE) {
            throw new IllegalArgumentException("Department store location is required");
        }
    }

    private String resolveWorkflowMode() {
        String configured = administrationService == null ? null : administrationService.getGlobalProperty(GP_WORKFLOW_MODE,
                MODE_PERIODIC_ACCOUNTABILITY);
        return MODE_FULL_VISIBILITY.equalsIgnoreCase(configured) ? MODE_FULL_VISIBILITY : MODE_PERIODIC_ACCOUNTABILITY;
    }

    private Map<Integer, BigDecimal> quantitiesByLineId(Integer[] lineIds, BigDecimal[] quantities) {
        Map<Integer, BigDecimal> values = new LinkedHashMap<Integer, BigDecimal>();
        if (lineIds == null || quantities == null) {
            return values;
        }
        int count = Math.min(lineIds.length, quantities.length);
        for (int i = 0; i < count; i++) {
            if (lineIds[i] != null && quantities[i] != null) {
                values.put(lineIds[i], quantities[i]);
            }
        }
        return values;
    }

    private Map<Integer, List<DispenseAllocation>> allocationsByLineId(Integer[] lineIds, BigDecimal[] quantities,
                                                                       String[] batchNumbers) {
        Map<Integer, List<DispenseAllocation>> mapped = new LinkedHashMap<Integer, List<DispenseAllocation>>();
        if (lineIds == null || quantities == null) {
            return mapped;
        }
        int count = Math.min(lineIds.length, quantities.length);
        for (int i = 0; i < count; i++) {
            if (lineIds[i] == null || quantities[i] == null) {
                continue;
            }
            String batchNumber = batchNumbers == null || batchNumbers.length <= i ? null : batchNumbers[i];
            List<DispenseAllocation> allocations = mapped.get(lineIds[i]);
            if (allocations == null) {
                allocations = new ArrayList<DispenseAllocation>();
                mapped.put(lineIds[i], allocations);
            }
            allocations.add(new DispenseAllocation(zeroIfNull(quantities[i]), trimToNull(batchNumber)));
        }
        return mapped;
    }

    private BigDecimal totalAllocated(List<DispenseAllocation> allocations) {
        BigDecimal total = BigDecimal.ZERO;
        for (DispenseAllocation allocation : allocations) {
            if (allocation != null) {
                total = total.add(zeroIfNull(allocation.quantity));
            }
        }
        return total;
    }

    private String batchSummary(List<DispenseAllocation> allocations) {
        List<String> batches = new ArrayList<String>();
        for (DispenseAllocation allocation : allocations) {
            if (allocation == null || allocation.quantity == null || allocation.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String batchNumber = trimToNull(allocation.batchNumber);
            if (batchNumber == null) {
                batchNumber = "FEFO";
            }
            if (!batches.contains(batchNumber)) {
                batches.add(batchNumber);
            }
        }
        if (batches.isEmpty()) {
            return null;
        }
        if (batches.size() == 1) {
            return batches.get(0);
        }
        StringBuilder summary = new StringBuilder();
        for (String batch : batches) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(batch);
        }
        return summary.toString();
    }

    private List<ConsumedBalance> decrementBalances(Integer itemId, Integer pharmacyLocationId, String batchNumber, BigDecimal quantity) {
        List<ConsumedBalance> consumedBalances = new ArrayList<ConsumedBalance>();
        if (batchNumber != null && !batchNumber.trim().isEmpty()) {
            InventoryBalance balance = decrementBalance(itemId, pharmacyLocationId, batchNumber, quantity);
            consumedBalances.add(consumedBalance(balance, quantity));
            return consumedBalances;
        }
        BigDecimal remaining = quantity;
        for (InventoryBalance balance : inventoryDao.getInventoryForPharmacy(pharmacyLocationId)) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || !itemId.equals(balance.getItem().getItemId())) {
                continue;
            }
            BigDecimal available = zeroIfNull(balance.getQuantityOnHand());
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal deducted = available.min(remaining);
            balance.setQuantityOnHand(available.subtract(deducted));
            inventoryDao.saveInventoryBalance(balance);
            consumedBalances.add(consumedBalance(balance, deducted));
            remaining = remaining.subtract(deducted);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Insufficient stock for requested department transaction");
        }
        return consumedBalances;
    }

    private InventoryBalance decrementBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber, BigDecimal quantity) {
        InventoryBalance balance = inventoryDao.getInventoryBalance(itemId, pharmacyLocationId, batchNumber);
        if (balance == null || zeroIfNull(balance.getQuantityOnHand()).compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Insufficient stock for requested department transaction");
        }
        balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(quantity));
        inventoryDao.saveInventoryBalance(balance);
        return balance;
    }

    private void incrementBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber, Date expiryDate,
                                  BigDecimal quantity, BigDecimal unitPrice) {
        InventoryBalance balance = inventoryDao.getInventoryBalance(itemId, pharmacyLocationId, batchNumber);
        if (balance == null) {
            balance = new InventoryBalance();
            Item item = new Item();
            item.setItemId(itemId);
            PharmacyLocation pharmacyLocation = new PharmacyLocation();
            pharmacyLocation.setPharmacyLocationId(pharmacyLocationId);
            balance.setItem(item);
            balance.setPharmacyLocation(pharmacyLocation);
            balance.setBatchNumber(trimToNull(batchNumber));
            balance.setExpiryDate(expiryDate);
            balance.setQuantityOnHand(BigDecimal.ZERO);
            balance.setUnitPrice(unitPrice);
            balance.setCreator(Context.getAuthenticatedUser());
            balance.setDateCreated(new Date());
            balance.setVoided(Boolean.FALSE);
        }
        if (unitPrice != null) {
            balance.setUnitPrice(unitPrice);
        }
        balance.setQuantityOnHand(zeroIfNull(balance.getQuantityOnHand()).add(quantity));
        inventoryDao.saveInventoryBalance(balance);
    }

    private void saveTransaction(Integer itemId, Integer pharmacyLocationId, String batchNumber, Date expiryDate,
                                 BigDecimal quantity, StockTransactionType transactionType, String referenceNumber,
                                 String remarks, BigDecimal unitPrice) {
        StockTransaction transaction = new StockTransaction();
        Item item = new Item();
        item.setItemId(itemId);
        PharmacyLocation pharmacyLocation = new PharmacyLocation();
        pharmacyLocation.setPharmacyLocationId(pharmacyLocationId);
        transaction.setItem(item);
        transaction.setPharmacyLocation(pharmacyLocation);
        transaction.setTransactionType(transactionType);
        transaction.setBatchNumber(trimToNull(batchNumber));
        transaction.setExpiryDate(expiryDate);
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(unitPrice);
        transaction.setTotalAmount(unitPrice == null || quantity == null ? null : unitPrice.multiply(quantity.abs()));
        transaction.setReferenceNumber(referenceNumber);
        transaction.setRemarks(remarks);
        transaction.setPerformedBy(Context.getAuthenticatedUser());
        transaction.setCreator(Context.getAuthenticatedUser());
        transaction.setDateCreated(new Date());
        transaction.setVoided(Boolean.FALSE);
        inventoryDao.saveStockTransaction(transaction);
    }

    private ConsumedBalance consumedBalance(InventoryBalance balance, BigDecimal quantity) {
        ConsumedBalance consumed = new ConsumedBalance();
        consumed.batchNumber = balance == null ? null : balance.getBatchNumber();
        consumed.expiryDate = balance == null ? null : balance.getExpiryDate();
        consumed.quantity = quantity;
        consumed.unitPrice = balance == null ? null : balance.getUnitPrice();
        return consumed;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class PreparedLine {
        private Item item;
        private BigDecimal previousRequestedQuantity;
        private BigDecimal previousReceivedQuantity;
        private BigDecimal previousUsedQuantity;
        private BigDecimal currentRemainingQuantity;
        private BigDecimal requestedQuantity;
        private String lineJustification;
    }

    private static class ConsumedBalance {
        private String batchNumber;
        private Date expiryDate;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }

    private static class DispenseAllocation {
        private BigDecimal quantity;
        private String batchNumber;

        private DispenseAllocation(BigDecimal quantity, String batchNumber) {
            this.quantity = quantity;
            this.batchNumber = batchNumber;
        }
    }
}
