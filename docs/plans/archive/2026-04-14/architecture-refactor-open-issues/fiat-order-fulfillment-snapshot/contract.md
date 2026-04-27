# Contract: Fiat order fulfillment snapshot

- Date: 2026-04-14
- Feature: Fiat order fulfillment snapshot
- Change Name: fiat-order-fulfillment-snapshot

## Purpose
本次變更的核心是 repo 內部資料模型重構，不依賴第三方 API 決定快照欄位；因此無需建立外部依賴契約記錄。

## Usage Rule
- 若 implementation 階段把 snapshot 直接序列化給外部服務，再補上相應 dependency record。
- 目前所有決策都由內部 `shop` / `product` / `dispatch` 邊界決定。
- `fiat_order` 的 fulfillment snapshot 欄位屬於內部 schema contract；已採 additive migration，歷史資料不自動回填，需由維護者人工檢視是否需要重建訂單。

## Dependency Records
None（issue 的根因是 `fiat_order` 與 `product` 之間的 truth ownership 錯置，不是外部服務契約不清。）
