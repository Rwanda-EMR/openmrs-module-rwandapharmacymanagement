package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.Location;
import org.openmrs.Concept;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.LabSupplyService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.LabSupplyRequestLineCommand;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.db.LabSupplyDao;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequest;
import org.openmrs.module.rwandapharmacymanagement.model.LabSupplyRequestLine;
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
public class LabSupplyServiceImpl extends BaseOpenmrsService implements LabSupplyService {

    private LabSupplyDao labSupplyDao;
    private InventoryDao inventoryDao;
    private PharmacyService pharmacyService;

    public void setLabSupplyDao(LabSupplyDao labSupplyDao) {
        this.labSupplyDao = labSupplyDao;
    }

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @Override
    public LabSupplyRequest createRequest(Location labUnit, List<LabSupplyRequestLineCommand> commands) {
        if (labUnit == null) {
            throw new IllegalArgumentException("Current laboratory unit is required");
        }
        List<PreparedLabSupplyLine> preparedLines = normalizeCommands(commands);
        if (preparedLines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one lab supply item");
        }
        User currentUser = Context.getAuthenticatedUser();
        Date now = new Date();
        LabSupplyRequest request = new LabSupplyRequest();
        request.setLabUnit(labUnit);
        request.setRequestStatus("REQUESTED");
        request.setCreator(currentUser);
        request.setDateCreated(now);
        request.setLines(new ArrayList<LabSupplyRequestLine>());
        for (PreparedLabSupplyLine preparedLine : preparedLines) {
            Item item = pharmacyService.getItem(preparedLine.itemId);
            if (item == null) {
                throw new IllegalArgumentException("Selected item was not found");
            }
            Concept labServiceConcept = Context.getConceptService().getConcept(preparedLine.labServiceConceptId);
            if (labServiceConcept == null) {
                throw new IllegalArgumentException("Selected lab service was not found");
            }
            LabSupplyRequestLine line = new LabSupplyRequestLine();
            line.setLabSupplyRequest(request);
            line.setItem(item);
            line.setLabServiceConcept(labServiceConcept);
            line.setRequestedQuantity(preparedLine.quantity);
            line.setCreator(currentUser);
            line.setDateCreated(now);
            request.getLines().add(line);
        }
        return labSupplyDao.saveRequest(request);
    }

    @Override
    public List<LabSupplyRequest> getPendingRequests() {
        return labSupplyDao.getPendingRequests();
    }

    @Override
    public long countPendingRequests() {
        return labSupplyDao.countPendingRequests();
    }

    @Override
    public List<LabSupplyRequest> getRequestsByCreator(User creator) {
        return labSupplyDao.getRequestsByCreator(creator);
    }

    @Override
    public List<LabSupplyRequest> getRequestsByCreator(User creator, int offset, int limit) {
        return labSupplyDao.getRequestsByCreator(creator, offset, limit);
    }

    @Override
    public long countRequestsByCreator(User creator) {
        return labSupplyDao.countRequestsByCreator(creator);
    }

    @Override
    public List<LabSupplyRequest> getApprovedRequestsByPharmacy(PharmacyLocation pharmacyLocation) {
        return labSupplyDao.getApprovedRequestsByPharmacy(pharmacyLocation);
    }

    @Override
    public LabSupplyRequest getRequest(Integer requestId) {
        return labSupplyDao.getRequest(requestId);
    }

    @Override
    public LabSupplyRequest approveRequest(Integer requestId, PharmacyLocation dispensingPharmacyLocation,
                                           Integer[] lineIds, BigDecimal[] approvedQuantities, String[] batchNumbers) {
        if (dispensingPharmacyLocation == null || dispensingPharmacyLocation.getPharmacyLocationId() == null) {
            throw new IllegalArgumentException("Dispensing pharmacy is required");
        }
        LabSupplyRequest request = labSupplyDao.getRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Lab supply request not found");
        }
        if (!"REQUESTED".equalsIgnoreCase(request.getRequestStatus())) {
            throw new IllegalArgumentException("Only requested lab supply items can be approved");
        }
        Map<Integer, BigDecimal> approvedByLineId = quantitiesByLineId(lineIds, approvedQuantities);
        Map<Integer, String> batchNumberByLineId = batchNumbersByLineId(lineIds, batchNumbers);
        List<InventoryBalance> balances = inventoryDao.getInventoryForPharmacy(dispensingPharmacyLocation.getPharmacyLocationId());
        Map<Integer, List<InventoryBalance>> balancesByItemId = new LinkedHashMap<Integer, List<InventoryBalance>>();
        Map<Integer, BigDecimal> availableByItemId = new LinkedHashMap<Integer, BigDecimal>();
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            if (!balancesByItemId.containsKey(itemId)) {
                balancesByItemId.put(itemId, new ArrayList<InventoryBalance>());
                availableByItemId.put(itemId, BigDecimal.ZERO);
            }
            balancesByItemId.get(itemId).add(balance);
            availableByItemId.put(itemId, availableByItemId.get(itemId).add(balance.getQuantityOnHand()));
        }
        User currentUser = Context.getAuthenticatedUser();
        Date now = new Date();
        for (LabSupplyRequestLine line : request.getLines()) {
            if (line == null || line.getItem() == null || line.getItem().getItemId() == null) {
                continue;
            }
            BigDecimal requestedQuantity = line.getRequestedQuantity() == null ? BigDecimal.ZERO : line.getRequestedQuantity();
            BigDecimal approvedQuantity = approvedByLineId.containsKey(line.getLabSupplyRequestLineId())
                    ? approvedByLineId.get(line.getLabSupplyRequestLineId()) : requestedQuantity;
            if (approvedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Approved quantity cannot be negative");
            }
            if (approvedQuantity.compareTo(requestedQuantity) > 0) {
                throw new IllegalArgumentException("Approved quantity cannot exceed requested quantity for " + line.getItem().getName());
            }
            Integer itemId = line.getItem().getItemId();
            BigDecimal availableQuantity = availableByItemId.containsKey(itemId) ? availableByItemId.get(itemId) : BigDecimal.ZERO;
            if (approvedQuantity.compareTo(availableQuantity) > 0) {
                throw new IllegalArgumentException("Insufficient stock for " + line.getItem().getName() + " at " + dispensingPharmacyLocation.getName());
            }
            line.setApprovedQuantity(approvedQuantity);
            labSupplyDao.saveRequestLine(line);
            consumeStock(balancesByItemId.get(itemId), approvedQuantity,
                    batchNumberByLineId.get(line.getLabSupplyRequestLineId()), line, request, dispensingPharmacyLocation, currentUser, now);
            availableByItemId.put(itemId, availableQuantity.subtract(approvedQuantity));
        }
        request.setDispensingPharmacyLocation(dispensingPharmacyLocation);
        request.setApprovedBy(currentUser);
        request.setApprovedDate(now);
        request.setRequestStatus("APPROVED");
        return labSupplyDao.saveRequest(request);
    }

    private List<PreparedLabSupplyLine> normalizeCommands(List<LabSupplyRequestLineCommand> commands) {
        Map<String, PreparedLabSupplyLine> linesByKey = new LinkedHashMap<String, PreparedLabSupplyLine>();
        if (commands == null) {
            return new ArrayList<PreparedLabSupplyLine>();
        }
        for (LabSupplyRequestLineCommand command : commands) {
            if (command == null || command.getItemId() == null || command.getLabServiceConceptId() == null || command.getQuantity() == null
                    || command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String key = command.getItemId() + ":" + command.getLabServiceConceptId();
            PreparedLabSupplyLine preparedLine = linesByKey.get(key);
            if (preparedLine == null) {
                preparedLine = new PreparedLabSupplyLine();
                preparedLine.itemId = command.getItemId();
                preparedLine.labServiceConceptId = command.getLabServiceConceptId();
                preparedLine.quantity = BigDecimal.ZERO;
                linesByKey.put(key, preparedLine);
            }
            preparedLine.quantity = preparedLine.quantity.add(command.getQuantity());
        }
        return new ArrayList<PreparedLabSupplyLine>(linesByKey.values());
    }

    private static class PreparedLabSupplyLine {
        private Integer itemId;
        private Integer labServiceConceptId;
        private BigDecimal quantity;
    }

    private Map<Integer, BigDecimal> quantitiesByLineId(Integer[] lineIds, BigDecimal[] approvedQuantities) {
        Map<Integer, BigDecimal> quantitiesByLineId = new LinkedHashMap<Integer, BigDecimal>();
        if (lineIds == null || approvedQuantities == null) {
            return quantitiesByLineId;
        }
        for (int i = 0; i < lineIds.length; i++) {
            Integer lineId = lineIds[i];
            BigDecimal quantity = approvedQuantities.length > i ? approvedQuantities[i] : null;
            if (lineId == null || quantity == null) {
                continue;
            }
            quantitiesByLineId.put(lineId, quantity);
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

    private void consumeStock(List<InventoryBalance> itemBalances, BigDecimal quantityToConsume, String selectedBatchNumber,
                              LabSupplyRequestLine line, LabSupplyRequest request,
                              PharmacyLocation dispensingPharmacyLocation, User currentUser, Date when) {
        if (quantityToConsume == null || quantityToConsume.compareTo(BigDecimal.ZERO) <= 0 || itemBalances == null) {
            return;
        }
        BigDecimal remaining = quantityToConsume;
        for (InventoryBalance balance : itemBalances) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            if (balance == null || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (selectedBatchNumber != null && !selectedBatchNumber.equals(normalizeStoredBatchNumber(balance.getBatchNumber()))) {
                continue;
            }
            BigDecimal deducted = remaining.min(balance.getQuantityOnHand());
            balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(deducted));
            inventoryDao.saveInventoryBalance(balance);
            StockTransaction transaction = new StockTransaction();
            transaction.setItem(line.getItem());
            transaction.setPharmacyLocation(dispensingPharmacyLocation);
            transaction.setBatchNumber(balance.getBatchNumber());
            transaction.setExpiryDate(balance.getExpiryDate());
            transaction.setQuantity(deducted.negate());
            transaction.setTransactionType(StockTransactionType.DISPENSE);
            transaction.setCreator(currentUser);
            transaction.setPerformedBy(currentUser);
            transaction.setDateCreated(when);
            transaction.setRemarks("Issued to lab unit request #" + request.getLabSupplyRequestId()
                    + (request.getLabUnit() == null || request.getLabUnit().getName() == null ? "" : " - " + request.getLabUnit().getName()));
            inventoryDao.saveStockTransaction(transaction);
            remaining = remaining.subtract(deducted);
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
}
