package ch.uzh.ifi.hase.soprafs24.websocket.util;

import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot; // Assuming GameSnapshot exists
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class GameRoomHandleTest {
  
  // no default constructor, need to be initialize manually
  private GameRoom gameRoom;

  // Mocks for dependencies (Players)
  @Mock
  private Player mockPlayer1;

  @Mock
  private Player mockPlayer2;

  @Mock
  private Game mockGame;

  @Mock
  private GameSnapshot mockGameSnapshot;

  // Test constants
  private final String testRoomId = "handler-room";
  private final int testMaxPlayers = 4;
  private final String testRoomName = "TestRoom";

  private final String validCardIdStr = "123";
  private final Long validCardIdLong = 123L;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    gameRoom = new GameRoom(testRoomId, testMaxPlayers);
    gameRoom.setRoomName(testRoomName);

    gameRoom.setGameInstance(mockGame);

    Set<Player> players = ConcurrentHashMap.newKeySet();
    players.add(mockPlayer1);
    players.add(mockPlayer2);
    ReflectionTestUtils.setField(gameRoom, "players", players);

  }

  @Test
  void handleBuyCard_Success_CallsGameBuyCardAndReturnsTrue() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(true); // mock successful buy

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertTrue(result, "handleBuyCard should return true on success");
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameLogicFails_CallsGameBuyCardAndReturnsFalse() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(false); // mock failed buy

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result, "handleBuyCard should return false when game logic fails");
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameIsNull_ReturnsFalse() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null); 

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result, "handleBuyCard should return false when game instance is null");
  }

  @Test
  void handleReserveCard_Success_CallsGameReserveCardAndReturnsTrue() {
    // arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(true);

    // act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertTrue(result, "handleReserveCard should return true on success");
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleReserveCard_GameLogicFails_CallsGameReserveCardAndReturnsFalse() {
    // arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(false);

    // act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result, "handleReserveCard should return false when game logic fails");
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleReserveCard_GameIsNull_ReturnsFalse() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null);

    // act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result, "handleReserveCard should return false when game instance is null");
  }
}
