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
    .poa-info-link { background:#eff6ff; border:1px solid #bfdbfe; border-radius:999px; color:#1d4ed8; cursor:pointer; font-size:11px; padding:5px 9px; text-decoration:none; white-space:nowrap; }
    .poa-info-link:hover, .poa-info-link:focus { background:#dbeafe; color:#1e40af; }
    .poa-muted { color:#64748b; font-size:12px; }
    .poa-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; margin-bottom:12px; }
    .poa-reject { background:#d9534f; border-color:#d43f3a; color:#fff; }
    .poa-line { background:#ffffff; border:1px solid #e2e8f0; border-radius:6px; margin-bottom:10px; padding:10px; }
    .poa-line-main { display:flex; gap:12px; justify-content:space-between; align-items:flex-start; }
    .poa-line-title { color:#0f172a; font-weight:600; line-height:1.3; }
    .poa-qty-badge { display:inline-block; margin-left:6px; padding:2px 7px; border-radius:999px; background:#f1f5f9; color:#475569; font-size:11px; font-weight:600; }
    .poa-requesting-stock { display:inline-block; margin-left:6px; padding:2px 7px; border-radius:999px; background:#ecfdf5; color:#047857; font-size:11px; font-weight:600; }
    .poa-serving-stock { display:inline-block; margin-left:6px; padding:2px 7px; border-radius:999px; background:#eff6ff; color:#1d4ed8; font-size:11px; font-weight:600; }
    .poa-serving-stock-low { background:#fff1f2; color:#be123c; }
    .poa-line-actions { display:flex; flex-wrap:wrap; gap:8px; justify-content:flex-end; align-items:center; min-width:330px; }
    .poa-line-remove { display:flex; gap:6px; align-items:center; margin:0; }
    .poa-line-remove input { width:155px; border:1px solid #cbd5e1; border-radius:4px; font-size:12px; padding:5px 7px; }
    .poa-remove-link { background:#fff1f2; border:1px solid #fecdd3; border-radius:999px; color:#be123c; cursor:pointer; font-size:11px; padding:5px 9px; text-decoration:none; white-space:nowrap; }
    .poa-remove-link:hover, .poa-remove-link:focus { background:#ffe4e6; color:#9f1239; }
    .poa-removed { display:inline-block; margin-left:6px; padding:3px 8px; border-radius:999px; background:#fff1f2; color:#991b1b; font-size:11px; font-weight:600; }
    .poa-previous-info { display:none; margin:10px 0 0; padding:10px; border:1px solid #dbe5ec; background:#f8fafc; border-radius:6px; }
    .poa-previous-info.is-visible { display:block; }
    .poa-info-grid { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:6px; }
    .poa-info-cell { border:1px solid #e2e8f0; background:#fff; border-radius:3px; padding:6px; }
    .poa-info-cell em { display:block; font-style:normal; color:#64748b; font-size:10px; text-transform:uppercase; }
    .poa-info-cell strong { display:block; margin-top:3px; color:#0f172a; font-size:12px; }
</style>

<% if (approved) { %><div class="poa-banner">Request approved. It is now visible in the pharmacy order queue for serving.</div><% } %>
<% if (rejected) { %><div class="poa-banner">Request rejected.</div><% } %>
<% if (lineRemoved) { %><div class="poa-banner">Request line removed. Remaining lines can still be approved.</div><% } %>

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
                    <% request.lines?.each { line ->
                        def previousInfo = previousInfoByLineId ? previousInfoByLineId[line.interPharmacyTransferLineId] : null
                        def servingAvailability = servingAvailabilityByLineId ? servingAvailabilityByLineId[line.interPharmacyTransferLineId] : null
                        def lineIsRemoved = line.voided == true
                    %>
                    <div class="poa-line">
                        <div class="poa-line-main">
                            <div class="poa-line-title">
                                ${ line.item?.name ?: '' }
                                <span class="poa-qty-badge">Qty ${ line.quantity ?: 0 }</span>
                                <span class="poa-serving-stock ${ servingAvailability?.insufficient ? 'poa-serving-stock-low' : '' }">Serving pharmacy available ${ servingAvailability?.quantity ?: '0' }</span>
                                <span class="poa-requesting-stock">Requesting pharmacy stock ${ requestingQuantityByLineId ? (requestingQuantityByLineId[line.interPharmacyTransferLineId] ?: '0') : '0' }</span>
                                <% if (lineIsRemoved) { %>
                                <span class="poa-removed">Removed${ line.voidReason ? ': ' + line.voidReason : '' }</span>
                                <% } %>
                            </div>
                            <% if (!lineIsRemoved) { %>
                            <div class="poa-line-actions">
                                <button type="button" class="poa-info-link" data-transfer-line-id="${ line.interPharmacyTransferLineId }" onclick="togglePreviousInfo(this)">Previous info</button>
                                <form method="post" class="poa-line-remove">
                                    <input type="hidden" name="pharmacyLocationId" value="${ selectedPharmacy?.pharmacyLocationId ?: '' }"/>
                                    <input type="hidden" name="transferId" value="${ request.interPharmacyTransferId }"/>
                                    <input type="hidden" name="transferLineId" value="${ line.interPharmacyTransferLineId }"/>
                                    <input type="text" name="lineVoidReason" placeholder="Reason for removing" required="required"/>
                                    <button type="submit" class="poa-remove-link" name="action" value="removeLine">Remove line</button>
                                </form>
                            </div>
                            <% } %>
                        </div>
                        <div class="poa-previous-info" id="previous-info-${ line.interPharmacyTransferLineId }">
                            <% if (!previousInfo || previousInfo.success == false) { %>
                            <span class="poa-muted">${ previousInfo?.message ?: 'Unable to load previous information.' }</span>
                            <% } else if (!previousInfo.hasPreviousRequest) { %>
                            <div class="poa-muted" style="margin-bottom:6px;">No previous request found for this item and requesting pharmacy. Current stock context is shown below.</div>
                            <div class="poa-info-grid">
                                <div class="poa-info-cell"><em>At current request</em><strong>${ previousInfo.quantityAtCurrentRequest ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Current stock</em><strong>${ previousInfo.currentQuantity ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Current request</em><strong>${ previousInfo.currentRequestedQuantity ?: '0' }</strong></div>
                            </div>
                            <% } else { %>
                            <div class="poa-muted" style="margin-bottom:6px;">Since request #${ previousInfo.previousTransferId ?: '' } on ${ previousInfo.previousRequestDate ?: '' }</div>
                            <div class="poa-info-grid">
                                <div class="poa-info-cell"><em>Before last request</em><strong>${ previousInfo.quantityBeforePreviousRequest ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Received</em><strong>${ previousInfo.receivedSincePreviousRequest ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Dispensed</em><strong>${ previousInfo.dispensedSincePreviousRequest ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Transferred out</em><strong>${ previousInfo.transferredOutSincePreviousRequest ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Adjustments</em><strong>${ previousInfo.adjustedSincePreviousRequest ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>At current request</em><strong>${ previousInfo.quantityAtCurrentRequest ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Current stock</em><strong>${ previousInfo.currentQuantity ?: '0' }</strong></div>
                                <div class="poa-info-cell"><em>Current request</em><strong>${ previousInfo.currentRequestedQuantity ?: '0' }</strong></div>
                            </div>
                            <% } %>
                        </div>
                    </div>
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
<script type="text/javascript">
    function togglePreviousInfo(button) {
        if (!button) {
            return;
        }
        var container = document.getElementById('previous-info-' + button.getAttribute('data-transfer-line-id'));
        if (!container) {
            return;
        }
        if (container.classList.contains('is-visible')) {
            container.classList.remove('is-visible');
            return;
        }
        container.classList.add('is-visible');
    }
</script>
