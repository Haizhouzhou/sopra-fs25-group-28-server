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

import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoomManager;
import ch.uzh.ifi.hase.soprafs24.websocket.util.WebSocketMessage;

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

      
    } catch (Exception e) {
    }

  }

  @OnError
  public void onError(Session session, Throwable throwable){
    log.error("WebSocket error for session {} : {}", session.getId(), throwable.getMessage());
    // TODO: broadcast Error to frontent?
  }
  
}
