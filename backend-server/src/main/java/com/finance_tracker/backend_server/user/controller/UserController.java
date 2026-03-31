package com.finance_tracker.backend_server.user.controller;

import com.finance_tracker.backend_server.user.dto.response.UserListResponse;
import com.finance_tracker.backend_server.user.dto.response.UserProfileResponse;
import com.finance_tracker.backend_server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user-facing operations.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the profile of the currently authenticated user.
     */
    @Operation(
            summary = "Get my profile",
            description = "Returns the username, email, and roles of the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        logger.debug("Fetching profile for authenticated user");
        return ResponseEntity.ok(userService.getMyProfile());
    }

    /**
     * Returns a list of all registered users (id, username, email only).
     */
    @Operation(
            summary = "List all users",
            description = "Returns the id, username, and email of every registered user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserListResponse.class))),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping
    public ResponseEntity<@NonNull UserListResponse> listAllUsers() {
        logger.debug("Listing all users");
        return ResponseEntity.ok(userService.listAllUsers());
    }
}
