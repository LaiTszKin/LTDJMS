package ltdjms.discord.currency.persistence;

import ltdjms.discord.currency.domain.MemberCurrencyAccount;

import java.util.Optional;

/**
 * Repository interface for member currency accounts.
 */
public interface MemberCurrencyAccountRepository {

    Optional<MemberCurrencyAccount> findByGuildIdAndUserId(long guildId, long userId);

    MemberCurrencyAccount save(MemberCurrencyAccount account);

    MemberCurrencyAccount findOrCreate(long guildId, long userId);

    MemberCurrencyAccount adjustBalance(long guildId, long userId, long amount);

    MemberCurrencyAccount setBalance(long guildId, long userId, long newBalance);

    boolean deleteByGuildIdAndUserId(long guildId, long userId);
}
