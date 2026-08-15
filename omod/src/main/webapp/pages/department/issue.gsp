<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>Department Stock Issue</h2>
<style type="text/css">
    .dept-card { background:#fff; border:1px solid #d8e2ea; border-radius:4px; margin-bottom:14px; overflow:hidden; }
    .dept-head { background:#f3f6f9; border-bottom:1px solid #d8e2ea; padding:10px 14px; font-weight:bold; }
    .dept-body { padding:14px; }
    .dept-table { width:100%; border-collapse:collapse; margin-bottom:10px; }
    .dept-table th, .dept-table td { border:1px solid #dbe5ec; padding:7px; font-size:12px; text-align:left; vertical-align:top; }
    .dept-table th { background:#f8fbfd; }
    .dept-btn { display:inline-block; padding:7px 12px; border:1px solid #1f5f99; background:#1f5f99; color:#fff; border-radius:4px; cursor:pointer; }
    .dept-btn-secondary { background:#fff; color:#1f5f99; }
    .dept-btn-danger { background:#fff1f2; border-color:#fecdd3; color:#be123c; }
    .dept-issue-table select { width:100%; }
    .dept-issue-table input[type=number] { width:110px; }
    .dept-muted { color:#64748b; font-size:12px; }
</style>

<div class="dept-card">
    <div class="dept-head">Select department</div>
    <div class="dept-body">
        <form method="get">
            <select name="departmentLocationId">
                <option value="">Select department store</option>
                <% departmentLocations.each { location -> %>
                <option value="${ location.pharmacyLocationId }" ${ selectedDepartment?.pharmacyLocationId == location.pharmacyLocationId ? 'selected="selected"' : '' }>${ location.name }</option>
                <% } %>
            </select>
            <button type="submit">Open</button>
        </form>
    </div>
</div>

<% if (workflowMode != 'FULL_VISIBILITY') { %>
<div class="dept-card"><div class="dept-body dept-muted">Internal issue recording is disabled because the current mode is Periodic Accountability.</div></div>
<% } else if (selectedDepartment) { %>
<div class="dept-card">
    <div class="dept-head">Record issue from ${ selectedDepartment.name }</div>
    <div class="dept-body">
        <form method="post">
            <input type="hidden" name="departmentLocationId" value="${ selectedDepartment.pharmacyLocationId }"/>
            <table class="dept-table dept-issue-table">
                <thead>
                <tr><th>Item/batch</th><th>Available</th><th>Quantity</th><th></th></tr>
                </thead>
                <tbody id="issue-lines">
                <tr class="issue-line">
                    <td>
                        <select name="itemId" required="required" onchange="syncIssueLine(this);">
                            <option value="">Select available stock</option>
                            <% stockBalances.each { balance -> %>
                            <option value="${ balance.item?.itemId }" data-batch="${ balance.batchNumber ?: '' }" data-available="${ balance.quantityOnHand ?: 0 }">${ balance.item?.name ?: '' } - ${ balance.quantityOnHand ?: 0 } ${ balance.batchNumber ? '(' + balance.batchNumber + ')' : '' }</option>
                            <% } %>
                        </select>
                        <input type="hidden" name="batchNumber"/>
                    </td>
                    <td class="line-available dept-muted">0</td>
                    <td><input type="number" step="0.01" min="0.01" name="quantity" required="required"/></td>
                    <td><button type="button" class="dept-btn dept-btn-danger" onclick="removeIssueLine(this);">Remove</button></td>
                </tr>
                </tbody>
            </table>
            <p><button type="button" class="dept-btn dept-btn-secondary" onclick="addIssueLine();">Add another item</button></p>
            <p><label>Issued to</label><br/><input type="text" name="issuedTo"/></p>
            <p><label>Reason/use</label><br/><input type="text" name="reason" style="width:60%;"/></p>
            <p><button type="submit" class="dept-btn">Record issue lines</button></p>
        </form>
    </div>
</div>

<div class="dept-card">
    <div class="dept-head">Current department stock</div>
    <div class="dept-body">
        <table class="dept-table">
            <thead><tr><th>Item</th><th>Batch</th><th>Expiry</th><th>Quantity</th></tr></thead>
            <tbody>
            <% if (!stockBalances) { %><tr><td colspan="4" class="dept-muted">No stock available.</td></tr><% } else { stockBalances.each { balance -> %>
            <tr><td>${ balance.item?.name ?: '' }</td><td>${ balance.batchNumber ?: '' }</td><td>${ balance.expiryDate ? ui.formatDatePretty(balance.expiryDate) : '' }</td><td>${ balance.quantityOnHand ?: 0 }</td></tr>
            <% } } %>
            </tbody>
        </table>
    </div>
</div>
<% } %>
<script type="text/javascript">
    function syncIssueLine(select) {
        var row = closestIssueRow(select);
        if (!row) {
            return;
        }
        var selected = select.options[select.selectedIndex];
        var batchInput = row.querySelector('input[name="batchNumber"]');
        var availableCell = row.querySelector('.line-available');
        if (batchInput) {
            batchInput.value = selected ? (selected.getAttribute('data-batch') || '') : '';
        }
        if (availableCell) {
            availableCell.innerHTML = selected ? (selected.getAttribute('data-available') || '0') : '0';
        }
    }

    function addIssueLine() {
        var body = document.getElementById('issue-lines');
        if (!body) {
            return;
        }
        var firstLine = body.querySelector('.issue-line');
        if (!firstLine) {
            return;
        }
        var newLine = firstLine.cloneNode(true);
        var select = newLine.querySelector('select[name="itemId"]');
        var quantity = newLine.querySelector('input[name="quantity"]');
        var batchInput = newLine.querySelector('input[name="batchNumber"]');
        var availableCell = newLine.querySelector('.line-available');
        if (select) {
            select.selectedIndex = 0;
        }
        if (quantity) {
            quantity.value = '';
        }
        if (batchInput) {
            batchInput.value = '';
        }
        if (availableCell) {
            availableCell.innerHTML = '0';
        }
        body.appendChild(newLine);
    }

    function removeIssueLine(button) {
        var row = closestIssueRow(button);
        var body = document.getElementById('issue-lines');
        if (!row || !body) {
            return;
        }
        if (body.querySelectorAll('.issue-line').length <= 1) {
            var select = row.querySelector('select[name="itemId"]');
            var quantity = row.querySelector('input[name="quantity"]');
            var batchInput = row.querySelector('input[name="batchNumber"]');
            var availableCell = row.querySelector('.line-available');
            if (select) {
                select.selectedIndex = 0;
            }
            if (quantity) {
                quantity.value = '';
            }
            if (batchInput) {
                batchInput.value = '';
            }
            if (availableCell) {
                availableCell.innerHTML = '0';
            }
            return;
        }
        body.removeChild(row);
    }

    function closestIssueRow(element) {
        while (element && !element.classList.contains('issue-line')) {
            element = element.parentNode;
        }
        return element;
    }
</script>
