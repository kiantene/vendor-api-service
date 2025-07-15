package com.nextgen.gameaggregator.operator.wallet.bet;

import com.nextgen.gameaggregator.core.common.WebClientApiCaller;
import com.nextgen.gameaggregator.core.exception.Http4xxException;
import com.nextgen.gameaggregator.core.exception.Http5xxException;
import com.nextgen.gameaggregator.core.exception.VendorApiException;
import com.nextgen.gameaggregator.core.exception.VendorNetworkException;
import com.nextgen.gameaggregator.game.launcher.saba.GameLaunchResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.junit.jupiter.api.Assertions.*;

public class WebClientApiCallerTest {
    private MockWebServer mockWebServer;
    private WebClientApiCaller apiCaller;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        apiCaller = new WebClientApiCaller();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void test4xxClientError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Bad Request"));

        String baseUrl = mockWebServer.url("/").toString();

        VendorApiException ex = assertThrows(VendorApiException.class, () ->
                apiCaller.post(
                        baseUrl,
                        "/test",
                        MediaType.APPLICATION_JSON,
                        Map.of(),                                   // empty headers
                        Map.of("key", "value"),                     // requestBody
                        new ParameterizedTypeReference<>() {
                        }
                )
        );

        assertTrue(ex.getCause() instanceof Http4xxException);
    }

    @Test
    void test5xxServerError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        String baseUrl = mockWebServer.url("/").toString();

        VendorApiException ex = assertThrows(VendorApiException.class, () ->
                apiCaller.post(
                        baseUrl,
                        "/test",
                        MediaType.APPLICATION_JSON,
                        null,
                        Map.of("key", "value"),
                        new ParameterizedTypeReference<>() {
                        }
                )
        );

        assertTrue(ex.getCause() instanceof Http5xxException);
    }

    @Test
    void testInvalidJsonResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("not-a-json")
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        VendorApiException ex = assertThrows(VendorApiException.class, () ->
                apiCaller.post(
                        baseUrl,
                        "/test",
                        MediaType.APPLICATION_JSON,
                        null,
                        Map.of("key", "value"),
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }
                )
        );

        assertEquals("Invalid response format", ex.getMessage());
    }

    @Test
    void testNetworkFailure() throws IOException {
        mockWebServer.shutdown(); // force connection failure

        String baseUrl = mockWebServer.url("/").toString();

        assertThrows(VendorNetworkException.class, () ->
                apiCaller.post(
                        baseUrl,
                        "/test",
                        MediaType.APPLICATION_JSON,
                        null,
                        Map.of("key", "value"),
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }
                )
        );
    }

    @Test
    void testSlowServerResponsePrintRootCause() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"ok\"}")
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(6, TimeUnit.SECONDS)  // delay longer than timeout
        );

        String baseUrl = mockWebServer.url("/").toString();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apiCaller.post(
                    baseUrl,
                    "/test",
                    MediaType.APPLICATION_JSON,
                    Map.of(),
                    Map.of("key", "value"),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
        });

        Throwable rootCause = getRootCause(ex);
        System.out.println("Root cause exception: " + rootCause.getClass().getName());
        rootCause.printStackTrace();

        // Optional: fail test or just stop here to inspect
        fail("Check root cause above before updating exception handling.");
    }

    @Test
    void testResponseTimeoutViaCustomServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0); // random port
        int port = serverSocket.getLocalPort();

        // Thread to accept and delay
        new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                Thread.sleep(6000); // Delay longer than 5s responseTimeout
            } catch (Exception ignored) {}
        }).start();

        String baseUrl = "http://localhost:" + port;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apiCaller.post(
                    baseUrl,
                    "/test",
                    MediaType.APPLICATION_JSON,
                    Map.of(),
                    Map.of("key", "value"),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
        });

        System.out.println("Exception type: " + getRootCause(ex).getClass().getName());
    }

    @Test
    void testDecodingException_GameLaunchResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{ invalid_json")
                .setHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        VendorApiException ex = assertThrows(VendorApiException.class, () -> {
            apiCaller.post(
                    baseUrl,
                    "/test",
                    MediaType.APPLICATION_JSON,
                    Map.of(),
                    Map.of("key", "value"),
                    new ParameterizedTypeReference<GameLaunchResponse>() {}
            );
        });

        assertTrue(ex.getCause() instanceof DecodingException);
        System.out.println("Caught expected DecodingException: " + ex.getCause().getClass().getName());
    }

    @Test
    void testDecodingException_wrongFieldType() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{ \"error_code\": \"not_an_integer\", \"message\": \"ok\", \"Data\": \"some string\" }")
                .setHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        VendorApiException ex = assertThrows(VendorApiException.class, () -> {
            apiCaller.post(
                    baseUrl,
                    "/test",
                    MediaType.APPLICATION_JSON,
                    Map.of(),
                    Map.of("key", "value"),
                    new ParameterizedTypeReference<GameLaunchResponse>() {}
            );
        });

        assertTrue(ex.getCause() instanceof DecodingException);
        System.out.println("Caught expected DecodingException: " + ex.getCause().getClass().getName());
    }

    @Test
    void testWrongContentType() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("This is plain text, not JSON")
                .setHeader("Content-Type", "text/plain"));

        String baseUrl = mockWebServer.url("/").toString();

        VendorApiException ex = assertThrows(VendorApiException.class, () -> {
            apiCaller.post(
                    baseUrl,
                    "/test",
                    MediaType.APPLICATION_JSON,   // you expect JSON response
                    Map.of(),
                    Map.of("key", "value"),
                    new ParameterizedTypeReference<GameLaunchResponse>() {}
            );
        });

        assertTrue(ex.getCause() instanceof DecodingException);
        System.out.println("Caught expected DecodingException due to wrong Content-Type: " + ex.getCause().getClass().getName());
    }
}
