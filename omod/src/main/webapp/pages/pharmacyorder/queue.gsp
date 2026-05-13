<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def availableByItemId = binding.hasVariable("availableByItemId") && availableByItemId ? availableByItemId : [:]
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
    .poq-muted { color:#64748b; font-size:12px; }
    .poq-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; margin-bottom:12px; }
    .poq-warn { padding:10px 12px; border-radius:4px; border:1px solid #f0ad4e; background:#fcf8e3; color:#8a6d3b; margin-bottom:12px; }
</style>

<% if (approved) { %><div class="poq-banner">Request approved and deducted from stock.</div><% } %>

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
            <% } else { requests.each { request -> %>
            <tr>
                <td>${ request.pharmacyItemRequestId }</td>
                <td>${ request.creator?.personName ?: request.creator?.username ?: '' }${ request.dateCreated ? ' - ' + ui.formatDatePretty(request.dateCreated) : '' }</td>
                <td>${ request.reason ?: '' }</td>
                <td colspan="2">
                    <form method="post">
                        <input type="hidden" name="requestId" value="${ request.pharmacyItemRequestId }"/>
                        <table class="poq-table">
                            <thead><tr><th>Item</th><th>Requested</th><th>Available here</th><th>Approved quantity</th></tr></thead>
                            <tbody>
                            <% request.lines?.each { line -> %>
                            <tr>
                                <td>${ line.item?.name ?: '' }<input type="hidden" name="lineId" value="${ line.pharmacyItemRequestLineId }"/></td>
                                <td>${ line.requestedQuantity ?: 0 }</td>
                                <td>${ availableByItemId[line.item?.itemId] ?: 0 }</td>
                                <td><input type="number" step="0.01" min="0" max="${ line.requestedQuantity ?: 0 }" name="approvedQuantity" value="${ line.requestedQuantity ?: 0 }"/></td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <p style="margin-top:8px;"><button type="submit" class="poq-btn">Approve and deduct stock</button></p>
                    </form>
                </td>
            </tr>
            <% } } %>
            </tbody>
        </table>
    </div>
</div>
