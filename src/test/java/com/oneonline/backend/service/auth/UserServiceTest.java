package com.oneonline.backend.service.auth;

import com.oneonline.backend.exception.UserAlreadyExistsException;
import com.oneonline.backend.exception.UserNotFoundException;
import com.oneonline.backend.model.entity.User;
import com.oneonline.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 *
 * Tests user management operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNickname("testuser");
        testUser.setPasswordHash("hashedPassword");
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserByIdSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(UserNotFoundException.class);
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get user by email successfully")
    void shouldGetUserByEmailSuccessfully() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByEmail("test@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by email")
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com"))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should get user by nickname successfully")
    void shouldGetUserByNicknameSuccessfully() {
        // Given
        when(userRepository.findByNickname("testuser"))
            .thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByNickname("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNickname()).isEqualTo("testuser");
        verify(userRepository).findByNickname("testuser");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found by nickname")
    void shouldThrowExceptionWhenUserNotFoundByNickname() {
        // Given
        when(userRepository.findByNickname("nonexistent"))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserByNickname("nonexistent"))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should find user by email returning Optional")
    void shouldFindUserByEmailReturningOptional() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findUserByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return empty Optional when user not found by email")
    void shouldReturnEmptyOptionalWhenUserNotFoundByEmail() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com"))
            .thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findUserByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get all active users")
    void shouldGetAllActiveUsers() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@example.com");

        when(userRepository.findAllByIsActive(true))
            .thenReturn(Arrays.asList(testUser, user2));

        // When
        List<User> result = userService.getAllActiveUsers();

        // Then
        assertThat(result).hasSize(2);
        verify(userRepository).findAllByIsActive(true);
    }

    @Test
    @DisplayName("Should create new user successfully")
    void shouldCreateNewUserSuccessfully() {
        // Given
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setNickname("newuser");
        newUser.setPasswordHash("password");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByNickname("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = userService.createUser(newUser);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).existsByNickname("newuser");
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("Should throw exception when creating user with existing email")
    void shouldThrowExceptionWhenCreatingUserWithExistingEmail() {
        // Given
        User newUser = new User();
        newUser.setEmail("test@example.com");
        newUser.setNickname("newuser");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(newUser))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("Email already exists");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when creating user with existing nickname")
    void shouldThrowExceptionWhenCreatingUserWithExistingNickname() {
        // Given
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setNickname("testuser");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByNickname("testuser")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(newUser))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("Nickname already exists");
        verify(userRepository).existsByNickname("testuser");
        verify(userRepository, never()).save(any());
    }
}
