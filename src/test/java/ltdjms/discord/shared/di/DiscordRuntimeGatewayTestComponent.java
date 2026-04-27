package ltdjms.discord.shared.di;

import javax.inject.Singleton;

import dagger.Component;
import ltdjms.discord.discord.domain.DiscordEmbedBuilder;
import ltdjms.discord.discord.domain.DiscordRuntimeGateway;

@Singleton
@Component(modules = DiscordModule.class)
interface DiscordRuntimeGatewayTestComponent {

  DiscordRuntimeGateway discordRuntimeGateway();

  DiscordEmbedBuilder discordEmbedBuilder();
}
