package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentStockReturnService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.DepartmentStockReturnLineCommand;
import org.openmrs.module.rwandapharmacymanagement.db.DepartmentStockReturnDao;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturn;
import org.openmrs.module.rwandapharmacymanagement.model.DepartmentStockReturnLine;
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
public class DepartmentStockReturnServiceImpl extends BaseOpenmrsService implements DepartmentStockReturnService {

    private DepartmentStockReturnDao departmentStockReturnDao;
    private InventoryDao inventoryDao;
    private PharmacyService pharmacyService;

    public void setDepartmentStockReturnDao(DepartmentStockReturnDao departmentStockReturnDao) {
        this.departmentStockReturnDao = departmentStockReturnDao;
    }

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @Override
    public DepartmentStockReturn createReturn(PharmacyLocation departmentLocation,
                                              PharmacyLocation destinationPharmacyLocation,
                                              String reason,
                                              List<DepartmentStockReturnLineCommand> commands) {
        requireDepartmentStore(departmentLocation);
        requireReceivingPharmacy(destinationPharmacyLocation);
        List<PreparedLine> preparedLines = normalizeCommands(departmentLocation, commands);
        if (preparedLines.isEmpty()) {
            throw new IllegalArgumentException("At least one return line is required");
        }
        User currentUser = Context.getAuthenticatedUser();
        Date now = new Date();
        DepartmentStockReturn stockReturn = new DepartmentStockReturn();
        stockReturn.setDepartmentLocation(departmentLocation);
        stockReturn.setDestinationPharmacyLocation(destinationPharmacyLocation);
        stockReturn.setReturnStatus(STATUS_SUBMITTED);
        stockReturn.setReason(trimToNull(reason));
        stockReturn.setCreator(currentUser);
        stockReturn.setDateCreated(now);
        stockReturn.setVoided(Boolean.FALSE);
        stockReturn.setUuid(UUID.randomUUID().toString());
        stockReturn.setLines(new ArrayList<DepartmentStockReturnLine>());
        for (PreparedLine preparedLine : preparedLines) {
            DepartmentStockReturnLine line = new DepartmentStockReturnLine();
            line.setDepartmentStockReturn(stockReturn);
            line.setItem(preparedLine.item);
            line.setBatchNumber(preparedLine.batchNumber);
            line.setExpiryDate(preparedLine.expiryDate);
            line.setQuantity(preparedLine.quantity);
            line.setUnitPrice(preparedLine.unitPrice);
            line.setTotalAmount(preparedLine.unitPrice == null ? null : preparedLine.unitPrice.multiply(preparedLine.quantity));
            line.setCreator(currentUser);
            line.setDateCreated(now);
            line.setVoided(Boolean.FALSE);
            line.setUuid(UUID.randomUUID().toString());
            stockReturn.getLines().add(line);
        }
        return departmentStockReturnDao.saveReturn(stockReturn);
    }

    @Override
    public DepartmentStockReturn receiveReturn(Integer returnId) {
        DepartmentStockReturn stockReturn = requireReturn(returnId);
        if (!STATUS_SUBMITTED.equals(stockReturn.getReturnStatus())) {
            throw new IllegalArgumentException("Only submitted department returns can be received");
        }
        if (stockReturn.getLines() == null || stockReturn.getLines().isEmpty()) {
            throw new IllegalArgumentException("Department return has no lines");
        }
        for (DepartmentStockReturnLine line : stockReturn.getLines()) {
            if (isActiveLine(line)) {
                transferLine(stockReturn, line);
                departmentStockReturnDao.saveReturnLine(line);
            }
        }
        stockReturn.setReturnStatus(STATUS_RECEIVED);
        stockReturn.setReceivedBy(Context.getAuthenticatedUser());
        stockReturn.setReceivedDate(new Date());
        return departmentStockReturnDao.saveReturn(stockReturn);
    }

    @Override
    public DepartmentStockReturn rejectReturn(Integer returnId, String rejectionReason) {
        DepartmentStockReturn stockReturn = requireReturn(returnId);
        if (STATUS_RECEIVED.equals(stockReturn.getReturnStatus())) {
            throw new IllegalArgumentException("Received department returns cannot be rejected");
        }
        stockReturn.setReturnStatus(STATUS_REJECTED);
        stockReturn.setRejectedBy(Context.getAuthenticatedUser());
        stockReturn.setRejectedDate(new Date());
        stockReturn.setRejectionReason(trimToNull(rejectionReason));
        return departmentStockReturnDao.saveReturn(stockReturn);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentStockReturn getReturn(Integer returnId) {
        return departmentStockReturnDao.getReturn(returnId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentStockReturn> getReturnsForDepartment(PharmacyLocation departmentLocation, int offset, int limit) {
        return departmentStockReturnDao.getReturnsForDepartment(departmentLocation, offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentStockReturn> getReturnsForDestination(PharmacyLocation destinationPharmacyLocation,
                                                                String status, int offset, int limit) {
        return departmentStockReturnDao.getReturnsForDestination(destinationPharmacyLocation, status, offset, limit);
    }

    private List<PreparedLine> normalizeCommands(PharmacyLocation departmentLocation,
                                                 List<DepartmentStockReturnLineCommand> commands) {
        Map<String, PreparedLine> linesByItemBatch = new LinkedHashMap<String, PreparedLine>();
        if (commands == null) {
            return new ArrayList<PreparedLine>();
        }
        for (DepartmentStockReturnLineCommand command : commands) {
            if (command == null || command.getItemId() == null || command.getQuantity() == null
                    || command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String batchNumber = trimToNull(command.getBatchNumber());
            String key = command.getItemId() + "|" + (batchNumber == null ? "" : batchNumber);
            PreparedLine preparedLine = linesByItemBatch.get(key);
            if (preparedLine == null) {
                InventoryBalance balance = requireAvailableBalance(command.getItemId(),
                        departmentLocation.getPharmacyLocationId(), batchNumber, command.getQuantity());
                preparedLine = new PreparedLine();
                preparedLine.item = balance.getItem();
                preparedLine.batchNumber = balance.getBatchNumber();
                preparedLine.expiryDate = balance.getExpiryDate();
                preparedLine.unitPrice = balance.getUnitPrice();
                preparedLine.quantity = BigDecimal.ZERO;
                linesByItemBatch.put(key, preparedLine);
            }
            preparedLine.quantity = preparedLine.quantity.add(command.getQuantity());
            requireAvailableBalance(command.getItemId(), departmentLocation.getPharmacyLocationId(), batchNumber,
                    preparedLine.quantity);
        }
        return new ArrayList<PreparedLine>(linesByItemBatch.values());
    }

    private void transferLine(DepartmentStockReturn stockReturn, DepartmentStockReturnLine line) {
        InventoryBalance sourceBalance = requireAvailableBalance(line.getItem().getItemId(),
                stockReturn.getDepartmentLocation().getPharmacyLocationId(), line.getBatchNumber(), line.getQuantity());
        sourceBalance.setQuantityOnHand(sourceBalance.getQuantityOnHand().subtract(line.getQuantity()));
        inventoryDao.saveInventoryBalance(sourceBalance);

        InventoryBalance destinationBalance = inventoryDao.getInventoryBalance(line.getItem().getItemId(),
                stockReturn.getDestinationPharmacyLocation().getPharmacyLocationId(), sourceBalance.getBatchNumber());
        if (destinationBalance == null) {
            destinationBalance = new InventoryBalance();
            destinationBalance.setItem(line.getItem());
            destinationBalance.setPharmacyLocation(stockReturn.getDestinationPharmacyLocation());
            destinationBalance.setBatchNumber(sourceBalance.getBatchNumber());
            destinationBalance.setExpiryDate(sourceBalance.getExpiryDate());
            destinationBalance.setQuantityOnHand(BigDecimal.ZERO);
            destinationBalance.setUnitPrice(sourceBalance.getUnitPrice());
            destinationBalance.setCreator(Context.getAuthenticatedUser());
            destinationBalance.setDateCreated(new Date());
            destinationBalance.setVoided(Boolean.FALSE);
            destinationBalance.setUuid(UUID.randomUUID().toString());
        }
        destinationBalance.setQuantityOnHand(zeroIfNull(destinationBalance.getQuantityOnHand()).add(line.getQuantity()));
        if (destinationBalance.getUnitPrice() == null) {
            destinationBalance.setUnitPrice(sourceBalance.getUnitPrice());
        }
        inventoryDao.saveInventoryBalance(destinationBalance);

        line.setExpiryDate(sourceBalance.getExpiryDate());
        line.setUnitPrice(sourceBalance.getUnitPrice());
        line.setTotalAmount(sourceBalance.getUnitPrice() == null ? null : sourceBalance.getUnitPrice().multiply(line.getQuantity()));
        saveTransaction(stockReturn, line, stockReturn.getDepartmentLocation(), line.getQuantity().negate(),
                "Department stock return out");
        saveTransaction(stockReturn, line, stockReturn.getDestinationPharmacyLocation(), line.getQuantity(),
                "Department stock return in");
    }

    private void saveTransaction(DepartmentStockReturn stockReturn, DepartmentStockReturnLine line,
                                 PharmacyLocation pharmacyLocation, BigDecimal quantity, String remarks) {
        StockTransaction transaction = new StockTransaction();
        transaction.setItem(line.getItem());
        transaction.setPharmacyLocation(pharmacyLocation);
        transaction.setTransactionType(StockTransactionType.DEPARTMENT_STOCK_RETURN);
        transaction.setBatchNumber(line.getBatchNumber());
        transaction.setExpiryDate(line.getExpiryDate());
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(line.getUnitPrice());
        transaction.setTotalAmount(line.getUnitPrice() == null ? null : line.getUnitPrice().multiply(quantity.abs()));
        transaction.setReferenceNumber(stockReturn.getDepartmentStockReturnId() == null ? null
                : stockReturn.getDepartmentStockReturnId().toString());
        transaction.setRemarks(remarks);
        transaction.setPerformedBy(Context.getAuthenticatedUser());
        transaction.setCreator(Context.getAuthenticatedUser());
        transaction.setDateCreated(new Date());
        transaction.setVoided(Boolean.FALSE);
        inventoryDao.saveStockTransaction(transaction);
    }

    private InventoryBalance requireAvailableBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber,
                                                     BigDecimal quantity) {
        InventoryBalance balance = inventoryDao.getInventoryBalance(itemId, pharmacyLocationId, batchNumber);
        if (balance == null || balance.getItem() == null || balance.getQuantityOnHand() == null
                || balance.getQuantityOnHand().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Return quantity cannot exceed department stock");
        }
        return balance;
    }

    private DepartmentStockReturn requireReturn(Integer returnId) {
        DepartmentStockReturn stockReturn = departmentStockReturnDao.getReturn(returnId);
        if (stockReturn == null || Boolean.TRUE.equals(stockReturn.getVoided())) {
            throw new IllegalArgumentException("Department return not found");
        }
        return stockReturn;
    }

    private void requireDepartmentStore(PharmacyLocation departmentLocation) {
        if (departmentLocation == null || departmentLocation.getPharmacyLocationId() == null
                || !Boolean.TRUE.equals(departmentLocation.getActive())
                || departmentLocation.getLocationType() != PharmacyLocationType.DEPARTMENT_STORE) {
            throw new IllegalArgumentException("Department store is required");
        }
    }

    private void requireReceivingPharmacy(PharmacyLocation destinationPharmacyLocation) {
        if (destinationPharmacyLocation == null || destinationPharmacyLocation.getPharmacyLocationId() == null
                || !Boolean.TRUE.equals(destinationPharmacyLocation.getActive())
                || destinationPharmacyLocation.getLocationType() == PharmacyLocationType.DEPARTMENT_STORE) {
            throw new IllegalArgumentException("Choose an active receiving pharmacy");
        }
    }

    private boolean isActiveLine(DepartmentStockReturnLine line) {
        return line != null && !Boolean.TRUE.equals(line.getVoided()) && line.getQuantity() != null
                && line.getQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class PreparedLine {
        private Item item;
        private String batchNumber;
        private Date expiryDate;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }
}
