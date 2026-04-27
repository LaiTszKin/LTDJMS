package ltdjms.discord.panel.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import ltdjms.discord.panel.commands.AdminPanelButtonHandler;
import ltdjms.discord.panel.commands.AdminProductPanelHandler;
import ltdjms.discord.panel.services.AdminPanelSessionManager.AdminPanelView;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.product.services.ProductService;
import ltdjms.discord.redemption.domain.RedemptionCodeRepository;
import ltdjms.discord.redemption.services.RedemptionService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;

class AdminPanelRefreshIntegrationTest {

  @Test
  void currencyConfigChangeShouldRefreshMainPanelOnly() {
    AdminPanelService adminPanelService = mock(AdminPanelService.class);
    AdminPanelSessionManager sessionManager = new AdminPanelSessionManager();
    AdminPanelButtonHandler buttonHandler =
        new AdminPanelButtonHandler(adminPanelService, sessionManager);
    AdminProductPanelHandler productPanelHandler =
        new AdminProductPanelHandler(
            mock(ProductService.class), mock(RedemptionService.class), sessionManager);
    AdminPanelUpdateListener listener =
        new AdminPanelUpdateListener(buttonHandler, productPanelHandler);

    InteractionHook mainHook = mock(InteractionHook.class);
    WebhookMessageEditAction<Message> mainEditAction = mockEditAction();
    when(adminPanelService.getCurrencyConfig(100L))
        .thenReturn(Result.err(DomainError.invalidInput("missing currency config")));
    when(mainHook.editOriginalEmbeds(any(MessageEmbed.class))).thenReturn(mainEditAction);
    when(mainEditAction.setComponents(anyList())).thenReturn(mainEditAction);
    doAnswer(invocation -> null).when(mainEditAction).queue(any(), any());

    sessionManager.registerSession(100L, 200L, mainHook);

    listener.accept(new ltdjms.discord.shared.events.CurrencyConfigChangedEvent(100L, "金幣", "💰"));

    verify(mainHook).editOriginalEmbeds(any(MessageEmbed.class));
  }

  @Test
  void productChangeShouldRefreshProductPanelOnly() {
    AdminPanelService adminPanelService = mock(AdminPanelService.class);
    ProductService productService = mock(ProductService.class);
    RedemptionService redemptionService = mock(RedemptionService.class);
    AdminPanelSessionManager sessionManager = new AdminPanelSessionManager();
    AdminPanelButtonHandler buttonHandler =
        new AdminPanelButtonHandler(adminPanelService, sessionManager);
    AdminProductPanelHandler productPanelHandler =
        new AdminProductPanelHandler(productService, redemptionService, sessionManager);
    AdminPanelUpdateListener listener =
        new AdminPanelUpdateListener(buttonHandler, productPanelHandler);

    InteractionHook productHook = mock(InteractionHook.class);
    WebhookMessageEditAction<Message> productEditAction = mockEditAction();
    when(productHook.editOriginalEmbeds(any(MessageEmbed.class))).thenReturn(productEditAction);
    when(productEditAction.setComponents(anyList())).thenReturn(productEditAction);
    doAnswer(invocation -> null).when(productEditAction).queue(any(), any());

    Product product =
        new Product(
            300L, 100L, "Test Product", null, null, null, null, Instant.now(), Instant.now());
    when(productService.getProduct(300L)).thenReturn(java.util.Optional.of(product));
    when(productService.getProducts(100L)).thenReturn(List.of(product));
    when(redemptionService.getCodeStats(300L))
        .thenReturn(new RedemptionCodeRepository.CodeStats(0, 0, 0, 0));
    when(redemptionService.getCodePage(300L, 1, 10))
        .thenReturn(new RedemptionService.CodePage(List.of(), 1, 1, 0, 10));

    sessionManager.registerSession(100L, 201L, productHook);
    sessionManager.updateSessionView(
        100L, 201L, AdminPanelView.PRODUCT_DETAIL, java.util.Map.of("productId", 300L, "page", 1));

    listener.accept(
        new ltdjms.discord.shared.events.ProductChangedEvent(
            100L, 300L, ltdjms.discord.shared.events.ProductChangedEvent.OperationType.UPDATED));

    verify(productHook).editOriginalEmbeds(any(MessageEmbed.class));
  }

  @SuppressWarnings("unchecked")
  private static WebhookMessageEditAction<Message> mockEditAction() {
    return mock(WebhookMessageEditAction.class);
  }
}
