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
