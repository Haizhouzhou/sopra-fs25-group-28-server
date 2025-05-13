package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GeminiAPIResponse;
import ch.uzh.ifi.hase.soprafs24.service.GeminiService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import org.springframework.stereotype.Component;

@Component
public class GeminiHint {

  private final Logger log = LoggerFactory.getLogger(GeminiHint.class);

  @Autowired
  private GeminiService geminiService;

  private static ObjectMapper objectMapper = new ObjectMapper();

  private String systemInstruction = """
        You are an AI assistant helping a player in the game of Splendor by providing strategic hints.

        Your task:
        - Analyze the current game state.
        - Suggest the optimal single action for the current player in one short sentence.
        - Only output the action itself (e.g., "Reserve card of id: 8.", "Buy card of id: 2.", "Take 3 different gem tokens: RED, GREEN, BLACK.").
        - Do NOT include explanations or reasons.
        - Only suggest actions that are valid and allowed by the current game state.

        Splendor game rules:

        - The goal is to reach 15 prestige points to win.
        - There are 40 tokens in total: 7 each of green, white, blue, black, red; 5 gold tokens.
        - Players collect gem tokens and development cards to acquire prestige points.
        - A development card has three fields: cost, bonus color, and prestige points.
          (Example: {"id": 2, "tier": 1, "cost": {"red": 2, "blue": 1}, "bonus": "black", "points": 1})
        - There are 90 development cards in total: 40 level 1, 30 level 2, and 20 level 3 cards.
        - At all times (unless the deck is empty), there are 4 face-up cards from each level available on the board.
        - When a card is purchased or reserved, a new card from the corresponding deck is immediately revealed (if available).
        - Bonuses from purchased development cards provide permanent discounts for buying new cards. Each bonus acts as a token of that color.
          (Example: If a player has 2 blue bonuses and wants to buy a card costing 2 blue tokens and 1 green token, only the green token must be paid.)
        - If a player has sufficient bonuses, cards can be purchased without spending any tokens.

        - There are 5 noble tiles on the board. Each noble tile has a bonus requirement and prestige points.
          (Example: {"id": 3, "requirements": {"red": 4, "blue": 4}, "points": 3})
        - When a player meets a noble's bonus requirements at the end of their turn, they automatically receive the noble and its points.

        Players can:
        - Reserve any available face-up development card and gain a gold token (if any remain). If there are no gold tokens left, still reserve the card but do not gain a gold token.
        - Purchase development cards from the board or from their own reserved cards using gem tokens and bonuses.
        - Take 3 gem tokens of different colors (except gold) from those available on the board.
        - Take 2 gem tokens of the same color (except gold), only if at least 4 tokens of that color are available before the action.

        Other rules:
        - Each player can hold up to 10 gem tokens (including gold).
        - Each player can have up to 3 reserved cards. The only way to remove a reserved card is to buy it.
        - Reserved cards are private to the player; cards and nobles on the table are visible to all players.

        Instructions:
        - Carefully check that the suggested action is valid in the current game state.
        - Only output a single action sentence, with one sentence of short concise explanation or reason, do not include other extra text.
        - Example outputs:
          Reserve card of id: 8.
          Buy card of id: 2.
          Take 3 different gem tokens: RED, GREEN, BLACK.
        """;
      //Do not include explanations or reasons.
      //And give one sentense of explanation.

  public String generateSplendorHint(Game game){

    GameSnapshot snapshot = game.getGameInformation();
    Map<String, Object> extendedSnapshot = objectMapper.convertValue(snapshot, new TypeReference<Map<String, Object>>() {});
    extendedSnapshot.put("visibleLevel1cards", game.getVisibleLevel1Cards());
    extendedSnapshot.put("visibleLevel2cards", game.getVisibleLevel2Cards());
    extendedSnapshot.put("visibleLevel3cards", game.getVisibleLevel3Cards());
    extendedSnapshot.put("visibleNobles", game.getVisibleNoble());

    String currentGameState = null;
    try {
      currentGameState = objectMapper.writeValueAsString(extendedSnapshot);
    } catch (Exception e) {
      log.error("error when serializing gameSnapshot : {}", e);
    }
    
    String userPrompt = "Game State:\n" + currentGameState;
    log.info("used prompt(include game state): {}", userPrompt);

    String rawJsonResponse = null;
    try {
      rawJsonResponse = geminiService.getGeminiHint(userPrompt, systemInstruction);
    } catch (Exception e) {
      log.error("调用 Gemini API 时发生异常: ", e);
    }
    if(rawJsonResponse == null || rawJsonResponse.isEmpty() || rawJsonResponse.startsWith("Error:")){
      log.warn("无效或错误的 JSON 响应字符串: {}", rawJsonResponse);
    }

    try {
      GeminiAPIResponse response = objectMapper.readValue(rawJsonResponse, GeminiAPIResponse.class);
      String extractedText = response.getFirstCandidateText();
      if (extractedText != null) {
        log.info("成功解析并提取文本: {}", extractedText);
        return extractedText;
      } else {
        log.warn("无法从解析后的响应中提取文本 (可能 finishReason 不是 STOP 或结构不匹配)。");
        return "无法从解析后的响应中提取文本 (可能 finishReason 不是 STOP 或结构不匹配)。";
      }
    } catch (Exception e) {
      log.error("处理 Gemini 响应时发生意外错误: {}", e.getMessage(), e);
      return "处理 Gemini 响应时发生意外错误";
    }
  }

}
