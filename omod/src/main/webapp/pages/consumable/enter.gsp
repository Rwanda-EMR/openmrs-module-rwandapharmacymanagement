<%
    ui.decorateWith("appui", "standardEmrPage", [ title: "Consumables Order" ])
    ui.includeCss("coreapps", "patientHeader.css")
    def patientDashboardLink = patient ? ui.pageLink("coreapps", "clinicianfacing/patient", [ patientId: patient.patientId ]) : ""
    def requestCountValue = requestCount ?: 0
%>
<style type="text/css">
    #content { max-width:100%; padding-top:12px; }
    .cons-page { display:grid; gap:16px; }
    .cons-header { display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; }
    .cons-header h2 { margin:0; }
    .cons-header p { margin:4px 0 0; color:#64748b; font-size:12px; }
    .cons-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; }
    .cons-warn { border-color:#fcd34d; background:#fffbeb; color:#92400e; }
    .cons-card { background:#fff; border:1px solid #d7dee8; border-radius:4px; overflow:hidden; }
    .cons-card-head { background:#1f5f99; color:#fff; padding:10px 14px; font-weight:600; font-size:13px; }
    .cons-card-body { padding:14px; }
    .cons-summary { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:10px; }
    .cons-summary-card { border:1px solid #dbe5f0; border-radius:4px; background:#f8fbff; padding:10px 12px; }
    .cons-summary-card em { display:block; font-style:normal; text-transform:uppercase; letter-spacing:.04em; font-size:10px; color:#64748b; }
    .cons-summary-card strong { display:block; margin-top:4px; color:#0f172a; font-size:13px; }
    .cons-grid { display:grid; grid-template-columns:minmax(0, 1.2fr) minmax(320px, .8fr); gap:16px; }
    .cons-field { margin-bottom:12px; }
    .cons-field label { display:block; margin-bottom:4px; font-size:12px; font-weight:600; color:#334155; }
    .cons-field input, .cons-field select { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font:inherit; background:#fff; }
    .cons-product-select { display:none; }
    .cons-suggestions { display:none; width:100%; max-height:220px; overflow-y:auto; border:1px solid #cbd5e1; border-top:0; border-radius:0 0 4px 4px; background:#fff; }
    .cons-suggestions.is-visible { display:block; }
    .cons-suggestion { padding:8px 10px; cursor:pointer; border-top:1px solid #eef2f7; font-size:12px; }
    .cons-suggestion:hover { background:#eef5fb; }
    .cons-suggestion strong { display:block; color:#0f172a; }
    .cons-suggestion span { display:block; color:#64748b; margin-top:2px; }
    .cons-suggestion-empty { padding:8px 10px; color:#64748b; font-size:12px; }
    .cons-availability { margin-top:10px; padding:10px 12px; border:1px solid #dbeafe; background:#f8fbff; border-radius:4px; }
    .cons-availability strong { color:#0f172a; }
    .cons-inline-note { font-size:11px; color:#64748b; margin-top:4px; }
    .cons-btn { display:inline-flex; align-items:center; justify-content:center; border:1px solid #cbd5e1; border-radius:4px; padding:8px 14px; cursor:pointer; background:#fff; color:#0f172a; font:inherit; }
    .cons-btn-primary { background:#16a34a; border-color:#15803d; color:#fff; }
    .cons-btn-add { width:100%; background:#1f5f99; border-color:#1f5f99; color:#fff; }
    .cons-table { width:100%; border-collapse:collapse; margin-top:10px; }
    .cons-table th, .cons-table td { border:1px solid #dbe5f0; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .cons-table th { background:#eef5fb; color:#475569; }
    .cons-total-grid { display:grid; grid-template-columns:repeat(3, minmax(0, 1fr)); gap:10px; margin-top:12px; }
    .cons-total-card { border:1px solid #dbe5f0; border-radius:4px; background:#fff; padding:10px; text-align:center; }
    .cons-total-card strong { display:block; color:#0369a1; font-size:16px; margin-top:4px; }
    .cons-remove { border:0; background:#fff1f2; color:#be123c; padding:4px 8px; border-radius:3px; cursor:pointer; }
    .cons-actions { display:flex; justify-content:space-between; gap:10px; align-items:center; flex-wrap:wrap; margin-top:12px; }
    .cons-muted { color:#64748b; font-size:12px; }
    .cons-link { border:0; background:#eff6ff; color:#1d4ed8; padding:4px 8px; border-radius:3px; cursor:pointer; text-decoration:none; }
    .cons-danger { background:#fff1f2; color:#be123c; }
    .cons-inline-form { display:flex; gap:6px; align-items:center; flex-wrap:wrap; margin:4px 0 0; }
    .cons-inline-form input, .cons-inline-form select { border:1px solid #cbd5e1; border-radius:4px; padding:5px 7px; font-size:12px; }
    .cons-hidden-row { display:none; }
    .cons-status { display:inline-flex; border-radius:999px; padding:3px 8px; font-size:11px; font-weight:600; background:#e0f2fe; color:#075985; }
    .cons-status-dispensed { background:#dcfce7; color:#166534; }
    .cons-status-cancelled { background:#fee2e2; color:#991b1b; }
    .cons-history-actions { display:flex; gap:6px; flex-wrap:wrap; }
    .cons-history-details { background:#f8fbff; }
    .cons-history-detail-grid { display:grid; grid-template-columns:repeat(3, minmax(0, 1fr)); gap:8px; margin-bottom:10px; }
    .cons-history-detail-grid div { border:1px solid #dbe5f0; background:#fff; border-radius:4px; padding:8px; font-size:12px; }
    .cons-pagination { display:flex; justify-content:space-between; align-items:center; gap:10px; margin-top:10px; }
    .cons-disabled { opacity:.55; cursor:not-allowed; }
    @media (max-width: 1100px) {
        .cons-summary, .cons-grid, .cons-total-grid, .cons-history-detail-grid { grid-template-columns:1fr; }
    }
</style>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' }
        <% if (patient) { %>,
        { label: "${ ui.encodeJavaScript(ui.encodeHtmlContent(ui.format(patient))) }", link: "${ patientDashboardLink }" }
        <% } %>,
        { label: "Consumables Order" }
    ];
</script>

<div class="cons-page">
    <div class="cons-header">
        <div>
            <h2>Consumables Order</h2>
            <p>Order non-medication items from the pharmacies currently holding stock.</p>
        </div>
        <% if (patient && patientDashboardLink) { %>
        <a href="${ patientDashboardLink }">Back to patient dashboard</a>
        <% } %>
    </div>

    <% if (ordered) { %>
    <div class="cons-banner">Consumables saved and queued for dispensing. Created requests: <strong>${ requestCountValue }</strong>.</div>
    <% } %>
    <% if (errorMessage) { %>
    <div class="cons-banner cons-warn">${ errorMessage }</div>
    <% } %>
    <% if (!patient) { %>
    <div class="cons-banner cons-warn">No patient was supplied. Open this page from the patient dashboard.</div>
    <% } else if (!currentLocation) { %>
    <div class="cons-banner cons-warn">No current OpenMRS location is set for this session.</div>
    <% } else if (!billingReady) { %>
    <div class="cons-banner cons-warn">${ ui.encodeHtmlContent(billingReadinessMessage ?: 'Patient is not ready for consumable billing.') }</div>
    <% } else if (!orderableItems) { %>
    <div class="cons-banner cons-warn">No consumable items are linked locally for ordering.</div>
    <% } %>

    <% if (patient) { %>
    <div class="cons-card">
        <div class="cons-card-head">Patient context</div>
        <div class="cons-card-body">
            <div class="cons-summary">
                <div class="cons-summary-card">
                    <em>Patient</em>
                    <strong>${ ui.encodeHtmlContent(ui.format(patient.personName)) }</strong>
                </div>
                <div class="cons-summary-card">
                    <em>Identifier</em>
                    <strong>${ patient.patientIdentifier ?: patient.patientId }</strong>
                </div>
                <div class="cons-summary-card">
                    <em>Location</em>
                    <strong>${ currentLocation ? currentLocation.name : 'Not set' }</strong>
                </div>
                <div class="cons-summary-card">
                    <em>Queue visibility</em>
                    <strong>Connected pharmacies by location tag</strong>
                </div>
                <div class="cons-summary-card">
                    <em>Orderable items</em>
                    <strong>${ orderableItems ? orderableItems.size() : 0 }</strong>
                </div>
            </div>
        </div>
    </div>
    <% } %>

    <div class="cons-grid">
        <div class="cons-card">
            <div class="cons-card-head">1. Order entry</div>
            <div class="cons-card-body">
                <div class="cons-field">
                    <label>Consumable item</label>
                    <input type="text" id="cons-product-search" placeholder="Type or select a consumable" autocomplete="off" oninput="handleConsumableProductInput()" onfocus="renderConsumableProductSuggestions()"/>
                    <div id="cons-product-suggestions" class="cons-suggestions"></div>
                    <select id="cons-product" class="cons-product-select">
                        <option value="">Select an available consumable</option>
                        <% orderableItems.each { item -> %>
                        <option value="${ item.productMasterItemId }"
                                data-label="${ item.itemName?.replace('"', '&quot;') ?: '' }"
                                data-code="${ item.npcCode ?: '' }"
                                data-material-type="${ item.materialType ?: '' }"
                                data-uom="${ item.defaultUnitOfMeasure ?: 'EA' }"
                                data-stock-sites="${ item.stockSites ?: 0 }"
                                data-stock-total="${ item.stockTotal ?: 0 }">
                            ${ item.itemName } (${ item.npcCode ?: '' }) - ${ item.stockTotal ?: 0 } available
                        </option>
                        <% } %>
                    </select>
                    <div class="cons-inline-note">Start typing and select a consumable from the suggestions.</div>
                </div>
                <div class="cons-availability">
                    <strong>Stock availability:</strong>
                    <span id="cons-availability-text">Select a consumable to load stock information across pharmacies.</span>
                    <div class="cons-inline-note">
                        Pharmacies with stock: <span id="cons-stock-sites">0</span> |
                        Total quantity available: <span id="cons-stock-total">0</span> |
                        Material type: <span id="cons-material-type">-</span>
                    </div>
                </div>
                <div class="cons-field" style="margin-top:12px;">
                    <label>Unit of measure</label>
                    <input type="text" id="cons-uom" value="EA"/>
                </div>
                <div class="cons-field">
                    <label>Quantity</label>
                    <input type="number" step="0.01" min="0" id="cons-qty"/>
                    <div class="cons-inline-note">Use plain units only. No prescription dose, route, duration, or visit fields are required here.</div>
                </div>
                <div class="cons-field">
                    <label>
                        <input type="checkbox" id="cons-not-billable" style="width:auto; margin-right:6px;"/>
                        Do not bill this item
                    </label>
                    <input type="text" id="cons-billing-exclusion-reason" placeholder="Reason this item should not be billed"/>
                    <div class="cons-inline-note">Reason is required when the item should not be billed.</div>
                </div>
                <% if (canAddConsumables) { %>
                <button type="button" class="cons-btn cons-btn-add" onclick="addConsumableLine()">Add item</button>
                <% } else { %>
                <div class="cons-inline-note">${ ui.encodeHtmlContent(billingReadinessMessage ?: 'Patient is not ready for consumable billing.') }</div>
                <% } %>
            </div>
        </div>

        <div class="cons-card">
            <div class="cons-card-head">2. Order summary</div>
            <div class="cons-card-body">
                <form method="post" id="cons-form">
                    <input type="hidden" name="patientId" value="${ patientReferenceForSubmit ?: '' }"/>
                    <input type="hidden" name="patientUuid" value="${ patientUuidForSubmit ?: '' }"/>
                    <table class="cons-table" id="cons-lines-table">
                        <thead>
                        <tr>
                            <th>Item</th>
                            <th>Type</th>
                            <th>UOM</th>
                            <th>Quantity</th>
                            <th>Billing</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody id="cons-lines-body">
                        <tr id="cons-empty-row">
                            <td colspan="6" class="cons-muted">No consumables added yet.</td>
                        </tr>
                        </tbody>
                    </table>
                    <div class="cons-total-grid">
                        <div class="cons-total-card">
                            <span>Total lines</span>
                            <strong id="cons-total-lines">0</strong>
                        </div>
                        <div class="cons-total-card">
                            <span>Total quantity</span>
                            <strong id="cons-total-qty">0</strong>
                        </div>
                    </div>
                    <div class="cons-actions">
                        <span class="cons-muted">Requests are visible to all connected dispensing pharmacies for the requesting location. Billing mappings are validated when the order is submitted, not while loading this page.</span>
                        <button type="submit" class="cons-btn cons-btn-primary"<% if (!billingReady) { %> disabled="disabled" style="opacity:.55; cursor:not-allowed;"<% } %>>Queue consumables</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <% if (patient) { %>
    <div class="cons-card">
        <div class="cons-card-head">Consumable order history</div>
        <div class="cons-card-body">
            <table class="cons-table">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Payment</th>
                    <th>Items</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <% if (!consumableHistory) { %>
                <tr><td colspan="6" class="cons-muted">No consumable order history found for this patient.</td></tr>
                <% } else { consumableHistory.each { row ->
                    def request = row.request
                    def editable = row.editable == true
                    def paid = row.paid == true
                    def hasDispensedLine = row.hasDispensedLine == true
                    def detailId = "cons-history-details-${ request.patientDispensingRequestId }"
                %>
                <tr>
                    <td>#${ request.patientDispensingRequestId }</td>
                    <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                    <td><span class="cons-status ${ request.requestStatus?.toString() == 'DISPENSED' ? 'cons-status-dispensed' : request.requestStatus?.toString() == 'CANCELLED' ? 'cons-status-cancelled' : '' }">${ request.requestStatus ?: '' }</span></td>
                    <td>
                        ${ paid ? 'Paid' : request.paymentExceptionApproved == true ? 'Payment exception' : request.billable != true ? 'Not billable' : request.paymentConfirmed == true ? 'Nothing to pay' : 'Not paid / pending' }
                        <% if (request.patientBillId) { %><div class="cons-inline-note">Bill #${ request.patientBillId }</div><% } %>
                    </td>
                    <td>${ row.activeLineCount ?: 0 }</td>
                    <td>
                        <div class="cons-history-actions">
                            <button type="button" class="cons-link" onclick="toggleConsumableHistory('${ detailId }')">View</button>
                            <% if (!editable) { %>
                            <span class="cons-muted">${ hasDispensedLine ? 'View only after dispensing starts' : paid ? 'View only after payment' : request.paymentExceptionApproved == true ? 'View only after payment exception' : 'View only' }</span>
                            <% } %>
                        </div>
                    </td>
                </tr>
                <tr id="${ detailId }" class="cons-hidden-row cons-history-details">
                    <td colspan="6">
                        <div class="cons-history-detail-grid">
                            <div><strong>Request ID</strong><br/>#${ request.patientDispensingRequestId }</div>
                            <div><strong>Requested location</strong><br/>${ request.requestingLocation?.name ?: '' }</div>
                            <div><strong>Medical act</strong><br/>${ request.medicalActReference ?: '' }</div>
                            <div><strong>Payment confirmed</strong><br/>${ request.paymentConfirmed == true ? 'Yes' : 'No' }</div>
                            <div><strong>Billable</strong><br/>${ request.billable == true ? 'Yes' : 'No' }</div>
                            <div><strong>Edit rule</strong><br/>${ editable ? 'Editable until payment or dispensing starts' : 'Locked' }</div>
                        </div>
                        <table class="cons-table">
                            <thead>
                            <tr>
                                <th>Item</th>
                                <th>UOM</th>
                                <th>Quantity</th>
                                <th>Billing</th>
                                <th>Line status</th>
                                <th>Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% request.lines?.findAll { it.voided != true }?.each { line ->
                                def productMasterItemId = row.productMasterByLineId ? row.productMasterByLineId[line.patientDispensingRequestLineId] : null
                            %>
                            <tr>
                                <td>${ line.item?.name ?: '' }</td>
                                <td>${ line.unitOfMeasure ?: '' }</td>
                                <td>${ line.requestedQuantity ?: 0 }</td>
                                <td>
                                    ${ line.billable == true ? 'Billable' : 'Not billable' }
                                    <% if (line.billingExclusionReason) { %><div class="cons-inline-note">${ line.billingExclusionReason }</div><% } %>
                                </td>
                                <td>${ line.lineStatus ?: '' }</td>
                                <td>
                                    <% if (editable && productMasterItemId) { %>
                                    <form method="post" class="cons-inline-form">
                                        <input type="hidden" name="patientId" value="${ patientReferenceForSubmit ?: '' }"/>
                                        <input type="hidden" name="patientUuid" value="${ patientUuidForSubmit ?: '' }"/>
                                        <input type="hidden" name="action" value="updateLine"/>
                                        <input type="hidden" name="requestId" value="${ request.patientDispensingRequestId }"/>
                                        <input type="hidden" name="requestLineId" value="${ line.patientDispensingRequestLineId }"/>
                                        <select name="productMasterItemId" required="required">
                                            <% orderableItems.each { item -> %>
                                            <option value="${ item.productMasterItemId }"${ item.productMasterItemId == productMasterItemId ? ' selected="selected"' : '' }>${ item.itemName }</option>
                                            <% } %>
                                        </select>
                                        <input type="text" name="unitOfMeasure" value="${ line.unitOfMeasure ?: 'EA' }" style="width:70px;"/>
                                        <input type="number" step="0.01" min="0.01" name="quantity" value="${ line.requestedQuantity ?: 0 }" style="width:90px;"/>
                                        <select name="billable">
                                            <option value="true"${ line.billable == true ? ' selected="selected"' : '' }>Billable</option>
                                            <option value="false"${ line.billable == false ? ' selected="selected"' : '' }>Not billable</option>
                                        </select>
                                        <input type="text" name="billingExclusionReason" value="${ line.billingExclusionReason ?: '' }" placeholder="Non-billable reason"/>
                                        <button type="submit" class="cons-link">Save</button>
                                    </form>
                                    <form method="post" class="cons-inline-form" onsubmit="return confirm('Delete this consumable line? Billing will be rebuilt if needed.');">
                                        <input type="hidden" name="patientId" value="${ patientReferenceForSubmit ?: '' }"/>
                                        <input type="hidden" name="patientUuid" value="${ patientUuidForSubmit ?: '' }"/>
                                        <input type="hidden" name="action" value="voidLine"/>
                                        <input type="hidden" name="requestId" value="${ request.patientDispensingRequestId }"/>
                                        <input type="hidden" name="requestLineId" value="${ line.patientDispensingRequestLineId }"/>
                                        <input type="hidden" name="voidReason" value="Deleted from consumable order history"/>
                                        <button type="submit" class="cons-link cons-danger">Delete</button>
                                    </form>
                                    <% } else { %>
                                    <span class="cons-muted">${ editable ? 'Item mapping unavailable for editing' : 'Locked' }</span>
                                    <% } %>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </td>
                </tr>
                <% } } %>
                </tbody>
            </table>
            <div class="cons-pagination">
                <div class="cons-muted">Showing ${ historyPageSize ?: 10 } per page. Total orders: ${ historyTotal ?: 0 }.</div>
                <div class="cons-history-actions">
                    <% if (historyPreviousPage != null) { %>
                    <a class="cons-link" href="${ ui.pageLink('rwandapharmacymanagement', 'consumable/enter', [patientId: patient.uuid, historyPage: historyPreviousPage]) }">Previous</a>
                    <% } else { %>
                    <span class="cons-link cons-disabled">Previous</span>
                    <% } %>
                    <span class="cons-muted">Page ${(historyPage ?: 0) + 1}</span>
                    <% if (historyNextPage != null) { %>
                    <a class="cons-link" href="${ ui.pageLink('rwandapharmacymanagement', 'consumable/enter', [patientId: patient.uuid, historyPage: historyNextPage]) }">Next</a>
                    <% } else { %>
                    <span class="cons-link cons-disabled">Next</span>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
    <% } %>

</div>

<script type="text/javascript">
    var allConsumableProducts = [];

    function parseConsumableNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }

    function trimConsumableText(value) {
        if (value == null) {
            return '';
        }
        if (typeof value.trim === 'function') {
            return value.trim();
        }
        var text = String(value);
        while (text.length && text.charAt(0) === ' ') {
            text = text.substring(1);
        }
        while (text.length && text.charAt(text.length - 1) === ' ') {
            text = text.substring(0, text.length - 1);
        }
        return text;
    }

    function escapeConsumableHtml(value) {
        var text = value == null ? '' : String(value);
        return text.split('&').join('&amp;')
            .split('<').join('&lt;')
            .split('>').join('&gt;')
            .split('"').join('&quot;')
            .split("'").join('&#39;');
    }

    function normalizeConsumableText(value) {
        return trimConsumableText(value).toLowerCase();
    }

    function initializeConsumableProducts() {
        var select = document.getElementById('cons-product');
        allConsumableProducts = [];
        if (!select || !select.options) {
            return;
        }
        for (var i = 0; i < select.options.length; i++) {
            var option = select.options[i];
            if (!option.value) {
                continue;
            }
            allConsumableProducts.push({
                value: option.value,
                label: option.getAttribute('data-label') || option.text,
                code: option.getAttribute('data-code') || '',
                materialType: option.getAttribute('data-material-type') || '',
                stockTotal: option.getAttribute('data-stock-total') || '0',
                optionIndex: i
            });
        }
    }

    function consumableProductMatchesInput(product, inputValue) {
        var typed = normalizeConsumableText(inputValue);
        if (!product || !typed) {
            return false;
        }
        return normalizeConsumableText(product.label) === typed
            || normalizeConsumableText(product.label + ' (' + product.code + ')') === typed;
    }

    function matchingConsumableProducts() {
        var input = document.getElementById('cons-product-search');
        var searchText = normalizeConsumableText(input ? input.value : '');
        var matches = [];
        for (var i = 0; i < allConsumableProducts.length; i++) {
            var product = allConsumableProducts[i];
            var label = normalizeConsumableText(product.label);
            var code = normalizeConsumableText(product.code);
            var type = normalizeConsumableText(product.materialType);
            if (!searchText || label.indexOf(searchText) >= 0 || code.indexOf(searchText) >= 0 || type.indexOf(searchText) >= 0) {
                matches.push(product);
            }
        }
        return matches;
    }

    function handleConsumableProductInput() {
        var input = document.getElementById('cons-product-search');
        var select = document.getElementById('cons-product');
        if (!input || !select) {
            return;
        }
        select.selectedIndex = 0;
        for (var i = 0; i < allConsumableProducts.length; i++) {
            var product = allConsumableProducts[i];
            if (consumableProductMatchesInput(product, input.value)) {
                selectConsumableProduct(product.optionIndex, product.label);
                return;
            }
        }
        updateConsumableAvailability();
        renderConsumableProductSuggestions();
    }

    function renderConsumableProductSuggestions() {
        var suggestions = document.getElementById('cons-product-suggestions');
        if (!suggestions) {
            return;
        }
        suggestions.innerHTML = '';
        var matches = matchingConsumableProducts();
        if (!matches.length) {
            suggestions.innerHTML = '<div class="cons-suggestion-empty">No matching consumables found.</div>';
            suggestions.classList.add('is-visible');
            return;
        }
        var maxSuggestions = Math.min(matches.length, 80);
        for (var i = 0; i < maxSuggestions; i++) {
            var product = matches[i];
            var row = document.createElement('div');
            row.className = 'cons-suggestion';
            row.setAttribute('data-option-index', product.optionIndex);
            row.setAttribute('data-label', product.label);
            row.innerHTML = '<strong>' + escapeConsumableHtml(product.label) + '</strong><span>'
                + escapeConsumableHtml((product.code ? product.code + ' | ' : '') + 'Qty: ' + product.stockTotal) + '</span>';
            row.onclick = function () {
                selectConsumableProduct(parseInt(this.getAttribute('data-option-index'), 10), this.getAttribute('data-label'));
            };
            suggestions.appendChild(row);
        }
        if (matches.length > maxSuggestions) {
            var more = document.createElement('div');
            more.className = 'cons-suggestion-empty';
            more.textContent = 'Keep typing to narrow ' + matches.length + ' matching consumables.';
            suggestions.appendChild(more);
        }
        suggestions.classList.add('is-visible');
    }

    function selectConsumableProduct(optionIndex, label) {
        var select = document.getElementById('cons-product');
        var input = document.getElementById('cons-product-search');
        if (select && !isNaN(optionIndex)) {
            select.selectedIndex = optionIndex;
        }
        if (input) {
            input.value = label || '';
        }
        hideConsumableProductSuggestions();
        updateConsumableAvailability();
    }

    function hideConsumableProductSuggestions() {
        var suggestions = document.getElementById('cons-product-suggestions');
        if (suggestions) {
            suggestions.classList.remove('is-visible');
        }
    }

    function toggleConsumableHistory(rowId) {
        var row = document.getElementById(rowId);
        if (!row) {
            return;
        }
        if (row.className.indexOf('cons-hidden-row') >= 0) {
            row.className = row.className.replace('cons-hidden-row', '');
        } else {
            row.className += ' cons-hidden-row';
        }
    }

    function selectedConsumableOption() {
        var select = document.getElementById('cons-product');
        return select && select.selectedIndex >= 0 ? select.options[select.selectedIndex] : null;
    }

    function updateConsumableAvailability() {
        var option = selectedConsumableOption();
        document.getElementById('cons-stock-sites').textContent = '0';
        document.getElementById('cons-stock-total').textContent = '0';
        document.getElementById('cons-material-type').textContent = option ? option.getAttribute('data-material-type') || '-' : '-';
        document.getElementById('cons-availability-text').textContent = option && option.value
            ? ('Available stock for ' + (option.getAttribute('data-label') || 'the selected item') + ' across all pharmacies.')
            : 'Select a consumable to load stock information across pharmacies.';
        if (option && option.value) {
            document.getElementById('cons-uom').value = option.getAttribute('data-uom') || 'EA';
            document.getElementById('cons-stock-sites').textContent = option.getAttribute('data-stock-sites') || '0';
            document.getElementById('cons-stock-total').textContent = option.getAttribute('data-stock-total') || '0';
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '/' + OPENMRS_CONTEXT_PATH + '/module/rwandapharmacymanagement/prescription/availability?productMasterItemId=' + encodeURIComponent(option.value), true);
            xhr.onreadystatechange = function () {
                if (xhr.readyState !== 4) {
                    return;
                }
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        var data = JSON.parse(xhr.responseText || '{}');
                        document.getElementById('cons-stock-sites').textContent = data.pharmaciesWithStock || 0;
                        document.getElementById('cons-stock-total').textContent = data.totalQuantity || 0;
                        document.getElementById('cons-availability-text').textContent = 'Available stock for ' + (option.getAttribute('data-label') || 'the selected item') + ' across all pharmacies.';
                    } catch (e) {
                        document.getElementById('cons-availability-text').textContent = 'Unable to load stock availability right now.';
                    }
                } else {
                    document.getElementById('cons-availability-text').textContent = 'Unable to load stock availability right now.';
                }
            };
            xhr.send(null);
        }
    }

    function addConsumableLine() {
        var option = selectedConsumableOption();
        var quantityInput = document.getElementById('cons-qty');
        var unitInput = document.getElementById('cons-uom');
        if (!option || !option.value) {
            alert('Select a consumable item first.');
            return;
        }
        var quantity = parseConsumableNumber(quantityInput.value);
        if (quantity <= 0) {
            alert('Quantity must be greater than zero.');
            return;
        }
        var billable = !(document.getElementById('cons-not-billable') && document.getElementById('cons-not-billable').checked);
        var billingExclusionReason = document.getElementById('cons-billing-exclusion-reason')
            ? trimConsumableText(document.getElementById('cons-billing-exclusion-reason').value)
            : '';
        if (!billable && !billingExclusionReason) {
            alert('Enter a reason for not billing this item.');
            document.getElementById('cons-billing-exclusion-reason').focus();
            return;
        }
        var tableBody = document.getElementById('cons-lines-body');
        var emptyRow = document.getElementById('cons-empty-row');
        if (emptyRow) {
            emptyRow.parentNode.removeChild(emptyRow);
        }

        var row = document.createElement('tr');
        row.setAttribute('data-quantity', quantity);
        row.setAttribute('data-material-type', option.getAttribute('data-material-type') || '');

        var itemCell = document.createElement('td');
        itemCell.innerHTML = (option.getAttribute('data-label') || '') + '<div class="cons-inline-note">' + (option.getAttribute('data-code') || '') + '</div>';
        row.appendChild(itemCell);

        var typeCell = document.createElement('td');
        typeCell.textContent = option.getAttribute('data-material-type') || '';
        row.appendChild(typeCell);

        var uomCell = document.createElement('td');
        uomCell.textContent = unitInput.value || 'EA';
        row.appendChild(uomCell);

        var qtyCell = document.createElement('td');
        qtyCell.textContent = quantity;
        row.appendChild(qtyCell);

        var billingCell = document.createElement('td');
        billingCell.textContent = billable ? 'Billable' : 'Not billable';
        if (!billable) {
            var billingNote = document.createElement('div');
            billingNote.className = 'cons-inline-note';
            billingNote.textContent = billingExclusionReason;
            billingCell.appendChild(billingNote);
        }
        row.appendChild(billingCell);

        var actionCell = document.createElement('td');
        var removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'cons-remove';
        removeButton.textContent = 'Remove';
        removeButton.onclick = function () {
            row.parentNode.removeChild(row);
            if (!tableBody.children.length) {
                var replacement = document.createElement('tr');
                replacement.id = 'cons-empty-row';
                replacement.innerHTML = '<td colspan="6" class="cons-muted">No consumables added yet.</td>';
                tableBody.appendChild(replacement);
            }
            updateConsumableTotals();
        };
        actionCell.appendChild(removeButton);
        var productInput = document.createElement('input');
        productInput.type = 'hidden';
        productInput.name = 'productMasterItemId';
        productInput.value = option.value;
        actionCell.appendChild(productInput);
        var uomInput = document.createElement('input');
        uomInput.type = 'hidden';
        uomInput.name = 'unitOfMeasure';
        uomInput.value = unitInput.value || 'EA';
        actionCell.appendChild(uomInput);
        var quantityHiddenInput = document.createElement('input');
        quantityHiddenInput.type = 'hidden';
        quantityHiddenInput.name = 'quantity';
        quantityHiddenInput.value = quantity;
        actionCell.appendChild(quantityHiddenInput);
        var billableInput = document.createElement('input');
        billableInput.type = 'hidden';
        billableInput.name = 'billable';
        billableInput.value = billable ? 'true' : 'false';
        actionCell.appendChild(billableInput);
        var exclusionReasonInput = document.createElement('input');
        exclusionReasonInput.type = 'hidden';
        exclusionReasonInput.name = 'billingExclusionReason';
        exclusionReasonInput.value = billable ? '' : billingExclusionReason;
        actionCell.appendChild(exclusionReasonInput);
        row.appendChild(actionCell);

        tableBody.appendChild(row);
        quantityInput.value = '';
        document.getElementById('cons-product').selectedIndex = 0;
        document.getElementById('cons-product-search').value = '';
        if (document.getElementById('cons-not-billable')) {
            document.getElementById('cons-not-billable').checked = false;
        }
        if (document.getElementById('cons-billing-exclusion-reason')) {
            document.getElementById('cons-billing-exclusion-reason').value = '';
        }
        updateConsumableTotals();
    }

    function updateConsumableTotals() {
        var rows = document.querySelectorAll('#cons-lines-body tr[data-quantity]');
        var totalQuantity = 0;
        for (var i = 0; i < rows.length; i++) {
            totalQuantity += parseConsumableNumber(rows[i].getAttribute('data-quantity'));
        }
        document.getElementById('cons-total-lines').textContent = rows.length;
        document.getElementById('cons-total-qty').textContent = totalQuantity;
    }

    initializeConsumableProducts();
    document.getElementById('cons-product').onchange = updateConsumableAvailability;
    document.getElementById('cons-form').onsubmit = function () {
        <% if (!billingReady) { %>
        alert('${ ui.encodeJavaScript(billingReadinessMessage ?: 'Patient is not ready for consumable billing.') }');
        return false;
        <% } %>
        if (!document.querySelectorAll('#cons-lines-body tr[data-quantity]').length) {
            alert('Add at least one consumable item before saving.');
            return false;
        }
        return true;
    };
    updateConsumableAvailability();
</script>
