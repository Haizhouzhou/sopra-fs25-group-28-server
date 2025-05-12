package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;

public class Player {

  private static final boolean PLAYER_NOT_READY = false;
  private static final boolean PLAYER_READY = true;


  private Session session;
  private String name;
  private String avatar;

    // TODO: add correspondance to User
  private Long userId;
  private boolean status = false;
  private boolean isInitialized = false;

  // Game status relate
  private Map<GemColor, Long> gems;
  private Map<GemColor, Long> bonusGems; //gems collect from development card
  private Long victoryPoints;
  public List<Card> reservedCards = new ArrayList<>();
  private final Object sendLock = new Object(); // 发送锁
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public Player(Session session, String name, Long userId){
    this.session = session;
    this.name = name;
    this.userId = userId;
    this.status = PLAYER_NOT_READY;
  }

  // getters
  public Session getSession(){return session;}
  public String getName(){return name;}

  public boolean getStatus(){return status;}
  public void setStatus(boolean status) {
      System.out.println("Player " + this.name + " (id:" + this.userId +
              ") status changing from " + this.status +
              " to " + status + " [instance: " +
              System.identityHashCode(this) + "]");
      this.status = status;
  }

  public boolean removeCardFromReserved(Card targetCard){
    if(reservedCards.contains(targetCard)){
      reservedCards.remove(targetCard);
      return true;
    }
    return false;
  }

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
  public Map<GemColor, Long> getAllGems(){return gems;}

  public Long getBonusGem(GemColor color){
    return this.bonusGems.get(color);
  }
  public void setBonusGem(GemColor color, Long amount){
    if(amount < 0){
      throw new IllegalArgumentException("cannot set negative gem amount");
    }
    this.bonusGems.put(color, amount);
  }
  public Map<GemColor, Long> getAllBonusGems(){return bonusGems;}

  public Long getVictoryPoints(){return victoryPoints;}
  public void setVictoryPoints(Long victoryPoints){this.victoryPoints = victoryPoints;}

  public List<Card> getReservedCards(){return reservedCards;}


  public String getAvatar() {return avatar;}

  public void setAvatar(String avatar) {this.avatar = avatar;}

  public void setSession(Session session) {this.session = session;}

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
  public void sendMessage(Object message) {
      if (session == null || !session.isOpen()) {
          System.err.println("WebSocket session is closed. Cannot send message.");
          return;
      }

      try {
          String messageStr;
          if (message instanceof String) {
              messageStr = (String) message;
          } else {
              messageStr = objectMapper.writeValueAsString(message); // 根据注解格式化 JSON
          }

          synchronized (sendLock) {
              session.getBasicRemote().sendText(messageStr);
          }

          System.out.println("Message sent to user: " + messageStr);

      } catch (IOException e) {
          System.err.println("Failed to send message to user:");
          e.printStackTrace();
      }
  }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Player player = (Player) o;

        return userId != null && userId.equals(player.userId);
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }

}
