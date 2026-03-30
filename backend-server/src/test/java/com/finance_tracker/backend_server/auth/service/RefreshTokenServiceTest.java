package com.finance_tracker.backend_server.auth.service;

import com.finance_tracker.backend_server.auth.entity.RefreshToken;
import com.finance_tracker.backend_server.auth.exception.TokenRefreshException;
import com.finance_tracker.backend_server.auth.repository.RefreshTokenRepository;
import com.finance_tracker.backend_server.security.service.RefreshTokenService;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    RefreshTokenService service;

    private User user;
    private RefreshToken existingToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenDurationMs", 86_400_000L);

        user = new User("john", "john@test.com", "encoded_pwd");
        user.setId(1L);

        existingToken = RefreshToken.builder()
                .id(5L)
                .user(user)
                .token("old-token")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();
    }


    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("success — creates new token and deletes previous one if it exists")
        void replacesExistingToken() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RefreshToken result = service.createRefreshToken(1L);

            verify(refreshTokenRepository).delete(existingToken);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getToken()).isNotBlank();
            assertThat(result.getExpiryDate()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("success — creates token when no previous token exists")
        void createsFirstToken() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RefreshToken result = service.createRefreshToken(1L);

            verify(refreshTokenRepository, never()).delete(any());
            assertThat(result.getToken()).isNotBlank();
        }

        @Test
        @DisplayName("throws IllegalArgumentException when user is not found")
        void userNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createRefreshToken(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyExpiration")
    class VerifyExpiration {

        @Test
        @DisplayName("success — returns token unchanged when not expired")
        void notExpired() {
            RefreshToken token = RefreshToken.builder()
                    .token("valid-token")
                    .user(user)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .build();

            RefreshToken result = service.verifyExpiration(token);

            assertThat(result).isSameAs(token);
            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws TokenRefreshException and deletes token when expired")
        void expired() {
            RefreshToken expiredToken = RefreshToken.builder()
                    .token("expired-token")
                    .user(user)
                    .expiryDate(Instant.now().minusSeconds(3600))
                    .build();

            assertThatThrownBy(() -> service.verifyExpiration(expiredToken))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessageContaining("expired");

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("deleteByUserId")
    class DeleteByUserId {

        @Test
        @DisplayName("success — deletes tokens and returns count")
        void success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(refreshTokenRepository.deleteByUser(user)).thenReturn(1);

            int deleted = service.deleteByUserId(1L);

            assertThat(deleted).isEqualTo(1);
            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when user is not found")
        void userNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteByUserId(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");

            verify(refreshTokenRepository, never()).deleteByUser(any());
        }
    }

    @Nested
    @DisplayName("findByToken")
    class FindByToken {

        @Test
        @DisplayName("returns token when it exists")
        void found() {
            when(refreshTokenRepository.findByToken("old-token"))
                    .thenReturn(Optional.of(existingToken));

            Optional<RefreshToken> result = service.findByToken("old-token");

            assertThat(result).isPresent().contains(existingToken);
        }

        @Test
        @DisplayName("returns empty Optional when token does not exist")
        void notFound() {
            when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

            Optional<RefreshToken> result = service.findByToken("missing");

            assertThat(result).isEmpty();
        }
    }
}