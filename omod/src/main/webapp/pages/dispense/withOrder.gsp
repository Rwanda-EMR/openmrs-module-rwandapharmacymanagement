<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def patientIdMap = binding.hasVariable("patientIdentifierByRequestId") && patientIdentifierByRequestId ? patientIdentifierByRequestId : [:]
    def batchMap = binding.hasVariable("batchOptionsByRequestLineId") && batchOptionsByRequestLineId ? batchOptionsByRequestLineId : [:]
    def prescriptionMap = binding.hasVariable("prescriptionDetailByRequestLineId") && prescriptionDetailByRequestLineId ? prescriptionDetailByRequestLineId : [:]
    def sourceHintMap = binding.hasVariable("sourcePharmacyHintByRequestLineId") && sourcePharmacyHintByRequestLineId ? sourcePharmacyHintByRequestLineId : [:]
    def remainingMap = binding.hasVariable("remainingQuantityByRequestLineId") && remainingQuantityByRequestLineId ? remainingQuantityByRequestLineId : [:]
    def selectionError = binding.hasVariable("dispensingSelectionError") ? dispensingSelectionError : null
    def viewOnlyMode = binding.hasVariable("viewOnly") && viewOnly ? true : false
    def consumableRequest = selectedRequest?.requestType?.toString() == 'CONSUMABLE'
    def paymentExceptionChoices = binding.hasVariable("paymentExceptionTypes") && paymentExceptionTypes ? paymentExceptionTypes : []
    def canApprovePaymentException = binding.hasVariable("canApprovePaymentException") && canApprovePaymentException ? true : false
    def paymentExceptionApproved = selectedRequest?.paymentExceptionApproved ? true : false
    def paymentSatisfied = selectedRequest?.paymentConfirmed || paymentExceptionApproved
    def userName = { user -> user?.personName?.toString() ?: user?.username ?: '' }
    def requesterName = { request ->
        request?.orderingProvider?.name ?: userName(request?.creator)
    }
%>
<style type="text/css">
    .serve-page { display:grid; gap:14px; }
    .serve-banner { padding:10px 12px; border-radius:4px; border:1px solid #f0ad4e; background:#fcf8e3; color:#8a6d3b; }
    .serve-error { padding:10px 12px; border-radius:4px; border:1px solid #ebccd1; background:#f2dede; color:#a94442; }
    .serve-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; overflow:hidden; }
    .serve-card-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:600; color:#334155; }
    .serve-card-body { padding:14px; }
    .serve-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
    .serve-meta { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:10px; }
    .serve-meta-card { border:1px solid #dbe5ec; border-radius:4px; padding:10px 12px; background:#fff; }
    .serve-meta-label { font-size:11px; color:#64748b; }
    .serve-meta-value { font-size:14px; font-weight:600; color:#0f172a; margin-top:3px; }
    .serve-field label { display:block; margin-bottom:4px; font-size:12px; font-weight:600; color:#334155; }
    .serve-field select { width:100%; padding:7px 8px; border:1px solid #cbd5e1; border-radius:4px; }
    .serve-actions { display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap; }
    .serve-btn { display:inline-block; padding:7px 12px; border-radius:4px; border:1px solid #337ab7; background:#337ab7; color:#fff; text-decoration:none; cursor:pointer; font-size:12px; font-weight:600; }
    .serve-btn-secondary { background:#fff; color:#334155; border-color:#cbd5e1; }
    .serve-detail { border:1px solid #dbe5ec; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .serve-detail-head { padding:10px 12px; background:#fafcfe; border-bottom:1px solid #dbe5ec; }
    .serve-detail-title { font-weight:700; color:#0f172a; }
    .serve-detail-sub { margin-top:4px; color:#64748b; font-size:12px; }
    .serve-detail-sub strong { color:#0f172a; }
    .serve-detail-body { padding:12px; }
    .serve-rx-grid { display:grid; grid-template-columns:repeat(5, minmax(0, 1fr)); gap:10px; margin-bottom:12px; }
    .serve-rx-block { border:1px solid #e5edf3; border-radius:4px; padding:8px 10px; background:#fff; }
    .serve-rx-label { font-size:10px; color:#64748b; text-transform:uppercase; letter-spacing:.04em; }
    .serve-rx-value { margin-top:3px; font-size:12px; color:#0f172a; }
    .serve-batch-table { width:100%; border-collapse:collapse; }
    .serve-batch-table th, .serve-batch-table td { padding:8px 10px; border:1px solid #e5edf3; text-align:left; font-size:12px; vertical-align:middle; }
    .serve-batch-table th { background:#f8fbfd; color:#475569; }
    .serve-batch-table input[type="number"] { width:100%; padding:6px 8px; border:1px solid #cbd5e1; border-radius:4px; }
    .serve-muted { color:#64748b; font-size:12px; }
    .serve-empty { padding:18px 14px; color:#64748b; }
    .serve-source-hint { margin-top:8px; padding:10px 12px; border-radius:4px; border:1px solid #d9edf7; background:#f4fbff; color:#245269; font-size:12px; }
    .serve-complete { margin-top:8px; padding:10px 12px; border-radius:4px; border:1px solid #d6e9c6; background:#f5fff3; color:#3c763d; font-size:12px; }
    .serve-pill { display:inline-block; padding:2px 8px; border-radius:999px; font-size:11px; font-weight:700; color:#fff; background:#16a34a; }
    .serve-no-bill-badge { display:inline-block; margin-left:8px; padding:2px 8px; border-radius:999px; background:#7c3aed; color:#fff; font-size:10px; font-weight:700; vertical-align:middle; }
    .serve-no-bill-note { margin-top:6px; padding:8px 10px; border-radius:4px; border:1px solid #ddd6fe; background:#f5f3ff; color:#5b21b6; font-size:12px; }
    .serve-exception-box { margin-top:14px; border:1px solid #f0ad4e; background:#fff8e8; border-radius:4px; padding:12px; }
    .serve-exception-box h4 { margin:0 0 8px; color:#8a6d3b; }
    .serve-exception-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
    .serve-exception-grid label { display:block; margin-bottom:4px; font-size:12px; font-weight:600; color:#334155; }
    .serve-exception-grid select, .serve-exception-grid input, .serve-exception-grid textarea { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:7px 8px; font:inherit; background:#fff; }
    .serve-exception-grid textarea { min-height:68px; }
    @media (max-width: 1100px) {
        .serve-grid, .serve-meta, .serve-rx-grid { grid-template-columns:1fr; }
    }
</style>

<h2>${ viewOnlyMode ? (consumableRequest ? 'View consumable request' : 'View prescription') : (consumableRequest ? 'Dispense consumables' : 'Dispense medication') }</h2>

<% if (selectionError) { %>
<div class="serve-error">${ selectionError }</div>
<% } %>

<% if (pharmacySelectionRequired) { %>
<div class="serve-banner">
    <form method="get" style="margin:0;display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
        <strong>Choose pharmacy</strong>
        <span>for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.</span>
        <% if (selectedRequest) { %><input type="hidden" name="requestId" value="${ selectedRequest.patientDispensingRequestId }"/><% } %>
        <select name="pharmacyLocationId" required="required">
            <option value="">Select pharmacy</option>
            <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
        </select>
        <button type="submit">Use pharmacy</button>
    </form>
</div>
<% } else if (!activePharmacy) { %>
<div class="serve-banner">No pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.</div>
<% } else { %>
<div class="serve-page">
    <div class="serve-card">
        <div class="serve-card-head">Queue selection</div>
        <div class="serve-card-body serve-grid">
            <% if (paymentRefreshRequested) { %>
            <div class="serve-banner" style="grid-column:1 / -1;">Payment status refresh completed.</div>
            <% } %>
            <div class="serve-field">
                <label>Dispensing pharmacy</label>
                <div class="serve-meta-value">${ activePharmacy.name }</div>
            </div>
            <div class="serve-field">
                <form method="get" style="margin:0;">
                    <label>Queued request</label>
                    <div class="serve-actions">
                        <% if (viewOnlyMode) { %><input type="hidden" name="viewOnly" value="true"/><% } %>
                        <select name="requestId">
                            <option value="">Select request to serve</option>
                            <% requests.each { request -> %>
                            <option value="${ request.patientDispensingRequestId }" ${ selectedRequest?.patientDispensingRequestId == request.patientDispensingRequestId ? 'selected="selected"' : '' }>
                                ${ patientIdMap[request.patientDispensingRequestId] ?: '' } - ${ request.patient?.personName ?: '' }${ requesterName(request) ? ' - requested by ' + requesterName(request) : '' }
                            </option>
                            <% } %>
                        </select>
                        <button type="submit" class="serve-btn">Load request</button>
                        <a class="serve-btn serve-btn-secondary" href="${ ui.pageLink('rwandapharmacymanagement', 'dispense/withOrder', [requestId: selectedRequest?.patientDispensingRequestId, viewOnly: viewOnlyMode, pharmacyLocationId: activePharmacy.pharmacyLocationId, refreshPayments: true]) }">Refresh payment status</a>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <% if (selectedRequest) { %>
    <div class="serve-card">
        <div class="serve-card-head">${ consumableRequest ? 'Consumable request details' : 'Prescription details' }</div>
        <div class="serve-card-body">
            <div class="serve-meta">
                <div class="serve-meta-card">
                    <div class="serve-meta-label">Patient</div>
                    <div class="serve-meta-value">${ selectedRequest.patient?.personName ?: '' }</div>
                </div>
                <div class="serve-meta-card">
                    <div class="serve-meta-label">Patient ID</div>
                    <div class="serve-meta-value">${ patientIdMap[selectedRequest.patientDispensingRequestId] ?: '' }</div>
                </div>
                <div class="serve-meta-card">
                    <div class="serve-meta-label">Target</div>
                    <div class="serve-meta-value">${ requestTargetDescriptionById[selectedRequest.patientDispensingRequestId] ?: '' }</div>
                </div>
                <div class="serve-meta-card">
                    <div class="serve-meta-label">Payment</div>
                    <div class="serve-meta-value">
                        <span class="serve-pill" style="${ selectedRequest?.paymentConfirmed ? '' : (paymentExceptionApproved ? 'background:#7c3aed;' : 'background:#f59e0b;') }">
                            ${ selectedRequest?.paymentConfirmed ? 'Paid' : (paymentExceptionApproved ? 'Exception approved' : 'Not paid') }
                        </span>
                    </div>
                </div>
                <div class="serve-meta-card">
                    <div class="serve-meta-label">Requested by</div>
                    <div class="serve-meta-value">${ requesterName(selectedRequest) ?: '' }</div>
                </div>
            </div>

            <% if (paymentExceptionApproved) { %>
            <div class="serve-exception-box">
                <h4>Dispense authorized without confirmed payment</h4>
                <div class="serve-muted">
                    Reason: <strong>${ selectedRequest.paymentExceptionType ?: '' }</strong>
                    ${ selectedRequest.paymentExceptionReason ? ' - ' + selectedRequest.paymentExceptionReason : '' }
                    ${ selectedRequest.paymentExceptionApprovedBy ? ' | Approved by ' + userName(selectedRequest.paymentExceptionApprovedBy) : '' }
                    ${ selectedRequest.paymentExceptionApprovedDate ? ' on ' + selectedRequest.paymentExceptionApprovedDate.format('dd/MMM/yyyy HH:mm') : '' }
                </div>
                <% if (selectedRequest.paymentExceptionNote) { %>
                <div class="serve-muted" style="margin-top:6px;">Note: ${ selectedRequest.paymentExceptionNote }</div>
                <% } %>
            </div>
            <% } else if (!selectedRequest?.paymentConfirmed && canApprovePaymentException) { %>
            <div class="serve-exception-box">
                <h4>Authorize dispense exception</h4>
                <div class="serve-muted" style="margin-bottom:10px;">Use this only when dispensing is approved even though billing has not confirmed payment.</div>
                <div class="serve-exception-grid">
                    <div>
                        <label>Exception type</label>
                        <select name="paymentExceptionType" form="payment-exception-form" required="required">
                            <option value="">Select reason type</option>
                            <% paymentExceptionChoices.each { type -> %>
                            <option value="${ type.name() }">${ type.name().replace('_', ' ') }</option>
                            <% } %>
                        </select>
                    </div>
                    <div>
                        <label>Reason</label>
                        <input type="text" name="paymentExceptionReason" form="payment-exception-form" required="required" placeholder="e.g. approved by finance, emergency, system down"/>
                    </div>
                    <div style="grid-column:1 / -1;">
                        <label>Note</label>
                        <textarea name="paymentExceptionNote" form="payment-exception-form" placeholder="Optional supporting details"></textarea>
                    </div>
                </div>
                <form method="post" id="payment-exception-form" style="margin-top:10px;">
                    <input type="hidden" name="requestId" value="${ selectedRequest.patientDispensingRequestId }"/>
                    <input type="hidden" name="action" value="approvePaymentException"/>
                    <% if (viewOnlyMode) { %><input type="hidden" name="viewOnly" value="true"/><% } %>
                    <button type="submit" class="serve-btn" style="background:#7c3aed;border-color:#6d28d9;">Approve dispense exception</button>
                </form>
            </div>
            <% } else if (!selectedRequest?.paymentConfirmed) { %>
            <div class="serve-exception-box">
                <h4>Payment exception approval required</h4>
                <div class="serve-muted">You do not have permission to approve dispensing without confirmed payment. Ask a user with payment exception approval rights to authorize this request.</div>
            </div>
            <% } %>

            <form method="post" id="serve-request-form" style="margin-top:14px;">
                <input type="hidden" name="requestId" value="${ selectedRequest.patientDispensingRequestId }"/>
                <% if (viewOnlyMode) { %><input type="hidden" name="viewOnly" value="true"/><% } %>
                <% selectedRequest.lines?.each { line ->
                    def details = prescriptionMap[line.patientDispensingRequestLineId] ?: [:]
                    def options = batchMap[line.patientDispensingRequestLineId] ?: []
                    def sourceHint = sourceHintMap[line.patientDispensingRequestLineId]
                    def remainingQty = remainingMap[line.patientDispensingRequestLineId] != null ? remainingMap[line.patientDispensingRequestLineId] : line.requestedQuantity
                    def lineServed = (remainingQty ?: 0) <= 0
                    def lineUnavailable = line.lineStatus?.toString() == 'UNAVAILABLE'
                    def lineHidden = line.item?.visibleToRequesters == false
                    def lineNotBillable = line.billable == false
                %>
                <div class="serve-detail" data-requested-qty="${ remainingQty ?: 0 }">
                    <div class="serve-detail-head">
                        <div class="serve-detail-title">
                            ${ line.item?.name }
                            <% if (lineNotBillable) { %><span class="serve-no-bill-badge">Not billable</span><% } %>
                        </div>
                        <div class="serve-detail-sub">
                            Requested quantity: <strong>${ line.requestedQuantity }</strong>
                            | Remaining: <strong>${ remainingQty }</strong>
                            | Allocated: <strong class="serve-allocated-total">0</strong>
                        </div>
                        <% if (lineNotBillable) { %>
                        <div class="serve-no-bill-note">This item was intentionally excluded from billing. Reason: <strong>${ line.billingExclusionReason ?: 'No reason recorded' }</strong></div>
                        <% } %>
                    </div>
                    <div class="serve-detail-body">
                        <% if (consumableRequest) { %>
                        <div class="serve-rx-grid" style="grid-template-columns:repeat(3, minmax(0, 1fr));">
                            <div class="serve-rx-block"><div class="serve-rx-label">Requested quantity</div><div class="serve-rx-value">${ line.requestedQuantity ?: '' }</div></div>
                            <div class="serve-rx-block"><div class="serve-rx-label">Unit of measure</div><div class="serve-rx-value">${ line.unitOfMeasure ?: '' }</div></div>
                            <div class="serve-rx-block"><div class="serve-rx-label">Payment</div><div class="serve-rx-value">${ lineNotBillable ? 'Not billable' : (paymentSatisfied ? (paymentExceptionApproved && !selectedRequest?.paymentConfirmed ? 'Exception authorized' : 'Ready to dispense') : 'Waiting for payment') }</div></div>
                        </div>
                        <% } else { %>
                        <div class="serve-rx-grid">
                            <div class="serve-rx-block"><div class="serve-rx-label">Dose</div><div class="serve-rx-value">${ details.dose ?: '' }</div></div>
                            <div class="serve-rx-block"><div class="serve-rx-label">Route</div><div class="serve-rx-value">${ details.route ?: '' }</div></div>
                            <div class="serve-rx-block"><div class="serve-rx-label">Frequency</div><div class="serve-rx-value">${ details.frequency ?: '' }</div></div>
                            <div class="serve-rx-block"><div class="serve-rx-label">Duration</div><div class="serve-rx-value">${ details.duration ?: '' } ${ details.durationUnits ?: '' }</div></div>
                            <div class="serve-rx-block"><div class="serve-rx-label">Instructions</div><div class="serve-rx-value">${ details.instructions ?: '' }</div></div>
                        </div>
                        <% } %>

                        <% if (lineServed) { %>
                        <div class="serve-complete">${ lineUnavailable ? 'This item was marked as not available in all pharmacies and has been closed from the queue.' : 'This item has already been fully served and cannot be dispensed again.' }</div>
                        <% } else if (lineHidden) { %>
                        <div class="serve-empty">This ${ consumableRequest ? 'item' : 'drug' } is hidden from requesters and cannot be dispensed.</div>
                        <div class="serve-source-hint" style="border-color:#f0ad4e;background:#fff8e8;color:#8a6d3b;">
                            Treat it as unavailable unless main pharmacy makes it visible again.
                        </div>
                        <% } else if (!options) { %>
                        <div class="serve-empty">No available stock batches were found for this ${ consumableRequest ? 'item' : 'drug' } at ${ activePharmacy.name }.</div>
                        <% if (sourceHint) { %><div class="serve-source-hint">Request this item from <strong>${ sourceHint }</strong>.</div><% } else { %>
                        <div class="serve-source-hint" style="border-color:#f0ad4e;background:#fff8e8;color:#8a6d3b;">
                            This drug is not available in any pharmacy. If it cannot be obtained now, you can close it from the queue.
                        </div>
                        <% } %>
                        <% } else { %>
                        <table class="serve-batch-table serve-batch-group" data-request-line-id="${ line.patientDispensingRequestLineId }">
                            <thead>
                            <tr>
                                <th>Batch</th>
                                <th>Expiry</th>
                                <th>Available</th>
                                <th>Remaining quantity</th>
                                <th>Dispense quantity</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% options.each { balance -> %>
                            <tr>
                                <td>
                                    <input type="hidden" name="requestLineId" value="${ line.patientDispensingRequestLineId }"/>
                                    <input type="hidden" name="itemId" value="${ line.item?.itemId }"/>
                                    <input type="hidden" name="batchNumber" value="${ balance.batchNumber ?: '' }"/>
                                    <input type="hidden" name="expiryDate" value="${ balance.expiryDate ? balance.expiryDate.format('yyyy-MM-dd') : '' }"/>
                                    ${ balance.batchNumber ?: '' }
                                </td>
                                <td>${ balance.expiryDate ? balance.expiryDate.format('dd/MMM/yyyy') : '' }</td>
                                <td>${ balance.quantityOnHand ?: 0 }</td>
                                <td>${ remainingQty ?: 0 }</td>
                                <td>
                                    <input type="number"
                                           step="0.01"
                                           min="0"
                                           max="${ balance.quantityOnHand ?: 0 }"
                                           name="quantity"
                                           class="serve-qty-input"
                                           data-available="${ balance.quantityOnHand ?: 0 }"
                                           data-request-line-id="${ line.patientDispensingRequestLineId }"
                                           value="0"
                                           ${ viewOnlyMode || lineServed ? 'disabled="disabled"' : '' }/>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                        <div class="serve-muted" style="margin-top:8px;">Batches are listed in FIFO order based on expiry date. Adjust quantities if you need to split dispensing across more than one batch.</div>
                        <% if (sourceHint) { %><div class="serve-source-hint">Current pharmacy cannot fully serve this line. Request the remaining stock from <strong>${ sourceHint }</strong>.</div><% } %>
                        <% } %>
                        <% if (!lineServed) { %>
                        <div style="margin-top:8px;">
                            <button type="submit"
                                    class="serve-btn serve-btn-secondary"
                                    name="markUnavailableLineId"
                                    value="${ line.patientDispensingRequestLineId }">Item not available in all pharmacies</button>
                        </div>
                        <% } %>
                    </div>
                </div>
                <% } %>

                <div class="serve-actions">
                    <a class="serve-btn serve-btn-secondary" href="${ ui.pageLink('rwandapharmacymanagement', 'queue/patientQueue') }">Back to queue</a>
                    <% if (viewOnlyMode) { %>
                    <a class="serve-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'dispense/withOrder', [requestId: selectedRequest.patientDispensingRequestId]) }">Open serve form</a>
                    <% } else if (!paymentSatisfied) { %>
                    <span class="serve-btn" style="background:#f59e0b;border-color:#d97706;">Waiting for payment or exception approval</span>
                    <% } else { %>
                    <button type="submit" class="serve-btn">${ consumableRequest ? 'Serve this request' : 'Serve this prescription' }</button>
                    <% } %>
                </div>
            </form>
        </div>
    </div>
    <% } else { %>
    <div class="serve-card">
        <div class="serve-card-head">Request details</div>
        <div class="serve-empty">Select a queued request to review it and serve it.</div>
    </div>
    <% } %>
</div>
<script type="text/javascript">
    function parseNumber(value) {
        var parsed = parseFloat(value || '0');
        return isNaN(parsed) ? 0 : parsed;
    }

    function updateAllocationSummary(group) {
        if (!group) {
            return 0;
        }
        var container = group.closest('.serve-detail');
        var requested = parseNumber(container.getAttribute('data-requested-qty'));
        var inputs = group.querySelectorAll('.serve-qty-input');
        var total = 0;
        for (var i = 0; i < inputs.length; i++) {
            total += parseNumber(inputs[i].value);
        }
        var label = container.querySelector('.serve-allocated-total');
        if (label) {
            label.textContent = total;
            label.style.color = total > requested + 0.0001 ? '#dc2626' : '#0f172a';
        }
        return total;
    }

    function allocateFifo() {
        var groups = document.querySelectorAll('.serve-batch-group');
        for (var g = 0; g < groups.length; g++) {
            var group = groups[g];
            var container = group.closest('.serve-detail');
            var remaining = parseNumber(container.getAttribute('data-requested-qty'));
            var inputs = group.querySelectorAll('.serve-qty-input');
            for (var i = 0; i < inputs.length; i++) {
                var input = inputs[i];
                var available = parseNumber(input.getAttribute('data-available'));
                var allocate = remaining > 0 ? Math.min(remaining, available) : 0;
                input.value = allocate > 0 ? allocate : 0;
                remaining = remaining - allocate;
            }
            updateAllocationSummary(group);
        }
    }

    function bindAllocationInputs() {
        var qtyInputs = document.querySelectorAll('.serve-qty-input');
        for (var i = 0; i < qtyInputs.length; i++) {
            qtyInputs[i].oninput = function () {
                updateAllocationSummary(this.closest('.serve-batch-group'));
            };
        }
    }

    function validateServeForm() {
        var activeSubmit = window.__serveSubmitButton || null;
        if (activeSubmit && activeSubmit.name === 'markUnavailableLineId') {
            return true;
        }
        var qtyInputs = document.querySelectorAll('.serve-qty-input');
        var total = 0;
        for (var i = 0; i < qtyInputs.length; i++) {
            total += parseNumber(qtyInputs[i].value);
        }
        if (!(total > 0)) {
            alert('Enter at least one dispense quantity before serving this prescription.');
            return false;
        }
        var groups = document.querySelectorAll('.serve-batch-group');
        for (var g = 0; g < groups.length; g++) {
            var group = groups[g];
            var container = group.closest('.serve-detail');
            var requested = parseNumber(container.getAttribute('data-requested-qty'));
            var allocated = updateAllocationSummary(group);
            if (allocated > requested + 0.0001) {
                alert('Allocated quantity cannot exceed the prescribed quantity for this drug.');
                return false;
            }
        }
        return true;
    }

    if (!${ viewOnlyMode ? 'true' : 'false' }) {
        var submitButtons = document.querySelectorAll('#serve-request-form button[type="submit"]');
        for (var b = 0; b < submitButtons.length; b++) {
            submitButtons[b].addEventListener('click', function () {
                window.__serveSubmitButton = this;
            });
        }
        document.getElementById('serve-request-form') && (document.getElementById('serve-request-form').onsubmit = validateServeForm);
        allocateFifo();
        bindAllocationInputs();
    }
</script>
<% } %>
