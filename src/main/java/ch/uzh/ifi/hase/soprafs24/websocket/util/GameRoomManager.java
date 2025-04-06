package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

public class GameRoomManager {
  private static GameRoomManager instance = new GameRoomManager();

  // Map roomId to GameRooms
  private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

  // Map websocket connections(sessions) to players
  private Map<String, Player> sessionPlayers = new ConcurrentHashMap<>();

  // Map websocket connections(sessions) to Rooms
  private Map<String, String> sessionRooms = new ConcurrentHashMap<>();

  private GameRoomManager(){}

  public static GameRoomManager getInstance(){return instance;}

  /**
   * Create a new GameRoom and add it to tracked GameRoom list, 
   * access to tracked GameRoom list through '.rooms'
   * @param maxPlayers
   * @return
   */
  public GameRoom creatRoom(int maxPlayers){
    String roomId = generateRoomId();
    GameRoom room = new GameRoom(roomId, maxPlayers);
    rooms.put(roomId, room);
    return room;
  }

  public boolean joinRoom(String roomId, Session session, String playerName){
    GameRoom room = rooms.get(roomId);
    if(room == null || room.isFull()){
      return false;
    }

    Player player = new Player(session, playerName);
    room.addPlayer(player);

    sessionPlayers.put(session.getId(), player);
    sessionRooms.put(session.getId(), roomId);

    // informe new player joining
    room.broadcastePlayerJoin(playerName);

    return true;
  }

  public void leaveRoom(Session session){
    String sessionId = session.getId();
    Player player = sessionPlayers.get(sessionId);
    String roomId = sessionRooms.get(sessionId);

    if(player != null && roomId != null){
      GameRoom room = rooms.get(roomId);
      if(room != null){
        room.removePlayer(player);

        if(room.isEmpty()){
          rooms.remove(roomId);
        }else{
          room.broadcastPlayerLeave(player.getName());
        }
      }

      // clear mapping
      sessionPlayers.remove(sessionId);
      sessionRooms.remove(sessionId);
    }
  }

  private String generateRoomId(){
    return UUID.randomUUID().toString().substring(0,8);
  }

}
