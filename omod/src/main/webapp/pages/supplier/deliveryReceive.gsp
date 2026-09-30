<%
    def currentPageTitle = "Receive stock"
    if (selectedView == "rms") {
        currentPageTitle = "Receive from RMS"
    } else if (selectedView == "supplier") {
        currentPageTitle = "Receive from other supplier"
    } else if (selectedView == "direct") {
        currentPageTitle = "Add new stock"
    } else if (selectedView == "receive" && selectedPurchaseOrder) {
        def receivePrefix = "PO"
        def supplierNameUpper = (selectedPurchaseOrder.supplier?.name ?: "").toUpperCase()
        def supplierCodeUpper = (selectedPurchaseOrder.supplier?.code ?: "").toUpperCase()
        if (selectedSource == "supplier" || (selectedSource != "rms" && !supplierNameUpper.contains("RMS") && !supplierCodeUpper.contains("RMS"))) {
            receivePrefix = "EXT"
        }
        currentPageTitle = "Order " + receivePrefix + "-" + selectedPurchaseOrder.purchaseOrderId.toString().padLeft(3, '0')
    }
    ui.decorateWith("rwandapharmacymanagement", "simplePage", [pageTitle: currentPageTitle])
    def contextPath = ui.contextPath() ?: ""
    if (!contextPath.startsWith("/")) {
        contextPath = "/" + contextPath
    }
    def templateUrl = ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceiptTemplate")
    def receiptPdfUrl = deliveryReceiptId ? ui.pageLink("rwandapharmacymanagement", "supplier/receiptPdf", [deliveryReceiptId: deliveryReceiptId]) : ""
    def itemSearchUrl = contextPath + "/module/rwandapharmacymanagement/product-master/search"
    def landingLink = ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive")
    def rmsLink = ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive", [view: "rms"])
    def supplierLink = ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive", [view: "supplier"])
    def directLink = ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive", [view: "direct"])
    def selectedLocationId = defaultReceiveLocation?.pharmacyLocationId ?: ''
    def formatShortDate = { value -> value ? value.format("yyyy-MM-dd") : "" }
    def fetchedDeliveryDate = fetchedReceipt?.deliveryDate ?: ""
    def fetchedReceiptNumber = fetchedReceipt?.deliveryId ?: ""
    def rmsOrderNumber = { purchaseOrder ->
        if (!purchaseOrder) {
            return ""
        }
        if (purchaseOrder.items) {
            def itemOrderNumber = purchaseOrder.items[0]?.orderNumber
            if (itemOrderNumber) {
                return itemOrderNumber
            }
        }
        if (purchaseOrder.dispatchReference && !purchaseOrder.dispatchReference.startsWith("{") && !purchaseOrder.dispatchReference.contains("rawResponse")) {
            return purchaseOrder.dispatchReference
        }
        return ""
    }
    def orderReference = { purchaseOrder ->
        if (!purchaseOrder) {
            return ""
        }
        def prefix = selectedSource == "supplier" || (selectedSource != "rms" && !(purchaseOrder.supplier?.name ?: "").toUpperCase().contains("RMS") && !(purchaseOrder.supplier?.code ?: "").toUpperCase().contains("RMS")) ? "EXT" : "PO"
        return prefix + "-" + purchaseOrder.purchaseOrderId.toString().padLeft(3, '0')
    }
    def orderStatus = { purchaseOrder ->
        purchaseOrder?.dispatchStatus ?: purchaseOrder?.approvalStatus ?: "APPROVED"
    }
    def statusClass = { value ->
        def key = (value ?: "").toUpperCase()
        if (key.contains("FETCHED")) {
            return "receive-badge-warning"
        }
        if (key.contains("PARTIAL")) {
            return "receive-badge-warning"
        }
        if (key.contains("APPROVED") || key.contains("SUBMITTED")) {
            return "receive-badge-info"
        }
        if (key.contains("DELIVERED") || key.contains("RECEIVED")) {
            return "receive-badge-success"
        }
        return "receive-badge-neutral"
    }
    def summarizeQuantity = { purchaseOrder ->
        if (!purchaseOrder?.items) {
            return 0
        }
        def total = 0G
        purchaseOrder.items.each { item ->
            if (item?.quantity != null) {
                total += item.quantity
            }
        }
        return total
    }
    def unitFromName = { name ->
        if (!name) {
            return "item"
        }
        def lowered = name.toLowerCase()
        if (lowered.contains("tablet")) return "tablet"
        if (lowered.contains("capsule")) return "capsule"
        if (lowered.contains("sachet")) return "sachet"
        if (lowered.contains("vial")) return "vial"
        if (lowered.contains("bottle")) return "bottle"
        if (lowered.contains("kit")) return "kit"
        return "item"
    }
%>
<style>
    .receive-shell {
        color: #25313c;
    }
    .receive-body-head {
        margin: 0 0 12px;
    }
    .receive-subtitle {
        margin: 0;
        color: #999;
        font-size: 12px;
    }
    .receive-flash {
        margin-bottom: 14px;
        padding: 10px 12px;
        border-radius: 4px;
        font-size: 12px;
        border: 1px solid transparent;
    }
    .receive-flash-info {
        background: #dff3fb;
        border-color: #b8e4f6;
        color: #1f6b84;
    }
    .receive-flash-success {
        background: #def4e6;
        border-color: #c5e6d1;
        color: #226345;
    }
    .receive-flash-error {
        background: #f7dfdf;
        border-color: #efc2c2;
        color: #973b3b;
    }
    .receive-card-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 14px;
    }
    .receive-entry-card {
        display: block;
        text-decoration: none;
        color: inherit;
        background: #fff;
        border: 1px solid #d8e0e7;
        border-radius: 4px;
        padding: 22px 18px 18px;
        text-align: center;
        min-height: 142px;
    }
    .receive-entry-icon {
        width: 48px;
        height: 48px;
        margin: 0 auto 14px;
        border-radius: 999px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        font-weight: 700;
    }
    .receive-entry-title {
        font-size: 15px;
        font-weight: 600;
        margin-bottom: 6px;
    }
    .receive-entry-copy {
        font-size: 12px;
        line-height: 1.45;
        color: #74818f;
        margin-bottom: 10px;
    }
    .receive-entry-meta {
        display: inline-block;
        border-radius: 999px;
        padding: 3px 8px;
        font-size: 10px;
    }
    .receive-icon-blue { background: #e6f0fd; color: #2c76c9; }
    .receive-icon-amber { background: #fff0de; color: #ea9a20; }
    .receive-icon-green { background: #e0f7e8; color: #1fb456; }
    .receive-meta-blue { background: #edf5ff; color: #2c76c9; }
    .receive-meta-amber { background: #fff4e6; color: #ea8b13; }
    .receive-meta-green { background: #ebf9ef; color: #22a553; }
    .receive-toolbar {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: center;
        margin-bottom: 10px;
        flex-wrap: wrap;
    }
    .receive-back {
        display: inline-block;
        padding: 4px 10px;
        border: 1px solid #cfd9e2;
        border-radius: 3px;
        background: #fff;
        color: #334250;
        text-decoration: none;
        font-size: 12px;
    }
    .receive-toolbar-copy {
        color: #94a2af;
        font-size: 12px;
    }
    .receive-panel {
        background: #fff;
        border: 1px solid #d8e0e7;
        border-radius: 4px;
        overflow: hidden;
        margin-bottom: 16px;
    }
    .receive-panel-header {
        padding: 12px 14px;
        background: #fff;
        border-bottom: 1px solid #dfe6ec;
        font-weight: 600;
        font-size: 14px;
    }
    .receive-order-list {
        list-style: none;
        margin: 0;
        padding: 0;
    }
    .receive-order-item {
        border-top: 1px solid #edf1f4;
    }
    .receive-order-item:first-child {
        border-top: none;
    }
    .receive-order-link {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: center;
        padding: 14px;
        text-decoration: none;
        color: inherit;
    }
    .receive-order-primary {
        font-size: 14px;
        font-weight: 600;
        color: #1b6dc3;
        margin-bottom: 4px;
    }
    .receive-order-copy {
        font-size: 12px;
        color: #657482;
        margin-bottom: 2px;
    }
    .receive-badge {
        display: inline-block;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 10px;
        font-weight: 600;
        margin-left: 8px;
    }
    .receive-badge-info { background: #d9f1fb; color: #247cab; }
    .receive-badge-warning { background: #fff2cc; color: #a67b00; }
    .receive-badge-success { background: #dbf4df; color: #23884b; }
    .receive-badge-neutral { background: #edf1f4; color: #607180; }
    .receive-order-meta {
        min-width: 220px;
        text-align: right;
        font-size: 12px;
        color: #5a6876;
    }
    .receive-order-arrow {
        color: #b1bdc9;
        margin-left: 8px;
        font-weight: 700;
    }
    .receive-search-box {
        width: 100%;
        box-sizing: border-box;
        padding: 9px 10px;
        border: 1px solid #d8e0e7;
        border-radius: 4px;
        font-size: 12px;
        color: #334250;
        margin-bottom: 12px;
    }
    .receive-upload-panel {
        background: #fff;
        border: 1px solid #d8e0e7;
        border-radius: 4px;
        overflow: hidden;
        margin-bottom: 14px;
    }
    .receive-upload-header {
        padding: 10px 14px;
        background: #dff3fb;
        border-bottom: 1px solid #bfe5f3;
        color: #116d90;
        font-weight: 600;
    }
    .receive-upload-body {
        padding: 14px;
    }
    .receive-upload-note {
        margin-bottom: 10px;
        padding: 10px 12px;
        background: #cfeaf5;
        border: 1px solid #b9dce9;
        border-radius: 4px;
        color: #146d8f;
        font-size: 12px;
    }
    .receive-upload-drop {
        border: 2px dashed #d8dee5;
        border-radius: 4px;
        padding: 20px 18px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;
    }
    .receive-upload-center {
        flex: 1 1 320px;
        text-align: center;
        color: #9aa7b3;
        font-size: 12px;
    }
    .receive-download-link {
        display: inline-block;
        padding: 8px 14px;
        border: 1px solid #cfd9e2;
        border-radius: 4px;
        color: #334250;
        text-decoration: none;
        background: #fff;
        font-size: 12px;
    }
    .receive-download-caption {
        margin-top: 4px;
        color: #a1aeb9;
        font-size: 11px;
        text-align: center;
    }
    .receive-manual-panel {
        background: #fff;
        border: 1px solid #d8e0e7;
        border-radius: 4px;
        overflow: visible;
    }
    .receive-manual-header {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: center;
        background: #1f73b7;
        color: #fff;
        padding: 10px 14px;
        font-weight: 600;
    }
    .receive-add-btn,
    .receive-confirm-btn,
    .receive-cancel-btn,
    .receive-remove-btn {
        border: 1px solid transparent;
        border-radius: 3px;
        cursor: pointer;
        font-size: 12px;
        text-decoration: none;
    }
    .receive-add-btn {
        padding: 6px 10px;
        background: rgba(255, 255, 255, 0.18);
        color: #fff;
        border-color: rgba(255, 255, 255, 0.2);
    }
    .receive-manual-body {
        position: relative;
        z-index: 20;
        overflow-x: auto;
        overflow-y: visible;
    }
    .receive-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 12px;
        min-width: 980px;
    }
    #receive-order-table {
        min-width: 1700px;
    }
    .receive-table th,
    .receive-table td {
        border: 1px solid #d8e0e7;
        padding: 8px 10px;
        text-align: left;
        vertical-align: middle;
        position: relative;
        overflow: visible;
    }
    .receive-table th {
        background: #f7fafc;
        color: #556575;
        font-size: 11px;
        white-space: nowrap;
    }
    .receive-table td input,
    .receive-table td select {
        width: 100%;
        box-sizing: border-box;
        padding: 6px 8px;
        border: 1px solid #c9d4df;
        border-radius: 3px;
        font-size: 12px;
        font-family: inherit;
    }
    .receive-table td input[readonly] {
        background: #f7f9fb;
        color: #4f5d6a;
    }
    .receive-code-field {
        min-width: 140px;
    }
    .receive-purchase-price-field {
        min-width: 140px;
    }
    .receive-batch-field,
    .receive-expiry-field,
    .receive-qty-field,
    .receive-price-field {
        min-width: 140px;
    }
    .receive-item-field {
        min-width: 270px;
    }
    .receive-line-field,
    .receive-unit-field {
        min-width: 90px;
    }
    .receive-accepted-field,
    .receive-rejected-field {
        min-width: 120px;
    }
    .receive-reason-field {
        min-width: 280px;
    }
    .receive-notes-field {
        min-width: 220px;
    }
    .receive-reason-field select {
        margin-bottom: 6px;
    }
    .receive-reason-field input,
    .receive-notes-field input {
        min-width: 200px;
    }
    .receive-remove-btn {
        width: 36px;
        height: 30px;
        background: #ff3d57;
        color: #fff;
        border-color: #ff3d57;
        font-weight: 700;
    }
    .receive-row-add {
        position: relative;
        z-index: 1;
        padding: 10px 12px;
        border-top: 1px solid #d8e0e7;
        background: #fff;
    }
    .receive-row-add button {
        padding: 6px 10px;
        border: 1px solid #cfd9e2;
        border-radius: 3px;
        background: #fff;
        color: #334250;
        cursor: pointer;
        font-size: 12px;
    }
    .receive-footer-bar {
        position: relative;
        z-index: 1;
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: center;
        padding: 14px;
        flex-wrap: wrap;
    }
    .receive-footer-field {
        min-width: 240px;
    }
    .receive-footer-field label {
        display: block;
        margin-bottom: 4px;
        font-size: 12px;
        color: #556575;
    }
    .receive-footer-field input {
        width: 100%;
        box-sizing: border-box;
        padding: 7px 9px;
        border: 1px solid #c9d4df;
        border-radius: 3px;
        font-size: 12px;
    }
    .receive-footer-actions {
        display: flex;
        gap: 10px;
        align-items: center;
        flex-wrap: wrap;
    }
    .receive-cancel-btn {
        padding: 8px 18px;
        border-color: #cfd9e2;
        background: #fff;
        color: #334250;
    }
    .receive-confirm-btn {
        padding: 8px 18px;
        background: #18bf5d;
        border-color: #18bf5d;
        color: #fff;
        font-weight: 600;
    }
    .receive-summary-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 12px;
        padding: 12px 14px;
        border-top: 1px solid #edf1f4;
    }
    .receive-summary-card {
        min-height: 52px;
    }
    .receive-summary-label {
        font-size: 11px;
        color: #a0aab5;
        text-transform: uppercase;
        margin-bottom: 4px;
    }
    .receive-summary-value {
        font-size: 14px;
        font-weight: 600;
        color: #22303c;
    }
    .receive-receipt-header {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: center;
        padding: 10px 14px;
        background: #1f73b7;
        color: #fff;
        font-weight: 600;
        flex-wrap: wrap;
    }
    .receive-receipt-tip {
        font-size: 12px;
        color: #d8ebfb;
        font-weight: 400;
    }
    .receive-unit-pill {
        display: inline-block;
        padding: 3px 8px;
        border-radius: 999px;
        background: #eef4ff;
        color: #4a76cf;
        font-size: 10px;
        font-weight: 600;
    }
    .receive-variance {
        text-align: center;
        font-weight: 700;
        color: #1a9b47;
    }
    .receive-variance.is-different {
        color: #d99214;
    }
    .receive-combobox {
        position: relative;
        min-width: 280px;
        z-index: 5;
    }
    .receive-suggestions {
        position: fixed;
        max-height: 220px;
        overflow-y: auto;
        background: #fff;
        border: 1px solid #cfd9e2;
        border-radius: 3px;
        box-shadow: 0 4px 12px rgba(16, 34, 49, 0.12);
        display: none;
        z-index: 2000;
        min-width: 280px;
    }
    .direct-line-row {
        background: #fff;
    }
    .direct-line-row td {
        background: #fff;
    }
    .receive-suggestions.active {
        display: block;
    }
    .receive-suggestion {
        padding: 7px 8px;
        border-bottom: 1px solid #edf1f4;
        cursor: pointer;
    }
    .receive-suggestion:last-child {
        border-bottom: none;
    }
    .receive-suggestion:hover,
    .receive-suggestion.is-active {
        background: #eef6fc;
    }
    .receive-item-search.is-invalid,
    .receive-combobox.is-invalid .receive-item-search {
        border-color: #d9534f;
        background: #fff6f6;
    }
    .receive-validation-note {
        margin-top: 4px;
        color: #b44343;
        font-size: 11px;
        display: none;
    }
    .receive-combobox.is-invalid .receive-validation-note {
        display: block;
    }
    .receive-empty-state {
        padding: 18px 14px;
        color: #7d8b98;
        font-size: 12px;
    }
    @media (max-width: 1100px) {
        .receive-card-grid {
            grid-template-columns: 1fr;
        }
        .receive-summary-grid {
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }
    }
    @media (max-width: 700px) {
        .receive-order-link {
            flex-direction: column;
            align-items: flex-start;
        }
        .receive-order-meta {
            min-width: 0;
            text-align: left;
        }
        .receive-summary-grid {
            grid-template-columns: 1fr;
        }
    }
</style>

<div class="receive-shell">
    <% if (errorMessage) { %>
    <div class="receive-flash receive-flash-error">${ errorMessage }</div>
    <% } %>
    <% if (received || uploadReceived) { %>
    <div class="receive-flash receive-flash-success">
        Stock receipt saved successfully.
        <% if (deliveryReceiptId) { %>
        <a href="${ receiptPdfUrl }" style="margin-left:12px;color:#fff;text-decoration:underline;">Download proof of receipt</a>
        <% } %>
    </div>
    <% } %>

    <% if (selectedView == "landing") { %>
    <div class="receive-card-grid">
        <a class="receive-entry-card" href="${ rmsLink }">
            <div class="receive-entry-icon receive-icon-blue">R</div>
            <div class="receive-entry-title">Receive from RMS</div>
            <div class="receive-entry-copy">Receive items against a purchase order sent to a supplier integrated through the RMS API flow.</div>
            <div class="receive-entry-meta receive-meta-blue">${ rmsPurchaseOrderCount } open orders</div>
        </a>
        <a class="receive-entry-card" href="${ supplierLink }">
            <div class="receive-entry-icon receive-icon-amber">S</div>
            <div class="receive-entry-title">Receive from other supplier</div>
            <div class="receive-entry-copy">Receive items from LABOPHAR, CAMERWA, or any other approved supplier outside the API-enabled RMS flow.</div>
            <div class="receive-entry-meta receive-meta-amber">${ externalPurchaseOrderCount } open orders</div>
        </a>
        <a class="receive-entry-card" href="${ directLink }">
            <div class="receive-entry-icon receive-icon-green">+</div>
            <div class="receive-entry-title">Add new stock directly</div>
            <div class="receive-entry-copy">Add items to stock manually or upload a spreadsheet with multiple items at once.</div>
            <div class="receive-entry-meta receive-meta-green">Manual entry or Excel upload</div>
        </a>
    </div>
    <% } %>

    <% if (selectedView == "rms" || selectedView == "supplier") { %>
    <div class="receive-body-head">
        <p class="receive-subtitle">${ selectedView == "rms" ? "Select a purchase order to begin receiving against it" : "Select a purchase order to receive against" }</p>
    </div>
    <div class="receive-toolbar">
        <a class="receive-back" href="${ landingLink }">&lsaquo; Back</a>
        <div class="receive-toolbar-copy">${ selectedView == "rms" ? rmsPurchaseOrderCount + " purchase orders sent to RMS" : externalPurchaseOrderCount + " orders from external suppliers" }</div>
    </div>
    <div class="receive-panel">
        <div class="receive-panel-header">${ selectedView == "rms" ? "RMS purchase orders — open" : "External supplier orders — open" }</div>
        <div style="padding:12px 14px 0;">
            <input type="text"
                   id="receive-order-search"
                   class="receive-search-box"
                   placeholder="${ selectedView == 'rms' ? 'Search by PO, RMS order number, supplier, program, or date' : 'Search by PO, supplier, program, or date' }"
                   oninput="filterReceiveOrders(this.value)"/>
        </div>
        <ul class="receive-order-list">
            <% def orderList = selectedView == "rms" ? rmsPurchaseOrders : externalPurchaseOrders %>
            <% if (!orderList) { %>
            <li class="receive-empty-state">No approved purchase orders are ready for receipt.</li>
            <% } %>
            <% orderList.each { purchaseOrder -> %>
            <li class="receive-order-item receive-order-search-item"
                data-search-text="${ (orderReference(purchaseOrder) + ' ' + rmsOrderNumber(purchaseOrder) + ' ' + (purchaseOrder.supplier?.name ?: '') + ' ' + (purchaseOrder.program ?: '') + ' ' + formatShortDate(purchaseOrder.requestedDate)).toLowerCase().replace('"', '&quot;') }">
                <a class="receive-order-link" href="${ ui.pageLink('rwandapharmacymanagement', 'supplier/deliveryReceive', [view: 'receive', source: selectedView, purchaseOrderId: purchaseOrder.purchaseOrderId]) }">
                    <div>
                        <div class="receive-order-primary">
                            ${ orderReference(purchaseOrder) }
                            <span class="receive-badge ${ statusClass(orderStatus(purchaseOrder)) }">${ orderStatus(purchaseOrder) }</span>
                        </div>
                        <% if (selectedView == "rms") { %>
                        <div class="receive-order-copy">RMS order number: <strong>${ rmsOrderNumber(purchaseOrder) ?: 'Not available' }</strong></div>
                        <% if (purchaseOrder.fetchedReceiptDate) { %>
                        <div class="receive-order-copy">Fetched from RMS on <strong>${ formatShortDate(purchaseOrder.fetchedReceiptDate) }</strong> and waiting for approval.</div>
                        <% } %>
                        <% } %>
                        <div class="receive-order-copy">${ purchaseOrder.items?.size() ?: 0 } line(s) &middot; ${ summarizeQuantity(purchaseOrder) } units requested &middot; ${ purchaseOrder.supplier?.name ?: '' }</div>
                        <div class="receive-order-copy">${ purchaseOrder.program ?: '' }</div>
                    </div>
                    <div class="receive-order-meta">
                        Ordered: <strong>${ formatShortDate(purchaseOrder.requestedDate) }</strong>
                        <span class="receive-order-arrow">&rsaquo;</span>
                    </div>
                </a>
            </li>
            <% } %>
        </ul>
    </div>
    <% } %>

    <% if (selectedView == "direct") { %>
    <div class="receive-toolbar">
        <a class="receive-back" href="${ landingLink }">&lsaquo; Back</a>
    </div>

    <div class="receive-upload-panel">
        <div class="receive-upload-header">Upload spreadsheet (optional shortcut)</div>
        <div class="receive-upload-body">
            <form method="post" enctype="multipart/form-data">
                <input type="hidden" name="action" value="uploadDirectReceipt"/>
                <input type="hidden" name="view" value="direct"/>
                <input type="hidden" name="pharmacyLocationId" value="${ selectedLocationId }"/>
                <div class="receive-upload-drop">
                    <div class="receive-upload-center">
                        <div style="font-size:28px;line-height:1;margin-bottom:6px;">[]</div>
                        <div>Click to upload or drag and drop</div>
                        <div style="margin-top:4px;">Download the CSV template, complete it, then upload the same CSV or an `.xls` file</div>
                        <div style="margin-top:12px;"><input type="file" name="receiptFile" accept=".csv,.xls" required="required"/></div>
                    </div>
                    <div style="text-align:center;">
                        <a class="receive-download-link" href="${ templateUrl }">Download template</a>
                        <div class="receive-download-caption">CSV template download</div>
                    </div>
                </div>
                <div class="receive-footer-bar" style="padding:14px 0 0;">
                    <div class="receive-footer-field">
                        <label>Delivery reference / GRN (optional)</label>
                        <input type="text" name="receiptNumber" placeholder="e.g. GRN-2025-041"/>
                    </div>
                    <div class="receive-footer-actions">
                        <button type="submit" class="receive-confirm-btn">Confirm upload &amp; add to stock</button>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <form method="post" id="direct-stock-form">
        <input type="hidden" name="action" value="receiveDirectManual"/>
        <input type="hidden" name="view" value="direct"/>
        <input type="hidden" name="pharmacyLocationId" value="${ selectedLocationId }"/>
        <div class="receive-manual-panel">
            <div class="receive-manual-header">
                <span>Manual stock entry</span>
                <button type="button" class="receive-add-btn" onclick="addDirectRow()">+ Add row</button>
            </div>
            <div class="receive-manual-body">
                <table class="receive-table">
                    <thead>
                    <tr>
                        <th class="receive-item-field">Item name</th>
                        <th class="receive-code-field">Item code</th>
                        <th class="receive-line-field">Program</th>
                        <th class="receive-batch-field">Batch number</th>
                        <th class="receive-expiry-field">Expiry date / N/A</th>
                        <th class="receive-price-field">Receiving unit price</th>
                        <th class="receive-qty-field">Qty</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody id="direct-line-body">
                    <tr class="direct-line-row">
                        <td class="receive-item-field">
                            <div class="receive-combobox">
                                <input type="text" class="receive-item-search" placeholder="Search item name or code" oninput="searchDirectItems(this)" onfocus="searchDirectItems(this)" onkeydown="handleDirectSuggestionKeydown(event, this)" autocomplete="off"/>
                                <input type="hidden" name="productMasterItemIds"/>
                                <input type="hidden" name="itemIds"/>
                                <input type="hidden" name="itemCodes"/>
                                <input type="hidden" name="programs"/>
                                <div class="receive-suggestions"></div>
                                <div class="receive-validation-note">Select an item from the dropdown list.</div>
                            </div>
                        </td>
                        <td class="receive-code-field"><input type="text" class="receive-item-code" readonly="readonly" placeholder="Item code"/></td>
                        <td class="receive-line-field">
                            <select class="receive-item-program" onchange="handleDirectProgramChange(this)">
                                <option value="">All programs</option>
                                <% productMasterPrograms.each { program -> %>
                                <option value="${ program }">${ program }</option>
                                <% } %>
                            </select>
                        </td>
                        <td class="receive-batch-field"><input type="text" name="batchNumbers" placeholder="e.g. B2025-001"/></td>
                        <td class="receive-expiry-field"><input type="text" name="expiryDates" placeholder="yyyy-MM-dd or N/A"/></td>
                        <td class="receive-price-field"><input type="number" step="0.01" min="0" name="unitPrices"/></td>
                        <td class="receive-qty-field"><input type="number" step="0.01" min="0.01" name="quantities" value="0"/></td>
                        <td><button type="button" class="receive-remove-btn" onclick="removeDirectRow(this)">x</button></td>
                    </tr>
                    </tbody>
                </table>
            </div>
            <div class="receive-row-add">
                <button type="button" onclick="addDirectRow()">+ Add another row</button>
            </div>
            <div class="receive-footer-bar">
                <div class="receive-footer-field">
                    <label>Delivery reference / GRN (optional)</label>
                    <input type="text" name="receiptNumber" placeholder="e.g. GRN-2025-041"/>
                </div>
                <div class="receive-footer-actions">
                    <a class="receive-cancel-btn" href="${ landingLink }">Clear all rows</a>
                    <button type="submit" class="receive-confirm-btn">Confirm &amp; add to stock</button>
                </div>
            </div>
        </div>
    </form>
    <% } %>

    <% if (selectedView == "receive" && selectedPurchaseOrder) { %>
    <div class="receive-body-head">
        <p class="receive-subtitle">${ selectedPurchaseOrder.supplier?.name ?: '' } &mdash; verify quantities and confirm receipt</p>
    </div>
    <div class="receive-toolbar">
        <a class="receive-back" href="${ ui.pageLink('rwandapharmacymanagement', 'supplier/deliveryReceive', [view: selectedSource]) }">&lsaquo; Back to orders</a>
    </div>

    <div class="receive-panel">
        <div class="receive-panel-header">Order summary</div>
        <div class="receive-summary-grid">
            <div class="receive-summary-card">
                <div class="receive-summary-label">Order reference</div>
                <div class="receive-summary-value">${ orderReference(selectedPurchaseOrder) }</div>
            </div>
            <div class="receive-summary-card">
                <div class="receive-summary-label">Supplier</div>
                <div class="receive-summary-value">${ selectedPurchaseOrder.supplier?.name ?: '' }</div>
            </div>
            <div class="receive-summary-card">
                <div class="receive-summary-label">Order date</div>
                <div class="receive-summary-value">${ formatShortDate(selectedPurchaseOrder.requestedDate) }</div>
            </div>
            <div class="receive-summary-card">
                <div class="receive-summary-label">Status</div>
                <div class="receive-summary-value"><span class="receive-badge ${ statusClass(orderStatus(selectedPurchaseOrder)) }">${ orderStatus(selectedPurchaseOrder) }</span></div>
            </div>
        </div>
    </div>

    <% if (selectedSource == "rms") { %>
    <div class="receive-upload-panel">
        <div class="receive-upload-header">Load delivery details from RMS</div>
        <div class="receive-upload-body">
            <div class="receive-upload-note">
                Click fetch to preload the receipt form using the delivery details returned by RMS for this purchase order.
            </div>
            <form method="get">
                <input type="hidden" name="view" value="receive"/>
                <input type="hidden" name="source" value="${ selectedSource }"/>
                <input type="hidden" name="purchaseOrderId" value="${ selectedPurchaseOrder.purchaseOrderId }"/>
                <input type="hidden" name="fetchRms" value="true"/>
                <div class="receive-footer-bar" style="padding:0;">
                    <div class="receive-footer-field">
                        <label>RMS order number</label>
                        <input type="text" value="${ selectedPurchaseOrder.items ? (selectedPurchaseOrder.items[0]?.orderNumber ?: '') : '' }" readonly="readonly"/>
                    </div>
                    <div class="receive-footer-actions">
                        <button type="submit" class="receive-confirm-btn">Fetch from RMS</button>
                    </div>
                </div>
            </form>
            <% if (fetchedReceipt) { %>
            <div class="receive-upload-note" style="margin-top:12px;background:#def4e6;border-color:#c5e6d1;color:#226345;">
                RMS delivery loaded: ${ fetchedReceipt.deliveryId ?: 'No delivery reference' }${ fetchedReceipt.deliveryDate ? ' on ' + fetchedReceipt.deliveryDate : '' }.
            </div>
            <div class="receive-upload-note" style="margin-top:12px;">
                Confirmation to RMS will be sent automatically after you confirm receipt. Adjust the received quantity, accepted quantity, and any rejected-item reason before submitting.
            </div>
            <% } %>
        </div>
    </div>
    <% } %>

    <% if (selectedSource == "supplier") { %>
    <div class="receive-upload-panel">
        <div class="receive-upload-header">Upload supplier-filled spreadsheet</div>
        <div class="receive-upload-body">
            <div class="receive-upload-note">
                Download the supplier receipt template, let the supplier fill the receiving columns, then upload it here to prefill receiving information for this purchase order.
            </div>
            <form method="post" enctype="multipart/form-data">
                <input type="hidden" name="action" value="uploadReceipt"/>
                <input type="hidden" name="view" value="receive"/>
                <input type="hidden" name="source" value="${ selectedSource }"/>
                <input type="hidden" name="purchaseOrderId" value="${ selectedPurchaseOrder.purchaseOrderId }"/>
                <input type="hidden" name="pharmacyLocationId" value="${ selectedLocationId }"/>
                <div class="receive-upload-drop">
                    <div class="receive-upload-center">
                        <div style="font-size:28px;line-height:1;margin-bottom:6px;">[]</div>
                        <div>Upload the completed supplier spreadsheet</div>
                        <div style="margin-top:4px;">Download the CSV template, complete it, then upload the same CSV or an `.xls` file</div>
                        <div style="margin-top:12px;"><input type="file" name="receiptFile" accept=".csv,.xls" required="required"/></div>
                    </div>
                    <div style="text-align:center;">
                        <a class="receive-download-link" href="${ ui.pageLink('rwandapharmacymanagement', 'supplier/deliveryReceiptTemplate', [purchaseOrderId: selectedPurchaseOrder.purchaseOrderId]) }">Download supplier template</a>
                        <div class="receive-download-caption">CSV with prefilled requested items</div>
                    </div>
                </div>
                <div class="receive-footer-bar" style="padding:14px 0 0;">
                    <div class="receive-footer-field">
                        <label>Delivery reference / GRN (optional)</label>
                        <input type="text" name="receiptNumber" placeholder="e.g. GRN-2025-041"/>
                    </div>
                    <div class="receive-footer-actions">
                        <button type="submit" class="receive-confirm-btn">Upload &amp; receive stock</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
    <% } %>

    <form method="post">
        <input type="hidden" name="action" value="receiveManual"/>
        <input type="hidden" name="view" value="receive"/>
        <input type="hidden" name="source" value="${ selectedSource }"/>
        <input type="hidden" name="purchaseOrderId" value="${ selectedPurchaseOrder.purchaseOrderId }"/>
        <input type="hidden" name="pharmacyLocationId" value="${ selectedLocationId }"/>
        <input type="hidden" name="deliveryDate" value="${ fetchedDeliveryDate }"/>
        <div class="receive-manual-panel">
            <div class="receive-receipt-header">
                <span>Receive lines &mdash; ${ fetchedReceipt ? 'confirm RMS delivery details and receive stock' : 'enter batch, expiry and confirm received quantities' }</span>
                <span class="receive-receipt-tip">Qty in blue = editable if delivery differs from order.</span>
            </div>
            <div class="receive-manual-body">
                <table class="receive-table" id="receive-order-table">
                    <thead>
                    <tr>
                        <th class="receive-item-field">Item</th>
                        <th class="receive-code-field">Item code</th>
                        <th class="receive-line-field">Program</th>
                        <th class="receive-line-field">Line no</th>
                        <th class="receive-unit-field">Unit</th>
                        <th class="receive-purchase-price-field">Purchase unit price</th>
                        <th class="receive-batch-field">Batch number</th>
                        <th class="receive-expiry-field">Expiry date / N/A</th>
                        <th class="receive-qty-field">Qty ordered</th>
                        <th class="receive-price-field">Receiving unit price</th>
                        <th class="receive-qty-field">Qty received</th>
                        <th class="receive-accepted-field">Qty accepted</th>
                        <th class="receive-rejected-field">Qty rejected</th>
                        <th class="receive-reason-field">Reject reason</th>
                        <th class="receive-notes-field">Notes</th>
                        <th class="receive-qty-field">Variance</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% receiveRows.each { row -> %>
                    <tr>
                        <td class="receive-item-field">
                            <input type="hidden" name="productMasterItemIds" value="${ row.productMasterItemId ?: '' }"/>
                            <input type="hidden" name="itemIds" value="${ row.itemId ?: '' }"/>
                            <input type="hidden" name="itemCodes" value="${ row.itemCode ?: '' }"/>
                            <input type="hidden" name="programs" value="${ row.program ?: '' }"/>
                            <input type="text" value="${ row.itemName ?: '' }" readonly="readonly"/>
                        </td>
                        <td class="receive-code-field"><input type="text" value="${ row.itemCode ?: '' }" readonly="readonly"/></td>
                        <td class="receive-line-field"><input type="text" value="${ row.program ?: '' }" readonly="readonly"/></td>
                        <td class="receive-line-field">
                            <input type="hidden" name="confirmationItemNos" value="${ row.confirmationItemNo ?: '' }"/>
                            <input type="text" value="${ row.confirmationItemNo ?: '' }" readonly="readonly"/>
                        </td>
                        <td class="receive-unit-field"><span class="receive-unit-pill">${ row.unit ?: unitFromName(row.itemName) }</span></td>
                        <td class="receive-purchase-price-field"><input type="text" value="${ row.purchaseUnitPrice ?: '' }" readonly="readonly"/></td>
                        <td class="receive-batch-field"><input type="text" name="batchNumbers" placeholder="e.g. B2025-001" value="${ row.batchNumber ?: '' }"/></td>
                        <td class="receive-expiry-field"><input type="text" name="expiryDates" value="${ row.expiryDate ?: '' }" placeholder="yyyy-MM-dd or N/A"/></td>
                        <td class="receive-qty-field">
                            <input type="hidden" name="orderedQuantities" value="${ row.orderedQuantity ?: '' }"/>
                            <input type="text" value="${ row.orderedQuantity ?: '' }" readonly="readonly" class="ordered-quantity"/>
                        </td>
                        <td class="receive-price-field"><input type="number" step="0.01" min="0" name="unitPrices" value="${ row.receivingUnitPrice ?: '' }"/></td>
                        <td class="receive-qty-field">
                            <input type="number" step="0.01" min="0" name="quantities" value="${ row.receivedQuantity ?: row.orderedQuantity ?: '' }" class="received-quantity" oninput="updateReceiveRow(this)"/>
                        </td>
                        <td class="receive-accepted-field">
                            <input type="number" step="0.01" min="0" name="acceptedQuantities" value="${ row.receivedQuantity ?: row.orderedQuantity ?: '' }" class="accepted-quantity" oninput="updateReceiveRow(this)"/>
                        </td>
                        <td class="receive-rejected-field"><input type="text" class="rejected-quantity" value="0" readonly="readonly"/></td>
                        <td class="receive-reason-field">
                            <select name="rejectionReasonCodes" class="rejection-reason-code" onchange="updateReceiveRow(this)" disabled="disabled">
                                <option value="">Select</option>
                                <option value="DAMAGED">Damaged</option>
                                <option value="EXPIRED">Expired</option>
                                <option value="SHORT_DATED">Short dated</option>
                                <option value="WRONG_ITEM">Wrong item</option>
                                <option value="OTHER">Other</option>
                            </select>
                            <input type="text" name="rejectionReasonDetails" class="rejection-reason-detail" placeholder="Why rejected?" disabled="disabled"/>
                        </td>
                        <td class="receive-notes-field"><input type="text" name="lineNotes" placeholder="Optional note"/></td>
                        <td class="receive-qty-field receive-variance">&#10003;</td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
            <div class="receive-footer-bar">
                <div class="receive-footer-field">
                    <label>Delivery reference / GRN (optional)</label>
                    <input type="text" name="receiptNumber" placeholder="e.g. GRN-2025-041" value="${ fetchedReceiptNumber }"/>
                </div>
                <div class="receive-footer-actions">
                    <a class="receive-cancel-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'supplier/deliveryReceive', [view: selectedSource]) }">Cancel</a>
                    <button type="submit" class="receive-confirm-btn">Confirm receipt &amp; update stock</button>
                </div>
            </div>
        </div>
    </form>
    <% } %>
</div>

<template id="direct-line-template">
    <tr class="direct-line-row">
        <td class="receive-item-field">
            <div class="receive-combobox">
                <input type="text" class="receive-item-search" placeholder="Search item name or code" oninput="searchDirectItems(this)" onfocus="searchDirectItems(this)" onkeydown="handleDirectSuggestionKeydown(event, this)" autocomplete="off"/>
                <input type="hidden" name="productMasterItemIds"/>
                <input type="hidden" name="itemIds"/>
                <input type="hidden" name="itemCodes"/>
                <input type="hidden" name="programs"/>
                <div class="receive-suggestions"></div>
                <div class="receive-validation-note">Select an item from the dropdown list.</div>
            </div>
        </td>
        <td class="receive-code-field"><input type="text" class="receive-item-code" readonly="readonly" placeholder="Item code"/></td>
        <td class="receive-line-field">
            <select class="receive-item-program" onchange="handleDirectProgramChange(this)">
                <option value="">All programs</option>
                <% productMasterPrograms.each { program -> %>
                <option value="${ program }">${ program }</option>
                <% } %>
            </select>
        </td>
        <td class="receive-batch-field"><input type="text" name="batchNumbers" placeholder="e.g. B2025-001"/></td>
        <td class="receive-expiry-field"><input type="text" name="expiryDates" placeholder="yyyy-MM-dd or N/A"/></td>
        <td class="receive-price-field"><input type="number" step="0.01" min="0" name="unitPrices"/></td>
        <td class="receive-qty-field"><input type="number" step="0.01" min="0.01" name="quantities" value="0"/></td>
        <td><button type="button" class="receive-remove-btn" onclick="removeDirectRow(this)">x</button></td>
    </tr>
</template>

<script type="text/javascript">
    var directProductMasterItems = [
        <% productMasterItems.eachWithIndex { item, idx -> %>
        { id: '${ item.productMasterItemId ?: '' }', code: '${ (item.npcCode ?: '').replace("\\", "\\\\").replace("'", "\\'") }', program: '${ (item.program ?: '').replace("\\", "\\\\").replace("'", "\\'") }', display: '${ ((item.itemName ?: '') + " (" + (item.npcCode ?: '') + ")" + (item.program ? " - " + item.program : "")).replace("\\", "\\\\").replace("'", "\\'") }', itemName: '${ (item.itemName ?: '').replace("\\", "\\\\").replace("'", "\\'") }' }${ idx < productMasterItems.size() - 1 ? ',' : '' }
        <% } %>
    ];

    function addDirectRow() {
        var body = document.getElementById('direct-line-body');
        var template = document.getElementById('direct-line-template');
        if (!body || !template) {
            return;
        }
        if (template.content && template.content.firstElementChild) {
            var clone = template.content.firstElementChild.cloneNode(true);
            body.appendChild(clone);
            if (typeof initializeOpenmrsDateFields === 'function') {
                initializeOpenmrsDateFields(clone);
            }
            return;
        }
        body.insertAdjacentHTML('beforeend', template.innerHTML);
        if (typeof initializeOpenmrsDateFields === 'function' && body.lastElementChild) {
            initializeOpenmrsDateFields(body.lastElementChild);
        }
    }

    function filterReceiveOrders(value) {
        var query = value ? value.toLowerCase().trim() : '';
        var rows = document.querySelectorAll('.receive-order-search-item');
        for (var i = 0; i < rows.length; i++) {
            var haystack = rows[i].getAttribute('data-search-text') || '';
            rows[i].style.display = !query || haystack.indexOf(query) !== -1 ? '' : 'none';
        }
    }

    function removeDirectRow(button) {
        var body = document.getElementById('direct-line-body');
        var row = button ? button.closest('.direct-line-row') : null;
        if (!body || !row) {
            return;
        }
        if (body.children.length === 1) {
            clearDirectRow(row);
            return;
        }
        row.remove();
    }

    function clearDirectRow(row) {
        var inputs = row.querySelectorAll('input');
        for (var i = 0; i < inputs.length; i++) {
            if (inputs[i].type === 'hidden') {
                inputs[i].value = '';
            } else if (inputs[i].classList.contains('receive-item-code')) {
                inputs[i].value = '';
            } else if (inputs[i].name === 'quantities') {
                inputs[i].value = '0';
            } else {
                inputs[i].value = '';
            }
        }
        var programField = row.querySelector('.receive-item-program');
        if (programField) {
            programField.value = '';
        }
        var itemSearch = row.querySelector('.receive-item-search');
        if (itemSearch) {
            itemSearch.value = '';
        }
        var suggestions = row.querySelector('.receive-suggestions');
        if (suggestions) {
            suggestions.innerHTML = '';
            suggestions.classList.remove('active');
        }
        var combo = row.querySelector('.receive-combobox');
        if (combo) {
            combo.classList.remove('is-invalid');
        }
    }

    function validateDirectStockForm() {
        var form = document.getElementById('direct-stock-form');
        if (!form) {
            return true;
        }
        var rows = form.querySelectorAll('.direct-line-row');
        var firstInvalidInput = null;
        for (var i = 0; i < rows.length; i++) {
            var itemInput = rows[i].querySelector('.receive-item-search');
            var hiddenItemCode = rows[i].querySelector('input[name="itemCodes"]');
            var hiddenProductMasterItemId = rows[i].querySelector('input[name="productMasterItemIds"]');
            var batchInput = rows[i].querySelector('input[name="batchNumbers"]');
            var qtyInput = rows[i].querySelector('input[name="quantities"]');
            var hasAnyValue = itemInput && itemInput.value && itemInput.value.trim()
                || batchInput && batchInput.value && batchInput.value.trim()
                || qtyInput && qtyInput.value && parseFloat(qtyInput.value || '0') > 0;
            if (!hasAnyValue) {
                setDirectItemValidity(itemInput, true);
                continue;
            }
            var isValid = hiddenProductMasterItemId && hiddenProductMasterItemId.value && hiddenProductMasterItemId.value.trim()
                && hiddenItemCode && hiddenItemCode.value && hiddenItemCode.value.trim();
            setDirectItemValidity(itemInput, !!isValid);
            if (!isValid && !firstInvalidInput) {
                firstInvalidInput = itemInput;
            }
        }
        if (firstInvalidInput) {
            firstInvalidInput.focus();
            return false;
        }
        return true;
    }

    function searchDirectItems(input) {
        if (!input) {
            return;
        }
        var combo = input.closest('.receive-combobox');
        if (!combo) {
            return;
        }
        var hiddenItemCode = combo.querySelector('input[name="itemCodes"]');
        var hiddenProductMasterItemId = combo.querySelector('input[name="productMasterItemIds"]');
        var hiddenProgram = combo.querySelector('input[name="programs"]');
        var suggestions = combo.querySelector('.receive-suggestions');
        var selectedProgram = getDirectRowProgram(input);
        if (hiddenItemCode) {
            hiddenItemCode.value = '';
        }
        if (hiddenProductMasterItemId) {
            hiddenProductMasterItemId.value = '';
        }
        if (hiddenProgram) {
            hiddenProgram.value = selectedProgram;
        }
        setCodeField(input, '');
        setDirectItemValidity(input, false);
        if (!suggestions) {
            return;
        }
        var query = (input.value || '').trim().toLowerCase();
        suggestions.innerHTML = '';
        if (!query) {
            suggestions.classList.remove('active');
            return;
        }
        var matches = [];
        for (var i = 0; i < directProductMasterItems.length; i++) {
            var item = directProductMasterItems[i];
            if (selectedProgram && (item.program || '') !== selectedProgram) {
                continue;
            }
            if ((item.display || '').toLowerCase().indexOf(query) !== -1
                    || (item.itemName || '').toLowerCase().indexOf(query) !== -1
                    || (item.code || '').toLowerCase().indexOf(query) !== -1
                    || (item.program || '').toLowerCase().indexOf(query) !== -1) {
                matches.push(item);
            }
        }
        if (!matches.length) {
            suggestions.innerHTML = '<div class="receive-suggestion">No matching items found.</div>';
            positionDirectSuggestions(input, suggestions);
            suggestions.classList.add('active');
            return;
        }
        for (var j = 0; j < matches.length; j++) {
            var option = document.createElement('div');
            option.className = 'receive-suggestion';
            option.textContent = matches[j].display;
            option.setAttribute('data-id', matches[j].id);
            option.setAttribute('data-code', matches[j].code);
            option.setAttribute('data-program', matches[j].program);
            option.setAttribute('data-display', matches[j].display);
            option.onmousedown = function(event) {
                event.preventDefault();
                selectDirectItem(input, this);
            };
            suggestions.appendChild(option);
        }
        positionDirectSuggestions(input, suggestions);
        suggestions.classList.add('active');
    }

    function selectDirectItem(input, option) {
        var combo = input.closest('.receive-combobox');
        if (!combo) {
            return;
        }
        var hiddenItemCode = combo.querySelector('input[name="itemCodes"]');
        var hiddenProductMasterItemId = combo.querySelector('input[name="productMasterItemIds"]');
        var hiddenProgram = combo.querySelector('input[name="programs"]');
        var suggestions = combo.querySelector('.receive-suggestions');
        input.value = option.getAttribute('data-display') || '';
        if (hiddenProductMasterItemId) {
            hiddenProductMasterItemId.value = option.getAttribute('data-id') || '';
        }
        if (hiddenItemCode) {
            hiddenItemCode.value = option.getAttribute('data-code') || '';
        }
        if (hiddenProgram) {
            hiddenProgram.value = option.getAttribute('data-program') || '';
        }
        setCodeField(input, option.getAttribute('data-code') || '');
        setProgramField(input, option.getAttribute('data-program') || '');
        setDirectItemValidity(input, true);
        if (suggestions) {
            suggestions.innerHTML = '';
            suggestions.classList.remove('active');
        }
    }

    function positionDirectSuggestions(input, suggestions) {
        if (!input || !suggestions) {
            return;
        }
        var rect = input.getBoundingClientRect();
        suggestions.style.left = rect.left + 'px';
        suggestions.style.top = (rect.bottom + 2) + 'px';
        suggestions.style.width = rect.width + 'px';
    }

    function setCodeField(input, code) {
        var row = input ? input.closest('tr') : null;
        if (!row) {
            return;
        }
        var codeField = row.querySelector('.receive-item-code');
        if (codeField) {
            codeField.value = code || '';
        }
    }

    function setProgramField(input, program) {
        var row = input ? input.closest('tr') : null;
        if (!row) {
            return;
        }
        var programField = row.querySelector('.receive-item-program');
        if (programField) {
            ensureProgramOption(programField, program || '');
            programField.value = program || '';
        }
    }

    function ensureProgramOption(select, program) {
        if (!select || !program) {
            return;
        }
        for (var i = 0; i < select.options.length; i++) {
            if (select.options[i].value === program) {
                return;
            }
        }
        var option = document.createElement('option');
        option.value = program;
        option.textContent = program;
        select.appendChild(option);
    }

    function getDirectRowProgram(input) {
        var row = input ? input.closest('tr') : null;
        var programField = row ? row.querySelector('.receive-item-program') : null;
        return programField ? (programField.value || '') : '';
    }

    function handleDirectProgramChange(select) {
        var row = select ? select.closest('tr') : null;
        if (!row) {
            return;
        }
        var selectedProgram = select.value || '';
        var hiddenProgram = row.querySelector('input[name="programs"]');
        var hiddenItemCode = row.querySelector('input[name="itemCodes"]');
        var hiddenProductMasterItemId = row.querySelector('input[name="productMasterItemIds"]');
        var itemSearch = row.querySelector('.receive-item-search');
        var suggestions = row.querySelector('.receive-suggestions');
        if (hiddenProgram) {
            hiddenProgram.value = selectedProgram;
        }
        if (hiddenItemCode) {
            hiddenItemCode.value = '';
        }
        if (hiddenProductMasterItemId) {
            hiddenProductMasterItemId.value = '';
        }
        setCodeField(itemSearch, '');
        if (itemSearch) {
            itemSearch.value = '';
            setDirectItemValidity(itemSearch, true);
        }
        if (suggestions) {
            suggestions.innerHTML = '';
            suggestions.classList.remove('active');
        }
    }

    function setDirectItemValidity(input, isValid) {
        var combo = input ? input.closest('.receive-combobox') : null;
        if (!combo) {
            return;
        }
        combo.classList.toggle('is-invalid', !isValid && !!(input && input.value && input.value.trim()));
    }

    function handleDirectSuggestionKeydown(event, input) {
        var combo = input.closest('.receive-combobox');
        if (!combo) {
            return;
        }
        var suggestions = combo.querySelector('.receive-suggestions');
        if (!suggestions || !suggestions.classList.contains('active')) {
            return;
        }
        var options = suggestions.querySelectorAll('.receive-suggestion');
        if (!options.length) {
            return;
        }
        var activeIndex = -1;
        for (var i = 0; i < options.length; i++) {
            if (options[i].classList.contains('is-active')) {
                activeIndex = i;
                break;
            }
        }
        if (event.key === 'ArrowDown') {
            event.preventDefault();
            activeIndex = activeIndex < options.length - 1 ? activeIndex + 1 : 0;
        } else if (event.key === 'ArrowUp') {
            event.preventDefault();
            activeIndex = activeIndex > 0 ? activeIndex - 1 : options.length - 1;
        } else if (event.key === 'Enter' && activeIndex >= 0) {
            event.preventDefault();
            selectDirectItem(input, options[activeIndex]);
            return;
        } else if (event.key === 'Escape') {
            suggestions.classList.remove('active');
            return;
        } else {
            return;
        }
        for (var j = 0; j < options.length; j++) {
            options[j].classList.toggle('is-active', j === activeIndex);
        }
    }

    function updateReceiveRow(input) {
        var row = input.closest('tr');
        if (!row) {
            return;
        }
        var ordered = row.querySelector('.ordered-quantity');
        var variance = row.querySelector('.receive-variance');
        var received = row.querySelector('.received-quantity');
        var accepted = row.querySelector('.accepted-quantity');
        var rejected = row.querySelector('.rejected-quantity');
        var reasonCode = row.querySelector('.rejection-reason-code');
        var reasonDetail = row.querySelector('.rejection-reason-detail');
        if (!ordered || !variance || !received || !accepted || !rejected || !reasonCode || !reasonDetail) {
            return;
        }
        var orderedValue = parseFloat(ordered.value || '0');
        var receivedValue = parseFloat(received.value || '0');
        var acceptedValue = parseFloat(accepted.value || '0');
        if (acceptedValue > receivedValue) {
            acceptedValue = receivedValue;
            accepted.value = receivedValue;
        }
        if (acceptedValue < 0) {
            acceptedValue = 0;
            accepted.value = '0';
        }
        var rejectedValue = receivedValue - acceptedValue;
        if (rejectedValue < 0) {
            rejectedValue = 0;
        }
        rejected.value = formatReceiveNumber(rejectedValue);
        var hasRejectedQuantity = rejectedValue > 0;
        reasonCode.disabled = !hasRejectedQuantity;
        reasonDetail.disabled = !hasRejectedQuantity;
        reasonDetail.required = hasRejectedQuantity;
        if (!hasRejectedQuantity) {
            reasonCode.value = '';
            reasonDetail.value = '';
        } else if (!reasonCode.value) {
            reasonCode.value = 'OTHER';
        }
        if (orderedValue === receivedValue) {
            variance.innerHTML = '&#10003;';
            variance.classList.remove('is-different');
        } else {
            variance.textContent = formatReceiveNumber(receivedValue - orderedValue);
            variance.classList.add('is-different');
        }
    }

    function formatReceiveNumber(value) {
        var text = (Math.round(value * 100) / 100).toFixed(2);
        if (text.slice(-3) === '.00') {
            return text.slice(0, -3);
        }
        return text;
    }

    (function initVariance() {
        var fields = document.querySelectorAll('.received-quantity');
        for (var i = 0; i < fields.length; i++) {
            updateReceiveRow(fields[i]);
        }
    })();

    document.addEventListener('click', function(event) {
        var dropdowns = document.querySelectorAll('.receive-suggestions');
        for (var i = 0; i < dropdowns.length; i++) {
            var combo = dropdowns[i].closest('.receive-combobox');
            if (combo && !combo.contains(event.target)) {
                dropdowns[i].classList.remove('active');
            }
        }
    });

    window.addEventListener('resize', function() {
        var inputs = document.querySelectorAll('.receive-item-search');
        for (var i = 0; i < inputs.length; i++) {
            var combo = inputs[i].closest('.receive-combobox');
            var suggestions = combo ? combo.querySelector('.receive-suggestions') : null;
            if (suggestions && suggestions.classList.contains('active')) {
                positionDirectSuggestions(inputs[i], suggestions);
            }
        }
    });

    window.addEventListener('scroll', function() {
        var inputs = document.querySelectorAll('.receive-item-search');
        for (var i = 0; i < inputs.length; i++) {
            var combo = inputs[i].closest('.receive-combobox');
            var suggestions = combo ? combo.querySelector('.receive-suggestions') : null;
            if (suggestions && suggestions.classList.contains('active')) {
                positionDirectSuggestions(inputs[i], suggestions);
            }
        }
    }, true);

    (function initDirectStockValidation() {
        var form = document.getElementById('direct-stock-form');
        if (!form) {
            return;
        }
        form.addEventListener('submit', function(event) {
            if (!validateDirectStockForm()) {
                event.preventDefault();
            }
        });
    })();
</script>
