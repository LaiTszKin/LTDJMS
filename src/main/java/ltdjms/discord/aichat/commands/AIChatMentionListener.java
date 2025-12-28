package ltdjms.discord.aichat.commands;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.aichat.services.AIChatService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/** AI 聊天提及監聽器，處理使用者提及機器人的訊息。 */
public class AIChatMentionListener extends ListenerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AIChatMentionListener.class);

  private final AIChatService aiChatService;

  /**
   * 創建 AIChatMentionListener。
   *
   * @param aiChatService AI 聊天服務
   */
  public AIChatMentionListener(AIChatService aiChatService) {
    this.aiChatService = aiChatService;
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    // Ignore bot messages
    if (event.getAuthor().isBot()) {
      return;
    }

    // Ignore DMs
    if (!event.isFromGuild()) {
      return;
    }

    String message = event.getMessage().getContentRaw();
    String botId = event.getJDA().getSelfUser().getId();
    String botMention = "<@" + botId + ">";
    String botNicknameMention = "<@!" + botId + ">";

    // Check if bot is mentioned
    if (!message.contains(botMention) && !message.contains(botNicknameMention)) {
      return;
    }

    // Remove mention and extract user message
    String originalUserMessage =
        message.replace(botMention, "").replace(botNicknameMention, "").trim();

    // If message is empty, use default greeting
    String userMessage = (originalUserMessage.isBlank()) ? "你好" : originalUserMessage;

    String channelId = event.getChannel().getId();
    String userId = event.getAuthor().getId();
    var channel = event.getChannel();

    LOGGER.info("Bot mentioned by user {} in channel {}: {}", userId, channelId, userMessage);

    // Generate AI response asynchronously
    event
        .getGuild()
        .retrieveOwner()
        .queue(
            owner -> {
              Result<List<String>, DomainError> result =
                  aiChatService.generateResponse(channelId, userId, userMessage);

              if (result.isOk()) {
                for (String reply : result.getValue()) {
                  channel.sendMessage(reply).queue();
                }
                LOGGER.info("AI response sent successfully");
              } else {
                // Error - send user-friendly error message
                DomainError error = result.getError();
                String errorMessage = getErrorMessage(error);
                channel.sendMessage(errorMessage).queue();
                LOGGER.warn("AI response error: {} - {}", error.category(), error.message());
              }
            });
  }

  private String getErrorMessage(DomainError error) {
    return switch (error.category()) {
      case AI_SERVICE_AUTH_FAILED -> ":x: AI 服務認證失敗，請聯絡管理員";
      case AI_SERVICE_RATE_LIMITED -> ":timer: AI 服務暫時忙碌，請稍後再試";
      case AI_SERVICE_TIMEOUT -> ":hourglass: AI 服務連線逾時，請稍後再試";
      case AI_SERVICE_UNAVAILABLE -> ":warning: AI 服務暫時無法使用";
      case AI_RESPONSE_EMPTY -> ":question: AI 沒有產生回應";
      case AI_RESPONSE_INVALID -> ":warning: AI 回應格式錯誤";
      default -> ":warning: 發生錯誤：" + error.message();
    };
  }
}
