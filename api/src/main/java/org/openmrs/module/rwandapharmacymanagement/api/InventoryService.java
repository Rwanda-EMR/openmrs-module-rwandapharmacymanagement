package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustment;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;

import java.util.Date;
import java.util.List;

public interface InventoryService extends OpenmrsService {

    List<InventoryBalance> getInventoryForPharmacy(Integer pharmacyLocationId);

    List<InventoryBalance> getAvailableInventoryForPharmacy(Integer pharmacyLocationId);

    InventoryBalance getInventoryBalance(Integer itemId, Integer pharmacyLocationId, String batchNumber);

    List<DeliveryReceiptLine> getRecentDeliveryReceiptLines();

    List<StockTransaction> getRecentStockTransactions();

    List<StockTransaction> getStockTransactions(Date fromInclusive, Date toExclusive, Integer pharmacyLocationId);

    List<StockTransaction> getStockTransactions(Integer itemId, Date fromInclusive, Date toExclusive, Integer pharmacyLocationId);

    List<InterPharmacyTransfer> getTransfers();

    List<InterPharmacyTransfer> getTransfers(String transferStatus, Integer sourcePharmacyLocationId,
                                             Integer destinationPharmacyLocationId, Integer maxResults);

    long countTransfers(String transferStatus, Integer sourcePharmacyLocationId, Integer destinationPharmacyLocationId);

    InterPharmacyTransfer getTransfer(Integer transferId);

    DeliveryReceipt getDeliveryReceipt(Integer deliveryReceiptId);

    DeliveryReceipt receiveDelivery(DeliveryReceipt deliveryReceipt);

    InterPharmacyTransfer requestTransfer(InterPharmacyTransfer transfer);

    InterPharmacyTransfer approveTransfer(Integer transferId);

    InterPharmacyTransfer approveTransfer(Integer transferId, java.math.BigDecimal[] quantities, String[] batchNumbers, java.util.Date[] expiryDates);

    InterPharmacyTransfer rejectTransfer(Integer transferId, String rejectionReason);

    InterPharmacyTransfer serveTransfer(Integer transferId);

    InterPharmacyTransfer serveTransfer(Integer transferId, java.math.BigDecimal[] quantities, String[] batchNumbers, java.util.Date[] expiryDates);

    InterPharmacyTransfer transferStock(InterPharmacyTransfer transfer);

    StockAdjustment saveStockAdjustment(StockAdjustment adjustment);

    StockTransaction saveStockTransaction(StockTransaction stockTransaction);
}
