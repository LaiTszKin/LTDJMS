package ltdjms.discord.currency.bot;

import ltdjms.discord.currency.commands.BalanceAdjustmentCommandHandler;
import ltdjms.discord.currency.commands.BalanceCommandHandler;
import ltdjms.discord.currency.commands.CurrencyConfigCommandHandler;
import ltdjms.discord.gametoken.commands.DiceGame1CommandHandler;
import ltdjms.discord.gametoken.commands.DiceGame1ConfigCommandHandler;
import ltdjms.discord.gametoken.commands.DiceGame2CommandHandler;
import ltdjms.discord.gametoken.commands.DiceGame2ConfigCommandHandler;
import ltdjms.discord.gametoken.commands.GameTokenAdjustCommandHandler;
import ltdjms.discord.panel.commands.AdminPanelCommandHandler;
import ltdjms.discord.panel.commands.UserPanelCommandHandler;
import ltdjms.discord.shared.localization.CommandLocalizations;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that receives slash command events and delegates to appropriate command handlers.
 * Includes metrics collection for command latency and success/error tracking.
 */
public class SlashCommandListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SlashCommandListener.class);

    // Command names
    public static final String CMD_BALANCE = "balance";
    public static final String CMD_CURRENCY_CONFIG = "currency-config";
    public static final String CMD_ADJUST_BALANCE = "adjust-balance";
    public static final String CMD_GAME_TOKEN_ADJUST = "game-token-adjust";
    public static final String CMD_DICE_GAME_1 = "dice-game-1";
    public static final String CMD_DICE_GAME_1_CONFIG = "dice-game-1-config";
    public static final String CMD_DICE_GAME_2 = "dice-game-2";
    public static final String CMD_DICE_GAME_2_CONFIG = "dice-game-2-config";
    public static final String CMD_USER_PANEL = "user-panel";
    public static final String CMD_ADMIN_PANEL = "admin-panel";

    private final BalanceCommandHandler balanceHandler;
    private final CurrencyConfigCommandHandler configHandler;
    private final BalanceAdjustmentCommandHandler adjustmentHandler;
    private final GameTokenAdjustCommandHandler gameTokenAdjustHandler;
    private final DiceGame1CommandHandler diceGame1Handler;
    private final DiceGame1ConfigCommandHandler diceGame1ConfigHandler;
    private final DiceGame2CommandHandler diceGame2Handler;
    private final DiceGame2ConfigCommandHandler diceGame2ConfigHandler;
    private final UserPanelCommandHandler userPanelHandler;
    private final AdminPanelCommandHandler adminPanelHandler;
    private final SlashCommandMetrics metrics;

    public SlashCommandListener(
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
        this(balanceHandler, configHandler, adjustmentHandler,
                gameTokenAdjustHandler, diceGame1Handler, diceGame1ConfigHandler,
                diceGame2Handler, diceGame2ConfigHandler, userPanelHandler, adminPanelHandler,
                new SlashCommandMetrics());
    }

    public SlashCommandListener(
            BalanceCommandHandler balanceHandler,
            CurrencyConfigCommandHandler configHandler,
            BalanceAdjustmentCommandHandler adjustmentHandler,
            GameTokenAdjustCommandHandler gameTokenAdjustHandler,
            DiceGame1CommandHandler diceGame1Handler,
            DiceGame1ConfigCommandHandler diceGame1ConfigHandler,
            DiceGame2CommandHandler diceGame2Handler,
            DiceGame2ConfigCommandHandler diceGame2ConfigHandler,
            UserPanelCommandHandler userPanelHandler,
            AdminPanelCommandHandler adminPanelHandler,
            SlashCommandMetrics metrics) {
        this.balanceHandler = balanceHandler;
        this.configHandler = configHandler;
        this.adjustmentHandler = adjustmentHandler;
        this.gameTokenAdjustHandler = gameTokenAdjustHandler;
        this.diceGame1Handler = diceGame1Handler;
        this.diceGame1ConfigHandler = diceGame1ConfigHandler;
        this.diceGame2Handler = diceGame2Handler;
        this.diceGame2ConfigHandler = diceGame2ConfigHandler;
        this.userPanelHandler = userPanelHandler;
        this.adminPanelHandler = adminPanelHandler;
        this.metrics = metrics;
    }

    /**
     * Gets the metrics collector for this listener.
     *
     * @return the metrics instance
     */
    public SlashCommandMetrics getMetrics() {
        return metrics;
    }

    /**
     * Registers all slash commands with Discord.
     * Should be called after JDA is ready.
     * Includes zh-TW localization for command names and descriptions.
     *
     * @param jda the JDA instance
     */
    public void registerCommands(JDA jda) {
        LOG.info("Registering slash commands with zh-TW localization...");

        jda.updateCommands().addCommands(
                // /balance - available to all users
                Commands.slash(CMD_BALANCE, "Check your current currency balance")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_BALANCE))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_BALANCE)),

                // /currency-config - admin only
                Commands.slash(CMD_CURRENCY_CONFIG, "Configure the server's currency settings")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_CURRENCY_CONFIG))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_CURRENCY_CONFIG))
                        .addOptions(
                                new OptionData(OptionType.STRING, "name", "The name of the currency (e.g., 'Gold')", false)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("name"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("name")),
                                new OptionData(OptionType.STRING, "icon", "The icon/emoji for the currency (e.g., '💰')", false)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("icon"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("icon"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

                // /adjust-balance - admin only
                Commands.slash(CMD_ADJUST_BALANCE, "Adjust a member's currency balance")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_ADJUST_BALANCE))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_ADJUST_BALANCE))
                        .addOptions(
                                new OptionData(OptionType.STRING, "mode", "The adjustment mode", true)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("mode"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("mode"))
                                        .addChoice("add", "add")
                                        .addChoice("deduct", "deduct")
                                        .addChoice("adjust", "adjust"),
                                new OptionData(OptionType.USER, "member", "The member whose balance to adjust", true)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("member"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("member")),
                                new OptionData(OptionType.INTEGER, "amount", "Amount to add/deduct, or target balance for adjust mode", true)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("amount"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("amount"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

                // /game-token-adjust - admin only
                Commands.slash(CMD_GAME_TOKEN_ADJUST, "Adjust a member's game token balance")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_GAME_TOKEN_ADJUST))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_GAME_TOKEN_ADJUST))
                        .addOptions(
                                new OptionData(OptionType.USER, "member", "The member whose tokens to adjust", true)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("member"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("member")),
                                new OptionData(OptionType.INTEGER, "amount", "Amount to add (positive) or subtract (negative)", true)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("amount"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("amount"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

                // /dice-game-1 - available to all users
                Commands.slash(CMD_DICE_GAME_1, "Play the dice mini-game (costs game tokens)")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_DICE_GAME_1))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_DICE_GAME_1)),

                // /dice-game-1-config - admin only
                Commands.slash(CMD_DICE_GAME_1_CONFIG, "Configure the dice game settings")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_DICE_GAME_1_CONFIG))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_DICE_GAME_1_CONFIG))
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "token-cost", "Number of game tokens required per play", false)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("token-cost"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("token-cost"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

                // /dice-game-2 - available to all users
                Commands.slash(CMD_DICE_GAME_2, "Play the dice game 2 mini-game with straights and triples (costs game tokens)")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_DICE_GAME_2))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_DICE_GAME_2)),

                // /dice-game-2-config - admin only
                Commands.slash(CMD_DICE_GAME_2_CONFIG, "Configure the dice game 2 settings")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_DICE_GAME_2_CONFIG))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_DICE_GAME_2_CONFIG))
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "token-cost", "Number of game tokens required per play", false)
                                        .setNameLocalizations(CommandLocalizations.getOptionNameLocalizations("token-cost"))
                                        .setDescriptionLocalizations(CommandLocalizations.getOptionDescriptionLocalizations("token-cost"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

                // /user-panel - available to all users
                Commands.slash(CMD_USER_PANEL, "View your currency balance, game tokens, and transaction history")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_USER_PANEL))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_USER_PANEL)),

                // /admin-panel - admin only
                Commands.slash(CMD_ADMIN_PANEL, "Manage member balances, game tokens, and game settings")
                        .setNameLocalizations(CommandLocalizations.getNameLocalizations(CMD_ADMIN_PANEL))
                        .setDescriptionLocalizations(CommandLocalizations.getDescriptionLocalizations(CMD_ADMIN_PANEL))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        ).queue(
                commands -> LOG.info("Registered {} slash commands with zh-TW localization", commands.size()),
                error -> LOG.error("Failed to register slash commands", error)
        );
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Only process commands in guilds
        if (!event.isFromGuild()) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        String commandName = event.getName();
        LOG.debug("Received slash command: {} from user={} in guild={}",
                commandName, event.getUser().getIdLong(), event.getGuild().getIdLong());

        // Start metrics tracking
        SlashCommandMetrics.ExecutionContext metricsContext = metrics.recordStart(commandName);
        boolean succeeded = false;

        try {
            switch (commandName) {
                case CMD_BALANCE -> balanceHandler.handle(event);
                case CMD_CURRENCY_CONFIG -> handleWithAdminCheck(event, configHandler);
                case CMD_ADJUST_BALANCE -> handleWithAdminCheck(event, adjustmentHandler);
                case CMD_GAME_TOKEN_ADJUST -> handleWithAdminCheck(event, gameTokenAdjustHandler);
                case CMD_DICE_GAME_1 -> diceGame1Handler.handle(event);
                case CMD_DICE_GAME_1_CONFIG -> handleWithAdminCheck(event, diceGame1ConfigHandler);
                case CMD_DICE_GAME_2 -> diceGame2Handler.handle(event);
                case CMD_DICE_GAME_2_CONFIG -> handleWithAdminCheck(event, diceGame2ConfigHandler);
                case CMD_USER_PANEL -> userPanelHandler.handle(event);
                case CMD_ADMIN_PANEL -> handleWithAdminCheck(event, adminPanelHandler);
                default -> {
                    LOG.warn("Unknown command received: {}", commandName);
                    event.reply("Unknown command.").setEphemeral(true).queue();
                }
            }
            succeeded = true;
        } catch (Exception e) {
            LOG.error("Error handling command: {} for user={} in guild={}",
                    commandName, event.getUser().getIdLong(), event.getGuild().getIdLong(), e);
            BotErrorHandler.handleUnexpectedError(event, e);
        } finally {
            metrics.recordEnd(metricsContext, succeeded);
        }
    }

    private void handleWithAdminCheck(SlashCommandInteractionEvent event, CommandHandler handler) {
        // Double-check admin permissions (Discord should enforce this, but we verify)
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You need Administrator permission to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        handler.handle(event);
    }

    /**
     * Functional interface for command handlers.
     */
    @FunctionalInterface
    public interface CommandHandler {
        void handle(SlashCommandInteractionEvent event);
    }
}
