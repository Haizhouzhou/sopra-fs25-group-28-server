package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.LeaderboardEntry;
import ch.uzh.ifi.hase.soprafs24.repository.LeaderboardRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardService(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    public void addWinForPlayer(Long playerId) {
        LeaderboardEntry entry = leaderboardRepository.findById(playerId).orElse(new LeaderboardEntry(playerId));
        entry.incrementWins();
        leaderboardRepository.save(entry);
    }

    public List<LeaderboardEntry> getLeaderboard() {
        List<LeaderboardEntry> allEntries = leaderboardRepository.findAll();
        return allEntries.stream()
                .sorted((a, b) -> Integer.compare(b.getWins(), a.getWins()))
                .collect(Collectors.toList());
    }
}
