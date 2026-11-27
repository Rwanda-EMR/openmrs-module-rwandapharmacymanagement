<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<style>
    .mmi-toolbar {
        display: flex;
        gap: 10px;
        align-items: center;
        margin: 0 0 12px 0;
        font-size: 12px;
        color: #666;
    }
    .mmi-message {
        margin: 0 0 12px 0;
        padding: 8px 12px;
        border: 1px solid #d9edf7;
        background: #f4fbff;
        color: #31708f;
        border-radius: 3px;
    }
    .mmi-warning {
        margin: 0 0 12px 0;
        padding: 8px 12px;
        border: 1px solid #faebcc;
        background: #fcf8e3;
        color: #8a6d3b;
        border-radius: 3px;
    }
    .mmi-table {
        width: 100%;
        border-collapse: collapse;
        background: #fff;
        font-size: 12px;
    }
    .mmi-table th, .mmi-table td {
        border: 1px solid #ddd;
        padding: 7px 8px;
        vertical-align: top;
        text-align: left;
    }
    .mmi-table th {
        background: #f5f5f5;
        color: #666;
    }
    .mmi-status {
        font-weight: bold;
        white-space: nowrap;
    }
    .mmi-status-recorded { color: #3c763d; }
    .mmi-status-failed { color: #a94442; }
    .mmi-status-other { color: #8a6d3b; }
</style>

<h2>MMI Medicine Deliveries</h2>

<p style="color:#666;font-size:12px;margin:0 0 12px 0;">
    Each delivery receipt is sent to MMI so that MMI bills in-house medicines at the delivery price (unit price plus the
    configured markup). Medicine lines on MMI vouchers point to the receipt number.
</p>

<% if (message) { %>
<div class="mmi-message">${ ui.escapeHtml(message) }</div>
<% } %>
<% if (!enabled) { %>
<div class="mmi-warning">
    Sending deliveries to MMI is switched off. Turn on <code>rwandapharmacymanagement.rhip.enabled</code> and
    <code>rwandapharmacymanagement.rhip.medicineDeliveryEnabled</code> to send them.
</div>
<% } %>

<form method="get" class="mmi-toolbar">
    <label for="mmi-days">Receipts from the last</label>
    <input type="number" id="mmi-days" name="days" min="1" max="365" value="${ days }" style="width:70px;"/>
    <span>days</span>
    <button type="submit">Show</button>
    <span>${ rows?.size() ?: 0 } receipt(s)</span>
</form>

<table class="mmi-table">
    <thead>
    <tr>
        <th>Received on</th>
        <th>Receipt number (MMI delivery reference)</th>
        <th>Received into</th>
        <th>Lines</th>
        <th>MMI status</th>
        <th>Last MMI answer</th>
        <th>Action</th>
    </tr>
    </thead>
    <tbody>
    <% if (!rows) { %>
    <tr><td colspan="7">No delivery receipts in this period.</td></tr>
    <% } else { rows.each { row -> %>
    <tr>
        <td>${ ui.format(row.receivedOn) }</td>
        <td>${ ui.escapeHtml(row.reference ?: '') }</td>
        <td>${ ui.escapeHtml(row.receipt.receivedIntoLocation?.name ?: '') }</td>
        <td>${ row.lineCount }</td>
        <td>
            <span class="mmi-status ${ row.recorded ? 'mmi-status-recorded' : (row.statusLabel == 'Failed' ? 'mmi-status-failed' : 'mmi-status-other') }">
                ${ row.statusLabel }
            </span>
            <% if (row.log?.dateCreated) { %>
            <div style="color:#999;">${ ui.format(row.log.dateCreated) }</div>
            <% } %>
        </td>
        <td>${ ui.escapeHtml(row.log?.summary ?: '') }</td>
        <td>
            <% if (enabled) { %>
            <form method="post" style="margin:0;"
                  onsubmit="${ row.recorded ? "return confirm('MMI already recorded this delivery. Sending it again may count its stock twice. Send anyway?');" : '' }">
                <input type="hidden" name="deliveryReceiptId" value="${ row.receipt.deliveryReceiptId }"/>
                <input type="hidden" name="days" value="${ days }"/>
                <button type="submit">${ row.log ? 'Resend' : 'Send' }</button>
            </form>
            <% } %>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>
