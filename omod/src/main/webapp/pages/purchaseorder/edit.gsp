<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage", [pageTitle: "Purchase order review"])
    def downloadUrl = ui.pageLink("rwandapharmacymanagement", "purchaseorder/pdf", [purchaseOrderId: purchaseOrder?.purchaseOrderId])
    def plainUserName = { user ->
        def personName = user?.personName
        def fullName = personName?.fullName?.trim()
        if (fullName) {
            return fullName
        }

        def combinedName = "${personName?.givenName ?: ''} ${personName?.familyName ?: ''}".trim()
        combinedName ?: (user?.username?.trim() ?: '')
    }
%>
<style>
    .po-review-shell {
        background: rgba(0, 0, 0, 0.04);
        padding: 20px 10px;
        border-radius: 4px;
    }
    .po-review-card {
        max-width: 860px;
        margin: 0 auto;
        background: #fff;
        border: 1px solid #d8d8d8;
        border-radius: 4px;
        overflow: hidden;
        box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
    }
    .po-review-header {
        background: #6f50a4;
        color: #fff;
        padding: 12px 16px;
        font-size: 13px;
        font-weight: bold;
    }
    .po-review-body {
        padding: 14px 16px 16px;
        max-height: calc(100vh - 180px);
        overflow: auto;
    }
    .po-meta-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 10px;
        margin-bottom: 14px;
    }
    .po-meta-card {
        border: 1px solid #ddd;
        background: #fafafa;
        padding: 10px;
        min-height: 72px;
    }
    .po-meta-label {
        font-size: 10px;
        color: #888;
        text-transform: uppercase;
        margin-bottom: 4px;
    }
    .po-meta-value {
        font-size: 14px;
        font-weight: bold;
        color: #333;
    }
    .po-status-pill {
        display: inline-block;
        padding: 3px 8px;
        border-radius: 3px;
        font-size: 11px;
        font-weight: bold;
        color: #fff;
        background: #f0ad4e;
    }
    .po-status-approved { background: #5cb85c; }
    .po-status-rejected { background: #d9534f; }
    .po-edit-table {
        width: 100%;
        border-collapse: collapse;
        margin-bottom: 14px;
        font-size: 12px;
    }
    .po-edit-table-wrap {
        overflow: auto;
    }
    .po-edit-table th, .po-edit-table td {
        border: 1px solid #ddd;
        padding: 7px 8px;
        vertical-align: top;
    }
    .po-edit-table th {
        background: #f5f5f5;
        color: #666;
        font-size: 11px;
    }
    .po-edit-table input[type='number'] {
        width: 86px;
        padding: 4px 6px;
    }
    .po-timeline {
        border-top: 1px solid #e5e5e5;
        padding-top: 10px;
        margin-top: 8px;
        color: #666;
        font-size: 12px;
    }
    .po-action-bar {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
        margin-top: 14px;
        align-items: center;
    }
</style>

<h2>Purchase order review</h2>

<% if (!purchaseOrder) { %>
<div style="padding:12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;">Purchase order not found.</div>
<% } else { %>
<% if (saved) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #bce8f1;background:#d9edf7;color:#31708f;border-radius:3px;">Purchase order saved.</div><% } %>
<% if (approved) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #d6e9c6;background:#dff0d8;color:#3c763d;border-radius:3px;">Purchase order approved and sent to supplier.</div><% } %>
<% if (rejected) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">Purchase order rejected.</div><% } %>
<% if (lineProblems) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">${ lineProblems.size() } item${ lineProblems.size() == 1 ? '' : 's' } block${ lineProblems.size() == 1 ? 's' : '' } this purchase order. They are highlighted in red below with the reason.</div><% } %>
<% if (submissionError) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">Purchase order was not approved: ${ ui.encodeHtmlContent(submissionError) }</div><% } %>
<% if (priceFailed) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">Purchase order cannot be approved until RMS returns prices for every item code. Zero prices from RMS are allowed.</div><% } %>
<% if (priceRefreshResult?.errorMessage) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">${ priceRefreshResult.errorMessage }</div><% } %>
<% if (priceRefreshResult?.unmatchedNpcCodes) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #faebcc;background:#fcf8e3;color:#8a6d3b;border-radius:3px;">RMS did not return prices for: ${ priceRefreshResult.unmatchedNpcCodes.join(', ') }</div><% } %>
<% if (errorReference) { %><div style="margin:0 0 12px 0;padding:8px 12px;border:1px solid #ebccd1;background:#f2dede;color:#a94442;border-radius:3px;">The purchase order action could not be completed. Give this reference to the administrator: <strong>${ errorReference }</strong>.</div><% } %>

<div class="po-review-shell">
    <div class="po-review-card">
        <div class="po-review-header">Purchase Order PO-${ purchaseOrder.purchaseOrderId }</div>
        <div class="po-review-body">
            <div class="po-meta-grid">
                <div class="po-meta-card">
                    <div class="po-meta-label">Purchase order ID</div>
                    <div class="po-meta-value">PO-${ purchaseOrder.purchaseOrderId }</div>
                </div>
                <div class="po-meta-card">
                    <div class="po-meta-label">Facility ID</div>
                    <div class="po-meta-value">${ purchaseOrder.facilityId ?: '' }</div>
                </div>
                <div class="po-meta-card">
                    <div class="po-meta-label">Status</div>
                    <div class="po-meta-value">
                        <span class="po-status-pill ${ purchaseOrder.approvalStatus == 'APPROVED' ? 'po-status-approved' : purchaseOrder.approvalStatus == 'REJECTED' ? 'po-status-rejected' : '' }">${ purchaseOrder.approvalStatus ?: 'PENDING' }</span>
                    </div>
                </div>
                <div class="po-meta-card">
                    <div class="po-meta-label">Supplier</div>
                    <div class="po-meta-value">${ purchaseOrder.supplier?.name ?: '' }</div>
                </div>
            </div>

            <form method="post">
                <input type="hidden" name="purchaseOrderId" value="${ purchaseOrder.purchaseOrderId }"/>
                <div class="po-edit-table-wrap">
                    <table class="po-edit-table">
                        <thead>
                        <tr>
                            <th>Order number</th>
                            <th>Program</th>
                            <th>Item name</th>
                            <th>Item code</th>
                            <th>Qty requested</th>
                            <th>Unit price</th>
                            <th>Total price</th>
                            <th>Remove</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% purchaseOrder.items.each { item -> %>
                        <% def lineProblem = lineProblems?.get(item) %>
                        <tr ${ lineProblem ? 'class="po-line-blocking" style="background:#f2dede;"' : '' }>
                            <td>
                                ${ item.orderNumber ?: '' }
                                <input type="hidden" name="purchaseOrderItemIds" value="${ item.purchaseOrderItemId }"/>
                            </td>
                            <td>${ item.program ?: '' }</td>
                            <td>
                                ${ item.itemName ?: '' }
                                <% if (lineProblem) { %><div style="color:#a94442;font-size:12px;font-weight:bold;">Blocks this order: ${ ui.encodeHtmlContent(lineProblem) }</div><% } %>
                            </td>
                            <td>${ item.itemCode ?: '' }</td>
	                            <td><input type="number" step="${ apiSupplierOrder ? '1' : '0.01' }" min="${ apiSupplierOrder ? '1' : '0.01' }" name="quantities" value="${ apiSupplierOrder && item.quantity != null ? item.quantity.stripTrailingZeros().toPlainString() : item.quantity }" ${ purchaseOrder.approvalStatus != 'PENDING' || !canManagePurchaseOrders ? 'readonly="readonly"' : '' }/></td>
	                            <td><input type="number" step="0.01" min="0" name="unitPrices" value="${ item.unitPrice ?: 0 }" ${ purchaseOrder.approvalStatus != 'PENDING' || !canManagePurchaseOrders ? 'readonly="readonly"' : '' }/></td>
                            <td>${ item.totalPrice ?: 0 }</td>
                            <td>
	                                <% if (purchaseOrder.approvalStatus == 'PENDING' && canManagePurchaseOrders) { %>
	                                <label><input type="checkbox" name="removeItemIds" value="${ item.purchaseOrderItemId }"/> Remove</label>
	                                <% } %>
                            </td>
                        </tr>
                        <% } %>
                        <tr>
                            <td colspan="6" style="text-align:right;font-weight:bold;background:#f5f5f5;">Total cost</td>
                            <td style="font-weight:bold;background:#f5f5f5;">${ purchaseOrder.totalCost ?: 0 }</td>
                            <td style="background:#f5f5f5;"></td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <% if ((purchaseOrder.approvalStatus == 'PENDING' || purchaseOrder.approvalStatus == 'DRAFT') && canManagePurchaseOrders) { %>
                <div style="margin-top:16px;padding:12px;border:1px solid #d9e2ec;background:#f8fafc;border-radius:3px;">
                    <h3 style="margin:0 0 10px 0;">Add item before approval</h3>
                    <div style="display:grid;grid-template-columns:minmax(260px,1fr) 140px;gap:10px;align-items:end;">
                        <label>
                            Product master item
                            <select name="productMasterItemIds" style="width:100%;">
                                <option value="">Select item</option>
                                <% productMasterItems?.each { item -> %>
                                <option value="${ item.productMasterItemId }">${ item.itemName ?: '' } (${ item.npcCode ?: '' }) — ${ item.program ?: 'General' }</option>
                                <% } %>
                            </select>
                        </label>
                        <label>
                            Quantity
                            <input type="number" step="0.01" min="0.01" name="productMasterQuantities"/>
                        </label>
                    </div>
                    <div style="color:#64748b;margin-top:6px;">Click Save changes to add this item to the purchase order.</div>
                </div>
                <% } %>

                <div class="po-timeline">
                    Submitted ${ purchaseOrder.requestedDate ? ui.formatDatePretty(purchaseOrder.requestedDate) : '' }
                    by ${ plainUserName(purchaseOrder.requestedBy) }.
                    <% if (purchaseOrder.approvalStatus == 'APPROVED') { %>
                    Approved ${ purchaseOrder.approvedDate ? ui.formatDatePretty(purchaseOrder.approvedDate) : '' }
                    by ${ plainUserName(purchaseOrder.approvedBy) }.
                    <% } %>
                </div>

                <div class="po-action-bar">
	                    <% if (purchaseOrder.approvalStatus == 'PENDING' && canManagePurchaseOrders) { %>
	                    <button type="submit" class="stock-btn" name="action" value="save">Save changes</button>
	                    <% } %>
	                    <% if (purchaseOrder.approvalStatus == 'PENDING' && canApprovePurchaseOrders) { %>
	                    <button type="submit" class="stock-btn stock-btn-success" name="action" value="approve">&#10003; Approve and send</button>
	                    <button type="submit" class="stock-btn" style="background:#d9534f;border-color:#d43f3a;color:#fff;" name="action" value="reject">&#10005; Reject</button>
	                    <% } %>
                    <% if (purchaseOrder.approvalStatus == 'APPROVED') { %>
                    <a class="stock-btn" href="${ downloadUrl }">Download PDF</a>
                    <% } %>
                    <a class="stock-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'purchaseorder/management') }">Close</a>
                </div>
            </form>
        </div>
    </div>
</div>
<% } %>
