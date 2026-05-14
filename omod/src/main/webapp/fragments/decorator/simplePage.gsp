<%
    def currentTitle = config.pageTitle ?: "Rwanda Pharmacy"
    ui.includeFragment("appui", "standardEmrIncludes", [ includeBootstrap: true ])
    ui.includeFragment("rwandapharmacymanagement", "common/openmrsCalendar")
    ui.includeJavascript("appui", "popper.min.js")
    ui.includeJavascript("appui", "bootstrap.min.js")
    ui.includeCss("appui", "bootstrap.min.css")
    ui.includeCss("appui", "header.css")
	    def queueCounts = context.loadClass("org.openmrs.module.rwandapharmacymanagement.web.support.QueueNotificationSupport").counts(session)
	    def monitoredCountLinks = ["queue/patientQueue", "pharmacyorder/queue", "lab/queue", "pharmacyorder/approve", "purchaseorder/management", "alerts/dashboard"]
	    def hasAnyPrivilege = { required ->
	        if (!required) {
	            return true
	        }
	        if (required instanceof Collection) {
	            return required.any { context.hasPrivilege(it) }
	        }
	        return context.hasPrivilege(required)
	    }
	    def navSections = [
            [title: "Overview", links: [
                    [path: "index", label: "Dashboard"]
            ]],
            [title: "Dispensing", links: [
                    [path: "queue/patientQueue", label: "Patient queue"],
                    [path: "dispense/history", label: "Dispensing history"],
                    [path: "pharmacyorder/queue", label: "Pharmacy item request queue"],
                    [path: "lab/queue", label: "Lab dispensing queue"],
                    [path: "lab/history", label: "Lab dispensing history"]
            ]],
            [title: "Stock", links: [
                    [path: "inventory/mainStore", label: "Main pharmacy"],
                    [path: "inventory/dispensingStore", label: "Dispensing pharmacies"],
                    [path: "inventory/receiptHistory", label: "Receipt history"],
                    [path: "inventory/valuation", label: "Stock valuation"],
	                    [path: "pharmacyorder/approve", label: "Pharmacy transfer approval"],
		                    [path: "transfer/mainToDispensing", label: "Main to dispensing"],
	                    [path: "transfer/returnToMain", label: "Return to main pharmacy"],
                    [path: "transfer/interPharmacy", label: "Dispensing to dispensing"],
                    [path: "admin/reconciliation", label: "Reconciliation"]
            ]],
            [title: "Supplier", links: [
                    [path: "supplier/settings", label: "Configuration"],
	                    [path: "purchaseorder/management", label: "Purchase orders", privilegeAny: ["Task: rwandapharmacymanagement.submitSupplierOrders", "Task: rwandapharmacymanagement.approvePurchaseOrders"]],
                    [path: "supplier/productSync", label: "Product sync"],
                    [path: "supplier/priceHistory", label: "Price history"],
                    [path: "supplier/deliveryReceive", label: "Receive delivery"]
            ]],
            [title: "Master Data", links: [
                    [path: "item/list", label: "Items"],
                    [path: "location/list", label: "Locations"],
                    [path: "admin/settings", label: "Settings"]
            ]],
            [title: "Control", links: [
                    [path: "alerts/dashboard", label: "Alerts"],
                    [path: "admin/linkBill", label: "Link prescription bills"],
                    [path: "admin/missingBillingMappings", label: "Missing billing mappings"],
                    [path: "safety/rules", label: "Safety rules"],
                    [path: "integration/logs", label: "Integration logs"]
            ]],
            [title: "Reports", links: [
                    [path: "report/pharmacyStock", label: "Individual pharmacy report"],
                    [path: "report/overallStock", label: "Overall hospital stock report"],
                    [path: "report/inventoryMovement", label: "Inventory movement report"],
                    [path: "report/itemMovement", label: "Item movement report"]
            ]]
    ]
%>
<!DOCTYPE html>
<html>
<head>
    <title>${ config.pageTitle ?: ui.message("rwandapharmacymanagement.title") }</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    ${ ui.resourceLinks() }
    <script src="/${ui.contextPath()}/csrfguard" type="text/javascript"></script>
    <style>
        :root {
            --rpm-nav: #222222;
            --rpm-nav-accent: #007f87;
            --rpm-nav-text: #9d9d9d;
            --rpm-nav-active: #ffffff;
            --rpm-bg: #f5f5f5;
            --rpm-card: #ffffff;
            --rpm-border: #dddddd;
            --rpm-text: #333333;
            --rpm-muted: #777777;
            --rpm-link: #337ab7;
            --rpm-link-dark: #23527c;
            --rpm-left-title: #aaaaaa;
        }
        * {
            box-sizing: border-box;
        }
        body {
            margin: 0;
            font-family: Arial, Helvetica, sans-serif;
            font-size: 13px;
            background: var(--rpm-bg);
            color: var(--rpm-text);
        }
        a {
            color: var(--rpm-link);
            text-decoration: none;
        }
        a:hover {
            text-decoration: underline;
        }
        .home-body {
            padding: 18px;
        }
        .breadcrumb-bar {
            background: #fff;
            border-bottom: 1px solid var(--rpm-border);
            padding: 6px 13px;
            font-size: 12px;
            color: var(--rpm-muted);
        }
        .module-wrap {
            display: flex;
        }
        .module-left {
            width: 210px;
            flex-shrink: 0;
            background: #fff;
            border-right: 1px solid var(--rpm-border);
        }
        .module-left-hdr {
            background: var(--rpm-nav-accent);
            color: #fff;
            padding: 8px 11px;
            font-size: 12px;
            font-weight: bold;
        }
        .lnav-section {
            border-bottom: 1px solid #eeeeee;
        }
        .lnav-section-toggle {
            width: 100%;
            border: 0;
            background: #fff;
            padding: 7px 11px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            cursor: pointer;
            text-align: left;
        }
        .lnav-section-toggle:hover {
            background: #f5f5f5;
        }
        .lnav-section-title {
            font-size: 10px;
            color: var(--rpm-left-title);
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        .lnav-section-caret {
            color: var(--rpm-left-title);
            font-size: 11px;
            transition: transform 0.2s ease;
        }
        .lnav-section-links {
            display: none;
            padding: 0 0 3px;
        }
        .lnav-section.is-open .lnav-section-links {
            display: block;
        }
        .lnav-section.is-open .lnav-section-caret {
            transform: rotate(90deg);
        }
        .lnav-link {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            padding: 5px 11px 5px 19px;
            color: var(--rpm-link);
            font-size: 12px;
        }
        .nav-count-badge {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 18px;
            height: 18px;
            padding: 0 6px;
            border-radius: 9px;
            background: #d9534f;
            color: #ffffff;
            font-size: 10px;
            font-weight: bold;
            line-height: 1;
            flex-shrink: 0;
        }
        .lnav-link:hover {
            background: #f5f5f5;
            text-decoration: none;
        }
        .lnav-link.active {
            background: #e8f4f8;
            border-left: 3px solid var(--rpm-link);
            padding-left: 16px;
            color: var(--rpm-link-dark);
            font-weight: bold;
        }
        .module-main {
            flex: 1;
            min-width: 0;
            background: var(--rpm-bg);
        }
        .content-area {
            padding: 12px;
        }
        .simple-page-content h2 {
            margin: 0 0 12px 0;
            font-size: 20px;
            font-weight: normal;
        }
        .simple-page-content h3 {
            margin: 14px 0 8px 0;
            font-size: 15px;
        }
        .simple-page-content h4 {
            margin: 10px 0 6px 0;
            font-size: 13px;
        }
        .rpm-searchable {
            position: relative;
        }
        .rpm-searchable-input {
            width: 100%;
            max-width: 100%;
            box-sizing: border-box;
            border: 1px solid #cccccc;
            border-radius: 3px;
            padding: 5px 8px;
            font-size: 12px;
            color: #333333;
            background: #fff;
            font-family: inherit;
        }
        .rpm-searchable-input.is-invalid {
            border-color: #d9534f;
            background: #fff6f6;
        }
        .rpm-searchable-list {
            position: fixed;
            display: none;
            max-height: 240px;
            overflow-y: auto;
            background: #fff;
            border: 1px solid #cfd9e2;
            border-radius: 3px;
            box-shadow: 0 4px 12px rgba(16, 34, 49, 0.12);
            z-index: 2500;
        }
        .rpm-searchable-list.active {
            display: block;
        }
        .rpm-searchable-option {
            padding: 7px 8px;
            border-bottom: 1px solid #edf1f4;
            cursor: pointer;
            font-size: 12px;
            background: #fff;
        }
        .rpm-searchable-option:last-child {
            border-bottom: none;
        }
        .rpm-searchable-option:hover,
        .rpm-searchable-option.is-active {
            background: #eef6fc;
        }
        .rpm-searchable-empty {
            padding: 7px 8px;
            font-size: 12px;
            color: #7b8793;
        }
        .home-section-title {
            font-size: 13px;
            color: #666666;
            margin-bottom: 12px;
            padding-bottom: 5px;
            border-bottom: 1px solid var(--rpm-border);
        }
        .tiles {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 10px;
            margin-bottom: 18px;
        }
        .tile {
            padding: 14px 14px 12px;
            text-align: left;
            color: #1f2d3d;
            border-radius: 3px;
            display: block;
            border: 1px solid #d8e1e8;
            background: #ffffff;
            box-shadow: 0 1px 2px rgba(15, 35, 55, 0.06);
            min-height: 108px;
        }
        .tile:hover {
            text-decoration: none;
            box-shadow: 0 4px 12px rgba(15, 35, 55, 0.12);
            transform: translateY(-1px);
        }
        .tile-icon {
            width: 38px;
            height: 38px;
            border-radius: 10px;
            font-size: 20px;
            margin-bottom: 10px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
        }
        .tile-label {
            font-size: 14px;
            font-weight: bold;
            display: block;
            line-height: 1.25;
            color: #1d2b36;
        }
        .tile-count-badge {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 22px;
            height: 22px;
            padding: 0 7px;
            margin-left: 6px;
            border-radius: 11px;
            background: #d9534f;
            color: #ffffff;
            font-size: 11px;
            font-weight: bold;
            vertical-align: middle;
        }
        .tile-meta {
            display: block;
            margin-top: 4px;
            font-size: 11px;
            color: #61707f;
            line-height: 1.35;
        }
        .tile-inventory {
            border-top: 3px solid #2f6fad;
            background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
        }
        .tile-inventory .tile-icon {
            background: #dcecff;
            color: #2f6fad;
        }
        .tile-dispense {
            border-top: 3px solid #2b8f67;
            background: linear-gradient(180deg, #f5fcf8 0%, #ffffff 100%);
        }
        .tile-dispense .tile-icon {
            background: #daf3e7;
            color: #2b8f67;
        }
        .tile-supplier {
            border-top: 3px solid #0f8894;
            background: linear-gradient(180deg, #f4fbfc 0%, #ffffff 100%);
        }
        .tile-supplier .tile-icon {
            background: #d7f0f3;
            color: #0f8894;
        }
        .tile-safety {
            border-top: 3px solid #c98a11;
            background: linear-gradient(180deg, #fffaf1 0%, #ffffff 100%);
        }
        .tile-safety .tile-icon {
            background: #ffedc8;
            color: #a46f08;
        }
        .tile-admin {
            border-top: 3px solid #8a486f;
            background: linear-gradient(180deg, #fcf7fb 0%, #ffffff 100%);
        }
        .tile-admin .tile-icon {
            background: #f1dceb;
            color: #8a486f;
        }
        .quick-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 12px;
        }
        .panel {
            background: #fff;
            border: 1px solid var(--rpm-border);
            border-radius: 3px;
            margin-bottom: 12px;
            overflow: hidden;
        }
        .panel-hdr {
            padding: 7px 11px;
            background: #f5f5f5;
            border-bottom: 1px solid var(--rpm-border);
            font-size: 12px;
            font-weight: bold;
        }
        .panel-body {
            padding: 12px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            background: #fff;
            font-size: 12px;
        }
        .rpm-table-pagination {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;
            margin: 8px 0 14px;
            font-size: 12px;
            color: #666;
        }
        .rpm-table-pagination-actions {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;
        }
        .rpm-table-pagination button {
            padding: 4px 8px;
            border: 1px solid #ccc;
            border-radius: 3px;
            background: #fff;
            color: #333;
            cursor: pointer;
            font-size: 11px;
        }
        .rpm-table-pagination button[disabled] {
            opacity: 0.5;
            cursor: default;
        }
        .rpm-table-pagination select {
            width: auto;
            min-width: 64px;
            padding: 4px 6px;
            font-size: 11px;
        }
        .rpm-page-hidden {
            display: none;
        }
        th {
            background: #f5f5f5;
            padding: 6px 9px;
            text-align: left;
            border: 1px solid var(--rpm-border);
            font-weight: bold;
            color: #555555;
            font-size: 11px;
            white-space: nowrap;
        }
        td {
            padding: 6px 9px;
            border: 1px solid var(--rpm-border);
            vertical-align: middle;
        }
        tr:nth-child(even) td {
            background: #f9f9f9;
        }
        .simple-page-content input[type="text"],
        .simple-page-content input[type="number"],
        .simple-page-content input[type="password"],
        .simple-page-content select,
        .simple-page-content textarea {
            width: 100%;
            max-width: 100%;
            box-sizing: border-box;
            border: 1px solid #cccccc;
            border-radius: 3px;
            padding: 5px 8px;
            font-size: 12px;
            color: #333333;
            background: #fff;
            font-family: inherit;
        }
        .simple-page-content button,
        .simple-page-content input[type="submit"] {
            display: inline-flex;
            align-items: center;
            gap: 3px;
            padding: 4px 9px;
            border-radius: 3px;
            border: 1px solid #2e6da4;
            background: #337ab7;
            color: #fff;
            font-size: 11px;
            cursor: pointer;
            font-family: inherit;
        }
        .simple-page-content button:hover,
        .simple-page-content input[type="submit"]:hover {
            background: #286090;
        }
        .simple-page-content fieldset {
            border: 1px solid var(--rpm-border);
            border-radius: 3px;
            padding: 10px 12px;
            background: #fff;
        }
        .simple-page-content legend {
            padding: 0 4px;
            font-size: 11px;
            font-weight: bold;
            color: #555555;
        }
        .simple-page-content p {
            margin: 0 0 10px 0;
        }
        @media (max-width: 768px) {
            .module-wrap {
                flex-direction: column;
            }
            .module-left {
                width: 100%;
                border-right: 0;
                border-bottom: 1px solid var(--rpm-border);
            }
        }
</style>
</head>
<body>
<div class="breadcrumb-bar">
    <a href="/${ contextPath }/index.htm">Home</a> &rsaquo;
    <a href="${ ui.pageLink("rwandapharmacymanagement", "index") }">Rwanda Pharmacy</a> &rsaquo;
    <span style="color:#555555">${ currentTitle }</span>
</div>
<div class="module-wrap">
    <div class="module-left">
        <div class="module-left-hdr">Rwanda Pharmacy</div>
	        <% navSections.each { section -> def visibleLinks = section.links.findAll { link -> hasAnyPrivilege(link.privilegeAny ?: link.privilege) }; if (visibleLinks) { %>
        <div class="lnav-section">
            <button type="button" class="lnav-section-toggle" aria-expanded="false">
                <span class="lnav-section-title">${ section.title }</span>
                <span class="lnav-section-caret">&#9656;</span>
            </button>
            <div class="lnav-section-links">
	                <% visibleLinks.each { link -> %>
                <%
                    def linkCount = queueCounts[link.path] ?: 0
                %>
                <a class="lnav-link" href="${ ui.pageLink("rwandapharmacymanagement", link.path) }">
                    <span>${ link.label }</span>
                    <% if (monitoredCountLinks.contains(link.path) && linkCount > 0) { %><span class="nav-count-badge">${ linkCount }</span><% } %>
                </a>
                <% } %>
            </div>
	        </div>
	        <% } %>
	        <% } %>
	    </div>
    <div class="module-main">
        <div class="content-area">
            <div class="simple-page-content">
                ${ config.content }
            </div>
        </div>
    </div>
</div>
<script type="text/javascript">
    (function() {
        function normalizePath(path) {
            if (!path) {
                return '/';
            }
            var normalized = path;
            while (normalized.length > 1 && normalized.charAt(normalized.length - 1) === '/') {
                normalized = normalized.substring(0, normalized.length - 1);
            }
            return normalized || '/';
        }

        function initializeSidebarNavigation() {
            var currentPath = normalizePath(window.location.pathname || '');
            var sections = document.querySelectorAll('.lnav-section');

            function setSectionOpen(section, isOpen) {
                if (!section) {
                    return;
                }
                var toggle = section.querySelector('.lnav-section-toggle');
                var links = section.querySelector('.lnav-section-links');
                section.classList.toggle('is-open', isOpen);
                if (toggle) {
                    toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
                }
                if (links) {
                    links.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
                }
            }

            function closeOtherSections(openSection) {
                for (var sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                    if (sections[sectionIndex] !== openSection) {
                        setSectionOpen(sections[sectionIndex], false);
                    }
                }
            }

            for (var i = 0; i < sections.length; i++) {
                (function(section) {
                    var toggle = section.querySelector('.lnav-section-toggle');
                    var linkNodes = section.querySelectorAll('.lnav-link');
                    var hasActiveLink = false;

                    for (var index = 0; index < linkNodes.length; index++) {
                        var linkPath = normalizePath(linkNodes[index].pathname || '');
                        var isActive = currentPath === linkPath;
                        linkNodes[index].classList.toggle('active', isActive);
                        if (isActive) {
                            hasActiveLink = true;
                        }
                    }

                    setSectionOpen(section, hasActiveLink);
                    if (toggle) {
                        toggle.addEventListener('click', function() {
                            var shouldOpen = !section.classList.contains('is-open');
                            closeOtherSections(section);
                            setSectionOpen(section, shouldOpen);
                        });
                    }
                })(sections[i]);
            }
        }

        initializeSidebarNavigation();

        function initializeTablePagination() {
            var tables = document.querySelectorAll('.simple-page-content table');
            for (var tableIndex = 0; tableIndex < tables.length; tableIndex++) {
                (function(table) {
                    if (!table || table.getAttribute('data-rpm-pagination-ready') === 'true' || table.getAttribute('data-no-pagination') === 'true') {
                        return;
                    }
                    var tbody = table.tBodies && table.tBodies.length ? table.tBodies[0] : null;
                    if (!tbody) {
                        return;
                    }

                    var rows = Array.prototype.slice.call(tbody.rows || []);
                    if (rows.length <= 10) {
                        return;
                    }

                    table.setAttribute('data-rpm-pagination-ready', 'true');

                    var controls = document.createElement('div');
                    controls.className = 'rpm-table-pagination';
                    controls.innerHTML =
                        '<div class="rpm-table-pagination-summary"></div>' +
                        '<div class="rpm-table-pagination-actions">' +
                        '<label>Rows per page ' +
                        '<select class="rpm-table-pagination-size">' +
                        '<option value="10">10</option>' +
                        '<option value="20">20</option>' +
                        '<option value="50">50</option>' +
                        '</select>' +
                        '</label>' +
                        '<button type="button" class="rpm-table-pagination-prev">Previous</button>' +
                        '<span class="rpm-table-pagination-page"></span>' +
                        '<button type="button" class="rpm-table-pagination-next">Next</button>' +
                        '</div>';
                    if (table.nextSibling) {
                        table.parentNode.insertBefore(controls, table.nextSibling);
                    } else {
                        table.parentNode.appendChild(controls);
                    }

                    var summary = controls.querySelector('.rpm-table-pagination-summary');
                    var pageLabel = controls.querySelector('.rpm-table-pagination-page');
                    var pageSizeSelect = controls.querySelector('.rpm-table-pagination-size');
                    var previousButton = controls.querySelector('.rpm-table-pagination-prev');
                    var nextButton = controls.querySelector('.rpm-table-pagination-next');
                    var currentPage = 1;
                    var pageSize = parseInt(pageSizeSelect.value, 10);

                    function visibleRows() {
                        return rows.filter(function(row) {
                            return row.style.display !== 'none';
                        });
                    }

                    function renderPagination() {
                        rows.forEach(function(row) {
                            row.classList.remove('rpm-page-hidden');
                        });

                        var activeRows = visibleRows();
                        var totalRows = activeRows.length;
                        var totalPages = Math.max(1, Math.ceil(totalRows / pageSize));
                        if (currentPage > totalPages) {
                            currentPage = totalPages;
                        }
                        var startIndex = totalRows === 0 ? 0 : (currentPage - 1) * pageSize;
                        var endIndex = Math.min(startIndex + pageSize, totalRows);

                        for (var rowIndex = 0; rowIndex < activeRows.length; rowIndex++) {
                            if (rowIndex < startIndex || rowIndex >= endIndex) {
                                activeRows[rowIndex].classList.add('rpm-page-hidden');
                            }
                        }

                        if (summary) {
                            if (!totalRows) {
                                summary.textContent = 'No rows';
                            } else {
                                summary.textContent = 'Showing ' + (startIndex + 1) + '-' + endIndex + ' of ' + totalRows;
                            }
                        }
                        if (pageLabel) {
                            pageLabel.textContent = 'Page ' + currentPage + ' of ' + totalPages;
                        }
                        if (previousButton) {
                            previousButton.disabled = currentPage <= 1;
                        }
                        if (nextButton) {
                            nextButton.disabled = currentPage >= totalPages;
                        }
                        controls.style.display = totalRows > pageSize ? 'flex' : (totalRows > 0 ? 'flex' : 'none');
                    }

                    if (pageSizeSelect) {
                        pageSizeSelect.addEventListener('change', function() {
                            pageSize = parseInt(pageSizeSelect.value, 10) || 10;
                            currentPage = 1;
                            renderPagination();
                        });
                    }
                    if (previousButton) {
                        previousButton.addEventListener('click', function() {
                            if (currentPage > 1) {
                                currentPage--;
                                renderPagination();
                            }
                        });
                    }
                    if (nextButton) {
                        nextButton.addEventListener('click', function() {
                            var totalPages = Math.max(1, Math.ceil(visibleRows().length / pageSize));
                            if (currentPage < totalPages) {
                                currentPage++;
                                renderPagination();
                            }
                        });
                    }

                    if (window.MutationObserver) {
                        var observer = new MutationObserver(function() {
                            rows = Array.prototype.slice.call(tbody.rows || []);
                            renderPagination();
                        });
                        observer.observe(tbody, { childList: true, subtree: true, attributes: true, attributeFilter: ['style'] });
                    }

                    renderPagination();
                })(tables[tableIndex]);
            }
        }

        initializeTablePagination();

        function shouldEnhance(select) {
            if (!select || select.multiple || select.getAttribute('data-no-searchable') === 'true') {
                return false;
            }
            if (select.getAttribute('data-searchable-ready') === 'true') {
                return false;
            }
            var explicit = select.getAttribute('data-searchable');
            if (explicit === 'true') {
                return true;
            }
            if (explicit === 'false') {
                return false;
            }
            var optionCount = 0;
            for (var i = 0; i < select.options.length; i++) {
                if ((select.options[i].text || '').trim()) {
                    optionCount++;
                }
            }
            return optionCount >= 8;
        }

        function createSearchable(select) {
            select.setAttribute('data-searchable-ready', 'true');

            var wrapper = document.createElement('div');
            wrapper.className = 'rpm-searchable';

            var input = document.createElement('input');
            input.type = 'text';
            input.className = 'rpm-searchable-input';
            input.autocomplete = 'off';
            input.placeholder = select.getAttribute('data-search-placeholder') || 'Type to search';

            var list = document.createElement('div');
            list.className = 'rpm-searchable-list';

            select.style.display = 'none';
            select.parentNode.insertBefore(wrapper, select);
            wrapper.appendChild(input);
            wrapper.appendChild(select);
            wrapper.appendChild(list);

            syncInput();

            input.addEventListener('focus', function() {
                renderList(input.value || '');
            });

            input.addEventListener('input', function() {
                select.value = '';
                input.classList.toggle('is-invalid', !!select.required && !!input.value.trim());
                renderList(input.value || '');
            });

            input.addEventListener('keydown', function(event) {
                var options = list.querySelectorAll('.rpm-searchable-option');
                if (!options.length || !list.classList.contains('active')) {
                    return;
                }
                var activeIndex = -1;
                for (var i = 0; i < options.length; i++) {
                    if (options[i].classList.contains('is-active')) {
                        activeIndex = i;
                        break;
                    }
                }
                if (event.key === 'ArrowDown') {
                    event.preventDefault();
                    activeIndex = activeIndex < options.length - 1 ? activeIndex + 1 : 0;
                    setActive(options, activeIndex);
                } else if (event.key === 'ArrowUp') {
                    event.preventDefault();
                    activeIndex = activeIndex > 0 ? activeIndex - 1 : options.length - 1;
                    setActive(options, activeIndex);
                } else if (event.key === 'Enter') {
                    if (activeIndex >= 0) {
                        event.preventDefault();
                        chooseOption(options[activeIndex]);
                    }
                } else if (event.key === 'Escape') {
                    list.classList.remove('active');
                }
            });

            input.addEventListener('blur', function() {
                window.setTimeout(function() {
                    list.classList.remove('active');
                    if (select.value) {
                        input.classList.remove('is-invalid');
                        return;
                    }
                    if (!input.value.trim()) {
                        input.classList.remove('is-invalid');
                        return;
                    }
                    syncInput();
                    input.classList.toggle('is-invalid', !!select.required && !select.value);
                }, 150);
            });

            select.addEventListener('change', syncInput);
            window.addEventListener('resize', positionList);
            window.addEventListener('scroll', positionList, true);

            function syncInput() {
                var selected = select.options[select.selectedIndex];
                input.value = selected && selected.value ? selected.text : '';
                input.classList.remove('is-invalid');
            }

            function renderList(query) {
                list.innerHTML = '';
                var normalized = (query || '').trim().toLowerCase();
                var matches = [];
                for (var i = 0; i < select.options.length; i++) {
                    var option = select.options[i];
                    var text = (option.text || '').trim();
                    if (!text) {
                        continue;
                    }
                    if (!normalized || text.toLowerCase().indexOf(normalized) !== -1) {
                        matches.push(option);
                    }
                }
                if (!matches.length) {
                    list.innerHTML = '<div class="rpm-searchable-empty">No matching options found.</div>';
                    positionList();
                    list.classList.add('active');
                    return;
                }
                for (var j = 0; j < matches.length; j++) {
                    var optionDiv = document.createElement('div');
                    optionDiv.className = 'rpm-searchable-option';
                    optionDiv.textContent = matches[j].text;
                    optionDiv.setAttribute('data-value', matches[j].value);
                    optionDiv.onmousedown = function(event) {
                        event.preventDefault();
                        chooseOption(this);
                    };
                    list.appendChild(optionDiv);
                }
                positionList();
                list.classList.add('active');
            }

            function chooseOption(optionDiv) {
                var value = optionDiv.getAttribute('data-value') || '';
                select.value = value;
                syncInput();
                list.classList.remove('active');
                var changeEvent;
                if (typeof Event === 'function') {
                    changeEvent = new Event('change', { bubbles: true });
                } else {
                    changeEvent = document.createEvent('Event');
                    changeEvent.initEvent('change', true, true);
                }
                select.dispatchEvent(changeEvent);
            }

            function positionList() {
                var rect = input.getBoundingClientRect();
                list.style.left = rect.left + 'px';
                list.style.top = (rect.bottom + 2) + 'px';
                list.style.width = rect.width + 'px';
            }

            function setActive(options, activeIndex) {
                for (var i = 0; i < options.length; i++) {
                    options[i].classList.toggle('is-active', i === activeIndex);
                }
            }
        }

        function initSearchableSelects(root) {
            var scope = root || document;
            var selects = scope.querySelectorAll ? scope.querySelectorAll('select') : [];
            for (var i = 0; i < selects.length; i++) {
                if (shouldEnhance(selects[i])) {
                    createSearchable(selects[i]);
                }
            }
        }

        document.addEventListener('DOMContentLoaded', function() {
            initSearchableSelects(document);
            if (window.MutationObserver) {
                var observer = new MutationObserver(function(mutations) {
                    for (var i = 0; i < mutations.length; i++) {
                        for (var j = 0; j < mutations[i].addedNodes.length; j++) {
                            var node = mutations[i].addedNodes[j];
                            if (node && node.nodeType === 1) {
                                initSearchableSelects(node);
                            }
                        }
                    }
                });
                observer.observe(document.body, { childList: true, subtree: true });
            }
        });
    })();
</script>
</body>
</html>
