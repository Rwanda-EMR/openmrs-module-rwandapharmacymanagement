package org.openmrs.module.rwandapharmacymanagement.integration.hie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HieMedicationApiClient {

    public String post(String baseUrl, String endpointPath, String username, String password,
                       String payload, Integer connectionTimeout, Integer readTimeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(buildUrl(baseUrl, endpointPath)).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(safeTimeout(connectionTimeout, 10000));
        connection.setReadTimeout(safeTimeout(readTimeout, 30000));
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        if (!isBlank(username) || !isBlank(password)) {
            String credentials = safe(username) + ":" + safe(password);
            connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        connection.setDoOutput(true);
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(safe(payload).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        int responseCode = connection.getResponseCode();
        String body = readStream(responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("HIE API call failed with HTTP " + responseCode + ": " + body);
        }
        return body;
    }

    private String buildUrl(String baseUrl, String endpointPath) {
        String resolvedBaseUrl = safe(baseUrl).trim();
        String resolvedPath = safe(endpointPath).trim();
        if (resolvedBaseUrl.endsWith("/")) {
            resolvedBaseUrl = resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1);
        }
        if (!resolvedPath.startsWith("/")) {
            resolvedPath = "/" + resolvedPath;
        }
        return resolvedBaseUrl + resolvedPath;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
