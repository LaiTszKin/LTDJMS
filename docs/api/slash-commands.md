# Slash Commands 參考

這份文件只整理 **目前實際註冊** 的 slash commands，以及與它們直接相關的使用限制。權威來源是 `src/main/java/ltdjms/discord/currency/bot/SlashCommandListener.java`。

## 目前對外指令

| 指令 | 權限 | 參數 | 作用 |
| --- | --- | --- | --- |
| `/currency-config` | 管理員 | `name?`、`icon?` | 設定或查看 guild 貨幣名稱與圖示 |
| `/dice-game-1` | 所有成員 | `tokens?` | 玩摘星手，消耗遊戲代幣換取貨幣獎勵 |
| `/dice-game-2` | 所有成員 | `tokens?` | 玩神龍擺尾，依骰型計算高額貨幣獎勵 |
| `/user-panel` | 所有成員 | 無 | 開啟個人面板，查看餘額、代幣、歷史與兌換入口 |
| `/admin-panel` | 管理員 | 無 | 開啟管理面板，管理餘額、代幣、遊戲、商品、AI 與派單設定 |
| `/shop` | 所有成員 | 無 | 開啟商店互動流程 |
| `/dispatch-panel` | 管理員 | 無 | 開啟護航派單面板 |

## 每個指令的閱讀重點

### `/currency-config`

- 若 `name`、`icon` 都不填，會顯示目前 guild 的貨幣設定
- 權限由 Discord 預設權限與 handler 內的管理員檢查雙重保護

### `/dice-game-1` / `/dice-game-2`

- 都接受可選的 `tokens` 參數
- 真正的遊戲邏輯在 `gametoken/services/`
- 回應會依使用者語系做本地化

### `/user-panel`

- 開啟後是 ephemeral 面板
- 會提供貨幣歷史、代幣歷史、商品兌換歷史與兌換入口
- 成功回覆後會建立 session，後續事件可更新同一塊面板

### `/admin-panel`

- 目前主入口按鈕涵蓋：
  - 使用者餘額管理
  - 遊戲代幣管理
  - 遊戲設定管理
  - 商品與兌換碼管理
  - AI 頻道設定
  - AI Agent 配置
  - 派單售後設定
  - 護航定價設定

### `/shop`

- 會進入商店互動流程，而不是單次文字回覆
- 後續可能走貨幣購買、法幣下單、履約通知等分支

### `/dispatch-panel`

- 只負責打開派單面板
- 真正的護航訂單建立與後續互動由 `dispatch/commands/` 與 `dispatch/services/` 負責

## AI 不是 slash command

AI Chat / AI Agent 是透過 **在允許頻道提及 Bot** 觸發，不是 slash command。

行為重點：

- 只在 guild 內處理，DM 會忽略
- 不在 AI 白名單的頻道 / 類別會直接忽略
- 若該頻道啟用 Agent，會走 Agent 工具鏈
- 一般 AI 回應可經過 Markdown 驗證、切段與格式整理

## 已整合進面板的舊指令

這些能力仍存在於服務層或 handler 中，但已不再對外註冊為獨立 slash command：

- `/balance`
- `/adjust-balance`
- `/game-token-adjust`
- `/dice-game-1-config`
- `/dice-game-2-config`

現在它們的對外入口是 `/user-panel` 與 `/admin-panel`。
