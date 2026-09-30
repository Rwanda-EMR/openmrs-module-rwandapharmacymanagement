<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def exportParams = [startDate: startDate, endDate: endDate, requestType: selectedRequestType]
    if (selectedPharmacyLocationId) {
        exportParams.pharmacyLocationId = selectedPharmacyLocationId
    }
    def exportExcelUrl = ui.pageLink("rwandapharmacymanagement", "report/patientRequestReportExport", exportParams + [format: "excel"])
    def exportPdfUrl = ui.pageLink("rwandapharmacymanagement", "report/patientRequestReportExport", exportParams + [format: "pdf"])
%>
<h2>Patient Requests to Pharmacy</h2>

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
    <div><strong>Requests:</strong> ${ totals?.requestCount ?: 0 }</div>
    <div><strong>Lines:</strong> ${ totals?.lineCount ?: 0 }</div>
    <div><strong>Prescriptions:</strong> ${ totals?.prescriptionLineCount ?: 0 }</div>
    <div><strong>Consumables:</strong> ${ totals?.consumableLineCount ?: 0 }</div>
    <div><strong>Total quantity:</strong> ${ totals?.requestedQuantity ?: 0 }</div>
</div>

<table>
    <thead>
    <tr>
        <th>Request</th>
        <th>Patient</th>
        <th>Type / status</th>
        <th>Locations</th>
        <th>Item</th>
        <th>Quantity</th>
        <th>Billing</th>
        <th>Reference</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="8">No patient dispensing requests found for the selected filters.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>
            #${ row.requestId ?: '' }<br/>
            <span style="color:#666;">${ row.requestDate ?: '' }</span><br/>
            <span style="color:#666;">By: ${ row.requestedBy ?: '' }</span>
        </td>
        <td>
            ${ row.patientName ?: '' }<br/>
            <span style="color:#666;">${ row.patientIdentifier ?: '' }</span>
        </td>
        <td>
            ${ row.requestType ?: '' }<br/>
            <span style="color:#666;">Request: ${ row.requestStatus ?: '' }</span><br/>
            <span style="color:#666;">Line: ${ row.lineStatus ?: '' }</span>
        </td>
        <td>
            Requesting: ${ row.requestingLocation ?: '' }<br/>
            <span style="color:#666;">Target: ${ row.targetPharmacy ?: '' }</span><br/>
            <span style="color:#666;">Routed: ${ row.routedPharmacy ?: '' }</span>
        </td>
        <td>
            <strong>${ row.itemName ?: '' }</strong><br/>
            <span style="color:#666;">${ row.itemCode ?: '' }</span>
        </td>
        <td>
            ${ row.requestedQuantity ?: '' } ${ row.unitOfMeasure ?: '' }
        </td>
        <td>
            Billable: ${ row.billable == false ? 'No' : 'Yes' }<br/>
            <span style="color:#666;">Bill: ${ row.patientBillId ?: '' }</span><br/>
            <span style="color:#666;">Paid: ${ row.paymentConfirmed ? 'Yes' : 'No' } | Exception: ${ row.paymentExceptionApproved ? 'Yes' : 'No' }</span><br/>
            <span style="color:#666;">${ row.billingExclusionReason ?: '' }</span>
        </td>
        <td>
            ${ row.reference ?: '' }<br/>
            <span style="color:#666;">Line: ${ row.requestLineId ?: '' }</span>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>
