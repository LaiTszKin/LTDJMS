package ltdjms.discord.dispatch.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.discord.domain.ButtonView;
import ltdjms.discord.discord.domain.EmbedView;
import ltdjms.discord.discord.services.DiscordComponentRenderer;
import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.services.DispatchAfterSalesStaffService;
import ltdjms.discord.dispatch.services.EscortDispatchOrderService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

/**
 * 派單面板互動處理器：
 *
 * <ul>
 *   <li>管理員面板上的成員選取
 *   <li>建立派單與歷史查詢
 *   <li>護航者私訊中的確認接單、送出完成
 *   <li>客戶私訊中的確認完成、申請售後
 *   <li>售後私訊中的接手與結案
 * </ul>
 */
public class DispatchPanelInteractionHandler extends ListenerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(DispatchPanelInteractionHandler.class);

  private static final Color INFO_COLOR = new Color(0x57F287);
  private static final Color WARNING_COLOR = new Color(0xFEE75C);
  private static final Color ERROR_COLOR = new Color(0xED4245);

  private final EscortDispatchOrderService orderService;
  private final DispatchAfterSalesStaffService afterSalesStaffService;
  private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

  public DispatchPanelInteractionHandler(
      EscortDispatchOrderService orderService,
      DispatchAfterSalesStaffService afterSalesStaffService) {
    this.orderService = orderService;
    this.afterSalesStaffService = afterSalesStaffService;
  }

  @Override
  public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
    String selectId = event.getComponentId();

    if (!isDispatchSelect(selectId)) {
      return;
    }

    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用派單面板").setEphemeral(true).queue();
      return;
    }

    String sessionKey = getSessionKey(event.getUser().getIdLong(), event.getGuild().getIdLong());
    SessionState state = sessionStates.computeIfAbsent(sessionKey, key -> new SessionState());

    try {
      switch (selectId) {
        case DispatchPanelView.SELECT_ESCORT_USER -> handleEscortUserSelect(event, state);
        case DispatchPanelView.SELECT_CUSTOMER_USER -> handleCustomerUserSelect(event, state);
        default -> LOG.warn("Unknown dispatch select menu: {}", selectId);
      }
    } catch (Exception e) {
      LOG.error("Error handling dispatch select interaction: {}", selectId, e);
      event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
    }
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
    String selectId = event.getComponentId();

    if (!isDispatchStringSelect(selectId)) {
      return;
    }

    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用派單面板").setEphemeral(true).queue();
      return;
    }

    String sessionKey = getSessionKey(event.getUser().getIdLong(), event.getGuild().getIdLong());
    SessionState state = sessionStates.computeIfAbsent(sessionKey, key -> new SessionState());

    try {
      switch (selectId) {
        case DispatchPanelView.SELECT_MODE -> handleModeSelect(event, state);
        case DispatchPanelView.SELECT_ORDER_OPTION, DispatchPanelView.SELECT_ORDER_OPTION_EXTRA ->
            handleOrderOptionSelect(event, state);
        case DispatchPanelView.SELECT_PENDING_ORDER -> handlePendingOrderSelect(event, state);
        default -> LOG.warn("Unknown dispatch string select menu: {}", selectId);
      }
    } catch (Exception e) {
      LOG.error("Error handling dispatch string select interaction: {}", selectId, e);
      event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
    }
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    String buttonId = event.getComponentId();

    if (DispatchPanelView.BUTTON_CREATE_ORDER.equals(buttonId)) {
      handleCreateOrder(event);
      return;
    }
    if (DispatchPanelView.BUTTON_ASSIGN_ORDER.equals(buttonId)) {
      handleAssignOrder(event);
      return;
    }
    if (DispatchPanelView.BUTTON_BACK_TO_MODE.equals(buttonId)) {
      handleBackToMode(event);
      return;
    }
    if (DispatchPanelView.BUTTON_HISTORY.equals(buttonId)) {
      handleHistory(event);
      return;
    }
    if (buttonId.startsWith(DispatchPanelView.BUTTON_CONFIRM_ORDER_PREFIX)) {
      handleOrderConfirmation(event, buttonId);
      return;
    }
    if (buttonId.startsWith(DispatchPanelView.BUTTON_COMPLETE_ORDER_PREFIX)) {
      handleEscortCompletionRequest(event, buttonId);
      return;
    }
    if (buttonId.startsWith(DispatchPanelView.BUTTON_CUSTOMER_CONFIRM_COMPLETION_PREFIX)) {
      handleCustomerCompletionConfirmation(event, buttonId);
      return;
    }
    if (buttonId.startsWith(DispatchPanelView.BUTTON_CUSTOMER_REQUEST_AFTER_SALES_PREFIX)) {
      handleCustomerAfterSalesRequest(event, buttonId);
      return;
    }
    if (buttonId.startsWith(DispatchPanelView.BUTTON_AFTER_SALES_CLAIM_PREFIX)) {
      handleAfterSalesClaim(event, buttonId);
      return;
    }
    if (buttonId.startsWith(DispatchPanelView.BUTTON_AFTER_SALES_CLOSE_PREFIX)) {
      handleAfterSalesClose(event, buttonId);
    }
  }

  private boolean isDispatchSelect(String selectId) {
    return DispatchPanelView.SELECT_ESCORT_USER.equals(selectId)
        || DispatchPanelView.SELECT_CUSTOMER_USER.equals(selectId);
  }

  private boolean isDispatchStringSelect(String selectId) {
    return DispatchPanelView.SELECT_MODE.equals(selectId)
        || DispatchPanelView.SELECT_ORDER_OPTION.equals(selectId)
        || DispatchPanelView.SELECT_ORDER_OPTION_EXTRA.equals(selectId)
        || DispatchPanelView.SELECT_PENDING_ORDER.equals(selectId);
  }

  private void handleModeSelect(StringSelectInteractionEvent event, SessionState state) {
    if (event.getValues().isEmpty()) {
      event.reply("請選擇操作").setEphemeral(true).queue();
      return;
    }

    String selectedMode = event.getValues().get(0);
    state.resetForMode(selectedMode);
    if (state.isCreateMode()) {
      refreshCreatePanel(event, state);
      return;
    }
    if (state.isAssignMode()) {
      refreshAssignPanel(event, state, null);
      return;
    }
    state.mode = null;
    event.reply("未知的派單操作").setEphemeral(true).queue();
  }

  private void handleOrderOptionSelect(StringSelectInteractionEvent event, SessionState state) {
    if (!state.isCreateMode()) {
      event.reply("請先選擇開單流程").setEphemeral(true).queue();
      return;
    }
    if (event.getValues().isEmpty()) {
      event.reply("請選擇護航品類").setEphemeral(true).queue();
      return;
    }

    state.escortOptionCode = event.getValues().get(0);
    state.statusMessage = "✅ 已選擇護航品類（尚未建立訂單）";
    refreshCreatePanel(event, state);
  }

  private void handlePendingOrderSelect(StringSelectInteractionEvent event, SessionState state) {
    if (!state.isAssignMode()) {
      event.reply("請先選擇派單流程").setEphemeral(true).queue();
      return;
    }
    if (event.getValues().isEmpty()) {
      event.reply("請選擇待派單訂單").setEphemeral(true).queue();
      return;
    }

    state.selectedOrderNumber = event.getValues().get(0);
    state.statusMessage = "✅ 已選擇待派單訂單";
    refreshAssignPanel(event, state, null);
  }

  private void handleEscortUserSelect(EntitySelectInteractionEvent event, SessionState state) {
    if (!state.isAssignMode()) {
      event.reply("請先選擇派單流程").setEphemeral(true).queue();
      return;
    }
    List<User> users = event.getMentions().getUsers();
    if (users.isEmpty()) {
      event.reply("請選擇護航者").setEphemeral(true).queue();
      return;
    }

    User user = users.get(0);
    state.escortUserId = user.getIdLong();
    state.escortUserMention = user.getAsMention();
    state.statusMessage = "✅ 已選擇護航者";

    refreshAssignPanel(event, state, null);
  }

  private void handleCustomerUserSelect(EntitySelectInteractionEvent event, SessionState state) {
    if (!state.isCreateMode()) {
      event.reply("請先選擇開單流程").setEphemeral(true).queue();
      return;
    }
    List<User> users = event.getMentions().getUsers();
    if (users.isEmpty()) {
      event.reply("請選擇客戶").setEphemeral(true).queue();
      return;
    }

    User user = users.get(0);
    state.customerUserId = user.getIdLong();
    state.customerUserMention = user.getAsMention();
    state.statusMessage = "✅ 已選擇客戶（尚未建立訂單）";

    refreshCreatePanel(event, state);
  }

  private void refreshCreatePanel(
      net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent event,
      SessionState state) {
    event
        .editMessageEmbeds(
            DispatchPanelView.buildCreateOrderEmbed(
                state.customerUserMention, state.escortOptionCode, state.getCreateStatusMessage()))
        .setComponents(
            DispatchPanelView.buildCreateOrderComponents(
                state.canCreateOpenOrder(), state.escortOptionCode))
        .queue();
  }

  private PendingAssignmentLoadResult loadPendingAssignmentOrders(long guildId) {
    Result<List<EscortDispatchOrder>, DomainError> result =
        orderService.findPendingAssignmentOrders(guildId, 25);
    if (result.isOk()) {
      return new PendingAssignmentLoadResult(result.getValue(), null);
    }

    LOG.warn(
        "Failed to load pending assignment dispatch orders: guildId={}, reason={}",
        guildId,
        result.getError().message());
    return new PendingAssignmentLoadResult(
        List.of(), "⚠️ 無法載入待派單訂單：" + result.getError().message());
  }

  private void refreshAssignPanel(
      net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent event,
      SessionState state,
      String statusOverride) {
    PendingAssignmentLoadResult loadResult =
        loadPendingAssignmentOrders(event.getGuild().getIdLong());
    state.pendingOrders = loadResult.orders();
    String status =
        statusOverride != null && !statusOverride.isBlank()
            ? statusOverride
            : loadResult.statusMessage() != null
                ? loadResult.statusMessage()
                : state.getAssignStatusMessage();

    event
        .editMessageEmbeds(
            DispatchPanelView.buildAssignOrderEmbed(
                state.pendingOrders, state.selectedOrderNumber, state.escortUserMention, status))
        .setComponents(
            DispatchPanelView.buildAssignOrderComponents(
                state.pendingOrders, state.selectedOrderNumber, state.canAssignOrder()))
        .queue();
  }

  private void handleCreateOrder(ButtonInteractionEvent event) {
    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用派單面板").setEphemeral(true).queue();
      return;
    }

    long guildId = event.getGuild().getIdLong();
    long adminUserId = event.getUser().getIdLong();
    String sessionKey = getSessionKey(adminUserId, guildId);

    SessionState state = sessionStates.get(sessionKey);
    if (state == null || !state.isCreateMode() || !state.canCreateOpenOrder()) {
      event.reply("請先完整選擇客戶與護航品類").setEphemeral(true).queue();
      return;
    }

    event
        .deferReply(true)
        .queue(
            hook ->
                validateCustomerMemberAndCreateOpenOrder(
                    event, hook, sessionKey, guildId, adminUserId, state),
            failure -> LOG.warn("Failed to defer dispatch order creation interaction", failure));
  }

  private void handleAssignOrder(ButtonInteractionEvent event) {
    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用派單面板").setEphemeral(true).queue();
      return;
    }

    long guildId = event.getGuild().getIdLong();
    long adminUserId = event.getUser().getIdLong();
    String sessionKey = getSessionKey(adminUserId, guildId);

    SessionState state = sessionStates.get(sessionKey);
    if (state == null || !state.isAssignMode() || !state.canAssignOrder()) {
      event.reply("請先完整選擇待派單訂單與護航者").setEphemeral(true).queue();
      return;
    }

    event
        .deferReply(true)
        .queue(
            hook -> validateEscortMemberAndAssignOrder(event, hook, sessionKey, adminUserId, state),
            failure -> LOG.warn("Failed to defer dispatch order assignment interaction", failure));
  }

  private void handleBackToMode(ButtonInteractionEvent event) {
    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用派單面板").setEphemeral(true).queue();
      return;
    }

    sessionStates.remove(getSessionKey(event.getUser().getIdLong(), event.getGuild().getIdLong()));
    event
        .editMessageEmbeds(DispatchPanelView.buildModeEmbed(null))
        .setComponents(DispatchPanelView.buildModeComponents())
        .queue();
  }

  private void handleHistory(ButtonInteractionEvent event) {
    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用派單面板").setEphemeral(true).queue();
      return;
    }

    long guildId = event.getGuild().getIdLong();
    Result<List<EscortDispatchOrder>, DomainError> result =
        orderService.findRecentOrders(guildId, 10);

    if (result.isErr()) {
      event.reply("查詢歷史失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    event.replyEmbeds(buildHistoryEmbed(result.getValue())).setEphemeral(true).queue();
  }

  private void validateCustomerMemberAndCreateOpenOrder(
      ButtonInteractionEvent event,
      InteractionHook hook,
      String sessionKey,
      long guildId,
      long adminUserId,
      SessionState state) {

    event
        .getGuild()
        .retrieveMemberById(state.customerUserId)
        .queue(
            customerMember -> createOpenOrder(hook, sessionKey, guildId, adminUserId, state),
            failure -> hook.sendMessage("找不到指定客戶，請確認該成員仍在伺服器中").setEphemeral(true).queue());
  }

  private void createOpenOrder(
      InteractionHook hook, String sessionKey, long guildId, long adminUserId, SessionState state) {

    Result<EscortDispatchOrder, DomainError> result =
        orderService.createManualOpenOrder(
            guildId, adminUserId, state.customerUserId, state.escortOptionCode);

    if (result.isErr()) {
      hook.sendMessage("建立護航訂單失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder order = result.getValue();
    sessionStates.remove(sessionKey);
    hook.sendMessage("✅ 護航訂單已建立：`" + order.orderNumber() + "`，可至「派單」流程指派護航者。")
        .setEphemeral(true)
        .queue();
  }

  private void validateEscortMemberAndAssignOrder(
      ButtonInteractionEvent event,
      InteractionHook hook,
      String sessionKey,
      long adminUserId,
      SessionState state) {
    event
        .getGuild()
        .retrieveMemberById(state.escortUserId)
        .queue(
            escortMember ->
                assignOrderAndNotifyEscort(
                    hook, sessionKey, adminUserId, state, escortMember.getUser()),
            failure -> hook.sendMessage("找不到被派單的護航者，請確認該成員仍在伺服器中").setEphemeral(true).queue());
  }

  private void assignOrderAndNotifyEscort(
      InteractionHook hook,
      String sessionKey,
      long adminUserId,
      SessionState state,
      User escortUser) {
    Result<EscortDispatchOrder, DomainError> result =
        orderService.assignPendingOrder(state.selectedOrderNumber, adminUserId, state.escortUserId);

    if (result.isErr()) {
      hook.sendMessage("派發護航訂單失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder order = result.getValue();
    sendPendingOrderDm(
        hook, sessionKey, order, escortUser, formatUserMention(order.customerUserId()));
  }

  private void sendPendingOrderDm(
      InteractionHook hook,
      String sessionKey,
      EscortDispatchOrder order,
      User escortUser,
      String customerMention) {
    String confirmButtonId = DispatchPanelView.BUTTON_CONFIRM_ORDER_PREFIX + order.orderNumber();
    MessageEmbed dmEmbed = buildEscortPendingEmbed(order, customerMention);

    escortUser
        .openPrivateChannel()
        .queue(
            channel ->
                channel
                    .sendMessageEmbeds(dmEmbed)
                    .setComponents(
                        List.of(
                            DiscordComponentRenderer.buildActionRow(
                                List.of(
                                    new ButtonView(
                                        confirmButtonId, "✅ 確認接單", ButtonStyle.SUCCESS, false)))))
                    .queue(
                        success -> {
                          sessionStates.remove(sessionKey);
                          hook.sendMessage("✅ 派單已建立：`" + order.orderNumber() + "`，已通知護航者。")
                              .setEphemeral(true)
                              .queue();
                        },
                        failure -> {
                          sessionStates.remove(sessionKey);
                          LOG.warn(
                              "Escort DM send failed: orderNumber={}, escortUserId={}",
                              order.orderNumber(),
                              escortUser.getIdLong(),
                              failure);
                          hook.sendMessage("⚠️ 派單已建立：`" + order.orderNumber() + "`，但無法私訊護航者，請手動通知。")
                              .setEphemeral(true)
                              .queue();
                        }),
            failure -> {
              sessionStates.remove(sessionKey);
              LOG.warn(
                  "Failed to open escort DM channel: orderNumber={}, escortUserId={}",
                  order.orderNumber(),
                  escortUser.getIdLong(),
                  failure);
              hook.sendMessage("⚠️ 派單已建立：`" + order.orderNumber() + "`，但無法開啟護航者私訊，請手動通知。")
                  .setEphemeral(true)
                  .queue();
            });
  }

  private void handleOrderConfirmation(ButtonInteractionEvent event, String buttonId) {
    if (event.isFromGuild()) {
      event.reply("請在機器人私訊中確認接單").setEphemeral(true).queue();
      return;
    }

    String orderNumber =
        extractOrderNumber(buttonId, DispatchPanelView.BUTTON_CONFIRM_ORDER_PREFIX);
    if (orderNumber.isBlank()) {
      event.reply("訂單編號無效").setEphemeral(true).queue();
      return;
    }

    Result<EscortDispatchOrder, DomainError> result =
        orderService.confirmOrder(orderNumber, event.getUser().getIdLong());

    if (result.isErr()) {
      event.reply(result.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder confirmedOrder = result.getValue();
    String completeButtonId =
        DispatchPanelView.BUTTON_COMPLETE_ORDER_PREFIX + confirmedOrder.orderNumber();

    event
        .editMessageEmbeds(buildEscortConfirmedEmbed(confirmedOrder))
        .setComponents(
            List.of(
                DiscordComponentRenderer.buildActionRow(
                    List.of(
                        new ButtonView(completeButtonId, "🏁 完成訂單", ButtonStyle.PRIMARY, false)))))
        .queue(
            success -> notifyCustomerOrderConfirmed(event, confirmedOrder),
            failure -> {
              LOG.warn(
                  "Failed to update escort confirmation message: orderNumber={}",
                  confirmedOrder.orderNumber(),
                  failure);
              notifyCustomerOrderConfirmed(event, confirmedOrder);
            });
  }

  private void handleEscortCompletionRequest(ButtonInteractionEvent event, String buttonId) {
    if (event.isFromGuild()) {
      event.reply("請在機器人私訊中操作").setEphemeral(true).queue();
      return;
    }

    String orderNumber =
        extractOrderNumber(buttonId, DispatchPanelView.BUTTON_COMPLETE_ORDER_PREFIX);
    if (orderNumber.isBlank()) {
      event.reply("訂單編號無效").setEphemeral(true).queue();
      return;
    }

    Result<EscortDispatchOrder, DomainError> result =
        orderService.requestCompletion(orderNumber, event.getUser().getIdLong());
    if (result.isErr()) {
      event.reply(result.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder order = result.getValue();
    event
        .editMessageEmbeds(buildEscortCompletionRequestedEmbed(order))
        .setComponents()
        .queue(
            success ->
                notifyCustomerCompletionOptions(
                    order, event.getUser().getAsMention(), event.getJDA()),
            failure -> {
              LOG.warn(
                  "Failed to update escort completion request message: orderNumber={}",
                  order.orderNumber(),
                  failure);
              notifyCustomerCompletionOptions(
                  order, event.getUser().getAsMention(), event.getJDA());
            });
  }

  private void handleCustomerCompletionConfirmation(ButtonInteractionEvent event, String buttonId) {
    if (event.isFromGuild()) {
      event.reply("請在機器人私訊中操作").setEphemeral(true).queue();
      return;
    }

    String orderNumber =
        extractOrderNumber(buttonId, DispatchPanelView.BUTTON_CUSTOMER_CONFIRM_COMPLETION_PREFIX);
    if (orderNumber.isBlank()) {
      event.reply("訂單編號無效").setEphemeral(true).queue();
      return;
    }

    Result<EscortDispatchOrder, DomainError> result =
        orderService.customerConfirmCompletion(orderNumber, event.getUser().getIdLong());
    if (result.isErr()) {
      event.reply(result.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder order = result.getValue();
    event
        .editMessageEmbeds(buildCustomerCompletedEmbed(order))
        .setComponents()
        .queue(
            success -> notifyEscortOrderCompleted(order, event.getJDA()),
            failure -> {
              LOG.warn(
                  "Failed to update customer completion message: orderNumber={}",
                  order.orderNumber(),
                  failure);
              notifyEscortOrderCompleted(order, event.getJDA());
            });
  }

  private void handleCustomerAfterSalesRequest(ButtonInteractionEvent event, String buttonId) {
    if (event.isFromGuild()) {
      event.reply("請在機器人私訊中操作").setEphemeral(true).queue();
      return;
    }

    String orderNumber =
        extractOrderNumber(buttonId, DispatchPanelView.BUTTON_CUSTOMER_REQUEST_AFTER_SALES_PREFIX);
    if (orderNumber.isBlank()) {
      event.reply("訂單編號無效").setEphemeral(true).queue();
      return;
    }

    Result<EscortDispatchOrder, DomainError> result =
        orderService.requestAfterSales(orderNumber, event.getUser().getIdLong());
    if (result.isErr()) {
      event.reply(result.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder order = result.getValue();
    AfterSalesNotifyResult notifyResult = notifyAfterSalesStaff(order, event.getJDA());

    event
        .editMessageEmbeds(buildCustomerAfterSalesRequestedEmbed(order, notifyResult.message()))
        .setComponents()
        .queue();
  }

  private void handleAfterSalesClaim(ButtonInteractionEvent event, String buttonId) {
    if (event.isFromGuild()) {
      event.reply("請在機器人私訊中操作").setEphemeral(true).queue();
      return;
    }

    String orderNumber =
        extractOrderNumber(buttonId, DispatchPanelView.BUTTON_AFTER_SALES_CLAIM_PREFIX);
    if (orderNumber.isBlank()) {
      event.reply("訂單編號無效").setEphemeral(true).queue();
      return;
    }

    Optional<EscortDispatchOrder> orderOpt = orderService.findByOrderNumber(orderNumber);
    if (orderOpt.isEmpty()) {
      event.reply("找不到該訂單").setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder currentOrder = orderOpt.get();
    long userId = event.getUser().getIdLong();
    if (!afterSalesStaffService.isAfterSalesStaff(currentOrder.guildId(), userId)) {
      event.reply("你不是此伺服器設定的售後人員").setEphemeral(true).queue();
      return;
    }

    Result<EscortDispatchOrder, DomainError> claimResult =
        orderService.claimAfterSales(orderNumber, userId);
    if (claimResult.isErr()) {
      event.reply(claimResult.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder claimedOrder = claimResult.getValue();
    String closeButtonId =
        DispatchPanelView.BUTTON_AFTER_SALES_CLOSE_PREFIX + claimedOrder.orderNumber();

    event
        .editMessageEmbeds(
            buildAfterSalesClaimedEmbed(claimedOrder, event.getUser().getAsMention()))
        .setComponents(
            List.of(
                DiscordComponentRenderer.buildActionRow(
                    List.of(
                        new ButtonView(
                            closeButtonId, "✅ 完成 / close file", ButtonStyle.SUCCESS, false)))))
        .queue(
            success ->
                notifyCustomerAfterSalesAssigned(
                    claimedOrder, event.getUser().getAsMention(), event.getJDA()),
            failure -> {
              LOG.warn(
                  "Failed to update after-sales claim message: orderNumber={}",
                  claimedOrder.orderNumber(),
                  failure);
              notifyCustomerAfterSalesAssigned(
                  claimedOrder, event.getUser().getAsMention(), event.getJDA());
            });
  }

  private void handleAfterSalesClose(ButtonInteractionEvent event, String buttonId) {
    if (event.isFromGuild()) {
      event.reply("請在機器人私訊中操作").setEphemeral(true).queue();
      return;
    }

    String orderNumber =
        extractOrderNumber(buttonId, DispatchPanelView.BUTTON_AFTER_SALES_CLOSE_PREFIX);
    if (orderNumber.isBlank()) {
      event.reply("訂單編號無效").setEphemeral(true).queue();
      return;
    }

    Result<EscortDispatchOrder, DomainError> closeResult =
        orderService.closeAfterSales(orderNumber, event.getUser().getIdLong());
    if (closeResult.isErr()) {
      event.reply(closeResult.getError().message()).setEphemeral(true).queue();
      return;
    }

    EscortDispatchOrder closedOrder = closeResult.getValue();
    event
        .editMessageEmbeds(buildAfterSalesClosedEmbed(closedOrder))
        .setComponents()
        .queue(
            success ->
                notifyCustomerAfterSalesClosed(
                    closedOrder, event.getUser().getAsMention(), event.getJDA()),
            failure -> {
              LOG.warn(
                  "Failed to update after-sales closed message: orderNumber={}",
                  closedOrder.orderNumber(),
                  failure);
              notifyCustomerAfterSalesClosed(
                  closedOrder, event.getUser().getAsMention(), event.getJDA());
            });
  }

  private void notifyCustomerOrderConfirmed(
      ButtonInteractionEvent event, EscortDispatchOrder order) {
    event
        .getJDA()
        .retrieveUserById(order.customerUserId())
        .queue(
            customerUser ->
                customerUser
                    .openPrivateChannel()
                    .queue(
                        channel ->
                            channel
                                .sendMessageEmbeds(
                                    buildCustomerOrderConfirmedEmbed(
                                        order, event.getUser().getAsMention()))
                                .queue(),
                        failure ->
                            LOG.warn(
                                "Failed to open customer DM channel for confirmation notice:"
                                    + " orderNumber={}, customerUserId={}",
                                order.orderNumber(),
                                order.customerUserId(),
                                failure)),
            failure ->
                LOG.warn(
                    "Failed to retrieve customer user for confirmation notice: orderNumber={},"
                        + " customerUserId={}",
                    order.orderNumber(),
                    order.customerUserId(),
                    failure));
  }

  private void notifyCustomerCompletionOptions(
      EscortDispatchOrder order, String escortMention, net.dv8tion.jda.api.JDA jda) {
    String customerCompleteButtonId =
        DispatchPanelView.BUTTON_CUSTOMER_CONFIRM_COMPLETION_PREFIX + order.orderNumber();
    String afterSalesButtonId =
        DispatchPanelView.BUTTON_CUSTOMER_REQUEST_AFTER_SALES_PREFIX + order.orderNumber();

    jda.retrieveUserById(order.customerUserId())
        .queue(
            customerUser ->
                customerUser
                    .openPrivateChannel()
                    .queue(
                        channel ->
                            channel
                                .sendMessageEmbeds(
                                    buildCustomerCompletionActionEmbed(order, escortMention))
                                .setComponents(
                                    List.of(
                                        DiscordComponentRenderer.buildActionRow(
                                            List.of(
                                                new ButtonView(
                                                    customerCompleteButtonId,
                                                    "✅ 確認完成",
                                                    ButtonStyle.SUCCESS,
                                                    false),
                                                new ButtonView(
                                                    afterSalesButtonId,
                                                    "🧰 申請售後",
                                                    ButtonStyle.SECONDARY,
                                                    false)))))
                                .queue(),
                        failure ->
                            LOG.warn(
                                "Failed to open customer DM channel for completion actions:"
                                    + " orderNumber={}, customerUserId={}",
                                order.orderNumber(),
                                order.customerUserId(),
                                failure)),
            failure ->
                LOG.warn(
                    "Failed to retrieve customer user for completion actions: orderNumber={},"
                        + " customerUserId={}",
                    order.orderNumber(),
                    order.customerUserId(),
                    failure));
  }

  private void notifyEscortOrderCompleted(EscortDispatchOrder order, net.dv8tion.jda.api.JDA jda) {
    jda.retrieveUserById(order.escortUserId())
        .queue(
            escortUser ->
                escortUser
                    .openPrivateChannel()
                    .queue(
                        channel ->
                            channel
                                .sendMessageEmbeds(buildEscortOrderCompletedEmbed(order))
                                .queue(),
                        failure ->
                            LOG.warn(
                                "Failed to notify escort order completion: orderNumber={},"
                                    + " escortUserId={}",
                                order.orderNumber(),
                                order.escortUserId(),
                                failure)),
            failure ->
                LOG.warn(
                    "Failed to retrieve escort user for completion notice: orderNumber={},"
                        + " escortUserId={}",
                    order.orderNumber(),
                    order.escortUserId(),
                    failure));
  }

  private AfterSalesNotifyResult notifyAfterSalesStaff(
      EscortDispatchOrder order, net.dv8tion.jda.api.JDA jda) {
    Result<Set<Long>, DomainError> staffResult =
        afterSalesStaffService.getStaffUserIds(order.guildId());
    if (staffResult.isErr()) {
      LOG.warn(
          "Failed to query after-sales staff, orderNumber={}, reason={}",
          order.orderNumber(),
          staffResult.getError().message());
      return new AfterSalesNotifyResult("⚠️ 已送出售後申請，但查詢售後人員失敗，請聯絡管理員。");
    }

    Set<Long> staffUserIds = staffResult.getValue();
    if (staffUserIds.isEmpty()) {
      return new AfterSalesNotifyResult("⚠️ 已送出售後申請，但目前尚未設定售後人員。");
    }

    List<Long> onlineStaffUserIds = findOnlineAfterSalesStaff(order.guildId(), staffUserIds, jda);
    List<Long> targetUserIds =
        onlineStaffUserIds.isEmpty() ? new ArrayList<>(staffUserIds) : onlineStaffUserIds;

    String claimButtonId = DispatchPanelView.BUTTON_AFTER_SALES_CLAIM_PREFIX + order.orderNumber();
    MessageEmbed embed = buildAfterSalesNotificationEmbed(order);

    for (Long staffUserId : targetUserIds) {
      jda.retrieveUserById(staffUserId)
          .queue(
              user ->
                  user.openPrivateChannel()
                      .queue(
                          channel ->
                              channel
                                  .sendMessageEmbeds(embed)
                                  .setComponents(
                                      List.of(
                                          DiscordComponentRenderer.buildActionRow(
                                              List.of(
                                                  new ButtonView(
                                                      claimButtonId,
                                                      "🧰 接手案件",
                                                      ButtonStyle.PRIMARY,
                                                      false)))))
                                  .queue(),
                          failure ->
                              LOG.warn(
                                  "Failed to DM after-sales staff: orderNumber={}, staffUserId={}",
                                  order.orderNumber(),
                                  staffUserId,
                                  failure)),
              failure ->
                  LOG.warn(
                      "Failed to retrieve after-sales staff user: orderNumber={}, staffUserId={}",
                      order.orderNumber(),
                      staffUserId,
                      failure));
    }

    if (onlineStaffUserIds.isEmpty()) {
      return new AfterSalesNotifyResult("✅ 已通知全部售後人員，等待接手。");
    }
    return new AfterSalesNotifyResult("✅ 已通知 " + onlineStaffUserIds.size() + " 位在線售後人員，等待接手。");
  }

  private List<Long> findOnlineAfterSalesStaff(
      long guildId, Set<Long> staffUserIds, net.dv8tion.jda.api.JDA jda) {
    Guild guild = jda.getGuildById(guildId);
    if (guild == null) {
      return List.of();
    }

    List<Long> onlineStaffUserIds = new ArrayList<>();
    for (Long staffUserId : staffUserIds) {
      try {
        Member member = guild.retrieveMemberById(staffUserId).complete();
        if (member != null && member.getOnlineStatus() == OnlineStatus.ONLINE) {
          onlineStaffUserIds.add(staffUserId);
        }
      } catch (Exception e) {
        LOG.debug(
            "Failed to resolve after-sales staff online status: guildId={}, staffUserId={}",
            guildId,
            staffUserId,
            e);
      }
    }
    return onlineStaffUserIds;
  }

  private void notifyCustomerAfterSalesAssigned(
      EscortDispatchOrder order, String assigneeMention, net.dv8tion.jda.api.JDA jda) {
    jda.retrieveUserById(order.customerUserId())
        .queue(
            customerUser ->
                customerUser
                    .openPrivateChannel()
                    .queue(
                        channel ->
                            channel
                                .sendMessageEmbeds(
                                    buildDispatchEmbed(
                                        "🧰 售後已接手",
                                        INFO_COLOR,
                                        "你的售後申請已有專人接手。",
                                        List.of(
                                            new EmbedView.FieldView(
                                                "訂單編號", "`" + order.orderNumber() + "`", false),
                                            new EmbedView.FieldView(
                                                "接手售後", assigneeMention, false))))
                                .queue(),
                        failure ->
                            LOG.warn(
                                "Failed to DM customer after after-sales assigned: orderNumber={},"
                                    + " customerUserId={}",
                                order.orderNumber(),
                                order.customerUserId(),
                                failure)),
            failure ->
                LOG.warn(
                    "Failed to retrieve customer for after-sales assigned message: orderNumber={},"
                        + " customerUserId={}",
                    order.orderNumber(),
                    order.customerUserId(),
                    failure));
  }

  private void notifyCustomerAfterSalesClosed(
      EscortDispatchOrder order, String closerMention, net.dv8tion.jda.api.JDA jda) {
    jda.retrieveUserById(order.customerUserId())
        .queue(
            customerUser ->
                customerUser
                    .openPrivateChannel()
                    .queue(
                        channel ->
                            channel
                                .sendMessageEmbeds(
                                    buildDispatchEmbed(
                                        "✅ 售後已結案",
                                        INFO_COLOR,
                                        "你的售後案件已完成處理並結案。",
                                        List.of(
                                            new EmbedView.FieldView(
                                                "訂單編號", "`" + order.orderNumber() + "`", false),
                                            new EmbedView.FieldView("結案人員", closerMention, false))))
                                .queue(),
                        failure ->
                            LOG.warn(
                                "Failed to DM customer after after-sales closed: orderNumber={},"
                                    + " customerUserId={}",
                                order.orderNumber(),
                                order.customerUserId(),
                                failure)),
            failure ->
                LOG.warn(
                    "Failed to retrieve customer for after-sales closed message: orderNumber={},"
                        + " customerUserId={}",
                    order.orderNumber(),
                    order.customerUserId(),
                    failure));
  }

  private MessageEmbed buildHistoryEmbed(List<EscortDispatchOrder> orders) {
    return DispatchPanelMessageFactory.buildHistoryEmbed(orders);
  }

  private String toStatusText(EscortDispatchOrder.Status status) {
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

  private MessageEmbed buildEscortPendingEmbed(EscortDispatchOrder order, String customerMention) {
    return DispatchPanelMessageFactory.buildEscortPendingEmbed(order, customerMention);
  }

  private MessageEmbed buildEscortConfirmedEmbed(EscortDispatchOrder order) {
    return DispatchPanelMessageFactory.buildEscortConfirmedEmbed(order);
  }

  private MessageEmbed buildEscortCompletionRequestedEmbed(EscortDispatchOrder order) {
    return DispatchPanelMessageFactory.buildEscortCompletionRequestedEmbed(order);
  }

  private MessageEmbed buildCustomerOrderConfirmedEmbed(
      EscortDispatchOrder order, String escortMention) {
    return DispatchPanelMessageFactory.buildCustomerOrderConfirmedEmbed(order, escortMention);
  }

  private MessageEmbed buildCustomerCompletionActionEmbed(
      EscortDispatchOrder order, String escortMention) {
    return DispatchPanelMessageFactory.buildCustomerCompletionActionEmbed(order, escortMention);
  }

  private MessageEmbed buildCustomerCompletedEmbed(EscortDispatchOrder order) {
    return DispatchPanelMessageFactory.buildCustomerCompletedEmbed(order);
  }

  private MessageEmbed buildEscortOrderCompletedEmbed(EscortDispatchOrder order) {
    return DispatchPanelMessageFactory.buildEscortOrderCompletedEmbed(order);
  }

  private MessageEmbed buildCustomerAfterSalesRequestedEmbed(
      EscortDispatchOrder order, String statusText) {
    return DispatchPanelMessageFactory.buildCustomerAfterSalesRequestedEmbed(order, statusText);
  }

  private MessageEmbed buildAfterSalesNotificationEmbed(EscortDispatchOrder order) {
    return DispatchPanelMessageFactory.buildAfterSalesNotificationEmbed(order);
  }

  private MessageEmbed buildAfterSalesClaimedEmbed(
      EscortDispatchOrder order, String assigneeMention) {
    return DispatchPanelMessageFactory.buildAfterSalesClaimedEmbed(order, assigneeMention);
  }

  private MessageEmbed buildAfterSalesClosedEmbed(EscortDispatchOrder order) {
    return DispatchPanelMessageFactory.buildAfterSalesClosedEmbed(order);
  }

  private MessageEmbed buildDispatchEmbed(
      String title, Color color, String description, List<EmbedView.FieldView> fields) {
    return buildDispatchEmbed(title, color, description, fields, null);
  }

  private MessageEmbed buildDispatchEmbed(
      String title,
      Color color,
      String description,
      List<EmbedView.FieldView> fields,
      String footer) {
    return DiscordComponentRenderer.buildEmbed(
        new EmbedView(title, description, color, fields, footer));
  }

  private String extractOrderNumber(String buttonId, String prefix) {
    return buttonId.substring(prefix.length()).trim().toUpperCase();
  }

  private String getSessionKey(long userId, long guildId) {
    return guildId + ":" + userId;
  }

  private String formatUserMention(long userId) {
    return userId <= 0 ? "待指定" : "<@" + userId + ">";
  }

  private boolean isAdmin(Member member, Guild guild) {
    if (member == null || guild == null) {
      return false;
    }
    if (member.hasPermission(Permission.ADMINISTRATOR)) {
      return true;
    }
    try {
      return guild.getOwnerIdLong() == member.getIdLong();
    } catch (Exception ignored) {
      return false;
    }
  }

  private record AfterSalesNotifyResult(String message) {}

  private record PendingAssignmentLoadResult(
      List<EscortDispatchOrder> orders, String statusMessage) {}

  private static final class SessionState {
    private String mode;

    private Long escortUserId;
    private String escortUserMention;

    private Long customerUserId;
    private String customerUserMention;

    private String escortOptionCode;
    private String selectedOrderNumber;
    private List<EscortDispatchOrder> pendingOrders = List.of();
    private String statusMessage;

    void resetForMode(String mode) {
      this.mode = mode;
      this.escortUserId = null;
      this.escortUserMention = null;
      this.customerUserId = null;
      this.customerUserMention = null;
      this.escortOptionCode = null;
      this.selectedOrderNumber = null;
      this.pendingOrders = List.of();
      this.statusMessage = null;
    }

    boolean isCreateMode() {
      return DispatchPanelView.MODE_CREATE.equals(mode);
    }

    boolean isAssignMode() {
      return DispatchPanelView.MODE_ASSIGN.equals(mode);
    }

    boolean canCreateOpenOrder() {
      return customerUserId != null && escortOptionCode != null && !escortOptionCode.isBlank();
    }

    boolean canAssignOrder() {
      return selectedOrderNumber != null
          && !selectedOrderNumber.isBlank()
          && escortUserId != null
          && pendingOrders.stream()
              .anyMatch(order -> order.orderNumber().equals(selectedOrderNumber));
    }

    String getCreateStatusMessage() {
      if (canCreateOpenOrder()) {
        return "✅ 已完成選擇，可建立護航訂單";
      }
      return statusMessage;
    }

    String getAssignStatusMessage() {
      if (canAssignOrder()) {
        return "✅ 已完成選擇，可派發護航訂單";
      }
      return statusMessage;
    }
  }
}
