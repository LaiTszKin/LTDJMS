package ltdjms.discord.panel.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

class UserPanelHistoryViewFactoryTest {

  @Test
  @DisplayName("空的代幣流水頁面應顯示空狀態文案")
  void buildTokenHistoryEmbedShouldShowEmptyState() {
    GameTokenTransactionService.TransactionPage page =
        new GameTokenTransactionService.TransactionPage(List.of(), 1, 1, 0, 10);

    MessageEmbed embed = UserPanelHistoryViewFactory.buildTokenHistoryEmbed(page);

    assertThat(embed.getTitle()).isEqualTo("📜 遊戲代幣流水");
    assertThat(embed.getDescription()).isEqualTo("目前沒有任何遊戲代幣流水紀錄");
    assertThat(embed.getFooter().getText()).isEqualTo("第 1/1 頁（共 0 筆）");
  }

  @Test
  @DisplayName("分頁按鈕應保留返回主頁並帶出前後頁")
  void buildTokenPaginationButtonsShouldIncludeBackPrevAndNext() {
    GameTokenTransactionService.TransactionPage page =
        new GameTokenTransactionService.TransactionPage(List.of(), 2, 3, 30, 10);

    List<Button> buttons = UserPanelHistoryViewFactory.buildTokenPaginationButtons(page);

    assertThat(buttons)
        .extracting(Button::getId)
        .containsExactly(
            UserPanelButtonHandler.BUTTON_BACK_TO_PANEL,
            UserPanelButtonHandler.BUTTON_PREFIX_TOKEN_PAGE + "1",
            UserPanelButtonHandler.BUTTON_PREFIX_TOKEN_PAGE + "3");
  }
}
