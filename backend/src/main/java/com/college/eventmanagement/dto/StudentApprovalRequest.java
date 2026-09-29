package com.college.eventmanagement.dto;

import com.college.eventmanagement.entity.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

public class StudentApprovalRequest {
    @NotNull(message = "Status is required")
    private ApprovalStatus status;

    private String rejectionReason;

    public StudentApprovalRequest() {}

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
