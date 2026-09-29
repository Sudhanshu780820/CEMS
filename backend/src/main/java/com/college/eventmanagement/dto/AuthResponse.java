package com.college.eventmanagement.dto;

import com.college.eventmanagement.entity.ApprovalStatus;
import com.college.eventmanagement.entity.Role;

public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private Role role;
    private String name;
    private ApprovalStatus studentStatus;
    private String enrollmentId;
    private String branch;

    public AuthResponse() {}

    public AuthResponse(String token, Long userId, String email, Role role, String name, ApprovalStatus studentStatus, String enrollmentId, String branch) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.name = name;
        this.studentStatus = studentStatus;
        this.enrollmentId = enrollmentId;
        this.branch = branch;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ApprovalStatus getStudentStatus() { return studentStatus; }
    public void setStudentStatus(ApprovalStatus studentStatus) { this.studentStatus = studentStatus; }

    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
}
