# AI Routing and Agent Architecture

## Mention Routing Decision Matrix

```
MessageReceivedEvent (bot mentioned)
  -> Agent config check:
     |-- Unavailable (exception) -> DENY (fail-closed)
     |-- Agent enabled -> AGENT_ROUTE
     |-- Agent NOT enabled -> AI allowlist check:
          |-- Channel or category in allowlist -> AI_CHAT_ROUTE
          |-- Not in allowlist -> DENY
```

Two independent configuration stores:
- **AI allowlist** (`ai_channel_restriction` + `ai_category_restriction` tables): Controls which channels can use basic AI chat
- **Agent enablement** (`ai_agent_channel_config` table): Controls which channels have access to Discord administration tools

## AI Chat Architecture

```
AIChatMentionListener
  -> AIChatMentionRoutingDecision.decide()
  -> LangChain4jAIChatService (streaming)
    -> DefaultPromptLoader (filesystem prompts)
    -> MarkdownValidatingAIChatService (decorator)
      -> DiscordMarkdownStreamProcessor pipeline:
         segment -> sanitize -> validate -> auto-fix -> paginate
```

Streaming response handling:
- Chunk types: `REASONING`, `TOOL_INTENT`, `CONTENT`
- Reasoning content shown as spoiler-formatted text when `AI_SHOW_REASONING` is enabled
- Messages are split at paragraph boundaries or 1980-character limit

## Agent Architecture

```
AIChatMentionListener (AGENT_ROUTE path)
  -> LangChain4jAIChatService (with tools enabled)
    -> LangChain4jAgentService (LangChain4j @AiService)
      -> StreamingChatModel (OpenAI-compatible)
      -> ChatMemoryProvider (SimplifiedChatMemoryProvider)
        -> DiscordThreadHistoryProvider (thread history from Discord API)
        -> InMemoryToolCallHistory (current session tool calls)
      -> 17 Discord administration tools (JDA-based)

Tool execution:
  -> ToolCallerAuthorizationGuard (ADMINISTRATOR check)
  -> ToolExecutionInterceptor (audit logging)
    -> tool_execution_log table
    -> DomainEventPublisher (LangChain4jToolExecutedEvent)
```

## Prompt Loading

Prompts are loaded from the filesystem (`PROMPTS_DIR_PATH`, default `./prompts/`):
- **Required**: `system/` subdirectory — loaded on every AI request
- **Optional**: `agent/` subdirectory — merged when agent mode is enabled
- Files are `.md`, sorted alphabetically, each becomes a `PromptSection`
- Size limit: 1MB per file (configurable via `PROMPT_MAX_SIZE_BYTES`)

## Markdown Processing Pipeline

```
Raw AI response chunk
  -> MarkdownHeadingSegmenter (split at heading boundaries)
    -> per segment:
      -> DiscordMarkdownSanitizer (remove HTML, collapse blockquotes, convert tables)
      -> MarkdownValidator (CommonMark parsing: detect unsupported syntax)
      -> if invalid: MarkdownAutoFixer (regex-based fixes)
        -> re-validate
      -> DiscordMarkdownPaginator (split at 1900 char page limit)
    -> emit validated pages to Discord
```
