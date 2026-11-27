package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RhipInsuranceApiClientTest {

    private HttpServer server;

    private final AtomicReference<String> origin = new AtomicReference<String>();

    private final AtomicReference<String> apiKey = new AtomicReference<String>();

    private int status = 200;

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/visit-dispense", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
            origin.set(exchange.getRequestHeaders().getFirst("Origin"));
            apiKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            byte[] body = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Retry-After", "30");
            exchange.sendResponseHeaders(status, body.length);
            OutputStream out = exchange.getResponseBody();
            out.write(body);
            out.close();
            }
        });
        server.start();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/visit-dispense";
    }

    @Test
    public void post_shouldSendTheOriginAndApiKeyHeaders() throws Exception {
        String body = new RhipInsuranceApiClient().post(url(), "key-1", "http://facility.example", "{}", 5000, 5000);
        Assert.assertEquals("{\"success\":true}", body);
        Assert.assertEquals("http://facility.example", origin.get());
        Assert.assertEquals("key-1", apiKey.get());
    }

    @Test
    public void post_shouldReportTheStatusAndRetryAfterOfAnError() throws Exception {
        status = 503;
        try {
            new RhipInsuranceApiClient().post(url(), "key-1", "http://facility.example", "{}", 5000, 5000);
            Assert.fail();
        }
        catch (RhipCallException e) {
            Assert.assertTrue(e.isSent());
            Assert.assertEquals(Integer.valueOf(503), e.getStatusCode());
            Assert.assertEquals("30", e.getRetryAfter());
        }
    }

    @Test
    public void post_shouldReportACallThatNeverReachedRhipAsNotSent() throws Exception {
        String unreachable = url();
        server.stop(0);
        try {
            new RhipInsuranceApiClient().post(unreachable, "key-1", "http://facility.example", "{}", 2000, 2000);
            Assert.fail();
        }
        catch (RhipCallException e) {
            Assert.assertFalse(e.isSent());
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }
}
