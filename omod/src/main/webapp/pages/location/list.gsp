<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.location.list.title") }</h2>
<style type="text/css">
    .location-form-shell.is-hidden { display:none; }
</style>
<p><button type="button" id="location-form-toggle-button" onclick="toggleLocationForm()">Add new location</button></p>
<div class="location-form-shell is-hidden" id="location-form-shell">
<form method="post">
    <fieldset>
        <legend>Save Pharmacy Location</legend>
        <p><label>Existing location</label><br/>
            <select id="location-editor">
                <option value="">Create new location</option>
                <% (allPharmacyLocations ?: pharmacyLocations).each { %>
                    <option value="${ it.pharmacyLocationId }">${ it.name }</option>
                <% } %>
            </select>
        </p>
        <input type="hidden" name="pharmacyLocationId" id="pharmacyLocationId"/>
        <p><label>Name</label><br/><input type="text" name="name" id="locationName" required/></p>
        <p><label>Code</label><br/><input type="text" name="code" id="locationCode" required/></p>
        <p>
            <label>OpenMRS Location Tags</label><br/>
            <select name="locationTagId" id="openmrsLocationTagId" multiple="multiple" size="5">
                <% locationTags.each { %>
                    <option value="${ it.locationTagId }">${ it.name }</option>
                <% } %>
            </select>
        </p>
        <p>
            <label>Type</label><br/>
            <select name="locationType" id="locationType">
                <option value="MAIN_PHARMACY">Main Pharmacy</option>
                <option value="DISPENSING_PHARMACY">Dispensing Pharmacy</option>
            </select>
        </p>
        <p>
            <button type="submit" name="action" value="save">Save Location</button>
            <button type="button" id="resetLocationForm">Clear</button>
        </p>
    </fieldset>
</form>
</div>
<table data-no-pagination="true">
    <thead><tr><th>Name</th><th>Code</th><th>OpenMRS Location Tags</th><th>Type</th><th>Main Store</th><th>Actions</th></tr></thead>
    <tbody>
    <% pharmacyLocations.each { %>
    <tr>
        <td>${ it.name }</td>
        <td>${ it.code }</td>
        <td>${ (it.locationTags ?: []).collect { tag -> tag.name }.join(', ') ?: (it.locationTag?.name ?: '') }</td>
        <td>${ it.locationType }</td>
        <td>${ it.mainStore }</td>
        <td>
            <button type="button"
                    class="edit-location-btn"
                    data-id="${ it.pharmacyLocationId }">Edit</button>
            <form method="post" style="display:inline-block;">
                <input type="hidden" name="pharmacyLocationId" value="${ it.pharmacyLocationId }"/>
                <% (it.locationTags ?: []).each { tag -> %><input type="hidden" name="locationTagId" value="${ tag.locationTagId }"/><% } %>
                <input type="hidden" name="name" value="${ it.name }"/>
                <input type="hidden" name="code" value="${ it.code }"/>
                <input type="hidden" name="locationType" value="${ it.locationType }"/>
                <button type="submit" name="action" value="delete">Delete</button>
            </form>
        </td>
    </tr>
    <% } %>
    </tbody>
</table>
<div style="display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;margin-top:10px;">
    <div style="font-size:12px;color:#666;">
        <% if ((locationTotalItems ?: 0) > 0) { %>
        Showing ${ locationStartItem }-${ locationEndItem } of ${ locationTotalItems }
        <% } else { %>
        No rows
        <% } %>
    </div>
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <form method="get" style="margin:0;display:flex;align-items:center;gap:6px;">
            <label style="font-size:11px;color:#555;">Rows per page</label>
            <select name="pageSize" onchange="this.form.submit()" style="width:auto;min-width:70px;">
                <% locationPageSizes.each { option -> %>
                <option value="${ option }" ${ option == locationPageSize ? 'selected="selected"' : '' }>${ option }</option>
                <% } %>
            </select>
            <input type="hidden" name="page" value="1"/>
        </form>
        <% if (locationHasPrevious) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "location/list", [page: locationPage - 1, pageSize: locationPageSize]) }">Previous</a>
        <% } else { %>
        <span style="opacity:0.55;">Previous</span>
        <% } %>
        <span style="font-size:12px;color:#666;">Page ${ locationPage } of ${ locationTotalPages }</span>
        <% if (locationHasNext) { %>
        <a href="${ ui.pageLink("rwandapharmacymanagement", "location/list", [page: locationPage + 1, pageSize: locationPageSize]) }">Next</a>
        <% } else { %>
        <span style="opacity:0.55;">Next</span>
        <% } %>
    </div>
</div>
<script type="text/javascript">
    (function() {
        var editor = document.getElementById('location-editor');
        var idField = document.getElementById('pharmacyLocationId');
        var nameField = document.getElementById('locationName');
        var codeField = document.getElementById('locationCode');
        var openmrsLocationTagField = document.getElementById('openmrsLocationTagId');
        var typeField = document.getElementById('locationType');
        var resetButton = document.getElementById('resetLocationForm');
        var pharmacyLocationMap = {
            <% (allPharmacyLocations ?: pharmacyLocations).eachWithIndex { pharmacyLocation, idx -> %>
            "${ pharmacyLocation.pharmacyLocationId }": {
                id: "${ pharmacyLocation.pharmacyLocationId }",
                name: "${ (pharmacyLocation.name ?: '').replace('\\', '\\\\').replace('"', '\\"') }",
                code: "${ (pharmacyLocation.code ?: '').replace('\\', '\\\\').replace('"', '\\"') }",
                locationTagIds: [<% def tags = pharmacyLocation.locationTags ?: [] %><% tags.eachWithIndex { tag, tagIdx -> %>"${ tag.locationTagId }"${ tagIdx + 1 < tags.size() ? ',' : '' }<% } %>],
                type: "${ pharmacyLocation.locationType ?: 'DISPENSING_PHARMACY' }"
            }${ idx + 1 < (allPharmacyLocations ?: pharmacyLocations).size() ? ',' : '' }
            <% } %>
        };

        function fillForm(record) {
            idField.value = record ? (record.id || '') : '';
            nameField.value = record ? (record.name || '') : '';
            codeField.value = record ? (record.code || '') : '';
            var selectedTags = record ? (record.locationTagIds || []) : [];
            Array.prototype.slice.call(openmrsLocationTagField.options).forEach(function(option) {
                option.selected = selectedTags.indexOf(option.value) >= 0;
            });
            typeField.value = record ? (record.type || 'DISPENSING_PHARMACY') : 'DISPENSING_PHARMACY';
            if (editor) {
                editor.value = record ? (record.id || '') : '';
            }
        }

        if (editor) {
            editor.addEventListener('change', function() {
                fillForm(pharmacyLocationMap[editor.value] || null);
            });
        }

        if (resetButton) {
            resetButton.addEventListener('click', function() {
                if (editor) {
                    editor.value = '';
                }
                fillForm(null);
            });
        }

        Array.prototype.slice.call(document.querySelectorAll('.edit-location-btn')).forEach(function(button) {
            button.addEventListener('click', function() {
                fillForm(pharmacyLocationMap[button.getAttribute('data-id')] || null);
                var shell = document.getElementById('location-form-shell');
                var toggleButton = document.getElementById('location-form-toggle-button');
                if (shell) {
                    shell.classList.remove('is-hidden');
                }
                if (toggleButton) {
                    toggleButton.textContent = 'Hide location form';
                }
            });
        });
    })();

    function toggleLocationForm() {
        var shell = document.getElementById('location-form-shell');
        var button = document.getElementById('location-form-toggle-button');
        if (!shell || !button) {
            return;
        }
        var hidden = shell.classList.contains('is-hidden');
        if (hidden) {
            shell.classList.remove('is-hidden');
            button.textContent = 'Hide location form';
        } else {
            shell.classList.add('is-hidden');
            button.textContent = 'Add new location';
        }
    }
</script>
