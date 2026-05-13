<% 
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
    def batchOptionsByItemId = [:]
    stockBalances?.each { balance ->
        def itemId = balance?.item?.itemId
        if (!itemId || !balance?.quantityOnHand || balance.quantityOnHand <= 0) {
            return
        }
        def options = batchOptionsByItemId[itemId] ?: []
        def optionKey = "${balance.batchNumber ?: ''}|${balance.expiryDate ? balance.expiryDate.format('yyyy-MM-dd') : ''}"
        if (!options.find { it.key == optionKey }) {
            options << [
                key: optionKey,
                batchNumber: balance.batchNumber ?: '',
                expiryDate: balance.expiryDate ? balance.expiryDate.format('yyyy-MM-dd') : '',
                display: (balance.batchNumber ?: 'No batch') + (balance.expiryDate ? " (${balance.expiryDate.format('dd/MMM/yyyy')})" : '')
            ]
        }
        batchOptionsByItemId[itemId] = options
    }
%>
<h2>${ ui.message("rwandapharmacymanagement.admin.reconciliation.title") }</h2>
<% if (pharmacySelectionRequired) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;">
    <form method="get" style="margin:0;">
        <div style="margin-bottom:6px;"><strong>Choose pharmacy</strong> for ${ pharmacySelectionLocationName ?: 'your current location' } before continuing.</div>
        <select name="pharmacyLocationId" required="required">
            <option value="">Select pharmacy</option>
            <% pharmacySelectionOptions.each { %><option value="${ it.pharmacyLocationId }">${ it.name }</option><% } %>
        </select>
        <button type="submit">Use pharmacy</button>
    </form>
</div>
<% } else if (!activePharmacy) { %>
<div style="margin:0 0 12px;padding:10px 12px;border:1px solid #f0ad4e;background:#fcf8e3;color:#8a6d3b;border-radius:4px;">
    No pharmacy is configured for ${ pharmacySelectionLocationName ?: 'your current location' }.
</div>
<% } %>
<% if (activePharmacy) { %><p><strong>Pharmacy:</strong> ${ activePharmacy.name }</p><% } %>
<% if (activePharmacy) { %>
<form method="post">
    <table id="adjustment-lines">
        <thead><tr><th>Item</th><th>New Quantity</th><th>Batch Number</th><th>Expiry Date</th></tr></thead>
        <tbody>
        <tr>
            <td><select name="itemId" onchange="updateBatchSuggestions(this)"><option value="">Select item</option><% items.each { %><option value="${ it.itemId }">${ it.name }</option><% } %></select></td>
            <td><input type="number" step="0.01" name="quantity"/></td>
            <td>
                <input type="text" name="batchNumber" list="batch-options-0"/>
                <datalist id="batch-options-0"></datalist>
            </td>
            <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
        </tr>
        </tbody>
    </table>
    <p><button type="button" onclick="addAdjustmentLine()">Add Adjustment Line</button></p>
    <p><label>Comments</label><br/><input type="text" name="comments" size="80"/></p>
    <p><button type="submit">Save Adjustment</button></p>
</form>
<table>
    <thead><tr><th>Item</th><th>Location</th><th>Batch</th><th>On Hand</th></tr></thead>
    <tbody>
    <% stockBalances.each { %>
    <tr>
        <td>${ it.item?.name }</td>
        <td>${ it.pharmacyLocation?.name }</td>
        <td>${ it.batchNumber ?: '' }</td>
        <td>${ it.quantityOnHand ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
<template id="adjustment-line-template">
    <tr>
        <td><select name="itemId" onchange="updateBatchSuggestions(this)"><option value="">Select item</option><% items.each { %><option value="${ it.itemId }">${ it.name }</option><% } %></select></td>
        <td><input type="number" step="0.01" name="quantity"/></td>
        <td>
            <input type="text" name="batchNumber"/>
            <datalist></datalist>
        </td>
        <td><input type="date" class="openmrs-date-field" name="expiryDate"/></td>
    </tr>
</template>
<script type="text/javascript">
    var batchOptionsByItemId = {
        <% batchOptionsByItemId.eachWithIndex { entry, index -> %>
        "${entry.key}": [
            <% entry.value.eachWithIndex { option, optionIndex -> %>
            { batchNumber: "${(option.batchNumber ?: '').replace('\\', '\\\\').replace('"', '\\"')}", expiryDate: "${option.expiryDate ?: ''}", display: "${(option.display ?: '').replace('\\', '\\\\').replace('"', '\\"')}" }${optionIndex < entry.value.size() - 1 ? ',' : ''}
            <% } %>
        ]${index < batchOptionsByItemId.size() - 1 ? ',' : ''}
        <% } %>
    };

    function updateBatchSuggestions(selectElement) {
        if (!selectElement) {
            return;
        }
        var row = selectElement.closest('tr');
        if (!row) {
            return;
        }
        var batchInput = row.querySelector('input[name="batchNumber"]');
        var expiryInput = row.querySelector('input[name="expiryDate"]');
        var datalist = row.querySelector('datalist');
        if (!batchInput || !datalist) {
            return;
        }
        if (!datalist.id) {
            datalist.id = 'batch-options-' + Math.random().toString(36).slice(2);
        }
        batchInput.setAttribute('list', datalist.id);
        datalist.innerHTML = '';
        batchInput.value = '';
        if (expiryInput) {
            expiryInput.value = '';
        }
        var options = batchOptionsByItemId[selectElement.value] || [];
        for (var i = 0; i < options.length; i++) {
            var option = document.createElement('option');
            option.value = options[i].batchNumber;
            option.label = options[i].display;
            option.setAttribute('data-expiry-date', options[i].expiryDate || '');
            datalist.appendChild(option);
        }
        batchInput.onchange = function() {
            var selected = null;
            for (var j = 0; j < options.length; j++) {
                if (options[j].batchNumber === batchInput.value) {
                    selected = options[j];
                    break;
                }
            }
            if (expiryInput) {
                expiryInput.value = selected && selected.expiryDate ? selected.expiryDate : '';
            }
        };
    }

    function addAdjustmentLine() {
        var tbody = document.getElementById('adjustment-lines').getElementsByTagName('tbody')[0];
        var template = document.getElementById('adjustment-line-template');
        if (template && template.content && template.content.firstElementChild) {
            var clone = template.content.firstElementChild.cloneNode(true);
            tbody.appendChild(clone);
            var select = clone.querySelector('select[name="itemId"]');
            if (select) {
                updateBatchSuggestions(select);
            }
            if (typeof initializeOpenmrsDateFields === 'function') {
                initializeOpenmrsDateFields(clone);
            }
        }
    }

    (function initializeBatchSuggestions() {
        var itemSelects = document.querySelectorAll('select[name="itemId"]');
        for (var i = 0; i < itemSelects.length; i++) {
            updateBatchSuggestions(itemSelects[i]);
        }
    })();
</script>
<% } %>
