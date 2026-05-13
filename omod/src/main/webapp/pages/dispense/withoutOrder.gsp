<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.dispense.withoutOrder.title") }</h2>
<% if (pharmacySelectionRequired) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;">
    <form method="get" style="margin:0;">
        <div style="margin-bottom:6px;"><strong>Choose pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.</div>
        <select name="pharmacyLocationId" required="required">
            <option value="">Select pharmacy</option>
            <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
        </select>
        <button type="submit">Use pharmacy</button>
    </form>
</div>
<% } else if (!activePharmacy) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;">
    No pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.
</div>
<% } %>
<% if (activePharmacy) { %><p><strong>Dispensing Pharmacy:</strong> ${ activePharmacy.name }</p><% } %>
<% if (activePharmacy) { %>
<h3>Available Stock And Suggested Selling Price</h3>
<table>
    <thead><tr><th>Item</th><th>Batch</th><th>On Hand</th><th>Cost Price</th><th>Selling Price</th></tr></thead>
    <tbody>
    <% stockBalances.each { %>
    <tr>
        <td>${ it.item?.name }</td>
        <td>${ it.batchNumber ?: '' }</td>
        <td>${ it.quantityOnHand ?: '' }</td>
        <td>${ it.unitPrice ?: '' }</td>
        <td>${ pricingService.calculateSellingPrice(it.unitPrice) ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
<p>Displayed selling price is a preview. The actual selling price is frozen and persisted when dispensing is saved.</p>
<form method="post">
    <p><label>Patient ID</label><br/><input type="number" name="patientId"/></p>
    <h3>Items To Dispense</h3>
    <table id="without-order-lines">
        <thead><tr><th>Item</th><th>Quantity</th><th>Batch Number</th></tr></thead>
        <tbody>
        <tr>
            <td><select name="itemId"><option value="">Select item</option><% items.each { %><option value="${ it.itemId }">${ it.name }</option><% } %></select></td>
            <td><input type="number" step="0.01" name="quantity"/></td>
            <td><input type="text" name="batchNumber"/></td>
        </tr>
        </tbody>
    </table>
    <p><button type="button" onclick="addWithoutOrderLine()">Add Item Line</button></p>
    <p><button type="submit">Dispense Without Order</button></p>
</form>
<template id="without-order-line-template">
    <tr>
        <td><select name="itemId"><option value="">Select item</option><% items.each { %><option value="${ it.itemId }">${ it.name }</option><% } %></select></td>
        <td><input type="number" step="0.01" name="quantity"/></td>
        <td><input type="text" name="batchNumber"/></td>
    </tr>
</template>
<script type="text/javascript">
    function addWithoutOrderLine() {
        var tbody = document.getElementById('without-order-lines').getElementsByTagName('tbody')[0];
        var template = document.getElementById('without-order-line-template');
        if (template && template.content && template.content.firstElementChild) {
            tbody.appendChild(template.content.firstElementChild.cloneNode(true));
        }
    }
</script>
<% } %>
