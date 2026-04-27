package ltdjms.discord.shared.di;

import javax.inject.Singleton;

import dagger.Component;
import ltdjms.discord.discord.domain.DiscordEmbedBuilder;
import ltdjms.discord.shared.runtime.DiscordRuntimeGateway;

@Singleton
@Component(modules = DiscordModule.class)
interface DiscordRuntimeGatewayTestComponent {

  DiscordRuntimeGateway discordRuntimeGateway();

  DiscordEmbedBuilder discordEmbedBuilder();
}
