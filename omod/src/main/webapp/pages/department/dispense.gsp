<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Department Request Pharmacy Dispense</h2>
<style type="text/css">
    .dept-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .dept-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .dept-body { padding:14px; }
    .dept-table { width:100%; border-collapse:collapse; margin-bottom:10px; }
    .dept-table th, .dept-table td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .dept-table th { background:#f8fbfd; }
    .dept-allocation-row { display:flex; gap:6px; align-items:flex-start; margin-bottom:6px; }
    .dept-allocation-row select { width:220px; }
    .dept-allocation-row input { width:90px; }
    .dept-batch-info { min-width:180px; }
    .dept-btn { display:inline-block; padding:7px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; }
    .dept-small-btn { padding:4px 8px; border:1px solid #94a3b8; background:#f8fafc; border-radius:4px; cursor:pointer; }
    .dept-item-title { display:block; color:#0f172a; font-weight:600; margin-bottom:5px; }
    .dept-remove-link { background:#fff1f2; border:1px solid #fecdd3; border-radius:999px; color:#be123c; cursor:pointer; font-size:11px; padding:5px 9px; text-decoration:none; white-space:nowrap; }
    .dept-remove-link:hover, .dept-remove-link:focus { background:#ffe4e6; color:#9f1239; }
    .dept-line-remove { display:flex; gap:6px; align-items:center; margin-top:6px; }
    .dept-line-remove input { width:155px; border:1px solid #cbd5e1; border-radius:4px; font-size:12px; padding:5px 7px; }
    .dept-muted { color:#64748b; font-size:12px; }
</style>

<div class="dept-card">
    <div class="dept-head">Finance-approved requests</div>
    <div class="dept-body">
        <% if (!requests) { %>
        <p class="dept-muted">No department requests are waiting for pharmacy dispense.</p>
        <% } else { requests.each { request -> %>
        <form method="post" style="margin-bottom:18px;" onsubmit="return validateDepartmentDispense(this)">
            <input type="hidden" name="requestId" value="${ request.departmentBulkRequestId }"/>
            <input type="hidden" name="removeLineId" value=""/>
            <input type="hidden" name="lineVoidReason" value=""/>
            <h4>#${ request.departmentBulkRequestId } - ${ request.departmentLocation?.name ?: '' } from ${ request.supplyingPharmacyLocation?.name ?: '' }</h4>
            <p class="dept-muted">Finance approved by ${ request.financeApprovedBy ?: '' } on ${ request.financeApprovedDate ? ui.formatDatePretty(request.financeApprovedDate) : '' }</p>
            <table class="dept-table">
                <thead><tr><th>Item</th><th>Requested</th><th>Approved</th><th>Batch allocations</th></tr></thead>
                <tbody>
                <% request.lines?.each { line -> if (line.voided != true) { %>
                <tr>
                    <td>
                        <span class="dept-item-title">${ line.item?.name ?: '' }</span>
                        <span class="dept-line-remove">
                            <input type="text" class="dept-line-remove-reason" placeholder="Reason for removing"/>
                            <button type="submit" class="dept-remove-link" name="action" value="removeLine" data-line-id="${ line.departmentBulkRequestLineId }" onclick="return prepareDepartmentDispenseLineRemoval(this)">Remove line</button>
                        </span>
                    </td>
                    <td>${ line.requestedQuantity ?: 0 }</td>
                    <td>${ line.approvedQuantity ?: 0 }</td>
                    <td>
                        <div class="dept-allocation-container" data-line-id="${ line.departmentBulkRequestLineId }" data-approved="${ line.approvedQuantity ?: 0 }">
                            <div class="dept-allocation-row">
                                <input type="hidden" name="lineId" value="${ line.departmentBulkRequestLineId }"/>
                                <select name="batchNumber" onchange="refreshDepartmentBatchInfo(this)">
                                    <option value="">Select batch</option>
                                    <% def batches = batchesByLineId[line.departmentBulkRequestLineId] ?: []
                                       batches.each { batch ->
                                           def batchNumber = batch.batchNumber ?: ''
	                                           def label = batchNumber ?: 'No batch number'
	                                           def expiryDate = batch.expiryDate ?: ''
	                                           def expiryLabel = expiryDate ? 'Exp: ' + expiryDate : 'No expiry'
	                                           def available = batch.available ?: 0
	                                    %>
	                                    <option value="${ batchNumber }" data-available="${ available }" data-expiry="${ expiryDate }">${ label } - ${ expiryLabel } - ${ available } available</option>
                                    <% } %>
                                </select>
                                <input type="number" step="0.01" min="0" name="dispensedQuantity" value="${ line.approvedQuantity ?: 0 }" oninput="refreshDepartmentLineTotal(this)"/>
                                <span class="dept-muted dept-batch-info">Select batch</span>
                                <button type="button" class="dept-small-btn" onclick="removeDepartmentAllocation(this)">Remove</button>
                            </div>
                        </div>
                        <div class="dept-muted">Total selected: <span class="dept-line-total">0</span> / ${ line.approvedQuantity ?: 0 }</div>
                        <button type="button" class="dept-small-btn" onclick="addDepartmentAllocation(${ line.departmentBulkRequestLineId })">Add another batch</button>
                        <% if (!batches) { %><div class="dept-muted">No available batch with quantity greater than zero.</div><% } %>
                    </td>
                </tr>
                <% } } %>
                </tbody>
            </table>
            <button type="submit" class="dept-btn">Dispense to department store</button>
        </form>
        <% } } %>
    </div>
</div>

<script type="text/javascript">
    var departmentBatchesByLineId = {
        <% batchesByLineId.eachWithIndex { entry, lineIndex -> %>
        "${ entry.key }": [
            <% entry.value.eachWithIndex { batch, batchIndex ->
                def batchNumber = batch.batchNumber ?: ''
                def expiryDate = batch.expiryDate ?: ''
                def available = batch.available ?: 0
                def label = batchNumber ?: 'No batch number'
                def displayLabel = label + ' - ' + (expiryDate ? 'Exp: ' + expiryDate : 'No expiry')
            %>
            { batchNumber: "${ batchNumber?.replace('\\', '\\\\')?.replace('"', '\\"') }", label: "${ displayLabel?.replace('\\', '\\\\')?.replace('"', '\\"') }", available: "${ available }", expiryDate: "${ expiryDate }" }${ batchIndex + 1 < entry.value.size() ? ',' : '' }
            <% } %>
        ]${ lineIndex + 1 < batchesByLineId.size() ? ',' : '' }
        <% } %>
    };

    function prepareDepartmentDispenseLineRemoval(button) {
        var form = button ? button.form : null;
        if (!form) {
            return false;
        }
        var container = button.closest('.dept-line-remove');
        var reasonInput = container ? container.querySelector('.dept-line-remove-reason') : null;
        var reason = reasonInput ? reasonInput.value : '';
        if (!reason || !reason.trim()) {
            alert('Enter a reason before removing this line.');
            if (reasonInput) {
                reasonInput.focus();
            }
            return false;
        }
        form.querySelector('input[name="removeLineId"]').value = button.getAttribute('data-line-id');
        form.querySelector('input[name="lineVoidReason"]').value = reason.trim();
        form.setAttribute('data-skip-validation', 'true');
        return true;
    }

    document.addEventListener('DOMContentLoaded', function() {
        Array.prototype.slice.call(document.querySelectorAll('.dept-allocation-row select')).forEach(function(select) {
            refreshDepartmentBatchInfo(select);
        });
        Array.prototype.slice.call(document.querySelectorAll('.dept-allocation-container')).forEach(function(container) {
            refreshDepartmentLineTotal(container);
        });
    });

    function addDepartmentAllocation(lineId) {
        var container = document.querySelector('.dept-allocation-container[data-line-id="' + lineId + '"]');
        if (!container) {
            return;
        }
        var row = document.createElement('div');
        row.className = 'dept-allocation-row';
        row.innerHTML = '<input type="hidden" name="lineId" value="' + lineId + '"/>' +
            '<select name="batchNumber" onchange="refreshDepartmentBatchInfo(this)">' +
            batchOptions(lineId) + '</select>' +
            '<input type="number" step="0.01" min="0" name="dispensedQuantity" value="0" oninput="refreshDepartmentLineTotal(this)"/>' +
            '<span class="dept-muted dept-batch-info">Select batch</span>' +
            '<button type="button" class="dept-small-btn" onclick="removeDepartmentAllocation(this)">Remove</button>';
        container.appendChild(row);
        refreshDepartmentLineTotal(container);
    }

    function batchOptions(lineId) {
        var options = '<option value="">Select batch</option>';
        (departmentBatchesByLineId[lineId] || []).forEach(function(batch) {
            options += '<option value="' + escapeHtml(batch.batchNumber) + '" data-available="' + escapeHtml(batch.available) +
                '" data-expiry="' + escapeHtml(batch.expiryDate) + '">' + escapeHtml(batch.label) + ' - ' +
                escapeHtml(batch.available) + ' available</option>';
        });
        return options;
    }

    function refreshDepartmentBatchInfo(select) {
        var row = select.closest('.dept-allocation-row');
        var info = row ? row.querySelector('.dept-batch-info') : null;
        var quantity = row ? row.querySelector('input[name="dispensedQuantity"]') : null;
        var option = select.options[select.selectedIndex];
        var available = option ? option.getAttribute('data-available') : '';
        var expiry = option ? option.getAttribute('data-expiry') : '';
        if (quantity) {
            quantity.max = available || '';
        }
        if (info) {
            info.textContent = available ? ('Available: ' + available + (expiry ? ', Expiry: ' + expiry : ', No expiry')) : 'Select batch';
        }
        refreshDepartmentLineTotal(select);
    }

    function refreshDepartmentLineTotal(element) {
        var container = element.classList && element.classList.contains('dept-allocation-container')
            ? element : element.closest('.dept-allocation-container');
        if (!container) {
            return;
        }
        var total = 0;
        Array.prototype.slice.call(container.querySelectorAll('input[name="dispensedQuantity"]')).forEach(function(input) {
            total += parseNumber(input.value);
        });
        var totalBox = container.parentNode.querySelector('.dept-line-total');
        if (totalBox) {
            totalBox.textContent = formatNumber(total);
        }
    }

    function removeDepartmentAllocation(button) {
        var container = button.closest('.dept-allocation-container');
        if (container && container.querySelectorAll('.dept-allocation-row').length > 1) {
            button.closest('.dept-allocation-row').remove();
            refreshDepartmentLineTotal(container);
        }
    }

    function validateDepartmentDispense(form) {
        if (form && form.getAttribute('data-skip-validation') === 'true') {
            return true;
        }
        var valid = true;
        Array.prototype.slice.call(form.querySelectorAll('.dept-allocation-container')).forEach(function(container) {
            var approved = parseNumber(container.getAttribute('data-approved'));
            var total = 0;
            Array.prototype.slice.call(container.querySelectorAll('.dept-allocation-row')).forEach(function(row) {
                var select = row.querySelector('select[name="batchNumber"]');
                var quantity = row.querySelector('input[name="dispensedQuantity"]');
                var selected = select.options[select.selectedIndex];
                var available = selected ? parseNumber(selected.getAttribute('data-available')) : 0;
                var requested = parseNumber(quantity.value);
                total += requested;
                if (requested > 0 && (!selected || selected.getAttribute('data-available') === null || requested > available)) {
                    valid = false;
                }
            });
            if (total < 0 || total > approved) {
                valid = false;
            }
        });
        if (!valid) {
            alert('Check batch allocations. Quantity must be within approved quantity and selected batch availability.');
        }
        return valid;
    }

    function parseNumber(value) {
        var parsed = parseFloat(value);
        return isNaN(parsed) ? 0 : parsed;
    }

    function formatNumber(value) {
        return Math.round(value * 100) / 100;
    }

    function escapeHtml(value) {
        return String(value || '').split('&').join('&amp;').split('"').join('&quot;').split('<').join('&lt;').split('>').join('&gt;');
    }
</script>
