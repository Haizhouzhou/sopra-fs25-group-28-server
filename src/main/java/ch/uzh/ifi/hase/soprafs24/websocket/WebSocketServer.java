package ch.uzh.ifi.hase.soprafs24.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoomManager;
import ch.uzh.ifi.hase.soprafs24.websocket.util.MyWebSocketMessage;

import ch.uzh.ifi.hase.soprafs24.config.SpringContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO：modify endpoint
@ServerEndpoint(value = "/WebServer/{tokenParam}",
        configurator = SpringConfigurator.class)
@Component
public class WebSocketServer {

    private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameRoomManager roomManager = SpringContext.getBean(GameRoomManager.class);


//  private GameRoomManager roomManager;

    @OnOpen
    public void onOpen(Session session, @PathParam("tokenParam") String token) {
        log.info("WebSocket connection opened: {}", session.getId());
        // TODO: incorperate UserService, obtain userId
        //  Long userId = 1L;
        //  String token = "a dummy token";
        roomManager.registerPlayer(session, token);
        // log.info("player registered");
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket connection closed: {}", session.getId());
        roomManager.leaveRoom(session);
        roomManager.deregisterPlayer(session);
    }

    /**
     * handle receive information from frontend
     *
     * @param session
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            MyWebSocketMessage wsMessage = objectMapper.readValue(message, MyWebSocketMessage.class);
            String messageType = wsMessage.getType();
            log.info("message  type: {}", messageType);

            // 检查消息类型是否为null
            if (messageType == null) {
                log.warn("Received message with null type, ignoring");
                return;
            }

            switch (messageType) {
                case MyWebSocketMessage.TYPE_CLIENT_CREATE_ROOM -> handleCreateRoom(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_JOIN_ROOM -> handleJoinRoom(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_LEAVE_ROOM -> handleLeaveRoom(session);
                case MyWebSocketMessage.TYPE_CLIENT_PLAYER_MESSAGE -> handleMessage(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_GET_ROOMS -> handleGetRooms(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_PLAYER_STATUS -> handlePlayerStatus(session, wsMessage);
                default -> log.warn("Unknown message type: {}", messageType);
            }
        }
        catch (Exception e) {
            log.info("error during message handling: {}", e);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error for session {} : {}", session.getId(), throwable.getMessage());
        // TODO: broadcast Error to frontent?
    }

    private void handleCreateRoom(Session session, MyWebSocketMessage message) {
        try {
            int maxPlayers = 4;
            String roomName = "Room"; // default
            if (message.getContent() instanceof Map content) {
                if (content.containsKey("maxPlayers")) {
                    maxPlayers = (Integer) content.get("maxPlayers");
                }
                if (content.containsKey("roomName")) {
                    roomName = (String) content.get("roomName");
                }
            }

            Player player = roomManager.getPlayerBySession(session);
            GameRoom room = roomManager.creatRoom(maxPlayers, player, roomName);
            boolean joined = roomManager.joinRoom(room.getRoomId(), session);

            if (joined) {
                MyWebSocketMessage joinedMsg = new MyWebSocketMessage();
                joinedMsg.setType(MyWebSocketMessage.TYPE_SERVER_ROOM_JOINED);
                joinedMsg.setRoomId(room.getRoomId());
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(joinedMsg));
            }
        } catch (Exception e) {
            log.error("Failed to create room", e);
        }
    }


    private void handleJoinRoom(Session session, MyWebSocketMessage message) {
        boolean joined = roomManager.joinRoom(message.getRoomId(), session);

        // if(joined){
        //   sendRoomJoinedMessage(session, message.getRoomId());
        // }
    }

    private void handleLeaveRoom(Session session) {
        roomManager.leaveRoom(session);
    }

    private void handleMessage(Session session, MyWebSocketMessage message) {
        // TODO: implement this
    }

    public GameRoomManager getRoomManager() {
        return roomManager;
    }


    @SuppressWarnings("unchecked")
    private void handleGetRooms(Session session, MyWebSocketMessage wsMessage) {
        // 1: 获取用户名和 userId（从 message.content 中）
        Object contentObj = wsMessage.getContent();
        if (contentObj instanceof Map) {
            Map<String, Object> contentMap = (Map<String, Object>) contentObj;
            String displayName = (String) contentMap.get("displayName");

            try {
                //从 content 中取出 userId
                Long userId = ((Number) contentMap.get("userId")).longValue();
                roomManager.registerUsername(userId, displayName);
                log.info("Registered username '{}' for userId {}", displayName, userId);
            } catch (Exception e) {
                log.warn("获取 userId 或注册失败: {}", e.getMessage());
            }
        }

        //2: 获取房间信息
        List<GameRoom> roomList = roomManager.getAllRooms();
        log.info("🧪 getAllRooms result: {}", roomList);

        List<Map<String, Object>> roomSummaries = new ArrayList<>();
        for (GameRoom room : roomList) {
            Map<String, Object> info = new HashMap<>();
            info.put("roomId", room.getRoomId());
            info.put("roomName", room.getRoomName());
            info.put("owner", room.getOwnerName());
            info.put("players", room.getCurrentPlayerCount());
            info.put("maxPlayers", room.getMaxPlayer());
            roomSummaries.add(info);
        }

        // 3: 发消息回去
        MyWebSocketMessage response = new MyWebSocketMessage();
        response.setType(MyWebSocketMessage.TYPE_SERVER_ROOM_LIST);
        response.setRoomId(null);
        response.setSessionId(null);
        response.setContent(roomSummaries);

        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(response));
        } catch (IOException e) {
            log.error("Failed to send room list", e);
        }
    }

    private void handlePlayerStatus(Session session, MyWebSocketMessage message) {
        try {
            Map<String, Object> content = (Map<String, Object>) message.getContent();
            log.info("PLAYER_STATUS content: {}", content);

            // 检查 content 是否为 null 或空
            if (content == null || content.isEmpty()) {
                log.warn("PLAYER_STATUS message with empty content");
                return;
            }

            // 检查 userId 和 status 字段是否存在
            if (!content.containsKey("userId") || !content.containsKey("status")) {
                log.warn("PLAYER_STATUS missing required fields. Content: {}", content);
                return;
            }

            Long userId = ((Number) content.get("userId")).longValue();
            boolean isReady = (Boolean) content.get("status");
            String roomId = message.getRoomId();

            log.info("Processing PLAYER_STATUS: userId={}, isReady={}, roomId={}", userId, isReady, roomId);

            GameRoom room = roomManager.getRoom(roomId);
            if (room != null) {
                Player player = roomManager.getPlayerBySession(session);
                log.info("Found player: {}, userId: {}", player != null ? player.getName() : "null", player != null ? player.getUserId() : "null");

                if (player != null && player.getUserId().equals(userId)) {
                    log.info("Setting player {} status to {}", player.getName(), isReady);
                    player.setStatus(isReady);
                    room.broadcastRoomStatus();
                } else {
                    log.warn("Player mismatch or not found. Session player: {}, Requested userId: {}",
                            player != null ? player.getUserId() : "null", userId);
                }
            } else {
                log.warn("Room not found: {}", roomId);
            }
        } catch (Exception e) {
            log.error("Error handling PLAYER_STATUS message: {}", e.getMessage(), e);
        }
    }



}



