package ltdjms.discord.shop.commands;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.currency.services.BalanceService;
import ltdjms.discord.discord.domain.ButtonView;
import ltdjms.discord.discord.services.DiscordComponentRenderer;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.product.services.ProductService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shop.services.CurrencyPurchaseService;
import ltdjms.discord.shop.services.FiatOrderService;
import ltdjms.discord.shop.services.ShopAdminNotificationService;
import ltdjms.discord.shop.services.ShopView;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

/** Handles select menu and button interactions for shop purchase. */
public class ShopSelectMenuHandler extends ListenerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ShopSelectMenuHandler.class);

  public static final String BUTTON_CONFIRM_PURCHASE = "shop_confirm_purchase_";
  public static final String BUTTON_CANCEL_PURCHASE = "shop_cancel_purchase";

  private final ProductService productService;
  private final BalanceService balanceService;
  private final CurrencyPurchaseService purchaseService;
  private final FiatOrderService fiatOrderService;
  private final ShopAdminNotificationService adminNotificationService;
  private final Set<String> inflightFiatOrders = ConcurrentHashMap.newKeySet();

  public ShopSelectMenuHandler(
      ProductService productService,
      BalanceService balanceService,
      CurrencyPurchaseService purchaseService,
      FiatOrderService fiatOrderService,
      ShopAdminNotificationService adminNotificationService) {
    this.productService = productService;
    this.balanceService = balanceService;
    this.purchaseService = purchaseService;
    this.fiatOrderService = fiatOrderService;
    this.adminNotificationService = adminNotificationService;
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
    String selectId = event.getComponentId();

    if (!selectId.equals(ShopView.SELECT_PURCHASE_PRODUCT)
        && !selectId.equals(ShopView.SELECT_FIAT_PRODUCT)) {
      return;
    }

    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    long guildId = event.getGuild().getIdLong();
    long userId = event.getUser().getIdLong();

    try {
      if (event.getValues().isEmpty()) {
        event.reply("請先選擇商品").setEphemeral(true).queue();
        return;
      }

      if (selectId.equals(ShopView.SELECT_FIAT_PRODUCT)) {
        handleFiatOrderSelect(event, guildId, userId);
        return;
      }

      String productIdStr = event.getValues().get(0);
      long productId = Long.parseLong(productIdStr);

      productService
          .getProduct(productId)
          .ifPresentOrElse(
              product -> {
                if (!product.hasCurrencyPrice()) {
                  event.reply("此商品不可用貨幣購買").setEphemeral(true).queue();
                  return;
                }

                // Get user balance
                var balanceResult = balanceService.tryGetBalance(guildId, userId);
                long userBalance = balanceResult.isOk() ? balanceResult.getValue().balance() : 0;

                event
                    .editMessageEmbeds(ShopView.buildPurchaseConfirmEmbed(product, userBalance))
                    .setComponents(
                        List.of(
                            DiscordComponentRenderer.buildActionRow(
                                List.of(
                                    new ButtonView(
                                        BUTTON_CONFIRM_PURCHASE + productId,
                                        "確認購買",
                                        ButtonStyle.SUCCESS,
                                        false),
                                    new ButtonView(
                                        BUTTON_CANCEL_PURCHASE,
                                        "取消",
                                        ButtonStyle.SECONDARY,
                                        false)))))
                    .queue();
              },
              () -> event.reply("找不到該商品").setEphemeral(true).queue());
    } catch (Exception e) {
      LOG.error("Error handling purchase select: {}", selectId, e);
      event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
    }
  }

  private void handleFiatOrderSelect(
      StringSelectInteractionEvent event, long guildId, long userId) {
    long productId = Long.parseLong(event.getValues().get(0));
    String inflightKey = buildFiatOrderInflightKey(guildId, userId, productId);
    if (!inflightFiatOrders.add(inflightKey)) {
      event.reply("⚠️ 這筆法幣訂單正在處理中，請稍候檢查互動結果。").setEphemeral(true).queue();
      return;
    }
    event
        .deferReply(true)
        .queue(
            hook ->
                processDeferredFiatOrderSelection(
                    hook, event.getUser(), guildId, userId, productId, inflightKey),
            failure -> {
              inflightFiatOrders.remove(inflightKey);
              LOG.warn(
                  "Failed to defer fiat order reply: guildId={}, userId={}, productId={}",
                  guildId,
                  userId,
                  productId,
                  failure);
            });
  }

  private void processDeferredFiatOrderSelection(
      InteractionHook hook,
      User user,
      long guildId,
      long userId,
      long productId,
      String inflightKey) {
    try {
      Result<FiatOrderService.FiatOrderResult, DomainError> orderResult =
          fiatOrderService.createFiatOnlyOrder(guildId, userId, productId);
      if (orderResult.isErr()) {
        completeDeferredFiatOrder(hook, "下單失敗：" + orderResult.getError().message(), inflightKey);
        return;
      }

      FiatOrderService.FiatOrderResult order = orderResult.getValue();
      user.openPrivateChannel()
          .queue(
              channel ->
                  channel
                      .sendMessage(order.formatDirectMessage())
                      .queue(
                          success ->
                              completeDeferredFiatOrder(
                                  hook,
                                  buildFiatOrderInteractionMessage(order, true, null),
                                  inflightKey),
                          failure -> {
                            LOG.warn(
                                "Failed to DM fiat order info: userId={}, orderNumber={}",
                                userId,
                                order.orderNumber(),
                                failure);
                            completeDeferredFiatOrder(
                                hook,
                                buildFiatOrderInteractionMessage(
                                    order, false, "⚠️ 無法私訊你，請直接使用以下資訊付款。"),
                                inflightKey);
                          }),
              failure -> {
                LOG.warn(
                    "Failed to open DM for fiat order info: userId={}, orderNumber={}",
                    userId,
                    order.orderNumber(),
                    failure);
                completeDeferredFiatOrder(
                    hook,
                    buildFiatOrderInteractionMessage(order, false, "⚠️ 無法開啟私訊，請直接使用以下資訊付款。"),
                    inflightKey);
              });
    } catch (Exception e) {
      LOG.error(
          "Error processing deferred fiat order: guildId={}, userId={}, productId={}",
          guildId,
          userId,
          productId,
          e);
      completeDeferredFiatOrder(hook, "發生錯誤，請稍後再試", inflightKey);
    }
  }

  private void completeDeferredFiatOrder(InteractionHook hook, String message, String inflightKey) {
    hook.editOriginal(message)
        .queue(
            success -> inflightFiatOrders.remove(inflightKey),
            failure -> {
              inflightFiatOrders.remove(inflightKey);
              LOG.warn("Failed to edit deferred fiat order reply", failure);
            });
  }

  private String buildFiatOrderInteractionMessage(
      FiatOrderService.FiatOrderResult order, boolean dmDelivered, String dmWarning) {
    StringBuilder sb = new StringBuilder();
    if (dmDelivered) {
      sb.append("✅ 法幣訂單已建立，完整付款資訊也已私訊給你。\n\n");
    } else {
      sb.append("✅ 法幣訂單已建立。\n");
      if (dmWarning != null && !dmWarning.isBlank()) {
        sb.append(dmWarning).append("\n\n");
      } else {
        sb.append("\n");
      }
    }
    sb.append("**商品：** ").append(order.product().name()).append("\n");
    sb.append("**訂單編號：** `").append(order.orderNumber()).append("`\n");
    sb.append("**超商代碼：** `").append(order.paymentNo()).append("`\n");
    sb.append("**金額：** ").append(order.product().formatFiatPriceTwd()).append("\n");
    if (order.expireDate() != null && !order.expireDate().isBlank()) {
      sb.append("**繳費期限：** ").append(order.expireDate()).append("\n");
    }
    if (order.paymentUrl() != null && !order.paymentUrl().isBlank()) {
      sb.append("**繳費說明：** ").append(order.paymentUrl()).append("\n");
    }
    if (order.fulfillmentWarning() != null && !order.fulfillmentWarning().isBlank()) {
      sb.append(order.fulfillmentWarning()).append("\n");
    }
    sb.append("\n若需查詢訂單或回報付款，請提供訂單編號給管理員。");
    return sb.toString();
  }

  private String buildFiatOrderInflightKey(long guildId, long userId, long productId) {
    return guildId + ":" + userId + ":" + productId;
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    String buttonId = event.getComponentId();

    if (!buttonId.startsWith(BUTTON_CONFIRM_PURCHASE) && !buttonId.equals(BUTTON_CANCEL_PURCHASE)) {
      return;
    }

    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    try {
      if (buttonId.equals(BUTTON_CANCEL_PURCHASE)) {
        event.reply("已取消購買").setEphemeral(true).queue();
        return;
      }

      // Extract product ID from button ID
      String productIdStr = buttonId.substring(BUTTON_CONFIRM_PURCHASE.length());
      long productId = Long.parseLong(productIdStr);

      long guildId = event.getGuild().getIdLong();
      long userId = event.getUser().getIdLong();

      // Process purchase
      Result<CurrencyPurchaseService.PurchaseResult, DomainError> purchaseResult =
          purchaseService.purchaseProduct(guildId, userId, productId);

      if (purchaseResult.isErr()) {
        event.reply("購買失敗：" + purchaseResult.getError().message()).setEphemeral(true).queue();
        return;
      }

      notifyAdminsOrderCreated(
          event.getGuild(), userId, purchaseResult.getValue().product(), "貨幣購買", null);
      event.reply(purchaseResult.getValue().formatSuccessMessage()).setEphemeral(true).queue();
    } catch (Exception e) {
      LOG.error("Error handling purchase button: {}", buttonId, e);
      event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
    }
  }

  private void notifyAdminsOrderCreated(
      net.dv8tion.jda.api.entities.Guild guild,
      long buyerUserId,
      Product product,
      String orderType,
      String orderReference) {
    if (guild == null) {
      return;
    }
    adminNotificationService.notifyAdminsOrderCreated(
        guild.getIdLong(), buyerUserId, product, orderType, orderReference);
  }
}
