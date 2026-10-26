<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Patient queue cleanup</h2>
<style type="text/css">
    .cleanup-panel { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .cleanup-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .cleanup-body { padding:14px; }
    .cleanup-grid { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:10px; align-items:end; }
    .cleanup-field label { display:block; font-size:12px; font-weight:600; color:#475569; margin-bottom:4px; }
    .cleanup-field input, .cleanup-field select { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:7px 8px; font-size:12px; }
    .cleanup-btn { display:inline-block; padding:7px 12px; border-radius:4px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; cursor:pointer; text-decoration:none; font-size:12px; font-weight:600; }
    .cleanup-danger { background:#be123c; border-color:#be123c; }
    .cleanup-muted { color:#64748b; font-size:12px; }
    .cleanup-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; margin-bottom:12px; }
    .cleanup-error { border-color:#fecdd3; background:#fff1f2; color:#be123c; }
    .cleanup-table { width:100%; border-collapse:collapse; margin-top:10px; }
    .cleanup-table th, .cleanup-table td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .cleanup-table th { background:#f8fbfd; color:#475569; }
    .cleanup-confirm { display:flex; gap:8px; align-items:center; margin:12px 0; }
    .cleanup-confirm input { margin:0; }
    @media (max-width: 900px) { .cleanup-grid { grid-template-columns:1fr; } }
</style>

<% if (updatedCount != null) { %>
<div class="cleanup-banner">${ updatedCount } patient request${ updatedCount == 1 ? '' : 's' } marked as not served.</div>
<% } %>
<% if (errorMessage) { %>
<div class="cleanup-banner cleanup-error">${ errorMessage }</div>
<% } %>

<div class="cleanup-panel">
    <div class="cleanup-head">Find pending requests</div>
    <div class="cleanup-body">
        <form method="get">
            <div class="cleanup-grid">
                <div class="cleanup-field">
                    <label>Pharmacy</label>
                    <select name="pharmacyLocationId">
                        <option value="">All pharmacies</option>
                        <% pharmacies?.each { pharmacy -> if (pharmacy?.active) { %>
                        <option value="${ pharmacy.pharmacyLocationId }" ${ selectedPharmacyLocationId == pharmacy.pharmacyLocationId ? 'selected="selected"' : '' }>${ pharmacy.name }${ pharmacy.mainStore ? ' (main)' : '' }</option>
                        <% } } %>
                    </select>
                </div>
                <div class="cleanup-field">
                    <label>From date</label>
                    <input type="date" name="fromDate" value="${ fromDate ?: '' }" required="required"/>
                </div>
                <div class="cleanup-field">
                    <label>To date</label>
                    <input type="date" name="toDate" value="${ toDate ?: '' }" required="required"/>
                </div>
                <div>
                    <button type="submit" class="cleanup-btn">Preview</button>
                </div>
            </div>
        </form>
    </div>
</div>

<% if (binding.hasVariable('previewCount') && previewCount != null) { %>
<div class="cleanup-panel">
    <div class="cleanup-head">Preview</div>
    <div class="cleanup-body">
        <p class="cleanup-muted">
            ${ previewCount } pending patient request${ previewCount == 1 ? '' : 's' } matched for ${ selectedPharmacyName ?: 'selected scope' } from ${ fromDate } to ${ toDate }.
            Only PENDING_PAYMENT and PENDING_DISPENSE requests are included.
        </p>
        <% if (previewRequests) { %>
        <table class="cleanup-table">
            <thead>
            <tr><th>ID</th><th>Created</th><th>Patient</th><th>Status</th><th>Target pharmacy</th><th>Items</th></tr>
            </thead>
            <tbody>
            <% previewRequests.each { request -> %>
            <tr>
                <td>${ request.patientDispensingRequestId }</td>
                <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                <td>${ request.patient?.personName ?: '' }</td>
                <td>${ request.requestStatus ?: '' }</td>
                <td>${ request.targetPharmacyLocation?.name ?: '' }</td>
                <td>${ request.lines?.size() ?: 0 }</td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <% if (previewCount > previewLimit) { %>
        <p class="cleanup-muted">Showing the first ${ previewLimit } matching requests.</p>
        <% } %>
        <form method="post" style="margin-top:12px;">
            <input type="hidden" name="pharmacyLocationId" value="${ selectedPharmacyLocationId ?: '' }"/>
            <input type="hidden" name="fromDate" value="${ fromDate ?: '' }"/>
            <input type="hidden" name="toDate" value="${ toDate ?: '' }"/>
            <div class="cleanup-field">
                <label>Reason</label>
                <input type="text" name="reason" required="required" placeholder="Reason for marking these requests as not served"/>
            </div>
            <label class="cleanup-confirm">
                <input type="checkbox" name="confirm" value="true" required="required"/>
                <span>I confirm these pending requests will not be served.</span>
            </label>
            <button type="submit" class="cleanup-btn cleanup-danger">Mark as not served</button>
        </form>
        <% } else { %>
        <p class="cleanup-muted">No matching pending requests were found.</p>
        <% } %>
    </div>
</div>
<% } %>
