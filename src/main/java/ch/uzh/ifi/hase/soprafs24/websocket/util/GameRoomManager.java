package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;


@Component
public class GameRoomManager {

    private final UserService userService;

    private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private Map<String, Player> sessionPlayers = new ConcurrentHashMap<>();
    private Map<String, String> sessionRooms = new ConcurrentHashMap<>();

    private Map<Long, String> userIdToUsername = new ConcurrentHashMap<>();

    private final Map<String, Player> clientSessionIdToPlayerMap = new HashMap<>();


    @Autowired
    public GameRoomManager(UserService userService) {
        this.userService = userService;
    }

    public Player registerPlayer(Session session, String token) {
        User correspondingUser = userService.getUserByToken(token);
        Long userId = correspondingUser.getId();

        // 查找是否已有该用户的 Player 实例（用于重新连接时复用）
        Player existing = sessionPlayers.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        Player player;
        if (existing != null) {
            player = existing;
            player.setSession(session);
        } else {
            player = new Player(session, correspondingUser.getName(), userId);
            sessionPlayers.put(session.getId(), player);
        }

        player.setAvatar(correspondingUser.getAvatar());
        System.out.println("[registerPlayer] avatar = " + correspondingUser.getAvatar());

        return player;
    }


    public void deregisterPlayer(Session session) {
        String sessionId = session.getId();
        sessionPlayers.remove(sessionId);
    }

    /**
     * getter and setter used for unit test
     * 
     */
    public Map<String, GameRoom> getRooms(){return rooms;}
    public void setRooms(Map<String, GameRoom> rooms){this.rooms = rooms;}
    public Map<String, Player> getSessionPlayersMap(){return sessionPlayers;}
    public void setSessionPlayersMap(Map<String, Player> sessionPlayers){this.sessionPlayers = sessionPlayers;}
    public Map<String, String> getSessionRoomsMap(){return sessionRooms;}
    public void setSessionRoomsMap(Map<String, String> sessionRooms){this.sessionRooms = sessionRooms;}

    public GameRoom createRoom(int maxPlayers, Session session, String roomName) {
        Player player = getPlayerBySession(session);
        if (player == null || session == null || !session.isOpen()) {
            System.out.println("createRoom: session is null or closed");
            return null;
        }

        String roomId = generateRoomId();
        GameRoom room = new GameRoom(roomId, maxPlayers);
        room.setOwnerName(player.getName());
        room.setOwnerId(player.getUserId());
        room.setRoomName(roomName);

        rooms.put(roomId, room);
        room.addPlayer(player);
        sessionRooms.put(session.getId(), roomId);

        room.broadcastRoomStatus();
        return room;
    }

    public boolean joinRoom(String roomId, Session session) {
        Player player = getPlayerBySession(session);
        if (player == null || session == null || !session.isOpen()) {
            System.out.println("joinRoom: session is null or closed");
            return false;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null || room.isFull()) {
            return false;
        }

        room.addPlayer(player);
        sessionRooms.put(session.getId(), roomId);
        sessionPlayers.put(session.getId(), player);

        room.broadcastRoomStatus();

        try {
            Map<String, Object> joinedMessage = new ConcurrentHashMap<>();
            joinedMessage.put("type", "ROOM_JOINED");
            joinedMessage.put("roomId", roomId);
            session.getBasicRemote().sendText(JsonUtils.toJson(joinedMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public void leaveRoom(Session session) {
        if (session == null) return;

        String sessionId = session.getId();
        Player player = sessionPlayers.get(sessionId);
        String roomId = sessionRooms.get(sessionId);

        if (player != null && roomId != null) {
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                room.removePlayer(player);
                if (room.isEmpty()) {
                    rooms.remove(roomId);
                    System.out.println("Room " + roomId + " is empty and has been removed.");
                } else {
                    room.broadcastRoomStatus();
                }
            }
        }

        sessionRooms.remove(sessionId);
        sessionPlayers.remove(sessionId);
    }




    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public List<GameRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Player getPlayerBySession(Session session) {
        String sessionId = session.getId();
        Player sessionPlayer = sessionPlayers.get(sessionId);

        System.out.println("getPlayerBySession - sessionId: " + sessionId +
                ", sessionPlayer: " + (sessionPlayer != null ?
                sessionPlayer.getName() + ", id:" + sessionPlayer.getUserId() : "null"));

        if (sessionPlayer == null) return null;

        // 获取玩家所在的房间
        String roomId = sessionRooms.get(sessionId);
        System.out.println("getPlayerBySession - roomId: " + roomId);

        if (roomId != null) {
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                System.out.println("Room player count: " + room.getPlayers().size());
                // 从房间中查找真正的Player实例
                for (Player roomPlayer : room.getPlayers()) {
                    System.out.println("Room player: " + roomPlayer.getName() +
                            ", id:" + roomPlayer.getUserId() +
                            ", instance:" + System.identityHashCode(roomPlayer));

                    if (roomPlayer.getUserId().equals(sessionPlayer.getUserId())) {
                        System.out.println("Returning ROOM player instance: " +
                                System.identityHashCode(roomPlayer));
                        return roomPlayer; // 返回房间中的Player实例
                    }
                }
            }
        }

        System.out.println("Returning SESSION player instance: " +
                System.identityHashCode(sessionPlayer));
        return sessionPlayer; // 如果找不到，返回原始实例
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


    public void registerClientSessionId(String clientSessionId, Player player) {
        clientSessionIdToPlayerMap.put(clientSessionId, player);
    }

    public Player getPlayerByClientSessionId(String clientSessionId) {
        return clientSessionIdToPlayerMap.get(clientSessionId);
    }


}
