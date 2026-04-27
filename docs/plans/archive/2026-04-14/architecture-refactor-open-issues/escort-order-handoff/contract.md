# Contract: Escort order handoff

- Date: 2026-04-14
- Feature: Escort order handoff
- Change Name: escort-order-handoff

## Purpose
本規格處理的是 repo 內部 aggregate handoff，沒有第三方 API 或 SDK 直接限制欄位設計；關鍵契約來自內部模組邊界，而非外部服務。

## Usage Rule
- 若 implementation 階段將 dispatch handoff 暴露到新的外部 callback / API，再補充 dependency record。
- 目前以內部資料模型與狀態責任為主，不額外虛構外部依賴。

## Dependency Records
None（`product`、`shop`、`dispatch` 都是 repo 內部模組；本次 issue 的根因是 aggregate 交接沒有落成 durable state，而不是外部服務契約錯誤。）
