# Naming Conventions

## Package Structure

All source code lives under `src/main/java/ltdjms/discord/`. Each business module uses a flat package name (e.g., `currency`, `gametoken`, `shop`, `dispatch`) without prefixed grouping.

Internal module structure follows consistent sub-packages:
- `domain/` — Records, repository interfaces, domain exceptions
- `persistence/` — JDBC/jOOQ repository implementations
- `services/` — Business logic orchestration
- `commands/` — Discord event handling

Example: `src/main/java/ltdjms/discord/shop/domain/FiatOrder.java`, `shop/persistence/JdbcFiatOrderRepository.java`, `shop/services/FiatOrderService.java`, `shop/commands/ShopCommandHandler.java`

## Class Naming

- **Repository interfaces**: Named after the entity, e.g., `FiatOrderRepository`, `ProductRepository`, `GameTokenAccountRepository`
- **JDBC implementations**: Prefixed with `Jdbc`, e.g., `JdbcFiatOrderRepository`, `JdbcProductRepository`
- **Service implementations**: Prefixed with `Default` when an interface exists, e.g., `DefaultBalanceService implements BalanceService`
- **Command handlers**: Postfixed with `Handler`, e.g., `ShopCommandHandler`, `UserPanelButtonHandler`
- **View/Builder classes**: Postfixed with `View`, `Factory`, or `Builder`, e.g., `ShopView`, `AdminPanelViewFactory`, `UserPanelEmbedBuilder`
- **Adapters**: Named after the source type, e.g., `SlashCommandAdapter`, `ButtonInteractionAdapter`
- **Mock implementations**: Prefixed with `Mock`, e.g., `MockDiscordContext`, `MockDiscordInteraction`

## Method Naming

- **Factory methods**: `create*()` for new entity instances, e.g., `FiatOrder.createPending()`, `EscortDispatchOrder.createManualOpenOrder()`
- **State transitions**: `mark*IfNeeded` for idempotent updates, `claim*Processing` / `release*Processing` for worker locking
- **Domain transitions**: Prefixed with `with`, e.g., `withRedeemed(userId)`, `withConfirmed()`, `withAdjustedBalance(amount)`
- **Validation**: `isValid()`, `isPaid()`, `isTerminal()`, `hasEnoughTokens()`, `belongsToGuild()`
- **Repository query**: `find*`, `save`, `saveAll`, `update`, `delete*`, `exists*`
- **Service orchestration**: Verb phrases describing the action, e.g., `purchaseProduct()`, `redeemCode()`, `generateCodes()`, `confirmOrder()`

## Constants

- Environment variable keys: `ENV_*` prefix with UPPER_SNAKE_CASE, e.g., `ENV_DISCORD_BOT_TOKEN`, `ENV_AI_SERVICE_API_KEY`
- Config paths: `CFG_*` prefix with dotted lowercase, e.g., `CFG_DISCORD_BOT_TOKEN`, `db.pool.maximum-pool-size`
- Default values: `DEFAULT_*` prefix, e.g., `DEFAULT_AI_SERVICE_MODEL`, `DEFAULT_REDIS_URI`
- Slash command names: `CMD_*` prefix, e.g., `CMD_SHOP`, `CMD_USER_PANEL`
- Discriminated event/source types: UPPER_SNAKE_CASE enum values, e.g., `CurrencyTransaction.Source.PRODUCT_PURCHASE`, `FiatOrder.Status.PAID`

Evidence: See `EnvironmentConfig.java`, `SlashCommandListener.java`, `CurrencyTransaction.java`, `FiatOrder.java`
