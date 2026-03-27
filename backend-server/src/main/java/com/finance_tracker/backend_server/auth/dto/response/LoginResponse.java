package com.finance_tracker.backend_server.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LoginResponse {

    private Long id;

    private String username;

    private String email;

    private List<String> roles;

    // Tokens for fallback when cookies are blocked (e.g., incognito mode)
    private String accessToken;
    private String refreshToken;

    // Constructor without tokens (for cookie-based auth in normal mode)
    public LoginResponse(Long id, String username, String email, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}
