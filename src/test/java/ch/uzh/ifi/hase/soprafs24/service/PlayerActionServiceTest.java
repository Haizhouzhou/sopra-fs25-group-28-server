// // package ch.uzh.ifi.hase.soprafs24.service;

// // import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
// // import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
// // import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
// // import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
// // import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;

// // import org.junit.jupiter.api.BeforeEach;
// // import org.junit.jupiter.api.Test;
// // import org.mockito.Mock;
// // import org.mockito.MockitoAnnotations;

// // import javax.websocket.Session;
// // import java.lang.reflect.Field;
// // import java.util.*;

// // import static org.junit.jupiter.api.Assertions.*;
// // import static org.mockito.Mockito.*;

// // public class PlayerActionServiceTest {

// //     @Mock
// //     private GameRoom mockGameRoom;

// //     private PlayerActionService service;
// //     private Game game;
// //     private Player player;

// //     @BeforeEach
// //     public void setup() {
// //         MockitoAnnotations.openMocks(this);

// //         service = new PlayerActionService();

// //         // Mock WebSocket session
// //         Session mockSession = mock(Session.class);
// //         player = new Player(mockSession, "TestPlayer", 1L);

// //         // Inject gems and bonusGems via reflection
// //         Map<GemColor, Long> gems = new HashMap<>();
// //         Map<GemColor, Long> bonusGems = new HashMap<>();
// //         for (GemColor color : GemColor.values()) {
// //             gems.put(color, 0L);
// //             bonusGems.put(color, 0L);
// //         }
// //         injectPrivateField(player, "gems", gems);
// //         injectPrivateField(player, "bonusGems", bonusGems);
// //         injectPrivateField(player, "victoryPoints", 0L);  // Prevent NPE in checkVictoryCondition

// //         Set<Player> players = new LinkedHashSet<>(); // maintain order
// //         players.add(player);

// //         // Spy the Game to mock endTurn
// //         Game realGame = new Game(mockGameRoom, "test-game-id", players);
// //         Game spyGame = spy(realGame);
// //         doNothing().when(spyGame).endTurn();
// //         this.game = spyGame;

// //         // Set current player
// //         game.getPlayers().clear();
// //         game.getPlayers().add(player);

// //         // Initialize board gems
// //         Map<GemColor, Long> boardGems = game.getAvailableGems();
// //         for (GemColor color : GemColor.values()) {
// //             boardGems.put(color, 5L);
// //         }
// //     }

// //     private void injectPrivateField(Object target, String fieldName, Object value) {
// //         try {
// //             Field field = target.getClass().getDeclaredField(fieldName);
// //             field.setAccessible(true);
// //             field.set(target, value);
// //         } catch (Exception e) {
// //             throw new RuntimeException("Failed to inject field: " + fieldName, e);
// //         }
// //     }

// //     @Test
// //     public void testTakeTwoSameGems_Success() {
// //         service.takeTwoSameGems(game, GemColor.RED);
// //         assertEquals(2, player.getGem(GemColor.RED));
// //         assertEquals(3, game.getAvailableGems().get(GemColor.RED));
// //     }

// //     @Test
// //     public void testTakeTwoSameGems_Failure_NotEnoughGems() {
// //         game.getAvailableGems().put(GemColor.BLUE, 1L);
// //         assertThrows(IllegalArgumentException.class, () -> {
// //             service.takeTwoSameGems(game, GemColor.BLUE);
// //         });
// //     }

// //     @Test
// //     public void testTakeThreeDifferentGems_Success() {
// //         List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.GREEN, GemColor.WHITE);
// //         service.takeThreeDifferentGems(game, colors);
// //         assertEquals(1, player.getGem(GemColor.RED));
// //         assertEquals(1, player.getGem(GemColor.GREEN));
// //         assertEquals(1, player.getGem(GemColor.WHITE));
// //     }

// //     @Test
// //     public void testTakeThreeDifferentGems_InvalidColors() {
// //         List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.RED, GemColor.GREEN);
// //         assertThrows(IllegalArgumentException.class, () -> {
// //             service.takeThreeDifferentGems(game, colors);
// //         });
// //     }
// //     @Test
// // public void testGetPaymentForCard_CanAffordWithGemsOnly() {
// //     // Create a card with simple cost
// //     Card card = mock(Card.class);
// //     Map<GemColor, Long> cost = Map.of(GemColor.RED, 2L);
// //     when(card.getCost()).thenReturn(cost);

// //     // Give player 2 red gems
// //     player.setGem(GemColor.RED, 2L);

// //     Map<GemColor, Long> payment = service.getPaymentForCard(game, card);

// //     assertNotNull(payment);
// //     assertEquals(2L, payment.get(GemColor.RED));
// //     assertEquals(0L, payment.get(GemColor.GOLD));
// // }

// // @Test
// // public void testGetPaymentForCard_CanAffordWithGold() {
// //     // Card requires 2 red, player has 1 red, 1 gold
// //     Card card = mock(Card.class);
// //     when(card.getCost()).thenReturn(Map.of(GemColor.RED, 2L));
// //     player.setGem(GemColor.RED, 1L);
// //     player.setGem(GemColor.GOLD, 1L);

// //     Map<GemColor, Long> payment = service.getPaymentForCard(game, card);

// //     assertNotNull(payment);
// //     assertEquals(1L, payment.get(GemColor.RED));
// //     assertEquals(1L, payment.get(GemColor.GOLD));
// // }

// // @Test
// // public void testGetPaymentForCard_CannotAfford() {
// //     // Requires 3 red, has 1 red, 1 gold (2 total < 3)
// //     Card card = mock(Card.class);
// //     when(card.getCost()).thenReturn(Map.of(GemColor.RED, 3L));
// //     player.setGem(GemColor.RED, 1L);
// //     player.setGem(GemColor.GOLD, 1L);

// //     Map<GemColor, Long> payment = service.getPaymentForCard(game, card);
// //     assertNull(payment);
// // }

// // @Test
// // public void testReserveCard_Success() {
// //     // Prepare a card to reserve
// //     Card card = mock(Card.class);
// //     when(card.getId()).thenReturn(100L);
// //     game.getVisibleLevel1Cards().add(card);

// //     service.reserveCard(game, 100L);

// //     assertTrue(player.getReservedCards().contains(card));
// //     assertEquals(1L, player.getGem(GemColor.GOLD)); // got gold gem
// //     assertFalse(game.getVisibleLevel1Cards().contains(card));
// // }

// // @Test
// // public void testReserveCard_Fail_TooManyReserved() {
// //     // Max 3 reserved
// //     for (int i = 0; i < 3; i++) {
// //         Card c = mock(Card.class);
// //         when(c.getId()).thenReturn((long) i);
// //         player.getReservedCards().add(c);
// //     }

// //     Card newCard = mock(Card.class);
// //     when(newCard.getId()).thenReturn(50L);
// //     game.getVisibleLevel2Cards().add(newCard);

// //     assertThrows(IllegalStateException.class, () -> service.reserveCard(game, 50L));
// // }

// // @Test
// // public void testBuyCard_Success() {
// //     // Setup a card that costs 2 red
// //     Card card = mock(Card.class);
// //     when(card.getId()).thenReturn(200L);
// //     when(card.getCost()).thenReturn(Map.of(GemColor.RED, 2L));
// //     when(card.getPoints()).thenReturn(Long.valueOf(1));
// //     when(card.getColor()).thenReturn(GemColor.RED);

// //     game.getVisibleLevel1Cards().add(card);
// //     player.setGem(GemColor.RED, 2L);

// //     service.buyCard(game, 200L);

// //     assertEquals(0L, player.getGem(GemColor.RED));
// //     assertEquals(1L, player.getVictoryPoints());
// //     assertEquals(1L, player.getBonusGem(GemColor.RED));
// // }

// // }
