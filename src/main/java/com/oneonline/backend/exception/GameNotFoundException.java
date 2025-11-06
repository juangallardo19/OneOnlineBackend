package com.oneonline.backend.exception;

/**
 * GameNotFoundException - Thrown when game session is not found
 *
 * HTTP Status: 404 NOT FOUND
 *
 * Used when:
 * - Game session ID doesn't exist
 * - Game was already ended
 * - Invalid session ID
 *
 * @author Juan Gallardo
 */
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String message) {
        super(message);
    }

    public GameNotFoundException(String sessionId) {
        super("Game session not found with ID: " + sessionId);
    }
}
