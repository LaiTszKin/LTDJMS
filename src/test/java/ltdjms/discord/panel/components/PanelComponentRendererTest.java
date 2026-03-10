package ltdjms.discord.panel.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.discord.domain.ButtonView;
import ltdjms.discord.discord.domain.EmbedView;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

class PanelComponentRendererTest {

  @Test
  @DisplayName("buildEmbed 應保留標題欄位與 footer")
  void buildEmbedShouldKeepTitleFieldsAndFooter() {
    MessageEmbed embed =
        PanelComponentRenderer.buildEmbed(
            new EmbedView(
                "標題",
                "描述",
                new Color(0x5865F2),
                List.of(
                    new EmbedView.FieldView("欄位一", "值一", true),
                    new EmbedView.FieldView("欄位二", "值二", false)),
                "footer"));

    assertThat(embed.getTitle()).isEqualTo("標題");
    assertThat(embed.getDescription()).isEqualTo("描述");
    assertThat(embed.getFields())
        .extracting(MessageEmbed.Field::getName)
        .containsExactly("欄位一", "欄位二");
    assertThat(embed.getFooter()).isNotNull();
    assertThat(embed.getFooter().getText()).isEqualTo("footer");
  }

  @Test
  @DisplayName("buildButtons 應保留順序與 disabled 狀態")
  void buildButtonsShouldKeepOrderAndDisabledState() {
    List<Button> buttons =
        PanelComponentRenderer.buildButtons(
            List.of(
                new ButtonView("primary", "主要", ButtonStyle.PRIMARY, false),
                new ButtonView("secondary", "次要", ButtonStyle.SECONDARY, true)));

    assertThat(buttons).extracting(Button::getId).containsExactly("primary", "secondary");
    assertThat(buttons.get(0).isDisabled()).isFalse();
    assertThat(buttons.get(1).isDisabled()).isTrue();
  }

  @Test
  @DisplayName("buildActionRows 應依輸入列數建立 ActionRow")
  void buildActionRowsShouldCreateExpectedRows() {
    List<ActionRow> rows =
        PanelComponentRenderer.buildActionRows(
            List.of(
                List.of(new ButtonView("a", "A", ButtonStyle.PRIMARY, false)),
                List.of(
                    new ButtonView("b", "B", ButtonStyle.SECONDARY, false),
                    new ButtonView("c", "C", ButtonStyle.SUCCESS, false))));

    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).getButtons()).extracting(Button::getId).containsExactly("a");
    assertThat(rows.get(1).getButtons()).extracting(Button::getId).containsExactly("b", "c");
  }
}
