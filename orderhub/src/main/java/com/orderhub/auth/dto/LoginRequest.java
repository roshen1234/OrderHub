package com.orderhub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record LoginRequest(
        @NotBlank(message = "Email is required") @Email String email,
        @NotBlank(message = "Password is required") String password
) {}
