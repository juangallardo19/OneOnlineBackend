package com.oneonline.backend.repository;

import com.oneonline.backend.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserRepository
 *
 * Tests database operations for User entity.
 * Uses H2 in-memory database for testing.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("User Repository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = new User();
        testUser1.setEmail("test1@example.com");
        testUser1.setNickname("testuser1");
        testUser1.setPasswordHash("$2a$10$hashedPassword");
        testUser1.setAuthProvider(User.AuthProvider.LOCAL);
        testUser1.setIsActive(true);
        testUser1.setCreatedAt(LocalDateTime.now());

        testUser2 = new User();
        testUser2.setEmail("test2@example.com");
        testUser2.setNickname("testuser2");
        testUser2.setOauth2Id("google-123456");
        testUser2.setAuthProvider(User.AuthProvider.GOOGLE);
        testUser2.setIsActive(true);
        testUser2.setCreatedAt(LocalDateTime.now());

        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // When
        Optional<User> found = userRepository.findByEmail("test1@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test1@example.com");
        assertThat(found.get().getNickname()).isEqualTo("testuser1");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by nickname")
    void shouldFindUserByNickname() {
        // When
        Optional<User> found = userRepository.findByNickname("testuser1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("testuser1");
    }

    @Test
    @DisplayName("Should return empty when nickname not found")
    void shouldReturnEmptyWhenNicknameNotFound() {
        // When
        Optional<User> found = userRepository.findByNickname("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by OAuth2 ID")
    void shouldFindUserByOauth2Id() {
        // When
        Optional<User> found = userRepository.findByOauth2Id("google-123456");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOauth2Id()).isEqualTo("google-123456");
        assertThat(found.get().getAuthProvider()).isEqualTo(User.AuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("Should find user by email and auth provider")
    void shouldFindUserByEmailAndAuthProvider() {
        // When
        Optional<User> found = userRepository.findByEmailAndAuthProvider(
            "test1@example.com", User.AuthProvider.LOCAL
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test1@example.com");
        assertThat(found.get().getAuthProvider()).isEqualTo(User.AuthProvider.LOCAL);
    }

    @Test
    @DisplayName("Should find user by auth provider and provider ID")
    void shouldFindUserByAuthProviderAndProviderId() {
        // When
        Optional<User> found = userRepository.findByAuthProviderAndProviderId(
            User.AuthProvider.GOOGLE, "google-123456"
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAuthProvider()).isEqualTo(User.AuthProvider.GOOGLE);
        assertThat(found.get().getOauth2Id()).isEqualTo("google-123456");
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // When
        boolean exists = userRepository.existsByEmail("test1@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should check if nickname exists")
    void shouldCheckIfNicknameExists() {
        // When
        boolean exists = userRepository.existsByNickname("testuser1");
        boolean notExists = userRepository.existsByNickname("nonexistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find all active users")
    void shouldFindAllActiveUsers() {
        // When
        List<User> activeUsers = userRepository.findAllByIsActive(true);

        // Then
        assertThat(activeUsers).hasSize(2);
    }

    @Test
    @DisplayName("Should find no inactive users")
    void shouldFindNoInactiveUsers() {
        // When
        List<User> inactiveUsers = userRepository.findAllByIsActive(false);

        // Then
        assertThat(inactiveUsers).isEmpty();
    }

    @Test
    @DisplayName("Should count users by auth provider")
    void shouldCountUsersByAuthProvider() {
        // When
        long localCount = userRepository.countByAuthProvider(User.AuthProvider.LOCAL);
        long googleCount = userRepository.countByAuthProvider(User.AuthProvider.GOOGLE);
        long githubCount = userRepository.countByAuthProvider(User.AuthProvider.GITHUB);

        // Then
        assertThat(localCount).isEqualTo(1);
        assertThat(googleCount).isEqualTo(1);
        assertThat(githubCount).isZero();
    }

    @Test
    @DisplayName("Should save new user")
    void shouldSaveNewUser() {
        // Given
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setNickname("newuser");
        newUser.setPasswordHash("$2a$10$newHashedPassword");
        newUser.setAuthProvider(User.AuthProvider.LOCAL);
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());

        // When
        User saved = userRepository.save(newUser);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Should update existing user")
    void shouldUpdateExistingUser() {
        // Given
        User user = userRepository.findByEmail("test1@example.com").get();
        user.setNickname("updatedNickname");

        // When
        User updated = userRepository.save(user);

        // Then
        assertThat(updated.getNickname()).isEqualTo("updatedNickname");
        assertThat(updated.getEmail()).isEqualTo("test1@example.com");
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Given
        User user = userRepository.findByEmail("test1@example.com").get();

        // When
        userRepository.delete(user);
        Optional<User> deleted = userRepository.findByEmail("test1@example.com");

        // Then
        assertThat(deleted).isEmpty();
    }
}
