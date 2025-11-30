package ltdjms.discord.currency.bot;

import ltdjms.discord.currency.commands.BalanceAdjustmentCommandHandler;
import ltdjms.discord.currency.commands.BalanceCommandHandler;
import ltdjms.discord.currency.commands.CurrencyConfigCommandHandler;
import ltdjms.discord.currency.persistence.GuildCurrencyConfigRepository;
import ltdjms.discord.currency.persistence.JdbcGuildCurrencyConfigRepository;
import ltdjms.discord.currency.persistence.JdbcMemberCurrencyAccountRepository;
import ltdjms.discord.currency.persistence.MemberCurrencyAccountRepository;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.BalanceService;
import ltdjms.discord.currency.services.CurrencyConfigService;
import ltdjms.discord.currency.services.DefaultBalanceService;
import ltdjms.discord.shared.DatabaseConfig;
import ltdjms.discord.shared.EnvironmentConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Main entry point for the Discord Currency Bot.
 * Bootstraps JDA with required intents and wires up command handlers.
 */
public class DiscordCurrencyBot {

    private static final Logger LOG = LoggerFactory.getLogger(DiscordCurrencyBot.class);

    private final JDA jda;
    private final DatabaseConfig databaseConfig;

    public DiscordCurrencyBot(EnvironmentConfig envConfig) throws InterruptedException {
        LOG.info("Starting Discord Currency Bot...");

        // Initialize database
        this.databaseConfig = new DatabaseConfig(envConfig);
        DataSource dataSource = databaseConfig.getDataSource();

        // Initialize repositories
        GuildCurrencyConfigRepository configRepository = new JdbcGuildCurrencyConfigRepository(dataSource);
        MemberCurrencyAccountRepository accountRepository = new JdbcMemberCurrencyAccountRepository(dataSource);

        // Initialize services
        BalanceService balanceService = new DefaultBalanceService(accountRepository, configRepository);
        CurrencyConfigService configService = new CurrencyConfigService(configRepository);
        BalanceAdjustmentService adjustmentService = new BalanceAdjustmentService(accountRepository, configRepository);

        // Initialize command handlers
        BalanceCommandHandler balanceHandler = new BalanceCommandHandler(balanceService);
        CurrencyConfigCommandHandler configHandler = new CurrencyConfigCommandHandler(configService);
        BalanceAdjustmentCommandHandler adjustmentHandler = new BalanceAdjustmentCommandHandler(adjustmentService);

        // Initialize slash command listener
        SlashCommandListener slashCommandListener = new SlashCommandListener(
                balanceHandler, configHandler, adjustmentHandler);

        // Build JDA instance
        this.jda = JDABuilder.createLight(envConfig.getDiscordBotToken())
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(slashCommandListener)
                .build();

        // Wait for JDA to be ready
        jda.awaitReady();

        // Register slash commands globally
        slashCommandListener.registerCommands(jda);

        LOG.info("Discord Currency Bot started successfully!");
    }

    /**
     * Returns the JDA instance for the bot.
     *
     * @return the JDA instance
     */
    public JDA getJda() {
        return jda;
    }

    /**
     * Shuts down the bot gracefully.
     */
    public void shutdown() {
        LOG.info("Shutting down Discord Currency Bot...");
        if (jda != null) {
            jda.shutdown();
        }
        if (databaseConfig != null) {
            databaseConfig.close();
        }
        LOG.info("Discord Currency Bot shutdown complete.");
    }

    public static void main(String[] args) {
        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            DiscordCurrencyBot bot = new DiscordCurrencyBot(envConfig);

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Received shutdown signal");
                bot.shutdown();
            }));

        } catch (IllegalStateException e) {
            LOG.error("Configuration error: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            LOG.error("Failed to start Discord Currency Bot", e);
            System.exit(1);
        }
    }
}
