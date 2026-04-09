# Spec: Caddy HTTPS ingress

- Date: 2026-04-09
- Feature: Caddy HTTPS ingress
- Owner: Codex

## Goal
讓自架 VPS 部署改用 repo 管理的 Caddy ingress，自動為設定的公開網域簽發 HTTPS 憑證並安全代理首頁與 ECPay callback，降低 operator 手動處理 TLS 與反向代理配置的負擔。

## Scope

### In Scope
- 以 Caddy sidecar 取代現有 Nginx sidecar，作為 Compose 對外 ingress。
- 導入以單一公開 domain 為核心的 TLS / ACME 設定，並持久化 Caddy certificate state。
- 保持 bot 內嵌 callback server 的 loopback bind 與 `/`、`ECPAY_CALLBACK_PATH` 路由真相來源不變。
- 更新部署所需的環境變數範本，讓 operator 能提供 domain 與 ACME email。

### Out of Scope
- 變更 `FiatPaymentCallbackService`、付款驗證、冪等與履約流程。
- 導入 Vercel proxy / rewrite 作為正式 callback 入口。
- 處理 DNS 供應商 API、自動建立 DNS 記錄，或支援 wildcard 憑證。

## Functional Behaviors (BDD)

### Requirement 1: Compose ingress 應以 Caddy 自動管理 HTTPS
**GIVEN** operator 在 VPS 上部署 Compose 服務  
**AND** 已提供公開 domain 與可接收 ACME 通知的 email  
**WHEN** operator 啟動 Compose  
**THEN** repo 內的 Caddy sidecar 必須對外接收 `80` / `443`  
**AND** Caddy 必須為設定的 domain 自動簽發與續期 HTTPS 憑證，並把首頁與 callback 請求代理到 bot 的 loopback HTTP server

**Requirements**:
- [ ] R1.1 `docker-compose.yml` 必須以 Caddy 取代既有 Nginx sidecar，並對外開放 `80` / `443`。
- [ ] R1.2 Caddy 設定必須由 repo 管理，使用公開 domain 與 ACME email 啟用 automatic HTTPS。
- [ ] R1.3 Caddy 憑證資料與設定狀態必須使用持久化 volume，避免容器重建後重複註冊憑證或遺失狀態。

### Requirement 2: Caddy ingress 不得破壞既有 callback 內部安全邊界
**GIVEN** `EcpayCallbackHttpServer` 目前固定綁定 loopback 並提供 `/` 與 callback route  
**AND** stage mode 下 callback server 不允許 public bind  
**WHEN** 專案切換成 Caddy ingress  
**THEN** bot 容器仍必須維持 `127.0.0.1:8085` loopback bind  
**AND** Caddy 只能代理既有 HTTP route，不得要求 callback server 改成 public bind 或改寫 business path semantics

**Requirements**:
- [ ] R2.1 Caddy 必須代理 `/` 與 `ECPAY_CALLBACK_PATH` 到 bot 的 `127.0.0.1:8085`。
- [ ] R2.2 既有 `APP_PUBLIC_BASE_URL` / `ECPAY_RETURN_URL` 推導與 override 優先序不得改變。
- [ ] R2.3 若 TLS / domain 設定缺失，部署文件與啟動行為必須讓 operator 能明確辨識無法對外提供安全 callback 入口。

## Error and Edge Cases
- [ ] domain 未設或填入無效 host 時，Compose / Caddy 設定必須能及早暴露錯誤，而不是悄悄退回不安全的對外模式。
- [ ] `80` / `443` 未對外開通、DNS 未指向 VPS、或 ACME 驗證失敗時，operator 必須能從 Caddy 日誌辨識簽證失敗原因。
- [ ] callback payload 與首頁請求都必須維持原本代理到 loopback server 的行為，不得因 ingress 切換造成 path 衝突。
- [ ] Caddy 持久化資料遺失或 volume 未掛載時，不得讓部署流程誤以為憑證已安全保存。
- [ ] stage mode 仍必須避免 public bind；TLS termination 只能發生在 ingress 層。

## Clarification Questions
None

## References
- Official docs:
  - https://caddyserver.com/docs/quick-starts/https
  - https://caddyserver.com/docs/caddyfile/directives/reverse_proxy
  - https://docs.docker.com/reference/compose-file/services/
- Related code files:
  - `docker-compose.yml`
  - `docker/nginx/default.conf`
  - `.env.example`
  - `src/main/java/ltdjms/discord/shop/services/EcpayCallbackHttpServer.java`
