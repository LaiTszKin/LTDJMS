package ltdjms.discord.shared.di;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.discord.domain.DiscordRuntimeGateway;
import ltdjms.discord.discord.services.JdaDiscordRuntimeGateway;

@DisplayName("DiscordModule integration")
class DiscordModuleIntegrationTest {

  @Test
  @DisplayName("應透過 Dagger 提供 singleton runtime gateway")
  void shouldProvideSingletonDiscordRuntimeGateway() {
    DiscordRuntimeGatewayTestComponent component =
        DaggerDiscordRuntimeGatewayTestComponent.create();

    DiscordRuntimeGateway first = component.discordRuntimeGateway();
    DiscordRuntimeGateway second = component.discordRuntimeGateway();

    assertThat(first).isNotNull();
    assertThat(first).isSameAs(second);
    assertThat(first).isInstanceOf(JdaDiscordRuntimeGateway.class);
    assertThat(first.isReady()).isFalse();
  }
}
