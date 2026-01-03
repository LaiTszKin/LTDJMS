package ltdjms.discord.markdown.autofix;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基於正規表達式的 Markdown 自動修復器。
 *
 * <p>此實作使用正規表達式來識別和修復常見的 Markdown 格式錯誤。
 */
public class RegexBasedAutoFixer implements MarkdownAutoFixer {

  private static final Logger log = LoggerFactory.getLogger(RegexBasedAutoFixer.class);

  /** 正規表達式：匹配行首的連續 # 符號後直接跟隨非空白字符 例如: "#Heading" 或 "##Subheading" */
  private static final Pattern HEADING_WITHOUT_SPACE =
      Pattern.compile("^(#{1,6})([^\\s#].*)$", Pattern.MULTILINE);

  @Override
  public String autoFix(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }

    String result = markdown;

    // 應用標題格式修復
    result = fixHeadingFormat(result);

    return result;
  }

  /**
   * 修復標題格式錯誤。
   *
   * <p>在 # 符號和標題文字之間插入空格。
   *
   * @param markdown 原始 Markdown
   * @return 修復後的 Markdown
   */
  private String fixHeadingFormat(String markdown) {
    // 先保護程式碼區塊
    List<String> codeBlocks = new ArrayList<>();
    String protectedContent = protectCodeBlocks(markdown, codeBlocks);

    // 修復非程式碼區塊中的標題
    String fixedContent = HEADING_WITHOUT_SPACE.matcher(protectedContent).replaceAll("$1 $2");

    // 還原程式碼區塊
    return restoreCodeBlocks(fixedContent, codeBlocks);
  }

  /** 保護程式碼區塊，替換為佔位符。 */
  private String protectCodeBlocks(String markdown, List<String> codeBlocks) {
    StringBuffer sb = new StringBuffer();
    Matcher matcher = Pattern.compile("```[\\s\\S]*?```").matcher(markdown);
    int lastEnd = 0;

    while (matcher.find()) {
      // 添加程式碼區塊之前的內容
      sb.append(markdown, lastEnd, matcher.start());
      // 儲存程式碼區塊
      codeBlocks.add(matcher.group());
      // 添加佔位符
      sb.append("\u0000CODE_BLOCK_").append(codeBlocks.size() - 1).append("\u0000");
      lastEnd = matcher.end();
    }

    sb.append(markdown.substring(lastEnd));
    return sb.toString();
  }

  /** 還原程式碼區塊。 */
  private String restoreCodeBlocks(String markdown, List<String> codeBlocks) {
    String result = markdown;
    for (int i = 0; i < codeBlocks.size(); i++) {
      result = result.replace("\u0000CODE_BLOCK_" + i + "\u0000", codeBlocks.get(i));
    }
    return result;
  }
}
