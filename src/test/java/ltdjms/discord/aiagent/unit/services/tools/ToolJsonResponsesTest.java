package ltdjms.discord.aiagent.unit.services.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.aiagent.services.tools.ToolJsonResponses;

@DisplayName("ToolJsonResponses 邊緣案例")
class ToolJsonResponsesTest {

  @Test
  @DisplayName("successWithField 應轉義欄位名稱中的特殊字元")
  void shouldEscapeFieldNameInSuccessWithField() {
    String result = ToolJsonResponses.successWithField("ok", "field\"name\n", "value");

    assertThat(result).contains("\"field\\\"name\\n\"");
  }

  @Test
  @DisplayName("successWithFields 應轉義多個欄位名稱中的特殊字元")
  void shouldEscapeFieldNamesInSuccessWithFields() {
    String result =
        ToolJsonResponses.successWithFields("ok", "first\"name", 1, "second\nname", "value");

    assertThat(result).contains("\"first\\\"name\"");
    assertThat(result).contains("\"second\\nname\"");
  }
}
