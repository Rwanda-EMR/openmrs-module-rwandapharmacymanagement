<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Supplier Price History</h2>
<table>
    <thead><tr><th>Effective Date</th><th>Supplier Code</th><th>Local Item</th><th>Unit Price</th><th>Currency</th></tr></thead>
    <tbody>
    <% supplierPriceHistory.each { %>
    <tr>
        <td>${ it.effectiveDate ?: it.dateCreated }</td>
        <td>${ it.supplierProductCode }</td>
        <td>${ it.item?.name ?: 'Unmapped' }</td>
        <td>${ it.unitPrice }</td>
        <td>${ it.currency ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
