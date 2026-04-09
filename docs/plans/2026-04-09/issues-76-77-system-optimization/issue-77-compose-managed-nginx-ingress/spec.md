# Spec: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導

- Date: 2026-04-09
- Feature: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導
- Owner: Codex

## Goal
讓自架 Compose 部署只需要提供公開 base URL 與既有金流憑證，就能由程式推導 `ReturnURL` 並由 repo 內管理 Nginx 入口，減少手動維護 callback / landing page 路由與內部 bind 參數。

## Scope

### In Scope
- 新增 `APP_PUBLIC_BASE_URL`，當 `ECPAY_RETURN_URL` 未設定時，從它與 `ECPAY_CALLBACK_PATH` 推導完整 callback URL。
- 在 Docker Compose 中新增 repo 內管理的 Nginx sidecar，代理所有 HTTP 請求到 bot 內嵌 `EcpayCallbackHttpServer`。
- 保持 bot 容器內的 callback server 使用 loopback 綁定，避免迫使 operator 手動設定 `127.0.0.1` / `0.0.0.0`。
- 更新 `.env.example`、README 與部署文件，改成以 `APP_PUBLIC_BASE_URL` 為自架部署主要入口設定。

### Out of Scope
- 移除 `.github/workflows/vercel-landing-page.yml` 或 Vercel 發布流程。
- 提供 TLS / ACME / 自動憑證更新。
- 變更 `EcpayCallbackHttpServer` 的 callback 驗證、paid transition、fulfillment 或 admin notification 邏輯。

## Functional Behaviors (BDD)

### Requirement 1: 公開 callback URL 應由單一 public base URL 推導
**GIVEN** 自架部署者只想提供單一公開 domain / base URL  
**AND** 系統已知既有 `ECPAY_CALLBACK_PATH`  
**WHEN** `ECPAY_RETURN_URL` 未顯式設定  
**THEN** 系統必須從 `APP_PUBLIC_BASE_URL` 與 callback path 自動推導完整 `ReturnURL`  
**AND** 顯式提供的 `ECPAY_RETURN_URL` 仍必須優先於推導值

**Requirements**:
- [x] R1.1 `APP_PUBLIC_BASE_URL` 可接受 bare host 或完整 URL，系統需正規化成可拼接的 base URL。
- [x] R1.2 `getEcpayReturnUrl()` 在 `ECPAY_RETURN_URL` 空白時，必須回傳 `APP_PUBLIC_BASE_URL + ECPAY_CALLBACK_PATH`。
- [x] R1.3 若 `ECPAY_RETURN_URL` 有值，必須維持 override 優先序。

### Requirement 2: Compose 需內建 repo 管理的 Nginx ingress
**GIVEN** bot 內嵌 HTTP server 已提供 `/` 與 `ECPAY_CALLBACK_PATH`  
**AND** operator 不想再自己寫外部 Nginx `location` 設定  
**WHEN** 使用 Docker Compose 啟動自架部署  
**THEN** repo 內必須提供一個受版本控管的 Nginx ingress  
**AND** ingress 必須把對外 HTTP 請求代理到 bot 內嵌 HTTP server

**Requirements**:
- [x] R2.1 Compose 新增 `nginx` service，作為對外 HTTP 入口。
- [x] R2.2 Nginx 設定由 repo 管理，無需 operator 另外寫 callback / landing page route。

### Requirement 3: Compose 部署不應再要求使用者手動處理 callback bind host
**GIVEN** stage mode 下 bot callback server 預設只能綁 loopback  
**AND** 使用者只想提供公開 base URL，不想再手動思考 `127.0.0.1`、`0.0.0.0` 與 port 對接  
**WHEN** 使用 Compose 的 Nginx ingress 模式部署  
**THEN** bot 的內部 bind host / port 應由 repo 管理的部署設定固定  
**AND** 文件不得再把 `ECPAY_CALLBACK_BIND_HOST=127.0.0.1` 視為自架部署者必填步驟

**Requirements**:
- [x] R3.1 Compose ingress 模式下，bot 容器仍使用 loopback callback bind 預設。
- [x] R3.2 文件應把 `APP_PUBLIC_BASE_URL` 視為主要設定入口，`ECPAY_CALLBACK_BIND_*` 降為進階 override / 內部實作細節。
- [x] R3.3 Vercel landing page 流程仍保留，但需被明確標記為獨立於 Compose ingress 的發布路徑。

## Error and Edge Cases
- [x] `APP_PUBLIC_BASE_URL` 為 bare host、含 trailing slash、或含 path prefix 時，推導結果都要可預期。
- [x] `ECPAY_RETURN_URL` 顯式設定時，不得被 `APP_PUBLIC_BASE_URL` 覆蓋。
- [x] Nginx ingress 啟用後，不得要求 bot 轉成 public bind 才能被 proxy 連到。
- [x] 文件不得再暗示 Compose 使用者需要手寫外部 Nginx location。
- [x] `docker compose config` 必須能展開成功，不出現無效 network / volume 衝突。

## Clarification Questions
None

## References
- Official docs:
  - `https://developers.ecpay.com.tw/16449/`
  - `https://developers.ecpay.com.tw/16538/`
  - `https://docs.docker.com/reference/compose-file/services/`
  - `https://nginx.org/en/docs/http/ngx_http_proxy_module.html`
- Related code files:
  - `src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`
  - `src/main/java/ltdjms/discord/shop/services/EcpayCallbackHttpServer.java`
  - `docker-compose.yml`
  - `.env.example`
  - `README.md`
  - `docs/configuration.md`
  - `docs/getting-started.md`
  - GitHub issue `#77`
