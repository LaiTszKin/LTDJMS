# Contract: Fiat order expiry lifecycle

- Date: 2026-04-14
- Feature: Fiat order expiry lifecycle
- Change Name: fiat-order-expiry-lifecycle

## Purpose
法幣訂單的到期狀態與 ECPay 付款期限直接相關；本文件記錄付款期限、查單補償與 caller obligations 的官方依據，避免本地狀態模型再次脫離上游契約。

## Usage Rule
- 本規格只記錄會直接影響 `fiat_order` 生命周期設計的外部契約。
- 若 implementation 另外引入人工取消 API 或新的金流渠道，需新增 dependency record。

## Dependency Records

### Dependency 1: ECPay B2C 站內付 / CVS 付款期限契約
- Type: `API / payment platform`
- Version / Scope: `Not fixed`
- Official Source: `https://developers.ecpay.com.tw/16449/`
- Why It Matters: `FiatOrderService` 建單後對買家宣告付款期限，本地 order lifecycle 必須能保存並執行該期限語義，並在逾期後產生可查詢的 terminal state
- Invocation Surface:
  - Entry points: 建單回應中的付款期限欄位（例如 `ExpireDate`）與相關付款資訊
  - Call pattern: `sync request/response`
  - Required inputs: 商店建單參數、付款方式設定
  - Expected outputs: 付款號碼、到期資訊、買家付款說明、可持久化的 local `expireAt`
- Constraints:
  - Supported behavior: 上游會回傳可供買家付款的期限資訊；本地不得把「停止查單」誤當成「已取消」
  - Limits: 實際到期時間由 ECPay 回傳資料決定；若無法解析，系統會退回到 request-time + configured expiry minutes 的保守計算
  - Compatibility: 本地欄位與文案需能表示上游到期語義，並保留 terminal reason
  - Security / access: 使用既有商店憑證與 callback/query 設定
- Failure Contract:
  - Error modes: 若建單失敗或未取得完整付款資訊，不得建立宣稱可付款的 pending order
  - Caller obligations: 保存上游提供的付款期限或明確計算依據，並用於後續 lifecycle transition 與 terminal reason trace
  - Forbidden assumptions: 不可假設 7 天 reconciliation window 等同付款有效期
- Verification Plan:
  - Spec mapping: `R1.1-R1.3`, `R3.1`
  - Design mapping: `Data / State Impact`, `Sequence / Control Flow`
  - Planned coverage: `UT-expire-at-persisted`, `IT-order-expires-after-deadline`
  - Evidence notes: ECPay 文件描述付款方式與付款期限資訊為建單輸出的核心部分，實作已將 `ExpireDate` 轉為 local `expireAt`

### Dependency 2: ECPay QueryTradeInfo / 補償查單契約
- Type: `API / payment platform`
- Version / Scope: `Not fixed`
- Official Source: `https://developers.ecpay.com.tw/16538/`
- Why It Matters: reconciliation 只能對仍可付款的訂單查單，且需與本地 expiry transition 協調，避免 terminal orders 再次參與補償查單
- Invocation Surface:
  - Entry points: `QueryTradeInfo` / 查單 API
  - Call pattern: `polling`
  - Required inputs: 訂單編號、商店驗證資訊
  - Expected outputs: paid/unpaid state、交易資訊
- Constraints:
  - Supported behavior: 官方查單只能回答目前交易狀態；不會替本地建立 expired terminal state
  - Limits: 查單結果可能晚於本地排程執行，需處理 race
  - Compatibility: 本地 `PENDING_PAYMENT`、`PAID`、`EXPIRED` 狀態需對應查單使用時機
  - Security / access: 使用既有 ECPay 查單憑證
- Failure Contract:
  - Error modes: 暫時失敗、超時、仍未付款
  - Caller obligations: 僅在仍有效的 pending order 上重試；expiry sweep 必須先於 reconciliation 執行，避免對 terminal state 重複查單
  - Forbidden assumptions: 不可假設「查不到已付款」等於「已自動取消」
- Verification Plan:
  - Spec mapping: `R2.1-R2.3`, `R3.3`
  - Design mapping: `Proposed Architecture`, `Validation Plan`
  - Planned coverage: `IT-reconciliation-excludes-expired`, `UT-paid-vs-expired-race`, `UT-expire-transition`
  - Evidence notes: 查單 API 只回傳交易狀態，需要本地自行管理 lifecycle terminal state
