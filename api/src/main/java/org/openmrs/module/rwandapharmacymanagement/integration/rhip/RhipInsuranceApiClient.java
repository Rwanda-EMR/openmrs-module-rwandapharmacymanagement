package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Posts to RHIP with Apache HttpClient. Java's HttpURLConnection silently drops the Origin header (a restricted
 * header), and RHIP answers 401 "Missing Origin header" without it.
 */
public class RhipInsuranceApiClient {

    public String post(String url, String apiKey, String apiOrigin, String payload,
                       Integer connectionTimeout, Integer readTimeout) throws IOException {
        int connectTimeoutMillis = safeTimeout(connectionTimeout, 15000);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMillis)
                .setConnectionRequestTimeout(connectTimeoutMillis)
                .setSocketTimeout(safeTimeout(readTimeout, 30000))
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(requestConfig);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");
        if (!isBlank(apiKey)) {
            post.setHeader("x-api-key", apiKey.trim());
        }
        if (!isBlank(apiOrigin)) {
            post.setHeader("Origin", apiOrigin.trim());
        }
        post.setEntity(new StringEntity(safe(payload), ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));

        int responseCode;
        String body;
        String retryAfter;
        CloseableHttpClient client = HttpClients.createDefault();
        try {
            CloseableHttpResponse response = client.execute(post);
            try {
                responseCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                Header retryAfterHeader = response.getFirstHeader("Retry-After");
                retryAfter = retryAfterHeader == null ? null : retryAfterHeader.getValue();
            }
            finally {
                response.close();
            }
        }
        catch (IOException e) {
            if (neverReachedRhip(e)) {
                // Nothing reached RHIP, so the call can safely be sent again
                throw RhipCallException.notSent(e);
            }
            // The request may have been recorded even though no reply came back
            throw RhipCallException.noReply(e);
        }
        finally {
            client.close();
        }
        if (responseCode < 200 || responseCode >= 300) {
            throw RhipCallException.httpError(responseCode, body, retryAfter);
        }
        return body;
    }

    private boolean neverReachedRhip(IOException e) {
        return e instanceof ConnectException || e instanceof ConnectTimeoutException
                || e instanceof UnknownHostException || e instanceof SSLHandshakeException;
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
