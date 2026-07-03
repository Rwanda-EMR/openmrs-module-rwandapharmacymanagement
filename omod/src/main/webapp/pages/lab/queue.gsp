<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def availableByItemId = binding.hasVariable("availableByItemId") && availableByItemId ? availableByItemId : [:]
    def batchOptionsByItemId = binding.hasVariable("batchOptionsByItemId") && batchOptionsByItemId ? batchOptionsByItemId : [:]
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
    .labq-page { display:grid; gap:14px; }
    .labq-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; overflow:hidden; }
    .labq-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .labq-body { padding:14px; }
    .labq-banner { padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; }
    .labq-table { width:100%; border-collapse:collapse; }
    .labq-table th, .labq-table td { border:1px solid #dbe5ec; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .labq-table th { background:#f8fbfd; color:#475569; }
    .labq-btn { display:inline-block; padding:7px 12px; border-radius:4px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; text-decoration:none; cursor:pointer; }
    .labq-muted { color:#64748b; font-size:12px; }
    .labq-warn { padding:10px 12px; border-radius:4px; border:1px solid #f0ad4e; background:#fcf8e3; color:#8a6d3b; }
    .labq-wait { display:inline-block; padding:2px 7px; border-radius:4px; font-size:11px; font-weight:700; color:#fff; background:#f0ad4e; }
    .labq-wait-high { background:#d9534f; }
</style>

<h2>Lab Dispensing Queue</h2>

<div class="labq-page">
    <% if (approved) { %><div class="labq-banner">Lab request approved and deducted from stock.</div><% } %>
    <% if (pharmacySelectionRequired) { %>
    <div class="labq-warn">
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
    <div class="labq-warn">No lab dispensing pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.</div>
    <% } %>

    <div class="labq-card">
        <div class="labq-head">Pending requests</div>
        <div class="labq-body">
            <p><strong>Active dispensing pharmacy:</strong> ${ activePharmacy?.name ?: '' }</p>
            <table class="labq-table">
                <thead>
                <tr><th>Request ID</th><th>Requester</th><th>Submitted</th><th>Wait time</th><th>Lab unit</th><th>Lab services</th><th>Dispensed by</th><th></th></tr>
                </thead>
                <tbody>
                <% if (!requests) { %>
                <tr><td colspan="8" class="labq-muted">No pending lab requests.</td></tr>
                <% } else { requests.each { request ->
                    def waitTime = waitTimeByRequestId[request.labSupplyRequestId] ?: '0 min'
                    def waitClass = waitTime.contains('h') || waitTime.startsWith('3') || waitTime.startsWith('4') || waitTime.startsWith('5') ? 'labq-wait labq-wait-high' : 'labq-wait'
                %>
                <tr>
                    <td>${ request.labSupplyRequestId }</td>
                    <td>${ plainUserName(request.creator) }</td>
                    <td>${ request.dateCreated ? ui.formatDatePretty(request.dateCreated) : '' }</td>
                    <td><span class="${ waitClass }">${ waitTime }</span></td>
                    <td>${ request.labUnit?.name ?: '' }</td>
                    <td>
                        <% request.lines?.collect { it.labServiceConcept?.displayString }?.findAll { it }?.unique()?.each { serviceName -> %>
                        <div>${ serviceName }</div>
                        <% } %>
                    </td>
                    <td>${ plainUserName(request.approvedBy) }</td>
                    <td><a class="labq-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'lab/queue', [requestId: request.labSupplyRequestId]) }">View</a></td>
                </tr>
                <% } } %>
                </tbody>
            </table>
        </div>
    </div>

    <% if (selectedRequest) { %>
    <div class="labq-card">
        <div class="labq-head">Request #${ selectedRequest.labSupplyRequestId }</div>
        <div class="labq-body">
            <form method="post">
                <input type="hidden" name="requestId" value="${ selectedRequest.labSupplyRequestId }"/>
                <table class="labq-table">
                    <thead>
                    <tr><th>Item</th><th>Lab service</th><th>Requested</th><th>Available here</th><th>Batch number</th><th>Approved quantity</th></tr>
                    </thead>
                    <tbody>
                    <% selectedRequest.lines?.each { line -> %>
                    <tr>
                        <td>
                            ${ line.item?.name ?: '' }
                            <input type="hidden" name="lineId" value="${ line.labSupplyRequestLineId }"/>
                        </td>
                        <td>${ line.labServiceConcept?.displayString ?: '' }</td>
                        <td>${ line.requestedQuantity ?: 0 }</td>
                        <td>${ availableByItemId[line.item?.itemId] ?: 0 }</td>
                        <td>
                            <select name="batchNumber">
                                <option value="__FIFO__">Auto / FIFO</option>
                                <% (batchOptionsByItemId[line.item?.itemId] ?: []).each { batch -> %>
                                <option value="${ batch.batchValue }">
                                    ${ batch.batchNumber ?: 'No batch' } (${ batch.available ?: 0 }${ batch.expiryDate ? ', exp ' + ui.formatDatePretty(batch.expiryDate) : '' })
                                </option>
                                <% } %>
                            </select>
                        </td>
                        <td><input type="number" step="0.01" min="0" max="${ line.requestedQuantity ?: 0 }" name="approvedQuantity" value="${ line.requestedQuantity ?: 0 }"/></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
                <p style="margin-top:12px;">
                    <button type="submit" class="labq-btn">Approve and deduct stock</button>
                </p>
            </form>
        </div>
    </div>
    <% } %>
</div>
