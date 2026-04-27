package ltdjms.discord.shared.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import ltdjms.discord.aiagent.services.DiscordThreadHistoryProvider;
import ltdjms.discord.aiagent.services.InMemoryToolCallHistory;
import ltdjms.discord.aiagent.services.SimplifiedChatMemoryProvider;
import ltdjms.discord.aiagent.services.TokenEstimator;
import ltdjms.discord.shared.runtime.DiscordRuntimeGateway;

@DisplayName("AIAgentModule canonical memory wiring")
class AIAgentModuleTest {

  @Test
  @DisplayName("provideChatMemoryProvider 應回傳 SimplifiedChatMemoryProvider")
  void shouldProvideSimplifiedChatMemoryProviderAsCanonicalRuntimeOwner() {
    AIAgentModule module = new AIAgentModule();

    ChatMemoryProvider provider =
        module.provideChatMemoryProvider(
            new DiscordThreadHistoryProvider(100, new TokenEstimator()),
            new InMemoryToolCallHistory(),
            mock(DiscordRuntimeGateway.class));

    assertThat(provider).isInstanceOf(SimplifiedChatMemoryProvider.class);
  }
}
