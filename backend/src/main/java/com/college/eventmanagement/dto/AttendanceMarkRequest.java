package com.college.eventmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class AttendanceMarkRequest {
    @NotBlank(message = "Attendance token is required")
    private String attendanceToken;

    private Double latitude;
    private Double longitude;

    public AttendanceMarkRequest() {}

    public AttendanceMarkRequest(String attendanceToken) {
        this.attendanceToken = attendanceToken;
    }

    public AttendanceMarkRequest(String attendanceToken, Double latitude, Double longitude) {
        this.attendanceToken = attendanceToken;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getAttendanceToken() { return attendanceToken; }
    public void setAttendanceToken(String attendanceToken) { this.attendanceToken = attendanceToken; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
