<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.alerts.dashboard.title") }</h2>
<h3>Low Stock</h3>
<table>
    <thead><tr><th>Item</th><th>Location</th><th>On Hand</th></tr></thead>
    <tbody>
    <% lowStockAlerts.each { %>
    <tr><td>${ it.item?.name }</td><td>${ it.pharmacyLocation?.name }</td><td>${ it.quantityOnHand }</td></tr>
    <% } %>
    </tbody>
</table>
<h3>Near Expiry</h3>
<table>
    <thead><tr><th>Item</th><th>Location</th><th>Expiry</th><th>On Hand</th></tr></thead>
    <tbody>
    <% expiryAlerts.each { %>
    <tr><td>${ it.item?.name }</td><td>${ it.pharmacyLocation?.name }</td><td>${ it.expiryDate }</td><td>${ it.quantityOnHand }</td></tr>
    <% } %>
    </tbody>
</table>
