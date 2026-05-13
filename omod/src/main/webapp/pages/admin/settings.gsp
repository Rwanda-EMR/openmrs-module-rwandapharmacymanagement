<%
    ui.decorateWith("rwandapharmacymanagement", "simplePage")
%>
<h2>${ ui.message("rwandapharmacymanagement.admin.settings.title") }</h2>
<form method="post">
    <p><label>${ ui.message("rwandapharmacymanagement.admin.settings.markup") }</label><br/><input type="number" step="0.01" name="dispensingMarkupPercentage" value="${ dispensingMarkupPercentage ?: 0 }"/></p>
    <p><label>Expiry Alert Days</label><br/><input type="number" name="expiryAlertDays" value="${ expiryAlertDays ?: 90 }"/></p>
    <p>
        <label>Prescription Payment Bill Check</label><br/>
        <select name="prescriptionPaymentBillMode">
            <option value="latest" ${ prescriptionPaymentBillMode != 'strict' ? 'selected="selected"' : '' }>Use related bill, then latest patient bill fallback</option>
            <option value="strict" ${ prescriptionPaymentBillMode == 'strict' ? 'selected="selected"' : '' }>Strictly use the bill linked to the prescription</option>
        </select>
        <br/><span style="color:#666;font-size:12px;">Strict mode prevents prescriptions from being marked paid using another bill from the same patient.</span>
    </p>
    <h3>Intouch SMS Settings</h3>
    <p><label><input type="checkbox" name="smsEnabled" value="true" ${ smsEnabled == 'true' ? 'checked="checked"' : '' }/> Enable prescription SMS</label></p>
    <p><label>Post URL</label><br/><input type="text" name="smsPostUrl" value="${ smsPostUrl ?: 'http://41.186.44.171:1401/send' }" size="80"/></p>
    <p><label>Username</label><br/><input type="text" name="smsUsername" value="${ smsUsername ?: '' }"/></p>
    <p><label>Password</label><br/><input type="password" name="smsPassword" value="${ smsPassword ?: '' }"/></p>
    <p><label>Sender</label><br/><input type="text" name="smsSender" value="${ smsSender ?: '' }"/></p>
    <p><label>Coding</label><br/><input type="text" name="smsCoding" value="${ smsCoding ?: '0' }"/></p>
    <p><label>DLR URL</label><br/><input type="text" name="smsDlrUrl" value="${ smsDlrUrl ?: '' }" size="80"/></p>
    <p><label>DLR Level</label><br/><input type="number" name="smsDlrLevel" value="${ smsDlrLevel ?: '2' }" min="1" max="3"/></p>
    <p>
        <label>Phone Person Attribute Type UUID</label><br/>
        <input type="text" name="smsPhonePersonAttributeTypeUuid" value="${ smsPhonePersonAttributeTypeUuid ?: '' }" size="80"/>
        <br/><span style="color:#666;font-size:12px;">SMS is sent as Intouch form parameters: from, to, content, username, password, coding, dlr-url, dlr-level.</span>
    </p>
    <p>
        <label>Prescription Frequency Concept Set</label><br/>
        <input type="text" name="prescriptionFrequencyConceptSet" value="${ prescriptionFrequencyConceptSet ?: '' }" placeholder="Concept UUID, ID, or exact name"/>
        <% if (prescriptionFrequencyConceptSetDisplay) { %>
        <br/><span style="color:#666;font-size:12px;">Current concept: ${ prescriptionFrequencyConceptSetDisplay }</span>
        <% } else { %>
        <br/><span style="color:#666;font-size:12px;">Configure a concept set whose members represent allowed prescription frequencies.</span>
        <% } %>
    </p>
    <p>
        <label>Lab Service Concept Set</label><br/>
        <input type="text" name="labServiceConceptSet" value="${ labServiceConceptSet ?: '' }" placeholder="Concept UUID, ID, or exact name"/>
        <% if (labServiceConceptSetDisplay) { %>
        <br/><span style="color:#666;font-size:12px;">Current concept: ${ labServiceConceptSetDisplay }</span>
        <% } else { %>
        <br/><span style="color:#666;font-size:12px;">Configure one question concept whose answers represent lab services that can be requested.</span>
        <% } %>
    </p>
    <p><button type="submit">Save Settings</button></p>
</form>
