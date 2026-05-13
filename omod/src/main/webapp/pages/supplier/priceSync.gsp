<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.supplier.priceSync.title") }</h2>
<p><a href="${ ui.pageLink("rwandapharmacymanagement", "supplier/priceHistory") }">View persisted supplier price history</a></p>
<form method="post">
    <button type="submit">Run Full Price Sync</button>
</form>
<form method="post">
    <fieldset>
        <legend>Lookup Prices</legend>
        <p><label>Program</label><br/><input type="text" name="program" value="${ selectedProgram ?: '' }" placeholder="CA or CP"/></p>
        <p><label>NPC Codes</label><br/><input type="text" name="npcCodes" value="${ selectedNpcCodes ?: '' }" size="80" placeholder="NPC000001,NPC000002"/></p>
        <p><button type="submit">Lookup Selected NPC Codes</button></p>
    </fieldset>
</form>
<% if (lookupResponse) { %>
    <h3>Lookup Result</h3>
    <p>${ lookupResponse.message ?: '' }</p>
    <table>
        <thead><tr><th>NPC Code</th><th>Description</th><th>Selling Price</th></tr></thead>
        <tbody>
        <% lookupResponse.matchedPrices?.each { price -> %>
        <tr>
            <td>${ price.npcCode }</td>
            <td>${ price.materialDescription }</td>
            <td>${ price.sellingPrice }</td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <p>Unmatched NPC Codes: ${ lookupResponse.unmatchedNpcCodes ?: [] }</p>
<% } %>
<h3>Current Price List</h3>
<table>
    <thead><tr><th>NPC Code</th><th>Description</th><th>Program</th><th>Price</th><th>Currency</th><th>Updated</th></tr></thead>
    <tbody>
    <% supplierPrices.each { price -> %>
    <tr>
        <td>${ price.npcCode }</td>
        <td>${ price.materialDescription }</td>
        <td>${ price.program }</td>
        <td>${ price.price }</td>
        <td>${ price.currency }</td>
        <td>${ price.updatedAt }</td>
    </tr>
    <% } %>
    </tbody>
</table>
