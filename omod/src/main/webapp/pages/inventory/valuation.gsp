<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.inventory.valuation.title") }</h2>
<table data-no-pagination="true">
    <thead><tr><th>Item</th><th>Location</th><th>Batch</th><th>On Hand</th><th>Cost Price</th><th>Cost Value</th><th>Selling Price</th><th>Selling Value</th></tr></thead>
    <tbody>
    <% valuationRows.each { row -> def balance = row.balance %>
    <tr>
        <td>${ balance.item?.name }</td>
        <td>${ balance.pharmacyLocation?.name }</td>
        <td>${ balance.batchNumber ?: '' }</td>
        <td>${ balance.quantityOnHand ?: '' }</td>
        <td>${ row.unitPrice ?: '' }</td>
        <td>${ row.totalValue ?: '' }</td>
        <td>${ row.sellingPrice ?: '' }</td>
        <td>${ row.totalSellingValue ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
<div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-top:10px;">
    <div style="font-size:12px;color:#666;">
        <% if ((valuationTotalItems ?: 0) > 0) { %>
        Showing ${ valuationStartItem }-${ valuationEndItem } of ${ valuationTotalItems }
        <% } else { %>
        No rows
        <% } %>
    </div>
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <form method="get" style="margin:0;display:flex;align-items:center;gap:6px;">
            <label style="font-size:11px;color:#555;">Rows per page</label>
            <select name="pageSize" onchange="this.form.submit()" style="width:auto;min-width:70px;">
                <% valuationPageSizes.each { option -> %>
                <option value="${ option }" ${ option == valuationPageSize ? 'selected="selected"' : '' }>${ option }</option>
                <% } %>
            </select>
            <input type="hidden" name="page" value="1"/>
        </form>
        <% if (valuationHasPrevious) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "inventory/valuation", [page: valuationPage - 1, pageSize: valuationPageSize]) }">Previous</a>
        <% } else { %>
        <span style="opacity:0.55;">Previous</span>
        <% } %>
        <span style="font-size:12px;color:#666;">Page ${ valuationPage } of ${ valuationTotalPages }</span>
        <% if (valuationHasNext) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "inventory/valuation", [page: valuationPage + 1, pageSize: valuationPageSize]) }">Next</a>
        <% } else { %>
        <span style="opacity:0.55;">Next</span>
        <% } %>
    </div>
</div>
<p><strong>Grand Total:</strong> ${ grandTotal }</p>
<p><strong>Grand Selling Total:</strong> ${ grandSellingTotal }</p>
