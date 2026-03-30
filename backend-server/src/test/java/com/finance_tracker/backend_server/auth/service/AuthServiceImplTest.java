package com.finance_tracker.backend_server.auth.service;

import com.finance_tracker.backend_server.auth.dto.request.ChangePasswordRequest;
import com.finance_tracker.backend_server.auth.dto.request.LoginRequest;
import com.finance_tracker.backend_server.auth.dto.request.SignupRequest;
import com.finance_tracker.backend_server.auth.entity.RefreshToken;
import com.finance_tracker.backend_server.auth.exception.TokenRefreshException;
import com.finance_tracker.backend_server.auth.service.impl.AuthServiceImpl;
import com.finance_tracker.backend_server.security.jwt.JwtUtils;
import com.finance_tracker.backend_server.security.service.RefreshTokenService;
import com.finance_tracker.backend_server.security.service.UserDetailsImpl;
import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import com.finance_tracker.backend_server.user.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock RoleService roleService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtils jwtUtils;
    @Mock RefreshTokenService refreshTokenService;

    @InjectMocks AuthServiceImpl service;

    private UserDetailsImpl userDetails;
    private User user;
    private RefreshToken refreshToken;
    private ResponseCookie jwtCookie;
    private ResponseCookie refreshCookie;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        user = new User("john", "john@test.com", "encoded_pwd");
        user.setId(1L);

        userDetails = new UserDetailsImpl(
                1L, "john", "john@test.com", "encoded_pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        refreshToken = RefreshToken.builder()
                .id(10L)
                .user(user)
                .token("refresh-uuid-token")
                .expiryDate(Instant.now().plusSeconds(86400))
                .build();

        jwtCookie = ResponseCookie.from("jwt", "jwt-value").build();
        refreshCookie = ResponseCookie.from("refresh", "refresh-value").build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // authenticateUser
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("authenticateUser")
    class AuthenticateUser {

        @Test
        @DisplayName("success — returns AuthResult with user info and tokens")
        void success() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
            when(jwtUtils.generateTokenFromUsername("john")).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);
            when(jwtUtils.generateRefreshJwtCookie("refresh-uuid-token")).thenReturn(refreshCookie);

            AuthService.AuthResult result =
                    service.authenticateUser(new LoginRequest("john", "password"));

            assertThat(result.loginResponse().getUsername()).isEqualTo("john");
            assertThat(result.loginResponse().getEmail()).isEqualTo("john@test.com");
            assertThat(result.loginResponse().getRoles()).containsExactly("ROLE_USER");
            assertThat(result.loginResponse().getAccessToken()).isEqualTo("access-token");
            assertThat(result.loginResponse().getRefreshToken()).isEqualTo("refresh-uuid-token");
            assertThat(result.jwtCookie()).isEqualTo(jwtCookie);
            assertThat(result.refreshCookie()).isEqualTo(refreshCookie);
        }

        @Test
        @DisplayName("throws BadCredentialsException when credentials are invalid")
        void badCredentials() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() ->
                    service.authenticateUser(new LoginRequest("john", "wrong")))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtUtils, never()).generateJwtCookie(any(UserDetailsImpl.class));
            verify(jwtUtils, never()).generateJwtCookie(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(anyLong());
        }
    }

    // -----------------------------------------------------------------------
    // logoutUser
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("logoutUser")
    class LogoutUser {

        @Test
        @DisplayName("success — deletes refresh token for authenticated user")
        void authenticatedUser() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(jwtUtils.getCleanJwtCookie()).thenReturn(jwtCookie);
            when(jwtUtils.getCleanJwtRefreshCookie()).thenReturn(refreshCookie);

            AuthService.LogoutResult result = service.logoutUser();

            assertThat(result.message()).isEqualTo("You've been signed out!");
            verify(refreshTokenService).deleteByUserId(1L);
        }

        @Test
        @DisplayName("success — skips token deletion for anonymous user")
        void anonymousUser() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("anonymousUser");
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(jwtUtils.getCleanJwtCookie()).thenReturn(jwtCookie);
            when(jwtUtils.getCleanJwtRefreshCookie()).thenReturn(refreshCookie);

            AuthService.LogoutResult result = service.logoutUser();

            assertThat(result.message()).isEqualTo("You've been signed out!");
            verify(refreshTokenService, never()).deleteByUserId(anyLong());
        }
    }

    // -----------------------------------------------------------------------
    // refreshToken
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("success — new JWT generated from valid cookie token")
        void successFromCookie() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn("refresh-uuid-token");
            when(refreshTokenService.findByToken("refresh-uuid-token"))
                    .thenReturn(Optional.of(refreshToken));
            when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
            when(jwtUtils.generateJwtCookie(user)).thenReturn(jwtCookie);
            when(jwtUtils.generateTokenFromUsername("john")).thenReturn("new-access-token");

            AuthService.RefreshResult result = service.refreshToken(request);

            assertThat(result.loginResponse().getUsername()).isEqualTo("john");
            assertThat(result.loginResponse().getEmail()).isEqualTo("john@test.com");
            assertThat(result.loginResponse().getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.jwtCookie()).isEqualTo(jwtCookie);
        }

        @Test
        @DisplayName("success — falls back to Authorization header when cookie is absent")
        void successFromHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Bearer refresh-uuid-token");
            when(refreshTokenService.findByToken("refresh-uuid-token"))
                    .thenReturn(Optional.of(refreshToken));
            when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
            when(jwtUtils.generateJwtCookie(user)).thenReturn(jwtCookie);
            when(jwtUtils.generateTokenFromUsername("john")).thenReturn("new-access-token");

            AuthService.RefreshResult result = service.refreshToken(request);

            assertThat(result.loginResponse().getAccessToken()).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("throws TokenRefreshException when both cookie and header are absent")
        void emptyToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn(null);

            assertThatThrownBy(() -> service.refreshToken(request))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessageContaining("Refresh Token is empty");
        }

        @Test
        @DisplayName("throws TokenRefreshException when token is not found in database")
        void tokenNotInDatabase() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn("unknown-token");
            when(refreshTokenService.findByToken("unknown-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.refreshToken(request))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessageContaining("not in database");
        }
    }

    // -----------------------------------------------------------------------
    // registerUser
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("success — user saved and confirmation message returned")
        void success() {
            SignupRequest request = SignupRequest.builder()
                    .username("newuser").email("new@test.com").password("password123")
                    .build();
            Role userRole = Role.builder().name(ERole.ROLE_USER).build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_pwd");
            when(roleService.findByName(ERole.ROLE_USER)).thenReturn(userRole);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = service.registerUser(request);

            assertThat(result).isEqualTo("User registered successfully!");
            verify(userRepository).save(argThat(u -> u.getUsername().equals("newuser")));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when username is already taken")
        void usernameTaken() {
            SignupRequest request = SignupRequest.builder()
                    .username("john").email("other@test.com").password("password123")
                    .build();
            when(userRepository.existsByUsername("john")).thenReturn(true);

            assertThatThrownBy(() -> service.registerUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username is already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when email is already in use")
        void emailInUse() {
            SignupRequest request = SignupRequest.builder()
                    .username("newuser").email("john@test.com").password("password123")
                    .build();
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

            assertThatThrownBy(() -> service.registerUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email is already in use");

            verify(userRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // changePassword
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("success — password updated for authenticated user")
        void success() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("current_pwd", "encoded_pwd")).thenReturn(true);
            when(passwordEncoder.matches("new_pwd", "encoded_pwd")).thenReturn(false);
            when(passwordEncoder.encode("new_pwd")).thenReturn("new_encoded_pwd");

            service.changePassword(new ChangePasswordRequest("current_pwd", "new_pwd"));

            assertThat(user.getPassword()).isEqualTo("new_encoded_pwd");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws IllegalStateException when principal is anonymous")
        void notAuthenticated() {
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn("anonymousUser");
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("current_pwd", "new_pwd")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not authenticated");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when current password is incorrect")
        void wrongCurrentPassword() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong_pwd", "encoded_pwd")).thenReturn(false);

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("wrong_pwd", "new_pwd")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password is incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when new password is same as current")
        void samePassword() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            // both calls to matches("current_pwd", "encoded_pwd") return true
            when(passwordEncoder.matches("current_pwd", "encoded_pwd")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("current_pwd", "current_pwd")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be the same");

            verify(userRepository, never()).save(any());
        }
    }
}
