package ch.uzh.ifi.hase.soprafs24.websocket.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Noble;
import ch.uzh.ifi.hase.soprafs24.websocket.util.Player;

public class GameTest {

  @Mock
  private Player mockPlayer1;
  @Mock
  private Player mockPlayer2;
  @Mock
  private Player mockPlayer3;
  @Mock
  private Noble mockNoble;

  private List<Player> players;
  private Game game;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // default victoryPoints
    given(mockPlayer1.getVictoryPoints()).willReturn(0L);
    given(mockPlayer2.getVictoryPoints()).willReturn(0L);
    given(mockPlayer3.getVictoryPoints()).willReturn(0L);

    // mock players list
    players = new ArrayList<>(List.of(mockPlayer1, mockPlayer2, mockPlayer3));

    Set<Player> playerSet = new LinkedHashSet<>(players);
    game = spy(new Game(null, "test-nonActionLogic-room", playerSet));

    given(mockPlayer1.getUserId()).willReturn(111L);
    given(mockPlayer2.getUserId()).willReturn(222L);
    given(mockPlayer3.getUserId()).willReturn(333L);

    doNothing().when(game).fillVisibleAllVisibleCardsOnBoard();
    doNothing().when(game).noblePurchase(any());
  }

  @Test
  void checkVictoryCondition_playerMeetsVictory_setsFinalRound() {
    given(mockPlayer1.getVictoryPoints()).willReturn(5L);

    boolean result = game.checkVictoryCondition();

    assertTrue(result);
    assertTrue((Boolean) ReflectionTestUtils.getField(game, "finalRound"));
  }

  @Test
  void checkVictoryCondition_noPlayerMeetsVictory_finalRoundFalse() {
    given(mockPlayer1.getVictoryPoints()).willReturn(2L);
    given(mockPlayer2.getVictoryPoints()).willReturn(2L);

    boolean result = game.checkVictoryCondition();

    assertFalse(result);
    assertFalse((Boolean) ReflectionTestUtils.getField(game, "finalRound"));
  }

  @Test
  void noblePurchase_playerQualifies_getsNobleAndPoints() {
    // arrange
    given(mockNoble.getCost()).willReturn(Map.of(GemColor.RED, 3L));
    given(mockNoble.getPoints()).willReturn(3L);
    given(mockPlayer1.getBonusGem(GemColor.RED)).willReturn(3L);

    List<Noble> visibleNobles = new ArrayList<>(List.of(mockNoble));

    ReflectionTestUtils.setField(game, "players", players);
    ReflectionTestUtils.setField(game, "visibleNoble", visibleNobles);
    ReflectionTestUtils.setField(game, "currentPlayer", 0);

    // act
    reset(game); // clear doNothing and doReturn, call real function here
    game.noblePurchase(game);

    // assert
    verify(mockPlayer1, atLeastOnce()).setVictoryPoints(anyLong());
    assertFalse(visibleNobles.contains(mockNoble));
  }

  @Test
  void noblePurchase_playerNotQualifies_doesNothing() {
    // arrange
    given(mockNoble.getCost()).willReturn(Map.of(GemColor.RED, 3L));
    given(mockNoble.getPoints()).willReturn(3L);
    given(mockPlayer1.getBonusGem(GemColor.RED)).willReturn(2L);

    List<Noble> visibleNobles = new ArrayList<>(List.of(mockNoble));

    ReflectionTestUtils.setField(game, "players", players);
    ReflectionTestUtils.setField(game, "visibleNoble", visibleNobles);
    ReflectionTestUtils.setField(game, "currentPlayer", 0);

    // act
    reset(game);
    game.noblePurchase(game);

    // assert
    verify(mockPlayer1, never()).setVictoryPoints(anyLong());
    assertTrue(visibleNobles.contains(mockNoble));
  }

  @Test
  void endTurn_normalAdvance() {
    // arrange
    // all player has not reach the goal
    int currentPlayerIndex_beforeAct = game.getCurrentPlayer();
    int currentRound_beforeAct = game.getCurrentRound();
    game.setGameState(Game.GameState.RUNNING);

    // act
    game.endTurn();

    // assert
    assertEquals(Game.GameState.RUNNING, game.getGameState());
    assertEquals((currentPlayerIndex_beforeAct + 1) % players.size(), game.getCurrentPlayer()); // currentPlayer update normally
    assertEquals(currentRound_beforeAct + 1, game.getCurrentRound());
    verify(game, times(1)).noblePurchase(any());
    verify(game, times(1)).fillVisibleAllVisibleCardsOnBoard();

  }

  @Test
  void endTurn_finalRoundEndsGame() {
    // arrange
    given(mockPlayer1.getVictoryPoints()).willReturn(99L); // a big number bigger than victory point
    ReflectionTestUtils.setField(game, "currentPlayer", players.size()-1); // last player
    game.setGameState(Game.GameState.RUNNING);

    // act
    game.endTurn();

    assertEquals(Game.GameState.FINISHED, game.getGameState());
    assertEquals(players.size()-1, game.getCurrentPlayer()); // currentPlayer does not update when game ends
  }

  @Test
  void getWinnerId_singleWinner() {
    given(mockPlayer1.getVictoryPoints()).willReturn(game.VICTORYPOINTS);
    given(mockPlayer2.getVictoryPoints()).willReturn(0L);
    given(mockPlayer3.getVictoryPoints()).willReturn(0L);

    ReflectionTestUtils.setField(game, "players", players);

    Long winnerId = game.getWinnerId();
    assertEquals(mockPlayer1.getUserId(), winnerId);
  }

  @Test
  void getWinnerId_multipleTiedWinners_samePoints_returnsFirst() {
    given(mockPlayer1.getVictoryPoints()).willReturn(game.VICTORYPOINTS);
    given(mockPlayer2.getVictoryPoints()).willReturn(game.VICTORYPOINTS);
    given(mockPlayer3.getVictoryPoints()).willReturn(3L);

    ReflectionTestUtils.setField(game, "players", players);

    Long winnerId = game.getWinnerId();
    assertEquals(mockPlayer1.getUserId(), winnerId);
  }

  @Test
  void getWinnerId_allZeroPoints_returnsFirst() {
    given(mockPlayer1.getVictoryPoints()).willReturn(0L);
    given(mockPlayer2.getVictoryPoints()).willReturn(0L);
    given(mockPlayer3.getVictoryPoints()).willReturn(0L);

    ReflectionTestUtils.setField(game, "players", players);

    Long winnerId = game.getWinnerId();
    assertNull(winnerId);
  }

  @Test
  void getWinnerId_multipleTiedWinners_differentPoints_returnsHighest(){
    given(mockPlayer1.getVictoryPoints()).willReturn(0L);
    given(mockPlayer2.getVictoryPoints()).willReturn(game.VICTORYPOINTS + 1L);
    given(mockPlayer3.getVictoryPoints()).willReturn(game.VICTORYPOINTS);

    ReflectionTestUtils.setField(game, "players", players);

    Long winnerId = game.getWinnerId();
    assertEquals(mockPlayer2.getUserId(), winnerId);
  }

  @Test
  void getWinnerId_noPlayers_returnsNull() {
    ReflectionTestUtils.setField(game, "players", new ArrayList<>());

    Long winnerId = game.getWinnerId();
    assertNull(winnerId);
  }

}