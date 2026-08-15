<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def exportParams = [startDate: startDate, endDate: endDate, requestType: selectedRequestType]
    if (selectedPharmacyLocationId) {
        exportParams.pharmacyLocationId = selectedPharmacyLocationId
    }
    def exportExcelUrl = ui.pageLink("rwandapharmacymanagement", "report/paymentExceptionReportExport", exportParams + [format: "excel"])
    def exportPdfUrl = ui.pageLink("rwandapharmacymanagement", "report/paymentExceptionReportExport", exportParams + [format: "pdf"])
%>
<h2>Payment Exception Report</h2>
<p style="color:#64748b;">Approved requests that were authorized to be dispensed without confirmed payment.</p>

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
    <div><strong>Approved requests:</strong> ${ totals?.requestCount ?: 0 }</div>
    <div><strong>Dispensed:</strong> ${ totals?.dispensedCount ?: 0 }</div>
    <div><strong>Pending dispense:</strong> ${ totals?.pendingDispenseCount ?: 0 }</div>
    <div><strong>Prescriptions:</strong> ${ totals?.prescriptionCount ?: 0 }</div>
    <div><strong>Consumables:</strong> ${ totals?.consumableCount ?: 0 }</div>
</div>

<table>
    <thead>
    <tr>
        <th>Approval</th>
        <th>Request</th>
        <th>Patient</th>
        <th>Type / status</th>
        <th>Exception</th>
        <th>Locations</th>
        <th>Items</th>
        <th>Dispense status</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="8">No payment exception approvals found for the selected filters.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>
            ${ row.approvedDate ?: '' }<br/>
            <span style="color:#666;">By: ${ row.approvedBy ?: '' }</span>
        </td>
        <td>
            #${ row.requestId ?: '' }<br/>
            <span style="color:#666;">Created: ${ row.requestDate ?: '' }</span><br/>
            <span style="color:#666;">Requested by: ${ row.requestedBy ?: '' }</span><br/>
            <span style="color:#666;">Reference: ${ row.requestReference ?: '' }</span><br/>
            <span style="color:#666;">Bill: ${ row.billId ?: '' } | Paid: ${ row.paymentConfirmed ? 'Yes' : 'No' }</span>
        </td>
        <td>
            ${ row.patientName ?: '' }<br/>
            <span style="color:#666;">${ row.patientIdentifier ?: '' }</span>
        </td>
        <td>
            ${ row.requestType ?: '' }<br/>
            <span style="color:#666;">${ row.requestStatus ?: '' }</span>
        </td>
        <td>
            <strong>${ row.exceptionType ?: '' }</strong><br/>
            ${ row.exceptionReason ?: '' }<br/>
            <span style="color:#666;">${ row.exceptionNote ?: '' }</span>
        </td>
        <td>
            Requesting: ${ row.requestingLocation ?: '' }<br/>
            <span style="color:#666;">Target: ${ row.targetPharmacy ?: '' }</span>
        </td>
        <td>
            ${ row.items ?: '' }<br/>
            <span style="color:#666;">Lines: ${ row.itemCount ?: 0 }</span>
        </td>
        <td>
            ${ row.dispensed ? 'Dispensed' : 'Pending dispense' }<br/>
            <span style="color:#666;">${ row.dispenseDates ?: '' }</span><br/>
            <span style="color:#666;">${ row.dispensingPharmacies ?: '' }</span><br/>
            <span style="color:#666;">${ row.dispensedBy ?: '' }</span>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>
