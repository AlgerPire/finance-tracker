package com.finance_tracker.backend_server.common.util;

import com.finance_tracker.backend_server.security.service.UserDetailsImpl;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityContextService {

    private final UserRepository userRepository;

    public SecurityContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            throw new IllegalStateException("Authentication required");
        }
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new IllegalStateException("Unexpected principal type");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
