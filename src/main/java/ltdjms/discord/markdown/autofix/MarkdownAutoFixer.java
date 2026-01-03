package ltdjms.discord.markdown.autofix;

/**
 * 自動修復 Markdown 格式錯誤的介面。
 *
 * <p>實作此介面的類別應該能夠識別並修復常見的 Markdown 格式問題， 例如標題格式錯誤、未閉合的程式碼區塊等。
 */
public interface MarkdownAutoFixer {

  /**
   * 嘗試自動修復 Markdown 文字中的格式錯誤。
   *
   * @param markdown 原始 Markdown 文字
   * @return 修復後的 Markdown 文字，如果無法修復則返回原始文字
   */
  String autoFix(String markdown);
}
