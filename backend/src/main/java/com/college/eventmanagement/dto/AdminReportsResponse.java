package com.college.eventmanagement.dto;

import java.util.List;
import java.util.Map;

public class AdminReportsResponse {
    private long totalEvents;
    private long totalRegistrations;
    private long totalAttendance;
    private double overallAttendancePercentage;
    private Map<String, Long> eventsByBranch;
    private Map<String, Long> eventsByCategory;
    private List<Map<String, Object>> mostPopularEvents;
    private Map<String, Long> studentParticipationByBranch;

    public AdminReportsResponse() {}

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public long getTotalRegistrations() { return totalRegistrations; }
    public void setTotalRegistrations(long totalRegistrations) { this.totalRegistrations = totalRegistrations; }

    public long getTotalAttendance() { return totalAttendance; }
    public void setTotalAttendance(long totalAttendance) { this.totalAttendance = totalAttendance; }

    public double getOverallAttendancePercentage() { return overallAttendancePercentage; }
    public void setOverallAttendancePercentage(double overallAttendancePercentage) { this.overallAttendancePercentage = overallAttendancePercentage; }

    public Map<String, Long> getEventsByBranch() { return eventsByBranch; }
    public void setEventsByBranch(Map<String, Long> eventsByBranch) { this.eventsByBranch = eventsByBranch; }

    public Map<String, Long> getEventsByCategory() { return eventsByCategory; }
    public void setEventsByCategory(Map<String, Long> eventsByCategory) { this.eventsByCategory = eventsByCategory; }

    public List<Map<String, Object>> getMostPopularEvents() { return mostPopularEvents; }
    public void setMostPopularEvents(List<Map<String, Object>> mostPopularEvents) { this.mostPopularEvents = mostPopularEvents; }

    public Map<String, Long> getStudentParticipationByBranch() { return studentParticipationByBranch; }
    public void setStudentParticipationByBranch(Map<String, Long> studentParticipationByBranch) { this.studentParticipationByBranch = studentParticipationByBranch; }
}
