package ltdjms.discord.panel.commands;

import ltdjms.discord.panel.services.AdminPanelService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

/**
 * Handles button and modal interactions for the admin panel.
 */
public class AdminPanelButtonHandler extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AdminPanelButtonHandler.class);

    private static final Color EMBED_COLOR = new Color(0xED4245);

    // Button IDs
    public static final String BUTTON_BALANCE = "admin_panel_balance";
    public static final String BUTTON_TOKENS = "admin_panel_tokens";
    public static final String BUTTON_GAMES = "admin_panel_games";
    public static final String BUTTON_BACK = "admin_panel_back";

    // Modal IDs
    public static final String MODAL_BALANCE_ADJUST = "admin_modal_balance_adjust";
    public static final String MODAL_TOKEN_ADJUST = "admin_modal_token_adjust";
    public static final String MODAL_GAME_CONFIG = "admin_modal_game_config";

    // Select Menu IDs
    public static final String SELECT_GAME = "admin_select_game";

    private final AdminPanelService adminPanelService;

    public AdminPanelButtonHandler(AdminPanelService adminPanelService) {
        this.adminPanelService = adminPanelService;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (!buttonId.startsWith("admin_panel_")) {
            return;
        }

        if (!event.isFromGuild() || event.getGuild() == null) {
            event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
            return;
        }

        LOG.debug("Processing admin panel button: buttonId={}, userId={}",
                buttonId, event.getUser().getIdLong());

        try {
            switch (buttonId) {
                case BUTTON_BALANCE -> showBalanceManagement(event);
                case BUTTON_TOKENS -> showTokenManagement(event);
                case BUTTON_GAMES -> showGameManagement(event);
                case BUTTON_BACK -> showMainPanel(event);
                default -> LOG.warn("Unknown admin panel button: {}", buttonId);
            }
        } catch (Exception e) {
            LOG.error("Error handling admin panel button: {}", buttonId, e);
            event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String selectId = event.getComponentId();

        if (!selectId.equals(SELECT_GAME)) {
            return;
        }

        String gameType = event.getValues().get(0);
        long guildId = event.getGuild().getIdLong();

        long currentCost = adminPanelService.getGameTokenCost(guildId, gameType);

        TextInput costInput = TextInput.create("new_cost", "新的代幣消耗數量", TextInputStyle.SHORT)
                .setPlaceholder("輸入新的代幣消耗數量")
                .setValue(String.valueOf(currentCost))
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(10)
                .build();

        Modal modal = Modal.create(MODAL_GAME_CONFIG + ":" + gameType, "調整遊戲代幣消耗")
                .addComponents(ActionRow.of(costInput))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (modalId.startsWith(MODAL_BALANCE_ADJUST)) {
            handleBalanceAdjustModal(event);
        } else if (modalId.startsWith(MODAL_TOKEN_ADJUST)) {
            handleTokenAdjustModal(event);
        } else if (modalId.startsWith(MODAL_GAME_CONFIG)) {
            handleGameConfigModal(event);
        }
    }

    private void showBalanceManagement(ButtonInteractionEvent event) {
        TextInput userIdInput = TextInput.create("user_id", "使用者 ID", TextInputStyle.SHORT)
                .setPlaceholder("輸入要調整的使用者 ID")
                .setRequired(true)
                .setMinLength(15)
                .setMaxLength(20)
                .build();

        TextInput modeInput = TextInput.create("mode", "調整模式", TextInputStyle.SHORT)
                .setPlaceholder("add（增加）、deduct（扣除）或 adjust（設定）")
                .setRequired(true)
                .setMinLength(3)
                .setMaxLength(6)
                .build();

        TextInput amountInput = TextInput.create("amount", "金額", TextInputStyle.SHORT)
                .setPlaceholder("輸入調整金額或目標餘額")
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(15)
                .build();

        Modal modal = Modal.create(MODAL_BALANCE_ADJUST, "調整使用者餘額")
                .addComponents(
                        ActionRow.of(userIdInput),
                        ActionRow.of(modeInput),
                        ActionRow.of(amountInput)
                )
                .build();

        event.replyModal(modal).queue();
    }

    private void showTokenManagement(ButtonInteractionEvent event) {
        TextInput userIdInput = TextInput.create("user_id", "使用者 ID", TextInputStyle.SHORT)
                .setPlaceholder("輸入要調整的使用者 ID")
                .setRequired(true)
                .setMinLength(15)
                .setMaxLength(20)
                .build();

        TextInput amountInput = TextInput.create("amount", "調整數量", TextInputStyle.SHORT)
                .setPlaceholder("正數增加，負數扣除")
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(15)
                .build();

        Modal modal = Modal.create(MODAL_TOKEN_ADJUST, "調整遊戲代幣")
                .addComponents(
                        ActionRow.of(userIdInput),
                        ActionRow.of(amountInput)
                )
                .build();

        event.replyModal(modal).queue();
    }

    private void showGameManagement(ButtonInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();

        long diceGame1Cost = adminPanelService.getGameTokenCost(guildId, "dice-game-1");
        long diceGame2Cost = adminPanelService.getGameTokenCost(guildId, "dice-game-2");

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("🎲 遊戲設定管理")
                .setColor(EMBED_COLOR)
                .setDescription("選擇要調整的遊戲：")
                .addField("骰子遊戲 1", String.format("目前代幣消耗：🎮 %,d", diceGame1Cost), false)
                .addField("骰子遊戲 2", String.format("目前代幣消耗：🎮 %,d", diceGame2Cost), false)
                .build();

        StringSelectMenu selectMenu = StringSelectMenu.create(SELECT_GAME)
                .setPlaceholder("選擇遊戲")
                .addOption("骰子遊戲 1", "dice-game-1", "調整骰子遊戲 1 的代幣消耗")
                .addOption("骰子遊戲 2", "dice-game-2", "調整骰子遊戲 2 的代幣消耗")
                .build();

        event.editMessageEmbeds(embed)
                .setComponents(
                        ActionRow.of(selectMenu),
                        ActionRow.of(Button.secondary(BUTTON_BACK, "⬅️ 返回主選單"))
                )
                .queue();
    }

    private void showMainPanel(ButtonInteractionEvent event) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("🔧 管理面板")
                .setDescription("選擇要管理的項目：")
                .setColor(EMBED_COLOR)
                .addField("💰 使用者餘額管理", "調整成員的貨幣餘額", false)
                .addField("🎮 遊戲代幣管理", "調整成員的遊戲代幣餘額", false)
                .addField("🎲 遊戲設定管理", "調整遊戲的代幣消耗設定", false)
                .setFooter("點擊下方按鈕進入對應功能")
                .build();

        event.editMessageEmbeds(embed)
                .setComponents(ActionRow.of(
                        Button.primary(BUTTON_BALANCE, "💰 使用者餘額管理"),
                        Button.primary(BUTTON_TOKENS, "🎮 遊戲代幣管理"),
                        Button.primary(BUTTON_GAMES, "🎲 遊戲設定管理")
                ))
                .queue();
    }

    private void handleBalanceAdjustModal(ModalInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();

        String userIdStr = event.getValue("user_id").getAsString().trim();
        String mode = event.getValue("mode").getAsString().trim().toLowerCase();
        String amountStr = event.getValue("amount").getAsString().trim();

        long userId;
        long amount;
        try {
            userId = Long.parseLong(userIdStr);
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            event.reply("輸入格式錯誤，請確認使用者 ID 和金額為有效數字").setEphemeral(true).queue();
            return;
        }

        if (!mode.equals("add") && !mode.equals("deduct") && !mode.equals("adjust")) {
            event.reply("調整模式必須為 add、deduct 或 adjust").setEphemeral(true).queue();
            return;
        }

        Result<AdminPanelService.BalanceAdjustmentResult, DomainError> result =
                adminPanelService.adjustBalance(guildId, userId, mode, amount);

        if (result.isErr()) {
            event.reply("調整失敗：" + result.getError().message()).setEphemeral(true).queue();
            return;
        }

        AdminPanelService.BalanceAdjustmentResult adjustResult = result.getValue();
        event.reply(String.format(
                "✅ 餘額調整成功！\n<@%d> 的餘額：\n調整前：%,d\n調整後：%,d",
                userId, adjustResult.previousBalance(), adjustResult.newBalance()
        )).setEphemeral(true).queue();
    }

    private void handleTokenAdjustModal(ModalInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();

        String userIdStr = event.getValue("user_id").getAsString().trim();
        String amountStr = event.getValue("amount").getAsString().trim();

        long userId;
        long amount;
        try {
            userId = Long.parseLong(userIdStr);
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            event.reply("輸入格式錯誤，請確認使用者 ID 和數量為有效數字").setEphemeral(true).queue();
            return;
        }

        Result<AdminPanelService.TokenAdjustmentResult, DomainError> result =
                adminPanelService.adjustTokens(guildId, userId, amount);

        if (result.isErr()) {
            event.reply("調整失敗：" + result.getError().message()).setEphemeral(true).queue();
            return;
        }

        AdminPanelService.TokenAdjustmentResult adjustResult = result.getValue();
        event.reply(String.format(
                "✅ 遊戲代幣調整成功！\n<@%d> 的代幣：\n調整前：🎮 %,d\n調整後：🎮 %,d",
                userId, adjustResult.previousTokens(), adjustResult.newTokens()
        )).setEphemeral(true).queue();
    }

    private void handleGameConfigModal(ModalInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        String gameType = event.getModalId().split(":")[1];

        String costStr = event.getValue("new_cost").getAsString().trim();

        long newCost;
        try {
            newCost = Long.parseLong(costStr);
        } catch (NumberFormatException e) {
            event.reply("輸入格式錯誤，請確認代幣消耗為有效數字").setEphemeral(true).queue();
            return;
        }

        Result<AdminPanelService.GameConfigUpdateResult, DomainError> result =
                adminPanelService.updateGameTokenCost(guildId, gameType, newCost);

        if (result.isErr()) {
            event.reply("更新失敗：" + result.getError().message()).setEphemeral(true).queue();
            return;
        }

        AdminPanelService.GameConfigUpdateResult updateResult = result.getValue();
        event.reply(updateResult.formatMessage()).setEphemeral(true).queue();
    }
}
