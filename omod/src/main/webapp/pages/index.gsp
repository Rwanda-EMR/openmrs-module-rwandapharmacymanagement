<%

	    ui.decorateWith("rwandapharmacymanagement", "simplePage")
	    def queueCounts = context.loadClass("org.openmrs.module.rwandapharmacymanagement.web.support.QueueNotificationSupport").counts(session)
	    def canApprovePurchaseOrders = context.hasPrivilege("Task: rwandapharmacymanagement.approvePurchaseOrders")
	    def canSubmitPurchaseOrders = context.hasPrivilege("Task: rwandapharmacymanagement.submitSupplierOrders")
	    def canManageSettings = context.hasPrivilege("Task: rwandapharmacymanagement.manageSettings")
	    def canTransferStock = context.hasPrivilege("Task: rwandapharmacymanagement.transferStock")
	    def canApproveMainToDispensingTransfer = context.hasPrivilege("Task: rwandapharmacymanagement.approveMainToDispensingTransfer")
	    def canAdjustStock = context.hasPrivilege("Task: rwandapharmacymanagement.adjustStock")
%>
<h2>${ ui.message("rwandapharmacymanagement.dashboard.title") }</h2>
<div class="home-section-title">Quick Actions</div>
<div class="tiles">
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "inventory/mainStore") }">
        <span class="tile-icon">▣</span>
        <span class="tile-label">Main Pharmacy Stock</span>
    </a>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "pharmacyorder/request") }">
        <span class="tile-icon">↘</span>
        <span class="tile-label">Pharmacy Item Request</span>
    </a>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "department/request") }">
        <span class="tile-icon">▥</span>
        <span class="tile-label">Department Bulk Request</span>
    </a>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "department/finance") }">
        <span class="tile-icon">✓</span>
        <span class="tile-label">Department Finance Approval <% if ((queueCounts['department/finance'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['department/finance'] }</span><% } %></span>
    </a>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "department/dispense") }">
        <span class="tile-icon">⇢</span>
        <span class="tile-label">Department Pharmacy Dispense <% if ((queueCounts['department/dispense'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['department/dispense'] }</span><% } %></span>
    </a>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "pharmacyorder/queue") }">
        <span class="tile-icon">☷</span>
        <span class="tile-label">Pharmacy Item Request Queue <% if ((queueCounts['pharmacyorder/queue'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['pharmacyorder/queue'] }</span><% } %></span>
    </a>
    <% if (canTransferStock || canApproveMainToDispensingTransfer) { %>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "pharmacyorder/approve") }">
        <span class="tile-icon">✓</span>
        <span class="tile-label">Pharmacy Transfer Approval <% if ((queueCounts['pharmacyorder/approve'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['pharmacyorder/approve'] }</span><% } %></span>
    </a>
    <% } %>
    <a class="tile tile-dispense" href="${ ui.pageLink("rwandapharmacymanagement", "lab/queue") }">
        <span class="tile-icon">≣</span>
        <span class="tile-label">Lab Dispensing Queue <% if ((queueCounts['lab/queue'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['lab/queue'] }</span><% } %></span>
    </a>
    <a class="tile tile-dispense" href="${ ui.pageLink("rwandapharmacymanagement", "queue/patientQueue") }">
        <span class="tile-icon">☰</span>
        <span class="tile-label">Patient Queue <% if ((queueCounts['queue/patientQueue'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['queue/patientQueue'] }</span><% } %></span>
    </a>
    <a class="tile tile-dispense" href="${ ui.pageLink("rwandapharmacymanagement", "dispense/withOrder") }">
        <span class="tile-icon">✓</span>
        <span class="tile-label">Dispense With Order</span>
    </a>
    <a class="tile tile-supplier" href="${ ui.pageLink("rwandapharmacymanagement", "supplier/deliveryReceive") }">
        <span class="tile-icon">⇣</span>
        <span class="tile-label">Receive Delivery</span>
    </a>
    <% if (canSubmitPurchaseOrders || canApprovePurchaseOrders) { %>
    <a class="tile tile-supplier" href="${ ui.pageLink("rwandapharmacymanagement", "purchaseorder/management") }">
        <span class="tile-icon">≡</span>
        <span class="tile-label">Purchase Orders <% if ((queueCounts['purchaseorder/management'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['purchaseorder/management'] }</span><% } %></span>
    </a>
    <% } %>
    <a class="tile tile-admin" href="${ ui.pageLink("rwandapharmacymanagement", "alerts/dashboard") }">
        <span class="tile-icon">!</span>
        <span class="tile-label">Alerts Dashboard <% if ((queueCounts['alerts/dashboard'] ?: 0) > 0) { %><span class="tile-count-badge">${ queueCounts['alerts/dashboard'] }</span><% } %></span>
    </a>
    <% if (canAdjustStock) { %>
    <a class="tile tile-admin" href="${ ui.pageLink("rwandapharmacymanagement", "admin/expiryControl") }">
        <span class="tile-icon">⌁</span>
        <span class="tile-label">Batch Expiry Control</span>
    </a>
    <% } %>
    <% if (canManageSettings) { %>
    <a class="tile tile-admin" href="${ ui.pageLink("rwandapharmacymanagement", "admin/missingBillingMappings") }">
        <span class="tile-icon">?</span>
        <span class="tile-label">Missing Billing Mappings</span>
    </a>
    <% } %>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "report/inventoryMovement") }">
        <span class="tile-icon">↕</span>
        <span class="tile-label">Inventory Movement Report</span>
    </a>
    <a class="tile tile-dispense" href="${ ui.pageLink("rwandapharmacymanagement", "report/paymentExceptionReport") }">
        <span class="tile-icon">⚑</span>
        <span class="tile-label">Payment Exception Report</span>
    </a>
    <a class="tile tile-dispense" href="${ ui.pageLink("rwandapharmacymanagement", "report/notBillableItemsReport") }">
        <span class="tile-icon">○</span>
        <span class="tile-label">Not Billable Items Report</span>
    </a>
    <a class="tile tile-inventory" href="${ ui.pageLink("rwandapharmacymanagement", "report/writeOffDisposalReport") }">
        <span class="tile-icon">⌧</span>
        <span class="tile-label">Write-off and Disposal Report</span>
    </a>
</div>
