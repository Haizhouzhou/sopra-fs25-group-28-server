package ch.uzh.ifi.hase.soprafs24.websocket.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
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

      // 构造游戏结束消息
      MyWebSocketMessage message = new MyWebSocketMessage();
      message.setType(MyWebSocketMessage.TYPE_SERVER_GAME_OVER);
      message.setRoomId(roomId);

      // 获取玩家信息
      List<Map<String, Object>> playerResults = new ArrayList<>();
      for (Player p : players) {
          Map<String, Object> pInfo = new HashMap<>();
          pInfo.put("userId", p.getUserId());
          pInfo.put("name", p.getName());
          pInfo.put("avatar", p.getAvatar());
          pInfo.put("victoryPoints", p.getVictoryPoints());
          playerResults.add(pInfo);
      }

      // 包装成 content 发送
      Map<String, Object> content = new HashMap<>();
      content.put("players", playerResults);
      content.put("winnerId", game.getWinnerId());

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
                    e.printStackTrace();
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

        System.out.println("当前玩家索引: " + currentPlayerIndex);
        System.out.println("尝试结束回合的玩家ID: " + playerId);
        System.out.println("当前回合玩家ID: " + currentPlayerId);

        // 检查是否是该玩家的回合
        boolean isTurn = game.isPlayerTurn(player);
        System.out.println("是否是玩家的回合: " + isTurn);

        if (!isTurn) {
            return false;
        }

        // 结束回合
        game.endTurn();

        // 记录新的当前玩家
        int newCurrentPlayerIndex = game.getCurrentPlayer();
        Player newCurrentPlayer = game.getPlayers().get(newCurrentPlayerIndex);
        System.out.println("新的当前玩家索引: " + newCurrentPlayerIndex);
        System.out.println("新的当前玩家ID: " + newCurrentPlayer.getUserId());

        // 发送游戏状态更新到所有玩家
        broadcastGameState();

        // 检查游戏是否结束
        if (game.getGameState() == Game.GameState.FINISHED) {
            EndGame();
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
            e.printStackTrace();
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

        for (String colorStr : colors) {
            GemColor color = GemColor.valueOf(colorStr.toUpperCase());
            if (game.getAvailableGems().get(color) <= 0) return false;
        }

        for (String colorStr : colors) {
            GemColor color = GemColor.valueOf(colorStr.toUpperCase());
            player.setGem(color, player.getGem(color) + 1);
            game.getAvailableGems().put(color, game.getAvailableGems().get(color) - 1);
        }

        broadcastGameState();
        return true;
    }


    // 拿两个相同颜色
    public boolean handleTakeDoubleGem(Player player, String colorStr) {
        if (game == null) return false;

        GemColor color = GemColor.valueOf(colorStr.toUpperCase());
        if (game.getAvailableGems().get(color) < 4) return false;

        player.setGem(color, player.getGem(color) + 2);
        game.getAvailableGems().put(color, game.getAvailableGems().get(color) - 2);

        broadcastGameState();
        return true;
    }

}
