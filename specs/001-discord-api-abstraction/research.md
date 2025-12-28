# Research Report: Discord API 抽象層最佳實踐

**Feature**: Discord API 抽象層
**Date**: 2025-12-27
**Status**: Phase 0 Complete

---

## 摘要

本報告基於對 Java Discord API (JDA) 的深入研究和現有專案代碼分析，涵蓋 Discord API 抽象模式、Session 管理、視圖元件建構器、錯誤處理與測試策略等關鍵議題。

---

## 1. Discord API 抽象模式

### Decision

**採用分層介面卡模式（Layered Adapter Pattern）**，結合現有的 DDD 分層架構，實作輕量級的 JDA 抽象層。

### Rationale

1. **專案背景相容性**：LTDJMS 已經採用 DDD 分層架構（`domain/` → `services/` → `commands/`），引入抽象層應順應現有模式
2. **測試友好**：分離 Discord 相關邏輯後，可以對 Domain 和 Service 層進行純業務邏輯測試，無需依賴 JDA Mock
3. **維護性**：JDA 版本升級時，只需修改抽象層實作，不影響業務邏輯
4. **學習曲線低**：輕量級抽象比完整框架（如 JDA-Commands）更易於團隊理解與維護

### Alternatives Considered

| 方案 | 優點 | 缺點 | 評估 |
|------|------|------|------|
| **直接使用 JDA** | 無額外抽象層，性能最佳 | 業務邏輯與 JDA API 耦合，測試困難 | ❌ 不利於測試與維護 |
| **完整框架（JDA-Commands）** | 註解驅動，功能完整 | 過度工程化，學習成本高，靈活性不足 | ❌ 專案需求相對簡單 |
| **Discord4J + Spring Boot** | 響應式設計，生態完整 | 需要重寫現有 JDA 代碼，學習成本高 | ❌ 遷移成本太高 |
| **分層介面卡模式** | 平衡抽象與實用，易於測試 | 需要手動維護介面卡 | ✅ **推薦方案** |

### Implementation Notes

核心抽象介面包括：
- `DiscordInteraction`: 統一的互動回應抽象
- `DiscordViewBuilder`: 視圖元件建構器抽象
- `DiscordButtonEvent` / `DiscordModalEvent`: 特定事件類型的抽象

```java
// 核心抽象介面範例
public interface DiscordInteraction {
    long getGuildId();
    long getUserId();
    boolean isEphemeral();
    void reply(String message);
    void replyEmbed(MessageEmbed embed);
    void editEmbed(MessageEmbed embed);
    void deferReply();
    InteractionHook getHook();
}

// JDA 介面卡實作
public class JdaInteractionAdapter implements DiscordInteraction {
    private final GenericInteractionCreateEvent event;
    // 將 JDA 事件方法映射到抽象介面
}
```

---

## 2. Session 管理模式

### Decision

**採用改良版的現有 SessionManager 模式**，基於 `InteractionHook` 的 TTL 快取，搭配 Guild 隔離策略。

### Rationale

1. **Discord 限制約束**：Interaction Hook 15 分鐘有效期，必須在 TTL 內清理過期 Session
2. **多 Guild 隔離**：Session Key 格式 `{guildId}:{userId}` 確保不同伺服器間 Session 獨立
3. **失效通知機制**：現有 `DomainEventPublisher` 可擴展為 Session 失效事件源
4. **錯誤處理完善**：已實作 `Unknown Message` 錯誤的自動 Session 移除

### Alternatives Considered

| 方案 | 優點 | 缺點 | 評估 |
|------|------|------|------|
| **純記憶體 ConcurrentHashMap** | 實作簡單，現有方案 | 重啟丟失，無法水平擴展 | ✅ **小規模適用** |
| **Redis Session Store** | 可水平擴展，持久化 | 增加依賴，延遲較高 | ⚠️ 適合大規模 |
| **State Machine 模式** | 狀態轉換清晰 | 過度設計，維護成本高 | ❌ 複雜度過高 |
| **Spring Session** | 整合 Spring 生態 | 需要引入 Spring 框架 | ❌ 專案未用 Spring |

### Implementation Notes

現有 `AdminPanelSessionManager` 已實作優秀的 Session 管理模式：

```java
// 核心特性
private final Map<String, Session> sessions = new ConcurrentHashMap<>();
private static final long TTL_SECONDS = 15 * 60; // 15 分鐘

// Session Key 格式
private String getKey(long guildId, long adminId) {
    return guildId + ":" + adminId;
}

// 過期清理機制
private boolean isExpired(Session session) {
    return Instant.now().isAfter(session.createdAt().plusSeconds(TTL_SECONDS));
}
```

建議改進：泛型化 SessionManager，支援多種 Session 類型（ADMIN_PANEL, USER_PANEL, SHOP_PURCHASE, REDEMPTION_FLOW）。

---

## 3. 視圖元件建構器模式

### Decision

**採用靜態工廠方法模式**，統一管理 Discord Embed、Button、SelectMenu 的建構邏輯，並實作長度限制檢查。

### Rationale

1. **一致性**：現有 `UserPanelEmbedBuilder` 和 `ShopView` 已採用此模式，證明有效
2. **長度限制處理**：集中處理 Discord API 限制（Embed 4096 字元、ActionRow 5 個按鈕）
3. **可測試性**：靜態方法易於單元測試，無需 Mock JDA 物件
4. **重用性**：避免在多個 Handler 中重複建構邏輯

### Alternatives Considered

| 方案 | 優點 | 缺點 | 評估 |
|------|------|------|------|
| **靜態工廠方法** | 簡單直接，無狀態 | 無法擴展，參數較多時冗長 | ✅ **推薦方案** |
| **Builder 模式** | 可讀性高，可選參數 | 程式碼量較多，學習成本 | ⚠️ 適合複雜視圖 |
| **模板引擎** | 模板與邏輯分離 | 引入額外依賴，性能較差 | ❌ 過度設計 |
| **JDA 原生 API** | 無抽象層 | 重複程式碼，限制檢查分散 | ❌ 維護性差 |

### Implementation Notes

Discord API 限制總結：

| 限制項 | 限制值 | 處理策略 |
|--------|--------|----------|
| Embed Description | 4096 字元 | 分頁或截斷 |
| Embed Title | 256 字元 | 截斷並加 "..." |
| Embed Field Name | 256 字元 | 截斷並加 "..." |
| Embed Field Value | 1024 字元 | 分多個 Field |
| Embed Fields | 25 個 | 分多個 Embed |
| Embeds per Message | 10 個 | 分多個訊息 |
| Buttons per ActionRow | 5 個 | 分多個 ActionRow |
| ActionRows per Message | 5 個 | 減少元件數量 |

建議新增 `DiscordEmbedBuilder` 工具類，提供 `buildSafeEmbed()` 和 `buildPaginatedEmbeds()` 方法自動處理長度限制。

---

## 4. 錯誤處理策略

### Decision

**採用分層錯誤處理模式**，結合現有的 `Result<T, E>` 和 `DomainError` 系統，並擴展 Discord API 特定錯誤類型。

### Rationale

1. **一致性**：現有 `BotErrorHandler` 和 `DomainError` 已建立統一錯誤處理模式
2. **Ephemeral 錯誤回應**：錯誤訊息應僅使用者可見（`setEphemeral(true)`），避免干擾其他使用者
3. **3 秒回應限制**：Discord Interaction 必須在 3 秒內回應，否則視為失敗
4. **Unknown Message 處理**：Hook 過期後自動移除 Session，避免重複錯誤

### Alternatives Considered

| 方案 | 優點 | 缺點 | 評估 |
|------|------|------|------|
| **Result 模式 + BotErrorHandler** | 型別安全，已現有 | 需手動處理所有錯誤分支 | ✅ **推薦方案** |
| **例外處理統一攔截** | 減少重複 try-catch | 違背 Checked Exception 設計 | ⚠️ 與 Result 模式衝突 |
| **全域異常處理器** | 集中管理 | 難以細粒度控制 | ❌ 靈活性不足 |
| **JDA 原生錯誤** | 無額外抽象 | 錯誤訊息不友善 | ❌ 使用者體驗差 |

### Implementation Notes

新增 Discord API 特定錯誤類型：

```java
public class DiscordError {
    public enum Category {
        INTERACTION_TIMEOUT,  // 3 秒內未回應
        HOOK_EXPIRED,         // Hook 過期（超過 15 分鐘）
        UNKNOWN_MESSAGE,      // 訊息已刪除或無法存取
        RATE_LIMITED,         // 超過 Rate Limit
        MISSING_PERMISSIONS,  // 缺少必要權限
        INVALID_COMPONENT_ID  // 無效的 Component ID
    }
}
```

擴展 `BotErrorHandler` 為 `DiscordErrorHandler`，統一處理所有 Interaction 類型的錯誤。

---

## 5. 測試策略

### Decision

**採用分層測試策略**，業務邏輯測試不依賴 JDA Mock，整合測試使用 Testcontainers 驗證完整流程。

### Rationale

1. **現有測試困境**：Java 版本限制導致 JDA Mock 在某些環境不可用
2. **業務邏輯分離**：Domain 和 Service 層測試無需依賴 Discord API
3. **整合測試補足**：使用 Testcontainers 驗證真實 Discord 互動流程
4. **測試金字塔**：70% 單元測試 → 20% 整合測試 → 10% E2E 測試

### Alternatives Considered

| 方案 | 優點 | 缺點 | 評估 |
|------|------|------|------|
| **純業務邏輯測試** | 無需 Mock，快速穩定 | 無法測試 Discord 整合 | ✅ **推薦方案** |
| **JDA Event Mock** | 可測試 Handler | Java 版本限制，維護成本高 | ⚠️ 部分可用 |
| **Testcontainers 整合測試** | 真實環境驗證 | 較慢，資源消耗大 | ✅ 輔助方案 |
| **Discord API 集成測試** | 完整流程測試 | 需要 Discord Token，不穩定 | ❌ 不推薦 |

### Implementation Notes

測試分層架構：

```
測試金字塔
───────────────────────────────────────────────
                 ↑
               E2E測試
             (10%) - 真實 Discord 環境（可選）
───────────────────────────────────────────────
                 ↑
            整合測試
          (20%) - Testcontainers + JDA
───────────────────────────────────────────────
                 ↑
          單元測試
        (70%) - 業務邏輯 + 視圖建構器
───────────────────────────────────────────────
```

測試覆蓋率目標：

| 層級 | 覆蓋率目標 | 測試類型 |
|------|-----------|----------|
| Domain | 90% | 單元測試 |
| Service | 85% | 單元測試 |
| Commands | 70% | 單元測試（視圖建構）+ 整合測試 |
| Adapter | 60% | 整合測試（需要 JDA） |

---

## 6. 實作優先順序

### 階段一（立即實作）

1. 改良現有 `BotErrorHandler`，支援所有 Interaction 類型
2. 新增 `DiscordError` 類型
3. 實作 `DiscordEmbedBuilder` 長度檢查工具

### 階段二（短期實作）

1. 泛型化 `SessionManager`，支援多種 Session 類型
2. 新增 `DiscordInteraction` 抽象介面
3. 重構 `UserPanelButtonHandler` 使用抽象層

### 階段三（中長期實作）

1. 完整測試覆蓋（補足 Handler 整合測試）
2. 性能優化（Session 定期清理任務）
3. 文檔完善（架構說明、使用範例）

---

## 參考資料

### 官方文檔

- [JDA Wiki - Interactions](https://jda.wiki/using-jda/interactions/)
- [Discord Developer Documentation - Messages](https://discord.com/developers/docs/resources/message)
- [Discord Component Reference](https://discord.com/developers/docs/components/reference)

### 社區資源

- [Architecting Discord Bot the Right Way](https://itsnikhil.medium.com/architecting-discord-bot-the-right-way-46e426a0b995)
- [Discord Bots and State Management](https://betterprogramming.pub/discord-bots-and-state-management-22775c1f7aeb)
- [Advanced Discord Bot Development Strategies](https://arnauld-alex.com/building-a-production-ready-discord-bot-architecture-beyond-discordjs)

### 專案內部資源

- `/Users/tszkinlai/Coding/LTDJMS/src/main/java/ltdjms/discord/currency/bot/BotErrorHandler.java`
- `/Users/tszkinlai/Coding/LTDJMS/src/main/java/ltdjms/discord/panel/services/AdminPanelSessionManager.java`
- `/Users/tszkinlai/Coding/LTDJMS/src/main/java/ltdjms/discord/panel/services/UserPanelEmbedBuilder.java`
- `/Users/tszkinlai/Coding/LTDJMS/docs/architecture/overview.md`

---

**Phase 0 狀態**: ✅ COMPLETE
