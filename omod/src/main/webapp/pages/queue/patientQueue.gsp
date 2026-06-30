<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def activeTab = binding.hasVariable("queueTab") && queueTab ? queueTab : 'recent'
    def baseQueueParams = [pharmacyLocationId: activePharmacy?.pharmacyLocationId, q: queueSearch]
    def tabLink = { key -> ui.pageLink('rwandapharmacymanagement', 'queue/patientQueue', baseQueueParams + [tab: key, page: 1]) }
%>
<style type="text/css">
    .queue-shell { display:grid; gap:14px; }
    .queue-banner { padding:10px 12px; border-radius:4px; border:1px solid #bce8f1; background:#d9edf7; color:#31708f; }
    .queue-warn { border-color:#f0ad4e; background:#fcf8e3; color:#8a6d3b; }
    .queue-panel { background:#fff; border:1px solid #d8e2ea; border-radius:4px; overflow:hidden; }
    .queue-panel-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .queue-toolbar { display:flex; justify-content:space-between; align-items:center; gap:10px; padding:12px 14px; border-bottom:1px solid #e5edf3; background:#fff; flex-wrap:wrap; }
    .queue-tabs { display:flex; gap:6px; flex-wrap:wrap; padding:10px 14px; border-bottom:1px solid #e5edf3; background:#f8fafc; }
    .queue-tab { display:inline-block; padding:7px 12px; border-radius:999px; border:1px solid #cbd5e1; background:#fff; color:#334155; text-decoration:none; font-size:12px; font-weight:600; }
    .queue-tab-active { background:#0f766e; border-color:#0f766e; color:#fff; }
    .queue-search { min-width:260px; width:100%; display:flex; gap:6px; align-items:center; flex-wrap:wrap; }
    .queue-search input { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font-size:12px; }
    .queue-search button { padding:8px 12px; border-radius:4px; border:1px solid #337ab7; background:#337ab7; color:#fff; font-size:12px; font-weight:600; cursor:pointer; }
    .queue-table { width:100%; border-collapse:collapse; }
    .queue-table th, .queue-table td { padding:10px 12px; border-bottom:1px solid #e5edf3; text-align:left; font-size:12px; vertical-align:middle; }
    .queue-table th { background:#fafcfe; color:#475569; font-weight:600; }
    .queue-name { font-weight:600; color:#0f172a; }
    .queue-id { font-family:monospace; font-size:11px; }
    .queue-badge { display:inline-block; min-width:18px; padding:2px 7px; border-radius:10px; font-size:11px; font-weight:700; text-align:center; color:#fff; background:#20a0d8; }
    .queue-wait { display:inline-block; padding:2px 7px; border-radius:4px; font-size:11px; font-weight:700; color:#fff; background:#f0ad4e; }
    .queue-wait-high { background:#d9534f; }
    .queue-paid { display:inline-block; padding:2px 7px; border-radius:4px; font-size:11px; font-weight:700; color:#fff; background:#22c55e; }
    .queue-unpaid { display:inline-block; padding:2px 7px; border-radius:4px; font-size:11px; font-weight:700; color:#fff; background:#f59e0b; }
    .queue-exception { display:inline-block; padding:2px 7px; border-radius:4px; font-size:11px; font-weight:700; color:#fff; background:#7c3aed; }
    .queue-action-group { display:flex; gap:6px; flex-wrap:wrap; }
    .queue-serve { display:inline-block; padding:4px 10px; border-radius:4px; border:1px solid #2e6da4; background:#337ab7; color:#fff; text-decoration:none; font-size:11px; font-weight:600; }
    .queue-serve:hover { background:#286090; }
    .queue-view { display:inline-block; padding:4px 10px; border-radius:4px; border:1px solid #64748b; background:#fff; color:#334155; text-decoration:none; font-size:11px; font-weight:600; }
    .queue-refresh { display:inline-flex; align-items:center; justify-content:center; width:24px; height:24px; border-radius:4px; border:1px solid #64748b; background:#fff; color:#334155; text-decoration:none; font-size:15px; font-weight:700; line-height:1; }
    .queue-refresh:hover { background:#f1f5f9; }
    .queue-blocked { display:inline-block; padding:4px 10px; border-radius:4px; border:1px solid #d1d5db; background:#f8fafc; color:#94a3b8; font-size:11px; font-weight:600; cursor:not-allowed; }
    .queue-empty { padding:18px 14px; color:#64748b; }
    .queue-pager { display:flex; justify-content:space-between; align-items:center; gap:10px; padding:10px 14px; border-top:1px solid #e5edf3; color:#64748b; font-size:12px; flex-wrap:wrap; }
    .queue-pager-actions { display:flex; gap:6px; align-items:center; }
</style>

<h2>Patient dispensing queue</h2>
<div style="color:#94a3b8;font-size:12px;margin-top:-6px;margin-bottom:12px;">
    Patients waiting <%= activePharmacy ? " - " + activePharmacy.name : (pharmacySelectionLocationName ? " - " + pharmacySelectionLocationName : "") %>
</div>

<% if (pharmacySelectionRequired) { %>
<div class="queue-banner queue-warn">
    <form method="get" style="margin:0;display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
        <strong>Choose pharmacy</strong>
        <span>for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.</span>
        <select name="pharmacyLocationId" required="required">
            <option value="">Select pharmacy</option>
            <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
        </select>
        <button type="submit">Use pharmacy</button>
    </form>
</div>
<% } else if (!activePharmacy) { %>
<div class="queue-banner queue-warn">No pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.</div>
<% } else { %>
<div class="queue-shell">
    <% if (paymentRefreshRequested) { %>
    <div class="queue-banner">Payment status refresh completed.</div>
    <% } else if (refreshedPaymentRequestId) { %>
    <div class="queue-banner">Payment status refreshed for the selected request.</div>
    <% } %>
    <div class="queue-panel">
        <div class="queue-panel-head">Queue (${ requests?.size() ?: 0 } shown, page size ${ queuePageSize ?: 100 })</div>
        <div class="queue-tabs">
            <a class="queue-tab ${ activeTab == 'recent' ? 'queue-tab-active' : '' }" href="${ tabLink('recent') }">Recent</a>
            <a class="queue-tab ${ activeTab == 'ready' ? 'queue-tab-active' : '' }" href="${ tabLink('ready') }">Ready</a>
            <a class="queue-tab ${ activeTab == 'waiting' ? 'queue-tab-active' : '' }" href="${ tabLink('waiting') }">Waiting Payment</a>
            <a class="queue-tab ${ activeTab == 'exception' ? 'queue-tab-active' : '' }" href="${ tabLink('exception') }">Exception Approved</a>
            <a class="queue-tab ${ activeTab == 'older' ? 'queue-tab-active' : '' }" href="${ tabLink('older') }">Older Pending</a>
            <a class="queue-tab ${ activeTab == 'search' ? 'queue-tab-active' : '' }" href="${ tabLink('search') }">Search All</a>
        </div>
        <div class="queue-toolbar">
            <form method="get" class="queue-search">
                <input type="hidden" name="pharmacyLocationId" value="${ activePharmacy.pharmacyLocationId }"/>
                <input type="hidden" name="tab" value="${ activeTab }"/>
                <input type="text" name="q" value="${ queueSearch ?: '' }" placeholder="Search patient identifier or patient name" style="max-width:360px;"/>
                <button type="submit">Search</button>
                <% if (queueSearch) { %><a class="queue-view" href="${ ui.pageLink('rwandapharmacymanagement', 'queue/patientQueue', [pharmacyLocationId: activePharmacy.pharmacyLocationId, tab: activeTab]) }">Clear</a><% } %>
            </form>
            <a class="queue-view" href="${ ui.pageLink('rwandapharmacymanagement', 'queue/patientQueue', [pharmacyLocationId: activePharmacy.pharmacyLocationId, q: queueSearch, tab: activeTab, page: queuePage, refreshPayments: true]) }">Refresh payment status</a>
        </div>
        <% if (!requests) { %>
        <div class="queue-empty">${ queueSearch ? 'No dispensing requests match that search.' : 'No dispensing requests are waiting for ' + activePharmacy.name + '.' }</div>
        <% } else { %>
        <table class="queue-table">
            <thead>
            <tr>
                <th>#</th>
                <th>Created</th>
                <th>Patient name</th>
                <th>Patient ID</th>
                <th>Age</th>
                <th>Gender</th>
                <th>Type</th>
                <th>Items</th>
                <th>Wait time</th>
                <th>Status</th>
                <th>Action</th>
            </tr>
            </thead>
            <tbody id="queue-table-body">
            <% requests.eachWithIndex { request, idx ->
                def age = request?.patient?.age
                def gender = request?.patient?.gender
                def waitTime = waitTimeByRequestId[request.patientDispensingRequestId] ?: '0 min'
                def waitClass = waitTime.contains('h') || waitTime.startsWith('3') || waitTime.startsWith('4') || waitTime.startsWith('5') ? 'queue-wait queue-wait-high' : 'queue-wait'
                def paid = paymentStatusByRequestId[request.patientDispensingRequestId]
                def exceptionApproved = paymentExceptionByRequestId[request.patientDispensingRequestId]
                def ready = paid || exceptionApproved
                def consumableRequest = request.requestType?.toString() == 'CONSUMABLE'
                def patientLabel = request.patient?.personName ? request.patient.personName.toString() : ''
                def patientIdLabel = patientIdentifierByRequestId[request.patientDispensingRequestId] ?: ''
            %>
            <tr class="queue-row" data-search="${ (patientLabel + ' ' + patientIdLabel).toLowerCase() }">
                <td>${ (queueFirstResult ?: 1) + idx }</td>
                <td>${ request.dateCreated ? request.dateCreated.format('dd/MM HH:mm') : '' }</td>
                <td class="queue-name">${ patientLabel }</td>
                <td class="queue-id">${ patientIdLabel }</td>
                <td>${ age == null ? '' : age }</td>
                <td>${ gender ?: '' }</td>
                <td>${ consumableRequest ? 'Consumable' : 'Prescription' }</td>
                <td><span class="queue-badge">${ drugCountByRequestId[request.patientDispensingRequestId] ?: 0 }</span></td>
                <td><span class="${ waitClass }">${ waitTime }</span></td>
                <td><span class="${ paid ? 'queue-paid' : (exceptionApproved ? 'queue-exception' : 'queue-unpaid') }">${ paid ? 'Paid' : (exceptionApproved ? 'Exception' : 'Waiting payment') }</span></td>
                <td>
                    <div class="queue-action-group">
                        <a class="queue-view" href="${ ui.pageLink('rwandapharmacymanagement', 'dispense/withOrder', [requestId: request.patientDispensingRequestId, viewOnly: true]) }">View</a>
                        <a class="queue-refresh" title="Refresh payment status" aria-label="Refresh payment status" href="${ ui.pageLink('rwandapharmacymanagement', 'queue/patientQueue', [pharmacyLocationId: activePharmacy.pharmacyLocationId, q: queueSearch, tab: activeTab, page: queuePage, refreshPaymentRequestId: request.patientDispensingRequestId]) }">&#8635;</a>
                        <% if (ready) { %>
                        <a class="queue-serve" href="${ ui.pageLink('rwandapharmacymanagement', 'dispense/withOrder', [requestId: request.patientDispensingRequestId]) }">Serve</a>
                        <% } else { %>
                        <span class="queue-blocked">Serve</span>
                        <% } %>
                    </div>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <% } %>
        <div class="queue-pager">
            <div>
                Showing ${ queueFirstResult ?: 0 }-${ queueLastResult ?: 0 } of ${ queueTotal ?: 0 }
                <% if (queueSearch) { %> matching "${ queueSearch }"<% } %>
            </div>
            <div class="queue-pager-actions">
                <% if (queueHasPreviousPage) { %>
                <a class="queue-view" href="${ ui.pageLink('rwandapharmacymanagement', 'queue/patientQueue', [pharmacyLocationId: activePharmacy.pharmacyLocationId, q: queueSearch, tab: activeTab, page: queuePage - 1]) }">Previous</a>
                <% } %>
                <span>Page ${ queuePage ?: 1 }</span>
                <% if (queueHasNextPage) { %>
                <a class="queue-view" href="${ ui.pageLink('rwandapharmacymanagement', 'queue/patientQueue', [pharmacyLocationId: activePharmacy.pharmacyLocationId, q: queueSearch, tab: activeTab, page: queuePage + 1]) }">Next</a>
                <% } %>
            </div>
        </div>
    </div>
</div>
<% } %>
