<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<style type="text/css">
    .queue-shell { display:grid; gap:14px; }
    .queue-banner { padding:10px 12px; border-radius:4px; border:1px solid #bce8f1; background:#d9edf7; color:#31708f; }
    .queue-warn { border-color:#f0ad4e; background:#fcf8e3; color:#8a6d3b; }
    .queue-panel { background:#fff; border:1px solid #d8e2ea; border-radius:4px; overflow:hidden; }
    .queue-panel-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .queue-toolbar { display:flex; justify-content:space-between; align-items:center; gap:10px; padding:12px 14px; border-bottom:1px solid #e5edf3; background:#fff; flex-wrap:wrap; }
    .queue-search { min-width:260px; max-width:420px; width:100%; }
    .queue-search input { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font-size:12px; }
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
    .queue-action-group { display:flex; gap:6px; flex-wrap:wrap; }
    .queue-serve { display:inline-block; padding:4px 10px; border-radius:4px; border:1px solid #2e6da4; background:#337ab7; color:#fff; text-decoration:none; font-size:11px; font-weight:600; }
    .queue-serve:hover { background:#286090; }
    .queue-view { display:inline-block; padding:4px 10px; border-radius:4px; border:1px solid #64748b; background:#fff; color:#334155; text-decoration:none; font-size:11px; font-weight:600; }
    .queue-blocked { display:inline-block; padding:4px 10px; border-radius:4px; border:1px solid #d1d5db; background:#f8fafc; color:#94a3b8; font-size:11px; font-weight:600; cursor:not-allowed; }
    .queue-empty { padding:18px 14px; color:#64748b; }
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
    <div class="queue-panel">
        <div class="queue-panel-head">Queue (${ requests?.size() ?: 0 } patients)</div>
        <div class="queue-toolbar">
            <div class="queue-search">
                <input type="text" id="queue-search-input" placeholder="Search by patient identifier or patient name"/>
            </div>
            <div class="queue-empty" id="queue-search-empty" style="display:none;padding:0;">No queue rows match that search.</div>
        </div>
        <% if (!requests) { %>
        <div class="queue-empty">No dispensing requests are waiting for ${ activePharmacy.name }.</div>
        <% } else { %>
        <table class="queue-table">
            <thead>
            <tr>
                <th>#</th>
                <th>Patient name</th>
                <th>Patient ID</th>
                <th>Age</th>
                <th>Gender</th>
                <th>Type</th>
                <th>Items</th>
                <th>Wait time</th>
                <th>Payment</th>
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
                def patientLabel = request.patient?.personName ? request.patient.personName.toString() : ''
                def patientIdLabel = patientIdentifierByRequestId[request.patientDispensingRequestId] ?: ''
            %>
            <tr class="queue-row" data-search="${ (patientLabel + ' ' + patientIdLabel).toLowerCase() }">
                <td>${ idx + 1 }</td>
                <td class="queue-name">${ patientLabel }</td>
                <td class="queue-id">${ patientIdLabel }</td>
                <td>${ age == null ? '' : age }</td>
                <td>${ gender ?: '' }</td>
                <td>${ request.requestType?.toString() == 'CONSUMABLE' ? 'Consumable' : 'Prescription' }</td>
                <td><span class="queue-badge">${ drugCountByRequestId[request.patientDispensingRequestId] ?: 0 }</span></td>
                <td><span class="${ waitClass }">${ waitTime }</span></td>
                <td><span class="${ paid ? 'queue-paid' : 'queue-unpaid' }">${ paid ? 'Paid' : 'Not paid' }</span></td>
                <td>
                    <div class="queue-action-group">
                        <a class="queue-view" href="${ ui.pageLink('rwandapharmacymanagement', 'dispense/withOrder', [requestId: request.patientDispensingRequestId, viewOnly: true]) }">View</a>
                        <% if (paid) { %>
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
    </div>
</div>
<script type="text/javascript">
    (function() {
        var input = document.getElementById('queue-search-input');
        var rows = document.querySelectorAll('.queue-row');
        var empty = document.getElementById('queue-search-empty');
        if (!input || !rows.length) {
            return;
        }
        input.oninput = function() {
            var query = (input.value || '').toLowerCase().trim();
            var visible = 0;
            for (var i = 0; i < rows.length; i++) {
                var row = rows[i];
                var match = !query || (row.getAttribute('data-search') || '').indexOf(query) >= 0;
                row.style.display = match ? '' : 'none';
                if (match) {
                    visible++;
                }
            }
            if (empty) {
                empty.style.display = visible ? 'none' : 'block';
            }
        };
    })();
</script>
<% } %>
