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
    @media (max-width: 1100px) {
        .cons-summary, .cons-grid, .cons-total-grid { grid-template-columns:1fr; }
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
    <% if (!patient) { %>
    <div class="cons-banner cons-warn">No patient was supplied. Open this page from the patient dashboard.</div>
    <% } else if (!currentLocation) { %>
    <div class="cons-banner cons-warn">No current OpenMRS location is set for this session.</div>
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
                    <select id="cons-product">
                        <option value="">Select an available consumable</option>
                        <% orderableItems.each { item -> %>
                        <option value="${ item.productMasterItemId }"
                                data-label="${ item.itemName?.replace('"', '&quot;') ?: '' }"
                                data-code="${ item.npcCode ?: '' }"
                                data-material-type="${ item.materialType ?: '' }"
                                data-uom="${ item.defaultUnitOfMeasure ?: 'EA' }">
                            ${ item.itemName } (${ item.npcCode ?: '' })
                        </option>
                        <% } %>
                    </select>
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
                <button type="button" class="cons-btn cons-btn-add" onclick="addConsumableLine()">Add item</button>
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
                            <th></th>
                        </tr>
                        </thead>
                        <tbody id="cons-lines-body">
                        <tr id="cons-empty-row">
                            <td colspan="5" class="cons-muted">No consumables added yet.</td>
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
                        <button type="submit" class="cons-btn cons-btn-primary">Queue consumables</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript">
    function parseConsumableNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
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
            ? ('Loading stock availability for ' + (option.getAttribute('data-label') || 'the selected item') + ' across all pharmacies.')
            : 'Select a consumable to load stock information across pharmacies.';
        if (option && option.value) {
            document.getElementById('cons-uom').value = option.getAttribute('data-uom') || 'EA';
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
                replacement.innerHTML = '<td colspan="5" class="cons-muted">No consumables added yet.</td>';
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
        row.appendChild(actionCell);

        tableBody.appendChild(row);
        quantityInput.value = '';
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

    document.getElementById('cons-product').onchange = updateConsumableAvailability;
    document.getElementById('cons-form').onsubmit = function () {
        if (!document.querySelectorAll('#cons-lines-body tr[data-quantity]').length) {
            alert('Add at least one consumable item before saving.');
            return false;
        }
        return true;
    };
    updateConsumableAvailability();
</script>
