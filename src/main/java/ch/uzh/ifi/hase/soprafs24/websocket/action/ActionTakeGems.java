package ch.uzh.ifi.hase.soprafs24.websocket.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;


public class ActionTakeGems extends Action<List<GemColor>>{

  @Override
  public boolean validate(Game game, Player player, List<GemColor> colorList){
    // check if the game is running
    if(game.getGameState() != Game.GameState.RUNNING){return false;}

    // check if its this player's turn
    if(!game.isPlayerTurn(player)) {return false;}

    if(colorList.contains(GemColor.GOLD)){return false;} // player can't take gold gem directly

    if(colorList.size() != colorList.stream().distinct().count()){return false;} // there is duplicate color

    if(colorList.size() == 3){
      for(GemColor color : colorList){
        if(game.getAvailableGemOfColor(color) == 0){return false;} // no enough available gem of that color
      }
    }else if(colorList.size() == 1){
      GemColor color = colorList.get(0);
      if(game.getAvailableGemOfColor(color) < 4){return false;} // no applicable in this situation
    }else{
      // incorrect Gem taking action
      return false;
    }

    return true;
  }
  
  @Override
  public boolean execute(Game game, Player player, List<GemColor> colorList){
    if(!validate(game, player, colorList)){return false;}

    if(colorList.size() == 3){
      for(GemColor color : colorList){
        player.setGem(color, player.getGem(color) + 1);
        game.setAvailableGemOfColor(color, game.getAvailableGemOfColor(color) - 1);
      }
    }else{ //colorList.size() == 1
      GemColor color = colorList.get(0);
      player.setGem(color, player.getGem(color) + 2);
      game.setAvailableGemOfColor(color, game.getAvailableGemOfColor(color) - 2);
    }

    return true;
  }
}
