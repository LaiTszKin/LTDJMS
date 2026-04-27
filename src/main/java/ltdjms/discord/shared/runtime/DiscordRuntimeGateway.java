package ltdjms.discord.shared.runtime;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.requests.RestAction;

/**
 * Discord runtime 的窄介面。
 *
 * <p>此介面只暴露業務模組實際需要的 Discord runtime 能力，避免把完整 JDA 表面積重新擴散到 aiagent、aichat 與 shop 模組。
 */
public interface DiscordRuntimeGateway {

  /** 依 guild ID 取得 Guild；找不到或 runtime not ready 時回傳 null。 */
  Guild getGuildById(long guildId);

  /** 依 category ID 取得 Category；找不到或 runtime not ready 時回傳 null。 */
  Category getCategoryById(long categoryId);

  /** 取得 bot 自身的 user ID；runtime not ready 時應拋出 IllegalStateException。 */
  long getSelfUserId();

  /** 依 user ID 取得非同步 user lookup action。 */
  RestAction<User> retrieveUserById(long userId);
}
