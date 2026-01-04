package ltdjms.discord.panel.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.aiagent.services.AIAgentChannelConfigService;
import ltdjms.discord.aichat.domain.AllowedCategory;
import ltdjms.discord.aichat.domain.AllowedChannel;
import ltdjms.discord.aichat.services.AIChannelRestrictionService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.Unit;

/** Unit tests for AIConfigManagementFacade. */
@ExtendWith(MockitoExtension.class)
class AIConfigManagementFacadeTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;
  private static final long TEST_CHANNEL_ID = 987654321098765432L;
  private static final long TEST_CATEGORY_ID = 111222333444555666L;

  @Mock private AIChannelRestrictionService aiChannelRestrictionService;
  @Mock private AIAgentChannelConfigService aiAgentChannelConfigService;

  private AIConfigManagementFacade facade;

  @BeforeEach
  void setUp() {
    facade = new AIConfigManagementFacade(aiChannelRestrictionService, aiAgentChannelConfigService);
  }

  @Nested
  @DisplayName("getAllowedChannels")
  class GetAllowedChannels {

    @Test
    @DisplayName("should return allowed channels from service")
    void shouldReturnAllowedChannels() {
      // Given
      Set<AllowedChannel> channels =
          Set.of(
              new AllowedChannel(TEST_CHANNEL_ID, "general"), new AllowedChannel(123L, "random"));
      when(aiChannelRestrictionService.getAllowedChannels(TEST_GUILD_ID))
          .thenReturn(Result.ok(channels));

      // When
      Result<Set<AllowedChannel>, DomainError> result = facade.getAllowedChannels(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(2);
      verify(aiChannelRestrictionService).getAllowedChannels(TEST_GUILD_ID);
    }

    @Test
    @DisplayName("should propagate error from service")
    void shouldPropagateError() {
      // Given
      DomainError error = DomainError.persistenceFailure("Database error", null);
      when(aiChannelRestrictionService.getAllowedChannels(TEST_GUILD_ID))
          .thenReturn(Result.err(error));

      // When
      Result<Set<AllowedChannel>, DomainError> result = facade.getAllowedChannels(TEST_GUILD_ID);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.PERSISTENCE_FAILURE);
    }
  }

  @Nested
  @DisplayName("getAllowedCategories")
  class GetAllowedCategories {

    @Test
    @DisplayName("should return allowed categories from service")
    void shouldReturnAllowedCategories() {
      // Given
      Set<AllowedCategory> categories =
          Set.of(
              new AllowedCategory(TEST_CATEGORY_ID, "Gaming"),
              new AllowedCategory(999L, "General"));
      when(aiChannelRestrictionService.getAllowedCategories(TEST_GUILD_ID))
          .thenReturn(Result.ok(categories));

      // When
      Result<Set<AllowedCategory>, DomainError> result = facade.getAllowedCategories(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(2);
      verify(aiChannelRestrictionService).getAllowedCategories(TEST_GUILD_ID);
    }

    @Test
    @DisplayName("should return empty set when no categories configured")
    void shouldReturnEmptySet() {
      // Given
      when(aiChannelRestrictionService.getAllowedCategories(TEST_GUILD_ID))
          .thenReturn(Result.ok(Set.of()));

      // When
      Result<Set<AllowedCategory>, DomainError> result = facade.getAllowedCategories(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).isEmpty();
    }
  }

  @Nested
  @DisplayName("addAllowedChannel")
  class AddAllowedChannel {

    @Test
    @DisplayName("should add channel successfully")
    void shouldAddChannel() {
      // Given
      AllowedChannel channel = new AllowedChannel(TEST_CHANNEL_ID, "general");
      when(aiChannelRestrictionService.addAllowedChannel(
              eq(TEST_GUILD_ID), any(AllowedChannel.class)))
          .thenReturn(Result.ok(channel));

      // When
      Result<AllowedChannel, DomainError> result =
          facade.addAllowedChannel(TEST_GUILD_ID, TEST_CHANNEL_ID, "general");

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().channelId()).isEqualTo(TEST_CHANNEL_ID);
      assertThat(result.getValue().channelName()).isEqualTo("general");
      verify(aiChannelRestrictionService)
          .addAllowedChannel(eq(TEST_GUILD_ID), any(AllowedChannel.class));
    }

    @Test
    @DisplayName("should propagate error when add fails")
    void shouldPropagateError() {
      // Given
      DomainError error = DomainError.invalidInput("Channel already exists");
      when(aiChannelRestrictionService.addAllowedChannel(
              eq(TEST_GUILD_ID), any(AllowedChannel.class)))
          .thenReturn(Result.err(error));

      // When
      Result<AllowedChannel, DomainError> result =
          facade.addAllowedChannel(TEST_GUILD_ID, TEST_CHANNEL_ID, "general");

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
    }
  }

  @Nested
  @DisplayName("addAllowedCategory")
  class AddAllowedCategory {

    @Test
    @DisplayName("should add category successfully")
    void shouldAddCategory() {
      // Given
      AllowedCategory category = new AllowedCategory(TEST_CATEGORY_ID, "Gaming");
      when(aiChannelRestrictionService.addAllowedCategory(
              eq(TEST_GUILD_ID), any(AllowedCategory.class)))
          .thenReturn(Result.ok(category));

      // When
      Result<AllowedCategory, DomainError> result =
          facade.addAllowedCategory(TEST_GUILD_ID, TEST_CATEGORY_ID, "Gaming");

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().categoryId()).isEqualTo(TEST_CATEGORY_ID);
      assertThat(result.getValue().categoryName()).isEqualTo("Gaming");
      verify(aiChannelRestrictionService)
          .addAllowedCategory(eq(TEST_GUILD_ID), any(AllowedCategory.class));
    }
  }

  @Nested
  @DisplayName("removeAllowedChannel")
  class RemoveAllowedChannel {

    @Test
    @DisplayName("should remove channel successfully")
    void shouldRemoveChannel() {
      // Given
      when(aiChannelRestrictionService.removeAllowedChannel(TEST_GUILD_ID, TEST_CHANNEL_ID))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result =
          facade.removeAllowedChannel(TEST_GUILD_ID, TEST_CHANNEL_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      verify(aiChannelRestrictionService).removeAllowedChannel(TEST_GUILD_ID, TEST_CHANNEL_ID);
    }
  }

  @Nested
  @DisplayName("removeAllowedCategory")
  class RemoveAllowedCategory {

    @Test
    @DisplayName("should remove category successfully")
    void shouldRemoveCategory() {
      // Given
      when(aiChannelRestrictionService.removeAllowedCategory(TEST_GUILD_ID, TEST_CATEGORY_ID))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result =
          facade.removeAllowedCategory(TEST_GUILD_ID, TEST_CATEGORY_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      verify(aiChannelRestrictionService).removeAllowedCategory(TEST_GUILD_ID, TEST_CATEGORY_ID);
    }
  }

  @Nested
  @DisplayName("getEnabledAgentChannels")
  class GetEnabledAgentChannels {

    @Test
    @DisplayName("should return enabled agent channels")
    void shouldReturnEnabledChannels() {
      // Given
      List<Long> channels = List.of(TEST_CHANNEL_ID, 123L, 456L);
      when(aiAgentChannelConfigService.getEnabledChannels(TEST_GUILD_ID))
          .thenReturn(Result.ok(channels));

      // When
      Result<List<Long>, DomainError> result = facade.getEnabledAgentChannels(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(3);
      assertThat(result.getValue()).contains(TEST_CHANNEL_ID);
      verify(aiAgentChannelConfigService).getEnabledChannels(TEST_GUILD_ID);
    }

    @Test
    @DisplayName("should return empty list when no channels enabled")
    void shouldReturnEmptyList() {
      // Given
      when(aiAgentChannelConfigService.getEnabledChannels(TEST_GUILD_ID))
          .thenReturn(Result.ok(List.of()));

      // When
      Result<List<Long>, DomainError> result = facade.getEnabledAgentChannels(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).isEmpty();
    }
  }

  @Nested
  @DisplayName("isAgentEnabled")
  class IsAgentEnabled {

    @Test
    @DisplayName("should return true when agent is enabled")
    void shouldReturnTrueWhenEnabled() {
      // Given
      when(aiAgentChannelConfigService.isAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID))
          .thenReturn(true);

      // When
      boolean result = facade.isAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when agent is not enabled")
    void shouldReturnFalseWhenNotEnabled() {
      // Given
      when(aiAgentChannelConfigService.isAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID))
          .thenReturn(false);

      // When
      boolean result = facade.isAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID);

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("enableAgentChannel")
  class EnableAgentChannel {

    @Test
    @DisplayName("should enable agent channel successfully")
    void shouldEnableAgentChannel() {
      // Given
      when(aiAgentChannelConfigService.setAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID, true))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result = facade.enableAgentChannel(TEST_GUILD_ID, TEST_CHANNEL_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      verify(aiAgentChannelConfigService).setAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID, true);
    }
  }

  @Nested
  @DisplayName("disableAgentChannel")
  class DisableAgentChannel {

    @Test
    @DisplayName("should disable agent channel successfully")
    void shouldDisableAgentChannel() {
      // Given
      when(aiAgentChannelConfigService.setAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID, false))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result = facade.disableAgentChannel(TEST_GUILD_ID, TEST_CHANNEL_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      verify(aiAgentChannelConfigService).setAgentEnabled(TEST_GUILD_ID, TEST_CHANNEL_ID, false);
    }
  }

  @Nested
  @DisplayName("removeAgentChannel")
  class RemoveAgentChannel {

    @Test
    @DisplayName("should remove agent channel config successfully")
    void shouldRemoveAgentChannel() {
      // Given
      when(aiAgentChannelConfigService.removeChannel(TEST_GUILD_ID, TEST_CHANNEL_ID))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result = facade.removeAgentChannel(TEST_GUILD_ID, TEST_CHANNEL_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      verify(aiAgentChannelConfigService).removeChannel(TEST_GUILD_ID, TEST_CHANNEL_ID);
    }

    @Test
    @DisplayName("should propagate error when remove fails")
    void shouldPropagateError() {
      // Given
      DomainError error = DomainError.persistenceFailure("Failed to remove channel", null);
      when(aiAgentChannelConfigService.removeChannel(TEST_GUILD_ID, TEST_CHANNEL_ID))
          .thenReturn(Result.err(error));

      // When
      Result<Unit, DomainError> result = facade.removeAgentChannel(TEST_GUILD_ID, TEST_CHANNEL_ID);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.PERSISTENCE_FAILURE);
    }
  }
}
