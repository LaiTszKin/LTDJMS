package ltdjms.discord.currency.services;

import ltdjms.discord.currency.domain.BalanceView;
import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.currency.domain.MemberCurrencyAccount;
import ltdjms.discord.currency.persistence.GuildCurrencyConfigRepository;
import ltdjms.discord.currency.persistence.MemberCurrencyAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of BalanceService.
 * Handles auto-creation of accounts and combines balance with currency configuration.
 */
public class DefaultBalanceService implements BalanceService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBalanceService.class);

    private final MemberCurrencyAccountRepository accountRepository;
    private final GuildCurrencyConfigRepository configRepository;

    public DefaultBalanceService(
            MemberCurrencyAccountRepository accountRepository,
            GuildCurrencyConfigRepository configRepository) {
        this.accountRepository = accountRepository;
        this.configRepository = configRepository;
    }

    @Override
    public BalanceView getBalance(long guildId, long userId) {
        LOG.debug("Getting balance for guildId={}, userId={}", guildId, userId);

        // Get or create member account
        MemberCurrencyAccount account = accountRepository.findOrCreate(guildId, userId);

        // Get guild currency configuration (or defaults)
        GuildCurrencyConfig config = configRepository.findByGuildId(guildId)
                .orElse(GuildCurrencyConfig.createDefault(guildId));

        BalanceView view = new BalanceView(
                guildId,
                userId,
                account.balance(),
                config.currencyName(),
                config.currencyIcon()
        );

        LOG.info("Retrieved balance: guildId={}, userId={}, balance={}, currency={}",
                guildId, userId, account.balance(), config.currencyName());

        return view;
    }
}
