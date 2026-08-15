<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Department Request Finance Approval History</h2>
<style type="text/css">
    .dept-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .dept-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .dept-body { padding:14px; }
    .dept-table { width:100%; border-collapse:collapse; margin-bottom:10px; }
    .dept-table th, .dept-table td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .dept-table th { background:#f8fbfd; }
    .dept-muted { color:#64748b; font-size:12px; }
    .dept-btn { display:inline-block; padding:7px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; text-decoration:none; }
    .dept-meta { display:grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap:8px; margin-bottom:10px; }
    .dept-meta div { background:#f8fbfd; border:1px solid #dbe5ec; padding:7px; font-size:12px; }
</style>

<div class="dept-card">
    <div class="dept-head">Finance approval history</div>
    <div class="dept-body">
        <% if (!requests) { %>
        <p class="dept-muted">No department finance approval history found.</p>
        <% } else { requests.each { request -> %>
        <div style="margin-bottom:18px;">
            <h4>#${ request.departmentBulkRequestId } - ${ request.departmentLocation?.name ?: '' } from ${ request.supplyingPharmacyLocation?.name ?: '' }</h4>
            <div class="dept-meta">
                <div><strong>Status</strong><br/>${ request.requestStatus ?: '' }</div>
                <div><strong>Mode</strong><br/>${ request.workflowMode ?: '' }</div>
                <div><strong>Requested by</strong><br/>${ request.creator ?: '' }<br/><span class="dept-muted">${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</span></div>
                <div><strong>Approved by</strong><br/>${ request.financeApprovedBy ?: '' }<br/><span class="dept-muted">${ request.financeApprovedDate ? ui.formatDatePretty(request.financeApprovedDate) : '' }</span></div>
            </div>
            <% if (request.justification) { %><p><strong>Overall justification:</strong> ${ request.justification }</p><% } %>
            <% if (request.requestStatus == 'DISPENSED') { %>
            <p><a class="dept-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'department/transferPdf', [requestId: request.departmentBulkRequestId]) }">Download PDF</a></p>
            <% } %>
            <% if (request.correctionNote) { %><p><strong>Correction/rejection note:</strong> ${ request.correctionNote }</p><% } %>
            <table class="dept-table">
                <thead>
                <tr>
                    <th>Item</th>
                    <th>Stock at Last Request</th>
                    <th>Quantity Received Since</th>
                    <th>Quantity Used Since</th>
                    <th>Current Remainder</th>
                    <th>Quantity Requested Now</th>
                    <th>Approved Quantity</th>
                    <th>Justification</th>
                </tr>
                </thead>
                <tbody>
                <% request.lines?.each { line -> %>
                <tr>
                    <td>${ line.item?.name ?: '' }</td>
                    <td>${ line.previousRequestedQuantity ?: 0 }</td>
                    <td>${ line.previousReceivedQuantity ?: 0 }</td>
                    <td>${ line.previousUsedQuantity ?: 0 }</td>
                    <td>${ line.currentRemainingQuantity ?: 0 }</td>
                    <td>${ line.requestedQuantity ?: 0 }</td>
                    <td>${ line.approvedQuantity ?: 0 }</td>
                    <td>${ line.lineJustification ?: '' }</td>
                </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <% } } %>
    </div>
</div>
