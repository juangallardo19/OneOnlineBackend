package com.oneonline.backend.service.game;

import com.oneonline.backend.model.domain.Card;
import com.oneonline.backend.model.domain.GameSession;
import com.oneonline.backend.model.domain.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * UnoManager Service
 *
 * Manages "UNO" calls when players have one card remaining.
 *
 * ONE RULES:
 * - When player has exactly 1 card, must call "UNO"
 * - If not called and caught by another player, penalized with +2 cards
 * - Must call UNO before next player starts their turn
 *
 * RESPONSIBILITIES:
 * - Track UNO calls
 * - Validate UNO eligibility
 * - Apply penalties for missed UNO calls
 * - Detect when UNO should be called
 *
 * @author Juan Gallardo
 */
@Slf4j
@Service
public class UnoManager {

    /**
     * Penalty for not calling UNO
     */
    private static final int NO_UNO_PENALTY = 2;

    /**
     * Time window to call UNO (milliseconds)
     * Players have this much time to call UNO after playing their second-to-last card
     */
    private static final long UNO_CALL_WINDOW_MS = 3000; // 3 seconds

    /**
     * Tracks when players reached 1 card (for penalty window)
     * Key: Player ID, Value: Timestamp when they reached 1 card
     */
    private final Map<String, LocalDateTime> oneCardTimestamps = new HashMap<>();

    /**
     * Call UNO for a player
     *
     * Validates:
     * - Player has exactly 1 card
     * - UNO not already called
     *
     * @param player Player calling UNO
     * @param session Game session
     * @return true if UNO call successful, false if invalid
     */
    public boolean callUno(Player player, GameSession session) {
        // Check if player has exactly 1 card
        if (!hasOneCard(player)) {
            log.warn("Player {} attempted to call UNO with {} cards",
                player.getNickname(), player.getHandSize());
            return false;
        }

        // Check if already called
        if (player.hasCalledUno()) {
            log.warn("Player {} already called UNO", player.getNickname());
            return false;
        }

        // Mark UNO as called
        player.callUno();

        // Remove from penalty tracking
        oneCardTimestamps.remove(player.getPlayerId());

        log.info("Player {} called UNO! (1 card remaining)", player.getNickname());
        return true;
    }

    /**
     * Check if player should call UNO
     *
     * Called after player plays a card.
     * If player now has 1 card, start penalty timer.
     *
     * @param player Player to check
     * @return true if player has 1 card and should call UNO
     */
    public boolean checkUnoCall(Player player) {
        if (hasOneCard(player)) {
            // Start penalty timer if not already started
            oneCardTimestamps.putIfAbsent(player.getPlayerId(), LocalDateTime.now());
            return true;
        } else {
            // Player has 0 or 2+ cards, reset UNO status
            player.resetUnoCall();
            oneCardTimestamps.remove(player.getPlayerId());
            return false;
        }
    }

    /**
     * Penalize player for not calling UNO
     *
     * Called when:
     * - Another player catches them
     * - Time window expires
     *
     * @param player Player to penalize
     * @param session Game session
     * @return Number of cards drawn as penalty
     */
    public int penalizeNoUno(Player player, GameSession session) {
        // Check if player has 1 card and hasn't called UNO
        if (!hasOneCard(player)) {
            log.warn("Cannot penalize {} - doesn't have 1 card", player.getNickname());
            return 0;
        }

        if (player.hasCalledUno()) {
            log.warn("Cannot penalize {} - UNO already called", player.getNickname());
            return 0;
        }

        // Draw penalty cards
        for (int i = 0; i < NO_UNO_PENALTY; i++) {
            Card card = session.getDeck().drawCard();
            if (card != null) {
                player.drawCard(card);
            } else {
                // Deck empty, refill from discard pile
                session.getDeck().refillFromDiscard(session.getDiscardPile());
                card = session.getDeck().drawCard();
                if (card != null) {
                    player.drawCard(card);
                }
            }
        }

        // Reset UNO call status
        player.resetUnoCall();
        oneCardTimestamps.remove(player.getPlayerId());

        log.info("Player {} penalized for not calling UNO: +{} cards",
            player.getNickname(), NO_UNO_PENALTY);

        return NO_UNO_PENALTY;
    }

    /**
     * Check if player has exactly one card
     *
     * @param player Player to check
     * @return true if exactly 1 card
     */
    public boolean hasOneCard(Player player) {
        return player.getHandSize() == 1;
    }

    /**
     * Check if player is eligible to call UNO
     *
     * @param player Player to check
     * @return true if has 1 card and hasn't called yet
     */
    public boolean canCallUno(Player player) {
        return hasOneCard(player) && !player.hasCalledUno();
    }

    /**
     * Check if UNO call window has expired
     *
     * Players have limited time to call UNO after reaching 1 card.
     *
     * @param playerId Player ID
     * @return true if window expired
     */
    public boolean hasUnoWindowExpired(String playerId) {
        LocalDateTime timestamp = oneCardTimestamps.get(playerId);
        if (timestamp == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedMs = java.time.Duration.between(timestamp, now).toMillis();

        return elapsedMs > UNO_CALL_WINDOW_MS;
    }

    /**
     * Auto-check and penalize expired UNO windows
     *
     * Called at end of each turn to check if any player
     * missed their UNO call window.
     *
     * @param session Game session
     * @return Number of players penalized
     */
    public int checkAndPenalizeExpiredWindows(GameSession session) {
        int penalized = 0;

        for (Player player : session.getPlayers()) {
            if (hasOneCard(player) &&
                !player.hasCalledUno() &&
                hasUnoWindowExpired(player.getPlayerId())) {

                penalizeNoUno(player, session);
                penalized++;
            }
        }

        return penalized;
    }

    /**
     * Catch another player who didn't call UNO
     *
     * Any player can call out another player who has 1 card
     * but didn't call UNO.
     *
     * @param caughtPlayer Player who was caught
     * @param catchingPlayer Player who caught them
     * @param session Game session
     * @return true if catch was valid and penalty applied
     */
    public boolean catchPlayerWithoutUno(Player caughtPlayer, Player catchingPlayer, GameSession session) {
        // Validate catch
        if (!hasOneCard(caughtPlayer)) {
            log.warn("{} attempted to catch {} but they don't have 1 card",
                catchingPlayer.getNickname(), caughtPlayer.getNickname());
            return false;
        }

        if (caughtPlayer.hasCalledUno()) {
            log.warn("{} attempted to catch {} but UNO was already called",
                catchingPlayer.getNickname(), caughtPlayer.getNickname());
            return false;
        }

        // Apply penalty
        penalizeNoUno(caughtPlayer, session);

        log.info("{} caught {} without UNO call!",
            catchingPlayer.getNickname(), caughtPlayer.getNickname());

        return true;
    }

    /**
     * Reset UNO status for player
     *
     * Called when player draws/plays cards and no longer has 1 card.
     *
     * @param player Player to reset
     */
    public void resetUnoStatus(Player player) {
        player.resetUnoCall();
        oneCardTimestamps.remove(player.getPlayerId());
    }

    /**
     * Clear all UNO tracking data
     *
     * Called at end of game or round.
     */
    public void clearAll() {
        oneCardTimestamps.clear();
        log.debug("UNO tracking data cleared");
    }

    /**
     * Get number of players who need to call UNO
     *
     * @param session Game session
     * @return Count of players with 1 card who haven't called UNO
     */
    public int getPlayersNeedingUnoCall(GameSession session) {
        return (int) session.getPlayers().stream()
            .filter(this::hasOneCard)
            .filter(p -> !p.hasCalledUno())
            .count();
    }
}
