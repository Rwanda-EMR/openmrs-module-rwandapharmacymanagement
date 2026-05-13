package org.openmrs.module.rwandapharmacymanagement.db;

import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceiptLine;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustment;
import org.openmrs.module.rwandapharmacymanagement.model.StockAdjustmentLine;
import org.openmrs.module.rwandapharmacymanagement.model.StockTransaction;

import java.util.Date;
import java.util.List;

public interface InventoryDao {

    List<InventoryBalance> getInventoryForPharmacy(Integer pharmacyLocationId);

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

    InventoryBalance saveInventoryBalance(InventoryBalance inventoryBalance);

    DeliveryReceipt saveDeliveryReceipt(DeliveryReceipt deliveryReceipt);

    DeliveryReceiptLine saveDeliveryReceiptLine(DeliveryReceiptLine deliveryReceiptLine);

    InterPharmacyTransfer saveTransfer(InterPharmacyTransfer transfer);

    InterPharmacyTransferLine saveTransferLine(InterPharmacyTransferLine transferLine);

    StockAdjustment saveAdjustment(StockAdjustment adjustment);

    StockAdjustmentLine saveAdjustmentLine(StockAdjustmentLine adjustmentLine);

    StockTransaction saveStockTransaction(StockTransaction stockTransaction);
}
