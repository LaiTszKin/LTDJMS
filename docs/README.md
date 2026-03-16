# LTDJMS Documentation

## Start Here

- Project overview: `README.md`
- Setup and deployment: `docs/getting-started.md`
- Configuration and external services: `docs/configuration.md`
- Architecture: `docs/architecture.md`
- Features and workflows: `docs/features.md`
- Developer guide: `docs/developer-guide.md`

## Document Guide

### Getting Started
- Audience: operators / developers
- Covers: prerequisites, local setup, Docker Compose workflow, deployment checks, smoke verification

### Configuration
- Audience: operators / developers
- Covers: env vars, config files, secrets, Discord / AI / ECPay / Redis / fulfillment integration

### Architecture
- Audience: developers / maintainers
- Covers: entrypoints, module boundaries, event pipeline, storage and callback flows

### Features
- Audience: admins / support / developers
- Covers: slash commands, user workflows, payment and dispatch flows, AI behavior guardrails

### Developer Guide
- Audience: developers / maintainers
- Covers: domain concepts, risk hotspots, testing expectations, debugging entrypoints, doc maintenance notes

## Supplemental References

- Slash commands: `docs/api/slash-commands.md`
- Detailed architecture: `docs/architecture/overview.md`, `docs/architecture/data-model.md`, `docs/architecture/sequence-diagrams.md`, `docs/architecture/cache-architecture.md`
- Module deep dives: `docs/modules/*.md`
- Development references: `docs/development/*.md`
- Operations references: `docs/operations/*.md`

## Reference List

- Source specs reviewed:
  - `2026-03-04 ecpay-payment-callback-fulfillment`
  - `2026-03-05 admin-panel-settings-embed-workflow`
  - `2026-03-10 unified-domain-event-pipeline`
- Existing docs updated:
  - `README.md`
  - `docs/README.md`
  - `docs/getting-started.md`
  - `docs/configuration.md`
  - `docs/architecture.md`
  - `docs/features.md`
  - `docs/developer-guide.md`
- Important code/config references:
  - `Makefile`
  - `docker-compose.yml`
  - `Dockerfile`
  - `.env.example`
  - `src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`
  - `src/main/java/ltdjms/discord/currency/bot/DiscordCurrencyBot.java`
  - `src/main/java/ltdjms/discord/currency/bot/SlashCommandListener.java`
  - `src/main/java/ltdjms/discord/shared/events/DomainEventPublisher.java`
  - `src/main/java/ltdjms/discord/shop/services/EcpayCallbackHttpServer.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatPaymentCallbackService.java`
- Remaining unknowns:
  - 生產環境回滾流程沒有專用腳本或 CI 工作流程證據
  - 部分外部服務僅能從現有文件得知入口，repo 未提供完整帳號申請 SOP
