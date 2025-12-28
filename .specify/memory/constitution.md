<!--
Sync Impact Report:
==================
Version change: (none) -> 1.0.0

Modified principles: N/A (initial constitution)
Added sections:
  - Core Principles (7 principles)
  - Development Standards
  - Quality Gates
  - Governance

Removed sections: N/A (initial constitution)

Templates requiring updates:
  - .specify/templates/plan-template.md (Constitution Check section) -  needs to reference new principles
  - .specify/templates/spec-template.md (requirements alignment) - needs to reflect testing and documentation standards
  - .specify/templates/tasks-template.md (task categorization) - needs to align with TDD principle

Follow-up TODOs: None
-->

# LTDJMS Project Constitution

## Core Principles

### I. Test-Driven Development (NON-NEGOTIABLE)

**MANDATORY**: TDD is strictly enforced for all new features and bug fixes.

- **Red-Green-Refactor cycle MUST be followed**:
  1. **Red**: Write tests first describing expected behavior
  2. **Green**: Implement minimal code to make tests pass
  3. **Refactor**: Optimize code structure while keeping tests green

- **Test execution order**: Tests MUST fail before implementation begins
- **Coverage requirement**: Minimum 80% line coverage for all production code
- **Error paths MUST be tested**: Including domain errors (insufficient balance, invalid input, etc.)
- **Rationale**: Ensures code quality, prevents regressions, and serves as living documentation

### II. Domain-Driven Design & Layered Architecture

**MANDATORY**: All features MUST respect the layered architecture pattern.

- **Layer separation is NON-NEGOTIABLE**:
  - `domain/`: Business entities and rules, NO infrastructure details
  - `persistence/`: Repository interfaces and implementations
  - `services/`: Business logic composing multiple repositories
  - `commands/`: JDA event handlers converting Discord events to service calls

- **Cross-layer dependencies flow DOWNWARD only**: Commands → Services → Repositories → Domain
- **Domain objects MUST be infrastructure-agnostic**: No JDA, no JDBC, no framework types in domain
- **Result<T, DomainError> pattern**: All service methods return typed results, not exceptions
- **Rationale**: Maintains testability, separates concerns, enables independent evolution of layers

### III. Configuration Flexibility

**MANDATORY**: Configuration MUST support multiple sources with clear precedence.

- **Configuration precedence (highest to lowest)**:
  1. System environment variables
  2. `.env` file in project root
  3. `application.conf` / `application.properties`

- **ALL configuration MUST be externalizable**: No hardcoded values in production code
- **Environment-specific configs**: Development, staging, and production settings
- **Rationale**: Enables deployment across environments without code changes

### IV. Database Schema Management

**MANDATORY**: Database changes MUST use structured migrations.

- **Flyway migrations required**:
  - All schema changes go through `src/main/resources/db/migration/`
  - Migration files MUST be versioned and ordered
  - Non-destructive changes preferred (additive migrations)

- **Schema enforcement**:
  - `src/main/resources/db/schema.sql` is the source of truth
  - Application startup validates schema consistency
  - Breaking changes require manual migration scripts

- **Rationale**: Prevents data loss, enables rollbacks, maintains schema-version alignment

### V. Observability & Structured Logging

**MANDATORY**: All operations MUST be observable through structured logging.

- **Logging requirements**:
  - Use SLF4J with Logback for all logging
  - Structured logs for operations (command execution, errors, metrics)
  - Log levels: ERROR (failures), WARN (recoverable issues), INFO (operations), DEBUG (details)

- **Metrics collection**:
  - Slash command execution time and success/failure rates
  - Service method performance tracking
  - Database query performance monitoring

- **Rationale**: Enables production debugging, performance optimization, and operational insight

### VI. Dependency Injection & Modularity

**MANDATORY**: All dependencies MUST be managed through Dagger 2.

- **DI requirements**:
  - Use `AppComponent` as the root composition point
  - All services and repositories injected, not instantiated directly
  - Single-responsibility modules for each feature area

- **Modularity**:
  - Feature modules: `currency/`, `gametoken/`, `panel/`, `product/`, `redemption/`
  - Shared infrastructure in `shared/` package
  - Clear module boundaries with minimal coupling

- **Rationale**: Improves testability, enables independent development, reduces coupling

### VII. Error Handling & User Experience

**MANDATORY**: All errors MUST be handled gracefully with user-friendly Discord responses.

- **Error handling requirements**:
  - Use `Result<T, DomainError>` for recoverable business errors
  - Use exceptions only for truly unexpected failures
  - All command handlers translate errors to ephemeral Discord messages

- **Domain error categories**:
  - `INVALID_INPUT`: User-provided data validation failures
  - `INSUFFICIENT_BALANCE`: Currency or token balance too low
  - `INSUFFICIENT_TOKENS`: Game token balance too low
  - `PERSISTENCE_FAILURE`: Database operation failures
  - `UNEXPECTED_FAILURE`: Unknown system errors

- **Rationale**: Provides clear feedback to users, prevents confusion, maintains bot stability

## Development Standards

### Code Style

- **Java 17+ language features**: Use records, pattern matching, text blocks where appropriate
- **Immutability preferred**: Domain objects should be immutable where possible
- **Clear naming**: Classes, methods, and variables must self-document their purpose
- **Single responsibility**: Each class/method has one clear reason to change

### Documentation

- **All public APIs MUST have Javadoc**: Methods, classes, and public fields
- **Feature documentation**: New features require updates to `docs/modules/` and `docs/api/`
- **Architecture diagrams**: Mermaid diagrams in `docs/architecture/` for complex flows
- **README updates**: Public-facing features documented in README.md

### Git Workflow

- **Conventional Commits format**: `feat(scope): description`, `fix(scope): description`, etc.
- **Feature branches**: `feature/feature-name`, `fix/bug-name` from `main`
- **Pull requests required**: All changes go through PR review
- **Squash merge**: Maintain clean commit history on main branch

## Quality Gates

### Pre-Commit Checklist

- [ ] All tests pass: `make test-integration`
- [ ] Coverage meets 80% threshold: `make coverage-check`
- [ ] Code compiles without warnings: `make build`
- [ ] Documentation updated for user-facing changes

### Continuous Integration

- **All PRs MUST pass CI** before merge consideration
- **CI checks include**:
  - Unit tests (`mvn test`)
  - Integration tests (`mvn verify`)
  - Code coverage (JaCoCo)
  - Security scanning (dependency vulnerabilities)

### Release Criteria

- **All tests passing**: Unit and integration
- **Coverage threshold met**: 80% across all modules
- **Documentation complete**: All new features documented
- **Changelog updated**: `CHANGELOG.md` reflects all changes
- **Migration scripts ready**: For any database schema changes

## Governance

### Amendment Procedure

1. **Proposal**: Document proposed change with rationale in GitHub Issue
2. **Discussion**: Team review and feedback period (minimum 3 days)
3. **Approval**: Requires consensus from core maintainers
4. **Implementation**: Update constitution.md with version bump
5. **Propagation**: Update dependent templates and documentation

### Versioning Policy

- **MAJOR**: Breaking governance changes or removed principles
- **MINOR**: New principles or materially expanded guidance
- **PATCH**: Clarifications, wording improvements, non-semantic changes

### Compliance Review

- **All PRs MUST verify compliance** with core principles
- **Constitution violations require explicit justification** in PR description
- **Complexity MUST be justified**: Any deviation from simplicity principles needs documented rationale
- **Runtime guidance**: See `docs/development/workflow.md` for day-to-day development practices

### Enforcement

- **PR reviews**: Constitution compliance is part of review checklist
- **CI gates**: Automated enforcement of test coverage and quality gates
- **Team culture**: Constitution is a living document, evolve through discussion

**Version**: 1.0.0 | **Ratified**: 2025-12-27 | **Last Amended**: 2025-12-27
