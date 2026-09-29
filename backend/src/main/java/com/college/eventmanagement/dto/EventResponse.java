package com.college.eventmanagement.dto;

import com.college.eventmanagement.entity.EventStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private VenueResponse venue;
    private OrganizerResponse organizer;
    private Integer maxCapacity;
    private Integer registeredCount;
    private Integer remainingSeats;
    private double capacityPercentage;
    private LocalDate registrationStartDate;
    private LocalDate registrationEndDate;
    private String eligibleBranches;
    private String eligibleAcademicYears;
    private String eligibleSections;
    private String eventImageUrl;
    private EventStatus status;
    private boolean isAttendanceActive;
    private String attendanceToken;

    // Student specific context flags
    private boolean isEligible = true;
    private String eligibilityMessage;
    private boolean isRegistered = false;
    private String registrationStatus;
    private boolean isRegistrationOpen;
    private boolean isFull;
    private String actionStatus; // "REGISTER NOW", "REGISTERED", "FULL", "REGISTRATION CLOSED", "NOT ELIGIBLE"

    public EventResponse() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public VenueResponse getVenue() { return venue; }
    public void setVenue(VenueResponse venue) { this.venue = venue; }

    public OrganizerResponse getOrganizer() { return organizer; }
    public void setOrganizer(OrganizerResponse organizer) { this.organizer = organizer; }

    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public Integer getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(Integer registeredCount) { this.registeredCount = registeredCount; }

    public Integer getRemainingSeats() { return remainingSeats; }
    public void setRemainingSeats(Integer remainingSeats) { this.remainingSeats = remainingSeats; }

    public double getCapacityPercentage() { return capacityPercentage; }
    public void setCapacityPercentage(double capacityPercentage) { this.capacityPercentage = capacityPercentage; }

    public LocalDate getRegistrationStartDate() { return registrationStartDate; }
    public void setRegistrationStartDate(LocalDate registrationStartDate) { this.registrationStartDate = registrationStartDate; }

    public LocalDate getRegistrationEndDate() { return registrationEndDate; }
    public void setRegistrationEndDate(LocalDate registrationEndDate) { this.registrationEndDate = registrationEndDate; }

    public String getEligibleBranches() { return eligibleBranches; }
    public void setEligibleBranches(String eligibleBranches) { this.eligibleBranches = eligibleBranches; }

    public String getEligibleAcademicYears() { return eligibleAcademicYears; }
    public void setEligibleAcademicYears(String eligibleAcademicYears) { this.eligibleAcademicYears = eligibleAcademicYears; }

    public String getEligibleSections() { return eligibleSections; }
    public void setEligibleSections(String eligibleSections) { this.eligibleSections = eligibleSections; }

    public String getEventImageUrl() { return eventImageUrl; }
    public void setEventImageUrl(String eventImageUrl) { this.eventImageUrl = eventImageUrl; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public boolean isAttendanceActive() { return isAttendanceActive; }
    public void setAttendanceActive(boolean attendanceActive) { isAttendanceActive = attendanceActive; }

    public String getAttendanceToken() { return attendanceToken; }
    public void setAttendanceToken(String attendanceToken) { this.attendanceToken = attendanceToken; }

    public boolean isEligible() { return isEligible; }
    public void setEligible(boolean eligible) { isEligible = eligible; }

    public String getEligibilityMessage() { return eligibilityMessage; }
    public void setEligibilityMessage(String eligibilityMessage) { this.eligibilityMessage = eligibilityMessage; }

    public boolean isRegistered() { return isRegistered; }
    public void setRegistered(boolean registered) { isRegistered = registered; }

    public String getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(String registrationStatus) { this.registrationStatus = registrationStatus; }

    public boolean isRegistrationOpen() { return isRegistrationOpen; }
    public void setRegistrationOpen(boolean registrationOpen) { isRegistrationOpen = registrationOpen; }

    public boolean isFull() { return isFull; }
    public void setFull(boolean full) { isFull = full; }

    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }
}
