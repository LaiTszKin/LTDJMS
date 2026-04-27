package ltdjms.discord.discord.domain;

/** 表示 Discord runtime 尚未由 bootstrap 發布。 */
public class DiscordRuntimeNotReadyException extends IllegalStateException {

  public DiscordRuntimeNotReadyException(String message) {
    super(message);
  }
}
