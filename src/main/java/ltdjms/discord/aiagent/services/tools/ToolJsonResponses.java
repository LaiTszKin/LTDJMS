package ltdjms.discord.aiagent.services.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 工具 JSON 響應構建器。
 *
 * <p>提供統一的 JSON 響應格式，包含成功和錯誤響應的構建方法。
 *
 * <p>所有 LangChain4J 工具應使用此類來構建標準化的 JSON 響應。
 */
public final class ToolJsonResponses {

  private ToolJsonResponses() {
    // 工具類，不允許實例化
  }

  /**
   * 構建錯誤響應。
   *
   * @param error 錯誤訊息
   * @return JSON 格式的錯誤響應
   */
  public static String error(String error) {
    return """
    {
      "success": false,
      "error": "%s"
    }
    """
        .formatted(escapeJson(error));
  }

  /**
   * 構建帶有額外數據的錯誤響應。
   *
   * @param error 錯誤訊息
   * @param details 額外的錯誤詳情
   * @return JSON 格式的錯誤響應
   */
  public static String error(String error, Map<String, Object> details) {
    StringBuilder json =
        new StringBuilder(
            """
            {
              "success": false,
              "error": "%s"
            """
                .formatted(escapeJson(error)));

    if (details != null && !details.isEmpty()) {
      json.append(",\n  \"details\": {");
      boolean first = true;
      for (Map.Entry<String, Object> entry : details.entrySet()) {
        if (!first) {
          json.append(",");
        }
        json.append("\n    \"")
            .append(escapeJson(entry.getKey()))
            .append("\": \"")
            .append(escapeJson(String.valueOf(entry.getValue())))
            .append("\"");
        first = false;
      }
      json.append("\n  }");
    }

    json.append("\n}");
    return json.toString();
  }

  /**
   * 構建成功響應（僅包含訊息）。
   *
   * @param message 成功訊息
   * @return JSON 格式的成功響應
   */
  public static String success(String message) {
    return """
    {
      "success": true,
      "message": "%s"
    }
    """
        .formatted(escapeJson(message));
  }

  /**
   * 構建成功響應（帶有單個數據字段）。
   *
   * @param message 成功訊息
   * @param fieldName 字段名稱
   * @param fieldValue 字段值
   * @return JSON 格式的成功響應
   */
  public static String successWithField(String message, String fieldName, Object fieldValue) {
    return """
    {
      "success": true,
      "message": "%s",
      "%s": "%s"
    }
    """
        .formatted(
            escapeJson(message), escapeJson(fieldName), escapeJson(String.valueOf(fieldValue)));
  }

  /**
   * 構建成功響應（帶有兩個數據字段）。
   *
   * @param message 成功訊息
   * @param field1Name 第一個字段名稱
   * @param field1Value 第一個字段值
   * @param field2Name 第二個字段名稱
   * @param field2Value 第二個字段值
   * @return JSON 格式的成功響應
   */
  public static String successWithFields(
      String message,
      String field1Name,
      Object field1Value,
      String field2Name,
      Object field2Value) {
    return """
    {
      "success": true,
      "message": "%s",
      "%s": "%s",
      "%s": "%s"
    }
    """
        .formatted(
            escapeJson(message),
            escapeJson(field1Name),
            escapeJson(String.valueOf(field1Value)),
            escapeJson(field2Name),
            escapeJson(String.valueOf(field2Value)));
  }

  /**
   * 構建自定義成功響應。
   *
   * <p>允許通過 Consumer 自定義響應內容。
   *
   * @param builder 構建器 Consumer
   * @return JSON 格式的成功響應
   */
  public static String customSuccess(Consumer<SuccessBuilder> builder) {
    SuccessBuilder successBuilder = new SuccessBuilder();
    builder.accept(successBuilder);
    return successBuilder.build();
  }

  /**
   * 轉義 JSON 字串中的特殊字符。
   *
   * @param value 原始字串
   * @return 轉義後的字串
   */
  public static String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /**
   * 成功響應構建器。
   *
   * <p>用於構建複雜的成功響應。
   */
  public static final class SuccessBuilder {
    private final Map<String, Object> fields = new LinkedHashMap<>();
    private String message;

    /** 私有構造函數。 */
    private SuccessBuilder() {}

    /**
     * 設置成功訊息。
     *
     * @param msg 訊息
     * @return this
     */
    public SuccessBuilder message(String msg) {
      this.message = msg;
      return this;
    }

    /**
     * 添加字符串字段。
     *
     * @param name 字段名
     * @param value 字段值
     * @return this
     */
    public SuccessBuilder put(String name, String value) {
      fields.put(name, value);
      return this;
    }

    /**
     * 添加數字字段。
     *
     * @param name 字段名
     * @param value 字段值
     * @return this
     */
    public SuccessBuilder put(String name, long value) {
      fields.put(name, value);
      return this;
    }

    /**
     * 添加字符串列表字段。
     *
     * @param name 字段名
     * @param values 字段值
     * @return this
     */
    public SuccessBuilder putList(String name, Iterable<String> values) {
      fields.put(name, values);
      return this;
    }

    /**
     * 構建 JSON 字串。
     *
     * @return JSON 字串
     */
    public String build() {
      StringBuilder json = new StringBuilder("{\n  \"success\": true");

      if (message != null) {
        json.append(",\n  \"message\": \"").append(escapeJson(message)).append("\"");
      }

      for (Map.Entry<String, Object> entry : fields.entrySet()) {
        json.append(",\n  \"").append(escapeJson(entry.getKey())).append("\": ");

        Object value = entry.getValue();
        if (value instanceof String) {
          json.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Long || value instanceof Integer) {
          json.append(value);
        } else if (value instanceof Iterable) {
          json.append("[");
          boolean first = true;
          for (Object item : (Iterable<?>) value) {
            if (!first) {
              json.append(", ");
            }
            json.append("\"").append(escapeJson(String.valueOf(item))).append("\"");
            first = false;
          }
          json.append("]");
        } else {
          json.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
        }
      }

      json.append("\n}");
      return json.toString();
    }
  }
}
