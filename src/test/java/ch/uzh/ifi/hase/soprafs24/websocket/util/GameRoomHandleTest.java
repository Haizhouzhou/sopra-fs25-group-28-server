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
    // Initialize mocks created with annotations (@Mock, @InjectMocks)
    MockitoAnnotations.openMocks(this);

    // Create a new GameRoom instance for each test
    gameRoom = new GameRoom(testRoomId, testMaxPlayers);
    gameRoom.setRoomName(testRoomName); // Set a name for testing

    gameRoom.setGameInstance(mockGame);


    // --- Manually add players to the private 'players' set ---
    // The 'players' set in GameRoom is private, so use ReflectionTestUtils
    // Ensure the type matches the actual implementation (e.g., ConcurrentHashMap.newKeySet())
    Set<Player> players = ConcurrentHashMap.newKeySet();
    players.add(mockPlayer1);
    players.add(mockPlayer2);
    ReflectionTestUtils.setField(gameRoom, "players", players);

    // Basic player setup
    // given(mockPlayer1.getUserId()).willReturn(1L);
    // given(mockPlayer2.getUserId()).willReturn(2L);

  }

  // ======================================
  // Tests for handleBuyCard
  // ======================================

  @Test
  void handleBuyCard_Success_CallsGameBuyCardAndReturnsTrue() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(true); // Simulate successful buy

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertTrue(result, "handleBuyCard should return true on success");
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameLogicFails_CallsGameBuyCardAndReturnsFalse() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(false); // Simulate failed buy

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // Assert
    assertFalse(result, "handleBuyCard should return false when game logic fails");
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameIsNull_ReturnsFalse() {
    // Arrange
    // InjectMocks provides mockGame, but we can override it for this test
    ReflectionTestUtils.setField(gameRoom, "game", null); // Ensure game is null

    // Act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // Assert
    assertFalse(result, "handleBuyCard should return false when game instance is null");
    // No need to verify mockGame interactions as it's null
  }

  // ======================================
  // Tests for handleReserveCard
  // ======================================

  @Test
  void handleReserveCard_Success_CallsGameReserveCardAndReturnsTrue() {
    // Arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(true); // Simulate successful reserve

    // Act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // Assert
    assertTrue(result, "handleReserveCard should return true on success");
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong); // Verify game.reserveCard was called
    // We infer broadcasting happens because result is true
  }

  @Test
  void handleReserveCard_GameLogicFails_CallsGameReserveCardAndReturnsFalse() {
    // Arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(false); // Simulate failed reserve

    // Act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // Assert
    assertFalse(result, "handleReserveCard should return false when game logic fails");
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong); // Verify game.reserveCard was still called
    // We infer broadcasting does NOT happen because result is false
  }

  @Test
  void handleReserveCard_GameIsNull_ReturnsFalse() {
    // Arrange
    ReflectionTestUtils.setField(gameRoom, "game", null); // Ensure game is null

    // Act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // Assert
    assertFalse(result, "handleReserveCard should return false when game instance is null");
  }
}
