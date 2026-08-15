package org.openmrs.module.rwandapharmacymanagement.integration.hie;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestStatus;

import java.math.BigDecimal;
import java.util.Map;

public class HieMedicationPayloadBuilderTest {

    private final HieMedicationPayloadBuilder builder = new HieMedicationPayloadBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void buildMedicationRequestShouldUseSameGroupIdentifierForAllLinesInPrescription() throws Exception {
        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setPatientDispensingRequestId(77);

        PatientDispensingRequestLine firstLine = line("line-one", "NPC-1", "Drug one");
        PatientDispensingRequestLine secondLine = line("line-two", "NPC-2", "Drug two");

        Map firstPayload = payload(builder.buildMedicationRequest(request, firstLine, "", "", "KFH", "", "", ""));
        Map secondPayload = payload(builder.buildMedicationRequest(request, secondLine, "", "", "KFH", "", "", ""));

        Assert.assertEquals("EP-KFH-77", ((Map) firstPayload.get("groupIdentifier")).get("value"));
        Assert.assertEquals(((Map) firstPayload.get("groupIdentifier")).get("value"),
                ((Map) secondPayload.get("groupIdentifier")).get("value"));
        Assert.assertNotEquals(firstPayload.get("id"), secondPayload.get("id"));
    }

    @Test
    public void buildMedicationRequestShouldSendStoppedStatusForDeletedLine() throws Exception {
        PatientDispensingRequest request = new PatientDispensingRequest();
        request.setPatientDispensingRequestId(88);
        PatientDispensingRequestLine line = line("deleted-line", "NPC-3", "Deleted drug");
        line.setVoided(Boolean.TRUE);
        line.setLineStatus(RequestStatus.CANCELLED);

        Map payload = payload(builder.buildMedicationRequest(request, line, "", "", "KFH", "", "", ""));

        Assert.assertEquals("stopped", payload.get("status"));
    }

    private PatientDispensingRequestLine line(String uuid, String code, String name) {
        Item item = new Item();
        item.setCode(code);
        item.setName(name);

        PatientDispensingRequestLine line = new PatientDispensingRequestLine();
        line.setUuid(uuid);
        line.setItem(item);
        line.setRequestedQuantity(BigDecimal.ONE);
        return line;
    }

    private Map payload(String json) throws Exception {
        return objectMapper.readValue(json, Map.class);
    }
}
