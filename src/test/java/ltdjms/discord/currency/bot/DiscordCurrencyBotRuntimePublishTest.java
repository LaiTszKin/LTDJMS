package ltdjms.discord.currency.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.shared.di.JDAProvider;
import net.dv8tion.jda.api.JDA;

@DisplayName("DiscordCurrencyBot runtime publish")
class DiscordCurrencyBotRuntimePublishTest {

  @Test
  @DisplayName("bootstrap helper 應將 ready JDA 發布到 gateway")
  void shouldPublishReadyJdaToGateway() {
    JDA jda = mock(JDA.class);

    try {
      DiscordCurrencyBot.publishRuntime(jda);
      assertThat(JDAProvider.getJda()).isSameAs(jda);
    } finally {
      JDAProvider.clear();
    }
  }
}
