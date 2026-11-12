package org.openmrs.module.rwandapharmacymanagement.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.PurchaseOrderPriceRefreshResult;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierAuthenticationResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierPriceLookupResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierPriceRecord;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierProductMasterProgram;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.Supplier;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrder;
import org.openmrs.module.rwandapharmacymanagement.web.dto.DeliveryReceiptImportRequest;

import java.util.List;

public interface SupplierIntegrationService extends OpenmrsService {

    SupplierAuthenticationResponse authenticate();

    IntegrationLog syncProductMaster();

    IntegrationLog syncPrices();

    IntegrationLog submitSupplierOrder(SupplierOrder supplierOrder);

    IntegrationLog submitSupplierOrder(SupplierOrder supplierOrder, Supplier supplier);

    IntegrationLog submitPurchaseOrder(PurchaseOrder purchaseOrder);

    /**
     * Returns why the purchase order cannot be sent to its supplier, or null when it can. For API suppliers every
     * line needs a program, all lines must share one program (one program per order, matching the product master),
     * and the order's program is set to it.
     */
    String getPurchaseOrderSubmissionError(PurchaseOrder purchaseOrder);

    /**
     * The reason each blocking line stops the purchase order from being sent, keyed by the line itself, so pages can
     * highlight them. Does not include missing supplier prices, which come from the price refresh.
     */
    java.util.Map<org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem, String> getPurchaseOrderLineProblems(
            PurchaseOrder purchaseOrder);

    PurchaseOrderPriceRefreshResult refreshPurchaseOrderPrices(PurchaseOrder purchaseOrder);

    DeliveryReceiptImportRequest fetchRmsDeliveryDetails(PurchaseOrder purchaseOrder);

    IntegrationLog fetchPendingRmsDeliveryDetails();

    IntegrationLog confirmDelivery(DeliveryReceipt deliveryReceipt);

    List<SupplierPriceRecord> loadSupplierPrices();

    List<SupplierProductMasterProgram> loadProductMaster();

    SupplierPriceLookupResponse lookupPrices(String program, List<String> npcCodes);
}
