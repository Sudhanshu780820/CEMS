package com.college.eventmanagement;

import com.college.eventmanagement.dto.EventCreateRequest;
import com.college.eventmanagement.dto.EventResponse;
import com.college.eventmanagement.entity.*;
import com.college.eventmanagement.repository.*;
import com.college.eventmanagement.service.EventService;
import com.college.eventmanagement.service.NotificationService;
import com.college.eventmanagement.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class VenueConflictTest {

    private EventService eventService;

    private Venue venue1;
    private Venue venue2;
    private Organizer testOrganizer;
    private User orgUser;

    private List<Event> existingEventsInDb;
    private List<Event> savedEvents;

    @BeforeEach
    void setUp() {
        existingEventsInDb = new ArrayList<>();
        savedEvents = new ArrayList<>();

        venue1 = new Venue();
        venue1.setId(10L);
        venue1.setName("Main Auditorium");
        venue1.setBuilding("Block A");
        venue1.setCapacity(300);
        venue1.setActive(true);

        venue2 = new Venue();
        venue2.setId(20L);
        venue2.setName("Seminar Hall 1");
        venue2.setBuilding("Block B");
        venue2.setCapacity(100);
        venue2.setActive(true);

        orgUser = new User();
        orgUser.setId(5L);
        orgUser.setEmail("anita@college.edu");
        orgUser.setRole(Role.ROLE_ORGANIZER);

        testOrganizer = new Organizer();
        testOrganizer.setId(1L);
        testOrganizer.setUser(orgUser);
        testOrganizer.setName("Dr. Anita");
        testOrganizer.setDepartment("Computer Science");
        testOrganizer.setContactEmail("anita@college.edu");

        // Proxy VenueRepository
        VenueRepository venueRepository = (VenueRepository) Proxy.newProxyInstance(
                VenueRepository.class.getClassLoader(),
                new Class<?>[]{VenueRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        if (Long.valueOf(10L).equals(id)) return Optional.of(venue1);
                        if (Long.valueOf(20L).equals(id)) return Optional.of(venue2);
                        return Optional.empty();
                    }
                    return null;
                }
        );

        // Proxy UserRepository
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEmail".equals(name)) {
                        String email = (String) args[0];
                        if ("anita@college.edu".equals(email)) return Optional.of(orgUser);
                        return Optional.empty();
                    }
                    if ("findByRole".equals(name)) {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        // Proxy OrganizerRepository
        OrganizerRepository organizerRepository = (OrganizerRepository) Proxy.newProxyInstance(
                OrganizerRepository.class.getClassLoader(),
                new Class<?>[]{OrganizerRepository.class},
                (proxy, method, args) -> Optional.of(testOrganizer)
        );

        // Proxy EventRepository simulating interval overlap query:
        // existing.startTime < requested.endTime AND existing.endTime > requested.startTime
        EventRepository eventRepository = (EventRepository) Proxy.newProxyInstance(
                EventRepository.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findConflictingEvents".equals(name)) {
                        Long venueId = (Long) args[0];
                        LocalDate eventDate = (LocalDate) args[1];
                        LocalTime startTime = (LocalTime) args[2];
                        LocalTime endTime = (LocalTime) args[3];
                        Long excludeId = (Long) args[4];

                        List<Event> conflicts = new ArrayList<>();
                        for (Event e : existingEventsInDb) {
                            if (excludeId != null && e.getId() != null && e.getId().equals(excludeId)) {
                                continue;
                            }
                            // Only active booking statuses conflict
                            boolean isBookingActive = e.getStatus() == EventStatus.PUBLISHED
                                    || e.getStatus() == EventStatus.REGISTRATION_OPEN
                                    || e.getStatus() == EventStatus.REGISTRATION_CLOSED
                                    || e.getStatus() == EventStatus.ONGOING
                                    || e.getStatus() == EventStatus.PENDING_APPROVAL;
                            if (!isBookingActive) continue;

                            if (e.getVenue().getId().equals(venueId) && e.getEventDate().equals(eventDate)) {
                                // Overlap logic: existing.start < requested.end AND existing.end > requested.start
                                if (e.getStartTime().isBefore(endTime) && e.getEndTime().isAfter(startTime)) {
                                    conflicts.add(e);
                                }
                            }
                        }
                        return conflicts;
                    }
                    if ("save".equals(name)) {
                        Event e = (Event) args[0];
                        if (e.getId() == null) {
                            e.setId(new Random().nextLong(1000L, 9999L));
                        }
                        savedEvents.add(e);
                        existingEventsInDb.add(e);
                        return e;
                    }
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return existingEventsInDb.stream().filter(e -> e.getId().equals(id)).findFirst();
                    }
                    return null;
                }
        );

        NotificationRepository notificationRepository = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return null;
                }
        );

        NotificationService notificationService = new NotificationService(notificationRepository, userRepository);

        VenueService venueService = new VenueService(venueRepository);

        eventService = new EventService(
                eventRepository,
                venueRepository,
                organizerRepository,
                userRepository,
                null,
                null,
                null,
                notificationService,
                venueService
        );
    }

    private Event createExistingEvent(Long id, Venue venue, LocalDate date, LocalTime start, LocalTime end, EventStatus status) {
        Event e = new Event();
        e.setId(id);
        e.setTitle("Existing Annual Meet");
        e.setDescription("Pre-existing event in venue");
        e.setCategory("Technical");
        e.setVenue(venue);
        e.setOrganizer(testOrganizer);
        e.setEventDate(date);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setMaxCapacity(150);
        e.setRegistrationStartDate(date.minusDays(5));
        e.setRegistrationEndDate(date.minusDays(1));
        e.setStatus(status);
        return e;
    }

    private EventCreateRequest createRequest(Venue venue, LocalDate date, LocalTime start, LocalTime end) {
        EventCreateRequest req = new EventCreateRequest();
        req.setTitle("Requested Robotics Workshop");
        req.setDescription("Autonomous robotics workshop");
        req.setCategory("Workshop");
        req.setVenueId(venue.getId());
        req.setEventDate(date);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setMaxCapacity(100);
        req.setRegistrationStartDate(date.minusDays(4));
        req.setRegistrationEndDate(date.minusDays(1));
        return req;
    }

    @Test
    @DisplayName("Partial overlap at start detects venue conflict")
    void testPartialOverlapStart() {
        LocalDate date = LocalDate.now().plusDays(5);
        // Existing: 10:00 to 12:00
        existingEventsInDb.add(createExistingEvent(1L, venue1, date, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED));

        // Requested: 11:00 to 13:00 (overlaps 11:00 to 12:00)
        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(11, 0), LocalTime.of(13, 0), null);
        assertTrue((Boolean) conflict.get("hasConflict"));
        assertNotNull(conflict.get("conflictingEvent"));
    }

    @Test
    @DisplayName("Partial overlap at end detects venue conflict")
    void testPartialOverlapEnd() {
        LocalDate date = LocalDate.now().plusDays(5);
        // Existing: 11:00 to 13:00
        existingEventsInDb.add(createExistingEvent(1L, venue1, date, LocalTime.of(11, 0), LocalTime.of(13, 0), EventStatus.PUBLISHED));

        // Requested: 10:00 to 12:00 (overlaps 11:00 to 12:00)
        Map<String, Object> conflict = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        assertTrue((Boolean) conflict.get("hasConflict"));
    }

    @Test
    @DisplayName("Encompassing and subset intervals detect venue conflict")
    void testEncompassingAndSubsetIntervals() {
        LocalDate date = LocalDate.now().plusDays(5);
        // Existing: 11:00 to 13:00
        existingEventsInDb.add(createExistingEvent(1L, venue1, date, LocalTime.of(11, 0), LocalTime.of(13, 0), EventStatus.PUBLISHED));

        // Requested encompasses existing: 09:00 to 15:00
        Map<String, Object> encompassing = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(9, 0), LocalTime.of(15, 0), null);
        assertTrue((Boolean) encompassing.get("hasConflict"));

        // Requested subset inside existing: 11:30 to 12:30
        Map<String, Object> subset = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(11, 30), LocalTime.of(12, 30), null);
        assertTrue((Boolean) subset.get("hasConflict"));
    }

    @Test
    @DisplayName("Back-to-back events (10:00-12:00 and 12:00-14:00) are allowed and do NOT conflict")
    void testBackToBackEventsAllowed() {
        LocalDate date = LocalDate.now().plusDays(5);
        // Existing: 10:00 to 12:00
        existingEventsInDb.add(createExistingEvent(1L, venue1, date, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED));

        // Requested immediately after: 12:00 to 14:00 (12:00 > 12:00 is false => NO overlap)
        Map<String, Object> after = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(12, 0), LocalTime.of(14, 0), null);
        assertFalse((Boolean) after.get("hasConflict"), "Back-to-back booking immediately after should not conflict");

        // Requested immediately before: 08:00 to 10:00 (10:00 < 10:00 is false => NO overlap)
        Map<String, Object> before = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(8, 0), LocalTime.of(10, 0), null);
        assertFalse((Boolean) before.get("hasConflict"), "Back-to-back booking immediately before should not conflict");
    }

    @Test
    @DisplayName("Same time slot on DIFFERENT venues or DIFFERENT dates does NOT conflict")
    void testDifferentVenuesOrDatesDoNotConflict() {
        LocalDate date = LocalDate.now().plusDays(5);
        // Existing at Venue 1: 10:00 to 12:00
        existingEventsInDb.add(createExistingEvent(1L, venue1, date, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED));

        // Same time, different venue (Venue 2)
        Map<String, Object> diffVenue = eventService.checkVenueConflict(venue2.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        assertFalse((Boolean) diffVenue.get("hasConflict"));

        // Same time, same venue, different date (date + 1)
        Map<String, Object> diffDate = eventService.checkVenueConflict(venue1.getId(), date.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        assertFalse((Boolean) diffDate.get("hasConflict"));
    }

    @Test
    @DisplayName("Cancelled or Rejected events do NOT block venue slots")
    void testInactiveStatusesDoNotBlockVenue() {
        LocalDate date = LocalDate.now().plusDays(5);
        // Existing is CANCELLED
        existingEventsInDb.add(createExistingEvent(1L, venue1, date, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.CANCELLED));
        // Existing is REJECTED
        existingEventsInDb.add(createExistingEvent(2L, venue1, date, LocalTime.of(14, 0), LocalTime.of(16, 0), EventStatus.REJECTED));

        Map<String, Object> conflict1 = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        assertFalse((Boolean) conflict1.get("hasConflict"));

        Map<String, Object> conflict2 = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(14, 0), LocalTime.of(16, 0), null);
        assertFalse((Boolean) conflict2.get("hasConflict"));
    }

    @Test
    @DisplayName("Self-exclusion when updating an event prevents false self-conflict")
    void testSelfConflictExclusionOnUpdate() {
        LocalDate date = LocalDate.now().plusDays(5);
        Event existing = createExistingEvent(55L, venue1, date, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED);
        existingEventsInDb.add(existing);

        // Check conflict with excludeEventId = 55
        Map<String, Object> check = eventService.checkVenueConflict(venue1.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0), 55L);
        assertFalse((Boolean) check.get("hasConflict"), "Editing own event should exclude itself from conflict checking");
    }

    @Test
    @DisplayName("Submitting event with venue conflict routes to Admin approval with PENDING_APPROVAL and hasVenueConflict=true")
    void testConflictRoutesToAdminApproval() {
        LocalDate date = LocalDate.now().plusDays(5);
        Event existing = createExistingEvent(1L, venue1, date, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED);
        existingEventsInDb.add(existing);

        // Request overlapping event: 11:00 to 13:00
        EventCreateRequest req = createRequest(venue1, date, LocalTime.of(11, 0), LocalTime.of(13, 0));

        EventResponse response = eventService.createEvent(req, "anita@college.edu");

        assertNotNull(response);
        assertEquals(EventStatus.PENDING_APPROVAL, response.getStatus(), "Conflicting event must receive PENDING_APPROVAL status");
        assertTrue(response.isHasVenueConflict(), "Conflicting event must have hasVenueConflict=true");
        assertNotNull(response.getConflictingEvent(), "Conflicting event summary must be populated");
        assertEquals("Existing Annual Meet", response.getConflictingEvent().getTitle());
        assertTrue(response.getConflictNotes().contains("Venue conflict with 'Existing Annual Meet'"));
    }

    @Test
    @DisplayName("Admin approval workflow: approve overrides conflict to PUBLISHED; reject marks REJECTED")
    void testAdminApprovalAndRejection() {
        LocalDate date = LocalDate.now().plusDays(5);
        Event pendingEvent = createExistingEvent(77L, venue1, date, LocalTime.of(11, 0), LocalTime.of(13, 0), EventStatus.PENDING_APPROVAL);
        pendingEvent.setHasVenueConflict(true);
        pendingEvent.setConflictNotes("Venue conflict with Annual Meet");
        existingEventsInDb.add(pendingEvent);

        // 1. Admin Approves
        EventResponse approved = eventService.approveEvent(77L);
        assertEquals(EventStatus.PUBLISHED, approved.getStatus());
        assertFalse(approved.isHasVenueConflict());

        // 2. Admin Rejects another pending event
        Event pendingEvent2 = createExistingEvent(88L, venue1, date, LocalTime.of(14, 0), LocalTime.of(16, 0), EventStatus.PENDING_APPROVAL);
        existingEventsInDb.add(pendingEvent2);

        EventResponse rejected = eventService.rejectEvent(88L, "Auditorium reserved for Chancellor visit.");
        assertEquals(EventStatus.REJECTED, rejected.getStatus());
        assertTrue(rejected.getConflictNotes().contains("Auditorium reserved for Chancellor visit."));
    }
}
