package ch.uzh.ifi.hase.soprafs24.websocket.action;

import org.springframework.stereotype.Component;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

@Component
public class ActionReserveCard extends Action<Long>{

  @Override
  public boolean validate(Game game, Player player, Long cardId){
    // check if the game is running
    if (game.getGameState() != Game.GameState.RUNNING) {
      return false;
    }

    // check if its this player's turn
    if (!game.isPlayerTurn(player)) {return false;}

    // if the player already reserved 3 cards
    if (player.getReservedCards().size() >= 3) {return false;}

    Card card = game.findCardById(cardId);
    if (card == null) {return false;}

    return true;
  }

  @Override
  public boolean execute(Game game, Player player, Long cardId){
    if(!validate(game, player, cardId)){return false;}

    // -- execute
    Card targetcard = game.findCardById(cardId);
    game.removeCardFromBoard(targetcard);

    player.getReservedCards().add(targetcard);

    if (game.getAvailableGemOfColor(GemColor.GOLD) > 0){
      player.setGem(GemColor.GOLD, player.getGem(GemColor.GOLD) + 1);
      game.setAvailableGemOfColor(GemColor.GOLD, game.getAvailableGemOfColor(GemColor.GOLD) - 1);
    }

    game.fillVisibleAllVisibleCardsOnBoard();

    // -- end of execution
    return true;
  }
  
}
