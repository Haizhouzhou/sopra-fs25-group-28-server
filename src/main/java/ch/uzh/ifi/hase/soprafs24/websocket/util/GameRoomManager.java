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

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, Player> sessionPlayers = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRooms = new ConcurrentHashMap<>();

    private final Map<Long, String> userIdToUsername = new ConcurrentHashMap<>();

    private final Map<String, Player> clientSessionIdToPlayerMap = new HashMap<>();


    @Autowired
    public GameRoomManager(UserService userService) {
        this.userService = userService;
    }

    public Player registerPlayer(Session session, String token) {
        User correspondingUser = userService.getUserByToken(token);
        Player player = new Player(session, correspondingUser.getName(), correspondingUser.getId());
        sessionPlayers.put(session.getId(), player);
        player.setAvatar(correspondingUser.getAvatar());

        System.out.println("[registerPlayer] avatar = " + correspondingUser.getAvatar());

        return player;
    }

    public void deregisterPlayer(Session session) {
        String sessionId = session.getId();
        sessionPlayers.remove(sessionId);
    }

    public GameRoom creatRoom(int maxPlayers, Player player, String roomName) {
        String roomId = generateRoomId();
        GameRoom room = new GameRoom(roomId, maxPlayers);
        room.setOwnerName(player.getName());
        room.setOwnerId(player.getUserId());
        room.setRoomName(roomName); // 保存房间名
        rooms.put(roomId, room);
        room.addPlayer(player);
        sessionRooms.put(player.getSession().getId(), roomId);
        room.broadcastRoomStatus();
        return room;
    }

    public boolean joinRoom(String roomId, Session session) {
        GameRoom room = rooms.get(roomId);
        if (room == null || room.isFull()) {
            return false;
        }

        Player player = sessionPlayers.get(session.getId());
        room.addPlayer(player);

        // 确保这行代码执行，将会话ID与房间ID关联起来
        sessionRooms.put(session.getId(), roomId);

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
        String sessionId = session.getId();
        Player player = sessionPlayers.get(sessionId);
        String roomId = sessionRooms.get(sessionId);

        if (player != null && roomId != null) {
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                room.removePlayer(player);
                if (room.isEmpty()) {
                    rooms.remove(roomId);
                } else {
                    room.broadcastRoomStatus();
                }
            }
            sessionRooms.remove(sessionId);
        }
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
