package ltdjms.discord.panel.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import ltdjms.discord.currency.services.CurrencyTransactionService;
import ltdjms.discord.discord.domain.ButtonView;
import ltdjms.discord.discord.domain.EmbedView;
import ltdjms.discord.gametoken.services.GameTokenTransactionService.TransactionPage;
import ltdjms.discord.panel.components.PanelComponentRenderer;
import ltdjms.discord.redemption.services.ProductRedemptionTransactionService;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

final class UserPanelHistoryViewFactory {

  private static final Color EMBED_COLOR = new Color(0x5865F2);

  private UserPanelHistoryViewFactory() {}

  static MessageEmbed buildTokenHistoryEmbed(TransactionPage page) {
    return buildHistoryEmbed(
        "📜 遊戲代幣流水",
        "目前沒有任何遊戲代幣流水紀錄",
        formatHistoryLines(
            page.transactions(), tx -> tx.getShortTimestamp() + " " + tx.formatForDisplay()),
        page.formatPageIndicator());
  }

  static MessageEmbed buildCurrencyHistoryEmbed(CurrencyTransactionService.TransactionPage page) {
    return buildHistoryEmbed(
        "💰 貨幣流水",
        "目前沒有任何貨幣流水紀錄",
        formatHistoryLines(
            page.transactions(), tx -> tx.getShortTimestamp() + " " + tx.formatForDisplay()),
        page.formatPageIndicator());
  }

  static List<Button> buildTokenPaginationButtons(TransactionPage page) {
    return buildPaginationButtons(
        UserPanelButtonHandler.BUTTON_PREFIX_TOKEN_PAGE,
        page.currentPage(),
        page.hasPreviousPage(),
        page.hasNextPage());
  }

  static List<Button> buildCurrencyPaginationButtons(
      CurrencyTransactionService.TransactionPage page) {
    return buildPaginationButtons(
        UserPanelButtonHandler.BUTTON_PREFIX_CURRENCY_PAGE,
        page.currentPage(),
        page.hasPreviousPage(),
        page.hasNextPage());
  }

  static MessageEmbed buildProductRedemptionHistoryEmbed(
      ProductRedemptionTransactionService.TransactionPage page) {
    return buildHistoryEmbed(
        "🛒 商品流水",
        "目前沒有任何商品兌換紀錄",
        formatHistoryLines(
            page.transactions(), tx -> tx.getShortTimestamp() + " " + tx.formatForDisplay()),
        page.formatPageIndicator());
  }

  static List<Button> buildProductRedemptionPaginationButtons(
      ProductRedemptionTransactionService.TransactionPage page) {
    return buildPaginationButtons(
        UserPanelButtonHandler.BUTTON_PREFIX_PRODUCT_REDEMPTION_PAGE,
        page.currentPage(),
        page.hasPreviousPage(),
        page.hasNextPage());
  }

  private static MessageEmbed buildHistoryEmbed(
      String title, String emptyStateMessage, List<String> lines, String footer) {
    String description = lines.isEmpty() ? emptyStateMessage : String.join("\n", lines) + "\n";
    return PanelComponentRenderer.buildEmbed(
        new EmbedView(title, description, EMBED_COLOR, List.of(), footer));
  }

  private static List<Button> buildPaginationButtons(
      String buttonPrefix, int currentPage, boolean hasPreviousPage, boolean hasNextPage) {
    List<ButtonView> buttonViews = new ArrayList<>();
    buttonViews.add(
        new ButtonView(
            UserPanelButtonHandler.BUTTON_BACK_TO_PANEL, "🔙 返回主頁", ButtonStyle.SECONDARY, false));

    if (hasPreviousPage) {
      buttonViews.add(
          new ButtonView(buttonPrefix + (currentPage - 1), "⬅️ 上一頁", ButtonStyle.SECONDARY, false));
    }

    if (hasNextPage) {
      buttonViews.add(
          new ButtonView(buttonPrefix + (currentPage + 1), "下一頁 ➡️", ButtonStyle.SECONDARY, false));
    }

    return PanelComponentRenderer.buildButtons(buttonViews);
  }

  private static <T> List<String> formatHistoryLines(
      List<T> transactions, Function<T, String> lineFormatter) {
    return transactions.stream().map(lineFormatter).toList();
  }
}
