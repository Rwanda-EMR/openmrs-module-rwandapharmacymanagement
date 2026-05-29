<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<style>
    .hie-toolbar {
        display: flex;
        gap: 10px;
        align-items: center;
        margin: 0 0 12px 0;
    }
    .hie-message {
        margin: 0 0 12px 0;
        padding: 8px 12px;
        border: 1px solid #d9edf7;
        background: #f4fbff;
        color: #31708f;
        border-radius: 3px;
    }
    .hie-table {
        width: 100%;
        border-collapse: collapse;
        background: #fff;
        font-size: 12px;
    }
    .hie-table th, .hie-table td {
        border: 1px solid #ddd;
        padding: 7px 8px;
        vertical-align: top;
        text-align: left;
    }
    .hie-table th {
        background: #f5f5f5;
        color: #666;
    }
    .hie-payload {
        max-width: 360px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        font-family: monospace;
        font-size: 11px;
    }
</style>

<h2>Pending HIE Queue</h2>

<% if (message) { %>
<div class="hie-message">${ message }</div>
<% } %>

<div class="hie-toolbar">
    <form method="post" style="margin:0;">
        <input type="hidden" name="action" value="retryAll"/>
        <button type="submit">Push all pending</button>
    </form>
    <span style="color:#666;font-size:12px;">${ pendingLogs?.size() ?: 0 } pending payload(s)</span>
</div>

<table class="hie-table">
    <thead>
    <tr>
        <th>Date queued</th>
        <th>Type</th>
        <th>Endpoint</th>
        <th>Attempts</th>
        <th>Last attempt</th>
        <th>Last response</th>
        <th>Payload</th>
        <th>Action</th>
    </tr>
    </thead>
    <tbody>
    <% if (!pendingLogs) { %>
    <tr><td colspan="8">No pending HIE payloads.</td></tr>
    <% } else { pendingLogs.each { log -> %>
    <tr>
        <td>${ log.dateCreated ?: '' }</td>
        <td>${ log.integrationType ?: '' }</td>
        <td>${ log.endpoint ?: '' }</td>
        <td>${ log.retryCount ?: 0 }</td>
        <td>${ log.lastAttemptDate ?: '' }</td>
        <td>${ log.responsePayload ?: log.summary ?: '' }</td>
        <td><div class="hie-payload" title="${ ui.escapeHtml(log.requestPayload ?: '') }">${ log.requestPayload ?: '' }</div></td>
        <td>
            <form method="post" style="margin:0;">
                <input type="hidden" name="action" value="retryOne"/>
                <input type="hidden" name="integrationLogId" value="${ log.integrationLogId }"/>
                <button type="submit">Push</button>
            </form>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>

<h2 style="margin-top:24px;">Pending Prescription SMS Queue</h2>

<div class="hie-toolbar">
    <form method="post" style="margin:0;">
        <input type="hidden" name="action" value="retryAllSms"/>
        <button type="submit">Push all pending SMS</button>
    </form>
    <span style="color:#666;font-size:12px;">${ pendingSmsLogs?.size() ?: 0 } pending SMS notification(s)</span>
</div>

<table class="hie-table">
    <thead>
    <tr>
        <th>Date queued</th>
        <th>Endpoint</th>
        <th>Attempts</th>
        <th>Last attempt</th>
        <th>Last response</th>
        <th>Payload</th>
        <th>Action</th>
    </tr>
    </thead>
    <tbody>
    <% if (!pendingSmsLogs) { %>
    <tr><td colspan="7">No pending SMS notifications.</td></tr>
    <% } else { pendingSmsLogs.each { log -> %>
    <tr>
        <td>${ log.dateCreated ?: '' }</td>
        <td>${ log.endpoint ?: '' }</td>
        <td>${ log.retryCount ?: 0 }</td>
        <td>${ log.lastAttemptDate ?: '' }</td>
        <td>${ log.responsePayload ?: log.summary ?: '' }</td>
        <td><div class="hie-payload" title="${ ui.escapeHtml(log.requestPayload ?: '') }">${ log.requestPayload ?: '' }</div></td>
        <td>
            <form method="post" style="margin:0;">
                <input type="hidden" name="action" value="retryOneSms"/>
                <input type="hidden" name="integrationLogId" value="${ log.integrationLogId }"/>
                <button type="submit">Push</button>
            </form>
        </td>
    </tr>
    <% } } %>
    </tbody>
</table>
