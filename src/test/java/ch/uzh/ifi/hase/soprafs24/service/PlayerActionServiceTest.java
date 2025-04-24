package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.websocket.Session;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlayerActionServiceTest {

    private PlayerActionService service;
    private Game game;
    private Player player;

    @BeforeEach
    public void setup() {
        service = new PlayerActionService();

        // Mock WebSocket session
        Session mockSession = mock(Session.class);
        player = new Player(mockSession, "TestPlayer", 1L);

        // Inject gems and bonusGems via reflection
        Map<GemColor, Long> gems = new HashMap<>();
        Map<GemColor, Long> bonusGems = new HashMap<>();
        for (GemColor color : GemColor.values()) {
            gems.put(color, 0L);
            bonusGems.put(color, 0L);
        }
        injectPrivateField(player, "gems", gems);
        injectPrivateField(player, "bonusGems", bonusGems);
        injectPrivateField(player, "victoryPoints", 0L);  // Prevent NPE in checkVictoryCondition

        Set<Player> players = new LinkedHashSet<>(); // maintain order
        players.add(player);

        // Spy the Game to mock endTurn
        Game realGame = new Game("test-game-id", players);
        Game spyGame = spy(realGame);
        doNothing().when(spyGame).endTurn();
        this.game = spyGame;

        // Set current player
        game.getPlayers().clear();
        game.getPlayers().add(player);

        // Initialize board gems
        Map<GemColor, Long> boardGems = game.getAvailableGems();
        for (GemColor color : GemColor.values()) {
            boardGems.put(color, 5L);
        }
    }

    private void injectPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }

    @Test
    public void testTakeTwoSameGems_Success() {
        service.takeTwoSameGems(game, GemColor.RED);
        assertEquals(2, player.getGem(GemColor.RED));
        assertEquals(3, game.getAvailableGems().get(GemColor.RED));
    }

    @Test
    public void testTakeTwoSameGems_Failure_NotEnoughGems() {
        game.getAvailableGems().put(GemColor.BLUE, 1L);
        assertThrows(IllegalArgumentException.class, () -> {
            service.takeTwoSameGems(game, GemColor.BLUE);
        });
    }

    @Test
    public void testTakeThreeDifferentGems_Success() {
        List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.GREEN, GemColor.WHITE);
        service.takeThreeDifferentGems(game, colors);
        assertEquals(1, player.getGem(GemColor.RED));
        assertEquals(1, player.getGem(GemColor.GREEN));
        assertEquals(1, player.getGem(GemColor.WHITE));
    }

    @Test
    public void testTakeThreeDifferentGems_InvalidColors() {
        List<GemColor> colors = Arrays.asList(GemColor.RED, GemColor.RED, GemColor.GREEN);
        assertThrows(IllegalArgumentException.class, () -> {
            service.takeThreeDifferentGems(game, colors);
        });
    }
}
