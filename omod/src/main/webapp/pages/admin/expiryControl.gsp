<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Main Pharmacy Batch Expiry Control</h2>
<% if (updated) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #b7e1c1;background:#edf9f0;color:#256029;border-radius:4px;">
    Batch expiry date updated.
</div>
<% } %>
<% if (writtenOff) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #b7e1c1;background:#edf9f0;color:#256029;border-radius:4px;">
    Stock written off and removed from available inventory.
</div>
<% } %>
<% if (disposed) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #b7e1c1;background:#edf9f0;color:#256029;border-radius:4px;">
    Written-off stock disposal recorded.
</div>
<% } %>
<% if (!activePharmacy) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;">
    No active main pharmacy is configured.
</div>
<% } %>
<% if (activePharmacy) { %>
<p><strong>Main pharmacy:</strong> ${ activePharmacy.name }</p>
<form method="get" style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;margin:0 0 12px;">
    <input type="hidden" name="pharmacyLocationId" value="${ activePharmacy.pharmacyLocationId }"/>
    <input type="search" name="q" value="${ q ?: '' }" placeholder="Search item name, code, programme, category, or batch" style="min-width:320px;padding:6px 8px;border:1px solid #ccc;border-radius:4px;"/>
    <button type="submit">Search</button>
    <% if (q) { %>
    <a href="${ ui.pageLink('rwandapharmacymanagement', 'admin/expiryControl', [pharmacyLocationId: activePharmacy.pharmacyLocationId]) }">Clear</a>
    <% } %>
</form>
<table>
    <thead>
    <tr>
        <th>Item</th>
        <th>Batch Number</th>
        <th>Quantity On Hand</th>
        <th>Current Expiry</th>
        <th>New Expiry</th>
        <th>Write-off</th>
        <th>Action</th>
    </tr>
    </thead>
    <tbody>
    <% stockBalances.each { balance -> %>
    <%
        def quantityOnHand = balance.quantityOnHand ?: 0
        def expired = balance.expiryDate && balance.expiryDate.before(new Date().clearTime())
    %>
    <tr>
        <td>${ balance.item?.name }</td>
        <td>${ balance.batchNumber ?: 'No batch' }</td>
        <td>${ quantityOnHand }</td>
        <td>
            ${ balance.expiryDate ? balance.expiryDate.format('dd/MMM/yyyy') : '' }
            <% if (expired && quantityOnHand > 0) { %>
            <span style="display:inline-block;margin-left:6px;padding:2px 6px;background:#d9534f;color:#fff;border-radius:4px;font-size:11px;">Expired</span>
            <% } %>
        </td>
        <td>
            <form method="post" id="expiry-form-${ balance.inventoryBalanceId }" style="margin:0;">
                <input type="hidden" name="action" value="updateExpiry"/>
                <input type="hidden" name="pharmacyLocationId" value="${ activePharmacy.pharmacyLocationId }"/>
                <input type="hidden" name="q" value="${ q ?: '' }"/>
                <input type="hidden" name="inventoryBalanceId" value="${ balance.inventoryBalanceId }"/>
                <input type="date" class="openmrs-date-field" name="expiryDate" value="${ balance.expiryDate ? balance.expiryDate.format('yyyy-MM-dd') : '' }" required="required"/>
            </form>
        </td>
        <td>
            <% if (expired && quantityOnHand > 0) { %>
            <form method="post" id="writeoff-form-${ balance.inventoryBalanceId }" style="margin:0;display:grid;gap:4px;min-width:220px;">
                <input type="hidden" name="action" value="writeOff"/>
                <input type="hidden" name="pharmacyLocationId" value="${ activePharmacy.pharmacyLocationId }"/>
                <input type="hidden" name="q" value="${ q ?: '' }"/>
                <input type="hidden" name="inventoryBalanceId" value="${ balance.inventoryBalanceId }"/>
                <input type="number" name="quantity" step="0.01" min="0.01" max="${ quantityOnHand }" value="${ quantityOnHand }" required="required"/>
                <input type="text" name="reason" placeholder="Write-off reason" value="Expired stock" required="required"/>
            </form>
            <% } else { %>
            <span style="color:#777;">Only expired batches with stock can be written off here.</span>
            <% } %>
        </td>
        <td>
            <button type="submit" form="expiry-form-${ balance.inventoryBalanceId }">Update</button>
            <% if (expired && quantityOnHand > 0) { %>
            <button type="submit" form="writeoff-form-${ balance.inventoryBalanceId }" onclick="return confirm('Write off this stock and remove it from available inventory?');">Write off</button>
            <% } %>
        </td>
    </tr>
    <% } %>
    <% if (!stockBalances) { %>
    <tr><td colspan="7">${ q ? 'No stock batches match that search.' : 'No stock batches are available for this pharmacy.' }</td></tr>
    <% } %>
    </tbody>
</table>

<h3 style="margin-top:24px;">Written-off Stock Awaiting Disposal</h3>
<p>
    <a href="${ ui.pageLink('rwandapharmacymanagement', 'admin/expiryControlExport') }">Download write-off and disposal report</a>
</p>
<table>
    <thead>
    <tr>
        <th>Date written off</th>
        <th>Item</th>
        <th>Batch</th>
        <th>Expiry</th>
        <th>Quantity</th>
        <th>Reason</th>
        <th>Disposal</th>
    </tr>
    </thead>
    <tbody>
    <% recentWriteOffs.each { tx ->
        def txId = tx.stockTransactionId?.toString()
        def alreadyDisposed = disposedWriteOffReferences?.contains(txId)
    %>
    <tr>
        <td>${ tx.dateCreated ? tx.dateCreated.format('dd/MMM/yyyy HH:mm') : '' }</td>
        <td>${ tx.item?.name ?: '' }</td>
        <td>${ tx.batchNumber ?: 'No batch' }</td>
        <td>${ tx.expiryDate ? tx.expiryDate.format('dd/MMM/yyyy') : '' }</td>
        <td>${ tx.quantity ? tx.quantity.abs() : 0 }</td>
        <td>${ tx.remarks ?: '' }</td>
        <td>
            <% if (alreadyDisposed) { %>
            <span style="display:inline-block;padding:2px 6px;background:#5cb85c;color:#fff;border-radius:4px;font-size:11px;">Disposed</span>
            <% } else { %>
            <form method="post" id="dispose-form-${ tx.stockTransactionId }" style="margin:0;display:grid;gap:4px;min-width:220px;">
                <input type="hidden" name="action" value="dispose"/>
                <input type="hidden" name="pharmacyLocationId" value="${ activePharmacy.pharmacyLocationId }"/>
                <input type="hidden" name="q" value="${ q ?: '' }"/>
                <input type="hidden" name="writeOffStockTransactionId" value="${ tx.stockTransactionId }"/>
                <input type="text" name="disposalMethod" placeholder="Disposal method" required="required"/>
                <input type="text" name="notes" placeholder="Notes / witness"/>
                <button type="submit" onclick="return confirm('Record physical disposal for this written-off stock?');">Record disposal</button>
            </form>
            <% } %>
        </td>
    </tr>
    <% } %>
    <% if (!recentWriteOffs) { %>
    <tr><td colspan="7">No write-offs found in the last 12 months.</td></tr>
    <% } %>
    </tbody>
</table>
<% } %>
