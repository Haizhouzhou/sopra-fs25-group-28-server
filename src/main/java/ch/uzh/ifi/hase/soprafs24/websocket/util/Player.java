package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;

import javax.websocket.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Player {

  private Session session;
  private String name;
  // TODO: add correspondance to User
  
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public Player(Session session, String name){
    this.session = session;
    this.name = name;
  }

  // define getter
  public Session getSession(){return session;}
  public String getName(){return name;}

  /**
   * send format message to player
   * @param message either a String or a WebSocketMessage Object
   */
  public void sendMessage(Object message){
    try {
      String messageStr;
      if (message instanceof String){
        messageStr = (String) message;
      }else{
        messageStr = objectMapper.writeValueAsString(message); // forming information according to @JasonProperty and getter function
      }

      session.getBasicRemote().sendText(messageStr);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
}
