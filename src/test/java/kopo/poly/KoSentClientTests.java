package kopo.poly;

import com.sun.net.httpserver.HttpServer;
import kopo.poly.service.impl.KoSentClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests against a LOCAL stub of the koSent FastAPI server.
 * The real Keras model is not running as part of the Java build.
 */
class KoSentClientTests {

    private HttpServer server;
    private KoSentClient client;
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/predict", exchange -> {
            String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestBody.set(json);
            if (exchange.getRequestURI().getPath().equals("/predict/batch")) {
                sendJson(exchange, 200, """
                    [{"index":0,"label":"pos","score":0.92,"score_str":"0.920000"},
                     {"index":1,"label":"neg","score":0.13,"score_str":"0.130000"}]
                    """);
            } else {
                sendJson(exchange, 200, """
                    {"label":"pos","score":0.87,"score_str":"0.870000"}
                    """);
            }
        });
        server.start();
        client = new KoSentClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void predictsKoreanSentenceWithActualHttpJson() {
        var prediction = client.predict("  정말 재미있는 영화  ");
        assertEquals("pos", prediction.label());
        assertEquals(0.87, prediction.score(), 1e-8);
        assertTrue(requestBody.get().contains("정말 재미있는 영화"));
        assertFalse(requestBody.get().contains("  정말 재미있는 영화  "));
    }

    @Test
    void batchPreservesOutputOrderAndPositiveScores() {
        var result = client.predictBatch(List.of("정말 좋았어요", "너무 지루했어요"));
        assertEquals(2, result.size());
        assertEquals("pos", result.get(0).label());
        assertEquals("neg", result.get(1).label());
        assertEquals(0.13, result.get(1).score(), 1e-8);
        assertTrue(requestBody.get().contains("texts"));
    }

    @Test
    void rejectsInvalidInputBeforeCallingServer() {
        assertThrows(IllegalArgumentException.class, () -> client.predict("   "));
        assertThrows(IllegalArgumentException.class, () -> client.predict("X".repeat(5001)));
        assertThrows(IllegalArgumentException.class, () -> client.predictBatch(List.of()));
    }

    private static void sendJson(com.sun.net.httpserver.HttpExchange exchange,
                                 int status, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        try (var out = exchange.getResponseBody()) {
            out.write(data);
        }
    }
}
