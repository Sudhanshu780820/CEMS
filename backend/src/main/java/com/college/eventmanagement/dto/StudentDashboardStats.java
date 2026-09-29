package com.college.eventmanagement.dto;

public class StudentDashboardStats {
    private long registeredEventsCount;
    private long attendedEventsCount;
    private long availableEventsCount;

    public StudentDashboardStats() {}

    public StudentDashboardStats(long registeredEventsCount, long attendedEventsCount, long availableEventsCount) {
        this.registeredEventsCount = registeredEventsCount;
        this.attendedEventsCount = attendedEventsCount;
        this.availableEventsCount = availableEventsCount;
    }

    public long getRegisteredEventsCount() { return registeredEventsCount; }
    public void setRegisteredEventsCount(long registeredEventsCount) { this.registeredEventsCount = registeredEventsCount; }

    public long getAttendedEventsCount() { return attendedEventsCount; }
    public void setAttendedEventsCount(long attendedEventsCount) { this.attendedEventsCount = attendedEventsCount; }

    public long getAvailableEventsCount() { return availableEventsCount; }
    public void setAvailableEventsCount(long availableEventsCount) { this.availableEventsCount = availableEventsCount; }
}
