package org.openmrs.module.rwandapharmacymanagement.page.controller.inventory;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.InventoryService;
import org.openmrs.module.rwandapharmacymanagement.api.PharmacyService;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransfer;
import org.openmrs.module.rwandapharmacymanagement.model.InterPharmacyTransferLine;
import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;
import org.openmrs.module.rwandapharmacymanagement.page.controller.support.PaginationSupport;
import org.openmrs.module.rwandapharmacymanagement.api.PricingService;
import org.openmrs.module.rwandapharmacymanagement.web.support.CurrentPharmacySupport;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class DispensingStorePageController {

    private static final String REQUEST_CART_SESSION_KEY = "rwandaPharmacyDispensingRequestCart";

    public void get(PageModel model,
                    @SpringBean("pharmacyService") PharmacyService pharmacyService,
                    @SpringBean("inventoryService") InventoryService inventoryService,
                    @SpringBean("pricingService") PricingService pricingService,
                    @RequestParam(value = "requestItemId", required = false) Integer requestItemId,
                    @RequestParam(value = "pharmacyLocationId", required = false) Integer pharmacyLocationId,
                    @RequestParam(value = "cart", required = false) Boolean showCart,
                    @RequestParam(value = "requested", required = false) String requested,
                    @RequestParam(value = "q", required = false) String query,
                    @RequestParam(value = "stockPage", required = false) Integer stockPage,
                    @RequestParam(value = "stockPageSize", required = false) Integer stockPageSize,
                    @RequestParam(value = "requestPage", required = false) Integer requestPage,
                    @RequestParam(value = "requestPageSize", required = false) Integer requestPageSize,
                    HttpSession session) {
        CurrentPharmacySupport.rememberSelection(pharmacyService, session, pharmacyLocationId);
        CurrentPharmacySupport.populateModel(model, pharmacyService, session);
        PharmacyLocation activePharmacy = (PharmacyLocation) model.getAttribute("activePharmacy");
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        List<InventoryBalance> allStockBalances = activePharmacy == null
                ? Collections.<InventoryBalance>emptyList()
                : inventoryService.getInventoryForPharmacy(activePharmacy.getPharmacyLocationId());
        allStockBalances = filterStockBalances(allStockBalances, query);
        int currentStockPage = PaginationSupport.sanitizePage(stockPage);
        int currentStockPageSize = PaginationSupport.sanitizePageSize(stockPageSize);
        currentStockPage = PaginationSupport.clampPage(currentStockPage, currentStockPageSize, allStockBalances.size());
        List<InventoryBalance> stockBalances = PaginationSupport.slice(allStockBalances, currentStockPage, currentStockPageSize);
        List<InventoryBalance> mainStoreBalances = mainStore == null
                ? Collections.<InventoryBalance>emptyList()
                : inventoryService.getInventoryForPharmacy(mainStore.getPharmacyLocationId());
        List<InterPharmacyTransfer> allRequests = activePharmacy == null
                ? Collections.<InterPharmacyTransfer>emptyList()
                : requestHistory(inventoryService, activePharmacy);
        int currentRequestPage = PaginationSupport.sanitizePage(requestPage);
        int currentRequestPageSize = PaginationSupport.sanitizePageSize(requestPageSize);
        currentRequestPage = PaginationSupport.clampPage(currentRequestPage, currentRequestPageSize, allRequests.size());
        List<InterPharmacyTransfer> requests = PaginationSupport.slice(allRequests, currentRequestPage, currentRequestPageSize);
        LinkedHashMap<String, CartLine> cart = getCart(session);
        Item selectedItem = requestItemId == null ? null : pharmacyService.getItem(requestItemId);
        model.addAttribute("activePharmacy", activePharmacy);
        model.addAttribute("mainStore", mainStore);
        model.addAttribute("today", new Date());
        model.addAttribute("stockBalances", stockBalances);
        PaginationSupport.addAttributes(model, "stock", currentStockPage, currentStockPageSize, allStockBalances.size());
        model.addAttribute("items", pharmacyService.getAllItems());
        model.addAttribute("requestSourceStock", buildStockRows(mainStoreBalances));
        model.addAttribute("mainStoreAvailableByItemId", buildAvailableByItemId(mainStoreBalances));
        model.addAttribute("cartRows", new ArrayList<CartLine>(cart.values()));
        model.addAttribute("cartCount", cart.size());
        model.addAttribute("cartTotalQuantity", cartTotalQuantity(cart));
        model.addAttribute("cartItemKeyByItemId", buildCartItemKeyByItemId(cart));
        model.addAttribute("showRequestModal", selectedItem != null);
        model.addAttribute("selectedItem", selectedItem);
        model.addAttribute("selectedCurrentStock", selectedItem == null ? BigDecimal.ZERO : currentStock(allStockBalances, selectedItem.getItemId()));
        model.addAttribute("selectedMainStoreStock", selectedItem == null ? BigDecimal.ZERO : currentStock(mainStoreBalances, selectedItem.getItemId()));
        model.addAttribute("selectedSellingPrice", selectedItem == null ? null : pricingService.calculateSellingPrice(unitPriceForItem(allStockBalances, selectedItem.getItemId())));
        model.addAttribute("selectedSuggestedQuantity", selectedItem == null ? null : suggestedQuantity(selectedItem));
        model.addAttribute("showCartModal", Boolean.TRUE.equals(showCart));
        model.addAttribute("requestSubmitted", requested != null);
        model.addAttribute("requests", requests);
        PaginationSupport.addAttributes(model, "request", currentRequestPage, currentRequestPageSize, allRequests.size());
        model.addAttribute("pricingService", pricingService);
        model.addAttribute("q", trimToEmpty(query));
    }

    private List<InventoryBalance> filterStockBalances(List<InventoryBalance> balances, String query) {
        String normalizedQuery = trimToEmpty(query).toLowerCase(Locale.ENGLISH);
        if (normalizedQuery.isEmpty()) {
            return balances;
        }

        List<InventoryBalance> filtered = new ArrayList<InventoryBalance>();
        for (InventoryBalance balance : balances) {
            if (matchesStockSearch(balance, normalizedQuery)) {
                filtered.add(balance);
            }
        }
        return filtered;
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
                       @SpringBean("inventoryService") InventoryService inventoryService,
                       @RequestParam(value = "action", required = false) String action,
                       @RequestParam(value = "itemId", required = false) Integer itemId,
                       @RequestParam(value = "itemIds", required = false) Integer[] itemIds,
                       @RequestParam(value = "itemQuantities", required = false) BigDecimal[] itemQuantities,
                       @RequestParam(value = "cartKey", required = false) String cartKey,
                       @RequestParam(value = "quantity", required = false) BigDecimal quantity,
                       @RequestParam(value = "reason", required = false) String reason,
                       HttpSession session,
                       UiUtils ui) {
        PharmacyLocation activePharmacy = CurrentPharmacySupport.requireCurrentPharmacy(pharmacyService, session);
        PharmacyLocation mainStore = pharmacyService.getMainStore();
        if (mainStore == null) {
            throw new IllegalArgumentException("Main pharmacy is not configured");
        }
        if ("addToRequestCart".equals(action)) {
            addItemToCart(session, pharmacyService, inventoryService, mainStore, itemId, quantity);
            return redirectToCart(ui);
        }
        if ("addRequestRowsToCart".equals(action)) {
            addItemsToCart(session, pharmacyService, inventoryService, mainStore, itemIds, itemQuantities);
            return redirectToCart(ui);
        }
        if ("updateCart".equals(action)) {
            updateCart(session, pharmacyService, inventoryService, mainStore, cartKey, quantity);
            return redirectToCart(ui);
        }
        if ("removeFromCart".equals(action)) {
            removeFromCart(session, cartKey);
            return redirectToCart(ui);
        }
        if ("clearCart".equals(action)) {
            session.removeAttribute(REQUEST_CART_SESSION_KEY);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore");
        }
        if ("submitRequestCart".equals(action)) {
            submitCart(session, pharmacyService, inventoryService, activePharmacy, mainStore.getPharmacyLocationId(), reason);
            return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore",
                    Collections.<String, Object>singletonMap("requested", "true"));
        }
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore");
    }

    private String redirectToCart(UiUtils ui) {
        return "redirect:" + ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore",
                Collections.<String, Object>singletonMap("cart", "true"));
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, CartLine> getCart(HttpSession session) {
        Object stored = session.getAttribute(REQUEST_CART_SESSION_KEY);
        if (stored instanceof LinkedHashMap) {
            return (LinkedHashMap<String, CartLine>) stored;
        }
        LinkedHashMap<String, CartLine> cart = new LinkedHashMap<String, CartLine>();
        session.setAttribute(REQUEST_CART_SESSION_KEY, cart);
        return cart;
    }

    private void addItemToCart(HttpSession session, PharmacyService pharmacyService, InventoryService inventoryService,
                               PharmacyLocation mainStore, Integer itemId, BigDecimal quantity) {
        if (itemId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Item item = pharmacyService.getItem(itemId);
        if (item == null) {
            return;
        }
        BigDecimal available = availableInPharmacy(inventoryService, mainStore, itemId);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Requested item is not currently available in the main pharmacy");
        }
        if (quantity.compareTo(available) > 0) {
            throw new IllegalArgumentException("Requested quantity is greater than what is available in the main pharmacy");
        }
        LinkedHashMap<String, CartLine> cart = getCart(session);
        String key = "ITEM-" + itemId;
        CartLine line = cart.get(key);
        if (line == null) {
            line = new CartLine();
            line.setCartKey(key);
            line.setItemId(itemId);
            line.setItemName(item.getName());
            line.setItemCode(item.getCode());
        }
        line.setQuantity(quantity);
        cart.put(key, line);
    }

    private void addItemsToCart(HttpSession session, PharmacyService pharmacyService, InventoryService inventoryService,
                                PharmacyLocation mainStore, Integer[] itemIds, BigDecimal[] quantities) {
        if (itemIds == null || quantities == null) {
            return;
        }
        for (int i = 0; i < itemIds.length; i++) {
            addItemToCart(session, pharmacyService, inventoryService, mainStore, itemIds[i], quantities.length > i ? quantities[i] : null);
        }
    }

    private void updateCart(HttpSession session, PharmacyService pharmacyService, InventoryService inventoryService,
                            PharmacyLocation mainStore, String cartKey, BigDecimal quantity) {
        LinkedHashMap<String, CartLine> cart = getCart(session);
        if (cartKey == null || !cart.containsKey(cartKey)) {
            return;
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            cart.remove(cartKey);
            return;
        }
        CartLine line = cart.get(cartKey);
        BigDecimal available = availableInPharmacy(inventoryService, mainStore, line.getItemId());
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Requested item is not currently available in the main pharmacy");
        }
        if (quantity.compareTo(available) > 0) {
            throw new IllegalArgumentException("Requested quantity is greater than what is available in the main pharmacy");
        }
        line.setQuantity(quantity);
    }

    private void removeFromCart(HttpSession session, String cartKey) {
        if (cartKey != null) {
            getCart(session).remove(cartKey);
        }
    }

    private void submitCart(HttpSession session, PharmacyService pharmacyService, InventoryService inventoryService,
                            PharmacyLocation activePharmacy, Integer sourcePharmacyLocationId, String reason) {
        LinkedHashMap<String, CartLine> cart = getCart(session);
        if (cart.isEmpty() || sourcePharmacyLocationId == null) {
            return;
        }
        PharmacyLocation source = pharmacyService.getPharmacyLocation(sourcePharmacyLocationId);
        if (source == null || source.getPharmacyLocationId().equals(activePharmacy.getPharmacyLocationId())) {
            throw new IllegalArgumentException("Select a valid supplying pharmacy");
        }
        InterPharmacyTransfer transfer = new InterPharmacyTransfer();
        transfer.setSourcePharmacyLocation(source);
        transfer.setDestinationPharmacyLocation(activePharmacy);
        transfer.setReason(reason);
        transfer.setTransferStatus("REQUESTED");
        transfer.setCreator(Context.getAuthenticatedUser());
        transfer.setDateCreated(new java.util.Date());
        List<InterPharmacyTransferLine> lines = new ArrayList<InterPharmacyTransferLine>();
        for (CartLine cartLine : cart.values()) {
            if (cartLine.getItemId() == null || cartLine.getQuantity() == null || cartLine.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            InterPharmacyTransferLine line = new InterPharmacyTransferLine();
            line.setInterPharmacyTransfer(transfer);
            line.setItem(pharmacyService.getItem(cartLine.getItemId()));
            line.setQuantity(cartLine.getQuantity());
            lines.add(line);
        }
        transfer.setLines(lines);
        inventoryService.requestTransfer(transfer);
        session.removeAttribute(REQUEST_CART_SESSION_KEY);
    }

    private List<Map<String, Object>> buildStockRows(List<InventoryBalance> balances) {
        List<Map<String, Object>> itemRows = new ArrayList<Map<String, Object>>();
        Map<Integer, Map<String, Object>> byItem = new LinkedHashMap<Integer, Map<String, Object>>();
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            Map<String, Object> itemRow = byItem.get(itemId);
            if (itemRow == null) {
                itemRow = new LinkedHashMap<String, Object>();
                itemRow.put("itemId", itemId);
                itemRow.put("name", balance.getItem().getName());
                itemRow.put("code", balance.getItem().getCode());
                itemRow.put("available", BigDecimal.ZERO);
                byItem.put(itemId, itemRow);
                itemRows.add(itemRow);
            }
            BigDecimal available = balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
            itemRow.put("available", ((BigDecimal) itemRow.get("available")).add(available));
        }
        return itemRows;
    }

    private BigDecimal unitPriceForItem(List<InventoryBalance> balances, Integer itemId) {
        if (itemId == null) {
            return null;
        }
        for (InventoryBalance balance : balances) {
            if (balance != null && balance.getItem() != null && itemId.equals(balance.getItem().getItemId())) {
                return balance.getUnitPrice();
            }
        }
        return null;
    }

    private Map<Integer, BigDecimal> buildAvailableByItemId(List<InventoryBalance> balances) {
        Map<Integer, BigDecimal> availableByItemId = new LinkedHashMap<Integer, BigDecimal>();
        for (InventoryBalance balance : balances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            Integer itemId = balance.getItem().getItemId();
            BigDecimal available = balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
            if (!availableByItemId.containsKey(itemId)) {
                availableByItemId.put(itemId, BigDecimal.ZERO);
            }
            availableByItemId.put(itemId, availableByItemId.get(itemId).add(available));
        }
        return availableByItemId;
    }

    private BigDecimal availableInPharmacy(InventoryService inventoryService, PharmacyLocation pharmacyLocation, Integer itemId) {
        if (pharmacyLocation == null || itemId == null) {
            return BigDecimal.ZERO;
        }
        return currentStock(inventoryService.getInventoryForPharmacy(pharmacyLocation.getPharmacyLocationId()), itemId);
    }

    private List<InterPharmacyTransfer> requestHistory(InventoryService inventoryService, PharmacyLocation activePharmacy) {
        if (activePharmacy == null || activePharmacy.getPharmacyLocationId() == null) {
            return new ArrayList<InterPharmacyTransfer>();
        }
        return new ArrayList<InterPharmacyTransfer>(
                inventoryService.getTransfers(null, null, activePharmacy.getPharmacyLocationId(), 200));
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

    private Map<Integer, String> buildCartItemKeyByItemId(LinkedHashMap<String, CartLine> cart) {
        Map<Integer, String> byItemId = new LinkedHashMap<Integer, String>();
        for (CartLine line : cart.values()) {
            if (line.getItemId() != null) {
                byItemId.put(line.getItemId(), line.getCartKey());
            }
        }
        return byItemId;
    }

    private BigDecimal currentStock(List<InventoryBalance> stockBalances, Integer itemId) {
        BigDecimal total = BigDecimal.ZERO;
        if (itemId == null) {
            return total;
        }
        for (InventoryBalance balance : stockBalances) {
            if (balance == null || balance.getItem() == null || balance.getItem().getItemId() == null) {
                continue;
            }
            if (itemId.equals(balance.getItem().getItemId()) && balance.getQuantityOnHand() != null) {
                total = total.add(balance.getQuantityOnHand());
            }
        }
        return total;
    }

    private BigDecimal suggestedQuantity(Item item) {
        if (item == null || item.getLowStockThreshold() == null || item.getLowStockThreshold() <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(item.getLowStockThreshold());
    }

    public static class CartLine implements Serializable {
        private String cartKey;
        private Integer itemId;
        private String itemName;
        private String itemCode;
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

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
    }
}
