<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage", [pageTitle: "Pharmacy Item Request History"])
    def plainUserName = { user ->
        def personName = user?.personName
        def fullName = personName?.fullName?.trim()
        if (fullName) {
            return fullName
        }
        def combinedName = "${personName?.givenName ?: ''} ${personName?.familyName ?: ''}".trim()
        combinedName ?: (user?.username?.trim() ?: '')
    }
    def downloadUrl = { request ->
        ui.pageLink("rwandapharmacymanagement", "pharmacyorder/historyPdf", [requestId: request?.pharmacyItemRequestId])
    }
%>
<style type="text/css">
    .poh-page { display:grid; gap:14px; }
    .poh-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; overflow:hidden; }
    .poh-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .poh-body { padding:14px; }
    .poh-filters { display:flex; gap:10px; flex-wrap:wrap; align-items:flex-end; margin-bottom:12px; }
    .poh-filters label { display:block; font-size:11px; color:#475569; font-weight:600; margin-bottom:3px; }
    .poh-filters input, .poh-filters select { padding:5px 7px; border:1px solid #cbd5e1; border-radius:4px; }
    .poh-btn { display:inline-block; border:1px solid #2f6fad; background:#2f6fad; color:#fff; border-radius:4px; padding:5px 10px; text-decoration:none; cursor:pointer; }
    .poh-btn-secondary { background:#fff; color:#2f6fad; }
    .poh-table { width:100%; border-collapse:collapse; }
    .poh-table th, .poh-table td { border:1px solid #dbe5ec; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .poh-table th { background:#f8fbfd; color:#475569; }
    .poh-muted { color:#64748b; font-size:12px; }
    .poh-warn { padding:10px 12px; border-radius:4px; border:1px solid #f0ad4e; background:#fcf8e3; color:#8a6d3b; }
    .poh-status { display:inline-flex; padding:3px 8px; border-radius:999px; font-size:11px; font-weight:600; background:#e0f2fe; color:#075985; }
    .poh-status-approved { background:#dcfce7; color:#166534; }
    .poh-status-rejected { background:#fee2e2; color:#991b1b; }
    .poh-removed { display:inline-block; margin-left:6px; padding:2px 7px; border-radius:999px; background:#fff1f2; color:#991b1b; font-size:11px; font-weight:600; }
    .poh-lines div { margin-bottom:4px; }
</style>

<h2>Pharmacy Item Request History</h2>

<div class="poh-page">
    <% if (pharmacySelectionRequired) { %>
    <div class="poh-warn">
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
    <div class="poh-warn">No active pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.</div>
    <% } %>

    <div class="poh-card">
        <div class="poh-head">Previous pharmacy item requests</div>
        <div class="poh-body">
            <p><strong>Active pharmacy:</strong> ${ activePharmacy?.name ?: '' }</p>
            <form method="get" class="poh-filters">
                <div>
                    <label>From</label>
                    <input type="date" name="fromDate" value="${ fromDate ?: '' }"/>
                </div>
                <div>
                    <label>To</label>
                    <input type="date" name="toDate" value="${ toDate ?: '' }"/>
                </div>
                <div>
                    <label>Status</label>
                    <select name="status">
                        <option value="" ${ !status ? 'selected="selected"' : '' }>All</option>
                        <option value="REQUESTED" ${ status == 'REQUESTED' ? 'selected="selected"' : '' }>Requested</option>
                        <option value="APPROVED" ${ status == 'APPROVED' ? 'selected="selected"' : '' }>Approved</option>
                        <option value="REJECTED" ${ status == 'REJECTED' ? 'selected="selected"' : '' }>Rejected</option>
                    </select>
                </div>
                <div>
                    <label>Item</label>
                    <input type="text" name="itemName" value="${ itemName ?: '' }" placeholder="Search item"/>
                </div>
                <button class="poh-btn" type="submit">Filter</button>
                <a class="poh-btn poh-btn-secondary" href="${ ui.pageLink('rwandapharmacymanagement', 'pharmacyorder/history') }">Reset</a>
            </form>

            <table class="poh-table">
                <thead>
                <tr>
                    <th>Request ID</th>
                    <th>Requested by</th>
                    <th>Submitted</th>
                    <th>Reviewed</th>
                    <th>Status</th>
                    <th>Items</th>
                    <th>Download</th>
                </tr>
                </thead>
                <tbody>
                <% if (!requests) { %>
                <tr><td colspan="7" class="poh-muted">No pharmacy item request history found.</td></tr>
                <% } else { requests.each { request -> %>
                <tr>
                    <td>#${ request.pharmacyItemRequestId ?: '' }</td>
                    <td>${ plainUserName(request.creator) }</td>
                    <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                    <td>
                        ${ request.approvedDate ? ui.formatDatePretty(request.approvedDate) : 'Pending review' }
                        ${ request.approvedBy ? ' by ' + plainUserName(request.approvedBy) : '' }
                    </td>
                    <td>
                        <span class="poh-status ${ request.requestStatus == 'APPROVED' ? 'poh-status-approved' : request.requestStatus == 'REJECTED' ? 'poh-status-rejected' : '' }">${ request.requestStatus ?: '' }</span>
                        <% if (request.requestStatus == 'REJECTED' && request.voidReason) { %>
                        <div class="poh-muted">Reason: ${ request.voidReason }</div>
                        <% } %>
                    </td>
                    <td class="poh-lines">
                        <% request.lines?.each { line -> %>
                        <div>
                            ${ line.item?.name ?: '' }: requested ${ line.requestedQuantity ?: 0 }${ line.approvedQuantity != null ? ', approved ' + line.approvedQuantity : '' }
                            <% if (line.voided == true) { %>
                            <span class="poh-removed">Removed${ line.voidReason ? ': ' + line.voidReason : '' }</span>
                            <% } %>
                        </div>
                        <% } %>
                    </td>
                    <td><a class="poh-btn poh-btn-secondary" href="${ downloadUrl(request) }">PDF</a></td>
                </tr>
                <% } } %>
                </tbody>
            </table>
            <p class="poh-muted">Showing up to ${ maxResults ?: 200 } records.</p>
        </div>
    </div>
</div>
