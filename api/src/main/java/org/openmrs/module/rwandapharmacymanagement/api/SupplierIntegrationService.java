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

    PurchaseOrderPriceRefreshResult refreshPurchaseOrderPrices(PurchaseOrder purchaseOrder);

    DeliveryReceiptImportRequest fetchRmsDeliveryDetails(PurchaseOrder purchaseOrder);

    IntegrationLog fetchPendingRmsDeliveryDetails();

    IntegrationLog confirmDelivery(DeliveryReceipt deliveryReceipt);

    List<SupplierPriceRecord> loadSupplierPrices();

    List<SupplierProductMasterProgram> loadProductMaster();

    SupplierPriceLookupResponse lookupPrices(String program, List<String> npcCodes);
}
