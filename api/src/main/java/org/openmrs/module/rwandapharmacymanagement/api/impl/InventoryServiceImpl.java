package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.context.Context;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.BillingPriceService;
import org.openmrs.module.rwandapharmacymanagement.api.dto.TransferBatchAllocationCommand;
import org.openmrs.module.rwandapharmacymanagement.api.dto.TransferPreviousInfo;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLineBatch;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustment;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustmentLine;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrder;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierPrice;
import org.openmrs.module.rwandapharmacymanagement.model.enums.PharmacyLocationType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional
public class InventoryServiceImpl extends BaseOpenmrsService implements InventoryService {

    private InventoryDao inventoryDao;
    private PharmacyService pharmacyService;
    private BillingPriceService billingPriceService;

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setBillingPriceService(BillingPriceService billingPriceService) {
        this.billingPriceService = billingPriceService;
    }

    @Override
    public List<InventoryBalance> getInventoryForPharmacy(Integer pharmacyLocationId) {
        return inventoryDao.getInventoryForPharmacy(pharmacyLocationId);
    }

    @Override
    public List<InventoryBalance> getAvailableInventoryForPharmacy(Integer pharmacyLocationId) {
        return inventoryDao.getAvailableInventoryForPharmacy(pharmacyLocationId);
    }

    @Override
    public java.util.Map<Integer, BigDecimal> getAvailableQuantitiesByItem(java.util.Collection<Integer> itemIds) {
        return inventoryDao.getAvailableQuantitiesByItem(itemIds);
    }

    @Override
    public boolean hasStockInActivePharmacy(Integer itemId) {
        return inventoryDao.hasStockInActivePharmacy(itemId);
    }

    @Override
    public InventoryBalance getInventoryBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber) {
        return inventoryDao.getInventoryBalance(itemId, pharmacyLocationId, batchNumber);
    }

    @Override
    public InventoryBalance updateBatchExpiryDate(Integer inventoryBalanceId, Integer pharmacyLocationId, Date expiryDate) {
        if (inventoryBalanceId == null) {
            throw new IllegalArgumentException("Inventory balance is required");
        }
        if (pharmacyLocationId == null) {
            throw new IllegalArgumentException("Pharmacy is required");
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        InventoryBalance balance = inventoryDao.getInventoryBalance(inventoryBalanceId);
        if (balance == null) {
            throw new IllegalArgumentException("Inventory batch was not found");
        }
        if (balance.getPharmacyLocation() == null || balance.getPharmacyLocation().getPharmacyLocationId() == null
                || !pharmacyLocationId.equals(balance.getPharmacyLocation().getPharmacyLocationId())) {
            throw new IllegalArgumentException("Inventory batch does not belong to the selected pharmacy");
        }
        balance.setExpiryDate(expiryDate);
        return inventoryDao.saveInventoryBalance(balance);
    }

    @Override
    public StockTransaction writeOffInventoryBalance(Integer inventoryBalanceId, Integer pharmacyLocationId,
                                                     BigDecimal quantity, String reason) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK);
        if (inventoryBalanceId == null) {
            throw new IllegalArgumentException("Inventory balance is required");
        }
        if (pharmacyLocationId == null) {
            throw new IllegalArgumentException("Pharmacy is required");
        }
        BigDecimal writeOffQuantity = normalizePositiveQuantity(quantity, "Write-off quantity is required");
        String writeOffReason = emptyToNull(reason);
        if (writeOffReason == null) {
            throw new IllegalArgumentException("Write-off reason is required");
        }
        InventoryBalance balance = inventoryDao.getInventoryBalance(inventoryBalanceId);
        if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
            throw new IllegalArgumentException("Inventory batch was not found");
        }
        if (balance.getPharmacyLocation() == null || balance.getPharmacyLocation().getPharmacyLocationId() == null
                || !pharmacyLocationId.equals(balance.getPharmacyLocation().getPharmacyLocationId())) {
            throw new IllegalArgumentException("Inventory batch does not belong to the selected pharmacy");
        }
        BigDecimal available = balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
        if (available.compareTo(writeOffQuantity) < 0) {
            throw new IllegalArgumentException("Write-off quantity cannot exceed quantity on hand");
        }
        balance.setQuantityOnHand(available.subtract(writeOffQuantity));
        inventoryDao.saveInventoryBalance(balance);
        StockTransaction transaction = buildTransaction(balance.getItem().getItemId(), pharmacyLocationId,
                balance.getBatchNumber(), balance.getExpiryDate(), writeOffQuantity.negate(),
                StockTransactionType.WRITE_OFF, null, writeOffReason, balance.getUnitPrice());
        return inventoryDao.saveStockTransaction(transaction);
    }

    @Override
    public StockTransaction recordWrittenOffStockDisposal(Integer writeOffStockTransactionId, String disposalMethod, String notes) {
        Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK);
        if (writeOffStockTransactionId == null) {
            throw new IllegalArgumentException("Write-off transaction is required");
        }
        String method = emptyToNull(disposalMethod);
        if (method == null) {
            throw new IllegalArgumentException("Disposal method is required");
        }
        StockTransaction writeOff = inventoryDao.getStockTransaction(writeOffStockTransactionId);
        if (writeOff == null || writeOff.getTransactionType() != StockTransactionType.WRITE_OFF
                || writeOff.getQuantity() == null || writeOff.getQuantity().compareTo(BigDecimal.ZERO) >= 0
                || writeOff.getItem() == null || writeOff.getItem().getItemId() == null
                || writeOff.getPharmacyLocation() == null || writeOff.getPharmacyLocation().getPharmacyLocationId() == null) {
            throw new IllegalArgumentException("Valid write-off transaction was not found");
        }
        if (hasDisposalForWriteOff(writeOffStockTransactionId)) {
            throw new IllegalArgumentException("This write-off has already been disposed");
        }
        String remarks = "Method: " + method;
        String trimmedNotes = emptyToNull(notes);
        if (trimmedNotes != null) {
            remarks = remarks + ". Notes: " + trimmedNotes;
        }
        StockTransaction disposal = buildTransaction(writeOff.getItem().getItemId(),
                writeOff.getPharmacyLocation().getPharmacyLocationId(), writeOff.getBatchNumber(),
                writeOff.getExpiryDate(), writeOff.getQuantity().abs(), StockTransactionType.DISPOSAL,
                String.valueOf(writeOffStockTransactionId), remarks, writeOff.getUnitPrice());
        return inventoryDao.saveStockTransaction(disposal);
    }

    @Override
    public List<DeliveryReceiptLine> getRecentDeliveryReceiptLines() {
        return inventoryDao.getRecentDeliveryReceiptLines();
    }

    @Override
    public List<DeliveryReceipt> getDeliveryReceipts(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId,
                                                     Integer purchaseOrderId) {
        return inventoryDao.getDeliveryReceipts(fromInclusive, toExclusive, pharmacyLocationId, purchaseOrderId);
    }

    @Override
    public List<StockTransaction> getRecentStockTransactions() {
        return inventoryDao.getRecentStockTransactions();
    }

    @Override
    public StockTransaction getStockTransaction(Integer stockTransactionId) {
        return inventoryDao.getStockTransaction(stockTransactionId);
    }

    @Override
    public List<StockTransaction> getStockTransactions(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
        return inventoryDao.getStockTransactions(fromInclusive, toExclusive, pharmacyLocationId);
    }

    @Override
    public List<StockTransaction> getStockTransactions(Integer itemId, Date fromInclusive, Date toExclusive, Integer pharmacyLocationId) {
        return inventoryDao.getStockTransactions(itemId, fromInclusive, toExclusive, pharmacyLocationId);
    }

    @Override
    public List<InterPharmacyTransfer> getTransfers() {
        return inventoryDao.getTransfers();
    }

    @Override
    public List<InterPharmacyTransfer> getTransfers(String transferStatus, Integer sourcePharmacyLocationId,
                                                    Integer destinationPharmacyLocationId, Integer maxResults) {
        return inventoryDao.getTransfers(transferStatus, sourcePharmacyLocationId, destinationPharmacyLocationId, maxResults);
    }

    @Override
    public long countTransfers(String transferStatus, Integer sourcePharmacyLocationId, Integer destinationPharmacyLocationId) {
        return inventoryDao.countTransfers(transferStatus, sourcePharmacyLocationId, destinationPharmacyLocationId);
    }

    @Override
    public InterPharmacyTransfer getTransfer(Integer transferId) {
        return inventoryDao.getTransfer(transferId);
    }

    @Override
    public TransferPreviousInfo getTransferPreviousInfo(Integer transferId, Integer transferLineId) {
        if (transferId == null || transferLineId == null) {
            throw new IllegalArgumentException("Transfer and transfer line are required");
        }
        InterPharmacyTransfer transfer = inventoryDao.getTransfer(transferId);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfer request not found");
        }
        InterPharmacyTransferLine line = findTransferLine(transfer, transferLineId);
        if (line == null || line.getItem() == null || line.getItem().getItemId() == null
                || transfer.getDestinationPharmacyLocation() == null
                || transfer.getDestinationPharmacyLocation().getPharmacyLocationId() == null) {
            throw new IllegalArgumentException("Transfer line is not valid for previous stock information");
        }

        Integer itemId = line.getItem().getItemId();
        Integer pharmacyLocationId = transfer.getDestinationPharmacyLocation().getPharmacyLocationId();
        Date currentRequestDate = transfer.getDateCreated() == null ? new Date() : transfer.getDateCreated();
        InterPharmacyTransferLine previousLine = inventoryDao.getLatestTransferLineBefore(pharmacyLocationId, itemId,
                currentRequestDate, transfer.getInterPharmacyTransferId());
        Date previousRequestDate = previousLine == null || previousLine.getInterPharmacyTransfer() == null
                ? null : previousLine.getInterPharmacyTransfer().getDateCreated();

        BigDecimal currentQuantity = currentQuantity(itemId, pharmacyLocationId);
        BigDecimal quantityAtCurrentRequest = currentQuantity.subtract(netTransactionQuantity(itemId, pharmacyLocationId,
                currentRequestDate, null));

        TransferPreviousInfo info = new TransferPreviousInfo();
        info.setTransferId(transfer.getInterPharmacyTransferId());
        info.setTransferLineId(line.getInterPharmacyTransferLineId());
        info.setItemName(line.getItem().getName());
        info.setRequestingPharmacyName(transfer.getDestinationPharmacyLocation().getName());
        info.setCurrentRequestDate(currentRequestDate);
        info.setCurrentRequestedQuantity(line.getQuantity());
        info.setCurrentQuantity(currentQuantity);
        info.setQuantityAtCurrentRequest(quantityAtCurrentRequest);

        if (previousLine == null || previousRequestDate == null) {
            return info;
        }

        info.setPreviousTransferId(previousLine.getInterPharmacyTransfer() == null ? null
                : previousLine.getInterPharmacyTransfer().getInterPharmacyTransferId());
        info.setPreviousRequestDate(previousRequestDate);
        info.setPreviousRequestedQuantity(previousLine.getQuantity());
        info.setQuantityBeforePreviousRequest(currentQuantity.subtract(netTransactionQuantity(itemId, pharmacyLocationId,
                previousRequestDate, null)));

        List<StockTransaction> transactions = inventoryDao.getStockTransactions(itemId, previousRequestDate,
                currentRequestDate, pharmacyLocationId, null);
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal dispensed = BigDecimal.ZERO;
        BigDecimal transferredOut = BigDecimal.ZERO;
        BigDecimal adjusted = BigDecimal.ZERO;
        if (transactions != null) {
            for (StockTransaction transaction : transactions) {
                BigDecimal quantity = transaction == null || transaction.getQuantity() == null
                        ? BigDecimal.ZERO : transaction.getQuantity();
                if (quantity.compareTo(BigDecimal.ZERO) == 0
                        || transaction.getTransactionType() == StockTransactionType.DISPOSAL) {
                    continue;
                }
                if (transaction.getTransactionType() == StockTransactionType.ADJUSTMENT
                        || transaction.getTransactionType() == StockTransactionType.RECONCILIATION) {
                    adjusted = adjusted.add(quantity);
                } else if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    received = received.add(quantity);
                } else if (transaction.getTransactionType() == StockTransactionType.DISPENSE
                        || transaction.getTransactionType() == StockTransactionType.DISPENSE_WITHOUT_ORDER) {
                    dispensed = dispensed.add(quantity.abs());
                } else {
                    transferredOut = transferredOut.add(quantity.abs());
                }
            }
        }
        info.setReceivedSincePreviousRequest(received);
        info.setDispensedSincePreviousRequest(dispensed);
        info.setTransferredOutSincePreviousRequest(transferredOut);
        info.setAdjustedSincePreviousRequest(adjusted);
        return info;
    }

    @Override
    public DeliveryReceipt getDeliveryReceipt(Integer deliveryReceiptId) {
        return inventoryDao.getDeliveryReceipt(deliveryReceiptId);
    }

    @Override
    public DeliveryReceipt receiveDelivery(DeliveryReceipt deliveryReceipt) {
        validateDeliveryReceipt(deliveryReceipt);
        if (deliveryReceipt.getCreator() == null) {
            deliveryReceipt.setCreator(Context.getAuthenticatedUser());
        }
        if (deliveryReceipt.getDateCreated() == null) {
            deliveryReceipt.setDateCreated(new Date());
        }
        if (deliveryReceipt.getLines() != null) {
            for (DeliveryReceiptLine line : deliveryReceipt.getLines()) {
                line.setDeliveryReceipt(deliveryReceipt);
                if ((line.getProgram() == null || line.getProgram().trim().isEmpty()) && line.getProductMasterItem() != null) {
                    line.setProgram(line.getProductMasterItem().getProgram());
                }
                if ((line.getProgram() == null || line.getProgram().trim().isEmpty()) && line.getItem() != null) {
                    line.setProgram(line.getItem().getProgram());
                }
                if (line.getUnitPrice() == null && line.getItem() != null) {
                    SupplierPrice latestPrice = pharmacyService.getLatestSupplierPrice(line.getItem().getCode(), line.getProgram());
                    if (latestPrice != null) {
                        line.setUnitPrice(latestPrice.getUnitPrice());
                    }
                }
                if (line.getCreator() == null) {
                    line.setCreator(Context.getAuthenticatedUser());
                }
                if (line.getDateCreated() == null) {
                    line.setDateCreated(new Date());
                }
            }
        }
        inventoryDao.saveDeliveryReceipt(deliveryReceipt);
        if (deliveryReceipt.getLines() != null) {
            List<org.openmrs.module.rwandapharmacymanagement.model.Item> receivedItems =
                    new ArrayList<org.openmrs.module.rwandapharmacymanagement.model.Item>();
            for (DeliveryReceiptLine line : deliveryReceipt.getLines()) {
                incrementBalance(line.getItem().getItemId(),
                        deliveryReceipt.getReceivedIntoLocation().getPharmacyLocationId(),
                        line.getBatchNumber(), line.getExpiryDate(), line.getAcceptedQuantity(), line.getUnitPrice());
                saveTransaction(line.getItem().getItemId(),
                        deliveryReceipt.getReceivedIntoLocation().getPharmacyLocationId(),
                        line.getBatchNumber(), line.getExpiryDate(), line.getAcceptedQuantity(),
                        StockTransactionType.DELIVERY_RECEIPT, deliveryReceipt.getReceiptNumber(), "Delivery received",
                        line.getUnitPrice());
                receivedItems.add(line.getItem());
            }
            billingPriceService.updatePricesForReceivedItems(receivedItems);
        }
        closeLinkedOrders(deliveryReceipt);
        return deliveryReceipt;
    }

    private void closeLinkedOrders(DeliveryReceipt deliveryReceipt) {
        if (deliveryReceipt == null) {
            return;
        }
        PurchaseOrder purchaseOrder = deliveryReceipt.getPurchaseOrder();
        if (purchaseOrder != null) {
            purchaseOrder.setDispatchStatus("RECEIVED");
            purchaseOrder.setFetchedReceiptPayload(null);
            purchaseOrder.setFetchedReceiptDate(null);
            pharmacyService.savePurchaseOrder(purchaseOrder);
            if (deliveryReceipt.getSupplierOrder() == null) {
                deliveryReceipt.setSupplierOrder(pharmacyService.getSupplierOrderByOrderNumberOrReference(
                        resolveSupplierOrderNumber(purchaseOrder), purchaseOrder.getDispatchReference()));
                inventoryDao.saveDeliveryReceipt(deliveryReceipt);
            }
        }
        SupplierOrder supplierOrder = deliveryReceipt.getSupplierOrder();
        if (supplierOrder != null) {
            supplierOrder.setStatus("RECEIVED");
            pharmacyService.saveSupplierOrder(supplierOrder);
        }
    }

    private String resolveSupplierOrderNumber(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return null;
        }
        String facilityId = purchaseOrder.getFacilityId() == null || purchaseOrder.getFacilityId().trim().isEmpty()
                ? "1660" : purchaseOrder.getFacilityId().trim();
        return "ord-" + facilityId + "-" + purchaseOrder.getPurchaseOrderId();
    }

    private void validateDeliveryReceipt(DeliveryReceipt deliveryReceipt) {
        if (deliveryReceipt == null) {
            throw new IllegalArgumentException("Delivery receipt is required");
        }
        if (deliveryReceipt.getReceivedIntoLocation() == null) {
            throw new IllegalArgumentException("Receive location is required");
        }
        if (deliveryReceipt.getLines() == null || deliveryReceipt.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one receipt line is required");
        }
        if (!Boolean.TRUE.equals(deliveryReceipt.getReceivedIntoLocation().getMainStore())) {
            return;
        }
        int lineIndex = 0;
        for (DeliveryReceiptLine line : deliveryReceipt.getLines()) {
            lineIndex++;
            if (line == null || line.getItem() == null) {
                continue;
            }
            if (line.getBatchNumber() == null || line.getBatchNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Batch number is required for main pharmacy receipt line " + lineIndex);
            }
            if (Boolean.TRUE.equals(line.getItem().getRequiresExpiry()) && line.getExpiryDate() == null) {
                throw new IllegalArgumentException("Expiration date is required for main pharmacy receipt line " + lineIndex
                        + " because the item is configured to require expiry");
            }
        }
    }

    @Override
    public InterPharmacyTransfer transferStock(InterPharmacyTransfer transfer) {
        if (transfer.getTransferStatus() == null || transfer.getTransferStatus().trim().isEmpty()) {
            transfer.setTransferStatus("SERVED");
        }
        validateRequestedQuantities(transfer);
        transfer.setCreator(Context.getAuthenticatedUser());
        transfer.setDateCreated(new Date());
        if (transfer.getLines() != null) {
            for (InterPharmacyTransferLine line : transfer.getLines()) {
                line.setInterPharmacyTransfer(transfer);
                line.setCreator(Context.getAuthenticatedUser());
                line.setDateCreated(new Date());
            }
        }
        refreshTransferLineCosts(transfer);
        inventoryDao.saveTransfer(transfer);
        performTransfer(transfer);
        return transfer;
    }

    @Override
    public InterPharmacyTransfer requestTransfer(InterPharmacyTransfer transfer) {
        validateRequestedQuantities(transfer);
        transfer.setTransferStatus("REQUESTED");
        transfer.setCreator(Context.getAuthenticatedUser());
        transfer.setDateCreated(new Date());
        if (transfer.getLines() != null) {
            for (InterPharmacyTransferLine line : transfer.getLines()) {
                line.setInterPharmacyTransfer(transfer);
                line.setCreator(Context.getAuthenticatedUser());
                line.setDateCreated(new Date());
            }
        }
        refreshTransferLineCosts(transfer);
        inventoryDao.saveTransfer(transfer);
        return transfer;
    }

    @Override
    public InterPharmacyTransfer approveTransfer(Integer transferId) {
        return approveTransfer(transferId, null, null, null);
    }

    @Override
    public InterPharmacyTransfer approveTransfer(Integer transferId, BigDecimal[] quantities, String[] batchNumbers, Date[] expiryDates) {
        InterPharmacyTransfer transfer = inventoryDao.getTransfer(transferId);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfer request not found");
        }
        if ("SERVED".equalsIgnoreCase(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Transferred requests cannot be approved again");
        }
        requireMainToDispensingApprovalPrivilege(transfer);
        requireNotOwnMainToDispensingRequest(transfer);
        applyServedLineUpdates(transfer, quantities, batchNumbers, expiryDates);
        validateRequestedQuantities(transfer);
        refreshTransferLineCosts(transfer);
        transfer.setTransferStatus("APPROVED");
        transfer.setApprovedBy(Context.getAuthenticatedUser());
        transfer.setApprovedDate(new Date());
        transfer.setRejectedBy(null);
        transfer.setRejectedDate(null);
        transfer.setRejectionReason(null);
        inventoryDao.saveTransfer(transfer);
        return transfer;
    }

    @Override
    public InterPharmacyTransfer rejectTransfer(Integer transferId, String rejectionReason) {
        InterPharmacyTransfer transfer = inventoryDao.getTransfer(transferId);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfer request not found");
        }
        if ("SERVED".equalsIgnoreCase(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Transferred requests cannot be rejected");
        }
        requireMainToDispensingApprovalPrivilege(transfer);
        transfer.setTransferStatus("REJECTED");
        transfer.setRejectedBy(Context.getAuthenticatedUser());
        transfer.setRejectedDate(new Date());
        transfer.setRejectionReason(rejectionReason);
        inventoryDao.saveTransfer(transfer);
        return transfer;
    }

    @Override
    public InterPharmacyTransfer removeTransferLine(Integer transferId, Integer transferLineId, String reason) {
        InterPharmacyTransfer transfer = inventoryDao.getTransfer(transferId);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfer request not found");
        }
        if ("SERVED".equalsIgnoreCase(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Transferred requests cannot be changed");
        }
        if ("REJECTED".equalsIgnoreCase(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Rejected requests cannot be changed");
        }
        String trimmedReason = emptyToNull(reason);
        if (trimmedReason == null) {
            throw new IllegalArgumentException("Reason is required when removing a request line");
        }
        requireTransferLineRemovalPrivilege(transfer);
        if ("REQUESTED".equalsIgnoreCase(transfer.getTransferStatus())) {
            requireNotOwnMainToDispensingRequest(transfer);
        }
        InterPharmacyTransferLine line = findTransferLine(transfer, transferLineId);
        if (line == null) {
            throw new IllegalArgumentException("Transfer request line not found");
        }
        if (!Boolean.TRUE.equals(line.getVoided())) {
            line.setVoided(Boolean.TRUE);
            line.setVoidedBy(Context.getAuthenticatedUser());
            line.setDateVoided(new Date());
            line.setVoidReason(trimmedReason);
            inventoryDao.saveTransferLine(line);
        }
        if (!hasActiveTransferLines(transfer)) {
            transfer.setTransferStatus("REJECTED");
            transfer.setRejectedBy(Context.getAuthenticatedUser());
            transfer.setRejectedDate(new Date());
            transfer.setRejectionReason("All request lines removed. " + trimmedReason);
        }
        inventoryDao.saveTransfer(transfer);
        return transfer;
    }

    @Override
    public InterPharmacyTransfer serveTransfer(Integer transferId) {
        return serveTransfer(transferId, null, null, null);
    }

    @Override
    public InterPharmacyTransfer serveTransfer(Integer transferId, BigDecimal[] quantities, String[] batchNumbers, Date[] expiryDates) {
        return serveTransfer(transferId, quantities, batchNumbers, expiryDates, null);
    }

    @Override
    public InterPharmacyTransfer serveTransfer(Integer transferId, BigDecimal[] quantities, String[] batchNumbers,
                                               Date[] expiryDates, List<TransferBatchAllocationCommand> batchAllocations) {
        InterPharmacyTransfer transfer = inventoryDao.getTransfer(transferId);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfer request not found");
        }
        if ("REJECTED".equalsIgnoreCase(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Rejected requests cannot be transferred");
        }
        if ("SERVED".equalsIgnoreCase(transfer.getTransferStatus())) {
            return transfer;
        }
        if (isMainToDispensingTransfer(transfer)) {
            if (!"APPROVED".equalsIgnoreCase(transfer.getTransferStatus())) {
                throw new IllegalArgumentException("Main to dispensing requests must be approved before transfer");
            }
            requireMainToDispensingTransferPrivilege(transfer);
            requireCurrentLocationCanTransferFromSource(transfer);
        }
        applyServedLineUpdates(transfer, quantities, batchNumbers, expiryDates);
        applyBatchAllocationUpdates(transfer, batchAllocations);
        validateRequestedQuantities(transfer);
        refreshTransferLineCosts(transfer);
        transfer.setTransferStatus("SERVED");
        transfer.setServedBy(Context.getAuthenticatedUser());
        transfer.setServedDate(new Date());
        inventoryDao.saveTransfer(transfer);
        performTransfer(transfer);
        return transfer;
    }

    private void requireMainToDispensingApprovalPrivilege(InterPharmacyTransfer transfer) {
        if (isMainToDispensingTransfer(transfer)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
        }
    }

    private void requireMainToDispensingTransferPrivilege(InterPharmacyTransfer transfer) {
        if (isMainToDispensingTransfer(transfer)) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        }
    }

    private void requireTransferLineRemovalPrivilege(InterPharmacyTransfer transfer) {
        if (!isMainToDispensingTransfer(transfer)) {
            return;
        }
        if ("APPROVED".equalsIgnoreCase(transfer.getTransferStatus())) {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK);
        } else {
            Context.requirePrivilege(RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
        }
    }

    private void requireNotOwnMainToDispensingRequest(InterPharmacyTransfer transfer) {
        if (!isMainToDispensingTransfer(transfer)) {
            return;
        }
        User currentUser = Context.getAuthenticatedUser();
        User creator = transfer.getCreator();
        if (currentUser != null && currentUser.getUserId() != null
                && creator != null && currentUser.getUserId().equals(creator.getUserId())) {
            throw new IllegalArgumentException("You cannot approve your own main to dispensing request");
        }
    }

    private void requireCurrentLocationCanTransferFromSource(InterPharmacyTransfer transfer) {
        Location currentLocation = Context.getUserContext() == null ? null : Context.getUserContext().getLocation();
        PharmacyLocation source = transfer == null ? null : transfer.getSourcePharmacyLocation();
        if (!hasOpenmrsLocationBinding(source)) {
            return;
        }
        if (!matchesOpenmrsLocation(source, currentLocation)) {
            throw new IllegalArgumentException("Only users at the main pharmacy location can transfer approved main to dispensing requests");
        }
    }

    private InterPharmacyTransferLine findTransferLine(InterPharmacyTransfer transfer, Integer transferLineId) {
        if (transfer == null || transfer.getLines() == null || transferLineId == null) {
            return null;
        }
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (line != null && transferLineId.equals(line.getInterPharmacyTransferLineId())) {
                return line;
            }
        }
        return null;
    }

    private BigDecimal currentQuantity(Integer itemId, Integer pharmacyLocationId) {
        BigDecimal total = BigDecimal.ZERO;
        if (itemId == null || pharmacyLocationId == null) {
            return total;
        }
        List<InventoryBalance> balances = inventoryDao.getInventoryForPharmacy(pharmacyLocationId);
        if (balances == null) {
            return total;
        }
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || !itemId.equals(balance.getItem().getItemId())) {
                continue;
            }
            total = total.add(balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand());
        }
        return total;
    }

    private BigDecimal netTransactionQuantity(Integer itemId, Integer pharmacyLocationId, Date fromInclusive, Date toExclusive) {
        BigDecimal total = BigDecimal.ZERO;
        List<StockTransaction> transactions = inventoryDao.getStockTransactions(itemId, fromInclusive, toExclusive,
                pharmacyLocationId, null);
        if (transactions == null) {
            return total;
        }
        for (StockTransaction transaction : transactions) {
            total = total.add(transaction == null || transaction.getQuantity() == null
                    || transaction.getTransactionType() == StockTransactionType.DISPOSAL
                    ? BigDecimal.ZERO : transaction.getQuantity());
        }
        return total;
    }

    private boolean hasOpenmrsLocationBinding(PharmacyLocation pharmacyLocation) {
        return pharmacyLocation != null
                && ((pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null)
                || (pharmacyLocation.getLocationTag() != null && pharmacyLocation.getLocationTag().getLocationTagId() != null)
                || (pharmacyLocation.getLocationTags() != null && !pharmacyLocation.getLocationTags().isEmpty()));
    }

    private boolean matchesOpenmrsLocation(PharmacyLocation pharmacyLocation, Location location) {
        if (pharmacyLocation == null || location == null || location.getLocationId() == null) {
            return false;
        }
        if (pharmacyLocation.getLocation() != null && pharmacyLocation.getLocation().getLocationId() != null
                && pharmacyLocation.getLocation().getLocationId().equals(location.getLocationId())) {
            return true;
        }
        if (location.getTags() == null || location.getTags().isEmpty()) {
            return false;
        }
        Set<Integer> currentTagIds = new HashSet<Integer>();
        for (LocationTag tag : location.getTags()) {
            if (tag != null && tag.getLocationTagId() != null) {
                currentTagIds.add(tag.getLocationTagId());
            }
        }
        if (pharmacyLocation.getLocationTag() != null && pharmacyLocation.getLocationTag().getLocationTagId() != null
                && currentTagIds.contains(pharmacyLocation.getLocationTag().getLocationTagId())) {
            return true;
        }
        if (pharmacyLocation.getLocationTags() != null) {
            for (LocationTag tag : pharmacyLocation.getLocationTags()) {
                if (tag != null && tag.getLocationTagId() != null && currentTagIds.contains(tag.getLocationTagId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMainToDispensingTransfer(InterPharmacyTransfer transfer) {
        return transfer != null
                && transfer.getSourcePharmacyLocation() != null
                && Boolean.TRUE.equals(transfer.getSourcePharmacyLocation().getMainStore())
                && transfer.getDestinationPharmacyLocation() != null
                && transfer.getDestinationPharmacyLocation().getLocationType() == PharmacyLocationType.DISPENSING_PHARMACY;
    }

    @Override
    public StockAdjustment saveStockAdjustment(StockAdjustment adjustment) {
        adjustment.setCreator(Context.getAuthenticatedUser());
        adjustment.setDateCreated(new Date());
        adjustment.setAdjustedBy(Context.getAuthenticatedUser());
        if (adjustment.getLines() != null) {
            for (StockAdjustmentLine line : adjustment.getLines()) {
                line.setStockAdjustment(adjustment);
                line.setCreator(Context.getAuthenticatedUser());
                line.setDateCreated(new Date());
            }
        }
        inventoryDao.saveAdjustment(adjustment);
        if (adjustment.getLines() != null) {
            for (StockAdjustmentLine line : adjustment.getLines()) {
                InventoryBalance existing = inventoryDao.getInventoryBalance(line.getItem().getItemId(),
                        adjustment.getPharmacyLocation().getPharmacyLocationId(), line.getBatchNumber());
                BigDecimal current = existing == null || existing.getQuantityOnHand() == null ? BigDecimal.ZERO : existing.getQuantityOnHand();
                BigDecimal delta = line.getQuantity().subtract(current);
                line.setSystemQuantity(current);
                incrementBalance(line.getItem().getItemId(), adjustment.getPharmacyLocation().getPharmacyLocationId(),
                        line.getBatchNumber(), line.getExpiryDate(), delta, existing == null ? null : existing.getUnitPrice());
                saveTransaction(line.getItem().getItemId(), adjustment.getPharmacyLocation().getPharmacyLocationId(),
                        line.getBatchNumber(), line.getExpiryDate(), delta,
                        StockTransactionType.ADJUSTMENT, adjustment.getStockAdjustmentId() == null ? null : adjustment.getStockAdjustmentId().toString(),
                        adjustment.getComments(), existing == null ? null : existing.getUnitPrice());
            }
        }
        return adjustment;
    }

    @Override
    public StockTransaction saveStockTransaction(StockTransaction stockTransaction) {
        return inventoryDao.saveStockTransaction(stockTransaction);
    }

    private void performTransfer(InterPharmacyTransfer transfer) {
        if (transfer.getLines() == null) {
            return;
        }
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (!isActiveTransferLine(line)) {
                continue;
            }
            List<ConsumedBalance> consumedBalances = hasActiveBatchAllocations(line)
                    ? decrementAllocatedBalances(line, transfer.getSourcePharmacyLocation().getPharmacyLocationId())
                    : decrementBalances(line.getItem().getItemId(), transfer.getSourcePharmacyLocation().getPharmacyLocationId(),
                    line.getBatchNumber(), line.getQuantity());
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal firstUnitPrice = null;
            for (ConsumedBalance consumed : consumedBalances) {
                if (firstUnitPrice == null && consumed.unitPrice != null) {
                    firstUnitPrice = consumed.unitPrice;
                }
                if (consumed.unitPrice != null) {
                    totalAmount = totalAmount.add(consumed.unitPrice.multiply(consumed.quantity));
                }
                incrementBalance(line.getItem().getItemId(), transfer.getDestinationPharmacyLocation().getPharmacyLocationId(),
                        consumed.batchNumber, consumed.expiryDate, consumed.quantity, consumed.unitPrice);
                saveTransaction(line.getItem().getItemId(), transfer.getSourcePharmacyLocation().getPharmacyLocationId(),
                        consumed.batchNumber, consumed.expiryDate, consumed.quantity.negate(),
                        StockTransactionType.INTER_PHARMACY_TRANSFER, transfer.getInterPharmacyTransferId() == null ? null : transfer.getInterPharmacyTransferId().toString(),
                        "Transfer out", consumed.unitPrice);
                saveTransaction(line.getItem().getItemId(), transfer.getDestinationPharmacyLocation().getPharmacyLocationId(),
                        consumed.batchNumber, consumed.expiryDate, consumed.quantity,
                        StockTransactionType.INTER_PHARMACY_TRANSFER, transfer.getInterPharmacyTransferId() == null ? null : transfer.getInterPharmacyTransferId().toString(),
                        "Transfer in", consumed.unitPrice);
            }
            line.setUnitPrice(firstUnitPrice);
            line.setTotalAmount(totalAmount);
        }
        inventoryDao.saveTransfer(transfer);
    }

    private List<ConsumedBalance> decrementAllocatedBalances(InterPharmacyTransferLine line, Integer pharmacyLocationId) {
        List<ConsumedBalance> consumedBalances = new ArrayList<ConsumedBalance>();
        for (InterPharmacyTransferLineBatch allocation : activeBatchAllocations(line)) {
            InventoryBalance balance = decrementBalance(line.getItem().getItemId(), pharmacyLocationId,
                    allocation.getBatchNumber(), allocation.getQuantity());
            ConsumedBalance consumed = consumedBalance(balance, allocation.getQuantity());
            consumedBalances.add(consumed);
            allocation.setExpiryDate(consumed.expiryDate);
            allocation.setUnitPrice(consumed.unitPrice);
            allocation.setTotalAmount(consumed.unitPrice == null ? null : consumed.unitPrice.multiply(consumed.quantity));
        }
        return consumedBalances;
    }

    private void applyServedLineUpdates(InterPharmacyTransfer transfer, BigDecimal[] quantities, String[] batchNumbers, Date[] expiryDates) {
        if (transfer.getLines() == null) {
            return;
        }
        int activeIndex = 0;
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (!isActiveTransferLine(line)) {
                continue;
            }
            if (quantities != null && quantities.length > activeIndex && quantities[activeIndex] != null
                    && quantities[activeIndex].compareTo(BigDecimal.ZERO) > 0) {
                line.setQuantity(quantities[activeIndex]);
            }
            if (batchNumbers != null && batchNumbers.length > activeIndex) {
                line.setBatchNumber(emptyToNull(batchNumbers[activeIndex]));
            }
            if (expiryDates != null && expiryDates.length > activeIndex) {
                line.setExpiryDate(expiryDates[activeIndex]);
            }
            activeIndex++;
        }
    }

    private void applyBatchAllocationUpdates(InterPharmacyTransfer transfer,
                                             List<TransferBatchAllocationCommand> batchAllocations) {
        if (batchAllocations == null || batchAllocations.isEmpty() || transfer == null || transfer.getLines() == null) {
            return;
        }
        Map<Integer, List<TransferBatchAllocationCommand>> allocationsByLineId =
                new LinkedHashMap<Integer, List<TransferBatchAllocationCommand>>();
        for (TransferBatchAllocationCommand allocation : batchAllocations) {
            if (allocation == null || allocation.getTransferLineId() == null || allocation.getQuantity() == null
                    || allocation.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            List<TransferBatchAllocationCommand> lineAllocations = allocationsByLineId.get(allocation.getTransferLineId());
            if (lineAllocations == null) {
                lineAllocations = new ArrayList<TransferBatchAllocationCommand>();
                allocationsByLineId.put(allocation.getTransferLineId(), lineAllocations);
            }
            lineAllocations.add(allocation);
        }
        if (allocationsByLineId.isEmpty()) {
            return;
        }
        Date now = new Date();
        User currentUser = Context.getAuthenticatedUser();
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (!isActiveTransferLine(line) || line.getInterPharmacyTransferLineId() == null) {
                continue;
            }
            List<TransferBatchAllocationCommand> lineAllocations =
                    allocationsByLineId.get(line.getInterPharmacyTransferLineId());
            if (lineAllocations == null || lineAllocations.isEmpty()) {
                continue;
            }
            List<InterPharmacyTransferLineBatch> persistedAllocations = line.getBatchAllocations();
            if (persistedAllocations == null) {
                persistedAllocations = new ArrayList<InterPharmacyTransferLineBatch>();
                line.setBatchAllocations(persistedAllocations);
            } else {
                persistedAllocations.clear();
            }
            for (TransferBatchAllocationCommand allocationCommand : lineAllocations) {
                InterPharmacyTransferLineBatch allocation = new InterPharmacyTransferLineBatch();
                allocation.setInterPharmacyTransferLine(line);
                allocation.setBatchNumber(emptyToNull(allocationCommand.getBatchNumber()));
                allocation.setQuantity(allocationCommand.getQuantity());
                allocation.setCreator(currentUser);
                allocation.setDateCreated(now);
                allocation.setVoided(Boolean.FALSE);
                persistedAllocations.add(allocation);
            }
            line.setBatchNumber(null);
            line.setExpiryDate(null);
        }
    }

    private void validateRequestedQuantities(InterPharmacyTransfer transfer) {
        if (transfer == null || transfer.getSourcePharmacyLocation() == null || transfer.getLines() == null) {
            return;
        }
        Integer sourceId = transfer.getSourcePharmacyLocation().getPharmacyLocationId();
        boolean hasActiveLine = false;
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (!isActiveTransferLine(line) || line.getItem() == null || line.getItem().getItemId() == null
                    || line.getQuantity() == null) {
                continue;
            }
            hasActiveLine = true;
            if (hasActiveBatchAllocations(line)) {
                validateBatchAllocations(line, sourceId);
                continue;
            }
            BigDecimal available = getAvailableQuantity(line.getItem().getItemId(), sourceId, line.getBatchNumber());
            if (available.compareTo(line.getQuantity()) < 0) {
                throw new IllegalArgumentException("Requested quantity exceeds stock available in the supplying pharmacy");
            }
        }
        if (!hasActiveLine) {
            throw new IllegalArgumentException("At least one active request line is required");
        }
    }

    private void validateBatchAllocations(InterPharmacyTransferLine line, Integer sourcePharmacyLocationId) {
        BigDecimal totalAllocated = BigDecimal.ZERO;
        Map<String, BigDecimal> allocatedByBatch = new LinkedHashMap<String, BigDecimal>();
        for (InterPharmacyTransferLineBatch allocation : activeBatchAllocations(line)) {
            if (allocation.getQuantity() == null || allocation.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Batch allocation quantity must be greater than zero");
            }
            String batchNumber = emptyToNull(allocation.getBatchNumber());
            if (batchNumber == null) {
                throw new IllegalArgumentException("Batch number is required for split batch transfer");
            }
            totalAllocated = totalAllocated.add(allocation.getQuantity());
            BigDecimal current = allocatedByBatch.containsKey(batchNumber) ? allocatedByBatch.get(batchNumber) : BigDecimal.ZERO;
            allocatedByBatch.put(batchNumber, current.add(allocation.getQuantity()));
        }
        if (line.getQuantity() == null || totalAllocated.compareTo(line.getQuantity()) != 0) {
            throw new IllegalArgumentException("Split batch quantities must equal the transfer line quantity");
        }
        for (Map.Entry<String, BigDecimal> entry : allocatedByBatch.entrySet()) {
            BigDecimal available = getAvailableQuantity(line.getItem().getItemId(), sourcePharmacyLocationId, entry.getKey());
            if (available.compareTo(entry.getValue()) < 0) {
                throw new IllegalArgumentException("Split batch quantity exceeds stock available for batch " + entry.getKey());
            }
        }
    }

    private BigDecimal getAvailableQuantity(Integer itemId, Integer pharmacyLocationId, String batchNumber) {
        if (pharmacyLocationId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (InventoryBalance balance : inventoryDao.getInventoryForPharmacy(pharmacyLocationId)) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            if (!balance.getItem().getItemId().equals(itemId)) {
                continue;
            }
            if (batchNumber != null && !batchNumber.trim().isEmpty()) {
                if (balance.getBatchNumber() == null || !batchNumber.trim().equals(balance.getBatchNumber())) {
                    continue;
                }
            }
            total = total.add(balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand());
        }
        return total;
    }

    private void refreshTransferLineCosts(InterPharmacyTransfer transfer) {
        if (transfer == null || transfer.getSourcePharmacyLocation() == null || transfer.getLines() == null) {
            return;
        }
        Integer sourceId = transfer.getSourcePharmacyLocation().getPharmacyLocationId();
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (!isActiveTransferLine(line) || line.getItem() == null || line.getItem().getItemId() == null) {
                continue;
            }
            setTransferLineCost(line, resolveSourceUnitPrice(line.getItem().getItemId(), sourceId, line.getBatchNumber()));
        }
    }

    private boolean hasActiveTransferLines(InterPharmacyTransfer transfer) {
        if (transfer == null || transfer.getLines() == null) {
            return false;
        }
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (isActiveTransferLine(line)) {
                return true;
            }
        }
        return false;
    }

    private boolean isActiveTransferLine(InterPharmacyTransferLine line) {
        return line != null && !Boolean.TRUE.equals(line.getVoided());
    }

    private boolean hasActiveBatchAllocations(InterPharmacyTransferLine line) {
        return !activeBatchAllocations(line).isEmpty();
    }

    private List<InterPharmacyTransferLineBatch> activeBatchAllocations(InterPharmacyTransferLine line) {
        List<InterPharmacyTransferLineBatch> activeAllocations = new ArrayList<InterPharmacyTransferLineBatch>();
        if (line == null || line.getBatchAllocations() == null) {
            return activeAllocations;
        }
        for (InterPharmacyTransferLineBatch allocation : line.getBatchAllocations()) {
            if (allocation != null && !Boolean.TRUE.equals(allocation.getVoided())) {
                activeAllocations.add(allocation);
            }
        }
        return activeAllocations;
    }

    private BigDecimal resolveSourceUnitPrice(Integer itemId, Integer pharmacyLocationId, String batchNumber) {
        if (itemId == null || pharmacyLocationId == null) {
            return null;
        }
        BigDecimal fallback = null;
        for (InventoryBalance balance : inventoryDao.getInventoryForPharmacy(pharmacyLocationId)) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null
                    || !itemId.equals(balance.getItem().getItemId())) {
                continue;
            }
            if (fallback == null && balance.getUnitPrice() != null) {
                fallback = balance.getUnitPrice();
            }
            if (batchNumber != null && !batchNumber.trim().isEmpty()
                    && balance.getBatchNumber() != null && batchNumber.trim().equals(balance.getBatchNumber())) {
                return balance.getUnitPrice();
            }
        }
        return fallback;
    }

    private void setTransferLineCost(InterPharmacyTransferLine line, BigDecimal unitPrice) {
        if (line == null) {
            return;
        }
        line.setUnitPrice(unitPrice);
        line.setTotalAmount(unitPrice == null || line.getQuantity() == null ? null : unitPrice.multiply(line.getQuantity()));
    }

    private void incrementBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber, Date expiryDate,
                                  BigDecimal quantity, BigDecimal unitPrice) {
        InventoryBalance balance = inventoryDao.getInventoryBalance(itemId, pharmacyLocationId, batchNumber);
        if (balance == null) {
            balance = new InventoryBalance();
            balance.setItem(new org.openmrs.module.rwandapharmacymanagement.model.Item());
            balance.getItem().setItemId(itemId);
            balance.setPharmacyLocation(new org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation());
            balance.getPharmacyLocation().setPharmacyLocationId(pharmacyLocationId);
            balance.setBatchNumber(emptyToNull(batchNumber));
            balance.setExpiryDate(expiryDate);
            balance.setQuantityOnHand(BigDecimal.ZERO);
            balance.setUnitPrice(unitPrice);
            balance.setCreator(Context.getAuthenticatedUser());
            balance.setDateCreated(new Date());
        }
        if (unitPrice != null) {
            balance.setUnitPrice(unitPrice);
        }
        balance.setQuantityOnHand(balance.getQuantityOnHand().add(quantity));
        inventoryDao.saveInventoryBalance(balance);
    }

    private InventoryBalance decrementBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber, BigDecimal quantity) {
        InventoryBalance balance = inventoryDao.getInventoryBalance(itemId, pharmacyLocationId, batchNumber);
        if (balance == null || balance.getQuantityOnHand() == null || balance.getQuantityOnHand().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Insufficient stock for requested transfer or dispense");
        }
        balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(quantity));
        inventoryDao.saveInventoryBalance(balance);
        return balance;
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
            BigDecimal available = balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
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
            throw new IllegalArgumentException("Insufficient stock for requested transfer or dispense");
        }
        return consumedBalances;
    }

    private ConsumedBalance consumedBalance(InventoryBalance balance, BigDecimal quantity) {
        ConsumedBalance consumed = new ConsumedBalance();
        consumed.batchNumber = balance == null ? null : balance.getBatchNumber();
        consumed.expiryDate = balance == null ? null : balance.getExpiryDate();
        consumed.quantity = quantity;
        consumed.unitPrice = balance == null ? null : balance.getUnitPrice();
        return consumed;
    }

    private void saveTransaction(Integer itemId, Integer pharmacyLocationId, String batchNumber, Date expiryDate,
                                 BigDecimal quantity, StockTransactionType type, String reference, String remarks, BigDecimal unitPrice) {
        inventoryDao.saveStockTransaction(buildTransaction(itemId, pharmacyLocationId, batchNumber, expiryDate, quantity,
                type, reference, remarks, unitPrice));
    }

    private StockTransaction buildTransaction(Integer itemId, Integer pharmacyLocationId, String batchNumber, Date expiryDate,
                                              BigDecimal quantity, StockTransactionType type, String reference,
                                              String remarks, BigDecimal unitPrice) {
        StockTransaction transaction = new StockTransaction();
        transaction.setItem(new org.openmrs.module.rwandapharmacymanagement.model.Item());
        transaction.getItem().setItemId(itemId);
        transaction.setPharmacyLocation(new org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation());
        transaction.getPharmacyLocation().setPharmacyLocationId(pharmacyLocationId);
        transaction.setBatchNumber(emptyToNull(batchNumber));
        transaction.setExpiryDate(expiryDate);
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(unitPrice);
        transaction.setTotalAmount(unitPrice == null ? null : unitPrice.multiply(quantity));
        transaction.setTransactionType(type);
        transaction.setReferenceNumber(reference);
        transaction.setRemarks(remarks);
        transaction.setCreator(Context.getAuthenticatedUser());
        transaction.setPerformedBy(Context.getAuthenticatedUser());
        transaction.setDateCreated(new Date());
        return transaction;
    }

    private BigDecimal normalizePositiveQuantity(BigDecimal quantity, String message) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return quantity;
    }

    private boolean hasDisposalForWriteOff(Integer writeOffStockTransactionId) {
        String referenceNumber = String.valueOf(writeOffStockTransactionId);
        for (StockTransaction transaction : inventoryDao.getStockTransactions(null, null, null, null)) {
            if (transaction == null || transaction.getTransactionType() != StockTransactionType.DISPOSAL) {
                continue;
            }
            if (referenceNumber.equals(emptyToNull(transaction.getReferenceNumber()))) {
                return true;
            }
        }
        return false;
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static class ConsumedBalance {
        private String batchNumber;
        private Date expiryDate;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }
}
