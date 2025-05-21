package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Noble;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;


public class GameSnapshot {
  private String gameId;
  private List<Long> playerOrder = new ArrayList<>();
  private int currentPlayerIndex;
  private int currentRound;
  private Map<GemColor, Long> availableGems;
  
  protected int level1CardDeckSize;
  protected int level2CardDeckSize;
  protected int level3CardDeckSize;

  private List<Long> visibleLevel1cardIds = new ArrayList<>();
  private List<Long> visibleLevel2cardIds = new ArrayList<>();
  private List<Long> visibleLevel3cardIds = new ArrayList<>();

  private List<Long> visibleNobleIds = new ArrayList<>();

  private List<PlayerSnapshot> playerSnapshots;

  private String roomName;

  // getter and setter
  public String getGameId(){return gameId;}
  public void setGameId(String gameId){this.gameId = gameId;}

  public List<Long> getPlayerOrder(){return playerOrder;}
  public void setPlayerOrder(List<Player> players){
    for(Player player : players){
      this.playerOrder.add(player.getUserId());
    }
  }

  public int getCurrentPlayerIndex(){return currentPlayerIndex;}
  public void setCurrentPlayerIndex(int currentPlayerIndex){this.currentPlayerIndex = currentPlayerIndex;}

  public int getCurrentRound(){return currentRound;}
  public void setCurrentRound(int currentRound){this.currentRound = currentRound;}

  public Map<GemColor, Long> getAvailableGems(){return availableGems;}
  public void setAvailableGems(Map<GemColor, Long> availableGems){this.availableGems = availableGems;}

  public List<Long> getVisibleLevel1cardIds(){return visibleLevel1cardIds;}
  public void setVisibleLevel1cardIds(List<Card> visibleLevel1cards){
    for(Card card : visibleLevel1cards){
      this.visibleLevel1cardIds.add(card.getId());
    }
  }

  public List<Long> getVisibleLevel2cardIds(){return visibleLevel2cardIds;}
  public void setVisibleLevel2cardIds(List<Card> visibleLevel2cards){
    for(Card card : visibleLevel2cards){
      this.visibleLevel2cardIds.add(card.getId());
    }
  }

  public List<Long> getVisibleLevel3cardIds(){return visibleLevel3cardIds;}
  public void setVisibleLevel3cardIds(List<Card> visibleLevel3cards){
    for(Card card : visibleLevel3cards){
      this.visibleLevel3cardIds.add(card.getId());
    }
  }

  public List<Long> getVisibleNobleIds(){return visibleNobleIds;}
  public void setVisibleNobleIds(List<Noble> visibleNobles){
    for(Noble noble : visibleNobles){
      this.visibleNobleIds.add(noble.getId());
    }
  }

  protected int getLevel1CardDeckSize(){return level1CardDeckSize;}
  protected int getLevel2CardDeckSize(){return level2CardDeckSize;}
  protected int getLevel3CardDeckSize(){return level3CardDeckSize;}
  protected void setLevel1CardDeckSize(int level1CardDeckSize){this.level1CardDeckSize = level1CardDeckSize;}
  protected void setLevel2CardDeckSize(int level2CardDeckSize){this.level2CardDeckSize = level2CardDeckSize;}
  protected void setLevel3CardDeckSize(int level3CardDeckSize){this.level3CardDeckSize = level3CardDeckSize;}

  public List<PlayerSnapshot> getPlayerSnapshots(){return playerSnapshots;}
  public void setPlayerSnapshots(List<PlayerSnapshot> playerSnapshots){this.playerSnapshots = playerSnapshots;}

  public String getRoomName() {return roomName;}

  public void setRoomName(String roomName) {this.roomName = roomName;}


  public static GameSnapshot createFromGame(Game game){
    GameSnapshot snapshot = new GameSnapshot();

    snapshot.setGameId(game.getGameId());
    snapshot.setPlayerOrder(game.getPlayers());
    snapshot.setCurrentPlayerIndex(game.getCurrentPlayer());
    snapshot.setCurrentRound(game.getCurrentRound());
    snapshot.setAvailableGems(game.getAvailableGems());
    snapshot.setVisibleLevel1cardIds(game.getVisibleLevel1Cards());
    snapshot.setVisibleLevel2cardIds(game.getVisibleLevel2Cards());
    snapshot.setVisibleLevel3cardIds(game.getVisibleLevel3Cards());
    snapshot.setVisibleNobleIds(game.getVisibleNoble());

    snapshot.setRoomName(game.getGameRoom().getRoomName());

    snapshot.setLevel1CardDeckSize(game.getLevel1CardDeckSize());
    snapshot.setLevel2CardDeckSize(game.getLevel2CardDeckSize());
    snapshot.setLevel3CardDeckSize(game.getLevel3CardDeckSize());

    List<PlayerSnapshot> playerSnapshots = new ArrayList<>();
    for(Player player : game.getPlayers()){
      playerSnapshots.add(PlayerSnapshot.createFromPlayer(player));
    }
    snapshot.setPlayerSnapshots(playerSnapshots);

      return snapshot;
  }

}
