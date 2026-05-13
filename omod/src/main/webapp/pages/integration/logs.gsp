<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.integration.logs.title") }</h2>
<table>
    <thead><tr><th>Date</th><th>Type</th><th>Status</th><th>Endpoint</th><th>Summary</th></tr></thead>
    <tbody>
    <% integrationLogs.each { %>
    <tr>
        <td>${ it.dateCreated }</td>
        <td>${ it.integrationType }</td>
        <td>${ it.status }</td>
        <td>${ it.endpoint ?: '' }</td>
        <td>${ it.summary ?: '' }</td>
    </tr>
    <% } %>
    </tbody>
</table>
