package ltdjms.discord.dispatch.commands;

import java.awt.Color;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;

/** 派單面板的視圖組裝工具。 */
public final class DispatchPanelView {

  private static final Color EMBED_COLOR = new Color(0x5865F2);
  private static final String NOT_SELECTED = "尚未選擇";

  public static final String SELECT_ESCORT_USER = "dispatch_select_escort_user";
  public static final String SELECT_CUSTOMER_USER = "dispatch_select_customer_user";

  public static final String BUTTON_CREATE_ORDER = "dispatch_create_order";
  public static final String BUTTON_HISTORY = "dispatch_history";
  public static final String BUTTON_CONFIRM_ORDER_PREFIX = "dispatch_confirm_order_";
  public static final String BUTTON_COMPLETE_ORDER_PREFIX = "dispatch_complete_order_";
  public static final String BUTTON_CUSTOMER_CONFIRM_COMPLETION_PREFIX =
      "dispatch_customer_confirm_completion_";
  public static final String BUTTON_CUSTOMER_REQUEST_AFTER_SALES_PREFIX =
      "dispatch_customer_request_after_sales_";
  public static final String BUTTON_AFTER_SALES_CLAIM_PREFIX = "dispatch_after_sales_claim_";
  public static final String BUTTON_AFTER_SALES_CLOSE_PREFIX = "dispatch_after_sales_close_";

  private DispatchPanelView() {
    // Utility class
  }

  public static MessageEmbed buildPanelEmbed(
      String escortUserMention, String customerUserMention, String statusMessage) {
    EmbedBuilder builder =
        new EmbedBuilder()
            .setTitle("🛡️ 護航派單面板")
            .setColor(EMBED_COLOR)
            .setDescription("請選擇護航者與客戶，完成後點擊「建立派單」。")
            .addField("護航者", fallback(escortUserMention), true)
            .addField("客戶", fallback(customerUserMention), true)
            .setFooter("限制：護航者與客戶不可為同一人");

    if (statusMessage != null && !statusMessage.isBlank()) {
      builder.addField("狀態", statusMessage, false);
    }

    return builder.build();
  }

  public static List<ActionRow> buildPanelComponents(boolean canCreateOrder) {
    EntitySelectMenu escortUserSelect =
        EntitySelectMenu.create(SELECT_ESCORT_USER, EntitySelectMenu.SelectTarget.USER)
            .setPlaceholder("選擇護航者")
            .setRequiredRange(1, 1)
            .build();

    EntitySelectMenu customerUserSelect =
        EntitySelectMenu.create(SELECT_CUSTOMER_USER, EntitySelectMenu.SelectTarget.USER)
            .setPlaceholder("選擇客戶")
            .setRequiredRange(1, 1)
            .build();

    Button createButton =
        canCreateOrder
            ? Button.success(BUTTON_CREATE_ORDER, "✅ 建立派單")
            : Button.success(BUTTON_CREATE_ORDER, "✅ 建立派單").asDisabled();
    Button historyButton = Button.secondary(BUTTON_HISTORY, "📜 歷史記錄");

    return List.of(
        ActionRow.of(escortUserSelect),
        ActionRow.of(customerUserSelect),
        ActionRow.of(createButton, historyButton));
  }

  private static String fallback(String value) {
    return value == null || value.isBlank() ? NOT_SELECTED : value;
  }
}
