package ltdjms.discord.dispatch.commands;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

import ltdjms.discord.discord.domain.EmbedView;
import ltdjms.discord.discord.services.DiscordComponentRenderer;
import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import net.dv8tion.jda.api.entities.MessageEmbed;

final class DispatchPanelMessageFactory {

  private static final Color INFO_COLOR = new Color(0x57F287);
  private static final Color WARNING_COLOR = new Color(0xFEE75C);
  private static final Color ERROR_COLOR = new Color(0xED4245);

  private DispatchPanelMessageFactory() {}

  static MessageEmbed buildHistoryEmbed(List<EscortDispatchOrder> orders) {
    if (orders.isEmpty()) {
      return buildDispatchEmbed("📜 護航派單歷史", INFO_COLOR, "目前沒有歷史訂單。", List.of());
    }

    StringBuilder history = new StringBuilder();
    int index = 1;
    for (EscortDispatchOrder order : orders) {
      history
          .append("**")
          .append(index++)
          .append(".** `")
          .append(order.orderNumber())
          .append("` | ")
          .append(toStatusText(order.status()))
          .append("\n")
          .append("護航：<@")
          .append(order.escortUserId())
          .append(">　客戶：<@")
          .append(order.customerUserId())
          .append(">\n")
          .append("建立：<t:")
          .append(order.createdAt().getEpochSecond())
          .append(":R>\n\n");
    }

    return buildDispatchEmbed("📜 護航派單歷史", INFO_COLOR, history.toString(), List.of());
  }

  static MessageEmbed buildEscortPendingEmbed(EscortDispatchOrder order, String customerMention) {
    return buildDispatchEmbed(
        "📩 新護航派單通知",
        INFO_COLOR,
        "你收到一張新的護航訂單，請點擊下方按鈕確認接單。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("客戶", customerMention, false),
            new EmbedView.FieldView(
                "建立時間", "<t:" + order.createdAt().getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildEscortConfirmedEmbed(EscortDispatchOrder order) {
    Instant confirmedAt = order.confirmedAt() != null ? order.confirmedAt() : Instant.now();
    return buildDispatchEmbed(
        "✅ 已確認接單",
        INFO_COLOR,
        "你已成功確認此護航訂單。服務完成後請點擊下方按鈕。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("確認時間", "<t:" + confirmedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildEscortCompletionRequestedEmbed(EscortDispatchOrder order) {
    Instant requestedAt =
        order.completionRequestedAt() != null ? order.completionRequestedAt() : Instant.now();
    return buildDispatchEmbed(
        "⏳ 已送出完成請求",
        WARNING_COLOR,
        "系統已通知客戶確認完成，若客戶 24 小時未回應將自動完成。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("送出時間", "<t:" + requestedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildCustomerOrderConfirmedEmbed(
      EscortDispatchOrder order, String escortMention) {
    Instant confirmedAt = order.confirmedAt() != null ? order.confirmedAt() : Instant.now();
    return buildDispatchEmbed(
        "📣 護航訂單已確認",
        INFO_COLOR,
        "你的護航訂單已由護航者確認。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("護航者", escortMention, false),
            new EmbedView.FieldView("確認時間", "<t:" + confirmedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildCustomerCompletionActionEmbed(
      EscortDispatchOrder order, String escortMention) {
    Instant requestedAt =
        order.completionRequestedAt() != null ? order.completionRequestedAt() : Instant.now();
    return buildDispatchEmbed(
        "🧾 請確認護航訂單狀態",
        WARNING_COLOR,
        "護航者已提交完成，請選擇確認完成或申請售後。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("護航者", escortMention, false),
            new EmbedView.FieldView("送出時間", "<t:" + requestedAt.getEpochSecond() + ":F>", false)),
        "24 小時未確認將視為訂單完成");
  }

  static MessageEmbed buildCustomerCompletedEmbed(EscortDispatchOrder order) {
    Instant completedAt = order.completedAt() != null ? order.completedAt() : Instant.now();
    return buildDispatchEmbed(
        "✅ 訂單已完成",
        INFO_COLOR,
        "你已確認此訂單完成。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("完成時間", "<t:" + completedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildEscortOrderCompletedEmbed(EscortDispatchOrder order) {
    Instant completedAt = order.completedAt() != null ? order.completedAt() : Instant.now();
    return buildDispatchEmbed(
        "✅ 客戶已確認完成",
        INFO_COLOR,
        "客戶已確認訂單完成。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("完成時間", "<t:" + completedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildCustomerAfterSalesRequestedEmbed(
      EscortDispatchOrder order, String statusText) {
    Instant requestedAt =
        order.afterSalesRequestedAt() != null ? order.afterSalesRequestedAt() : Instant.now();
    return buildDispatchEmbed(
        "🧰 已提交售後申請",
        ERROR_COLOR,
        statusText,
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("申請時間", "<t:" + requestedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildAfterSalesNotificationEmbed(EscortDispatchOrder order) {
    Instant requestedAt =
        order.afterSalesRequestedAt() != null ? order.afterSalesRequestedAt() : Instant.now();
    return buildDispatchEmbed(
        "🧰 新售後申請",
        ERROR_COLOR,
        "有客戶提交了護航售後申請，請接手處理。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("護航者", "<@" + order.escortUserId() + ">", true),
            new EmbedView.FieldView("客戶", "<@" + order.customerUserId() + ">", true),
            new EmbedView.FieldView("申請時間", "<t:" + requestedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildAfterSalesClaimedEmbed(
      EscortDispatchOrder order, String assigneeMention) {
    Instant assignedAt =
        order.afterSalesAssignedAt() != null ? order.afterSalesAssignedAt() : Instant.now();
    return buildDispatchEmbed(
        "🛠️ 已接手售後案件",
        INFO_COLOR,
        "你已成功接手此售後案件，完成後請點擊 close file。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("接手人員", assigneeMention, false),
            new EmbedView.FieldView("接手時間", "<t:" + assignedAt.getEpochSecond() + ":F>", false)));
  }

  static MessageEmbed buildAfterSalesClosedEmbed(EscortDispatchOrder order) {
    Instant closedAt =
        order.afterSalesClosedAt() != null ? order.afterSalesClosedAt() : Instant.now();
    return buildDispatchEmbed(
        "✅ 售後案件已結案",
        INFO_COLOR,
        "你已完成此售後案件。",
        List.of(
            new EmbedView.FieldView("訂單編號", "`" + order.orderNumber() + "`", false),
            new EmbedView.FieldView("結案時間", "<t:" + closedAt.getEpochSecond() + ":F>", false)));
  }

  private static String toStatusText(EscortDispatchOrder.Status status) {
    return switch (status) {
      case PENDING_CONFIRMATION -> "等待護航者確認";
      case CONFIRMED -> "護航者已確認";
      case PENDING_CUSTOMER_CONFIRMATION -> "等待客戶確認";
      case COMPLETED -> "已完成";
      case AFTER_SALES_REQUESTED -> "售後待接手";
      case AFTER_SALES_IN_PROGRESS -> "售後處理中";
      case AFTER_SALES_CLOSED -> "售後已結案";
    };
  }

  private static MessageEmbed buildDispatchEmbed(
      String title, Color color, String description, List<EmbedView.FieldView> fields) {
    return buildDispatchEmbed(title, color, description, fields, null);
  }

  private static MessageEmbed buildDispatchEmbed(
      String title,
      Color color,
      String description,
      List<EmbedView.FieldView> fields,
      String footer) {
    return DiscordComponentRenderer.buildEmbed(
        new EmbedView(title, description, color, fields, footer));
  }
}
