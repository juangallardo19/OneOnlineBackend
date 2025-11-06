package com.oneonline.backend.repository;

import com.oneonline.backend.model.entity.PlayerStats;
import com.oneonline.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PlayerStats Repository - Database access for PlayerStats entity
 *
 * Provides CRUD operations and custom queries for player statistics.
 *
 * METHODS:
 * - findByUser(user) - Find stats for a specific user
 * - findByUserId(userId) - Find stats by user ID
 * - findTopByWinRate(limit) - Get top players by win rate
 * - findTopByTotalWins(limit) - Get top players by total wins
 *
 * Design Pattern: Repository Pattern (Spring Data JPA)
 *
 * @author Juan Gallardo
 */
@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

    /**
     * Find player stats by User entity
     *
     * Used for:
     * - Loading user statistics
     * - Profile display
     *
     * @param user User entity
     * @return Optional<PlayerStats> if found, empty if not found
     */
    Optional<PlayerStats> findByUser(User user);

    /**
     * Find player stats by user ID
     *
     * Used for:
     * - Loading stats without loading full User entity
     * - Efficient queries
     *
     * @param userId User ID
     * @return Optional<PlayerStats> if found, empty if not found
     */
    @Query("SELECT ps FROM PlayerStats ps WHERE ps.user.id = :userId")
    Optional<PlayerStats> findByUserId(@Param("userId") Long userId);

    /**
     * Get top players by win rate
     *
     * Used for:
     * - Leaderboard by win percentage
     * - Requires minimum games played (10+)
     *
     * @param minGames Minimum games played to qualify
     * @return List of top players ordered by win rate DESC
     */
    @Query("SELECT ps FROM PlayerStats ps WHERE ps.totalGames >= :minGames ORDER BY ps.winRate DESC")
    List<PlayerStats> findTopByWinRate(@Param("minGames") int minGames);

    /**
     * Get top players by total wins
     *
     * Used for:
     * - Leaderboard by total wins
     *
     * @return List of top players ordered by total wins DESC
     */
    @Query("SELECT ps FROM PlayerStats ps ORDER BY ps.totalWins DESC")
    List<PlayerStats> findTopByTotalWins();

    /**
     * Get top players by current streak
     *
     * Used for:
     * - "Hot Streak" leaderboard
     *
     * @return List of players ordered by current streak DESC
     */
    @Query("SELECT ps FROM PlayerStats ps WHERE ps.currentStreak > 0 ORDER BY ps.currentStreak DESC")
    List<PlayerStats> findTopByCurrentStreak();

    /**
     * Get top players by best streak
     *
     * Used for:
     * - "Best Streak" achievements
     *
     * @return List of players ordered by best streak DESC
     */
    @Query("SELECT ps FROM PlayerStats ps ORDER BY ps.bestStreak DESC")
    List<PlayerStats> findTopByBestStreak();

    /**
     * Get average win rate across all players
     *
     * Used for:
     * - Analytics
     * - Comparing player performance to average
     *
     * @return Average win rate (0-100)
     */
    @Query("SELECT AVG(ps.winRate) FROM PlayerStats ps WHERE ps.totalGames > 0")
    Double getAverageWinRate();

    /**
     * Get total number of games played (all players)
     *
     * Used for:
     * - Analytics dashboard
     *
     * @return Total games played across all players
     */
    @Query("SELECT SUM(ps.totalGames) FROM PlayerStats ps")
    Long getTotalGamesPlayed();

    /**
     * Find players with win rate above threshold
     *
     * Used for:
     * - Finding skilled players
     * - Tournament invitations
     *
     * @param minWinRate Minimum win rate (0-100)
     * @param minGames Minimum games played
     * @return List of qualifying players
     */
    @Query("SELECT ps FROM PlayerStats ps WHERE ps.winRate >= :minWinRate AND ps.totalGames >= :minGames ORDER BY ps.winRate DESC")
    List<PlayerStats> findPlayersWithHighWinRate(
        @Param("minWinRate") double minWinRate,
        @Param("minGames") int minGames
    );
}
