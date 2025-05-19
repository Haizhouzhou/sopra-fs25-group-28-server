package ch.uzh.ifi.hase.soprafs24.websocket;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoomManager;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GeminiHint;
import ch.uzh.ifi.hase.soprafs24.websocket.util.MyWebSocketMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class WebSocketServerTest {

  @Mock
  private GameRoomManager roomManager;

  @Mock
  private GeminiHint geminiHint;

  @Mock
  private Session mockSession;

  @Mock
  private RemoteEndpoint.Basic mockRemoteEndpoint;

  @Mock
  private Player mockPlayer;

  @Mock
  private GameRoom mockRoom;

  @Mock
  private Game mockGame;
  @Captor
  private ArgumentCaptor<String> stringCaptor; 

  @Captor
  private ArgumentCaptor<MyWebSocketMessage> messageCaptor;

  @InjectMocks
  private WebSocketServer webSocketServer;

  private final ObjectMapper objectMapper = new ObjectMapper(); 
  private final String testToken = "test-token-123";
  private final String testRoomId = "room-abc";
  private final String testSessionId = "client-session-xyz"; 
  private final Long testUserId = 1L;

  @BeforeEach
  // @SuppressWarnings("unused")
  void setUp() throws IOException { 
    MockitoAnnotations.openMocks(this);
    // mock session setup
    given(mockSession.getId()).willReturn("session-id-1");
    given(mockSession.getBasicRemote()).willReturn(mockRemoteEndpoint);

    // mock player setup
    given(mockPlayer.getUserId()).willReturn(testUserId);

    given(roomManager.getPlayerBySession(mockSession)).willReturn(mockPlayer);
    given(roomManager.getRoom(testRoomId)).willReturn(mockRoom);
    given(mockRoom.getGame()).willReturn(mockGame);
  }

  // test for onOpen and onClose

  @Test
  void onOpen_shouldRegisterPlayer() {
  // act
  webSocketServer.onOpen(mockSession, testToken);

  // assert
  verify(roomManager).registerPlayer(mockSession, testToken);
  }

  @Test
  void onClose_shouldLeaveRoomAndDeregisterPlayer() {
    // act
    webSocketServer.onClose(mockSession);

    // assert
    verify(roomManager).leaveRoom(mockSession);
    verify(roomManager).deregisterPlayer(mockSession);
  }

  // test for onMessage
  @Test
  void onMessage_withNullType_shouldLogWarningAndReturn() {
      // arrange
      String jsonMessage = "{\"roomId\":\"" + testRoomId + "\"}"; // Message without type

      // act
      webSocketServer.onMessage(mockSession, jsonMessage);

      // assert
      verify(roomManager, never()).createRoom(anyInt(), any(Session.class), anyString());
      verify(roomManager, never()).joinRoom(anyString(), any(Session.class));
  }


    @Test
    void onMessage_withUnknownType_shouldLogWarningAndReturn() throws JsonProcessingException {
        // arrange
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", "UNKNOWN_TYPE");
        messageMap.put("roomId", testRoomId);
        String jsonMessage = objectMapper.writeValueAsString(messageMap);

        // act
        webSocketServer.onMessage(mockSession, jsonMessage);

        // assert
        verify(roomManager, never()).createRoom(anyInt(), any(Session.class), anyString());
    }


    @Test
    void handleCreateRoom_shouldCallRoomManagerAndSendResponse() throws IOException {
        // arrange
        MyWebSocketMessage wsMessage = new MyWebSocketMessage();
        wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_CREATE_ROOM);
        wsMessage.setSessionId(testSessionId);
        wsMessage.setContent(Map.of("maxPlayers", 4, "roomName", "Test Room"));
        String jsonMessage = objectMapper.writeValueAsString(wsMessage);

        given(roomManager.createRoom(anyInt(), eq(mockSession), anyString())).willReturn(mockRoom);
        given(mockRoom.getRoomId()).willReturn(testRoomId);

        // act
        webSocketServer.onMessage(mockSession, jsonMessage);

        // assert
        verify(roomManager).createRoom(eq(4), eq(mockSession), eq("Test Room"));
        verify(mockRemoteEndpoint).sendText(stringCaptor.capture());
    }


  @Test
  void handleJoinRoom_shouldCallRoomManagerAndBroadcast() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_JOIN_ROOM);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    given(roomManager.joinRoom(testRoomId, mockSession)).willReturn(true);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(roomManager).joinRoom(testRoomId, mockSession);
    verify(roomManager).registerClientSessionId(testSessionId, mockPlayer);
    verify(mockRoom).broadcastRoomStatus();
  }

  @Test
  void handleLeaveRoom_shouldCallRoomManager() throws JsonProcessingException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_LEAVE_ROOM);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(roomManager).leaveRoom(mockSession);
  }

  @Test
  void handleMessage_shouldBroadcastChatMessage() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_PLAYER_MESSAGE);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    wsMessage.setContent("test content");
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).broadcastMessage(messageCaptor.capture());
    assertEquals(MyWebSocketMessage.TYPE_SERVER_CHAT_MESSAGE, messageCaptor.getValue().getType());
    assertEquals("test content", messageCaptor.getValue().getContent());
  }

  @Test
  void handleGetRooms_shouldCallRoomManagerAndSendList() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_GET_ROOMS);
    wsMessage.setContent(Map.of("displayName", "TestUser", "userId", testUserId));
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // mock roomManager response
    given(roomManager.getAllRooms()).willReturn(List.of(mockRoom));

    given(mockRoom.getRoomName()).willReturn("Test Room Name");
    given(mockRoom.getOwnerName()).willReturn("Owner");
    given(mockRoom.getCurrentPlayerCount()).willReturn(2);
    given(mockRoom.getMaxPlayer()).willReturn(4);


    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(roomManager).registerUsername(testUserId, "TestUser");
    verify(roomManager).getAllRooms();
    verify(mockRemoteEndpoint).sendText(stringCaptor.capture());
  }

    @Test
    void handlePlayerStatus_shouldSetStatusAndBroadcast() throws IOException {
        // arrange
        boolean targetStatus = true;
        MyWebSocketMessage wsMessage = new MyWebSocketMessage();
        wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_PLAYER_STATUS);
        wsMessage.setRoomId(testRoomId);
        wsMessage.setSessionId(testSessionId);
        wsMessage.setContent(Map.of("userId", testUserId, "status", targetStatus));
        String jsonMessage = objectMapper.writeValueAsString(wsMessage);

        // mock player behavior
        given(mockPlayer.getUserId()).willReturn(testUserId);
        Set<Player> playersInRoom = Set.of(mockPlayer);
        given(mockRoom.getPlayers()).willReturn(playersInRoom);
        given(roomManager.getRoom(testRoomId)).willReturn(mockRoom);
        given(roomManager.getPlayerBySession(mockSession)).willReturn(mockPlayer);

        // act
        webSocketServer.onMessage(mockSession, jsonMessage);

        // assert
        verify(mockPlayer).setStatus(targetStatus);
        verify(mockRoom).broadcastRoomStatus();
    }



  @Test
  void handleStartGame_shouldCallRoomStartGameAndBroadcastState() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_START_GAME);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    given(mockRoom.getOwnerId()).willReturn(testUserId);
    given(mockRoom.getPlayers()).willReturn(Collections.singleton(mockPlayer)); 
    given(mockPlayer.getStatus()).willReturn(true); 
    given(mockGame.getGameInformation()).willReturn(mock(GameSnapshot.class));

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).startGame();
    verify(mockRoom).broadcastMessage(messageCaptor.capture());
    assertEquals(MyWebSocketMessage.TYPE_SERVER_GAME_STATE, messageCaptor.getValue().getType());
  }

  @Test
  void handleBuyCard_shouldCallRoomHandler() throws IOException {
    // arrange
    String cardIdToBuy = "card-5";
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_BUY_CARD);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    wsMessage.setContent(Map.of("target", cardIdToBuy));
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).handleBuyCard(mockPlayer, cardIdToBuy);
  }

  @Test
  void handleBuyCard_whenFails_shouldCallSendError() throws IOException {
    // arrange
    String cardIdToBuy = "card-5";
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_BUY_CARD);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    wsMessage.setContent(Map.of("target", cardIdToBuy));
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    given(mockRoom.handleBuyCard(mockPlayer, cardIdToBuy)).willReturn(false); // mock failure

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).handleBuyCard(mockPlayer, cardIdToBuy);
    verify(mockRoom).sendErrorToPlayer(eq(mockPlayer), anyString());
  }

  @Test
  void handleReserveCard_shouldCallRoomHandler() throws IOException {
    // arrange
    String cardIdToReserve = "card-10";
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_RESERVE_CARD);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    wsMessage.setContent(Map.of("target", cardIdToReserve));
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).handleReserveCard(mockPlayer, cardIdToReserve);
  }

  @Test
  void handleEndTurn_shouldCallRoomHandler() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_END_TURN);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).handleEndTurn(mockPlayer);
  }

  @Test
  void handleAiHint_shouldCallGeminiAndSendHint() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_AI_HINT);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    String expectedHint = "Buy the blue card.";
    given(mockGame.isPlayerTurn(mockPlayer)).willReturn(true); // Assume it's player's turn
    given(geminiHint.generateSplendorHint(mockGame)).willReturn(expectedHint);
    mockPlayer.hintAvailable = true;

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(geminiHint).generateSplendorHint(mockGame);
    verify(mockPlayer).sendMessage(messageCaptor.capture()); // Verify hint sent to player
    assertEquals(MyWebSocketMessage.TYPE_SERVER_AI_HINT, messageCaptor.getValue().getType());
    assertEquals(Map.of("hint", expectedHint), messageCaptor.getValue().getContent());
  }

  @Test
  void handleTakeThreeGems_shouldCallRoomHandler() throws IOException {
    // arrange
    List<String> colors = List.of("RED", "BLUE", "GREEN");
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_TAKE_THREE_GEMS);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    wsMessage.setContent(Map.of("colors", colors));
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).handleTakeThreeGems(mockPlayer, colors);
  }

  @Test
  void handleTakeDoubleGem_shouldCallRoomHandler() throws IOException {
    // arrange
    String color = "BLUE";
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_TAKE_DOUBLE_GEM);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    wsMessage.setContent(Map.of("color", color));
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).handleTakeDoubleGem(mockPlayer, color);
  }

  @Test
  void handleGetRoomState_shouldCallBroadcastStatus() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_GET_ROOM_STATE);
    wsMessage.setRoomId(testRoomId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).broadcastRoomStatus();
  }

  @Test
  void broadcastRoomListToLobby_shouldCallRoomManagerAndBroadcast() {
    // arrange
    given(roomManager.getAllRooms()).willReturn(List.of(mockRoom));
    given(mockRoom.getRoomId()).willReturn("id1");
    given(mockRoom.getRoomName()).willReturn("Room1");
    given(mockRoom.getOwnerName()).willReturn("Owner1");
    given(mockRoom.getCurrentPlayerCount()).willReturn(2);
    given(mockRoom.getMaxPlayer()).willReturn(4);

    // act
    webSocketServer.broadcastRoomListToLobby();

    // assert
    ArgumentCaptor<MyWebSocketMessage> msgCaptor = ArgumentCaptor.forClass(MyWebSocketMessage.class);
    verify(roomManager).broadcastToLobby(msgCaptor.capture());

    MyWebSocketMessage sentMsg = msgCaptor.getValue();
    assertEquals(MyWebSocketMessage.TYPE_SERVER_ROOM_LIST, sentMsg.getType());
    assertTrue(sentMsg.getContent() instanceof List);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) sentMsg.getContent();
    assertEquals(1, content.size());
    assertEquals("id1", content.get(0).get("roomId"));
    assertEquals("Room1", content.get(0).get("roomName"));
    assertEquals("Owner1", content.get(0).get("owner"));
    assertEquals(2, content.get(0).get("players"));
    assertEquals(4, content.get(0).get("maxPlayers"));
  }

  @Test
  void handleGetGameState_shouldCallUpdateGameStateForPlayer() throws IOException {
    // arrange
    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_GET_GAME_STATE);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    given(roomManager.getPlayerByClientSessionId(testSessionId)).willReturn(mockPlayer);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom).updateGameStateForPlayer(mockPlayer);
  }

  @Test
  void handleGetGameState_playerNull_shouldNotCallUpdateGameState() throws IOException {
    // arrange
    given(roomManager.getPlayerByClientSessionId(testSessionId)).willReturn(null);
    given(roomManager.getPlayerBySession(mockSession)).willReturn(null);

    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_GET_GAME_STATE);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom, never()).updateGameStateForPlayer(any());
  }

  @Test
  void handleGetGameState_roomNull_shouldNotCallUpdateGameState() throws IOException {
    // arrange
    given(roomManager.getRoom(testRoomId)).willReturn(null);
    given(roomManager.getPlayerByClientSessionId(testSessionId)).willReturn(mockPlayer);

    MyWebSocketMessage wsMessage = new MyWebSocketMessage();
    wsMessage.setType(MyWebSocketMessage.TYPE_CLIENT_GET_GAME_STATE);
    wsMessage.setRoomId(testRoomId);
    wsMessage.setSessionId(testSessionId);
    String jsonMessage = objectMapper.writeValueAsString(wsMessage);

    // act
    webSocketServer.onMessage(mockSession, jsonMessage);

    // assert
    verify(mockRoom, never()).updateGameStateForPlayer(any());
  }

}
