package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.service.GeminiService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

public class GeminiHintTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private Game mockGame;

    @InjectMocks
    private GeminiHint geminiHint;

    private Map<GemColor, Long> mockGems;
    private List<Card> mockVisibleLevel1Cards;
    private List<Card> mockVisibleLevel2Cards;
    private List<Card> mockVisibleLevel3Cards;
    private List<Noble> mockVisibleNoble;

    private GameSnapshot mockGameSnapshot;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockGems = new HashMap<>();
        mockGems.put(GemColor.BLACK, 0L);
        mockGems.put(GemColor.BLUE, 0L);
        mockGems.put(GemColor.GREEN, 0L);
        mockGems.put(GemColor.RED, 0L);
        mockGems.put(GemColor.WHITE, 0L);
        mockGems.put(GemColor.GOLD, 0L);

        mockVisibleLevel1Cards = new ArrayList<>();
        mockVisibleLevel2Cards = new ArrayList<>();
        mockVisibleLevel3Cards = new ArrayList<>();
        mockVisibleNoble = new ArrayList<>();

        setUpMockCard();
        setUpMockNoble();

        // mock GameSnapshot
        mockGameSnapshot = new GameSnapshot();
        mockGameSnapshot.setGameId("test-game");
        mockGameSnapshot.setCurrentPlayerIndex(0);
        mockGameSnapshot.setCurrentRound(1);
        mockGameSnapshot.setAvailableGems(mockGems);
        mockGameSnapshot.setVisibleLevel1cardIds(mockVisibleLevel1Cards);
        mockGameSnapshot.setVisibleLevel2cardIds(mockVisibleLevel2Cards);
        mockGameSnapshot.setVisibleLevel3cardIds(mockVisibleLevel3Cards);
        mockGameSnapshot.setVisibleNobleIds(mockVisibleNoble);
        mockGameSnapshot.setRoomName("test-gameRoom");

        // mock Game的行为
        given(mockGame.getGameInformation()).willReturn(mockGameSnapshot);
        given(mockGame.getVisibleLevel1Cards()).willReturn(mockVisibleLevel1Cards);
        given(mockGame.getVisibleLevel2Cards()).willReturn(mockVisibleLevel2Cards);
        given(mockGame.getVisibleLevel3Cards()).willReturn(mockVisibleLevel3Cards);
        given(mockGame.getVisibleNoble()).willReturn(mockVisibleNoble);
    }

  @Test
  public void generateSplendorHint_success() {
    String aiHintText = "Buy card of id: 2.";
    String mockJson = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                { "text": "Buy card of id: 2." }
              ],
              "role": "model"
            },
            "finishReason": "STOP"
          }
        ]
      }
      """;
    given(geminiService.getGeminiHint(anyString(), anyString())).willReturn(mockJson);

    String result = geminiHint.generateSplendorHint(mockGame);

    assertEquals(aiHintText, result);
  }

  @Test
  public void generateSplendorHint_geminiServiceReturnsNull() throws Exception {
    given(geminiService.getGeminiHint(anyString(), anyString())).willReturn(null);

    String result = geminiHint.generateSplendorHint(mockGame);

    assertTrue(
      result.contains("处理 Gemini 响应时发生意外错误")
          || result.contains("无法从解析后的响应中提取文本")
          || result.contains("无效或错误的 JSON 响应字符串")
    );
  }

  @Test
  public void generateSplendorHint_geminiServiceThrowsException() throws Exception {
    given(geminiService.getGeminiHint(anyString(), anyString())).willThrow(new RuntimeException("mock error"));

    String result = geminiHint.generateSplendorHint(mockGame);

    assertTrue(
      result.contains("处理 Gemini 响应时发生意外错误")
          || result.contains("无法从解析后的响应中提取文本")
          || result.contains("无效或错误的 JSON 响应字符串")
    );
  }

  // mock卡牌和贵族
  private void setUpMockCard() {
    mockVisibleLevel1Cards.add(new Card(
            1L, 1, GemColor.BLUE, 0L,
            Map.of(GemColor.BLACK, 3L)
    ));
    mockVisibleLevel1Cards.add(new Card(
            2L, 1, GemColor.BLUE, 1L,
            Map.of(GemColor.RED, 4L)
    ));
    mockVisibleLevel1Cards.add(new Card(
            3L, 1, GemColor.BLUE, 0L,
            Map.of(GemColor.WHITE, 1L, GemColor.BLACK, 2L)
    ));
    mockVisibleLevel1Cards.add(new Card(
            4L, 1, GemColor.BLUE, 0L,
            Map.of(GemColor.GREEN, 2L, GemColor.BLACK, 2L)
    ));

    mockVisibleLevel2Cards.add(new Card(41L, 2, GemColor.BLUE, 1L,
            Map.of(GemColor.BLUE, 2L, GemColor.GREEN, 2L, GemColor.RED, 3L)));
    mockVisibleLevel2Cards.add(new Card(51L, 2, GemColor.RED, 3L,
            Map.of(GemColor.RED, 6L)));
    mockVisibleLevel2Cards.add(new Card(62L, 2, GemColor.BLACK, 2L,
            Map.of(GemColor.WHITE, 5L)));
    mockVisibleLevel2Cards.add(new Card(81L, 2, GemColor.WHITE, 3L,
            Map.of(GemColor.WHITE, 6L)));

    mockVisibleLevel3Cards.add(new Card(47L, 3, GemColor.BLUE, 4L,
            Map.of(GemColor.WHITE, 7L)));
    mockVisibleLevel3Cards.add(new Card(57L, 3, GemColor.RED, 4L,
            Map.of(GemColor.GREEN, 7L)));
    mockVisibleLevel3Cards.add(new Card(67L, 3, GemColor.BLACK, 4L,
            Map.of(GemColor.RED, 7L)));
    mockVisibleLevel3Cards.add(new Card(87L, 3, GemColor.WHITE, 4L,
            Map.of(GemColor.BLACK, 7L)));
  }

  private void setUpMockNoble() {
    mockVisibleNoble.add(new Noble(1L, 3L, Map.of(
            GemColor.BLUE, 4L,
            GemColor.GREEN, 4L
    )));
    mockVisibleNoble.add(new Noble(3L, 3L, Map.of(
            GemColor.GREEN, 3L,
            GemColor.BLUE, 3L,
            GemColor.RED, 3L
    )));
    mockVisibleNoble.add(new Noble(4L, 3L, Map.of(
            GemColor.BLACK, 3L,
            GemColor.RED, 3L,
            GemColor.WHITE, 3L
    )));
    mockVisibleNoble.add(new Noble(6L, 3L, Map.of(
            GemColor.BLUE, 4L,
            GemColor.WHITE, 4L
    )));
    mockVisibleNoble.add(new Noble(9L, 3L, Map.of(
            GemColor.BLACK, 3L,
            GemColor.BLUE, 3L,
            GemColor.WHITE, 3L
    )));
  }
}