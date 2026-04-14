# Contract: AI agent channel gating

- Date: 2026-04-14
- Feature: AI agent channel gating
- Change Name: ai-agent-channel-gating

## Purpose
此變更只重構 repo 內部的 mention routing 與設定邊界，沒有外部 SDK / API 契約直接決定需求內容。

## Usage Rule
- 若後續 implementation 引入新的 Discord 平台限制判斷，再補充正式 dependency record。
- 本版規格以 repo 內證據為主，外部依賴不構成本次設計約束。

## Dependency Records
None（此次變更聚焦於 `aichat` / `aiagent` / `panel` 之間的內部 routing 責任分離；Discord/JDA 並未為此 issue 定義新的外部契約。）
