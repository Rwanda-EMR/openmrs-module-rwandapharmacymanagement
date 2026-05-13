<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def stockParams = { extra ->
        def params = [stockPage: stockPage, stockPageSize: stockPageSize, requestPage: requestPage, requestPageSize: requestPageSize]
        if (activePharmacy?.pharmacyLocationId) {
            params.pharmacyLocationId = activePharmacy.pharmacyLocationId
        }
        if (q) {
            params.q = q
        }
        params.putAll(extra)
        params
    }
    def clearStockSearchParams = { ->
        def params = [stockPage: 1, stockPageSize: stockPageSize, requestPage: requestPage, requestPageSize: requestPageSize]
        if (activePharmacy?.pharmacyLocationId) {
            params.pharmacyLocationId = activePharmacy.pharmacyLocationId
        }
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
            <select name="stockPageSize" onchange="this.form.submit()" style="width:auto;min-width:70px;">
                <% stockPageSizes.each { option -> %>
                <option value="${ option }" ${ option == stockPageSize ? 'selected="selected"' : '' }>${ option }</option>
                <% } %>
            </select>
            <% stockParams([stockPage: 1]).each { key, value -> if (key != 'stockPageSize' && value != null) { %>
            <input type="hidden" name="${ key }" value="${ value }"/>
            <% } } %>
        </form>
        <% if (stockHasPrevious) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", stockParams([stockPage: stockPage - 1])) }">Previous</a>
        <% } else { %>
        <span class="stock-btn" style="opacity:0.55;cursor:default;">Previous</span>
        <% } %>
        <span style="font-size:12px;color:#666;">Page ${ stockPage } of ${ stockTotalPages }</span>
        <% if (stockHasNext) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", stockParams([stockPage: stockPage + 1])) }">Next</a>
        <% } else { %>
        <span class="stock-btn" style="opacity:0.55;cursor:default;">Next</span>
        <% } %>
    </div>
</div>
<%
    }
    def renderRequestPager = { ->
%>
<div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin:10px 0 0;">
    <div style="font-size:12px;color:#666;">
        <% if ((requestTotalItems ?: 0) > 0) { %>
        Showing ${ requestStartItem }-${ requestEndItem } of ${ requestTotalItems }
        <% } else { %>
        No rows
        <% } %>
    </div>
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <form method="get" style="margin:0;display:flex;align-items:center;gap:6px;">
            <label style="font-size:11px;color:#555;">Rows per page</label>
            <select name="requestPageSize" onchange="this.form.submit()" style="width:auto;min-width:70px;">
                <% requestPageSizes.each { option -> %>
                <option value="${ option }" ${ option == requestPageSize ? 'selected="selected"' : '' }>${ option }</option>
                <% } %>
            </select>
            <% stockParams([requestPage: 1]).each { key, value -> if (key != 'requestPageSize' && value != null) { %>
            <input type="hidden" name="${ key }" value="${ value }"/>
            <% } } %>
        </form>
        <% if (requestHasPrevious) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", stockParams([requestPage: requestPage - 1])) }">Previous</a>
        <% } else { %>
        <span class="stock-btn" style="opacity:0.55;cursor:default;">Previous</span>
        <% } %>
        <span style="font-size:12px;color:#666;">Page ${ requestPage } of ${ requestTotalPages }</span>
        <% if (requestHasNext) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", stockParams([requestPage: requestPage + 1])) }">Next</a>
        <% } else { %>
        <span class="stock-btn" style="opacity:0.55;cursor:default;">Next</span>
        <% } %>
    </div>
</div>
<%
    }
%>
<h2>${ ui.message("rwandapharmacymanagement.inventory.dispensingStore.title") }</h2>

<% if (pharmacySelectionRequired) { %>
<div class="stock-warning" style="margin-bottom:12px;">
    <form method="get" style="margin:0;">
        <div style="margin-bottom:6px;"><strong>Choose pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.</div>
        <select name="pharmacyLocationId" required="required" style="margin-right:8px;">
            <option value="">Select pharmacy</option>
            <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
        </select>
        <button type="submit" class="stock-btn">Use pharmacy</button>
    </form>
</div>
<% } else if (!activePharmacy) { %>
<div class="stock-warning" style="margin-bottom:12px;">
    No pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.
</div>
<% } %>

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
        margin-bottom: 14px;
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
    .expiry-cell {
        color: #d9534f;
        font-weight: bold;
    }
    .qty-strong {
        font-weight: bold;
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
        width: min(960px, calc(100vw - 32px));
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
    .stock-note {
        margin-bottom: 10px;
        padding: 8px 10px;
        border: 1px solid #c4dfeb;
        background: #ebf6fb;
        color: #2a6277;
        font-size: 11px;
    }
    .stock-field label {
        display: block;
        margin-bottom: 3px;
        font-size: 11px;
        font-weight: bold;
        color: #555;
    }
    .stock-field input,
    .stock-field select,
    .stock-field textarea {
        width: 100%;
        box-sizing: border-box;
        padding: 5px 8px;
        border: 1px solid #ccc;
        border-radius: 3px;
        font-size: 12px;
        font-family: inherit;
    }
    .stock-cart-summary {
        margin: 8px 0 10px;
        font-size: 11px;
        color: #666;
    }
    .stock-subpanel {
        margin: 12px 0;
        padding: 10px 12px;
        border: 1px solid #ddd;
        background: #fafafa;
        border-radius: 3px;
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
    .request-row-list {
        display: grid;
        gap: 8px;
        margin-bottom: 8px;
    }
    .request-row {
        display: grid;
        grid-template-columns: minmax(0, 1.8fr) 140px 120px 110px;
        gap: 8px;
        align-items: end;
    }
    .request-row-available {
        font-size: 11px;
        color: #666;
        line-height: 1.3;
        padding: 6px 8px;
        border: 1px solid #ddd;
        border-radius: 3px;
        background: #fff;
        min-height: 30px;
        display: flex;
        align-items: center;
    }
    .request-row-actions {
        display: flex;
        gap: 8px;
        align-items: center;
        margin-top: 8px;
    }
    .cart-empty-note {
        padding: 8px 0;
        color: #777;
    }
    .requests-table-wrap {
        overflow-x: auto;
    }
    .requests-items div {
        margin-bottom: 2px;
    }
    .stock-warning {
        padding: 12px;
        border: 1px solid #f0ad4e;
        background: #fcf8e3;
        color: #8a6d3b;
        border-radius: 4px;
    }
    @media (max-width: 900px) {
        .request-row {
            grid-template-columns: 1fr;
        }
    }
</style>

<% if (activePharmacy) { %>
<div class="stock-panel">
    <div class="stock-panel-hdr">
        <div class="stock-panel-title">${ activePharmacy.name } • ${ stockTotalItems ?: 0 } batch records</div>
        <div class="stock-panel-actions">
            <a class="stock-btn stock-btn-primary" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", [cart: 'true']) }">Request cart<% if (cartCount) { %><span class="stock-cart-badge">${ cartCount }</span><% } %></a>
            <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "admin/reconciliation") }">Reconcile</a>
        </div>
    </div>
    <form class="stock-search-form" method="get">
        <input type="search" name="q" value="${ q ?: '' }" placeholder="Search item name, programme, or batch"/>
        <input type="hidden" name="stockPage" value="1"/>
        <input type="hidden" name="stockPageSize" value="${ stockPageSize }"/>
        <input type="hidden" name="requestPage" value="${ requestPage }"/>
        <input type="hidden" name="requestPageSize" value="${ requestPageSize }"/>
        <% if (activePharmacy?.pharmacyLocationId) { %>
        <input type="hidden" name="pharmacyLocationId" value="${ activePharmacy.pharmacyLocationId }"/>
        <% } %>
        <button type="submit" class="stock-btn stock-btn-primary">Search</button>
        <% if (q) { %>
        <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", clearStockSearchParams()) }">Clear</a>
        <% } %>
    </form>
    <table class="stock-table" data-no-pagination="true">
        <thead>
        <tr>
            <th>Item</th>
            <th>Programme</th>
            <th>Batch</th>
            <th>Expiry</th>
            <th>On hand</th>
            <th>Selling price</th>
            <th>Threshold</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <% stockBalances.each { balance ->
            def quantity = balance.quantityOnHand ?: 0
            def threshold = balance.item?.lowStockThreshold ?: 0
            def expired = balance.expiryDate && balance.expiryDate.before(today)
            def outOfStock = quantity == 0
            def lowStock = !outOfStock && threshold && quantity <= threshold
            def rowClass = outOfStock ? "stock-row-out" : (lowStock ? "stock-row-low" : "")
            def statusClass = expired ? "status-expired" : (outOfStock ? "status-out" : (lowStock ? "status-low" : "status-ok"))
            def statusText = expired ? "Expired" : (outOfStock ? "Out of stock" : (lowStock ? "Low stock" : "OK"))
            def programCode = balance.item?.program ?: (balance.item?.itemCategory?.name ? balance.item.itemCategory.name.replace("_", " ") : "GEN")
        %>
        <tr class="${ rowClass }">
            <td>${ balance.item?.name ?: '' }</td>
            <td><span class="batch-badge" style="background:#5bc0de;">${ programCode }</span></td>
            <td><span class="batch-badge">${ balance.batchNumber ?: 'N/A' }</span></td>
            <td class="${ expired ? 'expiry-cell' : '' }">${ balance.expiryDate ? ui.formatDatePretty(balance.expiryDate) : '' }</td>
            <td class="qty-strong">${ quantity }</td>
            <td>${ pricingService.calculateSellingPrice(balance.unitPrice) ?: '' }</td>
            <td>${ threshold }</td>
            <td><span class="status-pill ${ statusClass }">${ statusText }</span></td>
            <td>
                <div style="display:flex;gap:6px;flex-wrap:wrap;">
                    <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "admin/reconciliation") }">Adjust</a>
                    <% def mainStoreAvailable = mainStoreAvailableByItemId?.get(balance.item?.itemId) ?: 0 %>
                    <% if (cartItemKeyByItemId?.containsKey(balance.item?.itemId)) { def matchingRow = cartRows.find { row -> row.itemId == balance.item?.itemId } %>
                    <a class="stock-btn stock-btn-primary" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", [cart: 'true']) }">&#10003; In cart (${ matchingRow?.quantity ?: '' })</a>
                    <% } else if (mainStoreAvailable <= 0) { %>
                    <span class="stock-btn" style="opacity:0.55;cursor:not-allowed;">Unavailable</span>
                    <% } else { %>
                    <a class="stock-btn stock-btn-primary" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore", [requestItemId: balance.item?.itemId]) }">Request</a>
                    <% } %>
                </div>
            </td>
        </tr>
        <% } %>
        <% if (!stockBalances) { %>
        <tr>
            <td colspan="9" style="text-align:center;color:#777;">No stock is currently available in this pharmacy.</td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% renderStockPager() %>
</div>

<div class="stock-panel">
    <div class="stock-panel-hdr">
        <div class="stock-panel-title">Request history</div>
    </div>
    <div class="requests-table-wrap">
        <table class="stock-table" data-no-pagination="true">
            <thead>
            <tr>
                <th>ID</th>
                <th>Supplying pharmacy</th>
                <th>Status</th>
                <th>Requested</th>
                <th>Approved</th>
                <th>Served</th>
                <th>Items</th>
            </tr>
            </thead>
            <tbody>
            <% requests.each { request -> %>
            <tr>
                <td>${ request.interPharmacyTransferId }</td>
                <td>${ request.sourcePharmacyLocation?.name ?: '' }</td>
                <td>${ request.transferStatus ?: '' }</td>
                <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                <td>${ request.approvedDate ? ui.formatDatePretty(request.approvedDate) : '' }</td>
                <td>${ request.servedDate ? ui.formatDatePretty(request.servedDate) : '' }</td>
                <td class="requests-items">
                    <% request.lines?.each { line -> %>
                    <div>${ line.item?.name ?: '' } - ${ line.quantity ?: 0 }</div>
                    <% } %>
                </td>
            </tr>
            <% } %>
            <% if (!requests) { %>
            <tr>
                <td colspan="7" style="text-align:center;color:#777;">No requests found for this pharmacy yet.</td>
            </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <% renderRequestPager() %>
</div>
<% } %>

<% if (showRequestModal && selectedItem) { %>
<div class="stock-modal-backdrop"></div>
<div class="stock-modal" style="width:min(640px, calc(100vw - 32px));">
    <div class="stock-modal-header">
        <span>Request item — add to cart</span>
        <a class="stock-modal-close" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore") }">&times;</a>
    </div>
    <div class="stock-modal-body">
        <form method="post">
            <input type="hidden" name="action" value="addToRequestCart"/>
            <input type="hidden" name="itemId" value="${ selectedItem.itemId }"/>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:10px;">
                <div class="stock-field" style="grid-column:1 / -1;">
                    <label>Item</label>
                    <input type="text" value="${ selectedItem.name ?: '' }" readonly="readonly"/>
                </div>
                <div class="stock-field">
                    <label>Current stock on hand</label>
                    <input type="text" value="${ selectedCurrentStock } ${ selectedItem.code ? '(' + selectedItem.code + ')' : '' }" readonly="readonly"/>
                </div>
                <div class="stock-field">
                    <label>Threshold</label>
                    <input type="text" value="${ selectedItem.lowStockThreshold ?: 0 }" readonly="readonly"/>
                </div>
                <div class="stock-field">
                    <label>Main pharmacy available</label>
                    <input type="text" value="${ selectedMainStoreStock ?: 0 } ${ selectedItem.code ? '(' + selectedItem.code + ')' : '' }" readonly="readonly"/>
                </div>
                <div class="stock-field">
                    <label>Selling price</label>
                    <input type="text" value="${ selectedSellingPrice ?: '' }" readonly="readonly"/>
                </div>
                <div class="stock-field" style="grid-column:1 / -1;">
                    <label>Request quantity <span style="color:#a94442">*</span></label>
                    <input type="number" step="0.01" min="0.01" max="${ selectedMainStoreStock ?: '' }" name="quantity" value="${ selectedSuggestedQuantity && selectedMainStoreStock && selectedSuggestedQuantity <= selectedMainStoreStock ? selectedSuggestedQuantity : selectedMainStoreStock }" required="required"/>
                </div>
            </div>
            <div style="display:flex;gap:6px;align-items:center;">
                <button type="submit" class="stock-btn stock-btn-primary" ${ selectedMainStoreStock <= 0 ? 'disabled="disabled"' : '' }>Add to cart</button>
                <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore") }">Close</a>
            </div>
        </form>
    </div>
</div>
<% } %>

<% if (showCartModal && activePharmacy) { %>
<div class="stock-modal-backdrop"></div>
<div class="stock-modal">
    <div class="stock-modal-header">
        <span>Request cart — initiate stock request</span>
        <a class="stock-modal-close" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore") }">&times;</a>
    </div>
    <div class="stock-modal-body">
        <div class="stock-note">
            Add items that are currently available in the main pharmacy. Initiating the cart creates a requisition for approval and later service.
        </div>
        <div class="stock-subpanel" style="margin-top:0;">
            <div style="font-size:11px;font-weight:bold;color:#555;margin-bottom:4px;">Current request source</div>
            <div id="current-request-source-name" style="font-size:12px;color:#2c3c49;">
                ${ mainStore ? mainStore.name : 'Main pharmacy is not configured' }
            </div>
        </div>
        <% if (!cartRows) { %>
        <div class="cart-empty-note">Your request cart is empty. Add items below and then initiate the request.</div>
        <% } %>
        <div class="cart-table-wrap">
            <table class="stock-table">
                <thead>
                <tr>
                    <th>Item name</th>
                    <th>Item code</th>
                    <th>Requested qty</th>
                    <th>Remove</th>
                </tr>
                </thead>
                <tbody>
                <% cartRows.each { row -> %>
                <tr>
                    <td>${ row.itemName ?: '' }</td>
                    <td>${ row.itemCode ?: '' }</td>
                    <td style="min-width:150px;">
                        <form method="post" style="margin:0;display:flex;gap:6px;align-items:center;">
                            <input type="hidden" name="action" value="updateCart"/>
                            <input type="hidden" name="cartKey" value="${ row.cartKey }"/>
                            <input type="number" step="0.01" min="0.01" name="quantity" value="${ row.quantity }" style="width:78px;"/>
                            <button type="submit" class="stock-btn">Update</button>
                        </form>
                    </td>
                    <td>
                        <form method="post" style="margin:0;">
                            <input type="hidden" name="action" value="removeFromCart"/>
                            <input type="hidden" name="cartKey" value="${ row.cartKey }"/>
                            <button type="submit" class="stock-btn stock-btn-danger">&#10005; Remove</button>
                        </form>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <div class="stock-cart-summary">${ cartCount } item(s) in cart | Total units requested: ${ cartTotalQuantity }</div>

        <div class="stock-subpanel">
            <form method="post" style="margin:0;" id="request-rows-form">
                <input type="hidden" name="action" value="addRequestRowsToCart"/>
                <div class="request-row-list" id="request-row-list">
                    <div class="request-row">
                        <div class="stock-field" style="margin:0;">
                            <label>Item</label>
                            <select name="itemIds" class="request-item-select" data-no-searchable="true" onchange="updateRequestRow(this)">
                                <option value="">Select item</option>
                            </select>
                        </div>
                        <div>
                            <label style="display:block;margin-bottom:3px;font-size:11px;font-weight:bold;color:#555;">Available</label>
                            <div class="request-row-available">Select item</div>
                        </div>
                        <div class="stock-field" style="margin:0;">
                            <label>Qty</label>
                            <input type="number" step="0.01" min="0.01" name="itemQuantities" placeholder="Qty"/>
                        </div>
                        <div>
                            <button type="button" class="stock-btn" onclick="removeRequestRow(this)">Remove row</button>
                        </div>
                    </div>
                </div>
                <div class="request-row-actions">
                    <button type="button" class="stock-btn" onclick="addRequestRow()">Add another row</button>
                    <button type="submit" class="stock-btn">Add item(s)</button>
                </div>
            </form>
        </div>

        <form method="post" id="submit-request-cart-form" style="margin:0;">
            <input type="hidden" name="action" value="submitRequestCart"/>
            <div class="stock-field" style="margin:0 0 10px 0;">
                <label>Reason</label>
                <textarea name="reason" rows="2" placeholder="Why are these items needed?"></textarea>
            </div>
        </form>

        <div class="stock-panel-actions">
            <button type="submit" form="submit-request-cart-form" class="stock-btn stock-btn-primary" ${ !cartRows ? 'disabled="disabled"' : '' }>&#10003; Initiate request</button>
            <form method="post" style="margin:0;">
                <input type="hidden" name="action" value="clearCart"/>
                <button type="submit" class="stock-btn stock-btn-danger">Clear cart</button>
            </form>
            <a class="stock-btn" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/dispensingStore") }">Close</a>
        </div>
    </div>
</div>

<template id="request-row-template">
    <div class="request-row">
        <div class="stock-field" style="margin:0;">
            <label>Item</label>
            <select name="itemIds" class="request-item-select" data-no-searchable="true" onchange="updateRequestRow(this)">
                <option value="">Select item</option>
            </select>
        </div>
        <div>
            <label style="display:block;margin-bottom:3px;font-size:11px;font-weight:bold;color:#555;">Available</label>
            <div class="request-row-available">Select item</div>
        </div>
        <div class="stock-field" style="margin:0;">
            <label>Qty</label>
            <input type="number" step="0.01" min="0.01" name="itemQuantities" placeholder="Qty"/>
        </div>
        <div>
            <button type="button" class="stock-btn" onclick="removeRequestRow(this)">Remove row</button>
        </div>
    </div>
</template>

<script type="text/javascript">
    var requestSourceStock = [
        <% requestSourceStock.eachWithIndex { itemRow, itemIndex -> %>
        {
            itemId: "${ itemRow.itemId }",
            name: "${ itemRow.name?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }",
            code: "${ itemRow.code?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }",
            available: "${ itemRow.available ?: 0 }"
        }${ itemIndex + 1 < requestSourceStock.size() ? ',' : '' }
        <% } %>
    ];

    function createOption(value, label) {
        var option = document.createElement('option');
        option.value = value;
        option.textContent = label;
        return option;
    }

    function syncRequestSourceSelection() {
        Array.prototype.slice.call(document.querySelectorAll('#request-row-list .request-row')).forEach(function(row) {
            populateRequestRow(row);
        });
    }

    function populateRequestRow(row) {
        var itemSelect = row.querySelector('.request-item-select');
        var availableBox = row.querySelector('.request-row-available');
        var quantityInput = row.querySelector('input[name="itemQuantities"]');
        if (!itemSelect) {
            return;
        }
        var previousValue = itemSelect.value;
        itemSelect.innerHTML = '';
        itemSelect.appendChild(createOption('', 'Select item'));
        requestSourceStock.forEach(function(item) {
            itemSelect.appendChild(createOption(item.itemId, item.name + (item.code ? ' (' + item.code + ')' : '')));
        });
        if (previousValue) {
            itemSelect.value = previousValue;
        }
        if (itemSelect.value !== previousValue) {
            quantityInput.value = '';
        }
        updateRequestRow(itemSelect);
    }

    function updateRequestRow(select) {
        var row = select.closest('.request-row');
        if (!row) {
            return;
        }
        var availableBox = row.querySelector('.request-row-available');
        var quantityInput = row.querySelector('input[name="itemQuantities"]');
        var item = requestSourceStock.find(function(candidate) {
            return candidate.itemId === select.value;
        });
        if (!item) {
            availableBox.textContent = 'Select item';
            quantityInput.max = '';
            return;
        }
        availableBox.textContent = item.available + ' available';
        quantityInput.max = item.available;
    }

    function addRequestRow() {
        var container = document.getElementById('request-row-list');
        var template = document.getElementById('request-row-template');
        if (!container || !template || !template.content || !template.content.firstElementChild) {
            return;
        }
        var row = template.content.firstElementChild.cloneNode(true);
        container.appendChild(row);
        populateRequestRow(row);
    }

    function removeRequestRow(button) {
        var container = document.getElementById('request-row-list');
        var row = button ? button.closest('.request-row') : null;
        if (!container || !row) {
            return;
        }
        if (container.children.length === 1) {
            var select = row.querySelector('.request-item-select');
            var quantityInput = row.querySelector('input[name="itemQuantities"]');
            var availableBox = row.querySelector('.request-row-available');
            if (select) {
                select.selectedIndex = 0;
            }
            if (quantityInput) {
                quantityInput.value = '';
                quantityInput.max = '';
            }
            if (availableBox) {
                availableBox.textContent = 'Select item';
            }
            return;
        }
        row.remove();
    }

    syncRequestSourceSelection();
</script>
<% } %>
