package ch.uzh.ifi.hase.soprafs24.websocket.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the GameRoom class.
 */
public class GameRoomTest {

  // Class under test - we will use a real instance
  private GameRoom gameRoom;

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
    gameRoom = new GameRoom(testRoomId, testMaxPlayers);
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
    assertNotNull(ReflectionTestUtils.getField(gameRoom, "game"));
    given(mockGame.getGameInformation()).willReturn(mockGameSnapshot);
    Set<Player> playersSet = gameRoom.getPlayers();
    playersSet.add(mockPlayer1);
    playersSet.add(mockPlayer2);
    assertEquals(2, gameRoom.getCurrentPlayerCount());

    ArgumentCaptor<MyWebSocketMessage> messageCaptor = ArgumentCaptor.forClass(MyWebSocketMessage.class);

    // act
    gameRoom.broadcastGameState();

    // assert
    verify(mockGame, times(1)).getGameInformation();
    verify(mockPlayer1, times(1)).sendMessage(messageCaptor.capture());
    verify(mockPlayer2, times(1)).sendMessage(messageCaptor.capture());


    List<MyWebSocketMessage> capturedMessages = messageCaptor.getAllValues();
    assertEquals(2, capturedMessages.size()); // Ensure both captures happened

    MyWebSocketMessage sentMessage = capturedMessages.get(0); // Check the first captured message
    assertNotNull(sentMessage);
    assertEquals(MyWebSocketMessage.TYPE_SERVER_GAME_STATE, sentMessage.getType()    );
    assertEquals(testRoomId, sentMessage.getRoomId());
    assertSame(mockGameSnapshot, sentMessage.getContent());
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


}
