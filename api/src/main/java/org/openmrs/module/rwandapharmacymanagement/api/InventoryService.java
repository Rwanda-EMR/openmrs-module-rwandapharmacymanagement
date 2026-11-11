package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustment;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;
import org.openmrs.module.rwandapharmacymanagement.api.dto.TransferPreviousInfo;
import org.openmrs.module.rwandapharmacymanagement.api.dto.TransferBatchAllocationCommand;

import java.util.Date;
import java.util.List;

public interface InventoryService extends OpenmrsService {

    List<InventoryBalance> getInventoryForPharmacy(Integer pharmacyLocationId);

    List<InventoryBalance> getAvailableInventoryForPharmacy(Integer pharmacyLocationId);

    /**
     * Unexpired quantity on hand per item across all active pharmacies, including the main store.
     * Null item ids means all items.
     */
    java.util.Map<Integer, java.math.BigDecimal> getAvailableQuantitiesByItem(java.util.Collection<Integer> itemIds);

    /**
     * True when any active pharmacy holds a positive quantity of the item.
     */
    boolean hasStockInActivePharmacy(Integer itemId);

    InventoryBalance getInventoryBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber);

    InventoryBalance updateBatchExpiryDate(Integer inventoryBalanceId, Integer pharmacyLocationId, Date expiryDate);

    StockTransaction writeOffInventoryBalance(Integer inventoryBalanceId, Integer pharmacyLocationId,
                                              java.math.BigDecimal quantity, String reason);

    StockTransaction recordWrittenOffStockDisposal(Integer writeOffStockTransactionId, String disposalMethod, String notes);

    List<DeliveryReceiptLine> getRecentDeliveryReceiptLines();

    List<DeliveryReceipt> getDeliveryReceipts(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId,
                                              Integer purchaseOrderId);

    List<StockTransaction> getRecentStockTransactions();

    StockTransaction getStockTransaction(Integer stockTransactionId);

    List<StockTransaction> getStockTransactions(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId);

    List<StockTransaction> getStockTransactions(Integer itemId, Date fromInclusive, Date toExclusive, Integer pharmacyLocationId);

    List<InterPharmacyTransfer> getTransfers();

    List<InterPharmacyTransfer> getTransfers(String transferStatus, Integer sourcePharmacyLocationId,
                                             Integer destinationPharmacyLocationId, Integer maxResults);

    long countTransfers(String transferStatus, Integer sourcePharmacyLocationId, Integer destinationPharmacyLocationId);

    InterPharmacyTransfer getTransfer(Integer transferId);

    TransferPreviousInfo getTransferPreviousInfo(Integer transferId, Integer transferLineId);

    DeliveryReceipt getDeliveryReceipt(Integer deliveryReceiptId);

    DeliveryReceipt receiveDelivery(DeliveryReceipt deliveryReceipt);

    InterPharmacyTransfer requestTransfer(InterPharmacyTransfer transfer);

    InterPharmacyTransfer approveTransfer(Integer transferId);

    InterPharmacyTransfer approveTransfer(Integer transferId, java.math.BigDecimal[] quantities, String[] batchNumbers, java.util.Date[] expiryDates);

    InterPharmacyTransfer removeTransferLine(Integer transferId, Integer transferLineId, String reason);

    InterPharmacyTransfer rejectTransfer(Integer transferId, String rejectionReason);

    InterPharmacyTransfer serveTransfer(Integer transferId);

    InterPharmacyTransfer serveTransfer(Integer transferId, java.math.BigDecimal[] quantities, String[] batchNumbers, java.util.Date[] expiryDates);

    InterPharmacyTransfer serveTransfer(Integer transferId, java.math.BigDecimal[] quantities, String[] batchNumbers,
                                        java.util.Date[] expiryDates, List<TransferBatchAllocationCommand> batchAllocations);

    InterPharmacyTransfer transferStock(InterPharmacyTransfer transfer);

    StockAdjustment saveStockAdjustment(StockAdjustment adjustment);

    StockTransaction saveStockTransaction(StockTransaction stockTransaction);
}
