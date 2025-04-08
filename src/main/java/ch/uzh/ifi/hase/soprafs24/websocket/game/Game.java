package ch.uzh.ifi.hase.soprafs24.websocket.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class Game {

  public enum GameState {
    NOT_STARTED,    // 游戏尚未开始
    RUNNING,        // 游戏正在进行中
    FINISHED        // 游戏已结束
  }

  // unique id for the game
  private final String gameId;

  private final List<Player> players;

  // card deck of different levels of card
  // TODO: replace Long with Card class
  private final Stack<Long> level1Deck = new Stack<>();
  private final Stack<Long> level2Deck = new Stack<>();
  private final Stack<Long> level3Deck = new Stack<>();

  // visible card on the board
  // TODO: replace Long with Card class
  private final List<Long> visibleLevel1Cards = new ArrayList<>(4);
  private final List<Long> visibleLevel2Cards = new ArrayList<>(4);
  private final List<Long> visibleLevel3Cards = new ArrayList<>(4);

  private int currentRound = 0;

  // getters
  public String getGameId(){return this.gameId;}
  public List<Player> getPlayers(){return this.players;}

  public void initialize(){
    // TODO: fill the decks with predifined cards

    // shuffle the deck
    Collections.shuffle(level1Deck);
    Collections.shuffle(level2Deck);
    Collections.shuffle(level3Deck);

    // TODO: draw initial cards from the decks
    

    // TODO: initialize players' game status
    for(Player player : players){
      // TODO: not fully implemented yet
      player.initializeGameStatus();
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

  public Object getGameInformation(){
    return new Object();
  }
}
