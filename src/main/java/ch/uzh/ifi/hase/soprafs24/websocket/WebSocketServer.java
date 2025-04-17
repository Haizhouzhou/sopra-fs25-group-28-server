package ch.uzh.ifi.hase.soprafs24.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoomManager;
import ch.uzh.ifi.hase.soprafs24.websocket.util.MyWebSocketMessage;

// TODO：modify endpoint
@ServerEndpoint(value = "/WebServer/{tokenParam}")
@Component
public class WebSocketServer {

  private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GameRoomManager roomManager = GameRoomManager.getInstance();
  

  @OnOpen
  public void onOpen(Session session, @PathParam("tokenParam") String token){
   log.info("WebSocket connection opened: {}", session.getId());
   // TODO: incorperate UserService, obtain userId
  //  Long userId = 1L;
  //  String token = "a dummy token";
   roomManager.registerPlayer(session, token);
    // log.info("player registered");
  }

  @OnClose
  public void onClose(Session session){
    log.info("WebSocket connection closed: {}", session.getId());
    roomManager.leaveRoom(session);
    roomManager.deregisterPlayer(session);
  }

  /**
   * handle receive information from frontend
   * @param session
   */
  @OnMessage
  public void onMessage(Session session, String message){
    // log.info("receive message");
    try {      
      MyWebSocketMessage wsMessage = objectMapper.readValue(message, MyWebSocketMessage.class);
      // log.info("message  type: {}",wsMessage.getType());
      switch(wsMessage.getType()){
        case MyWebSocketMessage.TYPE_CLIENT_CREATE_ROOM -> handleCreateRoom(session, wsMessage);
        case MyWebSocketMessage.TYPE_CLIENT_JOIN_ROOM -> handleJoinRoom(session, wsMessage);
        case MyWebSocketMessage.TYPE_CLIENT_LEAVE_ROOM -> handleLeaveRoom(session);
        case MyWebSocketMessage.TYPE_CLIENT_PLAYER_MESSAGE -> handleMessage(session, wsMessage);
        default -> log.warn("Unkown message type: {}", wsMessage.getType());
      }
      
    } catch (Exception e) {
      log.info("error during message handling: {}", e);
    }

  }

  @OnError
  public void onError(Session session, Throwable throwable){
    log.error("WebSocket error for session {} : {}", session.getId(), throwable.getMessage());
    // TODO: broadcast Error to frontent?
  }

  private void handleCreateRoom(Session session, MyWebSocketMessage message){
    try {
      // maxPlayer is 4 by default
      int maxPlayers = 4;
      if(message.getContent() != null){
        // maxPlayers = Integer.parseInt(message.getContent());
      }

      GameRoom room = roomManager.creatRoom(maxPlayers);
      boolean joined = roomManager.joinRoom(room.getRoomId(), session);

    } catch (Exception e) {

    }
  }

  private void handleJoinRoom(Session session, MyWebSocketMessage message){
    boolean joined = roomManager.joinRoom(message.getRoomId(), session);

    // if(joined){
    //   sendRoomJoinedMessage(session, message.getRoomId());
    // }
  }

  private void handleLeaveRoom(Session session){
    roomManager.leaveRoom(session);
  }

  private void handleMessage(Session session, MyWebSocketMessage message){
    // TODO: implement this
  }

    public GameRoomManager getRoomManager() {
        return roomManager;
    }
}
