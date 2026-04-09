# Coordination: Issues 76-77 系統優化

- Date: 2026-04-09
- Batch: issues-76-77-system-optimization

## Coordination Goal
在不改變既有 ECPay 訂單、付款完成 callback 與商品履約主流程的前提下，同步改善法幣下單互動可靠性與 Compose 自架部署入口，讓使用者不再看到假失敗互動，部署者也只需要提供公開 base URL 與既有金流憑證即可完成 callback 對外路由。

## Batch Scope
- Included spec sets:
  - `issue-76-fiat-order-interaction-reliability`
  - `issue-77-compose-managed-nginx-ingress`
- Shared outcome:
  - 法幣下單在 Discord 端能先即時 ack，再穩定回填訂單摘要與 DM fallback。
  - Compose 自架部署改由 repo 內管理 Nginx ingress，並從單一公開 base URL 推導 callback URL。
- Out of scope:
  - 修改 `fiat_order` schema、付款單號規則或 paid→fulfillment→admin notification 的既有狀態流轉。
  - 移除 Vercel landing page workflow。
  - 直接提供 TLS 憑證、ACME、自動 DNS 或 WAF。
- Independence rule:
  - `issue 76` 必須能在不依賴 Nginx ingress 變更的情況下單獨完成。
  - `issue 77` 必須能在不改動法幣下單 Discord 互動邏輯的情況下單獨完成。

## Shared Context
- Current baseline:
  - `ShopSelectMenuHandler` 的法幣下單路徑會同步呼叫 `FiatOrderService.createFiatOnlyOrder(...)`，直到 DM 成功／失敗後才回覆 interaction，存在超過 Discord 3 秒限制的風險。
  - `EcpayCallbackHttpServer` 目前同時提供 `/` 與 `ECPAY_CALLBACK_PATH`，bot 內部預設綁定 `127.0.0.1:8085`。
  - 自架 Compose 目前沒有 repo 內管理的 Nginx ingress；宣傳首頁另有 `.github/workflows/vercel-landing-page.yml` 發布到 Vercel。
- Shared constraints:
  - 不改變 slash command 名稱、Discord custom ID、MerchantTradeNo 生成格式與 callback payload 驗證語意。
  - 不新增新的外部 SaaS 依賴；Nginx 只能作為 Compose 內管理的容器服務。
  - 仍以 `EnvironmentConfig` 為 runtime 設定真相來源。
- Shared invariants:
  - `ECPAY_STAGE_MODE=true` 的安全預設不能被新 ingress 設計繞過。
  - 付款成功後的 callback、fulfillment、admin notification 仍需保持既有冪等。
  - `ECPAY_RETURN_URL` 仍可作為顯式 override；若使用者明確提供，系統不可強制覆寫。

## Shared Preparation

### Shared Fields / Contracts
- Shared fields to introduce or reuse:
  - 新增 `APP_PUBLIC_BASE_URL` 作為自架 Compose 部署的公開入口真相；若 `ECPAY_RETURN_URL` 空白，則由它與 `ECPAY_CALLBACK_PATH` 推導 callback URL。
  - 既有 `ECPAY_CALLBACK_PATH` 保留為 callback path 真相。
- Canonical source of truth:
  - 公開 ingress / callback URL 推導：`src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`
  - 法幣下單 Discord 互動協調：`src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
- Required preparation before implementation: `None`

### Replacement / Legacy Direction
- Legacy behavior being replaced:
  - 法幣下單成功後才回覆 interaction 的同步流程。
  - 自架部署需手動維護外部 Nginx callback / landing page route 的做法。
- Required implementation direction:
  - 以 in-place replacement 為主，不建立長期平行的第二套 Fiat interaction 流程。
  - Compose ingress 以 sidecar Nginx 代理到 bot 內嵌 HTTP server，保留 bot 既有 `/` 與 callback route 實作。
- Compatibility window:
  - `APP_PUBLIC_BASE_URL` 為新增便利設定；`ECPAY_RETURN_URL` 保留相容 override。
  - Vercel landing page workflow 暫時保留，並在文件中明確標註其為獨立發布路徑。
- Cleanup required after cutover:
  - 移除文件中要求自架部署手動設定 callback bind host/外部 Nginx location 的指引。
  - 移除法幣下單互動只回「已私訊給你」而不含訂單摘要的舊假設。

## Spec Ownership Map

### Spec Set 1: `issue-76-fiat-order-interaction-reliability`
- Primary concern: 修正法幣下單 interaction 逾時、成功摘要不足與重複點擊重入。
- Allowed touch points:
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderService.java`
  - `src/test/java/ltdjms/discord/shop/commands/ShopSelectMenuHandlerTest.java`
  - `src/test/java/ltdjms/discord/shop/services/FiatOrderServiceTest.java`
- Must not change:
  - `EnvironmentConfig` 的公開 URL / ingress 推導規則
  - Docker / Nginx 交付資產
- Depends on shared preparation: `None`
- Cross-spec implementation dependency: `None`

### Spec Set 2: `issue-77-compose-managed-nginx-ingress`
- Primary concern: 讓自架 Compose 部署只需公開 base URL，就能由 repo 管理 ingress 與 callback URL 推導。
- Allowed touch points:
  - `src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`
  - `src/main/java/ltdjms/discord/shop/services/EcpayCallbackHttpServer.java`
  - `src/test/java/ltdjms/discord/shared/EnvironmentConfig*.java`
  - `docker-compose.yml`
  - `docker/nginx/**`
  - `.env.example`
  - `README.md`
  - `docs/configuration.md`
  - `docs/getting-started.md`
- Must not change:
  - `ShopSelectMenuHandler` 的 Discord 互動文案與防重入邏輯
  - `FiatPaymentCallbackService` 的 paid callback 驗證與冪等主流程
- Depends on shared preparation: `None`
- Cross-spec implementation dependency: `None`

## Conflict Boundaries
- Shared files requiring coordination: `None`
- Merge order / landing order:
  - 功能上獨立。
  - 建議的便利順序：`issue-77` → `issue-76`，因為文件中的部署指引會影響 issue 76 最終說明。
- Worktree notes:
  - 可平行 worktree；但合併前需在主線重新驗證 `make test` 與 `docker compose config`。

## Integration Checkpoints
- Combined behaviors to verify after merge:
  - 使用者在法幣下單成功時，不再看到 Discord 的 interaction failure，且就算 DM 失敗也能看到付款所需摘要。
  - 當只設定 `APP_PUBLIC_BASE_URL` 時，bot 仍能推導出正確 `ReturnURL` 並啟動 callback server。
  - Compose 啟動後，Nginx 對外提供 `/` 與 callback route，而 bot 仍維持 loopback 綁定。
- Required final test scope:
  - `ShopSelectMenuHandlerTest`
  - `FiatOrderServiceTest`
  - `EnvironmentConfigTest`
  - `EnvironmentConfigDotEnvIntegrationTest`
  - `docker compose config`
- Rollback notes:
  - `issue 76` 可獨立回退到舊互動邏輯，不影響 callback / ingress。
  - `issue 77` 可獨立回退到舊部署文件與無 sidecar Nginx 的狀態，不影響既有付款資料。

## Open Questions
None
