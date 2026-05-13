<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<style>
    .sync-toolbar {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
        margin-bottom: 14px;
        align-items: center;
    }
    .sync-note {
        margin: 0 0 14px 0;
        padding: 10px 12px;
        border: 1px solid #d9edf7;
        background: #f4fbff;
        color: #31708f;
        border-radius: 3px;
        font-size: 12px;
    }
    .sync-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 12px;
        background: #fff;
    }
    .sync-table th, .sync-table td {
        border: 1px solid #ddd;
        padding: 7px 8px;
        vertical-align: top;
        text-align: left;
    }
    .sync-table th {
        background: #f5f5f5;
        color: #666;
        font-size: 11px;
    }
    .sync-pagination {
        display: flex;
        gap: 8px;
        align-items: center;
        justify-content: space-between;
        margin: 12px 0 0;
        font-size: 12px;
        color: #666;
    }
    .sync-pagination-actions {
        display: flex;
        gap: 8px;
        align-items: center;
    }
    .sync-filter-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
        gap: 8px;
        margin: 0 0 14px 0;
    }
    .sync-filter-grid input {
        width: 100%;
        box-sizing: border-box;
        padding: 6px 8px;
        border: 1px solid #ccc;
        border-radius: 3px;
        font-size: 12px;
    }
    .sync-table-wrap {
        overflow: auto;
        border: 1px solid #ddd;
        border-radius: 3px;
    }
    .sync-table-wrap .sync-table th {
        position: sticky;
        top: 0;
        z-index: 1;
    }
    .sync-progress {
        display: none;
        margin: 0 0 12px 0;
        padding: 10px 12px;
        border: 1px solid #bcdff1;
        background: #eef8fd;
        color: #276b8d;
        border-radius: 3px;
        font-size: 12px;
        align-items: center;
        gap: 10px;
    }
    .sync-progress.active {
        display: flex;
    }
    .sync-spinner {
        width: 16px;
        height: 16px;
        border: 2px solid #b7dced;
        border-top-color: #2f84ab;
        border-radius: 50%;
        animation: sync-spin 0.8s linear infinite;
        flex: 0 0 auto;
    }
    @keyframes sync-spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }
</style>

<h2>Product master sync</h2>
<% if (errorMessage) { %>
<div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">
    ${ errorMessage }
</div>
<% } %>

<div id="sync-progress" class="sync-progress" aria-live="polite">
    <span class="sync-spinner"></span>
    <span id="sync-progress-text">Pulling data from supplier. Please wait...</span>
</div>

<div class="sync-toolbar">
    <form method="post" style="margin:0;" onsubmit="showSyncProgress('Pulling product master items from supplier. Please wait...')">
        <input type="hidden" name="action" value="updateItems"/>
        <button type="submit" class="stock-btn stock-btn-primary" ${ !supplierConfigured ? 'disabled="disabled"' : '' }>Update items</button>
    </form>
    <form method="post" style="margin:0;" onsubmit="showSyncProgress('Pulling supplier prices. Please wait...')">
        <input type="hidden" name="action" value="updatePrices"/>
        <button type="submit" class="stock-btn stock-btn-success" ${ !supplierConfigured ? 'disabled="disabled"' : '' }>Update prices</button>
    </form>
</div>

<form method="get" id="sync-filter-form" style="margin:0 0 14px 0;">
    <input type="hidden" name="page" id="sync-filter-page" value="1"/>
    <div class="sync-filter-grid">
        <input type="text" id="filter-npc" name="npcCode" value="${ npcCode ?: '' }" placeholder="Filter NPC code" oninput="queueSyncFilterSubmit()"/>
        <input type="text" id="filter-name" name="itemName" value="${ itemName ?: '' }" placeholder="Filter item name" oninput="queueSyncFilterSubmit()"/>
        <input type="text" id="filter-concept" name="conceptName" value="${ conceptName ?: '' }" placeholder="Filter concept name" oninput="queueSyncFilterSubmit()"/>
        <input type="text" id="filter-program" name="program" value="${ program ?: '' }" placeholder="Filter program" oninput="queueSyncFilterSubmit()"/>
        <input type="text" id="filter-material" name="materialType" value="${ materialType ?: '' }" placeholder="Filter material type" oninput="queueSyncFilterSubmit()"/>
    </div>
</form>

<h3>Product master items in database</h3>
<div class="sync-table-wrap">
    <table class="sync-table">
        <thead>
        <tr>
            <th>NPC code</th>
            <th>Item name</th>
            <th>Program</th>
            <th>Material type</th>
            <th>Price</th>
            <th>Drug</th>
            <th>Concept</th>
        </tr>
        </thead>
        <tbody id="product-sync-body">
        <% if (!productMasterItems) { %>
        <tr><td colspan="7" style="color:#777;">No product master items found.</td></tr>
        <% } %>
        <% productMasterItems.each { productMasterItem -> %>
        <tr class="product-sync-row">
            <td>${ productMasterItem.npcCode ?: '' }</td>
            <td>${ productMasterItem.itemName ?: '' }</td>
            <td>${ productMasterItem.program ?: '' }</td>
            <td>${ productMasterItem.materialType ?: '' }</td>
            <td>${ productMasterItem.itemPrice ?: 0 }</td>
            <td>${ productMasterItem.drug?.name ?: '' }</td>
            <td>${ conceptNameByNpcCode[productMasterItem.npcCode] ?: '' }</td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>
<div class="sync-pagination">
    <div>Showing page ${ currentPage } of ${ totalPages } | ${ totalItems } total item(s)</div>
    <div class="sync-pagination-actions">
        <% if (currentPage > 1) { %>
        <a class="stock-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'supplier/productSync', [page: currentPage - 1, npcCode: npcCode, itemName: itemName, conceptName: conceptName, program: program, materialType: materialType]) }">Previous</a>
        <% } %>
        <% if (currentPage < totalPages) { %>
        <a class="stock-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'supplier/productSync', [page: currentPage + 1, npcCode: npcCode, itemName: itemName, conceptName: conceptName, program: program, materialType: materialType]) }">Next</a>
        <% } %>
    </div>
</div>

<h3>Recent integration logs</h3>
<table class="sync-table">
    <thead><tr><th>Date</th><th>Type</th><th>Status</th><th>Summary</th></tr></thead>
    <tbody>
    <% integrationLogs.each { %>
    <tr>
        <td>${ it.dateCreated }</td>
        <td>${ it.integrationType }</td>
        <td>${ it.status }</td>
        <td>${ it.summary }</td>
    </tr>
    <% } %>
    </tbody>
</table>

<script type="text/javascript">
    var syncFilterTimer = null;

    function queueSyncFilterSubmit() {
        if (syncFilterTimer) {
            window.clearTimeout(syncFilterTimer);
        }
        syncFilterTimer = window.setTimeout(function() {
            var pageField = document.getElementById('sync-filter-page');
            var form = document.getElementById('sync-filter-form');
            if (pageField) {
                pageField.value = '1';
            }
            if (form) {
                form.submit();
            }
        }, 250);
    }

    function showSyncProgress(message) {
        var progress = document.getElementById('sync-progress');
        var text = document.getElementById('sync-progress-text');
        if (text) {
            text.textContent = message;
        }
        if (progress) {
            progress.classList.add('active');
        }
    }
</script>
