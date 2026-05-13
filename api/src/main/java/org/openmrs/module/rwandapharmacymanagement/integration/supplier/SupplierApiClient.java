package org.openmrs.module.rwandapharmacymanagement.integration.supplier;

import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierAuthenticationResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierDeliveryDetailsItem;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierDeliveryDetailsResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierMatchedPrice;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierPriceLookupResponse;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierPriceRecord;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierProductMasterItem;
import org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto.SupplierProductMasterProgram;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupplierApiClient {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"(?:token|jwt|jwtToken|access_token|accessToken|id_token|idToken)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JWT_BODY_PATTERN = Pattern.compile("^[A-Za-z0-9\\-_]+=*\\.[A-Za-z0-9\\-_]+=*\\.[A-Za-z0-9\\-_+=/]*$");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupplierAuthenticationResponse authenticate(String baseUrl, String username, String password,
                                                       Integer connectionTimeout, Integer readTimeout) {
        SupplierAuthenticationResponse response = new SupplierAuthenticationResponse();
        if (isBlank(baseUrl) || isBlank(username) || isBlank(password)) {
            response.setAuthenticated(false);
            response.setErrorMessage("Supplier credentials or base URL are missing");
            return response;
        }
        String payload = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";
        try {
            String body = executeRequest("POST", buildUrl(baseUrl, "/api/authenticate"), null, payload,
                    safeTimeout(connectionTimeout, 10000), safeTimeout(readTimeout, 30000));
            response.setRawResponse(body);
            response.setToken(extractAuthenticationToken(body));
            response.setAuthenticated(!isBlank(response.getToken()));
            response.setExpiresAt(new Date(System.currentTimeMillis() + (15 * 60 * 1000L)));
            if (!response.isAuthenticated()) {
                response.setErrorMessage("Authentication response did not include a JWT token. Response: "
                        + abbreviate(body, 240));
            }
        }
        catch (IOException e) {
            response.setAuthenticated(false);
            response.setErrorMessage(e.getMessage());
        }
        return response;
    }

    public List<SupplierProductMasterProgram> getProductMaster(String baseUrl, String token, Integer connectionTimeout, Integer readTimeout)
            throws IOException {
        String body = executeRequest("GET", buildUrl(baseUrl, "/api/integration/product-master"), token, null,
                safeTimeout(connectionTimeout, 10000), safeTimeout(readTimeout, 30000));
        return parseProductMaster(body);
    }

    public List<SupplierPriceRecord> loadPrices(String baseUrl, String token, Integer connectionTimeout, Integer readTimeout)
            throws IOException {
        String body = executeRequest("GET", buildUrl(baseUrl, "/api/integration/load-prices"), token, null,
                safeTimeout(connectionTimeout, 10000), safeTimeout(readTimeout, 30000));
        return parsePriceRecords(body);
    }

    public SupplierPriceLookupResponse lookupPrices(String baseUrl, String token, String payload,
                                                    Integer connectionTimeout, Integer readTimeout) throws IOException {
        String body = executeRequest("POST", buildUrl(baseUrl, "/api/integration/prices"), token, payload,
                safeTimeout(connectionTimeout, 10000), safeTimeout(readTimeout, 30000));
        return parsePriceLookup(body);
    }

    public Map<String, Object> submitOrder(String baseUrl, String token, String payload,
                                           Integer connectionTimeout, Integer readTimeout) throws IOException {
        String body = executeRequest("POST", buildUrl(baseUrl, "/api/integration/submit-order"), token, payload,
                safeTimeout(connectionTimeout, 10000), safeTimeout(readTimeout, 30000));
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("rawResponse", body);
        return response;
    }

    public Map<String, Object> confirmDelivery(String baseUrl, String token, String payload,
                                               Integer connectionTimeout, Integer readTimeout) throws IOException {
        String body = executeRequest("POST", buildUrl(baseUrl, "/api/integration/delivery-confirmation"), token, payload,
                safeTimeout(connectionTimeout, 10000), safeTimeout(readTimeout, 30000));
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("rawResponse", body);
        return response;
    }

    public SupplierDeliveryDetailsResponse getDeliveryDetails(String baseUrl, String token, String payload,
                                                              Integer connectionTimeout, Integer readTimeout) throws IOException {
        String body = executeRequest("POST", buildUrl(baseUrl, "/api/integration/delivery-details"), token, payload,
                safeTimeout(connectionTimeout, 10000), safeTimeout(readTimeout, 30000));
        return parseDeliveryDetails(body);
    }

    private String executeRequest(String method, String url, String token, String payload,
                                  int connectionTimeout, int readTimeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        if (!isBlank(token)) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (payload != null) {
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
        }
        int responseCode = connection.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = readStream(stream);
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Supplier API call failed with HTTP " + responseCode + ": " + body);
        }
        return body;
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

    private String extractToken(String body) {
        if (isBlank(body)) {
            return null;
        }
        String trimmed = body.trim();
        if (looksLikeJwt(trimmed)) {
            return trimmed;
        }
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            String token = extractToken(root);
            if (!isBlank(token)) {
                return token;
            }
        }
        catch (IOException ignored) {
            // Fall back to regex-based extraction for non-standard responses.
        }
        Matcher matcher = TOKEN_PATTERN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractAuthenticationToken(String body) {
        String token = extractToken(body);
        if (!isBlank(token)) {
            return token;
        }
        return isBlank(body) ? null : body.trim();
    }

    private String extractToken(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.asText();
            return looksLikeJwt(value) ? value : null;
        }
        if (node.isObject()) {
            String directToken = firstNonBlank(
                    text(node, "token"),
                    text(node, "jwt"),
                    text(node, "jwtToken"),
                    text(node, "access_token"),
                    text(node, "accessToken"),
                    text(node, "id_token"),
                    text(node, "idToken"));
            if (!isBlank(directToken)) {
                return directToken;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String token = extractToken(field.getValue());
                if (!isBlank(token)) {
                    return token;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode element : node) {
                String token = extractToken(element);
                if (!isBlank(token)) {
                    return token;
                }
            }
        }
        return null;
    }

    private SupplierDeliveryDetailsResponse parseDeliveryDetails(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        SupplierDeliveryDetailsResponse response = new SupplierDeliveryDetailsResponse();
        response.setOrderNo(firstText(root, "orderNo", "order_number"));
        response.setCustomerCode(firstText(root, "customerCode", "customer_code"));
        response.setCustomerName(firstText(root, "customerName", "customer_name"));
        response.setDeliveryId(firstText(root, "deliveryId", "delivery_id"));
        response.setDeliveryDate(firstText(root, "deliveryDate", "delivery_date"));

        List<SupplierDeliveryDetailsItem> items = new ArrayList<SupplierDeliveryDetailsItem>();
        JsonNode itemsNode = root == null ? null : root.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                SupplierDeliveryDetailsItem item = new SupplierDeliveryDetailsItem();
                item.setNpcCode(firstText(itemNode, "npcCode", "npc_code"));
                item.setDescription(firstText(itemNode, "description", "itemName", "item_name"));
                item.setDeliveryQuantity(firstText(itemNode, "deliveryQuantity", "delivery_quantity", "quantity"));
                item.setUnits(firstText(itemNode, "units", "uom"));
                item.setBatch(firstText(itemNode, "batch", "batchNumber", "batch_number"));
                item.setExpiryDate(firstText(itemNode, "expiryDate", "expiry_date"));
                item.setItemNo(firstText(itemNode, "itemNo", "item_no", "lineNumber", "line_number"));
                items.add(item);
            }
        }
        response.setItems(items);
        return response;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.get(fieldName);
            if (valueNode != null && !valueNode.isNull()) {
                String value = valueNode.asText();
                if (value != null) {
                    value = value.trim();
                }
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean looksLikeJwt(String value) {
        return !isBlank(value) && JWT_BODY_PATTERN.matcher(value.trim()).matches();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    private List<SupplierProductMasterProgram> parseProductMaster(String body) throws IOException {
        List<SupplierProductMasterProgram> programs = new ArrayList<SupplierProductMasterProgram>();
        JsonNode root = objectMapper.readTree(body);
        if (root != null && root.isArray()) {
            for (JsonNode programNode : root) {
                SupplierProductMasterProgram program = new SupplierProductMasterProgram();
                program.setProgram(text(programNode, "program"));
                List<SupplierProductMasterItem> items = new ArrayList<SupplierProductMasterItem>();
                JsonNode itemsNode = programNode.get("items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode itemNode : itemsNode) {
                        SupplierProductMasterItem item = new SupplierProductMasterItem();
                        item.setNpcCode(text(itemNode, "npcCode"));
                        item.setMaterialDescription(text(itemNode, "materialDescription"));
                        item.setMaterialType(text(itemNode, "materialType"));
                        items.add(item);
                    }
                }
                program.setItems(items);
                programs.add(program);
            }
        }
        return programs;
    }

    private List<SupplierPriceRecord> parsePriceRecords(String body) throws IOException {
        List<SupplierPriceRecord> records = new ArrayList<SupplierPriceRecord>();
        JsonNode root = objectMapper.readTree(body);
        if (root != null && root.isArray()) {
            for (JsonNode node : root) {
                SupplierPriceRecord record = new SupplierPriceRecord();
                record.setId(integer(node, "id"));
                record.setSapMaterial(text(node, "sapMaterial"));
                record.setNpcCode(text(node, "npcCode"));
                record.setMaterialDescription(text(node, "materialDescription"));
                record.setWbsElement(text(node, "wbsElement"));
                record.setProgram(text(node, "program"));
                record.setPrice(doubleValue(node, "price"));
                record.setCurrency(text(node, "currency"));
                record.setUpdatedAt(text(node, "updatedAt"));
                records.add(record);
            }
        }
        return records;
    }

    private SupplierPriceLookupResponse parsePriceLookup(String body) throws IOException {
        SupplierPriceLookupResponse response = new SupplierPriceLookupResponse();
        JsonNode root = objectMapper.readTree(body);
        List<SupplierMatchedPrice> matched = new ArrayList<SupplierMatchedPrice>();
        JsonNode matchedNode = root == null ? null : root.get("matchedPrices");
        if (matchedNode != null && matchedNode.isArray()) {
            for (JsonNode node : matchedNode) {
                SupplierMatchedPrice price = new SupplierMatchedPrice();
                price.setNpcCode(text(node, "npcCode"));
                price.setMaterialDescription(text(node, "materialDescription"));
                price.setSellingPrice(doubleValue(node, "sellingPrice"));
                matched.add(price);
            }
        }
        List<String> unmatched = new ArrayList<String>();
        JsonNode unmatchedNode = root == null ? null : root.get("unmatchedNpcCodes");
        if (unmatchedNode != null && unmatchedNode.isArray()) {
            for (JsonNode node : unmatchedNode) {
                unmatched.add(node.asText());
            }
        }
        response.setMatchedPrices(matched);
        response.setUnmatchedNpcCodes(unmatched);
        response.setMessage(text(root, "message"));
        return response;
    }

    private String text(JsonNode node, String field) {
        return node != null && node.get(field) != null ? node.get(field).asText() : "";
    }

    private Integer integer(JsonNode node, String field) {
        return node != null && node.get(field) != null ? Integer.valueOf(node.get(field).asInt()) : null;
    }

    private Double doubleValue(JsonNode node, String field) {
        return node != null && node.get(field) != null ? Double.valueOf(node.get(field).asDouble()) : null;
    }

    private String buildUrl(String baseUrl, String path) {
        if (isBlank(baseUrl)) {
            throw new IllegalStateException("Supplier base URL is not configured");
        }
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmedBase + path;
    }

    private int safeTimeout(Integer timeout, int defaultValue) {
        return timeout == null || timeout.intValue() <= 0 ? defaultValue : timeout.intValue();
    }

    private String escapeJson(String value) {
        return safe(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
