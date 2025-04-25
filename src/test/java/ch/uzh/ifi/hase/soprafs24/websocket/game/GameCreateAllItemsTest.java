package ch.uzh.ifi.hase.soprafs24.websocket.game;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Noble;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;


public class GameCreateAllItemsTest {

  @Mock
  private Player mockPlayer1;

  @Mock
  private GameRoom mockGameRoom;

  private Game game;

  // --- Expected counts from JSON files (ADJUST THESE VALUES) ---
  private final int EXPECTED_TOTAL_TIER_1_CARDS = 40;
  private final int EXPECTED_TOTAL_TIER_2_CARDS = 30;
  private final int EXPECTED_TOTAL_TIER_3_CARDS = 20;
  private final int EXPECTED_VISIBLE_CARDS_PER_TIER = 4;
  private final int EXPECTED_VISIBLE_NOBLES = 4;

  @BeforeEach
  void setUp() { // Changed visibility
    MockitoAnnotations.openMocks(this);

    // --- Game Instantiation ---
    String testRoomId = "test-createitems-room";
    Set<Player> initialPlayers = new HashSet<>();
    initialPlayers.add(mockPlayer1);
    // initialPlayers.add(mockPlayer2);

    // Use the constructor that takes players
    game = new Game(mockGameRoom, testRoomId, initialPlayers);

  }

  @Test
  public void initialize_shouldCreateAndDistributeItemsCorrectly() {
    // arrange
    
    // act
    // call initialize() which performs createAllItems, shuffle, and fillVisible
    game.initialize();

    // assert
    // Use ReflectionTestUtils to access private fields

    // 1. Check Decks (Size after initial draw)
    Stack<?> level1Deck = (Stack<?>) ReflectionTestUtils.getField(game, "level1Deck");
    Stack<?> level2Deck = (Stack<?>) ReflectionTestUtils.getField(game, "level2Deck");
    Stack<?> level3Deck = (Stack<?>) ReflectionTestUtils.getField(game, "level3Deck");

    assertNotNull(level1Deck, "Level 1 deck should not be null");
    assertNotNull(level2Deck, "Level 2 deck should not be null");
    assertNotNull(level3Deck, "Level 3 deck should not be null");

    int expectedL1DeckSize = EXPECTED_TOTAL_TIER_1_CARDS - EXPECTED_VISIBLE_CARDS_PER_TIER;
    int expectedL2DeckSize = EXPECTED_TOTAL_TIER_2_CARDS - EXPECTED_VISIBLE_CARDS_PER_TIER;
    int expectedL3DeckSize = EXPECTED_TOTAL_TIER_3_CARDS - EXPECTED_VISIBLE_CARDS_PER_TIER;

    assertEquals(expectedL1DeckSize, level1Deck.size(), "Incorrect number of cards remaining in Level 1 deck");
    assertEquals(expectedL2DeckSize, level2Deck.size(), "Incorrect number of cards remaining in Level 2 deck");
    assertEquals(expectedL3DeckSize, level3Deck.size(), "Incorrect number of cards remaining in Level 3 deck");

    // 2. Check Deck Contents (Type and Tier of top card)
    if (!level1Deck.isEmpty()) {
      Object topCardL1 = level1Deck.peek();
      assertTrue(topCardL1 instanceof Card, "Items in Level 1 deck should be Cards");
      assertEquals(1, ((Card) topCardL1).getTier(), "Cards in Level 1 deck should have tier 1");
    }
    if (!level2Deck.isEmpty()) {
      Object topCardL2 = level2Deck.peek();
      assertTrue(topCardL2 instanceof Card, "Items in Level 2 deck should be Cards");
      assertEquals(2, ((Card) topCardL2).getTier(), "Cards in Level 2 deck should have tier 2");
    }
    if (!level3Deck.isEmpty()) {
      Object topCardL3 = level3Deck.peek();
      assertTrue(topCardL3 instanceof Card, "Items in Level 3 deck should be Cards");
      assertEquals(3, ((Card) topCardL3).getTier(), "Cards in Level 3 deck should have tier 3");
    }

    // 3. Check Visible Nobles
    List<?> visibleNoble = (List<?>) ReflectionTestUtils.getField(game, "visibleNoble");
    assertNotNull(visibleNoble, "Visible nobles list should not be null");
    assertEquals(EXPECTED_VISIBLE_NOBLES, visibleNoble.size(), "Incorrect number of visible nobles");

    // 4. Check Noble Contents (Type)
    if (!visibleNoble.isEmpty()) {
        assertTrue(visibleNoble.get(0) instanceof Noble, "Items in visibleNoble list should be Nobles");
    }

    // 5. Check Visible Cards (ensure they were filled)
    List<?> visibleL1 = game.getVisibleLevel1Cards(); // Use getter
    List<?> visibleL2 = game.getVisibleLevel2Cards();
    List<?> visibleL3 = game.getVisibleLevel3Cards();

    assertEquals(EXPECTED_VISIBLE_CARDS_PER_TIER, visibleL1.size(), "Visible L1 list should have correct size");
    assertEquals(EXPECTED_VISIBLE_CARDS_PER_TIER, visibleL2.size(), "Visible L2 list should have correct size");
    assertEquals(EXPECTED_VISIBLE_CARDS_PER_TIER, visibleL3.size(), "Visible L3 list should have correct size");

    // Check that cards were actually drawn (assuming decks were large enough)
    assertNotNull(visibleL1.get(0), "First visible L1 card slot should be filled");
    assertNotNull(visibleL2.get(0), "First visible L2 card slot should be filled");
    assertNotNull(visibleL3.get(0), "First visible L3 card slot should be filled");
    assertTrue(visibleL1.get(0) instanceof Card, "Visible L1 item should be a Card");
    assertTrue(visibleL2.get(0) instanceof Card, "Visible L2 item should be a Card");
    assertTrue(visibleL3.get(0) instanceof Card, "Visible L3 item should be a Card");

    // 6. Check Initial Available Gems (also set by initialize)
    assertEquals(7L, game.getAvailableGems().get(GemColor.BLACK));
    assertEquals(7L, game.getAvailableGems().get(GemColor.RED));
    assertEquals(7L, game.getAvailableGems().get(GemColor.BLUE));
    assertEquals(7L, game.getAvailableGems().get(GemColor.GREEN));
    assertEquals(7L, game.getAvailableGems().get(GemColor.WHITE));
    assertEquals(5L, game.getAvailableGems().get(GemColor.GOLD));

    // 7. Check Game State
    assertEquals(Game.GameState.RUNNING, game.getGameState(), "Game state should be RUNNING after initialization");
  }
}