package ltdjms.discord.panel.commands;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import ltdjms.discord.panel.services.AdminPanelService;
import ltdjms.discord.panel.services.AdminPanelSessionManager;
import ltdjms.discord.panel.services.AdminPanelSessionManager.AdminPanelView;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;

class AdminPanelButtonHandlerRefreshTest {

  @Test
  void refreshMainPanelsShouldEditActiveMainSession() {
    AdminPanelService service = mock(AdminPanelService.class);
    AdminPanelSessionManager sessionManager = new AdminPanelSessionManager();
    AdminPanelButtonHandler handler = new AdminPanelButtonHandler(service, sessionManager);
    InteractionHook hook = mock(InteractionHook.class);
    WebhookMessageEditAction<Message> editAction = mockEditAction();

    when(service.getCurrencyConfig(100L))
        .thenReturn(Result.err(DomainError.invalidInput("no currency config")));
    when(hook.editOriginalEmbeds(any(MessageEmbed.class))).thenReturn(editAction);
    when(editAction.setComponents(anyList())).thenReturn(editAction);
    doAnswer(invocation -> null).when(editAction).queue(any(), any());

    sessionManager.registerSession(100L, 200L, hook);

    handler.refreshMainPanels(100L);

    verify(hook).editOriginalEmbeds(any(MessageEmbed.class));
    verify(editAction).setComponents(anyList());
  }

  @Test
  void refreshMainPanelsShouldSkipNonMainViews() {
    AdminPanelService service = mock(AdminPanelService.class);
    AdminPanelSessionManager sessionManager = new AdminPanelSessionManager();
    AdminPanelButtonHandler handler = new AdminPanelButtonHandler(service, sessionManager);
    InteractionHook hook = mock(InteractionHook.class);
    WebhookMessageEditAction<Message> editAction = mockEditAction();

    when(service.getCurrencyConfig(100L))
        .thenReturn(Result.err(DomainError.invalidInput("no currency config")));
    when(hook.editOriginalEmbeds(any(MessageEmbed.class))).thenReturn(editAction);
    when(editAction.setComponents(anyList())).thenReturn(editAction);
    doAnswer(invocation -> null).when(editAction).queue(any(), any());

    sessionManager.registerSession(100L, 200L, hook);
    sessionManager.updateSessionView(
        100L, 200L, AdminPanelView.PRODUCT_LIST, java.util.Map.of("productId", 300L));

    handler.refreshMainPanels(100L);

    verify(hook, never()).editOriginalEmbeds(any(MessageEmbed.class));
  }

  @SuppressWarnings("unchecked")
  private static WebhookMessageEditAction<Message> mockEditAction() {
    return mock(WebhookMessageEditAction.class);
  }
}
