<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Department Request Finance Approval</h2>
<style type="text/css">
    .dept-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .dept-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .dept-body { padding:14px; }
    .dept-table { width:100%; border-collapse:collapse; margin-bottom:10px; }
    .dept-table th, .dept-table td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .dept-table th { background:#f8fbfd; }
    .dept-btn { display:inline-block; padding:7px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; }
    .dept-danger { background:#d9534f; border-color:#d43f3a; }
    .dept-item-title { display:block; color:#0f172a; font-weight:600; margin-bottom:5px; }
    .dept-remove-link { background:#fff1f2; border:1px solid #fecdd3; border-radius:999px; color:#be123c; cursor:pointer; font-size:11px; padding:5px 9px; text-decoration:none; white-space:nowrap; }
    .dept-remove-link:hover, .dept-remove-link:focus { background:#ffe4e6; color:#9f1239; }
    .dept-line-remove { display:flex; gap:6px; align-items:center; margin-top:6px; }
    .dept-line-remove input { width:155px; border:1px solid #cbd5e1; border-radius:4px; font-size:12px; padding:5px 7px; }
    .dept-removed { display:inline-block; padding:3px 8px; border-radius:999px; background:#fff1f2; color:#991b1b; font-size:11px; font-weight:600; }
    .dept-muted { color:#64748b; font-size:12px; }
    .dept-stock-badge { display:inline-block; padding:3px 8px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-size:11px; font-weight:600; white-space:nowrap; }
    .dept-stock-low { background:#fff1f2; color:#be123c; }
</style>

<div class="dept-card">
    <div class="dept-head">Waiting for finance review</div>
    <div class="dept-body">
        <% if (!submittedRequests) { %>
        <p class="dept-muted">No department requests waiting for finance review.</p>
        <% } else { submittedRequests.each { request -> %>
        <form method="post" style="margin-bottom:18px;">
            <input type="hidden" name="requestId" value="${ request.departmentBulkRequestId }"/>
            <input type="hidden" name="removeLineId" value=""/>
            <input type="hidden" name="lineVoidReason" value=""/>
            <h4>#${ request.departmentBulkRequestId } - ${ request.departmentLocation?.name ?: '' } from ${ request.supplyingPharmacyLocation?.name ?: '' }</h4>
            <p class="dept-muted">Mode: ${ request.workflowMode ?: '' } | Requested by ${ request.creator ?: '' } | ${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</p>
            <p>${ request.justification ?: '' }</p>
            <table class="dept-table">
                <thead><tr><th>Item</th><th>Prev requested</th><th>Prev received</th><th>Used</th><th>Remaining</th><th>Requested</th><th>Supplying pharmacy available</th><th>Approved</th><th>Justification</th></tr></thead>
                <tbody>
                <% request.lines?.each { line ->
                    def servingAvailability = servingAvailabilityByLineId ? servingAvailabilityByLineId[line.departmentBulkRequestLineId] : null
                    def lineIsRemoved = line.voided == true
                %>
                <tr>
                    <td>
                        <span class="dept-item-title">${ line.item?.name ?: '' }</span>
                        <% if (lineIsRemoved) { %>
                        <div class="dept-removed">Removed${ line.voidReason ? ': ' + line.voidReason : '' }</div>
                        <% } else { %>
                        <input type="hidden" name="lineId" value="${ line.departmentBulkRequestLineId }"/>
                        <span class="dept-line-remove">
                            <input type="text" class="dept-line-remove-reason" placeholder="Reason for removing"/>
                            <button type="submit" class="dept-remove-link" name="action" value="removeLine" data-line-id="${ line.departmentBulkRequestLineId }" onclick="return prepareDepartmentLineRemoval(this)">Remove line</button>
                        </span>
                        <% } %>
                    </td>
                    <td>${ line.previousRequestedQuantity ?: 0 }</td>
                    <td>${ line.previousReceivedQuantity ?: 0 }</td>
                    <td>${ line.previousUsedQuantity ?: 0 }</td>
                    <td>${ line.currentRemainingQuantity ?: 0 }</td>
                    <td>${ line.requestedQuantity ?: 0 }</td>
                    <td><span class="dept-stock-badge ${ servingAvailability?.insufficient ? 'dept-stock-low' : '' }">${ servingAvailability?.quantity ?: '0' }</span></td>
                    <td>
                        <% if (lineIsRemoved) { %>
                        <span class="dept-muted">Not approvable</span>
                        <% } else { %>
                        <input type="number" step="0.01" min="0" max="${ line.requestedQuantity ?: 0 }" name="approvedQuantity" value="${ line.requestedQuantity ?: 0 }"/>
                        <% } %>
                    </td>
                    <td>${ line.lineJustification ?: '' }</td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <p><input type="text" name="note" placeholder="Rejection/correction note" style="width:50%;"/></p>
            <button type="submit" class="dept-btn" name="action" value="approve">Approve</button>
            <button type="submit" class="dept-btn" name="action" value="correction">Request correction</button>
            <button type="submit" class="dept-btn dept-danger" name="action" value="reject">Reject</button>
        </form>
        <% } } %>
    </div>
</div>

<script type="text/javascript">
    function prepareDepartmentLineRemoval(button) {
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
        return true;
    }
</script>

<div class="dept-card">
    <div class="dept-head">Returned for correction</div>
    <div class="dept-body">
        <% if (!correctionRequests) { %><p class="dept-muted">No correction requests.</p><% } else { correctionRequests.each { request -> %>
        <p>#${ request.departmentBulkRequestId } - ${ request.departmentLocation?.name ?: '' }: ${ request.correctionNote ?: '' }</p>
        <% } } %>
    </div>
</div>
