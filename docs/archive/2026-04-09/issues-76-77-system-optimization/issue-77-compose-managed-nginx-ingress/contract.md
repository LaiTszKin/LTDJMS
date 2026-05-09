# Contract: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導

- Date: 2026-04-09
- Feature: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導
- Change Name: issue-77-compose-managed-nginx-ingress

## Purpose
本次變更同時受 ECPay `ReturnURL` 契約、Docker Compose namespace wiring，以及 Nginx reverse proxy 行為約束；若其中任何一層假設錯誤，就會造成 callback 打不到 bot 或部署者仍需手動維護 ingress 細節。

## Dependency Records

### Dependency 1: ECPay ReturnURL / Payment Results Notification
- Type: `API`
- Version / Scope: `ECPay payment callback docs`
- Official Source: `https://developers.ecpay.com.tw/16449/`, `https://developers.ecpay.com.tw/16538/`
- Why It Matters: `ReturnURL` 是 ECPay 付款完成後的 server-side callback URL；自動推導規則不能破壞這個契約。
- Invocation Surface:
  - Entry points: `OrderInfo.ReturnURL`, payment result notification POST
  - Call pattern: merchant create order → ECPay sends server-side POST callback
  - Required inputs: 完整可達的 callback URL、merchant credentials
  - Expected outputs: ECPay 送 callback 到 `ReturnURL`，merchant 回 `1|OK`
- Constraints:
  - Supported behavior: `ReturnURL` 作為 server-side payment notification URL
  - Limits: callback URL 需可被 ECPay 後台模擬／正式通知送達
  - Compatibility: 不得與 client-side redirect URL 混用
  - Security / access: callback URL 屬公開入口，不可依賴本地手工路由失誤來維持可用性
- Failure Contract:
  - Error modes: `ReturnURL` 配錯、callback 打不到、merchant 端未正確回 `1|OK`
  - Caller obligations: 提供可達的公開 callback URL；收到 callback 後正確回應
  - Forbidden assumptions: 不可假設 deployment operator 會手動維護一份永遠正確的外部 Nginx location
- Verification Plan:
  - Spec mapping: `R1.1-R1.3`, `R2.1-R2.2`
  - Design mapping: `Current Architecture`, `Component 1`
  - Planned coverage: `UT-77-01~03`, `CFG-77-01`
  - Evidence notes: ECPay 官方文件把 `ReturnURL` 定義為 server-side callback URL，ATM/CVS/BARCODE 可在後台模擬 callback 驗證

### Dependency 2: Docker Compose service network namespaces
- Type: `CLI`
- Version / Scope: `Compose spec / current Docker docs`
- Official Source: `https://docs.docker.com/reference/compose-file/services/`
- Why It Matters: 本設計依賴 `network_mode: service:{name}` 讓 Nginx 與 bot 共用 network namespace，從而代理 bot 的 loopback callback server，而不要求 bot 改成 public bind。
- Invocation Surface:
  - Entry points: `docker-compose.yml` service definition
  - Call pattern: `nginx` service joins `bot` service network namespace
  - Required inputs: `network_mode: "service:bot"`, service startup order
  - Expected outputs: Nginx 可存取 bot 的 `127.0.0.1:8085`
- Constraints:
  - Supported behavior: `service:{name}` 允許一個 service 存取另一個 service 的 container network namespace
  - Limits: 使用 `network_mode` 時，不可同時指定 `networks`
  - Compatibility: Compose rendering 必須合法；namespace 共享會帶來較強的 service coupling
  - Security / access: 共享 namespace 不等於將 bot port 直接映射到 host
- Failure Contract:
  - Error modes: network_mode / networks 衝突、namespace wiring 錯誤導致 Nginx 打不到 bot
  - Caller obligations: Compose 配置必須避免同時宣告 `networks`
  - Forbidden assumptions: 不可假設 sidecar Nginx 仍能透過 default network hostname 連到 bot 的 loopback
- Verification Plan:
  - Spec mapping: `R2.1-R2.2`, `R3.1`
  - Design mapping: `Component 2`, `Risk and Tradeoffs`
  - Planned coverage: `CFG-77-01`
  - Evidence notes: Docker 官方文件明確支援 `network_mode: service:{name}`，並禁止與 `networks` 同時使用

### Dependency 3: Nginx `proxy_pass`
- Type: `hosted service`
- Version / Scope: `NGINX OSS http proxy module`
- Official Source: `https://nginx.org/en/docs/http/ngx_http_proxy_module.html`
- Why It Matters: sidecar ingress 需要以 reverse proxy 方式把 `/` 與 callback route 轉發到 bot 內嵌 HTTP server。
- Invocation Surface:
  - Entry points: `location /`, `proxy_pass`, `proxy_set_header`
  - Call pattern: inbound HTTP → Nginx reverse proxy → bot loopback HTTP server
  - Required inputs: upstream URL、host headers
  - Expected outputs: proxied response from bot embedded server
- Constraints:
  - Supported behavior: `proxy_pass` 可將請求轉發至另一個 HTTP server
  - Limits: proxy config 要明確指定 upstream URL 與必要 headers
  - Compatibility: static config 即可支援本案的單 upstream 代理
  - Security / access: 需保留原始 Host / forwarded headers 給上游
- Failure Contract:
  - Error modes: upstream 連線失敗、路由未命中、設定語法錯誤
  - Caller obligations: 讓 upstream 位址與 bot 實際 bind 一致
  - Forbidden assumptions: 不可假設 Nginx 自動知道 callback path 或 bot 監聽位址
- Verification Plan:
  - Spec mapping: `R2.1-R2.2`
  - Design mapping: `Component 2`, `Validation Plan`
  - Planned coverage: `CFG-77-01`
  - Evidence notes: Nginx 官方 `proxy_pass` 範例顯示 `location / { proxy_pass http://localhost:8000; ... }` 為標準反向代理模式
