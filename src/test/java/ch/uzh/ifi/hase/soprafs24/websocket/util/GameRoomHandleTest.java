package ch.uzh.ifi.hase.soprafs24.websocket.util;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameSnapshot; // Assuming GameSnapshot exists
import ch.uzh.ifi.hase.soprafs24.websocket.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

  // Test data
  private Map<GemColor, Long> mockGems;

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

    mockGems = new EnumMap<>(GemColor.class);
    for (GemColor color : GemColor.values()) {
      mockGems.put(color, 4L);
    }
    given(mockGame.getAvailableGems()).willReturn(mockGems);
  }

  @Test
  void handleBuyCard_Success_CallsGameBuyCardAndReturnsTrue() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(true); // mock successful buy

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertTrue(result);
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameLogicFails_CallsGameBuyCardAndReturnsFalse() {
    // arrange
    given(mockGame.buyCard(mockPlayer1, validCardIdLong)).willReturn(false); // mock failed buy

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
    verify(mockGame, times(1)).buyCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleBuyCard_GameIsNull_ReturnsFalse() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null); 

    // act
    boolean result = gameRoom.handleBuyCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
  }

  @Test
  void handleReserveCard_Success_CallsGameReserveCardAndReturnsTrue() {
    // arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(true);

    // act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertTrue(result);
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleReserveCard_GameLogicFails_CallsGameReserveCardAndReturnsFalse() {
    // arrange
    given(mockGame.reserveCard(mockPlayer1, validCardIdLong)).willReturn(false);

    // act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
    verify(mockGame, times(1)).reserveCard(mockPlayer1, validCardIdLong);
  }

  @Test
  void handleReserveCard_GameIsNull_ReturnsFalse() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null);

    // act
    boolean result = gameRoom.handleReserveCard(mockPlayer1, validCardIdStr);

    // assert
    assertFalse(result);
  }

  @Test
  void handleTakeThreeGems_Success(){
    // arrange
    List<String> colors = Arrays.asList("red", "blue", "green");
    given(mockGame.takeGems(eq(mockPlayer1), anyList())).willReturn(true);

    // act
    boolean result = gameRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertTrue(result);
    verify(mockGame).takeGems(eq(mockPlayer1), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_DuplicateColors() {
    // arrange
    List<String> colors = Arrays.asList("red", "red", "blue");

    // act
    boolean result = gameRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
  }

  @Test
  void handleTakeThreeGems_Fail_ColorsIsNull() {
    // arrange
    
    // act
    boolean result = gameRoom.handleTakeThreeGems(mockPlayer1, null);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_MoreThanThreeColor() {
    // arrange
    List<String> colors = Arrays.asList("green", "red", "blue", "black");

    // act
    boolean result = gameRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_InvalidColor() {
    // arrange
    List<String> colors = Arrays.asList("incorrect_color", "red", "blue");

    // act
    boolean result = gameRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeThreeGems_Fail_GameIsNull() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null);
    List<String> colors = Arrays.asList("red", "blue", "green");

    // act
    boolean result = gameRoom.handleTakeThreeGems(mockPlayer1, colors);

    // assert
    assertFalse(result);
  }

  @Test
  void handleTakeDoubleGem_Success() {
    // arrange
    String colorStr = "blue";
    GemColor color = GemColor.BLUE;
    given(mockGame.takeGems(eq(mockPlayer1), eq(List.of(color)))).willReturn(true);

    // act
    boolean result = gameRoom.handleTakeDoubleGem(mockPlayer1, colorStr);

    // assert
    assertTrue(result);
    verify(mockGame).takeGems(eq(mockPlayer1), eq(List.of(color)));
  }

  @Test
  void handleTakeDoubleGem_Fail_GameIsNull() {
    // arrange
    ReflectionTestUtils.setField(gameRoom, "game", null);

    // act
    boolean result = gameRoom.handleTakeDoubleGem(mockPlayer1, "blue");

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeDoubleGem_Fail_InvalidColor() {
    // arrange
    String colorStr = "incorrect_color";

    // act
    boolean result = gameRoom.handleTakeDoubleGem(mockPlayer1, colorStr);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

  @Test
  void handleTakeDoubleGem_Fail_ColorIsNull() {
    // arrange
    String colorStr = null;

    // act
    boolean result = gameRoom.handleTakeDoubleGem(mockPlayer1, colorStr);

    // assert
    assertFalse(result);
    verify(mockGame, never()).takeGems(any(), anyList());
  }

}
