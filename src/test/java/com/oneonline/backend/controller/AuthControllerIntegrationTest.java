package com.oneonline.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneonline.backend.dto.request.LoginRequest;
import com.oneonline.backend.dto.request.RegisterRequest;
import com.oneonline.backend.model.entity.User;
import com.oneonline.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 *
 * Tests authentication endpoints with real Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setNickname("newuser");
        request.setPassword("SecurePass123!");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.nickname").value("newuser"))
            .andExpect(jsonPath("$.userId").exists())
            .andExpect(jsonPath("$.expiresAt").exists())
            .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("Should reject registration with existing email")
    void shouldRejectRegistrationWithExistingEmail() throws Exception {
        // Given - existing user
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setNickname("existinguser");
        existingUser.setPasswordHash(passwordEncoder.encode("password"));
        existingUser.setAuthProvider(User.AuthProvider.LOCAL);
        existingUser.setIsActive(true);
        existingUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(existingUser);

        // New registration with same email
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setNickname("differentnickname");
        request.setPassword("SecurePass123!");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should reject registration with existing nickname")
    void shouldRejectRegistrationWithExistingNickname() throws Exception {
        // Given - existing user
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setNickname("existingnick");
        existingUser.setPasswordHash(passwordEncoder.encode("password"));
        existingUser.setAuthProvider(User.AuthProvider.LOCAL);
        existingUser.setIsActive(true);
        existingUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(existingUser);

        // New registration with same nickname
        RegisterRequest request = new RegisterRequest();
        request.setEmail("different@example.com");
        request.setNickname("existingnick");
        request.setPassword("SecurePass123!");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should reject registration with invalid email")
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setNickname("validnickname");
        request.setPassword("SecurePass123!");

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        // Given - create user first
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setNickname("testuser");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setAuthProvider(User.AuthProvider.LOCAL);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("testuser@example.com");
        request.setPassword("password123");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.email").value("testuser@example.com"))
            .andExpect(jsonPath("$.nickname").value("testuser"))
            .andExpect(jsonPath("$.userId").exists())
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectLoginWithInvalidPassword() throws Exception {
        // Given - create user
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setNickname("testuser");
        user.setPasswordHash(passwordEncoder.encode("correctpassword"));
        user.setAuthProvider(User.AuthProvider.LOCAL);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("testuser@example.com");
        request.setPassword("wrongpassword");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject login with non-existent email")
    void shouldRejectLoginWithNonExistentEmail() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject login with empty credentials")
    void shouldRejectLoginWithEmptyCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
