package ltdjms.discord.panel.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ltdjms.discord.panel.commands.AdminPanelButtonHandler;
import ltdjms.discord.panel.commands.AdminProductPanelHandler;
import ltdjms.discord.shared.events.CurrencyConfigChangedEvent;
import ltdjms.discord.shared.events.DiceGameConfigChangedEvent;
import ltdjms.discord.shared.events.ProductChangedEvent;
import ltdjms.discord.shared.events.RedemptionCodesGeneratedEvent;

class AdminPanelUpdateListenerTest {

  private AdminPanelButtonHandler adminPanelButtonHandler;
  private AdminProductPanelHandler adminProductPanelHandler;
  private AdminPanelUpdateListener listener;

  @BeforeEach
  void setUp() {
    adminPanelButtonHandler = mock(AdminPanelButtonHandler.class);
    adminProductPanelHandler = mock(AdminProductPanelHandler.class);
    listener = new AdminPanelUpdateListener(adminPanelButtonHandler, adminProductPanelHandler);
  }

  @Test
  void acceptShouldRefreshMainPanelsForCurrencyConfigChanges() {
    listener.accept(new CurrencyConfigChangedEvent(123L, "金幣", "💰"));

    verify(adminPanelButtonHandler).refreshMainPanels(123L);
    verify(adminProductPanelHandler, never()).refreshProductPanels(123L);
  }

  @Test
  void acceptShouldRefreshMainPanelsForDiceConfigChanges() {
    listener.accept(
        new DiceGameConfigChangedEvent(456L, DiceGameConfigChangedEvent.GameType.DICE_GAME_1));

    verify(adminPanelButtonHandler).refreshMainPanels(456L);
    verify(adminProductPanelHandler, never()).refreshProductPanels(456L);
  }

  @Test
  void acceptShouldRefreshProductPanelsForProductChanges() {
    listener.accept(new ProductChangedEvent(100L, 1L, ProductChangedEvent.OperationType.UPDATED));

    verify(adminProductPanelHandler).refreshProductPanels(100L);
    verify(adminPanelButtonHandler, never()).refreshMainPanels(100L);
  }

  @Test
  void acceptShouldRefreshProductPanelsForRedemptionCodeGeneration() {
    listener.accept(new RedemptionCodesGeneratedEvent(321L, 9L, 5));

    verify(adminProductPanelHandler).refreshProductPanels(321L);
    verify(adminPanelButtonHandler, never()).refreshMainPanels(321L);
  }
}
