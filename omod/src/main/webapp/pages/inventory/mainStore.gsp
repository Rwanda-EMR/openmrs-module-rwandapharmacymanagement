<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def today = new Date()
    def stockParams = { extra ->
        def params = [page: stockPage, pageSize: stockPageSize]
        if (q) {
            params.q = q
        }
        params.putAll(extra)
        params
    }
    def renderStockPager = { ->
%>
<div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin:10px 0 0;">
    <div style="font-size:12px;color:#666;">
        <% if ((stockTotalItems ?: 0) > 0) { %>
        Showing ${ stockStartItem }-${ stockEndItem } of ${ stockTotalItems }
        <% } else { %>
        No rows
        <% } %>
    </div>
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <form method="get" style="margin:0;display:flex;align-items:center;gap:6px;">
            <label style="font-size:11px;color:#555;">Rows per page</label>
            <select name="pageSize" onchange="this.form.submit()" style="width:auto;min-width:70px;">
                <% stockPageSizes.each { option -> %>
                <option value="${ option }" ${ option == stockPageSize ? 'selected="selected"' : '' }>${ option }</option>
                <% } %>
            </select>
            <input type="hidden" name="page" value="1"/>
            <% if (q) { %><input type="hidden" name="q" value="${ q }"/><% } %>
        </form>
        <% if (stockHasPrevious) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", stockParams([page: stockPage - 1])) }">Previous</a>
        <% } else { %>
        <span class="stock-btn" style="opacity:0.55;cursor:default;">Previous</span>
        <% } %>
        <span style="font-size:12px;color:#666;">Page ${ stockPage } of ${ stockTotalPages }</span>
        <% if (stockHasNext) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", stockParams([page: stockPage + 1])) }">Next</a>
        <% } else { %>
        <span class="stock-btn" style="opacity:0.55;cursor:default;">Next</span>
        <% } %>
    </div>
</div>
<%
    }
%>
<h2>${ ui.message("rwandapharmacymanagement.inventory.mainStore.title") }</h2>

<style>
    :root {
        --stock-accent-blue: #2f6fad;
        --stock-accent-blue-dark: #255a8d;
        --stock-accent-blue-soft: #dcecff;
        --stock-accent-green-dark: #237555;
        --stock-accent-green-soft: #daf3e7;
        --stock-accent-teal: #0f8894;
        --stock-accent-red-dark: #a13d3b;
        --stock-accent-red-soft: #f7dedd;
        --stock-neutral-border: #cfd9e2;
        --stock-neutral-text: #2c3c49;
        --stock-neutral-bg: #ffffff;
        --stock-neutral-hover: #f6f9fb;
    }
    .stock-panel {
        background: #fff;
        border: 1px solid #ddd;
        border-radius: 3px;
        overflow: hidden;
    }
    .stock-panel-hdr {
        padding: 8px 12px;
        background: #f5f5f5;
        border-bottom: 1px solid #ddd;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
    }
    .stock-panel-title {
        font-size: 12px;
        font-weight: bold;
        color: #333;
    }
    .stock-panel-actions {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
    }
    .stock-search-form {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;
        padding: 10px 12px;
        border-bottom: 1px solid #ddd;
        background: #fff;
    }
    .stock-search-form input[type="search"] {
        width: min(360px, 100%);
        padding: 5px 8px;
        border: 1px solid #ccc;
        border-radius: 3px;
        font-size: 12px;
    }
    .stock-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 5px 11px;
        border-radius: 3px;
        border: 1px solid var(--stock-neutral-border);
        background: var(--stock-neutral-bg);
        color: var(--stock-neutral-text);
        font-size: 11px;
        font-weight: bold;
        text-decoration: none;
        line-height: 1.2;
        box-shadow: 0 1px 2px rgba(15, 35, 55, 0.05);
    }
    .stock-btn:hover {
        text-decoration: none;
        background: var(--stock-neutral-hover);
        border-color: #bcc9d3;
        color: #21303b;
    }
    .stock-btn-primary {
        background: var(--stock-accent-blue-soft);
        border-color: #b8d3f1;
        color: var(--stock-accent-blue-dark);
    }
    .stock-btn-success {
        background: var(--stock-accent-green-soft);
        border-color: #bfe4d3;
        color: var(--stock-accent-green-dark);
    }
    .stock-btn-danger {
        background: var(--stock-accent-red-soft);
        border-color: #e8b8b6;
        color: var(--stock-accent-red-dark);
    }
    .stock-cart-badge {
        display: inline-block;
        margin-left: 4px;
        min-width: 14px;
        padding: 1px 4px;
        border-radius: 8px;
        background: #d9534f;
        color: #fff;
        font-size: 9px;
        font-weight: bold;
        text-align: center;
        vertical-align: middle;
    }
    .stock-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 12px;
    }
    .stock-table th {
        background: #f5f5f5;
        color: #555;
        font-size: 11px;
        font-weight: bold;
        padding: 7px 8px;
        border: 1px solid #ddd;
        text-align: left;
        white-space: nowrap;
    }
    .stock-table td {
        padding: 8px;
        border: 1px solid #ddd;
        vertical-align: top;
    }
    .stock-table tr:nth-child(even) td {
        background: #fafafa;
    }
    .stock-row-low td {
        background: #fcf8e3 !important;
    }
    .stock-row-out td {
        background: #f9ecec !important;
    }
    .program-badge {
        display: inline-block;
        padding: 2px 6px;
        border-radius: 3px;
        background: #5bc0de;
        color: #fff;
        font-size: 10px;
        font-weight: bold;
        text-transform: uppercase;
    }
    .batch-badge {
        display: inline-block;
        padding: 2px 6px;
        border-radius: 3px;
        background: #8d99a6;
        color: #fff;
        font-size: 10px;
        font-weight: bold;
    }
    .expiry-cell {
        color: #d9534f;
        font-weight: bold;
    }
    .qty-strong {
        font-weight: bold;
    }
    .status-pill {
        display: inline-block;
        padding: 3px 8px;
        border-radius: 3px;
        color: #fff;
        font-size: 10px;
        font-weight: bold;
        text-transform: uppercase;
        white-space: nowrap;
    }
    .status-ok { background: #5cb85c; }
    .status-low { background: #f0ad4e; }
    .status-out { background: #d9534f; }
    .status-expired { background: #d9534f; }
    .row-actions {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
    }
    .row-actions .stock-btn {
        padding: 3px 8px;
        font-size: 11px;
    }
    .stock-btn-added {
        background: var(--stock-accent-green-soft);
        border-color: #bfe4d3;
        color: var(--stock-accent-green-dark);
    }
    .stock-modal-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.35);
        z-index: 900;
    }
    .stock-modal {
        position: fixed;
        z-index: 901;
        top: 88px;
        left: 50%;
        transform: translateX(-50%);
        width: min(640px, calc(100vw - 32px));
        max-height: calc(100vh - 120px);
        background: #fff;
        border-radius: 3px;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.28);
        overflow: hidden;
    }
    .stock-modal-header {
        padding: 10px 14px;
        background: var(--stock-accent-teal);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 13px;
        font-weight: bold;
    }
    .stock-modal-close {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 26px;
        height: 26px;
        border-radius: 13px;
        border: 1px solid rgba(255, 255, 255, 0.55);
        background: rgba(255, 255, 255, 0.16);
        color: #fff;
        font-size: 19px;
        font-weight: bold;
        line-height: 1;
    }
    .stock-modal-close:hover {
        text-decoration: none;
        background: rgba(255, 255, 255, 0.28);
    }
    .stock-modal-body {
        padding: 12px 14px 14px;
        max-height: calc(100vh - 180px);
        overflow: auto;
    }
    .stock-form-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 10px;
        margin-bottom: 10px;
    }
    .stock-field label {
        display: block;
        margin-bottom: 3px;
        font-size: 11px;
        font-weight: bold;
        color: #555;
    }
    .stock-field input,
    .stock-field textarea {
        width: 100%;
        box-sizing: border-box;
        padding: 5px 8px;
        border: 1px solid #ccc;
        border-radius: 3px;
        font-size: 12px;
        font-family: inherit;
    }
    .stock-field-full {
        grid-column: 1 / -1;
    }
    .stock-help {
        margin-top: 3px;
        font-size: 10px;
        color: #999;
    }
    .stock-modal-actions {
        display: flex;
        gap: 6px;
        align-items: center;
    }
    .stock-cart-summary {
        margin: 8px 0 10px;
        font-size: 11px;
        color: #666;
    }
    .cart-table-wrap {
        max-height: 320px;
        overflow-x: auto;
        overflow-y: auto;
        border: 1px solid #ddd;
        border-radius: 3px;
    }
    .cart-table-wrap .stock-table th {
        position: sticky;
        top: 0;
        z-index: 1;
    }
    .cart-total {
        font-weight: bold;
        color: #333;
    }
    .stock-note {
        margin-bottom: 10px;
        padding: 8px 10px;
        border: 1px solid #c4dfeb;
        background: #ebf6fb;
        color: #2a6277;
        font-size: 11px;
    }
    .stock-subpanel {
        margin: 12px 0;
        padding: 10px 12px;
        border: 1px solid #ddd;
        background: #fafafa;
        border-radius: 3px;
    }
    .product-master-rows {
        display: grid;
        gap: 8px;
        margin-bottom: 8px;
    }
    .product-master-row {
        display: grid;
        grid-template-columns: minmax(0, 1.8fr) 120px 110px;
        gap: 8px;
        align-items: end;
    }
    .product-master-actions {
        display: flex;
        gap: 8px;
        align-items: center;
        margin-top: 8px;
    }
    .product-master-filter {
        display: flex;
        gap: 8px;
        align-items: end;
        flex-wrap: wrap;
        margin-bottom: 10px;
    }
    .product-master-filter .stock-field {
        margin: 0;
        min-width: 220px;
    }
    .product-master-filter-count {
        color: #666;
        font-size: 12px;
        padding-bottom: 6px;
    }
    .product-master-combobox {
        position: relative;
    }
    .product-master-typeahead {
        width: 100%;
        box-sizing: border-box;
        padding: 5px 8px;
        border: 1px solid #ccc;
        border-radius: 3px;
        font-size: 12px;
    }
    .product-master-suggestions {
        position: absolute;
        top: calc(100% + 2px);
        left: 0;
        right: 0;
        max-height: 220px;
        overflow-y: auto;
        background: #fff;
        border: 1px solid #cfd9e2;
        border-radius: 3px;
        box-shadow: 0 4px 12px rgba(15, 35, 55, 0.12);
        z-index: 10;
        display: none;
    }
    .product-master-suggestions.active {
        display: block;
    }
    .product-master-suggestion {
        padding: 7px 8px;
        border-bottom: 1px solid #eef2f5;
        cursor: pointer;
        font-size: 12px;
        color: #2c3c49;
    }
    .product-master-suggestion:last-child {
        border-bottom: none;
    }
    .product-master-suggestion:hover,
    .product-master-suggestion.is-active {
        background: #eef6fc;
    }
    .product-master-suggestion-empty {
        padding: 7px 8px;
        font-size: 12px;
        color: #7a8791;
    }
    .cart-empty-note {
        padding: 8px 0;
        color: #777;
    }
    .stock-alert {
        margin: 0 0 12px 0;
        padding: 8px 12px;
        border-radius: 3px;
        font-size: 12px;
    }
    .stock-alert-error {
        border: 1px solid #ebccd1;
        background: #f2dede;
        color: #a94442;
    }
</style>

<% if (errorReference) { %>
<div class="stock-alert stock-alert-error">
    The order cart action could not be completed. Give this reference to the administrator: <strong>${ errorReference }</strong>.
</div>
<% } %>

<div class="stock-panel">
    <div class="stock-panel-hdr">
        <div class="stock-panel-title">${ mainStoreName } • ${ stockTotalItems ?: 0 } batch records</div>
        <div class="stock-panel-actions">
            <a class="stock-btn stock-btn-primary" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", [cart: 'true']) }">Order cart<% if (cartCount) { %><span class="stock-cart-badge">${ cartCount }</span><% } %></a>
            <a class="stock-btn stock-btn-success" href="${ ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive") }">Receive delivery</a>
            <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "admin/reconciliation") }">Reconcile</a>
        </div>
    </div>
    <form class="stock-search-form" method="get">
        <input type="search" name="q" value="${ q ?: '' }" placeholder="Search item name, programme, or batch"/>
        <input type="hidden" name="page" value="1"/>
        <input type="hidden" name="pageSize" value="${ stockPageSize }"/>
        <button type="submit" class="stock-btn stock-btn-primary">Search</button>
        <% if (q) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", [pageSize: stockPageSize]) }">Clear</a>
        <% } %>
    </form>
    <table class="stock-table" data-no-pagination="true">
        <thead>
        <tr>
            <th>Item</th>
            <th>Programme</th>
            <th>Batch</th>
            <th>Expiry</th>
            <th>Qty on hand</th>
            <th>Threshold</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <% stockBalances.each { balance ->
            def item = balance.item
            def quantity = balance.quantityOnHand ?: 0
            def threshold = item?.lowStockThreshold ?: 0
            def expired = balance.expiryDate && balance.expiryDate.before(today)
            def outOfStock = quantity == 0
            def lowStock = !outOfStock && threshold && quantity <= threshold
            def rowClass = outOfStock ? "stock-row-out" : (lowStock ? "stock-row-low" : "")
            def statusClass = expired ? "status-expired" : (outOfStock ? "status-out" : (lowStock ? "status-low" : "status-ok"))
            def statusText = expired ? "Expired" : (outOfStock ? "Out of stock" : (lowStock ? "Low stock" : "OK"))
            def programCode = item?.program ?: (item?.itemCategory?.name ? item.itemCategory.name.replace("_", " ") : "GEN")
        %>
        <tr class="${ rowClass }">
            <td>${ item?.name ?: '' }</td>
            <td><span class="program-badge">${ programCode }</span></td>
            <td><span class="batch-badge">${ balance.batchNumber ?: 'N/A' }</span></td>
            <td class="${ expired ? 'expiry-cell' : '' }">${ balance.expiryDate ? ui.formatDatePretty(balance.expiryDate) : '' }</td>
            <td class="qty-strong">${ quantity }</td>
            <td>${ threshold }</td>
            <td><span class="status-pill ${ statusClass }">${ statusText }</span></td>
            <td>
                <div class="row-actions">
                    <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "admin/reconciliation") }">Adjust</a>
                    <% if (cartItemKeyByItemId?.containsKey(item?.itemId)) { def matchingRow = cartRows.find { row -> row.itemId == item?.itemId } %>
                    <a class="stock-btn stock-btn-added" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", [cart: 'true']) }">&#10003; In cart (${ matchingRow?.quantity ?: '' })</a>
                    <% } else { %>
                    <a class="stock-btn stock-btn-primary" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore", [orderItemId: item?.itemId]) }">Order</a>
                    <% } %>
                </div>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% renderStockPager() %>
</div>

<% if (showOrderModal && selectedItem) { %>
<div class="stock-modal-backdrop"></div>
<div class="stock-modal">
    <div class="stock-modal-header">
        <span>Order medication — RMS</span>
        <a class="stock-modal-close" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore") }">&times;</a>
    </div>
    <div class="stock-modal-body">
        <form method="post">
            <input type="hidden" name="action" value="addToCart"/>
            <input type="hidden" name="itemId" value="${ selectedItem.itemId }"/>
            <div class="stock-form-grid">
                <div class="stock-field stock-field-full">
                    <label>Item</label>
                    <input type="text" value="${ selectedItem.name }" readonly="readonly"/>
                </div>
                <div class="stock-field">
                    <label>Current stock on hand</label>
                    <input type="text" value="${ selectedCurrentStock } ${ selectedItem.code ? '(' + selectedItem.code + ')' : '' }" readonly="readonly"/>
                </div>
                <div class="stock-field">
                    <label>Threshold (reorder level)</label>
                    <input type="text" value="${ selectedItem.lowStockThreshold ?: 0 }" readonly="readonly"/>
                </div>
                <div class="stock-field stock-field-full">
                    <label>Order quantity <span style="color:#a94442">*</span></label>
                    <input type="number" step="0.01" min="0.01" name="quantity" value="${ selectedSuggestedQuantity ?: '' }" required="required"/>
                    <div class="stock-help">Suggested: ${ selectedSuggestedQuantity ?: 1 } unit(s)</div>
                </div>
            </div>
            <div class="stock-modal-actions">
                <button type="submit" class="stock-btn stock-btn-primary">Add to cart</button>
                <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore") }">Close</a>
            </div>
        </form>
    </div>
</div>
<% } %>

<% if (showCartModal) { %>
<div class="stock-modal-backdrop"></div>
<div class="stock-modal" style="width:min(900px, calc(100vw - 32px));">
    <div class="stock-modal-header">
        <span>Order cart — review before initiating order</span>
        <a class="stock-modal-close" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore") }">&times;</a>
    </div>
    <div class="stock-modal-body">
        <div class="stock-note">
            Review all items and quantities carefully. Initiating the order will create purchase orders grouped by program for facility approval.
        </div>
        <% if (!cartRows) { %>
        <div class="cart-empty-note">Your order cart is empty. You can still add items directly from the product master below.</div>
        <% } %>
        <div class="cart-table-wrap">
            <table class="stock-table">
                <thead>
                <tr>
                    <th>Item name</th>
                    <th>Item code</th>
                    <th>Program</th>
                    <th>Unit price</th>
                    <th>Order qty</th>
                    <th>Total price</th>
                    <th>Remove</th>
                </tr>
                </thead>
                <tbody>
                <% cartRows.each { row -> %>
                <tr>
                    <td>${ row.itemName }</td>
                    <td>${ row.itemCode ?: '' }</td>
                    <td><span class="program-badge">${ row.program ?: 'GEN' }</span></td>
                    <% def updateFormId = "cart-update-" + row.cartKey?.replaceAll('[^A-Za-z0-9_-]', '-') %>
                    <td style="min-width:120px;">
                        <form id="${ updateFormId }" method="post" style="margin:0;">
                            <input type="hidden" name="action" value="updateCart"/>
                            <input type="hidden" name="cartKey" value="${ row.cartKey }"/>
                            <input type="number" step="0.01" min="0" name="unitPrice" value="${ row.unitPrice ?: 0 }" style="width:92px;"/>
                        </form>
                    </td>
                    <td style="min-width:150px;">
                        <div style="margin:0;display:flex;gap:6px;align-items:center;">
                            <input type="number" step="0.01" min="0.01" name="quantity" form="${ updateFormId }" value="${ row.quantity }" style="width:78px;"/>
                            <button type="submit" form="${ updateFormId }" class="stock-btn">Update</button>
                        </div>
                    </td>
                    <td class="cart-total">${ (row.unitPrice ?: 0) * (row.quantity ?: 0) }</td>
                    <td>
                        <form method="post" style="margin:0;">
                            <input type="hidden" name="action" value="removeFromCart"/>
                            <input type="hidden" name="cartKey" value="${ row.cartKey }"/>
                            <button type="submit" class="stock-btn stock-btn-danger" style="background:#d9534f;border-color:#c9302c;color:#fff;">&#10005; Remove</button>
                        </form>
                    </td>
                </tr>
                <% } %>
                <tr>
                    <td colspan="5" style="text-align:right;font-weight:bold;background:#f5f5f5;">Total cost</td>
                    <td class="cart-total" style="background:#f5f5f5;">
                        ${ cartRows.collect { row -> (row.unitPrice ?: 0) * (row.quantity ?: 0) }.sum() ?: 0 }
                    </td>
                    <td style="background:#f5f5f5;"></td>
                </tr>
                </tbody>
            </table>
        </div>
        <div class="stock-cart-summary">${ cartCount } item(s) in cart | Total units ordered: ${ cartTotalQuantity }</div>
        <div class="stock-subpanel">
            <form method="post" style="margin:0;">
                <input type="hidden" name="action" value="addProductMasterRowsToCart"/>
                <div class="stock-field" style="margin:0 0 8px 0;">
                    <label>Add item(s) from product master</label>
                </div>
                <div class="product-master-filter">
                    <div class="stock-field">
                        <label>Program</label>
                        <select id="product-master-program-filter" data-no-searchable="true" onchange="applyProductMasterProgramFilter()" style="width:100%;box-sizing:border-box;padding:5px 8px;border:1px solid #ccc;border-radius:3px;font-size:12px;">
                            <option value="">All programs</option>
                            <% productMasterPrograms.each { program -> %>
                            <option value="${ program }">${ program }</option>
                            <% } %>
                        </select>
                    </div>
                    <div class="product-master-filter-count" id="product-master-filter-count"></div>
                </div>
                <div class="product-master-rows" id="product-master-rows">
                    <div class="product-master-row">
                        <div class="stock-field" style="margin:0;">
                            <select name="productMasterItemIds" data-no-searchable="true" style="width:100%;box-sizing:border-box;padding:5px 8px;border:1px solid #ccc;border-radius:3px;font-size:12px;">
                                <option value="">Select item</option>
                                <% productMasterItems.each { item -> %>
                                <option value="${ item.productMasterItemId }" data-program="${ item.program ?: '' }">${ item.itemName ?: '' } (${ item.npcCode ?: '' })</option>
                                <% } %>
                            </select>
                        </div>
                        <div class="stock-field" style="margin:0;">
                            <input type="number" step="0.01" min="0.01" name="productMasterQuantities" placeholder="Qty"/>
                        </div>
                        <div>
                            <button type="button" class="stock-btn" onclick="removeProductMasterRow(this)">Remove row</button>
                        </div>
                    </div>
                </div>
                <div class="product-master-actions">
                    <button type="button" class="stock-btn" onclick="addProductMasterRow()">Add another row</button>
                    <button type="submit" class="stock-btn">Add item(s)</button>
                </div>
            </form>
        </div>
        <div class="stock-field" style="margin:0 0 10px 0;max-width:320px;">
            <label>Select supplier</label>
            <select name="supplierId" data-no-searchable="true" form="submit-cart-form" required="required" style="width:100%;box-sizing:border-box;padding:5px 8px;border:1px solid #ccc;border-radius:3px;font-size:12px;">
                <option value="">Choose supplier</option>
                <% suppliers.each { supplier -> %>
                <option value="${ supplier.supplierId }">${ supplier.name }</option>
                <% } %>
            </select>
        </div>
        <div class="stock-modal-actions" style="flex-wrap:nowrap;">
            <form method="post" id="submit-cart-form" style="margin:0;">
                <input type="hidden" name="action" value="submitCart"/>
                <button type="submit" class="stock-btn stock-btn-primary" ${ !cartRows ? 'disabled="disabled"' : '' }>&#10003; Initiate order</button>
            </form>
            <form method="post" style="margin:0;">
                <input type="hidden" name="action" value="clearCart"/>
                <button type="submit" class="stock-btn stock-btn-danger">Clear cart</button>
            </form>
            <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore") }">Close</a>
        </div>
    </div>
</div>
<% } %>

<div id="product-master-row-template" style="display:none;">
    <div class="product-master-row">
        <div class="stock-field" style="margin:0;">
            <select name="productMasterItemIds" data-no-searchable="true" style="width:100%;box-sizing:border-box;padding:5px 8px;border:1px solid #ccc;border-radius:3px;font-size:12px;">
                <option value="">Select item</option>
                <% productMasterItems.each { item -> %>
                <option value="${ item.productMasterItemId }" data-program="${ item.program ?: '' }">${ item.itemName ?: '' } (${ item.npcCode ?: '' })</option>
                <% } %>
            </select>
        </div>
        <div class="stock-field" style="margin:0;">
            <input type="number" step="0.01" min="0.01" name="productMasterQuantities" placeholder="Qty"/>
        </div>
        <div>
            <button type="button" class="stock-btn" onclick="removeProductMasterRow(this)">Remove row</button>
        </div>
    </div>
</div>

<script type="text/javascript">
    function addProductMasterRow() {
        var container = document.getElementById('product-master-rows');
        var template = document.getElementById('product-master-row-template');
        if (!container || !template) {
            return;
        }
        container.insertAdjacentHTML('beforeend', template.innerHTML);
        applyProductMasterProgramFilter();
    }

    function removeProductMasterRow(button) {
        var container = document.getElementById('product-master-rows');
        if (!container || !button) {
            return;
        }
        var row = button.closest('.product-master-row');
        if (!row) {
            return;
        }
        if (container.children.length === 1) {
            var selectedId = row.querySelector('select[name="productMasterItemIds"]');
            var quantity = row.querySelector('input[name="productMasterQuantities"]');
            if (selectedId) {
                selectedId.selectedIndex = 0;
            }
            if (quantity) {
                quantity.value = '';
            }
            applyProductMasterProgramFilter();
            return;
        }
        row.remove();
        applyProductMasterProgramFilter();
    }

    function applyProductMasterProgramFilter() {
        var filter = document.getElementById('product-master-program-filter');
        var selectedProgram = filter ? filter.value : '';
        var selects = document.querySelectorAll('select[name="productMasterItemIds"]');
        var availableCount = 0;
        var counted = {};

        Array.prototype.slice.call(selects).forEach(function(select) {
            var selectedOption = select.options[select.selectedIndex];
            var selectedStillAllowed = true;

            Array.prototype.slice.call(select.options).forEach(function(option) {
                if (!option.value) {
                    option.disabled = false;
                    option.hidden = false;
                    return;
                }
                var optionProgram = option.getAttribute('data-program') || '';
                var allowed = !selectedProgram || optionProgram === selectedProgram;
                option.disabled = !allowed;
                option.hidden = !allowed;
                if (allowed && !counted[option.value]) {
                    availableCount++;
                    counted[option.value] = true;
                }
                if (option === selectedOption && !allowed) {
                    selectedStillAllowed = false;
                }
            });

            if (!selectedStillAllowed) {
                select.selectedIndex = 0;
            }
        });

        var countLabel = document.getElementById('product-master-filter-count');
        if (countLabel) {
            countLabel.textContent = selectedProgram ? availableCount + ' item(s) available' : '';
        }
    }

    applyProductMasterProgramFilter();
</script>
