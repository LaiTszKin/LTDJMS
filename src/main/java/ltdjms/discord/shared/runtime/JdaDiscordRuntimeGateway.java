package ltdjms.discord.shared.runtime;

import ltdjms.discord.shared.di.JDAProvider;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.requests.RestAction;

/** JDA-backed implementation of {@link DiscordRuntimeGateway}. */
public final class JdaDiscordRuntimeGateway implements DiscordRuntimeGateway {

  @Override
  public Guild getGuildById(long guildId) {
    return JDAProvider.getJda().getGuildById(guildId);
  }

  @Override
  public Category getCategoryById(long categoryId) {
    return JDAProvider.getJda().getCategoryById(categoryId);
  }

  @Override
  public long getSelfUserId() {
    return JDAProvider.getJda().getSelfUser().getIdLong();
  }

  @Override
  public RestAction<User> retrieveUserById(long userId) {
    return JDAProvider.getJda().retrieveUserById(userId);
  }
}
