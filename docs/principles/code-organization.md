# Code Organization

## Module Independence

Each business module is self-contained within its package tree. Cross-module dependencies flow through injected service interfaces, never through command handlers or view classes.

The only cross-module integration points are:
- `ShopSelectMenuHandler` → `CurrencyPurchaseService` + `EscortDispatchHandoffService`
- `FiatOrderPostPaymentWorker` → `EscortDispatchHandoffService` + `ProductRewardService`
- `ProductRewardService` → `BalanceAdjustmentService` + `GameTokenService`
- `Panel facades` → multiple service interfaces

## Handler Thinness

Command handlers (`commands/`) are responsible only for:
- Input validation (Discord option types, permissions)
- Calling service methods
- Building Discord responses (embeds, components)
- Error handling for Discord interaction failures

Complex response assembly is delegated to factory/builder classes (e.g., `ShopView`, `AdminPanelViewFactory`, `DispatchPanelMessageFactory`).

**Evidence**: Compare `DispatchPanelInteractionHandler.java` (1233 lines, orchestrates complex multi-step DM flows) with simpler handlers like `ShopCommandHandler.java` (calls service, embeds result).

## Record Immutability

Domain models are Java records with:
- Immutable state (all fields final)
- Factory methods on records for creation (`createPending`, `createManualOpenOrder`)
- `with*` methods for state transitions returning new instances
- Validation in compact constructors

**Evidence**: `FiatOrder.java`, `EscortDispatchOrder.java`, `MemberCurrencyAccount.java`, `Product.java`

## View Separation

Discord embed and component assembly is separated from business logic:
- `ShopView` — static embed builders for shop UI
- `AdminPanelViewFactory` — admin panel embed builders
- `DispatchPanelView` — dispatch panel embed builders
- `DispatchPanelMessageFactory` — DM notification embed builders (10+ distinct states)
- `UserPanelEmbedBuilder` — user panel embed
- `PanelComponentRenderer` — delegates to `DiscordComponentRenderer`

## Facade Pattern for Panel Operations

Admin panel operations use facade classes that aggregate multiple service interfaces:
- `CurrencyManagementFacade` — balance + config services
- `GameTokenManagementFacade` — token services
- `GameConfigManagementFacade` — dice game config services
- `AIConfigManagementFacade` — AI allowlist + agent config services
- `MemberInfoFacade` — balance + token + transaction + redemption services

**Evidence**: See `src/main/java/ltdjms/discord/panel/services/`
