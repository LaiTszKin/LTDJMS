package ltdjms.discord.aichat.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.aichat.domain.AIChannelRestriction;
import ltdjms.discord.aichat.domain.AllowedCategory;
import ltdjms.discord.aichat.domain.AllowedChannel;
import ltdjms.discord.aichat.persistence.AIChannelRestrictionRepository;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.Unit;

@DisplayName("DefaultAIChannelRestrictionService")
@ExtendWith(MockitoExtension.class)
class DefaultAIChannelRestrictionServiceTest {

  @Mock private AIChannelRestrictionRepository repository;

  @Nested
  @DisplayName("isChannelAllowed")
  class IsChannelAllowedTests {

    @Test
    @DisplayName("should return true when channel is allowed")
    void shouldReturnTrueWhenChannelAllowed() {
      // Given
      long guildId = 123L;
      long channelId = 456L;
      long categoryId = 789L;

      AIChannelRestriction restriction = mock(AIChannelRestriction.class);
      when(restriction.isChannelAllowed(channelId, categoryId)).thenReturn(true);

      when(repository.findRestrictionByGuildId(guildId)).thenReturn(Result.ok(restriction));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      boolean result = service.isChannelAllowed(guildId, channelId, categoryId);

      // Then
      assertThat(result).isTrue();
      verify(repository).findRestrictionByGuildId(guildId);
      verify(restriction).isChannelAllowed(channelId, categoryId);
    }

    @Test
    @DisplayName("should return false when channel is not allowed")
    void shouldReturnFalseWhenChannelNotAllowed() {
      // Given
      long guildId = 123L;
      long channelId = 456L;
      long categoryId = 789L;

      AIChannelRestriction restriction = mock(AIChannelRestriction.class);
      when(restriction.isChannelAllowed(channelId, categoryId)).thenReturn(false);

      when(repository.findRestrictionByGuildId(guildId)).thenReturn(Result.ok(restriction));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      boolean result = service.isChannelAllowed(guildId, channelId, categoryId);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when repository returns error")
    void shouldReturnFalseWhenRepositoryError() {
      // Given
      long guildId = 123L;
      long channelId = 456L;
      long categoryId = 789L;

      DomainError error =
          new DomainError(DomainError.Category.PERSISTENCE_FAILURE, "DB error", null);
      when(repository.findRestrictionByGuildId(guildId)).thenReturn(Result.err(error));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      boolean result = service.isChannelAllowed(guildId, channelId, categoryId);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should call three-parameter version with categoryId=0")
    void shouldCallThreeParameterVersionWithZeroCategoryId() {
      // Given
      long guildId = 123L;
      long channelId = 456L;

      AIChannelRestriction restriction = mock(AIChannelRestriction.class);
      when(restriction.isChannelAllowed(channelId, 0L)).thenReturn(true);

      when(repository.findRestrictionByGuildId(guildId)).thenReturn(Result.ok(restriction));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      boolean result = service.isChannelAllowed(guildId, channelId);

      // Then
      assertThat(result).isTrue();
      verify(repository).findRestrictionByGuildId(guildId);
      verify(restriction).isChannelAllowed(channelId, 0L);
    }
  }

  @Nested
  @DisplayName("getAllowedChannels")
  class GetAllowedChannelsTests {

    @Test
    @DisplayName("should return allowed channels from repository")
    void shouldReturnAllowedChannels() {
      // Given
      long guildId = 123L;
      AllowedChannel channel1 = new AllowedChannel(456L, "channel1");
      AllowedChannel channel2 = new AllowedChannel(789L, "channel2");
      Set<AllowedChannel> channels = Set.of(channel1, channel2);

      when(repository.findByGuildId(guildId)).thenReturn(Result.ok(channels));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Set<AllowedChannel>, DomainError> result = service.getAllowedChannels(guildId);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(2);
      verify(repository).findByGuildId(guildId);
    }

    @Test
    @DisplayName("should return error when repository fails")
    void shouldReturnErrorWhenRepositoryFails() {
      // Given
      long guildId = 123L;
      DomainError error =
          new DomainError(DomainError.Category.PERSISTENCE_FAILURE, "DB error", null);

      when(repository.findByGuildId(guildId)).thenReturn(Result.err(error));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Set<AllowedChannel>, DomainError> result = service.getAllowedChannels(guildId);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError()).isEqualTo(error);
    }

    @Test
    @DisplayName("should return empty set when no channels allowed")
    void shouldReturnEmptySetWhenNoChannelsAllowed() {
      // Given
      long guildId = 123L;

      when(repository.findByGuildId(guildId)).thenReturn(Result.ok(Set.of()));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Set<AllowedChannel>, DomainError> result = service.getAllowedChannels(guildId);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).isEmpty();
    }
  }

  @Nested
  @DisplayName("getAllowedCategories")
  class GetAllowedCategoriesTests {

    @Test
    @DisplayName("should return allowed categories from repository")
    void shouldReturnAllowedCategories() {
      // Given
      long guildId = 123L;
      AllowedCategory category1 = new AllowedCategory(456L, "category1");
      AllowedCategory category2 = new AllowedCategory(789L, "category2");
      Set<AllowedCategory> categories = Set.of(category1, category2);

      when(repository.findAllowedCategories(guildId)).thenReturn(Result.ok(categories));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Set<AllowedCategory>, DomainError> result = service.getAllowedCategories(guildId);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(2);
      verify(repository).findAllowedCategories(guildId);
    }

    @Test
    @DisplayName("should return error when repository fails")
    void shouldReturnErrorWhenRepositoryFails() {
      // Given
      long guildId = 123L;
      DomainError error =
          new DomainError(DomainError.Category.PERSISTENCE_FAILURE, "DB error", null);

      when(repository.findAllowedCategories(guildId)).thenReturn(Result.err(error));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Set<AllowedCategory>, DomainError> result = service.getAllowedCategories(guildId);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError()).isEqualTo(error);
    }
  }

  @Nested
  @DisplayName("addAllowedChannel")
  class AddAllowedChannelTests {

    @Test
    @DisplayName("should add channel successfully")
    void shouldAddChannelSuccessfully() {
      // Given
      long guildId = 123L;
      AllowedChannel channel = new AllowedChannel(456L, "test-channel");

      when(repository.addChannel(guildId, channel)).thenReturn(Result.ok(channel));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<AllowedChannel, DomainError> result = service.addAllowedChannel(guildId, channel);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).isEqualTo(channel);
      verify(repository).addChannel(guildId, channel);
    }

    @Test
    @DisplayName("should return error when add channel fails")
    void shouldReturnErrorWhenAddChannelFails() {
      // Given
      long guildId = 123L;
      AllowedChannel channel = new AllowedChannel(456L, "test-channel");
      DomainError error =
          new DomainError(DomainError.Category.INVALID_INPUT, "Duplicate channel", null);

      when(repository.addChannel(guildId, channel)).thenReturn(Result.err(error));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<AllowedChannel, DomainError> result = service.addAllowedChannel(guildId, channel);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError()).isEqualTo(error);
    }
  }

  @Nested
  @DisplayName("addAllowedCategory")
  class AddAllowedCategoryTests {

    @Test
    @DisplayName("should add category successfully")
    void shouldAddCategorySuccessfully() {
      // Given
      long guildId = 123L;
      AllowedCategory category = new AllowedCategory(456L, "test-category");

      when(repository.addCategory(guildId, category)).thenReturn(Result.ok(category));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<AllowedCategory, DomainError> result = service.addAllowedCategory(guildId, category);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).isEqualTo(category);
      verify(repository).addCategory(guildId, category);
    }

    @Test
    @DisplayName("should return error when add category fails")
    void shouldReturnErrorWhenAddCategoryFails() {
      // Given
      long guildId = 123L;
      AllowedCategory category = new AllowedCategory(456L, "test-category");
      DomainError error =
          new DomainError(DomainError.Category.INVALID_INPUT, "Duplicate category", null);

      when(repository.addCategory(guildId, category)).thenReturn(Result.err(error));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<AllowedCategory, DomainError> result = service.addAllowedCategory(guildId, category);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError()).isEqualTo(error);
    }
  }

  @Nested
  @DisplayName("removeAllowedChannel")
  class RemoveAllowedChannelTests {

    @Test
    @DisplayName("should remove channel successfully")
    void shouldRemoveChannelSuccessfully() {
      // Given
      long guildId = 123L;
      long channelId = 456L;

      when(repository.removeChannel(guildId, channelId)).thenReturn(Result.ok(Unit.INSTANCE));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Unit, DomainError> result = service.removeAllowedChannel(guildId, channelId);

      // Then
      assertThat(result.isOk()).isTrue();
      verify(repository).removeChannel(guildId, channelId);
    }

    @Test
    @DisplayName("should return error when remove channel fails")
    void shouldReturnErrorWhenRemoveChannelFails() {
      // Given
      long guildId = 123L;
      long channelId = 456L;
      DomainError error =
          new DomainError(DomainError.Category.INVALID_INPUT, "Channel not found", null);

      when(repository.removeChannel(guildId, channelId)).thenReturn(Result.err(error));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Unit, DomainError> result = service.removeAllowedChannel(guildId, channelId);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError()).isEqualTo(error);
    }
  }

  @Nested
  @DisplayName("removeAllowedCategory")
  class RemoveAllowedCategoryTests {

    @Test
    @DisplayName("should remove category successfully")
    void shouldRemoveCategorySuccessfully() {
      // Given
      long guildId = 123L;
      long categoryId = 456L;

      when(repository.removeCategory(guildId, categoryId)).thenReturn(Result.ok(Unit.INSTANCE));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Unit, DomainError> result = service.removeAllowedCategory(guildId, categoryId);

      // Then
      assertThat(result.isOk()).isTrue();
      verify(repository).removeCategory(guildId, categoryId);
    }

    @Test
    @DisplayName("should return error when remove category fails")
    void shouldReturnErrorWhenRemoveCategoryFails() {
      // Given
      long guildId = 123L;
      long categoryId = 456L;
      DomainError error =
          new DomainError(DomainError.Category.INVALID_INPUT, "Category not found", null);

      when(repository.removeCategory(guildId, categoryId)).thenReturn(Result.err(error));

      DefaultAIChannelRestrictionService service =
          new DefaultAIChannelRestrictionService(repository);

      // When
      Result<Unit, DomainError> result = service.removeAllowedCategory(guildId, categoryId);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError()).isEqualTo(error);
    }
  }
}
