# Error Handling

## Result Type

Business logic uses a sealed `Result<T, E>` type modeled after Rust's `Result`:

```java
sealed interface Result<T, E> permits Ok, Err { ... }
```

- `Ok<T, E>(T value)` — success path, throws on `getError()`
- `Err<T, E>(E error)` — failure path, throws on `getValue()`
- Both records enforce `Objects.requireNonNull` on their carried value

Standard combinators: `map()`, `flatMap()`, `mapError()`, `getOrElse(T)`.

A singleton `Result.okVoid()` returns `Result<Unit, E>` for void-success operations.

**Evidence**: See `src/main/java/ltdjms/discord/shared/Result.java`, `src/main/java/ltdjms/discord/shared/Unit.java`

## DomainError Categories

Errors are categorized via `DomainError.Category` enum:

- **Input validation**: `INVALID_INPUT`, `INSUFFICIENT_BALANCE`, `INSUFFICIENT_PERMISSIONS`
- **Persistence**: `PERSISTENCE_FAILURE`
- **Discord interaction**: `DISCORD_INTERACTION_TIMEOUT`, `DISCORD_HOOK_EXPIRED`, `DISCORD_RATE_LIMITED`, `DISCORD_MISSING_PERMISSIONS`
- **AI service**: `AI_SERVICE_TIMEOUT`, `AI_SERVICE_AUTH_FAILED`, `AI_SERVICE_RATE_LIMITED`, `AI_SERVICE_UNAVAILABLE`, `AI_RESPONSE_EMPTY`, `AI_RESPONSE_INVALID`
- **Prompt loading**: `PROMPT_DIR_NOT_FOUND`, `PROMPT_FILE_TOO_LARGE`, `PROMPT_READ_FAILED`, `PROMPT_INVALID_ENCODING`
- **Channel restrictions**: `CHANNEL_NOT_ALLOWED`, `CHANNEL_NOT_FOUND`, `DUPLICATE_CHANNEL`, `CATEGORY_NOT_FOUND`, `DUPLICATE_CATEGORY`

Static factory methods in `DomainError` provide convenient creation for the most common categories.

**Evidence**: See `src/main/java/ltdjms/discord/shared/DomainError.java`

## Exception Handling

### Startup Failures
- Missing required config (Discord token, AI API key) throws `IllegalStateException` — bot fails to start
- Schema migration failure throws `SchemaMigrationException` — bot fails to start
- AI module assembly fails if API key is missing

### Runtime Error Isolation
- `DomainEventPublisher` catches per-listener exceptions: one failing listener never blocks others
- `CacheService` implementations (Redis, NoOp) are designed to never throw — errors are logged
- Bot error handler (`BotErrorHandler`) catches unexpected exceptions in Discord interactions and surfaces them to users

### When NOT to use Result
- Configuration validation at startup uses exceptions (fail-fast)
- Infrastructure errors (database connection, Redis unavailable) use exceptions
- Runtime gateways that throw `IllegalStateException` for not-ready states

**Evidence**: See `DomainEventPublisher.java`, `CacheService.java`, `RedisCacheService.java`, `BotErrorHandler.java`
