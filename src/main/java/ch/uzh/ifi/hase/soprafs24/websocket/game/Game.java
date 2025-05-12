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
import ch.uzh.ifi.hase.soprafs24.websocket.action.ActionBuyCard;
import ch.uzh.ifi.hase.soprafs24.websocket.action.ActionReserveCard;
import ch.uzh.ifi.hase.soprafs24.websocket.action.ActionTakeGems;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Noble;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class Game {

  public enum GameState {
    NOT_STARTED,    // 游戏尚未开始
    RUNNING,        // 游戏正在进行中
    FINISHED        // 游戏已结束
  }

  private ActionBuyCard actionBuyCard = new ActionBuyCard();
  private ActionReserveCard actionReserveCard = new ActionReserveCard();
  private ActionTakeGems actionTakeGems = new ActionTakeGems();

  public final Long VICTORYPOINTS = 5L; //modified for M3 demo

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

  // TODO: final round handle
  private boolean finalRound = false;


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
  public GameRoom getGameRoom() {return this.gameRoom;}
  public Long getAvailableGemOfColor(GemColor color){return availableGems.get(color);}

  // setters
  public void setGameState(GameState gameState){this.gameState = gameState;}
  public void setAvailableGemOfColor(GemColor color, Long amount){
    availableGems.put(color, amount);
  }

  // other helper methods of modifying game board
  public boolean removeCardFromBoard(Card targetcard){
    if (visibleLevel1Cards.contains(targetcard)) {
      visibleLevel1Cards.set(visibleLevel1Cards.indexOf(targetcard), null);
      return true;
    } else if (visibleLevel2Cards.contains(targetcard)) {
      visibleLevel2Cards.set(visibleLevel2Cards.indexOf(targetcard), null);
      return true;
    } else if (visibleLevel3Cards.contains(targetcard)) {
      visibleLevel3Cards.set(visibleLevel3Cards.indexOf(targetcard), null);
      return true;
    } else{
      return false;
    }
  }

  // constructor
  public Game(GameRoom gameRoom, String gameId, Set<Player> players) {
    this.gameRoom = gameRoom;
    this.gameId = gameId;
    this.players = new ArrayList<>(players);
    Collections.shuffle(this.players);
  }

  public Long getWinnerId() {
    Player winner = null;
    Long maxPoints = -1L;

    for (Player player : players) {
      if (player.getVictoryPoints() > maxPoints && player.getVictoryPoints() >= VICTORYPOINTS) {
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

  public void fillVisibleAllVisibleCardsOnBoard(){
    fillVisibleCards(level1Deck, visibleLevel1Cards);
    fillVisibleCards(level2Deck, visibleLevel2Cards);
    fillVisibleCards(level3Deck, visibleLevel3Cards);
  }

  // TODO: may need some modification
  public GameSnapshot getGameInformation(){
    return GameSnapshot.createFromGame(this);
  }

  // TODO: make it public or private?
  /**
   *
   * @return true if there is player reach the condition of winning
   */
  public boolean checkVictoryCondition() {
    for(Player player : players){
      if(player.getVictoryPoints() >= VICTORYPOINTS){
        finalRound = true;
        return true;
      }
    }
    finalRound = false;
    return false;
  }



  // Game.java
  public void endTurn() {
    // 1. Refill empty visible card slots
    fillVisibleAllVisibleCardsOnBoard();

    // 2. Check if the current player qualifies for a noble
    noblePurchase(this);

    // 3. Check for victory condition
    checkVictoryCondition();

    // 4. if its finalRound and all players have player the same number of turns
    if(finalRound && currentPlayer == players.size()-1){
      setGameState(GameState.FINISHED);
      System.out.println("Final round completed. Game finished.");
      return; // return here？
    }

    // 5. Increment round
    currentRound++;

    // 6. Advance to next player
    // Player currentTurnPlayer = players.get(currentPlayer);
    // System.out.println("回合结束，当前玩家: " + currentTurnPlayer.getUserId());

    currentPlayer = (currentPlayer + 1) % players.size();

    // Player nextTurnPlayer = players.get(currentPlayer);
    // System.out.println("下一回合玩家: " + nextTurnPlayer.getUserId());
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
     * check if the player can buy a card
     * @param player 玩家
     * @param card 卡牌
     * @return 是否能购买
     */
    public boolean canBuyCard(Player player, Card card) {
      if(card == null){
        return false;
      }
      return actionBuyCard.validate(this, player, card.getId());
    }

    /**
     * player buy card
     * @param player 购买卡牌的玩家
     * @param cardId 卡牌ID
     * @return true if the action success
     */
    public boolean buyCard(Player player, Long cardId) {
      return actionBuyCard.execute(this, player, cardId);
    }

    /**
     * 玩家预留卡牌
     * @param player 预留卡牌的玩家
     * @param cardId 卡牌ID
     * @return 操作是否成功
     */
    public boolean reserveCard(Player player, Long cardId) {
      return actionReserveCard.execute(this, player, cardId);
    }

    /**
     * player take Gems
     * @param player player who want to take gems
     * @param colorList which color(s) the player wants to take
     * @return true if action success
     */
    public boolean takeGems(Player player, List<GemColor> colorList){
      return actionTakeGems.execute(this, player, colorList);
    }
}
