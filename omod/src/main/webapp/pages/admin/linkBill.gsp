<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage", [pageTitle: "Link Prescription Bills"])
%>
<style type="text/css">
    .bill-page { display:grid; gap:14px; }
    .bill-card { background:#fff; border:1px solid #d7dee8; border-radius:4px; overflow:hidden; }
    .bill-card-head { background:#1f5f99; color:#fff; padding:10px 14px; font-weight:600; font-size:13px; }
    .bill-card-body { padding:14px; }
    .bill-grid { display:grid; grid-template-columns:180px minmax(220px, 1fr) auto; gap:10px; align-items:end; }
    .bill-field label { display:block; margin-bottom:4px; font-size:12px; font-weight:600; color:#334155; }
    .bill-field input { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font:inherit; }
    .bill-btn { display:inline-flex; align-items:center; justify-content:center; border:1px solid #cbd5e1; border-radius:4px; padding:8px 14px; cursor:pointer; background:#fff; color:#0f172a; font:inherit; text-decoration:none; }
    .bill-btn-primary { background:#16a34a; border-color:#15803d; color:#fff; }
    .bill-table { width:100%; border-collapse:collapse; }
    .bill-table th, .bill-table td { border:1px solid #dbe5f0; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .bill-table th { background:#eef5fb; color:#475569; }
    .bill-muted { color:#64748b; font-size:12px; }
    .bill-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; }
    .bill-warn { border-color:#fcd34d; background:#fffbeb; color:#92400e; }
    .bill-lines div + div { margin-top:3px; }
    @media (max-width: 900px) { .bill-grid { grid-template-columns:1fr; } }
</style>

<div class="bill-page">
    <div>
        <h2>Link Patient Request Bills</h2>
        <div class="bill-muted">Find billable prescription or consumable requests that do not have a linked patient bill.</div>
    </div>

    <% if (linkedRequestId) { %><div class="bill-banner">Bill linked to request #${ linkedRequestId }.</div><% } %>
    <% if (errorMessage) { %><div class="bill-banner bill-warn">${ errorMessage }</div><% } %>

    <div class="bill-card">
        <div class="bill-card-head">Search</div>
        <div class="bill-card-body">
            <form method="get" class="bill-grid">
                <div class="bill-field">
                    <label>Request ID</label>
                    <input type="number" name="requestId" value="${ requestId ?: '' }"/>
                </div>
                <div class="bill-field">
                    <label>Patient identifier or name</label>
                    <input type="text" name="patient" value="${ patientSearch ?: '' }"/>
                </div>
                <button type="submit" class="bill-btn bill-btn-primary">Find requests</button>
            </form>
        </div>
    </div>

    <div class="bill-card">
        <div class="bill-card-head">Patient requests without linked bills</div>
        <div class="bill-card-body">
            <% if (!rows) { %>
            <div class="bill-muted">No matching requests found.</div>
            <% } else { %>
            <table class="bill-table">
                <thead>
                <tr>
                    <th>Request</th>
                    <th>Patient</th>
                    <th>Created</th>
                    <th>Status</th>
                    <th>Type</th>
                    <th>Items</th>
                    <th>Link bill</th>
                </tr>
                </thead>
                <tbody>
                <% rows.each { row -> %>
                <% def request = row.request %>
                <tr>
                    <td>#${ request.patientDispensingRequestId ?: '' }</td>
                    <td>
                        <strong>${ request.patient?.personName?.fullName ?: '' }</strong><br/>
                        <span class="bill-muted">${ request.patient?.patientIdentifier?.identifier ?: '' }</span>
                    </td>
                    <td>${ request.dateCreated ? request.dateCreated.format('yyyy-MM-dd HH:mm') : '' }</td>
                    <td>${ request.requestStatus ?: '' }</td>
                    <td>${ request.requestType ?: 'PRESCRIPTION' }</td>
                    <td class="bill-lines">
                        <% request.lines?.each { line -> %>
                        <div>${ line.item?.name ?: '' }: ${ line.requestedQuantity ?: 0 }</div>
                        <% } %>
                    </td>
                    <td>
                        <form method="post">
                            <input type="hidden" name="requestId" value="${ request.patientDispensingRequestId ?: '' }"/>
                            <div class="bill-field">
                                <label>Patient bill ID</label>
                                <input type="number" name="patientBillId" value="${ row.suggestedBillId ?: '' }" required="required"/>
                            </div>
                            <% if (row.suggestedBillId) { %>
                            <div class="bill-muted">Suggested latest bill: ${ row.suggestedBillId }</div>
                            <% } %>
                            <button type="submit" class="bill-btn bill-btn-primary" style="margin-top:8px;">Link bill</button>
                        </form>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <% } %>
        </div>
    </div>
</div>
