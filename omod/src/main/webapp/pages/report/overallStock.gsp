<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def exportParams = [:]
    if (selectedMaterialType) {
        exportParams.materialType = selectedMaterialType
    }
    if (selectedProgram) {
        exportParams.program = selectedProgram
    }
    def exportExcelUrl = ui.pageLink("rwandapharmacymanagement", "report/overallStockExport", exportParams + [format: "excel"])
    def exportPdfUrl = ui.pageLink("rwandapharmacymanagement", "report/overallStockExport", exportParams + [format: "pdf"])
%>
<h2>Overall Hospital Pharmacy Report</h2>
<form method="get" style="margin-bottom:12px;">
    <label>Material type</label>
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
    <button type="submit">Filter</button>
</form>
<p>
    <a href="${ exportExcelUrl }">Export Excel</a>
    |
    <a href="${ exportPdfUrl }">Export PDF</a>
</p>
<p><strong>Total quantity:</strong> ${ totalQuantity }</p>
<p><strong>Total cost value:</strong> ${ totalCostValue }</p>
<p><strong>Total selling value:</strong> ${ totalSellingValue }</p>
<table>
    <thead><tr><th>Item</th><th>Code</th><th>Material Type</th><th>Program</th><th>Total Quantity</th><th>Pharmacies Holding Stock</th><th>Total Cost Value</th><th>Total Selling Value</th></tr></thead>
    <tbody>
    <% rows.each { row -> %>
    <tr>
        <td>${ row.itemName ?: '' }</td>
        <td>${ row.itemCode ?: '' }</td>
        <td>${ row.materialType ?: '' }</td>
        <td>${ row.program ?: '' }</td>
        <td>${ row.quantity ?: '' }</td>
        <td>${ row.pharmacies ?: 0 }</td>
        <td>${ row.totalValue ?: '' }</td>
        <td>${ row.totalSellingValue ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
