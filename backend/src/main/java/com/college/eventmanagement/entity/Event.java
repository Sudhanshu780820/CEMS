package com.college.eventmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_date_venue", columnList = "venue_id, event_date"),
    @Index(name = "idx_events_status", columnList = "status")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "registered_count", nullable = false)
    private Integer registeredCount = 0;

    @Column(name = "registration_start_date", nullable = false)
    private LocalDate registrationStartDate;

    @Column(name = "registration_end_date", nullable = false)
    private LocalDate registrationEndDate;

    // Comma-separated or empty (empty means open to ALL)
    @Column(name = "eligible_branches", length = 255)
    private String eligibleBranches = "";

    @Column(name = "eligible_academic_years", length = 255)
    private String eligibleAcademicYears = "";

    @Column(name = "eligible_sections", length = 100)
    private String eligibleSections = "";

    @Column(name = "event_image_url", length = 500)
    private String eventImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventStatus status = EventStatus.PUBLISHED;

    @Column(name = "attendance_token", length = 100)
    private String attendanceToken;

    @Column(name = "is_attendance_active", nullable = false)
    private boolean isAttendanceActive = false;

    @Column(name = "has_venue_conflict", nullable = false)
    private boolean hasVenueConflict = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conflicting_event_id")
    private Event conflictingEvent;

    @Column(name = "conflict_notes", length = 500)
    private String conflictNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Event() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.registeredCount == null) {
            this.registeredCount = 0;
        }
        if (this.status == null) {
            this.status = EventStatus.PUBLISHED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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

    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }

    public Organizer getOrganizer() { return organizer; }
    public void setOrganizer(Organizer organizer) { this.organizer = organizer; }

    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public Integer getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(Integer registeredCount) { this.registeredCount = registeredCount; }

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

    public String getAttendanceToken() { return attendanceToken; }
    public void setAttendanceToken(String attendanceToken) { this.attendanceToken = attendanceToken; }

    public boolean isAttendanceActive() { return isAttendanceActive; }
    public void setAttendanceActive(boolean attendanceActive) { isAttendanceActive = attendanceActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isHasVenueConflict() { return hasVenueConflict; }
    public void setHasVenueConflict(boolean hasVenueConflict) { this.hasVenueConflict = hasVenueConflict; }

    public Event getConflictingEvent() { return conflictingEvent; }
    public void setConflictingEvent(Event conflictingEvent) { this.conflictingEvent = conflictingEvent; }

    public String getConflictNotes() { return conflictNotes; }
    public void setConflictNotes(String conflictNotes) { this.conflictNotes = conflictNotes; }
}
