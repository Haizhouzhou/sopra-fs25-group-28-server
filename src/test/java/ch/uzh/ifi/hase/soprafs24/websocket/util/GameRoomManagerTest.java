package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

public class GameRoomManagerTest {

  @Mock
  private UserService mockUserService;

  @Mock
  private Session mockSession;

  @Mock
  private GameRoom mockRoom;

  @Mock
  private User mockUser; // used for registerPlayer test

  @Mock
  private RemoteEndpoint.Basic mockBasicRemote;

  @InjectMocks
  private GameRoomManager gameRoomManager;

  // test data
  private Map<String, String> mockSessionRoomsMap;
  private Map<String, GameRoom> mockRoomsMap;
  private Player testPlayer;

  // some mock variables
  private String testSessionId = "1";
  private Long testUserId = 1L;
  private String testNameforUser = "testName";
  private String testRoomId = "testRoomId";

  @BeforeEach
  public void setUp(){
    MockitoAnnotations.openMocks(this);

    mockSessionRoomsMap = new HashMap<>();
    mockRoomsMap = new HashMap<>();

    gameRoomManager.setRooms(mockRoomsMap);
    gameRoomManager.setSessionRoomsMap(mockSessionRoomsMap);

    // gameRoomManager.setSessionPlayersMap(mockSessionPlayersMap);

    testPlayer = new Player(mockSession, testNameforUser, testUserId);

    gameRoomManager.players.clear(); // empty set, add testPlayer when needed

    given(mockSession.getId()).willReturn(testSessionId);
    given(mockSession.isOpen()).willReturn(true);
    given(mockSession.getBasicRemote()).willReturn(mockBasicRemote);
  }

  @Test
  public void getPlayerBySession_WhenPlayerNotRegistered_shouldReturnNull(){
    // arrange: empty map

    // act
    Player result = gameRoomManager.getPlayerBySession(mockSession);

    // assert
    assertNull(result, "return null if sessionPlayers is empty (player not found)");
  }

  @Test
  public void getPlayerBySession_success(){
    // arrange
    gameRoomManager.players.add(testPlayer);

    // but sessionRooms is still empty

    // act
    Player result = gameRoomManager.getPlayerBySession(mockSession);

    // assert
    assertNotNull(result, "Result should not be null");
    // Crucially, check if it's the exact same instance stored in sessionPlayers
    assertSame(testPlayer, result, "Should return the instance from sessionPlayers map");
  }

  @Test
  public void joinRoom_Success_WhenRoomExistsAndNotFull() throws IOException {
    // arrange

    gameRoomManager.players.add(testPlayer);
    given(mockRoom.isFull()).willReturn(false);
    mockRoomsMap.put(testRoomId, mockRoom);

    // act
    boolean result = gameRoomManager.joinRoom(testRoomId, mockSession);

    // assert
    assertTrue(result, "joinRoom should return true on success");
    verify(mockRoom).addPlayer(eq(testPlayer));
    verify(mockRoom).broadcastRoomStatus();
    assertEquals(testRoomId, mockSessionRoomsMap.get(testSessionId));

    // 检查消息内容
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockBasicRemote).sendText(messageCaptor.capture());

    String message = messageCaptor.getValue();
    assertTrue(message.contains("\"type\":\"ROOM_JOINED\""));
    assertTrue(message.contains("\"roomId\":\"" + testRoomId + "\""));
  }

  @Test
  void joininRoom_Failure_PlayerIsNull() throws Exception{
    // arrange
    // getPlayerBySession return null

    // act
    boolean result = gameRoomManager.joinRoom(testRoomId, mockSession);

    // assert
    assertFalse(result);
  }

  @Test
  void joininRoom_Failure_SessionIsNull() throws Exception{
    // arrange

    // act
    boolean result = gameRoomManager.joinRoom(testRoomId, null);

    // assert
    assertFalse(result);
  }

  @Test
  public void joinRoom_Failure_WhenRoomIsFull() throws IOException {
    // arrange
    gameRoomManager.players.add(testPlayer);
    given(mockRoom.isFull()).willReturn(true); // Room IS full
    mockRoomsMap.put(testRoomId, mockRoom);

    // act
    boolean result = gameRoomManager.joinRoom(testRoomId, mockSession);

    // assert
    assertFalse(result, "joinRoom should return false when room is full");
    verify(mockRoom, never()).addPlayer(any(Player.class));
    assertNull(mockSessionRoomsMap.get(testSessionId), "sessionRooms map should NOT contain the session-room mapping");
    verify(mockRoom, never()).broadcastRoomStatus();
    verify(mockBasicRemote, never()).sendText(anyString());
  }

  @Test
  public void leaveRoom_Success_WhenRoomNotEmptyAfterLeave() {
    // arrange
    // Player leavingPlayer = new Player(mockSession, testRoomId, testUserId);
    testPlayer = new Player(mockSession, testNameforUser, testUserId);
    gameRoomManager.players.add(testPlayer);
    // mockSessionPlayersMap.put(testSessionId, leavingPlayer);
    mockSessionRoomsMap.put(testSessionId, testRoomId);
    mockRoomsMap.put(testRoomId, mockRoom);

    given(mockRoom.isEmpty()).willReturn(false);

    // act
    gameRoomManager.leaveRoom(mockSession);

    // assert
    verify(mockRoom, times(1)).removePlayer(eq(testPlayer));
    verify(mockRoom, times(1)).isEmpty();
    assertTrue(mockRoomsMap.containsKey(testRoomId), "Room should still exist in the manager");
    verify(mockRoom, times(1)).broadcastRoomStatus();
    assertNull(mockSessionRoomsMap.get(testSessionId), "Session-room mapping should be removed");
  }

  @Test
  public void leaveRoom_Success_WhenRoomIsEmptyAfterLeave() {
    // arrange
    // mockSessionPlayersMap.put(testSessionId, testPlayer);
    gameRoomManager.players.add(testPlayer);
    mockSessionRoomsMap.put(testSessionId, testRoomId);
    mockRoomsMap.put(testRoomId, mockRoom);

    given(mockRoom.isEmpty()).willReturn(true); // Room becomes empty

    // act
    gameRoomManager.leaveRoom(mockSession);

    // assert
    verify(mockRoom, times(1)).removePlayer(eq(testPlayer));
    verify(mockRoom, times(1)).isEmpty();

    assertFalse(mockRoomsMap.containsKey(testRoomId), "Room should be removed from the manager");
    verify(mockRoom, never()).broadcastRoomStatus();
    assertNull(mockSessionRoomsMap.get(testSessionId), "Session-room mapping should be removed");
  }

    @Test
    public void leaveRoom_Failure_WhenPlayerNotInAnyRoom() {
      // arrange
      // String sessionId = testSessionId;
      // given(mockSession.getId()).willReturn(sessionId);
      // gameRoomManager.players.add(testPlayer);
      // mockSessionPlayersMap.put(sessionId, testPlayer);

      // 注入 map
      // gameRoomManager.setSessionPlayersMap(mockSessionPlayersMap);

      gameRoomManager.setSessionRoomsMap(mockSessionRoomsMap);
      gameRoomManager.setRooms(mockRoomsMap);

      // act
      gameRoomManager.leaveRoom(mockSession);

      // assert
      verify(mockRoom, never()).removePlayer(any(Player.class));
      verify(mockRoom, never()).isEmpty();
      verify(mockRoom, never()).broadcastRoomStatus();

      // 应该正确清除玩家和session信息
      // assertFalse(mockSessionPlayersMap.containsKey(sessionId)); //逻辑已修改，不该在这里清除sessionPlayer
      assertFalse(mockSessionRoomsMap.containsKey(testSessionId));
    }



    @Test
    public void createRoom_Success_ShouldCreateConfigureAndStoreRoom() {
        // arrange
        int maxPlayers = 4;
        String roomName = "testRoom_tobeCreate";
        Player creatorPlayer = testPlayer;
        Session creatorSession = mockSession;
        String creatorSessionId = testSessionId;
        Long creatorUserId = testUserId;
        String creatorName = testNameforUser;

        // 将玩家注册进 gameRoomManager.players
        gameRoomManager.players.add(creatorPlayer);

        // mock session
        given(creatorSession.getId()).willReturn(creatorSessionId);
        given(creatorSession.isOpen()).willReturn(true);

        // 注入 mock map
        gameRoomManager.setSessionRoomsMap(mockSessionRoomsMap);
        gameRoomManager.setRooms(mockRoomsMap);

        // act
        GameRoom createdRoom = gameRoomManager.createRoom(maxPlayers, creatorSession, roomName);

        // assert
        assertNotNull(createdRoom);
        String createdRoomId = createdRoom.getRoomId();

        assertNotNull(createdRoomId);
        assertFalse(createdRoomId.isEmpty());
        assertEquals(creatorName, createdRoom.getOwnerName());
        assertEquals(creatorUserId, createdRoom.getOwnerId());
        assertEquals(roomName, createdRoom.getRoomName());
        assertSame(createdRoom, mockRoomsMap.get(createdRoomId));
        assertTrue(createdRoom.getPlayers().contains(creatorPlayer));
        assertEquals(createdRoomId, mockSessionRoomsMap.get(creatorSessionId));
    }




    @Test
  public void registerPlayer_Success_ShouldCreateAndStorePlayer() {
    // arrange
    String testToken = "testToken";
    String expectedAvatar = "avatar_url_123";
    String expectedSessionId = testSessionId; // Use the session ID from setUp

    given(mockUser.getName()).willReturn(testNameforUser);
    given(mockUser.getId()).willReturn(testUserId);
    given(mockUser.getAvatar()).willReturn(expectedAvatar);
    given(mockUserService.getUserByToken(eq(testToken))).willReturn(mockUser);
    assertFalse(gameRoomManager.players.contains(testPlayer)); //Player should not be registered yet
    // act
    Player registeredPlayer = gameRoomManager.registerPlayer(mockSession, testToken);

    // assert
    verify(mockUserService, times(1)).getUserByToken(eq(testToken));

    assertNotNull(registeredPlayer);
    assertEquals(testNameforUser, registeredPlayer.getName());
    assertEquals(testUserId, registeredPlayer.getUserId());
    assertEquals(expectedAvatar, registeredPlayer.getAvatar());
    assertTrue(gameRoomManager.players.contains(registeredPlayer));
  }

}
