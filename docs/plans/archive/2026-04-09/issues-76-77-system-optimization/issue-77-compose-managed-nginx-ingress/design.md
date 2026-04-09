# Design: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導

- Date: 2026-04-09
- Feature: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導
- Change Name: issue-77-compose-managed-nginx-ingress

## Design Goal
把自架部署的公開入口真相收斂成單一 `APP_PUBLIC_BASE_URL`，並以 Compose 內管理的 Nginx sidecar 代理到 bot 既有的嵌入式 HTTP server，讓 operator 不再手動處理 callback `ReturnURL`、外部 Nginx route 與 loopback bind 細節。

## Change Summary
- Requested change: 依 issue #77 讓程式自主管理容器內 Nginx 配置，並從公開 base URL 推導 callback URL。
- Existing baseline: `EcpayCallbackHttpServer` 直接在 bot 內提供 `/` 與 callback route，但 Compose 沒有 ingress；文件要求 operator 自己處理 `ECPAY_RETURN_URL` 與 callback bind / 外部代理設定。
- Proposed design delta: 新增 `APP_PUBLIC_BASE_URL` 推導 `ECPAY_RETURN_URL`；Compose 增加 Nginx sidecar 共享 bot network namespace，代理所有 HTTP 請求到 bot 的 loopback callback server。

## Scope Mapping
- Spec requirements covered: `R1.1-R1.3`, `R2.1-R2.2`, `R3.1-R3.3`
- Affected modules:
  - `src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`
  - `src/main/java/ltdjms/discord/shop/services/EcpayCallbackHttpServer.java`
  - `src/test/java/ltdjms/discord/shared/EnvironmentConfigTest.java`
  - `src/test/java/ltdjms/discord/shared/EnvironmentConfigDotEnvIntegrationTest.java`
  - `docker-compose.yml`
  - `docker/nginx/**`
  - `.env.example`
  - `README.md`
  - `docs/configuration.md`
  - `docs/getting-started.md`
- External contracts involved:
  - `ECPay ReturnURL / Payment Results Notification`
  - `Docker Compose service network namespaces`
  - `Nginx proxy_pass`
- Coordination reference: `../coordination.md`

## Current Architecture
現況中有三個來源彼此鬆散：
1. `EnvironmentConfig.getEcpayReturnUrl()` 只接受顯式 `ECPAY_RETURN_URL`。
2. `EcpayCallbackHttpServer` 在 bot 內綁定 host / port，提供 `/` 與 callback route。
3. 自架部署文件要求 operator 手動設定 `ECPAY_RETURN_URL` 與 `ECPAY_CALLBACK_*`，必要時再自行寫外部代理。

這造成的問題是：
- 使用者明明只知道公開 domain，也必須自己拼出完整 callback URL。
- callback server 的 loopback bind 與外部路由分離，容易配置錯誤。
- Vercel workflow 與自架部署說明混在一起，讓 landing page / callback 的實際入口不夠明確。

## Proposed Architecture
- `EnvironmentConfig` 新增 `APP_PUBLIC_BASE_URL`，作為自架部署的公開入口真相。
- 若 `ECPAY_RETURN_URL` 空白，則 `EnvironmentConfig` 以 `APP_PUBLIC_BASE_URL + ECPAY_CALLBACK_PATH` 推導 callback URL；若有顯式值，維持 override。
- Compose 新增 `nginx` sidecar，使用 `network_mode: service:bot` 與 bot 共用 network namespace，直接代理到 bot 的 `127.0.0.1:8085`。
- bot 的 `EcpayCallbackHttpServer` 維持既有 loopback bind 與 `/` + callback route，不再要求 operator 手動改成 public bind。

## Component Changes

### Component 1: `EnvironmentConfig`
- Responsibility: 定義公開 base URL 與 callback URL 的推導規則。
- Inputs: `APP_PUBLIC_BASE_URL`、`ECPAY_RETURN_URL`、`ECPAY_CALLBACK_PATH`
- Outputs: 正規化後的 base URL 與最終 `getEcpayReturnUrl()`
- Dependencies: `ECPay ReturnURL / Payment Results Notification`
- Invariants:
  - 顯式 `ECPAY_RETURN_URL` 優先於推導值。
  - `APP_PUBLIC_BASE_URL` 只作為 fallback，不改變既有 override 優先序。

### Component 2: `docker-compose.yml` + `docker/nginx/**`
- Responsibility: 提供 repo 管理的自架 ingress。
- Inputs: bot 服務、Nginx static config、host port mapping
- Outputs: 對外 HTTP 入口、對 bot loopback HTTP server 的 reverse proxy
- Dependencies: `Docker Compose service network namespaces`, `Nginx proxy_pass`
- Invariants:
  - Nginx 只能代理到 bot 的 loopback HTTP server，不迫使 bot 使用 public bind。
  - Nginx 配置由 repo 管理，不要求 operator 手寫 route。

### Component 3: `EcpayCallbackHttpServer`
- Responsibility: 維持既有首頁與 callback 路由的 HTTP 真相來源。
- Inputs: `ECPAY_CALLBACK_BIND_HOST`、`ECPAY_CALLBACK_BIND_PORT`、`ECPAY_CALLBACK_PATH`
- Outputs: landing page / callback response
- Dependencies: `FiatPaymentCallbackService`
- Invariants:
  - route shape 與 callback 驗證邏輯保持不變。
  - 在 Compose ingress 模式下仍使用 loopback bind。

## Sequence / Control Flow
1. operator 設定 `APP_PUBLIC_BASE_URL`（例如 bare domain 或完整 base URL）。
2. `EnvironmentConfig` 在 `ECPAY_RETURN_URL` 缺席時，推導 callback URL。
3. bot 啟動後照舊在 loopback host/port 啟動 `EcpayCallbackHttpServer`。
4. Compose 內的 Nginx sidecar 共享 bot network namespace，對外接收 HTTP，並把請求 proxy 到 bot 的 `127.0.0.1:8085`。
5. 對外 `/` 與 callback route 由 Nginx 進入，但實際內容仍由 bot 既有 embedded server 提供。

## Data / State Impact
- Created or updated data:
  - 新增 `APP_PUBLIC_BASE_URL` 設定欄位
  - 新增 `docker/nginx/default.conf`
- Consistency rules:
  - `APP_PUBLIC_BASE_URL` 是 Compose 自架部署的公開入口真相；`ECPAY_RETURN_URL` 只作 override。
  - Nginx sidecar 與 bot 共用 network namespace，因此不能同時再為 Nginx 宣告 `networks`。
- Migration / rollout needs:
  - 舊部署可繼續保留顯式 `ECPAY_RETURN_URL`。
  - 新部署可只填 `APP_PUBLIC_BASE_URL` 與既有 ECPay 憑證。

## Risk and Tradeoffs
- Key risks:
  - `network_mode: service:bot` 會讓 Nginx 與 bot 在 network lifecycle 上更緊耦合。
  - 若 operator 把 `APP_PUBLIC_BASE_URL` 設成錯誤 domain，callback 仍會打不到。
- Rejected alternatives:
  - 直接把 bot 的 8085 對外映射成 80：雖然更簡單，但不符合 issue 對「容器內 Nginx 層」的要求，也少了 ingress 可觀測層。
  - 把 bot bind host 改成 `0.0.0.0` 再走獨立 Nginx 容器網路：會與既有 stage/loopback 安全預設產生衝突。
  - 移除 `ECPAY_RETURN_URL` 改成只接受 base URL：會破壞既有相容性，且不利特殊部署 override。
- Operational constraints:
  - TLS 仍需由 Nginx 之外的外部入口或日後擴充處理。
  - 文件必須清楚區分 Compose 自架 ingress 與 Vercel landing page workflow。

## Validation Plan
- Tests:
  - Unit：`EnvironmentConfig` 的 base URL normalization 與 return URL 推導
  - Integration：`docker compose config`
  - Review：README / configuration / getting-started 文件對齊
- Contract checks:
  - 用 ECPay 官方文件確認 `ReturnURL` 是 server-side callback URL，推導邏輯不可偏離這個契約。
  - 用 Docker 官方文件確認 `network_mode: service:{name}` 與 `networks` 互斥。
  - 用 Nginx 官方 `proxy_pass` 文件確認 reverse proxy 設定方式。
- Rollback / fallback:
  - 可回退到沒有 Nginx sidecar 的 Compose 與顯式 `ECPAY_RETURN_URL` 設定方式。
  - 若 ingress wiring 有誤，使用者仍可手動提供 `ECPAY_RETURN_URL` 作為短期 fallback。

## Open Questions
None
