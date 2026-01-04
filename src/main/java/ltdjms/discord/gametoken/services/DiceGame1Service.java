package ltdjms.discord.gametoken.services;

import ltdjms.discord.gametoken.domain.DiceGame1Config;

/**
 * Service interface for the dice-game-1 mini-game. Handles dice rolling, reward calculation, and
 * currency distribution.
 *
 * <p>The number of dice rolled is determined by the number of tokens spent: 1 token = 1 dice.
 */
public interface DiceGame1Service {

  /**
   * Plays the dice game for a member using the provided configuration and token amount. Rolls one
   * dice per token spent and calculates the total reward based on dice values. The reward is added
   * to the member's currency account.
   *
   * @param guildId the Discord guild ID
   * @param userId the Discord user ID
   * @param config the game configuration containing reward multiplier
   * @param diceCount the number of dice to roll (equals tokens spent)
   * @return the game result
   */
  DiceGameResult play(long guildId, long userId, DiceGame1Config config, int diceCount);

  /** Result of a dice game. */
  record DiceGameResult(
      long guildId,
      long userId,
      java.util.List<Integer> diceRolls,
      long totalReward,
      long previousBalance,
      long newBalance) {
    /** Formats the result as a Discord message. */
    public String formatMessage(String currencyIcon, String currencyName) {
      StringBuilder sb = new StringBuilder();
      sb.append("**Dice Game Results**\n");
      sb.append("Rolls: ");

      for (int i = 0; i < diceRolls.size(); i++) {
        int roll = diceRolls.get(i);
        sb.append(diceEmoji(roll));
        if (i < diceRolls.size() - 1) {
          sb.append(" ");
        }
      }

      sb.append("\n\n");
      sb.append(
          String.format("Total Reward: %s %,d %s\n", currencyIcon, totalReward, currencyName));
      sb.append(String.format("New Balance: %s %,d %s", currencyIcon, newBalance, currencyName));

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
