package com.college.eventmanagement.dto;

import com.college.eventmanagement.entity.AttendanceStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ManualAttendanceRequest {
    @NotEmpty(message = "Student IDs list cannot be empty")
    private List<Long> studentIds;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    public ManualAttendanceRequest() {}

    public ManualAttendanceRequest(List<Long> studentIds, AttendanceStatus status) {
        this.studentIds = studentIds;
        this.status = status;
    }

    public List<Long> getStudentIds() { return studentIds; }
    public void setStudentIds(List<Long> studentIds) { this.studentIds = studentIds; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
}
