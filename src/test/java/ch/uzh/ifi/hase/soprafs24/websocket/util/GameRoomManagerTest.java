package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
  private Map<String, Player> mockSessionPlayersMap;
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

    mockSessionPlayersMap = new HashMap<>();
    mockSessionRoomsMap = new HashMap<>();
    mockRoomsMap = new HashMap<>();

    gameRoomManager.setRooms(mockRoomsMap);
    gameRoomManager.setSessionRoomsMap(mockSessionRoomsMap);
    gameRoomManager.setSessionPlayersMap(mockSessionPlayersMap);

    testPlayer = new Player(mockSession, testNameforUser, testUserId);

    given(mockSession.getId()).willReturn(testSessionId);
    given(mockSession.getBasicRemote()).willReturn(mockBasicRemote);
  }

  @Test
  public void getPlayerBySession_WhenPlayerNotFoundInSessionPlayerMap_shouldReturnNull(){
    // arrange: empty map

    // act
    Player result = gameRoomManager.getPlayerBySession(mockSession);

    // assert
    assertNull(result, "return null if sessionPlayers is empty (player not found)");
  }

  @Test
  public void getPlayerBySession_WhenPlayerRegisterButNotInRoom(){
    // arrange
    mockSessionPlayersMap.put(testSessionId, testPlayer);
    // but sessionRooms is still empty

    // act
    Player result = gameRoomManager.getPlayerBySession(mockSession);

    // assert
    // Assert
    assertNotNull(result, "Result should not be null");
    // Crucially, check if it's the exact same instance stored in sessionPlayers
    assertSame(testPlayer, result, "Should return the instance from sessionPlayers map");
  }

  @Test
  public void getPlayerBySession_WhenRoomNotFound_ShouldReturnSessionPlayer() {
    // arrange
    mockSessionPlayersMap.put(testSessionId, testPlayer);
    mockSessionRoomsMap.put(testSessionId, testRoomId);
    // rooms map remains empty (no room with testRoomId)

    // act
    Player result = gameRoomManager.getPlayerBySession(mockSession);

    // assert
    assertNotNull(result);
    assertSame(testPlayer, result, "Should return the instance from sessionPlayers map when room is missing");
  }

  @Test
  public void getPlayerBySession_WhenPlayerFoundInRoom_ShouldReturnRoomPlayerInstance() {
    // arrange
    // Player instance associated directly with the session
    mockSessionPlayersMap.put(testSessionId, testPlayer);
    Session mockRoomPlayerSession = mock(Session.class);
    Player roomPlayer = new Player(mockRoomPlayerSession, testNameforUser, testUserId); // Same userId

    given(mockRoom.getPlayers()).willReturn(Set.of(roomPlayer));

    mockRoomsMap.put(testRoomId, mockRoom);
    gameRoomManager.setRooms(mockRoomsMap);
    mockSessionRoomsMap.put(testSessionId, testRoomId);
    gameRoomManager.setSessionRoomsMap(mockSessionRoomsMap);

    // Sanity check: sessionPlayer and roomPlayer should be different instances
    assertNotSame(testPlayer, roomPlayer, "Test setup error: sessionPlayer and roomPlayer should be different instances");

    // act
    Player result = gameRoomManager.getPlayerBySession(mockSession);

    // assert
    assertNotNull(result);
    //the returned instance is the one from the room, not the one from sessionPlayers
    assertSame(roomPlayer, result, "Should return the instance from the GameRoom's player list");
    assertNotSame(testPlayer, result, "Should NOT return the instance from the sessionPlayers map");
  }

    @Test
    public void joinRoom_Success_WhenRoomExistsAndNotFull() throws IOException {
        // arrange
        String sessionId = testSessionId;
        given(mockSession.getId()).willReturn(sessionId);
        given(mockSession.isOpen()).willReturn(true);
        given(mockSession.getBasicRemote()).willReturn(mockBasicRemote);

        mockSessionPlayersMap.put(sessionId, testPlayer);
        mockRoomsMap.put(testRoomId, mockRoom);

        // 注入 maps
        gameRoomManager.setSessionPlayersMap(mockSessionPlayersMap);
        gameRoomManager.setSessionRoomsMap(mockSessionRoomsMap);
        gameRoomManager.setRooms(mockRoomsMap);

        given(mockRoom.isFull()).willReturn(false);

        // act
        boolean result = gameRoomManager.joinRoom(testRoomId, mockSession);

        // assert
        assertTrue(result, "joinRoom should return true on success");
        verify(mockRoom).addPlayer(eq(testPlayer));
        verify(mockRoom).broadcastRoomStatus();
        assertEquals(testRoomId, mockSessionRoomsMap.get(sessionId));

        // 检查消息内容
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockBasicRemote).sendText(messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertTrue(message.contains("\"type\":\"ROOM_JOINED\""));
        assertTrue(message.contains("\"roomId\":\"" + testRoomId + "\""));
    }


  @Test
  public void joinRoom_Failure_WhenRoomIsFull() throws IOException {
    // arrange
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
    Player leavingPlayer = new Player(mockSession, testRoomId, testUserId);
    mockSessionPlayersMap.put(testSessionId, leavingPlayer);
    mockSessionRoomsMap.put(testSessionId, testRoomId);
    mockRoomsMap.put(testRoomId, mockRoom);

    given(mockRoom.isEmpty()).willReturn(false);

    // act
    gameRoomManager.leaveRoom(mockSession);

    // assert
    verify(mockRoom, times(1)).removePlayer(eq(leavingPlayer));
    verify(mockRoom, times(1)).isEmpty();
    assertTrue(mockRoomsMap.containsKey(testRoomId), "Room should still exist in the manager");
    verify(mockRoom, times(1)).broadcastRoomStatus();
    assertNull(mockSessionRoomsMap.get(testSessionId), "Session-room mapping should be removed");
  }

  @Test
  public void leaveRoom_Success_WhenRoomIsEmptyAfterLeave() {
    // arrange
    mockSessionPlayersMap.put(testSessionId, testPlayer);
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
        String sessionId = testSessionId;
        given(mockSession.getId()).willReturn(sessionId);
        mockSessionPlayersMap.put(sessionId, testPlayer);

        // 注入 map
        gameRoomManager.setSessionPlayersMap(mockSessionPlayersMap);
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
        assertFalse(mockSessionRoomsMap.containsKey(sessionId));
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

        // 将玩家注册进 sessionPlayersMap
        mockSessionPlayersMap.put(creatorSessionId, creatorPlayer);

        // mock session
        given(creatorSession.getId()).willReturn(creatorSessionId);
        given(creatorSession.isOpen()).willReturn(true);

        // 注入 mock map
        gameRoomManager.setSessionPlayersMap(mockSessionPlayersMap);
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
    assertFalse(mockSessionPlayersMap.containsKey(expectedSessionId)); //Player should not be registered yet

    // act
    Player registeredPlayer = gameRoomManager.registerPlayer(mockSession, testToken);

    // assert
    verify(mockUserService, times(1)).getUserByToken(eq(testToken));

    assertNotNull(registeredPlayer);
    assertEquals(testNameforUser, registeredPlayer.getName());
    assertEquals(testUserId, registeredPlayer.getUserId());
    assertEquals(expectedAvatar, registeredPlayer.getAvatar());
    assertTrue(mockSessionPlayersMap.containsKey(expectedSessionId));
    assertSame(registeredPlayer, mockSessionPlayersMap.get(expectedSessionId));
  }

}
