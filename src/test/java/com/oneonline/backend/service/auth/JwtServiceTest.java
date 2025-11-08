package com.oneonline.backend.service.auth;

import com.oneonline.backend.model.entity.User;
import com.oneonline.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtService
 *
 * Tests JWT token generation, validation, and claim extraction.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Tests")
class JwtServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private String testToken;
    private Long testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testEmail = "test@example.com";
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail(testEmail);
    }

    @Test
    @DisplayName("Should generate access token for user")
    void shouldGenerateAccessTokenForUser() {
        // Given
        when(jwtTokenProvider.generateAccessToken(testUserId, testEmail))
            .thenReturn(testToken);

        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        assertThat(token).isEqualTo(testToken);
        verify(jwtTokenProvider).generateAccessToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should generate access token for user ID and email")
    void shouldGenerateAccessTokenForUserIdAndEmail() {
        // Given
        when(jwtTokenProvider.generateAccessToken(testUserId, testEmail))
            .thenReturn(testToken);

        // When
        String token = jwtService.generateAccessToken(testUserId, testEmail);

        // Then
        assertThat(token).isEqualTo(testToken);
        verify(jwtTokenProvider).generateAccessToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should generate refresh token for user")
    void shouldGenerateRefreshTokenForUser() {
        // Given
        String refreshToken = "refresh.token.here";
        when(jwtTokenProvider.generateRefreshToken(testUserId, testEmail))
            .thenReturn(refreshToken);

        // When
        String token = jwtService.generateRefreshToken(testUser);

        // Then
        assertThat(token).isEqualTo(refreshToken);
        verify(jwtTokenProvider).generateRefreshToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should generate refresh token for user ID and email")
    void shouldGenerateRefreshTokenForUserIdAndEmail() {
        // Given
        String refreshToken = "refresh.token.here";
        when(jwtTokenProvider.generateRefreshToken(testUserId, testEmail))
            .thenReturn(refreshToken);

        // When
        String token = jwtService.generateRefreshToken(testUserId, testEmail);

        // Then
        assertThat(token).isEqualTo(refreshToken);
        verify(jwtTokenProvider).generateRefreshToken(testUserId, testEmail);
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        // Given
        when(jwtTokenProvider.validateToken(testToken)).thenReturn(true);

        // When
        boolean isValid = jwtService.validateToken(testToken);

        // Then
        assertThat(isValid).isTrue();
        verify(jwtTokenProvider).validateToken(testToken);
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
        verify(jwtTokenProvider).validateToken(invalidToken);
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        // Given
        when(jwtTokenProvider.getEmailFromToken(testToken)).thenReturn(testEmail);

        // When
        String email = jwtService.getEmailFromToken(testToken);

        // Then
        assertThat(email).isEqualTo(testEmail);
        verify(jwtTokenProvider).getEmailFromToken(testToken);
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(testToken)).thenReturn(testUserId);

        // When
        Long userId = jwtService.getUserIdFromToken(testToken);

        // Then
        assertThat(userId).isEqualTo(testUserId);
        verify(jwtTokenProvider).getUserIdFromToken(testToken);
    }

    @Test
    @DisplayName("Should get expiration date from token")
    void shouldGetExpirationDateFromToken() {
        // Given
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtTokenProvider.getExpirationDate(testToken)).thenReturn(expirationDate);

        // When
        Date date = jwtService.getExpirationDate(testToken);

        // Then
        assertThat(date).isEqualTo(expirationDate);
        verify(jwtTokenProvider).getExpirationDate(testToken);
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // Given - token expired 1 hour ago
        Date pastDate = new Date(System.currentTimeMillis() - 3600000);
        when(jwtTokenProvider.getExpirationDate(testToken)).thenReturn(pastDate);

        // When
        boolean isExpired = jwtService.isTokenExpired(testToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void shouldDetectNonExpiredToken() {
        // Given - token expires in 1 hour
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtTokenProvider.getExpirationDate(testToken)).thenReturn(futureDate);

        // When
        boolean isExpired = jwtService.isTokenExpired(testToken);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should get access token expiration time")
    void shouldGetAccessTokenExpirationTime() {
        // Given
        Long expirationMs = 86400000L; // 24 hours
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(expirationMs);

        // When
        Long expiration = jwtService.getAccessTokenExpirationMs();

        // Then
        assertThat(expiration).isEqualTo(expirationMs);
        verify(jwtTokenProvider).getAccessTokenExpirationMs();
    }

    @Test
    @DisplayName("Should get refresh token expiration time")
    void shouldGetRefreshTokenExpirationTime() {
        // Given
        Long expirationMs = 604800000L; // 7 days
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(expirationMs);

        // When
        Long expiration = jwtService.getRefreshTokenExpirationMs();

        // Then
        assertThat(expiration).isEqualTo(expirationMs);
        verify(jwtTokenProvider).getRefreshTokenExpirationMs();
    }
}
