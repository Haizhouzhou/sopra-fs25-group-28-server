package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

public class GameRoom {

  private static final boolean ROOM_READY = true;
  private static final boolean ROOM_WAITING = false;

  private String roomId;
  private int maxPlayer;
  private Set<Player> players = ConcurrentHashMap.newKeySet();
  private boolean roomStatus;
  private Game game;

  public GameRoom(String roomId, int maxPlayer){
    this.roomId = roomId;
    this.maxPlayer = maxPlayer;
    this.roomStatus = ROOM_WAITING;
  }

  public String getRoomId(){return roomId;}

  public int getMaxPlayer(){return maxPlayer;}

  public int getCurrentPlayerCount(){return players.size();}

  public Set<Player> getPlayers(){return players;}

  public boolean isFull(){return players.size() >= maxPlayer;}

  public boolean isEmpty(){return players.isEmpty();}

  /**
   * if all players are ready, host players can start the game
   */
  public void startGame(){
    // something like game.initialize()
    game = new Game(roomId, players);
  }

  /**
   * if any players reach the goal, call this function and save game record
   * TODO: to be implemente
   */
  public void EndGame(){

  }

  private boolean getRoomStatus(){
    this.roomStatus = ROOM_READY;
    for(Player player : players){
      roomStatus = roomStatus && player.getStatus();
    }
    return roomStatus;
  }

  public Map<String,Object> getRoomInformation(){
    Map<String, Object> roomInfo = new HashMap<>();

    roomInfo.put("maxPlayers", maxPlayer);
    roomInfo.put("currentPlayers", getCurrentPlayerCount());
    roomInfo.put("isReady", getRoomStatus());

    List<Map<String,Object>> playersInfo = new ArrayList<>();
    for(Player player : players){
      Map<String,Object> playerInfo = new HashMap<>();
      // TODO: add user id here, something like
      // playerInfo.put("playerId",player.getUser().getname);
      playerInfo.put("name",player.getName());
      playerInfo.put("room_status", player.getStatus());
      playersInfo.add(playerInfo);
    }

    roomInfo.put("players",playersInfo);
    return roomInfo;
  }

  public void addPlayer(Player player){
    players.add(player);
  }

  public void removePlayer(Player player){
    players.remove(player);
  }

  public void changePlayerStatus(Player targetPlayer){
    Player actualPlayer = findPlayerInRoom(targetPlayer);
    // switch status
    boolean currentStatus = actualPlayer.getStatus();
    actualPlayer.setStatus(!currentStatus);
    
    // update and broadcastRoomStatus
    broadcastRoomStatus();
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

  public void broadcastRoomStatus(){
    Map<String, Object> roomInfo = getRoomInformation();

    MyWebSocketMessage message = new MyWebSocketMessage();
    message.setType(MyWebSocketMessage.TYPE_SERVER_ROOM_STATE);
    message.setRoomId(roomId);
    message.setContent(roomInfo);

    broadcastMessage(message);

  }

  private Player findPlayerInRoom(Player targetPlayer){
    for(Player player : players){
      if(player.equals(targetPlayer)){
        return player;
      }
    }
    return null;
  }
  
}
