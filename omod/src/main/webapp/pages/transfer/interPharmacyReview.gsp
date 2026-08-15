<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage", [pageTitle: "Dispensing To Dispensing Review"])
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
<style>
    .review-shell {
        background: rgba(0, 0, 0, 0.04);
        padding: 20px 10px;
        border-radius: 4px;
    }
    .review-card {
        max-width: 860px;
        margin: 0 auto;
        background: #fff;
        border: 1px solid #d8d8d8;
        border-radius: 4px;
        overflow: hidden;
        box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
    }
    .review-header {
        background: #2f6fad;
        color: #fff;
        padding: 12px 16px;
        font-size: 13px;
        font-weight: bold;
    }
    .review-body {
        padding: 14px 16px 16px;
        max-height: calc(100vh - 180px);
        overflow: auto;
    }
    .review-meta-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 10px;
        margin-bottom: 14px;
    }
    .review-meta-card {
        border: 1px solid #ddd;
        background: #fafafa;
        padding: 10px;
        min-height: 72px;
    }
    .review-meta-label {
        font-size: 10px;
        color: #888;
        text-transform: uppercase;
        margin-bottom: 4px;
    }
    .review-meta-value {
        font-size: 14px;
        font-weight: bold;
        color: #333;
    }
    .review-status-pill {
        display: inline-block;
        padding: 3px 8px;
        border-radius: 3px;
        font-size: 11px;
        font-weight: bold;
        color: #fff;
        background: #f0ad4e;
    }
    .review-status-approved { background: #5cb85c; }
    .review-status-rejected { background: #d9534f; }
    .review-status-served { background: #337ab7; }
    .review-table {
        width: 100%;
        border-collapse: collapse;
        margin-bottom: 14px;
        font-size: 12px;
    }
    .review-table-wrap {
        overflow: auto;
    }
    .review-table th, .review-table td {
        border: 1px solid #ddd;
        padding: 7px 8px;
        vertical-align: top;
    }
    .review-table th {
        background: #f5f5f5;
        color: #666;
        font-size: 11px;
    }
    .review-table input[type='number'] { width: 86px; padding: 4px 6px; }
    .review-timeline {
        border-top: 1px solid #e5e5e5;
        padding-top: 10px;
        margin-top: 8px;
        color: #666;
        font-size: 12px;
    }
    .review-action-bar {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
        margin-top: 14px;
        align-items: center;
    }
    .review-item-title { display:block; color:#222; font-weight:bold; margin-bottom:5px; }
    .review-remove-link { background:#fff1f2; border:1px solid #fecdd3; border-radius:999px; color:#be123c; cursor:pointer; font-size:11px; padding:5px 9px; text-decoration:none; white-space:nowrap; }
    .review-remove-link:hover, .review-remove-link:focus { background:#ffe4e6; color:#9f1239; }
    .review-line-remove { display:flex; gap:6px; align-items:center; margin-top:6px; }
    .review-line-remove input { width:155px; border:1px solid #cbd5e1; border-radius:4px; font-size:12px; padding:5px 7px; }
</style>

<h2>Dispensing To Dispensing Review</h2>
<% if (!transfer) { %>
<div style="padding:12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;">Transfer request not found.</div>
<% } else { %>
<div class="review-shell">
    <div class="review-card">
        <div class="review-header">Request DTD-${ transfer.interPharmacyTransferId }</div>
        <div class="review-body">
            <div class="review-meta-grid">
                <div class="review-meta-card"><div class="review-meta-label">Source</div><div class="review-meta-value">${ transfer.sourcePharmacyLocation?.name ?: '' }</div></div>
                <div class="review-meta-card"><div class="review-meta-label">Destination</div><div class="review-meta-value">${ transfer.destinationPharmacyLocation?.name ?: '' }</div></div>
                <div class="review-meta-card"><div class="review-meta-label">Status</div><div class="review-meta-value"><span class="review-status-pill ${ transfer.transferStatus == 'APPROVED' ? 'review-status-approved' : transfer.transferStatus == 'REJECTED' ? 'review-status-rejected' : transfer.transferStatus == 'SERVED' ? 'review-status-served' : '' }">${ transfer.transferStatus == 'SERVED' ? 'TRANSFERRED' : (transfer.transferStatus ?: 'REQUESTED') }</span></div></div>
                <div class="review-meta-card"><div class="review-meta-label">Reason</div><div class="review-meta-value">${ transfer.reason ?: '' }</div></div>
            </div>

            <form method="post">
                <input type="hidden" name="transferId" value="${ transfer.interPharmacyTransferId }"/>
                <input type="hidden" name="transferLineId" value=""/>
                <input type="hidden" name="lineVoidReason" value=""/>
                <div class="review-table-wrap">
                    <table class="review-table">
                        <thead>
                        <tr><th>Item</th><th>Qty</th><th>Unit cost</th><th>Line value</th><th>Batch</th><th>Expiry</th><th>Available</th></tr>
                        </thead>
                        <tbody>
                        <% transfer.lines?.each { line -> if (line.voided != true) { %>
                        <tr class="review-inter-line" data-source-id="${ transfer.sourcePharmacyLocation?.pharmacyLocationId ?: '' }" data-item-id="${ line.item?.itemId ?: '' }" data-batch-number="${ line.batchNumber?.replace('\\', '\\\\')?.replace('"', '&quot;') ?: '' }" data-stored-unit-price="${ line.unitPrice ?: 0 }">
                            <td>
                                <span class="review-item-title">${ line.item?.name ?: '' }</span>
                                <% if (transfer.transferStatus == 'REQUESTED' || transfer.transferStatus == 'APPROVED') { %>
                                <span class="review-line-remove">
                                    <input type="text" class="review-line-remove-reason" placeholder="Reason for removing"/>
                                    <button type="submit" class="review-remove-link" name="action" value="removeLine" data-line-id="${ line.interPharmacyTransferLineId }" onclick="return prepareTransferLineRemoval(this)">Remove line</button>
                                </span>
                                <% } %>
                            </td>
                            <td><input type="number" step="0.01" min="0.01" name="quantity" value="${ line.quantity }" ${ transfer.transferStatus == 'SERVED' || transfer.transferStatus == 'REJECTED' ? 'readonly="readonly"' : '' }/></td>
                            <td class="review-inter-unit-price">${ line.unitPrice ?: 0 }</td>
                            <td class="review-inter-line-value">${ line.totalAmount ?: ((line.unitPrice ?: 0) * (line.quantity ?: 0)) }</td>
                            <td>
                                <select name="batchNumber" ${ transfer.transferStatus == 'SERVED' || transfer.transferStatus == 'REJECTED' ? 'disabled="disabled"' : '' }>
                                    <option value="">Select batch</option>
                                </select>
                            </td>
                            <td><input type="date" class="openmrs-date-field" name="expiryDate" value="${ line.expiryDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(line.expiryDate) : '' }" ${ transfer.transferStatus == 'SERVED' || transfer.transferStatus == 'REJECTED' ? 'readonly="readonly"' : '' }/></td>
                            <td class="review-inter-available"></td>
                        </tr>
                        <% } } %>
                        <tr>
                            <td colspan="3" style="text-align:right;font-weight:bold;background:#f5f5f5;">Total transfer value</td>
                            <td id="review-inter-transfer-total" style="font-weight:bold;background:#f5f5f5;">${ transfer.lines?.findAll { line -> line.voided != true }?.collect { line -> line.totalAmount ?: ((line.unitPrice ?: 0) * (line.quantity ?: 0)) }?.sum() ?: 0 }</td>
                            <td colspan="3" style="background:#f5f5f5;"></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <div class="review-timeline">
                    Requested ${ transfer.dateCreated ? ui.formatDatePretty(transfer.dateCreated) : '' } by ${ plainUserName(transfer.creator) }.
                </div>
                <div class="review-action-bar">
                    <% if (transfer.transferStatus == 'REQUESTED' || transfer.transferStatus == 'APPROVED') { %>
                    <button type="submit" class="stock-btn stock-btn-primary" name="action" value="transfer">Transfer items</button>
                    <input type="text" name="rejectionReason" placeholder="Reason"/>
                    <button type="submit" class="stock-btn" style="background:#d9534f;border-color:#d43f3a;color:#fff;" name="action" value="reject">Reject</button>
                    <% } %>
                    <a class="stock-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'transfer/interPharmacy') }">Close</a>
                </div>
            </form>
        </div>
    </div>
</div>
<script type="text/javascript">
    var reviewStockByPharmacy = {
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
                    { batchNumber: "${ batch.batchNumber?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }", expiryDate: "${ batch.expiryDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(batch.expiryDate) : '' }", available: "${ batch.available }", unitPrice: "${ batch.unitPrice ?: 0 }" }${ batchIndex + 1 < itemRow.batches.size() ? ',' : '' }
                    <% } %>
                ]
            }${ itemIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ pharmacyIndex + 1 < stockByPharmacy.size() ? ',' : '' }
        <% } %>
    };
    function prepareTransferLineRemoval(button) {
        var form = button ? button.form : null;
        if (!form) {
            return false;
        }
        var container = button.closest('.review-line-remove');
        var reasonInput = container ? container.querySelector('.review-line-remove-reason') : null;
        var reason = reasonInput ? reasonInput.value : '';
        if (!reason || !reason.trim()) {
            alert('Enter a reason before removing this line.');
            if (reasonInput) {
                reasonInput.focus();
            }
            return false;
        }
        form.querySelector('input[name="transferLineId"]').value = button.getAttribute('data-line-id');
        form.querySelector('input[name="lineVoidReason"]').value = reason.trim();
        return true;
    }
    function reviewInterNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }
    function reviewInterMoney(value) {
        return reviewInterNumber(value).toFixed(2);
    }
    function updateReviewInterLineCost(lineRow, unitPrice) {
        var quantityInput = lineRow.querySelector('input[name="quantity"]');
        var unitCell = lineRow.querySelector('.review-inter-unit-price');
        var valueCell = lineRow.querySelector('.review-inter-line-value');
        var price = reviewInterNumber(unitPrice);
        if (unitCell) {
            unitCell.textContent = reviewInterMoney(price);
        }
        if (valueCell) {
            valueCell.textContent = reviewInterMoney(price * reviewInterNumber(quantityInput ? quantityInput.value : 0));
        }
        updateReviewInterTotal();
    }
    function updateReviewInterTotal() {
        var total = 0;
        Array.prototype.slice.call(document.querySelectorAll('.review-inter-line-value')).forEach(function(cell) {
            total += reviewInterNumber(cell.textContent);
        });
        var totalCell = document.getElementById('review-inter-transfer-total');
        if (totalCell) {
            totalCell.textContent = reviewInterMoney(total);
        }
    }
    Array.prototype.slice.call(document.querySelectorAll('.review-inter-line')).forEach(function(lineRow) {
        var sourceId = lineRow.getAttribute('data-source-id');
        var itemId = lineRow.getAttribute('data-item-id');
        var batchSelect = lineRow.querySelector('select[name="batchNumber"]');
        var expiryInput = lineRow.querySelector('input[name="expiryDate"]');
        var quantityInput = lineRow.querySelector('input[name="quantity"]');
        var availableLabel = lineRow.querySelector('.review-inter-available');
        var storedUnitPrice = lineRow.getAttribute('data-stored-unit-price') || '0';
        var currentBatchNumber = lineRow.getAttribute('data-batch-number') || '';
        var item = (reviewStockByPharmacy[sourceId] || []).find(function(candidate) { return candidate.itemId === itemId; });
        if (!batchSelect || !item) { return; }
        batchSelect.innerHTML = '';
        batchSelect.appendChild(new Option('Select batch', ''));
        item.batches.forEach(function(batch) {
            batchSelect.appendChild(new Option((batch.batchNumber || 'No batch') + ' (' + batch.available + ')', batch.batchNumber || ''));
        });
        batchSelect.value = currentBatchNumber;
        availableLabel.textContent = item.available;
        quantityInput.max = item.available;
        updateReviewInterLineCost(lineRow, storedUnitPrice || item.unitPrice);
        quantityInput.addEventListener('input', function() {
            var selectedBatch = item.batches.find(function(batch) { return (batch.batchNumber || '') === batchSelect.value; });
            updateReviewInterLineCost(lineRow, selectedBatch ? selectedBatch.unitPrice : (storedUnitPrice || item.unitPrice));
        });
        batchSelect.addEventListener('change', function() {
            var selectedBatch = item.batches.find(function(batch) { return (batch.batchNumber || '') === batchSelect.value; });
            if (selectedBatch) {
                expiryInput.value = selectedBatch.expiryDate || '';
                quantityInput.max = selectedBatch.available || item.available;
                availableLabel.textContent = selectedBatch.available || item.available;
                updateReviewInterLineCost(lineRow, selectedBatch.unitPrice);
            } else {
                updateReviewInterLineCost(lineRow, storedUnitPrice || item.unitPrice);
            }
        });
    });
    updateReviewInterTotal();
</script>
<% } %>
