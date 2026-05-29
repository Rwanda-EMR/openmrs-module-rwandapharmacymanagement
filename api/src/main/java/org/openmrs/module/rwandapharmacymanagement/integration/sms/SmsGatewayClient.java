package org.openmrs.module.rwandapharmacymanagement.integration.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmsGatewayClient {

    public static final String FIELD_POST_URL = "postUrl";
    public static final String FIELD_SENDER = "sender";
    public static final String FIELD_RECIPIENT = "recipient";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_CODING = "coding";
    public static final String FIELD_DLR_URL = "dlrUrl";
    public static final String FIELD_DLR_LEVEL = "dlrLevel";

    public String send(String postUrl, String username, String password, String sender, String recipient,
                       String content, String coding, String dlrUrl, String dlrLevel,
                       Integer connectionTimeout, Integer readTimeout) throws IOException {
        if (isBlank(postUrl)) {
            throw new IOException("SMS post URL is missing");
        }
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("from", sender);
        parameters.put("to", recipient);
        parameters.put("content", content);
        parameters.put("username", username);
        parameters.put("password", password);
        parameters.put("coding", isBlank(coding) ? "0" : coding);
        parameters.put("dlr-level", isBlank(dlrLevel) ? "2" : dlrLevel);
        if (!isBlank(dlrUrl)) {
            parameters.put("dlr-url", dlrUrl);
        }
        return executeRequest(postUrl, formEncode(parameters), connectionTimeout, readTimeout);
    }

    public String retry(String payload, String username, String password,
                        Integer connectionTimeout, Integer readTimeout) throws IOException {
        Map<String, String> parameters = parseJsonPayload(payload);
        return send(parameters.get(FIELD_POST_URL), username, password,
                parameters.get(FIELD_SENDER),
                parameters.get(FIELD_RECIPIENT),
                parameters.get(FIELD_CONTENT),
                parameters.get(FIELD_CODING),
                parameters.get(FIELD_DLR_URL),
                parameters.get(FIELD_DLR_LEVEL),
                connectionTimeout,
                readTimeout);
    }

    public String buildRetryPayload(String postUrl, String sender, String recipient, String content,
                                    String coding, String dlrUrl, String dlrLevel) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put(FIELD_POST_URL, postUrl);
        parameters.put(FIELD_SENDER, sender);
        parameters.put(FIELD_RECIPIENT, recipient);
        parameters.put(FIELD_CONTENT, content);
        parameters.put(FIELD_CODING, isBlank(coding) ? "0" : coding);
        parameters.put(FIELD_DLR_URL, dlrUrl);
        parameters.put(FIELD_DLR_LEVEL, isBlank(dlrLevel) ? "2" : dlrLevel);
        return toJson(parameters);
    }

    private String executeRequest(String url, String payload,
                                  Integer connectionTimeout, Integer readTimeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(safeTimeout(connectionTimeout, 10000));
        connection.setReadTimeout(safeTimeout(readTimeout, 30000));
        connection.setRequestProperty("Accept", "text/plain, */*");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setDoOutput(true);
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        int responseCode = connection.getResponseCode();
        String body = readStream(responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("SMS gateway call failed with HTTP " + responseCode + ": " + body);
        }
        if (isBlank(body) || !body.trim().startsWith("Success")) {
            throw new IOException("SMS gateway returned an unsuccessful response: " + body);
        }
        return body;
    }

    private String formEncode(Map<String, String> parameters) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private Map<String, String> parseJsonPayload(String payload) throws IOException {
        Map<String, String> values = new LinkedHashMap<String, String>();
        String trimmed = payload == null ? "" : payload.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IOException("SMS retry payload is not JSON");
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        int index = 0;
        while (index < body.length()) {
            while (index < body.length() && Character.isWhitespace(body.charAt(index))) {
                index++;
            }
            if (index >= body.length()) {
                break;
            }
            ParsedString key = parseJsonString(body, index);
            index = key.nextIndex;
            while (index < body.length() && (Character.isWhitespace(body.charAt(index)) || body.charAt(index) == ':')) {
                index++;
            }
            ParsedString value = parseJsonString(body, index);
            values.put(key.value, value.value);
            index = value.nextIndex;
            while (index < body.length() && (Character.isWhitespace(body.charAt(index)) || body.charAt(index) == ',')) {
                index++;
            }
        }
        return values;
    }

    private ParsedString parseJsonString(String text, int start) throws IOException {
        if (start >= text.length() || text.charAt(start) != '"') {
            throw new IOException("Invalid SMS retry payload JSON string");
        }
        StringBuilder builder = new StringBuilder();
        int index = start + 1;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '"') {
                return new ParsedString(builder.toString(), index + 1);
            }
            if (current == '\\') {
                index++;
                if (index >= text.length()) {
                    throw new IOException("Invalid SMS retry payload escape");
                }
                char escaped = text.charAt(index);
                if (escaped == 'n') {
                    builder.append('\n');
                } else if (escaped == 'r') {
                    builder.append('\r');
                } else if (escaped == 't') {
                    builder.append('\t');
                } else {
                    builder.append(escaped);
                }
            } else {
                builder.append(current);
            }
            index++;
        }
        throw new IOException("Unterminated SMS retry payload JSON string");
    }

    private String toJson(Map<String, String> parameters) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (builder.length() > 1) {
                builder.append(",");
            }
            builder.append("\"").append(jsonEscape(entry.getKey())).append("\":");
            builder.append("\"").append(jsonEscape(entry.getValue())).append("\"");
        }
        builder.append("}");
        return builder.toString();
    }

    private String jsonEscape(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static class ParsedString {
        private final String value;
        private final int nextIndex;

        private ParsedString(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

    private String encode(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    private int safeTimeout(Integer timeout, int defaultValue) {
        return timeout == null || timeout.intValue() <= 0 ? defaultValue : timeout.intValue();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
