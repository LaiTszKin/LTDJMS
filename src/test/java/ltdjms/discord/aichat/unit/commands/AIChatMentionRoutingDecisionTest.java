package ltdjms.discord.aichat.unit.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.aiagent.services.AIAgentChannelConfigService;
import ltdjms.discord.aichat.commands.AIChatMentionRoutingDecision;
import ltdjms.discord.aichat.services.AIChannelRestrictionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIChatMentionRoutingDecision 決策矩陣")
class AIChatMentionRoutingDecisionTest {

  @Mock private AIChannelRestrictionService channelRestrictionService;
  @Mock private AIAgentChannelConfigService agentConfigService;

  @Test
  @DisplayName("Agent 啟用時應優先走 Agent 路徑且不檢查 allowlist")
  void shouldPreferAgentRouteWhenAgentEnabled() {
    long guildId = 123L;
    long channelId = 456L;
    long restrictionChannelId = 789L;
    long categoryId = 100L;

    when(agentConfigService.isAgentEnabled(guildId, channelId)).thenReturn(true);

    AIChatMentionRoutingDecision decision =
        new AIChatMentionRoutingDecision(channelRestrictionService, agentConfigService);

    AIChatMentionRoutingDecision.Decision result =
        decision.decide(guildId, channelId, restrictionChannelId, categoryId);

    assertThat(result.route()).isEqualTo(AIChatMentionRoutingDecision.Route.AGENT_ROUTE);
    assertThat(result.source()).isEqualTo(AIChatMentionRoutingDecision.Source.AGENT_ENABLED);
    verify(channelRestrictionService, never()).isChannelAllowed(anyLong(), anyLong(), anyLong());
  }

  @Test
  @DisplayName("Agent 未啟用時應回到 allowlist 決策")
  void shouldUseAllowlistWhenAgentDisabled() {
    long guildId = 123L;
    long channelId = 456L;
    long restrictionChannelId = 789L;
    long categoryId = 100L;

    when(agentConfigService.isAgentEnabled(guildId, channelId)).thenReturn(false);
    when(channelRestrictionService.isChannelAllowed(guildId, restrictionChannelId, categoryId))
        .thenReturn(true);

    AIChatMentionRoutingDecision decision =
        new AIChatMentionRoutingDecision(channelRestrictionService, agentConfigService);

    AIChatMentionRoutingDecision.Decision result =
        decision.decide(guildId, channelId, restrictionChannelId, categoryId);

    assertThat(result.route()).isEqualTo(AIChatMentionRoutingDecision.Route.AI_CHAT_ROUTE);
    assertThat(result.source()).isEqualTo(AIChatMentionRoutingDecision.Source.AI_ALLOWLIST);
    verify(channelRestrictionService).isChannelAllowed(guildId, restrictionChannelId, categoryId);
  }

  @Test
  @DisplayName("Agent 設定服務不可用時應 fail closed")
  void shouldDenyWhenAgentConfigUnavailable() {
    long guildId = 123L;
    long channelId = 456L;
    long restrictionChannelId = 789L;
    long categoryId = 100L;

    doThrow(new IllegalStateException("agent config unavailable"))
        .when(agentConfigService)
        .isAgentEnabled(guildId, channelId);

    AIChatMentionRoutingDecision decision =
        new AIChatMentionRoutingDecision(channelRestrictionService, agentConfigService);

    AIChatMentionRoutingDecision.Decision result =
        decision.decide(guildId, channelId, restrictionChannelId, categoryId);

    assertThat(result.route()).isEqualTo(AIChatMentionRoutingDecision.Route.DENY);
    assertThat(result.source())
        .isEqualTo(AIChatMentionRoutingDecision.Source.AGENT_CONFIG_UNAVAILABLE);
    verify(channelRestrictionService, never()).isChannelAllowed(anyLong(), anyLong(), anyLong());
  }
}
