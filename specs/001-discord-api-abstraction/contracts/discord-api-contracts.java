# Discord API 抽象層 - API 契約

**Feature**: Discord API 抽象層
**Date**: 2025-12-27
**Status**: Phase 1 - Design

---

## 概述

本文檔定義 Discord API 抽象層的 Java 介面契約。這些介面將作為業務邏輯與 JDA 實作之間的抽象層。

---

## 核心介面定義

### DiscordInteraction

```java
package ltdjms.discord.discord.domain;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;

/**
 * Discord 互動回應的統一抽象介面
 *
 * <p>此介面提供與 Discord 互動的所有必要操作，包括：
 * <ul>
 *   <li>發送訊息回應</li>
 *   <li>發送 Embed 訊息</li>
 *   <li>編輯現有訊息</li>
 *   <li>延遲回應（defer reply）</li>
 * </ul>
 *
 * @see DiscordInteractionImpl
 */
public interface DiscordInteraction {

    /**
     * 取得 Guild ID
     *
     * @return Guild ID
     */
    long getGuildId();

    /**
     * 取得使用者 ID
     *
     * @return 使用者 ID
     */
    long getUserId();

    /**
     * 檢查此互動是否為 ephemeral（僅使用者可見）
     *
     * @return true 如果是 ephemeral
     */
    boolean isEphemeral();

    /**
     * 回覆純文字訊息
     *
     * @param message 訊息內容
     */
    void reply(String message);

    /**
     * 回覆 Embed 訊息
     *
     * @param embed Embed 物件
     */
    void replyEmbed(MessageEmbed embed);

    /**
     * 編輯現有訊息的 Embed
     *
     * @param embed 新的 Embed 物件
     */
    void editEmbed(MessageEmbed embed);

    /**
     * 延遲回應（告知 Discord 將稍後回應）
     */
    void deferReply();

    /**
     * 取得底層 JDA InteractionHook
     *
     * @return InteractionHook 物件
     */
    InteractionHook getHook();

    /**
     * 檢查互動是否已被確認
     *
     * @return true 如果已被確認
     */
    boolean isAcknowledged();
}
```

---

### DiscordContext

```java
package ltdjms.discord.discord.domain;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import java.util.Optional;

/**
 * Discord 事件上下文的提取介面
 *
 * <p>此介面提供從 Discord 事件中提取上下文資訊的統一方法：
 * <ul>
 *   <li>Guild ID、User ID、Channel ID</li>
 *   <li>使用者 Mention 格式</li>
 *   <li>命令參數</li>
 * </ul>
 */
public interface DiscordContext {

    /**
     * 取得 Guild ID
     *
     * @return Guild ID
     */
    long getGuildId();

    /**
     * 取得使用者 ID
     *
     * @return 使用者 ID
     */
    long getUserId();

    /**
     * 取得頻道 ID
     *
     * @return 頻道 ID
     */
    long getChannelId();

    /**
     * 取得使用者的 Mention 格式（例如 "<@123456789>"）
     *
     * @return Mention 字串
     */
    String getUserMention();

    /**
     * 取得命令參數
     *
     * @param name 參數名稱
     * @return Optional 包含參數值，如果不存在則為空
     */
    Optional<OptionMapping> getOption(String name);

    /**
     * 取得命令參數作為 String
     *
     * @param name 參數名稱
     * @return Optional 包含字串值
     */
    Optional<String> getOptionAsString(String name);

    /**
     * 取得命令參數作為 long
     *
     * @param name 參數名稱
     * @return Optional 包含 long 值
     */
    Optional<Long> getOptionAsLong(String name);

    /**
     * 取得命令參數作為 User
     *
     * @param name 參數名稱
     * @return Optional 包含 User 物件
     */
    Optional<User> getOptionAsUser(String name);
}
```

---

### DiscordEmbedBuilder

```java
package ltdjms.discord.discord.domain;

import net.dv8tion.jda.api.entities.MessageEmbed;
import java.awt.Color;
import java.util.List;

/**
 * Discord Embed 建構器抽象介面
 *
 * <p>此介面提供流式 API 來建構 Discord Embed 訊息，
 * 並自動處理 Discord API 的長度限制。
 */
public interface DiscordEmbedBuilder {

    /**
     * 設定 Embed 標題
     *
     * @param title 標題（最多 256 字元）
     * @return this 建構器
     */
    DiscordEmbedBuilder setTitle(String title);

    /**
     * 設定 Embed 描述
     *
     * @param description 描述（最多 4096 字元）
     * @return this 建構器
     */
    DiscordEmbedBuilder setDescription(String description);

    /**
     * 設定 Embed 顏色
     *
     * @param color 顏色物件
     * @return this 建構器
     */
    DiscordEmbedBuilder setColor(Color color);

    /**
     * 新增一個欄位
     *
     * @param name 欄位名稱（最多 256 字元）
     * @param value 欄位值（最多 1024 字元）
     * @param inline 是否為內聯顯示
     * @return this 建構器
     */
    DiscordEmbedBuilder addField(String name, String value, boolean inline);

    /**
     * 設定 Footer
     *
     * @param text Footer 文字（最多 2048 字元）
     * @return this 建構器
     */
    DiscordEmbedBuilder setFooter(String text);

    /**
     * 建構最終的 MessageEmbed 物件
     *
     * @return MessageEmbed 物件
     */
    MessageEmbed build();

    /**
     * 建構多個 Embed（用於分頁）
     *
     * @param data 視圖資料
     * @return Embed 列表
     */
    List<MessageEmbed> buildPaginated(EmbedView data);
}
```

---

### DiscordSessionManager

```java
package ltdjms.discord.discord.domain;

import net.dv8tion.jda.api.interactions.InteractionHook;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Discord 互動 Session 管理器
 *
 * <p>此介面提供 Session 的註冊、檢索、更新和失效功能。
 * Session 用於管理跨多次互動的狀態（例如使用者面板）。
 *
 * @param <K> Session 類型（必須是 Enum）
 */
public interface DiscordSessionManager<K extends Enum<K>> {

    /**
     * 註冊一個新的 Session
     *
     * @param type Session 類型
     * @param guildId Guild ID
     * @param userId 使用者 ID
     * @param hook InteractionHook
     * @param metadata 元資料映射
     */
    void registerSession(K type, long guildId, long userId,
                        InteractionHook hook, Map<String, Object> metadata);

    /**
     * 取得 Session
     *
     * @param type Session 類型
     * @param guildId Guild ID
     * @param userId 使用者 ID
     * @return Optional 包含 Session，如果不存在或已過期則為空
     */
    Optional<Session<K>> getSession(K type, long guildId, long userId);

    /**
     * 清除指定的 Session
     *
     * @param type Session 類型
     * @param guildId Guild ID
     * @param userId 使用者 ID
     */
    void clearSession(K type, long guildId, long userId);

    /**
     * 清除所有過期的 Session
     */
    void clearExpiredSessions();

    /**
     * Session 記錄
     *
     * @param <K> Session 類型
     */
    record Session<K>(
        K type,
        InteractionHook hook,
        Instant createdAt,
        long ttlSeconds,
        Map<String, Object> metadata
    ) {
        /**
         * 檢查 Session 是否已過期
         *
         * @return true 如果已過期
         */
        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(ttlSeconds));
        }
    }
}
```

---

### DiscordButtonEvent

```java
package ltdjms.discord.discord.domain;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import java.util.List;

/**
 * Discord 按鈕互動事件抽象
 *
 * <p>此介面擴展 DiscordInteraction，增加按鈕特定的操作。
 */
public interface DiscordButtonEvent extends DiscordInteraction {

    /**
     * 取得按鈕 ID
     *
     * @return 按鈕識別碼
     */
    String getButtonId();

    /**
     * 編輯訊息的 Embed
     *
     * @param embed 新的 Embed 物件
     */
    void editEmbed(MessageEmbed embed);

    /**
     * 編輯訊息的元件（按鈕、選擇選單等）
     *
     * @param components ActionRow 列表
     */
    void editComponents(List<ActionRow> components);
}
```

---

### DiscordModalEvent

```java
package ltdjms.discord.discord.domain;

/**
 * Discord Modal 互動事件抽象
 *
 * <p>此介面擴展 DiscordInteraction，增加 Modal 特定的操作。
 */
public interface DiscordModalEvent extends DiscordInteraction {

    /**
     * 取得 Modal ID
     *
     * @return Modal 識別碼
     */
    String getModalId();

    /**
     * 取得指定欄位的值
     *
     * @param fieldId 欄位 ID
     * @return 欄位值
     */
    String getValue(String fieldId);

    /**
     * 取得指定欄位的值作為 String
     *
     * @param fieldId 欄位 ID
     * @return Optional 包含字串值
     */
    Optional<String> getValueAsString(String fieldId);
}
```

---

## 值物件定義

### EmbedView

```java
package ltdjms.discord.discord.domain;

import net.dv8tion.jda.api.entities.MessageEmbed;
import java.awt.Color;
import java.util.List;

/**
 * Embed 視圖的不可變資料結構
 *
 * @param title 標題
 * @param description 描述
 * @param color 顏色
 * @param fields 欄位列表
 * @param footer Footer 文字
 */
public record EmbedView(
    String title,
    String description,
    Color color,
    List<FieldView> fields,
    String footer
) {
    /**
     * 欄位視圖
     *
     * @param name 名稱
     * @param value 值
     * @param inline 是否內聯顯示
     */
    public record FieldView(
        String name,
        String value,
        boolean inline
    ) {
        /**
         * 轉換為 JDA MessageEmbed.Field
         *
         * @return MessageEmbed.Field 物件
         */
        public MessageEmbed.Field toJdaField() {
            return new MessageEmbed.Field(name, value, inline);
        }
    }
}
```

### ButtonView

```java
package ltdjms.discord.discord.domain;

import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

/**
 * 按鈕視圖的不可變資料結構
 *
 * @param id 按鈕識別碼
 * @param label 標籤文字
 * @param style 按鈕樣式
 * @param disabled 是否停用
 */
public record ButtonView(
    String id,
    String label,
    ButtonStyle style,
    boolean disabled
) {
    /**
     * 轉換為 JDA Button
     *
     * @return Button 物件
     */
    public net.dv8tion.jda.api.interactions.components.buttons.Button toJdaButton() {
        return net.dv8tion.jda.api.interactions.components.buttons.Button
            .of(style, id)
            .withLabel(label)
            .withDisabled(disabled);
    }
}
```

---

## 錯誤處理契約

### DiscordError

```java
package ltdjms.discord.discord.domain;

/**
 * Discord API 特定錯誤
 *
 * @param category 錯誤類別
 * @param message 錯誤訊息
 * @param cause 原始異常（可為 null）
 */
public record DiscordError(
    Category category,
    String message,
    Throwable cause
) {
    /**
     * 錯誤類別
     */
    public enum Category {
        /** 3 秒內未回應，Interaction 失效 */
        INTERACTION_TIMEOUT,

        /** Hook 過期（超過 15 分鐘） */
        HOOK_EXPIRED,

        /** 訊息已刪除或無法存取 */
        UNKNOWN_MESSAGE,

        /** 超過 Rate Limit */
        RATE_LIMITED,

        /** 缺少必要權限 */
        MISSING_PERMISSIONS,

        /** 無效的 Component ID */
        INVALID_COMPONENT_ID
    }

    /**
     * 建立逾時錯誤
     */
    public static DiscordError interactionTimeout(String interactionId) {
        return new DiscordError(
            Category.INTERACTION_TIMEOUT,
            "Interaction " + interactionId + " 已超時",
            null
        );
    }

    /**
     * 建立未知訊息錯誤
     */
    public static DiscordError unknownMessage(String messageId) {
        return new DiscordError(
            Category.UNKNOWN_MESSAGE,
            "訊息 " + messageId + " 不存在或已刪除",
            null
        );
    }

    /**
     * 建立速率限制錯誤
     */
    public static DiscordError rateLimited(int retryAfter) {
        return new DiscordError(
            Category.RATE_LIMITED,
            "請求過於頻繁，請在 " + retryAfter + " 秒後重試",
            null
        );
    }
}
```

---

## 使用範例

### 在 Command Handler 中使用

```java
public class BalanceCommandHandler {

    private final DiscordContextFactory contextFactory;
    private final DiscordInteractionFactory interactionFactory;
    private final BalanceService balanceService;

    public void handle(SlashCommandInteractionEvent event) {
        // 使用抽象介面提取上下文
        DiscordContext context = contextFactory.fromSlashEvent(event);

        // 執行業務邏輯
        long guildId = context.getGuildId();
        long userId = context.getUserId();

        Result<BalanceView, DomainError> result =
            balanceService.getBalance(guildId, userId);

        if (result.isErr()) {
            // 使用抽象介面回應錯誤
            DiscordInteraction interaction =
                interactionFactory.fromSlashEvent(event);
            interaction.reply("❌ " + result.getError().message());
            return;
        }

        // 使用建構器建立 Embed
        BalanceView view = result.getValue();
        MessageEmbed embed = DiscordEmbedBuilder.create()
            .setTitle("💰 餘額查詢")
            .setDescription(context.getUserMention() + " 的餘額")
            .addField("貨幣", view.formatCurrency(), true)
            .setColor(new Color(0x5865F2))
            .build();

        DiscordInteraction interaction =
            interactionFactory.fromSlashEvent(event);
        interaction.replyEmbed(embed);
    }
}
```

---

**Phase 1 狀態**: ✅ COMPLETE
