package ltdjms.discord.shared.di;

import java.util.concurrent.atomic.AtomicReference;

import net.dv8tion.jda.api.JDA;

/**
 * JDA 實例提供者。
 *
 * <p>這是暫時保留的 compatibility bridge。正式 owner 已移到 injected {@code
 * DiscordRuntimeGateway}，新程式不得再以此類作為主要邊界。
 */
@Deprecated(forRemoval = false)
public final class JDAProvider {

  private static final AtomicReference<JDA> jdaRef = new AtomicReference<>();

  private JDAProvider() {
    // 防止實例化
  }

  /** 設置 JDA 實例。僅供 transitional bridge 使用。 */
  public static void setJda(JDA jda) {
    jdaRef.set(jda);
  }

  /**
   * 獲取 JDA 實例。
   *
   * @return JDA 實例
   * @throws IllegalStateException 如果 JDA 尚未設置
   */
  public static JDA getJda() {
    JDA jda = jdaRef.get();
    if (jda == null) {
      throw new IllegalStateException("JDA 實例尚未設置");
    }
    return jda;
  }

  /** 清除 JDA 實例（主要用於測試與 bridge reset）。 */
  public static void clear() {
    jdaRef.set(null);
  }
}
