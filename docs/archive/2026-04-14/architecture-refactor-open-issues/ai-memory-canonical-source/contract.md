# Contract: AI memory canonical source

- Date: 2026-04-14
- Feature: AI memory canonical source
- Change Name: ai-memory-canonical-source

## Purpose
AI Agent 的 conversation memory 實作直接建立在 LangChain4j `ChatMemoryProvider` 抽象上；本文件記錄官方抽象對 runtime owner 的要求，避免本地設計同時維持多個名義上的 memory source。

## Usage Rule
- 只記錄直接影響 runtime memory 形狀與責任劃分的官方依據。
- 任何本地 audit / diagnostic storage 都不應被包裝成 ChatMemoryProvider 除非它真的參與 runtime 還原。

## Dependency Records

### Dependency 1: LangChain4j ChatMemory / ChatMemoryProvider
- Type: `library`
- Version / Scope: `Not fixed`
- Official Source: `https://docs.langchain4j.dev/tutorials/chat-memory/`
- Why It Matters: 本地已選定 `SimplifiedChatMemoryProvider` 作為唯一 runtime canonical owner；`PersistentChatMemoryProvider` 與 `RedisPostgresChatMemoryStore` 只保留 legacy / compatibility / audit 語境
- Invocation Surface:
  - Entry points: `ChatMemoryProvider#get(Object memoryId)`、`ChatMemory`
  - Call pattern: `sync in-process`
  - Required inputs: conversation/memory id、上游準備好的訊息來源
  - Expected outputs: runtime 對話上下文視窗
- Constraints:
  - Supported behavior: `ChatMemoryProvider` 應明確定義如何為給定 memory id 建構/取得 runtime memory
  - Limits: 抽象本身不負責自動提供 persistence；是否持久化由實作者決定
  - Compatibility: 本地 provider 命名與文件必須對應實際行為
  - Security / access: 無額外外部授權模型
- Failure Contract:
  - Error modes: memory source 不可用、memory id 無效、底層訊息來源缺失
  - Caller obligations: 為同一 memory id 提供一致語意；避免同時存在多個互相衝突的 runtime provider
  - Forbidden assumptions: 不可假設「存在資料表」就等於「runtime 會使用該資料表還原上下文」
- Verification Plan:
  - Spec mapping: `R1.1-R3.3`
  - Design mapping: `Current Architecture`, `Proposed Architecture`, `Component Changes`
  - Coverage: `AIAgentModuleTest`, `SimplifiedChatMemoryProviderTest`, `LangChain4jAIChatServiceTest`, `PersistentChatMemoryProviderTest`, `RedisPostgresChatMemoryStoreTest`, docs/backfill
  - Evidence notes: 官方文件把 ChatMemoryProvider 定位為 runtime conversation memory 的提供者，而非模糊的歷史存放集合；本地 runtime wiring 已與此一致
