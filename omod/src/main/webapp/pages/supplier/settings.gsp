<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.supplier.settings.title") }</h2>
<h3>Legacy RMS Defaults</h3>
<form method="post">
    <p><label>Base URL</label><br/><input type="text" name="baseUrl" value="${ supplierSetting?.baseUrl ?: '' }" size="80"/></p>
    <p><label>Username</label><br/><input type="text" name="username" value="${ supplierSetting?.username ?: '' }"/></p>
    <p><label>Password</label><br/><input type="password" name="password" value="${ supplierSetting?.password ?: '' }"/></p>
    <p><label>Facility ID / FOSAID</label><br/><input type="text" name="facilityId" value="${ facilityId ?: '1660' }"/></p>
    <p><label>Hospital Name</label><br/><input type="text" name="hospitalName" value="${ hospitalName ?: '' }" size="80"/></p>
    <p><label>Connection Timeout</label><br/><input type="number" name="connectionTimeout" value="${ supplierSetting?.connectionTimeout ?: 10000 }"/></p>
    <p><label>Read Timeout</label><br/><input type="number" name="readTimeout" value="${ supplierSetting?.readTimeout ?: 30000 }"/></p>
    <p><label><input type="checkbox" name="integrationEnabled" value="true" ${ supplierSetting?.integrationEnabled ? 'checked="checked"' : '' }/> Enable integration</label></p>
    <p>
        <button type="submit" name="action" value="save">Save Settings</button>
        <button type="submit" name="action" value="authenticate">Save And Authenticate</button>
    </p>
</form>

<h3>Suppliers</h3>
<table>
    <thead><tr><th>Name</th><th>Code</th><th>Email</th><th>API</th><th>Actions</th></tr></thead>
    <tbody>
    <% suppliers.each { supplier -> %>
    <tr>
        <td>${ supplier.name ?: '' }</td>
        <td>${ supplier.code ?: '' }</td>
        <td>${ supplier.emailAddress ?: '' }</td>
        <td>${ supplier.apiEnabled ? 'Enabled' : 'Disabled' }</td>
        <td><a href="${ ui.pageLink('rwandapharmacymanagement', 'supplier/settings', [supplierId: supplier.supplierId]) }">Edit</a></td>
    </tr>
    <% } %>
    </tbody>
</table>

<h4>${ selectedSupplier ? 'Edit Supplier' : 'Add Supplier' }</h4>
<form method="post">
    <input type="hidden" name="supplierId" value="${ selectedSupplier?.supplierId ?: '' }"/>
    <p><label>Name</label><br/><input type="text" name="supplierName" value="${ selectedSupplier?.name ?: '' }" required="required"/></p>
    <p><label>Code</label><br/><input type="text" name="supplierCode" value="${ selectedSupplier?.code ?: '' }" required="required"/></p>
    <p><label>Email Address</label><br/><input type="email" name="supplierEmailAddress" value="${ selectedSupplier?.emailAddress ?: '' }"/></p>
    <p><label>API Base URL</label><br/><input type="text" name="supplierApiBaseUrl" value="${ selectedSupplier?.apiBaseUrl ?: '' }" size="80"/></p>
    <p><label>API Username</label><br/><input type="text" name="supplierApiUsername" value="${ selectedSupplier?.apiUsername ?: '' }"/></p>
    <p><label>API Password</label><br/><input type="password" name="supplierApiPassword" value="${ selectedSupplier?.apiPassword ?: '' }"/></p>
    <p><label>Connection Timeout</label><br/><input type="number" name="supplierConnectionTimeout" value="${ selectedSupplier?.connectionTimeout ?: 10000 }"/></p>
    <p><label>Read Timeout</label><br/><input type="number" name="supplierReadTimeout" value="${ selectedSupplier?.readTimeout ?: 30000 }"/></p>
    <p><label><input type="checkbox" name="supplierApiEnabled" value="true" ${ selectedSupplier?.apiEnabled ? 'checked="checked"' : '' }/> Supplier uses API</label></p>
    <p><label><input type="checkbox" name="supplierActive" value="true" ${ selectedSupplier == null || selectedSupplier?.active ? 'checked="checked"' : '' }/> Active</label></p>
    <p><button type="submit" name="action" value="saveSupplier">Save Supplier</button></p>
</form>

<h3>SMTP Configuration</h3>
<form method="post">
    <p><label>SMTP Host</label><br/><input type="text" name="smtpHost" value="${ smtpHost ?: '' }" size="60"/></p>
    <p><label>SMTP Port</label><br/><input type="text" name="smtpPort" value="${ smtpPort ?: '587' }"/></p>
    <p><label>SMTP Username</label><br/><input type="text" name="smtpUsername" value="${ smtpUsername ?: '' }"/></p>
    <p><label>SMTP Password</label><br/><input type="password" name="smtpPassword" value="${ smtpPassword ?: '' }"/></p>
    <p><label>From Email Address</label><br/><input type="email" name="smtpFrom" value="${ smtpFrom ?: '' }" size="60"/></p>
    <p><label><input type="checkbox" name="smtpAuth" value="true" ${ smtpAuth == 'true' ? 'checked="checked"' : '' }/> Require authentication</label></p>
    <p><label><input type="checkbox" name="smtpStarttls" value="true" ${ smtpStarttls == 'true' ? 'checked="checked"' : '' }/> Enable STARTTLS</label></p>
    <p><button type="submit" name="action" value="saveSmtp">Save SMTP Settings</button></p>
</form>
