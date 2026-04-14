# Contract: Admin panel session refresh

- Date: 2026-04-14
- Feature: Admin panel session refresh
- Change Name: admin-panel-session-refresh

## Purpose
本次變更處理 repo 內部 session abstraction 與 event-driven refresh，不涉及新的外部服務契約。

## Usage Rule
- 若 implementation 階段引入 Discord rate limit / REST edit 批次策略，需再補充外部契約。
- 目前以內部 session 模型與 view metadata 為主。

## Dependency Records
None（issue 的根因是內部 session abstraction 缺少 guild-wide traversal 與 view metadata，不是外部平台契約未滿足。）
