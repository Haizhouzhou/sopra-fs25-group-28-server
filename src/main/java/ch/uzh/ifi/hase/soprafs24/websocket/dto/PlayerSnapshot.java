package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class PlayerSnapshot {
  private Long userId;
  private String name;
  // player status in game
  private Map<GemColor, Long> gems;
  private Map<GemColor, Long> bonusGems; //gems collect from development card
  private Long victoryPoints;
  private List<Long> reservedCardIds = new ArrayList<>();

  // getter and setter
  public Long getUserId(){return userId;}
  public void setUserId(Long userId){this.userId = userId;}

  public Long getVictoryPoints(){return victoryPoints;}
  public void setVictoryPoints(Long victoryPoints){this.victoryPoints = victoryPoints;}

  public Map<GemColor, Long> getGems(){return gems;}
  public void setGems(Map<GemColor, Long> gems){this.gems = gems;}

  public Map<GemColor, Long> getBonusGems(){return bonusGems;}
  public void setBonusGems(Map<GemColor, Long> bonusGems){this.bonusGems = bonusGems;}

  public List<Long> getReservedCardIds(){return reservedCardIds;}

  public void setReservedCardIds(List<Card> reservedCards) {
    if (reservedCards != null) {
        for (Card reservedCard : reservedCards) {
            this.reservedCardIds.add(reservedCard.getId());
        }
    }
  }

  public String getName() {return name;}

  public void setName(String name) {this.name = name;}

  public static PlayerSnapshot createFromPlayer(Player player){
    PlayerSnapshot snapshot = new PlayerSnapshot();
    
    snapshot.setUserId(player.getUserId());
    snapshot.setVictoryPoints(player.getVictoryPoints());
    snapshot.setGems(player.getAllGems());
    snapshot.setBonusGems(player.getAllBonusGems());
    snapshot.setReservedCardIds(player.getReservedCards());
    snapshot.name = player.getName();

    return snapshot;
  }

  
}
