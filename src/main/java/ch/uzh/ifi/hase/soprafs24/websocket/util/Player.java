package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;

public class Player {

  private static final boolean PLAYER_NOT_READY = false;
  private static final boolean PLAYER_READY = true;


  private Session session;
  private String name;
  // TODO: add correspondance to User
  private Long userId;
  private boolean status;
  private boolean isInitialized = false;

  // Game status relate
  private Map<GemColor, Long> gems;
  private Map<GemColor, Long> bonusGems; //gems collect from development card
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

  public Long getGem(GemColor color){
    return this.gems.get(color);
  }
  public void setGem(GemColor color, Long amount){
    if(amount < 0){
      throw new IllegalArgumentException("cannot set negative gem amount");
    }
    this.gems.put(color, amount);
  }

  public Long getBonusGem(GemColor color){
    return this.bonusGems.get(color);
  }
  public void setBonusGem(GemColor color, Long amount){
    if(amount < 0){
      throw new IllegalArgumentException("cannot set negative gem amount");
    }
    this.bonusGems.put(color, amount);
  }

  // called when intialized a game
  public void initializeGameStatus(){
    
    this.gems = new HashMap<>();
    gems.put(GemColor.BLACK, 0L);
    gems.put(GemColor.BLUE, 0L);
    gems.put(GemColor.GREEN, 0L);
    gems.put(GemColor.RED, 0L);
    gems.put(GemColor.WHITE, 0L);
    gems.put(GemColor.GOLD, 0L);

    this.bonusGems = new HashMap<>();
    bonusGems.put(GemColor.BLACK, 0L);
    bonusGems.put(GemColor.BLUE, 0L);
    bonusGems.put(GemColor.GREEN, 0L);
    bonusGems.put(GemColor.RED, 0L);
    bonusGems.put(GemColor.WHITE, 0L);
    bonusGems.put(GemColor.GOLD, 0L);

    this.victoryPoints = 0L;

    this.isInitialized = true;
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
