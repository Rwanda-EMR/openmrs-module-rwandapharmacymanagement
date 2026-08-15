<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def pdfParams = [
            pharmacyLocationId: filter?.pharmacyLocationId,
            itemCategoryId: filter?.itemCategoryId,
            q: filter?.searchTerm,
            expiryStatus: filter?.expiryStatus,
            stockStatus: filter?.stockStatus,
            expiryAlertDays: filter?.expiryAlertDays,
            daysOfStockAlertDays: filter?.daysOfStockAlertDays,
            consumptionLookbackDays: filter?.consumptionLookbackDays
    ]
%>
<style type="text/css">
    .alert-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .alert-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .alert-body { padding:14px; }
    .alert-filter { display:grid; grid-template-columns:repeat(4, minmax(150px, 1fr)); gap:10px; align-items:end; }
    .alert-filter label { display:block; font-size:12px; color:#475569; margin-bottom:4px; }
    .alert-filter input, .alert-filter select { width:100%; box-sizing:border-box; padding:7px 9px; border:1px solid #cbd5e1; border-radius:4px; }
    .alert-actions { display:flex; gap:8px; flex-wrap:wrap; align-items:center; }
    .alert-btn { display:inline-block; padding:8px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; text-decoration:none; }
    .alert-btn-light { background:#fff; color:#1f5f99; }
    .alert-table-wrap { overflow-x:auto; }
    .alert-table { width:100%; border-collapse:collapse; min-width:920px; }
    .alert-table th, .alert-table td { border:1px solid #dbe5ec; padding:7px 8px; font-size:12px; text-align:left; vertical-align:middle; }
    .alert-table th { background:#f8fbfd; color:#475569; }
    .alert-muted { color:#64748b; font-size:12px; }
    .alert-status { display:inline-block; border-radius:999px; padding:3px 8px; font-size:11px; font-weight:bold; }
    .alert-status-danger { background:#fee2e2; color:#991b1b; }
    .alert-status-warning { background:#fef3c7; color:#92400e; }
    .alert-summary { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:12px; }
    .alert-summary div { background:#f8fbfd; border:1px solid #dbe5ec; border-radius:4px; padding:8px 12px; min-width:150px; }
    @media (max-width: 900px) { .alert-filter { grid-template-columns:1fr 1fr; } }
    @media (max-width: 600px) { .alert-filter { grid-template-columns:1fr; } }
</style>

<h2>${ ui.message("rwandapharmacymanagement.alerts.dashboard.title") }</h2>

<div class="alert-card">
    <div class="alert-head">Filters</div>
    <div class="alert-body">
        <form method="get" class="alert-filter">
            <div>
                <label>Pharmacy location</label>
                <select name="pharmacyLocationId">
                    <option value="" ${ !filter?.pharmacyLocationId ? 'selected="selected"' : '' }>Whole hospital</option>
                    <% pharmacyLocations?.each { location -> %>
                    <option value="${ location.pharmacyLocationId }" ${ filter?.pharmacyLocationId == location.pharmacyLocationId ? 'selected="selected"' : '' }>${ location.name }</option>
                    <% } %>
                </select>
            </div>
            <div>
                <label>Category</label>
                <select name="itemCategoryId">
                    <option value="" ${ !filter?.itemCategoryId ? 'selected="selected"' : '' }>All categories</option>
                    <% itemCategories?.each { category -> %>
                    <option value="${ category.itemCategoryId }" ${ filter?.itemCategoryId == category.itemCategoryId ? 'selected="selected"' : '' }>${ category.name }</option>
                    <% } %>
                </select>
            </div>
            <div>
                <label>Item name/code</label>
                <input type="text" name="q" value="${ filter?.searchTerm ?: '' }" placeholder="Search item"/>
            </div>
            <div>
                <label>Expiry status</label>
                <select name="expiryStatus">
                    <option value="all" ${ filter?.expiryStatus == 'all' ? 'selected="selected"' : '' }>All expiry alerts</option>
                    <option value="expired" ${ filter?.expiryStatus == 'expired' ? 'selected="selected"' : '' }>Expired only</option>
                    <option value="expiring" ${ filter?.expiryStatus == 'expiring' ? 'selected="selected"' : '' }>Expiring soon only</option>
                </select>
            </div>
            <div>
                <label>Stock status</label>
                <select name="stockStatus">
                    <option value="all" ${ filter?.stockStatus == 'all' ? 'selected="selected"' : '' }>Stockout and low stock</option>
                    <option value="stockout" ${ filter?.stockStatus == 'stockout' ? 'selected="selected"' : '' }>Stockout only</option>
                    <option value="low" ${ filter?.stockStatus == 'low' ? 'selected="selected"' : '' }>Low stock only</option>
                </select>
            </div>
            <div>
                <label>Expiry Alert Days</label>
                <input type="number" min="1" name="expiryAlertDays" value="${ filter?.expiryAlertDays ?: 90 }"/>
            </div>
            <div>
                <label>Days of Stock Alert Days</label>
                <input type="number" min="1" name="daysOfStockAlertDays" value="${ filter?.daysOfStockAlertDays ?: 14 }"/>
            </div>
            <div>
                <label>Consumption Lookback Days</label>
                <input type="number" min="1" name="consumptionLookbackDays" value="${ filter?.consumptionLookbackDays ?: 90 }"/>
            </div>
            <div class="alert-actions">
                <button type="submit" class="alert-btn">Apply filters</button>
                <a class="alert-btn alert-btn-light" href="${ ui.pageLink('rwandapharmacymanagement', 'alerts/dashboard') }">Clear</a>
                <a class="alert-btn alert-btn-light" href="${ ui.pageLink('rwandapharmacymanagement', 'alerts/alertPdf', pdfParams) }">Download PDF</a>
            </div>
        </form>
        <p class="alert-muted">
            Without a selected location, stockout and low stock are calculated for the whole hospital by summing all active pharmacy locations.
        </p>
    </div>
</div>

<div class="alert-summary">
    <div>Expiration alerts<br/><strong>${ expiryAlerts?.size() ?: 0 }</strong></div>
    <div>Stockout/low stock alerts<br/><strong>${ lowStockAlerts?.size() ?: 0 }</strong></div>
    <div>Selected location<br/><strong>${ selectedLocationName ?: 'Whole hospital' }</strong></div>
</div>

<div class="alert-card">
    <div class="alert-head">Expiration Alerts</div>
    <div class="alert-body">
        <div class="alert-table-wrap">
            <table class="alert-table">
                <thead>
                <tr><th>Item</th><th>Code</th><th>Location</th><th>Batch</th><th>Expiry</th><th>Days to Expiry</th><th>On Hand</th><th>Status</th></tr>
                </thead>
                <tbody>
                <% if (!expiryAlerts) { %>
                <tr><td colspan="8" class="alert-muted">No expiration alerts found.</td></tr>
                <% } else { expiryAlerts.each { row -> %>
                <tr>
                    <td>${ row.item?.name ?: '' }</td>
                    <td>${ row.item?.code ?: '' }</td>
                    <td>${ row.pharmacyLocation?.name ?: 'Whole hospital' }</td>
                    <td>${ row.batchNumber ?: '' }</td>
                    <td>${ row.expiryDate ? row.expiryDate.format('dd/MMM/yyyy') : '' }</td>
                    <td>${ row.daysToExpiry }</td>
                    <td>${ row.quantityOnHand ?: 0 }</td>
                    <td><span class="alert-status ${ row.status == 'Expired' ? 'alert-status-danger' : 'alert-status-warning' }">${ row.status }</span></td>
                </tr>
                <% } } %>
                </tbody>
            </table>
        </div>
    </div>
</div>

<div class="alert-card">
    <div class="alert-head">Stockout and Low Stock Alerts</div>
    <div class="alert-body">
        <div class="alert-table-wrap">
            <table class="alert-table">
                <thead>
                <tr><th>Item</th><th>Code</th><th>Location Scope</th><th>Current Qty</th><th>Consumed</th><th>Avg/Day</th><th>Days of Stock</th><th>Status</th></tr>
                </thead>
                <tbody>
                <% if (!lowStockAlerts) { %>
                <tr><td colspan="8" class="alert-muted">No stockout or low stock alerts found.</td></tr>
                <% } else { lowStockAlerts.each { row -> %>
                <tr>
                    <td>${ row.item?.name ?: '' }</td>
                    <td>${ row.item?.code ?: '' }</td>
                    <td>${ row.pharmacyLocation?.name ?: 'Whole hospital' }</td>
                    <td>${ row.currentQuantity ?: 0 }</td>
                    <td>${ row.consumedQuantity ?: 0 }</td>
                    <td>${ row.averageDailyConsumption ?: 0 }</td>
                    <td>${ row.daysOfStock != null ? row.daysOfStock : '' }</td>
                    <td><span class="alert-status ${ row.status == 'Stockout' ? 'alert-status-danger' : 'alert-status-warning' }">${ row.status }</span></td>
                </tr>
                <% } } %>
                </tbody>
            </table>
        </div>
    </div>
</div>
