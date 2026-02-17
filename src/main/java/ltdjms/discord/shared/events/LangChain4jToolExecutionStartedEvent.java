package ltdjms.discord.shared.events;

import java.time.Instant;

/**
 * LangChain4J 工具執行開始事件。
 *
 * <p>當 LangChain4J 即將執行工具時發布，用於即時 UI 進度通知。
 *
 * @param guildId 伺服器 ID
 * @param channelId 頻道 ID
 * @param userId 用戶 ID
 * @param toolName 工具名稱
 * @param timestamp 事件時間戳
 */
public record LangChain4jToolExecutionStartedEvent(
    long guildId, long channelId, long userId, String toolName, Instant timestamp)
    implements DomainEvent {}
