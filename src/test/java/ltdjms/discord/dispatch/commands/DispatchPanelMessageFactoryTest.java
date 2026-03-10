package ltdjms.discord.dispatch.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import net.dv8tion.jda.api.entities.MessageEmbed;

class DispatchPanelMessageFactoryTest {

  @Test
  @DisplayName("空歷史清單應顯示無歷史訂單訊息")
  void buildHistoryEmbedShouldShowEmptyState() {
    MessageEmbed embed = DispatchPanelMessageFactory.buildHistoryEmbed(List.of());

    assertThat(embed.getTitle()).isEqualTo("📜 護航派單歷史");
    assertThat(embed.getDescription()).isEqualTo("目前沒有歷史訂單。");
  }

  @Test
  @DisplayName("歷史清單應保留訂單狀態與成員資訊")
  void buildHistoryEmbedShouldRenderOrderStatusAndMentions() {
    Instant createdAt = Instant.parse("2026-03-10T10:15:30Z");
    EscortDispatchOrder order =
        EscortDispatchOrder.createPending("ESC-20260310-ABC123", 1L, 10L, 20L, 30L)
            .withConfirmed(createdAt.plusSeconds(60));

    MessageEmbed embed = DispatchPanelMessageFactory.buildHistoryEmbed(List.of(order));

    assertThat(embed.getDescription()).contains("ESC-20260310-ABC123");
    assertThat(embed.getDescription()).contains("護航者已確認");
    assertThat(embed.getDescription()).contains("<@20>");
    assertThat(embed.getDescription()).contains("<@30>");
  }
}
