<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def exportParams = [itemId: selectedItemId, startDate: startDate, endDate: endDate]
    if (selectedPharmacyLocationId) {
        exportParams.pharmacyLocationId = selectedPharmacyLocationId
    }
    def exportExcelUrl = ui.pageLink("rwandapharmacymanagement", "report/itemMovementExport", exportParams + [format: "excel"])
    def exportPdfUrl = ui.pageLink("rwandapharmacymanagement", "report/itemMovementExport", exportParams + [format: "pdf"])
%>
<h2>Item Movement Report</h2>

<form method="get" style="margin-bottom:12px;display:flex;gap:10px;align-items:end;flex-wrap:wrap;">
    <div style="min-width:280px;">
        <label>Item</label><br/>
        <select name="itemId" required="required" style="width:100%;">
            <option value="">Select item</option>
            <% items.each { item -> %>
            <option value="${ item.itemId }" ${ selectedItemId == item.itemId ? 'selected="selected"' : '' }>
                ${ item.name } (${ item.code ?: '' }${ item.program ? ' - ' + item.program : '' })
            </option>
            <% } %>
        </select>
    </div>
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
    <button type="submit">Load report</button>
</form>

<% if (selectedItem) { %>
<p>
    <a href="${ exportExcelUrl }">Export Excel</a>
    |
    <a href="${ exportPdfUrl }">Export PDF</a>
</p>
<% } %>

<% if (selectedItem) { %>
<div style="display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:10px;margin:12px 0;">
    <div><strong>Item:</strong> ${ selectedItem.name }</div>
    <div><strong>Opening balance:</strong> ${ openingBalance }</div>
    <div><strong>Received:</strong> ${ totalReceived }</div>
    <div><strong>Issued:</strong> ${ totalIssued }</div>
    <div><strong>Closing balance:</strong> ${ closingBalance }</div>
    <div><strong>Current months of stock:</strong> ${ currentMonthsOfStock == null ? 'N/A' : currentMonthsOfStock }</div>
</div>
<% } %>

<table>
    <thead>
    <tr>
        <th>Date of movement</th>
        <th>Receiving pharmacy / user</th>
        <th>Issuing pharmacy</th>
        <th>Performed by</th>
        <th>Requested by</th>
        <th>Requested where</th>
        <th>Served by</th>
        <th>Served where</th>
        <th>Batch number</th>
        <th>Expiry date</th>
        <th>Quantity received</th>
        <th>Quantity issued</th>
        <th>Balance in stock</th>
        <th>Months of stock</th>
        <th>Movement type</th>
        <th>Reference</th>
        <th>Remarks</th>
    </tr>
    </thead>
    <tbody>
    <% if (!selectedItem) { %>
    <tr><td colspan="17">Select an item to load the movement report.</td></tr>
    <% } else if (!rows) { %>
    <tr><td colspan="17">No movements found for the selected item and period.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>${ row.movementDate ?: '' }</td>
        <td>${ row.receivingPharmacyOrUser ?: '' }</td>
        <td>${ row.issuingPharmacy ?: '' }</td>
        <td>${ row.performedBy ?: '' }</td>
        <td>${ row.requestedBy ?: '' }</td>
        <td>${ row.requestedWhere ?: '' }</td>
        <td>${ row.servedBy ?: '' }</td>
        <td>${ row.servedWhere ?: '' }</td>
        <td>${ row.batchNumber ?: '' }</td>
        <td>${ row.expiryDate ?: '' }</td>
        <td>${ row.quantityReceived }</td>
        <td>${ row.quantityIssued }</td>
        <td>${ row.balanceInStock }</td>
        <td>${ row.monthsOfStock == null ? 'N/A' : row.monthsOfStock }</td>
        <td>${ row.transactionType ?: '' }</td>
        <td>${ row.referenceNumber ?: '' }</td>
        <td>${ row.remarks ?: '' }</td>
    </tr>
    <% } } %>
    </tbody>
</table>
