package com.college.eventmanagement;

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

public class EventVisibilityTest {

    private EventService eventService;
    private Venue defaultVenue;
    private Organizer defaultOrganizer;

    // Test data stores
    private List<Event> allRepositoryEvents;
    private User testStudentUser;
    private Student testStudent;
    private User testAdminUser;

    @BeforeEach
    void setUp() {
        allRepositoryEvents = new ArrayList<>();

        defaultVenue = new Venue();
        defaultVenue.setId(1L);
        defaultVenue.setName("Auditorium Hall A");
        defaultVenue.setBuilding("Main Academic Block");
        defaultVenue.setCapacity(200);
        defaultVenue.setActive(true);

        User orgUser = new User();
        orgUser.setId(10L);
        orgUser.setEmail("sharma@college.edu");
        orgUser.setRole(Role.ROLE_ORGANIZER);

        defaultOrganizer = new Organizer();
        defaultOrganizer.setId(1L);
        defaultOrganizer.setUser(orgUser);
        defaultOrganizer.setName("Dr. Sharma");
        defaultOrganizer.setDepartment("Computer Science");
        defaultOrganizer.setContactEmail("sharma@college.edu");

        testStudentUser = new User();
        testStudentUser.setId(20L);
        testStudentUser.setEmail("rahul@college.edu");
        testStudentUser.setRole(Role.ROLE_STUDENT);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setUser(testStudentUser);
        testStudent.setFullName("Rahul Verma");
        testStudent.setBranch("CSE");
        testStudent.setCurrentYear("2nd Year");
        testStudent.setSection("A");
        testStudent.setApprovalStatus(ApprovalStatus.APPROVED);

        testAdminUser = new User();
        testAdminUser.setId(99L);
        testAdminUser.setEmail("admin@college.edu");
        testAdminUser.setRole(Role.ROLE_ADMIN);

        // Proxy EventRepository
        EventRepository eventRepository = (EventRepository) Proxy.newProxyInstance(
                EventRepository.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("searchUpcomingPublishedEvents".equals(name)) {
                        LocalDate today = (LocalDate) args[0];
                        LocalTime now = (LocalTime) args[1];
                        List<Event> result = new ArrayList<>();
                        for (Event e : allRepositoryEvents) {
                            if (e.getStatus() == EventStatus.PUBLISHED) {
                                boolean isFuture = e.getEventDate().isAfter(today);
                                boolean isTodayUpcoming = e.getEventDate().isEqual(today) && e.getEndTime().isAfter(now);
                                if (isFuture || isTodayUpcoming) {
                                    result.add(e);
                                }
                            }
                        }
                        return result;
                    }
                    if ("searchEvents".equals(name)) {
                        return new ArrayList<>(allRepositoryEvents);
                    }
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return allRepositoryEvents.stream().filter(e -> e.getId().equals(id)).findFirst();
                    }
                    return null;
                }
        );

        // Proxy UserRepository
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String email = (String) args[0];
                    if ("rahul@college.edu".equals(email)) return Optional.of(testStudentUser);
                    if ("admin@college.edu".equals(email)) return Optional.of(testAdminUser);
                    return Optional.empty();
                }
        );

        // Proxy StudentRepository
        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> {
                    Long userId = (Long) args[0];
                    if (Long.valueOf(20L).equals(userId)) return Optional.of(testStudent);
                    return Optional.empty();
                }
        );

        // Proxy RegistrationRepository
        RegistrationRepository registrationRepository = (RegistrationRepository) Proxy.newProxyInstance(
                RegistrationRepository.class.getClassLoader(),
                new Class<?>[]{RegistrationRepository.class},
                (proxy, method, args) -> Optional.empty()
        );

        VenueService venueService = new VenueService(null);

        eventService = new EventService(
                eventRepository,
                null,
                null,
                userRepository,
                studentRepository,
                registrationRepository,
                null,
                null,
                venueService
        );
    }

    private Event createSampleEvent(Long id, String title, LocalDate eventDate, LocalTime startTime, LocalTime endTime, EventStatus status) {
        Event e = new Event();
        e.setId(id);
        e.setTitle(title);
        e.setDescription("Event description for " + title);
        e.setCategory("Technical");
        e.setVenue(defaultVenue);
        e.setOrganizer(defaultOrganizer);
        e.setEventDate(eventDate);
        e.setStartTime(startTime);
        e.setEndTime(endTime);
        e.setMaxCapacity(100);
        e.setRegisteredCount(10);
        e.setRegistrationStartDate(eventDate.minusDays(5));
        e.setRegistrationEndDate(eventDate.minusDays(1));
        e.setStatus(status);
        return e;
    }

    @Test
    @DisplayName("Students cannot see past events whose date has passed in discovery")
    void testPastEventsHiddenFromStudentDiscovery() {
        LocalDate today = LocalDate.now();

        Event pastEvent = createSampleEvent(1L, "Past Symposium", today.minusDays(2), LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED);
        Event upcomingEvent = createSampleEvent(2L, "Upcoming AI Hackathon", today.plusDays(3), LocalTime.of(10, 0), LocalTime.of(16, 0), EventStatus.PUBLISHED);

        allRepositoryEvents.add(pastEvent);
        allRepositoryEvents.add(upcomingEvent);

        List<EventResponse> discovery = eventService.getEventsForDiscovery("rahul@college.edu", null, null, null, null);

        assertEquals(1, discovery.size());
        assertEquals("Upcoming AI Hackathon", discovery.get(0).getTitle());
    }

    @Test
    @DisplayName("Students cannot see events whose end time passed earlier today")
    void testTodayCompletedEventsHiddenFromStudentDiscovery() {
        LocalDate today = LocalDate.now();

        // An event from 00:00 to 00:01 today (guaranteed ended unless executed at midnight)
        Event finishedToday = createSampleEvent(1L, "Morning Briefing", today, LocalTime.of(0, 1), LocalTime.of(0, 2), EventStatus.PUBLISHED);
        // An event far in the future today or tomorrow
        Event futureEvent = createSampleEvent(2L, "Future Symposium", today.plusDays(1), LocalTime.of(14, 0), LocalTime.of(16, 0), EventStatus.PUBLISHED);

        allRepositoryEvents.add(finishedToday);
        allRepositoryEvents.add(futureEvent);

        List<EventResponse> discovery = eventService.getEventsForDiscovery("rahul@college.edu", null, null, null, null);

        assertEquals(1, discovery.size());
        assertEquals("Future Symposium", discovery.get(0).getTitle());
    }

    @Test
    @DisplayName("Students cannot see events with status COMPLETED, PENDING_APPROVAL, REJECTED, or CANCELLED")
    void testNonPublishedAndCompletedEventsHiddenFromStudents() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Event completed = createSampleEvent(1L, "Completed Fest", tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.COMPLETED);
        Event pending = createSampleEvent(2L, "Pending Tech Day", tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PENDING_APPROVAL);
        Event rejected = createSampleEvent(3L, "Rejected Coding Clash", tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.REJECTED);
        Event cancelled = createSampleEvent(4L, "Cancelled Robotics", tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.CANCELLED);
        Event published = createSampleEvent(5L, "Live Published Event", tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED);

        allRepositoryEvents.addAll(List.of(completed, pending, rejected, cancelled, published));

        List<EventResponse> discovery = eventService.getEventsForDiscovery("rahul@college.edu", null, null, null, null);

        assertEquals(1, discovery.size());
        assertEquals("Live Published Event", discovery.get(0).getTitle());
    }

    @Test
    @DisplayName("Administrator can view all institutional events including completed in audit search")
    void testAdminCanSearchAllEvents() {
        LocalDate today = LocalDate.now();

        Event pastEvent = createSampleEvent(1L, "Past Symposium", today.minusDays(5), LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.COMPLETED);
        Event upcomingEvent = createSampleEvent(2L, "Upcoming AI Hackathon", today.plusDays(3), LocalTime.of(10, 0), LocalTime.of(16, 0), EventStatus.PUBLISHED);

        allRepositoryEvents.add(pastEvent);
        allRepositoryEvents.add(upcomingEvent);

        List<EventResponse> adminEvents = eventService.getEventsForDiscovery("admin@college.edu", null, null, null, null);

        assertEquals(2, adminEvents.size(), "Admin should be able to view all events including past/completed for reports");
    }

    @Test
    @DisplayName("Event response marks completed events with actionStatus COMPLETED and registrationOpen false")
    void testMapToEventResponseActionStatusForCompletedEvent() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Event pastEvent = createSampleEvent(1L, "Historical Fest", yesterday, LocalTime.of(10, 0), LocalTime.of(12, 0), EventStatus.PUBLISHED);

        EventResponse response = eventService.mapToEventResponse(pastEvent, testStudent);

        assertFalse(response.isRegistrationOpen());
        assertEquals("COMPLETED", response.getActionStatus());
    }
}
