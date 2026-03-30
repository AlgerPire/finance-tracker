package com.finance_tracker.backend_server.user.controller;

import com.finance_tracker.backend_server.user.dto.response.PagedUserResponse;
import com.finance_tracker.backend_server.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing admin users.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Users", description = "Admin user management APIs")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    /**
     * The admin user service.
     */
    private final AdminUserService adminUserService;

    /**
     * Constructor.
     */
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Endpoint to list all users.
     */
    @Operation(
            summary = "List all users",
            description = "Returns a paginated list of all registered users with their roles. "
                    + "Sortable by username, email. Default: username ASC.")
    @GetMapping
    public ResponseEntity<@NonNull PagedUserResponse> listAllUsers(
            @ParameterObject
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC)
            Pageable pageable) {
        logger.info("Listing all users with pageable: {}", pageable);
        return ResponseEntity.ok(adminUserService.listAllUsers(pageable));
    }
}
