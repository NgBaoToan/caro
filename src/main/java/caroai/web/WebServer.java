package caroai.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Small same-origin web adapter; the desktop entry point remains available. */
public final class WebServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(WebServer.class.getName());
    private static final int MAX_BODY = 32_768;
    private static final Map<String, String> ASSETS = Map.of(
            "/", "index.html", "/index.html", "index.html", "/app.js", "app.js", "/style.css", "style.css",
            "/game-file.js", "game-file.js", "/sound.js", "sound.js");
    private final HttpServer server;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore searches = new Semaphore(4);

    public WebServer(String host, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 32);
        server.setExecutor(workers);
        server.createContext("/", this::handle);
        server.start();
    }

    public int port() { return server.getAddress().getPort(); }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Content-Security-Policy",
                    "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
                            + "connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/healthz") && exchange.getRequestMethod().equals("GET")) {
                send(exchange, 200, "text/plain", "ok");
                return;
            }
            if (path.equals("/api/game")) {
                api(exchange);
                return;
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405, "text/plain", "Method not allowed");
                return;
            }
            String asset = ASSETS.get(path);
            if (path.equals("/move.wav")) {
                try (var stream = WebServer.class.getResourceAsStream("/caroai/audio/move.wav")) {
                    if (stream == null) throw new IOException("Missing move sound");
                    byte[] bytes = stream.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "audio/wav");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
                return;
            }
            if (asset == null) {
                send(exchange, 404, "text/plain", "Not found");
                return;
            }
            try (var stream = WebServer.class.getResourceAsStream("/web/" + asset)) {
                if (stream == null) throw new IOException("Missing web asset: " + asset);
                String type = asset.endsWith(".js") ? "text/javascript"
                        : asset.endsWith(".css") ? "text/css" : "text/html";
                send(exchange, 200, type, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // Disconnecting clients must not affect other games.
            LOG.log(Level.FINE, "HTTP connection closed", e);
        }
    }

    private void api(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.getResponseHeaders().set("Allow", "POST");
            error(exchange, 405, "Chỉ hỗ trợ POST.");
            return;
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (origin != null && !sameOrigin(origin, host)) {
            error(exchange, 403, "Nguồn yêu cầu không hợp lệ.");
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].trim()
                .equalsIgnoreCase("application/x-www-form-urlencoded")) {
            error(exchange, 415, "Định dạng yêu cầu không hợp lệ.");
            return;
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
        if (body.length > MAX_BODY) {
            error(exchange, 413, "Ván cờ quá lớn.");
            return;
        }
        if (!searches.tryAcquire()) {
            error(exchange, 429, "Máy chủ đang bận. Hãy thử lại sau vài giây.");
            return;
        }
        try {
            Map<String, String> form = new HashMap<>();
            for (String field : new String(body, StandardCharsets.UTF_8).split("&")) {
                String[] pair = field.split("=", 2);
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                if (form.putIfAbsent(key, value) != null) throw new IllegalArgumentException();
            }
            send(exchange, 200, "application/json", WebGame.respond(form));
        } catch (IllegalArgumentException e) {
            error(exchange, 400, "Nước đi hoặc cấu hình ván cờ không hợp lệ.");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Web game failed", e);
            error(exchange, 500, "Không tính được nước đi. Bạn có thể thử lại hoặc hoàn tác.");
        } finally {
            searches.release();
        }
    }

    private static boolean sameOrigin(String origin, String host) {
        try {
            URI uri = URI.create(origin);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getRawAuthority() != null && uri.getRawAuthority().equalsIgnoreCase(host);
        } catch (IllegalArgumentException e) { return false; }
    }

    private static void error(HttpExchange exchange, int code, String message) throws IOException {
        send(exchange, code, "application/json", "{\"error\":\"" + message + "\"}");
    }

    private static void send(HttpExchange exchange, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override public void close() {
        server.stop(0);
        workers.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        String host = System.getenv().getOrDefault("HOST", "127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8088"));
        WebServer app = new WebServer(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        LOG.info("CaroAI Web: http://" + host + ":" + app.port());
    }
}
