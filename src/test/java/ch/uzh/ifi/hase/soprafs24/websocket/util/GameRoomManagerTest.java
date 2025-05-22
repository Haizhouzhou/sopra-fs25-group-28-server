package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
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
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

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

  @Test
  public void registerPlayer_ExistingPlayer_NotInGame_ShouldUpdateSessionAndInfo() {
    // arrange
    String testToken = "testToken";
    String expectedAvatar = "avatar_url_123";
    Player existingPlayer = new Player(mockSession, testNameforUser, testUserId);

    // 玩家之前已注册但未在游戏中
    gameRoomManager.players.add(existingPlayer);

    given(mockUser.getName()).willReturn(testNameforUser);
    given(mockUser.getId()).willReturn(testUserId);
    given(mockUser.getAvatar()).willReturn(expectedAvatar);
    given(mockUserService.getUserByToken(eq(testToken))).willReturn(mockUser);

    // act
    Player result = gameRoomManager.registerPlayer(mockSession, testToken);

    // assert
    assertSame(existingPlayer, result);
    assertEquals(testNameforUser, result.getName());
    assertEquals(expectedAvatar, result.getAvatar());
    assertEquals(mockSession, result.getSession());
  }

  @Test
  public void registerPlayer_ExistingPlayer_InGame_ShouldRestoreBelongsToGameId() {
    // arrange
    String testToken = "testToken";
    String expectedAvatar = "avatar_url_123";
    Player existingPlayer = new Player(mockSession, testNameforUser, testUserId);
    existingPlayer.setIsInGame(true);

    // 模拟房间和游戏
    GameRoom mockRoom = org.mockito.Mockito.mock(GameRoom.class);
    Game mockGame = org.mockito.Mockito.mock(Game.class);

    String runningGameId = "runningGameId";
    given(mockGame.getGameState()).willReturn(Game.GameState.RUNNING);
    given(mockGame.getGameId()).willReturn(runningGameId);
    given(mockRoom.getPlayers()).willReturn(Set.of(existingPlayer));
    given(mockRoom.getGame()).willReturn(mockGame);
    given(mockGame.getPlayers()).willReturn(List.of(existingPlayer));
    // 放入房间Map
    Map<String, GameRoom> roomsMap = new HashMap<>();
    roomsMap.put("room1", mockRoom);
    gameRoomManager.setRooms(roomsMap);

    // 玩家已注册
    gameRoomManager.players.add(existingPlayer);

    given(mockUser.getName()).willReturn(testNameforUser);
    given(mockUser.getId()).willReturn(testUserId);
    given(mockUser.getAvatar()).willReturn(expectedAvatar);
    given(mockUserService.getUserByToken(eq(testToken))).willReturn(mockUser);

    // act
    Player result = gameRoomManager.registerPlayer(mockSession, testToken);

    // assert
    assertSame(existingPlayer, result);
    assertEquals(runningGameId, result.getBelongsToGameId());
    assertTrue(result.getIsInGame());
    assertEquals(testNameforUser, result.getName());
    assertEquals(expectedAvatar, result.getAvatar());
    assertEquals(mockSession, result.getSession());
  }

  @Test
  public void registerPlayer_ExistingPlayer_InGameButNotInAnyRoom_ShouldNotSetBelongsToGameId() {
    // arrange
    String testToken = "testToken";
    String expectedAvatar = "avatar_url_123";
    Player existingPlayer = new Player(mockSession, testNameforUser, testUserId);
    existingPlayer.setIsInGame(true);

    // 房间Map为空
    gameRoomManager.setRooms(new HashMap<>());
    gameRoomManager.players.add(existingPlayer);

    given(mockUser.getName()).willReturn(testNameforUser);
    given(mockUser.getId()).willReturn(testUserId);
    given(mockUser.getAvatar()).willReturn(expectedAvatar);
    given(mockUserService.getUserByToken(eq(testToken))).willReturn(mockUser);

    // act
    Player result = gameRoomManager.registerPlayer(mockSession, testToken);

    // assert
    assertSame(existingPlayer, result);
    assertNull(result.getBelongsToGameId());
    assertTrue(result.getIsInGame());
    assertEquals(testNameforUser, result.getName());
    assertEquals(expectedAvatar, result.getAvatar());
    assertEquals(mockSession, result.getSession());
  }

  @Test
  public void deregisterPlayer_PlayerExists_ShouldDisconnectAndReset() {
    // arrange
    gameRoomManager.players.add(testPlayer);
    testPlayer.setBelongsToGameId("someGameId");
    gameRoomManager.lobbySessions.add(mockSession);

    // act
    gameRoomManager.deregisterPlayer(mockSession);

    // assert
    // session 应从 lobbySessions 移除
    assertFalse(gameRoomManager.lobbySessions.contains(mockSession));
    // player 的 session 应为 null
    assertNull(testPlayer.getSession());
    // player 的 belongsToGameId 应为 null
    assertNull(testPlayer.getBelongsToGameId());
  }

  @Test
  public void deregisterPlayer_PlayerNotExists_ShouldDoNothing() {
    // arrange
    // players 集合为空
    assertFalse(gameRoomManager.players.contains(testPlayer));
    gameRoomManager.lobbySessions.add(mockSession);

    // act
    gameRoomManager.deregisterPlayer(mockSession);

    // assert
    // session 应从 lobbySessions 移除
    assertFalse(gameRoomManager.lobbySessions.contains(mockSession));
  }

  @Test
  public void deregisterPlayer_PlayerExists_SessionNotInLobbySessions() {
    // arrange
    gameRoomManager.players.add(testPlayer);
    testPlayer.setBelongsToGameId("someGameId");
    // mockSession 没加入 lobbySessions

    // act
    gameRoomManager.deregisterPlayer(mockSession);

    // assert
    assertNull(testPlayer.getSession());
    assertNull(testPlayer.getBelongsToGameId());
  }

  @Test
  public void broadcastToLobby_AllSessionsOpen_ShouldSendMessageToAll() throws Exception {
    // arrange
    Session mockSession1 = mock(Session.class);
    Session mockSession2 = mock(Session.class);
    RemoteEndpoint.Basic mockBasicRemote1 = mock(RemoteEndpoint.Basic.class);
    RemoteEndpoint.Basic mockBasicRemote2 = mock(RemoteEndpoint.Basic.class);

    given(mockSession1.isOpen()).willReturn(true);
    given(mockSession2.isOpen()).willReturn(true);
    given(mockSession1.getBasicRemote()).willReturn(mockBasicRemote1);
    given(mockSession2.getBasicRemote()).willReturn(mockBasicRemote2);

    gameRoomManager.lobbySessions.clear();
    gameRoomManager.lobbySessions.add(mockSession1);
    gameRoomManager.lobbySessions.add(mockSession2);

    Map<String, Object> msg = new HashMap<>();
    msg.put("msg", "hello lobby");

    // act
    gameRoomManager.broadcastToLobby(msg);

    // assert
    verify(mockBasicRemote1, times(1)).sendText(contains("\"msg\":\"hello lobby\""));
    verify(mockBasicRemote2, times(1)).sendText(contains("\"msg\":\"hello lobby\""));
  }

  @Test
  public void broadcastToLobby_SkipClosedSessions() throws Exception {
    // arrange
    Session openSession = mock(Session.class);
    Session closedSession = mock(Session.class);
    RemoteEndpoint.Basic openRemote = mock(RemoteEndpoint.Basic.class);

    given(openSession.isOpen()).willReturn(true);
    given(closedSession.isOpen()).willReturn(false);
    given(openSession.getBasicRemote()).willReturn(openRemote);

    gameRoomManager.lobbySessions.clear();
    gameRoomManager.lobbySessions.add(openSession);
    gameRoomManager.lobbySessions.add(closedSession);

    Map<String, Object> msg = new HashMap<>();
    msg.put("v", 123);

    // act
    gameRoomManager.broadcastToLobby(msg);

    // assert
    verify(openRemote).sendText(contains("\"v\":123"));
    verify(closedSession, never()).getBasicRemote();
  }

  @Test
  public void broadcastToLobby_ObjectMapperThrowsException_ShouldNotSend() throws Exception {
    // arrange
    Object badObject = new Object() {
      @Override
      public String toString() { throw new RuntimeException("fail"); }
    };

    Session session = mock(Session.class);
    given(session.isOpen()).willReturn(true);
    RemoteEndpoint.Basic remote = mock(RemoteEndpoint.Basic.class);
    given(session.getBasicRemote()).willReturn(remote);

    gameRoomManager.lobbySessions.clear();
    gameRoomManager.lobbySessions.add(session);

    // act
    gameRoomManager.broadcastToLobby(badObject);

    // assert
    verify(remote, never()).sendText(anyString());
  }

  @Test
  public void broadcastToLobby_SendTextThrowsException_OtherSessionsStillSent() throws Exception {
    // arrange
    Session session1 = mock(Session.class);
    Session session2 = mock(Session.class);
    RemoteEndpoint.Basic remote1 = mock(RemoteEndpoint.Basic.class);
    RemoteEndpoint.Basic remote2 = mock(RemoteEndpoint.Basic.class);

    given(session1.isOpen()).willReturn(true);
    given(session2.isOpen()).willReturn(true);
    given(session1.getBasicRemote()).willReturn(remote1);
    given(session2.getBasicRemote()).willReturn(remote2);

    // 让session1的发送抛异常
    doThrow(new IOException("fail send")).when(remote1).sendText(anyString());

    gameRoomManager.lobbySessions.clear();
    gameRoomManager.lobbySessions.add(session1);
    gameRoomManager.lobbySessions.add(session2);

    Map<String, Object> msg = new HashMap<>();
    msg.put("foo", "bar");

    // act
    gameRoomManager.broadcastToLobby(msg);

    // assert
    verify(remote1, times(1)).sendText(contains("\"foo\":\"bar\""));
    verify(remote2, times(1)).sendText(contains("\"foo\":\"bar\""));
  }

}
