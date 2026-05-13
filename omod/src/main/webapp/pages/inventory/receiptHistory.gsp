<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def plainUserName = { user ->
        def personName = user?.personName
        def fullName = personName?.fullName?.trim()
        if (fullName) {
            return fullName
        }

        def combinedName = "${personName?.givenName ?: ''} ${personName?.familyName ?: ''}".trim()
        combinedName ?: (user?.username?.trim() ?: '')
    }
    def requestedByForLine = { line ->
        plainUserName(line?.deliveryReceipt?.purchaseOrder?.requestedBy)
                ?: plainUserName(line?.deliveryReceipt?.supplierOrder?.requestedBy)
                ?: ''
    }
    def receivedOrAcceptedByForLine = { line ->
        plainUserName(line?.deliveryReceipt?.creator) ?: plainUserName(line?.creator) ?: ''
    }
%>
<h2>${ ui.message("rwandapharmacymanagement.inventory.receiptHistory.title") }</h2>
<table data-no-pagination="true">
    <thead><tr><th>Date</th><th>Receipt</th><th>Item</th><th>Batch</th><th>Expiry</th><th>Requested Qty</th><th>Accepted Qty</th><th>Requested By</th><th>Received / Accepted By</th><th>Unit Price</th></tr></thead>
    <tbody>
    <% receiptLines.each { line -> %>
    <tr>
        <td>${ line.dateCreated }</td>
        <td>${ line.deliveryReceipt?.receiptNumber }</td>
        <td>${ line.item?.name }</td>
        <td>${ line.batchNumber ?: '' }</td>
        <td>${ line.expiryDate ?: '' }</td>
        <td>${ line.orderedQuantity ?: '' }</td>
        <td>${ line.acceptedQuantity ?: '' }</td>
        <td>${ requestedByForLine(line) }</td>
        <td>${ receivedOrAcceptedByForLine(line) }</td>
        <td>${ line.unitPrice ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
<div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-top:10px;">
    <div style="font-size:12px;color:#666;">
        <% if ((receiptTotalItems ?: 0) > 0) { %>
        Showing ${ receiptStartItem }-${ receiptEndItem } of ${ receiptTotalItems }
        <% } else { %>
        No rows
        <% } %>
    </div>
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <form method="get" style="margin:0;display:flex;align-items:center;gap:6px;">
            <label style="font-size:11px;color:#555;">Rows per page</label>
            <select name="pageSize" onchange="this.form.submit()" style="width:auto;min-width:70px;">
                <% receiptPageSizes.each { option -> %>
                <option value="${ option }" ${ option == receiptPageSize ? 'selected="selected"' : '' }>${ option }</option>
                <% } %>
            </select>
            <input type="hidden" name="page" value="1"/>
        </form>
        <% if (receiptHasPrevious) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "inventory/receiptHistory", [page: receiptPage - 1, pageSize: receiptPageSize]) }">Previous</a>
        <% } else { %>
        <span style="opacity:0.55;">Previous</span>
        <% } %>
        <span style="font-size:12px;color:#666;">Page ${ receiptPage } of ${ receiptTotalPages }</span>
        <% if (receiptHasNext) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "inventory/receiptHistory", [page: receiptPage + 1, pageSize: receiptPageSize]) }">Next</a>
        <% } else { %>
        <span style="opacity:0.55;">Next</span>
        <% } %>
    </div>
</div>
