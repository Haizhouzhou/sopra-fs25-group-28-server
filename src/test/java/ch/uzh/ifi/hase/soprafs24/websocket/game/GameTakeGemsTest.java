package ch.uzh.ifi.hase.soprafs24.websocket.game;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.GameRoom;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GameTakeGemsTest {

    @Mock
    private Player mockPlayer;

    @Mock
    private Player mockOtherPlayer;

    @Mock
    private GameRoom mockGameRoom;

    private Game game;

    // game state setup ---
    private String testRoomId = "test-take-gem-room";
    private Map<GemColor, Long> mockAvailableGems;
    private Set<Player> initialPlayers;

    @BeforeEach
    void setUp() {
      MockitoAnnotations.openMocks(this);

      // --- Game Instantiation ---
      initialPlayers = new HashSet<>();
      initialPlayers.add(mockPlayer);
      initialPlayers.add(mockOtherPlayer);

      game = new Game(mockGameRoom, testRoomId, initialPlayers);

      // Gems
      mockAvailableGems = new EnumMap<>(GemColor.class);
      for (GemColor color : GemColor.values()) {
        mockAvailableGems.put(color, 4L);
      }
      ReflectionTestUtils.setField(game, "availableGems", mockAvailableGems);

      // Player gems
      for (GemColor color : GemColor.values()) {
        when(mockPlayer.getGem(color)).thenReturn(0L);
      }
      when(mockPlayer.getUserId()).thenReturn(1L);
      when(mockOtherPlayer.getUserId()).thenReturn(2L);

      // Game status
      game.setGameState(Game.GameState.RUNNING);
      ReflectionTestUtils.setField(game, "currentPlayer", game.getPlayers().indexOf(mockPlayer));
    }

    @Test
    public void takeGems_Success_ThreeDifferentColors() {
      List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.BLUE, GemColor.GREEN);

      boolean result = game.takeGems(mockPlayer, colors);

      assertTrue(result);
      // Each color setGem should be called with 1
      for (GemColor color : colors) {
        verify(mockPlayer).setGem(eq(color), eq(1L));
        assertEquals(3L, mockAvailableGems.get(color), color + " available should decrease by 1");
      }
    }

    @Test
    public void takeGems_Success_TwoSameColor() {
      // Simulate taking two BLUE
      List<GemColor> colors = Collections.singletonList(GemColor.BLUE);

      boolean result = game.takeGems(mockPlayer, colors);

      assertTrue(result);
      verify(mockPlayer).setGem(eq(GemColor.BLUE), eq(2L));
      assertEquals(2L, mockAvailableGems.get(GemColor.BLUE));
    }

    @Test
    public void takeGems_Fail_NotPlayerTurn() {
      ReflectionTestUtils.setField(game, "currentPlayer", game.getPlayers().indexOf(mockOtherPlayer));
      List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.BLUE, GemColor.GREEN);

      boolean result = game.takeGems(mockPlayer, colors);

      assertFalse(result);
      verify(mockPlayer, never()).setGem(any(), anyLong());
    }

    @Test
    public void takeGems_Fail_GameNotRunning() {
      game.setGameState(Game.GameState.FINISHED);
      List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.BLUE, GemColor.GREEN);

      boolean result = game.takeGems(mockPlayer, colors);

      assertFalse(result);
      verify(mockPlayer, never()).setGem(any(), anyLong());
    }

    @Test
    public void takeGems_Fail_TakeGoldDirectly() {
      List<GemColor> colors = Arrays.asList(GemColor.GOLD, GemColor.BLUE, GemColor.RED);

      boolean result = game.takeGems(mockPlayer, colors);

      assertFalse(result);
      verify(mockPlayer, never()).setGem(any(), anyLong());
    }

    @Test
    public void takeGems_Fail_InsufficientGemsForThreeDifferent() {
      mockAvailableGems.put(GemColor.RED, 0L);
      List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.BLUE, GemColor.GREEN);

      boolean result = game.takeGems(mockPlayer, colors);

      assertFalse(result);
      verify(mockPlayer, never()).setGem(any(), anyLong());
    }

    @Test
    public void takeGems_Fail_InsufficientGemsForDouble() {
      mockAvailableGems.put(GemColor.BLUE, 2L);
      List<GemColor> colors = Collections.singletonList(GemColor.BLUE);

      boolean result = game.takeGems(mockPlayer, colors);

      assertFalse(result);
      verify(mockPlayer, never()).setGem(any(), anyLong());
    }

    @Test
    public void takeGems_Fail_InvalidColorListSize() {
      // Empty
      List<GemColor> colors = Collections.emptyList();
      assertFalse(game.takeGems(mockPlayer, colors));
      // Two colors, not allowed
      List<GemColor> twoColors = Arrays.asList(GemColor.RED, GemColor.BLUE);
      assertFalse(game.takeGems(mockPlayer, twoColors));
      verify(mockPlayer, never()).setGem(any(), anyLong());
    }

    @Test
    public void takeGems_Fail_RepeatedColorInThree() {
      List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.RED, GemColor.BLUE);
      assertFalse(game.takeGems(mockPlayer, colors));
      verify(mockPlayer, never()).setGem(any(), anyLong());
    }
}