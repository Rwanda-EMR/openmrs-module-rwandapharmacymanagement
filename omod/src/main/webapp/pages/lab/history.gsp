<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
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
<style type="text/css">
    .labh-page { display:grid; gap:14px; }
    .labh-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; overflow:hidden; }
    .labh-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .labh-body { padding:14px; }
    .labh-table { width:100%; border-collapse:collapse; }
    .labh-table th, .labh-table td { border:1px solid #dbe5ec; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .labh-table th { background:#f8fbfd; color:#475569; }
    .labh-muted { color:#64748b; font-size:12px; }
    .labh-warn { padding:10px 12px; border-radius:4px; border:1px solid #f0ad4e; background:#fcf8e3; color:#8a6d3b; }
    .labh-status { display:inline-flex; align-items:center; padding:3px 8px; border-radius:999px; font-size:11px; font-weight:600; background:#dcfce7; color:#166534; }
    .labh-lines div { margin-bottom:4px; }
</style>

<h2>Lab Dispensing History</h2>

<div class="labh-page">
    <% if (pharmacySelectionRequired) { %>
    <div class="labh-warn">
        <form method="get" style="margin:0;">
            <strong>Choose pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.
            <select name="pharmacyLocationId" required="required">
                <option value="">Select pharmacy</option>
                <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
            </select>
            <button type="submit">Use pharmacy</button>
        </form>
    </div>
    <% } else if (!activePharmacy) { %>
    <div class="labh-warn">No lab dispensing pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.</div>
    <% } %>

    <div class="labh-card">
        <div class="labh-head">Approved lab requests</div>
        <div class="labh-body">
            <p><strong>Active dispensing pharmacy:</strong> ${ activePharmacy?.name ?: '' }</p>
            <table class="labh-table">
                <thead>
                <tr>
                    <th>Request ID</th>
                    <th>Requester</th>
                    <th>Lab unit</th>
                    <th>Submitted</th>
                    <th>Approved</th>
                    <th>Dispensed by</th>
                    <th>Status</th>
                    <th>Items</th>
                </tr>
                </thead>
                <tbody>
                <% if (!requests) { %>
                <tr><td colspan="8" class="labh-muted">No lab dispensing history found for this pharmacy.</td></tr>
                <% } else { requests.each { request -> %>
                <tr>
                    <td>${ request.labSupplyRequestId ?: '' }</td>
                    <td>${ plainUserName(request.creator) }</td>
                    <td>${ request.labUnit?.name ?: '' }</td>
                    <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                    <td>
                        ${ request.approvedDate ? ui.formatDatePretty(request.approvedDate) : '' }
                    </td>
                    <td>${ plainUserName(request.approvedBy) }</td>
                    <td><span class="labh-status">${ request.requestStatus ?: '' }</span></td>
                    <td class="labh-lines">
                        <% request.lines?.each { line -> %>
                        <div>${ line.item?.name ?: '' }${ line.labServiceConcept?.displayString ? ' - ' + line.labServiceConcept.displayString : '' }: requested ${ line.requestedQuantity ?: 0 }, approved ${ line.approvedQuantity ?: 0 }</div>
                        <% } %>
                    </td>
                </tr>
                <% } } %>
                </tbody>
            </table>
        </div>
    </div>
</div>
