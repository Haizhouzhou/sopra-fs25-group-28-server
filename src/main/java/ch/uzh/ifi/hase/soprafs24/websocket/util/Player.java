package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.Map;

import javax.websocket.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Player {

  private static final boolean PLAYER_NOT_READY = false;
  private static final boolean PLAYER_READY = true;


  private Session session;
  private String name;
  // TODO: add correspondance to User
  private Long userId;
  private boolean status;

  // Game status relate
  private Map<String, Long> gems;
  private Map<String, Long> bonusGems; //gems collect from development card
  private Long victoryPoints;
  
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public Player(Session session, String name, Long userId){
    this.session = session;
    this.name = name;
    this.userId = userId;
    this.status = PLAYER_NOT_READY;
  }

  // define getter
  public Session getSession(){return session;}
  public String getName(){return name;}

  public boolean getStatus(){return status;}
  public void setStatus(boolean status){this.status = status;}

  public Long getUserId(){return userId;}
  public void setUserId(Long userId){this.userId = userId;}

  // called when intialized a game
  public void initializeGameStatus(){

  }

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
