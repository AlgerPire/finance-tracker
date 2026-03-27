package com.finance_tracker.backend_server.auth.service;

import com.finance_tracker.backend_server.auth.dto.request.ChangePasswordRequest;
import com.finance_tracker.backend_server.auth.dto.request.LoginRequest;
import com.finance_tracker.backend_server.auth.dto.request.SignupRequest;
import com.finance_tracker.backend_server.auth.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

/**
 * Service interface for authentication operations.
 * <p>
 * This interface defines the contract for all authentication-related operations
 * including user sign-in, sign-out, token refresh, user registration, and password management.
 * </p>
 *
 * @author Finance Tracker Team
 * @version 1.0
 * @since 2024
 */
public interface AuthService {

    /**
     * Authenticates a user with the provided credentials.
     * <p>
     * This method validates the username and password, generates JWT access and refresh tokens,
     * and returns user information along with authentication cookies.
     * </p>
     *
     * @param loginRequest the login request containing username and password
     * @return {@link AuthResult} containing the login response, JWT cookie, and refresh token cookie
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    AuthResult authenticateUser(LoginRequest loginRequest);

    /**
     * Logs out the currently authenticated user.
     * <p>
     * This method invalidates the user's refresh token and clears authentication cookies.
     * </p>
     *
     * @return {@link LogoutResult} containing the logout message and clean cookies
     */
    LogoutResult logoutUser();

    /**
     * Refreshes the JWT access token using a valid refresh token.
     * <p>
     * This method supports both cookie-based and header-based token retrieval
     * for compatibility with different browser modes (normal and incognito).
     * </p>
     *
     * @param request the HTTP servlet request containing the refresh token
     * @return {@link RefreshResult} containing the new access token and updated cookies
     * @throws com.finance_tracker.backend_server.auth.exception.TokenRefreshException if refresh token is invalid or expired
     */
    RefreshResult refreshToken(HttpServletRequest request);

    /**
     * Registers a new user in the system.
     * <p>
     * This method creates a new user account with the specified username, email,
     * password, and roles. If no roles are specified, the default USER role is assigned.
     * </p>
     *
     * @param signupRequest the signup request containing user registration details
     * @return a message indicating the result of the registration
     * @throws IllegalArgumentException if username or email already exists
     */
    String registerUser(SignupRequest signupRequest);

    /**
     * Changes the password for the currently authenticated user.
     * <p>
     * This method validates the current password, ensures the new password is different,
     * and updates the user's password in the database.
     * </p>
     *
     * @param changePasswordRequest the request containing current and new passwords
     * @throws IllegalArgumentException if current password is incorrect or new password matches current
     * @throws IllegalStateException if user is not authenticated
     */
    void changePassword(ChangePasswordRequest changePasswordRequest);

    /**
     * Result object containing authentication response data.
     *
     * @param loginResponse the login response with user details and tokens
     * @param jwtCookie the JWT access token cookie
     * @param refreshCookie the refresh token cookie
     */
    record AuthResult(LoginResponse loginResponse, ResponseCookie jwtCookie, ResponseCookie refreshCookie) {}

    /**
     * Result object containing logout response data.
     *
     * @param message the logout confirmation message
     * @param jwtCookie the clean JWT cookie (invalidated)
     * @param refreshCookie the clean refresh token cookie (invalidated)
     */
    record LogoutResult(String message, ResponseCookie jwtCookie, ResponseCookie refreshCookie) {}

    /**
     * Result object containing token refresh response data.
     *
     * @param loginResponse the response with new access token
     * @param jwtCookie the new JWT access token cookie
     */
    record RefreshResult(LoginResponse loginResponse, ResponseCookie jwtCookie) {}
}
