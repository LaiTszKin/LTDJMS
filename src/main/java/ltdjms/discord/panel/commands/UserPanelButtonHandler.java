package ltdjms.discord.panel.commands;

import ltdjms.discord.gametoken.domain.GameTokenTransaction;
import ltdjms.discord.gametoken.services.GameTokenTransactionService.TransactionPage;
import ltdjms.discord.panel.services.UserPanelService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles button interactions for the user panel.
 * Processes token history viewing and pagination.
 */
public class UserPanelButtonHandler extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(UserPanelButtonHandler.class);

    private static final Color EMBED_COLOR = new Color(0x5865F2);

    // Button ID prefix for pagination
    public static final String BUTTON_PREFIX_HISTORY = "user_panel_token_history";
    public static final String BUTTON_PREFIX_PAGE = "user_panel_page_";
    public static final String BUTTON_BACK_TO_PANEL = "user_panel_back";

    private final UserPanelService userPanelService;

    public UserPanelButtonHandler(UserPanelService userPanelService) {
        this.userPanelService = userPanelService;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        // Only handle our button interactions
        if (!buttonId.startsWith("user_panel_")) {
            return;
        }

        // Only process in guilds
        if (!event.isFromGuild() || event.getGuild() == null) {
            event.reply("此功能只能在伺服器中使用").setEphemeral(true).queue();
            return;
        }

        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        LOG.debug("Processing button interaction: buttonId={}, guildId={}, userId={}",
                buttonId, guildId, userId);

        try {
            if (buttonId.equals(BUTTON_PREFIX_HISTORY)) {
                // Show first page of token history
                showTokenHistoryPage(event, guildId, userId, 1);
            } else if (buttonId.startsWith(BUTTON_PREFIX_PAGE)) {
                // Parse page number from button ID
                String pageStr = buttonId.substring(BUTTON_PREFIX_PAGE.length());
                int page = Integer.parseInt(pageStr);
                showTokenHistoryPage(event, guildId, userId, page);
            } else if (buttonId.equals(BUTTON_BACK_TO_PANEL)) {
                // This would require regenerating the panel, which needs more context
                // For now, just acknowledge
                event.reply("請使用 /user-panel 重新開啟個人面板").setEphemeral(true).queue();
            } else {
                LOG.warn("Unknown button ID: {}", buttonId);
            }
        } catch (NumberFormatException e) {
            LOG.error("Failed to parse page number from button ID: {}", buttonId, e);
            event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
        } catch (Exception e) {
            LOG.error("Error handling button interaction: {}", buttonId, e);
            event.reply("發生錯誤，請稍後再試").setEphemeral(true).queue();
        }
    }

    private void showTokenHistoryPage(ButtonInteractionEvent event, long guildId, long userId, int page) {
        TransactionPage transactionPage = userPanelService.getTokenTransactionPage(guildId, userId, page);

        MessageEmbed embed = buildTransactionHistoryEmbed(transactionPage);
        List<Button> buttons = buildPaginationButtons(transactionPage);

        if (buttons.isEmpty()) {
            event.editMessageEmbeds(embed).setComponents().queue();
        } else {
            event.editMessageEmbeds(embed)
                    .setActionRow(buttons)
                    .queue();
        }

        LOG.debug("Showed token history page {} for guildId={}, userId={}",
                page, guildId, userId);
    }

    private MessageEmbed buildTransactionHistoryEmbed(TransactionPage page) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("📜 遊戲代幣流水")
                .setColor(EMBED_COLOR);

        if (page.isEmpty()) {
            builder.setDescription("目前沒有任何遊戲代幣流水紀錄");
        } else {
            StringBuilder sb = new StringBuilder();
            for (GameTokenTransaction tx : page.transactions()) {
                sb.append(tx.getShortTimestamp())
                        .append(" ")
                        .append(tx.formatForDisplay())
                        .append("\n");
            }
            builder.setDescription(sb.toString());
        }

        builder.setFooter(page.formatPageIndicator());

        return builder.build();
    }

    private List<Button> buildPaginationButtons(TransactionPage page) {
        List<Button> buttons = new ArrayList<>();

        if (page.hasPreviousPage()) {
            buttons.add(Button.secondary(
                    BUTTON_PREFIX_PAGE + (page.currentPage() - 1),
                    "⬅️ 上一頁"
            ));
        }

        if (page.hasNextPage()) {
            buttons.add(Button.secondary(
                    BUTTON_PREFIX_PAGE + (page.currentPage() + 1),
                    "下一頁 ➡️"
            ));
        }

        return buttons;
    }
}
