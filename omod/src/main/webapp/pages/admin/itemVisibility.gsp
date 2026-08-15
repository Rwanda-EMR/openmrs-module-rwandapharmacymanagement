<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Item Visibility Control</h2>
<style type="text/css">
    .iv-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .iv-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .iv-body { padding:14px; }
    .iv-filter { display:flex; flex-wrap:wrap; gap:8px; align-items:center; margin:0; }
    .iv-filter input[type='text'] { width:280px; padding:7px 9px; border:1px solid #cbd5e1; border-radius:4px; }
    .iv-filter select { padding:7px 9px; border:1px solid #cbd5e1; border-radius:4px; }
    .iv-btn { display:inline-block; padding:7px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; text-decoration:none; }
    .iv-btn-light { background:#fff; color:#1f5f99; }
    .iv-table { width:100%; border-collapse:collapse; }
    .iv-table th, .iv-table td { border:1px solid #dbe5ec; padding:8px; font-size:12px; text-align:left; vertical-align:middle; }
    .iv-table th { background:#f8fbfd; color:#475569; }
    .iv-hidden-row { background:#fff7ed; }
    .iv-status { display:inline-block; padding:3px 8px; border-radius:999px; font-size:11px; font-weight:bold; }
    .iv-status-hidden { background:#ffedd5; color:#9a3412; }
    .iv-status-visible { background:#dcfce7; color:#166534; }
    .iv-muted { color:#64748b; font-size:12px; }
    .iv-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; margin-bottom:12px; }
</style>

<% if (updated) { %><div class="iv-banner">Item visibility updated.</div><% } %>

<div class="iv-card">
    <div class="iv-head">Search items</div>
    <div class="iv-body">
        <form method="get" class="iv-filter">
            <input type="text" name="q" value="${ searchTerm ?: '' }" placeholder="Search by item or code"/>
            <select name="visibility">
                <option value="all" ${ visibilityFilter == 'all' ? 'selected="selected"' : '' }>All items</option>
                <option value="hidden" ${ visibilityFilter == 'hidden' ? 'selected="selected"' : '' }>Hidden only</option>
                <option value="visible" ${ visibilityFilter == 'visible' ? 'selected="selected"' : '' }>Visible only</option>
            </select>
            <button type="submit" class="iv-btn">Search</button>
            <a class="iv-btn iv-btn-light" href="${ ui.pageLink('rwandapharmacymanagement', 'admin/itemVisibility') }">Clear</a>
        </form>
        <p class="iv-muted">Hidden items are listed first, then items are sorted alphabetically.</p>
    </div>
</div>

<div class="iv-card">
    <div class="iv-head">Requester visibility</div>
    <div class="iv-body">
        <table class="iv-table">
            <thead>
            <tr><th>Item</th><th>Code</th><th>Category</th><th>Total Available</th><th>Status</th><th>Action</th></tr>
            </thead>
            <tbody>
            <% if (!items) { %>
            <tr><td colspan="6" class="iv-muted">No items found.</td></tr>
            <% } else { items.each { item ->
                def hidden = item.visibleToRequesters == false
                def totalAvailable = availableQuantities?.get(item.itemId) ?: 0
            %>
            <tr class="${ hidden ? 'iv-hidden-row' : '' }">
                <td>${ item.name ?: '' }</td>
                <td>${ item.code ?: '' }</td>
                <td>${ item.itemCategory?.name ?: '' }</td>
                <td>${ totalAvailable }</td>
                <td><span class="iv-status ${ hidden ? 'iv-status-hidden' : 'iv-status-visible' }">${ hidden ? 'Hidden' : 'Visible' }</span></td>
                <td>
                    <form method="post" style="margin:0;">
                        <input type="hidden" name="itemId" value="${ item.itemId }"/>
                        <input type="hidden" name="visibleToRequesters" value="${ hidden ? 'true' : 'false' }"/>
                        <input type="hidden" name="q" value="${ searchTerm ?: '' }"/>
                        <input type="hidden" name="visibility" value="${ visibilityFilter ?: 'all' }"/>
                        <button type="submit" class="iv-btn ${ hidden ? '' : 'iv-btn-light' }">${ hidden ? 'Show to requesters' : 'Hide from requesters' }</button>
                    </form>
                </td>
            </tr>
            <% } } %>
            </tbody>
        </table>
    </div>
</div>
