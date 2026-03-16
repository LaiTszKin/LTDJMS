# Features

## 1. Feature Summary

- Supported user workflows:
  - guild 貨幣與遊戲代幣管理
  - 商品商店瀏覽、貨幣購買與兌換碼兌換
  - 綠界超商代碼法幣下單與付款後履約
  - 護航派單、完單確認、售後流程
  - AI Chat 與 AI Agent 頻道治理
- Primary entrypoints:
  - Slash commands：`/currency-config`, `/dice-game-1`, `/dice-game-2`, `/user-panel`, `/admin-panel`, `/shop`, `/dispatch-panel`
  - Mention-based AI：在允許頻道中提及 Bot
  - ECPay callback：內嵌 HTTP server
- Notable limitations or guardrails:
  - 大多數互動僅支援 guild，不支援 DM 直接執行 slash command
  - `/currency-config`, `/admin-panel`, `/dispatch-panel` 需要管理員權限
  - AI 只在允許頻道 / 類別運作，且 Agent 需額外啟用頻道配置
  - 法幣履約依賴 callback 成功到達；未設定 `ECPAY_RETURN_URL` 不會啟用回推服務

## 2. Feature Details

### Guild 經濟系統與遊戲代幣

- User value: 讓每個 guild 維護自己的貨幣名稱、成員餘額、遊戲代幣與小遊戲獎勵
- Trigger or entrypoint: `/currency-config`, `/user-panel`, `/admin-panel`, `/dice-game-1`, `/dice-game-2`
- Core flow: 管理員設定貨幣與遊戲參數；使用者從個人面板查看餘額並消耗代幣進行骰子遊戲；服務層更新帳戶與交易後發佈事件
- Edge or failure notes: 餘額不足、代幣不足、非管理員修改設定都會被拒絕；事件監聽器負責更新快取與面板內容
- Evidence: `SlashCommandListener.java`, `CurrencyConfigService.java`, `GameTokenService.java`, `AdminPanelService.java`

### 商品、商店與兌換碼

- User value: 管理員可建立商品與兌換碼，使用者可在商店中瀏覽、用 guild 貨幣購買，或透過兌換碼領取獎勵
- Trigger or entrypoint: `/shop`, `/admin-panel`, `/user-panel`
- Core flow: 商品資料由 `ProductService` 管理；貨幣購買由 `CurrencyPurchaseService` 扣款、記錄交易、發放獎勵；兌換碼由 `RedemptionService` 生成與兌換
- Edge or failure notes: 獎勵發放失敗時，貨幣購買流程會嘗試自動退款；商品若配置後端履約，購買後會額外觸發 webhook
- Evidence: `ProductService.java`, `CurrencyPurchaseService.java`, `RedemptionService.java`, `ShopButtonHandler.java`, `ShopSelectMenuHandler.java`

### 綠界法幣付款與付款後履約

- User value: 對限定法幣商品產生超商代碼，付款完成後才履約，避免未付款訂單被提前處理
- Trigger or entrypoint: `/shop` 內的法幣購買互動，以及 ECPay callback
- Core flow: `FiatOrderService` 向 ECPay 取號並建立待付款訂單；`EcpayCallbackHttpServer` 接收回推，`FiatPaymentCallbackService` 驗證 / 解密後將訂單標記為已付款，並只觸發一次履約與管理員通知
- Edge or failure notes: callback 重送需維持冪等；公開 callback bind host 若未設定 shared secret 會直接阻止啟動；缺少 `ECPAY_HASH_KEY` / `ECPAY_HASH_IV` 無法處理已付款回推
- Evidence: `FiatOrderService.java`, `EcpayCallbackHttpServer.java`, `FiatPaymentCallbackService.java`, `ProductFulfillmentApiService.java`

### 護航派單與售後流程

- User value: 管理員可在 Discord 內建立護航派單、追蹤完單與售後收斂，不需切換外部系統
- Trigger or entrypoint: `/dispatch-panel`, 管理面板售後人員設定、DM 互動按鈕
- Core flow: 管理員選擇護航者與客戶建立訂單；護航者於私訊確認接單並回推客戶；後續可申請完單、客戶確認或售後，由售後人員接案與結案
- Edge or failure notes: 護航者與客戶不可為同一人；DM 失敗時會回覆操作方；售後案件使用 claim 流程避免多位售後同時接手
- Evidence: `DispatchPanelCommandHandler.java`, `DispatchPanelInteractionHandler.java`, `EscortDispatchOrderService.java`, `DispatchAfterSalesStaffService.java`

### AI Chat、AI 頻道限制與 AI Agent

- User value: 在指定頻道提及 Bot 可獲得 AI 回應，並可為個別頻道啟用 Agent 模式執行 Discord 工具
- Trigger or entrypoint: 提及 Bot、`/admin-panel` 內的 AI 頻道與 Agent 配置
- Core flow: `AIChatMentionListener` 先驗證頻道是否允許，再呼叫 AI provider 產生回應；若某頻道啟用 Agent，則由 Agent 工具鏈處理更進階的 Discord 操作並記錄工具事件
- Edge or failure notes: 不在白名單頻道時會靜默忽略；AI provider 認證失敗、逾時或格式錯誤會映射成 domain error；討論串會繼承父頻道的 Agent 配置
- Evidence: `AIChatMentionListener.java`, `DefaultAIChannelRestrictionService.java`, `DefaultAIAgentChannelConfigService.java`, `ToolExecutionInterceptor.java`

### 管理面板嵌入式設定流程

- User value: 將多欄位設定改成可預覽、可確認的 ephemeral embed 流程，降低一次性 modal 輸入錯誤
- Trigger or entrypoint: `/admin-panel` 中的商品、護航定價、AI 設定等管理互動
- Core flow: 使用者先開啟設定面板，再透過 select menu 與局部 modal 填值，最後按下確認才真正寫入 service 層
- Edge or failure notes: 必填欄位未完成時會阻止提交；僅數值型簡單設定仍可能保留直接 modal；長內容會切段避免超過 Discord embed 限制
- Evidence: `AdminPanelButtonHandler.java`, `AdminProductPanelHandler.java`, `AdminPanelButtonHandlerTest.java`, `AdminProductPanelHandlerTest.java`
