<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.safety.rules.title") }</h2>
<form method="post">
    <p><label>Rule Name</label><br/><input type="text" name="ruleName" required/></p>
    <p><label>Rule Type</label><br/><select name="ruleType"><option value="DRUG_INTERACTION">Drug Interaction</option><option value="DISEASE_CONTRAINDICATION">Disease Contraindication</option><option value="GENDER_RESTRICTION">Gender Restriction</option></select></p>
    <p><label>Severity</label><br/><select name="severity"><option value="INFO">Info</option><option value="WARNING">Warning</option><option value="BLOCKING">Blocking</option></select></p>
    <p><label><input type="checkbox" name="blocking" value="true"/> Blocking rule</label></p>
    <p><label>Gender Restriction</label><br/><input type="text" name="genderRestriction" placeholder="M or F"/></p>
    <p><label>Definition</label><br/><textarea name="ruleDefinition" rows="4" cols="80"></textarea></p>
    <p>Use local item codes in the definition. Examples: <code>PCM001|IBU001</code> for an interaction, <code>ORA001</code> plus gender <code>F</code> for a gender restriction.</p>
    <p><button type="submit">Save Rule</button></p>
</form>
<table>
    <thead><tr><th>Name</th><th>Type</th><th>Severity</th><th>Blocking</th><th>Gender</th><th>Definition</th></tr></thead>
    <tbody>
    <% rules.each { %>
    <tr><td>${ it.ruleName }</td><td>${ it.ruleType }</td><td>${ it.severity }</td><td>${ it.blocking }</td><td>${ it.genderRestriction ?: '' }</td><td>${ it.ruleDefinition ?: '' }</td></tr>
    <% } %>
    </tbody>
</table>
