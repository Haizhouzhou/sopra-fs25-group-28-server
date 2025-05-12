package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.entity.LeaderboardEntry;

@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
}
