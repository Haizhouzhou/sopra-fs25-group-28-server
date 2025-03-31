package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {

  private String roomId;
  private int maxPlayer;
  private Set<Player> players = ConcurrentHashMap.newKeySet();

  public GameRoom(String roomId, int maxPlayer){
    this.roomId = roomId;
    this.maxPlayer = maxPlayer;
  }

  public String getRoomId(){return roomId;}

  public int getMaxPlayer(){return maxPlayer;}

  public int getCurrentPlayerCount(){return players.size();}

  public Set<Player> getPlayers(){return players;}

  public boolean isFull(){return players.size() >= maxPlayer;}

  public boolean isEmpty(){return players.isEmpty();}

  public void addPlayer(Player player){
    players.add(player);
  }

  public void removePlayer(Player player){
    players.remove(player);
  }

  /**
   * broadcast Message to all players in the room
   * @param message either a String or a WebSocketMessage Object
   */
  public void broadcastMessage(Object message){
    for(Player player : players){
      player.sendMessage(message);
    }
  }

  public void broadcastePlayerJoin(String playerName){
    WebSocketMessage message = new WebSocketMessage();
    message.setType(WebSocketMessage.TYPE_JOIN_ROOM);
    message.setPlayerName(playerName);
    broadcastMessage(message);
  }

  public void broadcastPlayerLeave(String playerName){
    WebSocketMessage message = new WebSocketMessage();
    message.setType(WebSocketMessage.TYPE_LEAVE_ROOM);
    message.setPlayerName(playerName);
    broadcastMessage(message);
  }
  
}
