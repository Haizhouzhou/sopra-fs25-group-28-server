package ch.uzh.ifi.hase.soprafs24.websocket.game;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*; // Import common utilities

import static org.junit.jupiter.api.Assertions.*; // Import assertions
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*; // Import Mockito verification methods

public class GameReserveCardTest {

    @Mock
    private Player mockPlayer;

    @Mock
    private Player mockOtherPlayer;

    @Mock
    private GameRoom mockGameRoom;

    private Game game;

    // test data
    private Card cardToReserveL1;
    private Card cardToReserveL2;
    private Card refillCardL1; // Card to refill the slot

    // game State setup ---
    private String testRoomId = "test-reserve-room";
    private Set<Player> initialPlayers;
    private Map<GemColor, Long> mockAvailableGems;
    private List<Card> visibleLevel1Cards;
    private List<Card> visibleLevel2Cards;
    private List<Card> visibleLevel3Cards;
    private Stack<Card> level1Deck;
    private Stack<Card> level2Deck;
    private Stack<Card> level3Deck;
    private List<Card> playerReservedCards; // The actual list mockPlayer will return

    @BeforeEach
    void setUp() { // Changed visibility
      MockitoAnnotations.openMocks(this);

      // --- Card Definitions ---
      cardToReserveL1 = new Card(5L, 1, GemColor.GREEN, 0L, Map.of(GemColor.BLUE, 1L, GemColor.WHITE, 1L));
      cardToReserveL2 = new Card(15L, 2, GemColor.BLACK, 2L, Map.of(GemColor.RED, 5L));
      refillCardL1 = new Card(35L, 1, GemColor.RED, 0L, Map.of(GemColor.GREEN, 1L));

      // --- Game Instantiation ---
      initialPlayers = new HashSet<>();
      initialPlayers.add(mockPlayer);
      initialPlayers.add(mockOtherPlayer);

      game = new Game(mockGameRoom, testRoomId, initialPlayers);
      // If GameRoom needed: game = new Game(mockGameRoom, testRoomId, initialPlayers);

      // --- Mock Player Setup (mockPlayer) ---
      given(mockPlayer.getUserId()).willReturn(101L);
      given(mockPlayer.getGem(GemColor.GOLD)).willReturn(1L); // Player starts with 1 gold

      // IMPORTANT: Use a real, modifiable list for reserved cards
      playerReservedCards = new ArrayList<>();
      given(mockPlayer.getReservedCards()).willReturn(playerReservedCards); // Mock returns the actual list

      // --- Mock Player Setup (mockOtherPlayer) ---
      given(mockOtherPlayer.getUserId()).willReturn(102L);
      // No need to mock gems/reserved for other player unless testing interactions

      // --- Game State setup
      // Available Gems (Mutable)
      mockAvailableGems = new HashMap<>();
      mockAvailableGems.put(GemColor.BLACK, 7L);
      mockAvailableGems.put(GemColor.RED, 7L);
      mockAvailableGems.put(GemColor.WHITE, 7L);
      mockAvailableGems.put(GemColor.GREEN, 7L);
      mockAvailableGems.put(GemColor.BLUE, 7L);
      mockAvailableGems.put(GemColor.GOLD, 4L); // Start with 4 available gold
      ReflectionTestUtils.setField(game, "availableGems", mockAvailableGems);

      // Visible Cards
      visibleLevel1Cards = new ArrayList<>(Collections.nCopies(4, null));
      visibleLevel2Cards = new ArrayList<>(Collections.nCopies(4, null));
      visibleLevel3Cards = new ArrayList<>(Collections.nCopies(4, null));

      visibleLevel1Cards.set(1, cardToReserveL1); // Place L1 card at index 1
      visibleLevel2Cards.set(2, cardToReserveL2); // Place L2 card at index 2

      ReflectionTestUtils.setField(game, "visibleLevel1Cards", visibleLevel1Cards);
      ReflectionTestUtils.setField(game, "visibleLevel2Cards", visibleLevel2Cards);
      ReflectionTestUtils.setField(game, "visibleLevel3Cards", visibleLevel3Cards);

      // Decks
      level1Deck = new Stack<>();
      level2Deck = new Stack<>();
      level3Deck = new Stack<>();
      level1Deck.push(refillCardL1); // Add refill card to L1 deck

      ReflectionTestUtils.setField(game, "level1Deck", level1Deck);
      ReflectionTestUtils.setField(game, "level2Deck", level2Deck);
      ReflectionTestUtils.setField(game, "level3Deck", level3Deck);

      // Game Status
      game.setGameState(Game.GameState.RUNNING);
      // Set currentPlayer to mockPlayer's index
      ReflectionTestUtils.setField(game, "currentPlayer", game.getPlayers().indexOf(mockPlayer));
    }

    @Test
    public void reserveCard_Success_VisibleL1() {
      // arrange
      Long cardIdToReserve = cardToReserveL1.getId(); // ID: 5L
      int cardIndex = 1; // Index where cardToReserveL1 was placed
      int refillIndex = 0;
      Long initialPlayerGold = mockPlayer.getGem(GemColor.GOLD); // 1L
      Long initialAvailableGold = mockAvailableGems.get(GemColor.GOLD); // 4L

      Long expectedPlayerGold = initialPlayerGold + 1; // Should gain 1 gold
      Long expectedAvailableGold = initialAvailableGold - 1; // Should decrease by 1

      // act
      boolean result = game.reserveCard(mockPlayer, cardIdToReserve);

      // assert
      assertTrue(result, "reserveCard should return true for a valid reservation");

      // Verify card removed from visible list and refilled
      assertNotEquals(cardToReserveL1, visibleLevel1Cards.get(cardIndex), "Original card should be gone from slot " + cardIndex);
      assertNotNull(visibleLevel1Cards.get(refillIndex), "Visible slot " + refillIndex + " should be refilled");
      assertEquals(refillCardL1.getId(), visibleLevel1Cards.get(refillIndex).getId(), "Incorrect card refilled in slot " + cardIndex);

      // Verify card added to player's reserved list
      assertTrue(playerReservedCards.contains(cardToReserveL1), "Card should be added to player's reserved list");
      assertEquals(1, playerReservedCards.size(), "Player should have 1 reserved card");

      // Verify player gains gold
      ArgumentCaptor<Long> goldCaptor = ArgumentCaptor.forClass(Long.class);
      verify(mockPlayer).setGem(eq(GemColor.GOLD), goldCaptor.capture());
      assertEquals(expectedPlayerGold, goldCaptor.getValue(), "Player should gain 1 gold gem");

      // Verify available gold decreases
      assertEquals(expectedAvailableGold, mockAvailableGems.get(GemColor.GOLD), "Available gold gems should decrease by 1");
    }

    @Test
    public void reserveCard_Success_VisibleL2_NoGoldAvailable() {
      // arrange
      mockAvailableGems.put(GemColor.GOLD, 0L); // Set available gold to 0
      Long cardIdToReserve = cardToReserveL2.getId(); // ID: 15L
      int cardIndex = 2; // Index where cardToReserveL2 was placed
      Long initialPlayerGold = mockPlayer.getGem(GemColor.GOLD); // 1L

      Long expectedPlayerGold = initialPlayerGold; // Player gold should NOT change
      Long expectedAvailableGold = 0L; // Available gold should remain 0

      // act
      boolean result = game.reserveCard(mockPlayer, cardIdToReserve);

      // assert
      assertTrue(result, "reserveCard should return true even if no gold is available");

      // Verify card removed from visible list (no refill card in L2 deck)
      assertNull(visibleLevel2Cards.get(cardIndex), "Visible slot " + cardIndex + " should become null");

      // Verify card added to player's reserved list
      assertTrue(playerReservedCards.contains(cardToReserveL2), "Card should be added to player's reserved list");
      assertEquals(1, playerReservedCards.size(), "Player should have 1 reserved card");

      // Verify player does NOT gain gold
      verify(mockPlayer, never()).setGem(eq(GemColor.GOLD), anyLong()); // setGem(GOLD) should not be called

      // Verify available gold remains 0
      assertEquals(expectedAvailableGold, mockAvailableGems.get(GemColor.GOLD), "Available gold gems should remain 0");
    }

    @Test
    public void reserveCard_Fail_NotPlayerTurn() {
      // arrange
      ReflectionTestUtils.setField(game, "currentPlayer", game.getPlayers().indexOf(mockOtherPlayer)); // Other player's turn
      Long cardIdToReserve = cardToReserveL1.getId();

      // act
      boolean result = game.reserveCard(mockPlayer, cardIdToReserve);

      // assert
      assertFalse(result, "reserveCard should return false if it's not the player's turn");

      // Verify no state changes
      verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
      assertTrue(playerReservedCards.isEmpty(), "Player reserved cards should remain empty");
      assertEquals(cardToReserveL1, visibleLevel1Cards.get(1), "Visible card should not be removed");
      assertEquals(4L, mockAvailableGems.get(GemColor.GOLD), "Available gold should not change"); // Check initial value
    }

    @Test
    public void reserveCard_Fail_GameNotRunning() {
      // arrange
      game.setGameState(Game.GameState.FINISHED); // Game is finished
      Long cardIdToReserve = cardToReserveL1.getId();

      // act
      boolean result = game.reserveCard(mockPlayer, cardIdToReserve);

      // assert
      assertFalse(result, "reserveCard should return false if game is not running");
      verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
      assertTrue(playerReservedCards.isEmpty());
    }

    @Test
    public void reserveCard_Fail_CardNotFound() {
      // arrange
      Long nonExistentCardId = 999L;

      // act
      boolean result = game.reserveCard(mockPlayer, nonExistentCardId);

      // assert
      assertFalse(result, "reserveCard should return false if card ID is not found");
      verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
      assertTrue(playerReservedCards.isEmpty());
    }

    @Test
    public void reserveCard_Fail_Already3Reserved() {
      // arrange
      // Add 3 dummy cards to the player's list BEFORE the action
      playerReservedCards.add(new Card(201L, 1, GemColor.RED, 0L, Map.of()));
      playerReservedCards.add(new Card(202L, 1, GemColor.RED, 0L, Map.of()));
      playerReservedCards.add(new Card(203L, 1, GemColor.RED, 0L, Map.of()));
      assertEquals(3, playerReservedCards.size(), "Precondition: Player should have 3 reserved cards");

      Long cardIdToReserve = cardToReserveL1.getId();

      // act
      boolean result = game.reserveCard(mockPlayer, cardIdToReserve);

      // assert
      assertFalse(result, "reserveCard should return false if player already has 3 reserved cards");

      // Verify no state changes
      verify(mockPlayer, never()).setGem(any(GemColor.class), anyLong());
      assertEquals(3, playerReservedCards.size(), "Player reserved card count should remain 3");
      assertFalse(playerReservedCards.contains(cardToReserveL1), "Target card should not be added");
      assertEquals(cardToReserveL1, visibleLevel1Cards.get(1), "Visible card should not be removed");
      assertEquals(4L, mockAvailableGems.get(GemColor.GOLD), "Available gold should not change");
    }
}