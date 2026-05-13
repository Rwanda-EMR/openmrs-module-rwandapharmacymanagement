<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.supplier.orderForm.title") }</h2>
<p>Procurement orders use the latest synced supplier price as a planning estimate. Delivered stock valuation still uses the actual supplier price captured at receipt time.</p>
<form method="post">
    <table id="supplier-order-lines">
        <thead><tr><th>Item</th><th>Requested Quantity</th></tr></thead>
        <tbody>
        <tr>
            <td><select name="itemId"><option value="">Select item</option><% items.each { %><option value="${ it.itemId }">${ it.name } (${ it.code ?: '' })</option><% } %></select></td>
            <td><input type="number" step="0.01" name="quantity"/></td>
        </tr>
        </tbody>
    </table>
    <p><button type="button" onclick="addSupplierOrderLine()">Add Order Line</button></p>
    <p><button type="submit">Submit Supplier Order</button></p>
</form>
<template id="supplier-order-line-template">
    <tr>
        <td><select name="itemId"><option value="">Select item</option><% items.each { %><option value="${ it.itemId }">${ it.name } (${ it.code ?: '' })</option><% } %></select></td>
        <td><input type="number" step="0.01" name="quantity"/></td>
    </tr>
</template>
<script type="text/javascript">
    function addSupplierOrderLine() {
        var tbody = document.getElementById('supplier-order-lines').getElementsByTagName('tbody')[0];
        var template = document.getElementById('supplier-order-line-template');
        if (template && template.content && template.content.firstElementChild) {
            tbody.appendChild(template.content.firstElementChild.cloneNode(true));
        }
    }
</script>
