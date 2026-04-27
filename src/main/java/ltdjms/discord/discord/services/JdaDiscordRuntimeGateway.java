package ltdjms.discord.discord.services;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import ltdjms.discord.discord.domain.DiscordRuntimeGateway;
import ltdjms.discord.discord.domain.DiscordRuntimeNotReadyException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

/**
 * JDA-backed Discord runtime gateway。
 *
 * <p>此實作由 Dagger 注入為 singleton，bootstrap 在 JDA ready 後呼叫 {@link #publishReady(JDA)} 將 live runtime
 * 發布進圖中。
 */
public final class JdaDiscordRuntimeGateway implements DiscordRuntimeGateway {

  private final AtomicReference<JDA> jdaRef = new AtomicReference<>();

  @Override
  public boolean isReady() {
    return jdaRef.get() != null;
  }

  @Override
  public void publishReady(JDA jda) {
    Objects.requireNonNull(jda, "jda");
    if (!jdaRef.compareAndSet(null, jda)) {
      throw new IllegalStateException("Discord runtime has already been published");
    }
  }

  @Override
  public JDA requireReadyJda() {
    JDA jda = jdaRef.get();
    if (jda == null) {
      throw new DiscordRuntimeNotReadyException("Discord runtime is not ready yet");
    }
    return jda;
  }

  @Override
  public Optional<Guild> findGuild(long guildId) {
    return Optional.ofNullable(requireReadyJda().getGuildById(guildId));
  }

  @Override
  public Optional<GuildChannel> findGuildChannel(long guildId, long channelId) {
    Guild guild = findGuild(guildId).orElse(null);
    if (guild == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(guild.getGuildChannelById(channelId));
  }

  @Override
  public Optional<ThreadChannel> findThreadChannel(long guildId, long threadId) {
    Guild guild = findGuild(guildId).orElse(null);
    if (guild == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(guild.getThreadChannelById(threadId));
  }

  @Override
  public long selfUserId() {
    return requireReadyJda().getSelfUser().getIdLong();
  }
}
