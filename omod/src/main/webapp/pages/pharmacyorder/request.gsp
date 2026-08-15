<%
    ui.decorateWith("appui", "standardEmrPage", [ title: "Pharmacy Item Request" ])
%>
<style type="text/css">
    #content { max-width:100%; padding-top:12px; }
    .po-page { display:grid; gap:16px; }
    .po-header { display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; }
    .po-header h2 { margin:0; }
    .po-header p { margin:4px 0 0; color:#64748b; font-size:12px; }
    .po-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; }
    .po-warn { border-color:#fcd34d; background:#fffbeb; color:#92400e; }
    .po-card { background:#fff; border:1px solid #d7dee8; border-radius:4px; overflow:hidden; }
    .po-card-head { background:#1f5f99; color:#fff; padding:10px 14px; font-weight:600; font-size:13px; }
    .po-card-body { padding:14px; }
    .po-grid { display:grid; grid-template-columns:minmax(0, 1.1fr) minmax(340px, .9fr); gap:16px; }
    .po-field { margin-bottom:12px; }
    .po-field label { display:block; margin-bottom:4px; font-size:12px; font-weight:600; color:#334155; }
    .po-field input, .po-field select { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font:inherit; background:#fff; }
    .po-btn { display:inline-flex; align-items:center; justify-content:center; border:1px solid #cbd5e1; border-radius:4px; padding:8px 14px; cursor:pointer; background:#fff; color:#0f172a; font:inherit; text-decoration:none; }
    .po-btn-primary { background:#16a34a; border-color:#15803d; color:#fff; }
    .po-btn-add { width:100%; background:#1f5f99; border-color:#1f5f99; color:#fff; }
    .po-table { width:100%; border-collapse:collapse; margin-top:10px; }
    .po-table th, .po-table td { border:1px solid #dbe5f0; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .po-table th { background:#eef5fb; color:#475569; }
    .po-muted { color:#64748b; font-size:12px; }
    .po-actions { display:flex; justify-content:space-between; gap:10px; align-items:center; flex-wrap:wrap; margin-top:12px; }
    .po-status { display:inline-flex; align-items:center; border-radius:999px; padding:3px 8px; font-size:11px; font-weight:600; text-transform:uppercase; }
    .po-status-requested { background:#fef3c7; color:#92400e; }
    .po-status-approved { background:#dcfce7; color:#166534; }
    .po-status-rejected { background:#fee2e2; color:#991b1b; }
    .po-removed { display:inline-block; margin-left:6px; padding:2px 7px; border-radius:999px; background:#fff1f2; color:#991b1b; font-size:11px; font-weight:600; }
    .po-history-lines div + div { margin-top:4px; }
    .po-pagination { display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap; margin-top:12px; }
    @media (max-width: 1100px) { .po-grid { grid-template-columns:1fr; } }
</style>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "Pharmacy Item Request" }
    ];
</script>

<div class="po-page">
    <div class="po-header">
        <div>
            <h2>Pharmacy Item Request</h2>
            <p>Request stocked items directly from any pharmacy without linking the request to a patient.</p>
        </div>
    </div>

    <% if (requested) { %><div class="po-banner">Pharmacy request submitted.</div><% } %>
    <% if (errorMessage) { %><div class="po-banner po-warn">${ errorMessage }</div><% } %>
    <div class="po-grid">
        <div class="po-card">
            <div class="po-card-head">1. Request entry</div>
            <div class="po-card-body">
                <div class="po-field">
                    <label>Pharmacy</label>
                    <select id="po-source">
                        <option value="">Select pharmacy</option>
                        <% sourcePharmacies.each { pharmacy -> %>
                        <option value="${ pharmacy.pharmacyLocationId }">${ pharmacy.name }${ pharmacy.mainStore ? ' (main)' : '' }</option>
                        <% } %>
                    </select>
                </div>
                <div class="po-field">
                    <label>Item</label>
                    <select id="po-item"><option value="">Select item</option></select>
                </div>
                <div class="po-field">
                    <label>Quantity</label>
                    <input type="number" step="0.01" min="0" id="po-qty"/>
                    <div class="po-muted">Available: <span id="po-available">0</span></div>
                </div>
                <button type="button" class="po-btn po-btn-add" onclick="addPharmacyOrderLine()">Add item</button>
            </div>
        </div>

        <div class="po-card">
            <div class="po-card-head">2. Request summary</div>
            <div class="po-card-body">
                <form method="post" id="po-request-form">
                    <input type="hidden" name="requestedPharmacyLocationId" id="po-form-source"/>
                    <table class="po-table">
                        <thead><tr><th>Item</th><th>Quantity</th><th></th></tr></thead>
                        <tbody id="po-lines-body">
                        <tr id="po-empty-row"><td colspan="3" class="po-muted">No items added yet.</td></tr>
                        </tbody>
                    </table>
                    <div class="po-field" style="margin-top:12px;">
                        <label>Reason</label>
                        <input type="text" name="reason"/>
                    </div>
                    <div class="po-actions">
                        <span class="po-muted">The selected pharmacy will review and approve the request.</span>
                        <button type="submit" class="po-btn po-btn-primary">Submit request</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <div class="po-card">
        <div class="po-card-head">3. My request history</div>
        <div class="po-card-body">
            <% if (!requestHistory) { %>
            <div class="po-muted">No previous pharmacy item requests found for the current user.</div>
            <% } else { %>
            <table class="po-table">
                <thead>
                <tr>
                    <th>Request ID</th>
                    <th>Submitted</th>
                    <th>Pharmacy</th>
                    <th>Status</th>
                    <th>Reviewed at</th>
                    <th>Items</th>
                    <th>Download</th>
                </tr>
                </thead>
                <tbody>
                <% requestHistory.each { request -> %>
                <tr>
                    <td>#${ request.pharmacyItemRequestId ?: '' }</td>
                    <td>${ request.dateCreated ? request.dateCreated.format('yyyy-MM-dd HH:mm') : '' }</td>
                    <td>${ request.requestedPharmacyLocation?.name ?: '' }</td>
                    <td>
                        <span class="po-status ${ request.requestStatus == 'APPROVED' ? 'po-status-approved' : request.requestStatus == 'REJECTED' ? 'po-status-rejected' : 'po-status-requested' }">
                            ${ request.requestStatus ?: '' }
                        </span>
                        <% if (request.requestStatus == 'REJECTED' && request.voidReason) { %>
                        <div class="po-muted">Reason: ${ request.voidReason }</div>
                        <% } %>
                    </td>
                    <td>
                        <% if (request.approvedDate) { %>
                        ${ request.approvedDate.format('yyyy-MM-dd HH:mm') }${ request.requestedPharmacyLocation?.name ? ' - ' + request.requestedPharmacyLocation.name : '' }
                        <% } else { %>
                        <span class="po-muted">Pending review</span>
                        <% } %>
                    </td>
                    <td class="po-history-lines">
                        <% request.lines?.each { line -> %>
                        <div>
                            ${ line.item?.name ?: '' }: ${ line.requestedQuantity ?: 0 }
                            <% if (line.voided == true) { %>
                            <span class="po-removed">Removed${ line.voidReason ? ': ' + line.voidReason : '' }</span>
                            <% } %>
                            <% if (line.approvedQuantity != null) { %>
                            | approved ${ line.approvedQuantity }
                            <% } %>
                        </div>
                        <% } %>
                    </td>
                    <td><a class="po-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'pharmacyorder/historyPdf', [requestId: request.pharmacyItemRequestId]) }">PDF</a></td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <div class="po-pagination">
                <div class="po-muted">Page ${ historyPage ?: 1 } of ${ historyTotalPages ?: 1 } | ${ historyTotalCount ?: 0 } requests</div>
                <div>
                    <% if ((historyPage ?: 1) > 1) { %>
                    <a class="po-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'pharmacyorder/request', [historyPage: (historyPage ?: 1) - 1]) }">Previous</a>
                    <% } %>
                    <% if ((historyPage ?: 1) < (historyTotalPages ?: 1)) { %>
                    <a class="po-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'pharmacyorder/request', [historyPage: (historyPage ?: 1) + 1]) }">Next</a>
                    <% } %>
                </div>
            </div>
            <% } %>
        </div>
    </div>
</div>

<script type="text/javascript">
    var orderStockByPharmacy = {
        <% stockByPharmacy.eachWithIndex { entry, pharmacyIndex -> %>
        "${ entry.key }": [
            <% entry.value.eachWithIndex { itemRow, itemIndex -> %>
            { itemId: "${ itemRow.itemId }", name: "${ itemRow.name?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }", available: "${ itemRow.available ?: 0 }" }${ itemIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ pharmacyIndex + 1 < stockByPharmacy.size() ? ',' : '' }
        <% } %>
    };
    function selectedOrderItem() {
        var sourceId = document.getElementById('po-source').value;
        var itemId = document.getElementById('po-item').value;
        return (orderStockByPharmacy[sourceId] || []).filter(function(item) { return item.itemId === itemId; })[0];
    }
    function refreshOrderItems() {
        var sourceId = document.getElementById('po-source').value;
        var itemSelect = document.getElementById('po-item');
        document.getElementById('po-form-source').value = sourceId;
        itemSelect.innerHTML = '<option value="">Select item</option>';
        (orderStockByPharmacy[sourceId] || []).forEach(function(item) {
            var option = document.createElement('option');
            option.value = item.itemId;
            option.textContent = item.name + ' (' + item.available + ')';
            itemSelect.appendChild(option);
        });
        refreshOrderAvailability();
    }
    function refreshOrderAvailability() {
        var item = selectedOrderItem();
        document.getElementById('po-available').textContent = item ? item.available : '0';
    }
    function addPharmacyOrderLine() {
        var sourceId = document.getElementById('po-source').value;
        var item = selectedOrderItem();
        var qty = document.getElementById('po-qty').value;
        if (!sourceId || !item || !qty || parseFloat(qty) <= 0) { return; }
        var empty = document.getElementById('po-empty-row');
        if (empty) { empty.parentNode.removeChild(empty); }
        var row = document.createElement('tr');
        row.innerHTML = '<td>' + item.name + '<input type="hidden" name="itemId" value="' + item.itemId + '"/></td>'
            + '<td>' + qty + '<input type="hidden" name="quantity" value="' + qty + '"/></td>'
            + '<td><input type="hidden" name="linePharmacyLocationId" value="' + sourceId + '"/>'
            + '<button type="button" class="po-btn" onclick="removePharmacyOrderLine(this)">Remove</button></td>';
        document.getElementById('po-lines-body').appendChild(row);
        document.getElementById('po-source').disabled = true;
        document.getElementById('po-qty').value = '';
    }
    function removePharmacyOrderLine(button) {
        var row = button.closest('tr');
        if (row) { row.parentNode.removeChild(row); }
        var body = document.getElementById('po-lines-body');
        if (!body.querySelector('input[name="itemId"]')) {
            var empty = document.createElement('tr');
            empty.id = 'po-empty-row';
            empty.innerHTML = '<td colspan="3" class="po-muted">No items added yet.</td>';
            body.appendChild(empty);
            document.getElementById('po-source').disabled = false;
            document.getElementById('po-form-source').value = document.getElementById('po-source').value;
        }
    }
    document.getElementById('po-request-form').onsubmit = function() {
        if (!document.getElementById('po-form-source').value || !document.querySelector('#po-lines-body input[name="itemId"]')) {
            alert('Select one pharmacy and add at least one item before submitting.');
            return false;
        }
        return true;
    };
    document.getElementById('po-source').addEventListener('change', refreshOrderItems);
    document.getElementById('po-item').addEventListener('change', refreshOrderAvailability);
</script>
