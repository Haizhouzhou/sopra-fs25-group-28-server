package ch.uzh.ifi.hase.soprafs24.websocket.game;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Noble;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;


public class Game {

  public enum GameState {
    NOT_STARTED,    // 游戏尚未开始
    RUNNING,        // 游戏正在进行中
    FINISHED        // 游戏已结束
  }

  private final Long VICTORYPOINTS = 1L;

  // unique id for the game
  private final String gameId;

  private final List<Player> players;

  // available gems on the board
  private Map<GemColor, Long> availableGems = new HashMap<>();

  // card deck of different levels of card
  private Stack<Card> level1Deck = new Stack<>();
  private Stack<Card> level2Deck = new Stack<>();
  private Stack<Card> level3Deck = new Stack<>();

  // visible card on the board
  private List<Card> visibleLevel1Cards = new ArrayList<>(4);
  private List<Card> visibleLevel2Cards = new ArrayList<>(4);
  private List<Card> visibleLevel3Cards = new ArrayList<>(4);

  // nobles on the board
  private List<Noble> visibleNoble = new ArrayList<>(4);

  private int currentPlayer = 0;
  private int currentRound = 0;

  private GameRoom gameRoom;

  // 游戏状态
  private GameState gameState = GameState.NOT_STARTED;


    // getters
  public String getGameId(){return this.gameId;}
  public List<Player> getPlayers(){return this.players;}
  public int getCurrentPlayer(){return currentPlayer;}
  public int getCurrentRound(){return currentRound;}
  public Map<GemColor, Long> getAvailableGems(){return availableGems;}
  public List<Card> getVisibleLevel1Cards(){return visibleLevel1Cards;}
  public List<Card> getVisibleLevel2Cards(){return visibleLevel2Cards;}
  public List<Card> getVisibleLevel3Cards(){return visibleLevel3Cards;}
  public List<Noble> getVisibleNoble(){return visibleNoble;}
  public GameState getGameState(){return gameState;}
  public void setGameState(GameState gameState){this.gameState = gameState;}


  public Game(GameRoom gameRoom, String gameId, Set<Player> players) {
        this.gameRoom = gameRoom;
        this.gameId = gameId;
        this.players = new ArrayList<>(players);
        Collections.shuffle(this.players);
   }

  public GameRoom getGameRoom() {
        return this.gameRoom;
  }

  public Long getWinnerId() {
    Player winner = null;
    Long maxPoints = -1L;

    for (Player player : players) {
        if (player.getVictoryPoints() > maxPoints) {
            maxPoints = player.getVictoryPoints();
            winner = player;
        }
    }

     return winner != null ? winner.getUserId() : null;
    }


  public void initialize(){
    // fill the decks with predifined cards
    // and select 4 nobles from json
    createAllItems();

    // shuffle the deck
    Collections.shuffle(level1Deck);
    Collections.shuffle(level2Deck);
    Collections.shuffle(level3Deck);

    // draw initial cards from the decks
    fillVisibleCards(level1Deck, visibleLevel1Cards);
    fillVisibleCards(level2Deck, visibleLevel2Cards);
    fillVisibleCards(level3Deck, visibleLevel3Cards);

    // initialize available gems on board
    availableGems.put(GemColor.BLACK, 7L);
    availableGems.put(GemColor.RED, 7L);
    availableGems.put(GemColor.BLUE, 7L);
    availableGems.put(GemColor.GREEN, 7L);
    availableGems.put(GemColor.WHITE, 7L);
    availableGems.put(GemColor.GOLD, 5L);

    // TODO: initialize players' game status
    for(Player player : players){
      // TODO: not fully implemented yet
      player.initializeGameStatus();
    }

    this.gameState = GameState.RUNNING;
  }

  private void createAllItems(){
    ObjectMapper objectMapper = new ObjectMapper();

    // create cards from json
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("cards.json");

      if (inputStream == null) {
        throw new IllegalArgumentException("cards.json not found in resources");
      }

      List<Card> cards = objectMapper.readValue(
        inputStream,
        objectMapper.getTypeFactory().constructCollectionType(List.class, Card.class)
      );

      for(Card card : cards){
        switch(card.getTier()){
          case 1 -> level1Deck.add(card);
          case 2 -> level2Deck.add(card);
          case 3 -> level3Deck.add(card);
          default -> throw new IllegalArgumentException("Unknown card tier: " +card.getTier());
        }
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to load cards from JSON,", e);
    }

    // create nobles from json
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("noblemen.json");
      if (inputStream == null) {
        throw new IllegalArgumentException("noblemen.json not found in resources");
      }
      List<Noble> nobles = objectMapper.readValue(
        inputStream,
        objectMapper.getTypeFactory().constructCollectionType(List.class, Noble.class)
      );

      Collections.shuffle(nobles);

      for(int i = 0; i<4; i++){
        visibleNoble.add(nobles.get(i));
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to load nobles from JSON,", e);
    }

  }

  private void fillVisibleCards(Stack<Card> deck, List<Card> visibleCards){
    for(int i = 0; i<4; i++){
      if(visibleCards.size()<=i){
        visibleCards.add(null);
      }

      if(visibleCards.get(i) == null && !deck.isEmpty()){
        visibleCards.set(i, deck.pop());
      }
    }
  }

  /**
   * initialize the game
   * @param players List of players participate in the game
   *
   */
//   public Game(String gameRoomId, Set<Player> players){
//     this.gameId = gameRoomId;
//     this.players = new ArrayList<>(players);
//     Collections.shuffle(this.players);
//   }

  // TODO: may need some modification
  public GameSnapshot getGameInformation(){
    return GameSnapshot.createFromGame(this);
  }

  // TODO: make it public or private?
  /**
   *
   * @return true if there is player reach the condition of winning
   */
  public boolean checkVictoryCondition(){
    for(Player player : players){
      if(player.getVictoryPoints() >= VICTORYPOINTS){
        this.gameState = GameState.FINISHED;
        return true;
      }
    }
    return false;
  }
    // Game.java
    public void endTurn() {
        // 1. Refill empty visible card slots
        fillVisibleCards(level1Deck, visibleLevel1Cards);
        fillVisibleCards(level2Deck, visibleLevel2Cards);
        fillVisibleCards(level3Deck, visibleLevel3Cards);

        // 2. Check if the current player qualifies for a noble
        noblePurchase(this);

        // 3. Check for victory condition
        checkVictoryCondition();

        // 4. Increment round
        currentRound++;

        // 5. Advance to next player
        Player currentTurnPlayer = players.get(currentPlayer);
        System.out.println("回合结束，当前玩家: " + currentTurnPlayer.getUserId());

        currentPlayer = (currentPlayer + 1) % players.size();

        Player nextTurnPlayer = players.get(currentPlayer);
        System.out.println("下一回合玩家: " + nextTurnPlayer.getUserId());
    }

    public void noblePurchase(Game game) {
        Player player = game.getPlayers().get(game.getCurrentPlayer());

        for (Noble noble : new ArrayList<>(game.getVisibleNoble())) { // avoid concurrent modification
            boolean qualifies = true;

            for (Map.Entry<GemColor, Long> entry : noble.getCost().entrySet()) {
                GemColor color = entry.getKey();
                long required = entry.getValue();

                if (player.getBonusGem(color) < required) {
                    qualifies = false;
                    break;
                }
            }

            if (qualifies) {
                // Award the noble
                player.setVictoryPoints(player.getVictoryPoints() + noble.getPoints());

                // Remove from board
                game.getVisibleNoble().remove(noble);

                // Optionally add to player's noble collection if needed in future
                // player.getCollectedNobles().add(noble);

                break; // Only one noble per turn
            }
        }
    }

    /**
     * 获取玩家
     * @param playerId 玩家ID
     * @return 玩家对象，如果未找到则返回null
     */
    public Player getPlayerById(Long playerId) {
        for (Player player : players) {
            if (player.getUserId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    /**
     * 检查是否是玩家的回合
     * @param player 要检查的玩家
     * @return 如果是该玩家的回合则返回true
     */
    public boolean isPlayerTurn(Player player) {
        // 获取当前回合的玩家
        Player currentTurnPlayer = players.get(currentPlayer);
        // 比较userId而不是索引
        return player.getUserId().equals(currentTurnPlayer.getUserId());
    }

    /**
     * 根据ID查找卡牌
     * @param cardId 卡牌ID
     * @return 找到的卡牌，如果未找到则返回null
     */
    public Card findCardById(Long cardId) {
        // 检查所有可见卡牌
        for (Card card : visibleLevel1Cards) {
            if (card != null && card.getId().equals(cardId)) {
                return card;
            }
        }
        for (Card card : visibleLevel2Cards) {
            if (card != null && card.getId().equals(cardId)) {
                return card;
            }
        }
        for (Card card : visibleLevel3Cards) {
            if (card != null && card.getId().equals(cardId)) {
                return card;
            }
        }

        // 检查所有玩家的预留卡牌
        for (Player player : players) {
            for (Card card : player.getReservedCards()) {
                if (card != null && card.getId().equals(cardId)) {
                    return card;
                }
            }
        }

        return null;
    }

    /**
     * 检查玩家是否能购买卡牌
     * @param player 玩家
     * @param card 卡牌
     * @return 是否能购买
     */
    public boolean canBuyCard(Player player, Card card) {
        if (card == null) {
            return false;
        }

        // 检查卡牌所需资源
        Map<GemColor, Long> cost = card.getCost();
        Map<GemColor, Long> playerGems = new HashMap<>(player.getAllGems());
        Map<GemColor, Long> playerBonus = new HashMap<>(player.getAllBonusGems());

        Long goldPossess = player.getGem(GemColor.GOLD);

        for(GemColor color : GemColor.values()){
            if(color == GemColor.GOLD){continue;}
            Long colorCost = cost.getOrDefault(color, 0L);
            Long colorPossess = playerGems.getOrDefault(color, 0L);
            Long colorDiscount = playerBonus.getOrDefault(color, 0L);

            // min(0, colorCost - colorDiscount)计算实际需要多少gem token
            // max(colorPossess - 实际需要token) 计算需要多少金币来补足缺口
            Long actualCost = Math.max(0L, colorCost - colorDiscount); //减去折扣后，对应的cost最小为0
            Long goldDeficit = Math.min(0, colorPossess - actualCost); //拥有的gem token减去实际cost后
            // System.out.println("GemColor" + color + ", 此时goldPossess: " + goldPossess + ", 实际cost: " + actualCost + ", 玩家拥有对应gem: " + colorPossess + ", Gold缺口: " + goldDeficit);
            goldPossess += goldDeficit;
            
            if(goldPossess < 0){return false;}
        }
        return true;
    }

    /**
     * 玩家购买卡牌
     * @param player 购买卡牌的玩家
     * @param cardId 卡牌ID
     * @return 操作是否成功
     */
    public boolean buyCard(Player player, Long cardId) {
        // 检查是否是该玩家的回合
        if (!isPlayerTurn(player)) {
            return false;
        }

        // 检查游戏是否正在进行
        if (gameState != GameState.RUNNING) {
            return false;
        }

        Card card = findCardById(cardId);
        if (card == null) {
            return false;
        }

        // 检查玩家是否能购买这张卡
        if (!canBuyCard(player, card)) {
            return false;
        }

        // 执行购买操作
        // 扣除玩家宝石
        Map<GemColor, Long> cost = card.getCost();
        for (Map.Entry<GemColor, Long> entry : cost.entrySet()) {
            GemColor color = entry.getKey();
            Long requiredAmount = entry.getValue();

            // 减去玩家拥有的对应颜色卡牌折扣
            requiredAmount -= player.getBonusGem(color);

            if (requiredAmount <= 0) {
                continue; // 不需要支付这种颜色的宝石
            }

            // 玩家拥有的这种颜色的宝石
            Long playerAmount = player.getGem(color);

            if (playerAmount >= requiredAmount) {
                // 玩家有足够的这种颜色的宝石
                player.setGem(color, playerAmount - requiredAmount);
                // 归还宝石到公共区域
                availableGems.put(color, availableGems.get(color) + requiredAmount);
            } else {
                // 需要用黄金宝石补充
                Long deficit = requiredAmount - playerAmount;
                // 先用尽这种颜色的宝石
                if (playerAmount > 0) {
                    player.setGem(color, 0L);
                    availableGems.put(color, availableGems.get(color) + playerAmount);
                }
                // 再使用黄金宝石
                player.setGem(GemColor.GOLD, player.getGem(GemColor.GOLD) - deficit);
                availableGems.put(GemColor.GOLD, availableGems.get(GemColor.GOLD) + deficit);
            }
        }

        // 增加玩家对应颜色的折扣
        GemColor cardColor = card.getColor();
        player.setBonusGem(cardColor, player.getBonusGem(cardColor) + 1);

        // 增加玩家的分数
        player.setVictoryPoints(player.getVictoryPoints() + card.getPoints());

        // 从游戏板上移除卡牌
        boolean removed = false;
        if (visibleLevel1Cards.contains(card)) {
            visibleLevel1Cards.set(visibleLevel1Cards.indexOf(card), null);
            removed = true;
        } else if (visibleLevel2Cards.contains(card)) {
            visibleLevel2Cards.set(visibleLevel2Cards.indexOf(card), null);
            removed = true;
        } else if (visibleLevel3Cards.contains(card)) {
            visibleLevel3Cards.set(visibleLevel3Cards.indexOf(card), null);
            removed = true;
        } else {
            // 可能是预留的卡牌
            List<Card> reservedCards = player.getReservedCards();
            if (reservedCards.contains(card)) {
                reservedCards.remove(card);
                removed = true;
            }
        }

        if (!removed) {
            // 如果卡牌没有被移除，可能是出现了错误
            return false;
        }

        // 补充新卡牌
        fillVisibleCards(level1Deck, visibleLevel1Cards);
        fillVisibleCards(level2Deck, visibleLevel2Cards);
        fillVisibleCards(level3Deck, visibleLevel3Cards);

        return true;
    }

    /**
     * 玩家预留卡牌
     * @param player 预留卡牌的玩家
     * @param cardId 卡牌ID
     * @return 操作是否成功
     */
    public boolean reserveCard(Player player, Long cardId) {
        // 检查是否是该玩家的回合
        if (!isPlayerTurn(player)) {
            return false;
        }

        // 检查游戏是否正在进行
        if (gameState != GameState.RUNNING) {
            return false;
        }

        // 检查玩家已预留的卡牌数量是否已达上限
        if (player.getReservedCards().size() >= 3) {
            return false;
        }

        Card card = findCardById(cardId);
        if (card == null) {
            return false;
        }

        // 从游戏板上移除卡牌
        boolean removed = false;
        if (visibleLevel1Cards.contains(card)) {
            visibleLevel1Cards.set(visibleLevel1Cards.indexOf(card), null);
            removed = true;
        } else if (visibleLevel2Cards.contains(card)) {
            visibleLevel2Cards.set(visibleLevel2Cards.indexOf(card), null);
            removed = true;
        } else if (visibleLevel3Cards.contains(card)) {
            visibleLevel3Cards.set(visibleLevel3Cards.indexOf(card), null);
            removed = true;
        }

        if (!removed) {
            // 如果卡牌没有被移除，可能是出现了错误
            return false;
        }

        // 添加到玩家的预留卡牌中
        player.getReservedCards().add(card);

        // 玩家获得一个黄金宝石（如果还有）
        if (availableGems.get(GemColor.GOLD) > 0) {
            player.setGem(GemColor.GOLD, player.getGem(GemColor.GOLD) + 1);
            availableGems.put(GemColor.GOLD, availableGems.get(GemColor.GOLD) - 1);
        }

        // 补充新卡牌
        fillVisibleCards(level1Deck, visibleLevel1Cards);
        fillVisibleCards(level2Deck, visibleLevel2Cards);
        fillVisibleCards(level3Deck, visibleLevel3Cards);

        return true;
    }

}
