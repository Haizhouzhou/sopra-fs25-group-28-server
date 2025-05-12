package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "LEADERBOARD_ENTRY")
public class LeaderboardEntry {

    @Id
    private Long playerId;

    @Column(nullable = false)
    private int wins = 0;

    public LeaderboardEntry() {}

    public LeaderboardEntry(Long playerId) {
        this.playerId = playerId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public int getWins() {
        return wins;
    }

    public void incrementWins() {
        this.wins++;
    }
}
