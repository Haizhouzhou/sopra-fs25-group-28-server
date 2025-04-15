package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class GameSnapshotTest {

  private Map<GemColor, Long> mockGems;
  private Map<GemColor, Long> mockBonusGems;
  private List<Long> mockReservedCardIds;

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

    mockReservedCardIds = new ArrayList<>();
  }

  @Test
  public void testGameSnapshotSerialization() throws Exception{
    //arrange
    PlayerSnapshot p1 = mock(PlayerSnapshot.class);
    when(p1.getUserId()).thenReturn(1L);
    when(p1.getVictoryPoints()).thenReturn(3L);
    when(p1.getGems()).thenReturn(mockGems);
    when(p1.getBonusGems()).thenReturn(mockBonusGems);
    when(p1.getReservedCardIds()).thenReturn(mockReservedCardIds);

    Player player = mock(Player.class); // mock player
    List<Player> players = List.of(player);

    Game game = mock(Game.class);
    when(game.getGameId()).thenReturn("test-game");
    when(game.getPlayers()).thenReturn(players);
    when(game.getCurrentPlayer()).thenReturn(0);
    when(game.getCurrentRound()).thenReturn(1);
    when(game.getAvailableGems()).thenReturn(mockGems);
    when(game.getVisibleLevel1Cards()).thenReturn(new ArrayList<>());
    when(game.getVisibleLevel2Cards()).thenReturn(new ArrayList<>());
    when(game.getVisibleLevel3Cards()).thenReturn(new ArrayList<>());
    when(game.getVisibleNoble()).thenReturn(new ArrayList<>());
  
    try (MockedStatic<PlayerSnapshot> mockedStatic = mockStatic(PlayerSnapshot.class)) {
      mockedStatic.when(() -> PlayerSnapshot.createFromPlayer(player)).thenReturn(p1);

      // Act
      GameSnapshot snapshot = GameSnapshot.createFromGame(game);

      // Assert
      assert snapshot.getGameId().equals("test-game");
      assert snapshot.getPlayerSnapshots().size() == 1;
      assert snapshot.getPlayerSnapshots().get(0).getUserId() == 1L;
    }
  }
  
}
