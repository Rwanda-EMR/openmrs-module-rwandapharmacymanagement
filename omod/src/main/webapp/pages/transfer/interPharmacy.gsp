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
<h2>Pharmacy Requisition</h2>
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

<% if (!activePharmacy) { %>
<p>No active dispensing pharmacy is selected for your current location.</p>
<% } %>
<div class="transfer-toggle-wrap">
    <button type="button" id="inter-transfer-toggle-button" onclick="toggleInterTransferShell()">Add new request</button>
</div>
<div class="transfer-shell is-hidden" id="inter-transfer-shell">
<form method="post">
    <p><label>Supplying Pharmacy</label><br/><select name="sourcePharmacyLocationId" id="sourcePharmacyLocationId"><option value="">Select supplying pharmacy</option><% sourcePharmacyLocations.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %></select></p>
    <p><label>Receiving Pharmacy</label><br/><select name="destinationPharmacyLocationId" id="destinationPharmacyLocationId"><% if (receivingPharmacy) { %><option value="${ receivingPharmacy.pharmacyLocationId }" selected="selected">${ receivingPharmacy.name }</option><% } else { %><option value="">No receiving pharmacy selected</option><% } %></select></p>
    <div class="po-table-wrap">
    <table id="inter-transfer-lines" class="po-table">
        <thead><tr><th>Item</th><th>Available in supplying pharmacy</th><th>Quantity requested</th><th>Unit cost</th><th>Line value</th><th>Batch Number</th><th>Expiry Date</th></tr></thead>
        <tbody>
        <tr>
            <td><select name="itemId"><option value="">Select item</option></select></td>
            <td class="source-available"></td>
            <td><input type="number" step="0.01" name="quantity"/></td>
            <td class="inter-line-unit-price">0</td>
            <td class="inter-line-value">0</td>
            <td><select name="batchNumber"><option value="">Select batch</option></select></td>
            <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
        </tr>
        </tbody>
    </table>
    </div>
    <p><button type="button" onclick="addInterTransferLine()">Add Request Line</button></p>
    <p><label>Reason</label><br/><input type="text" name="reason"/></p>
    <p><button type="submit" name="action" value="request" ${ activePharmacy ? '' : 'disabled="disabled"' }>Submit Request</button></p>
</form>
</div>

<h3>Internal Requisitions</h3>
<div class="po-table-wrap">
<table class="po-table">
    <thead><tr><th>ID</th><th>Supplying Pharmacy</th><th>Receiving Pharmacy</th><th>Value</th><th>Status</th><th>Requested By</th><th>Transferred By</th><th>Actions</th></tr></thead>
    <tbody>
    <% requests.each { request -> %>
    <tr>
        <td>${ request.interPharmacyTransferId }</td>
        <td>${ request.sourcePharmacyLocation?.name ?: '' }</td>
        <td>${ request.destinationPharmacyLocation?.name ?: '' }</td>
        <td>${ request.lines?.collect { line -> line.totalAmount ?: ((line.unitPrice ?: 0) * (line.quantity ?: 0)) }?.sum() ?: 0 }</td>
        <td>
            <span class="po-chip ${ request.transferStatus == 'APPROVED' ? 'po-chip-approved' : request.transferStatus == 'REJECTED' ? 'po-chip-rejected' : request.transferStatus == 'SERVED' ? 'po-chip-served' : 'po-chip-pending' }">${ request.transferStatus == 'SERVED' ? 'TRANSFERRED' : (request.transferStatus ?: '') }</span>
        </td>
        <td>${ plainUserName(request.creator) } ${ request.dateCreated ? '(' + ui.formatDatePretty(request.dateCreated) + ')' : '' }</td>
        <td>${ plainUserName(request.servedBy) } ${ request.servedDate ? '(' + ui.formatDatePretty(request.servedDate) + ')' : '' }</td>
        <td>
            <div class="po-action-row">
                <a href="${ ui.pageLink('rwandapharmacymanagement', 'transfer/interPharmacyReview', [transferId: request.interPharmacyTransferId]) }">View</a>
            </div>
        </td>
    </tr>
    <% } %>
    </tbody>
</table>
</div>

<template id="inter-transfer-line-template">
    <tr>
        <td><select name="itemId"><option value="">Select item</option></select></td>
        <td class="source-available"></td>
        <td><input type="number" step="0.01" name="quantity"/></td>
        <td class="inter-line-unit-price">0</td>
        <td class="inter-line-value">0</td>
        <td><select name="batchNumber"><option value="">Select batch</option></select></td>
        <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
    </tr>
</template>
<script type="text/javascript">
    var stockByPharmacy = {
        <% stockByPharmacy.eachWithIndex { entry, pharmacyIndex -> %>
        "${ entry.key }": [
            <% entry.value.eachWithIndex { itemRow, itemIndex -> %>
            {
                itemId: "${ itemRow.itemId }",
                name: "${ itemRow.name?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }",
                available: "${ itemRow.available }",
                unitPrice: "${ itemRow.unitPrice ?: 0 }",
                batches: [
                    <% itemRow.batches.eachWithIndex { batch, batchIndex -> %>
                    {
                        batchNumber: "${ batch.batchNumber?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }",
                        expiryDate: "${ batch.expiryDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(batch.expiryDate) : '' }",
                        available: "${ batch.available }",
                        unitPrice: "${ batch.unitPrice ?: 0 }"
                    }${ batchIndex + 1 < itemRow.batches.size() ? ',' : '' }
                    <% } %>
                ]
            }${ itemIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ pharmacyIndex + 1 < stockByPharmacy.size() ? ',' : '' }
        <% } %>
    };

    function createOption(value, label) {
        var option = document.createElement('option');
        option.value = value;
        option.textContent = label;
        return option;
    }

    function parseInterNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }

    function formatInterMoney(value) {
        return parseInterNumber(value).toFixed(2);
    }

    function updateInterLineCost(row, unitPrice) {
        var quantityInput = row.querySelector('input[name="quantity"]');
        var unitPriceCell = row.querySelector('.inter-line-unit-price');
        var lineValueCell = row.querySelector('.inter-line-value');
        var price = parseInterNumber(unitPrice);
        if (unitPriceCell) {
            unitPriceCell.textContent = formatInterMoney(price);
        }
        if (lineValueCell) {
            lineValueCell.textContent = formatInterMoney(price * parseInterNumber(quantityInput ? quantityInput.value : 0));
        }
    }

    function populateItemsForRow(row) {
        var sourceId = document.getElementById('sourcePharmacyLocationId').value;
        var itemSelect = row.querySelector('select[name="itemId"]');
        itemSelect.innerHTML = '';
        itemSelect.appendChild(createOption('', 'Select item'));
        (stockByPharmacy[sourceId] || []).forEach(function(item) {
            itemSelect.appendChild(createOption(item.itemId, item.name + ' (' + item.available + ')'));
        });
        updateInterRow(row);
    }

    function updateInterRow(row) {
        var sourceId = document.getElementById('sourcePharmacyLocationId').value;
        var itemSelect = row.querySelector('select[name="itemId"]');
        var batchSelect = row.querySelector('select[name="batchNumber"]');
        var expiryInput = row.querySelector('input[name="expiryDate"]');
        var quantityInput = row.querySelector('input[name="quantity"]');
        var availableCell = row.querySelector('.source-available');
        var selectedBatchValue = batchSelect.value;
        var item = (stockByPharmacy[sourceId] || []).find(function(candidate) {
            return candidate.itemId === itemSelect.value;
        });

        batchSelect.innerHTML = '';
        batchSelect.appendChild(createOption('', 'Select batch'));
        if (!item) {
            availableCell.textContent = '';
            quantityInput.max = '';
            expiryInput.value = '';
            updateInterLineCost(row, 0);
            return;
        }

        availableCell.textContent = item.available;
        quantityInput.max = item.available;
        updateInterLineCost(row, item.unitPrice);
        item.batches.forEach(function(batch) {
            var label = (batch.batchNumber || 'No batch') + ' (' + batch.available + ')';
            batchSelect.appendChild(createOption(batch.batchNumber || '', label));
        });
        batchSelect.value = selectedBatchValue;

        var selectedBatch = item.batches.find(function(batch) {
            return (batch.batchNumber || '') === batchSelect.value;
        });
        if (selectedBatch) {
            expiryInput.value = selectedBatch.expiryDate || '';
            quantityInput.max = selectedBatch.available || item.available;
            availableCell.textContent = selectedBatch.available || item.available;
            updateInterLineCost(row, selectedBatch.unitPrice);
        } else {
            expiryInput.value = '';
            updateInterLineCost(row, item.unitPrice);
        }
    }

    function bindInterRow(row) {
        row.querySelector('select[name="itemId"]').addEventListener('change', function() {
            updateInterRow(row);
        });
        row.querySelector('select[name="batchNumber"]').addEventListener('change', function() {
            updateInterRow(row);
        });
        row.querySelector('input[name="quantity"]').addEventListener('input', function() {
            updateInterRow(row);
        });
        populateItemsForRow(row);
    }

    function addInterTransferLine() {
        var tbody = document.getElementById('inter-transfer-lines').getElementsByTagName('tbody')[0];
        var template = document.getElementById('inter-transfer-line-template');
        if (template && template.content && template.content.firstElementChild) {
            var row = template.content.firstElementChild.cloneNode(true);
            tbody.appendChild(row);
            bindInterRow(row);
            if (typeof initializeOpenmrsDateFields === 'function') {
                initializeOpenmrsDateFields(row);
            }
        }
    }

    document.getElementById('sourcePharmacyLocationId').addEventListener('change', function() {
        Array.prototype.slice.call(document.querySelectorAll('#inter-transfer-lines tbody tr')).forEach(function(row) {
            populateItemsForRow(row);
        });
    });

    document.getElementById('destinationPharmacyLocationId').addEventListener('change', function() {
        var sourceId = document.getElementById('sourcePharmacyLocationId');
        if (sourceId.value && sourceId.value === this.value) {
            this.value = '';
        }
    });

    Array.prototype.slice.call(document.querySelectorAll('#inter-transfer-lines tbody tr')).forEach(bindInterRow);

    Array.prototype.slice.call(document.querySelectorAll('.serve-line')).forEach(function(lineRow) {
        var sourceId = lineRow.getAttribute('data-source-id');
        var itemId = lineRow.getAttribute('data-item-id');
        var batchSelect = lineRow.querySelector('select[name="batchNumber"]');
        var expiryInput = lineRow.querySelector('input[name="expiryDate"]');
        var quantityInput = lineRow.querySelector('input[name="quantity"]');
        var availableLabel = lineRow.querySelector('.serve-available');
        var item = (stockByPharmacy[sourceId] || []).find(function(candidate) {
            return candidate.itemId === itemId;
        });
        batchSelect.innerHTML = '';
        batchSelect.appendChild(createOption('', 'Select batch'));
        if (!item) {
            return;
        }
        item.batches.forEach(function(batch) {
            var label = (batch.batchNumber || 'No batch') + ' (' + batch.available + ')';
            batchSelect.appendChild(createOption(batch.batchNumber || '', label));
        });
        availableLabel.textContent = 'Available: ' + item.available;
        quantityInput.max = item.available;
        batchSelect.addEventListener('change', function() {
            var selectedBatch = item.batches.find(function(batch) {
                return (batch.batchNumber || '') === batchSelect.value;
            });
            if (selectedBatch) {
                expiryInput.value = selectedBatch.expiryDate || '';
                quantityInput.max = selectedBatch.available || item.available;
                availableLabel.textContent = 'Available: ' + (selectedBatch.available || item.available);
            } else {
                expiryInput.value = '';
                quantityInput.max = item.available;
                availableLabel.textContent = 'Available: ' + item.available;
            }
        });
    });
</script>
<script type="text/javascript">
    function toggleInterTransferShell() {
        var shell = document.getElementById('inter-transfer-shell');
        var button = document.getElementById('inter-transfer-toggle-button');
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
