package ltdjms.discord.shop.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.EnvironmentConfig;
import ltdjms.discord.shared.Result;

@ExtendWith(MockitoExtension.class)
@DisplayName("EcpayCvsPaymentService 測試")
class EcpayCvsPaymentServiceTest {

  @Mock private EnvironmentConfig config;

  private EcpayCvsPaymentService service;

  @BeforeEach
  void setUp() {
    service = new EcpayCvsPaymentService(config);
  }

  @Test
  @DisplayName("金額小於等於零應回傳 INVALID_INPUT")
  void shouldReturnInvalidInputWhenAmountInvalid() {
    Result<EcpayCvsPaymentService.CvsPaymentCode, DomainError> result =
        service.generateCvsPaymentCode(0, "商品", "測試");

    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
  }

  @Test
  @DisplayName("綠界設定缺漏應回傳 INVALID_INPUT")
  void shouldReturnInvalidInputWhenConfigMissing() {
    when(config.getEcpayMerchantId()).thenReturn("");
    when(config.getEcpayHashKey()).thenReturn("");
    when(config.getEcpayHashIv()).thenReturn("");
    when(config.getEcpayReturnUrl()).thenReturn("");

    Result<EcpayCvsPaymentService.CvsPaymentCode, DomainError> result =
        service.generateCvsPaymentCode(100, "商品", "測試");

    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
    assertThat(result.getError().message()).contains("綠界金流尚未完成設定");
  }

  @Test
  @DisplayName("ReturnURL 應維持為設定值本身")
  void shouldKeepReturnUrlUnchanged() {
    String securedUrl = service.buildCallbackReturnUrl("https://example.com/ecpay/callback");

    assertThat(securedUrl).isEqualTo("https://example.com/ecpay/callback");
  }

  @Test
  @DisplayName("正式環境不可搭配官方測試金鑰")
  void shouldRejectOfficialStageCredentialsWhenProdModeEnabled() {
    when(config.getEcpayMerchantId()).thenReturn("3002607");
    when(config.getEcpayHashKey()).thenReturn("pwFHCqoQZGmho4w6");
    when(config.getEcpayHashIv()).thenReturn("EkRm7iFT261dpevs");
    when(config.getEcpayReturnUrl()).thenReturn("https://example.com/ecpay/callback");
    when(config.getEcpayStageMode()).thenReturn(false);

    Result<EcpayCvsPaymentService.CvsPaymentCode, DomainError> result =
        service.generateCvsPaymentCode(100, "商品", "測試");

    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
    assertThat(result.getError().message()).contains("官方測試");
  }

  @Test
  @DisplayName("綠界回傳 decrypt fail 時應提示檢查環境與金鑰")
  void shouldExplainLikelyEnvironmentMismatchWhenDecryptFailReturned() {
    when(config.getEcpayMerchantId()).thenReturn("merchant");
    when(config.getEcpayHashKey()).thenReturn("1234567890123456");
    when(config.getEcpayHashIv()).thenReturn("6543210987654321");
    when(config.getEcpayReturnUrl()).thenReturn("https://example.com/ecpay/callback");
    when(config.getEcpayStageMode()).thenReturn(true);
    when(config.getEcpayCvsExpireMinutes()).thenReturn(60);

    service =
        new EcpayCvsPaymentService(
            config,
            new FakeHttpClient(
                """
                {"TransCode":0,"TransMsg":"The parameter [Data] decrypt fail"}
                """),
            new com.fasterxml.jackson.databind.ObjectMapper(),
            Clock.fixed(Instant.parse("2026-04-09T00:00:00Z"), ZoneOffset.UTC));

    Result<EcpayCvsPaymentService.CvsPaymentCode, DomainError> result =
        service.generateCvsPaymentCode(100, "商品", "測試");

    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().message()).contains("ECPAY_STAGE_MODE");
    assertThat(result.getError().message()).contains("HashKey");
  }

  private static final class FakeHttpClient extends HttpClient {

    private final String responseBody;

    private FakeHttpClient(String responseBody) {
      this.responseBody = responseBody;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
      return Optional.empty();
    }

    @Override
    public Optional<Duration> connectTimeout() {
      return Optional.empty();
    }

    @Override
    public Redirect followRedirects() {
      return Redirect.NEVER;
    }

    @Override
    public Optional<ProxySelector> proxy() {
      return Optional.empty();
    }

    @Override
    public SSLContext sslContext() {
      return null;
    }

    @Override
    public SSLParameters sslParameters() {
      return new SSLParameters();
    }

    @Override
    public Optional<java.net.Authenticator> authenticator() {
      return Optional.empty();
    }

    @Override
    public Version version() {
      return Version.HTTP_1_1;
    }

    @Override
    public Optional<Executor> executor() {
      return Optional.empty();
    }

    @Override
    public <T> HttpResponse<T> send(
        HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
      return new FakeHttpResponse<>(request, responseBody);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
      return CompletableFuture.completedFuture(new FakeHttpResponse<>(request, responseBody));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler,
        HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
      return CompletableFuture.completedFuture(new FakeHttpResponse<>(request, responseBody));
    }
  }

  private static final class FakeHttpResponse<T> implements HttpResponse<T> {

    private final HttpRequest request;
    private final T body;

    @SuppressWarnings("unchecked")
    private FakeHttpResponse(HttpRequest request, String body) {
      this.request = request;
      this.body = (T) body;
    }

    @Override
    public int statusCode() {
      return 200;
    }

    @Override
    public HttpRequest request() {
      return request;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
      return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
      return HttpHeaders.of(Map.of(), (name, value) -> true);
    }

    @Override
    public T body() {
      return body;
    }

    @Override
    public Optional<SSLSession> sslSession() {
      return Optional.empty();
    }

    @Override
    public URI uri() {
      return request.uri();
    }

    @Override
    public HttpClient.Version version() {
      return HttpClient.Version.HTTP_1_1;
    }
  }
}
