# Contract: Interactive env setup assistant

- Date: 2026-04-09
- Feature: Interactive env setup assistant
- Change Name: interactive-env-setup-assistant

## Purpose
這個 spec 主要是 repo 內 shell 腳本與 `.env` operator workflow 的重構；沒有新的外部平台、API、SDK 或 hosted service 契約主導實作，真正需要對齊的是 repo 內的 `.env.example`、`scripts/sync-env.sh` 與 `Makefile` 現況。

## Usage Rule
- Write one dependency record per external library, framework, SDK, API, CLI, platform service, or hosted system that materially constrains implementation.
- If no external dependency materially affects the change, write `None` under `## Dependency Records` and briefly explain why this document is not needed for the current scope.
- Every claim in this file must be backed by the official documentation or the verified upstream source actually used during planning.

## Dependency Records
None

Reason:
- 互動式設定助手不新增外部網路整合；它只重組 repo 內既有 `.env` 範本與腳本入口。
- 實作約束主要來自本專案既有設定欄位與 operator workflow，而非外部服務契約。
