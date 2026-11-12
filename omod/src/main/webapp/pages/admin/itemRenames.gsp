<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def attr = { value -> (value ?: '').toString().replace('&', '&amp;').replace('"', '&quot;').replace('<', '&lt;').replace('>', '&gt;') }
%>

<h2>Item Renames</h2>

<% if (message) { %><p style="padding:8px;background:#e8f5e9;color:#1b5e20;">${ ui.encodeHtmlContent(message) }</p><% } %>
<% if (error) { %><p style="padding:8px;background:#ffebee;color:#b71c1c;">${ ui.encodeHtmlContent(error) }</p><% } %>

<p>
    Approving a name renames the pharmacy item, its OpenMRS drug and its MOH Billing service to exactly that name,
    so MOH Billing keeps finding the service when the drug is ordered. Past bills show the new name.
</p>

<form method="post" style="margin:0 0 12px 0;">
    <input type="hidden" name="action" value="check"/>
    <button type="submit">Run consistency check</button>
    <span style="color:#666;font-size:12px;">Flags items whose drug or billing service name differs from the item name.</span>
</form>

<table>
    <thead>
    <tr>
        <th>Pharmacy item</th>
        <th>OpenMRS drug</th>
        <th>Proposed names</th>
        <th>Action</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows || rows.isEmpty()) { %>
    <tr><td colspan="4">No pending item renames.</td></tr>
    <% } %>
    <% rows?.each { row -> %>
    <tr>
        <td>
            <strong>${ ui.encodeHtmlContent(row.itemName ?: '') }</strong>
            <div style="color:#666;font-size:12px;">Item ${ row.itemId }${ row.itemCode ? ' &middot; ' + ui.encodeHtmlContent(row.itemCode) : '' }</div>
        </td>
        <td>${ ui.encodeHtmlContent(row.drugName ?: 'No drug') }</td>
        <td>
            <form method="post" id="rename-${ row.itemId }" style="margin:0;">
                <input type="hidden" name="action" value="approve"/>
                <input type="hidden" name="itemId" value="${ row.itemId }"/>
                <% row.candidates.eachWithIndex { candidate, index -> %>
                <label style="display:block;margin-bottom:6px;">
                    <input type="radio" name="chosenName" value="${ attr(candidate.name) }" ${ index == 0 ? 'checked="checked"' : '' }/>
                    <strong>${ ui.encodeHtmlContent(candidate.name ?: '') }</strong>
                    <span style="color:#666;font-size:12px;">
                        ${ ui.encodeHtmlContent(candidate.source) }${ candidate.program ? ', program ' + ui.encodeHtmlContent(candidate.program) : '' }
                        ${ candidate.reason ? ' &middot; ' + ui.encodeHtmlContent(candidate.reason) : '' }
                    </span>
                </label>
                <% } %>
                <label style="display:block;">
                    Other name:
                    <input type="text" name="customName" value="" placeholder="Leave empty to use the selected name"/>
                </label>
            </form>
        </td>
        <td>
            <button type="submit" form="rename-${ row.itemId }">Approve</button>
            <form method="post" style="margin:6px 0 0 0;">
                <input type="hidden" name="action" value="reject"/>
                <input type="hidden" name="itemId" value="${ row.itemId }"/>
                <button type="submit">Reject</button>
            </form>
        </td>
    </tr>
    <% } %>
    </tbody>
</table>
