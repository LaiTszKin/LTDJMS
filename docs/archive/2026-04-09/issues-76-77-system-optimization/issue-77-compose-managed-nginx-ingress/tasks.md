# Tasks: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導

- Date: 2026-04-09
- Feature: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導

## **Task 1: 新增公開 base URL 推導規則**

對應 `R1.1-R1.3`，核心目標是把 `APP_PUBLIC_BASE_URL` 變成 Compose 自架部署的主要公開入口設定。

- 1. [x] 擴充 `EnvironmentConfig` 與對應測試
  - 1.1 [x] 新增 `APP_PUBLIC_BASE_URL` 讀取與正規化邏輯
  - 1.2 [x] 讓 `getEcpayReturnUrl()` 在顯式值缺席時自動推導 callback URL

## **Task 2: 新增 Compose 管理的 Nginx ingress**

對應 `R2.1-R2.2`、`R3.1`，核心目標是在不改 callback 業務邏輯的情況下，把對外 HTTP 入口收進 repo 管理。

- 2. [x] 建立 sidecar Nginx 與 Compose wiring
  - 2.1 [x] 新增 repo 內的 Nginx 設定檔並代理到 bot 內嵌 HTTP server
  - 2.2 [x] 更新 `docker-compose.yml`，讓自架部署預設帶出 Nginx ingress

## **Task 3: 文件與交付驗證對齊**

對應 `R3.2-R3.3`，核心目標是把使用者操作說明收斂成「填 base URL」而非「自己配 callback bind / 外部 Nginx」。

- 3. [x] 更新 `.env.example` 與部署文件
  - 3.1 [x] 把 `APP_PUBLIC_BASE_URL` 納入主要設定說明，將 `ECPAY_RETURN_URL` 降為 override
  - 3.2 [x] 以 `docker compose config` 驗證 Compose ingress 組態

## Notes
- 本 spec 不移除 Vercel workflow；只要求文件清楚標示它不是 Compose 自架 callback ingress 的必要組件。
- 本 spec 不新增 TLS 自動化；若 `APP_PUBLIC_BASE_URL` 使用 `https://`，預設假設 TLS 在 ingress 之外終結。
