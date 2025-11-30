package ltdjms.discord.gametoken.persistence;

import ltdjms.discord.gametoken.domain.DiceGame1Config;

import java.util.Optional;

/**
 * Repository interface for dice-game-1 configuration.
 */
public interface DiceGame1ConfigRepository {

    Optional<DiceGame1Config> findByGuildId(long guildId);

    DiceGame1Config save(DiceGame1Config config);

    DiceGame1Config findOrCreateDefault(long guildId);

    DiceGame1Config updateTokensPerPlay(long guildId, long tokensPerPlay);

    boolean deleteByGuildId(long guildId);
}
