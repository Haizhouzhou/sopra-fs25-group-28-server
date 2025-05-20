package ch.uzh.ifi.hase.soprafs24.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoomManager;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GeminiHint;
import ch.uzh.ifi.hase.soprafs24.websocket.util.MyWebSocketMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

// TODO：modify endpoint
@ServerEndpoint(value = "/WebServer/{tokenParam}",
        configurator = SpringConfigurator.class)
@Component
public class WebSocketServer {

    private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameRoomManager roomManager;
    // private final GameRoomManager roomManager = SpringContext.getBean(GameRoomManager.class);
    private final GeminiHint geminiHint;
    // private final GeminiHint geminiHint = SpringContext.getBean(GeminiHint.class);

    @Autowired
    public WebSocketServer(GameRoomManager roomManager, GeminiHint geminiHint) {
        this.roomManager = roomManager;
        this.geminiHint = geminiHint;
    }


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
                case MyWebSocketMessage.TYPE_CLIENT_START_GAME -> handleStartGame(session, wsMessage);

                //action in game, added
                case MyWebSocketMessage.TYPE_CLIENT_BUY_CARD -> handleBuyCard(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_RESERVE_CARD -> handleReserveCard(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_END_TURN -> handleEndTurn(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_AI_HINT -> handleAiHint(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_TAKE_THREE_GEMS -> handleTakeThreeGems(session, wsMessage);
                case MyWebSocketMessage.TYPE_CLIENT_TAKE_DOUBLE_GEM -> handleTakeDoubleGem(session, wsMessage);

                case MyWebSocketMessage.TYPE_CLIENT_GET_ROOM_STATE -> handleGetRoomState(session, wsMessage);

                case MyWebSocketMessage.TYPE_CLIENT_GET_GAME_STATE -> handleGetGameState(session, wsMessage);


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

            // 直接用 session 创建房间
            GameRoom room = roomManager.createRoom(maxPlayers, session, roomName);
            if (room == null) {
                log.warn("❌ Failed to create room: session is closed");
                return;
            }

            // 给客户端发送 ROOM_JOINED 消息
            MyWebSocketMessage joinedMsg = new MyWebSocketMessage();
            joinedMsg.setType(MyWebSocketMessage.TYPE_SERVER_ROOM_JOINED);
            joinedMsg.setRoomId(room.getRoomId());
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(joinedMsg));

        } catch (Exception e) {
            log.error("Failed to create room {}", e);
        }

        broadcastRoomListToLobby();
    }



    private void handleJoinRoom(Session session, MyWebSocketMessage message) {
        boolean joined = roomManager.joinRoom(message.getRoomId(), session);
        if (!joined) {
            log.warn("joinRoom: session is null or closed");
            return;
        }

        Player player = getPlayerFromMessage(session, message);
        if (player == null) {
            log.warn("No player found for session: {}", session.getId());
            return;
        }

        // String clientSessionId = message.getSessionId();
        // if (clientSessionId != null) {
        //     roomManager.registerClientSessionId(clientSessionId, player);
        //     log.info("✅ Registered clientSessionId {} → userId {}", clientSessionId, player.getUserId());
        // }

        GameRoom room = roomManager.getRoom(message.getRoomId());
        if (room != null) {
            room.broadcastRoomStatus();
        }

        broadcastRoomListToLobby();
    }


    private void handleLeaveRoom(Session session) {
        roomManager.leaveRoom(session);

        broadcastRoomListToLobby();
    }

    private void handleMessage(Session session, MyWebSocketMessage message) {
        try {
            // 获取消息内容
            String roomId = message.getRoomId();
            Object content = message.getContent();

            // 获取发送消息的玩家
            Player player = getPlayerFromMessage(session, message);
            if (player == null) return;

            // 获取房间
            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) return;

            // 构造聊天消息
            MyWebSocketMessage chatMessage = new MyWebSocketMessage();
            chatMessage.setType(MyWebSocketMessage.TYPE_SERVER_CHAT_MESSAGE);
            chatMessage.setRoomId(roomId);
            chatMessage.setContent(content); // 或处理成期望的格式

            // 广播给房间中的所有玩家
            room.broadcastMessage(chatMessage);

            System.out.println("Broadcasting chat message to room " + roomId + ": " + content);
        } catch (Exception e) {
            log.error("Error handling player message", e);
        }
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

    protected void broadcastRoomListToLobby(){

        // 获取房间列表信息
        List<GameRoom> roomList = roomManager.getAllRooms();
        
        // log.info("🧪 getAllRooms result: {}", roomList);

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

        MyWebSocketMessage response = new MyWebSocketMessage();
        response.setType(MyWebSocketMessage.TYPE_SERVER_ROOM_LIST);
        response.setRoomId(null);
        response.setSessionId(null);
        response.setContent(roomSummaries);
        
        roomManager.broadcastToLobby(response);
    }

    /**
     * 用于处理玩家在房间中改变“准备/未准备”状态
     * @param session
     * @param message
     */
    private void handlePlayerStatus(Session session, MyWebSocketMessage message) {
        try {
            Map<String, Object> content = (Map<String, Object>) message.getContent();
            log.info("PLAYER_STATUS content: {}", content);

            if (content == null || content.isEmpty()) {
                log.warn("PLAYER_STATUS message with empty content");
                return;
            }

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
                Player foundPlayer = null;
                for (Player p : room.getPlayers()) {
                    if (p.getUserId().equals(userId)) {
                        foundPlayer = p;
                        break;
                    }
                }

                if (foundPlayer != null) {
                    log.info("Setting player {} status to {}", foundPlayer.getName(), isReady);
                    System.out.println("Player " + foundPlayer.getName() + " (id:" + userId + ") status changing from " + foundPlayer.getStatus() + " to " + isReady + " [instance: " + System.identityHashCode(foundPlayer) + "]");
                    foundPlayer.setStatus(isReady);
                    room.broadcastRoomStatus();
                } else {
                    log.warn("Player not found in room: userId={}", userId);
                }
            } else {
                log.warn("Room not found: {}", roomId);
            }
        } catch (Exception e) {
            log.error("Error handling PLAYER_STATUS message: {}", e.getMessage(), e);
        }
    }


    /**
     * 用于处理房间内，所有玩家准备好后，房主开始游戏
     */
    private void handleStartGame(Session session, MyWebSocketMessage message) {
        try {
            String roomId = message.getRoomId();
            log.info("Request to start game in room: {}", roomId);

            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) {
                log.warn("Cannot start game: Room {} not found", roomId);
                return;
            }

            Player player = getPlayerFromMessage(session, message);
            if (player == null) {
                log.warn("Cannot start game: Player not found for session");
                return;
            }

            // 检查是否是房主
            if (!player.getUserId().equals(room.getOwnerId())) {
                log.warn("Cannot start game: Player {} is not the room owner", player.getName());
                return;
            }

            // 检查所有非房主玩家是否都已准备
            boolean allReady = true;
            for (Player p : room.getPlayers()) {
                if (!p.getUserId().equals(room.getOwnerId()) && !p.getStatus()) {
                    allReady = false;
                    break;
                }
            }

            if (!allReady) {
                log.warn("Cannot start game: Not all players are ready");
                return;
            }

            // 开始游戏
            log.info("Starting game for room: {}", roomId);
            room.startGame();

            // 广播游戏状态给所有玩家
            MyWebSocketMessage gameStateMsg = new MyWebSocketMessage();
            gameStateMsg.setType(MyWebSocketMessage.TYPE_SERVER_GAME_STATE);
            gameStateMsg.setRoomId(roomId);


            gameStateMsg.setContent(room.getGame().getGameInformation());

            log.info("Broadcasting GAME_STATE message to all players in room {}", roomId);
            room.broadcastMessage(gameStateMsg);
        } catch (Exception e) {
            log.error("Error starting game", e);
        }
    }

    /**
     * 游戏内，处理玩家购买卡牌的请求
     */
    private void handleBuyCard(Session session, MyWebSocketMessage message) {
        try {
            // 获取房间ID和玩家
            String roomId = message.getRoomId();
            Player player = getPlayerFromMessage(session, message);

            if (player == null) {
                log.warn("Player not found for session");
                return;
            }

            // 从前端请求信息中，获取房间id
            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) {
                log.warn("Room not found: {}", roomId);
                return;
            }

            // 从前端请求信息中，获取卡牌ID
            Map<String, Object> content = (Map<String, Object>) message.getContent();
            String target = (String) content.get("target");

            if (target == null) {
                log.warn("No target card specified");
                return;
            }

            // 执行购买卡牌操作
            boolean success = room.handleBuyCard(player, target);

            if (!success) {
                // 发送错误消息给玩家
                room.sendErrorToPlayer(player, "无法购买卡牌，请检查是否是您的回合或资源是否足够。");
            }
        } catch (Exception e) {
            log.error("Error handling BUY_CARD message: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理玩家预留卡牌的请求
     */
    private void handleReserveCard(Session session, MyWebSocketMessage message) {
        try {
            // 获取房间ID和玩家
            String roomId = message.getRoomId();
            Player player = getPlayerFromMessage(session, message);

            if (player == null) {
                log.warn("Player not found for session");
                return;
            }

            // 获取房间
            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) {
                log.warn("Room not found: {}", roomId);
                return;
            }

            // 获取卡牌ID
            Map<String, Object> content = (Map<String, Object>) message.getContent();
            String target = (String) content.get("target");

            if (target == null) {
                log.warn("No target card specified");
                return;
            }

            // 执行预留卡牌操作
            boolean success = room.handleReserveCard(player, target);

            if (!success) {
                // 发送错误消息给玩家
                room.sendErrorToPlayer(player, "无法预留卡牌，请检查是否是您的回合或是否已达到预留上限。");
            }
        } catch (Exception e) {
            log.error("Error handling RESERVE_CARD message: {}", e.getMessage(), e);
        }
    }

    /**
     * 游戏内，处理玩家结束回合的请求
     */
    private void handleEndTurn(Session session, MyWebSocketMessage message) {
        try {
            log.info("Handling END_TURN message");

            // 获取房间ID和玩家
            String roomId = message.getRoomId();
            Player player = getPlayerFromMessage(session, message);

            if (player == null) {
                log.warn("Player not found for session");
                return;
            }

            // 获取房间
            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) {
                log.warn("Room not found: {}", roomId);
                return;
            }

            // 获取当前玩家索引
            Game game = room.getGame();
            int currentPlayerBefore = game.getCurrentPlayer();
            log.info("Current player before end turn: {}", currentPlayerBefore);

            // 执行结束回合操作
            boolean success = room.handleEndTurn(player);

            // 记录结束回合操作的结果
            log.info("End turn operation success: {}", success);

            // 记录操作后的当前玩家索引
            int currentPlayerAfter = game.getCurrentPlayer();
            log.info("Current player after end turn: {}", currentPlayerAfter);

            if (!success) {
                // 发送错误消息给玩家
                room.sendErrorToPlayer(player, "无法结束回合，请检查是否是您的回合。");
            }

            Map<String, Object> content = (Map<String, Object>) message.getContent();
            log.info("END_TURN message content: {}", content);

        } catch (Exception e) {
            log.error("Error handling END_TURN message: {}", e.getMessage(), e);
        }
    }

    /**
     * 游戏内，处理玩家请求AI提示的请求
     */
    private void handleAiHint(Session session, MyWebSocketMessage message) {
        try {
            String roomId = message.getRoomId();
            Player player = getPlayerFromMessage(session, message);
            if (player == null) return;

            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) return;

            Game game = room.getGame();
            if (game == null) return;

            if(player.hintAvailable == false){
                room.sendErrorToPlayer(player, "only 1 hint per player per game");
                return;
            }

            if (!game.isPlayerTurn(player)) {
                room.sendErrorToPlayer(player, "只能在您的回合请求提示。");
                return;
            }


            player.hintAvailable = false;

            // GeminiHint
            String hint = geminiHint.generateSplendorHint(game);

            // WebSocket return
            MyWebSocketMessage hintMessage = new MyWebSocketMessage();
            hintMessage.setType(MyWebSocketMessage.TYPE_SERVER_AI_HINT);
            hintMessage.setRoomId(roomId);
            hintMessage.setContent(Map.of("hint", hint));

            player.sendMessage(hintMessage);
        } catch (Exception e) {
            log.error("Error handling AI_HINT message: {}", e.getMessage(), e);
        }
    }

    protected Player getPlayerFromMessage(Session session, MyWebSocketMessage message) {
        String clientSessionId = message.getSessionId();
        Player player = null;

        if (clientSessionId != null) {
            player = roomManager.getPlayerBySession(session);
            // player = roomManager.getPlayerByClientSessionId(clientSessionId);
            log.info("getPlayerBySession, SessionId :{}, player.getUserId():{}", session.getId(), player != null ? player.getUserId() : "null");
        }

        // if (player == null) {
        //     player = roomManager.getPlayerBySession(session);
        //     log.info("fallback to getPlayerBySession: {}", player != null ? player.getUserId() : "null");
        // }

        return player;
    }

    /**
     * 游戏内，处理玩家拿三个不同颜色gem的请求
     */
    private void handleTakeThreeGems(Session session, MyWebSocketMessage message) {
        try {
            String roomId = message.getRoomId();
            Player player = getPlayerFromMessage(session, message);
            if (player == null) return;

            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) return;

            Map<String, Object> content = (Map<String, Object>) message.getContent();
            List<String> colors = (List<String>) content.get("colors");

            boolean success = room.handleTakeThreeGems(player, colors);
            if (!success) {
                room.sendErrorToPlayer(player, "CAN NOT TAKE THREE GEMS");
            }
        } catch (Exception e) {
            log.error("handleTakeThreeGems error: ", e);
        }
    }

    /**
     * 游戏内，处理玩家拿两个相同颜色gems的请求
     */
    private void handleTakeDoubleGem(Session session, MyWebSocketMessage message) {
        try {
            String roomId = message.getRoomId();
            Player player = getPlayerFromMessage(session, message);
            if (player == null) return;

            GameRoom room = roomManager.getRoom(roomId);
            if (room == null) return;

            Map<String, Object> content = (Map<String, Object>) message.getContent();
            String color = (String) content.get("color");

            boolean success = room.handleTakeDoubleGem(player, color);
            if (!success) {
                room.sendErrorToPlayer(player, "CAN NOT TAKE DOUBLE GEM");
            }
        } catch (Exception e) {
            log.error("handleTakeDoubleGem error: ", e);
        }
    }

    private void handleGetRoomState(Session session, MyWebSocketMessage message) {
        String roomId = message.getRoomId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room != null) {
            room.broadcastRoomStatus(); // 或者只发给当前玩家也行
        }
    }

    private void handleGetGameState(Session session, MyWebSocketMessage message){
        String roomId = message.getRoomId();
        GameRoom room = roomManager.getRoom(roomId);
        Player player = getPlayerFromMessage(session, message);
        if (player == null || room == null) return;

        room.updateGameStateForPlayer(player);
    }

}