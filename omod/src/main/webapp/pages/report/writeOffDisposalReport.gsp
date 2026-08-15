<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Write-off and Disposal Report</h2>
<p style="color:#64748b;">Tracks stock removed from available inventory and whether the physical disposal was recorded.</p>

<form method="get" style="margin-bottom:12px;display:flex;gap:10px;align-items:end;flex-wrap:wrap;">
    <div>
        <label>Start date</label><br/>
        <input type="date" name="startDate" value="${ startDate ?: '' }"/>
    </div>
    <div>
        <label>End date</label><br/>
        <input type="date" name="endDate" value="${ endDate ?: '' }"/>
    </div>
    <div>
        <label>Pharmacy</label><br/>
        <select name="pharmacyLocationId">
            <option value="">All pharmacies</option>
            <% pharmacies.each { pharmacy -> %>
            <option value="${ pharmacy.pharmacyLocationId }" ${ selectedPharmacyLocationId == pharmacy.pharmacyLocationId ? 'selected="selected"' : '' }>${ pharmacy.name }</option>
            <% } %>
        </select>
    </div>
    <div style="min-width:260px;">
        <label>Item</label><br/>
        <select name="itemId" style="width:100%;">
            <option value="">All items</option>
            <% items.each { item -> %>
            <option value="${ item.itemId }" ${ selectedItemId == item.itemId ? 'selected="selected"' : '' }>${ item.name } (${ item.code ?: '' })</option>
            <% } %>
        </select>
    </div>
    <div>
        <label>Status</label><br/>
        <select name="status">
            <option value="ALL" ${ selectedStatus == 'ALL' ? 'selected="selected"' : '' }>All</option>
            <option value="PENDING_DISPOSAL" ${ selectedStatus == 'PENDING_DISPOSAL' ? 'selected="selected"' : '' }>Pending disposal</option>
            <option value="DISPOSED" ${ selectedStatus == 'DISPOSED' ? 'selected="selected"' : '' }>Disposed</option>
        </select>
    </div>
    <button type="submit">Load report</button>
</form>

<div style="display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:10px;margin:12px 0;">
    <div><strong>Write-offs:</strong> ${ totals?.writeOffCount ?: 0 }</div>
    <div><strong>Disposed:</strong> ${ totals?.disposedCount ?: 0 }</div>
    <div><strong>Pending disposal:</strong> ${ totals?.pendingDisposalCount ?: 0 }</div>
    <div><strong>Total quantity:</strong> ${ totals?.writeOffQuantity ?: 0 }</div>
    <div><strong>Total value:</strong> ${ totals?.writeOffValue ?: 0 }</div>
</div>

<table>
    <thead>
    <tr>
        <th>Write-off</th>
        <th>Pharmacy</th>
        <th>Item</th>
        <th>Batch / expiry</th>
        <th>Quantity / value</th>
        <th>Reason</th>
        <th>Disposal status</th>
        <th>Disposal details</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="8">No write-offs found for the selected filters.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>
            #${ row.writeOffTransactionId ?: '' }<br/>
            <span style="color:#666;">${ row.writeOffDate ?: '' }</span><br/>
            <span style="color:#666;">By: ${ row.writtenOffBy ?: '' }</span>
        </td>
        <td>${ row.pharmacyName ?: '' }</td>
        <td>
            ${ row.itemName ?: '' }<br/>
            <span style="color:#666;">${ row.itemCode ?: '' }</span>
        </td>
        <td>
            ${ row.batchNumber ?: 'No batch' }<br/>
            <span style="color:#666;">Expiry: ${ row.expiryDate ?: '' }</span>
        </td>
        <td>
            Qty: ${ row.writeOffQuantity ?: 0 }<br/>
            <span style="color:#666;">Unit: ${ row.unitPrice ?: '' }</span><br/>
            <span style="color:#666;">Value: ${ row.writeOffValue ?: 0 }</span>
        </td>
        <td>${ row.writeOffReason ?: '' }</td>
        <td>
            <% if (row.disposed) { %>
            <span style="display:inline-block;padding:2px 6px;background:#5cb85c;color:#fff;border-radius:4px;font-size:11px;">Disposed</span>
            <% } else { %>
            <span style="display:inline-block;padding:2px 6px;background:#f0ad4e;color:#fff;border-radius:4px;font-size:11px;">Pending disposal</span>
            <% } %>
        </td>
        <td>
            <% if (row.disposed) { %>
            Disposal #${ row.disposalTransactionId ?: '' }<br/>
            <span style="color:#666;">${ row.disposalDate ?: '' }</span><br/>
            <span style="color:#666;">${ row.disposalMethod ?: '' }</span><br/>
            <span style="color:#666;">By: ${ row.disposedBy ?: '' }</span>
            <% } else { %>
            <span style="color:#666;">Physical disposal not yet recorded.</span>
            <% } %>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>
