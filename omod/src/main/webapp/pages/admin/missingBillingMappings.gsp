<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>

<h2>Stocked Items Missing Billing Services</h2>

<% if (message) { %><p style="padding:8px;background:#e8f5e9;color:#1b5e20;">${ message }</p><% } %>
<% if (error) { %><p style="padding:8px;background:#ffebee;color:#b71c1c;">${ error }</p><% } %>

<p>
    Showing stocked pharmacy drugs and consumables that do not have an active MOH Billing facility service price and
    insurance billable service whose name, short name, or description exactly matches the pharmacy item or OpenMRS drug name.
</p>

<p><strong>${ rowCount ?: 0 }</strong> stocked item${ rowCount == 1 ? '' : 's' } missing an exact billing name match.</p>

<table>
    <thead>
    <tr>
        <th>Pharmacy item</th>
        <th>Type</th>
        <th>Drug</th>
        <th>Concepts</th>
        <th>On hand</th>
        <th>Pharmacies</th>
        <th>Highest current batch cost</th>
        <th>Markup</th>
        <th>Selling price</th>
        <th>Possible facility service names</th>
        <th>Action</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows || rows.isEmpty()) { %>
    <tr>
        <td colspan="11">All stocked items have active billing service matches.</td>
    </tr>
    <% } %>
    <% rows?.each { row -> %>
    <tr>
        <td>
            <strong>${ row.item?.name ?: '' }</strong><br/>
            <span style="color:#666;">${ row.item?.code ?: '' }</span>
        </td>
        <td>${ row.itemType ?: '' }</td>
        <td>${ row.drugName ?: '' }</td>
        <td>
            Item: ${ row.itemConceptId ?: '' }<br/>
            Drug: ${ row.drugConceptId ?: '' }
        </td>
        <td>${ row.quantityOnHand ?: '' }</td>
        <td>${ row.pharmacies?.join(', ') ?: '' }</td>
        <td>${ row.currentStockCost == null ? 'No current cost' : row.currentStockCost }</td>
        <td>${ row.markupPercentage ?: 0 }%</td>
        <td>
            <% if (canCreateBillingMappings && row.calculatedPrice != null) { %>
            <input type="number" name="displayedPrice" form="create-billing-${ row.item?.itemId }"
                   value="${ row.calculatedPrice }" min="0" step="0.01" required="required"
                   aria-label="Selling price"/>
            <% } else { %>
            <strong>${ row.calculatedPrice == null ? 'Unavailable' : row.calculatedPrice }</strong>
            <% } %>
            <div style="color:#666;font-size:12px;">Calculated from highest current batch cost and markup; editable.</div>
        </td>
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
        <td>
            <% if (canCreateBillingMappings && row.calculatedPrice != null) { %>
            <form method="post" id="create-billing-${ row.item?.itemId }" style="margin:0;">
                <input type="hidden" name="action" value="createBillingMapping"/>
                <input type="hidden" name="itemId" value="${ row.item?.itemId }"/>
                <button type="submit">Create billing service</button>
            </form>
            <% } else if (!canCreateBillingMappings) { %>
            <span style="color:#777;">Enable creation in Admin Settings</span>
            <% } else { %>
            <span style="color:#777;">Cost required before creation</span>
            <% } %>
        </td>
    </tr>
    <% } %>
    </tbody>
</table>
