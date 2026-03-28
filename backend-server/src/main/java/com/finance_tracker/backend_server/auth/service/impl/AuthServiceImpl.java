package com.finance_tracker.backend_server.auth.service.impl;

import com.finance_tracker.backend_server.auth.dto.request.ChangePasswordRequest;
import com.finance_tracker.backend_server.auth.dto.request.LoginRequest;
import com.finance_tracker.backend_server.auth.dto.request.SignupRequest;
import com.finance_tracker.backend_server.auth.dto.response.LoginResponse;
import com.finance_tracker.backend_server.auth.entity.RefreshToken;
import com.finance_tracker.backend_server.auth.exception.TokenRefreshException;
import com.finance_tracker.backend_server.auth.service.AuthService;
import com.finance_tracker.backend_server.security.jwt.JwtUtils;
import com.finance_tracker.backend_server.security.service.RefreshTokenService;
import com.finance_tracker.backend_server.security.service.UserDetailsImpl;
import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import com.finance_tracker.backend_server.user.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link com.finance_tracker.backend_server.auth.service.AuthService} interface.
 * <p>
 * This service handles all authentication-related business logic including
 * user authentication, token management, registration, and password changes.
 * </p>
 *
 * @author FullSecurity Team
 * @version 1.0
 * @since 2024
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    /**
     * Constructs an AuthServiceImpl with required dependencies.
     *
     * @param authenticationManager Spring Security authentication manager
     * @param userRepository repository for user data access
     * @param roleService service for role management
     * @param passwordEncoder encoder for password hashing
     * @param jwtUtils utility class for JWT operations
     * @param refreshTokenService service for refresh token management
     */
    @Autowired
    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthResult authenticateUser(LoginRequest loginRequest) {
        logger.debug("Attempting authentication for user: {}", loginRequest.getUsername());

        // Authenticate user credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generate JWT tokens
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails.getUsername());

        // Extract user roles
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Create refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
        ResponseCookie jwtRefreshCookie = jwtUtils.generateRefreshJwtCookie(refreshToken.getToken());

        // Build login response
        LoginResponse loginResponse = LoginResponse.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(roles)
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build();

        logger.info("User {} authenticated successfully", loginRequest.getUsername());

        return new AuthResult(loginResponse, jwtCookie, jwtRefreshCookie);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogoutResult logoutUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!isAnonymousUser(principal)) {
            Long userId = ((UserDetailsImpl) principal).getId();
            refreshTokenService.deleteByUserId(userId);
            logger.info("User with ID {} logged out successfully", userId);
        }

        ResponseCookie jwtCookie = jwtUtils.getCleanJwtCookie();
        ResponseCookie jwtRefreshCookie = jwtUtils.getCleanJwtRefreshCookie();

        return new LogoutResult("You've been signed out!", jwtCookie, jwtRefreshCookie);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RefreshResult refreshToken(HttpServletRequest request) {
        // Try to get refresh token from cookie first (normal mode)
        String refreshToken = jwtUtils.getJwtRefreshFromCookies(request);

        // If not in cookie, try Authorization header (incognito/private mode)
        if (isNullOrEmpty(refreshToken)) {
            refreshToken = extractRefreshTokenFromHeader(request);
        }

        if (isNullOrEmpty(refreshToken)) {
            throw new TokenRefreshException("", "Refresh Token is empty!");
        }

        Optional<RefreshToken> tokenOptional = refreshTokenService.findByToken(refreshToken);

        if (tokenOptional.isEmpty()) {
            throw new TokenRefreshException(refreshToken, "Refresh token is not in database!");
        }

        RefreshToken refreshTokenObj = refreshTokenService.verifyExpiration(tokenOptional.get());
        User user = refreshTokenObj.getUser();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(user);
        String newJwtToken = jwtUtils.generateTokenFromUsername(user.getUsername());

        LoginResponse refreshResponse = LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(newJwtToken)
                .build();

        logger.debug("Token refreshed successfully for user: {}", user.getUsername());

        return new RefreshResult(refreshResponse, jwtCookie);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String registerUser(SignupRequest signupRequest) {
        // Validate username uniqueness
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        // Create new user account
        User user = new User(
                signupRequest.getUsername(),
                signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword())
        );

        // Assign roles
        Set<Role> roles = resolveRoles(signupRequest.getRole());
        user.setRoles(roles);

        userRepository.save(user);

        logger.info("User {} registered successfully", signupRequest.getUsername());

        return "User registered successfully!";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (isAnonymousUser(principal)) {
            throw new IllegalStateException("Nuk jeni i autentifikuar");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) principal;
        Long userId = userDetails.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Përdoruesi nuk u gjet"));

        // Verify current password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Fjalëkalimi aktual është i pasaktë");
        }

        // Ensure new password is different
        if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Fjalëkalimi i ri duhet të jetë i ndryshëm nga ai aktual");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);

        logger.info("Password changed successfully for user ID: {}", userId);
    }

    /**
     * Resolves the set of roles from string role names.
     *
     * @param strRoles set of role name strings
     * @return set of Role entities
     */
    private Set<Role> resolveRoles(Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(roleService.findByName(ERole.ROLE_USER));
        } else {
            strRoles.forEach(role -> {
                if (role.equalsIgnoreCase("admin")) {
                    roles.add(roleService.findByName(ERole.ROLE_ADMIN));
                } else {
                    roles.add(roleService.findByName(ERole.ROLE_USER));
                }
            });
        }

        return roles;
    }

    /**
     * Extracts refresh token from Authorization header.
     *
     * @param request the HTTP request
     * @return the refresh token or null if not present
     */
    private String extractRefreshTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Checks if the principal represents an anonymous user.
     *
     * @param principal the security principal
     * @return true if anonymous, false otherwise
     */
    private boolean isAnonymousUser(Object principal) {
        return "anonymousUser".equals(principal.toString());
    }

    /**
     * Checks if a string is null or empty.
     *
     * @param str the string to check
     * @return true if null or empty, false otherwise
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
