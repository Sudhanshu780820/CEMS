package com.college.eventmanagement.dto;

public class OrganizerDashboardStats {
    private long totalEvents;
    private long upcomingEvents;
    private long completedEvents;
    private long totalParticipants;
    private long todayEventsCount;
    private double overallAttendanceRate;

    public OrganizerDashboardStats() {}

    public OrganizerDashboardStats(long totalEvents, long upcomingEvents, long completedEvents, long totalParticipants, long todayEventsCount, double overallAttendanceRate) {
        this.totalEvents = totalEvents;
        this.upcomingEvents = upcomingEvents;
        this.completedEvents = completedEvents;
        this.totalParticipants = totalParticipants;
        this.todayEventsCount = todayEventsCount;
        this.overallAttendanceRate = overallAttendanceRate;
    }

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public long getUpcomingEvents() { return upcomingEvents; }
    public void setUpcomingEvents(long upcomingEvents) { this.upcomingEvents = upcomingEvents; }

    public long getCompletedEvents() { return completedEvents; }
    public void setCompletedEvents(long completedEvents) { this.completedEvents = completedEvents; }

    public long getTotalParticipants() { return totalParticipants; }
    public void setTotalParticipants(long totalParticipants) { this.totalParticipants = totalParticipants; }

    public long getTodayEventsCount() { return todayEventsCount; }
    public void setTodayEventsCount(long todayEventsCount) { this.todayEventsCount = todayEventsCount; }

    public double getOverallAttendanceRate() { return overallAttendanceRate; }
    public void setOverallAttendanceRate(double overallAttendanceRate) { this.overallAttendanceRate = overallAttendanceRate; }
}
