<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Dispensing Report</h2>

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
    <button type="submit">Load report</button>
</form>

<p>
    <a href="/${ contextPath }/module/rwandapharmacymanagement/report/dispensingReportExport.page?format=excel&startDate=${ startDate }&endDate=${ endDate }${ selectedPharmacyLocationId ? '&pharmacyLocationId=' + selectedPharmacyLocationId : '' }">Export Excel</a>
    |
    <a href="/${ contextPath }/module/rwandapharmacymanagement/report/dispensingReportExport.page?format=pdf&startDate=${ startDate }&endDate=${ endDate }${ selectedPharmacyLocationId ? '&pharmacyLocationId=' + selectedPharmacyLocationId : '' }">Export PDF</a>
</p>

<div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin:12px 0;">
    <div><strong>Dispensed lines:</strong> ${ totals?.lineCount ?: 0 }</div>
    <div><strong>Total quantity:</strong> ${ totals?.quantityDispensed ?: 0 }</div>
    <div><strong>Total value:</strong> ${ totals?.lineTotal ?: 0 }</div>
</div>

<table>
    <thead>
    <tr>
        <th>Dispense date</th>
        <th>Patient</th>
        <th>Identifier</th>
        <th>Pharmacy</th>
        <th>Item dispensed</th>
        <th>Batch / expiry</th>
        <th>Quantity</th>
        <th>Unit price</th>
        <th>Total</th>
        <th>Prescription info</th>
        <th>Dispensed by</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="11">No dispensing records found for the selected filters.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>${ row.dispenseDate ?: '' }</td>
        <td>${ row.patientName ?: '' }</td>
        <td>${ row.patientIdentifier ?: '' }</td>
        <td>${ row.pharmacyName ?: '' }</td>
        <td>
            <strong>${ row.itemName ?: '' }</strong><br/>
            <span style="color:#666;">${ row.itemCode ?: '' }</span>
        </td>
        <td>
            ${ row.batchNumber ?: '' }<br/>
            <span style="color:#666;">${ row.expiryDate ?: '' }</span>
        </td>
        <td>${ row.quantityDispensed ?: 0 }</td>
        <td>${ row.sellingPrice ?: '' }</td>
        <td>${ row.lineTotal ?: '' }</td>
        <td>
            <% if (row.dispensedWithoutOrder) { %>
            Dispensed without order
            <% } else { %>
            Request: ${ row.requestId ?: '' }<br/>
            Reference: ${ row.prescriptionReference ?: '' }<br/>
            Bill: ${ row.patientBillId ?: '' }<br/>
            Status: ${ row.requestStatus ?: '' }
            <% } %>
        </td>
        <td>${ row.dispensedBy ?: '' }</td>
    </tr>
    <% } } %>
    </tbody>
</table>
