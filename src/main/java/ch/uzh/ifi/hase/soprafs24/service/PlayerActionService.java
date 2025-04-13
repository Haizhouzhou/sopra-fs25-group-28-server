package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;

import java.util.HashSet;
import java.util.List;

public class PlayerActionService {

    public void takeTwoSameGems(User player, GemColor color) {                          //TODO: Implement player variables: blue, red, green, black, white, gold counters
        switch (color) {                                                                //TODO: Implement Gaem variables: blue, red, green, black, white, gold counters
            case BLUE:
                if (blue < 2) {
                    throw new IllegalArgumentException("Not enough blue gems in supply.");
                }
                blue -= 2;
                player.setBlue(player.getBlue() + 2); // assuming this method exists
                break;
    
            case RED:
                if (red < 2) {
                    throw new IllegalArgumentException("Not enough red gems in supply.");
                }
                red -= 2;
                player.setRed(player.getRed() + 2);
                break;
    
            case GREEN:
                if (green < 2) {
                    throw new IllegalArgumentException("Not enough green gems in supply.");
                }
                green -= 2;
                player.setGreen(player.getGreen() + 2);
                break;
    
            case BLACK:
                if (black < 2) {
                    throw new IllegalArgumentException("Not enough black gems in supply.");
                }
                black -= 2;
                player.setBlack(player.getBlack() + 2);
                break;
    
            case WHITE:
                if (white < 2) {
                    throw new IllegalArgumentException("Not enough white gems in supply.");
                }
                white -= 2;
                player.setWhite(player.getWhite() + 2);
                break;
    
            default:
                throw new IllegalArgumentException("Invalid gem color.");
        }
    }
    

    // Take up to 3 different gems
public void takeThreeDifferentGems(User player, List<GemColor> colors) {
    // Check that the list contains exactly 3 different colors
    if (colors == null || colors.size() != 3 || new HashSet<>(colors).size() != 3) {
        throw new IllegalArgumentException("You must select exactly 3 different gem colors.");
    }

    for (GemColor color : colors) {
        switch (color) {
            case BLUE:
                if (blue < 1) {
                    throw new IllegalArgumentException("Not enough blue gems in supply.");
                }
                break;

            case RED:
                if (red < 1) {
                    throw new IllegalArgumentException("Not enough red gems in supply.");
                }
                break;

            case GREEN:
                if (green < 1) {
                    throw new IllegalArgumentException("Not enough green gems in supply.");
                }
                break;

            case BLACK:
                if (black < 1) {
                    throw new IllegalArgumentException("Not enough black gems in supply.");
                }
                break;

            case WHITE:
                if (white < 1) {
                    throw new IllegalArgumentException("Not enough white gems in supply.");
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid gem color selected.");
        }
    }

    // If all colors are available, apply the changes
    for (GemColor color : colors) {
        switch (color) {
            case BLUE:
                blue -= 1;
                player.setBlue(player.getBlue() + 1);
                break;

            case RED:
                red -= 1;
                player.setRed(player.getRed() + 1);
                break;

            case GREEN:
                green -= 1;
                player.setGreen(player.getGreen() + 1);
                break;

            case BLACK:
                black -= 1;
                player.setBlack(player.getBlack() + 1);
                break;

            case WHITE:
                white -= 1;
                player.setWhite(player.getWhite() + 1);
                break;
        }
    }
}


    public void buyCard(User player, Card card) {                                                  //TODO: Implement player List: bought cards
        // Implement logic to buy card
    }

    public void reserveCard(Player player, Long cardId) {
        // Validate player's reserved cards
        List<Long> reservedCards = player.getReservedCards();                                    //TODO: Implement player List: reservedCards                
        if (reservedCards.size() >= 3) {
            throw new IllegalStateException("Cannot reserve more than 3 cards.");
        }
    
        // Find and remove the card from the board
        boolean cardFound = false;
    
        if (visibleLevel1Cards.contains(cardId)) {
            visibleLevel1Cards.remove(cardId);
            cardFound = true;
        } else if (visibleLevel2Cards.contains(cardId)) {
            visibleLevel2Cards.remove(cardId);
            cardFound = true;
        } else if (visibleLevel3Cards.contains(cardId)) {
            visibleLevel3Cards.remove(cardId);
            cardFound = true;
        }
    
        if (!cardFound) {
            throw new IllegalArgumentException("Card ID not found on the board.");
        }
    
        // Add the card to the player's reserved hand
        reservedCards.add(cardId);
    
        // Give the player a gold gem
        player.setGold(player.getGold() + 1);
    }
    public boolean canAffordCard(Player player, Card card) {                                     //TODO: Implement Method to check if cost can be covered

        return true;
    }

    

    // Add other actions here
}
