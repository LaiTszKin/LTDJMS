package ltdjms.discord.shop.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.EnvironmentConfig;
import ltdjms.discord.shared.Result;

/** Queries ECPay trade status for reconciliation when callbacks are delayed or missing. */
public class EcpayTradeQueryService {

  private static final Logger LOG = LoggerFactory.getLogger(EcpayTradeQueryService.class);

  static final String STAGE_ENDPOINT =
      "https://payment-stage.ecpay.com.tw/Cashier/QueryTradeInfo/V5";
  static final String PROD_ENDPOINT = "https://payment.ecpay.com.tw/Cashier/QueryTradeInfo/V5";

  private final EnvironmentConfig config;
  private final HttpClient httpClient;
  private final Clock clock;

  public EcpayTradeQueryService(EnvironmentConfig config) {
    this(config, HttpClient.newHttpClient(), Clock.systemUTC());
  }

  EcpayTradeQueryService(EnvironmentConfig config, HttpClient httpClient, Clock clock) {
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  public Result<QueryTradeResult, DomainError> queryTrade(String orderNumber) {
    if (orderNumber == null || orderNumber.isBlank()) {
      return Result.err(DomainError.invalidInput("訂單編號不可為空"));
    }

    String merchantId = config.getEcpayMerchantId().trim();
    String hashKey = config.getEcpayHashKey().trim();
    String hashIv = config.getEcpayHashIv().trim();
    if (merchantId.isBlank() || hashKey.isBlank() || hashIv.isBlank()) {
      return Result.err(DomainError.invalidInput("綠界金流尚未完成設定（MerchantID/HashKey/HashIV）"));
    }

    try {
      Map<String, String> params = new LinkedHashMap<>();
      params.put("MerchantID", merchantId);
      params.put("MerchantTradeNo", orderNumber.trim());
      params.put("TimeStamp", String.valueOf(Instant.now(clock).getEpochSecond()));
      params.put("CheckMacValue", buildCheckMacValue(params, hashKey, hashIv));

      String requestBody = buildFormBody(params);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(config.getEcpayStageMode() ? STAGE_ENDPOINT : PROD_ENDPOINT))
              .timeout(Duration.ofSeconds(15))
              .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
        return Result.err(
            DomainError.unexpectedFailure("綠界查單失敗（HTTP " + response.statusCode() + "）", null));
      }

      Map<String, String> parsed = parseFormBody(response.body());
      String tradeStatus = textOrNull(parsed.get("TradeStatus"));
      String tradeNo = textOrNull(parsed.get("TradeNo"));
      String message =
          firstNonBlank(parsed.get("RtnMsg"), parsed.get("TradeMsg"), parsed.get("PaymentType"));
      long tradeAmt = parseLongOrDefault(parsed.get("TradeAmt"), -1L);
      boolean paid = "1".equals(tradeStatus);

      return Result.ok(
          new QueryTradeResult(orderNumber, paid, tradeStatus, tradeNo, tradeAmt, message));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Result.err(DomainError.unexpectedFailure("綠界查單被中斷", e));
    } catch (Exception e) {
      LOG.warn("Failed to query ECPay trade info: orderNumber={}", orderNumber, e);
      return Result.err(DomainError.unexpectedFailure("綠界查單失敗", e));
    }
  }

  private String buildFormBody(Map<String, String> params) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (!first) {
        builder.append('&');
      }
      builder
          .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
          .append('=')
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
      first = false;
    }
    return builder.toString();
  }

  private Map<String, String> parseFormBody(String body) {
    Map<String, String> values = new LinkedHashMap<>();
    if (body == null || body.isBlank()) {
      return values;
    }
    for (String pair : body.split("&")) {
      if (pair.isBlank()) {
        continue;
      }
      int idx = pair.indexOf('=');
      String key = idx >= 0 ? pair.substring(0, idx) : pair;
      String value = idx >= 0 ? pair.substring(idx + 1) : "";
      values.put(
          java.net.URLDecoder.decode(key, StandardCharsets.UTF_8),
          java.net.URLDecoder.decode(value, StandardCharsets.UTF_8));
    }
    return values;
  }

  static String buildCheckMacValue(Map<String, String> params, String hashKey, String hashIv)
      throws Exception {
    StringBuilder builder = new StringBuilder("HashKey=").append(hashKey);
    params.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry ->
                builder.append('&').append(entry.getKey()).append('=').append(entry.getValue()));
    builder.append("&HashIV=").append(hashIv);

    String encoded =
        URLEncoder.encode(builder.toString(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    encoded =
        encoded
            .replace("%2d", "-")
            .replace("%5f", "_")
            .replace("%2e", ".")
            .replace("%21", "!")
            .replace("%2a", "*")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%20", "+")
            .replace("%7e", "~");

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(encoded.getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder();
    for (byte b : hash) {
      hex.append(String.format(Locale.ROOT, "%02x", b));
    }
    return hex.toString().toUpperCase(Locale.ROOT);
  }

  private long parseLongOrDefault(String value, long fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private String textOrNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  public record QueryTradeResult(
      String orderNumber,
      boolean paid,
      String tradeStatus,
      String tradeNo,
      long tradeAmount,
      String message) {}
}
