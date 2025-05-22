package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Card;

public class PlayerSnapshotTest {

  private Map<GemColor, Long> mockGems;
  private Map<GemColor, Long> mockBonusGems;
  private List<Card> mockReservedCards;

  private String mockGameId = "mockGameId";

  @BeforeEach
  public void testSetup(){
    mockGems = new HashMap<>();
    mockGems.put(GemColor.BLACK, 0L);
    mockGems.put(GemColor.BLUE, 0L);
    mockGems.put(GemColor.GREEN, 0L);
    mockGems.put(GemColor.RED, 0L);
    mockGems.put(GemColor.WHITE, 0L);
    mockGems.put(GemColor.GOLD, 0L);

    mockBonusGems = new HashMap<>();
    mockBonusGems.put(GemColor.BLACK, 2L);
    mockBonusGems.put(GemColor.BLUE, 2L);
    mockBonusGems.put(GemColor.GREEN, 2L);
    mockBonusGems.put(GemColor.RED, 2L);
    mockBonusGems.put(GemColor.WHITE, 2L);
    mockBonusGems.put(GemColor.GOLD, 0L);

    mockReservedCards = new ArrayList<>();
  }

  @Test
  public void testPlayerSnapshotSerialization() throws Exception{
    //arrange
    Player p1 = mock(Player.class);
    when(p1.getUserId()).thenReturn(1L);
    when(p1.getVictoryPoints()).thenReturn(3L);
    when(p1.getAllGems()).thenReturn(mockGems);
    when(p1.getAllBonusGems()).thenReturn(mockBonusGems);
    when(p1.getReservedCards()).thenReturn(mockReservedCards);
    when(p1.getBelongsToGameId()).thenReturn(mockGameId);

    //act
    PlayerSnapshot snapshot = PlayerSnapshot.createFromPlayer(p1, mockGameId);
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(snapshot);
    ReadContext ctx = JsonPath.parse(json);

    //assert
    System.out.println("Serialized PlayerSnapshot: " + json);
    assertNotNull(json);
    assertEquals(p1.getUserId(), ctx.read("$.userId", Long.class));
    assertEquals(p1.getAllGems().get(GemColor.GREEN), ctx.read("$.gems.GREEN", Long.class));
  }
  
}
