<%
    ui.decorateWith("appui", "standardEmrPage", [ title: "Department Bulk Item Request" ])
%>
<h2>Department Bulk Item Request</h2>
<style type="text/css">
    #content { max-width:100%; padding-top:12px; }
    .dept-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .dept-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .dept-body { padding:14px; }
    .dept-table { width:100%; border-collapse:collapse; }
    .dept-table th, .dept-table td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .dept-table th { background:#f8fbfd; }
    .dept-table input, .dept-table select { width:100%; }
    .dept-btn { display:inline-block; padding:7px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; text-decoration:none; }
    .dept-btn-light { background:#fff; color:#1f5f99; }
    .dept-muted { color:#64748b; font-size:12px; }
    .dept-row-available { color:#475569; font-size:12px; }
    .dept-view-panel { padding:12px; border:1px solid #dbe5ec; border-radius:4px; background:#f8fbfd; }
    .dept-view-grid { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:10px; margin-bottom:12px; }
    .dept-view-card { border:1px solid #e2e8f0; border-radius:4px; background:#fff; padding:8px 10px; }
    .dept-view-label { display:block; font-size:10px; text-transform:uppercase; letter-spacing:.04em; color:#64748b; }
    .dept-view-value { display:block; margin-top:3px; color:#0f172a; font-size:12px; }
    .dept-correction-note { border:1px solid #fcd34d; background:#fffbeb; color:#92400e; border-radius:4px; padding:10px 12px; margin-bottom:12px; }
    .dept-view-lines { width:100%; border-collapse:collapse; }
    .dept-view-lines th, .dept-view-lines td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .dept-view-lines th { background:#eef5fb; color:#475569; }
    .dept-detail-row.is-hidden { display:none; }
    @media (max-width: 1100px) {
        .dept-view-grid { grid-template-columns:1fr; }
    }
</style>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "Department Bulk Item Request" }
    ];
</script>

<div class="dept-card">
    <div class="dept-head">New request</div>
    <div class="dept-body">
        <% if (!departmentLocations) { %>
        <p class="dept-muted">No department store is configured. Create a location with type Department Store first.</p>
        <% } else if (!supplyingPharmacies) { %>
        <p class="dept-muted">No supplying pharmacy is configured. Create or activate a main pharmacy/dispensing pharmacy first.</p>
        <% } else { %>
        <p class="dept-muted">Workflow mode: ${ workflowMode == 'FULL_VISIBILITY' ? 'Full Visibility' : 'Periodic Accountability' }</p>
        <form method="post">
            <p>
                <label>Department store</label><br/>
                <select name="departmentLocationId" required="required" onchange="refreshDepartmentAccountability()">
                    <% departmentLocations.each { location -> %><option value="${ location.pharmacyLocationId }">${ location.name }</option><% } %>
                </select>
            </p>
            <p>
                <label>Request items from</label><br/>
                <select name="supplyingPharmacyLocationId" id="dept-source-pharmacy" required="required" onchange="refreshDepartmentItems(true)">
                    <option value="">Select pharmacy</option>
                    <% supplyingPharmacies.each { location ->
                        def sourceType = location.mainStore ? 'Main pharmacy' : (location.locationType?.toString() == 'DISPENSING_PHARMACY' ? 'Dispensing pharmacy' : (location.locationType ?: 'Pharmacy'))
                    %><option value="${ location.pharmacyLocationId }">${ location.name } (${ sourceType })</option><% } %>
                </select>
                <br/><span class="dept-muted">This list includes the main pharmacy and any active non-department pharmacy locations.</span>
            </p>
            <table class="dept-table" id="dept-request-lines">
                <thead>
                <tr>
                    <th>Item</th>
                    <th>Stock at Last Request</th>
                    <th>Quantity Received Since</th>
                    <th>Quantity Used Since</th>
                    <th>Current Remainder</th>
                    <th>Available</th>
                    <th>Quantity Requested Now</th>
                    <th>Justification</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td><select name="itemId" class="dept-item-select" onchange="refreshDepartmentLine(this)"><option value="">Select pharmacy first</option></select></td>
                    <td><input type="number" step="0.01" min="0" name="previousRequestedQuantity" oninput="recalculateDepartmentRemainder(this)"/></td>
                    <td><input type="number" step="0.01" min="0" name="previousReceivedQuantity" oninput="recalculateDepartmentRemainder(this)"/></td>
                    <td><input type="number" step="0.01" min="0" name="previousUsedQuantity" oninput="recalculateDepartmentRemainder(this)"/></td>
                    <td><input type="number" step="0.01" min="0" name="currentRemainingQuantity" readonly="readonly"/></td>
                    <td><div class="dept-row-available">Select item</div></td>
                    <td><input type="number" step="0.01" min="0.01" name="requestedQuantity"/></td>
                    <td><input type="text" name="lineJustification"/></td>
                    <td><button type="button" onclick="removeLine(this)">Remove</button></td>
                </tr>
                </tbody>
            </table>
            <p><button type="button" onclick="addLine()">Add item</button></p>
            <p><label>Overall justification</label><br/><textarea name="justification" rows="3" style="width:100%;"></textarea></p>
            <p><button type="submit" class="dept-btn">Submit for finance approval</button></p>
        </form>
        <% } %>
    </div>
</div>

<div class="dept-card">
    <div class="dept-head">My recent requests</div>
    <div class="dept-body">
        <table class="dept-table">
            <thead><tr><th>ID</th><th>Department</th><th>Supplying pharmacy</th><th>Status</th><th>Mode</th><th>Requested</th><th>Items</th><th>Correction</th><th>Actions</th><th>Download</th></tr></thead>
            <tbody>
            <% if (!recentRequests) { %>
            <tr><td colspan="10" class="dept-muted">No department requests found.</td></tr>
            <% } else { recentRequests.each { request -> %>
            <tr>
                <td>${ request.departmentBulkRequestId }</td>
                <td>${ request.departmentLocation?.name ?: '' }</td>
                <td>${ request.supplyingPharmacyLocation?.name ?: '' }</td>
                <td>${ request.requestStatus ?: '' }</td>
                <td>${ request.workflowMode ?: '' }</td>
                <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                <td><% request.lines?.each { line -> %><div>${ line.item?.name ?: '' }: ${ line.requestedQuantity ?: 0 }</div><% } %></td>
                <td>${ request.correctionNote ?: '' }</td>
                <td><button type="button" class="dept-btn dept-btn-light" onclick="toggleDepartmentRequestDetails('${ request.departmentBulkRequestId }')">View</button></td>
                <td>
                    <% if (request.requestStatus == 'DISPENSED') { %>
                    <a class="dept-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'department/transferPdf', [requestId: request.departmentBulkRequestId]) }">PDF</a>
                    <% } else { %>
                    <span class="dept-muted">Available after transfer</span>
                    <% } %>
                </td>
            </tr>
            <tr id="dept-request-details-${ request.departmentBulkRequestId }" class="dept-detail-row is-hidden">
                <td colspan="10">
                    <div class="dept-view-panel">
                        <% if (request.correctionNote) { %>
                        <div class="dept-correction-note"><strong>Correction reason:</strong> ${ request.correctionNote }</div>
                        <% } %>
                        <div class="dept-view-grid">
                            <div class="dept-view-card"><span class="dept-view-label">Request ID</span><span class="dept-view-value">${ request.departmentBulkRequestId }</span></div>
                            <div class="dept-view-card"><span class="dept-view-label">Department</span><span class="dept-view-value">${ request.departmentLocation?.name ?: '' }</span></div>
                            <div class="dept-view-card"><span class="dept-view-label">Supplying pharmacy</span><span class="dept-view-value">${ request.supplyingPharmacyLocation?.name ?: '' }</span></div>
                            <div class="dept-view-card"><span class="dept-view-label">Status</span><span class="dept-view-value">${ request.requestStatus ?: '' }</span></div>
                            <div class="dept-view-card"><span class="dept-view-label">Workflow mode</span><span class="dept-view-value">${ request.workflowMode ?: '' }</span></div>
                            <div class="dept-view-card"><span class="dept-view-label">Requested date</span><span class="dept-view-value">${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</span></div>
                            <div class="dept-view-card"><span class="dept-view-label">Finance approved by</span><span class="dept-view-value">${ request.financeApprovedBy ?: '' }</span></div>
                            <div class="dept-view-card"><span class="dept-view-label">Finance approved date</span><span class="dept-view-value">${ request.financeApprovedDate ? ui.formatDatePretty(request.financeApprovedDate) : '' }</span></div>
                        </div>
                        <% if (request.justification) { %>
                        <div class="dept-view-card" style="margin-bottom:12px;">
                            <span class="dept-view-label">Overall justification</span>
                            <span class="dept-view-value">${ request.justification }</span>
                        </div>
                        <% } %>
                        <table class="dept-view-lines">
                            <thead>
                            <tr>
                                <th>Item</th>
                                <th>Requested</th>
                                <th>Approved</th>
                                <th>Dispensed</th>
                                <th>Stock at last request</th>
                                <th>Received since</th>
                                <th>Used since</th>
                                <th>Current remainder</th>
                                <th>Line justification</th>
                                <th>Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% request.lines?.each { line -> %>
                            <tr>
                                <td>${ line.item?.name ?: '' }</td>
                                <td>${ line.requestedQuantity ?: 0 }</td>
                                <td>${ line.approvedQuantity ?: '' }</td>
                                <td>${ line.dispensedQuantity ?: '' }</td>
                                <td>${ line.previousRequestedQuantity ?: 0 }</td>
                                <td>${ line.previousReceivedQuantity ?: 0 }</td>
                                <td>${ line.previousUsedQuantity ?: 0 }</td>
                                <td>${ line.currentRemainingQuantity ?: 0 }</td>
                                <td>${ line.lineJustification ?: '' }</td>
                                <td>${ line.voided ? 'Removed: ' + (line.voidReason ?: '') : 'Active' }</td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </td>
            </tr>
            <% } } %>
            </tbody>
        </table>
    </div>
</div>

<script type="text/javascript">
    var departmentStockByPharmacy = {
        <% stockByPharmacy.eachWithIndex { entry, pharmacyIndex -> %>
        "${ entry.key }": [
            <% entry.value.eachWithIndex { itemRow, itemIndex -> %>
            { itemId: "${ itemRow.itemId }", name: "${ itemRow.name?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }", available: "${ itemRow.available ?: 0 }" }${ itemIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ pharmacyIndex + 1 < stockByPharmacy.size() ? ',' : '' }
        <% } %>
    };
    var departmentAccountability = {
        <% accountabilityByDepartment.eachWithIndex { deptEntry, deptIndex -> %>
        "${ deptEntry.key }": {
            <% deptEntry.value.eachWithIndex { itemEntry, itemIndex -> %>
            "${ itemEntry.key }": { stockAtLastRequest: "${ itemEntry.value.stockAtLastRequest ?: 0 }", receivedSince: "${ itemEntry.value.receivedSince ?: 0 }", usedSince: "${ itemEntry.value.usedSince ?: 0 }" }${ itemIndex + 1 < deptEntry.value.size() ? ',' : '' }
            <% } %>
        }${ deptIndex + 1 < accountabilityByDepartment.size() ? ',' : '' }
        <% } %>
    };

    function selectedDepartmentSourceItems() {
        var source = document.getElementById('dept-source-pharmacy');
        return source ? (departmentStockByPharmacy[source.value] || []) : [];
    }

    function toggleDepartmentRequestDetails(requestId) {
        var row = document.getElementById('dept-request-details-' + requestId);
        if (!row) {
            return;
        }
        row.classList.toggle('is-hidden');
    }

    function refreshDepartmentItems(clearSelection) {
        Array.prototype.slice.call(document.querySelectorAll('.dept-item-select')).forEach(function(select) {
            var previous = clearSelection ? '' : select.value;
            populateDepartmentItemOptions(select, previous);
            refreshDepartmentLine(select);
        });
    }

    function populateDepartmentItemOptions(select, selectedValue) {
        select.innerHTML = '<option value="">Select item</option>';
        selectedDepartmentSourceItems().forEach(function(item) {
            var option = document.createElement('option');
            option.value = item.itemId;
            option.textContent = item.name + ' (' + item.available + ' available)';
            option.setAttribute('data-available', item.available);
            select.appendChild(option);
        });
        select.value = selectedValue || '';
        if (select.value !== selectedValue) {
            select.value = '';
        }
    }

    function refreshDepartmentLine(select) {
        refreshDepartmentAvailability(select);
        populateDepartmentAccountability(select);
        recalculateDepartmentRemainder(select);
    }

    function refreshDepartmentAvailability(select) {
        var row = select.closest('tr');
        var availableBox = row ? row.querySelector('.dept-row-available') : null;
        var quantityInput = row ? row.querySelector('input[name="requestedQuantity"]') : null;
        var option = select.options[select.selectedIndex];
        var available = option ? option.getAttribute('data-available') : '';
        if (availableBox) {
            availableBox.textContent = available ? available + ' available' : 'Select item';
        }
        if (quantityInput) {
            quantityInput.max = available || '';
        }
    }

    function populateDepartmentAccountability(select) {
        var departmentSelect = document.querySelector('select[name="departmentLocationId"]');
        var departmentId = departmentSelect ? departmentSelect.value : '';
        var itemId = select ? select.value : '';
        var values = departmentAccountability[departmentId] && departmentAccountability[departmentId][itemId]
            ? departmentAccountability[departmentId][itemId]
            : null;
        var row = select.closest('tr');
        var stockAtLastRequest = row.querySelector('input[name="previousRequestedQuantity"]');
        var receivedSince = row.querySelector('input[name="previousReceivedQuantity"]');
        var usedSince = row.querySelector('input[name="previousUsedQuantity"]');
        if (stockAtLastRequest) {
            stockAtLastRequest.value = values ? (values.stockAtLastRequest || '0') : '';
        }
        if (receivedSince) {
            receivedSince.value = values ? (values.receivedSince || '0') : '';
        }
        if (usedSince) {
            usedSince.value = values ? (values.usedSince || '0') : '';
        }
    }

    function refreshDepartmentAccountability() {
        Array.prototype.slice.call(document.querySelectorAll('.dept-item-select')).forEach(function(select) {
            populateDepartmentAccountability(select);
            recalculateDepartmentRemainder(select);
        });
    }

    function recalculateDepartmentRemainder(element) {
        var row = element.closest('tr');
        if (!row) {
            return;
        }
        var stockAtLastRequest = parseNumber(row.querySelector('input[name="previousRequestedQuantity"]').value);
        var receivedSince = parseNumber(row.querySelector('input[name="previousReceivedQuantity"]').value);
        var usedSince = parseNumber(row.querySelector('input[name="previousUsedQuantity"]').value);
        var remainder = Math.max(0, stockAtLastRequest + receivedSince - usedSince);
        row.querySelector('input[name="currentRemainingQuantity"]').value = formatNumber(remainder);
    }

    function parseNumber(value) {
        var parsed = parseFloat(value);
        return isNaN(parsed) ? 0 : parsed;
    }

    function formatNumber(value) {
        return Math.round(value * 100) / 100;
    }

    function addLine() {
        var tbody = document.querySelector('#dept-request-lines tbody');
        var first = tbody.querySelector('tr');
        var clone = first.cloneNode(true);
        Array.prototype.slice.call(clone.querySelectorAll('input')).forEach(function(input) { input.value = ''; });
        Array.prototype.slice.call(clone.querySelectorAll('select')).forEach(function(select) {
            populateDepartmentItemOptions(select, '');
            refreshDepartmentLine(select);
        });
        tbody.appendChild(clone);
    }
    function removeLine(button) {
        var tbody = document.querySelector('#dept-request-lines tbody');
        if (tbody.querySelectorAll('tr').length > 1) {
            button.closest('tr').remove();
        }
    }
</script>
