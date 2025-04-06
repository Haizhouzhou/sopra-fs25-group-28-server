package ch.uzh.ifi.hase.soprafs24.websocket.game;

import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;

public class Game {

  public enum GameState {
    NOT_STARTED,    // 游戏尚未开始
    RUNNING,        // 游戏正在进行中
    FINISHED        // 游戏已结束
  }

  // unique id for the game
  private final String gameId;

  private final List<Player> players;

  private int currentRound = 0;

  // getters
  public String getGameId(){return this.gameId;}

  public List<Player> getPlayers(){return this.players;}

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
