<%
    ui.decorateWith("appui", "standardEmrPage", [ title: "Department Store" ])
%>
<h2>Department Store</h2>
<style type="text/css">
    .dept-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .dept-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .dept-body { padding:14px; }
    .dept-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
    .dept-table { width:100%; border-collapse:collapse; margin-bottom:10px; }
    .dept-table th, .dept-table td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .dept-table th { background:#f8fbfd; }
    .dept-table-scroll { overflow-x:auto; width:100%; }
    .dept-request-table { min-width:1050px; }
    .dept-request-table select { max-width:280px; }
    .dept-request-table input { max-width:100%; box-sizing:border-box; }
    .dept-btn { display:inline-block; padding:7px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; }
    .dept-btn-secondary { background:#fff; color:#1f5f99; }
    .dept-btn-danger { background:#fff1f2; border-color:#fecdd3; color:#be123c; }
    .dept-muted { color:#64748b; font-size:12px; }
    .dept-banner { padding:10px 12px; border-radius:4px; margin-bottom:14px; border:1px solid #fde68a; background:#fffbeb; color:#92400e; }
    .dept-input-sm { width:86px; }
    .dept-full { width:100%; }
</style>

<% if (departmentCandidates && departmentCandidates.size() > 1) { %>
<div class="dept-card">
    <div class="dept-head">Choose department store</div>
    <div class="dept-body">
        <form method="get">
            <label>More than one department store is linked to your current OpenMRS location/tag.</label><br/>
            <select name="departmentLocationId" required="required">
                <option value="">Select department store</option>
                <% departmentCandidates.each { candidate -> %>
                <option value="${ candidate.pharmacyLocationId }" ${ department?.pharmacyLocationId == candidate.pharmacyLocationId ? 'selected="selected"' : '' }>${ candidate.name }</option>
                <% } %>
            </select>
            <button type="submit" class="dept-btn">Use department</button>
        </form>
    </div>
</div>
<% } %>

<% if (!department) { %>
<div class="dept-banner">
    <% if (departmentSelectionRequired) { %>
    Choose the department store before using this workspace.
    <% } else { %>
    Your current OpenMRS location is not linked to an active Department Store pharmacy location.
    Configure the department store with this OpenMRS location or matching location tag.
    <% } %>
</div>
<% } else { %>
<div class="dept-card">
    <div class="dept-head">Workspace: ${ department.name }</div>
    <div class="dept-body dept-muted">
        This page is limited to the department store matched to your current OpenMRS location.
    </div>
</div>

<div class="dept-grid">
    <div class="dept-card">
        <div class="dept-head">Request items from pharmacy</div>
        <div class="dept-body">
            <form method="post">
                <input type="hidden" name="action" value="request"/>
                <input type="hidden" name="departmentLocationId" value="${ department.pharmacyLocationId }"/>
                <p>
                    <label>Request items from</label><br/>
                    <select name="supplyingPharmacyLocationId" id="dept-source-pharmacy" required="required" onchange="refreshDepartmentItems(true)">
                        <option value="">Select pharmacy</option>
                        <% supplyingPharmacies.each { location ->
                            def sourceType = location.mainStore ? 'Main pharmacy' : (location.locationType?.toString() == 'DISPENSING_PHARMACY' ? 'Dispensing pharmacy' : (location.locationType ?: 'Pharmacy'))
                        %><option value="${ location.pharmacyLocationId }">${ location.name } (${ sourceType })</option><% } %>
                    </select>
                </p>
                <div class="dept-table-scroll">
                    <table class="dept-table dept-request-table" id="dept-request-lines">
                        <thead>
                        <tr><th>Item</th><th>Available</th><th>Previous stock</th><th>Received</th><th>Used</th><th>Current remaining</th><th>Request qty</th><th>Reason</th><th></th></tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td><select name="itemId" class="dept-item-select" onchange="refreshDepartmentLine(this)"><option value="">Select pharmacy first</option></select></td>
                            <td class="dept-row-available dept-muted">Select item</td>
                            <td><input class="dept-input-sm" type="number" step="0.01" min="0" name="previousRequestedQuantity" oninput="recalculateDepartmentRemainder(this)"/></td>
                            <td><input class="dept-input-sm" type="number" step="0.01" min="0" name="previousReceivedQuantity" oninput="recalculateDepartmentRemainder(this)"/></td>
                            <td><input class="dept-input-sm" type="number" step="0.01" min="0" name="previousUsedQuantity" oninput="recalculateDepartmentRemainder(this)"/></td>
                            <td><input class="dept-input-sm" type="number" step="0.01" min="0" name="currentRemainingQuantity" readonly="readonly"/></td>
                            <td><input class="dept-input-sm" type="number" step="0.01" min="0.01" name="requestedQuantity" required="required"/></td>
                            <td><input type="text" name="lineJustification"/></td>
                            <td><button type="button" class="dept-btn dept-btn-danger" onclick="removeRequestLine(this)">Remove</button></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <p><button type="button" class="dept-btn dept-btn-secondary" onclick="addRequestLine()">Add another item</button></p>
                <p><label>General justification</label><br/><textarea name="justification" class="dept-full"></textarea></p>
                <p><button type="submit" class="dept-btn">Submit request</button></p>
            </form>
        </div>
    </div>

    <div class="dept-card">
        <div class="dept-head">Issue stock internally</div>
        <div class="dept-body">
            <% if (workflowMode != 'FULL_VISIBILITY') { %>
            <div class="dept-muted">Internal issue recording is disabled because the current mode is Periodic Accountability.</div>
            <% } else { %>
            <form method="post">
                <input type="hidden" name="action" value="issue"/>
                <input type="hidden" name="departmentLocationId" value="${ department.pharmacyLocationId }"/>
                <table class="dept-table">
                    <thead><tr><th>Item/batch</th><th>Available</th><th>Quantity</th><th></th></tr></thead>
                    <tbody id="issue-lines">
                    <tr class="issue-line">
                        <td>
                            <select name="issueItemId" required="required" onchange="syncIssueLine(this)">
                                <option value="">Select available stock</option>
                                <% stockBalances.each { balance -> %>
                                <option value="${ balance.item?.itemId }" data-batch="${ balance.batchNumber ?: '' }" data-available="${ balance.quantityOnHand ?: 0 }">${ balance.item?.name ?: '' } - ${ balance.quantityOnHand ?: 0 } ${ balance.batchNumber ? '(' + balance.batchNumber + ')' : '' }</option>
                                <% } %>
                            </select>
                            <input type="hidden" name="issueBatchNumber"/>
                        </td>
                        <td class="line-available dept-muted">0</td>
                        <td><input class="dept-input-sm" type="number" step="0.01" min="0.01" name="issueQuantity" required="required"/></td>
                        <td><button type="button" class="dept-btn dept-btn-danger" onclick="removeIssueLine(this)">Remove</button></td>
                    </tr>
                    </tbody>
                </table>
                <p><button type="button" class="dept-btn dept-btn-secondary" onclick="addIssueLine()">Add another item</button></p>
                <p><label>Issued to</label><br/><input type="text" name="issuedTo"/></p>
                <p><label>Reason/use</label><br/><input type="text" name="reason" class="dept-full"/></p>
                <p><button type="submit" class="dept-btn">Record issue</button></p>
            </form>
            <% } %>
        </div>
    </div>
</div>

<div class="dept-grid">
    <div class="dept-card">
        <div class="dept-head">Current department stock</div>
        <div class="dept-body">
            <table class="dept-table">
                <thead><tr><th>Item</th><th>Batch</th><th>Expiry</th><th>Quantity</th></tr></thead>
                <tbody>
                <% if (!stockBalances) { %><tr><td colspan="4" class="dept-muted">No stock available.</td></tr><% } else { stockBalances.each { balance -> %>
                <tr><td>${ balance.item?.name ?: '' }</td><td>${ balance.batchNumber ?: '' }</td><td>${ balance.expiryDate ? ui.formatDatePretty(balance.expiryDate) : '' }</td><td>${ balance.quantityOnHand ?: 0 }</td></tr>
                <% } } %>
                </tbody>
            </table>
        </div>
    </div>

    <div class="dept-card">
        <div class="dept-head">Recent internal issues</div>
        <div class="dept-body">
            <table class="dept-table">
                <thead><tr><th>Date</th><th>Item</th><th>Batch</th><th>Qty</th><th>Issued to</th><th>Reason</th></tr></thead>
                <tbody>
                <% if (!recentIssues) { %><tr><td colspan="6" class="dept-muted">No recent issues.</td></tr><% } else { recentIssues.each { issue -> %>
                <tr><td>${ issue.dateCreated ? ui.formatDatePretty(issue.dateCreated) : '' }</td><td>${ issue.item?.name ?: '' }</td><td>${ issue.batchNumber ?: '' }</td><td>${ issue.quantity ?: 0 }</td><td>${ issue.issuedTo ?: '' }</td><td>${ issue.reason ?: '' }</td></tr>
                <% } } %>
                </tbody>
            </table>
        </div>
    </div>
</div>

<div class="dept-card">
    <div class="dept-head">My department requests</div>
    <div class="dept-body">
        <table class="dept-table">
            <thead><tr><th>ID</th><th>Date</th><th>Supplying pharmacy</th><th>Status</th><th>Items</th><th>Download</th></tr></thead>
            <tbody>
            <% if (!departmentRequests) { %><tr><td colspan="6" class="dept-muted">No department requests found.</td></tr><% } else { departmentRequests.each { request -> %>
            <tr>
                <td>${ request.departmentBulkRequestId }</td>
                <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                <td>${ request.supplyingPharmacyLocation?.name ?: '' }</td>
                <td>${ request.requestStatus ?: '' }</td>
                <td>${ request.lines?.findAll { it.voided != true }?.size() ?: 0 }</td>
                <td>
                    <% if (request.requestStatus == 'DISPENSED') { %>
                    <a class="dept-btn dept-btn-secondary" href="${ ui.pageLink('rwandapharmacymanagement', 'department/transferPdf', [requestId: request.departmentBulkRequestId]) }">PDF</a>
                    <% } %>
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
        <% accountabilityByItem.eachWithIndex { itemEntry, itemIndex -> %>
        "${ itemEntry.key }": { stockAtLastRequest: "${ itemEntry.value.stockAtLastRequest ?: 0 }", receivedSince: "${ itemEntry.value.receivedSince ?: 0 }", usedSince: "${ itemEntry.value.usedSince ?: 0 }" }${ itemIndex + 1 < accountabilityByItem.size() ? ',' : '' }
        <% } %>
    };
    function selectedDepartmentSourceItems() {
        var source = document.getElementById('dept-source-pharmacy');
        return source ? (departmentStockByPharmacy[source.value] || []) : [];
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
        var itemId = select ? select.value : '';
        var values = departmentAccountability[itemId] || null;
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
    function recalculateDepartmentRemainder(element) {
        var row = element.closest('tr');
        if (!row) {
            return;
        }
        var stockAtLastRequest = parseNumber(row.querySelector('input[name="previousRequestedQuantity"]').value);
        var receivedSince = parseNumber(row.querySelector('input[name="previousReceivedQuantity"]').value);
        var usedSince = parseNumber(row.querySelector('input[name="previousUsedQuantity"]').value);
        row.querySelector('input[name="currentRemainingQuantity"]').value = formatNumber(Math.max(0, stockAtLastRequest + receivedSince - usedSince));
    }
    function parseNumber(value) {
        var parsed = parseFloat(value);
        return isNaN(parsed) ? 0 : parsed;
    }
    function formatNumber(value) {
        return Math.round(value * 100) / 100;
    }
    function addRequestLine() {
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
    function removeRequestLine(button) {
        var tbody = document.querySelector('#dept-request-lines tbody');
        if (tbody.querySelectorAll('tr').length > 1) {
            button.closest('tr').remove();
        }
    }
    function syncIssueLine(select) {
        var row = closestIssueRow(select);
        var selected = select.options[select.selectedIndex];
        var batchInput = row ? row.querySelector('input[name="issueBatchNumber"]') : null;
        var availableCell = row ? row.querySelector('.line-available') : null;
        var quantity = row ? row.querySelector('input[name="issueQuantity"]') : null;
        if (batchInput) {
            batchInput.value = selected ? (selected.getAttribute('data-batch') || '') : '';
        }
        if (availableCell) {
            availableCell.innerHTML = selected ? (selected.getAttribute('data-available') || '0') : '0';
        }
        if (quantity) {
            quantity.max = selected ? (selected.getAttribute('data-available') || '') : '';
        }
    }
    function addIssueLine() {
        var body = document.getElementById('issue-lines');
        var firstLine = body ? body.querySelector('.issue-line') : null;
        if (!firstLine) {
            return;
        }
        var newLine = firstLine.cloneNode(true);
        var select = newLine.querySelector('select[name="issueItemId"]');
        var quantity = newLine.querySelector('input[name="issueQuantity"]');
        var batchInput = newLine.querySelector('input[name="issueBatchNumber"]');
        var availableCell = newLine.querySelector('.line-available');
        if (select) {
            select.selectedIndex = 0;
        }
        if (quantity) {
            quantity.value = '';
            quantity.max = '';
        }
        if (batchInput) {
            batchInput.value = '';
        }
        if (availableCell) {
            availableCell.innerHTML = '0';
        }
        body.appendChild(newLine);
    }
    function removeIssueLine(button) {
        var row = closestIssueRow(button);
        var body = document.getElementById('issue-lines');
        if (!row || !body) {
            return;
        }
        if (body.querySelectorAll('.issue-line').length <= 1) {
            var select = row.querySelector('select[name="issueItemId"]');
            var quantity = row.querySelector('input[name="issueQuantity"]');
            var batchInput = row.querySelector('input[name="issueBatchNumber"]');
            var availableCell = row.querySelector('.line-available');
            if (select) {
                select.selectedIndex = 0;
            }
            if (quantity) {
                quantity.value = '';
                quantity.max = '';
            }
            if (batchInput) {
                batchInput.value = '';
            }
            if (availableCell) {
                availableCell.innerHTML = '0';
            }
            return;
        }
        body.removeChild(row);
    }
    function closestIssueRow(element) {
        while (element && !element.classList.contains('issue-line')) {
            element = element.parentNode;
        }
        return element;
    }
</script>
<% } %>
