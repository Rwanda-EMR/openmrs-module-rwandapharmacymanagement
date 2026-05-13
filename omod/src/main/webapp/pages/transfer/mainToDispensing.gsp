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
<h2>${ ui.message("rwandapharmacymanagement.transfer.mainToDispensing.title") }</h2>
<style type="text/css">
    .transfer-shell.is-hidden { display:none; }
    .transfer-toggle-wrap { margin:0 0 12px; }
    .po-table-wrap {
        background: #fff;
        border: 1px solid #ddd;
        border-radius: 3px;
        overflow: hidden;
        margin-bottom: 12px;
    }
    .po-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 12px;
    }
    .po-table th, .po-table td {
        border: 1px solid #ddd;
        padding: 7px 8px;
        text-align: left;
        vertical-align: top;
    }
    .po-table th {
        background: #f5f5f5;
        font-size: 11px;
        color: #666;
    }
    .po-chip {
        display: inline-block;
        padding: 2px 6px;
        border-radius: 3px;
        font-size: 10px;
        font-weight: bold;
        color: #fff;
    }
    .po-chip-pending { background: #f0ad4e; }
    .po-chip-approved { background: #5cb85c; }
    .po-chip-rejected { background: #d9534f; }
    .po-chip-served { background: #337ab7; }
    .po-action-row {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
        align-items: center;
    }
</style>

<% if (!mainStore) { %>
    <div style="padding:12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;">
        No main pharmacy is configured yet. Create one in Pharmacy Locations before using this page.
    </div>
<% } else if (pharmacySelectionRequired) { %>
    <div style="padding:12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;margin-bottom:12px;">
        <form method="get">
            <strong>Choose dispensing pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.
            <select name="pharmacyLocationId" required="required">
                <option value="">Select dispensing pharmacy</option>
                <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
            </select>
            <button type="submit">Use pharmacy</button>
        </form>
    </div>
<% } else if (!activePharmacy) { %>
    <div style="padding:12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;margin-bottom:12px;">
        No dispensing pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.
    </div>
<% } else { %>
<div class="transfer-toggle-wrap">
    <button type="button" id="main-transfer-toggle-button" onclick="toggleMainTransferShell()">Add new request</button>
</div>
<div class="transfer-shell is-hidden" id="main-transfer-shell">
<form method="post">
    <p><label>Source</label><br/><input type="text" value="${ mainStore.name }" readonly="readonly"/></p>
    <p><label>Destination dispensing pharmacy</label><br/>
        <input type="text" value="${ activePharmacy.name }" readonly="readonly"/>
        <input type="hidden" name="destinationPharmacyLocationId" value="${ activePharmacy.pharmacyLocationId }"/>
    </p>
    <div class="po-table-wrap">
    <table id="main-transfer-lines" class="po-table">
        <thead><tr><th>Item</th><th>Available in ${ mainStore.name }</th><th>Quantity requested</th><th>Unit cost</th><th>Line value</th><th>Batch Number</th><th>Expiry Date</th></tr></thead>
        <tbody>
        <tr>
            <td><select name="itemId"><option value="">Select item</option><% mainStockSummary.each { row -> %><option value="${ row.itemId }">${ row.name } (${ row.available })</option><% } %></select></td>
            <td class="main-stock-available"></td>
            <td><input type="number" step="0.01" name="quantity"/></td>
            <td class="main-line-unit-price">0</td>
            <td class="main-line-value">0</td>
            <td><input type="text" name="batchNumber"/></td>
            <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
        </tr>
        </tbody>
    </table>
    </div>
    <p><button type="button" onclick="addMainTransferLine()">Add Request Line</button></p>
    <p><label>Reason</label><br/><input type="text" name="reason"/></p>
    <p><button type="submit" name="action" value="request">Submit Request</button></p>
</form>
</div>
<% } %>

<h3>Main Store Requests</h3>
<div class="po-table-wrap">
<table class="po-table">
    <thead><tr><th>ID</th><th>Destination</th><th>Value</th><th>Status</th><th>Requested By</th><th>Transferred By</th><th>Actions</th></tr></thead>
    <tbody>
    <% requests.each { request -> %>
    <tr>
        <td>${ request.interPharmacyTransferId }</td>
        <td>${ request.destinationPharmacyLocation?.name ?: '' }</td>
        <td>${ request.lines?.collect { line -> line.totalAmount ?: ((line.unitPrice ?: 0) * (line.quantity ?: 0)) }?.sum() ?: 0 }</td>
        <td>
            <span class="po-chip ${ request.transferStatus == 'APPROVED' ? 'po-chip-approved' : request.transferStatus == 'REJECTED' ? 'po-chip-rejected' : request.transferStatus == 'SERVED' ? 'po-chip-served' : 'po-chip-pending' }">${ request.transferStatus == 'SERVED' ? 'TRANSFERRED' : (request.transferStatus ?: '') }</span>
        </td>
        <td>${ plainUserName(request.creator) } ${ request.dateCreated ? '(' + ui.formatDatePretty(request.dateCreated) + ')' : '' }</td>
        <td>${ plainUserName(request.servedBy) } ${ request.servedDate ? '(' + ui.formatDatePretty(request.servedDate) + ')' : '' }</td>
        <td>
            <div class="po-action-row">
	                <% if (canApproveMainToDispensingTransfer) { %>
	                <a href="${ ui.pageLink('rwandapharmacymanagement', 'transfer/mainToDispensingReview', [transferId: request.interPharmacyTransferId]) }">Review</a>
	                <% } else { %>
	                <span style="color:#777;">Awaiting finance approval</span>
	                <% } %>
            </div>
        </td>
    </tr>
    <% } %>
    </tbody>
</table>
</div>

<template id="main-transfer-line-template">
    <tr>
        <td><select name="itemId"><option value="">Select item</option><% mainStockSummary.each { row -> %><option value="${ row.itemId }">${ row.name } (${ row.available })</option><% } %></select></td>
        <td class="main-stock-available"></td>
        <td><input type="number" step="0.01" name="quantity"/></td>
        <td class="main-line-unit-price">0</td>
        <td class="main-line-value">0</td>
        <td><input type="text" name="batchNumber"/></td>
        <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
    </tr>
</template>
<script type="text/javascript">
    var mainStockByItem = {
        <% mainStockByItem.eachWithIndex { entry, itemIndex -> %>
        "${ entry.key }": [
            <% entry.value.eachWithIndex { batch, batchIndex -> %>
            {
                batchNumber: "${ batch.batchNumber?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }",
                expiryDate: "${ batch.expiryDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(batch.expiryDate) : '' }",
                available: "${ batch.available }",
                unitPrice: "${ batch.unitPrice ?: 0 }"
            }${ batchIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ itemIndex + 1 < mainStockByItem.size() ? ',' : '' }
        <% } %>
    };

    var mainStockSummary = {
        <% mainStockSummary.eachWithIndex { row, idx -> %>
        "${ row.itemId }": "${ row.available }"${ idx + 1 < mainStockSummary.size() ? ',' : '' }
        <% } %>
    };
    var mainStockUnitPrice = {
        <% mainStockSummary.eachWithIndex { row, idx -> %>
        "${ row.itemId }": "${ row.unitPrice ?: 0 }"${ idx + 1 < mainStockSummary.size() ? ',' : '' }
        <% } %>
    };

    function parseMainNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }

    function formatMainMoney(value) {
        return parseMainNumber(value).toFixed(2);
    }

    function updateMainLine(row) {
        var select = row.querySelector('select[name="itemId"]');
        var availableCell = row.querySelector('.main-stock-available');
        var quantityInput = row.querySelector('input[name="quantity"]');
        var unitPriceCell = row.querySelector('.main-line-unit-price');
        var lineValueCell = row.querySelector('.main-line-value');
        var available = select ? (mainStockSummary[select.value] || '') : '';
        var unitPrice = select ? parseMainNumber(mainStockUnitPrice[select.value]) : 0;
        if (availableCell) {
            availableCell.textContent = available;
        }
        if (quantityInput) {
            quantityInput.max = available;
        }
        if (unitPriceCell) {
            unitPriceCell.textContent = formatMainMoney(unitPrice);
        }
        if (lineValueCell) {
            lineValueCell.textContent = formatMainMoney(unitPrice * parseMainNumber(quantityInput ? quantityInput.value : 0));
        }
    }

    function addMainTransferLine() {
        var tbody = document.getElementById('main-transfer-lines').getElementsByTagName('tbody')[0];
        var template = document.getElementById('main-transfer-line-template');
        if (template && template.content && template.content.firstElementChild) {
            var row = template.content.firstElementChild.cloneNode(true);
            tbody.appendChild(row);
            bindMainLine(row);
            if (typeof initializeOpenmrsDateFields === 'function') {
                initializeOpenmrsDateFields(row);
            }
        }
    }

    function bindMainLine(row) {
        var select = row.querySelector('select[name="itemId"]');
        if (select) {
            select.addEventListener('change', function() {
                updateMainLine(row);
            });
        }
        var quantityInput = row.querySelector('input[name="quantity"]');
        if (quantityInput) {
            quantityInput.addEventListener('input', function() {
                updateMainLine(row);
            });
        }
        updateMainLine(row);
    }

    Array.prototype.slice.call(document.querySelectorAll('#main-transfer-lines tbody tr')).forEach(bindMainLine);

    Array.prototype.slice.call(document.querySelectorAll('.serve-main-line')).forEach(function(lineRow) {
        var itemId = lineRow.getAttribute('data-item-id');
        var batchSelect = lineRow.querySelector('select[name="batchNumber"]');
        var expiryInput = lineRow.querySelector('input[name="expiryDate"]');
        var quantityInput = lineRow.querySelector('input[name="quantity"]');
        var availableLabel = lineRow.querySelector('.main-serve-available');
        var batches = mainStockByItem[itemId] || [];

        batchSelect.innerHTML = '';
        batchSelect.appendChild(new Option('Select batch', ''));
        batches.forEach(function(batch) {
            batchSelect.appendChild(new Option((batch.batchNumber || 'No batch') + ' (' + batch.available + ')', batch.batchNumber || ''));
        });

        availableLabel.textContent = 'Available: ' + (mainStockSummary[itemId] || '');
        quantityInput.max = mainStockSummary[itemId] || '';

        batchSelect.addEventListener('change', function() {
            var selectedBatch = batches.find(function(batch) {
                return (batch.batchNumber || '') === batchSelect.value;
            });
            if (selectedBatch) {
                expiryInput.value = selectedBatch.expiryDate || '';
                quantityInput.max = selectedBatch.available || mainStockSummary[itemId] || '';
                availableLabel.textContent = 'Available: ' + (selectedBatch.available || mainStockSummary[itemId] || '');
            } else {
                expiryInput.value = '';
                quantityInput.max = mainStockSummary[itemId] || '';
                availableLabel.textContent = 'Available: ' + (mainStockSummary[itemId] || '');
            }
        });
    });
</script>
<script type="text/javascript">
    function toggleMainTransferShell() {
        var shell = document.getElementById('main-transfer-shell');
        var button = document.getElementById('main-transfer-toggle-button');
        if (!shell || !button) {
            return;
        }
        var hidden = shell.classList.contains('is-hidden');
        if (hidden) {
            shell.classList.remove('is-hidden');
            button.textContent = 'Hide request form';
        } else {
            shell.classList.add('is-hidden');
            button.textContent = 'Add new request';
        }
    }
</script>
