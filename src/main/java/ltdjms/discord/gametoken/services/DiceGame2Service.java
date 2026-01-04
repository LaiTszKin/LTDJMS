package ltdjms.discord.gametoken.services;

import java.util.List;

import ltdjms.discord.gametoken.domain.DiceGame2Config;

/**
 * Service interface for the dice-game-2 mini-game. Handles dice rolling, reward calculation with
 * straights and triples, and currency distribution.
 *
 * <p>The number of dice rolled is determined by the number of tokens spent: 1 token = 3 dice.
 *
 * <p>Reward rules:
 *
 * <ul>
 *   <li>Straights (consecutive increasing sequence of length >= 3): sum × straightMultiplier
 *   <li>Triples (exactly 3 consecutive same values):
 *       <ul>
 *         <li>Sum < 10 (values 1-3): tripleLowBonus
 *         <li>Sum >= 10 (values 4-6): tripleHighBonus
 *       </ul>
 *   <li>4+ consecutive same values: NOT a triple, counted as non-straight
 *   <li>Non-straight/non-triple dice: sum × baseMultiplier
 * </ul>
 */
public interface DiceGame2Service {

  /**
   * Plays the dice game for a member using the provided configuration and token amount. Rolls dice
   * (3 per token) and calculates the total reward based on straights, triples, and remaining dice.
   * The reward is added to the member's currency account.
   *
   * @param guildId the Discord guild ID
   * @param userId the Discord user ID
   * @param config the game configuration containing multipliers and bonuses
   * @param diceCount the number of dice to roll (equals tokensSpent * 3)
   * @return the game result
   */
  DiceGame2Result play(long guildId, long userId, DiceGame2Config config, int diceCount);

  /** Result of a dice game 2. */
  record DiceGame2Result(
      long guildId,
      long userId,
      List<Integer> diceRolls,
      long totalReward,
      long previousBalance,
      long newBalance,
      List<List<Integer>> straightSegments,
      List<List<Integer>> tripleSegments,
      long straightReward,
      long nonStraightReward,
      long tripleReward) {
    /** Formats the result as a Discord message. */
    public String formatMessage(String currencyIcon, String currencyName) {
      StringBuilder sb = new StringBuilder();
      sb.append("**Dice Game 2 Results**\n");
      sb.append("Rolls: ");

      for (int i = 0; i < diceRolls.size(); i++) {
        int roll = diceRolls.get(i);
        sb.append(diceEmoji(roll));
        if (i < diceRolls.size() - 1) {
          sb.append(" ");
        }
      }

      sb.append("\n\n");

      // Show reward breakdown
      if (!straightSegments.isEmpty()) {
        sb.append(
            String.format("Straights: %s %,d %s\n", currencyIcon, straightReward, currencyName));
      }
      if (!tripleSegments.isEmpty()) {
        sb.append(
            String.format(
                "Triples: %s %,d %s (%d group%s)\n",
                currencyIcon,
                tripleReward,
                currencyName,
                tripleSegments.size(),
                tripleSegments.size() > 1 ? "s" : ""));
      }
      if (nonStraightReward > 0) {
        sb.append(
            String.format("Base: %s %,d %s\n", currencyIcon, nonStraightReward, currencyName));
      }

      sb.append("\n");
      sb.append(
          String.format("**Total Reward:** %s %,d %s\n", currencyIcon, totalReward, currencyName));
      sb.append(
          String.format("**New Balance:** %s %,d %s", currencyIcon, newBalance, currencyName));

      return sb.toString();
    }

    private String diceEmoji(int value) {
      return switch (value) {
        case 1 -> ":one:";
        case 2 -> ":two:";
        case 3 -> ":three:";
        case 4 -> ":four:";
        case 5 -> ":five:";
        case 6 -> ":six:";
        default -> String.valueOf(value);
      };
    }
  }
}
