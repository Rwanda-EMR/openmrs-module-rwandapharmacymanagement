<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Department Return Queue</h2>
<style type="text/css">
    .dept-return-warn { padding:10px 12px; border:1px solid #fde68a; background:#fffbeb; color:#92400e; border-radius:4px; margin-bottom:12px; }
    .dept-return-table { width:100%; border-collapse:collapse; background:#fff; }
    .dept-return-table th, .dept-return-table td { border:1px solid #ddd; padding:7px 8px; text-align:left; vertical-align:top; font-size:12px; }
    .dept-return-table th { background:#f5f5f5; color:#555; }
    .dept-return-actions { display:flex; gap:6px; flex-wrap:wrap; align-items:center; }
</style>

<% if (pharmacySelectionRequired) { %>
<div class="dept-return-warn">
    <form method="get">
        <strong>Choose receiving pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' }.
        <select name="pharmacyLocationId" required="required">
            <option value="">Select pharmacy</option>
            <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
        </select>
        <button type="submit">Use pharmacy</button>
    </form>
</div>
<% } else if (!activePharmacy) { %>
<div class="dept-return-warn">No pharmacy is selected for receiving department returns at ${ pharmacySelectionLocationName ?: 'your current location' }.</div>
<% } %>

<table class="dept-return-table">
    <thead><tr><th>ID</th><th>Department</th><th>Destination</th><th>Reason</th><th>Items</th><th>Requested by</th><th>Actions</th></tr></thead>
    <tbody>
    <% if (!returns) { %>
    <tr><td colspan="7">No pending department returns found.</td></tr>
    <% } else { returns.each { stockReturn -> %>
    <tr>
        <td>${ stockReturn.departmentStockReturnId }</td>
        <td>${ stockReturn.departmentLocation?.name ?: '' }</td>
        <td>${ stockReturn.destinationPharmacyLocation?.name ?: '' }</td>
        <td>${ stockReturn.reason ?: '' }</td>
        <td>
            <% stockReturn.lines?.each { line -> if (line.voided != true) { %>
            <div>${ line.item?.name ?: '' }: ${ line.quantity ?: 0 }${ line.batchNumber ? ' - ' + line.batchNumber : '' }</div>
            <% } } %>
        </td>
        <td>${ stockReturn.creator?.displayString ?: '' } ${ stockReturn.dateCreated ? '(' + ui.formatDatePretty(stockReturn.dateCreated) + ')' : '' }</td>
        <td>
            <div class="dept-return-actions">
                <form method="post"><input type="hidden" name="returnId" value="${ stockReturn.departmentStockReturnId }"/><button type="submit" name="action" value="receive">Receive</button></form>
                <form method="post"><input type="hidden" name="returnId" value="${ stockReturn.departmentStockReturnId }"/><input type="text" name="rejectionReason" placeholder="Reason"/><button type="submit" name="action" value="reject">Reject</button></form>
            </div>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>
