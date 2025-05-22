package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;



@Component
public class GameRoomManager {

    private final UserService userService;

    private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    protected final Set<Player> players = ConcurrentHashMap.newKeySet();
    // private Map<String, Player> sessionPlayers = new ConcurrentHashMap<>(); // session may change during app functioning
    private Map<String, String> sessionRooms = new ConcurrentHashMap<>();

    protected final Set<Session> lobbySessions = ConcurrentHashMap.newKeySet();

    private Map<Long, String> userIdToUsername = new ConcurrentHashMap<>();

    @Autowired
    public GameRoomManager(UserService userService) {
    this.userService = userService;
    }


    public Player registerPlayer(Session session, String token) {
        User correspondingUser = userService.getUserByToken(token);
        Long userId = correspondingUser.getId();

        // 查找是否已有该用户的 Player 实例（用于重新连接时复用）
        Player existing = players.stream()
            .filter(p -> p.getUserId().equals(userId))
            .findFirst()
            .orElse(null);

        Player player;
        if (existing != null) {
            player = existing;
            player.setSession(session);
            for(GameRoom room : rooms.values()){
                System.out.println("roomId:"+room.getRoomId()+ " contains(player)? = " + (room.getPlayers().contains(player)));
                System.out.println("roomId:"+room.getRoomId()+ " .getGame().getPlayers().contains(player)? = " + (room.getGame().getPlayers().contains(player)));
                System.out.println("room.getGame().getGameState() = "+ (room.getGame() != null ? room.getGame().getGameState() : null));

                if(player.getIsInGame() && room.getGame() != null && room.getGame().getGameState() == Game.GameState.RUNNING){
                    System.out.println("roomId:"+room.getRoomId()+ " .getGame().getPlayers().contains(player)? = " + (room.getGame().getPlayers().contains(player)));
                    if(room.getGame().getPlayers().contains(player)){
                        System.out.println("玩家刷新，所属游戏id映射回去 " );
                        player.setBelongsToGameId(room.getGame().getGameId());   
                        System.out.println("registerPlayer对象地址：" + System.identityHashCode(player));
                    }
                }
                
                System.out.println("player.getBelongsToGameId:" + ((player.getBelongsToGameId()!=null) ? player.getBelongsToGameId() : null));
            }
        } else {
            player = new Player(session, correspondingUser.getName(), userId);
            players.add(player);
        }

        player.setName(correspondingUser.getName());
        player.setAvatar(correspondingUser.getAvatar());
        System.out.println("[registerPlayer] avatar = " + correspondingUser.getAvatar());

        // 加入lobby
        lobbySessions.add(session);

        return player;
    }


    public void deregisterPlayer(Session session) {
        // 找到对应的Player / find corresponding player
        Player player = players.stream()
            .filter(p-> p.getSession() != null && p.getSession().getId().equals(session.getId()))
            .findFirst()
            .orElse(null);
        
        // remove from lobbySession
        lobbySessions.remove(session);

        // 断开session引用 / disconnect session instead of remove Player
        if(player != null){
            player.setSession(null);

            // reset player belongs to null game
            player.setBelongsToGameId(null);
            System.out.println("call setBelongsToGameId(null) in deregisterPlayer");
        }
        
    }

    /**
     * getter and setter used for unit test
     * 
     */
    public Map<String, GameRoom> getRooms(){return rooms;}
    public void setRooms(Map<String, GameRoom> rooms){this.rooms = rooms;}
    // public Map<String, Player> getSessionPlayersMap(){return sessionPlayers;}
    // public void setSessionPlayersMap(Map<String, Player> sessionPlayers){this.sessionPlayers = sessionPlayers;}
    public Map<String, String> getSessionRoomsMap(){return sessionRooms;}
    public void setSessionRoomsMap(Map<String, String> sessionRooms){this.sessionRooms = sessionRooms;}

    public GameRoom createRoom(int maxPlayers, Session session, String roomName) {
        Player player = getPlayerBySession(session);
        if (player == null || session == null || !session.isOpen()) {
            System.out.println("createRoom: session is null or closed");
            return null;
        }

        String roomId = generateRoomId();
        GameRoom room = new GameRoom(roomId, maxPlayers, userService);

        room.setOwnerName(player.getName());
        room.setOwnerId(player.getUserId());
        room.setRoomName(roomName);

        // reset player ready status --> not ready
        player.setStatus(false);
        // reset player belongs to null game
        player.setBelongsToGameId(null);
        System.out.println("call setBelongsToGameId(null) in createRoom");

        rooms.put(roomId, room);
        room.addPlayer(player);
        sessionRooms.put(session.getId(), roomId);

        // 加入了房间，离开lobby
        lobbySessions.remove(session);

        room.broadcastRoomStatus();
        return room;
    }

    public boolean joinRoom(String roomId, Session session) {
        Player player = getPlayerBySession(session);
        if ( session == null || player == null || !session.isOpen()) {
            System.err.println("joinRoom: session is null or closed");
            return false;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null || room.isFull()) {
            System.err.println("joinRoom: room is null or full");
            return false;
        }

        room.addPlayer(player);
        sessionRooms.put(session.getId(), roomId);

        // reset player ready status --> not ready
        player.setStatus(false);
        // reset player belongs to null game
        // player.setBelongsToGameId(null);
        // System.out.println("call setBelongsToGameId(null) in joinRoom"); 

        // 离开lobby
        lobbySessions.remove(session);

        room.broadcastRoomStatus();

        try {
            Map<String, Object> joinedMessage = new ConcurrentHashMap<>();
            joinedMessage.put("type", "ROOM_JOINED");
            joinedMessage.put("roomId", roomId);
            session.getBasicRemote().sendText(JsonUtils.toJson(joinedMessage));
        } catch (Exception e) {
            System.err.println("GameRoomManager.joinRoom Exception :"+ e);
        }

        return true;
    }

    public void leaveRoom(Session session) {
        if (session == null) {System.out.println("leaveRoom exit because session is null"); return;}

        Player player = getPlayerBySession(session);
        String roomId = sessionRooms.get(session.getId());

        if (player != null && roomId != null) {
            // reset player ready status
            player.setStatus(false);
            // reset player belongs to null game
            // player.setIsInGame(false);
            // System.out.println("-------");
            // System.out.println("call setIsInGame(false) in leaveRoom");
            // System.out.println("-------");
            player.setBelongsToGameId(null);
            System.out.println("call setBelongsToGameId(null) in leaveRoom");

            GameRoom room = rooms.get(roomId);
            if (room != null) {
                room.removePlayer(player);
                if (room.isEmpty()) {
                    rooms.remove(roomId);
                    room.manuallyDestroyTimer();
                    System.out.println("Room " + roomId + " is empty and has been removed.");
                } else {
                    room.broadcastRoomStatus();
                }
            }
        }

        sessionRooms.remove(session.getId());

        // 重新加入lobby
        lobbySessions.add(session);
    }

    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public List<GameRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Player getPlayerBySession(Session session) {
        if(session == null) return null;
        return players.stream()
            .filter(p-> p.getSession() != null && p.getSession().getId().equals(session.getId()))
            .findFirst()
            .orElse(null);
    }


    public void registerUsername(Long userId, String username) {
        userIdToUsername.put(userId, username);
    }

    public String getUsername(Long userId) {
        return userIdToUsername.getOrDefault(userId, "Unknown");
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void broadcastToLobby(Object message){
        String msgJson;
        try {
            msgJson = new ObjectMapper().writeValueAsString(message);
        } catch (Exception e) {
            System.out.println("broadcastToLobby, write message as string fail: " + e);
            return;
        }

        for(Session session:lobbySessions){
            if(session.isOpen()){
                try {
                    session.getBasicRemote().sendText(msgJson);
                    
                } catch (Exception e) {
                    System.out.println("broadcastToLobby, send message to sessionRemote fail: " + e);
                }
            }
        }
    }

}
