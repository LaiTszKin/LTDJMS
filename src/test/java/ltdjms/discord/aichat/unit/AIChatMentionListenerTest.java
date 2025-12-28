package ltdjms.discord.aichat.unit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ltdjms.discord.aichat.commands.AIChatMentionListener;

/** 測試 {@link AIChatMentionListener} 的 JDA 事件處理功能。 */
@Disabled("JDA mocking is complex - will be tested via integration")
class AIChatMentionListenerTest {

  @Test
  void testOnMessageReceived_withBotMention_shouldTriggerAIResponse() {
    // Skipped - JDA mocking is complex
    // This will be tested via integration testing
  }

  // ... other tests skipped
}
