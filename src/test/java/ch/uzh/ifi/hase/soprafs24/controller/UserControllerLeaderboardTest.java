package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.LeaderboardEntry;
import ch.uzh.ifi.hase.soprafs24.service.LeaderboardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserControllerLeaderboardTest {

    @Mock
    private UserService userService;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getLeaderboard_returnsLeaderboardSuccessfully() {
        LeaderboardEntry player1 = new LeaderboardEntry(1L);
        player1.incrementWins();

        when(leaderboardService.getLeaderboard()).thenReturn(List.of(player1));

        List<LeaderboardEntry> result = userController.getLeaderboard();

        verify(leaderboardService, times(1)).getLeaderboard();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPlayerId());
    }
}
