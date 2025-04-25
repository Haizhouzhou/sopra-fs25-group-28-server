// package ch.uzh.ifi.hase.soprafs24.websocket.util;

// import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
// import ch.uzh.ifi.hase.soprafs24.rest.dto.GeminiAPIResponse;
// import ch.uzh.ifi.hase.soprafs24.service.GeminiIntegrationTest;
// import ch.uzh.ifi.hase.soprafs24.service.GeminiService;
// import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
// import ch.uzh.ifi.hase.soprafs24.websocket.dto.PlayerSnapshot;
// import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

// import com.fasterxml.jackson.databind.ObjectMapper;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockedStatic;
// import org.mockito.MockitoAnnotations;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;

// import java.util.*;

// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.*;

// public class GeminiHintTest {

//   private static final Logger log = LoggerFactory.getLogger(GeminiIntegrationTest.class);

//   private ObjectMapper objectMapper = new ObjectMapper();

//   @Autowired
//   private GeminiService geminiService;

//   @InjectMocks
//   private GeminiHint geminiHint;

//   private Map<GemColor, Long> mockGems;
//   private Map<GemColor, Long> mockBonusGems;
//   private List<Card> mockVisibleLevel1Cards;
//   private List<Card> mockVisibleLevel2Cards;
//   private List<Card> mockVisibleLevel3Cards;

//   @Mock
//   private Game mockGame;

//   @Mock
//   private Player mockPlayer;

//   @Mock
//   private GameSnapshot mockGameSnapshot;


//   // nobles on the board
//   private List<Noble> mockVisibleNoble;
//   private List<Long> mockReservedCardIds;
//   @BeforeEach
//   public void testSetup(){
//     MockitoAnnotations.openMocks(this);

//     mockGems = new HashMap<>();
//     mockGems.put(GemColor.BLACK, 0L);
//     mockGems.put(GemColor.BLUE, 0L);
//     mockGems.put(GemColor.GREEN, 0L);
//     mockGems.put(GemColor.RED, 0L);
//     mockGems.put(GemColor.WHITE, 0L);
//     mockGems.put(GemColor.GOLD, 0L);

//     mockBonusGems = new HashMap<>();
//     mockBonusGems.put(GemColor.BLACK, 2L);
//     mockBonusGems.put(GemColor.BLUE, 2L);
//     mockBonusGems.put(GemColor.GREEN, 2L);
//     mockBonusGems.put(GemColor.RED, 2L);
//     mockBonusGems.put(GemColor.WHITE, 2L);
//     mockBonusGems.put(GemColor.GOLD, 0L);

//     mockVisibleLevel1Cards = new ArrayList<>(4);
//     mockVisibleLevel2Cards = new ArrayList<>(4);
//     mockVisibleLevel3Cards = new ArrayList<>(4);
//     mockVisibleNoble = new ArrayList<>(4);

//     setUpMockCard();
//     setUpMockNoble();

//     mockReservedCardIds = new ArrayList<>();

//     // Player player = mock(Player.class); // mock player
//     List<Player> players = List.of(mockPlayer);

//     GameRoom gameRoom = mock(GameRoom.class);
//     when(gameRoom.getRoomName()).thenReturn("test-gameRoom");

//     // Game game = mock(Game.class);
//     when(mockGame.getGameId()).thenReturn("test-game");
//     when(mockGame.getPlayers()).thenReturn(players);
//     when(mockGame.getCurrentPlayer()).thenReturn(0);
//     when(mockGame.getCurrentRound()).thenReturn(1);
//     when(mockGame.getAvailableGems()).thenReturn(mockGems);
//     when(mockGame.getVisibleLevel1Cards()).thenReturn(mockVisibleLevel1Cards);
//     when(mockGame.getVisibleLevel2Cards()).thenReturn(mockVisibleLevel2Cards);
//     when(mockGame.getVisibleLevel3Cards()).thenReturn(mockVisibleLevel3Cards);
//     when(mockGame.getVisibleNoble()).thenReturn(mockVisibleNoble);
//     when(mockGame.getGameRoom()).thenReturn(gameRoom);


//     when(mockGameSnapshot.getGameId()).thenReturn("test-game");
//     when(mockGameSnapshot.getPlayerOrder()).thenReturn(players);


//     // public static GameSnapshot createFromGame(Game game){
//     //   GameSnapshot snapshot = new GameSnapshot();
  
//     //   snapshot.setGameId(game.getGameId());
//     //   snapshot.setPlayerOrder(game.getPlayers());
//     //   snapshot.setCurrentPlayerIndex(game.getCurrentPlayer());
//     //   snapshot.setCurrentRound(game.getCurrentRound());
//     //   snapshot.setAvailableGems(game.getAvailableGems());
//     //   snapshot.setVisibleLevel1cardIds(game.getVisibleLevel1Cards());
//     //   snapshot.setVisibleLevel2cardIds(game.getVisibleLevel2Cards());
//     //   snapshot.setVisibleLevel3cardIds(game.getVisibleLevel3Cards());
//     //   snapshot.setVisibleNobleIds(game.getVisibleNoble());
  
//     //   snapshot.setRoomName(game.getGameRoom().getRoomName());
  
//     //   List<PlayerSnapshot> playerSnapshots = new ArrayList<>();
//     //   for(Player player : game.getPlayers()){
//     //     playerSnapshots.add(PlayerSnapshot.createFromPlayer(player));
//     //   }
//     //   snapshot.setPlayerSnapshots(playerSnapshots);
  
//     //   return snapshot;
//     // }




//     when(mockGame.getGameInformation()).thenReturn(value);
//   }

//   @Test
//   public void generateSplendorHint_success(){
//   }

//   private void setUpMockCard(){
//     mockVisibleLevel1Cards.add(new Card(
//     1L, 1, GemColor.BLUE, 0L,
//         Map.of(GemColor.BLACK, 3L)
//     ));
//     mockVisibleLevel1Cards.add(new Card(
//         2L, 1, GemColor.BLUE, 1L,
//         Map.of(GemColor.RED, 4L)
//     ));
//     mockVisibleLevel1Cards.add(new Card(
//         3L, 1, GemColor.BLUE, 0L,
//         Map.of(GemColor.WHITE, 1L, GemColor.BLACK, 2L)
//     ));
//     mockVisibleLevel1Cards.add(new Card(
//         4L, 1, GemColor.BLUE, 0L,
//         Map.of(GemColor.GREEN, 2L, GemColor.BLACK, 2L)
//     ));

//     mockVisibleLevel2Cards.add(new Card(41L, 2, GemColor.BLUE, 1L,
//         Map.of(GemColor.BLUE, 2L, GemColor.GREEN, 2L, GemColor.RED, 3L)));
//     mockVisibleLevel2Cards.add(new Card(51L, 2, GemColor.RED, 3L,
//         Map.of(GemColor.RED, 6L)));
//     mockVisibleLevel2Cards.add(new Card(62L, 2, GemColor.BLACK, 2L,
//         Map.of(GemColor.WHITE, 5L)));
//     mockVisibleLevel2Cards.add(new Card(81L, 2, GemColor.WHITE, 3L,
//         Map.of(GemColor.WHITE, 6L)));

//     mockVisibleLevel3Cards.add(new Card(47L, 3, GemColor.BLUE, 4L,
//         Map.of(GemColor.WHITE, 7L)));
//     mockVisibleLevel3Cards.add(new Card(57L, 3, GemColor.RED, 4L,
//         Map.of(GemColor.GREEN, 7L)));
//     mockVisibleLevel3Cards.add(new Card(67L, 3, GemColor.BLACK, 4L,
//         Map.of(GemColor.RED, 7L)));
//     mockVisibleLevel3Cards.add(new Card(87L, 3, GemColor.WHITE, 4L,
//         Map.of(GemColor.BLACK, 7L)));

//   }

//   private void setUpMockNoble(){
//     mockVisibleNoble.add(new Noble(1L, 3L, Map.of(
//         GemColor.BLUE, 4L,
//         GemColor.GREEN, 4L
//     )));

//     mockVisibleNoble.add(new Noble(3L, 3L, Map.of(
//         GemColor.GREEN, 3L,
//         GemColor.BLUE, 3L,
//         GemColor.RED, 3L
//     )));

//     mockVisibleNoble.add(new Noble(4L, 3L, Map.of(
//         GemColor.BLACK, 3L,
//         GemColor.RED, 3L,
//         GemColor.WHITE, 3L
//     )));

//     mockVisibleNoble.add(new Noble(6L, 3L, Map.of(
//         GemColor.BLUE, 4L,
//         GemColor.WHITE, 4L
//     )));

//     mockVisibleNoble.add(new Noble(9L, 3L, Map.of(
//         GemColor.BLACK, 3L,
//         GemColor.BLUE, 3L,
//         GemColor.WHITE, 3L
//     )));
//   }
  
// }
