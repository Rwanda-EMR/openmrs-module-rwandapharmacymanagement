<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.item.list.title") }</h2>
<style type="text/css">
    .item-form-shell.is-hidden { display:none; }
</style>
<p><button type="button" id="item-form-toggle-button" onclick="toggleItemForm()">Add new item</button></p>
<div class="item-form-shell is-hidden" id="item-form-shell">
<form method="post">
    <fieldset>
        <legend>New Item</legend>
        <p><label>Name</label><br/><input type="text" name="name" required/></p>
        <p>
            <label>NPC code status</label><br/>
            <label><input type="radio" name="hasNpcCode" value="true" onchange="toggleNpcCode()" checked="checked"/> Has NPC code</label>
            <label style="margin-left:12px;"><input type="radio" name="hasNpcCode" value="false" onchange="toggleNpcCode()"/> No NPC code</label>
        </p>
        <p id="npc-code-row"><label>NPC code</label><br/><input type="text" id="npc-code" name="npcCode" required="required"/></p>
        <p><label>Program</label><br/><input type="text" name="program" placeholder="e.g. CA or CP"/></p>
        <p>
            <label>Item type</label><br/>
            <select name="materialType" id="item-material-type" onchange="toggleDrugField()">
                <option value="ZCON">Consumable</option>
                <option value="ZMED">Drug</option>
            </select>
        </p>
        <p id="concept-id-row">
            <label>OpenMRS concept ID</label><br/>
            <input type="number" id="concept-id" name="conceptId"/>
            <br/><span style="color:#666;font-size:12px;">Optional for consumables. If this concept is mapped in MOH Billing, the consumable will be billed; otherwise it will still queue as paid.</span>
        </p>
        <p id="drug-id-row" style="display:none;">
            <label>OpenMRS drug ID</label><br/>
            <input type="number" id="drug-id" name="drugId"/>
            <br/><span style="color:#666;font-size:12px;">Required for drug items so prescriptions can create Drug Orders.</span>
        </p>
        <p><label>Low Stock Threshold</label><br/><input type="number" name="lowStockThreshold"/></p>
        <p><label><input type="checkbox" name="requiresBatch" value="true"/> Requires batch</label></p>
        <p><label><input type="checkbox" name="requiresExpiry" value="true"/> Requires expiry</label></p>
        <p><button type="submit">Save Item</button></p>
    </fieldset>
</form>
</div>
<table data-no-pagination="true">
    <thead><tr><th>Name</th><th>Code</th><th>Program</th><th>Threshold</th></tr></thead>
    <tbody>
    <% items.each { %>
    <tr>
        <td>${ it.name }</td>
        <td>${ it.code }</td>
        <td>${ it.program ?: '' }</td>
        <td>${ it.lowStockThreshold ?: "" }</td>
    </tr>
    <% } %>
    </tbody>
</table>
<div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-top:10px;">
    <div style="font-size:12px;color:#666;">
        <% if ((itemTotalItems ?: 0) > 0) { %>
        Showing ${ itemStartItem }-${ itemEndItem } of ${ itemTotalItems }
        <% } else { %>
        No rows
        <% } %>
    </div>
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <form method="get" style="margin:0;display:flex;align-items:center;gap:6px;">
            <label style="font-size:11px;color:#555;">Rows per page</label>
            <select name="pageSize" onchange="this.form.submit()" style="width:auto;min-width:70px;">
                <% itemPageSizes.each { option -> %>
                <option value="${ option }" ${ option == itemPageSize ? 'selected="selected"' : '' }>${ option }</option>
                <% } %>
            </select>
            <input type="hidden" name="page" value="1"/>
        </form>
        <% if (itemHasPrevious) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "item/list", [page: itemPage - 1, pageSize: itemPageSize]) }">Previous</a>
        <% } else { %>
        <span style="opacity:0.55;">Previous</span>
        <% } %>
        <span style="font-size:12px;color:#666;">Page ${ itemPage } of ${ itemTotalPages }</span>
        <% if (itemHasNext) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "item/list", [page: itemPage + 1, pageSize: itemPageSize]) }">Next</a>
        <% } else { %>
        <span style="opacity:0.55;">Next</span>
        <% } %>
    </div>
</div>
<script type="text/javascript">
    function toggleItemForm() {
        var shell = document.getElementById('item-form-shell');
        var button = document.getElementById('item-form-toggle-button');
        if (!shell || !button) {
            return;
        }
        var hidden = shell.classList.contains('is-hidden');
        if (hidden) {
            shell.classList.remove('is-hidden');
            button.textContent = 'Hide item form';
        } else {
            shell.classList.add('is-hidden');
            button.textContent = 'Add new item';
        }
    }
    function selectedHasNpcCode() {
        var selected = document.querySelector('input[name="hasNpcCode"]:checked');
        return selected && selected.value === 'true';
    }
    function toggleNpcCode() {
        var input = document.getElementById('npc-code');
        var row = document.getElementById('npc-code-row');
        var hasNpcCode = selectedHasNpcCode();
        if (row) {
            row.style.display = hasNpcCode ? '' : 'none';
        }
        if (input) {
            input.required = hasNpcCode;
            if (!hasNpcCode) {
                input.value = '';
            }
        }
    }
    function toggleDrugField() {
        var type = document.getElementById('item-material-type');
        var conceptRow = document.getElementById('concept-id-row');
        var conceptInput = document.getElementById('concept-id');
        var row = document.getElementById('drug-id-row');
        var input = document.getElementById('drug-id');
        var drug = type && type.value === 'ZMED';
        if (conceptRow) {
            conceptRow.style.display = drug ? 'none' : '';
        }
        if (conceptInput) {
            conceptInput.required = false;
            if (drug) {
                conceptInput.value = '';
            }
        }
        if (row) {
            row.style.display = drug ? '' : 'none';
        }
        if (input) {
            input.required = drug;
            if (!drug) {
                input.value = '';
            }
        }
    }
    toggleNpcCode();
    toggleDrugField();
</script>
