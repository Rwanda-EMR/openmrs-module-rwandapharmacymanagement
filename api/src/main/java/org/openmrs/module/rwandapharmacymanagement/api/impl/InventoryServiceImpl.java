package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.context.Context;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.db.InventoryDao;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
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
import java.util.List;
import java.util.Set;

@Transactional
public class InventoryServiceImpl extends BaseOpenmrsService implements InventoryService {

    private InventoryDao inventoryDao;
    private PharmacyService pharmacyService;

    public void setInventoryDao(InventoryDao inventoryDao) {
        this.inventoryDao = inventoryDao;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
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
    public InventoryBalance getInventoryBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber) {
        return inventoryDao.getInventoryBalance(itemId, pharmacyLocationId, batchNumber);
    }

    @Override
    public List<DeliveryReceiptLine> getRecentDeliveryReceiptLines() {
        return inventoryDao.getRecentDeliveryReceiptLines();
    }

    @Override
    public List<StockTransaction> getRecentStockTransactions() {
        return inventoryDao.getRecentStockTransactions();
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
                if (line.getUnitPrice() == null && line.getItem() != null) {
                    SupplierPrice latestPrice = pharmacyService.getLatestSupplierPrice(line.getItem().getCode());
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
            for (DeliveryReceiptLine line : deliveryReceipt.getLines()) {
                incrementBalance(line.getItem().getItemId(),
                        deliveryReceipt.getReceivedIntoLocation().getPharmacyLocationId(),
                        line.getBatchNumber(), line.getExpiryDate(), line.getAcceptedQuantity(), line.getUnitPrice());
                saveTransaction(line.getItem().getItemId(),
                        deliveryReceipt.getReceivedIntoLocation().getPharmacyLocationId(),
                        line.getBatchNumber(), line.getExpiryDate(), line.getAcceptedQuantity(),
                        StockTransactionType.DELIVERY_RECEIPT, deliveryReceipt.getReceiptNumber(), "Delivery received",
                        line.getUnitPrice());
            }
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
    public InterPharmacyTransfer serveTransfer(Integer transferId) {
        return serveTransfer(transferId, null, null, null);
    }

    @Override
    public InterPharmacyTransfer serveTransfer(Integer transferId, BigDecimal[] quantities, String[] batchNumbers, Date[] expiryDates) {
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
            List<ConsumedBalance> consumedBalances = decrementBalances(line.getItem().getItemId(),
                    transfer.getSourcePharmacyLocation().getPharmacyLocationId(), line.getBatchNumber(), line.getQuantity());
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

    private void applyServedLineUpdates(InterPharmacyTransfer transfer, BigDecimal[] quantities, String[] batchNumbers, Date[] expiryDates) {
        if (transfer.getLines() == null) {
            return;
        }
        for (int i = 0; i < transfer.getLines().size(); i++) {
            InterPharmacyTransferLine line = transfer.getLines().get(i);
            if (quantities != null && quantities.length > i && quantities[i] != null && quantities[i].compareTo(BigDecimal.ZERO) > 0) {
                line.setQuantity(quantities[i]);
            }
            if (batchNumbers != null && batchNumbers.length > i) {
                line.setBatchNumber(emptyToNull(batchNumbers[i]));
            }
            if (expiryDates != null && expiryDates.length > i) {
                line.setExpiryDate(expiryDates[i]);
            }
        }
    }

    private void validateRequestedQuantities(InterPharmacyTransfer transfer) {
        if (transfer == null || transfer.getSourcePharmacyLocation() == null || transfer.getLines() == null) {
            return;
        }
        Integer sourceId = transfer.getSourcePharmacyLocation().getPharmacyLocationId();
        for (InterPharmacyTransferLine line : transfer.getLines()) {
            if (line == null || line.getItem() == null || line.getItem().getItemId() == null || line.getQuantity() == null) {
                continue;
            }
            BigDecimal available = getAvailableQuantity(line.getItem().getItemId(), sourceId, line.getBatchNumber());
            if (available.compareTo(line.getQuantity()) < 0) {
                throw new IllegalArgumentException("Requested quantity exceeds stock available in the supplying pharmacy");
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
            if (line == null || line.getItem() == null || line.getItem().getItemId() == null) {
                continue;
            }
            setTransferLineCost(line, resolveSourceUnitPrice(line.getItem().getItemId(), sourceId, line.getBatchNumber()));
        }
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
        inventoryDao.saveStockTransaction(transaction);
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
