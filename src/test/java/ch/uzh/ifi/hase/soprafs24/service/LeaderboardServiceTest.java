package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.entity.LeaderboardEntry;
import ch.uzh.ifi.hase.soprafs24.repository.LeaderboardRepository;

public class LeaderboardServiceTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private final Long playerId = 1L;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addWinForPlayer_existingPlayer_incrementsWin() {
        LeaderboardEntry existingEntry = new LeaderboardEntry(playerId);
        existingEntry.incrementWins();

        when(leaderboardRepository.findById(playerId)).thenReturn(Optional.of(existingEntry));
        when(leaderboardRepository.save(any())).thenReturn(existingEntry);

        leaderboardService.addWinForPlayer(playerId);

        verify(leaderboardRepository, times(1)).save(existingEntry);
        assertEquals(2, existingEntry.getWins());
    }

    @Test
    public void addWinForPlayer_newPlayer_createsEntry() {
        when(leaderboardRepository.findById(playerId)).thenReturn(Optional.empty());
        ArgumentCaptor<LeaderboardEntry> captor = ArgumentCaptor.forClass(LeaderboardEntry.class);

        leaderboardService.addWinForPlayer(playerId);

        verify(leaderboardRepository, times(1)).save(captor.capture());
        assertEquals(playerId, captor.getValue().getPlayerId());
        assertEquals(1, captor.getValue().getWins());
    }

    @Test
    public void getLeaderboard_sortedByWinsDescending() {
        LeaderboardEntry player1 = new LeaderboardEntry(1L);
        player1.incrementWins();
        player1.incrementWins();
    
        LeaderboardEntry player2 = new LeaderboardEntry(2L);
        player2.incrementWins();
    
        when(leaderboardRepository.findAll()).thenReturn(List.of(player2, player1));
    
        List<LeaderboardEntry> leaderboard = leaderboardService.getLeaderboard();
    
        assertEquals(1L, leaderboard.get(0).getPlayerId()); 
        assertEquals(2, leaderboard.size());
    }
    
}
