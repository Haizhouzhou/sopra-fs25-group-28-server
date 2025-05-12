package ch.uzh.ifi.hase.soprafs24.websocket.action;

import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public abstract class Action<P> {

  /**
   * if action is feasible
   */
  public abstract boolean validate(Game game, Player player, P param);

  /**
   * execute action
   * will automatically check if the action is applicable
   */
  public abstract boolean execute(Game game, Player player, P param);
  
}
