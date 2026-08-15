<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def exportParams = [startDate: startDate, endDate: endDate, requestType: selectedRequestType]
    if (selectedPharmacyLocationId) {
        exportParams.pharmacyLocationId = selectedPharmacyLocationId
    }
    def exportExcelUrl = ui.pageLink("rwandapharmacymanagement", "report/notBillableItemsReportExport", exportParams + [format: "excel"])
    def exportPdfUrl = ui.pageLink("rwandapharmacymanagement", "report/notBillableItemsReportExport", exportParams + [format: "pdf"])
%>
<h2>Not Billable Items Report</h2>
<p style="color:#64748b;">Items intentionally excluded from billing during prescription or consumable ordering.</p>

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
        <label>Type</label><br/>
        <select name="requestType">
            <option value="ALL" ${ selectedRequestType == 'ALL' ? 'selected="selected"' : '' }>All</option>
            <% requestTypes.each { type -> %>
            <option value="${ type.name() }" ${ selectedRequestType == type.name() ? 'selected="selected"' : '' }>${ type.name().replace('_', ' ') }</option>
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

<div style="display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:10px;margin:12px 0;">
    <div><strong>Lines:</strong> ${ totals?.lineCount ?: 0 }</div>
    <div><strong>Prescriptions:</strong> ${ totals?.prescriptionCount ?: 0 }</div>
    <div><strong>Consumables:</strong> ${ totals?.consumableCount ?: 0 }</div>
    <div><strong>Dispensed:</strong> ${ totals?.dispensedCount ?: 0 }</div>
    <div><strong>Pending:</strong> ${ totals?.pendingCount ?: 0 }</div>
</div>

<table>
    <thead>
    <tr>
        <th>Request</th>
        <th>Patient</th>
        <th>Item</th>
        <th>Reason</th>
        <th>Requested by / where</th>
        <th>Billing</th>
        <th>Serve status</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="7">No not-billable items found for the selected filters.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>
            #${ row.requestId ?: '' } / line ${ row.requestLineId ?: '' }<br/>
            <span style="color:#666;">${ row.requestDate ?: '' }</span><br/>
            <span style="color:#666;">${ row.requestType ?: '' } | ${ row.requestStatus ?: '' }</span>
        </td>
        <td>
            ${ row.patientName ?: '' }<br/>
            <span style="color:#666;">${ row.patientIdentifier ?: '' }</span>
        </td>
        <td>
            ${ row.itemName ?: '' }<br/>
            <span style="color:#666;">${ row.itemCode ?: '' }</span><br/>
            <span style="color:#666;">Qty: ${ row.quantity ?: '' } ${ row.unitOfMeasure ?: '' }</span>
        </td>
        <td>${ row.reason ?: '' }</td>
        <td>
            ${ row.requestedBy ?: '' }<br/>
            <span style="color:#666;">Requesting: ${ row.requestingLocation ?: '' }</span><br/>
            <span style="color:#666;">Target: ${ row.targetPharmacy ?: '' }</span>
        </td>
        <td>
            Not billable<br/>
            <span style="color:#666;">Bill: ${ row.billId ?: '' } | Paid: ${ row.paymentConfirmed ? 'Yes' : 'No' }</span>
        </td>
        <td>
            ${ row.dispensed ? 'Dispensed' : 'Pending / not fully served' }<br/>
            <span style="color:#666;">Line: ${ row.lineStatus ?: '' }</span><br/>
            <span style="color:#666;">Qty dispensed: ${ row.quantityDispensed ?: '' }</span><br/>
            <span style="color:#666;">${ row.dispenseDates ?: '' }</span><br/>
            <span style="color:#666;">${ row.dispensedBy ?: '' }</span><br/>
            <span style="color:#666;">${ row.dispensingPharmacies ?: '' }</span>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>
