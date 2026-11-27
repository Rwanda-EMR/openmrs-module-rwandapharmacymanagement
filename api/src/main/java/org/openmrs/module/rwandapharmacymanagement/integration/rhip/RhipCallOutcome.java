package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns RHIP's answers to the MMI pharmacy calls into an integration log status and a summary staff can act on.
 * HTTP 200 does not mean every line was taken, so successful answers are read for the lines MMI left out.
 */
public final class RhipCallOutcome {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RhipCallOutcome() {
    }

    /**
     * @return RETRY_PENDING only when sending the same request again is safe, FAILED otherwise
     */
    public static IntegrationStatus failureStatus(Exception e) {
        return e instanceof RhipCallException && ((RhipCallException) e).isRetriable()
                ? IntegrationStatus.RETRY_PENDING : IntegrationStatus.FAILED;
    }

    /**
     * @return the HTTP status RHIP answered a failed call with, or null when it did not answer
     */
    public static Integer httpStatus(Exception e) {
        return e instanceof RhipCallException ? ((RhipCallException) e).getStatusCode() : null;
    }

    public static String failureSummary(IntegrationType type, Exception e) {
        String what = describe(type);
        if (!(e instanceof RhipCallException)) {
            return what + " could not be sent to RHIP: " + e.getMessage();
        }
        RhipCallException callException = (RhipCallException) e;
        String sendAgain = type == IntegrationType.RHIP_MEDICINE_DELIVERY
                ? "Use Resend on the MMI deliveries page" : "It will be sent again";
        if (!callException.isSent()) {
            return what + " could not reach RHIP. " + sendAgain;
        }
        if (callException.getStatusCode() == null) {
            return what + " got no reply from RHIP and may have been recorded. "
                    + (type == IntegrationType.RHIP_MEDICINE_DELIVERY
                    ? "Check with RHIP before sending it again."
                    : "Check the patient's MMI prescription status before sending it again.");
        }
        String reason = message(callException.getResponseBody());
        StringBuilder summary = new StringBuilder(what).append(" refused by RHIP (HTTP ")
                .append(callException.getStatusCode()).append(")");
        if (reason != null) {
            summary.append(": ").append(reason);
        }
        if (callException.isRetriable()) {
            summary.append(". ").append(sendAgain);
            if (callException.getRetryAfter() != null) {
                summary.append(" (RHIP asked to wait ").append(callException.getRetryAfter()).append(" s)");
            }
        }
        return summary.toString();
    }

    /**
     * @return a summary of what MMI recorded, naming the lines it rejected, skipped or still holds as remaining
     */
    public static String successSummary(IntegrationType type, String responseBody) {
        String what = describe(type);
        JsonNode root = parse(responseBody);
        JsonNode data = root == null ? null : root.get("data");
        List<String> problems = new ArrayList<String>();
        StringBuilder summary = new StringBuilder(what).append(" recorded by MMI");
        if (type == IntegrationType.RHIP_INPATIENT_PRESCRIPTION) {
            addLines(problems, data == null ? null : data.get("invalid"), "code", null, "reason");
            if (!problems.isEmpty()) {
                summary.append("; not recorded: ").append(join(problems));
            }
        }
        else if (type == IntegrationType.RHIP_VISIT_DISPENSE) {
            String status = text(data, "dispensingStatus");
            if (status != null) {
                summary.append(" (").append(status).append(")");
            }
            addLines(problems, data == null ? null : data.get("skipped"), "productCode", null, "reason");
            if (!problems.isEmpty()) {
                summary.append("; not dispensed: ").append(join(problems));
            }
            List<String> remaining = new ArrayList<String>();
            addLines(remaining, data == null ? null : data.get("remaining"), "productCode", "quantity", null);
            if (!remaining.isEmpty()) {
                summary.append("; still to dispense: ").append(join(remaining));
            }
        }
        else {
            String message = text(root, "message");
            if (message != null) {
                summary.append(": ").append(message);
            }
        }
        return summary.toString();
    }

    /**
     * @return true when MMI left out at least one line of a successful call
     */
    public static boolean hasRejectedLines(IntegrationType type, String responseBody) {
        JsonNode root = parse(responseBody);
        JsonNode data = root == null ? null : root.get("data");
        if (data == null) {
            return false;
        }
        JsonNode lines = type == IntegrationType.RHIP_INPATIENT_PRESCRIPTION ? data.get("invalid")
                : type == IntegrationType.RHIP_VISIT_DISPENSE ? data.get("skipped") : null;
        return lines != null && lines.isArray() && lines.size() > 0;
    }

    private static String describe(IntegrationType type) {
        if (type == IntegrationType.RHIP_INPATIENT_PRESCRIPTION) {
            return "Inpatient prescription";
        }
        if (type == IntegrationType.RHIP_VISIT_DISPENSE) {
            return "Dispense";
        }
        if (type == IntegrationType.RHIP_MEDICINE_DELIVERY) {
            return "Medicine delivery";
        }
        return "RHIP call";
    }

    private static void addLines(List<String> target, JsonNode lines, String codeField, String quantityField,
                                 String reasonField) {
        if (lines == null || !lines.isArray()) {
            return;
        }
        for (JsonNode line : lines) {
            String code = text(line, codeField);
            if (code == null) {
                code = text(line, "code");
            }
            StringBuilder entry = new StringBuilder(code == null ? "?" : code);
            String quantity = quantityField == null ? null : text(line, quantityField);
            if (quantity != null) {
                entry.append(" x ").append(quantity);
            }
            String reason = reasonField == null ? null : text(line, reasonField);
            if (reason != null) {
                entry.append(" (").append(reason).append(")");
            }
            target.add(entry.toString());
        }
    }

    private static String message(String responseBody) {
        JsonNode root = parse(responseBody);
        String message = text(root, "message");
        if (message == null) {
            message = text(root, "error");
        }
        if (message == null && responseBody != null && !responseBody.trim().isEmpty()) {
            message = responseBody.trim().length() > 300 ? responseBody.trim().substring(0, 300) : responseBody.trim();
        }
        return message;
    }

    private static JsonNode parse(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(body);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.trim().isEmpty() ? null : text.trim();
    }

    private static String join(List<String> values) {
        StringBuilder ret = new StringBuilder();
        for (String value : values) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(value);
        }
        return ret.toString();
    }
}
