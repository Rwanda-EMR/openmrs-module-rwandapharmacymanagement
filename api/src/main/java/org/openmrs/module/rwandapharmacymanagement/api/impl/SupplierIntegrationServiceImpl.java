package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Drug;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.notification.Message;
import org.openmrs.notification.MessageException;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.SupplierIntegrationService;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.SupplierApiClient;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.SupplierPayloadBuilder;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.PurchaseOrderPriceRefreshResult;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierAuthenticationResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierDeliveryDetailsItem;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierDeliveryDetailsResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierMatchedPrice;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierPriceLookupResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierPriceRecord;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierProductMasterProgram;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.Supplier;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrder;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierPrice;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierProductMapping;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierOrderLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.openmrs.module.rwandapharmacymanagement.reporting.PurchaseOrderPdfRenderer;
import org.openmrs.module.rwandapharmacymanagement.reporting.DeliveryReceiptTemplateRenderer;
import org.openmrs.module.rwandapharmacymanagement.util.UserNameUtils;
import org.openmrs.module.rwandapharmacymanagement.web.dto.DeliveryReceiptImportItem;
import org.openmrs.module.rwandapharmacymanagement.web.dto.DeliveryReceiptImportRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.PasswordAuthentication;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

@Transactional
public class SupplierIntegrationServiceImpl extends BaseOpenmrsService implements SupplierIntegrationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AdministrationService administrationService;
    private IntegrationLogService integrationLogService;
    private PharmacyService pharmacyService;
    private SupplierApiClient supplierApiClient;
    private org.openmrs.module.rwandapharmacymanagement.api.ItemRenameService itemRenameService;

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    public void setIntegrationLogService(IntegrationLogService integrationLogService) {
        this.integrationLogService = integrationLogService;
    }

    public void setPharmacyService(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    public void setSupplierApiClient(SupplierApiClient supplierApiClient) {
        this.supplierApiClient = supplierApiClient;
    }

    public void setItemRenameService(org.openmrs.module.rwandapharmacymanagement.api.ItemRenameService itemRenameService) {
        this.itemRenameService = itemRenameService;
    }

    @Override
    public SupplierAuthenticationResponse authenticate() {
        return authenticateConfiguredSupplier();
    }

    private SupplierAuthenticationResponse authenticateConfiguredSupplier() {
        long start = System.currentTimeMillis();
        String baseUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL);
        String username = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_USERNAME);
        String password = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_PASSWORD);
        Integer connectTimeout = integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000);
        Integer readTimeout = integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000);
        SupplierAuthenticationResponse authentication = supplierApiClient.authenticate(
                baseUrl,
                username,
                password,
                connectTimeout,
                readTimeout);
        if (authentication.getExpiresAt() == null) {
            authentication.setExpiresAt(new Date(System.currentTimeMillis() + (15 * 60 * 1000L)));
        }
        IntegrationLog log = createLog(IntegrationType.AUTHENTICATION,
                authentication.isAuthenticated() ? IntegrationStatus.SUCCESS : IntegrationStatus.FAILED,
                authentication.isAuthenticated() ? "Supplier authentication succeeded" : safe(authentication.getErrorMessage()));
        log.setEndpoint(buildEndpoint(baseUrl, "/api/authenticate"));
        log.setRequestPayload(truncate(buildAuthenticationPayload(username, password)));
        log.setDurationMs(System.currentTimeMillis() - start);
        log.setResponsePayload(truncate(authentication.getRawResponse()));
        integrationLogService.saveLog(log);
        return authentication;
    }

    private SupplierAuthenticationResponse authenticateSupplier(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier is required for API authentication");
        }
        long start = System.currentTimeMillis();
        String baseUrl = supplier.getApiBaseUrl();
        String username = supplier.getApiUsername();
        String password = supplier.getApiPassword();
        SupplierAuthenticationResponse authentication = supplierApiClient.authenticate(
                baseUrl,
                username,
                password,
                supplier.getConnectionTimeout(),
                supplier.getReadTimeout());
        if (authentication.getExpiresAt() == null) {
            authentication.setExpiresAt(new Date(System.currentTimeMillis() + (15 * 60 * 1000L)));
        }
        IntegrationLog log = createLog(IntegrationType.AUTHENTICATION,
                authentication.isAuthenticated() ? IntegrationStatus.SUCCESS : IntegrationStatus.FAILED,
                authentication.isAuthenticated() ? "Supplier authentication succeeded" : safe(authentication.getErrorMessage()));
        log.setEndpoint(buildEndpoint(baseUrl, "/api/authenticate"));
        log.setRequestPayload(truncate(buildAuthenticationPayload(username, password)));
        log.setDurationMs(System.currentTimeMillis() - start);
        log.setResponsePayload(truncate(authentication.getRawResponse()));
        integrationLogService.saveLog(log);
        return authentication;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IntegrationLog syncProductMaster() {
        String baseUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL);
        long start = System.currentTimeMillis();
        try {
            SupplierAuthenticationResponse auth = authenticate();
            List<SupplierProductMasterProgram> response = supplierApiClient.getProductMaster(baseUrl, auth.getToken(),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000));
            persistProductMappings(response);
            IntegrationLog log = createLog(IntegrationType.PRODUCT_MASTER_SYNC, IntegrationStatus.SUCCESS,
                    "Product master sync completed");
            log.setEndpoint(buildEndpoint(baseUrl, "/api/integration/product-master"));
            log.setDurationMs(System.currentTimeMillis() - start);
            log.setResponsePayload(truncate(String.valueOf(response)));
            return integrationLogService.saveLog(log);
        }
        catch (Exception e) {
            return saveFailedLog(IntegrationType.PRODUCT_MASTER_SYNC, baseUrl,
                    "/api/integration/product-master", start, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IntegrationLog syncPrices() {
        String baseUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL);
        long start = System.currentTimeMillis();
        try {
            SupplierAuthenticationResponse auth = authenticate();
            List<SupplierPriceRecord> response = supplierApiClient.loadPrices(baseUrl, auth.getToken(),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000));
            persistSupplierPrices(response);
            IntegrationLog log = createLog(IntegrationType.PRICE_SYNC, IntegrationStatus.SUCCESS, "Price sync completed");
            log.setEndpoint(buildEndpoint(baseUrl, "/api/integration/load-prices"));
            log.setDurationMs(System.currentTimeMillis() - start);
            log.setResponsePayload(truncate(String.valueOf(response)));
            return integrationLogService.saveLog(log);
        }
        catch (IOException e) {
            return integrationLogService.saveLog(errorLog(IntegrationType.PRICE_SYNC, baseUrl,
                    "/api/integration/load-prices", start, e));
        }
    }

    @Override
    public IntegrationLog submitSupplierOrder(SupplierOrder supplierOrder) {
        Supplier supplier = supplierOrder == null ? null : supplierOrder.getSupplier();
        if (supplier == null) {
            supplier = pharmacyService.getSupplierByCode("RMS");
        }
        return submitSupplierOrder(supplierOrder, supplier);
    }

    @Override
    public IntegrationLog submitSupplierOrder(SupplierOrder supplierOrder, Supplier supplier) {
        if (supplierOrder != null) {
            supplierOrder.setSupplier(supplier);
        }
        if (supplier == null) {
            throw new IllegalStateException("No supplier selected for this order");
        }
        if (!Boolean.TRUE.equals(supplier.getApiEnabled())) {
            return submitSupplierOrderByEmail(supplierOrder, supplier);
        }
        String baseUrl = supplier.getApiBaseUrl();
        String facilityId = facilityId();
        long start = System.currentTimeMillis();
        try {
            prepareSupplierOrderForPersistence(supplierOrder);
            pharmacyService.saveSupplierOrder(supplierOrder);
            SupplierAuthenticationResponse auth = authenticateSupplier(supplier);
            if (!auth.isAuthenticated()) {
                throw new IOException(auth.getErrorMessage());
            }
            String payload = SupplierPayloadBuilder.buildSubmitOrderPayload(supplierOrder, facilityId);
            Map<String, Object> response = supplierApiClient.submitOrder(baseUrl, auth.getToken(), payload,
                    supplier.getConnectionTimeout(),
                    supplier.getReadTimeout());
            supplierOrder.setStatus("SUBMITTED");
            supplierOrder.setSupplierOrderReference(response == null ? null : stringValue(response.get("rawResponse")));
            pharmacyService.saveSupplierOrder(supplierOrder);
            IntegrationLog log = createLog(IntegrationType.ORDER_SUBMISSION, IntegrationStatus.SUCCESS,
                    "Supplier order submission completed");
            log.setEndpoint(buildEndpoint(baseUrl, "/api/integration/submit-order"));
            log.setRequestPayload(truncate(payload));
            log.setResponsePayload(truncate(String.valueOf(response)));
            log.setDurationMs(System.currentTimeMillis() - start);
            return integrationLogService.saveLog(log);
        }
        catch (IOException e) {
            return integrationLogService.saveLog(errorLog(IntegrationType.ORDER_SUBMISSION, baseUrl,
                    "/api/integration/submit-order", start, e));
        }
    }

    @Override
    public IntegrationLog submitPurchaseOrder(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            throw new IllegalArgumentException("Purchase order is required");
        }
        String productMasterValidationError = validatePurchaseOrderProductMasterItems(purchaseOrder);
        if (!isBlank(productMasterValidationError)) {
            IntegrationLog log = createLog(IntegrationType.ORDER_SUBMISSION, IntegrationStatus.FAILED,
                    productMasterValidationError);
            purchaseOrder.setDispatchStatus("FAILED");
            purchaseOrder.setDispatchReference(productMasterValidationError);
            pharmacyService.savePurchaseOrder(purchaseOrder);
            return integrationLogService.saveLog(log);
        }
        if (isRmsPurchaseOrder(purchaseOrder)) {
            PurchaseOrderPriceRefreshResult refreshResult = refreshPurchaseOrderPrices(purchaseOrder);
            if (refreshResult.hasBlockingIssue()) {
                String message = refreshResult.getErrorMessage() == null
                        ? "Price lookup did not return prices for: " + refreshResult.getUnmatchedNpcCodes()
                        : refreshResult.getErrorMessage();
                IntegrationLog log = createLog(IntegrationType.ORDER_SUBMISSION, IntegrationStatus.FAILED, message);
                log.setEndpoint(buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL),
                        "/api/integration/submit-order"));
                purchaseOrder.setDispatchStatus("FAILED");
                purchaseOrder.setDispatchReference(message);
                pharmacyService.savePurchaseOrder(purchaseOrder);
                return integrationLogService.saveLog(log);
            }
        }
        Supplier supplier = purchaseOrder.getSupplier();
        if (supplier == null) {
            throw new IllegalStateException("Purchase order does not have a supplier");
        }
        SupplierOrder supplierOrder = buildSupplierOrder(purchaseOrder, supplier);
        IntegrationLog log = Boolean.TRUE.equals(supplier.getApiEnabled())
                ? submitSupplierOrder(supplierOrder, supplier)
                : submitSupplierOrderByEmail(supplierOrder, supplier, purchaseOrder);
        if (log.getStatus() == IntegrationStatus.SUCCESS) {
            purchaseOrder.setDispatchStatus("DISPATCHED");
            purchaseOrder.setDispatchReference(supplierOrder.getSupplierOrderReference());
        } else {
            purchaseOrder.setDispatchStatus("FAILED");
            purchaseOrder.setDispatchReference(log.getSummary());
        }
        pharmacyService.savePurchaseOrder(purchaseOrder);
        return log;
    }

    @Override
    public PurchaseOrderPriceRefreshResult refreshPurchaseOrderPrices(PurchaseOrder purchaseOrder) {
        PurchaseOrderPriceRefreshResult result = new PurchaseOrderPriceRefreshResult();
        if (purchaseOrder == null) {
            result.setErrorMessage("Purchase order is required");
            return result;
        }
        if (purchaseOrder.getItems() == null || purchaseOrder.getItems().isEmpty()) {
            purchaseOrder.setTotalCost(BigDecimal.ZERO);
            pharmacyService.savePurchaseOrder(purchaseOrder);
            return result;
        }
        String facilityId = normalized(purchaseOrder.getFacilityId());
        String program = normalized(purchaseOrder.getProgram());
        if (isBlank(program)) {
            program = normalized(purchaseOrder.getItems().get(0).getProgram());
        }
        LinkedHashSet<String> npcCodes = new LinkedHashSet<String>();
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            String npcCode = normalized(item.getItemCode());
            if (!isBlank(npcCode)) {
                npcCodes.add(npcCode);
            }
        }
        if (isBlank(facilityId) || isBlank(program) || npcCodes.isEmpty()) {
            result.setErrorMessage("Facility ID, program, and item codes are required for price lookup");
            return result;
        }
        SupplierPriceLookupResponse response;
        try {
            response = lookupPrices(facilityId, program, new ArrayList<String>(npcCodes));
        }
        catch (RuntimeException e) {
            result.setErrorMessage("Unable to refresh purchase order prices: " + e.getMessage());
            return result;
        }
        Map<String, BigDecimal> pricesByNpcCode = new HashMap<String, BigDecimal>();
        if (response.getMatchedPrices() != null) {
            for (SupplierMatchedPrice matchedPrice : response.getMatchedPrices()) {
                if (!isBlank(matchedPrice.getNpcCode()) && matchedPrice.getSellingPrice() != null) {
                    pricesByNpcCode.put(normalizedPriceCode(matchedPrice.getNpcCode()),
                            BigDecimal.valueOf(matchedPrice.getSellingPrice()));
                }
            }
        }
        List<String> unmatched = new ArrayList<String>();
        for (String npcCode : npcCodes) {
            if (!pricesByNpcCode.containsKey(normalizedPriceCode(npcCode))) {
                unmatched.add(npcCode);
            }
        }
        if (response.getUnmatchedNpcCodes() != null) {
            for (String unmatchedNpcCode : response.getUnmatchedNpcCodes()) {
                String normalizedUnmatched = normalized(unmatchedNpcCode);
                if (!isBlank(normalizedUnmatched) && !containsIgnoreCase(unmatched, normalizedUnmatched)) {
                    unmatched.add(normalizedUnmatched);
                }
            }
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            BigDecimal quantity = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal unitPrice = pricesByNpcCode.get(normalizedPriceCode(item.getItemCode()));
            if (unitPrice != null) {
                item.setUnitPrice(unitPrice);
                result.incrementUpdatedCount();
            }
            BigDecimal lineTotal = (item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice()).multiply(quantity);
            item.setTotalPrice(lineTotal);
            totalCost = totalCost.add(lineTotal);
        }
        purchaseOrder.setTotalCost(totalCost);
        result.setUnmatchedNpcCodes(unmatched);
        pharmacyService.savePurchaseOrder(purchaseOrder);
        return result;
    }

    @Override
    public DeliveryReceiptImportRequest fetchRmsDeliveryDetails(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            throw new IllegalArgumentException("Purchase order is required");
        }
        if (purchaseOrder.getSupplier() == null || !Boolean.TRUE.equals(purchaseOrder.getSupplier().getApiEnabled())) {
            throw new IllegalArgumentException("Selected purchase order is not configured for RMS delivery fetch");
        }
        if (!"APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus())) {
            throw new IllegalArgumentException("Only approved purchase orders can fetch RMS delivery details");
        }

        String baseUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL);
        String facilityId = facilityId();
        String orderNumber = resolveRmsOrderNumber(purchaseOrder);
        long start = System.currentTimeMillis();
        String payload = SupplierPayloadBuilder.buildDeliveryDetailsPayload(facilityId, orderNumber);

        try {
            SupplierAuthenticationResponse auth = authenticate();
            SupplierDeliveryDetailsResponse response = supplierApiClient.getDeliveryDetails(baseUrl, auth.getToken(), payload,
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000));
            DeliveryReceiptImportRequest preview = mapDeliveryDetails(purchaseOrder, response, orderNumber);
            purchaseOrder.setDispatchStatus("FETCHED");
            purchaseOrder.setFetchedReceiptPayload(objectMapper.writeValueAsString(preview));
            purchaseOrder.setFetchedReceiptDate(new Date());
            pharmacyService.savePurchaseOrder(purchaseOrder);

            IntegrationLog log = createLog(IntegrationType.DELIVERY_DETAILS_FETCH, IntegrationStatus.SUCCESS,
                    "RMS delivery details fetched for " + orderNumber);
            log.setEndpoint(buildEndpoint(baseUrl, "/api/integration/delivery-details"));
            log.setRequestPayload(truncate(payload));
            log.setResponsePayload(truncate(objectMapper.writeValueAsString(response)));
            log.setDurationMs(System.currentTimeMillis() - start);
            integrationLogService.saveLog(log);
            return preview;
        }
        catch (IOException e) {
            integrationLogService.saveLog(errorLog(IntegrationType.DELIVERY_DETAILS_FETCH, baseUrl,
                    "/api/integration/delivery-details", start, e));
            throw new IllegalStateException("Unable to fetch RMS delivery details: " + e.getMessage(), e);
        }
    }

    @Override
    public IntegrationLog fetchPendingRmsDeliveryDetails() {
        long start = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;
        for (PurchaseOrder purchaseOrder : pharmacyService.getPurchaseOrders("APPROVED", null, null, 0, 500)) {
            if (!isEligibleForRmsDeliveryFetch(purchaseOrder)) {
                continue;
            }
            try {
                fetchRmsDeliveryDetails(purchaseOrder);
                successCount++;
            }
            catch (Exception e) {
                failureCount++;
            }
        }

        IntegrationStatus status = failureCount == 0 ? IntegrationStatus.SUCCESS
                : (successCount == 0 ? IntegrationStatus.FAILED : IntegrationStatus.SKIPPED);
        IntegrationLog log = createLog(IntegrationType.DELIVERY_DETAILS_FETCH, status,
                "RMS delivery fetch scheduler checked " + (successCount + failureCount)
                        + " order(s); success=" + successCount + ", failed=" + failureCount);
        log.setDurationMs(System.currentTimeMillis() - start);
        return integrationLogService.saveLog(log);
    }

    private IntegrationLog submitSupplierOrderByEmail(SupplierOrder supplierOrder, Supplier supplier) {
        long start = System.currentTimeMillis();
        try {
            prepareSupplierOrderForPersistence(supplierOrder);
            pharmacyService.saveSupplierOrder(supplierOrder);
            sendOrderEmail(supplierOrder, supplier);
            supplierOrder.setStatus("EMAIL_SENT");
            supplierOrder.setSupplierOrderReference("EMAIL:" + supplier.getEmailAddress());
            pharmacyService.saveSupplierOrder(supplierOrder);
            IntegrationLog log = createLog(IntegrationType.ORDER_SUBMISSION, IntegrationStatus.SUCCESS,
                    "Supplier order emailed successfully");
            log.setEndpoint("mailto:" + supplier.getEmailAddress());
            log.setRequestPayload(truncate(buildEmailBody(supplierOrder, supplier, resolveHospitalName())));
            log.setDurationMs(System.currentTimeMillis() - start);
            return integrationLogService.saveLog(log);
        }
        catch (Exception e) {
            return integrationLogService.saveLog(errorLog(IntegrationType.ORDER_SUBMISSION,
                    "mailto:" + supplier.getEmailAddress(), "/email", start,
                    e instanceof IOException ? (IOException) e : new IOException(e.getMessage(), e)));
        }
    }

    private IntegrationLog submitSupplierOrderByEmail(SupplierOrder supplierOrder, Supplier supplier, PurchaseOrder purchaseOrder) {
        long start = System.currentTimeMillis();
        try {
            prepareSupplierOrderForPersistence(supplierOrder);
            pharmacyService.saveSupplierOrder(supplierOrder);
            sendPurchaseOrderEmail(purchaseOrder, supplierOrder, supplier);
            supplierOrder.setStatus("EMAIL_SENT");
            supplierOrder.setSupplierOrderReference("EMAIL:" + supplier.getEmailAddress());
            pharmacyService.saveSupplierOrder(supplierOrder);
            IntegrationLog log = createLog(IntegrationType.ORDER_SUBMISSION, IntegrationStatus.SUCCESS,
                    "Supplier order emailed successfully");
            log.setEndpoint("mailto:" + supplier.getEmailAddress());
            log.setRequestPayload(truncate(buildEmailBody(supplierOrder, supplier, resolveHospitalName())));
            log.setDurationMs(System.currentTimeMillis() - start);
            return integrationLogService.saveLog(log);
        }
        catch (Exception e) {
            return integrationLogService.saveLog(errorLog(IntegrationType.ORDER_SUBMISSION,
                    "mailto:" + supplier.getEmailAddress(), "/email", start,
                    e instanceof IOException ? (IOException) e : new IOException(e.getMessage(), e)));
        }
    }

    @Override
    public IntegrationLog confirmDelivery(DeliveryReceipt deliveryReceipt) {
        String baseUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL);
        String facilityId = facilityId();
        long start = System.currentTimeMillis();
        try {
            SupplierAuthenticationResponse auth = authenticate();
            String payload = SupplierPayloadBuilder.buildDeliveryConfirmationPayload(deliveryReceipt, facilityId);
            Map<String, Object> response = supplierApiClient.confirmDelivery(baseUrl, auth.getToken(), payload,
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000));
            IntegrationLog log = createLog(IntegrationType.DELIVERY_CONFIRMATION, IntegrationStatus.SUCCESS,
                    "Delivery confirmation submitted");
            log.setEndpoint(buildEndpoint(baseUrl, "/api/integration/delivery-confirmation"));
            log.setRequestPayload(truncate(payload));
            log.setResponsePayload(truncate(String.valueOf(response)));
            log.setDurationMs(System.currentTimeMillis() - start);
            return integrationLogService.saveLog(log);
        }
        catch (IOException e) {
            return integrationLogService.saveLog(errorLog(IntegrationType.DELIVERY_CONFIRMATION, baseUrl,
                    "/api/integration/delivery-confirmation", start, e));
        }
    }

    @Override
    public List<SupplierPriceRecord> loadSupplierPrices() {
        try {
            List<SupplierPriceRecord> response = supplierApiClient.loadPrices(
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL),
                    authenticate().getToken(),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000));
            persistSupplierPrices(response);
            return response;
        }
        catch (IOException e) {
            integrationLogService.saveLog(errorLog(IntegrationType.PRICE_SYNC,
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL),
                    "/api/integration/load-prices", System.currentTimeMillis(), e));
            throw new IllegalStateException("Unable to load supplier prices: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SupplierProductMasterProgram> loadProductMaster() {
        try {
            List<SupplierProductMasterProgram> response = supplierApiClient.getProductMaster(
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL),
                    authenticate().getToken(),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000));
            persistProductMappings(response);
            return response;
        }
        catch (IOException e) {
            integrationLogService.saveLog(errorLog(IntegrationType.PRODUCT_MASTER_SYNC,
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL),
                    "/api/integration/product-master", System.currentTimeMillis(), e));
            throw new IllegalStateException("Unable to load product master: " + e.getMessage(), e);
        }
    }

    @Override
    public SupplierPriceLookupResponse lookupPrices(String program, List<String> npcCodes) {
        String facilityId = facilityId();
        return lookupPrices(facilityId, program, npcCodes);
    }

    private SupplierPriceLookupResponse lookupPrices(String facilityId, String program, List<String> npcCodes) {
        String baseUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL);
        long start = System.currentTimeMillis();
        try {
            SupplierAuthenticationResponse auth = authenticate();
            String payload = SupplierPayloadBuilder.buildPriceLookupPayload(facilityId, program, npcCodes);
            SupplierPriceLookupResponse response = supplierApiClient.lookupPrices(baseUrl, auth.getToken(), payload,
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, 30000));
            IntegrationLog log = createLog(IntegrationType.PRICE_SYNC, IntegrationStatus.SUCCESS, "Price lookup completed");
            log.setEndpoint(buildEndpoint(baseUrl, "/api/integration/prices"));
            log.setRequestPayload(truncate(payload));
            log.setResponsePayload(truncate(String.valueOf(response.getMatchedPrices())));
            log.setDurationMs(System.currentTimeMillis() - start);
            integrationLogService.saveLog(log);
            return response;
        }
        catch (IOException e) {
            integrationLogService.saveLog(errorLog(IntegrationType.PRICE_SYNC, baseUrl, "/api/integration/prices", start, e));
            throw new IllegalStateException("Unable to lookup supplier prices: " + e.getMessage(), e);
        }
    }

    private String normalizedPriceCode(String value) {
        return normalized(value).toUpperCase(Locale.ENGLISH);
    }

    private boolean containsIgnoreCase(List<String> values, String value) {
        if (values == null || value == null) {
            return false;
        }
        for (String existing : values) {
            if (value.equalsIgnoreCase(existing)) {
                return true;
            }
        }
        return false;
    }

    private IntegrationLog createLog(IntegrationType type, IntegrationStatus status, String summary) {
        IntegrationLog log = new IntegrationLog();
        log.setIntegrationType(type);
        log.setStatus(status);
        log.setSummary(summary);
        log.setCreator(Context.getAuthenticatedUser());
        log.setDateCreated(new Date());
        return log;
    }

    private IntegrationLog errorLog(IntegrationType type, String baseUrl, String path, long start, Exception e) {
        IntegrationLog log = createLog(type, IntegrationStatus.FAILED, safe(e.getMessage()));
        log.setEndpoint(buildEndpoint(baseUrl, path));
        log.setDurationMs(System.currentTimeMillis() - start);
        return log;
    }

    private Integer integerProperty(String property, int defaultValue) {
        try {
            return Integer.valueOf(administrationService.getGlobalProperty(property, String.valueOf(defaultValue)));
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    private String buildEndpoint(String baseUrl, String path) {
        if (baseUrl == null) {
            return path;
        }
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + path;
    }

    private String buildAuthenticationPayload(String username, String password) {
        return "{\"username\":\"" + escapeJson(safe(username)) + "\",\"password\":\""
                + escapeJson(safe(password)) + "\"}";
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 4000 ? value.substring(0, 4000) : value;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String facilityId() {
        String facilityId = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID, "");
        if (facilityId == null || facilityId.trim().isEmpty()) {
            facilityId = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID_LEGACY, "1660");
        }
        return facilityId;
    }

    private String normalized(String value) {
        return safe(value).trim();
    }

    private DeliveryReceiptImportRequest mapDeliveryDetails(PurchaseOrder purchaseOrder,
                                                            SupplierDeliveryDetailsResponse response,
                                                            String fallbackOrderNumber) {
        if (response == null) {
            throw new IllegalStateException("RMS delivery details response was empty");
        }
        if (response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalStateException("RMS returned no delivery line items for order " + fallbackOrderNumber);
        }

        DeliveryReceiptImportRequest request = new DeliveryReceiptImportRequest();
        request.setPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        request.setOrderNo(isBlank(response.getOrderNo()) ? fallbackOrderNumber : response.getOrderNo().trim());
        request.setCustomerCode(trimToNull(response.getCustomerCode()));
        request.setCustomerName(trimToNull(response.getCustomerName()));
        request.setDeliveryId(trimToNull(response.getDeliveryId()));
        request.setDeliveryDate(trimToNull(response.getDeliveryDate()));

        List<DeliveryReceiptImportItem> items = new ArrayList<DeliveryReceiptImportItem>();
        for (SupplierDeliveryDetailsItem responseItem : response.getItems()) {
            if (responseItem == null) {
                continue;
            }
            PurchaseOrderItem matchingOrderItem = findMatchingPurchaseOrderItem(purchaseOrder, responseItem);
            String resolvedItemCode = resolveDeliveryItemCode(responseItem, matchingOrderItem);
            if (isBlank(resolvedItemCode)) {
                continue;
            }
            DeliveryReceiptImportItem item = new DeliveryReceiptImportItem();
            item.setNpcCode(resolvedItemCode);
            item.setProductMasterItemId(matchingOrderItem == null || matchingOrderItem.getProductMasterItem() == null
                    ? null : matchingOrderItem.getProductMasterItem().getProductMasterItemId());
            item.setProgram(matchingOrderItem == null ? null : matchingOrderItem.getProgram());
            item.setDescription(firstNonBlank(trimToNull(responseItem.getDescription()),
                    matchingOrderItem == null ? null : trimToNull(matchingOrderItem.getItemName())));
            item.setDeliveryQuantity(parseDecimal(responseItem.getDeliveryQuantity()));
            item.setUnits(trimToNull(responseItem.getUnits()));
            item.setBatch(trimToNull(responseItem.getBatch()));
            item.setExpiryDate(trimToNull(responseItem.getExpiryDate()));
            item.setItemNo(trimToNull(responseItem.getItemNo()));
            items.add(item);
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("RMS returned delivery lines, but none had a usable npcCode");
        }
        request.setItems(items);
        return request;
    }

    private PurchaseOrderItem findMatchingPurchaseOrderItem(PurchaseOrder purchaseOrder,
                                                            SupplierDeliveryDetailsItem responseItem) {
        if (purchaseOrder == null || purchaseOrder.getItems() == null || responseItem == null) {
            return null;
        }

        String itemNo = trimToNull(responseItem.getItemNo());
        if (!isBlank(itemNo)) {
            for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getItems()) {
                if (purchaseOrderItem != null && sameText(itemNo, purchaseOrderItem.getOrderNumber())) {
                    return purchaseOrderItem;
                }
            }
        }

        String description = trimToNull(responseItem.getDescription());
        if (!isBlank(description)) {
            for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getItems()) {
                if (purchaseOrderItem != null && sameText(description, purchaseOrderItem.getItemName())) {
                    return purchaseOrderItem;
                }
            }
        }

        String npcCode = trimToNull(responseItem.getNpcCode());
        if (!isBlank(npcCode)) {
            for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getItems()) {
                if (purchaseOrderItem != null && sameText(npcCode, purchaseOrderItem.getItemCode())) {
                    return purchaseOrderItem;
                }
            }
        }
        return null;
    }

    private String resolveDeliveryItemCode(SupplierDeliveryDetailsItem responseItem, PurchaseOrderItem matchingOrderItem) {
        String npcCode = trimToNull(responseItem == null ? null : responseItem.getNpcCode());
        if (matchingOrderItem == null || isBlank(matchingOrderItem.getItemCode())) {
            return npcCode;
        }
        if (!isBlank(trimToNull(responseItem.getItemNo()))) {
            return trimToNull(matchingOrderItem.getItemCode());
        }
        if (sameText(responseItem.getDescription(), matchingOrderItem.getItemName())) {
            return trimToNull(matchingOrderItem.getItemCode());
        }
        if (sameText(npcCode, responseItem.getDescription()) || sameText(npcCode, matchingOrderItem.getItemName())) {
            return trimToNull(matchingOrderItem.getItemCode());
        }
        return npcCode;
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? trimToNull(second) : trimToNull(first);
    }

    private boolean sameText(String left, String right) {
        return !isBlank(left) && !isBlank(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean isEligibleForRmsDeliveryFetch(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null || purchaseOrder.getSupplier() == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(purchaseOrder.getSupplier().getApiEnabled())) {
            return false;
        }
        if (!"APPROVED".equalsIgnoreCase(purchaseOrder.getApprovalStatus())) {
            return false;
        }
        String dispatchStatus = normalized(purchaseOrder.getDispatchStatus());
        return !"RECEIVED".equalsIgnoreCase(dispatchStatus)
                && !"SERVED".equalsIgnoreCase(dispatchStatus)
                && !"CLOSED".equalsIgnoreCase(dispatchStatus);
    }

    private String resolveRmsOrderNumber(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                if (!isBlank(item.getOrderNumber())) {
                    return item.getOrderNumber().trim();
                }
            }
        }
        if (!isBlank(purchaseOrder.getDispatchReference())
                && !purchaseOrder.getDispatchReference().trim().startsWith("{")
                && !purchaseOrder.getDispatchReference().contains("rawResponse")) {
            return purchaseOrder.getDispatchReference().trim();
        }
        if (!isBlank(purchaseOrder.getFacilityId()) && purchaseOrder.getPurchaseOrderId() != null) {
            return "ord-" + purchaseOrder.getFacilityId().trim() + "-" + purchaseOrder.getPurchaseOrderId();
        }
        throw new IllegalStateException("No RMS order number is available for purchase order " + purchaseOrder.getPurchaseOrderId());
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalizedValue = value.replace(" ", "").replace("\n", "").replace("\r", "").trim();
        if (normalizedValue.isEmpty()) {
            return null;
        }
        return new BigDecimal(normalizedValue);
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void persistSupplierPrices(List<SupplierPriceRecord> records) {
        if (records == null) {
            return;
        }
        for (SupplierPriceRecord record : records) {
            String npcCode = normalized(record.getNpcCode());
            String program = normalized(record.getProgram());
            if (npcCode.isEmpty()) {
                continue;
            }
            SupplierPrice supplierPrice = pharmacyService.getLatestSupplierPrice(npcCode, program);
            if (supplierPrice == null) {
                supplierPrice = new SupplierPrice();
                supplierPrice.setCreator(Context.getAuthenticatedUser());
                supplierPrice.setDateCreated(new Date());
            }
            ProductMasterItem productMasterItem = pharmacyService.getProductMasterItemByNpcCodeAndProgram(npcCode, program);
            if (productMasterItem == null) {
                productMasterItem = pharmacyService.getProductMasterItemByNpcCode(npcCode);
            }
            Item item = null;
            if (productMasterItem != null) {
                Drug linkedDrug = productMasterItem.getDrug();
                Concept linkedConcept = linkedDrug == null
                        ? resolveConceptForItem(productMasterItem.getItemName(), productMasterItem.getMaterialType())
                        : linkedDrug.getConcept();
                item = ensureLocalItem(npcCode, productMasterItem.getItemName(), linkedConcept, linkedDrug, program);
                pharmacyService.mapProductMasterItemToItem(productMasterItem, item);
            } else {
                item = pharmacyService.getItemByCodeAndProgram(npcCode, program);
                if (item == null) {
                    item = pharmacyService.getItemByCode(npcCode);
                }
            }
            supplierPrice.setItem(item);
            supplierPrice.setSupplierProductCode(npcCode);
            supplierPrice.setProgram(program);
            if (record.getPrice() != null) {
                supplierPrice.setUnitPrice(java.math.BigDecimal.valueOf(record.getPrice()));
            }
            supplierPrice.setCurrency(normalized(record.getCurrency()));
            supplierPrice.setEffectiveDate(new Date());
            pharmacyService.saveSupplierPrice(supplierPrice);
            if (productMasterItem != null && record.getPrice() != null) {
                productMasterItem.setItemPrice(java.math.BigDecimal.valueOf(record.getPrice()));
                if (productMasterItem.getCreator() == null) {
                    productMasterItem.setCreator(Context.getAuthenticatedUser());
                }
                if (productMasterItem.getDateCreated() == null) {
                    productMasterItem.setDateCreated(new Date());
                }
                if (isDrugMaterialType(productMasterItem.getMaterialType()) && productMasterItem.getDrug() == null) {
                    productMasterItem.setDrug(findOrCreateDrug(productMasterItem.getItemName()));
                }
                if (!isDrugMaterialType(productMasterItem.getMaterialType())) {
                    productMasterItem.setDrug(null);
                }
                pharmacyService.saveProductMasterItem(productMasterItem);
            }
        }
    }

    private void persistProductMappings(List<SupplierProductMasterProgram> programs) {
        if (programs == null) {
            return;
        }
        Set<String> processedProductKeys = new HashSet<String>();
        Set<String> returnedProductKeys = new HashSet<String>();
        Map<String, ProductMasterItem> productMasterByCode = new HashMap<String, ProductMasterItem>();
        Map<String, SupplierProductMapping> mappingByCode = new HashMap<String, SupplierProductMapping>();
        for (SupplierProductMasterProgram program : programs) {
            if (program.getItems() == null) {
                continue;
            }
            for (org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierProductMasterItem itemDto : program.getItems()) {
                String npcCode = normalized(itemDto.getNpcCode());
                String programName = normalized(program.getProgram());
                String productKey = productMasterKey(npcCode, programName);
                if (npcCode.isEmpty() || processedProductKeys.contains(productKey)) {
                    continue;
                }
                processedProductKeys.add(productKey);
                returnedProductKeys.add(productKey);
                SupplierProductMapping mapping = mappingByCode.containsKey(npcCode)
                        ? mappingByCode.get(npcCode)
                        : pharmacyService.getSupplierProductMappingByCode(npcCode);
                if (mapping == null) {
                    mapping = new SupplierProductMapping();
                    mapping.setCreator(Context.getAuthenticatedUser());
                    mapping.setDateCreated(new Date());
                }
                mapping.setSupplierProductCode(npcCode);
                mapping.setSupplierProductName(normalized(itemDto.getMaterialDescription()));
                mapping.setActive(Boolean.TRUE);
                mappingByCode.put(npcCode, mapping);
                ProductMasterItem productMasterItem = productMasterByCode.containsKey(productKey)
                        ? productMasterByCode.get(productKey)
                        : pharmacyService.getProductMasterItemByNpcCodeAndProgram(npcCode, programName);
                if (productMasterItem == null) {
                    productMasterItem = new ProductMasterItem();
                    productMasterItem.setCreator(Context.getAuthenticatedUser());
                    productMasterItem.setDateCreated(new Date());
                }
                productMasterItem.setItemName(normalized(itemDto.getMaterialDescription()));
                productMasterItem.setNpcCode(npcCode);
                productMasterItem.setProgram(programName);
                productMasterItem.setMaterialType(normalized(itemDto.getMaterialType()));
                productMasterItem.setActive(Boolean.TRUE);
                productMasterItem.setSource(ProductMasterItem.SOURCE_API_SYNCED);
                pharmacyService.saveProductMasterItem(productMasterItem);
                productMasterByCode.put(productKey, productMasterItem);

                try {
                    linkProductMasterItemToLocalItem(productMasterItem, mapping, programName);
                }
                catch (Exception e) {
                    saveProductMasterMappingFailure(productMasterItem, mapping, e);
                }
            }
        }
        deactivateMissingProductMasterItems(returnedProductKeys);
    }

    private void deactivateMissingProductMasterItems(Set<String> returnedProductKeys) {
        if (returnedProductKeys == null) {
            return;
        }
        for (ProductMasterItem productMasterItem : pharmacyService.getAllProductMasterItems()) {
            if (productMasterItem == null || productMasterItem.getProductMasterItemId() == null) {
                continue;
            }
            if (!ProductMasterItem.SOURCE_API_SYNCED.equalsIgnoreCase(normalized(productMasterItem.getSource()))) {
                continue;
            }
            String npcCode = normalized(productMasterItem.getNpcCode());
            if (npcCode.isEmpty()) {
                continue;
            }
            String productKey = productMasterKey(npcCode, productMasterItem.getProgram());
            if (returnedProductKeys.contains(productKey)) {
                continue;
            }
            productMasterItem.setActive(Boolean.FALSE);
            pharmacyService.saveProductMasterItem(productMasterItem);
        }
    }

    /**
     * An already linked item keeps its name, drug and concept: a supplier rename only records a proposal, which an
     * admin approves on the item renames page so the item, drug and MOH Billing service are renamed together.
     */
    private void linkProductMasterItemToLocalItem(ProductMasterItem productMasterItem, SupplierProductMapping mapping,
                                                  String programName) {
        boolean drugProduct = isDrugMaterialType(productMasterItem.getMaterialType());
        Item linkedItem = pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
        if (linkedItem == null && mapping != null) {
            linkedItem = mapping.getItem();
        }
        Item ensuredLocalItem;
        if (linkedItem != null && (linkedItem.getDrug() != null || !drugProduct)) {
            productMasterItem.setDrug(drugProduct ? linkedItem.getDrug() : null);
            pharmacyService.saveProductMasterItem(productMasterItem);
            ensuredLocalItem = linkedItem;
        } else {
            Concept concept = resolveConceptForItem(productMasterItem.getItemName(), productMasterItem.getMaterialType());
            Drug drug = drugProduct ? findOrCreateDrug(productMasterItem.getItemName()) : null;
            productMasterItem.setDrug(drug);
            pharmacyService.saveProductMasterItem(productMasterItem);
            ensuredLocalItem = ensureLocalItem(productMasterItem.getNpcCode(), productMasterItem.getItemName(),
                    concept, drug, programName);
        }
        pharmacyService.mapProductMasterItemToItem(productMasterItem, ensuredLocalItem);

        mapping.setItem(ensuredLocalItem);
        pharmacyService.saveSupplierProductMapping(mapping);
        if (itemRenameService != null) {
            itemRenameService.proposeSupplierRename(ensuredLocalItem, productMasterItem);
        }
    }

    private void saveProductMasterMappingFailure(ProductMasterItem productMasterItem, SupplierProductMapping mapping,
                                                 Exception exception) {
        IntegrationLog log = createLog(IntegrationType.PRODUCT_MASTER_SYNC, IntegrationStatus.FAILED,
                "Product master item saved, but local item mapping failed for "
                        + safe(productMasterItem == null ? null : productMasterItem.getNpcCode()));
        log.setEndpoint("/api/integration/product-master");
        log.setRequestPayload(truncate(String.valueOf(productMasterItem)));
        log.setResponsePayload(truncate(rootCause(exception).getMessage()));
        integrationLogService.saveLog(log);
        if (mapping != null) {
            pharmacyService.saveSupplierProductMapping(mapping);
        }
    }

    private String productMasterKey(String npcCode, String program) {
        return normalized(npcCode).toUpperCase(Locale.ENGLISH) + "|" + normalized(program).toUpperCase(Locale.ENGLISH);
    }

    private IntegrationLog saveFailedLog(IntegrationType type, String baseUrl, String path, long start, Exception e) {
        IntegrationLog log = errorLog(type, baseUrl, path, start, e);
        Throwable rootCause = rootCause(e);
        if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().equals(e.getMessage())) {
            log.setResponsePayload(truncate(rootCause.getMessage()));
        }
        return integrationLogService.saveLog(log);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private Drug findOrCreateDrug(String itemName) {
        String fullDrugName = normalized(itemName);
        if (fullDrugName.isEmpty()) {
            return null;
        }
        Drug existingDrug = Context.getConceptService().getDrug(fullDrugName);
        if (existingDrug != null) {
            return existingDrug;
        }
        Concept concept = findOrCreateConcept(drugBaseConceptName(fullDrugName));
        Drug drug = new Drug();
        drug.setConcept(concept);
        drug.setName(fullDrugName);
        drug.setCreator(Context.getAuthenticatedUser());
        drug.setDateCreated(new Date());
        return Context.getConceptService().saveDrug(drug);
    }

    private Concept resolveConceptForItem(String itemName, String materialType) {
        if (isDrugMaterialType(materialType)) {
            return findOrCreateConcept(drugBaseConceptName(itemName));
        }
        return findOrCreateConcept(nonDrugConceptName(itemName));
    }

    private Item ensureLocalItem(String itemCode, String itemName, Concept concept, Drug drug, String program) {
        Item existingItem = pharmacyService.getItemByExactName(normalized(itemName));
        if (existingItem == null) {
            existingItem = pharmacyService.getItemByCode(normalized(itemCode));
        }
        if (existingItem == null) {
            existingItem = findExistingItemForRmsProduct(itemName, concept, drug);
        }
        if (existingItem != null) {
            // Never re-point an item's drug or concept: MOH Billing finds the billing service by the drug's name.
            if (existingItem.getDrug() == null) {
                existingItem.setDrug(drug);
            }
            if (concept != null && existingItem.getConcept() == null) {
                existingItem.setConcept(concept);
            }
            if (existingItem.getName() == null || existingItem.getName().trim().isEmpty()) {
                existingItem.setName(normalized(itemName));
            }
            existingItem.setSupplierManaged(Boolean.TRUE);
            pharmacyService.saveItem(existingItem);
            return existingItem;
        }
        Item item = new Item();
        item.setCode(normalized(itemCode));
        item.setName(normalized(itemName));
        item.setProgram("");
        item.setDrug(drug);
        item.setConcept(concept);
        item.setDispensable(Boolean.TRUE);
        item.setStockable(Boolean.TRUE);
        item.setSupplierManaged(Boolean.TRUE);
        item.setRequiresBatch(Boolean.FALSE);
        item.setRequiresExpiry(Boolean.FALSE);
        item.setCreator(Context.getAuthenticatedUser());
        item.setDateCreated(new Date());
        return pharmacyService.saveItem(item);
    }

    private Item findExistingItemForRmsProduct(String itemName, Concept concept, Drug drug) {
        String normalizedItemName = normalized(itemName);
        Integer drugId = drug == null ? null : drug.getDrugId();
        Integer conceptId = concept == null ? null : concept.getConceptId();
        if (drugId == null && conceptId == null) {
            return null;
        }
        for (Item item : pharmacyService.getAllItems()) {
            if (item == null) {
                continue;
            }
            Integer existingDrugId = item.getDrug() == null ? null : item.getDrug().getDrugId();
            if (drugId != null && drugId.equals(existingDrugId)) {
                return item;
            }
            Integer existingConceptId = item.getConcept() == null ? null : item.getConcept().getConceptId();
            if (drugId == null && conceptId != null && conceptId.equals(existingConceptId)
                    && normalizedItemName.equals(normalized(item.getName()))) {
                return item;
            }
        }
        return null;
    }

    private Concept findOrCreateConcept(String conceptName) {
        String normalizedConceptName = normalized(conceptName);
        Concept existingConcept = Context.getConceptService().getConceptByName(normalizedConceptName);
        if (existingConcept != null) {
            return existingConcept;
        }
        List<Concept> matchingConcepts = Context.getConceptService().getConceptsByName(normalizedConceptName);
        if (matchingConcepts != null) {
            for (Concept concept : matchingConcepts) {
                if (concept == null || concept.getNames() == null) {
                    continue;
                }
                for (ConceptName name : concept.getNames()) {
                    if (name != null && normalizedConceptName.equalsIgnoreCase(normalized(name.getName()))) {
                        return concept;
                    }
                }
            }
        }
        Concept concept = new Concept();
        ConceptName preferredName = new ConceptName(normalizedConceptName, Locale.ENGLISH);
        preferredName.setLocalePreferred(true);
        concept.addName(preferredName);
        concept.setDatatype(resolveConceptDatatype());
        concept.setConceptClass(resolveConceptClass());
        concept.setSet(false);
        concept.setCreator(Context.getAuthenticatedUser());
        concept.setDateCreated(new Date());
        return Context.getConceptService().saveConcept(concept);
    }

    private ConceptDatatype resolveConceptDatatype() {
        ConceptDatatype datatype = Context.getConceptService().getConceptDatatypeByName("N/A");
        if (datatype == null) {
            datatype = Context.getConceptService().getConceptDatatypeByName("Text");
        }
        if (datatype == null) {
            throw new IllegalStateException("Unable to resolve a concept datatype for supplier product sync");
        }
        return datatype;
    }

    private ConceptClass resolveConceptClass() {
        ConceptClass conceptClass = Context.getConceptService().getConceptClassByName("Drug");
        if (conceptClass == null) {
            conceptClass = Context.getConceptService().getConceptClassByName("Misc");
        }
        if (conceptClass == null) {
            throw new IllegalStateException("Unable to resolve a concept class for supplier product sync");
        }
        return conceptClass;
    }

    private String drugBaseConceptName(String itemName) {
        String baseName = normalized(itemName).replaceAll("\\s+", " ");
        String stripped = baseName.replaceFirst("\\s+\\d.*$", "").trim();
        return stripped.isEmpty() ? baseName : stripped;
    }

    private String nonDrugConceptName(String itemName) {
        return normalized(itemName).replaceAll("\\s+", " ");
    }

    private boolean isDrugMaterialType(String materialType) {
        return "ZMED".equalsIgnoreCase(normalized(materialType));
    }

    private void prepareSupplierOrderForPersistence(SupplierOrder supplierOrder) {
        if (supplierOrder == null) {
            return;
        }
        if (supplierOrder.getStatus() == null || supplierOrder.getStatus().trim().isEmpty()) {
            supplierOrder.setStatus("PENDING_SUBMISSION");
        }
        if (supplierOrder.getRequestedBy() == null) {
            supplierOrder.setRequestedBy(Context.getAuthenticatedUser());
        }
        if (supplierOrder.getCreator() == null) {
            supplierOrder.setCreator(Context.getAuthenticatedUser());
        }
        if (supplierOrder.getDateCreated() == null) {
            supplierOrder.setDateCreated(new Date());
        }
        if (supplierOrder.getLines() != null) {
            for (SupplierOrderLine line : supplierOrder.getLines()) {
                line.setSupplierOrder(supplierOrder);
                line.setCreator(Context.getAuthenticatedUser());
                line.setDateCreated(new Date());
                if (line.getSupplierProductCode() == null && line.getItem() != null) {
                    line.setSupplierProductCode(line.getItem().getCode());
                }
                if (line.getSupplierProductName() == null && line.getItem() != null) {
                    line.setSupplierProductName(line.getItem().getName());
                }
            }
        }
    }

    private SupplierOrder buildSupplierOrder(PurchaseOrder purchaseOrder, Supplier supplier) {
        SupplierOrder supplierOrder = new SupplierOrder();
        supplierOrder.setOrderNumber(resolveOrderNumber(purchaseOrder));
        supplierOrder.setSupplier(supplier);
        supplierOrder.setFacilityLocation(Context.getLocationService().getDefaultLocation());
        supplierOrder.setRequestedBy(purchaseOrder.getRequestedBy());
        supplierOrder.setStatus("PENDING_SUBMISSION");
        supplierOrder.setCreator(Context.getAuthenticatedUser());
        supplierOrder.setDateCreated(new Date());
        java.util.ArrayList<SupplierOrderLine> lines = new java.util.ArrayList<SupplierOrderLine>();
        if (purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                ProductMasterItem productMasterItem = item.getProductMasterItem();
                SupplierOrderLine line = new SupplierOrderLine();
                line.setSupplierOrder(supplierOrder);
                line.setSupplierProductCode(productMasterItem == null ? item.getItemCode() : productMasterItem.getNpcCode());
                line.setSupplierProductName(productMasterItem == null ? item.getItemName() : productMasterItem.getItemName());
                line.setProgramName(productMasterItem == null ? item.getProgram() : productMasterItem.getProgram());
                line.setOrderedQuantity(item.getQuantity());
                line.setUnitPrice(item.getUnitPrice());
                line.setCreator(Context.getAuthenticatedUser());
                line.setDateCreated(new Date());
                Item localItem = productMasterItem == null ? null : pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
                if (localItem == null) {
                    localItem = pharmacyService.getItemByCodeAndProgram(item.getItemCode(), item.getProgram());
                }
                if (localItem == null && isBlank(item.getProgram())) {
                    localItem = pharmacyService.getItemByCode(item.getItemCode());
                }
                if (localItem != null) {
                    line.setItem(localItem);
                }
                lines.add(line);
            }
        }
        supplierOrder.setLines(lines);
        return supplierOrder;
    }

    private void sendPurchaseOrderEmail(PurchaseOrder purchaseOrder, SupplierOrder supplierOrder, Supplier supplier) throws Exception {
        final String smtpUsername = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_USERNAME, "");
        final String smtpPassword = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_PASSWORD, "");
        String smtpHost = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_HOST, "");
        Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.host", smtpHost);
        properties.setProperty("mail.smtp.port", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_PORT, "587"));
        properties.setProperty("mail.smtp.auth", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_AUTH, "true"));
        properties.setProperty("mail.smtp.starttls.enable", administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_STARTTLS, "true"));
        properties.setProperty("mail.smtp.ssl.trust", smtpHost);
        properties.setProperty("mail.smtp.connectiontimeout", "15000");
        properties.setProperty("mail.smtp.timeout", "30000");
        properties.setProperty("mail.smtp.writetimeout", "30000");
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (smtpUsername == null || smtpUsername.trim().isEmpty()) {
                    return null;
                }
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
        MimeMessage message = new MimeMessage(session);
        String sender = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMTP_FROM, "");
        if (sender != null && !sender.trim().isEmpty()) {
            InternetAddress senderAddress = new InternetAddress(sender);
            message.setFrom(senderAddress);
            message.setSender(senderAddress);
        }
        message.setRecipients(javax.mail.Message.RecipientType.TO,
                InternetAddress.parse(supplier.getEmailAddress(), false));
        String hospitalName = resolveHospitalName();
        message.setSubject("Purchase order " + resolveOrderNumber(purchaseOrder)
                + (isBlank(hospitalName) ? "" : " - " + hospitalName));

        Multipart multipart = new MimeMultipart();
        BodyPart body = new MimeBodyPart();
        body.setText(buildEmailBody(supplierOrder, supplier, hospitalName));
        multipart.addBodyPart(body);

        byte[] pdfBytes = PurchaseOrderPdfRenderer.render(purchaseOrder, hospitalName);
        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(pdfBytes, "application/pdf")));
        attachment.setFileName(resolveOrderNumber(purchaseOrder) + ".pdf");
        multipart.addBodyPart(attachment);

        byte[] receiptTemplateBytes = DeliveryReceiptTemplateRenderer.renderPurchaseOrderTemplate(purchaseOrder);
        MimeBodyPart receiptTemplateAttachment = new MimeBodyPart();
        receiptTemplateAttachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                receiptTemplateBytes,
                "text/csv; charset=UTF-8")));
        receiptTemplateAttachment.setFileName(resolveOrderNumber(purchaseOrder) + "-delivery-receipt.csv");
        multipart.addBodyPart(receiptTemplateAttachment);

        message.setContent(multipart);
        Transport.send(message);
    }

    private String resolveOrderNumber(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return "";
        }
        String facilityId = purchaseOrder.getFacilityId() == null || purchaseOrder.getFacilityId().trim().isEmpty()
                ? "1660" : purchaseOrder.getFacilityId().trim();
        return "ord-" + facilityId + "-" + purchaseOrder.getPurchaseOrderId();
    }

    private void sendOrderEmail(SupplierOrder supplierOrder, Supplier supplier) throws MessageException {
        Message message = new Message();
        message.setRecipients(supplier.getEmailAddress());
        String hospitalName = resolveHospitalName();
        message.setSubject("Pharmacy replenishment order " + supplierOrder.getOrderNumber()
                + (isBlank(hospitalName) ? "" : " - " + hospitalName));
        message.setContent(buildEmailBody(supplierOrder, supplier, hospitalName));
        message.setContentType("text/plain");
        Context.getMessageService().sendMessage(message);
    }

    private String buildEmailBody(SupplierOrder supplierOrder, Supplier supplier) {
        return buildEmailBody(supplierOrder, supplier, resolveHospitalName());
    }

    private String buildEmailBody(SupplierOrder supplierOrder, Supplier supplier, String hospitalName) {
        StringBuilder builder = new StringBuilder();
        if (!isBlank(hospitalName)) {
            builder.append("Hospital: ").append(hospitalName).append("\n");
        }
        builder.append("Supplier: ").append(supplier.getName()).append("\n");
        builder.append("Order number: ").append(supplierOrder.getOrderNumber()).append("\n");
        builder.append("Requested by: ")
                .append(UserNameUtils.plainName(supplierOrder.getRequestedBy()))
                .append("\n\nItems:\n");
        if (supplierOrder.getLines() != null) {
            for (SupplierOrderLine line : supplierOrder.getLines()) {
                builder.append("- ")
                        .append(resolveProductName(line))
                        .append(" | NPC: ").append(safe(line.getSupplierProductCode()))
                        .append(" | Program: ").append(safe(line.getProgramName()))
                        .append(" | Qty: ").append(line.getOrderedQuantity() == null ? "0" : line.getOrderedQuantity().stripTrailingZeros().toPlainString())
                        .append(" | Unit price: ").append(line.getUnitPrice() == null ? "0" : line.getUnitPrice().stripTrailingZeros().toPlainString())
                        .append("\n");
            }
        }
        return builder.toString();
    }

    private String resolveHospitalName() {
        return administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, "");
    }

    private boolean isRmsPurchaseOrder(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null || purchaseOrder.getSupplier() == null) {
            return false;
        }
        return Boolean.TRUE.equals(purchaseOrder.getSupplier().getApiEnabled());
    }

    private String validatePurchaseOrderProductMasterItems(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return "Purchase order is required";
        }
        if (purchaseOrder.getItems() == null || purchaseOrder.getItems().isEmpty()) {
            return "Purchase order must include at least one Product Master item";
        }
        boolean apiSupplier = isRmsPurchaseOrder(purchaseOrder);
        List<String> invalidItems = new ArrayList<String>();
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            ProductMasterItem productMasterItem = item == null ? null : item.getProductMasterItem();
            if (productMasterItem == null) {
                invalidItems.add(safe(item == null ? null : item.getItemName()));
                continue;
            }
            if (Boolean.FALSE.equals(productMasterItem.getActive())) {
                invalidItems.add(productMasterItem.getItemName() + " is inactive");
                continue;
            }
            if (apiSupplier && !ProductMasterItem.SOURCE_API_SYNCED.equalsIgnoreCase(normalized(productMasterItem.getSource()))) {
                invalidItems.add(productMasterItem.getItemName() + " is not from the API product master");
            }
        }
        if (!invalidItems.isEmpty()) {
            return "Purchase order contains items that are not orderable for the selected supplier: " + invalidItems;
        }
        return null;
    }

    private String resolveProductName(SupplierOrderLine line) {
        if (line == null) {
            return "";
        }
        if (line.getSupplierProductName() != null && !line.getSupplierProductName().trim().isEmpty()) {
            return line.getSupplierProductName();
        }
        return line.getItem() == null ? "" : safe(line.getItem().getName());
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
