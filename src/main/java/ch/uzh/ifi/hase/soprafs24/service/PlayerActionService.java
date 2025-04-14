package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;

import java.util.*;

public class PlayerActionService {

    public void takeTwoSameGems(Player player, GemColor color, Map<GemColor, Long> boardGems) {
        if (boardGems.getOrDefault(color, 0L) < 2) {
            throw new IllegalArgumentException("Not enough gems of color: " + color);
        }

        boardGems.put(color, boardGems.get(color) - 2);
        player.setGem(color, player.getGem(color) + 2);
    }

    public void takeThreeDifferentGems(Player player, List<GemColor> colors, Map<GemColor, Long> boardGems) {
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
    }

    public Map<GemColor, Long> getPaymentForCard(Player player, Card card) {
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

    public void buyCard(Game game, Player player, Long cardId) {
        Card targetCard = null;
    
        // Search in visible level 1 cards
        for (Card card : game.getVisibleLevel1Cards()) {
            if (card != null && card.getId().equals(cardId)) {
                targetCard = card;
                break;
            }
        }
    
        // If not found, check level 2
        if (targetCard == null) {
            for (Card card : game.getVisibleLevel2Cards()) {
                if (card != null && card.getId().equals(cardId)) {
                    targetCard = card;
                    break;
                }
            }
        }
    
        // If not found, check level 3
        if (targetCard == null) {
            for (Card card : game.getVisibleLevel3Cards()) {
                if (card != null && card.getId().equals(cardId)) {
                    targetCard = card;
                    break;
                }
            }
        }
    
        // Still not found? Try reserved cards
        if (targetCard == null) {
            for (Card card : player.getReservedCards()) {
                if (card.getId().equals(cardId)) {
                    targetCard = card;
                    break;
                }
            }
        }
    
        // If still null, it doesn't exist anywhere valid
        if (targetCard == null) {
            throw new IllegalArgumentException("Card with ID " + cardId + " not found in visible or reserved cards.");
        }
        Map<GemColor, Long> payment = getPaymentForCard(player, targetCard);
        if (payment == null) {
            throw new IllegalStateException("Player cannot afford the card.");
        }
            // Step 3: Deduct the gems from the player
    for (Map.Entry<GemColor, Long> entry : payment.entrySet()) {
        GemColor color = entry.getKey();
        long amountToDeduct = entry.getValue();
        long currentAmount = player.getGem(color);
        player.setGem(color, currentAmount - amountToDeduct);
    }

    // Add the card to player's purchased cards
    player.getPurchasedCards().add(targetCard);

    // Add the points of the card to the player's victory points
    long currentPoints = player.getVictoryPoints();
    player.setVictoryPoints(currentPoints + targetCard.getPoints());

    // Increase the bonus gem (discount) for the color of this card
    GemColor bonusColor = targetCard.getColor();
    long currentBonus = player.getBonusGem(bonusColor);
    player.setBonusGem(bonusColor, currentBonus + 1);

    
        
    }                                                                               //TODO: implement: reserved cards, purchased cards, remove card from board or hand
    

    public void reserveCard(Player player, Long cardId, List<Card> visibleCards, Map<GemColor, Long> boardGems) {
        if (player.getReservedCards().size() >= 3) {
            throw new IllegalStateException("Cannot reserve more than 3 cards.");
        }

        Card cardToReserve = null;
        for (Card card : visibleCards) {
            if (card.getId().equals(cardId)) {
                cardToReserve = card;
                break;
            }
        }

        if (cardToReserve == null) {
            throw new IllegalArgumentException("Card ID not found on the board.");
        }

        visibleCards.remove(cardToReserve);
        player.getReservedCards().add(cardToReserve);

        if (boardGems.get(GemColor.GOLD) > 0) {
            boardGems.put(GemColor.GOLD, boardGems.get(GemColor.GOLD) - 1);
            player.setGem(GemColor.GOLD, player.getGem(GemColor.GOLD) + 1);
        }
    }
}
