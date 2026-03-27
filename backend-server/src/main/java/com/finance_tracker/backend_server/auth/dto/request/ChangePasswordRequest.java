package com.finance_tracker.backend_server.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current Password is mandatory")
    private String currentPassword;

    @NotBlank(message = "New Password is mandatory")
    @Size(min = 6, message = "New Password should contain at least 6 characters")
    private String newPassword;
}

