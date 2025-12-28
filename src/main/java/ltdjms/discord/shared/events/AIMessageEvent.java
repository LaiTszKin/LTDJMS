package ltdjms.discord.shared.events;

import java.time.Instant;

/**
 * AI 訊息事件，用於通知其他模組 AI 訊息已發送。
 *
 * @param guildId Discord 伺服器 ID
 * @param channelId Discord 頻道 ID
 * @param userId 使用者 ID
 * @param userMessage 使用者原始訊息
 * @param aiResponse AI 回應內容
 * @param timestamp 事件時間戳
 */
public record AIMessageEvent(
    long guildId,
    String channelId,
    String userId,
    String userMessage,
    String aiResponse,
    Instant timestamp)
    implements DomainEvent {}
