<%
    ui.decorateWith("appui", "standardEmrPage", [ title: "Lab Reagents And Consumables Request" ])
%>
<style type="text/css">
    #content { max-width:100%; padding-top:12px; }
    .lab-page { display:grid; gap:16px; }
    .lab-header { display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; }
    .lab-header h2 { margin:0; }
    .lab-header p { margin:4px 0 0; color:#64748b; font-size:12px; }
    .lab-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; }
    .lab-warn { border-color:#fcd34d; background:#fffbeb; color:#92400e; }
    .lab-card { background:#fff; border:1px solid #d7dee8; border-radius:4px; overflow:hidden; }
    .lab-card-head { background:#1f5f99; color:#fff; padding:10px 14px; font-weight:600; font-size:13px; }
    .lab-card-body { padding:14px; }
    .lab-summary { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:10px; }
    .lab-summary-card { border:1px solid #dbe5f0; border-radius:4px; background:#f8fbff; padding:10px 12px; }
    .lab-summary-card em { display:block; font-style:normal; text-transform:uppercase; letter-spacing:.04em; font-size:10px; color:#64748b; }
    .lab-summary-card strong { display:block; margin-top:4px; color:#0f172a; font-size:13px; }
    .lab-grid { display:grid; grid-template-columns:minmax(0, 1.2fr) minmax(320px, .8fr); gap:16px; }
    .lab-field { margin-bottom:12px; }
    .lab-field label { display:block; margin-bottom:4px; font-size:12px; font-weight:600; color:#334155; }
    .lab-field input, .lab-field select { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font:inherit; background:#fff; }
    .lab-availability { margin-top:10px; padding:10px 12px; border:1px solid #dbeafe; background:#f8fbff; border-radius:4px; }
    .lab-availability strong { color:#0f172a; }
    .lab-inline-note { font-size:11px; color:#64748b; margin-top:4px; }
    .lab-btn { display:inline-flex; align-items:center; justify-content:center; border:1px solid #cbd5e1; border-radius:4px; padding:8px 14px; cursor:pointer; background:#fff; color:#0f172a; font:inherit; }
    .lab-btn-primary { background:#16a34a; border-color:#15803d; color:#fff; }
    .lab-btn-add { width:100%; background:#1f5f99; border-color:#1f5f99; color:#fff; }
    .lab-table { width:100%; border-collapse:collapse; margin-top:10px; }
    .lab-table th, .lab-table td { border:1px solid #dbe5f0; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .lab-table th { background:#eef5fb; color:#475569; }
    .lab-total-grid { display:grid; grid-template-columns:repeat(2, minmax(0, 1fr)); gap:10px; margin-top:12px; }
    .lab-total-card { border:1px solid #dbe5f0; border-radius:4px; background:#fff; padding:10px; text-align:center; }
    .lab-total-card strong { display:block; color:#0369a1; font-size:16px; margin-top:4px; }
    .lab-remove { border:0; background:#fff1f2; color:#be123c; padding:4px 8px; border-radius:3px; cursor:pointer; }
    .lab-actions { display:flex; justify-content:space-between; gap:10px; align-items:center; flex-wrap:wrap; margin-top:12px; }
    .lab-muted { color:#64748b; font-size:12px; }
    .lab-status { display:inline-flex; align-items:center; padding:3px 8px; border-radius:999px; font-size:11px; font-weight:600; background:#e2e8f0; color:#334155; }
    .lab-status-requested { background:#fef3c7; color:#92400e; }
    .lab-status-approved { background:#dcfce7; color:#166534; }
    .lab-history-lines div { margin-bottom:4px; }
    .lab-pagination { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-top:12px; flex-wrap:wrap; }
    @media (max-width: 1100px) {
        .lab-summary, .lab-grid, .lab-total-grid { grid-template-columns:1fr; }
    }
</style>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "Lab Reagents And Consumables Request" }
    ];
</script>

<div class="lab-page">
    <div class="lab-header">
        <div>
            <h2>Lab Reagents And Consumables Request</h2>
            <p>Request stocked laboratory materials without using the pharmacy side menu workflow.</p>
        </div>
    </div>

    <% if (requested) { %>
    <div class="lab-banner">Lab supply request submitted to lab dispensing queue.</div>
    <% } %>
    <% if (!currentLocation) { %>
    <div class="lab-banner lab-warn">No current OpenMRS location is set for this session.</div>
    <% } else if (!labServices) { %>
    <div class="lab-banner lab-warn">No lab services are configured. Set the Lab Service Concept Set in pharmacy settings before submitting lab requests.</div>
    <% } else if (!availableItems) { %>
    <div class="lab-banner lab-warn">No stocked lab supplies are available for the connected lab dispensing pharmacies.</div>
    <% } %>

    <div class="lab-card">
        <div class="lab-card-head">Request context</div>
        <div class="lab-card-body">
            <div class="lab-summary">
                <div class="lab-summary-card">
                    <em>Lab unit</em>
                    <strong>${ currentLocation ? currentLocation.name : 'Not set' }</strong>
                </div>
                <div class="lab-summary-card">
                    <em>Queue visibility</em>
                    <strong>Connected lab dispensing pharmacies</strong>
                </div>
                <div class="lab-summary-card">
                    <em>Orderable items</em>
                    <strong>${ availableItems ? availableItems.size() : 0 }</strong>
                </div>
                <div class="lab-summary-card">
                    <em>Lab services</em>
                    <strong>${ labServices ? labServices.size() : 0 }</strong>
                </div>
            </div>
        </div>
    </div>

    <div class="lab-grid">
        <div class="lab-card">
            <div class="lab-card-head">1. Request entry</div>
            <div class="lab-card-body">
                <div class="lab-field">
                    <label>Item</label>
                    <select id="lab-item">
                        <option value="">Select an available lab supply</option>
                        <% availableItems.each { item -> %>
                        <option value="${ item.itemId }" data-name="${ item.name ?: '' }" data-available="${ item.available ?: 0 }">
                            ${ item.name } (${ item.available ?: 0 } available)
                        </option>
                        <% } %>
                    </select>
                </div>
                <div class="lab-field">
                    <label>Lab service requested</label>
                    <select id="lab-service">
                        <option value="">Select lab service</option>
                        <% labServices.each { service -> %>
                        <option value="${ service.conceptId }" data-name="${ service.displayString ?: '' }">${ service.displayString ?: '' }</option>
                        <% } %>
                    </select>
                </div>
                <div class="lab-availability">
                    <strong>Stock availability:</strong>
                    <span id="lab-availability-text">Select an item to load availability across connected lab dispensing pharmacies.</span>
                    <div class="lab-inline-note">
                        Total quantity available: <span id="lab-stock-total">0</span>
                    </div>
                </div>
                <div class="lab-field" style="margin-top:12px;">
                    <label>Quantity</label>
                    <input type="number" step="0.01" min="0" id="lab-qty"/>
                    <div class="lab-inline-note">Use plain quantity only. The lab dispensing team will review and can adjust approved quantity.</div>
                </div>
                <button type="button" class="lab-btn lab-btn-add" onclick="addLabLine()">Add item</button>
            </div>
        </div>

        <div class="lab-card">
            <div class="lab-card-head">2. Request summary</div>
            <div class="lab-card-body">
                <form method="post" id="lab-request-form">
                    <table class="lab-table">
                        <thead>
                        <tr>
                            <th>Item</th>
                            <th>Lab service</th>
                            <th>Quantity</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody id="lab-lines-body">
                        <tr id="lab-empty-row">
                            <td colspan="4" class="lab-muted">No items added yet.</td>
                        </tr>
                        </tbody>
                    </table>
                    <div class="lab-total-grid">
                        <div class="lab-total-card">
                            <span>Total lines</span>
                            <strong id="lab-total-lines">0</strong>
                        </div>
                        <div class="lab-total-card">
                            <span>Total quantity</span>
                            <strong id="lab-total-qty">0</strong>
                        </div>
                    </div>
                    <div class="lab-actions">
                        <span class="lab-muted">Requests go to the lab dispensing queue, where staff can review, adjust quantity, approve, and deduct from stock.</span>
                        <button type="submit" class="lab-btn lab-btn-primary">Submit lab request</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <div class="lab-card">
        <div class="lab-card-head">3. My request history</div>
        <div class="lab-card-body">
            <% if (!requestHistory) { %>
            <div class="lab-muted">No previous lab supply requests found for the current user.</div>
            <% } else { %>
            <table class="lab-table">
                <thead>
                <tr>
                    <th>Request ID</th>
                    <th>Submitted</th>
                    <th>Lab unit</th>
                    <th>Status</th>
                    <th>Reviewed at</th>
                    <th>Items</th>
                </tr>
                </thead>
                <tbody>
                <% requestHistory.each { request -> %>
                <tr>
                    <td>#${ request.labSupplyRequestId ?: '' }</td>
                    <td>${ request.dateCreated ? request.dateCreated.format('yyyy-MM-dd HH:mm') : '' }</td>
                    <td>${ request.labUnit?.name ?: '' }</td>
                    <td>
                        <span class="lab-status ${ request.requestStatus == 'APPROVED' ? 'lab-status-approved' : 'lab-status-requested' }">
                            ${ request.requestStatus ?: '' }
                        </span>
                    </td>
                    <td>
                        <% if (request.approvedDate) { %>
                        ${ request.approvedDate.format('yyyy-MM-dd HH:mm') }${ request.dispensingPharmacyLocation?.name ? ' - ' + request.dispensingPharmacyLocation.name : '' }
                        <% } else { %>
                        <span class="lab-muted">Pending review</span>
                        <% } %>
                    </td>
                    <td class="lab-history-lines">
                        <% request.lines?.each { line -> %>
                        <div>
                            ${ line.item?.name ?: '' }${ line.labServiceConcept?.displayString ? ' - ' + line.labServiceConcept.displayString : '' }: ${ line.requestedQuantity ?: 0 }
                            <% if (line.approvedQuantity != null) { %>
                            | approved ${ line.approvedQuantity }
                            <% } %>
                        </div>
                        <% } %>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <div class="lab-pagination">
                <div class="lab-muted">Page ${ historyPage ?: 1 } of ${ historyTotalPages ?: 1 } | ${ historyTotalCount ?: 0 } requests</div>
                <div>
                    <% if ((historyPage ?: 1) > 1) { %>
                    <a class="lab-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'lab/request', [historyPage: (historyPage ?: 1) - 1]) }">Previous</a>
                    <% } %>
                    <% if ((historyPage ?: 1) < (historyTotalPages ?: 1)) { %>
                    <a class="lab-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'lab/request', [historyPage: (historyPage ?: 1) + 1]) }">Next</a>
                    <% } %>
                </div>
            </div>
            <% } %>
        </div>
    </div>
</div>

<script type="text/javascript">
    function parseLabNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }

    function selectedLabOption() {
        var select = document.getElementById('lab-item');
        return select && select.selectedIndex >= 0 ? select.options[select.selectedIndex] : null;
    }

    function selectedLabServiceOption() {
        var select = document.getElementById('lab-service');
        return select && select.selectedIndex >= 0 ? select.options[select.selectedIndex] : null;
    }

    function updateLabAvailability() {
        var option = selectedLabOption();
        document.getElementById('lab-stock-total').textContent = option ? (option.getAttribute('data-available') || '0') : '0';
        document.getElementById('lab-availability-text').textContent = option && option.value
            ? ('Available stock for ' + (option.getAttribute('data-name') || 'the selected item') + ' across connected lab dispensing pharmacies.')
            : 'Select an item to load availability across connected lab dispensing pharmacies.';
    }

    function addLabLine() {
        var option = selectedLabOption();
        var serviceOption = selectedLabServiceOption();
        var quantityInput = document.getElementById('lab-qty');
        if (!option || !option.value) {
            alert('Select an item first.');
            return;
        }
        if (!serviceOption || !serviceOption.value) {
            alert('Select the lab service requested.');
            return;
        }
        var quantity = parseLabNumber(quantityInput.value);
        if (quantity <= 0) {
            alert('Quantity must be greater than zero.');
            return;
        }
        var tableBody = document.getElementById('lab-lines-body');
        var emptyRow = document.getElementById('lab-empty-row');
        if (emptyRow) {
            emptyRow.parentNode.removeChild(emptyRow);
        }
        var row = document.createElement('tr');
        row.setAttribute('data-quantity', quantity);
        row.innerHTML = '<td>' + (option.getAttribute('data-name') || '') + '</td>'
            + '<td>' + (serviceOption.getAttribute('data-name') || '') + '</td>'
            + '<td>' + quantity + '</td>'
            + '<td><button type="button" class="lab-remove" onclick="removeLabLine(this)">Remove</button>'
            + '<input type="hidden" name="itemId" value="' + option.value + '"/>'
            + '<input type="hidden" name="labServiceConceptId" value="' + serviceOption.value + '"/>'
            + '<input type="hidden" name="quantity" value="' + quantity + '"/></td>';
        tableBody.appendChild(row);
        quantityInput.value = '';
        updateLabTotals();
    }

    function removeLabLine(button) {
        var row = button.closest('tr');
        if (row && row.parentNode) {
            row.parentNode.removeChild(row);
        }
        var tbody = document.getElementById('lab-lines-body');
        if (!tbody.querySelector('tr[data-quantity]')) {
            var replacement = document.createElement('tr');
            replacement.id = 'lab-empty-row';
            replacement.innerHTML = '<td colspan="4" class="lab-muted">No items added yet.</td>';
            tbody.appendChild(replacement);
        }
        updateLabTotals();
    }

    function updateLabTotals() {
        var rows = document.querySelectorAll('#lab-lines-body tr[data-quantity]');
        var totalQuantity = 0;
        for (var i = 0; i < rows.length; i++) {
            totalQuantity += parseLabNumber(rows[i].getAttribute('data-quantity'));
        }
        document.getElementById('lab-total-lines').textContent = rows.length;
        document.getElementById('lab-total-qty').textContent = totalQuantity;
    }

    document.getElementById('lab-item').onchange = updateLabAvailability;
    document.getElementById('lab-request-form').onsubmit = function() {
        if (!document.querySelectorAll('#lab-lines-body tr[data-quantity]').length) {
            alert('Add at least one item before submitting.');
            return false;
        }
        return true;
    };
    updateLabAvailability();
    updateLabTotals();
</script>
