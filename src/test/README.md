# Test Suite Documentation

## Overview
This test suite provides comprehensive coverage for the OneOnline Backend application, including unit tests, integration tests, and repository tests.

## Test Structure

```
src/test/java/
├── com/oneonline/backend/
│   ├── service/
│   │   ├── auth/
│   │   │   ├── JwtServiceTest.java          (Unit tests for JWT operations)
│   │   │   └── UserServiceTest.java         (Unit tests for user management)
│   │   ├── game/
│   │   │   └── CardValidatorTest.java       (Unit tests for card validation)
│   │   └── ranking/
│   │       └── RankingServiceTest.java      (Unit tests for ranking operations)
│   ├── repository/
│   │   └── UserRepositoryTest.java          (Repository tests for User entity)
│   ├── controller/
│   │   └── AuthControllerIntegrationTest.java  (Integration tests for auth endpoints)
│   └── OneOnlineBackendApplicationTests.java   (Context loading test)
└── resources/
    └── application-test.properties          (Test configuration)
```

## Test Coverage

### Unit Tests (Mockito)

#### 1. JwtServiceTest
Tests JWT token operations:
- Access token generation
- Refresh token generation
- Token validation
- Claim extraction (email, userId, expiration)
- Token expiration detection

**Coverage:** 12 test cases

#### 2. UserServiceTest
Tests user management operations:
- User retrieval (by ID, email, nickname)
- User creation and validation
- Duplicate email/nickname handling
- Active user listing

**Coverage:** 10 test cases

#### 3. CardValidatorTest
Tests game card validation logic:
- Number card validation
- Action card validation (Skip, Reverse, Draw Two)
- Wild card validation
- Wild Draw Four legality checking
- Valid card detection in hand
- Color/number/type matching

**Coverage:** 20+ test cases

#### 4. RankingServiceTest
Tests ranking and leaderboard operations:
- Global top 100 retrieval
- Room winner statistics
- Null winner handling

**Coverage:** 5 test cases

### Repository Tests (DataJpaTest)

#### UserRepositoryTest
Tests database operations for User entity:
- Find by email, nickname, OAuth2 ID
- Find by auth provider
- Existence checks
- CRUD operations
- Active user filtering
- Auth provider counting

**Coverage:** 13 test cases

### Integration Tests (SpringBootTest + MockMvc)

#### AuthControllerIntegrationTest
Tests authentication REST endpoints with full Spring context:
- User registration (success and error cases)
- User login (success and error cases)
- Email/nickname uniqueness validation
- Password validation
- Invalid input handling

**Coverage:** 8 test cases

## Running Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests "com.oneonline.backend.service.auth.JwtServiceTest"
```

### Run Tests with Coverage Report
```bash
./gradlew test jacocoTestReport
```
Coverage report will be generated at: `build/reports/jacoco/test/html/index.html`

### Run Tests in Continuous Mode
```bash
./gradlew test --continuous
```

## Test Configuration

### application-test.properties
- H2 in-memory database (PostgreSQL mode)
- Disabled Flyway (uses JPA DDL auto-creation)
- Test JWT secrets and timeouts
- Reduced logging levels

### Test Profiles
All tests use `@ActiveProfiles("test")` to ensure isolation from production/development configurations.

## Test Best Practices

1. **Isolation**: Each test is independent and can run in any order
2. **Cleanup**: Repository tests use `@Transactional` for automatic rollback
3. **Mocking**: Unit tests use Mockito for dependency mocking
4. **Assertions**: Using AssertJ for fluent assertions
5. **Naming**: Descriptive test names following "shouldDoSomethingWhenCondition" pattern
6. **Coverage**: Focus on critical business logic and edge cases

## Future Test Enhancements

- [ ] Add tests for GameEngine service
- [ ] Add tests for RoomManager service
- [ ] Add tests for WebSocket controllers
- [ ] Add tests for OAuth2 authentication flow
- [ ] Add performance/load tests
- [ ] Add tests for bot AI logic
- [ ] Increase code coverage to 80%+

## Dependencies

### Test Dependencies (from build.gradle)
```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'com.h2database:h2'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

Includes:
- JUnit 5 (Jupiter)
- Mockito
- AssertJ
- Spring Test
- Spring Security Test
- H2 Database

## Notes

- Integration tests may take longer to run as they bootstrap the full Spring context
- H2 database is used for tests to avoid PostgreSQL dependency
- JWT secrets in test configuration are for testing purposes only
- All tests follow AAA pattern (Arrange, Act, Assert)
