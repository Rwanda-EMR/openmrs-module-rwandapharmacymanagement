<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Dispensing Sales Summary</h2>
<h3>Revenue By Pharmacy</h3>
<table>
    <thead><tr><th>Pharmacy</th><th>Total Sales</th></tr></thead>
    <tbody>
    <% pharmacyTotals.each { entry -> %>
    <tr>
        <td>${ entry.key }</td>
        <td>${ entry.value }</td>
    </tr>
    <% } %>
    </tbody>
</table>
<h3>Revenue By Item</h3>
<table>
    <thead><tr><th>Item</th><th>Total Sales</th></tr></thead>
    <tbody>
    <% itemTotals.each { entry -> %>
    <tr>
        <td>${ entry.key }</td>
        <td>${ entry.value }</td>
    </tr>
    <% } %>
    </tbody>
</table>
<h3>Recent Dispenses</h3>
<table>
    <thead><tr><th>Date</th><th>Pharmacy</th><th>Patient</th><th>Line Total</th></tr></thead>
    <tbody>
    <% dispenses.each { dispense -> %>
        <% dispense.lines?.each { line -> %>
        <tr>
            <td>${ dispense.dateCreated }</td>
            <td>${ dispense.dispensingPharmacyLocation?.name ?: '' }</td>
            <td>${ dispense.patient?.patientId ?: '' }</td>
            <td>${ line.lineTotal ?: '' }</td>
        </tr>
        <% } %>
    <% } %>
    </tbody>
</table>
