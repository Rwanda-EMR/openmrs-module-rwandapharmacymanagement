<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage", [pageTitle: "Main To Dispensing Review"])
    def plainUserName = { user ->
        def personName = user?.personName
        def fullName = personName?.fullName?.trim()
        if (fullName) {
            return fullName
        }

        def combinedName = "${personName?.givenName ?: ''} ${personName?.familyName ?: ''}".trim()
        combinedName ?: (user?.username?.trim() ?: '')
    }
    def canApproveTransfer = binding.hasVariable('canApproveMainToDispensingTransfer') ? (canApproveMainToDispensingTransfer ?: false) : false
    def canTransferItems = binding.hasVariable('canTransferMainToDispensing') ? (canTransferMainToDispensing ?: false) : false
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
</style>

<h2>Main To Dispensing Review</h2>
<% if (!transfer) { %>
<div style="padding:12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;">Transfer request not found.</div>
<% } else { %>
<div class="review-shell">
    <div class="review-card">
        <div class="review-header">Request MTD-${ transfer.interPharmacyTransferId }</div>
        <div class="review-body">
            <div class="review-meta-grid">
                <div class="review-meta-card"><div class="review-meta-label">Source</div><div class="review-meta-value">${ transfer.sourcePharmacyLocation?.name ?: '' }</div></div>
                <div class="review-meta-card"><div class="review-meta-label">Destination</div><div class="review-meta-value">${ transfer.destinationPharmacyLocation?.name ?: '' }</div></div>
                <div class="review-meta-card"><div class="review-meta-label">Status</div><div class="review-meta-value"><span class="review-status-pill ${ transfer.transferStatus == 'APPROVED' ? 'review-status-approved' : transfer.transferStatus == 'REJECTED' ? 'review-status-rejected' : transfer.transferStatus == 'SERVED' ? 'review-status-served' : '' }">${ transfer.transferStatus == 'SERVED' ? 'TRANSFERRED' : (transfer.transferStatus ?: 'REQUESTED') }</span></div></div>
                <div class="review-meta-card"><div class="review-meta-label">Reason</div><div class="review-meta-value">${ transfer.reason ?: '' }</div></div>
            </div>

            <form method="post">
                <input type="hidden" name="transferId" value="${ transfer.interPharmacyTransferId }"/>
                <div class="review-table-wrap">
                    <table class="review-table">
                        <thead>
                        <tr><th>Item</th><th>Qty</th><th>Unit cost</th><th>Line value</th><th>Batch</th><th>Expiry</th><th>Available</th></tr>
                        </thead>
                        <tbody>
                        <% transfer.lines?.each { line -> %>
                        <% def canEditLine = transfer.transferStatus == 'APPROVED' && canTransferItems %>
                        <tr class="review-main-line" data-item-id="${ line.item?.itemId ?: '' }" data-batch-number="${ line.batchNumber?.replace('\\', '\\\\')?.replace('"', '&quot;') ?: '' }" data-stored-unit-price="${ line.unitPrice ?: 0 }">
                            <td>${ line.item?.name ?: '' }</td>
                            <td><input type="number" step="0.01" min="0.01" name="quantity" value="${ line.quantity }" ${ canEditLine ? '' : 'readonly="readonly"' }/></td>
                            <td class="review-main-unit-price">${ line.unitPrice ?: 0 }</td>
                            <td class="review-main-line-value">${ line.totalAmount ?: ((line.unitPrice ?: 0) * (line.quantity ?: 0)) }</td>
                            <td>
                                <select name="batchNumber" ${ canEditLine ? '' : 'disabled="disabled"' }>
                                    <option value="">Select batch</option>
                                </select>
                            </td>
                            <td><input type="date" class="openmrs-date-field" name="expiryDate" value="${ line.expiryDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(line.expiryDate) : '' }" ${ canEditLine ? '' : 'readonly="readonly"' }/></td>
                            <td class="review-main-available"></td>
                        </tr>
                        <% } %>
                        <tr>
                            <td colspan="3" style="text-align:right;font-weight:bold;background:#f5f5f5;">Total transfer value</td>
                            <td id="review-main-transfer-total" style="font-weight:bold;background:#f5f5f5;">${ transfer.lines?.collect { line -> line.totalAmount ?: ((line.unitPrice ?: 0) * (line.quantity ?: 0)) }?.sum() ?: 0 }</td>
                            <td colspan="3" style="background:#f5f5f5;"></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <div class="review-timeline">
                    Requested ${ transfer.dateCreated ? ui.formatDatePretty(transfer.dateCreated) : '' } by ${ plainUserName(transfer.creator) }.
                </div>
                <div class="review-action-bar">
                    <% if (transfer.transferStatus == 'REQUESTED' && canApproveTransfer) { %>
                    <button type="submit" class="stock-btn stock-btn-primary" name="action" value="approve">Approve request</button>
                    <input type="text" name="rejectionReason" placeholder="Reason"/>
                    <button type="submit" class="stock-btn" style="background:#d9534f;border-color:#d43f3a;color:#fff;" name="action" value="reject">Reject</button>
                    <% } else if (transfer.transferStatus == 'REQUESTED') { %>
                    <span style="color:#777;">Awaiting finance approval</span>
                    <% } else if (transfer.transferStatus == 'APPROVED' && canTransferItems) { %>
                    <button type="submit" class="stock-btn stock-btn-primary" name="action" value="transfer">Transfer items</button>
                    <% } else if (transfer.transferStatus == 'APPROVED') { %>
                    <span style="color:#777;">Awaiting main store transfer</span>
                    <% } else if (transfer.transferStatus == 'SERVED') { %>
                    <span style="color:#777;">Transferred</span>
                    <% } else if (transfer.transferStatus == 'REJECTED') { %>
                    <span style="color:#777;">Rejected</span>
                    <% } %>
                    <a class="stock-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'transfer/mainToDispensing') }">Close</a>
                </div>
            </form>
        </div>
    </div>
</div>
<script type="text/javascript">
    var reviewMainStockByItem = {
        <% mainStockByItem.eachWithIndex { entry, itemIndex -> %>
        "${ entry.key }": [
            <% entry.value.eachWithIndex { batch, batchIndex -> %>
            { batchNumber: "${ batch.batchNumber?.replace('\\', '\\\\')?.replace('"', '\\"') ?: '' }", expiryDate: "${ batch.expiryDate ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(batch.expiryDate) : '' }", available: "${ batch.available }", unitPrice: "${ batch.unitPrice ?: 0 }" }${ batchIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ itemIndex + 1 < mainStockByItem.size() ? ',' : '' }
        <% } %>
    };
    var reviewMainStockSummary = {
        <% mainStockSummary.eachWithIndex { entry, idx -> %>
        "${ entry.key }": "${ entry.value }"${ idx + 1 < mainStockSummary.size() ? ',' : '' }
        <% } %>
    };
    function reviewMainNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }
    function reviewMainMoney(value) {
        return reviewMainNumber(value).toFixed(2);
    }
    function updateReviewMainLineCost(lineRow, unitPrice) {
        var quantityInput = lineRow.querySelector('input[name="quantity"]');
        var unitCell = lineRow.querySelector('.review-main-unit-price');
        var valueCell = lineRow.querySelector('.review-main-line-value');
        var price = reviewMainNumber(unitPrice);
        if (unitCell) {
            unitCell.textContent = reviewMainMoney(price);
        }
        if (valueCell) {
            valueCell.textContent = reviewMainMoney(price * reviewMainNumber(quantityInput ? quantityInput.value : 0));
        }
        updateReviewMainTotal();
    }
    function updateReviewMainTotal() {
        var total = 0;
        Array.prototype.slice.call(document.querySelectorAll('.review-main-line-value')).forEach(function(cell) {
            total += reviewMainNumber(cell.textContent);
        });
        var totalCell = document.getElementById('review-main-transfer-total');
        if (totalCell) {
            totalCell.textContent = reviewMainMoney(total);
        }
    }
    Array.prototype.slice.call(document.querySelectorAll('.review-main-line')).forEach(function(lineRow) {
        var itemId = lineRow.getAttribute('data-item-id');
        var batchSelect = lineRow.querySelector('select[name="batchNumber"]');
        var expiryInput = lineRow.querySelector('input[name="expiryDate"]');
        var quantityInput = lineRow.querySelector('input[name="quantity"]');
        var availableLabel = lineRow.querySelector('.review-main-available');
        var batches = reviewMainStockByItem[itemId] || [];
        var storedUnitPrice = lineRow.getAttribute('data-stored-unit-price') || '0';
        var currentBatchNumber = lineRow.getAttribute('data-batch-number') || '';
        if (!batchSelect) { return; }
        batchSelect.innerHTML = '';
        batchSelect.appendChild(new Option('Select batch', ''));
        batches.forEach(function(batch) {
            batchSelect.appendChild(new Option((batch.batchNumber || 'No batch') + ' (' + batch.available + ')', batch.batchNumber || ''));
        });
        batchSelect.value = currentBatchNumber;
        availableLabel.textContent = reviewMainStockSummary[itemId] || '';
        quantityInput.max = reviewMainStockSummary[itemId] || '';
        updateReviewMainLineCost(lineRow, storedUnitPrice);
        quantityInput.addEventListener('input', function() {
            var selectedBatch = batches.find(function(batch) { return (batch.batchNumber || '') === batchSelect.value; });
            updateReviewMainLineCost(lineRow, selectedBatch ? selectedBatch.unitPrice : storedUnitPrice);
        });
        batchSelect.addEventListener('change', function() {
            var selectedBatch = batches.find(function(batch) { return (batch.batchNumber || '') === batchSelect.value; });
            if (selectedBatch) {
                expiryInput.value = selectedBatch.expiryDate || '';
                quantityInput.max = selectedBatch.available || reviewMainStockSummary[itemId] || '';
                availableLabel.textContent = selectedBatch.available || reviewMainStockSummary[itemId] || '';
                updateReviewMainLineCost(lineRow, selectedBatch.unitPrice);
            } else {
                updateReviewMainLineCost(lineRow, storedUnitPrice);
            }
        });
    });
    updateReviewMainTotal();
</script>
<% } %>
