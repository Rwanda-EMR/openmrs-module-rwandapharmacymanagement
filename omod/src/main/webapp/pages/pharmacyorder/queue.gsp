<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def availableByItemId = binding.hasVariable("availableByItemId") && availableByItemId ? availableByItemId : [:]
    def batchOptionsByItemId = binding.hasVariable("batchOptionsByItemId") && batchOptionsByItemId ? batchOptionsByItemId : [:]
%>
<h2>Pharmacy Item Request Queue</h2>
<style type="text/css">
    .poq-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .poq-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .poq-body { padding:14px; }
    .poq-table { width:100%; border-collapse:collapse; }
    .poq-table th, .poq-table td { border:1px solid #dbe5ec; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .poq-table th { background:#f8fbfd; color:#475569; }
    .poq-btn { display:inline-block; padding:7px 12px; border-radius:4px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; text-decoration:none; cursor:pointer; }
    .poq-btn-reject { background:#d9534f; border-color:#d43f3a; }
    .poq-remove-btn { background:#fff1f2; border:1px solid #fecdd3; border-radius:999px; color:#be123c; cursor:pointer; font-size:11px; padding:5px 9px; text-decoration:none; white-space:nowrap; }
    .poq-muted { color:#64748b; font-size:12px; }
    .poq-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; margin-bottom:12px; }
    .poq-warn { padding:10px 12px; border-radius:4px; border:1px solid #f0ad4e; background:#fcf8e3; color:#8a6d3b; margin-bottom:12px; }
    .poq-error { border-color:#fecaca; background:#fef2f2; color:#991b1b; }
    .poq-actions { display:flex; gap:8px; align-items:center; flex-wrap:wrap; margin-top:8px; }
    .poq-inline-form { display:flex; gap:6px; align-items:center; flex-wrap:wrap; margin:0; }
    .poq-inline-form input[type="text"] { border:1px solid #cbd5e1; border-radius:4px; font-size:12px; padding:5px 7px; }
    .poq-removed { display:inline-block; margin-left:6px; padding:3px 8px; border-radius:999px; background:#fff1f2; color:#991b1b; font-size:11px; font-weight:600; }
</style>

<% if (approved) { %><div class="poq-banner">Request approved and deducted from stock.</div><% } %>
<% if (rejected) { %><div class="poq-banner">Request rejected.</div><% } %>
<% if (lineRemoved) { %><div class="poq-banner">Request line removed. Remaining items can still be served.</div><% } %>
<% if (errorMessage) { %><div class="poq-warn poq-error">${ errorMessage }</div><% } %>

<% if (pharmacySelectionRequired) { %>
<div class="poq-warn">
    <form method="get">
        <strong>Choose pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.
        <select name="pharmacyLocationId" required="required">
            <option value="">Select pharmacy</option>
            <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
        </select>
        <button type="submit">Use pharmacy</button>
    </form>
</div>
<% } else if (!activePharmacy) { %>
<div class="poq-warn">No pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.</div>
<% } else { %>
<div class="poq-card">
    <div class="poq-head">Active pharmacy</div>
    <div class="poq-body"><strong>${ activePharmacy.name }</strong></div>
</div>
<% } %>

<div class="poq-card">
    <div class="poq-head">Pending user item requests</div>
    <div class="poq-body">
        <table class="poq-table">
            <thead><tr><th>ID</th><th>Requested by</th><th>Reason</th><th>Items</th><th>Action</th></tr></thead>
            <tbody>
            <% if (!requests) { %>
            <tr><td colspan="5" class="poq-muted">No pending user item requests.</td></tr>
            <% } else { requests.each { request ->
                def approveFormId = "approve-request-" + request.pharmacyItemRequestId
            %>
            <tr>
                <td>${ request.pharmacyItemRequestId }</td>
                <td>${ request.creator?.personName ?: request.creator?.username ?: '' }${ request.dateCreated ? ' - ' + ui.formatDatePretty(request.dateCreated) : '' }</td>
                <td>${ request.reason ?: '' }</td>
                <td colspan="2">
                    <table class="poq-table">
                        <thead><tr><th>Item</th><th>Requested</th><th>Available here</th><th>Batch number</th><th>Approved quantity</th><th>Line action</th></tr></thead>
                        <tbody>
                        <% request.lines?.each { line -> if (line.voided != true) { %>
                            <tr>
                                <td>${ line.item?.name ?: '' }<input form="${ approveFormId }" type="hidden" name="lineId" value="${ line.pharmacyItemRequestLineId }"/></td>
                                <td>${ line.requestedQuantity ?: 0 }</td>
                                <td>${ availableByItemId[line.item?.itemId] ?: 0 }</td>
                                <td>
                                    <select form="${ approveFormId }" name="batchNumber">
                                        <option value="__FIFO__">Auto / FIFO</option>
                                        <% (batchOptionsByItemId[line.item?.itemId] ?: []).each { batch -> %>
                                        <option value="${ batch.batchValue }">
                                            ${ batch.batchNumber ?: 'No batch' } (${ batch.available ?: 0 }${ batch.expiryDate ? ', exp ' + ui.formatDatePretty(batch.expiryDate) : '' })
                                        </option>
                                        <% } %>
                                    </select>
                                </td>
                                <td><input form="${ approveFormId }" type="number" step="0.01" min="0" max="${ line.requestedQuantity ?: 0 }" name="approvedQuantity" value="${ line.requestedQuantity ?: 0 }"/></td>
                                <td>
                                    <form method="post" class="poq-inline-form">
                                        <input type="hidden" name="action" value="removeLine"/>
                                        <input type="hidden" name="requestId" value="${ request.pharmacyItemRequestId }"/>
                                        <input type="hidden" name="requestLineId" value="${ line.pharmacyItemRequestLineId }"/>
                                        <input type="text" name="lineVoidReason" placeholder="Reason" required="required"/>
                                        <button type="submit" class="poq-remove-btn">Remove item</button>
                                    </form>
                                </td>
                            </tr>
                        <% } else { %>
                            <tr>
                                <td>${ line.item?.name ?: '' } <span class="poq-removed">Removed${ line.voidReason ? ': ' + line.voidReason : '' }</span></td>
                                <td>${ line.requestedQuantity ?: 0 }</td>
                                <td colspan="4" class="poq-muted">This item will not be served.</td>
                            </tr>
                        <% } } %>
                        </tbody>
                    </table>
                    <form method="post" id="${ approveFormId }">
                        <input type="hidden" name="requestId" value="${ request.pharmacyItemRequestId }"/>
                        <input type="hidden" name="action" value="approve"/>
                    </form>
                    <div class="poq-actions">
                        <button form="${ approveFormId }" type="submit" class="poq-btn">Approve and deduct stock</button>
                        <form method="post" class="poq-inline-form">
                            <input type="hidden" name="requestId" value="${ request.pharmacyItemRequestId }"/>
                            <input type="hidden" name="action" value="reject"/>
                            <input type="text" name="rejectionReason" placeholder="Reason for rejection" required="required"/>
                            <button type="submit" class="poq-btn poq-btn-reject">Reject request</button>
                        </form>
                    </div>
                </td>
            </tr>
            <% } } %>
            </tbody>
        </table>
    </div>
</div>
