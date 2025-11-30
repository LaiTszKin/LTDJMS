package ltdjms.discord.gametoken.persistence;

import ltdjms.discord.gametoken.domain.GameTokenAccount;

import java.util.Optional;

/**
 * Repository interface for game token accounts.
 */
public interface GameTokenAccountRepository {

    Optional<GameTokenAccount> findByGuildIdAndUserId(long guildId, long userId);

    GameTokenAccount save(GameTokenAccount account);

    GameTokenAccount findOrCreate(long guildId, long userId);

    GameTokenAccount adjustTokens(long guildId, long userId, long amount);

    GameTokenAccount setTokens(long guildId, long userId, long newTokens);

    boolean deleteByGuildIdAndUserId(long guildId, long userId);
}
