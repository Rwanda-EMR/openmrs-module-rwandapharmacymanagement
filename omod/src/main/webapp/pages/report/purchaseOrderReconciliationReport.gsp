<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def exportParams = [startDate: startDate, endDate: endDate]
    if (selectedPharmacyLocationId) {
        exportParams.pharmacyLocationId = selectedPharmacyLocationId
    }
    if (selectedPurchaseOrderId) {
        exportParams.purchaseOrderId = selectedPurchaseOrderId
    }
    def exportExcelUrl = ui.pageLink("rwandapharmacymanagement", "report/purchaseOrderReconciliationReportExport", exportParams + [format: "excel"])
    def exportPdfUrl = ui.pageLink("rwandapharmacymanagement", "report/purchaseOrderReconciliationReportExport", exportParams + [format: "pdf"])
%>
<h2>Purchase Order Reconciliation</h2>

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
    <div>
        <label>Purchase order</label><br/>
        <select name="purchaseOrderId">
            <option value="">All purchase orders</option>
            <% purchaseOrders.each { purchaseOrder -> %>
            <option value="${ purchaseOrder.purchaseOrderId }" ${ selectedPurchaseOrderId == purchaseOrder.purchaseOrderId ? 'selected="selected"' : '' }>
                PO-${ purchaseOrder.purchaseOrderId }${ purchaseOrder.supplier ? ' - ' + purchaseOrder.supplier.name : '' }
            </option>
            <% } %>
        </select>
    </div>
    <button type="submit">Load report</button>
</form>

<p>
    <a href="${ exportExcelUrl }">Export Excel</a>
    |
    <a href="${ exportPdfUrl }">Export PDF</a>
</p>

<div style="display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:10px;margin:12px 0;">
    <div><strong>Purchase orders:</strong> ${ totals?.purchaseOrderCount ?: 0 }</div>
    <div><strong>Receipts:</strong> ${ totals?.receiptCount ?: 0 }</div>
    <div><strong>Lines:</strong> ${ totals?.lineCount ?: 0 }</div>
    <div><strong>Ordered:</strong> ${ totals?.orderedQuantity ?: 0 }</div>
    <div><strong>Received:</strong> ${ totals?.receivedQuantity ?: 0 }</div>
    <div><strong>Difference:</strong> ${ totals?.differenceQuantity ?: 0 }</div>
</div>

<div style="display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin:12px 0;">
    <div><strong>Fully received:</strong> ${ totals?.fullyReceivedLineCount ?: 0 }</div>
    <div><strong>Partially received:</strong> ${ totals?.partiallyReceivedLineCount ?: 0 }</div>
    <div><strong>Not received:</strong> ${ totals?.notReceivedLineCount ?: 0 }</div>
    <div><strong>Receipt-only:</strong> ${ totals?.receiptOnlyLineCount ?: 0 }</div>
</div>

<table>
    <thead>
    <tr>
        <th>Purchase order</th>
        <th>Receipt</th>
        <th>Pharmacy</th>
        <th>Item</th>
        <th>Ordered</th>
        <th>Received</th>
        <th>Difference</th>
        <th>Status</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="8">No purchase order or receipt rows found for the selected filters.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>
            ${ row.purchaseOrderReference ?: '' }<br/>
            <span style="color:#666;">${ row.supplierName ?: '' }</span><br/>
            <span style="color:#666;">${ row.orderDate ?: '' }</span>
        </td>
        <td>
            ${ row.receiptReference ?: '' }<br/>
            <span style="color:#666;">${ row.receiptDate ?: '' }</span>
        </td>
        <td>${ row.pharmacyName ?: '' }</td>
        <td>
            <strong>${ row.itemName ?: '' }</strong><br/>
            <span style="color:#666;">${ row.itemCode ?: '' }</span><br/>
            <span style="color:#666;">${ row.program ?: '' }</span>
        </td>
        <td>${ row.orderedQuantity ?: 0 }</td>
        <td>${ row.receivedQuantity ?: 0 }</td>
        <td>${ row.differenceQuantity ?: 0 }</td>
        <td>${ row.status ?: '' }</td>
    </tr>
    <% } } %>
    </tbody>
</table>
