<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>

<h2>Stocked Drugs Missing Billing Service Names</h2>

<p>
    Showing stocked pharmacy drugs that do not have an active MOH Billing facility service price whose
    name, short name, or description exactly matches the pharmacy item or OpenMRS drug name.
</p>

<p><strong>${ rowCount ?: 0 }</strong> stocked drug${ rowCount == 1 ? '' : 's' } missing an exact billing name match.</p>

<table>
    <thead>
    <tr>
        <th>Pharmacy item</th>
        <th>Drug</th>
        <th>Concepts</th>
        <th>On hand</th>
        <th>Pharmacies</th>
        <th>Possible facility service names</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows || rows.isEmpty()) { %>
    <tr>
        <td colspan="6">All stocked drugs have exact active billing service name matches.</td>
    </tr>
    <% } %>
    <% rows?.each { row -> %>
    <tr>
        <td>
            <strong>${ row.item?.name ?: '' }</strong><br/>
            <span style="color:#666;">${ row.item?.code ?: '' }</span>
        </td>
        <td>${ row.drugName ?: '' }</td>
        <td>
            Item: ${ row.itemConceptId ?: '' }<br/>
            Drug: ${ row.drugConceptId ?: '' }
        </td>
        <td>${ row.quantityOnHand ?: '' }</td>
        <td>${ row.pharmacies?.join(', ') ?: '' }</td>
        <td>
            <% if (row.candidateFacilityServices && !row.candidateFacilityServices.isEmpty()) { %>
            <ul style="margin:0;padding-left:16px;">
                <% row.candidateFacilityServices.each { serviceName -> %>
                <li>${ serviceName }</li>
                <% } %>
            </ul>
            <% } else { %>
            <span style="color:#777;">No active similar service found</span>
            <% } %>
        </td>
    </tr>
    <% } %>
    </tbody>
</table>
