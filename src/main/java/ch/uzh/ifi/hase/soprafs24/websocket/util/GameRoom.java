package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.service.LeaderboardService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;


public class GameRoom {

  private static final boolean ROOM_READY = true;
  private static final boolean ROOM_WAITING = false;

  private String roomId;
  private int maxPlayer;
  private Set<Player> players = ConcurrentHashMap.newKeySet();
  private boolean roomStatus;
  private Game game;
  
  protected final ScheduledExecutorService roundTimerExecutor = Executors.newSingleThreadScheduledExecutor();
  protected  ScheduledFuture<?> roundTimerFuture; // 当前回合的timer
  protected final long ROUND_TIMEOUT_MILLIS = 35000; // 后端限制35秒


  private String roomName = "";
  private Long ownerId;
  private String ownerName;
  private final LeaderboardService leaderboardService;



  public GameRoom(String roomId, int maxPlayer, LeaderboardService leaderboardService) {
    this.roomId = roomId;
    this.maxPlayer = maxPlayer;
    this.leaderboardService = leaderboardService;
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

  // used for testing
  public Game getGameInstance(){return game;}
  public void setGameInstance(Game game){this.game = game;}


    /**
   * if all players are ready, host players can start the game
   */
  public void startGame(){
    // something like game.initialize()
    game = new Game(this, roomId, players);
    game.initialize();
    System.out.println("Game started in room: " + roomId);

    // Start the first timer
    Player firstPlayer = game.getPlayers().get(game.getCurrentPlayer());
    startRoundTimer(firstPlayer);
  }

  /**
   * if any players reach the goal, call this function and save game record
   * TODO: to be implemente
   */
  public void EndGame() {
    if (game == null) {
        System.out.println("尝试结束游戏，但 game 为 null");
        return;
    }

    System.out.println("游戏结束，开始广播最终结果");

    // 🏆 Record the win
    // TODO: 确认leaderboard的entry
    Long winnerId = game.getWinnerId();
    if (winnerId != null) {
        leaderboardService.addWinForPlayer(winnerId);
        System.out.println("Leaderboard updated for winner ID: " + winnerId);
    }

    // 🎯 Broadcast final results
    MyWebSocketMessage message = new MyWebSocketMessage();
    message.setType(MyWebSocketMessage.TYPE_SERVER_GAME_OVER);
    message.setRoomId(roomId);

    List<Map<String, Object>> playerResults = new ArrayList<>();
    for (Player p : players) {
        Map<String, Object> pInfo = new HashMap<>();
        pInfo.put("userId", p.getUserId());
        pInfo.put("name", p.getName());
        pInfo.put("avatar", p.getAvatar());
        pInfo.put("victoryPoints", p.getVictoryPoints());
        playerResults.add(pInfo);
    }

    Map<String, Object> content = new HashMap<>();
    content.put("players", playerResults);
    content.put("winnerId", winnerId);

    message.setContent(content);
    broadcastMessage(message);
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
        System.out.println("broadcastRoomStatus - Room: " + roomId +
                ", max players: " + maxPlayer);
        for (Player p : players) {
            System.out.println("Player in broadcastRoomStatus: " + p.getName() +
                    ", id:" + p.getUserId() +
                    ", status:" + p.getStatus() +
                    ", instance:" + System.identityHashCode(p));
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "ROOM_STATE");
        msg.put("roomName", this.roomName);

        List<Map<String, Object>> playerStates = new ArrayList<>();
        for (Player p : players) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("userId", p.getUserId());
            pInfo.put("name", p.getName());
            pInfo.put("room_status", p.getStatus());
            pInfo.put("avatar", p.getAvatar());
            pInfo.put("isOwner", p.getUserId().equals(this.ownerId));
            playerStates.add(pInfo);
        }

        for (Player p : players) {
            System.out.println("Player: " + p.getName() + ", Status: " + p.getStatus());
        }

        msg.put("players", playerStates);
        msg.put("ownerId", this.ownerId);
        msg.put("ownerName", this.ownerName);

        String jsonMessage = JsonUtils.toJson(msg);

        for (Player player : players) {
            Session session = player.getSession();
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(jsonMessage);
                } catch (IOException e) {
                    System.err.println("IOException " + e);
                }
            } else {
                System.out.println("Skipping closed session for player: " + player.getName());
            }
        }

        System.out.println("Broadcasting room state: " + jsonMessage);
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

  public Game getGame() {
      return this.game;
  }

  /**
   * activate timer
   */
  public void startRoundTimer(Player currentPlayer){
    // deactivate old timer
    cancelRoundTimer();

    Session session = currentPlayer.getSession();
    if(session == null || !session.isOpen()){
        // System.out.println("[Timer] 玩家离线，自动跳过回合: " + currentPlayer.getUserId());
        handleEndTurn(currentPlayer);
    }else{
        roundTimerFuture = roundTimerExecutor.schedule(() -> {
            // System.out.println("[Timer] 玩家超时未操作，自动跳过回合: " + currentPlayer.getUserId());
            // timer时间到 handleEndTurn
            handleEndTurn(currentPlayer);
        }, ROUND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

  }

  /**
   * deactivate timer
   */
  public void cancelRoundTimer(){
    if (roundTimerFuture != null && !roundTimerFuture.isDone()){
        roundTimerFuture.cancel(true);
    }
  }

    /**
     * 处理玩家购买卡牌的操作
     * @param player 玩家
     * @param cardIdStr 卡牌ID字符串
     * @return 操作是否成功
     */
    public boolean handleBuyCard(Player player, String cardIdStr) {
        if (game == null) {
            return false;
        }

        try {
            Long cardId = Long.parseLong(cardIdStr);
            boolean success = game.buyCard(player, cardId);

            if (success) {
                // 发送游戏状态更新到所有玩家
                broadcastGameState();
            }

            return success;
        } catch (NumberFormatException e) {
            // 卡牌ID格式无效
            return false;
        }
    }

    /**
     * 处理玩家预留卡牌的操作
     * @param player 玩家
     * @param cardIdStr 卡牌ID字符串
     * @return 操作是否成功
     */
    public boolean handleReserveCard(Player player, String cardIdStr) {
        if (game == null) {
            return false;
        }

        try {
            Long cardId = Long.parseLong(cardIdStr);
            boolean success = game.reserveCard(player, cardId);

            if (success) {
                // 发送游戏状态更新到所有玩家
                broadcastGameState();
            }

            return success;
        } catch (NumberFormatException e) {
            // 卡牌ID格式无效
            return false;
        }
    }

    /**
     * 处理玩家结束回合的操作
     * @param player 玩家
     * @return 操作是否成功
     */
    public boolean handleEndTurn(Player player) {
        if (game == null) {
            System.out.println("Game is null");
            return false;
        }

        // 获取当前玩家ID和当前回合玩家ID
        Long playerId = player.getUserId();
        int currentPlayerIndex = game.getCurrentPlayer();
        Player currentTurnPlayer = game.getPlayers().get(currentPlayerIndex);
        Long currentPlayerId = currentTurnPlayer.getUserId();

        // System.out.println("当前玩家索引: " + currentPlayerIndex);
        // System.out.println("尝试结束回合的玩家ID: " + playerId);
        // System.out.println("当前回合玩家ID: " + currentPlayerId);

        // 检查是否是该玩家的回合
        boolean isTurn = game.isPlayerTurn(player);
        // System.out.println("是否是玩家的回合: " + isTurn);

        if (!isTurn) {
            return false;
        }

        // deactivate timer 
        cancelRoundTimer();

        // 结束回合
        //game.endTurn的作用：更新游戏状态，更新要做行动的玩家，处理noble逻辑，判断是否有玩家胜出
        //增加所有玩家进行同样数量轮次的逻辑：更改Game.GameState设置成FINISHED的节点，也就是在Game.endTurn中处理
        //GameRoom中的handle EndTurn应该不需要更改
        game.endTurn();

        // 记录新的当前玩家
        int newCurrentPlayerIndex = game.getCurrentPlayer();
        Player newCurrentPlayer = game.getPlayers().get(newCurrentPlayerIndex);
        // System.out.println("新的当前玩家索引: " + newCurrentPlayerIndex);
        // System.out.println("新的当前玩家ID: " + newCurrentPlayer.getUserId());

        // 发送游戏状态更新到所有玩家
        broadcastGameState();

        // 检查游戏是否结束
        if (game.getGameState() == Game.GameState.FINISHED) {
            EndGame();
            roundTimerExecutor.shutdownNow();
        }else{
            // activate new timer for next online player
            List<Player> playerList = game.getPlayers();
            int totalPlayers = playerList.size();
            int idx = game.getCurrentPlayer();
            boolean foundOnline = false;
            for (int i = 0; i < totalPlayers; i++) {
                Player next = playerList.get(idx);
                Session session = next.getSession();
                if (session != null && session.isOpen()) {
                    foundOnline = true;
                    startRoundTimer(next);
                    break;
                }
                idx = (idx + 1) % totalPlayers;
            }
            // 如果没有在线玩家，不再递归
            if (!foundOnline) {
                System.out.println("所有玩家都离线，停止定时器递归。");
            }
        }

        return true;
    }

    /**
     * 向所有玩家广播当前游戏状态
     */
    public void broadcastGameState() {
        if (game == null) return;

        try {
            System.out.println("准备广播游戏状态...");

            // 获取最新游戏状态
            GameSnapshot snapshot = game.getGameInformation();

            // 打印详细信息
            System.out.println("玩家顺序: " + snapshot.getPlayerOrder());
            System.out.println("当前玩家索引: " + snapshot.getCurrentPlayerIndex());
            if (snapshot.getCurrentPlayerIndex() >= 0 &&
                    snapshot.getCurrentPlayerIndex() < snapshot.getPlayerOrder().size()) {
                System.out.println("当前玩家ID: " +
                        snapshot.getPlayerOrder().get(snapshot.getCurrentPlayerIndex()));
            }

            // 创建消息
            MyWebSocketMessage message = new MyWebSocketMessage();
            message.setType(MyWebSocketMessage.TYPE_SERVER_GAME_STATE);
            message.setRoomId(roomId);
            message.setContent(snapshot);

            // 广播给每个玩家
            for (Player player : players) {
                Session session = player.getSession();
                if (session != null && session.isOpen()) {
                    System.out.println("正在向玩家 " + player.getUserId() + " 发送游戏状态");
                    player.sendMessage(message);
                } else {
                    System.out.println("跳过玩家 " + player.getUserId() + "：连接已关闭");
                }
            }


            System.out.println("游戏状态广播完成");
        } catch (Exception e) {
            System.err.println("广播游戏状态时出错: " + e.getMessage());
        }
    }

    public void updateGameStateForPlayer(Player requestPlayer){
        if (game == null || requestPlayer == null) return;

        try {
            // 获取最新游戏状态
            GameSnapshot snapshot = game.getGameInformation();

            // 创建消息
            MyWebSocketMessage message = new MyWebSocketMessage();
            message.setType(MyWebSocketMessage.TYPE_SERVER_GAME_STATE);
            message.setRoomId(roomId);
            message.setContent(snapshot);

            Session session = requestPlayer.getSession();
            if (session != null && session.isOpen()) {
                System.out.println("正在向玩家 " + requestPlayer.getUserId() + " 发送游戏状态");
                requestPlayer.sendMessage(message);
            } else {
                System.out.println("跳过玩家 " + requestPlayer.getUserId() + "：连接已关闭");
            }

        } catch (Exception e) {
            System.err.println("更新游戏状态时出错: " + e.getMessage());
        }
    }

    /**
     * 发送错误消息给指定玩家
     * @param player 玩家
     * @param errorMessage 错误消息
     */
    public void sendErrorToPlayer(Player player, String errorMessage) {
        MyWebSocketMessage message = new MyWebSocketMessage();
        message.setType(MyWebSocketMessage.TYPE_SERVER_ERROR);
        message.setRoomId(roomId);

        Map<String, String> content = new HashMap<>();
        content.put("message", errorMessage);
        message.setContent(content);

        player.sendMessage(message);
    }


    // 拿三个不同颜色
    public boolean handleTakeThreeGems(Player player, List<String> colors) {
        if (game == null || colors == null || colors.size() != 3) return false;

        Set<String> uniqueColors = new HashSet<>(colors);
        if (uniqueColors.size() != 3) return false;

        // convert string to GemColor
        List<GemColor> colorList = new ArrayList<>();
        try {
            for (String colorStr : colors) {
                colorList.add(GemColor.valueOf(colorStr.toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            // incorrect color string
            return false;
        }

        // call game.takeGems
        boolean success = game.takeGems(player, colorList);

        if (success) {
            broadcastGameState();
        }
        return success;
    }


    // 拿两个相同颜色
    public boolean handleTakeDoubleGem(Player player, String colorStr) {
        if (game == null || colorStr == null) return false;

        GemColor color;
        try {
            color = GemColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        List<GemColor> colorList = List.of(color);
        boolean success = game.takeGems(player, colorList);

        if (success) {
            broadcastGameState();
        }
        return success;
    }

    public void manuallyDestroyTimer(){
        if (roundTimerExecutor != null && !roundTimerExecutor.isShutdown()) {
            roundTimerExecutor.shutdownNow();
        }
        // 你可以在这里加别的资源清理
        System.out.println("GameRoom " + roomId + " destroyed.");
    }
}
