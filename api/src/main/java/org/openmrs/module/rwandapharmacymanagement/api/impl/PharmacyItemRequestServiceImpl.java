package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyItemRequestService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PharmacyItemRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.db.PharmacyItemRequestDao;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyItemRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Transactional
public class PharmacyItemRequestServiceImpl extends BaseOpenmrsService implements PharmacyItemRequestService {

    private PharmacyItemRequestDao pharmacyItemRequestDao;
    private InventoryDao inventoryDao;
    private PharmacyService pharmacyService;

    public void setPharmacyItemRequestDao(PharmacyItemRequestDao pharmacyItemRequestDao) {
        this.pharmacyItemRequestDao = pharmacyItemRequestDao;
    }

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @Override
    public PharmacyItemRequest createRequest(PharmacyLocation requestedPharmacyLocation, String reason,
                                             List<PharmacyItemRequestLineCommand> commands) {
        if (requestedPharmacyLocation == null || requestedPharmacyLocation.getPharmacyLocationId() == null) {
            throw new IllegalArgumentException("Requested pharmacy is required");
        }
        List<PreparedLine> preparedLines = normalizeCommands(commands);
        if (preparedLines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one item");
        }
        User currentUser = Context.getAuthenticatedUser();
        Date now = new Date();
        PharmacyItemRequest request = new PharmacyItemRequest();
        request.setRequestedPharmacyLocation(requestedPharmacyLocation);
        request.setReason(reason);
        request.setRequestStatus("REQUESTED");
        request.setCreator(currentUser);
        request.setDateCreated(now);
        request.setLines(new ArrayList<PharmacyItemRequestLine>());
        for (PreparedLine preparedLine : preparedLines) {
            Item item = pharmacyService.getItem(preparedLine.itemId);
            if (item == null) {
                throw new IllegalArgumentException("Selected item was not found");
            }
            if (!Boolean.TRUE.equals(item.getVisibleToRequesters())) {
                throw new IllegalArgumentException("Selected item is hidden from requesters");
            }
            PharmacyItemRequestLine line = new PharmacyItemRequestLine();
            line.setPharmacyItemRequest(request);
            line.setItem(item);
            line.setRequestedQuantity(preparedLine.quantity);
            line.setCreator(currentUser);
            line.setDateCreated(now);
            request.getLines().add(line);
        }
        return pharmacyItemRequestDao.saveRequest(request);
    }

    @Override
    public List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation) {
        return pharmacyItemRequestDao.getPendingRequestsForPharmacy(pharmacyLocation);
    }

    @Override
    public List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, int offset, int limit) {
        return pharmacyItemRequestDao.getPendingRequestsForPharmacy(pharmacyLocation, offset, limit);
    }

    @Override
    public List<PharmacyItemRequest> getPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, String search,
                                                                   int offset, int limit) {
        return pharmacyItemRequestDao.getPendingRequestsForPharmacy(pharmacyLocation, search, offset, limit);
    }

    @Override
    public long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation) {
        return pharmacyItemRequestDao.countPendingRequestsForPharmacy(pharmacyLocation);
    }

    @Override
    public long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation, String search) {
        return pharmacyItemRequestDao.countPendingRequestsForPharmacy(pharmacyLocation, search);
    }

    @Override
    public List<PharmacyItemRequest> getRequestsByCreator(User creator, int offset, int limit) {
        return pharmacyItemRequestDao.getRequestsByCreator(creator, offset, limit);
    }

    @Override
    public long countRequestsByCreator(User creator) {
        return pharmacyItemRequestDao.countRequestsByCreator(creator);
    }

    @Override
    public List<PharmacyItemRequest> getRequestsForPharmacy(PharmacyLocation pharmacyLocation, Date fromInclusive,
                                                            Date toExclusive, String status, String itemName,
                                                            int maxResults) {
        return pharmacyItemRequestDao.getRequestsForPharmacy(pharmacyLocation, fromInclusive, toExclusive, status,
                itemName, maxResults);
    }

    @Override
    public PharmacyItemRequest getRequest(Integer requestId) {
        return pharmacyItemRequestDao.getRequest(requestId);
    }

    @Override
    public PharmacyItemRequest approveRequest(Integer requestId, PharmacyLocation activePharmacy,
                                              Integer[] lineIds, BigDecimal[] approvedQuantities, String[] batchNumbers) {
        PharmacyItemRequest request = requireRequestedRequest(requestId, activePharmacy);
        Map<Integer, BigDecimal> approvedByLineId = quantitiesByLineId(lineIds, approvedQuantities);
        Map<Integer, String> batchNumberByLineId = batchNumbersByLineId(lineIds, batchNumbers);
        Map<Integer, List<InventoryBalance>> balancesByItemId = balancesByItemId(activePharmacy);
        User currentUser = Context.getAuthenticatedUser();
        Date now = new Date();
        boolean hasActiveApprovedLine = false;
        for (PharmacyItemRequestLine line : request.getLines()) {
            if (line == null || line.getItem() == null || line.getItem().getItemId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(line.getVoided())) {
                continue;
            }
            BigDecimal requestedQuantity = line.getRequestedQuantity() == null ? BigDecimal.ZERO : line.getRequestedQuantity();
            BigDecimal approvedQuantity = approvedByLineId.containsKey(line.getPharmacyItemRequestLineId())
                    ? approvedByLineId.get(line.getPharmacyItemRequestLineId()) : requestedQuantity;
            if (approvedQuantity.compareTo(BigDecimal.ZERO) < 0 || approvedQuantity.compareTo(requestedQuantity) > 0) {
                throw new IllegalArgumentException("Approved quantity must be between zero and requested quantity");
            }
            if (approvedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                hasActiveApprovedLine = true;
            }
            line.setApprovedQuantity(approvedQuantity);
            pharmacyItemRequestDao.saveRequestLine(line);
            consumeStock(balancesByItemId.get(line.getItem().getItemId()), approvedQuantity,
                    batchNumberByLineId.get(line.getPharmacyItemRequestLineId()), line, request, activePharmacy, currentUser, now);
        }
        if (!hasActiveApprovedLine) {
            throw new IllegalArgumentException("At least one active request line must have an approved quantity");
        }
        request.setApprovedBy(currentUser);
        request.setApprovedDate(now);
        request.setRequestStatus("APPROVED");
        return pharmacyItemRequestDao.saveRequest(request);
    }

    @Override
    public PharmacyItemRequest rejectRequest(Integer requestId, PharmacyLocation activePharmacy, String reason) {
        PharmacyItemRequest request = requireRequestedRequest(requestId, activePharmacy);
        String trimmedReason = trimToNull(reason);
        if (trimmedReason == null) {
            throw new IllegalArgumentException("Reason is required when rejecting a request");
        }
        User currentUser = Context.getAuthenticatedUser();
        request.setRequestStatus("REJECTED");
        request.setApprovedBy(currentUser);
        request.setApprovedDate(new Date());
        request.setVoidReason(trimmedReason);
        return pharmacyItemRequestDao.saveRequest(request);
    }

    @Override
    public PharmacyItemRequest removeRequestLine(Integer requestId, PharmacyLocation activePharmacy,
                                                 Integer requestLineId, String reason) {
        PharmacyItemRequest request = requireRequestedRequest(requestId, activePharmacy);
        String trimmedReason = trimToNull(reason);
        if (trimmedReason == null) {
            throw new IllegalArgumentException("Reason is required when removing a request line");
        }
        PharmacyItemRequestLine line = findRequestLine(request, requestLineId);
        if (line == null) {
            throw new IllegalArgumentException("Pharmacy item request line not found");
        }
        if (!Boolean.TRUE.equals(line.getVoided())) {
            line.setVoided(Boolean.TRUE);
            line.setVoidedBy(Context.getAuthenticatedUser());
            line.setDateVoided(new Date());
            line.setVoidReason(trimmedReason);
            pharmacyItemRequestDao.saveRequestLine(line);
        }
        if (!hasActiveRequestLines(request)) {
            request.setRequestStatus("REJECTED");
            request.setApprovedBy(Context.getAuthenticatedUser());
            request.setApprovedDate(new Date());
            request.setVoidReason("All request lines removed. " + trimmedReason);
        }
        return pharmacyItemRequestDao.saveRequest(request);
    }

    private List<PreparedLine> normalizeCommands(List<PharmacyItemRequestLineCommand> commands) {
        Map<Integer, PreparedLine> byItem = new LinkedHashMap<Integer, PreparedLine>();
        if (commands == null) {
            return new ArrayList<PreparedLine>();
        }
        for (PharmacyItemRequestLineCommand command : commands) {
            if (command == null || command.getItemId() == null || command.getQuantity() == null
                    || command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PreparedLine line = byItem.get(command.getItemId());
            if (line == null) {
                line = new PreparedLine();
                line.itemId = command.getItemId();
                line.quantity = BigDecimal.ZERO;
                byItem.put(command.getItemId(), line);
            }
            line.quantity = line.quantity.add(command.getQuantity());
        }
        return new ArrayList<PreparedLine>(byItem.values());
    }

    private Map<Integer, List<InventoryBalance>> balancesByItemId(PharmacyLocation pharmacyLocation) {
        Map<Integer, List<InventoryBalance>> balancesByItemId = new LinkedHashMap<Integer, List<InventoryBalance>>();
        for (InventoryBalance balance : inventoryDao.getInventoryForPharmacy(pharmacyLocation.getPharmacyLocationId())) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            if (!balancesByItemId.containsKey(itemId)) {
                balancesByItemId.put(itemId, new ArrayList<InventoryBalance>());
            }
            balancesByItemId.get(itemId).add(balance);
        }
        return balancesByItemId;
    }

    private Map<Integer, BigDecimal> quantitiesByLineId(Integer[] lineIds, BigDecimal[] approvedQuantities) {
        Map<Integer, BigDecimal> quantitiesByLineId = new LinkedHashMap<Integer, BigDecimal>();
        if (lineIds == null || approvedQuantities == null) {
            return quantitiesByLineId;
        }
        for (int i = 0; i < lineIds.length; i++) {
            Integer lineId = lineIds[i];
            BigDecimal quantity = approvedQuantities.length > i ? approvedQuantities[i] : null;
            if (lineId != null && quantity != null) {
                quantitiesByLineId.put(lineId, quantity);
            }
        }
        return quantitiesByLineId;
    }

    private Map<Integer, String> batchNumbersByLineId(Integer[] lineIds, String[] batchNumbers) {
        Map<Integer, String> batchNumbersByLineId = new LinkedHashMap<Integer, String>();
        if (lineIds == null || batchNumbers == null) {
            return batchNumbersByLineId;
        }
        for (int i = 0; i < lineIds.length; i++) {
            Integer lineId = lineIds[i];
            String batchNumber = batchNumbers.length > i ? normalizeBatchNumber(batchNumbers[i]) : null;
            if (lineId != null && batchNumber != null) {
                batchNumbersByLineId.put(lineId, batchNumber);
            }
        }
        return batchNumbersByLineId;
    }

    private PharmacyItemRequest requireRequestedRequest(Integer requestId, PharmacyLocation activePharmacy) {
        if (activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            throw new IllegalArgumentException("Active pharmacy is required");
        }
        PharmacyItemRequest request = pharmacyItemRequestDao.getRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Pharmacy item request not found");
        }
        if (request.getRequestedPharmacyLocation() == null || request.getRequestedPharmacyLocation().getPharmacyLocationId() == null
                || !activePharmacy.getPharmacyLocationId().equals(request.getRequestedPharmacyLocation().getPharmacyLocationId())) {
            throw new IllegalArgumentException("Selected request does not belong to the active pharmacy");
        }
        if (!"REQUESTED".equalsIgnoreCase(request.getRequestStatus())) {
            throw new IllegalArgumentException("Only requested pharmacy item requests can be served, changed, or rejected");
        }
        return request;
    }

    private PharmacyItemRequestLine findRequestLine(PharmacyItemRequest request, Integer requestLineId) {
        if (request == null || requestLineId == null || request.getLines() == null) {
            return null;
        }
        for (PharmacyItemRequestLine line : request.getLines()) {
            if (line != null && requestLineId.equals(line.getPharmacyItemRequestLineId())) {
                return line;
            }
        }
        return null;
    }

    private boolean hasActiveRequestLines(PharmacyItemRequest request) {
        if (request == null || request.getLines() == null) {
            return false;
        }
        for (PharmacyItemRequestLine line : request.getLines()) {
            if (line != null && !Boolean.TRUE.equals(line.getVoided())) {
                return true;
            }
        }
        return false;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void consumeStock(List<InventoryBalance> itemBalances, BigDecimal quantityToConsume, String selectedBatchNumber,
                              PharmacyItemRequestLine line, PharmacyItemRequest request,
                              PharmacyLocation pharmacyLocation, User currentUser, Date when) {
        if (quantityToConsume == null || quantityToConsume.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal remaining = quantityToConsume;
        if (itemBalances != null) {
            for (InventoryBalance balance : itemBalances) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
                BigDecimal available = balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (selectedBatchNumber != null && !selectedBatchNumber.equals(normalizeStoredBatchNumber(balance.getBatchNumber()))) {
                    continue;
                }
                BigDecimal deducted = remaining.min(available);
                balance.setQuantityOnHand(available.subtract(deducted));
                inventoryDao.saveInventoryBalance(balance);
                StockTransaction transaction = new StockTransaction();
                transaction.setItem(line.getItem());
                transaction.setPharmacyLocation(pharmacyLocation);
                transaction.setBatchNumber(balance.getBatchNumber());
                transaction.setExpiryDate(balance.getExpiryDate());
                transaction.setQuantity(deducted.negate());
                transaction.setTransactionType(StockTransactionType.DISPENSE);
                transaction.setCreator(currentUser);
                transaction.setPerformedBy(currentUser);
                transaction.setDateCreated(when);
                transaction.setRemarks("Issued to user pharmacy request #" + request.getPharmacyItemRequestId());
                inventoryDao.saveStockTransaction(transaction);
                remaining = remaining.subtract(deducted);
            }
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            String batchText = selectedBatchNumber == null ? "" : " in selected batch";
            throw new IllegalArgumentException("Insufficient stock" + batchText + " for "
                    + (line.getItem() == null ? "selected item" : line.getItem().getName()));
        }
    }

    private String normalizeBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.trim().isEmpty() || "__FIFO__".equals(batchNumber)) {
            return null;
        }
        if ("__NO_BATCH__".equals(batchNumber)) {
            return "";
        }
        return batchNumber.trim();
    }

    private String normalizeStoredBatchNumber(String batchNumber) {
        return batchNumber == null || batchNumber.trim().isEmpty() ? "" : batchNumber.trim();
    }

    private static class PreparedLine {
        private Integer itemId;
        private BigDecimal quantity;
    }
}
