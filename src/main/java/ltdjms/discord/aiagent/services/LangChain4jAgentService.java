package ltdjms.discord.aiagent.services;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4J AI Agent 服務介面。
 *
 * <p>此介面使用 LangChain4J 的 @AiService 模式定義 AI 服務。 通過 AiServices.builder() 動態創建實現類。
 *
 * <p>支援串流回應，使用 {@link TokenStream} 進行增量輸出。
 */
public interface LangChain4jAgentService {

  /**
   * 與 AI 進行串流對話。
   *
   * <p>LangChain4J 會自動：
   *
   * <ul>
   *   <li>使用 ChatMemoryProvider 加載會話歷史
   *   <li>處理工具調用
   *   <li>管理多輪對話狀態
   *   <li>以串流方式返回回應
   * </ul>
   *
   * @param conversationId 會話 ID（用作 ChatMemory 的 memoryId）
   * @param userMessage 用戶訊息
   * @param parameters 調用參數（用於傳遞 guildId、channelId、userId 等上下文）
   * @return TokenStream 用於接收串流回應
   */
  TokenStream chat(
      @MemoryId String conversationId,
      @UserMessage String userMessage,
      InvocationParameters parameters);
}
