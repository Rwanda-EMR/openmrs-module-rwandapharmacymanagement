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
    public long countPendingRequestsForPharmacy(PharmacyLocation pharmacyLocation) {
        return pharmacyItemRequestDao.countPendingRequestsForPharmacy(pharmacyLocation);
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
    public PharmacyItemRequest getRequest(Integer requestId) {
        return pharmacyItemRequestDao.getRequest(requestId);
    }

    @Override
    public PharmacyItemRequest approveRequest(Integer requestId, PharmacyLocation activePharmacy,
                                              Integer[] lineIds, BigDecimal[] approvedQuantities) {
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
            throw new IllegalArgumentException("Only requested pharmacy item requests can be approved");
        }
        Map<Integer, BigDecimal> approvedByLineId = quantitiesByLineId(lineIds, approvedQuantities);
        Map<Integer, List<InventoryBalance>> balancesByItemId = balancesByItemId(activePharmacy);
        User currentUser = Context.getAuthenticatedUser();
        Date now = new Date();
        for (PharmacyItemRequestLine line : request.getLines()) {
            if (line == null || line.getItem() == null || line.getItem().getItemId() == null) {
                continue;
            }
            BigDecimal requestedQuantity = line.getRequestedQuantity() == null ? BigDecimal.ZERO : line.getRequestedQuantity();
            BigDecimal approvedQuantity = approvedByLineId.containsKey(line.getPharmacyItemRequestLineId())
                    ? approvedByLineId.get(line.getPharmacyItemRequestLineId()) : requestedQuantity;
            if (approvedQuantity.compareTo(BigDecimal.ZERO) < 0 || approvedQuantity.compareTo(requestedQuantity) > 0) {
                throw new IllegalArgumentException("Approved quantity must be between zero and requested quantity");
            }
            line.setApprovedQuantity(approvedQuantity);
            pharmacyItemRequestDao.saveRequestLine(line);
            consumeStockFifo(balancesByItemId.get(line.getItem().getItemId()), approvedQuantity, line, request, activePharmacy, currentUser, now);
        }
        request.setApprovedBy(currentUser);
        request.setApprovedDate(now);
        request.setRequestStatus("APPROVED");
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

    private void consumeStockFifo(List<InventoryBalance> itemBalances, BigDecimal quantityToConsume,
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
            throw new IllegalArgumentException("Insufficient stock for " + (line.getItem() == null ? "selected item" : line.getItem().getName()));
        }
    }

    private static class PreparedLine {
        private Integer itemId;
        private BigDecimal quantity;
    }
}
