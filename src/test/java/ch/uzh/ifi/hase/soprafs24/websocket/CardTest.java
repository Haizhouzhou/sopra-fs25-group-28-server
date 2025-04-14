package ch.uzh.ifi.hase.soprafs24.websocket;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;

public class CardTest {

  @Test
  public void testCardCreationFromJson() throws Exception{
    //arrange
    ObjectMapper mapper = new ObjectMapper();
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-cards.json");
    assertNotNull(inputStream, "test-cards.json not found in resources");
    

    //act
    List<Card> cards = mapper.readValue(
      inputStream,
      mapper.getTypeFactory().constructCollectionType(List.class, Card.class)
    );

    assertEquals(1, cards.size());
    Card card = cards.get(0);
    assertEquals(1L, card.getId());
    assertEquals(1, card.getTier());
    assertEquals(GemColor.BLUE, card.getColor());
    assertEquals(0L, card.getPoints());
    Map<GemColor, Long> cost = card.getCost();
    assertEquals(1, cost.size());
    assertEquals(3L, cost.get(GemColor.BLACK));
  }
  
}
