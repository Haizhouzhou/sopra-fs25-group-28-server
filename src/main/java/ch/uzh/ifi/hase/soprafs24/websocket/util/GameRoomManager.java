package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import org.springframework.beans.factory.annotation.Autowired;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

public class GameRoomManager {
  private static GameRoomManager instance = new GameRoomManager();

  private static UserService userService;
    
  // 使用 @Autowired 注入到静态变量
  @Autowired
  public void setUserService(UserService userService) {
      this.userService = userService;
  }

  // Map roomId to GameRooms
  private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

  // Map websocket connections(sessions) to players
  private Map<String, Player> sessionPlayers = new ConcurrentHashMap<>();

  // Map websocket connections(sessions) to Rooms
  private Map<String, String> sessionRooms = new ConcurrentHashMap<>();

  private GameRoomManager(){}

  public static GameRoomManager getInstance(){return instance;}

  // TODO: change name into User entity to add correspondance
  public Player registerPlayer(Session session, String token){

    User correspondingUser = userService.getUserByToken(token);

    Player player = new Player(session, correspondingUser.getName(), correspondingUser.getId());
    sessionPlayers.put(session.getId(), player);
    return player;
  }

  public void deregisterPlayer(Session session){
    String sessionId = session.getId();
    sessionPlayers.remove(sessionId);
  }

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

  public boolean joinRoom(String roomId, Session session){
    GameRoom room = rooms.get(roomId);
    if(room == null || room.isFull()){
      return false;
    }

    // Player player = new Player(session, playerName);
    Player player = sessionPlayers.get(session.getId());
    room.addPlayer(player);

    // sessionPlayers.put(session.getId(), player);
    sessionRooms.put(session.getId(), roomId);

    // informe new player joining
    room.broadcastRoomStatus();

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
          room.broadcastRoomStatus();
        }
      }
      // clear mapping
      // sessionPlayers.remove(sessionId);
      sessionRooms.remove(sessionId);
    }

    // MyWebSocketMessage message = new MyWebSocketMessage();
    // message.setType(MyWebSocketMessage.TYPE_SERVER_ROOM_LEFT);
    // message.setRoomId(roomId);
    // message.setSessionId(sessionId);
    // player.sendMessage(message);
  }

  private String generateRoomId(){
    return UUID.randomUUID().toString().substring(0,8);
  }

}
