package ltdjms.discord.aichat.commands;

import ltdjms.discord.aiagent.services.AIAgentChannelConfigService;
import ltdjms.discord.aichat.services.AIChannelRestrictionService;

/**
 * Mention routing decision matrix for AI chat and AI agent channels.
 *
 * <p>Agent-enabled channels take priority over the general AI allowlist. If the agent config
 * service is unavailable, mention handling fails closed instead of falling back to AI chat.
 */
public final class AIChatMentionRoutingDecision {

  public enum Route {
    AGENT_ROUTE,
    AI_CHAT_ROUTE,
    DENY
  }

  public enum Source {
    AGENT_ENABLED,
    AI_ALLOWLIST,
    AGENT_CONFIG_UNAVAILABLE,
    AI_ALLOWLIST_DENIED
  }

  public record Decision(Route route, Source source, String detailMessage) {
    public java.util.Optional<String> detail() {
      return java.util.Optional.ofNullable(detailMessage);
    }
  }

  private final AIChannelRestrictionService channelRestrictionService;
  private final AIAgentChannelConfigService agentConfigService;

  public AIChatMentionRoutingDecision(
      AIChannelRestrictionService channelRestrictionService,
      AIAgentChannelConfigService agentConfigService) {
    this.channelRestrictionService = channelRestrictionService;
    this.agentConfigService = agentConfigService;
  }

  public Decision decide(long guildId, long channelId, long restrictionChannelId, long categoryId) {
    Boolean agentEnabled = resolveAgentEnabled(guildId, channelId);
    if (agentEnabled == null) {
      return new Decision(
          Route.DENY, Source.AGENT_CONFIG_UNAVAILABLE, "agent config service unavailable");
    }

    if (agentEnabled) {
      return new Decision(Route.AGENT_ROUTE, Source.AGENT_ENABLED, null);
    }

    boolean allowed =
        channelRestrictionService.isChannelAllowed(guildId, restrictionChannelId, categoryId);
    if (allowed) {
      return new Decision(Route.AI_CHAT_ROUTE, Source.AI_ALLOWLIST, null);
    }

    return new Decision(Route.DENY, Source.AI_ALLOWLIST_DENIED, "allowlist denied");
  }

  private Boolean resolveAgentEnabled(long guildId, long channelId) {
    if (agentConfigService == null) {
      return null;
    }

    try {
      return agentConfigService.isAgentEnabled(guildId, channelId);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
