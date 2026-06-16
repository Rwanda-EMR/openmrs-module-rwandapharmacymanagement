<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def exportParams = [:]
    if (selectedPharmacy) {
        exportParams.pharmacyLocationId = selectedPharmacy.pharmacyLocationId
    }
    if (selectedMaterialType) {
        exportParams.materialType = selectedMaterialType
    }
    if (selectedProgram) {
        exportParams.program = selectedProgram
    }
    def exportExcelUrl = ui.pageLink("rwandapharmacymanagement", "report/pharmacyStockExport", exportParams + [format: "excel"])
    def exportPdfUrl = ui.pageLink("rwandapharmacymanagement", "report/pharmacyStockExport", exportParams + [format: "pdf"])
%>
<h2>Individual Pharmacy Report</h2>
<form method="get" style="margin-bottom:12px;">
    <label>Select pharmacy</label>
    <select name="pharmacyLocationId">
        <% pharmacies.each { pharmacy -> %>
        <option value="${ pharmacy.pharmacyLocationId }" ${ selectedPharmacy?.pharmacyLocationId == pharmacy.pharmacyLocationId ? 'selected="selected"' : '' }>${ pharmacy.name }</option>
        <% } %>
    </select>
    <label style="margin-left:10px;">Material type</label>
    <select name="materialType">
        <option value="">All material types</option>
        <% materialTypes.each { option -> %>
        <option value="${ option }" ${ selectedMaterialType == option ? 'selected="selected"' : '' }>${ option }</option>
        <% } %>
    </select>
    <label style="margin-left:10px;">Program</label>
    <select name="program">
        <option value="">All programs</option>
        <% programs.each { option -> %>
        <option value="${ option }" ${ selectedProgram == option ? 'selected="selected"' : '' }>${ option }</option>
        <% } %>
    </select>
    <button type="submit">Load report</button>
</form>
<p>
    <a href="${ exportExcelUrl }">Export Excel</a>
    |
    <a href="${ exportPdfUrl }">Export PDF</a>
</p>
<p><strong>Pharmacy:</strong> ${ selectedPharmacy?.name ?: '' }</p>
<p><strong>Total quantity:</strong> ${ totalQuantity }</p>
<p><strong>Total cost value:</strong> ${ totalCostValue }</p>
<p><strong>Total selling value:</strong> ${ totalSellingValue }</p>
<table>
    <thead><tr><th>Item</th><th>Material Type</th><th>Program</th><th>Batch</th><th>Expiry</th><th>On Hand</th><th>Cost Price</th><th>Cost Value</th><th>Selling Price</th><th>Selling Value</th></tr></thead>
    <tbody>
    <% rows.each { row -> def balance = row.balance %>
    <tr>
        <td>${ balance.item?.name }</td>
        <td>${ row.materialType ?: '' }</td>
        <td>${ row.program ?: '' }</td>
        <td>${ balance.batchNumber ?: '' }</td>
        <td>${ balance.expiryDate ? balance.expiryDate.format('dd/MMM/yyyy') : '' }</td>
        <td>${ balance.quantityOnHand ?: '' }</td>
        <td>${ row.unitPrice ?: '' }</td>
        <td>${ row.totalValue ?: '' }</td>
        <td>${ row.sellingPrice ?: '' }</td>
        <td>${ row.totalSellingValue ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
