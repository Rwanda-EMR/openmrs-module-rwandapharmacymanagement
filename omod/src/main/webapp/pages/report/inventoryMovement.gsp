<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Inventory Movement Report</h2>
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
        <label>Material type</label><br/>
        <select name="materialType">
            <option value="">All material types</option>
            <% materialTypes.each { option -> %>
            <option value="${ option }" ${ selectedMaterialType == option ? 'selected="selected"' : '' }>${ option }</option>
            <% } %>
        </select>
    </div>
    <div>
        <label>Program</label><br/>
        <select name="program">
            <option value="">All programs</option>
            <% programs.each { option -> %>
            <option value="${ option }" ${ selectedProgram == option ? 'selected="selected"' : '' }>${ option }</option>
            <% } %>
        </select>
    </div>
    <div>
        <label>Mode</label><br/>
        <select name="mode">
            <option value="summary" ${ mode == 'summary' ? 'selected="selected"' : '' }>Summary</option>
            <option value="detail" ${ mode == 'detail' ? 'selected="selected"' : '' }>Detailed by batch</option>
        </select>
    </div>
    <button type="submit">Load report</button>
</form>

<p>
    <a href="${ ui.pageLink('rwandapharmacymanagement', 'report/inventoryMovementExport', [format: 'excel', startDate: startDate, endDate: endDate, pharmacyLocationId: selectedPharmacyLocationId, materialType: selectedMaterialType, program: selectedProgram, mode: mode]) }">Export Excel</a>
    |
    <a href="${ ui.pageLink('rwandapharmacymanagement', 'report/inventoryMovementExport', [format: 'pdf', startDate: startDate, endDate: endDate, pharmacyLocationId: selectedPharmacyLocationId, materialType: selectedMaterialType, program: selectedProgram, mode: mode]) }">Export PDF</a>
</p>

<div style="display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;margin:12px 0;">
    <div><strong>Opening:</strong> ${ totals.openingQty }</div>
    <div><strong>Received:</strong> ${ totals.receivedQty }</div>
    <div><strong>Dispensed:</strong> ${ totals.dispensedQty }</div>
    <div><strong>Closing:</strong> ${ totals.closingQty }</div>
    <div><strong>Transfer in:</strong> ${ totals.transferInQty }</div>
    <div><strong>Transfer out:</strong> ${ totals.transferOutQty }</div>
    <div><strong>Stock at hand:</strong> ${ totals.stockAtHand }</div>
    <div><strong>Stock at hand value:</strong> ${ totals.stockAtHandValue }</div>
</div>

<table>
    <thead>
    <tr>
        <th>Item</th>
        <th>Code</th>
        <th>Material Type</th>
        <th>Program</th>
        <th>Pharmacy</th>
        <th>Batch</th>
        <th>Expiry</th>
        <th>Opening</th>
        <th>Received</th>
        <th>Transfer In</th>
        <th>Adjustment In</th>
        <th>Dispensed</th>
        <th>Transfer Out</th>
        <th>Adjustment Out</th>
        <th>Closing</th>
        <th>Stock At Hand</th>
        <th>Unit Cost</th>
        <th>Closing Value</th>
        <th>Stock At Hand Value</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="19">No inventory movement found for the selected filters.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>${ row.itemName ?: '' }</td>
        <td>${ row.itemCode ?: '' }</td>
        <td>${ row.materialType ?: '' }</td>
        <td>${ row.program ?: '' }</td>
        <td>${ row.pharmacyName ?: '' }</td>
        <td>${ row.batchNumber ?: '' }</td>
        <td>${ row.expiryDate ? row.expiryDate.format('yyyy-MM-dd') : '' }</td>
        <td>${ row.openingQty }</td>
        <td>${ row.receivedQty }</td>
        <td>${ row.transferInQty }</td>
        <td>${ row.adjustmentInQty }</td>
        <td>${ row.dispensedQty }</td>
        <td>${ row.transferOutQty }</td>
        <td>${ row.adjustmentOutQty }</td>
        <td>${ row.closingQty }</td>
        <td>${ row.stockAtHand }</td>
        <td>${ row.unitCost ?: '' }</td>
        <td>${ row.closingValue }</td>
        <td>${ row.stockAtHandValue }</td>
    </tr>
    <% } } %>
    </tbody>
</table>
