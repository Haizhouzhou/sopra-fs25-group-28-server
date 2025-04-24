package ch.uzh.ifi.hase.soprafs24.websocket;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Noble;

public class NobleTest {

  @Test
  public void testNobleCreationFromJson() throws Exception{
    //arrange
    ObjectMapper mapper = new ObjectMapper();
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-nobles.json");
    assertNotNull(inputStream, "test-nobles.json not found in resources");
    
    //act
    List<Noble> nobles = mapper.readValue(
      inputStream, 
      mapper.getTypeFactory().constructCollectionType(List.class, Noble.class)
    );

    assertEquals(1, nobles.size());
    Noble noble = nobles.get(0);
    assertEquals(1L, noble.getId());
    assertEquals(3L, noble.getPoints());
    Map<GemColor, Long> cost = noble.getCost();
    assertEquals(2, cost.size());
    assertEquals(4L, cost.get(GemColor.BLUE));
    assertEquals(4L, cost.get(GemColor.GREEN));
  }
  
}
