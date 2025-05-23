package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

/**
 * Unit tests for the GameRoom class.
 */
public class GameRoomTest {

  // no default constructor, need to be initialize manually
  private GameRoom gameRoom;

  @MockBean
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
  private final String testRoomId = "testRoomId";
  private final int testMaxPlayers = 4;
  private final String testRoomName = "TestRoom";

  @BeforeEach
  void setUp() {
    // Initialize mocks annotated with @Mock
    MockitoAnnotations.openMocks(this);

    // Create a new GameRoom instance for each test
    gameRoom = new GameRoom(testRoomId, testMaxPlayers, userService);
    gameRoom.setRoomName(testRoomName); // Set a name for testing

    gameRoom.setGameInstance(mockGame);
  }

  @Test
  void getRoomInformation_WhenRoomIsEmpty_ShouldReturnCorrectInfo() {
    // arrange

    // act
    Map<String, Object> roomInfo = gameRoom.getRoomInformation();

    // assert
    assertNotNull(roomInfo, "Returned map should not be null");

    // Check room properties
    assertEquals(testMaxPlayers, roomInfo.get("maxPlayers"));
    assertEquals(0, roomInfo.get("currentPlayers"));
    assertEquals(testRoomName, roomInfo.get("roomName"));
    assertTrue((Boolean) roomInfo.get("isReady"));

    // Check players list
    Object playersObject = roomInfo.get("players");
    assertNotNull(playersObject, "Players list should exist");
    assertTrue(playersObject instanceof List, "Players should be a List");
    List<Map<String, Object>> playersInfo = (List<Map<String, Object>>) playersObject;
    assertTrue(playersInfo.isEmpty(), "Players list should be empty");
  }

  @Test
  void getRoomInformation_WithMultiplePlayersMixedStatus_ShouldReturnCorrectInfo() {
    // Arrange
    // 1. Configure mock player 1 (Ready)
    Long player1Id = 101L;
    String player1Name = "Alice";
    String player1Avatar = "avatar1";
    boolean player1Status = true; // Ready
    given(mockPlayer1.getUserId()).willReturn(player1Id);
    given(mockPlayer1.getName()).willReturn(player1Name);
    given(mockPlayer1.getAvatar()).willReturn(player1Avatar);
    given(mockPlayer1.getStatus()).willReturn(player1Status); // Crucial for isReady calculation

    // 2. Configure mock player 2 (Not Ready)
    Long player2Id = 102L;
    String player2Name = "Bob";
    String player2Avatar = "avatar2";
    boolean player2Status = false; // Not Ready
    given(mockPlayer2.getUserId()).willReturn(player2Id);
    given(mockPlayer2.getName()).willReturn(player2Name);
    given(mockPlayer2.getAvatar()).willReturn(player2Avatar);
    given(mockPlayer2.getStatus()).willReturn(player2Status); // Crucial for isReady calculation

    // 3. Add players to the room
    // Accessing the internal set directly for testing setup might be needed
    // if addPlayer is complex or has side effects we don't want here.
    // Let's assume we can directly modify the set for this unit test.
    Set<Player> playersSet = gameRoom.getPlayers(); // Get the actual set
    playersSet.add(mockPlayer1);
    playersSet.add(mockPlayer2);
    // If direct access isn't possible/desired, use gameRoom.addPlayer(mockPlayer1); etc.

    // Act
    Map<String, Object> roomInfo = gameRoom.getRoomInformation();

    // Assert
    assertNotNull(roomInfo);

    // Check top-level properties
    assertEquals(testMaxPlayers, roomInfo.get("maxPlayers"));
    assertEquals(2, roomInfo.get("currentPlayers"), "Should reflect 2 players added");
    assertEquals(testRoomName, roomInfo.get("roomName"));
    // getRoomStatus() calculates: true && player1Status(true) && player2Status(false) = false
    assertFalse((Boolean) roomInfo.get("isReady"), "isReady should be false as one player is not ready");

    // Check players list
    Object playersObject = roomInfo.get("players");
    assertNotNull(playersObject);
    assertTrue(playersObject instanceof List);
    List<Map<String, Object>> playersInfo = (List<Map<String, Object>>) playersObject;
    assertEquals(2, playersInfo.size(), "Players list should contain 2 entries");

    // Verify content of player entries (order in Set is not guaranteed, so check presence)
    boolean foundPlayer1 = false;
    boolean foundPlayer2 = false;
    for (Map<String, Object> playerInfo : playersInfo) {
      Long currentUserId = (Long) playerInfo.get("userId");
      if (currentUserId.equals(player1Id)) {
        assertEquals(player1Name, playerInfo.get("name"));
        assertEquals(player1Avatar, playerInfo.get("avatar"));
        assertEquals(player1Status, playerInfo.get("room_status"));
        foundPlayer1 = true;
      } else if (currentUserId.equals(player2Id)) {
        assertEquals(player2Name, playerInfo.get("name"));
        assertEquals(player2Avatar, playerInfo.get("avatar"));
        assertEquals(player2Status, playerInfo.get("room_status"));
        foundPlayer2 = true;
      }
    }
    assertTrue(foundPlayer1);
    assertTrue(foundPlayer2);
  }

    @Test
    void broadcastGameState_WithGameAndPlayers_ShouldSendStateToAllPlayers() {
      // arrange
      gameRoom.setGameInstance(mockGame); // 设置 mockGame 实例

      Set<Player> playersSet = gameRoom.getPlayers();
      playersSet.add(mockPlayer1);
      playersSet.add(mockPlayer2);

      // mock 玩家 session 和行为
      Session mockSession1 = mock(Session.class);
      Session mockSession2 = mock(Session.class);
      RemoteEndpoint.Basic mockRemote1 = mock(RemoteEndpoint.Basic.class);
      RemoteEndpoint.Basic mockRemote2 = mock(RemoteEndpoint.Basic.class);

      given(mockPlayer1.getSession()).willReturn(mockSession1);
      given(mockPlayer2.getSession()).willReturn(mockSession2);
      given(mockSession1.isOpen()).willReturn(true);
      given(mockSession2.isOpen()).willReturn(true);
      given(mockSession1.getBasicRemote()).willReturn(mockRemote1);
      given(mockSession2.getBasicRemote()).willReturn(mockRemote2);

      // game.getGameInformation() 返回 mock snapshot
      given(mockGame.getGameInformation()).willReturn(mockGameSnapshot);

      // 捕获消息
      ArgumentCaptor<MyWebSocketMessage> messageCaptor1 = ArgumentCaptor.forClass(MyWebSocketMessage.class);
      ArgumentCaptor<MyWebSocketMessage> messageCaptor2 = ArgumentCaptor.forClass(MyWebSocketMessage.class);

      // act
      gameRoom.broadcastGameState();

      // assert
      verify(mockGame, times(1)).getGameInformation();

      verify(mockPlayer1, times(1)).sendMessage(messageCaptor1.capture());
      verify(mockPlayer2, times(1)).sendMessage(messageCaptor2.capture());

      MyWebSocketMessage msg1 = messageCaptor1.getValue();
      MyWebSocketMessage msg2 = messageCaptor2.getValue();

      assertEquals(MyWebSocketMessage.TYPE_SERVER_GAME_STATE, msg1.getType());
      assertEquals(MyWebSocketMessage.TYPE_SERVER_GAME_STATE, msg2.getType());

      assertEquals(gameRoom.getRoomId(), msg1.getRoomId());
      assertEquals(gameRoom.getRoomId(), msg2.getRoomId());

      assertSame(mockGameSnapshot, msg1.getContent());
      assertSame(mockGameSnapshot, msg2.getContent());
    }


  @Test
  void broadcastGameState_WhenGameIsNull_ShouldDoNothing() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null);
    assertNull(ReflectionTestUtils.getField(gameRoom, "game"));

    Set<Player> playersSet = gameRoom.getPlayers();
    playersSet.add(mockPlayer1);

    // act
    gameRoom.broadcastGameState();

    // assert
    verify(mockGame, never()).getGameInformation(); // mockGame is the instance created by Mockito, even if not set in gameRoom
    verify(mockPlayer1, never()).sendMessage(any(MyWebSocketMessage.class));
  }

  @Test
  void broadcastGameState_WhenNoPlayers_ShouldFetchStateButNotSend() {
    // arrange
    assertNotNull(ReflectionTestUtils.getField(gameRoom, "game"));
    given(mockGame.getGameInformation()).willReturn(mockGameSnapshot);
    assertTrue(gameRoom.getPlayers().isEmpty());

    // act
    gameRoom.broadcastGameState();

    // assert
    verify(mockGame, times(1)).getGameInformation();

    verify(mockPlayer1, never()).sendMessage(any());
    verify(mockPlayer2, never()).sendMessage(any());
  }

  @Test
  void updateGameStateForPlayer_ShouldSendStateToOnlyThatPlayer() {
    // arrange
    gameRoom.setGameInstance(mockGame);

    // mock 1个玩家
    Set<Player> playersSet = gameRoom.getPlayers();
    playersSet.add(mockPlayer1);

    // mock 玩家 session 和行为
    Session mockSession1 = mock(Session.class);
    RemoteEndpoint.Basic mockRemote1 = mock(RemoteEndpoint.Basic.class);

    given(mockPlayer1.getSession()).willReturn(mockSession1);
    given(mockSession1.isOpen()).willReturn(true);
    given(mockSession1.getBasicRemote()).willReturn(mockRemote1);

    // game.getGameInformation() 返回 mock snapshot
    given(mockGame.getGameInformation()).willReturn(mockGameSnapshot);

    // 捕获消息
    ArgumentCaptor<MyWebSocketMessage> messageCaptor = ArgumentCaptor.forClass(MyWebSocketMessage.class);

    // act
    gameRoom.updateGameStateForPlayer(mockPlayer1);

    // assert
    verify(mockGame, times(1)).getGameInformation();
    verify(mockPlayer1, times(1)).sendMessage(messageCaptor.capture());

    MyWebSocketMessage msg = messageCaptor.getValue();

    assertEquals(MyWebSocketMessage.TYPE_SERVER_GAME_STATE, msg.getType());
    assertEquals(gameRoom.getRoomId(), msg.getRoomId());
    assertSame(mockGameSnapshot, msg.getContent());
  }

  @Test
  void updateGameStateForPlayer_SessionClosed_ShouldNotSend() {
    // arrange
    Set<Player> playersSet = gameRoom.getPlayers();
    playersSet.add(mockPlayer1);

    Session mockSession1 = mock(Session.class);
    given(mockPlayer1.getSession()).willReturn(mockSession1);
    given(mockSession1.isOpen()).willReturn(false); // session 关闭

    given(mockGame.getGameInformation()).willReturn(mockGameSnapshot);

    // act
    gameRoom.updateGameStateForPlayer(mockPlayer1);

    // assert
    verify(mockPlayer1, never()).sendMessage(any(MyWebSocketMessage.class));
  }

  @Test
  void updateGameStateForPlayer_GameIsNull_ShouldDoNothing() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null);

    Set<Player> playersSet = gameRoom.getPlayers();
    playersSet.add(mockPlayer1);

    Session mockSession1 = mock(Session.class);
    given(mockPlayer1.getSession()).willReturn(mockSession1);
    given(mockSession1.isOpen()).willReturn(true);

    // act
    gameRoom.updateGameStateForPlayer(mockPlayer1);

    // assert
    verify(mockPlayer1, never()).sendMessage(any(MyWebSocketMessage.class));
  }

  @Test
  void updateGameStateForPlayer_SessionIsNull_ShouldNotSend() {
    // arrange
    Set<Player> playersSet = gameRoom.getPlayers();
    playersSet.add(mockPlayer1);

    given(mockPlayer1.getSession()).willReturn(null); // session 为 null

    given(mockGame.getGameInformation()).willReturn(mockGameSnapshot);

    // act
    gameRoom.updateGameStateForPlayer(mockPlayer1);

    // assert
    verify(mockPlayer1, never()).sendMessage(any(MyWebSocketMessage.class));
  }

  @Test
  void updateGameStateForPlayer_PlayerIsNull_ShouldDoNothing() {
    // arrange

    // act & assert
    assertDoesNotThrow(() -> gameRoom.updateGameStateForPlayer(null));
  }

 @Test
  void startGame_ShouldCreateGameAndCallInitialize() {
    // arrange
    Player player = mock(Player.class);
    Session session = mock(Session.class);
    given(session.isOpen()).willReturn(true);
    given(player.getSession()).willReturn(session);
    given(player.getName()).willReturn("TestName");
    given(player.getUserId()).willReturn(123L);

    gameRoom.getPlayers().add(player);

    // act & assert
    assertDoesNotThrow(() -> gameRoom.startGame());
    assertNotNull(gameRoom.getGame());
  }

  @Test
  void sendErrorToPlayer_ShouldSendProperErrorMessage() {
    // arrange
    String errorMsg = "Test Error";
    ArgumentCaptor<MyWebSocketMessage> captor = ArgumentCaptor.forClass(MyWebSocketMessage.class);

    // act
    gameRoom.sendErrorToPlayer(mockPlayer1, errorMsg);

    // assert
    verify(mockPlayer1).sendMessage(captor.capture());
    MyWebSocketMessage message = captor.getValue();
    assertEquals(MyWebSocketMessage.TYPE_SERVER_ERROR, message.getType());
    assertEquals(gameRoom.getRoomId(), message.getRoomId());
    assertTrue(message.getContent() instanceof Map);

    @SuppressWarnings("unchecked")
    Map<String, String> content = (Map<String, String>) message.getContent();
    assertEquals(errorMsg, content.get("message"));
  }

  @Test
  void manuallyDestroyTimer_ShouldShutdownExecutorGracefully() {
    // arrange
    GameRoom spyRoom = org.mockito.Mockito.spy(gameRoom);
    ScheduledExecutorService executor = spyRoom.roundTimerExecutor;

    // act
    spyRoom.manuallyDestroyTimer();

    // assert
    assertTrue(executor.isShutdown() || executor.isTerminated());
  }

  @Test
  void cancelRoundTimer_WhenFutureIsNullOrDone_ShouldNotThrow() {
    // arrange 1: future is null
    ReflectionTestUtils.setField(gameRoom, "roundTimerFuture", null);
    // act & assert : no exception
    gameRoom.cancelRoundTimer();

    // arrange 2 :future complete
    ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
    given(mockFuture.isDone()).willReturn(true);
    ReflectionTestUtils.setField(gameRoom, "roundTimerFuture", mockFuture);
    // act & assert
    gameRoom.cancelRoundTimer();

    // arrange 3 :future incomplete (isDone Return false)
    ScheduledFuture<?> mockFuture2 = mock(ScheduledFuture.class);
    given(mockFuture2.isDone()).willReturn(false);
    ReflectionTestUtils.setField(gameRoom, "roundTimerFuture", mockFuture2);
    gameRoom.cancelRoundTimer();
    verify(mockFuture2, times(1)).cancel(false);
  }


}
