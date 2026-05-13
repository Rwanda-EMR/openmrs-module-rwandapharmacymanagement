<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage", [pageTitle: "Purchase order management"])
    def downloadBase = ui.pageLink("rwandapharmacymanagement", "purchaseorder/pdf")
%>
<style>
    .po-summary-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 12px;
        margin-bottom: 14px;
    }
    .po-summary-card {
        background: #fff;
        border: 1px solid #ddd;
        padding: 12px 14px;
        border-radius: 3px;
    }
    .po-summary-count {
        font-size: 28px;
        line-height: 1;
        font-weight: bold;
        margin-bottom: 4px;
    }
    .po-summary-label {
        font-size: 12px;
        color: #666;
    }
    .po-toolbar {
        background: #fff7df;
        border: 1px solid #ead7a7;
        padding: 8px 10px;
        margin-bottom: 10px;
        font-size: 12px;
    }
    .po-filter-row {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
        align-items: center;
        margin-bottom: 10px;
    }
    .po-filter-row a {
        display: inline-block;
        padding: 4px 8px;
        border: 1px solid #ccc;
        background: #fff;
        border-radius: 3px;
        font-size: 11px;
    }
    .po-filter-row a.active {
        background: #337ab7;
        border-color: #2e6da4;
        color: #fff;
    }
    .po-table-wrap {
        background: #fff;
        border: 1px solid #ddd;
        border-radius: 3px;
        overflow: hidden;
    }
    .po-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 12px;
    }
    .po-table th, .po-table td {
        border: 1px solid #ddd;
        padding: 7px 8px;
        text-align: left;
        vertical-align: top;
    }
    .po-table th {
        background: #f5f5f5;
        font-size: 11px;
        color: #666;
    }
    .po-chip {
        display: inline-block;
        padding: 2px 6px;
        border-radius: 3px;
        font-size: 10px;
        font-weight: bold;
        color: #fff;
    }
    .po-chip-pending { background: #f0ad4e; }
    .po-chip-approved { background: #5cb85c; }
    .po-chip-rejected { background: #d9534f; }
    .po-chip-dispatched { background: #337ab7; }
    .po-action-row {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
    }
</style>

<h2>Purchase order management</h2>

<% if (retried) { %>
<div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #d6e9c6;background:#dff0d8;color:#3c763d;border-radius:3px;">Purchase order send was retried.</div>
<% } %>
<% if (retryFailed) { %>
<div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">Only approved purchase orders with failed supplier dispatch can be retried.</div>
<% } %>
<% if (retryPriceFailed) { %>
<div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">Retry stopped because RMS did not return prices for every item code. Zero prices from RMS are allowed.</div>
<% } %>
<% if (errorReference) { %>
<div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">
    The purchase order action could not be completed. Give this reference to the administrator: <strong>${ errorReference }</strong>.
</div>
<% } %>

<div class="po-summary-grid">
    <div class="po-summary-card"><div class="po-summary-count" style="color:#f0ad4e;">${ pendingCount }</div><div class="po-summary-label">Pending</div></div>
    <div class="po-summary-card"><div class="po-summary-count" style="color:#5cb85c;">${ approvedCount }</div><div class="po-summary-label">Approved</div></div>
    <div class="po-summary-card"><div class="po-summary-count" style="color:#337ab7;">${ dispatchedCount }</div><div class="po-summary-label">Dispatched</div></div>
    <div class="po-summary-card"><div class="po-summary-count" style="color:#d9534f;">${ rejectedCount }</div><div class="po-summary-label">Rejected</div></div>
</div>

<div class="po-filter-row">
    <form method="get" style="display:flex;gap:8px;align-items:end;flex-wrap:wrap;margin:0;">
        <div>
            <label style="display:block;font-size:11px;color:#666;margin-bottom:3px;">From</label>
            <input type="text" class="openmrs-date-field" name="dateFrom" value="${ dateFrom }"/>
        </div>
        <div>
            <label style="display:block;font-size:11px;color:#666;margin-bottom:3px;">To</label>
            <input type="text" class="openmrs-date-field" name="dateTo" value="${ dateTo }"/>
        </div>
        <div>
            <label style="display:block;font-size:11px;color:#666;margin-bottom:3px;">Status</label>
            <select name="status">
                <option value="ALL" ${ selectedStatus == 'ALL' ? 'selected="selected"' : '' }>All</option>
                <option value="PENDING" ${ selectedStatus == 'PENDING' ? 'selected="selected"' : '' }>Pending</option>
                <option value="APPROVED" ${ selectedStatus == 'APPROVED' ? 'selected="selected"' : '' }>Approved</option>
                <option value="DISPATCHED" ${ selectedStatus == 'DISPATCHED' ? 'selected="selected"' : '' }>Dispatched</option>
                <option value="REJECTED" ${ selectedStatus == 'REJECTED' ? 'selected="selected"' : '' }>Rejected</option>
            </select>
        </div>
        <div><button type="submit" class="stock-btn">Apply filter</button></div>
    </form>
</div>

<div class="po-filter-row">
    <a class="${ selectedStatus == 'ALL' ? 'active' : '' }" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/management') }">All</a>
    <a class="${ selectedStatus == 'PENDING' ? 'active' : '' }" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/management', [status: 'PENDING']) }">Pending</a>
    <a class="${ selectedStatus == 'APPROVED' ? 'active' : '' }" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/management', [status: 'APPROVED']) }">Approved</a>
    <a class="${ selectedStatus == 'DISPATCHED' ? 'active' : '' }" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/management', [status: 'DISPATCHED']) }">Dispatched</a>
    <a class="${ selectedStatus == 'REJECTED' ? 'active' : '' }" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/management', [status: 'REJECTED']) }">Rejected</a>
</div>

<div class="po-table-wrap">
    <table class="po-table">
        <thead>
        <tr>
            <th>Purchase order ID</th>
            <th>Requested date</th>
            <th>Facility ID</th>
            <th>Program</th>
            <th>Supplier</th>
            <th>Items</th>
            <th>Total cost</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <% if (!purchaseOrders) { %>
        <tr><td colspan="9" style="color:#777;">No purchase orders found for this filter.</td></tr>
        <% } %>
        <% purchaseOrders.each { purchaseOrder -> %>
        <tr>
            <td>PO-${ purchaseOrder.purchaseOrderId }</td>
            <td>${ purchaseOrder.requestedDate ?: '' }</td>
            <td>${ purchaseOrder.facilityId ?: '' }</td>
            <td>${ purchaseOrder.program ?: '' }</td>
            <td>${ purchaseOrder.supplier?.name ?: '' }</td>
            <td>${ purchaseOrder.items?.size() ?: 0 }</td>
            <td>${ purchaseOrder.totalCost ?: 0 }</td>
            <td>
                <span class="po-chip ${ purchaseOrder.approvalStatus == 'APPROVED' ? 'po-chip-approved' : purchaseOrder.approvalStatus == 'REJECTED' ? 'po-chip-rejected' : 'po-chip-pending' }">${ purchaseOrder.approvalStatus ?: 'PENDING' }</span>
                <% if (purchaseOrder.dispatchStatus == 'DISPATCHED') { %>
                <span class="po-chip po-chip-dispatched">${ purchaseOrder.dispatchStatus }</span>
                <% } else if (purchaseOrder.dispatchStatus == 'FAILED') { %>
                <span class="po-chip po-chip-rejected">${ purchaseOrder.dispatchStatus }</span>
                <% } else if (purchaseOrder.dispatchStatus == 'FETCHED') { %>
                <span class="po-chip po-chip-pending">FETCHED</span>
                <% } %>
            </td>
            <td>
                <div class="po-action-row">
                    <a class="stock-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/edit', [purchaseOrderId: purchaseOrder.purchaseOrderId]) }">View</a>
                    <% if (purchaseOrder.approvalStatus == 'PENDING' && canApprovePurchaseOrders) { %>
                    <a class="stock-btn stock-btn-primary" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/edit', [purchaseOrderId: purchaseOrder.purchaseOrderId]) }">Review</a>
                    <% } %>
                    <% if (purchaseOrder.approvalStatus == 'APPROVED') { %>
                    <a class="stock-btn" href="${ downloadBase }?purchaseOrderId=${ purchaseOrder.purchaseOrderId }">Download PDF</a>
                    <% } %>
                    <% if (purchaseOrder.approvalStatus == 'APPROVED' && purchaseOrder.dispatchStatus == 'FAILED') { %>
                    <form method="post" style="display:inline;margin:0;">
                        <input type="hidden" name="purchaseOrderId" value="${ purchaseOrder.purchaseOrderId }"/>
                        <input type="hidden" name="action" value="retryDispatch"/>
                        <button type="submit" class="stock-btn stock-btn-primary">Retry send</button>
                    </form>
                    <% } %>
                </div>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>
