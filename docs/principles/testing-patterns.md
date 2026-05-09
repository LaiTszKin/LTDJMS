# Testing Patterns

## Test Organization

Tests mirror the source package structure under `src/test/java/ltdjms/discord/`. Each module has corresponding test classes.

## Test Categories

### Pure Unit Tests
- Use JUnit 5 with `@Nested` + `@DisplayName` grouping
- AssertJ for fluent assertions (`.isPresent()`, `.contains()`, `.isEqualTo()`)
- Test both `Ok` and `Err` paths
- Test `DomainError` factory methods

**Evidence**: `ResultTest.java`, `CacheKeyGeneratorTest.java`

### Contract Tests
Define a private static test implementation of an interface, then verify the interface contract without mocking.

**Evidence**: `DiscordContextTest.java`, `DiscordEmbedBuilderTest.java`, `DiscordSessionManagerTest.java`

### Mockito Tests
- `@ExtendWith(MockitoExtension.class)` and `@Mock` fields
- `@BeforeEach setUp()` constructs the SUT
- `Mockito.when(...).thenReturn(...)` for stubbing
- `verify(mock).method(args)` for interaction verification
- `verify(mock, never()).method(...)` for negative tests

**Evidence**: `CacheInvalidationListenerTest.java`, `InteractionSessionManagerTest.java`

### Dagger Wiring Tests
Define test-specific `@Component` interfaces including only the modules under test. Verify Dagger can construct the object graph and `@IntoSet` multibindings work.

**Evidence**: `DomainEventPublisherDaggerWiringTest.java`, `AppComponentLoadTest.java`

### Integration Tests
- Suffixed `*IntegrationTest`, run during `verify` phase
- Use Testcontainers (PostgreSQL, Redis)
- Test full persistence and cache behavior

**Evidence**: `CacheInfrastructureIntegrationTest.java`, `RedisCacheServiceIntegrationTest.java`, `DatabaseMigrationRunnerIntegrationTest.java`

## Mock/Fake Strategy

Mock implementations live in the main source tree (`discord/mock/`), not in test sources:
- `MockDiscordContext` — testable Discord context with `ConcurrentHashMap` for options
- `MockDiscordInteraction` — records all calls in `ArrayList`s for verification
- `MockDiscordEmbedBuilder` — records builder state, produces real JDA `MessageEmbed`

This allows both unit tests and integration tests to use the same test doubles.

**Evidence**: See `src/main/java/ltdjms/discord/discord/mock/`

## Given-When-Then Structure

Tests consistently use Given-When-Then structure via comments:

```java
@Test
void shouldInvalidateBalanceCache() {
    // Given
    BalanceChangedEvent event = new BalanceChangedEvent(guildId, userId, 1000L);

    // When
    listener.accept(event);

    // Then
    verify(cacheService).invalidate(expectedKey);
}
```

## Build Verification

- JaCoCo enforces 80% line coverage at `verify` phase
- Dagger-generated classes, Jdbc/Jooq repositories, command handlers, and AI agent infrastructure are excluded from coverage targets
- Spotless checks Google Java Format compliance
- Four test profiles: `unit-tests`, `integration-tests`, `performance-tests`, `property-based-tests`

**Evidence**: See `pom.xml`
