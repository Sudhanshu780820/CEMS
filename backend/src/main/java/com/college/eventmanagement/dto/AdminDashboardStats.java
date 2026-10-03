package com.college.eventmanagement.dto;

public class AdminDashboardStats {
    private long totalStudents;
    private long pendingStudentApprovals;
    private long pendingEventApprovals;
    private long approvedStudents;
    private long totalEvents;
    private long upcomingEvents;
    private long totalOrganizers;
    private long totalRegistrations;
    private double overallAttendanceRate;

    public AdminDashboardStats() {}

    public AdminDashboardStats(long totalStudents, long pendingStudentApprovals, long pendingEventApprovals, long approvedStudents, long totalEvents, long upcomingEvents, long totalOrganizers, long totalRegistrations, double overallAttendanceRate) {
        this.totalStudents = totalStudents;
        this.pendingStudentApprovals = pendingStudentApprovals;
        this.pendingEventApprovals = pendingEventApprovals;
        this.approvedStudents = approvedStudents;
        this.totalEvents = totalEvents;
        this.upcomingEvents = upcomingEvents;
        this.totalOrganizers = totalOrganizers;
        this.totalRegistrations = totalRegistrations;
        this.overallAttendanceRate = overallAttendanceRate;
    }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public long getPendingStudentApprovals() { return pendingStudentApprovals; }
    public void setPendingStudentApprovals(long pendingStudentApprovals) { this.pendingStudentApprovals = pendingStudentApprovals; }

    public long getPendingEventApprovals() { return pendingEventApprovals; }
    public void setPendingEventApprovals(long pendingEventApprovals) { this.pendingEventApprovals = pendingEventApprovals; }

    public long getApprovedStudents() { return approvedStudents; }
    public void setApprovedStudents(long approvedStudents) { this.approvedStudents = approvedStudents; }

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public long getUpcomingEvents() { return upcomingEvents; }
    public void setUpcomingEvents(long upcomingEvents) { this.upcomingEvents = upcomingEvents; }

    public long getTotalOrganizers() { return totalOrganizers; }
    public void setTotalOrganizers(long totalOrganizers) { this.totalOrganizers = totalOrganizers; }

    public long getTotalRegistrations() { return totalRegistrations; }
    public void setTotalRegistrations(long totalRegistrations) { this.totalRegistrations = totalRegistrations; }

    public double getOverallAttendanceRate() { return overallAttendanceRate; }
    public void setOverallAttendanceRate(double overallAttendanceRate) { this.overallAttendanceRate = overallAttendanceRate; }
}
