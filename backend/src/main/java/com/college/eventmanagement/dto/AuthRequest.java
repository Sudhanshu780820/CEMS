package com.college.eventmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    @NotBlank(message = "Email or Enrollment ID is required")
    private String usernameOrEnrollmentId;

    @NotBlank(message = "Password is required")
    private String password;

    public AuthRequest() {}

    public AuthRequest(String usernameOrEnrollmentId, String password) {
        this.usernameOrEnrollmentId = usernameOrEnrollmentId;
        this.password = password;
    }

    public String getUsernameOrEnrollmentId() { return usernameOrEnrollmentId; }
    public void setUsernameOrEnrollmentId(String usernameOrEnrollmentId) { this.usernameOrEnrollmentId = usernameOrEnrollmentId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
