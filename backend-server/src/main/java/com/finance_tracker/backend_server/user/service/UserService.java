package com.finance_tracker.backend_server.user.service;

import com.finance_tracker.backend_server.user.dto.response.UserListResponse;
import com.finance_tracker.backend_server.user.dto.response.UserProfileResponse;

/**
 * Service interface for user-facing operations.
 */
public interface UserService {

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @return the authenticated user's username, email, and roles
     */
    UserProfileResponse getMyProfile();

    /**
     * Returns a list of all registered users with their id, username, and email.
     *
     * @return list of user summaries
     */
    UserListResponse listAllUsers();
}
