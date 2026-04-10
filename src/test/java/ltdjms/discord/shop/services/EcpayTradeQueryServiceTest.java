package ltdjms.discord.shop.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.shared.EnvironmentConfig;
import ltdjms.discord.shared.Result;

@ExtendWith(MockitoExtension.class)
@DisplayName("EcpayTradeQueryService 測試")
class EcpayTradeQueryServiceTest {

  @Mock private EnvironmentConfig config;
  @Mock private HttpClient httpClient;
  @Mock private HttpResponse<String> response;

  @Test
  @DisplayName("查單成功時應解析 paid 狀態並打到 stage endpoint")
  void shouldParsePaidResponseFromStageEndpoint() throws Exception {
    when(config.getEcpayMerchantId()).thenReturn("2000132");
    when(config.getEcpayHashKey()).thenReturn("5294y06JbISpM5x9");
    when(config.getEcpayHashIv()).thenReturn("v77hoKGq4kWxNNIS");
    when(config.getEcpayStageMode()).thenReturn(true);
    when(httpClient.send(
            any(HttpRequest.class),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
        .thenReturn(response);
    when(response.statusCode()).thenReturn(200);
    when(response.body())
        .thenReturn(
            "TradeStatus=1&TradeNo=2204012345&TradeAmt=1200&RtnMsg=%E4%BB%98%E6%AC%BE%E6%88%90%E5%8A%9F");

    EcpayTradeQueryService service =
        new EcpayTradeQueryService(
            config, httpClient, Clock.fixed(Instant.parse("2026-04-11T12:00:00Z"), ZoneOffset.UTC));

    Result<EcpayTradeQueryService.QueryTradeResult, ltdjms.discord.shared.DomainError> result =
        service.queryTrade("FD260411000003");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue().paid()).isTrue();
    assertThat(result.getValue().tradeStatus()).isEqualTo("1");
    assertThat(result.getValue().tradeNo()).isEqualTo("2204012345");
    assertThat(result.getValue().tradeAmount()).isEqualTo(1200L);

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient)
        .send(
            requestCaptor.capture(),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    assertThat(requestCaptor.getValue().uri().toString())
        .isEqualTo(EcpayTradeQueryService.STAGE_ENDPOINT);
  }

  @Test
  @DisplayName("HTTP 非 200 時應回傳錯誤")
  void shouldReturnErrorWhenHttpStatusIsNot200() throws Exception {
    when(config.getEcpayMerchantId()).thenReturn("2000132");
    when(config.getEcpayHashKey()).thenReturn("5294y06JbISpM5x9");
    when(config.getEcpayHashIv()).thenReturn("v77hoKGq4kWxNNIS");
    when(config.getEcpayStageMode()).thenReturn(false);
    when(httpClient.send(
            any(HttpRequest.class),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
        .thenReturn(response);
    when(response.statusCode()).thenReturn(500);

    EcpayTradeQueryService service =
        new EcpayTradeQueryService(
            config, httpClient, Clock.fixed(Instant.parse("2026-04-11T12:00:00Z"), ZoneOffset.UTC));

    Result<EcpayTradeQueryService.QueryTradeResult, ltdjms.discord.shared.DomainError> result =
        service.queryTrade("FD260411000003");

    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().message()).contains("HTTP 500");
  }

  @Test
  @DisplayName("空白訂單編號應直接拒絕")
  void shouldRejectBlankOrderNumber() {
    EcpayTradeQueryService service =
        new EcpayTradeQueryService(
            config, httpClient, Clock.fixed(Instant.parse("2026-04-11T12:00:00Z"), ZoneOffset.UTC));

    Result<EcpayTradeQueryService.QueryTradeResult, ltdjms.discord.shared.DomainError> result =
        service.queryTrade("  ");

    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().message()).contains("訂單編號不可為空");
  }
}
