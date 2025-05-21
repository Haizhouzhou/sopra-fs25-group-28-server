package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.Arrays;
import java.util.EnumMap; // Assuming GameSnapshot exists
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.websocket.Session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

public class GameRoomHandleTest {
  
  // no default constructor, need to be initialize manually
  private GameRoom gameRoom;
  private GameRoom spyRoom;

  @Mock
  private UserService userService;

  // Mocks for dependencies (Players)
  @Mock
  private Player mockPlayer1;

  @Mock
  private Player mockPlayer2;

  @Mock
  private Game mockGame;

  @Mock
  private GameSnapshot mockGameSnapshot;

  // Test constants
  private final String testRoomId = "handler-room";
  private final int testMaxPlayers = 4;
  private final String testRoomName = "TestRoom";

  private final String validCardIdStr = "123";
  private final Long validCardIdLong = 123L;

  // Test data
  private Map<GemColor, Long> mockGems;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    gameRoom = new GameRoom(testRoomId, testMaxPlayers, userService);
    gameRoom.setRoomName(testRoomName);

    gameRoom.setGameInstance(mockGame);

    Set<Player> players = ConcurrentHashMap.newKeySet();
    players.add(mockPlayer1);
    players.add(mockPlayer2);
    ReflectionTestUtils.setField(gameRoom, "players", players);

    spyRoom = spy(gameRoom);
    doNothing().when(spyRoom).broadcastGameState();

    mockGems = new EnumMap<>(GemColor.class);
    for (GemColor color : GemColor.values()) {
      mockGems.put(color, 4L);
    }
    given(mockGame.getAvailableGems()).willReturn(mockGems);
  }

  @Test
  void handleBuyCard_Success_CallsGameBuyCardAndReturnsTrue() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(true); // mock successful buy

    // act
    boolean result = spyRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertTrue(result);
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameLogicFails_CallsGameBuyCardAndReturnsFalse() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(false); // mock failed buy

    // act
    boolean result = spyRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameIsNull_ReturnsFalse() {
    // arrange
    ReflectionTestUtils.setField(spyRoom, "game", null); 

    // act
    boolean result = spyRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
  }

  @Test
  void handleReserveCard_Success_CallsGameReserveCardAndReturnsTrue() {
    // arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(true);

    // act
    boolean result = spyRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertTrue(result);
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleReserveCard_GameLogicFails_CallsGameReserveCardAndReturnsFalse() {
    // arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(false);

    // act
    boolean result = spyRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleReserveCard_GameIsNull_ReturnsFalse() {
    // arrange
    ReflectionTestUtils.setField(spyRoom, "game", null);

    // act
    boolean result = spyRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
  }

  @Test
  void handleTakeThreeGems_Success(){
    // arrange
    List<String> colors = Arrays.asList("red", "blue", "green");
    given(mockGame.takeGems(eq(mockPlayer1), anyList())).willReturn(true);

    // act
    boolean result = spyRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertTrue(result);
    verify(mockGame).takeGems(eq(mockPlayer1), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_DuplicateColors() {
    // arrange
    List<String> colors = Arrays.asList("red", "red", "blue");

    // act
    boolean result = spyRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
  }

  @Test
  void handleTakeThreeGems_Fail_ColorsIsNull() {
    // arrange
    
    // act
    boolean result = spyRoom.handleTakeThreeGems(mockPlayer1, null);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_MoreThanThreeColor() {
    // arrange
    List<String> colors = Arrays.asList("green", "red", "blue", "black");

    // act
    boolean result = spyRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_InvalidColor() {
    // arrange
    List<String> colors = Arrays.asList("incorrect_color", "red", "blue");

    // act
    boolean result = spyRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_GameIsNull() {
    // arrange
    ReflectionTestUtils.setField(spyRoom, "game", null);
    List<String> colors = Arrays.asList("red", "blue", "green");

    // act
    boolean result = spyRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
  }

  @Test
  void handleTakeDoubleGem_Success() {
    // arrange
    String colorStr = "blue";
    GemColor color = GemColor.BLUE;
    given(mockGame.takeGems(eq(mockPlayer1), eq(List.of(color)))).willReturn(true);

    // act
    boolean result = spyRoom.handleTakeDoubleGem(mockPlayer1, colorStr);

    // assert
    assertTrue(result);
    verify(mockGame).takeGems(eq(mockPlayer1), eq(List.of(color)));
  }

  @Test
  void handleTakeDoubleGem_Fail_GameIsNull() {
    // arrange
    ReflectionTestUtils.setField(spyRoom, "game", null);

    // act
    boolean result = spyRoom.handleTakeDoubleGem(mockPlayer1, "blue");

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeDoubleGem_Fail_InvalidColor() {
    // arrange
    String colorStr = "incorrect_color";

    // act
    boolean result = spyRoom.handleTakeDoubleGem(mockPlayer1, colorStr);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeDoubleGem_Fail_ColorIsNull() {
    // arrange
    String colorStr = null;

    // act
    boolean result = spyRoom.handleTakeDoubleGem(mockPlayer1, colorStr);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleEndTurn_Success_PlayerTurn_ReturnsTrueAndCallsGame() {
    // arrange
    // 当前回合玩家是mockPlayer1
    given(mockGame.getCurrentPlayer()).willReturn(0);
    given(mockGame.getPlayers()).willReturn(List.of(mockPlayer1, mockPlayer2));
    given(mockPlayer1.getUserId()).willReturn(11L);
    given(mockPlayer2.getUserId()).willReturn(22L);
    given(mockGame.isPlayerTurn(mockPlayer1)).willReturn(true);
    // 游戏未结束
    given(mockGame.getGameState()).willReturn(Game.GameState.RUNNING);

    // act
    boolean result = spyRoom.handleEndTurn(mockPlayer1);

    // assert
    assertTrue(result);
    verify(mockGame, times(1)).endTurn();
    verify(mockGame, atLeastOnce()).getCurrentPlayer();
    verify(mockGame, atLeastOnce()).getPlayers();
    verify(mockGame, times(1)).isPlayerTurn(mockPlayer1);
  }

  @Test
  void handleEndTurn_Fail_NotPlayersTurn_ReturnsFalse() {
      // arrange
      given(mockGame.getCurrentPlayer()).willReturn(0);
      given(mockGame.getPlayers()).willReturn(List.of(mockPlayer1, mockPlayer2));
      given(mockPlayer1.getUserId()).willReturn(11L);
      given(mockGame.isPlayerTurn(mockPlayer1)).willReturn(false);

      // act
      boolean result = spyRoom.handleEndTurn(mockPlayer1);

      // assert
      assertFalse(result);
      verify(mockGame, never()).endTurn();
  }

  @Test
  void handleEndTurn_Fail_GameIsNull_ReturnsFalse() {
    // arrange
    ReflectionTestUtils.setField(spyRoom, "game", null);

    // act
    boolean result = spyRoom.handleEndTurn(mockPlayer1);

    // assert
    assertFalse(result);
  }

  @Test
  void handleEndTurn_GameFinished_CallsEndGame() {
    // arrange
    given(mockGame.getCurrentPlayer()).willReturn(0);
    given(mockGame.getPlayers()).willReturn(List.of(mockPlayer1, mockPlayer2));
    given(mockPlayer1.getUserId()).willReturn(11L);
    given(mockGame.isPlayerTurn(mockPlayer1)).willReturn(true);
    // 回合结束后，游戏状态为FINISHED
    given(mockGame.getGameState()).willReturn(Game.GameState.FINISHED);

    // act
    boolean result = spyRoom.handleEndTurn(mockPlayer1);

    // assert
    assertTrue(result);
    verify(spyRoom, times(1)).EndGame();
    verify(mockGame, times(1)).endTurn();
  }

  /**
   * test timer call handleEndTurn when time is up
   */
  @Test
  @Timeout(2)
  void startRoundTimer_ShouldCallHandleEndTURNAutoMatically() throws Exception{
    // arrange
    AtomicBoolean triggered = new AtomicBoolean(false);

    GameRoom testRoom = new GameRoom(testRoomId, testMaxPlayers, userService) {
      @Override
        public void startRoundTimer(Player currentPlayer) {
          cancelRoundTimer();
          System.out.println("DEBUG: schedule timer 50ms");
          roundTimerFuture = roundTimerExecutor.schedule(
            () -> {
              System.out.println("DEBUG: Timer fired!");
              handleEndTurn(currentPlayer);
            },
            50,
            java.util.concurrent.TimeUnit.MILLISECONDS
          );
        }
      @Override
      public boolean handleEndTurn(Player currentPlayer) {

        triggered.set(true);
        return true;
      }
      
    };
    testRoom.setRoomName(testRoomName);
    testRoom.setGameInstance(mockGame);

    Set<Player> players = ConcurrentHashMap.newKeySet();
    players.add(mockPlayer1);
    players.add(mockPlayer2);
    ReflectionTestUtils.setField(testRoom, "players", players);

    given(mockGame.getCurrentPlayer()).willReturn(0);
    given(mockGame.getPlayers()).willReturn(List.of(mockPlayer1, mockPlayer2));
    given(mockPlayer1.getUserId()).willReturn(11L);

    // act
    testRoom.startRoundTimer(mockPlayer1);
    Thread.sleep(200); // 给 timer 线程触发的机会

    // assert
    assertTrue(triggered.get(), "Timer should trigger handleEndTurn()");
  }
  
  @Test
  @Timeout(2)
  void startRoundTimer_ShouldSkipWhenPlayerIsOffline() throws Exception {
    // arrange
    AtomicBoolean triggered = new AtomicBoolean(false);

    // mock session
    Session mockSession = mock(Session.class);
    given(mockSession.isOpen()).willReturn(false); // 玩家不在线
    given(mockPlayer1.getSession()).willReturn(mockSession);

    // 只覆盖 handleEndTurn，timer 逻辑用 GameRoom 原实现
    GameRoom testRoom = new GameRoom(testRoomId, testMaxPlayers, userService) {
      @Override
      public boolean handleEndTurn(Player currentPlayer) {
        triggered.set(true);
        return true;
      }
    };
    testRoom.setRoomName(testRoomName);
    testRoom.setGameInstance(mockGame);

    Set<Player> players = ConcurrentHashMap.newKeySet();
    players.add(mockPlayer1);
    players.add(mockPlayer2);

    given(mockGame.getCurrentPlayer()).willReturn(0);
    given(mockGame.getPlayers()).willReturn(List.of(mockPlayer1, mockPlayer2));
    given(mockPlayer1.getUserId()).willReturn(11L);

    // act
    testRoom.startRoundTimer(mockPlayer1);

    // assert
    assertTrue(triggered.get(), "Timer should trigger handleEndTurn when player is offline");
  }


}
