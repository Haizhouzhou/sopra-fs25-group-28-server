package ch.uzh.ifi.hase.soprafs24.websocket.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class GameBuyCardTest {

  @Mock
  private Player mockPlayer;

  @Mock
  private Player mockOtherPlayer;

  @Mock
  private GameRoom mockGameRoom;

  // @InjectMocks
  private Game game;

  // test data
  private Card cardAbleToBuy;
  private Card cardAbleToBuyReserved;
  private Card cardNotAbleToBuy;
  private Map<GemColor, Long> mockPlayerGems;
  private Map<GemColor, Long> mockPlayerBonus;

  // varibale used for instantiate Game manually
  private String testRoomId = "test-buy-room";
  private Set<Player> initialPlayers;

  private Long affordableCardId = 1L;
  private Long unaffordableCardId = 21L;
  private Long affordableReservedCardId = 41L;

  // Game state setup
  private Map<GemColor, Long> mockAvailableGems;
  private List<Card> visibleLevel1Cards;
  private List<Card> visibleLevel2Cards;
  private List<Card> visibleLevel3Cards;
  private Stack<Card> level1Deck;
  private Stack<Card> level2Deck;
  private Stack<Card> level3Deck;
  private List<Player> playersInGame;

  @BeforeEach
  private void setUp(){
    MockitoAnnotations.openMocks(this);

    // card setup
    cardAbleToBuy = new Card(affordableCardId, 1, GemColor.BLUE, 1L, Map.of(GemColor.BLACK, 2L, GemColor.RED, 2L));

    cardNotAbleToBuy = new Card(unaffordableCardId, 3, GemColor.RED, 3L, Map.of(GemColor.WHITE, 4L, GemColor.GREEN, 4L));

    cardAbleToBuyReserved = new Card(affordableReservedCardId, 1, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 2L, GemColor.RED, 2L));

    // instantiate game
    initialPlayers = new HashSet<Player>();
    initialPlayers.add(mockPlayer);
    initialPlayers.add(mockOtherPlayer);
    given(mockGameRoom.getRoomId()).willReturn(testRoomId);
    game = new Game(mockGameRoom, testRoomId, initialPlayers);

    Map<GemColor, Long> initialAvailableGems = Map.of(GemColor.BLACK, 6L,
                            GemColor.RED, 6L,
                            GemColor.WHITE, 7L,
                            GemColor.GREEN, 7L,
                            GemColor.BLUE, 7L,
                            GemColor.GOLD, 4L
    );
    mockAvailableGems = new HashMap<>(initialAvailableGems);
    ReflectionTestUtils.setField(game, "availableGems", mockAvailableGems);
    
    Card level1Card2 = new Card(2L, 1, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    Card level1Card3 = new Card(3L, 1, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    Card level1Card4 = new Card(4L, 1, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    visibleLevel1Cards = new ArrayList<>();
    visibleLevel1Cards.add(cardAbleToBuy);
    visibleLevel1Cards.add(level1Card2);
    visibleLevel1Cards.add(level1Card3);
    visibleLevel1Cards.add(level1Card4);
    ReflectionTestUtils.setField(game, "visibleLevel1Cards", visibleLevel1Cards);

    Card level2Card1 = new Card(11L, 2, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 2L, GemColor.RED, 2L));
    Card level2Card2 = new Card(12L, 2, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    Card level2Card3 = new Card(13L, 2, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    Card level2Card4 = new Card(14L, 2, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    visibleLevel2Cards = new ArrayList<>();
    visibleLevel2Cards.add(level2Card1);
    visibleLevel2Cards.add(level2Card2);
    visibleLevel2Cards.add(level2Card3);
    visibleLevel2Cards.add(level2Card4);
    ReflectionTestUtils.setField(game, "visibleLevel2Cards", visibleLevel2Cards);

    Card level3Card2 = new Card(22L, 3, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    Card level3Card3 = new Card(23L, 3, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    Card level3Card4 = new Card(24L, 3, GemColor.BLUE, 0L, Map.of(GemColor.BLACK, 3L, GemColor.RED, 1L));
    visibleLevel3Cards = new ArrayList<>();
    visibleLevel3Cards.add(cardNotAbleToBuy);
    visibleLevel3Cards.add(level3Card2);
    visibleLevel3Cards.add(level3Card3);
    visibleLevel3Cards.add(level3Card4);
    ReflectionTestUtils.setField(game, "visibleLevel3Cards", visibleLevel3Cards);

    // card deck to test refilling
    level1Deck = new Stack<>();
    level1Deck.push(new Card(31L, 1, GemColor.BLACK, 0L, Map.of(GemColor.WHITE, 2L)));
    ReflectionTestUtils.setField(game, "level1Deck", level1Deck);
    
    game.setGameState(Game.GameState.RUNNING);
    ReflectionTestUtils.setField(game, "currentPlayer", game.getPlayers().indexOf(mockPlayer));

    // mock player setup
    given(mockPlayer.getGem(GemColor.BLACK)).willReturn(1L);
    given(mockPlayer.getGem(GemColor.RED)).willReturn(1L);
    given(mockPlayer.getGem(GemColor.WHITE)).willReturn(0L);
    given(mockPlayer.getGem(GemColor.GREEN)).willReturn(0L);
    given(mockPlayer.getGem(GemColor.BLUE)).willReturn(0L);
    given(mockPlayer.getGem(GemColor.GOLD)).willReturn(1L);
    
    given(mockPlayer.getBonusGem(GemColor.BLACK)).willReturn(1L);
    given(mockPlayer.getBonusGem(GemColor.RED)).willReturn(0L);
    given(mockPlayer.getBonusGem(GemColor.WHITE)).willReturn(0L);
    given(mockPlayer.getBonusGem(GemColor.GREEN)).willReturn(0L);
    given(mockPlayer.getBonusGem(GemColor.BLUE)).willReturn(0L);

    mockPlayerGems = Map.of(GemColor.BLACK, 1L,
                            GemColor.RED, 1L,
                            GemColor.WHITE, 0L,
                            GemColor.GREEN, 0L,
                            GemColor.BLUE, 0L,
                            GemColor.GOLD, 1L
    );
    mockPlayerBonus = Map.of(GemColor.BLACK, 1L,
                            GemColor.RED, 0L,
                            GemColor.WHITE, 0L,
                            GemColor.GREEN, 0L,
                            GemColor.BLUE, 0L
    );

    given(mockPlayer.getUserId()).willReturn(1L);
    given(mockPlayer.getVictoryPoints()).willReturn(3L);
    given(mockPlayer.getAllGems()).willReturn(mockPlayerGems);
    given(mockPlayer.getAllBonusGems()).willReturn(mockPlayerBonus);

    List<Card> mockReservedCards = new ArrayList<>();
    mockReservedCards.add(cardAbleToBuyReserved);
    given(mockPlayer.getReservedCards()).willReturn(mockReservedCards);

    // 关键：让removeCardFromReserved真的移除集合
    org.mockito.Mockito.doAnswer(invocation -> {
      Card card = invocation.getArgument(0);
      return mockReservedCards.remove(card);
    }).when(mockPlayer).removeCardFromReserved(any(Card.class));
  }

  @Test
  public void canBuyCard_test(){
    // arrange, done in setup@BeforeEach
    
    // act
    boolean expectTrue = game.canBuyCard(mockPlayer, cardAbleToBuy);
    boolean expectFalse = game.canBuyCard(mockPlayer, cardNotAbleToBuy);

    // assert
    assertTrue(expectTrue, "canBuyCard未正确处理能买卡的情况");
    assertFalse(expectFalse, "canBuyCard未正确处理不能买卡的情况");

  }
  
  @Test
  public void buyCard_Success(){
    // arrange
    Long cardIdToBuy = affordableCardId;
    GemColor cardColor = cardAbleToBuy.getColor();

    Long expectedPlayerBlackGems = 0L;
    Long expectedPlayerRedGems = 0L;
    Long expectedPlayerGoldGems = 0L;

    Long expectedPlayerBlueBonus = mockPlayerBonus.get(cardColor) + 1L;

    Long expectedPlayerPoints = mockPlayer.getVictoryPoints() + cardAbleToBuy.getPoints();

    Long expectedAvailableBlackGems = mockAvailableGems.get(GemColor.BLACK) + 1L;
    Long expectedAvailableRedGems = mockAvailableGems.get(GemColor.RED) + 1L;
    Long expectedAvailableGoldGems = mockAvailableGems.get(GemColor.GOLD) + 1L;

    // act
    boolean result = game.buyCard(mockPlayer, cardIdToBuy);

    // assert
    assertTrue(result);

    // Verify player gem deductions (using ArgumentCaptor)
    ArgumentCaptor<Long> blackGemCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> redGemCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> goldGemCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockPlayer).setGem(eq(GemColor.BLACK), blackGemCaptor.capture());
    verify(mockPlayer).setGem(eq(GemColor.RED), redGemCaptor.capture());
    verify(mockPlayer).setGem(eq(GemColor.GOLD), goldGemCaptor.capture());
    // Verify other colors were NOT changed (optional but good)
    verify(mockPlayer, never()).setGem(eq(GemColor.WHITE), anyLong());
    verify(mockPlayer, never()).setGem(eq(GemColor.GREEN), anyLong());
    verify(mockPlayer, never()).setGem(eq(GemColor.BLUE), anyLong());

    assertEquals(expectedPlayerBlackGems, blackGemCaptor.getValue(), "Incorrect black gems left");
    assertEquals(expectedPlayerRedGems, redGemCaptor.getValue(), "Incorrect red gems left");
    assertEquals(expectedPlayerGoldGems, goldGemCaptor.getValue(), "Incorrect gold gems left");


    // Verify board gem additions (check map state directly)
    assertEquals(expectedAvailableBlackGems, mockAvailableGems.get(GemColor.BLACK), "Incorrect board black gems");
    assertEquals(expectedAvailableRedGems, mockAvailableGems.get(GemColor.RED), "Incorrect board red gems");
    assertEquals(expectedAvailableGoldGems, mockAvailableGems.get(GemColor.GOLD), "Incorrect board gold gems");

    // Verify player bonus increase
    ArgumentCaptor<Long> bonusCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockPlayer).setBonusGem(eq(cardColor), bonusCaptor.capture());
    assertEquals(expectedPlayerBlueBonus, bonusCaptor.getValue(), "Incorrect player bonus gems");

    // Verify player points increase
    ArgumentCaptor<Long> pointsCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockPlayer).setVictoryPoints(pointsCaptor.capture());
    assertEquals(expectedPlayerPoints, pointsCaptor.getValue(), "Incorrect player victory points");

    // Verify card removed from visible list and refilled (check list state)
    assertNotEquals(cardAbleToBuy, visibleLevel1Cards.get(0), "Original card should be gone");
    // Since deck had a card, it should have refilled
    assertNotNull(visibleLevel1Cards.get(0), "Visible slot should be refilled from deck");
    assertEquals(31L, visibleLevel1Cards.get(0).getId(), "Incorrect card refilled");
  }

  @Test
  public void buyCard_Success_fromReservedCard(){
    // arrange
    Long cardIdToBuy = affordableReservedCardId;
    GemColor cardColor = cardAbleToBuyReserved.getColor(); 

    Long expectedPlayerBlackGems = 0L;
    Long expectedPlayerRedGems = 0L;
    Long expectedPlayerGoldGems = 0L;

    Long expectedPlayerBlueBonus = mockPlayerBonus.get(cardColor) + 1L;

    Long expectedPlayerPoints = mockPlayer.getVictoryPoints() + cardAbleToBuyReserved.getPoints();

    Long expectedAvailableBlackGems = mockAvailableGems.get(GemColor.BLACK) + 1L;
    Long expectedAvailableRedGems = mockAvailableGems.get(GemColor.RED) + 1L;
    Long expectedAvailableGoldGems = mockAvailableGems.get(GemColor.GOLD) + 1L;

    // Get the modifiable list returned by the mock
    List<Card> playerReservedCards = mockPlayer.getReservedCards();
    assertTrue(playerReservedCards.contains(cardAbleToBuyReserved), "Precondition: Card should be in reserved list");


    // act
    boolean result = game.buyCard(mockPlayer, cardIdToBuy);

    // assert
    assertTrue(result);

    // Verify player gem deductions (using ArgumentCaptor)
    ArgumentCaptor<Long> blackGemCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> redGemCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> goldGemCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockPlayer).setGem(eq(GemColor.BLACK), blackGemCaptor.capture());
    verify(mockPlayer).setGem(eq(GemColor.RED), redGemCaptor.capture());
    verify(mockPlayer).setGem(eq(GemColor.GOLD), goldGemCaptor.capture());
    // Verify other colors were NOT changed (optional but good)
    verify(mockPlayer, never()).setGem(eq(GemColor.WHITE), anyLong());
    verify(mockPlayer, never()).setGem(eq(GemColor.GREEN), anyLong());
    verify(mockPlayer, never()).setGem(eq(GemColor.BLUE), anyLong());

    assertEquals(expectedPlayerBlackGems, blackGemCaptor.getValue(), "Incorrect black gems left");
    assertEquals(expectedPlayerRedGems, redGemCaptor.getValue(), "Incorrect red gems left");
    assertEquals(expectedPlayerGoldGems, goldGemCaptor.getValue(), "Incorrect gold gems left");

    // Verify board gem additions
    assertEquals(expectedAvailableBlackGems, mockAvailableGems.get(GemColor.BLACK), "Incorrect board black gems");
    assertEquals(expectedAvailableRedGems, mockAvailableGems.get(GemColor.RED), "Incorrect board red gems");
    assertEquals(expectedAvailableGoldGems, mockAvailableGems.get(GemColor.GOLD), "Incorrect board gold gems");

    // Verify player bonus increase
    ArgumentCaptor<Long> bonusCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockPlayer).setBonusGem(eq(cardColor), bonusCaptor.capture()); // Card color is BLUE
    assertEquals(expectedPlayerBlueBonus, bonusCaptor.getValue(), "Incorrect player bonus gems");

    // Verify player points increase
    ArgumentCaptor<Long> pointsCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockPlayer).setVictoryPoints(pointsCaptor.capture());
    assertEquals(expectedPlayerPoints, pointsCaptor.getValue(), "Incorrect player victory points");

    // Verify card removed from reserved list
     assertFalse(playerReservedCards.contains(cardAbleToBuyReserved), "Card should be removed from reserved list");
  }

  @Test
  public void buyCard_Fail_GameNotRunning(){
    // arrange
    game.setGameState(Game.GameState.FINISHED);
    Long cardIdToBuy = affordableCardId;

    // act
    boolean result = game.buyCard(mockPlayer, cardIdToBuy);

    // assert
    assertFalse(result);
    verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
  }

  @Test
  public void buyCard_Fail_CardNotFound(){
    //arrange
    Long nonExistentCardId = 1000L;

    // act
    boolean result = game.buyCard(mockPlayer, nonExistentCardId);

    // assert
    assertFalse(result);
    verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
  }

  @Test
  public void buyCard_Fail_CannotAfford(){
    // arrange
    Long cardIdToBuy = unaffordableCardId;

    // act
    boolean result = game.buyCard(mockPlayer, cardIdToBuy);

    // assert
    assertFalse(result);
    verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
  }

  @Test
  public void buyCard_Fail_NotPlayerTurn() {
    // arrange
    ReflectionTestUtils.setField(game, "currentPlayer", game.getPlayers().indexOf(mockOtherPlayer)); // Set current player to someone else
    Long cardIdToBuy = affordableCardId;

    // act
    boolean result = game.buyCard(mockPlayer, cardIdToBuy);

    // Assert
    assertFalse(result, "buyCard should return false if it's not the player's turn");
    // Verify no state-changing methods were called on the player
    verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
    verify(mockPlayer, never()).setBonusGem(any(GemColor.class), anyLong());
    verify(mockPlayer, never()).setVictoryPoints(anyLong());
  }

}
