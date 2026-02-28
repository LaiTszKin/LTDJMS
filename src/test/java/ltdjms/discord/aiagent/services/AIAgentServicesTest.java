package ltdjms.discord.aiagent.services;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ltdjms.discord.aiagent.domain.ChannelPermission;
import ltdjms.discord.aiagent.domain.ConversationMessage;
import ltdjms.discord.aiagent.domain.MessageRole;
import ltdjms.discord.aiagent.domain.PermissionSetting;
import ltdjms.discord.aiagent.domain.ToolCallInfo;
import net.dv8tion.jda.api.Permission;

/** Unit tests for aiagent.services package utility classes. */
@DisplayName("AIAgent Services")
class AIAgentServicesTest {

  @Nested
  @DisplayName("PermissionParser")
  class PermissionParserTests {

    @Test
    @DisplayName("should parse read-only description")
    void shouldParseReadOnlyDescription() {
      // When
      ChannelPermission result = PermissionParser.parse("read-only", 123L);

      // Then
      assertThat(result.roleId()).isEqualTo(123L);
      assertThat(result.permissionSet()).containsExactly(Permission.VIEW_CHANNEL);
    }

    @Test
    @DisplayName("should parse readonly description without dash")
    void shouldParseReadOnlyDescriptionWithoutDash() {
      // When
      ChannelPermission result = PermissionParser.parse("readonly", 123L);

      // Then
      assertThat(result.permissionSet()).containsExactly(Permission.VIEW_CHANNEL);
    }

    @Test
    @DisplayName("should parse full access description")
    void shouldParseFullAccessDescription() {
      // When
      ChannelPermission result = PermissionParser.parse("full access", 123L);

      // Then
      assertThat(result.permissionSet()).contains(Permission.values());
    }

    @Test
    @DisplayName("should parse all description")
    void shouldParseAllDescription() {
      // When
      ChannelPermission result = PermissionParser.parse("all permissions", 123L);

      // Then
      assertThat(result.permissionSet()).contains(Permission.values());
    }

    @Test
    @DisplayName("should parse moderator description")
    void shouldParseModeratorDescription() {
      // When
      ChannelPermission result = PermissionParser.parse("moderator", 123L);

      // Then
      assertThat(result.permissionSet())
          .containsExactlyInAnyOrder(
              Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MODERATE_MEMBERS);
    }

    @Test
    @DisplayName("should parse write permission")
    void shouldParseWritePermission() {
      // When
      ChannelPermission result = PermissionParser.parse("write", 123L);

      // Then
      assertThat(result.permissionSet()).contains(Permission.MESSAGE_SEND);
    }

    @Test
    @DisplayName("should parse speak permission")
    void shouldParseSpeakPermission() {
      // When
      ChannelPermission result = PermissionParser.parse("speak", 123L);

      // Then
      assertThat(result.permissionSet()).contains(Permission.VOICE_SPEAK);
    }

    @Test
    @DisplayName("should parse permissions from list")
    void shouldParsePermissionsFromList() {
      // Given
      List<String> perms = List.of("view", "write", "send");

      // When
      ChannelPermission result = PermissionParser.parse(perms, 123L);

      // Then
      assertThat(result.permissionSet())
          .containsExactlyInAnyOrder(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);
    }

    @Test
    @DisplayName("should parse permissions from map")
    void shouldParsePermissionsFromMap() {
      // Given
      Map<String, Object> map = Map.of("roleId", 456L, "permissions", List.of("view", "write"));

      // When
      ChannelPermission result = PermissionParser.parse(map, 123L);

      // Then
      assertThat(result.roleId()).isEqualTo(456L);
      assertThat(result.permissionSet())
          .containsExactlyInAnyOrder(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);
    }

    @Test
    @DisplayName("should return read-only for unknown input")
    void shouldReturnReadOnlyForUnknownInput() {
      // When
      ChannelPermission result = PermissionParser.parse("unknown", 123L);

      // Then
      assertThat(result.permissionSet()).containsExactly(Permission.VIEW_CHANNEL);
    }

    @Test
    @DisplayName("should return read-only for null input")
    void shouldReturnReadOnlyForNullInput() {
      // When
      ChannelPermission result = PermissionParser.parse(null, 123L);

      // Then
      assertThat(result.permissionSet()).containsExactly(Permission.VIEW_CHANNEL);
    }

    @Test
    @DisplayName("should parse multiple permissions")
    void shouldParseMultiplePermissions() {
      // Given
      List<?> perms =
          List.of(
              Map.of("roleId", 123L, "permissions", List.of("view", "write")),
              Map.of("roleId", 456L, "permissions", List.of("read")));

      // When
      List<ChannelPermission> results = PermissionParser.parseMultiple(perms);

      // Then
      assertThat(results).hasSize(2);
      assertThat(results.get(0).roleId()).isEqualTo(123L);
      assertThat(results.get(1).roleId()).isEqualTo(456L);
    }

    @Test
    @DisplayName("should validate valid description")
    void shouldValidateValidDescription() {
      assertThat(PermissionParser.isValidDescription("full access")).isTrue();
      assertThat(PermissionParser.isValidDescription("read only")).isTrue();
      assertThat(PermissionParser.isValidDescription("write")).isTrue();
      assertThat(PermissionParser.isValidDescription("admin")).isTrue();
    }

    @Test
    @DisplayName("should invalidate invalid description")
    void shouldInvalidateInvalidDescription() {
      assertThat(PermissionParser.isValidDescription(null)).isFalse();
      assertThat(PermissionParser.isValidDescription("")).isFalse();
      assertThat(PermissionParser.isValidDescription("   ")).isFalse();
      assertThat(PermissionParser.isValidDescription("unknown stuff")).isFalse();
      assertThat(PermissionParser.isValidDescription("wallet controls")).isFalse();
    }

    @Test
    @DisplayName("should not treat partial word matches as full access")
    void shouldNotTreatPartialWordMatchesAsFullAccess() {
      // When
      ChannelPermission result = PermissionParser.parse("wallet controls", 123L);

      // Then
      assertThat(result.permissionSet()).containsExactly(Permission.VIEW_CHANNEL);
    }

    @Test
    @DisplayName("should parse from PermissionSetting")
    void shouldParseFromPermissionSetting() {
      // Given
      Set<PermissionSetting.PermissionEnum> allowSet =
          Set.of(
              PermissionSetting.PermissionEnum.VIEW_CHANNEL,
              PermissionSetting.PermissionEnum.MESSAGE_SEND);
      PermissionSetting setting = new PermissionSetting(123L, allowSet, null);

      // When
      ChannelPermission result = PermissionParser.parse(setting);

      // Then
      assertThat(result.roleId()).isEqualTo(123L);
      assertThat(result.permissionSet())
          .containsExactlyInAnyOrder(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);
    }

    @Test
    @DisplayName("should return read-only when PermissionSetting has empty allowSet")
    void shouldReturnReadOnlyWhenEmptyAllowSet() {
      // Given
      PermissionSetting setting = new PermissionSetting(123L, Set.of(), null);

      // When
      ChannelPermission result = PermissionParser.parse(setting);

      // Then
      assertThat(result.permissionSet()).containsExactly(Permission.VIEW_CHANNEL);
    }
  }

  @Nested
  @DisplayName("TokenEstimator")
  class TokenEstimatorTests {

    @Test
    @DisplayName("should handle null tool result when estimating tokens")
    void shouldHandleNullToolResult() {
      // Given
      ToolCallInfo toolCall = new ToolCallInfo(null, Map.of(), true, null);
      ConversationMessage message =
          new ConversationMessage(
              MessageRole.TOOL, "content", Instant.now(), Optional.of(toolCall));
      TokenEstimator estimator = new TokenEstimator(4000);

      // When
      int tokens = estimator.estimateTokens(message);

      // Then
      assertThat(tokens).isEqualTo(52);
    }

    @Test
    @DisplayName("should handle null content when estimating tokens")
    void shouldHandleNullContent() {
      // Given
      ConversationMessage message =
          new ConversationMessage(MessageRole.USER, null, Instant.now(), Optional.empty());
      TokenEstimator estimator = new TokenEstimator(4000);

      // When
      int tokens = estimator.estimateTokens(message);

      // Then
      assertThat(tokens).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("ToolContext")
  class ToolContextTests {

    @Test
    @DisplayName("should create ToolContext successfully")
    void shouldCreateToolContext() {
      // Given
      long guildId = 123L;
      long channelId = 456L;
      long userId = 789L;

      // When
      ToolContext context = new ToolContext(guildId, channelId, userId, null);

      // Then
      assertThat(context.guildId()).isEqualTo(guildId);
      assertThat(context.channelId()).isEqualTo(channelId);
      assertThat(context.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("should have all fields accessible")
    void shouldHaveAllFieldsAccessible() {
      // Given
      ToolContext context = new ToolContext(123L, 456L, 789L, null);

      // Then
      assertThat(context.guildId()).isNotEqualTo(0L);
      assertThat(context.channelId()).isNotEqualTo(0L);
      assertThat(context.userId()).isNotEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("ToolExecutionContext")
  class ToolExecutionContextTests {

    @Test
    @DisplayName("should set and get context")
    void shouldSetAndGetContext() {
      // When
      ToolExecutionContext.setContext(123L, 456L, 789L);

      // Then
      assertThat(ToolExecutionContext.isContextSet()).isTrue();
      ToolExecutionContext.Context context = ToolExecutionContext.getContext();
      assertThat(context.guildId()).isEqualTo(123L);
      assertThat(context.channelId()).isEqualTo(456L);
      assertThat(context.userId()).isEqualTo(789L);

      // Cleanup
      ToolExecutionContext.clearContext();
    }

    @Test
    @DisplayName("should clear context")
    void shouldClearContext() {
      // Given
      ToolExecutionContext.setContext(123L, 456L, 789L);

      // When
      ToolExecutionContext.clearContext();

      // Then
      assertThat(ToolExecutionContext.isContextSet()).isFalse();
    }

    @Test
    @DisplayName("should throw exception when getting context without setting")
    void shouldThrowExceptionWhenGettingContextWithoutSetting() {
      // Given
      ToolExecutionContext.clearContext(); // Ensure no context is set

      // When/Then
      assertThatThrownBy(() -> ToolExecutionContext.getContext())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("工具執行上下文未設置");
    }

    @Test
    @DisplayName("should return false when context is not set")
    void shouldReturnFalseWhenContextNotSet() {
      // Given
      ToolExecutionContext.clearContext();

      // When
      boolean result = ToolExecutionContext.isContextSet();

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should handle context with same thread")
    void shouldHandleContextWithSameThread() {
      // When
      ToolExecutionContext.setContext(111L, 222L, 333L);
      ToolExecutionContext.Context context = ToolExecutionContext.getContext();

      // Then
      assertThat(context.guildId()).isEqualTo(111L);

      // Cleanup
      ToolExecutionContext.clearContext();
    }

    @Test
    @DisplayName("should allow setting new context")
    void shouldAllowSettingNewContext() {
      // Given
      ToolExecutionContext.setContext(123L, 456L, 789L);

      // When - set new context
      ToolExecutionContext.setContext(999L, 888L, 777L);

      // Then
      ToolExecutionContext.Context context = ToolExecutionContext.getContext();
      assertThat(context.guildId()).isEqualTo(999L);

      // Cleanup
      ToolExecutionContext.clearContext();
    }
  }
}
