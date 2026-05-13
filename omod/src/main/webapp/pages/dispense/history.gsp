<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.dispense.history.title") }</h2>
<table>
    <thead><tr><th>Date</th><th>Patient</th><th>Pharmacy</th><th>Mode</th><th>Item</th><th>Batch</th><th>Qty</th><th>Cost Price</th><th>Selling Price</th><th>Line Total</th></tr></thead>
    <tbody>
    <% dispenses.each { dispense -> %>
        <% dispense.lines?.each { line -> %>
        <tr>
            <td>${ dispense.dateCreated }</td>
            <td>${ dispense.patient?.patientId ?: '' }</td>
            <td>${ dispense.dispensingPharmacyLocation?.name ?: '' }</td>
            <td>${ dispense.dispensedWithoutOrder ? 'Without Order' : 'With Order' }</td>
            <td>${ line.item?.name ?: '' }</td>
            <td>${ line.batchNumber ?: '' }</td>
            <td>${ line.quantityDispensed ?: '' }</td>
            <td>${ line.costPrice ?: '' }</td>
            <td>${ line.sellingPrice ?: '' }</td>
            <td>${ line.lineTotal ?: '' }</td>
        </tr>
        <% } %>
    <% } %>
    </tbody>
</table>
