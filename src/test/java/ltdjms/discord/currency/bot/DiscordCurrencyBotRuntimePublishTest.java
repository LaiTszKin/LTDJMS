package ltdjms.discord.currency.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.discord.domain.DiscordRuntimeGateway;
import net.dv8tion.jda.api.JDA;

@DisplayName("DiscordCurrencyBot runtime publish")
class DiscordCurrencyBotRuntimePublishTest {

  @Test
  @DisplayName("bootstrap helper 應將 ready JDA 發布到 gateway")
  void shouldPublishReadyJdaToGateway() {
    DiscordRuntimeGateway gateway = mock(DiscordRuntimeGateway.class);
    JDA jda = mock(JDA.class);

    DiscordCurrencyBot.publishRuntime(gateway, jda);

    verify(gateway).publishReady(jda);
    assertThat(jda).isNotNull();
  }
}
