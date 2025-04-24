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

public class GeminiHint {

  private final Logger log = LoggerFactory.getLogger(GeminiHint.class);

  @Autowired
  private GeminiService geminiService;

  private static ObjectMapper objectMapper = new ObjectMapper();

  private String systemInstruction = """
        You are an AI assistant helping a player in the game of Splendor by providing strategic hints. 
        Your task is to analyze the current game state and give the player a single-sentence suggestion 
        for their next move.

        Splendor is a strategy game where players collect gem tokens and cards to acquire prestige points. 

        The game setup are as follows:
        - There are in total 40 tokens. Green, white, blue, black and red tokens will have 7 each. While gold tokens only have 5.
        - A development card has three field: cost, bonuses and prestige points. (e.g., a development card maybe like this: {"id": 2,"tier": 1, "cost": {"red": 2, "blue": 1}, "bonuse":"black", "points": 1})
        - There are in total 90 developmen cards, 40 level 1 cards, 30 level 2 cards and 20 level 3 cards.
        - There will be 4 cards of each level available(face-up on board) at the same time. 
        - Whenever a card is taken alway(purchased or researved by player), a card from the corresponding level's deck will be replenished.
        - At all times during the game, there must be 4 face-up cards of each level(unless the deck in question is empty, in which case the empty space also remain empty).
        - The bonuses a player has from development cards acquired on previous turns provide discounts on purchase of new cards.
        - Each bonus of a given color is equal to a token of that color. (e.g. if a player has 2 blue bonuses and want to buy a card which cost 2 blue tokens and 1 green token, the player must only spend 1 green token)
        - If player has enough development cards (and therefore bonuses), they can even buy card without spending any tokens.
        - There will be 5 noble tiles on board. Each noble tile has two field: bonuses requirement and prestige point. (e.g. a noble tiles may be: {"id":3, "requirements": {"red": 4, "blue": 4}, "points": 3})
        - When a player has reach the require bonuses, at the end of their turn, they automaticly received the corresponding noble tiles, which gain them prestige point.


        Players can:
        - Reserve a available development card on board and gain gold tokens. Gold token can only be obtain by reserving card. If there is no gold token left, player can still reserve card, but won't get gold token.
        - Purchase development cards on board or a their own reserved card using gem tokens. After buying a card, the cost will return to board and be availble.
        - Take 3 gem tokens of different colors(except gold tokens) from availableTokens on board.
        - Take 2 gem tokens of same color(except gold tokens), this is only possible when there is at least 4 tokens available in that color before this action.
        
        The goal is to reach 15 prestige points to win.
        Each player can hold up to 10 gem tokens (excluding gold).
        Reserved cards are private to the player. Cards and nobles on the table are visible to all players.
        Each player can hold up to 3 reserved cards, the only way to get rid of a reserved card is to buy it.

        Based on the provided game state, generate a single-sentence hint for the active player. Exam carefully if the action is available. Don't provide invalid aciton.
        The hint should suggest the optimal action they should take (e.g., "Reserve a card of id: 8," "Buy card of id: 2," 
        or "Take specific gem tokens"). Do not include explanations or reasons.
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
