package ltdjms.discord.aiagent.commands;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ltdjms.discord.shared.di.JDAProvider;
import ltdjms.discord.shared.events.LangChain4jToolExecutedEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

/** Unit tests for ToolExecutionListener. */
@DisplayName("ToolExecutionListener")
class ToolExecutionListenerTest {

  private ToolExecutionListener listener;
  private JDA jda;
  private Guild guild;
  private ThreadChannel threadChannel;
  private MessageCreateAction messageCreateAction;

  @BeforeEach
  void setUp() {
    listener = new ToolExecutionListener();

    jda = mock(JDA.class);
    guild = mock(Guild.class);
    threadChannel = mock(ThreadChannel.class);
    messageCreateAction = mock(MessageCreateAction.class);

    // Setup JDAProvider mock
    JDAProvider.setJda(jda);

    // Mock sendMessage behavior
    when(threadChannel.sendMessage(any(CharSequence.class))).thenReturn(messageCreateAction);
    doNothing().when(messageCreateAction).queue();
  }

  @Nested
  @DisplayName("accept")
  class AcceptTests {

    @Test
    @DisplayName("should handle LangChain4jToolExecutedEvent with success")
    void shouldHandleToolExecutedEventWithSuccess() {
      // Given
      when(jda.getGuildById(anyLong())).thenReturn(guild);
      when(guild.getThreadChannelById(anyLong())).thenReturn(threadChannel);

      LangChain4jToolExecutedEvent event =
          new LangChain4jToolExecutedEvent(
              123L, 456L, 789L, "TestTool", "Success!", true, Instant.now());

      // When
      listener.accept(event);

      // Then
      verify(threadChannel).sendMessage(any(CharSequence.class));
    }

    @Test
    @DisplayName("should handle LangChain4jToolExecutedEvent with failure")
    void shouldHandleToolExecutedEventWithFailure() {
      // Given
      when(jda.getGuildById(anyLong())).thenReturn(guild);
      when(guild.getThreadChannelById(anyLong())).thenReturn(threadChannel);

      LangChain4jToolExecutedEvent event =
          new LangChain4jToolExecutedEvent(
              123L, 456L, 789L, "TestTool", "Error occurred", false, Instant.now());

      // When
      listener.accept(event);

      // Then
      verify(threadChannel).sendMessage(any(CharSequence.class));
    }

    @Test
    @DisplayName("should ignore null event gracefully")
    void shouldIgnoreNullEventGracefully() {
      // When
      listener.accept(null);

      // Then - should not throw exception
      verify(threadChannel, never()).sendMessage(any(CharSequence.class));
    }
  }

  @Nested
  @DisplayName("handleToolExecuted")
  class HandleToolExecutedTests {

    @Test
    @DisplayName("should send success message when tool executes successfully")
    void shouldSendSuccessMessage() {
      // Given
      when(jda.getGuildById(123L)).thenReturn(guild);
      when(guild.getThreadChannelById(456L)).thenReturn(threadChannel);

      LangChain4jToolExecutedEvent event =
          new LangChain4jToolExecutedEvent(
              123L, 456L, 789L, "WeatherTool", "Data fetched", true, Instant.now());

      // When
      listener.accept(event);

      // Then
      verify(threadChannel).sendMessage("✅ 工具「WeatherTool」執行成功");
    }

    @Test
    @DisplayName("should send failure message when tool execution fails")
    void shouldSendFailureMessage() {
      // Given
      when(jda.getGuildById(123L)).thenReturn(guild);
      when(guild.getThreadChannelById(456L)).thenReturn(threadChannel);

      LangChain4jToolExecutedEvent event =
          new LangChain4jToolExecutedEvent(
              123L, 456L, 789L, "WeatherTool", "API timeout", false, Instant.now());

      // When
      listener.accept(event);

      // Then
      verify(threadChannel).sendMessage("❌ 工具「WeatherTool」執行失敗：API timeout");
    }

    @Test
    @DisplayName("should not send message when guild is not found")
    void shouldNotSendMessageWhenGuildNotFound() {
      // Given
      when(jda.getGuildById(123L)).thenReturn(null);

      LangChain4jToolExecutedEvent event =
          new LangChain4jToolExecutedEvent(
              123L, 456L, 789L, "TestTool", "Result", true, Instant.now());

      // When
      listener.accept(event);

      // Then
      verify(threadChannel, never()).sendMessage(any(CharSequence.class));
    }

    @Test
    @DisplayName("should not send message when channel is not found")
    void shouldNotSendMessageWhenChannelNotFound() {
      // Given
      when(jda.getGuildById(123L)).thenReturn(guild);
      when(guild.getThreadChannelById(456L)).thenReturn(null);

      LangChain4jToolExecutedEvent event =
          new LangChain4jToolExecutedEvent(
              123L, 456L, 789L, "TestTool", "Result", true, Instant.now());

      // When
      listener.accept(event);

      // Then
      verify(threadChannel, never()).sendMessage(any(CharSequence.class));
    }
  }
}
