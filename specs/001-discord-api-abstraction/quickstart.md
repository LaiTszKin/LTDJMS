# Quickstart Guide: Discord API 抽象層

**Feature**: Discord API 抽象層
**Date**: 2025-12-27
**Branch**: 001-discord-api-abstraction

---

## 概述

本文檔提供 Discord API 抽象層的快速入門指南，說明如何使用抽象介面來實作 Discord 互動功能，而無需直接依賴 JDA API。

---

## 前置準備

### 依賴設定

專案已包含必要的依賴：

```xml
<!-- JDA (Discord API) -->
<dependency>
    <groupId>net.dv8tion</groupId>
    <artifactId>JDA</artifactId>
    <version>5.2.2</version>
</dependency>

<!-- Dagger 2 (依賴注入) -->
<dependency>
    <groupId>com.google.dagger</groupId>
    <artifactId>dagger</artifactId>
    <version>2.52</version>
</dependency>
```

### 模組結構

```
src/main/java/ltdjms/discord/
├── discord/                    # NEW: Discord API 抽象層
│   ├── domain/                 # 抽象介面定義
│   ├── services/               # JDA 實作
│   ├── mock/                   # 測試用 Mock 實作
│   └── adapter/                # JDA 事件適配器
└── shared/di/
    └── DiscordModule.java      # Dagger 模組
```

---

## 基本使用

### 1. 使用 DiscordInteraction 回應訊息

```java
import ltdjms.discord.discord.domain.DiscordInteraction;
import ltdjms.discord.discord.services.JdaDiscordInteraction;

public class ExampleCommandHandler {

    private final BalanceService balanceService;

    public void handle(SlashCommandInteractionEvent event) {
        // 建立抽象介面
        DiscordInteraction interaction =
            new JdaDiscordInteraction(event);

        // 使用抽象介面回應
        long guildId = interaction.getGuildId();
        long userId = interaction.getUserId();

        Result<BalanceView, DomainError> result =
            balanceService.getBalance(guildId, userId);

        if (result.isErr()) {
            interaction.reply("❌ " + result.getError().message());
        } else {
            BalanceView view = result.getValue();
            interaction.reply("餘額: " + view.getBalance());
        }
    }
}
```

### 2. 使用 DiscordEmbedBuilder 建構 Embed

```java
import ltdjms.discord.discord.domain.DiscordEmbedBuilder;
import ltdjms.discord.discord.services.JdaDiscordEmbedBuilder;

public class ExampleEmbedBuilder {

    public MessageEmbed buildBalanceEmbed(BalanceView view, String userMention) {
        return DiscordEmbedBuilder.create()
            .setTitle("💰 餘額查詢")
            .setDescription(userMention + " 的帳戶餘額")
            .addField("貨幣", view.formatCurrency(), true)
            .addField("代幣", view.formatTokens(), true)
            .setColor(new Color(0x5865F2))
            .setFooter("查詢時間: " + Instant.now())
            .build();
    }
}
```

### 3. 使用 DiscordSessionManager 管理面板

```java
import ltdjms.discord.discord.domain.DiscordSessionManager;
import ltdjms.discord.discord.services.InteractionSessionManager;
import ltdjms.discord.discord.domain.SessionType;

public class UserPanelHandler {

    private final DiscordSessionManager<SessionType> sessionManager =
        new InteractionSessionManager<>();

    public void openPanel(ButtonInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        // 註冊面板 Session
        sessionManager.registerSession(
            SessionType.USER_PANEL,
            guildId,
            userId,
            event.getHook(),
            Map.of("page", 1)
        );

        // 發送面板訊息
        MessageEmbed embed = buildPanelEmbed(guildId, userId);
        event.replyEmbeds(embed).setComponents(buildButtons()).queue();
    }

    public void updatePanel(long guildId, long userId, MessageEmbed newEmbed) {
        // 檢索 Session
        Optional<DiscordSessionManager.Session<SessionType>> sessionOpt =
            sessionManager.getSession(SessionType.USER_PANEL, guildId, userId);

        if (sessionOpt.isEmpty()) {
            // Session 已過期
            return;
        }

        // 更新面板
        DiscordSessionManager.Session<SessionType> session = sessionOpt.get();
        session.hook().editMessageEmbeds(newEmbed).queue();
    }
}
```

---

## 遷移現有代碼

### 遷移前（直接使用 JDA）

```java
public class OldBalanceCommandHandler {

    public void handle(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();
        String mention = event.getUser().getAsMention();

        Result<BalanceView, DomainError> result =
            balanceService.getBalance(guildId, userId);

        if (result.isErr()) {
            event.reply("❌ " + result.getError().message())
                .setEphemeral(true).queue();
        } else {
            BalanceView view = result.getValue();
            EmbedBuilder builder = new EmbedBuilder()
                .setTitle("💰 餘額查詢")
                .setDescription(mention + " 的帳戶餘額")
                .addField("貨幣", view.formatCurrency(), true);
            event.replyEmbeds(builder.build()).queue();
        }
    }
}
```

### 遷移後（使用抽象層）

```java
public class NewBalanceCommandHandler {

    private final DiscordInteractionFactory interactionFactory;

    public void handle(SlashCommandInteractionEvent event) {
        // 使用抽象介面
        DiscordInteraction interaction =
            interactionFactory.fromSlashEvent(event);
        DiscordContext context =
            interactionFactory.toContext(event);

        Result<BalanceView, DomainError> result =
            balanceService.getBalance(
                context.getGuildId(),
                context.getUserId()
            );

        if (result.isErr()) {
            interaction.reply("❌ " + result.getError().message());
        } else {
            BalanceView view = result.getValue();
            MessageEmbed embed = DiscordEmbedBuilder.create()
                .setTitle("💰 餘額查詢")
                .setDescription(context.getUserMention() + " 的帳戶餘額")
                .addField("貨幣", view.formatCurrency(), true)
                .build();
            interaction.replyEmbed(embed);
        }
    }
}
```

---

## 測試

### 單元測試（使用 Mock）

```java
import ltdjms.discord.discord.mock.MockDiscordInteraction;
import ltdjms.discord.discord.mock.MockDiscordContext;

class BalanceServiceTest {

    @Test
    void shouldReturnBalance() {
        // Given: 使用 Mock 抽象介面
        MockDiscordContext mockContext = new MockDiscordContext(
            123L,  // guildId
            456L   // userId
        );

        // When: 執行業務邏輯
        Result<BalanceView, DomainError> result =
            balanceService.getBalance(
                mockContext.getGuildId(),
                mockContext.getUserId()
            );

        // Then: 驗證結果
        assertThat(result.isOk()).isTrue();
        assertThat(result.getValue().getBalance()).isEqualTo(1000L);
    }
}
```

### 整合測試（使用 Testcontainers）

```java
@Testcontainers
class BalanceCommandHandlerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @Test
    void shouldHandleBalanceCommand() {
        // Given: 建立 JDA 事件
        SlashCommandInteractionEvent event =
            DiscordTestUtils.createMockSlashEvent(
                "/balance",
                123L,
                456L
            );

        // When: 處理命令
        handler.handle(event);

        // Then: 驗證回應
        await().atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(event).replyEmbeds(argThat(embed ->
                    embed.getTitle().contains("餘額查詢")
                ));
            });
    }
}
```

---

## 依賴注入設定

### DiscordModule (Dagger 2)

```java
package ltdjms.discord.shared.di;

import dagger.Module;
import dagger.Provides;
import ltdjms.discord.discord.domain.DiscordSessionManager;
import ltdjms.discord.discord.domain.SessionType;
import ltdjms.discord.discord.services.InteractionSessionManager;

@Module
public class DiscordModule {

    @Provides
    public DiscordSessionManager<SessionType> provideSessionManager() {
        return new InteractionSessionManager<>();
    }
}
```

### 在 Command Handler 中注入

```java
public class BalanceCommandHandler {

    private final BalanceService balanceService;
    private final DiscordInteractionFactory interactionFactory;

    @Inject
    public BalanceCommandHandler(
        BalanceService balanceService,
        DiscordInteractionFactory interactionFactory
    ) {
        this.balanceService = balanceService;
        this.interactionFactory = interactionFactory;
    }
}
```

---

## 常見問題

### Q: 如何處理 Embed 長度限制？

A: `DiscordEmbedBuilder` 會自動處理長度限制：

```java
// 超長描述會自動分頁
List<MessageEmbed> embeds = DiscordEmbedBuilder.create()
    .buildPaginated(EmbedView.withLongDescription());

// 逐一發送
DiscordInteraction interaction = ...;
for (MessageEmbed embed : embeds) {
    interaction.replyEmbed(embed);
}
```

### Q: Session 過期後如何處理？

A: 檢查 `Optional` 並友善提示：

```java
Optional<Session<SessionType>> sessionOpt =
    sessionManager.getSession(SessionType.USER_PANEL, guildId, userId);

if (sessionOpt.isEmpty()) {
    interaction.reply("⚠️ 面板已過期，請重新開啟");
    return;
}
```

### Q: 如何測試不依賴 JDA 的代碼？

A: 使用 Mock 實作：

```java
// 使用 Mock 進行單元測試
MockDiscordInteraction mockInteraction = new MockDiscordInteraction();
mockInteraction.setGuildId(123L);
mockInteraction.setUserId(456L);

// 測試業務邏輯
service.handle(mockInteraction);

// 驗證回應
assertThat(mockInteraction.getReplies())
    .contains("餘額: 1000");
```

---

## 逐步實作指南

### 階段一：建立抽象介面

1. 建立 `DiscordInteraction` 介面
2. 建立 `DiscordContext` 介面
3. 建立 `DiscordEmbedBuilder` 介面

### 階段二：實作 JDA 適配器

1. 實作 `JdaDiscordInteraction`
2. 實作 `JdaDiscordContext`
3. 實作 `JdaDiscordEmbedBuilder`

### 階段三：建立 Session 管理

1. 實作 `InteractionSessionManager`
2. 定義 `SessionType` 枚舉
3. 註冊到 Dagger 模組

### 階段四：遷現有代碼

1. 選擇一個簡單的 Command Handler 作為範例
2. 遷移到使用抽象介面
3. 驗證測試通過
4. 逐步遷移其他 Handler

---

## 參考資源

- **研究報告**: [research.md](research.md)
- **數據模型**: [data-model.md](data-model.md)
- **API 契約**: [contracts/discord-api-contracts.java](contracts/discord-api-contracts.md)
- **實作計畫**: [plan.md](plan.md)

---

**Phase 1 狀態**: ✅ COMPLETE
