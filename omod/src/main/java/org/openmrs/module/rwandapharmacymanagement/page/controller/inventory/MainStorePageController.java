package org.openmrs.module.rwandapharmacymanagement.page.controller.inventory;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrder;
import org.openmrs.module.rwandapharmacymanagement.model.PurchaseOrderItem;
import org.openmrs.module.rwandapharmacymanagement.model.ProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.model.Supplier;
import org.openmrs.module.rwandapharmacymanagement.model.SupplierPrice;
import org.openmrs.module.rwandapharmacymanagement.page.controller.purchaseorder.ManagementPageController;
import org.openmrs.module.rwandapharmacymanagement.page.controller.support.PaginationSupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.DeliveryReceiptSupport;
import org.openmrs.module.rwandapharmacymanagement.web.support.FlashMessageSupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class MainStorePageController {

    private static final Log log = LogFactory.getLog(MainStorePageController.class);
    private static final String CART_SESSION_KEY = "rwandaPharmacySupplierOrderCart";
    private static final String FLASH_ERROR_REFERENCE = "rpm.mainStore.errorReference";
    private static final String APPROVAL_STATUS_DRAFT = "DRAFT";
    private static final String APPROVAL_STATUS_PENDING = "PENDING";
    private static final String DISPATCH_STATUS_DRAFT = "DRAFT";
    private static final String DISPATCH_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    public void get(PageModel model,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @RequestParam(value = "orderItemId", required = false) Integer orderItemId,
                    @RequestParam(value = "cart", required = false) Boolean showCart,
                    @RequestParam(value = "q", required = false) String query,
                    @RequestParam(value = "page", required = false) Integer page,
                    @RequestParam(value = "pageSize", required = false) Integer pageSize,
                    @RequestParam(value = "supplierId", required = false) Integer supplierId,
                    HttpSession session) {
        org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation mainStore = pharmacyService.getMainStore();
        List<InventoryBalance> allStockBalances = mainStore == null
                ? Collections.<InventoryBalance>emptyList()
                : inventoryService.getInventoryForPharmacy(mainStore.getPharmacyLocationId());
        allStockBalances = filterStockBalances(allStockBalances, query);
        int currentPage = PaginationSupport.sanitizePage(page);
        int currentPageSize = PaginationSupport.sanitizePageSize(pageSize);
        currentPage = PaginationSupport.clampPage(currentPage, currentPageSize, allStockBalances.size());
        List<InventoryBalance> stockBalances = PaginationSupport.slice(allStockBalances, currentPage, currentPageSize);
        LinkedHashMap<String, CartLine> cart = getCart(session, pharmacyService);
        Item selectedItem = orderItemId == null ? null : pharmacyService.getItem(orderItemId);
        model.addAttribute("stockBalances", stockBalances);
        PaginationSupport.addAttributes(model, "stock", currentPage, currentPageSize, allStockBalances.size());
        model.addAttribute("mainStore", mainStore);
        model.addAttribute("pricingService", pricingService);
        model.addAttribute("cartCount", cart.size());
        model.addAttribute("cartTotalQuantity", cartTotalQuantity(cart));
        model.addAttribute("cartItemKeys", cart.keySet());
        model.addAttribute("cartItemKeyByItemId", buildCartItemKeyByItemId(cart));
        model.addAttribute("showOrderModal", selectedItem != null);
        model.addAttribute("showCartModal", Boolean.TRUE.equals(showCart));
        model.addAttribute("selectedItem", selectedItem);
        model.addAttribute("selectedCurrentStock", selectedItem == null ? BigDecimal.ZERO : currentStock(allStockBalances, selectedItem.getItemId()));
        model.addAttribute("selectedSuggestedQuantity", selectedItem == null ? null : suggestedQuantity(selectedItem));
        model.addAttribute("selectedUnitPrice", selectedItem == null ? null : latestUnitPrice(pharmacyService, selectedItem.getCode(), selectedItem.getProgram()));
        model.addAttribute("cartRows", buildCartRows(cart));
        model.addAttribute("orderSubmitted", Boolean.FALSE);
        model.addAttribute("mainStoreName", mainStore == null ? "Main Pharmacy" : mainStore.getName());
        model.addAttribute("q", trimToEmpty(query));
        Supplier selectedSupplier = supplierId == null ? null : pharmacyService.getSupplier(supplierId);
        List<ProductMasterItem> productMasterItems = filterProductMasterItemsForSupplier(
                pharmacyService.getAllProductMasterItems(), selectedSupplier);
        model.addAttribute("productMasterItems", productMasterItems);
        model.addAttribute("productMasterPrograms", productMasterPrograms(productMasterItems));
        model.addAttribute("suppliers", pharmacyService.getAllSuppliers());
        model.addAttribute("selectedSupplier", selectedSupplier);
        model.addAttribute("selectedSupplierId", supplierId);
        model.addAttribute("errorReference", FlashMessageSupport.consumeString(session, FLASH_ERROR_REFERENCE));
    }

    private List<ProductMasterItem> filterProductMasterItemsForSupplier(List<ProductMasterItem> productMasterItems,
                                                                        Supplier supplier) {
        List<ProductMasterItem> filtered = new ArrayList<ProductMasterItem>();
        if (productMasterItems == null || supplier == null) {
            return filtered;
        }
        for (ProductMasterItem item : productMasterItems) {
            if (isProductMasterItemAllowedForSupplier(item, supplier)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean isProductMasterItemAllowedForSupplier(ProductMasterItem item, Supplier supplier) {
        if (item == null || Boolean.FALSE.equals(item.getActive())) {
            return false;
        }
        if (supplier != null && Boolean.TRUE.equals(supplier.getApiEnabled())) {
            return ProductMasterItem.SOURCE_API_SYNCED.equalsIgnoreCase(trimToEmpty(item.getSource()));
        }
        return true;
    }

    private List<String> productMasterPrograms(List<ProductMasterItem> productMasterItems) {
        LinkedHashSet<String> programs = new LinkedHashSet<String>();
        if (productMasterItems != null) {
            for (ProductMasterItem item : productMasterItems) {
                String program = trimToEmpty(item == null ? null : item.getProgram());
                if (!program.isEmpty()) {
                    programs.add(program);
                }
            }
        }
        List<String> sortedPrograms = new ArrayList<String>(programs);
        Collections.sort(sortedPrograms);
        return sortedPrograms;
    }

    private List<InventoryBalance> filterStockBalances(List<InventoryBalance> balances, String query) {
        String normalizedQuery = trimToEmpty(query).toLowerCase(Locale.ENGLISH);
        List<InventoryBalance> filtered = new ArrayList<InventoryBalance>();
        for (InventoryBalance balance : balances) {
            if (hasPositiveQuantity(balance) && (normalizedQuery.isEmpty() || matchesStockSearch(balance, normalizedQuery))) {
                filtered.add(balance);
            }
        }
        return filtered;
    }

    private boolean hasPositiveQuantity(InventoryBalance balance) {
        return balance != null
                && balance.getQuantityOnHand() != null
                && balance.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean matchesStockSearch(InventoryBalance balance, String normalizedQuery) {
        if (balance == null) {
            return false;
        }
        Item item = balance.getItem();
        return contains(item == null ? null : item.getName(), normalizedQuery)
                || contains(item == null ? null : item.getProgram(), normalizedQuery)
                || contains(item == null || item.getItemCategory() == null ? null : item.getItemCategory().getName(), normalizedQuery)
                || contains(balance.getBatchNumber(), normalizedQuery);
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(normalizedQuery);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public String post(@SpringBean("pharmacyService") PharmacyService pharmacyService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "itemId", required = false) Integer itemId,
                       @RequestParam(value = "productMasterItemId", required = false) Integer productMasterItemId,
                       @RequestParam(value = "productMasterItemIds", required = false) Integer[] productMasterItemIds,
                       @RequestParam(value = "productMasterQuantities", required = false) BigDecimal[] productMasterQuantities,
                       @RequestParam(value = "cartKey", required = false) String cartKey,
                       @RequestParam(value = "quantity", required = false) BigDecimal quantity,
                       @RequestParam(value = "unitPrice", required = false) BigDecimal unitPrice,
                       @RequestParam(value = "supplierId", required = false) Integer supplierId,
                       HttpSession session,
                       UiUtils ui) {
        try {
            if ("addToCart".equals(action)) {
                throw new IllegalArgumentException("Select a supplier and add items from Product Master");
            }
            if ("addProductMasterToCart".equals(action)) {
                addProductMasterItemToCart(session, pharmacyService, supplierId, productMasterItemId, quantity);
                persistDraftCart(pharmacyService, getCart(session, pharmacyService));
                return redirectToCart(ui, supplierId);
            }
            if ("addProductMasterRowsToCart".equals(action)) {
                addProductMasterItemsToCart(session, pharmacyService, supplierId, productMasterItemIds, productMasterQuantities);
                persistDraftCart(pharmacyService, getCart(session, pharmacyService));
                return redirectToCart(ui, supplierId);
            }
            if ("updateCart".equals(action)) {
                updateCart(session, pharmacyService, cartKey, quantity, unitPrice);
                persistDraftCart(pharmacyService, getCart(session, pharmacyService));
                return redirectToCart(ui, supplierId);
            }
            if ("removeFromCart".equals(action)) {
                removeFromCart(session, pharmacyService, cartKey);
                persistDraftCart(pharmacyService, getCart(session, pharmacyService));
                return redirectToCart(ui, supplierId);
            }
            if ("clearCart".equals(action)) {
                session.removeAttribute(CART_SESSION_KEY);
                clearDraftPurchaseOrders(pharmacyService);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/mainStore");
            }
            if ("submitCart".equals(action)) {
                initiatePurchaseOrders(session, pharmacyService, supplierId);
                FlashMessageSupport.set(session, ManagementPageController.FLASH_INITIATED, Boolean.TRUE);
                return "redirect:" + ui.pageLink("rwandapharmacymanagement", "purchaseorder/management");
            }
        }
        catch (RuntimeException e) {
            return redirectToCartError(ui, session, e);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/mainStore");
    }

    private String redirectToCart(UiUtils ui, Integer supplierId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("cart", "true");
        if (supplierId != null) {
            params.put("supplierId", supplierId);
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", params);
    }

    private String redirectToCartError(UiUtils ui, HttpSession session, RuntimeException e) {
        String reference = "RPM-PO-CART-" + System.currentTimeMillis();
        log.error("Purchase order cart action failed. Reference: " + reference, e);
        FlashMessageSupport.set(session, FLASH_ERROR_REFERENCE, reference);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("cart", "true");
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", params);
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, CartLine> getCart(HttpSession session, PharmacyService pharmacyService) {
        Object stored = session.getAttribute(CART_SESSION_KEY);
        if (stored instanceof LinkedHashMap) {
            return (LinkedHashMap<String, CartLine>) stored;
        }
        LinkedHashMap<String, CartLine> cart = draftCart(pharmacyService);
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    private void addLocalItemToCart(HttpSession session, PharmacyService pharmacyService,
                                    Integer itemId, BigDecimal quantity) {
        if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Item item = pharmacyService.getItem(itemId);
        if (item == null) {
            return;
        }
        String key = cartKeyForItem(itemId);
        LinkedHashMap<String, CartLine> cart = getCart(session, pharmacyService);
        CartLine line = cart.get(key);
        if (line == null) {
            line = new CartLine();
            line.setCartKey(key);
            line.setItemId(itemId);
            line.setItemName(item.getName());
            line.setItemCode(item.getCode());
            line.setProgram(item.getProgram());
            line.setUnitPrice(latestUnitPrice(pharmacyService, item.getCode(), line.getProgram()));
        }
        line.setQuantity(quantity);
        cart.put(key, line);
    }

    private void addProductMasterItemToCart(HttpSession session, PharmacyService pharmacyService,
                                            Integer supplierId, Integer productMasterItemId, BigDecimal quantity) {
        if (productMasterItemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Supplier supplier = supplierId == null ? null : pharmacyService.getSupplier(supplierId);
        if (supplier == null) {
            throw new IllegalArgumentException("Select a supplier before adding Product Master items");
        }
        ProductMasterItem productMasterItem = pharmacyService.getProductMasterItem(productMasterItemId);
        if (!isProductMasterItemAllowedForSupplier(productMasterItem, supplier)) {
            return;
        }
        Item mappedItem = pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
        if (mappedItem == null) {
            mappedItem = DeliveryReceiptSupport.resolveOrCreateMappedItem(pharmacyService, productMasterItem);
        }
        if (mappedItem == null) {
            throw new IllegalArgumentException("Unable to resolve a pharmacy item for product master item: "
                    + productMasterItem.getItemName() + " (" + productMasterItem.getNpcCode() + ", "
                    + normalizeProgram(productMasterItem.getProgram()) + ")");
        }
        String key = cartKeyForProductMaster(productMasterItemId);
        LinkedHashMap<String, CartLine> cart = getCart(session, pharmacyService);
        CartLine line = cart.get(key);
        if (line == null) {
            line = new CartLine();
            line.setCartKey(key);
            line.setItemId(mappedItem.getItemId());
            line.setProductMasterItemId(productMasterItemId);
            line.setItemName(productMasterItem.getItemName());
            line.setItemCode(productMasterItem.getNpcCode());
            line.setProgram(productMasterItem.getProgram());
            line.setUnitPrice(productMasterItem.getItemPrice());
        }
        line.setQuantity(quantity);
        cart.put(key, line);
    }

    private void addProductMasterItemsToCart(HttpSession session, PharmacyService pharmacyService,
                                             Integer supplierId, Integer[] productMasterItemIds, BigDecimal[] productMasterQuantities) {
        if (productMasterItemIds == null || productMasterQuantities == null) {
            return;
        }
        for (int i = 0; i < productMasterItemIds.length; i++) {
            Integer currentItemId = productMasterItemIds[i];
            BigDecimal currentQuantity = productMasterQuantities.length > i ? productMasterQuantities[i] : null;
            addProductMasterItemToCart(session, pharmacyService, supplierId, currentItemId, currentQuantity);
        }
    }

    private void updateCart(HttpSession session, PharmacyService pharmacyService, String cartKey, BigDecimal quantity, BigDecimal unitPrice) {
        LinkedHashMap<String, CartLine> cart = getCart(session, pharmacyService);
        if (cartKey == null || !cart.containsKey(cartKey)) {
            return;
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            cart.remove(cartKey);
            return;
        }
        CartLine line = cart.get(cartKey);
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : unitPrice);
    }

    private void removeFromCart(HttpSession session, PharmacyService pharmacyService, String cartKey) {
        if (cartKey == null) {
            return;
        }
        getCart(session, pharmacyService).remove(cartKey);
    }

    private void initiatePurchaseOrders(HttpSession session, PharmacyService pharmacyService, Integer supplierId) {
        LinkedHashMap<String, CartLine> cart = getCart(session, pharmacyService);
        if (cart.isEmpty() || supplierId == null) {
            return;
        }
        Supplier supplier = pharmacyService.getSupplier(supplierId);
        if (supplier == null) {
            return;
        }
        validateCartForSupplier(pharmacyService, cart, supplier);
        persistDraftCart(pharmacyService, cart);
        List<PurchaseOrder> draftOrders = pharmacyService.getDraftPurchaseOrdersForUser(Context.getAuthenticatedUser());
        for (PurchaseOrder purchaseOrder : draftOrders) {
            if (purchaseOrder == null || purchaseOrder.getItems() == null || purchaseOrder.getItems().isEmpty()) {
                continue;
            }
            purchaseOrder.setSupplier(supplier);
            purchaseOrder.setFacilityId(facilityId());
            purchaseOrder.setRequestedBy(Context.getAuthenticatedUser());
            purchaseOrder.setRequestedDate(new Date());
            purchaseOrder.setApprovalStatus(APPROVAL_STATUS_PENDING);
            purchaseOrder.setDispatchStatus(DISPATCH_STATUS_PENDING_APPROVAL);
            String orderNumber = "ord-" + purchaseOrder.getFacilityId() + "-" + purchaseOrder.getPurchaseOrderId();
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                item.setOrderNumber(orderNumber);
            }
            pharmacyService.savePurchaseOrder(purchaseOrder);
        }
        session.removeAttribute(CART_SESSION_KEY);
    }

    private void validateCartForSupplier(PharmacyService pharmacyService, LinkedHashMap<String, CartLine> cart,
                                         Supplier supplier) {
        if (cart == null || cart.isEmpty()) {
            return;
        }
        for (CartLine line : cart.values()) {
            ProductMasterItem productMasterItem = line.getProductMasterItemId() == null
                    ? null : pharmacyService.getProductMasterItem(line.getProductMasterItemId());
            if (!isProductMasterItemAllowedForSupplier(productMasterItem, supplier)) {
                throw new IllegalArgumentException("Cart contains an item that is not orderable for the selected supplier");
            }
        }
    }

    private LinkedHashMap<String, CartLine> draftCart(PharmacyService pharmacyService) {
        LinkedHashMap<String, CartLine> cart = new LinkedHashMap<String, CartLine>();
        List<PurchaseOrder> draftOrders = pharmacyService.getDraftPurchaseOrdersForUser(Context.getAuthenticatedUser());
        if (draftOrders == null) {
            return cart;
        }
        for (PurchaseOrder draftOrder : draftOrders) {
            if (draftOrder == null || draftOrder.getItems() == null) {
                continue;
            }
            for (PurchaseOrderItem item : draftOrder.getItems()) {
                CartLine line = cartLineFromPurchaseOrderItem(pharmacyService, item);
                if (line != null) {
                    cart.put(line.getCartKey(), line);
                }
            }
        }
        return cart;
    }

    private CartLine cartLineFromPurchaseOrderItem(PharmacyService pharmacyService, PurchaseOrderItem item) {
        if (item == null || item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        CartLine line = new CartLine();
        ProductMasterItem productMasterItem = item.getProductMasterItem();
        if (productMasterItem != null && productMasterItem.getProductMasterItemId() != null) {
            if (Boolean.FALSE.equals(productMasterItem.getActive())) {
                return null;
            }
            line.setCartKey(cartKeyForProductMaster(productMasterItem.getProductMasterItemId()));
            line.setProductMasterItemId(productMasterItem.getProductMasterItemId());
            Item mappedItem = pharmacyService.getMappedItemForProductMasterItem(productMasterItem);
            line.setItemId(mappedItem == null ? null : mappedItem.getItemId());
        } else {
            return null;
        }
        line.setItemName(item.getItemName());
        line.setItemCode(item.getItemCode());
        line.setProgram(item.getProgram());
        line.setUnitPrice(item.getUnitPrice());
        line.setQuantity(item.getQuantity());
        return line;
    }

    private void persistDraftCart(PharmacyService pharmacyService, LinkedHashMap<String, CartLine> cart) {
        clearDraftPurchaseOrders(pharmacyService);
        if (cart == null || cart.isEmpty()) {
            return;
        }
        Map<String, List<CartLine>> linesByProgram = new LinkedHashMap<String, List<CartLine>>();
        for (CartLine cartLine : cart.values()) {
            if (cartLine.getQuantity() == null || cartLine.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String program = normalizeProgram(cartLine.getProgram());
            if (!linesByProgram.containsKey(program)) {
                linesByProgram.put(program, new ArrayList<CartLine>());
            }
            linesByProgram.get(program).add(cartLine);
        }
        for (Map.Entry<String, List<CartLine>> entry : linesByProgram.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            PurchaseOrder draftOrder = new PurchaseOrder();
            draftOrder.setProgram(entry.getKey());
            draftOrder.setFacilityId(facilityId());
            draftOrder.setRequestedBy(Context.getAuthenticatedUser());
            draftOrder.setRequestedDate(new Date());
            draftOrder.setApprovalStatus(APPROVAL_STATUS_DRAFT);
            draftOrder.setDispatchStatus(DISPATCH_STATUS_DRAFT);
            draftOrder.setCreator(Context.getAuthenticatedUser());
            draftOrder.setDateCreated(new Date());
            draftOrder.setItems(new ArrayList<PurchaseOrderItem>());
            draftOrder.setTotalCost(BigDecimal.ZERO);
            pharmacyService.savePurchaseOrder(draftOrder);
            String orderNumber = "draft-" + draftOrder.getFacilityId() + "-" + draftOrder.getPurchaseOrderId();
            BigDecimal totalCost = BigDecimal.ZERO;
            for (CartLine cartLine : entry.getValue()) {
                PurchaseOrderItem item = purchaseOrderItemFromCartLine(pharmacyService, draftOrder, cartLine,
                        entry.getKey(), orderNumber);
                if (item == null) {
                    continue;
                }
                totalCost = totalCost.add(item.getTotalPrice());
                draftOrder.getItems().add(item);
            }
            if (draftOrder.getItems().isEmpty()) {
                draftOrder.setVoided(Boolean.TRUE);
                draftOrder.setVoidedBy(Context.getAuthenticatedUser());
                draftOrder.setDateVoided(new Date());
                draftOrder.setVoidReason("No active Product Master items remained in the draft cart");
                pharmacyService.savePurchaseOrder(draftOrder);
                continue;
            }
            draftOrder.setTotalCost(totalCost);
            pharmacyService.savePurchaseOrder(draftOrder);
        }
    }

    private PurchaseOrderItem purchaseOrderItemFromCartLine(PharmacyService pharmacyService, PurchaseOrder purchaseOrder,
                                                            CartLine cartLine, String program, String orderNumber) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(purchaseOrder);
        ProductMasterItem productMasterItem = cartLine.getProductMasterItemId() == null
                ? null : pharmacyService.getProductMasterItem(cartLine.getProductMasterItemId());
        if (productMasterItem == null || Boolean.FALSE.equals(productMasterItem.getActive())) {
            return null;
        }
        item.setProductMasterItem(productMasterItem);
        item.setOrderNumber(requiredText(orderNumber, "draft"));
        item.setProgram(requiredText(program, "General"));
        item.setItemName(requiredText(cartLine.getItemName(), "Unknown item"));
        item.setItemCode(requiredText(cartLine.getItemCode(), "UNKNOWN"));
        item.setQuantity(cartLine.getQuantity());
        item.setUnitPrice(cartLine.getUnitPrice() == null ? BigDecimal.ZERO : cartLine.getUnitPrice());
        item.setTotalPrice(item.getUnitPrice().multiply(cartLine.getQuantity()));
        item.setCreator(Context.getAuthenticatedUser());
        item.setDateCreated(new Date());
        return item;
    }

    private String requiredText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void clearDraftPurchaseOrders(PharmacyService pharmacyService) {
        List<PurchaseOrder> draftOrders = pharmacyService.getDraftPurchaseOrdersForUser(Context.getAuthenticatedUser());
        if (draftOrders == null) {
            return;
        }
        for (PurchaseOrder draftOrder : draftOrders) {
            if (draftOrder == null) {
                continue;
            }
            draftOrder.setVoided(Boolean.TRUE);
            draftOrder.setVoidedBy(Context.getAuthenticatedUser());
            draftOrder.setDateVoided(new Date());
            draftOrder.setVoidReason("Replaced by active purchase order draft cart");
            pharmacyService.savePurchaseOrder(draftOrder);
        }
    }

    private BigDecimal currentStock(List<InventoryBalance> stockBalances, Integer itemId) {
        BigDecimal total = BigDecimal.ZERO;
        for (InventoryBalance balance : stockBalances) {
            if (balance.getItem() != null && itemId.equals(balance.getItem().getItemId()) && balance.getQuantityOnHand() != null) {
                total = total.add(balance.getQuantityOnHand());
            }
        }
        return total;
    }

    private BigDecimal suggestedQuantity(Item item) {
        if (item == null || item.getLowStockThreshold() == null || item.getLowStockThreshold() <= 0) {
            return BigDecimal.ONE;
        }
        return new BigDecimal(item.getLowStockThreshold());
    }

    private BigDecimal latestUnitPrice(PharmacyService pharmacyService, String code, String program) {
        SupplierPrice latestPrice = code == null ? null : pharmacyService.getLatestSupplierPrice(code, program);
        if (latestPrice == null && program != null && !program.trim().isEmpty()) {
            latestPrice = pharmacyService.getLatestSupplierPrice(code);
        }
        return latestPrice == null ? null : latestPrice.getUnitPrice();
    }

    private List<CartLine> buildCartRows(LinkedHashMap<String, CartLine> cart) {
        return new ArrayList<CartLine>(cart.values());
    }

    private Map<Integer, String> buildCartItemKeyByItemId(LinkedHashMap<String, CartLine> cart) {
        Map<Integer, String> mapping = new LinkedHashMap<Integer, String>();
        for (CartLine line : cart.values()) {
            if (line.getItemId() != null) {
                mapping.put(line.getItemId(), line.getCartKey());
            }
        }
        return mapping;
    }

    private BigDecimal cartTotalQuantity(LinkedHashMap<String, CartLine> cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cart.values()) {
            if (line.getQuantity() != null) {
                total = total.add(line.getQuantity());
            }
        }
        return total;
    }

    private String resolveMainStoreName(List<InventoryBalance> stockBalances) {
        for (InventoryBalance balance : stockBalances) {
            if (balance.getPharmacyLocation() != null && Boolean.TRUE.equals(balance.getPharmacyLocation().getMainStore())) {
                String locationName = balance.getPharmacyLocation().getLocation() == null
                        ? null : balance.getPharmacyLocation().getLocation().getName();
                return balance.getPharmacyLocation().getName() + (locationName == null ? "" : " — " + locationName);
            }
        }
        for (InventoryBalance balance : stockBalances) {
            if (balance.getPharmacyLocation() != null) {
                return balance.getPharmacyLocation().getName();
            }
        }
        return "Central Pharmacy";
    }

    private String facilityId() {
        String facilityId = Context.getAdministrationService()
                .getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID, "");
        if (facilityId == null || facilityId.trim().isEmpty()) {
            facilityId = Context.getAdministrationService()
                    .getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID_LEGACY, "1660");
        }
        return facilityId;
    }

    private String cartKeyForItem(Integer itemId) {
        return "ITEM-" + itemId;
    }

    private String cartKeyForProductMaster(Integer productMasterItemId) {
        return "PM-" + productMasterItemId;
    }

    private String normalizeProgram(String program) {
        return (program == null || program.trim().isEmpty()) ? "General" : program.trim();
    }

    public static class CartLine implements Serializable {
        private String cartKey;
        private Integer itemId;
        private Integer productMasterItemId;
        private String itemName;
        private String itemCode;
        private String program;
        private BigDecimal unitPrice;
        private BigDecimal quantity;

        public String getCartKey() {
            return cartKey;
        }

        public void setCartKey(String cartKey) {
            this.cartKey = cartKey;
        }

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public Integer getProductMasterItemId() {
            return productMasterItemId;
        }

        public void setProductMasterItemId(Integer productMasterItemId) {
            this.productMasterItemId = productMasterItemId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public String getProgram() {
            return program;
        }

        public void setProgram(String program) {
            this.program = program;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
    }
}
