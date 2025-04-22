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


  private String roomName = "";
  private Long ownerId;
  private String ownerName;


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

  public String getOwnerName() {
        return ownerName;
    }

  public void setRoomName(String roomName) {this.roomName = roomName;}

  public String getRoomName() {return roomName;}

  public void setOwnerId(Long ownerId) {this.ownerId = ownerId;}

  public Long getOwnerId() {return this.ownerId;}


    /**
   * if all players are ready, host players can start the game
   */
  public void startGame(){
    // something like game.initialize()
    game = new Game(roomId, players);
    game.initialize();
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
      playerInfo.put("userId", player.getUserId()); // 添加
      playerInfo.put("room_status", player.getStatus());
      playerInfo.put("name",player.getName());
      playerInfo.put("avatar", player.getAvatar()); // 添加

        playersInfo.add(playerInfo);
    }

    roomInfo.put("players",playersInfo);
    roomInfo.put("roomName", roomName);

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

    public void broadcastRoomStatus() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "ROOM_STATE");

        List<Map<String, Object>> playerStates = new ArrayList<>();
        for (Player p : players) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("userId", p.getUserId());
            pInfo.put("name", p.getName());
            // 确保这里使用正确的方法获取状态
            pInfo.put("room_status", p.getStatus()); // 而不是某个过时的值
            pInfo.put("avatar", p.getAvatar());
            pInfo.put("isOwner", p.getUserId().equals(this.ownerId));
            playerStates.add(pInfo);
        }

        // 打印每个玩家的状态，用于调试
        for (Player p : players) {
            System.out.println("Player: " + p.getName() + ", Status: " + p.getStatus());
        }

        msg.put("players", playerStates);
        msg.put("ownerId", this.ownerId);
        msg.put("ownerName", this.ownerName);
        broadcast(JsonUtils.toJson(msg));
        System.out.println("Broadcasting room state: " + JsonUtils.toJson(msg));
    }





  public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
  }


  private Player findPlayerInRoom(Player targetPlayer){
    for(Player player : players){
      if(player.equals(targetPlayer)){
        return player;
      }
    }
    return null;
  }


  public void broadcast(String message) {
      for (Player player : players) {
          player.sendMessage(message);
      }
  }
}
