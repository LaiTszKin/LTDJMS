package ltdjms.discord.gametoken.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.currency.services.GameRewardService;
import ltdjms.discord.gametoken.domain.DiceGame1Config;

/**
 * Default implementation of {@link DiceGame1Service}. Handles dice rolling, reward calculation, and
 * currency distribution via {@link GameRewardService}.
 *
 * <p>The number of dice rolled is determined by the number of tokens spent: 1 token = 1 dice.
 */
public class DefaultDiceGame1Service implements DiceGame1Service {

  private static final Logger LOG = LoggerFactory.getLogger(DiceGame1Service.class);

  private final GameRewardService gameRewardService;
  private final Random random;

  public DefaultDiceGame1Service(GameRewardService gameRewardService) {
    this(gameRewardService, new Random());
  }

  /** Constructor with injectable Random for testing. */
  public DefaultDiceGame1Service(GameRewardService gameRewardService, Random random) {
    this.gameRewardService = gameRewardService;
    this.random = random;
  }

  /**
   * Plays the dice game for a member using the provided configuration and token amount. Rolls one
   * dice per token spent and calculates the total reward based on dice values. The reward is added
   * to the member's currency account via {@link GameRewardService}.
   *
   * @param guildId the Discord guild ID
   * @param userId the Discord user ID
   * @param config the game configuration containing reward multiplier
   * @param diceCount the number of dice to roll (equals tokens spent)
   * @return the game result
   */
  public DiceGameResult play(long guildId, long userId, DiceGame1Config config, int diceCount) {
    LOG.debug(
        "Playing dice-game-1 for guildId={}, userId={}, diceCount={}, rewardPerDice={}",
        guildId,
        userId,
        diceCount,
        config.rewardPerDiceValue());

    // Roll dice
    List<Integer> diceRolls = rollDice(diceCount);

    // Calculate total reward using configured multiplier
    long totalReward = calculateTotalReward(diceRolls, config.rewardPerDiceValue());

    // Get previous balance before applying reward
    long previousBalance =
        gameRewardService.creditReward(
            guildId, userId, 0, CurrencyTransaction.Source.DICE_GAME_1_WIN);

    // Apply reward to currency account via GameRewardService
    long newBalance =
        gameRewardService.creditReward(
            guildId, userId, totalReward, CurrencyTransaction.Source.DICE_GAME_1_WIN);

    DiceGameResult result =
        new DiceGameResult(guildId, userId, diceRolls, totalReward, previousBalance, newBalance);

    LOG.info(
        "Dice game completed: guildId={}, userId={}, rolls={}, reward={}, newBalance={}",
        guildId,
        userId,
        diceRolls,
        totalReward,
        newBalance);

    return result;
  }

  /**
   * Rolls the specified number of dice.
   *
   * @param count the number of dice to roll
   * @return list of dice values (1-6)
   */
  List<Integer> rollDice(int count) {
    List<Integer> rolls = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      rolls.add(random.nextInt(6) + 1); // 1-6
    }
    return rolls;
  }

  /**
   * Calculates the total reward based on dice rolls and configured reward multiplier.
   *
   * @param diceRolls the list of dice values
   * @param rewardPerDiceValue the reward multiplier per dice value
   * @return the total reward
   */
  long calculateTotalReward(List<Integer> diceRolls, long rewardPerDiceValue) {
    long sum = 0;
    for (int i = 0; i < diceRolls.size(); i++) {
      sum += diceRolls.get(i);
    }
    return sum * rewardPerDiceValue;
  }
}
