<%
    ui.decorateWith("appui", "standardEmrPage", [ title: "Prescription" ])
    ui.includeCss("coreapps", "patientHeader.css")
    def prescriptionSavedFlag = false
    if (binding.hasVariable("prescriptionSaved")) {
        prescriptionSavedFlag = prescriptionSaved ? true : false
    }
    def pharmacyList = candidatePharmacies ?: []
    def patientDashboardLink = patient ? ui.pageLink("coreapps", "clinicianfacing/patient", [ patientId: patient.patientId ]) : ""
%>
<style type="text/css">
    #content { max-width: 100%; padding-top: 12px; }
    .rx-page { display:grid; gap:16px; }
    .rx-page-title { display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; }
    .rx-page-title h2 { margin:0; }
    .rx-page-title p { margin:4px 0 0; color:#64748b; font-size:12px; }
    .rx-header-note { display:inline-flex; align-items:center; gap:8px; color:#64748b; font-size:12px; }
    .rx-toggle-wrap { display:flex; justify-content:flex-end; }
    .rx-shell.is-hidden { display:none; }
    .rx-patient-header { background:#fff; border:1px solid #d7dee8; border-radius:4px; padding:16px 18px; }
    .rx-patient-header .name { cursor:pointer; color:#2c3e50; }
    .rx-patient-header .name:hover { color:#00455c; text-decoration:underline; }
    .rx-patient-summary { display:grid; grid-template-columns:minmax(0, 1.25fr) minmax(280px, .75fr); gap:18px; }
    .rx-context-strip { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:10px; margin-top:14px; }
    .rx-context-card { border:1px solid #dbe5f0; border-radius:4px; background:#f8fbff; padding:10px 12px; }
    .rx-context-card em { display:block; font-style:normal; text-transform:uppercase; letter-spacing:.04em; font-size:10px; color:#64748b; }
    .rx-context-card strong { display:block; margin-top:4px; color:#0f172a; font-size:13px; }
    .rx-shell { display:grid; grid-template-columns:minmax(0, 1.18fr) minmax(340px, .92fr); gap:18px; align-items:start; }
    .rx-panel { background:#fff; border:1px solid #d7dee8; border-radius:4px; overflow:hidden; }
    .rx-panel-head { background:#1f5f99; color:#fff; padding:10px 14px; font-weight:600; font-size:13px; }
    .rx-panel-body { padding:14px; }
    .rx-grid-2 { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
    .rx-grid-3 { display:grid; grid-template-columns:1fr 1fr 1fr; gap:12px; }
    .rx-field { margin-bottom:12px; }
    .rx-field label { display:block; margin-bottom:4px; font-size:12px; font-weight:600; color:#334155; }
    .rx-field input, .rx-field select, .rx-field textarea { width:100%; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font:inherit; background:#fff; }
    .rx-field textarea { min-height:68px; resize:vertical; }
    .rx-readonly { background:#f8fafc !important; color:#334155; }
    .rx-muted { color:#64748b; font-size:12px; }
    .rx-banner { margin:0 0 14px; padding:10px 12px; border-radius:4px; border:1px solid #bfdbfe; background:#eff6ff; color:#1d4ed8; }
    .rx-warn { border-color:#fcd34d; background:#fffbeb; color:#92400e; }
    .rx-btn { display:inline-flex; align-items:center; justify-content:center; border:1px solid #cbd5e1; border-radius:4px; padding:8px 14px; cursor:pointer; background:#fff; color:#0f172a; font:inherit; }
    .rx-btn-primary { background:#16a34a; border-color:#15803d; color:#fff; }
    .rx-btn-danger { background:#e11d48; border-color:#be123c; color:#fff; }
    .rx-btn-add { width:100%; background:#1f5f99; border-color:#1f5f99; color:#fff; }
    .rx-summary-table { width:100%; border-collapse:collapse; margin-top:10px; }
    .rx-summary-table th, .rx-summary-table td { border:1px solid #dbe5f0; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .rx-summary-table th { background:#eef5fb; color:#475569; }
    .rx-total { display:grid; grid-template-columns:repeat(3, 1fr); gap:10px; margin-top:12px; }
    .rx-total-card { border:1px solid #dbe5f0; border-radius:4px; background:#fff; padding:10px; text-align:center; }
    .rx-total-card strong { display:block; color:#0369a1; font-size:16px; margin-top:4px; }
    .rx-summary-actions { display:flex; justify-content:space-between; gap:10px; align-items:center; flex-wrap:wrap; }
    .rx-summary-actions-right { display:flex; gap:10px; }
    .rx-inline-note { font-size:11px; color:#64748b; margin-top:4px; }
    .rx-suggestions { display:none; width:100%; max-height:220px; overflow-y:auto; border:1px solid #cbd5e1; border-top:0; border-radius:0 0 4px 4px; background:#fff; }
    .rx-suggestions.is-visible { display:block; }
    .rx-suggestion { padding:8px 10px; cursor:pointer; border-top:1px solid #eef2f7; font-size:12px; }
    .rx-suggestion:hover { background:#eef5fb; }
    .rx-suggestion strong { display:block; color:#0f172a; }
    .rx-suggestion span { display:block; color:#64748b; margin-top:2px; }
    .rx-suggestion-empty { padding:8px 10px; color:#64748b; font-size:12px; }
    .rx-check-row { display:flex; align-items:center; gap:8px; margin-top:6px; color:#475569; font-size:12px; }
    .rx-check-row input { width:auto; margin:0; }
    .rx-remove-link { border:0; background:#fff1f2; color:#be123c; padding:4px 8px; border-radius:3px; cursor:pointer; }
    .rx-history-panel { background:#fff; border:1px solid #d7dee8; border-radius:4px; overflow:hidden; }
    .rx-history-head { background:#0f766e; color:#fff; padding:10px 14px; font-weight:600; font-size:13px; }
    .rx-history-body { padding:14px; }
    .rx-history-request { border:1px solid #dbe5f0; border-radius:4px; padding:12px; margin-bottom:12px; background:#f8fafc; }
    .rx-history-grid { display:grid; grid-template-columns:repeat(4, minmax(0, 1fr)); gap:10px; margin-bottom:10px; }
    .rx-history-grid strong, .rx-history-line strong { display:block; font-size:11px; text-transform:uppercase; color:#64748b; margin-bottom:4px; }
    .rx-history-line { border-top:1px solid #dbe5f0; padding-top:10px; margin-top:10px; }
    .rx-history-line-grid { display:grid; grid-template-columns:repeat(5, minmax(0, 1fr)); gap:10px; }
    .rx-history-table { width:100%; border-collapse:collapse; }
    .rx-history-table th, .rx-history-table td { border:1px solid #dbe5f0; padding:8px; font-size:12px; text-align:left; vertical-align:top; }
    .rx-history-table th { background:#eef5fb; color:#475569; }
    .rx-line-actions { display:flex; gap:8px; flex-wrap:wrap; align-items:center; }
    .rx-icon-action { display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; border:1px solid #d7dee8; border-radius:3px; background:#fff; color:#2c3e50; text-decoration:none; cursor:pointer; }
    .rx-icon-action:hover { background:#f8fafc; text-decoration:none; }
    .rx-icon-action.rx-danger { color:#be123c; border-color:#fecdd3; background:#fff1f2; }
    .rx-dialog-backdrop { position:fixed; inset:0; background:rgba(15, 23, 42, .55); display:flex; align-items:center; justify-content:center; z-index:2000; }
    .rx-dialog-backdrop.is-hidden { display:none; }
    .rx-dialog { width:min(380px, calc(100vw - 32px)); background:#fff; border-radius:3px; box-shadow:0 14px 24px rgba(0, 0, 0, .35); overflow:hidden; font-family:"OpenSans", Arial, sans-serif; }
    .rx-dialog-header { background:#045d56; color:#fff; padding:10px 16px; border-bottom:1px solid #034741; font-weight:600; display:flex; align-items:center; gap:10px; }
    .rx-dialog-header i { color:#fff; }
    .rx-dialog-body { padding:16px; color:#2c3e50; }
    .rx-dialog-body p { font-style:italic; color:#5a6782; margin-bottom:12px; }
    .rx-dialog-body textarea { width:100%; min-height:96px; border:1px solid #cbd5e1; border-radius:4px; padding:8px 10px; font:inherit; resize:vertical; }
    .rx-dialog-actions { display:flex; justify-content:flex-end; gap:8px; padding:0 16px 16px; }
    .rx-dialog-actions .rx-btn { min-width:92px; border-radius:2px; }
    .rx-dialog-actions .rx-btn-danger { background:#f91863; border-color:#d90e54; }
    .rx-dialog-actions .rx-btn-primary { background:#7ac943; border-color:#66b136; }
    .rx-inline-form { margin-top:10px; padding:12px; border:1px solid #dbe5f0; border-radius:4px; background:#fff; }
    .rx-inline-form.is-hidden { display:none; }
    .rx-inline-actions { display:flex; gap:8px; justify-content:flex-end; flex-wrap:wrap; }
    .rx-status-void { color:#b91c1c; font-weight:600; }
    .rx-pagination { display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; }
    .rx-pagination-links { display:flex; gap:8px; }
    @media (max-width: 1100px) {
        .rx-patient-summary, .rx-context-strip { grid-template-columns:1fr; }
        .rx-shell { grid-template-columns:1fr; }
        .rx-grid-3, .rx-total { grid-template-columns:1fr; }
        .rx-summary-actions { align-items:flex-start; }
        .rx-history-grid, .rx-history-line-grid { grid-template-columns:1fr; }
    }
</style>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' }
        <% if (patient) { %>,
        { label: "${ ui.encodeJavaScript(ui.encodeHtmlContent(ui.format(patient))) }", link: "${ patientDashboardLink }" }
        <% } %>,
        { label: "Prescription" }
    ];
</script>

<div class="rx-page">
    <div class="rx-page-title">
        <div>
            <h2>Prescription</h2>
            <p>Create and queue drug orders without the module sidebar.</p>
        </div>
        <div class="rx-header-note">
            <% if (patient && patientDashboardLink) { %>
            <a href="${ patientDashboardLink }">Back to patient dashboard</a>
            <% } %>
        </div>
    </div>
    <div class="rx-toggle-wrap">
        <button type="button" class="rx-btn rx-btn-primary" id="rx-toggle-button" onclick="togglePrescriptionShell()">Add Prescription</button>
    </div>

    <% if (prescriptionSavedFlag) { %>
    <div class="rx-banner">Prescription saved and queued for the dispensing pharmacies linked to this location.</div>
    <% } %>
    <% if (!patient) { %>
    <div class="rx-banner rx-warn">No patient was supplied. Open this page from the patient dashboard.</div>
    <% } else if (!currentLocation) { %>
    <div class="rx-banner rx-warn">No current OpenMRS location is set for this session.</div>
    <% } else if (!candidatePharmacies) { %>
    <div class="rx-banner rx-warn">No dispensing pharmacy is configured for ${ currentLocation.name }.</div>
    <% } else if (!activeVisit) { %>
    <div class="rx-banner rx-warn">This patient has no active visit. Start a visit before creating a prescription.</div>
    <% } %>

    <% if (patient) { %>
    <div class="patient-header row rx-patient-header">
        <div class="demographics col-12 col-md-7">
            <div class="row align-items-center">
                <div class="col-12 col-sm-auto">
                    <h1 class="mt-0 mb-2 name" onclick="window.location='${ patientDashboardLink }'">
                        <span class="preferred-name">${ ui.encodeHtmlContent(ui.format(patient.personName)) }</span>
                    </h1>
                </div>
                <div class="gender-age col-auto">
                    <span>${ patient.gender == 'M' ? 'Male' : patient.gender == 'F' ? 'Female' : (patient.gender ?: '') }&nbsp;</span>
                    <span>
                        <% if (patient.birthdate) { %>
                        ${ patient.age != null ? patient.age : 'Unknown' } years
                        (${ ui.format(patient.birthdate) })
                        <% } else { %>Unknown age<% } %>
                    </span>
                </div>
                <div class="col-12 mt-2">
                    <span class="contact-info-label">Prescription workflow</span>
                </div>
            </div>
        </div>
        <div class="identifiers mt-2 col-12 col-md-5 d-flex flex-column align-items-md-end">
            <div>
                <em>Patient identifier</em>
                <span>${ patient.patientIdentifier ?: patient.patientId }</span>
            </div>
            <% if (patientBillId) { %>
            <div>
                <em>Billing reference</em>
                <span>${ patientBillId }</span>
            </div>
            <% } %>
            <div>
                <em>Queued pharmacies</em>
                <span><% pharmacyList.eachWithIndex { pharmacy, idx -> %>${ pharmacy.name }${ idx + 1 < pharmacyList.size() ? ', ' : '' }<% } %></span>
            </div>
        </div>
        <div class="secondLineFragments col-12 mt-2 mb-1">
            <div class="rx-context-strip">
                <div class="rx-context-card">
                    <em>Active visit</em>
                    <strong>${ activeVisit ? activeVisit.visitId : 'None' }</strong>
                </div>
                <div class="rx-context-card">
                    <em>Location</em>
                    <strong>${ currentLocation ? currentLocation.name : 'Not set' }</strong>
                </div>
                <div class="rx-context-card">
                    <em>Dispensing pharmacies</em>
                    <strong>${ pharmacyList ? pharmacyList.size() : 0 }</strong>
                </div>
                <div class="rx-context-card">
                    <em>Draft drugs</em>
                    <strong id="rx-header-draft-count">0</strong>
                </div>
            </div>
        </div>
    </div>
    <% } %>

<div class="rx-shell is-hidden" id="rx-shell">
    <div class="rx-panel">
        <div class="rx-panel-head">1. Prescription entry</div>
        <div class="rx-panel-body">
            <div class="rx-field">
                <label>Drug name / item</label>
                <input type="text" id="rx-product-search" placeholder="Type or select a drug" autocomplete="off" oninput="handlePrescriptionProductInput()" onfocus="renderPrescriptionProductSuggestions()"/>
                <input type="hidden" id="rx-product"/>
                <div id="rx-product-suggestions" class="rx-suggestions"></div>
                <datalist id="rx-product-options" style="display:none;">
                    <% prescribableItems.each { item -> %>
                    <% def stocked = stockedProductMasterItemIds?.contains(item.productMasterItemId) %>
                    <% def stockQuantity = stockQuantityByProductMasterItemId?.get(item.productMasterItemId) ?: java.math.BigDecimal.ZERO %>
                    <% def stockQuantityLabel = stockQuantity.stripTrailingZeros().toPlainString() %>
                    <% def optionLabel = stocked ? "${ item.itemName } (Qty: ${ stockQuantityLabel })" : "${ item.itemName } - not in stock" %>
                    <option value="${ optionLabel?.replace('"', '&quot;') ?: '' }"
                            data-product-id="${ item.productMasterItemId }"
                            data-label="${ item.itemName?.replace('"', '&quot;') ?: '' }"
                            data-stocked="${ stocked ? 'true' : 'false' }"
                            data-stock-total="${ stockQuantityLabel }"></option>
                    <% } %>
                </datalist>
                <div class="rx-inline-note">Start typing and select a drug from the suggestions.</div>
                <label class="rx-check-row">
                    <input type="checkbox" id="rx-include-unstocked" onchange="applyPrescriptionDrugFilter()"/>
                    <span>Include drugs not currently in stock</span>
                </label>
            </div>
            <div class="rx-grid-2" style="margin-top:12px;">
                <div class="rx-field">
                    <label>Route</label>
                    <select id="rx-route">
                        <option value="">Select route</option>
                        <% routes.each { route -> %><option value="${ route.displayString }">${ route.displayString }</option><% } %>
                    </select>
                </div>
                <div class="rx-field">
                    <label>Frequency</label>
                    <select id="rx-frequency" onchange="autoCalculateQuantity()" oninput="autoCalculateQuantity()">
                        <option value="">Select frequency</option>
                        <% frequencyOptions.each { frequency -> %><option value="${ frequency.label }" data-frequency-per-day="${ frequency.frequencyPerDay ?: '' }">${ frequency.label }</option><% } %>
                    </select>
                    <div class="rx-inline-note">Quantity uses the numeric meaning parsed from the selected frequency.</div>
                </div>
            </div>
            <div class="rx-grid-3">
                <div class="rx-field">
                    <label>Dose</label>
                    <input type="number" step="0.01" min="0" id="rx-dose" oninput="autoCalculateQuantity()" onchange="autoCalculateQuantity()"/>
                </div>
                <div class="rx-field">
                    <label>Duration</label>
                    <input type="number" min="1" id="rx-duration" oninput="autoCalculateQuantity()" onchange="autoCalculateQuantity()"/>
                </div>
                <div class="rx-field">
                    <label>Duration unit</label>
                    <select id="rx-duration-unit" onchange="autoCalculateQuantity()" oninput="autoCalculateQuantity()">
                        <option value="">Select unit</option>
                        <% durationUnits.each { durationUnit -> %><option value="${ durationUnit.displayString }">${ durationUnit.displayString }</option><% } %>
                    </select>
                </div>
            </div>
            <div class="rx-grid-2">
                <div class="rx-field">
                    <label for="rx-start-date-field">Medication start date</label>
                    <input id="rx-start-date-field" type="date" class="openmrs-date-field" name="rxStartDateDraft"/>
                </div>
                <div class="rx-field">
                    <label>Total quantity to dispense</label>
                    <input type="number" step="0.01" min="0.01" id="rx-quantity"/>
                    <div class="rx-inline-note">Auto-calculated from dose, frequency, and duration. You can still adjust it.</div>
                </div>
            </div>
            <div class="rx-field">
                <label>Dose instructions</label>
                <textarea id="rx-instructions"></textarea>
            </div>
            <% if (patient && activeVisit && candidatePharmacies) { %>
            <button type="button" class="rx-btn rx-btn-add" onclick="addToPrescription()">Add drug to list</button>
            <% } %>
        </div>
    </div>
    <div class="rx-panel">
        <div class="rx-panel-head">2. Prescribed items (<span id="rx-count">0</span>)</div>
        <div class="rx-panel-body">
            <form method="post" id="rx-submit-form">
                <input type="hidden" name="patientId" value="${ patientReferenceForSubmit ?: patient?.patientId ?: patient?.uuid ?: '' }"/>
                <input type="hidden" name="patientUuid" value="${ patientUuidForSubmit ?: patient?.uuid ?: '' }"/>
                <input type="hidden" name="patientBillId" value="${ patientBillId ?: '' }"/>
                <div class="rx-summary-actions">
                    <div class="rx-muted">Build the prescription here, then submit once for the whole list.</div>
                    <div class="rx-summary-actions-right">
                        <button type="button" class="rx-btn rx-btn-danger" onclick="clearPrescription()">Clear prescription</button>
                        <button type="submit" class="rx-btn rx-btn-primary">Submit prescription</button>
                    </div>
                </div>
                <div id="rx-summary-empty" class="rx-muted" style="margin-top:12px;">No prescribed items yet.</div>
                <div id="rx-hidden-fields"></div>
                <table class="rx-summary-table" id="rx-summary-table" style="display:none;">
                    <thead>
                    <tr><th>Drug</th><th>Start date</th><th>Qty</th><th>Frequency</th><th>Duration</th><th></th></tr>
                    </thead>
                    <tbody id="rx-summary-body"></tbody>
                </table>
                <div class="rx-total" id="rx-total-block" style="display:none;">
                    <div class="rx-total-card">Drugs<strong id="rx-total-drugs">0</strong></div>
                    <div class="rx-total-card">Total units<strong id="rx-total-units">0</strong></div>
                </div>
            </form>
        </div>
    </div>
</div>
</div>

<div class="rx-history-panel">
    <div class="rx-history-head">Prescription history</div>
    <div class="rx-history-body">
        <% if (!historyEntries) { %>
        <div class="rx-muted">No prescription history found for this patient.</div>
        <% } else { %>
        <table class="rx-history-table">
            <thead>
            <tr>
                <th>#</th>
                <th>Drug name</th>
                <th>Start date</th>
                <th>Dispensed qty</th>
                <th>Provider</th>
                <th>Dispensing status</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <% int historyRowNumber = 1 + ((historyPage ?: 1) - 1) * (historyPageSize ?: 8) %>
            <% historyEntries.each { entry -> %>
            <% entry.lines.each { line -> %>
            <tr>
                <td>${ historyRowNumber++ }</td>
                <td>${ line.itemName ?: '' }</td>
                <td>${ line.startDate ? line.startDate.format('yyyy-MM-dd') : '' }</td>
                <td>${ line.dispensedQuantity ?: 0 }</td>
                <td>${ line.provider ?: '' }</td>
                <td class="${ line.lineStatus == 'VOIDED' ? 'rx-status-void' : '' }">${ line.lineStatus ?: '' }</td>
                <td>
                    <% if (line.editable) { %>
                    <div class="rx-line-actions">
                        <a href="#" class="rx-icon-action" onclick="toggleLineEdit('${ line.requestLineId }'); return false;" title="Edit prescribed drug">
                            <i class="icon-pencil"></i>
                        </a>
                        <a href="#" class="rx-icon-action rx-danger" onclick="openVoidPrescriptionDialog('${ entry.requestId }', '${ line.requestLineId }', '${ historyPage }', '${ ui.encodeJavaScript(line.itemName ?: '') }'); return false;" title="Delete prescribed drug">
                            <i class="icon-trash"></i>
                        </a>
                    </div>
                    <% } else { %>
                    <span class="rx-muted">Locked</span>
                    <% } %>
                </td>
            </tr>
            <% if (line.editable) { %>
            <tr id="rx-edit-row-${ line.requestLineId }" class="rx-inline-form is-hidden">
                <td colspan="7">
                    <form method="post">
                        <input type="hidden" name="action" value="updateLine"/>
                        <input type="hidden" name="patientId" value="${ patientReferenceForSubmit ?: patient?.patientId ?: patient?.uuid ?: '' }"/>
                        <input type="hidden" name="patientUuid" value="${ patientUuidForSubmit ?: patient?.uuid ?: '' }"/>
                        <input type="hidden" name="requestId" value="${ entry.requestId ?: '' }"/>
                        <input type="hidden" name="requestLineId" value="${ line.requestLineId ?: '' }"/>
                        <input type="hidden" name="historyPage" value="${ historyPage ?: 1 }"/>
                        <div class="rx-grid-2">
                            <div class="rx-field">
                                <label>Drug name</label>
                                <select id="rx-edit-product-${ line.requestLineId }" name="productMasterItemId">
                                    <% prescribableItems.each { item -> %>
                                    <% def editStocked = stockedProductMasterItemIds?.contains(item.productMasterItemId) %>
                                    <% def editStockLabel = editStocked ? ' - in stock' : ' - not in stock' %>
                                    <option value="${ item.productMasterItemId }" ${ line.productMasterItemId == item.productMasterItemId ? 'selected="selected"' : '' }>${ item.itemName }${ editStockLabel }</option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="rx-field">
                                <label>Medication start date</label>
                                <input id="rx-edit-start-date-${ line.requestLineId }" type="date" class="openmrs-date-field" name="startDate" value="${ line.startDate ? line.startDate.format('yyyy-MM-dd') : '' }" required="required"/>
                            </div>
                        </div>
                        <div class="rx-grid-3">
                            <div class="rx-field">
                                <label>Dose</label>
                                <input id="rx-edit-dose-${ line.requestLineId }" type="number" step="0.01" min="0" name="dose" value="${ line.dose ?: '' }" oninput="autoCalculateEditQuantity('${ line.requestLineId }')"/>
                            </div>
                            <div class="rx-field">
                                <label>Route</label>
                                <select id="rx-edit-route-${ line.requestLineId }" name="route">
                                    <option value="">Select route</option>
                                    <% routes.each { route -> %>
                                    <option value="${ route.displayString }" ${ line.route == route.displayString ? 'selected="selected"' : '' }>${ route.displayString }</option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="rx-field">
                                <label>Frequency</label>
                                <select id="rx-edit-frequency-${ line.requestLineId }" name="frequency" onchange="autoCalculateEditQuantity('${ line.requestLineId }')">
                                    <option value="">Select frequency</option>
                                    <% frequencyOptions.each { frequency -> %>
                                    <option value="${ frequency.label }" data-frequency-per-day="${ frequency.frequencyPerDay ?: '' }" ${ line.frequency == frequency.label ? 'selected="selected"' : '' }>${ frequency.label }</option>
                                    <% } %>
                                </select>
                            </div>
                        </div>
                        <div class="rx-grid-3">
                            <div class="rx-field">
                                <label>Duration</label>
                                <input id="rx-edit-duration-${ line.requestLineId }" type="number" min="1" name="duration" value="${ line.duration ?: '' }" oninput="autoCalculateEditQuantity('${ line.requestLineId }')"/>
                            </div>
                            <div class="rx-field">
                                <label>Duration unit</label>
                                <select id="rx-edit-duration-unit-${ line.requestLineId }" name="durationUnit" onchange="autoCalculateEditQuantity('${ line.requestLineId }')">
                                    <option value="">Select unit</option>
                                    <% durationUnits.each { durationUnit -> %>
                                    <option value="${ durationUnit.displayString }" ${ line.durationUnits == durationUnit.displayString ? 'selected="selected"' : '' }>${ durationUnit.displayString }</option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="rx-field">
                                <label>Total quantity to dispense</label>
                                <input id="rx-edit-quantity-${ line.requestLineId }" type="number" step="0.01" min="0.01" name="quantity" value="${ line.requestedQuantity ?: '' }" required="required"/>
                            </div>
                        </div>
                        <div class="rx-field">
                            <label>Dose instructions</label>
                            <textarea name="instructions">${ line.instructions ?: '' }</textarea>
                        </div>
                        <div class="rx-inline-actions">
                            <button type="button" class="rx-btn" onclick="toggleLineEdit('${ line.requestLineId }')">Cancel</button>
                            <button type="submit" class="rx-btn rx-btn-primary">Save changes</button>
                        </div>
                    </form>
                </td>
            </tr>
            <% } %>
            <% } %>
            <% } %>
            </tbody>
        </table>
        <% } %>
        <% def historyBaseParams = [ patientId: patient?.uuid ] %>
        <div class="rx-pagination">
            <div class="rx-muted">Page ${ historyPage } of ${ historyTotalPages } | ${ historyTotalCount } prescriptions</div>
            <div class="rx-pagination-links">
                <% if (historyPage > 1) { %>
                <a class="rx-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'prescription/enter', historyBaseParams + [historyPage: historyPage - 1]) }">Previous</a>
                <% } %>
                <% if (historyPage < historyTotalPages) { %>
                <a class="rx-btn" href="${ ui.pageLink('rwandapharmacymanagement', 'prescription/enter', historyBaseParams + [historyPage: historyPage + 1]) }">Next</a>
                <% } %>
            </div>
        </div>
    </div>
</div>

<div id="rx-void-dialog-backdrop" class="rx-dialog-backdrop is-hidden">
    <div class="rx-dialog" role="dialog" aria-modal="true" aria-labelledby="rx-void-dialog-title">
        <div class="rx-dialog-header" id="rx-void-dialog-title"><i class="icon-folder-open"></i> Discontinued prescribed drug</div>
        <div class="rx-dialog-body">
            <p id="rx-void-dialog-message" class="rx-muted" style="margin-top:0;">Provide a reason for Discontinuing this prescribed drug.</p>
            <div class="rx-field" style="margin-bottom:0;">
                <label for="rx-void-reason">Void reason</label>
                <textarea id="rx-void-reason"></textarea>
            </div>
        </div>
        <div class="rx-dialog-actions">
            <button type="button" class="rx-btn" onclick="closeVoidPrescriptionDialog()">Cancel</button>
            <button type="button" class="rx-btn rx-btn-primary" onclick="submitVoidPrescriptionDialog()">Confirm</button>
        </div>
    </div>
</div>

<script type="text/javascript">
    var prescriptionDraft = [];
    var pendingVoidRequest = null;
    var allPrescriptionProducts = [];

    function byId(id) { return document.getElementById(id); }

    function normalizeText(value) {
        return value ? value.toString().trim() : '';
    }

    function normalizeFrequencyLabel(label) {
        return normalizeText(label)
            .replace(new RegExp('\\(\\s*\\d+\\s*\\)', 'g'), '')
            .replace(new RegExp('\\s+', 'g'), ' ')
            .trim()
            .toLowerCase();
    }

    function parseNumericToken(label) {
        var text = normalizeFrequencyLabel(label);
        var abbreviationCount = parseFrequencyAbbreviationCount(text);
        if (abbreviationCount) {
            return abbreviationCount;
        }
        var numberWords = {
            'once': 1, 'one': 1, 'twice': 2, 'two': 2, 'thrice': 3, 'three': 3,
            'four': 4, 'five': 5, 'six': 6, 'seven': 7, 'eight': 8, 'nine': 9,
            'ten': 10, 'eleven': 11, 'twelve': 12,
            'thirteen': 13, 'fourteen': 14, 'fifteen': 15, 'sixteen': 16,
            'seventeen': 17, 'eighteen': 18, 'nineteen': 19, 'twenty': 20
        };
        for (var key in numberWords) {
            if (numberWords.hasOwnProperty(key) && text.match(new RegExp('\\b' + key + '\\b'))) {
                return numberWords[key];
            }
        }
        var digitMatch = text.match(new RegExp('\\b(\\d+)\\b'));
        if (digitMatch) {
            return parseInt(digitMatch[1], 10);
        }
        return 0;
    }

    function parseFrequencyAbbreviationCount(text) {
        if (!text) {
            return 0;
        }
        if (text.match(new RegExp('\\b(qd|od|q24h|24 hourly)\\b'))) {
            return 1;
        }
        if (text.match(new RegExp('\\b(bid|bd|q12h|12 hourly)\\b'))) {
            return 2;
        }
        if (text.match(new RegExp('\\b(tid|tds|q8h|8 hourly)\\b'))) {
            return 3;
        }
        if (text.match(new RegExp('\\b(qid|qds|q6h|6 hourly)\\b'))) {
            return 4;
        }
        return 0;
    }

    function parseFrequency(label) {
        var text = normalizeFrequencyLabel(label);
        if (!text) {
            return null;
        }
        if (text.indexOf('per need') >= 0 || text.indexOf('as needed') >= 0 || text === 'prn') {
            return null;
        }
        if (text.indexOf('every other day') >= 0 || text.indexOf('alternate day') >= 0) {
            return { count: 0.5, period: 'day' };
        }
        if (text.match(new RegExp('\\b(qod|eod)\\b'))) {
            return { count: 0.5, period: 'day' };
        }
        var count = parseNumericToken(text);
        if (!count) {
            if (text.indexOf('daily') >= 0 || text.indexOf('per day') >= 0 || text.indexOf('a day') >= 0
                    || text.indexOf('morning') >= 0 || text.indexOf('evening') >= 0 || text.indexOf('night') >= 0) {
                count = 1;
            } else if (text.indexOf('weekly') >= 0 || text.indexOf('a week') >= 0 || text.indexOf('per week') >= 0) {
                count = 1;
            } else if (text.indexOf('monthly') >= 0 || text.indexOf('a month') >= 0 || text.indexOf('per month') >= 0) {
                count = 1;
            }
        }
        var period = 'day';
        if (text.indexOf('week') >= 0) {
            period = 'week';
        } else if (text.indexOf('month') >= 0) {
            period = 'month';
        } else if (text.indexOf('day') >= 0 || text.indexOf('daily') >= 0) {
            period = 'day';
        }
        if (!count) {
            return null;
        }
        return { count: count, period: period };
    }

    function durationToPeriods(duration, durationUnit, targetPeriod) {
        var amount = parseFloat(duration || '0');
        var unit = normalizeText(durationUnit).toLowerCase();
        if (!(amount > 0)) {
            return 0;
        }
        var days = amount;
        if (unit.indexOf('week') === 0) {
            days = amount * 7;
        } else if (unit.indexOf('month') === 0) {
            days = amount * 30;
        }
        if (targetPeriod === 'week') {
            return days / 7;
        }
        if (targetPeriod === 'month') {
            return days / 30;
        }
        return days;
    }

    function selectedFrequencyPerDay(selectElement) {
        if (!selectElement || selectElement.selectedIndex < 0) {
            return 0;
        }
        var option = selectElement.options[selectElement.selectedIndex];
        var value = option ? parseFloat(option.getAttribute('data-frequency-per-day') || '0') : 0;
        return value > 0 ? value : 0;
    }

    function calculateQuantityFromSchedule(doseValue, frequencyValue, durationValue, durationUnitValue, frequencyPerDayValue) {
        var dose = parseFloat(doseValue || '0');
        var frequencyPerDay = parseFloat(frequencyPerDayValue || '0');
        var frequency = frequencyPerDay > 0 ? { count: frequencyPerDay, period: 'day' } : parseFrequency(frequencyValue);
        if (!(dose > 0) || !frequency) {
            return '';
        }
        var periods = durationToPeriods(durationValue, durationUnitValue, frequency.period);
        if (!(periods > 0)) {
            return '';
        }
        return String(Math.ceil(dose * frequency.count * periods));
    }

    function currentDraftCalculatedQuantity() {
        return calculateQuantityFromSchedule(
                byId('rx-dose').value,
                byId('rx-frequency').value,
                byId('rx-duration').value,
                byId('rx-duration-unit').value,
                selectedFrequencyPerDay(byId('rx-frequency')));
    }

    function autoCalculateQuantity() {
        var quantityInput = byId('rx-quantity');
        if (!quantityInput) {
            return;
        }
        var calculatedQuantity = currentDraftCalculatedQuantity();
        if (!calculatedQuantity) {
            return;
        }
        quantityInput.value = calculatedQuantity;
    }

    function autoCalculateEditQuantity(lineId) {
        var doseInput = byId('rx-edit-dose-' + lineId);
        var durationInput = byId('rx-edit-duration-' + lineId);
        var durationUnitInput = byId('rx-edit-duration-unit-' + lineId);
        var frequencyInput = byId('rx-edit-frequency-' + lineId);
        var quantityInput = byId('rx-edit-quantity-' + lineId);
        if (!doseInput || !durationInput || !durationUnitInput || !frequencyInput || !quantityInput) {
            return;
        }
        var calculatedQuantity = calculateQuantityFromSchedule(
                doseInput.value,
                frequencyInput.value,
                durationInput.value,
                durationUnitInput.value,
                selectedFrequencyPerDay(frequencyInput));
        if (!calculatedQuantity) {
            return;
        }
        quantityInput.value = calculatedQuantity;
    }

    function initializePrescriptionProducts() {
        var dataList = byId('rx-product-options');
        allPrescriptionProducts = [];
        if (!dataList || !dataList.options) {
            return;
        }
        for (var i = 0; i < dataList.options.length; i++) {
            var option = dataList.options[i];
            allPrescriptionProducts.push({
                value: option.value,
                productMasterItemId: option.getAttribute('data-product-id') || '',
                label: option.getAttribute('data-label') || option.value,
                stocked: option.getAttribute('data-stocked') === 'true',
                stockTotal: option.getAttribute('data-stock-total') || ''
            });
        }
    }

    function productMatchesInput(product, inputValue) {
        var typed = normalizeText(inputValue).toLowerCase();
        if (!product || !typed) {
            return false;
        }
        return normalizeText(product.value).toLowerCase() === typed
                || normalizeText(product.label).toLowerCase() === typed;
    }

    function isPrescriptionProductVisible(product) {
        var includeUnstocked = byId('rx-include-unstocked') && byId('rx-include-unstocked').checked;
        return product && (includeUnstocked || product.stocked);
    }

    function handlePrescriptionProductInput() {
        var input = byId('rx-product-search');
        var hidden = byId('rx-product');
        if (!input || !hidden) {
            return;
        }
        hidden.value = '';
        for (var i = 0; i < allPrescriptionProducts.length; i++) {
            var product = allPrescriptionProducts[i];
            if (isPrescriptionProductVisible(product) && productMatchesInput(product, input.value)) {
                hidden.value = product.productMasterItemId;
                hidePrescriptionProductSuggestions();
                return;
            }
        }
        renderPrescriptionProductSuggestions();
    }

    function matchingPrescriptionProducts() {
        var includeUnstocked = byId('rx-include-unstocked') && byId('rx-include-unstocked').checked;
        var searchText = normalizeText(byId('rx-product-search') ? byId('rx-product-search').value : '').toLowerCase();
        var matches = [];
        for (var i = 0; i < allPrescriptionProducts.length; i++) {
            var product = allPrescriptionProducts[i];
            var label = normalizeText(product.label).toLowerCase();
            var value = normalizeText(product.value).toLowerCase();
            var matchesSearch = !searchText || label.indexOf(searchText) >= 0;
            var visible = (includeUnstocked || product.stocked) && (matchesSearch || value.indexOf(searchText) >= 0);
            if (!visible) {
                continue;
            }
            matches.push(product);
        }
        return matches;
    }

    function renderPrescriptionProductSuggestions() {
        var suggestions = byId('rx-product-suggestions');
        if (!suggestions) {
            return;
        }
        suggestions.innerHTML = '';
        if (byId('rx-product')) {
            byId('rx-product').value = '';
        }
        var matches = matchingPrescriptionProducts();
        if (!matches.length) {
            suggestions.innerHTML = '<div class="rx-suggestion-empty">No matching drugs found.</div>';
            suggestions.classList.add('is-visible');
            return;
        }
        var maxSuggestions = Math.min(matches.length, 80);
        for (var i = 0; i < maxSuggestions; i++) {
            var product = matches[i];
            var row = document.createElement('div');
            row.className = 'rx-suggestion';
            row.setAttribute('data-product-id', product.productMasterItemId);
            row.setAttribute('data-product-value', product.value);
            row.innerHTML = '<strong>' + escapeHtml(product.label) + '</strong><span>'
                    + escapeHtml(product.stocked ? ('Qty: ' + product.stockTotal) : 'Not in stock') + '</span>';
            row.onclick = function () {
                selectPrescriptionProduct(this.getAttribute('data-product-id'), this.getAttribute('data-product-value'));
            };
            suggestions.appendChild(row);
        }
        if (matches.length > maxSuggestions) {
            var more = document.createElement('div');
            more.className = 'rx-suggestion-empty';
            more.textContent = 'Keep typing to narrow ' + matches.length + ' matching drugs.';
            suggestions.appendChild(more);
        }
        suggestions.classList.add('is-visible');
    }

    function selectPrescriptionProduct(productMasterItemId, label) {
        if (byId('rx-product-search')) {
            byId('rx-product-search').value = label || '';
        }
        if (byId('rx-product')) {
            byId('rx-product').value = productMasterItemId || '';
        }
        hidePrescriptionProductSuggestions();
    }

    function hidePrescriptionProductSuggestions() {
        var suggestions = byId('rx-product-suggestions');
        if (suggestions) {
            suggestions.classList.remove('is-visible');
        }
    }

    function applyPrescriptionDrugFilter() {
        if (byId('rx-product')) {
            byId('rx-product').value = '';
        }
        renderPrescriptionProductSuggestions();
    }

    function toggleLineEdit(lineId) {
        var row = byId('rx-edit-row-' + lineId);
        if (!row) {
            return;
        }
        row.classList.toggle('is-hidden');
        if (!row.classList.contains('is-hidden')) {
            if (typeof initializeOpenmrsDateFields === 'function') {
                initializeOpenmrsDateFields(row);
            }
        }
    }

    function openVoidPrescriptionDialog(requestId, requestLineId, historyPage, itemName) {
        pendingVoidRequest = {
            requestId: requestId,
            requestLineId: requestLineId,
            historyPage: historyPage || '${ historyPage ?: 1 }'
        };
        byId('rx-void-dialog-message').textContent = 'Provide a reason for Discontinuing "' + (itemName || 'this prescribed drug') + '".';
        byId('rx-void-reason').value = '';
        byId('rx-void-dialog-backdrop').classList.remove('is-hidden');
        byId('rx-void-reason').focus();
    }

    function closeVoidPrescriptionDialog() {
        pendingVoidRequest = null;
        byId('rx-void-dialog-backdrop').classList.add('is-hidden');
    }

    function submitVoidPrescriptionDialog() {
        var reason = normalizeText(byId('rx-void-reason').value);
        if (!pendingVoidRequest || !reason) {
            byId('rx-void-reason').focus();
            return;
        }
        var form = document.createElement('form');
        form.method = 'post';
        appendHidden(form, 'action', 'voidLine');
        appendHidden(form, 'patientId', '${ patientReferenceForSubmit ?: patient?.patientId ?: patient?.uuid ?: '' }');
        appendHidden(form, 'patientUuid', '${ patientUuidForSubmit ?: patient?.uuid ?: '' }');
        appendHidden(form, 'requestId', pendingVoidRequest.requestId);
        appendHidden(form, 'requestLineId', pendingVoidRequest.requestLineId);
        appendHidden(form, 'historyPage', pendingVoidRequest.historyPage);
        appendHidden(form, 'voidReason', reason);
        document.body.appendChild(form);
        form.submit();
    }

    function appendHidden(form, name, value) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value || '';
        form.appendChild(input);
    }

    function collectDraftLine() {
        handlePrescriptionProductInput();
        var productId = byId('rx-product') ? byId('rx-product').value : '';
        if (!productId) {
            alert('Select a drug from the suggestions first.');
            return null;
        }
        var quantity = parseFloat(byId('rx-quantity').value || '0');
        if (!(quantity > 0)) {
            alert('Quantity must be greater than zero.');
            return null;
        }
        var selectedProduct = findPrescriptionProduct(productId);
        return {
            productMasterItemId: productId,
            label: selectedProduct ? selectedProduct.value : normalizeText(byId('rx-product-search').value),
            dose: normalizeText(byId('rx-dose').value),
            route: normalizeText(byId('rx-route').value),
            frequency: normalizeText(byId('rx-frequency').value),
            duration: normalizeText(byId('rx-duration').value),
            durationUnit: normalizeText(byId('rx-duration-unit').value),
            startDate: normalizeText(byId('rx-start-date-field').value),
            quantity: normalizeText(byId('rx-quantity').value),
            instructions: normalizeText(byId('rx-instructions').value)
        };
    }

    function resetDraftForm() {
        byId('rx-product').value = '';
        if (byId('rx-product-search')) {
            byId('rx-product-search').value = '';
        }
        byId('rx-route').value = '';
        byId('rx-frequency').value = '';
        byId('rx-dose').value = '';
        byId('rx-duration').value = '';
        byId('rx-duration-unit').value = '';
        byId('rx-start-date-field').value = '';
        if (byId('rx-start-date-display')) {
            byId('rx-start-date-display').value = '';
        }
        byId('rx-quantity').value = '';
        byId('rx-instructions').value = '';
        hidePrescriptionProductSuggestions();
    }

    function findPrescriptionProduct(productMasterItemId) {
        for (var i = 0; i < allPrescriptionProducts.length; i++) {
            if (allPrescriptionProducts[i].productMasterItemId === productMasterItemId) {
                return allPrescriptionProducts[i];
            }
        }
        return null;
    }

    function addToPrescription() {
        var line = collectDraftLine();
        if (!line) {
            return;
        }
        if (!line.startDate) {
            alert('Select the medication start date before adding the drug.');
            return;
        }
        prescriptionDraft.push(line);
        renderPrescriptionList();
        resetDraftForm();
    }

    function removeDraftLine(index) {
        prescriptionDraft.splice(index, 1);
        renderPrescriptionList();
    }

    function clearPrescription() {
        prescriptionDraft = [];
        renderPrescriptionList();
    }

    function renderPrescriptionList() {
        var body = byId('rx-summary-body');
        var hidden = byId('rx-hidden-fields');
        var empty = byId('rx-summary-empty');
        var table = byId('rx-summary-table');
        var totalBlock = byId('rx-total-block');
        body.innerHTML = '';
        hidden.innerHTML = '';
        var totalUnits = 0;
        for (var i = 0; i < prescriptionDraft.length; i++) {
            var line = prescriptionDraft[i];
            var qty = parseFloat(line.quantity || '0');
            totalUnits += qty || 0;
            var row = document.createElement('tr');
            row.innerHTML = '<td><strong>' + escapeHtml(line.label) + '</strong><br/><span class="rx-muted">' + escapeHtml(line.instructions || '') + '</span></td>'
                + '<td>' + escapeHtml(line.startDate) + '</td>'
                + '<td>' + escapeHtml(line.quantity) + '</td>'
                + '<td>' + escapeHtml(line.frequency) + '</td>'
                + '<td>' + escapeHtml(line.duration ? (line.duration + ' ' + line.durationUnit) : '') + '</td>'
                + '<td><button type="button" class="rx-remove-link" onclick="removeDraftLine(' + i + ')">Remove</button></td>';
            body.appendChild(row);
            hidden.appendChild(hiddenInput('productMasterItemId', line.productMasterItemId));
            hidden.appendChild(hiddenInput('dose', line.dose));
            hidden.appendChild(hiddenInput('route', line.route));
            hidden.appendChild(hiddenInput('frequency', line.frequency));
            hidden.appendChild(hiddenInput('duration', line.duration));
            hidden.appendChild(hiddenInput('durationUnit', line.durationUnit));
            hidden.appendChild(hiddenInput('startDate', line.startDate));
            hidden.appendChild(hiddenInput('quantity', line.quantity));
            hidden.appendChild(hiddenInput('instructions', line.instructions));
        }
        byId('rx-count').textContent = prescriptionDraft.length;
        if (byId('rx-header-draft-count')) {
            byId('rx-header-draft-count').textContent = prescriptionDraft.length;
        }
        byId('rx-total-drugs').textContent = prescriptionDraft.length;
        byId('rx-total-units').textContent = prescriptionDraft.length ? totalUnits : '0';
        empty.style.display = prescriptionDraft.length ? 'none' : 'block';
        table.style.display = prescriptionDraft.length ? 'table' : 'none';
        totalBlock.style.display = prescriptionDraft.length ? 'grid' : 'none';
    }

    function hiddenInput(name, value) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value || '';
        return input;
    }

    function escapeHtml(value) {
        var text = value || '';
        text = text.split('&').join('&amp;');
        text = text.split('<').join('&lt;');
        text = text.split('>').join('&gt;');
        text = text.split('"').join('&quot;');
        text = text.split("'").join('&#39;');
        return text;
    }

    function togglePrescriptionShell() {
        var shell = byId('rx-shell');
        var button = byId('rx-toggle-button');
        var hidden = shell.classList.contains('is-hidden');
        if (hidden) {
            shell.classList.remove('is-hidden');
            button.textContent = 'Hide Prescription';
        } else {
            shell.classList.add('is-hidden');
            button.textContent = 'Add Prescription';
        }
    }

    initializePrescriptionProducts();
    hidePrescriptionProductSuggestions();
    byId('rx-frequency').onchange = autoCalculateQuantity;
    byId('rx-frequency').oninput = autoCalculateQuantity;
    byId('rx-dose').oninput = autoCalculateQuantity;
    byId('rx-dose').onchange = autoCalculateQuantity;
    byId('rx-duration').oninput = autoCalculateQuantity;
    byId('rx-duration').onchange = autoCalculateQuantity;
    byId('rx-duration-unit').onchange = autoCalculateQuantity;
    byId('rx-duration-unit').oninput = autoCalculateQuantity;
    byId('rx-submit-form').onsubmit = function () {
        if (!prescriptionDraft.length) {
            alert('Add at least one drug to the prescribed items list before submitting.');
            return false;
        }
        return true;
    };
    renderPrescriptionList();
</script>
