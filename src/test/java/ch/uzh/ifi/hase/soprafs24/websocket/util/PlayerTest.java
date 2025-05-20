package ch.uzh.ifi.hase.soprafs24.websocket.util;

import ch.uzh.ifi.hase.soprafs24.entity.GemColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class PlayerTest {

  @Mock
  private Session mockSession;

  @Mock
  private RemoteEndpoint.Basic mockBasicRemote;

  @Mock
  private Card mockCard1;

  @Mock
  private Card mockCard2;

  private Player player;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    player = new Player(null, "Alice", 42L);
  }

  @Test
  void testConstructorAndGetters() {
    assertEquals("Alice", player.getName());
    assertEquals(42L, player.getUserId());
    assertNull(player.getSession());
    assertFalse(player.getStatus());
  }

  @Test
  void testSetAndGetStatus() {
    player.setStatus(true);
    assertTrue(player.getStatus());
    player.setStatus(false);
    assertFalse(player.getStatus());
  }

  @Test
  void testSetAndGetUserId() {
    player.setUserId(100L);
    assertEquals(100L, player.getUserId());
  }

  @Test
  void testInitializeGameStatusAndGems() {
    player.initializeGameStatus();
    Map<GemColor, Long> gems = player.getAllGems();
    Map<GemColor, Long> bonus = player.getAllBonusGems();
    assertEquals(0L, gems.get(GemColor.RED));
    assertEquals(0L, gems.get(GemColor.BLACK));
    assertEquals(0L, bonus.get(GemColor.GREEN));
    assertEquals(0L, player.getVictoryPoints());
    assertTrue(player.getAllGems().values().stream().allMatch(x -> x == 0L));
    assertTrue(player.getAllBonusGems().values().stream().allMatch(x -> x == 0L));
  }

  @Test
  void testSetAndGetGem() {
    player.initializeGameStatus();
    player.setGem(GemColor.RED, 3L);
    assertEquals(3L, player.getGem(GemColor.RED));
    player.setGem(GemColor.RED, 0L);
    assertEquals(0L, player.getGem(GemColor.RED));
  }

  @Test
  void testSetGemNegativeThrows() {
    player.initializeGameStatus();
    assertThrows(IllegalArgumentException.class, () -> player.setGem(GemColor.RED, -1L));
  }

  @Test
  void testSetAndGetBonusGem() {
    player.initializeGameStatus();
    player.setBonusGem(GemColor.BLUE, 2L);
    assertEquals(2L, player.getBonusGem(GemColor.BLUE));
  }

  @Test
  void testSetBonusGemNegativeThrows() {
    player.initializeGameStatus();
    assertThrows(IllegalArgumentException.class, () -> player.setBonusGem(GemColor.BLUE, -2L));
  }

  @Test
  void testVictoryPoints() {
    player.setVictoryPoints(10L);
    assertEquals(10L, player.getVictoryPoints());
  }

  @Test
  void testReservedCardsAddAndRemove() {
    player.getReservedCards().add(mockCard1);
    assertTrue(player.getReservedCards().contains(mockCard1));
    assertFalse(player.getReservedCards().contains(mockCard2));

    assertTrue(player.removeCardFromReserved(mockCard1));
    assertFalse(player.getReservedCards().contains(mockCard1));
    assertFalse(player.removeCardFromReserved(mockCard2));
  }

  @Test
  void testEqualsAndHashCode() {
    Player player2 = new Player(null, "Bob", 42L);
    Player player3 = new Player(null, "Charlie", 43L);

    assertEquals(player, player2);
    assertEquals(player.hashCode(), player2.hashCode());
    assertNotEquals(player, player3);
  }

  @Test
  void testAvatar() {
    assertNull(player.getAvatar());
    player.setAvatar("cat.png");
    assertEquals("cat.png", player.getAvatar());
  }

  @Test
  void testSession() {
    player.setSession(mockSession);
    assertEquals(mockSession, player.getSession());
  }

  @Test
  void testSendMessage_string() throws Exception {
    player.setSession(mockSession);
    given(mockSession.isOpen()).willReturn(true);
    given(mockSession.getBasicRemote()).willReturn(mockBasicRemote);

    player.sendMessage("hello world");
    verify(mockBasicRemote, times(1)).sendText("hello world");
  }

  @Test
  void testSendMessage_object() throws Exception {
    player.setSession(mockSession);
    given(mockSession.isOpen()).willReturn(true);
    given(mockSession.getBasicRemote()).willReturn(mockBasicRemote);

    Object obj = new Object() {
        public String msg = "hi";
    };
    player.sendMessage(obj);
    verify(mockBasicRemote, times(1)).sendText(contains("\"msg\":\"hi\""));
  }

  @Test
  void testSendMessage_sessionClosed() {
    player.setSession(mockSession);
    given(mockSession.isOpen()).willReturn(false);

    // 不抛异常，也不发消息
    player.sendMessage("should not send");
    verify(mockSession, times(1)).isOpen();
    verify(mockSession, never()).getBasicRemote();
  }
}