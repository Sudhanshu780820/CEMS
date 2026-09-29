package com.college.eventmanagement.dto;

public class AttendanceStatsResponse {
    private long totalRegistered;
    private long presentCount;
    private long absentCount;
    private double attendancePercentage;

    public AttendanceStatsResponse() {}

    public AttendanceStatsResponse(long totalRegistered, long presentCount, long absentCount, double attendancePercentage) {
        this.totalRegistered = totalRegistered;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.attendancePercentage = attendancePercentage;
    }

    public long getTotalRegistered() { return totalRegistered; }
    public void setTotalRegistered(long totalRegistered) { this.totalRegistered = totalRegistered; }

    public long getPresentCount() { return presentCount; }
    public void setPresentCount(long presentCount) { this.presentCount = presentCount; }

    public long getAbsentCount() { return absentCount; }
    public void setAbsentCount(long absentCount) { this.absentCount = absentCount; }

    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }
}
