package ltdjms.discord.panel.commands;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.discord.domain.EmbedView;
import ltdjms.discord.panel.components.PanelComponentRenderer;
import ltdjms.discord.panel.services.AdminPanelSessionManager;
import ltdjms.discord.panel.services.AdminPanelSessionManager.AdminPanelSessionContext;
import ltdjms.discord.panel.services.AdminPanelSessionManager.AdminPanelView;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.product.services.ProductService;
import ltdjms.discord.redemption.domain.RedemptionCode;
import ltdjms.discord.redemption.domain.RedemptionCodeRepository;
import ltdjms.discord.redemption.services.RedemptionService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.Unit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

/**
 * Handles button, select menu, and modal interactions for product and redemption code management.
 */
public class AdminProductPanelHandler extends ListenerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(AdminProductPanelHandler.class);

  private static final Color EMBED_COLOR = new Color(0xED4245);
  private static final int PAGE_SIZE = 10;

  // Button IDs
  public static final String BUTTON_PRODUCTS = "admin_panel_products";
  public static final String BUTTON_CREATE_PRODUCT = "admin_create_product";
  public static final String BUTTON_GENERATE_CODES = "admin_generate_codes";
  public static final String BUTTON_VIEW_CODES = "admin_view_codes";
  public static final String BUTTON_PRODUCT_BACK = "admin_product_back";
  public static final String BUTTON_PREFIX_EDIT_PRODUCT = "admin_edit_product_";
  public static final String BUTTON_PREFIX_DELETE_PRODUCT = "admin_delete_product_";
  public static final String BUTTON_PREFIX_SET_FIAT_VALUE = "admin_set_fiat_value_";
  public static final String BUTTON_PREFIX_INTEGRATION_CONFIG = "admin_integration_config_";
  public static final String BUTTON_PREFIX_CODE_PAGE = "admin_code_page_";
  public static final String BUTTON_CODE_BACK = "admin_code_back";
  public static final String BUTTON_INTEGRATION_PANEL_CONFIRM = "admin_integration_panel_confirm";
  public static final String BUTTON_INTEGRATION_PANEL_CLOSE = "admin_integration_panel_close";

  // Select Menu IDs
  public static final String SELECT_PRODUCT = "admin_select_product";
  public static final String SELECT_INTEGRATION_PANEL_AUTO_ESCORT =
      "admin_select_integration_panel_auto_escort";
  public static final String SELECT_INTEGRATION_PANEL_ESCORT_OPTION =
      "admin_select_integration_panel_escort_option";
  public static final String SELECT_INTEGRATION_PANEL_ESCORT_OPTION_EXTRA =
      "admin_select_integration_panel_escort_option_extra";

  // Modal IDs
  public static final String MODAL_CREATE_PRODUCT = "admin_modal_create_product";
  public static final String MODAL_EDIT_PRODUCT = "admin_modal_edit_product_";
  public static final String MODAL_SET_FIAT_VALUE = "admin_modal_set_fiat_value_";
  public static final String MODAL_INTEGRATION_CONFIG = "admin_modal_integration_config_";
  public static final String MODAL_GENERATE_CODES = "admin_modal_generate_codes_";

  private final ProductService productService;
  private final RedemptionService redemptionService;
  private final AdminPanelSessionManager adminPanelSessionManager;

  private final Map<String, IntegrationConfigSessionState> integrationConfigSessions =
      new ConcurrentHashMap<>();

  public AdminProductPanelHandler(
      ProductService productService,
      RedemptionService redemptionService,
      AdminPanelSessionManager adminPanelSessionManager) {
    this.productService = productService;
    this.redemptionService = redemptionService;
    this.adminPanelSessionManager = adminPanelSessionManager;
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    String buttonId = event.getComponentId();

    if (!isProductPanelButton(buttonId)) {
      return;
    }

    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用管理面板").setEphemeral(true).queue();
      return;
    }

    LOG.debug(
        "Processing product panel button: buttonId={}, userId={}",
        buttonId,
        event.getUser().getIdLong());

    try {
      if (buttonId.equals(BUTTON_PRODUCTS)) {
        showProductList(event);
      } else if (buttonId.equals(BUTTON_CREATE_PRODUCT)) {
        openCreateProductModal(event);
      } else if (buttonId.equals(BUTTON_GENERATE_CODES)) {
        openGenerateCodesModal(event);
      } else if (buttonId.equals(BUTTON_VIEW_CODES)) {
        showCodeList(event, 1);
      } else if (buttonId.equals(BUTTON_PRODUCT_BACK)) {
        showProductList(event);
      } else if (buttonId.equals(BUTTON_CODE_BACK)) {
        showProductDetail(event);
      } else if (buttonId.equals(BUTTON_INTEGRATION_PANEL_CONFIRM)) {
        handleIntegrationPanelConfirm(event);
      } else if (buttonId.equals(BUTTON_INTEGRATION_PANEL_CLOSE)) {
        handleIntegrationPanelClose(event);
      } else if (buttonId.startsWith(BUTTON_PREFIX_EDIT_PRODUCT)) {
        String productIdStr = buttonId.substring(BUTTON_PREFIX_EDIT_PRODUCT.length());
        openEditProductModal(event, Long.parseLong(productIdStr));
      } else if (buttonId.startsWith(BUTTON_PREFIX_DELETE_PRODUCT)) {
        String productIdStr = buttonId.substring(BUTTON_PREFIX_DELETE_PRODUCT.length());
        handleDeleteProduct(event, Long.parseLong(productIdStr));
      } else if (buttonId.startsWith(BUTTON_PREFIX_SET_FIAT_VALUE)) {
        String productIdStr = buttonId.substring(BUTTON_PREFIX_SET_FIAT_VALUE.length());
        openSetFiatValueModal(event, Long.parseLong(productIdStr));
      } else if (buttonId.startsWith(BUTTON_PREFIX_INTEGRATION_CONFIG)) {
        String productIdStr = buttonId.substring(BUTTON_PREFIX_INTEGRATION_CONFIG.length());
        openIntegrationConfigPanel(event, Long.parseLong(productIdStr));
      } else if (buttonId.startsWith(BUTTON_PREFIX_CODE_PAGE)) {
        String pageStr = buttonId.substring(BUTTON_PREFIX_CODE_PAGE.length());
        showCodeList(event, Integer.parseInt(pageStr));
      }
    } catch (Exception e) {
      LOG.error("Error handling product panel button: {}", buttonId, e);
      event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
    }
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
    String selectId = event.getComponentId();

    if (!selectId.equals(SELECT_PRODUCT)
        && !selectId.equals(SELECT_INTEGRATION_PANEL_AUTO_ESCORT)
        && !selectId.equals(SELECT_INTEGRATION_PANEL_ESCORT_OPTION)
        && !selectId.equals(SELECT_INTEGRATION_PANEL_ESCORT_OPTION_EXTRA)) {
      return;
    }

    if (!event.isFromGuild() || event.getGuild() == null) {
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用管理面板").setEphemeral(true).queue();
      return;
    }

    try {
      if (selectId.equals(SELECT_PRODUCT)) {
        handleProductSelect(event);
      } else if (selectId.equals(SELECT_INTEGRATION_PANEL_AUTO_ESCORT)) {
        handleIntegrationPanelAutoEscortSelect(event);
      } else if (selectId.equals(SELECT_INTEGRATION_PANEL_ESCORT_OPTION)
          || selectId.equals(SELECT_INTEGRATION_PANEL_ESCORT_OPTION_EXTRA)) {
        handleIntegrationPanelEscortOptionSelect(event);
      }
    } catch (Exception e) {
      LOG.error("Error handling string select: {}", selectId, e);
      event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
    }
  }

  @Override
  public void onModalInteraction(ModalInteractionEvent event) {
    String modalId = event.getModalId();

    if (!modalId.startsWith("admin_modal_")) {
      return;
    }

    if (!isProductPanelModal(modalId)) {
      return;
    }

    if (!event.isFromGuild() || event.getGuild() == null) {
      event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
      return;
    }

    if (!isAdmin(event.getMember(), event.getGuild())) {
      event.reply("你沒有權限使用管理面板").setEphemeral(true).queue();
      return;
    }

    try {
      if (modalId.equals(MODAL_CREATE_PRODUCT)) {
        handleCreateProductModal(event);
      } else if (modalId.startsWith(MODAL_EDIT_PRODUCT)) {
        handleEditProductModal(event);
      } else if (modalId.startsWith(MODAL_SET_FIAT_VALUE)) {
        handleSetFiatValueModal(event);
      } else if (modalId.startsWith(MODAL_INTEGRATION_CONFIG)) {
        handleIntegrationConfigModal(event);
      } else if (modalId.startsWith(MODAL_GENERATE_CODES)) {
        handleGenerateCodesModal(event);
      }
    } catch (Exception e) {
      LOG.error("Error handling modal: {}", modalId, e);
      event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
    }
  }

  private boolean isProductPanelButton(String buttonId) {
    return buttonId.equals(BUTTON_PRODUCTS)
        || buttonId.equals(BUTTON_CREATE_PRODUCT)
        || buttonId.equals(BUTTON_GENERATE_CODES)
        || buttonId.equals(BUTTON_VIEW_CODES)
        || buttonId.equals(BUTTON_PRODUCT_BACK)
        || buttonId.equals(BUTTON_CODE_BACK)
        || buttonId.equals(BUTTON_INTEGRATION_PANEL_CONFIRM)
        || buttonId.equals(BUTTON_INTEGRATION_PANEL_CLOSE)
        || buttonId.startsWith(BUTTON_PREFIX_EDIT_PRODUCT)
        || buttonId.startsWith(BUTTON_PREFIX_DELETE_PRODUCT)
        || buttonId.startsWith(BUTTON_PREFIX_SET_FIAT_VALUE)
        || buttonId.startsWith(BUTTON_PREFIX_INTEGRATION_CONFIG)
        || buttonId.startsWith(BUTTON_PREFIX_CODE_PAGE);
  }

  private boolean isProductPanelModal(String modalId) {
    return modalId.equals(MODAL_CREATE_PRODUCT)
        || modalId.startsWith(MODAL_EDIT_PRODUCT)
        || modalId.startsWith(MODAL_SET_FIAT_VALUE)
        || modalId.startsWith(MODAL_INTEGRATION_CONFIG)
        || modalId.startsWith(MODAL_GENERATE_CODES);
  }

  // ===== Product List =====

  private void showProductList(ButtonInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    long adminId = event.getUser().getIdLong();
    String sessionKey = getSessionKey(adminId, guildId);
    adminPanelSessionManager.updateSessionView(
        guildId, adminId, AdminPanelView.PRODUCT_LIST, Map.of());
    integrationConfigSessions.remove(sessionKey);

    List<Product> products = productService.getProducts(guildId);

    MessageEmbed embed = buildProductListEmbed(products);
    event.editMessageEmbeds(embed).setComponents(buildProductListComponents(products)).queue();
  }

  private MessageEmbed buildProductListEmbed(List<Product> products) {
    return AdminProductPanelViewFactory.buildProductListEmbed(products);
  }

  // ===== Product Selection and Detail =====

  private void handleProductSelect(StringSelectInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    long productId = Long.parseLong(event.getValues().get(0));
    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product -> {
              adminPanelSessionManager.updateSessionView(
                  guildId,
                  event.getUser().getIdLong(),
                  AdminPanelView.PRODUCT_DETAIL,
                  Map.of("productId", productId, "page", 1));
              showProductDetailEmbed(event, product);
            },
            () -> event.reply("找不到該商品").setEphemeral(true).queue());
  }

  private void showProductDetail(ButtonInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    long adminId = event.getUser().getIdLong();
    AdminPanelSessionContext session =
        adminPanelSessionManager.getSessionContext(guildId, adminId).orElse(null);
    if (session == null
        || (session.currentView() != AdminPanelView.PRODUCT_DETAIL
            && session.currentView() != AdminPanelView.PRODUCT_CODE_LIST)) {
      showProductList(event);
      return;
    }

    long productId = getLongMetadata(session, "productId", -1L);
    if (productId < 0) {
      showProductList(event);
      return;
    }
    adminPanelSessionManager.updateSessionView(
        guildId, adminId, AdminPanelView.PRODUCT_DETAIL, Map.of("productId", productId, "page", 1));

    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product -> {
              MessageEmbed embed = buildProductDetailEmbed(product);
              RedemptionCodeRepository.CodeStats stats =
                  redemptionService.getCodeStats(product.id());

              event
                  .editMessageEmbeds(embed)
                  .setComponents(buildProductDetailComponents(product, stats))
                  .queue();
            },
            () -> {
              adminPanelSessionManager.updateSessionView(
                  guildId, adminId, AdminPanelView.PRODUCT_LIST, Map.of());
              showProductList(event);
            });
  }

  private void showProductDetailEmbed(StringSelectInteractionEvent event, Product product) {
    adminPanelSessionManager.updateSessionView(
        event.getGuild().getIdLong(),
        event.getUser().getIdLong(),
        AdminPanelView.PRODUCT_DETAIL,
        Map.of("productId", product.id(), "page", 1));

    MessageEmbed embed = buildProductDetailEmbed(product);
    RedemptionCodeRepository.CodeStats stats = redemptionService.getCodeStats(product.id());

    event
        .editMessageEmbeds(embed)
        .setComponents(buildProductDetailComponents(product, stats))
        .queue();
  }

  private MessageEmbed buildProductDetailEmbed(Product product) {
    RedemptionCodeRepository.CodeStats stats = redemptionService.getCodeStats(product.id());
    return AdminProductPanelViewFactory.buildProductDetailEmbed(product, stats);
  }

  private List<ActionRow> buildProductDetailComponents(
      Product product, RedemptionCodeRepository.CodeStats stats) {
    return AdminProductPanelViewFactory.buildProductDetailComponents(product, stats);
  }

  // ===== Create Product =====

  private void openCreateProductModal(ButtonInteractionEvent event) {
    event.replyModal(AdminProductPanelModalFactory.createCreateProductModal()).queue();
  }

  private void handleCreateProductModal(ModalInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();

    String name = event.getValue("name").getAsString().trim();
    String description = null;
    String rewardTypeStr = getModalValueOrNull(event, "reward_type");
    String rewardAmountStr = getModalValueOrNull(event, "reward_amount");
    String currencyPriceStr = getModalValueOrNull(event, "currency_price");
    String fiatPriceTwdStr = getModalValueOrNull(event, "fiat_price_twd");

    // Parse reward type and amount
    Product.RewardType rewardType = null;
    Long rewardAmount = null;

    if (rewardTypeStr != null && !rewardTypeStr.isBlank()) {
      try {
        rewardType = Product.RewardType.valueOf(rewardTypeStr.toUpperCase());
      } catch (IllegalArgumentException e) {
        event.reply("獎勵類型無效，請輸入 CURRENCY 或 TOKEN").setEphemeral(true).queue();
        return;
      }
    }

    if (rewardAmountStr != null && !rewardAmountStr.isBlank()) {
      try {
        rewardAmount = Long.parseLong(rewardAmountStr);
      } catch (NumberFormatException e) {
        event.reply("獎勵數量格式錯誤，請輸入有效數字").setEphemeral(true).queue();
        return;
      }
    }

    // Parse currency price
    Long currencyPrice = null;
    if (currencyPriceStr != null && !currencyPriceStr.isBlank()) {
      try {
        currencyPrice = Long.parseLong(currencyPriceStr);
      } catch (NumberFormatException e) {
        event.reply("貨幣價格格式錯誤，請輸入有效數字").setEphemeral(true).queue();
        return;
      }
    }

    Long fiatPriceTwd = null;
    if (fiatPriceTwdStr != null && !fiatPriceTwdStr.isBlank()) {
      try {
        fiatPriceTwd = Long.parseLong(fiatPriceTwdStr);
      } catch (NumberFormatException e) {
        event.reply("實際價值（TWD）格式錯誤，請輸入有效數字").setEphemeral(true).queue();
        return;
      }
    }

    Result<Product, DomainError> result =
        productService.createProduct(
            guildId, name, description, rewardType, rewardAmount, currencyPrice, fiatPriceTwd);

    if (result.isErr()) {
      event.reply("建立失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    Product product = result.getValue();

    long adminId = event.getUser().getIdLong();
    adminPanelSessionManager.updatePanel(
        guildId, adminId, hook -> refreshProductListView(hook, guildId, adminId));

    event.reply(String.format("✅ 商品「%s」建立成功！", product.name())).setEphemeral(true).queue();
  }

  // ===== Edit Product =====

  private void openEditProductModal(ButtonInteractionEvent event, long productId) {
    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product ->
                event
                    .replyModal(
                        AdminProductPanelModalFactory.createEditProductModal(productId, product))
                    .queue(),
            () -> event.reply("找不到該商品").setEphemeral(true).queue());
  }

  private void handleEditProductModal(ModalInteractionEvent event) {
    String modalId = event.getModalId();
    long productId = Long.parseLong(modalId.substring(MODAL_EDIT_PRODUCT.length()));

    String name = event.getValue("name").getAsString().trim();
    String description = getModalValueOrNull(event, "description");
    String rewardTypeStr = getModalValueOrNull(event, "reward_type");
    String rewardAmountStr = getModalValueOrNull(event, "reward_amount");
    String currencyPriceStr = getModalValueOrNull(event, "currency_price");

    // Parse reward type and amount
    Product.RewardType rewardType = null;
    Long rewardAmount = null;

    if (rewardTypeStr != null && !rewardTypeStr.isBlank()) {
      try {
        rewardType = Product.RewardType.valueOf(rewardTypeStr.toUpperCase());
      } catch (IllegalArgumentException e) {
        event.reply("獎勵類型無效，請輸入 CURRENCY 或 TOKEN").setEphemeral(true).queue();
        return;
      }
    }

    if (rewardAmountStr != null && !rewardAmountStr.isBlank()) {
      try {
        rewardAmount = Long.parseLong(rewardAmountStr);
      } catch (NumberFormatException e) {
        event.reply("獎勵數量格式錯誤，請輸入有效數字").setEphemeral(true).queue();
        return;
      }
    }

    // Parse currency price
    Long currencyPrice = null;
    if (currencyPriceStr != null && !currencyPriceStr.isBlank()) {
      try {
        currencyPrice = Long.parseLong(currencyPriceStr);
      } catch (NumberFormatException e) {
        event.reply("貨幣價格格式錯誤，請輸入有效數字").setEphemeral(true).queue();
        return;
      }
    }
    Product existing = productService.getProduct(productId).orElse(null);
    if (existing == null) {
      event.reply("找不到該商品").setEphemeral(true).queue();
      return;
    }

    Result<Product, DomainError> result =
        productService.updateProduct(
            productId,
            name,
            description,
            rewardType,
            rewardAmount,
            currencyPrice,
            existing.fiatPriceTwd());

    if (result.isErr()) {
      event.reply("更新失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    event.reply("✅ 商品更新成功！").setEphemeral(true).queue();
  }

  private void openSetFiatValueModal(ButtonInteractionEvent event, long productId) {
    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product ->
                event
                    .replyModal(
                        AdminProductPanelModalFactory.createSetFiatValueModal(productId, product))
                    .queue(),
            () -> event.reply("找不到該商品").setEphemeral(true).queue());
  }

  private void handleSetFiatValueModal(ModalInteractionEvent event) {
    String modalId = event.getModalId();
    long productId = Long.parseLong(modalId.substring(MODAL_SET_FIAT_VALUE.length()));
    Product existing = productService.getProduct(productId).orElse(null);
    if (existing == null) {
      event.reply("找不到該商品").setEphemeral(true).queue();
      return;
    }

    String fiatPriceTwdStr = getModalValueOrNull(event, "fiat_price_twd");
    Long fiatPriceTwd = null;
    if (fiatPriceTwdStr != null && !fiatPriceTwdStr.isBlank()) {
      try {
        fiatPriceTwd = Long.parseLong(fiatPriceTwdStr);
      } catch (NumberFormatException e) {
        event.reply("實際價值（TWD）格式錯誤，請輸入有效數字").setEphemeral(true).queue();
        return;
      }
    }

    Result<Product, DomainError> result =
        productService.updateProduct(
            productId,
            existing.name(),
            existing.description(),
            existing.rewardType(),
            existing.rewardAmount(),
            existing.currencyPrice(),
            fiatPriceTwd);
    if (result.isErr()) {
      event.reply("更新失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }
    if (fiatPriceTwd == null) {
      event.reply("✅ 已清除實際價值（TWD）").setEphemeral(true).queue();
      return;
    }
    event.reply("✅ 已更新實際價值（TWD）").setEphemeral(true).queue();
  }

  private void openIntegrationConfigPanel(ButtonInteractionEvent event, long productId) {
    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product -> {
              long guildId = event.getGuild().getIdLong();
              String sessionKey = getSessionKey(event.getUser().getIdLong(), guildId);
              IntegrationConfigSessionState state =
                  new IntegrationConfigSessionState(
                      product.id(),
                      product.name(),
                      product.autoCreateEscortOrder(),
                      product.escortOptionCode(),
                      null);
              integrationConfigSessions.put(sessionKey, state);

              event
                  .replyEmbeds(buildIntegrationConfigPanelEmbed(state))
                  .setComponents(buildIntegrationConfigPanelComponents(state))
                  .setEphemeral(true)
                  .queue(
                      hook -> {
                        state.panelHook = hook;
                        integrationConfigSessions.put(sessionKey, state);
                      });
            },
            () -> event.reply("找不到該商品").setEphemeral(true).queue());
  }

  private void handleIntegrationPanelAutoEscortSelect(StringSelectInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    String sessionKey = getSessionKey(event.getUser().getIdLong(), guildId);
    IntegrationConfigSessionState state = integrationConfigSessions.get(sessionKey);
    if (state == null) {
      event.reply("設定面板已過期，請重新開啟").setEphemeral(true).queue();
      return;
    }

    state.autoCreateEscortOrder = Boolean.parseBoolean(event.getValues().get(0));
    if (!state.autoCreateEscortOrder) {
      state.escortOptionCode = null;
    }
    state.statusMessage = "✅ 已更新自動護航開單設定（尚未送出）";

    event
        .editMessageEmbeds(buildIntegrationConfigPanelEmbed(state))
        .setComponents(buildIntegrationConfigPanelComponents(state))
        .queue();
  }

  private void handleIntegrationPanelEscortOptionSelect(StringSelectInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    String sessionKey = getSessionKey(event.getUser().getIdLong(), guildId);
    IntegrationConfigSessionState state = integrationConfigSessions.get(sessionKey);
    if (state == null) {
      event.reply("設定面板已過期，請重新開啟").setEphemeral(true).queue();
      return;
    }

    String selected = event.getValues().get(0);
    state.escortOptionCode = "__none__".equals(selected) ? null : selected;
    state.statusMessage = "✅ 已更新護航選項（尚未送出）";

    event
        .editMessageEmbeds(buildIntegrationConfigPanelEmbed(state))
        .setComponents(buildIntegrationConfigPanelComponents(state))
        .queue();
  }

  private void handleIntegrationPanelConfirm(ButtonInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    String sessionKey = getSessionKey(event.getUser().getIdLong(), guildId);
    IntegrationConfigSessionState state = integrationConfigSessions.get(sessionKey);
    if (state == null) {
      event.reply("設定面板已過期，請重新開啟").setEphemeral(true).queue();
      return;
    }
    if (!canSubmitIntegrationConfig(state)) {
      state.statusMessage = "❌ 請先完成必要欄位（後端 URL、護航選項）";
      event
          .editMessageEmbeds(buildIntegrationConfigPanelEmbed(state))
          .setComponents(buildIntegrationConfigPanelComponents(state))
          .queue();
      return;
    }

    Product existing = productService.getProduct(state.productId).orElse(null);
    if (existing == null) {
      state.statusMessage = "❌ 找不到商品，請返回商品列表後重試";
      event
          .editMessageEmbeds(buildIntegrationConfigPanelEmbed(state))
          .setComponents(buildIntegrationConfigPanelComponents(state))
          .queue();
      return;
    }

    String finalEscortOptionCode = state.autoCreateEscortOrder ? state.escortOptionCode : null;
    Result<Product, DomainError> result =
        productService.updateProduct(
            state.productId,
            existing.name(),
            existing.description(),
            existing.rewardType(),
            existing.rewardAmount(),
            existing.currencyPrice(),
            existing.fiatPriceTwd(),
            state.autoCreateEscortOrder,
            finalEscortOptionCode);
    if (result.isErr()) {
      state.statusMessage = "❌ 更新失敗：" + result.getError().message();
      event
          .editMessageEmbeds(buildIntegrationConfigPanelEmbed(state))
          .setComponents(buildIntegrationConfigPanelComponents(state))
          .queue();
      return;
    }

    Product updated = result.getValue();
    state.autoCreateEscortOrder = updated.autoCreateEscortOrder();
    state.escortOptionCode = updated.escortOptionCode();
    String escortStatus =
        updated.autoCreateEscortOrder() ? "已啟用（" + updated.escortOptionCode() + "）" : "未啟用";
    state.statusMessage = String.format("✅ 接入設定已更新：自動護航開單 %s", escortStatus);

    refreshProductPanels(guildId);
    event
        .editMessageEmbeds(buildIntegrationConfigPanelEmbed(state))
        .setComponents(buildIntegrationConfigPanelComponents(state))
        .queue();
  }

  private void handleIntegrationPanelClose(ButtonInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    String sessionKey = getSessionKey(event.getUser().getIdLong(), guildId);
    integrationConfigSessions.remove(sessionKey);

    MessageEmbed closedEmbed =
        PanelComponentRenderer.buildEmbed(
            new EmbedView("🔗 接入設定面板", "已關閉設定面板", EMBED_COLOR, List.of(), null));
    event.editMessageEmbeds(closedEmbed).setComponents(List.of()).queue();
  }

  private MessageEmbed buildIntegrationConfigPanelEmbed(IntegrationConfigSessionState state) {
    return AdminProductPanelViewFactory.buildIntegrationConfigPanelEmbed(state);
  }

  private List<ActionRow> buildIntegrationConfigPanelComponents(
      IntegrationConfigSessionState state) {
    return AdminProductPanelViewFactory.buildIntegrationConfigPanelComponents(state);
  }

  private boolean canSubmitIntegrationConfig(IntegrationConfigSessionState state) {
    return AdminProductPanelViewFactory.canSubmitIntegrationConfig(state);
  }

  private void openIntegrationConfigModal(ButtonInteractionEvent event, long productId) {
    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product -> {
              TextInput autoEscortInput =
                  TextInput.create("auto_create_escort_order", "自動護航開單", TextInputStyle.SHORT)
                      .setPlaceholder("true / false")
                      .setRequired(false)
                      .setMaxLength(10)
                      .setValue(Boolean.toString(product.autoCreateEscortOrder()))
                      .build();

              TextInput escortOptionInput =
                  TextInput.create("escort_option_code", "護航選項代碼", TextInputStyle.SHORT)
                      .setPlaceholder("例如：CONF_DAM_300W")
                      .setRequired(false)
                      .setMaxLength(120)
                      .build();
              if (product.escortOptionCode() != null && !product.escortOptionCode().isBlank()) {
                escortOptionInput =
                    TextInput.create("escort_option_code", "護航選項代碼", TextInputStyle.SHORT)
                        .setRequired(false)
                        .setMaxLength(120)
                        .setValue(product.escortOptionCode())
                        .build();
              }

              Modal modal =
                  Modal.create(MODAL_INTEGRATION_CONFIG + productId, "接入設定")
                      .addComponents(ActionRow.of(autoEscortInput), ActionRow.of(escortOptionInput))
                      .build();
              event.replyModal(modal).queue();
            },
            () -> event.reply("找不到該商品").setEphemeral(true).queue());
  }

  private void handleIntegrationConfigModal(ModalInteractionEvent event) {
    String modalId = event.getModalId();
    long productId = Long.parseLong(modalId.substring(MODAL_INTEGRATION_CONFIG.length()));

    Product existing = productService.getProduct(productId).orElse(null);
    if (existing == null) {
      event.reply("找不到該商品").setEphemeral(true).queue();
      return;
    }

    String autoEscortRaw = getModalValueOrNull(event, "auto_create_escort_order");
    String escortOptionCode = getModalValueOrNull(event, "escort_option_code");

    Result<Boolean, DomainError> autoEscortResult = parseBooleanInput(autoEscortRaw, false);
    if (autoEscortResult.isErr()) {
      event.reply("更新失敗：" + autoEscortResult.getError().message()).setEphemeral(true).queue();
      return;
    }
    boolean autoCreateEscortOrder = autoEscortResult.getValue();

    Result<Product, DomainError> result =
        productService.updateProduct(
            productId,
            existing.name(),
            existing.description(),
            existing.rewardType(),
            existing.rewardAmount(),
            existing.currencyPrice(),
            existing.fiatPriceTwd(),
            autoCreateEscortOrder,
            escortOptionCode);
    if (result.isErr()) {
      event.reply("更新失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    Product updated = result.getValue();
    String escortStatus =
        updated.autoCreateEscortOrder() ? "已啟用（" + updated.escortOptionCode() + "）" : "未啟用";
    event.reply(String.format("✅ 接入設定已更新\n自動護航開單：%s", escortStatus)).setEphemeral(true).queue();
  }

  // ===== Delete Product =====

  private void handleDeleteProduct(ButtonInteractionEvent event, long productId) {
    Result<Unit, DomainError> result = productService.deleteProduct(productId);

    if (result.isErr()) {
      event.reply("刪除失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    // Clear session and show product list
    long guildId = event.getGuild().getIdLong();
    adminPanelSessionManager.updateSessionView(
        guildId, event.getUser().getIdLong(), AdminPanelView.PRODUCT_LIST, Map.of());

    event.reply("✅ 商品已刪除").setEphemeral(true).queue();
  }

  // ===== Generate Codes =====

  private void openGenerateCodesModal(ButtonInteractionEvent event) {
    long guildId = event.getGuild().getIdLong();
    AdminPanelSessionContext session =
        adminPanelSessionManager
            .getSessionContext(guildId, event.getUser().getIdLong())
            .orElse(null);
    if (session == null
        || (session.currentView() != AdminPanelView.PRODUCT_DETAIL
            && session.currentView() != AdminPanelView.PRODUCT_CODE_LIST)) {
      event.reply("請先選擇商品").setEphemeral(true).queue();
      return;
    }

    long productId = getLongMetadata(session, "productId", -1L);
    if (productId < 0) {
      event.reply("請先選擇商品").setEphemeral(true).queue();
      return;
    }

    event.replyModal(AdminProductPanelModalFactory.createGenerateCodesModal(productId)).queue();
  }

  private void handleGenerateCodesModal(ModalInteractionEvent event) {
    String modalId = event.getModalId();
    long productId = Long.parseLong(modalId.substring(MODAL_GENERATE_CODES.length()));

    String countStr = event.getValue("count").getAsString().trim();
    String quantityStr = event.getValue("quantity").getAsString().trim();
    String expiresStr = getModalValueOrNull(event, "expires");

    int count;
    try {
      count = Integer.parseInt(countStr);
    } catch (NumberFormatException e) {
      event.reply("數量格式錯誤，請輸入有效數字").setEphemeral(true).queue();
      return;
    }

    int quantity;
    try {
      quantity = Integer.parseInt(quantityStr);
    } catch (NumberFormatException e) {
      event.reply("兌換數量格式錯誤，請輸入有效數字").setEphemeral(true).queue();
      return;
    }

    Instant expiresAt = null;
    if (expiresStr != null && !expiresStr.isBlank()) {
      try {
        LocalDate date = LocalDate.parse(expiresStr, DateTimeFormatter.ISO_LOCAL_DATE);
        expiresAt = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
      } catch (DateTimeParseException e) {
        event.reply("日期格式錯誤，請使用 YYYY-MM-DD 格式").setEphemeral(true).queue();
        return;
      }
    }

    Result<List<RedemptionCode>, DomainError> result =
        redemptionService.generateCodes(productId, count, expiresAt, quantity);

    if (result.isErr()) {
      event.reply("生成失敗：" + result.getError().message()).setEphemeral(true).queue();
      return;
    }

    List<RedemptionCode> codes = result.getValue();

    // Build response with generated codes
    StringBuilder sb = new StringBuilder();
    sb.append("✅ 成功生成 ").append(codes.size()).append(" 個兌換碼：\n\n```\n");
    for (RedemptionCode code : codes) {
      sb.append(code.code()).append("\n");
    }
    sb.append("```");

    if (quantity > 1) {
      sb.append("\n每個碼可兌換數量：").append(quantity);
    }

    if (expiresAt != null) {
      sb.append("\n到期日期：").append(expiresStr);
    }

    // Discord message length limit
    String response = sb.toString();
    if (response.length() > 2000) {
      response = String.format("✅ 成功生成 %d 個兌換碼！\n\n由於數量較多，請至兌換碼列表查看。", codes.size());
    }

    event.reply(response).setEphemeral(true).queue();
  }

  // ===== View Codes =====

  private void showCodeList(ButtonInteractionEvent event, int page) {
    long guildId = event.getGuild().getIdLong();
    long adminId = event.getUser().getIdLong();
    AdminPanelSessionContext session =
        adminPanelSessionManager.getSessionContext(guildId, adminId).orElse(null);
    if (session == null
        || (session.currentView() != AdminPanelView.PRODUCT_DETAIL
            && session.currentView() != AdminPanelView.PRODUCT_CODE_LIST)) {
      showProductList(event);
      return;
    }

    long productId = getLongMetadata(session, "productId", -1L);
    if (productId < 0) {
      showProductList(event);
      return;
    }
    adminPanelSessionManager.updateSessionView(
        guildId,
        adminId,
        AdminPanelView.PRODUCT_CODE_LIST,
        Map.of("productId", productId, "page", page));

    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product -> {
              RedemptionService.CodePage codePage =
                  redemptionService.getCodePage(productId, page, PAGE_SIZE);

              MessageEmbed embed = buildCodeListEmbed(product, codePage);
              List<ActionRow> components = buildCodeListComponents(codePage);

              event.editMessageEmbeds(embed).setComponents(components).queue();
            },
            () -> {
              adminPanelSessionManager.updateSessionView(
                  guildId, adminId, AdminPanelView.PRODUCT_LIST, Map.of());
              showProductList(event);
            });
  }

  private MessageEmbed buildCodeListEmbed(Product product, RedemptionService.CodePage codePage) {
    return AdminProductPanelViewFactory.buildCodeListEmbed(product, codePage);
  }

  private List<ActionRow> buildCodeListComponents(RedemptionService.CodePage codePage) {
    return AdminProductPanelViewFactory.buildCodeListComponents(codePage);
  }

  // ===== Helpers =====

  private String getSessionKey(long userId, long guildId) {
    return userId + "_" + guildId;
  }

  private long getLongMetadata(AdminPanelSessionContext context, String key, long defaultValue) {
    Object value = context.metadata().get(key);
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String stringValue) {
      try {
        return Long.parseLong(stringValue);
      } catch (NumberFormatException ignored) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private int getIntMetadata(AdminPanelSessionContext context, String key, int defaultValue) {
    Object value = context.metadata().get(key);
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String stringValue) {
      try {
        return Integer.parseInt(stringValue);
      } catch (NumberFormatException ignored) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private String getModalValueOrNull(ModalInteractionEvent event, String id) {
    var mapping = event.getValue(id);
    if (mapping == null) {
      return null;
    }
    String value = mapping.getAsString();
    return value.isBlank() ? null : value.trim();
  }

  private Result<Boolean, DomainError> parseBooleanInput(String raw, boolean defaultValue) {
    if (raw == null || raw.isBlank()) {
      return Result.ok(defaultValue);
    }
    String normalized = raw.trim().toLowerCase();
    return switch (normalized) {
      case "true", "1", "yes", "y", "on" -> Result.ok(true);
      case "false", "0", "no", "n", "off" -> Result.ok(false);
      default ->
          Result.err(DomainError.invalidInput("自動護航開單僅接受 true/false, 1/0, yes/no, y/n, on/off"));
    };
  }

  /** 目前商品面板的檢視狀態。 */
  public enum ProductView {
    DETAIL,
    CODE_LIST
  }

  static class IntegrationConfigSessionState {
    final long productId;
    final String productName;
    boolean autoCreateEscortOrder;
    String escortOptionCode;
    String statusMessage;
    InteractionHook panelHook;

    IntegrationConfigSessionState(
        long productId,
        String productName,
        boolean autoCreateEscortOrder,
        String escortOptionCode,
        String statusMessage) {
      this.productId = productId;
      this.productName = productName;
      this.autoCreateEscortOrder = autoCreateEscortOrder;
      this.escortOptionCode = escortOptionCode;
      this.statusMessage = statusMessage;
    }
  }

  /** 供測試設定 session 狀態，避免繁瑣事件建構。 */
  void setProductSessionForTest(
      long adminId, long guildId, ProductView view, long productId, int page) {
    AdminPanelView panelView =
        view == ProductView.CODE_LIST
            ? AdminPanelView.PRODUCT_CODE_LIST
            : AdminPanelView.PRODUCT_DETAIL;
    adminPanelSessionManager.updateSessionView(
        guildId, adminId, panelView, Map.of("productId", productId, "page", page));
  }

  /** 事件發生後刷新目前已開啟商品面板的管理員畫面。 */
  public void refreshProductPanels(long guildId) {
    adminPanelSessionManager.updatePanelsByGuild(
        guildId,
        context -> {
          try {
            if (context.currentView() == AdminPanelView.PRODUCT_LIST) {
              refreshProductListView(context.hook(), guildId, context.adminId());
              return;
            }
            if (context.currentView() == AdminPanelView.PRODUCT_DETAIL
                || context.currentView() == AdminPanelView.PRODUCT_CODE_LIST) {
              long productId = getLongMetadata(context, "productId", -1L);
              if (productId < 0) {
                adminPanelSessionManager.clearSession(guildId, context.adminId());
                return;
              }
              int page = getIntMetadata(context, "page", 1);
              if (context.currentView() == AdminPanelView.PRODUCT_CODE_LIST) {
                refreshCodeListView(context.hook(), guildId, context.adminId(), productId, page);
              } else {
                refreshProductDetailView(context.hook(), guildId, context.adminId(), productId);
              }
            }
          } catch (Exception e) {
            LOG.warn(
                "Failed to refresh product panel for adminId={}, guildId={}",
                context.adminId(),
                guildId,
                e);
            adminPanelSessionManager.clearSession(guildId, context.adminId());
          }
        });
  }

  private void refreshProductDetailView(
      InteractionHook hook, long guildId, long adminId, long productId) {
    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product -> {
              var stats = redemptionService.getCodeStats(product.id());
              hook.editOriginalEmbeds(buildProductDetailEmbed(product))
                  .setComponents(buildProductDetailComponents(product, stats))
                  .queue(
                      msg ->
                          LOG.trace(
                              "Refreshed product detail for adminId={}, guildId={}",
                              adminId,
                              guildId),
                      err -> {
                        LOG.warn(
                            "Failed to edit product detail message for adminId={}, guildId={}",
                            adminId,
                            guildId,
                            err);
                        adminPanelSessionManager.clearSession(guildId, adminId);
                      });
            },
            () -> {
              adminPanelSessionManager.updateSessionView(
                  guildId, adminId, AdminPanelView.PRODUCT_LIST, Map.of());
              refreshProductListView(hook, guildId, adminId);
            });
  }

  private void refreshCodeListView(
      InteractionHook hook, long guildId, long adminId, long productId, int page) {
    productService
        .getProduct(productId)
        .ifPresentOrElse(
            product -> {
              var codePage = redemptionService.getCodePage(productId, page, PAGE_SIZE);
              hook.editOriginalEmbeds(buildCodeListEmbed(product, codePage))
                  .setComponents(buildCodeListComponents(codePage))
                  .queue(
                      msg ->
                          LOG.trace(
                              "Refreshed code list for adminId={}, guildId={}", adminId, guildId),
                      err -> {
                        LOG.warn(
                            "Failed to edit code list message for adminId={}, guildId={}",
                            adminId,
                            guildId,
                            err);
                        adminPanelSessionManager.clearSession(guildId, adminId);
                      });
            },
            () -> {
              adminPanelSessionManager.updateSessionView(
                  guildId, adminId, AdminPanelView.PRODUCT_LIST, Map.of());
              refreshProductListView(hook, guildId, adminId);
            });
  }

  private void refreshProductListView(InteractionHook hook, long guildId, long adminId) {
    var products = productService.getProducts(guildId);
    hook.editOriginalEmbeds(buildProductListEmbed(products))
        .setComponents(buildProductListComponents(products))
        .queue(
            msg -> LOG.trace("Refreshed product list for adminId={}, guildId={}", adminId, guildId),
            err -> {
              LOG.warn(
                  "Failed to edit product list message for adminId={}, guildId={}",
                  adminId,
                  guildId,
                  err);
              adminPanelSessionManager.clearSession(guildId, adminId);
            });
  }

  private List<ActionRow> buildProductListComponents(List<Product> products) {
    return AdminProductPanelViewFactory.buildProductListComponents(products);
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
}
