package ltdjms.discord.discord.domain;

import java.util.Optional;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

/**
 * Discord runtime access 的正式注入邊界。
 *
 * <p>此介面只暴露本 repo 目前需要的最小 runtime 能力，讓呼叫端不必直接依賴 process-global static 狀態來取得 live JDA。
 */
public interface DiscordRuntimeGateway {

  /** 回傳 runtime 是否已由 bootstrap 發布。 */
  boolean isReady();

  /** 發布 ready 的 JDA 實例。 */
  void publishReady(JDA jda);

  /** 取得已 ready 的 JDA，若尚未發布則拋出明確例外。 */
  JDA requireReadyJda();

  /** 解析指定 guild。 */
  Optional<Guild> findGuild(long guildId);

  /** 解析指定 guild 內的一般 guild channel。 */
  Optional<GuildChannel> findGuildChannel(long guildId, long channelId);

  /** 解析指定 guild 內的 thread channel。 */
  Optional<ThreadChannel> findThreadChannel(long guildId, long threadId);

  /** 取得 bot self user id。 */
  long selfUserId();
}
