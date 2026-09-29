package com.college.eventmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class AttendanceMarkRequest {
    @NotBlank(message = "Attendance token is required")
    private String attendanceToken;

    public AttendanceMarkRequest() {}

    public AttendanceMarkRequest(String attendanceToken) {
        this.attendanceToken = attendanceToken;
    }

    public String getAttendanceToken() { return attendanceToken; }
    public void setAttendanceToken(String attendanceToken) { this.attendanceToken = attendanceToken; }
}
