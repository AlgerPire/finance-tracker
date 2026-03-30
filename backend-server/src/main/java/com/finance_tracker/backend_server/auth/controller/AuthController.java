package com.finance_tracker.backend_server.auth.controller;

import com.finance_tracker.backend_server.auth.dto.request.ChangePasswordRequest;
import com.finance_tracker.backend_server.auth.dto.request.LoginRequest;
import com.finance_tracker.backend_server.auth.dto.request.SignupRequest;
import com.finance_tracker.backend_server.auth.dto.response.LoginResponse;
import com.finance_tracker.backend_server.auth.exception.TokenRefreshException;
import com.finance_tracker.backend_server.auth.service.AuthService;
import com.finance_tracker.backend_server.auth.service.AuthService.*;
import com.finance_tracker.backend_server.common.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * <p>
 * This controller handles all authentication-related HTTP endpoints including
 * user sign-in, sign-out, token refresh, registration, and password management.
 * All business logic is delegated to the {@link com.finance_tracker.backend_server.auth.service.AuthService}.
 * </p>
 *
 * @author Finance Tracker Team
 * @version 1.0
 * @since 2026
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Constructs an AuthController with the required service.
     *
     * @param authService the authentication service
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns JWT tokens.
     * <p>
     * This endpoint validates user credentials and returns both access and refresh tokens
     * via cookies and response body (for incognito mode support).
     * </p>
     *
     * @param loginRequest the login credentials
     * @return ResponseEntity containing user info and tokens
     */
    @Operation(summary = "Sign in user", description = "Authenticates user and returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.debug("Sign-in request received for user: {}", loginRequest.getUsername());

        AuthResult result = authService.authenticateUser(loginRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.jwtCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.loginResponse());
    }

    /**
     * Registers a new user in the system.
     * <p>
     * This endpoint is currently commented out to prevent public registration.
     * Uncomment to enable user self-registration.
     * </p>
     *
     * @param signUpRequest the registration details
     * @return ResponseEntity with registration result
     */
    @Operation(summary = "Register new user", description = "Creates a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Username or email already exists")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        logger.debug("Sign-up request received for user: {}", signUpRequest.getUsername());

        try {
            String message = authService.registerUser(signUpRequest);
            return ResponseEntity.ok(new MessageResponse(message));
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Logs out the currently authenticated user.
     * <p>
     * This endpoint invalidates the user's refresh token and clears authentication cookies.
     * </p>
     *
     * @return ResponseEntity with logout confirmation
     */
    @Operation(summary = "Sign out user", description = "Logs out user and invalidates tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully signed out")
    })
    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        logger.debug("Sign-out request received");

        LogoutResult result = authService.logoutUser();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.jwtCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(new MessageResponse(result.message()));
    }

    /**
     * Refreshes the JWT access token using a valid refresh token.
     * <p>
     * This endpoint supports both cookie-based and header-based token retrieval
     * for compatibility with different browser modes.
     * </p>
     *
     * @param request the HTTP request containing the refresh token
     * @return ResponseEntity with new access token
     */
    @Operation(summary = "Refresh token", description = "Generates new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        logger.debug("Token refresh request received");

        try {
            RefreshResult result = authService.refreshToken(request);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, result.jwtCookie().toString())
                    .body(result.loginResponse());
        } catch (TokenRefreshException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Changes the password for the currently authenticated user.
     * <p>
     * This endpoint requires ADMIN role and validates both current and new passwords.
     * </p>
     *
     * @param changePasswordRequest the password change request
     * @return ResponseEntity with operation result
     */
    @Operation(summary = "Change password", description = "Changes password for authenticated user (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid current password or validation error"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        logger.debug("Password change request received");

        try {
            authService.changePassword(changePasswordRequest);
            return ResponseEntity.ok(new MessageResponse("Password changed successfully!"));
        } catch (IllegalStateException e) {
            logger.warn("Password change failed - not authenticated: {}", e.getMessage());
            return ResponseEntity.status(401).body(new MessageResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Password change failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during password change: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new MessageResponse("An error occurred while trying to change the password: " + e.getMessage()));

        }
    }
}