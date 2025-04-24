package ch.uzh.ifi.hase.soprafs24.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class PlayerActionService {

    public void takeTwoSameGems(Game game, GemColor color) {
        Player player = game.getPlayers().get(game.getCurrentPlayer());
        Map<GemColor, Long> boardGems = game.getAvailableGems();
    
        if (boardGems.getOrDefault(color, 0L) < 2) {
            throw new IllegalArgumentException("Not enough gems of color: " + color);
        }
    
        boardGems.put(color, boardGems.get(color) - 2);
        player.setGem(color, player.getGem(color) + 2);
        game.endTurn();
    }
    

    public void takeThreeDifferentGems(Game game, List<GemColor> colors) {
        Player player = game.getPlayers().get(game.getCurrentPlayer());
        Map<GemColor, Long> boardGems = game.getAvailableGems();
    
        if (colors == null || colors.size() != 3 || new HashSet<>(colors).size() != 3) {
            throw new IllegalArgumentException("You must select exactly 3 different gem colors.");
        }
    
        for (GemColor color : colors) {
            if (boardGems.getOrDefault(color, 0L) < 1) {
                throw new IllegalArgumentException("Not enough gems of color: " + color);
            }
        }
    
        for (GemColor color : colors) {
            boardGems.put(color, boardGems.get(color) - 1);
            player.setGem(color, player.getGem(color) + 1);
        }
        game.endTurn();
    }
    

    public Map<GemColor, Long> getPaymentForCard(Game game, Card card) {
        Player player = game.getPlayers().get(game.getCurrentPlayer());
        Map<GemColor, Long> cost = card.getCost();
        Map<GemColor, Long> payment = new HashMap<>();
    
        long whiteCost = Math.max(0, cost.getOrDefault(GemColor.WHITE, 0L) - player.getBonusGem(GemColor.WHITE));
        long blueCost = Math.max(0, cost.getOrDefault(GemColor.BLUE, 0L) - player.getBonusGem(GemColor.BLUE));
        long greenCost = Math.max(0, cost.getOrDefault(GemColor.GREEN, 0L) - player.getBonusGem(GemColor.GREEN));
        long redCost = Math.max(0, cost.getOrDefault(GemColor.RED, 0L) - player.getBonusGem(GemColor.RED));
        long blackCost = Math.max(0, cost.getOrDefault(GemColor.BLACK, 0L) - player.getBonusGem(GemColor.BLACK));
    
        long whiteGems = player.getGem(GemColor.WHITE);
        long blueGems = player.getGem(GemColor.BLUE);
        long greenGems = player.getGem(GemColor.GREEN);
        long redGems = player.getGem(GemColor.RED);
        long blackGems = player.getGem(GemColor.BLACK);
        long goldGems = player.getGem(GemColor.GOLD);
    
        long goldUsed = 0;
    
        long whiteUsed = Math.min(whiteCost, whiteGems);
        goldUsed += (whiteCost - whiteUsed);
        payment.put(GemColor.WHITE, whiteUsed);
    
        long blueUsed = Math.min(blueCost, blueGems);
        goldUsed += (blueCost - blueUsed);
        payment.put(GemColor.BLUE, blueUsed);
    
        long greenUsed = Math.min(greenCost, greenGems);
        goldUsed += (greenCost - greenUsed);
        payment.put(GemColor.GREEN, greenUsed);
    
        long redUsed = Math.min(redCost, redGems);
        goldUsed += (redCost - redUsed);
        payment.put(GemColor.RED, redUsed);
    
        long blackUsed = Math.min(blackCost, blackGems);
        goldUsed += (blackCost - blackUsed);
        payment.put(GemColor.BLACK, blackUsed);
    
        if (goldUsed > goldGems) {
            return null; // Cannot afford
        }
    
        payment.put(GemColor.GOLD, goldUsed);
        return payment;
    }
    

    public void buyCard(Game game, Long cardId) {
        Player player = game.getPlayers().get(game.getCurrentPlayer());
        Card targetCard = null;
    
        // Search in visible level 1 cards
        for (Card card : game.getVisibleLevel1Cards()) {
            if (card != null && card.getId().equals(cardId)) {
                targetCard = card;
                game.getVisibleLevel1Cards().remove(card);
                break;
            }
        }
    
        // If not found, check level 2
        if (targetCard == null) {
            for (Card card : game.getVisibleLevel2Cards()) {
                if (card != null && card.getId().equals(cardId)) {
                    targetCard = card;
                    game.getVisibleLevel2Cards().remove(card);
                    break;
                }
            }
        }
    
        // If not found, check level 3
        if (targetCard == null) {
            for (Card card : game.getVisibleLevel3Cards()) {
                if (card != null && card.getId().equals(cardId)) {
                    targetCard = card;
                    game.getVisibleLevel3Cards().remove(card);
                    break;
                }
            }
        }
    
        // Still not found? Try reserved cards
        if (targetCard == null) {
            for (Card card : player.getReservedCards()) {
                if (card.getId().equals(cardId)) {
                    targetCard = card;
                    player.getReservedCards().remove(card);
                    break;
                }
            }
        }
    
        if (targetCard == null) {
            throw new IllegalArgumentException("Card with ID " + cardId + " not found in visible or reserved cards.");
        }
    
        // Get payment
        Map<GemColor, Long> payment = getPaymentForCard(game, targetCard);
        if (payment == null) {
            throw new IllegalStateException("Player cannot afford the card.");
        }
    
        // Deduct gems from player
        for (Map.Entry<GemColor, Long> entry : payment.entrySet()) {
            GemColor color = entry.getKey();
            long amountToDeduct = entry.getValue();
            player.setGem(color, player.getGem(color) - amountToDeduct);
        }
    

        // Update points
        player.setVictoryPoints(player.getVictoryPoints() + targetCard.getPoints());
    
        // Increase the bonus gem (discount)
        GemColor bonusColor = targetCard.getColor();
        player.setBonusGem(bonusColor, player.getBonusGem(bonusColor) + 1);
        game.endTurn();
    }
    

    public void reserveCard(Game game, Long cardId) {
        Player player = game.getPlayers().get(game.getCurrentPlayer());
    
        if (player.getReservedCards().size() >= 3) {
            throw new IllegalStateException("Cannot reserve more than 3 cards.");
        }
    
        Card cardToReserve = null;
    
        // Search all visible levels
        for (Card card : game.getVisibleLevel1Cards()) {
            if (card.getId().equals(cardId)) {
                cardToReserve = card;
                game.getVisibleLevel1Cards().remove(card);
                break;
            }
        }
    
        if (cardToReserve == null) {
            for (Card card : game.getVisibleLevel2Cards()) {
                if (card.getId().equals(cardId)) {
                    cardToReserve = card;
                    game.getVisibleLevel2Cards().remove(card);
                    break;
                }
            }
        }
    
        if (cardToReserve == null) {
            for (Card card : game.getVisibleLevel3Cards()) {
                if (card.getId().equals(cardId)) {
                    cardToReserve = card;
                    game.getVisibleLevel3Cards().remove(card);
                    break;
                }
            }
        }
    
        if (cardToReserve == null) {
            throw new IllegalArgumentException("Card ID not found in any visible cards.");
        }
    
        player.getReservedCards().add(cardToReserve);
    
        // Give gold gem if available
        Map<GemColor, Long> boardGems = game.getAvailableGems();
        if (boardGems.getOrDefault(GemColor.GOLD, 0L) > 0) {
            boardGems.put(GemColor.GOLD, boardGems.get(GemColor.GOLD) - 1);
            player.setGem(GemColor.GOLD, player.getGem(GemColor.GOLD) + 1);
        }
        game.endTurn();
    }
    


}
