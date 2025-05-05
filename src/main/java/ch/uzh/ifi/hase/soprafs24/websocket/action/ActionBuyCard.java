package ch.uzh.ifi.hase.soprafs24.websocket.action;

import java.util.HashMap;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;

public class ActionBuyCard extends Action<Long>{
  
  /**
   * @param param CardId<Long>
   * @return true if Buy specific card is applicable
   */
  @Override
  public boolean validate(Game game, Player player, Long cardId){
    Card targetCard = game.findCardById(cardId);
    // check if the card is on the board
    if(targetCard == null){return false;}

    // check if the player is active
    if(!game.isPlayerTurn(player)){return false;}

    // check if the game is ongoing
    if(game.getGameState()!=Game.GameState.RUNNING){return false;}

    // check if the player have enough resource to buy this card
    Map<GemColor, Long> cost = targetCard.getCost();
    Map<GemColor, Long> playerGems = new HashMap<>(player.getAllGems());
    Map<GemColor, Long> playerBonus = new HashMap<>(player.getAllBonusGems());

    Long goldPossess = player.getGem(GemColor.GOLD);

    for(GemColor color : GemColor.values()){
      if(color == GemColor.GOLD){continue;}
      Long colorCost = cost.getOrDefault(color, 0L);
      Long colorPossess = playerGems.getOrDefault(color, 0L);
      Long colorDiscount = playerBonus.getOrDefault(color, 0L);

      // min(0, colorCost - colorDiscount) calculate gem token
      // max(colorPossess - 实际需要token) calculate how many gold is needed 
      Long actualCost = Math.max(0L, colorCost - colorDiscount); //减去折扣后，对应的cost最小为0
      Long goldDeficit = Math.min(0, colorPossess - actualCost); //拥有的gem token减去实际cost后
      // System.out.println("GemColor" + color + ", 此时goldPossess: " + goldPossess + ", 实际cost: " + actualCost + ", 玩家拥有对应gem: " + colorPossess + ", Gold缺口: " + goldDeficit);
      goldPossess += goldDeficit;
      
      if(goldPossess < 0){return false;}
    }
    return true;
  }

  /**
   * execute buy card action
   * will automatically check if the action is applicable
   * @return true if the action conducted successfully
   */
  @Override
  public boolean execute(Game game, Player player, Long cardId){
    // check if the action is applicable
    if(!validate(game, player, cardId)){return false;}

    // -- execute action --
    Card targetCard = game.findCardById(cardId);

    // subtract player's gems
    Map<GemColor, Long> cost = targetCard.getCost();
    for(Map.Entry<GemColor, Long> entry: cost.entrySet()){
      GemColor color = entry.getKey();
      Long requiredAmount = entry.getValue();

      // calculate discount
      requiredAmount -= player.getBonusGem(color);
      if(requiredAmount <= 0){
        continue; // player don't have to pay this type of gem
      }

      Long playerAmount = player.getGem(color);

      if (playerAmount >= requiredAmount){ // player has enough gems for this color
        // subtract player's gems
        player.setGem(color, playerAmount - requiredAmount);
        // return the gems to board
        game.setAvailableGemOfColor(color, game.getAvailableGemOfColor(color) + requiredAmount);
      
      }else{ //player has to use gold gem
        Long deficit = requiredAmount - playerAmount;
        
        player.setGem(color, 0L);
        game.setAvailableGemOfColor(color, game.getAvailableGemOfColor(color) + playerAmount);

        player.setGem(GemColor.GOLD, player.getGem(GemColor.GOLD) - deficit);
        game.setAvailableGemOfColor(GemColor.GOLD, game.getAvailableGemOfColor(GemColor.GOLD) + deficit);
      }

    }

    // increase player's bonus
    GemColor cardColor = targetCard.getColor();
    player.setBonusGem(cardColor, player.getBonusGem(cardColor) + 1);

    // increase player's victory point
    player.setVictoryPoints(player.getVictoryPoints() + targetCard.getPoints());

    // remove card from the board or from player's reserved List
    boolean removed = false;
    removed |= game.removeCardFromBoard(targetCard);
    removed |= player.removeCardFromReserved(targetCard);

    if(!removed){
      // 如果卡牌没有被正确移除，可能出现了错误
      return false;
    }

    game.fillVisibleAllVisibleCardsOnBoard();
    // -- end of execution --
    return true;
  }
}
