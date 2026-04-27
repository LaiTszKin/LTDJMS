package ltdjms.discord.panel.services;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.panel.commands.AdminPanelButtonHandler;
import ltdjms.discord.panel.commands.AdminProductPanelHandler;
import ltdjms.discord.shared.events.CurrencyConfigChangedEvent;
import ltdjms.discord.shared.events.DiceGameConfigChangedEvent;
import ltdjms.discord.shared.events.DomainEvent;
import ltdjms.discord.shared.events.ProductChangedEvent;
import ltdjms.discord.shared.events.RedemptionCodesGeneratedEvent;

/** Listener for domain events that triggers real-time updates for active admin panels. */
public class AdminPanelUpdateListener implements Consumer<DomainEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(AdminPanelUpdateListener.class);

  private final AdminPanelButtonHandler adminPanelButtonHandler;
  private final AdminProductPanelHandler adminProductPanelHandler;

  public AdminPanelUpdateListener(
      AdminPanelButtonHandler adminPanelButtonHandler,
      AdminProductPanelHandler adminProductPanelHandler) {
    this.adminPanelButtonHandler = adminPanelButtonHandler;
    this.adminProductPanelHandler = adminProductPanelHandler;
  }

  @Override
  public void accept(DomainEvent event) {
    if (event instanceof CurrencyConfigChangedEvent e) {
      handleCurrencyConfigChanged(e);
    } else if (event instanceof DiceGameConfigChangedEvent e) {
      handleDiceGameConfigChanged(e);
    } else if (event instanceof ProductChangedEvent e) {
      handleProductChanged(e);
    } else if (event instanceof RedemptionCodesGeneratedEvent e) {
      handleRedemptionCodesGenerated(e);
    }
  }

  private void handleCurrencyConfigChanged(CurrencyConfigChangedEvent event) {
    LOG.debug(
        "Admin panel refresh triggered by currency config change for guildId={}", event.guildId());
    adminPanelButtonHandler.refreshMainPanels(event.guildId());
  }

  private void handleDiceGameConfigChanged(DiceGameConfigChangedEvent event) {
    LOG.debug(
        "Admin panel refresh triggered by {} config change for guildId={}",
        event.gameType(),
        event.guildId());
    adminPanelButtonHandler.refreshMainPanels(event.guildId());
  }

  private void handleProductChanged(ProductChangedEvent event) {
    LOG.debug(
        "Admin panel refresh triggered by product {} for guildId={}, productId={}",
        event.operationType(),
        event.guildId(),
        event.productId());
    adminProductPanelHandler.refreshProductPanels(event.guildId());
  }

  private void handleRedemptionCodesGenerated(RedemptionCodesGeneratedEvent event) {
    LOG.debug(
        "Admin panel refresh triggered by redemption codes generated for guildId={}, productId={},"
            + " count={}",
        event.guildId(),
        event.productId(),
        event.count());
    adminProductPanelHandler.refreshProductPanels(event.guildId());
  }
}
