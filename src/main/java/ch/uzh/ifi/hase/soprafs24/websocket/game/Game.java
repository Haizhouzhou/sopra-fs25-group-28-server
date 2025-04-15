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

public class Game {

  public enum GameState {
    NOT_STARTED,    // 游戏尚未开始
    RUNNING,        // 游戏正在进行中
    FINISHED        // 游戏已结束
  }

  private final Long VICTORYPOINTS = 20L;

  // unique id for the game
  private final String gameId;

  private final List<Player> players;

  // available gems on the board
  private final Map<GemColor, Long> availableGems = new HashMap<>();

  // card deck of different levels of card
  private final Stack<Card> level1Deck = new Stack<>();
  private final Stack<Card> level2Deck = new Stack<>();
  private final Stack<Card> level3Deck = new Stack<>();

  // visible card on the board
  private final List<Card> visibleLevel1Cards = new ArrayList<>(4);
  private final List<Card> visibleLevel2Cards = new ArrayList<>(4);
  private final List<Card> visibleLevel3Cards = new ArrayList<>(4);

  // nobles on the board
  private final List<Noble> visibleNoble = new ArrayList<>(4);

  private int currentPlayer = 0;
  private int currentRound = 0;

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
  public Game(String gameRoomId, Set<Player> players){
    this.gameId = gameRoomId;
    this.players = new ArrayList<>(players);
    Collections.shuffle(this.players);
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
  public boolean checkVictoryCondition(){
    for(Player player : players){
      if(player.getVictoryPoints() >= VICTORYPOINTS){
        return true;
      }
    }
    return false;
  }
}
