package ltdjms.discord.panel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.gametoken.domain.DiceGame1Config;
import ltdjms.discord.gametoken.domain.DiceGame2Config;
import ltdjms.discord.gametoken.persistence.DiceGame1ConfigRepository;
import ltdjms.discord.gametoken.persistence.DiceGame2ConfigRepository;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.events.DiceGameConfigChangedEvent;
import ltdjms.discord.shared.events.DomainEventPublisher;

/**
 * Facade for game configuration management operations. Aggregates dice game configuration
 * repositories and event publishing to provide a simplified interface for panel operations.
 */
public class GameConfigManagementFacade {

  private static final Logger LOG = LoggerFactory.getLogger(GameConfigManagementFacade.class);

  private final DiceGame1ConfigRepository diceGame1ConfigRepository;
  private final DiceGame2ConfigRepository diceGame2ConfigRepository;
  private final DomainEventPublisher eventPublisher;

  public GameConfigManagementFacade(
      DiceGame1ConfigRepository diceGame1ConfigRepository,
      DiceGame2ConfigRepository diceGame2ConfigRepository,
      DomainEventPublisher eventPublisher) {
    this.diceGame1ConfigRepository = diceGame1ConfigRepository;
    this.diceGame2ConfigRepository = diceGame2ConfigRepository;
    this.eventPublisher = eventPublisher;
  }

  /** Gets the full configuration for dice-game-1. */
  public DiceGame1Config getDiceGame1Config(long guildId) {
    return diceGame1ConfigRepository.findOrCreateDefault(guildId);
  }

  /** Gets the full configuration for dice-game-2. */
  public DiceGame2Config getDiceGame2Config(long guildId) {
    return diceGame2ConfigRepository.findOrCreateDefault(guildId);
  }

  /** Updates dice-game-1 configuration. */
  public Result<DiceGame1Config, DomainError> updateDiceGame1Config(
      long guildId, Long minTokens, Long maxTokens, Long rewardPerDice) {
    LOG.debug(
        "Updating dice-game-1 config: guildId={}, min={}, max={}, reward={}",
        guildId,
        minTokens,
        maxTokens,
        rewardPerDice);

    try {
      DiceGame1Config current = diceGame1ConfigRepository.findOrCreateDefault(guildId);
      DiceGame1Config updated = current;

      if (minTokens != null || maxTokens != null) {
        long newMin = minTokens != null ? minTokens : current.minTokensPerPlay();
        long newMax = maxTokens != null ? maxTokens : current.maxTokensPerPlay();
        updated = diceGame1ConfigRepository.updateTokensPerPlayRange(guildId, newMin, newMax);
      }

      if (rewardPerDice != null) {
        updated = diceGame1ConfigRepository.updateRewardPerDiceValue(guildId, rewardPerDice);
      }

      LOG.info("Dice-game-1 config updated: guildId={}", guildId);

      // Publish event after successful update
      eventPublisher.publish(
          new DiceGameConfigChangedEvent(guildId, DiceGameConfigChangedEvent.GameType.DICE_GAME_1));

      return Result.ok(updated);
    } catch (IllegalArgumentException e) {
      return Result.err(DomainError.invalidInput(e.getMessage()));
    }
  }

  /** Updates dice-game-2 configuration. */
  public Result<DiceGame2Config, DomainError> updateDiceGame2Config(
      long guildId,
      Long minTokens,
      Long maxTokens,
      Long straightMultiplier,
      Long baseMultiplier,
      Long tripleLowBonus,
      Long tripleHighBonus) {
    LOG.debug(
        "Updating dice-game-2 config: guildId={}, min={}, max={}, "
            + "straight={}, base={}, tripleLow={}, tripleHigh={}",
        guildId,
        minTokens,
        maxTokens,
        straightMultiplier,
        baseMultiplier,
        tripleLowBonus,
        tripleHighBonus);

    try {
      DiceGame2Config current = diceGame2ConfigRepository.findOrCreateDefault(guildId);
      DiceGame2Config updated = current;

      if (minTokens != null || maxTokens != null) {
        long newMin = minTokens != null ? minTokens : current.minTokensPerPlay();
        long newMax = maxTokens != null ? maxTokens : current.maxTokensPerPlay();
        updated = diceGame2ConfigRepository.updateTokensPerPlayRange(guildId, newMin, newMax);
      }

      if (straightMultiplier != null || baseMultiplier != null) {
        long newStraight =
            straightMultiplier != null ? straightMultiplier : updated.straightMultiplier();
        long newBase = baseMultiplier != null ? baseMultiplier : updated.baseMultiplier();
        updated = diceGame2ConfigRepository.updateMultipliers(guildId, newStraight, newBase);
      }

      if (tripleLowBonus != null || tripleHighBonus != null) {
        long newLow = tripleLowBonus != null ? tripleLowBonus : updated.tripleLowBonus();
        long newHigh = tripleHighBonus != null ? tripleHighBonus : updated.tripleHighBonus();
        updated = diceGame2ConfigRepository.updateTripleBonuses(guildId, newLow, newHigh);
      }

      LOG.info("Dice-game-2 config updated: guildId={}", guildId);

      // Publish event after successful update
      eventPublisher.publish(
          new DiceGameConfigChangedEvent(guildId, DiceGameConfigChangedEvent.GameType.DICE_GAME_2));

      return Result.ok(updated);
    } catch (IllegalArgumentException e) {
      return Result.err(DomainError.invalidInput(e.getMessage()));
    }
  }
}
