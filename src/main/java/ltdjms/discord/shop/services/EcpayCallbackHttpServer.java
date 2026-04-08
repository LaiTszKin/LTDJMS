package ltdjms.discord.shop.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ltdjms.discord.shared.EnvironmentConfig;

/** Embedded HTTP server for receiving ECPay ReturnURL callback pushes. */
public class EcpayCallbackHttpServer {

  private static final Logger LOG = LoggerFactory.getLogger(EcpayCallbackHttpServer.class);
  private static final int CALLBACK_WORKER_THREADS = 8;
  private static final int MAX_CALLBACK_BODY_BYTES = 64 * 1024;
  private static final String LANDING_PAGE_RESOURCE_PATH = "/web/index.html";
  private static final String LANDING_PAGE_INDEX_PATH = "/index.html";

  private final EnvironmentConfig config;
  private final FiatPaymentCallbackService callbackService;
  private HttpServer server;
  private ExecutorService executor;

  public EcpayCallbackHttpServer(
      EnvironmentConfig config, FiatPaymentCallbackService callbackService) {
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.callbackService =
        Objects.requireNonNull(callbackService, "callbackService must not be null");
  }

  public synchronized void start() {
    if (server != null) {
      return;
    }
    if (config.getEcpayReturnUrl() == null || config.getEcpayReturnUrl().isBlank()) {
      LOG.info("Skip starting ECPay callback server because ECPAY_RETURN_URL is not configured");
      return;
    }
    String bindHost = sanitizeBindHost(config.getEcpayCallbackBindHost());
    int bindPort = normalizeBindPort(config.getEcpayCallbackBindPort());
    String callbackPath = normalizePath(config.getEcpayCallbackPath());
    if ("/".equals(callbackPath) || LANDING_PAGE_INDEX_PATH.equals(callbackPath)) {
      throw new IllegalStateException("ECPAY callback 路徑不可與首頁路徑衝突");
    }
    if (config.getEcpayStageMode() && isPubliclyExposedBindHost(bindHost)) {
      throw new IllegalStateException(
          "ECPAY_STAGE_MODE=true 時，callback server 不可綁定公開位址。"
              + "請改用 127.0.0.1 / localhost / ::1，或切換正式環境設定。");
    }

    try {
      server = HttpServer.create(new InetSocketAddress(bindHost, bindPort), 0);
      server.createContext(callbackPath, this::handleCallbackRequest);
      server.createContext("/", this::handleLandingPageRequest);
      executor = Executors.newFixedThreadPool(CALLBACK_WORKER_THREADS);
      server.setExecutor(executor);
      server.start();
      LOG.info(
          "Embedded web server started: host={}, port={}, landingPath=/, callbackPath={}",
          bindHost,
          bindPort,
          callbackPath);
    } catch (IOException e) {
      LOG.error(
          "Failed to start ECPay callback server: host={}, port={}, path={}",
          bindHost,
          bindPort,
          callbackPath,
          e);
      shutdownExecutor();
      throw new IllegalStateException("無法啟動綠界回推伺服器", e);
    }
  }

  public synchronized void stop() {
    if (server == null) {
      return;
    }
    server.stop(0);
    server = null;
    shutdownExecutor();
    LOG.info("ECPay callback server stopped");
  }

  private void handleCallbackRequest(HttpExchange exchange) throws IOException {
    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
      writeResponse(exchange, 405, "Method Not Allowed");
      return;
    }

    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
    String requestBody;
    try (InputStream requestBodyStream = exchange.getRequestBody()) {
      requestBody = readRequestBodyWithLimit(requestBodyStream, MAX_CALLBACK_BODY_BYTES);
    } catch (PayloadTooLargeException e) {
      LOG.warn("ECPay callback payload exceeded limit: limit={} bytes", MAX_CALLBACK_BODY_BYTES);
      writeResponse(exchange, 413, "Payload Too Large");
      return;
    }

    FiatPaymentCallbackService.CallbackResult result =
        callbackService.handleCallback(requestBody, contentType);
    writeResponse(exchange, result.httpStatus(), result.responseBody());
  }

  private void handleLandingPageRequest(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
      writeResponse(exchange, 405, "Method Not Allowed");
      return;
    }

    String requestPath = exchange.getRequestURI().getPath();
    if (!"/".equals(requestPath) && !LANDING_PAGE_INDEX_PATH.equals(requestPath)) {
      writeResponse(exchange, 404, "Not Found");
      return;
    }

    byte[] response = loadLandingPageBytes();
    if (response == null) {
      writeResponse(exchange, 404, "Not Found");
      return;
    }

    writeResponse(exchange, 200, response, "text/html; charset=UTF-8");
  }

  private void writeResponse(HttpExchange exchange, int statusCode, String body)
      throws IOException {
    writeResponse(
        exchange, statusCode, body.getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8");
  }

  private void writeResponse(HttpExchange exchange, int statusCode, byte[] body, String contentType)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(statusCode, -1);
      exchange.close();
      return;
    }
    exchange.sendResponseHeaders(statusCode, body.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(body);
    }
  }

  private byte[] loadLandingPageBytes() throws IOException {
    try (InputStream inputStream =
        EcpayCallbackHttpServer.class.getResourceAsStream(LANDING_PAGE_RESOURCE_PATH)) {
      if (inputStream == null) {
        LOG.warn("Landing page resource not found: {}", LANDING_PAGE_RESOURCE_PATH);
        return null;
      }
      return inputStream.readAllBytes();
    }
  }

  private String sanitizeBindHost(String bindHost) {
    if (bindHost == null || bindHost.isBlank()) {
      return "127.0.0.1";
    }
    return bindHost.trim();
  }

  private int normalizeBindPort(int port) {
    if (port < 1 || port > 65535) {
      return 8085;
    }
    return port;
  }

  private String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/ecpay/callback";
    }
    String normalized = path.trim();
    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }
    return normalized;
  }

  private String readRequestBodyWithLimit(InputStream inputStream, int maxBytes)
      throws IOException, PayloadTooLargeException {
    byte[] buffer = new byte[4096];
    int totalBytes = 0;

    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      int read;
      while ((read = inputStream.read(buffer)) != -1) {
        totalBytes += read;
        if (totalBytes > maxBytes) {
          throw new PayloadTooLargeException();
        }
        output.write(buffer, 0, read);
      }
      return output.toString(StandardCharsets.UTF_8);
    }
  }

  private boolean isPubliclyExposedBindHost(String bindHost) {
    String normalized = bindHost.trim().toLowerCase();
    return !normalized.equals("127.0.0.1")
        && !normalized.equals("localhost")
        && !normalized.equals("::1")
        && !normalized.equals("[::1]");
  }

  private void shutdownExecutor() {
    if (executor == null) {
      return;
    }
    executor.shutdown();
    executor = null;
  }

  private static final class PayloadTooLargeException extends Exception {}
}
