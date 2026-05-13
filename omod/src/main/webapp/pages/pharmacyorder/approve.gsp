<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Pharmacy Order Approval</h2>
<style type="text/css">
    .poa-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .poa-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .poa-body { padding:14px; }
    .poa-table { width:100%; border-collapse:collapse; }
    .poa-table th, .poa-table td { border:1px solid #dbe5ec; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .poa-table th { background:#f8fbfd; color:#475569; }
    .poa-btn { display:inline-block; padding:7px 12px; border-radius:4px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; text-decoration:none; cursor:pointer; }
    .poa-muted { color:#64748b; font-size:12px; }
    .poa-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; margin-bottom:12px; }
    .poa-reject { background:#d9534f; border-color:#d43f3a; color:#fff; }
</style>

<% if (approved) { %><div class="poa-banner">Request approved. It is now visible in the pharmacy order queue for serving.</div><% } %>
<% if (rejected) { %><div class="poa-banner">Request rejected.</div><% } %>

<div class="poa-card">
    <div class="poa-head">Filter requests</div>
    <div class="poa-body">
        <form method="get">
            <select name="pharmacyLocationId">
                <option value="">All supplying pharmacies</option>
                <% pharmacies.each { pharmacy -> if (pharmacy?.active) { %>
                <option value="${ pharmacy.pharmacyLocationId }" ${ selectedPharmacy?.pharmacyLocationId == pharmacy.pharmacyLocationId ? 'selected="selected"' : '' }>${ pharmacy.name }${ pharmacy.mainStore ? ' (main)' : '' }</option>
                <% } } %>
            </select>
            <button type="submit">Apply</button>
        </form>
    </div>
</div>

<div class="poa-card">
    <div class="poa-head">Requests waiting for approval</div>
    <div class="poa-body">
        <table class="poa-table">
            <thead><tr><th>ID</th><th>Supplying pharmacy</th><th>Receiving pharmacy</th><th>Requested</th><th>Reason</th><th>Items</th><th>Action</th></tr></thead>
            <tbody>
            <% if (!requests) { %>
            <tr><td colspan="7" class="poa-muted">No pharmacy orders waiting for approval.</td></tr>
            <% } else { requests.each { request -> %>
            <tr>
                <td>${ request.interPharmacyTransferId }</td>
                <td>${ request.sourcePharmacyLocation?.name ?: '' }</td>
                <td>${ request.destinationPharmacyLocation?.name ?: '' }</td>
                <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                <td>${ request.reason ?: '' }</td>
                <td>
                    <% request.lines?.each { line -> %>
                    <div>${ line.item?.name ?: '' }: ${ line.quantity ?: 0 }</div>
                    <% } %>
                </td>
                <td>
                    <form method="post" style="display:inline-block;">
                        <input type="hidden" name="pharmacyLocationId" value="${ selectedPharmacy?.pharmacyLocationId ?: '' }"/>
                        <input type="hidden" name="transferId" value="${ request.interPharmacyTransferId }"/>
                        <button type="submit" class="poa-btn" name="action" value="approve">Approve</button>
                    </form>
                    <form method="post" style="display:inline-block;">
                        <input type="hidden" name="pharmacyLocationId" value="${ selectedPharmacy?.pharmacyLocationId ?: '' }"/>
                        <input type="hidden" name="transferId" value="${ request.interPharmacyTransferId }"/>
                        <input type="text" name="rejectionReason" placeholder="Reason"/>
                        <button type="submit" class="poa-btn poa-reject" name="action" value="reject">Reject</button>
                    </form>
                </td>
            </tr>
            <% } } %>
            </tbody>
        </table>
    </div>
</div>
