package ch.uzh.ifi.hase.soprafs24.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoomManager;
import ch.uzh.ifi.hase.soprafs24.websocket.util.WebSocketMessage;

// TODO：modify endpoint
@ServerEndpoint(value = "/WebServer/")
@Component
public class WebSocketServer {

  private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GameRoomManager roomManager = GameRoomManager.getInstance();
  

  @OnOpen
  public void onOpen(Session session){
   log.info("WebSocket connection opened: {}", session.getId());

  }

  @OnClose
  public void onClose(Session session){
    log.info("WebSocket connection closed: {}", session.getId());
    roomManager.leaveRoom(session);

  }

  /**
   * handle receive information from frontend
   * @param session
   */
  @OnMessage
  public void onMessage(Session session, String message){
    try {
      WebSocketMessage wsMessage = objectMapper.readValue(message, WebSocketMessage.class);

      switch(wsMessage.getType()){
        case WebSocketMessage.TYPE_CREATE_ROOM:
          handleCreateRoom(session, wsMessage);
          break;
        case WebSocketMessage.TYPE_JOIN_ROOM:

          break;
        default:
          log.warn("Unkown message type: {}", wsMessage.getType());
      }
      
    } catch (Exception e) {
    }

  }

  @OnError
  public void onError(Session session, Throwable throwable){
    log.error("WebSocket error for session {} : {}", session.getId(), throwable.getMessage());
    // TODO: broadcast Error to frontent?
  }

  private void handleCreateRoom(Session session, WebSocketMessage message){
    try {
      // maxPlayer is 4 by default
      int maxPlayers = 4;
      if(message.getContent() != null && !message.getContent().isEmpty()){
        maxPlayers = Integer.parseInt(message.getContent());
      }

      GameRoom room = roomManager.creatRoom(maxPlayers);
      boolean joined = roomManager.joinRoom(room.getRoomId(), session, message.getPlayerName());

      if(joined){
        sendRoomCreatedMessage(session, room.getRoomId());
      }

    } catch (Exception e) {
    }
  }
  
  private void sendRoomCreatedMessage(Session session, String roomId){
    try {
      WebSocketMessage response = new WebSocketMessage();
      response.setType(WebSocketMessage.TYPE_CREATE_ROOM);
      response.setRoomId(roomId);
      session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
    } catch (Exception e) {
      log.error("Error sending room created message", e);
    }
  }

  private void handleJoinRoom(Session session, WebSocketMessage message){
    boolean joined = roomManager.joinRoom(message.getRoomId(), session, message.getPlayerName());

    if(joined){
      sendRoomJoinedMessage(session, message.getRoomId());
    }
  }

  private void sendRoomJoinedMessage(Session session, String roomId){
    try {
      WebSocketMessage response = new WebSocketMessage();
      response.setType(WebSocketMessage.TYPE_JOIN_ROOM);
      response.setRoomId(roomId);
      session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
    } catch (Exception e) {
      log.error("Error sending room joined message", e);
    }
  }
}
