package com.college.eventmanagement.service;

import com.college.eventmanagement.dto.*;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ConflictException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    private final VenueService venueService;

    public EventService(EventRepository eventRepository,
                        VenueRepository venueRepository,
                        OrganizerRepository organizerRepository,
                        UserRepository userRepository,
                        StudentRepository studentRepository,
                        RegistrationRepository registrationRepository,
                        AttendanceRepository attendanceRepository,
                        NotificationService notificationService,
                        VenueService venueService) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.organizerRepository = organizerRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.notificationService = notificationService;
        this.venueService = venueService;
    }

    @Transactional
    public EventResponse createEvent(EventCreateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organizer organizer;
        if (user.getRole() == Role.ROLE_ADMIN) {
            // Admin can assign to first organizer or create on behalf
            organizer = organizerRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("No organizer available in system. Please create an organizer first."));
        } else {
            organizer = organizerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BadRequestException("Organizer profile not found for user: " + userEmail));
        }

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.getVenueId()));

        if (!venue.isActive()) {
            throw new BadRequestException("Selected venue is currently inactive.");
        }

        validateBasicEventConstraints(request, venue);

        List<Event> conflicts = eventRepository.findConflictingEvents(
                venue.getId(),
                request.getEventDate(),
                request.getStartTime(),
                request.getEndTime(),
                null
        );

        Event event = new Event();
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription().trim());
        event.setCategory(request.getCategory().trim());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setVenue(venue);
        event.setOrganizer(organizer);
        event.setMaxCapacity(request.getMaxCapacity());
        event.setRegisteredCount(0);
        event.setRegistrationStartDate(request.getRegistrationStartDate());
        event.setRegistrationEndDate(request.getRegistrationEndDate());
        event.setEligibleBranches(cleanCommaSeparated(request.getEligibleBranches()));
        event.setEligibleAcademicYears(cleanCommaSeparated(request.getEligibleAcademicYears()));
        event.setEligibleSections(cleanCommaSeparated(request.getEligibleSections()));
        event.setEventImageUrl(request.getEventImageUrl());

        if (!conflicts.isEmpty()) {
            Event conflict = conflicts.get(0);
            event.setStatus(EventStatus.PENDING_APPROVAL);
            event.setHasVenueConflict(true);
            event.setConflictingEvent(conflict);
            event.setConflictNotes("Venue conflict with '" + conflict.getTitle() + "' at " + venue.getName()
                    + " (" + conflict.getStartTime() + " - " + conflict.getEndTime() + ")");

            // In-app notification for organizer
            notificationService.createNotification(
                    organizer.getUser(),
                    "Event Submitted for Approval (Venue Conflict)",
                    "Venue conflict detected. This event has been submitted for admin approval because the venue is already booked during this time.",
                    NotificationType.GENERAL
            );

            // In-app notification for administrators
            List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "Pending Event Approval: Venue Conflict",
                        "New event '" + event.getTitle() + "' requested for " + venue.getName() + " on "
                                + event.getEventDate() + " has a venue conflict and requires administrative review.",
                        NotificationType.EVENT_UPDATE
                );
            }
        } else {
            event.setStatus(request.getStatus() != null ? request.getStatus() : EventStatus.PUBLISHED);
            event.setHasVenueConflict(false);
            event.setConflictingEvent(null);
            event.setConflictNotes(null);
        }

        event = eventRepository.save(event);
        return mapToEventResponse(event, null);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventCreateRequest request, String userEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only Admin or the event's Organizer can update
        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to update this event.");
        }

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.getVenueId()));

        validateBasicEventConstraints(request, venue);

        List<Event> conflicts = eventRepository.findConflictingEvents(
                venue.getId(),
                request.getEventDate(),
                request.getStartTime(),
                request.getEndTime(),
                eventId
        );

        boolean venueOrTimeChanged = !event.getVenue().getId().equals(venue.getId())
                || !event.getEventDate().equals(request.getEventDate())
                || !event.getStartTime().equals(request.getStartTime())
                || !event.getEndTime().equals(request.getEndTime());

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription().trim());
        event.setCategory(request.getCategory().trim());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setVenue(venue);
        event.setMaxCapacity(request.getMaxCapacity());
        event.setRegistrationStartDate(request.getRegistrationStartDate());
        event.setRegistrationEndDate(request.getRegistrationEndDate());
        event.setEligibleBranches(cleanCommaSeparated(request.getEligibleBranches()));
        event.setEligibleAcademicYears(cleanCommaSeparated(request.getEligibleAcademicYears()));
        event.setEligibleSections(cleanCommaSeparated(request.getEligibleSections()));
        if (request.getEventImageUrl() != null) {
            event.setEventImageUrl(request.getEventImageUrl());
        }

        if (!conflicts.isEmpty()) {
            Event conflict = conflicts.get(0);
            event.setStatus(EventStatus.PENDING_APPROVAL);
            event.setHasVenueConflict(true);
            event.setConflictingEvent(conflict);
            event.setConflictNotes("Venue conflict with '" + conflict.getTitle() + "' at " + venue.getName()
                    + " (" + conflict.getStartTime() + " - " + conflict.getEndTime() + ")");

            // In-app notification for organizer
            notificationService.createNotification(
                    event.getOrganizer().getUser(),
                    "Event Update Awaiting Admin Approval",
                    "Venue conflict detected. Your event update has been submitted for admin approval because the venue is already booked during this time.",
                    NotificationType.GENERAL
            );

            // In-app notification for administrators
            List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "Pending Event Approval: Rescheduled Venue Conflict",
                        "Event '" + event.getTitle() + "' was rescheduled with a venue conflict at " + venue.getName()
                                + " on " + event.getEventDate() + " and requires administrative review.",
                        NotificationType.EVENT_UPDATE
                );
            }
        } else {
            if (event.getStatus() == EventStatus.PENDING_APPROVAL && event.isHasVenueConflict()) {
                event.setStatus(EventStatus.PUBLISHED);
            } else if (request.getStatus() != null) {
                event.setStatus(request.getStatus());
            }
            event.setHasVenueConflict(false);
            event.setConflictingEvent(null);
            event.setConflictNotes(null);

            // Notify registered participants if schedule changed and event remains published
            if (venueOrTimeChanged && event.getStatus() == EventStatus.PUBLISHED) {
                notifyParticipants(event, "Event Schedule/Venue Updated",
                        "The schedule or venue for '" + event.getTitle() + "' has been updated. New date: "
                        + event.getEventDate() + " at " + event.getStartTime() + " in " + event.getVenue().getName(),
                        NotificationType.EVENT_UPDATE);
            }
        }

        event = eventRepository.save(event);
        return mapToEventResponse(event, null);
    }

    @Transactional
    public void cancelEvent(Long eventId, String userEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ROLE_ADMIN && !event.getOrganizer().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not authorized to cancel this event.");
        }

        event.setStatus(EventStatus.CANCELLED);
        event.setAttendanceActive(false);
        eventRepository.save(event);

        notifyParticipants(event, "Event Cancelled",
                "The event '" + event.getTitle() + "' scheduled for " + event.getEventDate() + " has been CANCELLED.",
                NotificationType.EVENT_CANCELLATION);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsForDiscovery(String userEmail, String query, String category,
                                                     LocalDate startDate, LocalDate endDate) {
        Student student = null;
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_STUDENT) {
                student = studentRepository.findByUserId(user.getId()).orElse(null);
            }
        }

        LocalDate today = LocalDate.now();
        java.time.LocalTime currentTime = java.time.LocalTime.now();

        List<Event> events;
        // Admins can search across all institutional events including past/completed
        if (user != null && user.getRole() == Role.ROLE_ADMIN) {
            events = eventRepository.searchEvents(
                    null,
                    (category != null && !category.isBlank()) ? category.trim() : null,
                    startDate,
                    endDate,
                    (query != null && !query.isBlank()) ? query.trim() : null
            );
        } else {
            // Students & unauthenticated public: strictly upcoming published events whose end time hasn't passed!
            events = eventRepository.searchUpcomingPublishedEvents(
                    today,
                    currentTime,
                    (category != null && !category.isBlank()) ? category.trim() : null,
                    startDate,
                    endDate,
                    (query != null && !query.isBlank()) ? query.trim() : null
            );
        }

        final Student finalStudent = student;
        return events.stream()
                .map(e -> mapToEventResponse(e, finalStudent))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id, String userEmail) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        Student student = null;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_STUDENT) {
                student = studentRepository.findByUserId(user.getId()).orElse(null);
            }
        }

        return mapToEventResponse(event, student);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getOrganizerEvents(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ROLE_ADMIN) {
            return eventRepository.findAll().stream()
                    .map(e -> mapToEventResponse(e, null))
                    .collect(Collectors.toList());
        }

        Organizer organizer = organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer profile not found"));

        return eventRepository.findByOrganizerOrderByEventDateDescStartTimeDesc(organizer).stream()
                .map(e -> mapToEventResponse(e, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizerDashboardStats getOrganizerDashboardStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organizer organizer = organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer profile not found"));

        List<Event> events = eventRepository.findByOrganizerOrderByEventDateDescStartTimeDesc(organizer);
        LocalDate today = LocalDate.now();

        long totalEvents = events.size();
        long upcomingEvents = events.stream().filter(e -> e.getEventDate().isAfter(today) || (e.getEventDate().isEqual(today) && e.getStatus() != EventStatus.COMPLETED && e.getStatus() != EventStatus.CANCELLED)).count();
        long completedEvents = events.stream().filter(e -> e.getStatus() == EventStatus.COMPLETED || e.getEventDate().isBefore(today)).count();
        long todayEvents = events.stream().filter(e -> e.getEventDate().isEqual(today)).count();
        long totalParticipants = events.stream().mapToInt(Event::getRegisteredCount).sum();

        // Calculate attendance rate for organizer's events
        long totalAttended = 0;
        for (Event e : events) {
            totalAttended += attendanceRepository.countByEventIdAndStatus(e.getId(), AttendanceStatus.PRESENT);
        }
        double attendanceRate = totalParticipants > 0
                ? Math.round(((double) totalAttended / totalParticipants) * 1000.0) / 10.0
                : 0.0;

        return new OrganizerDashboardStats(
                totalEvents,
                upcomingEvents,
                completedEvents,
                totalParticipants,
                todayEvents,
                attendanceRate
        );
    }

    private void validateBasicEventConstraints(EventCreateRequest request, Venue venue) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Event start time (" + request.getStartTime() + ") must be before end time (" + request.getEndTime() + ").");
        }

        if (request.getRegistrationStartDate().isAfter(request.getRegistrationEndDate())) {
            throw new BadRequestException("Registration start date cannot be after registration end date.");
        }

        if (request.getRegistrationEndDate().isAfter(request.getEventDate())) {
            throw new BadRequestException("Registration end date cannot be after the event date.");
        }

        if (request.getMaxCapacity() > venue.getCapacity()) {
            throw new BadRequestException("Maximum capacity (" + request.getMaxCapacity() + ") cannot exceed venue capacity (" + venue.getCapacity() + ") for " + venue.getName() + ".");
        }
    }

    @Transactional
    public EventResponse approveEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        event.setStatus(EventStatus.PUBLISHED);
        event.setHasVenueConflict(false);
        event = eventRepository.save(event);

        notificationService.createNotification(
                event.getOrganizer().getUser(),
                "Event Approved by Administrator",
                "Your event '" + event.getTitle() + "' scheduled on " + event.getEventDate() + " has been approved and is now published.",
                NotificationType.GENERAL
        );

        return mapToEventResponse(event, null);
    }

    @Transactional
    public EventResponse rejectEvent(Long eventId, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        event.setStatus(EventStatus.REJECTED);
        if (reason != null && !reason.isBlank()) {
            event.setConflictNotes("Rejected by admin: " + reason.trim());
        }
        event = eventRepository.save(event);

        notificationService.createNotification(
                event.getOrganizer().getUser(),
                "Event Request Rejected",
                "Your event '" + event.getTitle() + "' was rejected by the administration. Reason: "
                        + (reason != null ? reason : "Administrative decision / Venue conflict."),
                NotificationType.EVENT_CANCELLATION
        );

        return mapToEventResponse(event, null);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPendingEvents() {
        return eventRepository.findByStatusOrderByCreatedAtDesc(EventStatus.PENDING_APPROVAL).stream()
                .map(e -> mapToEventResponse(e, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkVenueConflict(Long venueId, LocalDate eventDate, LocalTime startTime, LocalTime endTime, Long excludeEventId) {
        Map<String, Object> result = new HashMap<>();
        if (venueId == null || eventDate == null || startTime == null || endTime == null) {
            result.put("hasConflict", false);
            return result;
        }

        List<Event> conflicts = eventRepository.findConflictingEvents(venueId, eventDate, startTime, endTime, excludeEventId);
        if (conflicts.isEmpty()) {
            result.put("hasConflict", false);
        } else {
            Event c = conflicts.get(0);
            result.put("hasConflict", true);
            EventResponse.ConflictingEventSummary summary = new EventResponse.ConflictingEventSummary(
                    c.getId(),
                    c.getTitle(),
                    c.getVenue().getName(),
                    c.getEventDate(),
                    c.getStartTime(),
                    c.getEndTime(),
                    c.getOrganizer().getName()
            );
            result.put("conflictingEvent", summary);
            result.put("warningMessage", "Venue conflict: another event ('" + c.getTitle() + "') is already scheduled at "
                    + c.getVenue().getName() + " during this time slot (" + c.getStartTime() + " - " + c.getEndTime() + ").");
        }
        return result;
    }

    public EventResponse mapToEventResponse(Event event, Student student) {
        EventResponse r = new EventResponse();
        r.setId(event.getId());
        r.setTitle(event.getTitle());
        r.setDescription(event.getDescription());
        r.setCategory(event.getCategory());
        r.setEventDate(event.getEventDate());
        r.setStartTime(event.getStartTime());
        r.setEndTime(event.getEndTime());
        r.setVenue(venueService.mapToResponse(event.getVenue()));

        OrganizerResponse orgRes = new OrganizerResponse();
        orgRes.setId(event.getOrganizer().getId());
        orgRes.setName(event.getOrganizer().getName());
        orgRes.setDepartment(event.getOrganizer().getDepartment());
        orgRes.setContactEmail(event.getOrganizer().getContactEmail());
        r.setOrganizer(orgRes);

        r.setMaxCapacity(event.getMaxCapacity());
        r.setRegisteredCount(event.getRegisteredCount());
        int remaining = Math.max(0, event.getMaxCapacity() - event.getRegisteredCount());
        r.setRemainingSeats(remaining);

        double pct = event.getMaxCapacity() > 0
                ? Math.round(((double) event.getRegisteredCount() / event.getMaxCapacity()) * 1000.0) / 10.0
                : 0.0;
        r.setCapacityPercentage(pct);

        r.setRegistrationStartDate(event.getRegistrationStartDate());
        r.setRegistrationEndDate(event.getRegistrationEndDate());
        r.setEligibleBranches(event.getEligibleBranches());
        r.setEligibleAcademicYears(event.getEligibleAcademicYears());
        r.setEligibleSections(event.getEligibleSections());
        r.setEventImageUrl(event.getEventImageUrl());
        r.setStatus(event.getStatus());
        r.setAttendanceActive(event.isAttendanceActive());
        r.setAttendanceToken(event.getAttendanceToken());

        // Venue conflict mapping
        r.setHasVenueConflict(event.isHasVenueConflict());
        r.setConflictNotes(event.getConflictNotes());
        if (event.getConflictingEvent() != null) {
            Event c = event.getConflictingEvent();
            EventResponse.ConflictingEventSummary summary = new EventResponse.ConflictingEventSummary(
                    c.getId(),
                    c.getTitle(),
                    c.getVenue().getName(),
                    c.getEventDate(),
                    c.getStartTime(),
                    c.getEndTime(),
                    c.getOrganizer().getName()
            );
            r.setConflictingEvent(summary);
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        boolean isPast = event.getStatus() == EventStatus.COMPLETED
                || event.getEventDate().isBefore(today)
                || (event.getEventDate().isEqual(today) && event.getEndTime().isBefore(now));

        // Distinct registration state calculation: NOT_STARTED, OPEN, CLOSED
        String regState;
        if (today.isBefore(event.getRegistrationStartDate())) {
            regState = "NOT_STARTED";
        } else if (today.isAfter(event.getRegistrationEndDate())) {
            regState = "CLOSED";
        } else {
            regState = "OPEN";
        }
        r.setRegistrationState(regState);

        boolean isOpen = "OPEN".equals(regState) && event.getStatus() == EventStatus.PUBLISHED && !isPast;
        r.setRegistrationOpen(isOpen);
        r.setFull(remaining <= 0);

        // Compute eligibility and action status for student
        if (student != null) {
            boolean eligible = checkStudentEligibility(event, student);
            r.setEligible(eligible);
            if (!eligible) {
                r.setEligibilityMessage("You are not eligible to register for this event.");
            }

            Optional<Registration> regOpt = registrationRepository.findByEventIdAndStudentId(event.getId(), student.getId());
            if (regOpt.isPresent() && regOpt.get().getStatus() != RegistrationStatus.CANCELLED) {
                r.setRegistered(true);
                r.setRegistrationStatus(regOpt.get().getStatus().name());
                r.setActionStatus("REGISTERED");
            } else if (event.getStatus() == EventStatus.CANCELLED) {
                r.setActionStatus("CANCELLED");
            } else if (event.getStatus() == EventStatus.PENDING_APPROVAL) {
                r.setActionStatus("PENDING APPROVAL");
            } else if (event.getStatus() == EventStatus.REJECTED) {
                r.setActionStatus("REJECTED");
            } else if (isPast) {
                r.setActionStatus("COMPLETED");
            } else if (!eligible) {
                r.setActionStatus("NOT ELIGIBLE");
            } else if (remaining <= 0) {
                r.setActionStatus("FULL");
            } else if ("NOT_STARTED".equals(regState)) {
                r.setActionStatus("REGISTRATION NOT STARTED");
            } else if ("CLOSED".equals(regState)) {
                r.setActionStatus("REGISTRATION CLOSED");
            } else {
                r.setActionStatus("REGISTER NOW");
            }
        } else {
            if (event.getStatus() == EventStatus.CANCELLED) {
                r.setActionStatus("CANCELLED");
            } else if (event.getStatus() == EventStatus.PENDING_APPROVAL) {
                r.setActionStatus("PENDING APPROVAL");
            } else if (event.getStatus() == EventStatus.REJECTED) {
                r.setActionStatus("REJECTED");
            } else if (isPast) {
                r.setActionStatus("COMPLETED");
            } else if (remaining <= 0) {
                r.setActionStatus("FULL");
            } else if ("NOT_STARTED".equals(regState)) {
                r.setActionStatus("REGISTRATION NOT STARTED");
            } else if ("CLOSED".equals(regState)) {
                r.setActionStatus("REGISTRATION CLOSED");
            } else {
                r.setActionStatus("REGISTER NOW");
            }
        }

        return r;
    }

    public boolean checkStudentEligibility(Event event, Student student) {
        // Branch check
        String branches = event.getEligibleBranches();
        if (branches != null && !branches.isBlank()) {
            List<String> list = Arrays.stream(branches.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            if (!list.contains(student.getBranch().toLowerCase())) {
                return false;
            }
        }

        // Year check
        String years = event.getEligibleAcademicYears();
        if (years != null && !years.isBlank()) {
            List<String> list = Arrays.stream(years.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            if (!list.contains(student.getCurrentYear().toLowerCase()) && !list.contains(student.getAcademicYear().toLowerCase())) {
                return false;
            }
        }

        // Section check
        String sections = event.getEligibleSections();
        if (sections != null && !sections.isBlank()) {
            List<String> list = Arrays.stream(sections.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            if (!list.contains(student.getSection().toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    private void notifyParticipants(Event event, String title, String message, NotificationType type) {
        List<Registration> registrations = registrationRepository.findByEvent(event);
        for (Registration r : registrations) {
            notificationService.createNotification(r.getStudent().getUser(), title, message, type);
        }
    }

    private String cleanCommaSeparated(String input) {
        if (input == null || input.isBlank()) return "";
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
