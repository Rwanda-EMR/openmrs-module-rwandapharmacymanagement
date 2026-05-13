<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def plainUserName = { user ->
        def personName = user?.personName
        def fullName = personName?.fullName?.trim()
        if (fullName) {
            return fullName
        }
        def combinedName = "${personName?.givenName ?: ''} ${personName?.familyName ?: ''}".trim()
        combinedName ?: (user?.username?.trim() ?: '')
    }
%>
<h2>Return to Main Pharmacy</h2>
<style type="text/css">
    .return-shell.is-hidden { display:none; }
    .return-toggle-wrap { margin:0 0 12px; }
    .return-warn { padding:12px; border:1px solid #f0ad4e; background:#fcf8e3; color:#8a6d3b; border-radius:4px; margin-bottom:12px; }
    .return-table-wrap { background:#fff; border:1px solid #ddd; border-radius:3px; overflow:hidden; margin-bottom:12px; }
    .return-table { width:100%; border-collapse:collapse; font-size:12px; }
    .return-table th, .return-table td { border:1px solid #ddd; padding:7px 8px; text-align:left; vertical-align:top; }
    .return-table th { background:#f5f5f5; font-size:11px; color:#666; }
    .return-chip { display:inline-block; padding:2px 6px; border-radius:3px; font-size:10px; font-weight:bold; color:#fff; }
    .return-chip-pending { background:#f0ad4e; }
    .return-chip-approved { background:#5cb85c; }
    .return-chip-rejected { background:#d9534f; }
    .return-chip-served { background:#337ab7; }
    .return-action-row { display:flex; gap:6px; flex-wrap:wrap; align-items:center; }
</style>

<% if (!mainStore) { %>
    <div class="return-warn">No main pharmacy is configured yet. Create one in Pharmacy Locations before using this page.</div>
<% } else if (canRequestReturn) { %>
<div class="return-toggle-wrap">
    <button type="button" id="return-toggle-button" onclick="toggleReturnShell()">Request return from dispensing</button>
</div>
<div class="return-shell is-hidden" id="return-shell">
    <form method="post">
        <p><label>Requesting pharmacy</label><br/><input type="text" value="${ mainStore.name }" readonly="readonly"/></p>
        <p><label>Dispensing pharmacy to return items</label><br/>
            <select name="sourcePharmacyLocationId" id="return-source" required="required">
                <option value="">Select dispensing pharmacy</option>
                <% dispensingPharmacies.each { pharmacy -> %>
                <option value="${ pharmacy.pharmacyLocationId }">${ pharmacy.name }</option>
                <% } %>
            </select>
        </p>
        <div class="return-table-wrap">
            <table id="return-lines" class="return-table">
                <thead><tr><th>Item</th><th>Available in dispensing pharmacy</th><th>Quantity requested</th><th>Unit cost</th><th>Line value</th><th>Batch Number</th><th>Expiry Date</th></tr></thead>
                <tbody>
                <tr>
                    <td><select name="itemId"><option value="">Select item</option></select></td>
                    <td class="return-stock-available"></td>
                    <td><input type="number" step="0.01" name="quantity"/></td>
                    <td class="return-line-unit-price">0</td>
                    <td class="return-line-value">0</td>
                    <td><select name="batchNumber"><option value="">Select batch</option></select></td>
                    <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
                </tr>
                </tbody>
            </table>
        </div>
        <p><button type="button" onclick="addReturnLine()">Add Request Line</button></p>
        <p><label>Reason</label><br/><input type="text" name="reason"/></p>
        <p><button type="submit" name="action" value="requestReturn">Submit return request</button></p>
    </form>
</div>
<% } else { %>
    <div class="return-warn">Only the main pharmacy can request items back from a dispensing pharmacy. Dispensing pharmacies can approve or reject requests assigned to them.</div>
<% } %>

<% if (pharmacySelectionRequired) { %>
    <div class="return-warn">
        <form method="get">
            <strong>Choose dispensing pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' } to approve return requests.
            <select name="pharmacyLocationId" required="required">
                <option value="">Select dispensing pharmacy</option>
                <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
            </select>
            <button type="submit">Use pharmacy</button>
        </form>
    </div>
<% } else if (!activePharmacy) { %>
    <div class="return-warn">No dispensing pharmacy is selected for approvals at ${ pharmacySelectionLocationName ?: 'your current location' }. The table shows all return requests to main.</div>
<% } %>

<h3>Main Pharmacy Return Requests</h3>
<div class="return-table-wrap">
    <table class="return-table">
        <thead><tr><th>ID</th><th>Returning pharmacy</th><th>Destination</th><th>Reason</th><th>Items</th><th>Value</th><th>Status</th><th>Requested By</th><th>Transferred By</th><th>Actions</th></tr></thead>
        <tbody>
        <% if (!requests) { %>
        <tr><td colspan="10">No return requests found.</td></tr>
        <% } else { requests.each { request -> %>
        <tr>
            <td>${ request.interPharmacyTransferId }</td>
            <td>${ request.sourcePharmacyLocation?.name ?: '' }</td>
            <td>${ request.destinationPharmacyLocation?.name ?: '' }</td>
            <td>${ request.reason ?: '' }</td>
            <td>
                <% request.lines?.each { line -> %>
                <div>${ line.item?.name ?: '' }: ${ line.quantity ?: 0 }${ line.batchNumber ? ' - ' + line.batchNumber : '' }</div>
                <% } %>
            </td>
            <td>${ request.lines?.collect { line -> line.totalAmount ?: ((line.unitPrice ?: 0) * (line.quantity ?: 0)) }?.sum() ?: 0 }</td>
            <td>
                <span class="return-chip ${ request.transferStatus == 'APPROVED' ? 'return-chip-approved' : request.transferStatus == 'REJECTED' ? 'return-chip-rejected' : request.transferStatus == 'SERVED' ? 'return-chip-served' : 'return-chip-pending' }">${ request.transferStatus == 'SERVED' ? 'TRANSFERRED' : (request.transferStatus ?: '') }</span>
            </td>
            <td>${ plainUserName(request.creator) } ${ request.dateCreated ? '(' + ui.formatDatePretty(request.dateCreated) + ')' : '' }</td>
            <td>${ plainUserName(request.servedBy) } ${ request.servedDate ? '(' + ui.formatDatePretty(request.servedDate) + ')' : '' }</td>
            <td>
                <div class="return-action-row">
                    <% if (activePharmacy && request.transferStatus == 'REQUESTED') { %>
                    <form method="post"><input type="hidden" name="transferId" value="${ request.interPharmacyTransferId }"/><button type="submit" name="action" value="approve">Approve transfer back</button></form>
                    <form method="post"><input type="hidden" name="transferId" value="${ request.interPharmacyTransferId }"/><input type="text" name="rejectionReason" placeholder="Reason"/><button type="submit" name="action" value="reject">Reject</button></form>
                    <% } %>
                </div>
            </td>
        </tr>
        <% } } %>
        </tbody>
    </table>
</div>

<template id="return-line-template">
    <tr>
        <td><select name="itemId"><option value="">Select item</option></select></td>
        <td class="return-stock-available"></td>
        <td><input type="number" step="0.01" name="quantity"/></td>
        <td class="return-line-unit-price">0</td>
        <td class="return-line-value">0</td>
        <td><select name="batchNumber"><option value="">Select batch</option></select></td>
        <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
    </tr>
</template>

<script type="text/javascript">
    var returnStockByPharmacy = {
        <% stockByPharmacy.eachWithIndex { entry, pharmacyIndex -> %>
        "${ entry.key }": [
            <% entry.value.eachWithIndex { itemRow, itemIndex -> %>
            {
                itemId: "${ itemRow.itemId }",
                name: "${ itemRow.name?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }",
                available: "${ itemRow.available }",
                unitPrice: "${ itemRow.unitPrice ?: 0 }",
                batches: [
                    <% (itemRow.batches ?: []).eachWithIndex { batch, batchIndex -> %>
                    {
                        batchNumber: "${ batch.batchNumber?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }",
                        expiryDate: "${ batch.expiryDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(batch.expiryDate) : '' }",
                        available: "${ batch.available }",
                        unitPrice: "${ batch.unitPrice ?: 0 }"
                    }${ batchIndex + 1 < (itemRow.batches ?: []).size() ? ',' : '' }
                    <% } %>
                ]
            }${ itemIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ pharmacyIndex + 1 < stockByPharmacy.size() ? ',' : '' }
        <% } %>
    };

    function createReturnOption(value, label) {
        var option = document.createElement('option');
        option.value = value;
        option.textContent = label;
        return option;
    }

    function parseReturnNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }

    function formatReturnMoney(value) {
        return parseReturnNumber(value).toFixed(2);
    }

    function selectedReturnItem(row) {
        var sourceId = document.getElementById('return-source').value;
        var itemId = row.querySelector('select[name="itemId"]').value;
        return (returnStockByPharmacy[sourceId] || []).filter(function(item) {
            return item.itemId === itemId;
        })[0];
    }

    function populateReturnItems(row) {
        var sourceId = document.getElementById('return-source').value;
        var itemSelect = row.querySelector('select[name="itemId"]');
        itemSelect.innerHTML = '';
        itemSelect.appendChild(createReturnOption('', 'Select item'));
        (returnStockByPharmacy[sourceId] || []).forEach(function(item) {
            itemSelect.appendChild(createReturnOption(item.itemId, item.name + ' (' + item.available + ')'));
        });
        updateReturnLine(row);
    }

    function updateReturnLine(row) {
        var item = selectedReturnItem(row);
        var batchSelect = row.querySelector('select[name="batchNumber"]');
        var expiryInput = row.querySelector('input[name="expiryDate"]');
        var quantityInput = row.querySelector('input[name="quantity"]');
        var availableCell = row.querySelector('.return-stock-available');
        var unitPriceCell = row.querySelector('.return-line-unit-price');
        var lineValueCell = row.querySelector('.return-line-value');
        var selectedBatchValue = batchSelect.value;

        batchSelect.innerHTML = '';
        batchSelect.appendChild(createReturnOption('', 'Select batch'));
        if (!item) {
            availableCell.textContent = '';
            quantityInput.max = '';
            expiryInput.value = '';
            unitPriceCell.textContent = '0.00';
            lineValueCell.textContent = '0.00';
            return;
        }

        availableCell.textContent = item.available;
        quantityInput.max = item.available;
        var unitPrice = item.unitPrice;
        item.batches.forEach(function(batch) {
            batchSelect.appendChild(createReturnOption(batch.batchNumber || '', (batch.batchNumber || 'No batch') + ' (' + batch.available + ')'));
        });
        batchSelect.value = selectedBatchValue;
        var selectedBatch = item.batches.find(function(batch) {
            return (batch.batchNumber || '') === batchSelect.value;
        });
        if (selectedBatch) {
            availableCell.textContent = selectedBatch.available || item.available;
            quantityInput.max = selectedBatch.available || item.available;
            expiryInput.value = selectedBatch.expiryDate || '';
            unitPrice = selectedBatch.unitPrice;
        } else {
            expiryInput.value = '';
        }
        unitPriceCell.textContent = formatReturnMoney(unitPrice);
        lineValueCell.textContent = formatReturnMoney(parseReturnNumber(unitPrice) * parseReturnNumber(quantityInput.value));
    }

    function bindReturnLine(row) {
        row.querySelector('select[name="itemId"]').addEventListener('change', function() { updateReturnLine(row); });
        row.querySelector('select[name="batchNumber"]').addEventListener('change', function() { updateReturnLine(row); });
        row.querySelector('input[name="quantity"]').addEventListener('input', function() { updateReturnLine(row); });
        populateReturnItems(row);
    }

    function addReturnLine() {
        var tbody = document.getElementById('return-lines').getElementsByTagName('tbody')[0];
        var template = document.getElementById('return-line-template');
        if (template && template.content && template.content.firstElementChild) {
            var row = template.content.firstElementChild.cloneNode(true);
            tbody.appendChild(row);
            bindReturnLine(row);
            if (typeof initializeOpenmrsDateFields === 'function') {
                initializeOpenmrsDateFields(row);
            }
        }
    }

    function toggleReturnShell() {
        var shell = document.getElementById('return-shell');
        var button = document.getElementById('return-toggle-button');
        if (!shell || !button) {
            return;
        }
        var hidden = shell.className.indexOf('is-hidden') >= 0;
        shell.className = hidden ? 'return-shell' : 'return-shell is-hidden';
        button.textContent = hidden ? 'Hide request form' : 'Request return from dispensing';
    }

    var returnSource = document.getElementById('return-source');
    if (returnSource) {
        returnSource.addEventListener('change', function() {
            Array.prototype.slice.call(document.querySelectorAll('#return-lines tbody tr')).forEach(function(row) {
                populateReturnItems(row);
            });
        });
    }
    Array.prototype.slice.call(document.querySelectorAll('#return-lines tbody tr')).forEach(function(row) {
        bindReturnLine(row);
    });
</script>
