package com.oneonline.backend.service.ranking;

import com.oneonline.backend.model.entity.GameHistory;
import com.oneonline.backend.model.entity.GlobalRanking;
import com.oneonline.backend.model.entity.PlayerStats;
import com.oneonline.backend.model.entity.User;
import com.oneonline.backend.repository.GameHistoryRepository;
import com.oneonline.backend.repository.GlobalRankingRepository;
import com.oneonline.backend.repository.PlayerStatsRepository;
import com.oneonline.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RankingService
 *
 * Tests ranking and leaderboard operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ranking Service Tests")
class RankingServiceTest {

    @Mock
    private GlobalRankingRepository globalRankingRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private GameHistoryRepository gameHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RankingService rankingService;

    private GlobalRanking ranking1;
    private GlobalRanking ranking2;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // Setup test users
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("player1@example.com");
        user1.setNickname("player1");

        user2 = new User();
        user2.setId(2L);
        user2.setEmail("player2@example.com");
        user2.setNickname("player2");

        // Setup test rankings
        ranking1 = new GlobalRanking();
        ranking1.setId(1L);
        ranking1.setUser(user1);
        ranking1.setRankPosition(1);
        ranking1.setPoints(100);
        ranking1.setTotalWins(10);
        ranking1.setTotalGames(12);
        ranking1.setWinRate(83.33);

        ranking2 = new GlobalRanking();
        ranking2.setId(2L);
        ranking2.setUser(user2);
        ranking2.setRankPosition(2);
        ranking2.setPoints(80);
        ranking2.setTotalWins(8);
        ranking2.setTotalGames(10);
        ranking2.setWinRate(80.0);
    }

    @Test
    @DisplayName("Should get global top 100 rankings")
    void shouldGetGlobalTop100Rankings() {
        // Given
        List<GlobalRanking> top100 = Arrays.asList(ranking1, ranking2);
        when(globalRankingRepository.findTop100ByOrderByPointsDesc())
            .thenReturn(top100);

        // When
        List<GlobalRanking> result = rankingService.getGlobalTop100();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRankPosition()).isEqualTo(1);
        assertThat(result.get(0).getPoints()).isEqualTo(100);
        assertThat(result.get(1).getRankPosition()).isEqualTo(2);
        assertThat(result.get(1).getPoints()).isEqualTo(80);
        verify(globalRankingRepository).findTop100ByOrderByPointsDesc();
    }

    @Test
    @DisplayName("Should get empty list when no rankings exist")
    void shouldGetEmptyListWhenNoRankingsExist() {
        // Given
        when(globalRankingRepository.findTop100ByOrderByPointsDesc())
            .thenReturn(Arrays.asList());

        // When
        List<GlobalRanking> result = rankingService.getGlobalTop100();

        // Then
        assertThat(result).isEmpty();
        verify(globalRankingRepository).findTop100ByOrderByPointsDesc();
    }

    @Test
    @DisplayName("Should get room top winners")
    void shouldGetRoomTopWinners() {
        // Given
        String roomCode = "ABC123";
        GameHistory game1 = new GameHistory();
        game1.setRoomCode(roomCode);
        game1.setWinner(user1);

        GameHistory game2 = new GameHistory();
        game2.setRoomCode(roomCode);
        game2.setWinner(user1);

        GameHistory game3 = new GameHistory();
        game3.setRoomCode(roomCode);
        game3.setWinner(user2);

        when(gameHistoryRepository.findByRoomCode(roomCode))
            .thenReturn(Arrays.asList(game1, game2, game3));

        // When
        Map<User, Integer> result = rankingService.getRoomTopWinners(roomCode);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(user1)).isEqualTo(2);
        assertThat(result.get(user2)).isEqualTo(1);
        verify(gameHistoryRepository).findByRoomCode(roomCode);
    }

    @Test
    @DisplayName("Should return empty map when no games in room")
    void shouldReturnEmptyMapWhenNoGamesInRoom() {
        // Given
        String roomCode = "EMPTY1";
        when(gameHistoryRepository.findByRoomCode(roomCode))
            .thenReturn(Arrays.asList());

        // When
        Map<User, Integer> result = rankingService.getRoomTopWinners(roomCode);

        // Then
        assertThat(result).isEmpty();
        verify(gameHistoryRepository).findByRoomCode(roomCode);
    }

    @Test
    @DisplayName("Should handle games with null winners")
    void shouldHandleGamesWithNullWinners() {
        // Given
        String roomCode = "TEST01";
        GameHistory game1 = new GameHistory();
        game1.setRoomCode(roomCode);
        game1.setWinner(null);  // No winner (game abandoned?)

        GameHistory game2 = new GameHistory();
        game2.setRoomCode(roomCode);
        game2.setWinner(user1);

        when(gameHistoryRepository.findByRoomCode(roomCode))
            .thenReturn(Arrays.asList(game1, game2));

        // When
        Map<User, Integer> result = rankingService.getRoomTopWinners(roomCode);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(user1)).isEqualTo(1);
    }
}
