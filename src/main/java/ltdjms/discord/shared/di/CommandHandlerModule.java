package ltdjms.discord.shared.di;

import dagger.Module;
import dagger.Provides;
import ltdjms.discord.currency.bot.SlashCommandListener;
import ltdjms.discord.currency.commands.BalanceAdjustmentCommandHandler;
import ltdjms.discord.currency.commands.BalanceCommandHandler;
import ltdjms.discord.currency.commands.CurrencyConfigCommandHandler;
import ltdjms.discord.currency.persistence.GuildCurrencyConfigRepository;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.BalanceService;
import ltdjms.discord.currency.services.CurrencyConfigService;
import ltdjms.discord.gametoken.commands.DiceGame1CommandHandler;
import ltdjms.discord.gametoken.commands.DiceGame1ConfigCommandHandler;
import ltdjms.discord.gametoken.commands.DiceGame2CommandHandler;
import ltdjms.discord.gametoken.commands.DiceGame2ConfigCommandHandler;
import ltdjms.discord.gametoken.commands.GameTokenAdjustCommandHandler;
import ltdjms.discord.gametoken.persistence.DiceGame1ConfigRepository;
import ltdjms.discord.gametoken.persistence.DiceGame2ConfigRepository;
import ltdjms.discord.gametoken.services.DiceGame1Service;
import ltdjms.discord.gametoken.services.DiceGame2Service;
import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import ltdjms.discord.panel.commands.AdminPanelButtonHandler;
import ltdjms.discord.panel.commands.AdminPanelCommandHandler;
import ltdjms.discord.panel.commands.UserPanelButtonHandler;
import ltdjms.discord.panel.commands.UserPanelCommandHandler;
import ltdjms.discord.panel.services.AdminPanelService;
import ltdjms.discord.panel.services.UserPanelService;

import javax.inject.Singleton;

/**
 * Dagger module providing command handler dependencies.
 */
@Module
public class CommandHandlerModule {

    @Provides
    @Singleton
    public BalanceCommandHandler provideBalanceCommandHandler(BalanceService balanceService) {
        return new BalanceCommandHandler(balanceService);
    }

    @Provides
    @Singleton
    public CurrencyConfigCommandHandler provideCurrencyConfigCommandHandler(CurrencyConfigService configService) {
        return new CurrencyConfigCommandHandler(configService);
    }

    @Provides
    @Singleton
    public BalanceAdjustmentCommandHandler provideBalanceAdjustmentCommandHandler(
            BalanceAdjustmentService adjustmentService) {
        return new BalanceAdjustmentCommandHandler(adjustmentService);
    }

    @Provides
    @Singleton
    public GameTokenAdjustCommandHandler provideGameTokenAdjustCommandHandler(GameTokenService tokenService) {
        return new GameTokenAdjustCommandHandler(tokenService);
    }

    @Provides
    @Singleton
    public DiceGame1CommandHandler provideDiceGame1CommandHandler(
            GameTokenService tokenService,
            DiceGame1Service diceGameService,
            DiceGame1ConfigRepository configRepository,
            GuildCurrencyConfigRepository currencyConfigRepository,
            GameTokenTransactionService transactionService) {
        return new DiceGame1CommandHandler(
                tokenService, diceGameService, configRepository, currencyConfigRepository, transactionService);
    }

    @Provides
    @Singleton
    public DiceGame1ConfigCommandHandler provideDiceGame1ConfigCommandHandler(
            DiceGame1ConfigRepository configRepository) {
        return new DiceGame1ConfigCommandHandler(configRepository);
    }

    @Provides
    @Singleton
    public DiceGame2CommandHandler provideDiceGame2CommandHandler(
            GameTokenService tokenService,
            DiceGame2Service diceGameService,
            DiceGame2ConfigRepository configRepository,
            GuildCurrencyConfigRepository currencyConfigRepository,
            GameTokenTransactionService transactionService) {
        return new DiceGame2CommandHandler(
                tokenService, diceGameService, configRepository, currencyConfigRepository, transactionService);
    }

    @Provides
    @Singleton
    public DiceGame2ConfigCommandHandler provideDiceGame2ConfigCommandHandler(
            DiceGame2ConfigRepository configRepository) {
        return new DiceGame2ConfigCommandHandler(configRepository);
    }

    @Provides
    @Singleton
    public UserPanelService provideUserPanelService(
            BalanceService balanceService,
            GameTokenService gameTokenService,
            GameTokenTransactionService transactionService) {
        return new UserPanelService(balanceService, gameTokenService, transactionService);
    }

    @Provides
    @Singleton
    public UserPanelCommandHandler provideUserPanelCommandHandler(UserPanelService userPanelService) {
        return new UserPanelCommandHandler(userPanelService);
    }

    @Provides
    @Singleton
    public UserPanelButtonHandler provideUserPanelButtonHandler(UserPanelService userPanelService) {
        return new UserPanelButtonHandler(userPanelService);
    }

    @Provides
    @Singleton
    public AdminPanelService provideAdminPanelService(
            BalanceService balanceService,
            BalanceAdjustmentService balanceAdjustmentService,
            GameTokenService gameTokenService,
            GameTokenTransactionService transactionService,
            DiceGame1ConfigRepository diceGame1ConfigRepository,
            DiceGame2ConfigRepository diceGame2ConfigRepository) {
        return new AdminPanelService(
                balanceService, balanceAdjustmentService, gameTokenService,
                transactionService, diceGame1ConfigRepository, diceGame2ConfigRepository);
    }

    @Provides
    @Singleton
    public AdminPanelCommandHandler provideAdminPanelCommandHandler(AdminPanelService adminPanelService) {
        return new AdminPanelCommandHandler(adminPanelService);
    }

    @Provides
    @Singleton
    public AdminPanelButtonHandler provideAdminPanelButtonHandler(AdminPanelService adminPanelService) {
        return new AdminPanelButtonHandler(adminPanelService);
    }

    @Provides
    @Singleton
    public SlashCommandListener provideSlashCommandListener(
            BalanceCommandHandler balanceHandler,
            CurrencyConfigCommandHandler configHandler,
            BalanceAdjustmentCommandHandler adjustmentHandler,
            GameTokenAdjustCommandHandler gameTokenAdjustHandler,
            DiceGame1CommandHandler diceGame1Handler,
            DiceGame1ConfigCommandHandler diceGame1ConfigHandler,
            DiceGame2CommandHandler diceGame2Handler,
            DiceGame2ConfigCommandHandler diceGame2ConfigHandler,
            UserPanelCommandHandler userPanelHandler,
            AdminPanelCommandHandler adminPanelHandler) {
        return new SlashCommandListener(
                balanceHandler, configHandler, adjustmentHandler,
                gameTokenAdjustHandler, diceGame1Handler, diceGame1ConfigHandler,
                diceGame2Handler, diceGame2ConfigHandler, userPanelHandler, adminPanelHandler);
    }
}
