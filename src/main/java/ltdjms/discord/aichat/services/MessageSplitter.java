package ltdjms.discord.aichat.services;

import java.util.ArrayList;
import java.util.List;

/** 訊息分割工具，將長訊息分割為符合 Discord 限制的多則訊息。 */
public final class MessageSplitter {

  private static final int MAX_MESSAGE_LENGTH = 1980; // 預留 20 字元緩衝

  private MessageSplitter() {}

  /**
   * 分割訊息為多則訊息。
   *
   * @param content 原始內容
   * @return 分割後的訊息列表
   */
  public static List<String> split(String content) {
    if (content == null) {
      return List.of("");
    }

    if (content.isEmpty()) {
      return List.of("");
    }

    List<String> paragraphSplit = splitByParagraphs(content);
    if (paragraphSplit.size() > 1 && withinLimit(paragraphSplit)) {
      return paragraphSplit;
    }

    List<String> sentenceSplit = splitBySentences(content);
    if (sentenceSplit.size() > 1 && withinLimit(sentenceSplit)) {
      return sentenceSplit;
    }

    if (content.length() <= MAX_MESSAGE_LENGTH) {
      return List.of(content);
    }

    List<String> messages = new ArrayList<>();
    int start = 0;

    while (start < content.length()) {
      int end = Math.min(start + MAX_MESSAGE_LENGTH, content.length());

      // 優先在段落處分割
      int lastNewline = content.lastIndexOf('\n', end);
      if (lastNewline > start) {
        end = lastNewline + 1;
      } else {
        // 其次在句子處分割
        int lastSentence = findLastSentenceBoundary(content, start, end);
        if (lastSentence > start) {
          end = lastSentence + 1;
        }
      }

      messages.add(content.substring(start, end).trim());
      start = end;
    }

    return messages;
  }

  private static boolean withinLimit(List<String> parts) {
    for (String part : parts) {
      if (part.length() > MAX_MESSAGE_LENGTH) {
        return false;
      }
    }
    return true;
  }

  private static List<String> splitByParagraphs(String content) {
    String[] rawParts = content.split("\\n\\s*\\n");
    List<String> parts = new ArrayList<>();
    for (String part : rawParts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        parts.add(trimmed);
      }
    }
    return parts;
  }

  private static List<String> splitBySentences(String content) {
    List<String> parts = new ArrayList<>();
    int start = 0;
    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      if (isSentenceBoundary(c)) {
        String part = content.substring(start, i + 1).trim();
        if (!part.isEmpty()) {
          parts.add(part);
        }
        start = i + 1;
      }
    }
    if (start < content.length()) {
      String tail = content.substring(start).trim();
      if (!tail.isEmpty()) {
        parts.add(tail);
      }
    }
    return parts;
  }

  private static boolean isSentenceBoundary(char c) {
    return c == '。' || c == '！' || c == '？';
  }

  private static int findLastSentenceBoundary(String content, int start, int end) {
    String boundaries = "。！？";
    int last = -1;

    for (char c : boundaries.toCharArray()) {
      int pos = content.lastIndexOf(c, end - 1);
      if (pos > start && pos > last) {
        last = pos;
      }
    }

    return last;
  }
}
